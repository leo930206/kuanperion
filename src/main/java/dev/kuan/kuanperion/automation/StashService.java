// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Main-thread orchestration; runtime state never persists or mutates legacy Farming/Mining. */
public final class StashService {
 public record Notice(String text,boolean warning,String feature){
  public Notice(String text,boolean warning){this(text,warning,"Trade");}
 }
 private final AutomationSettingsStore settings;
 private final StashTask task;
 private final Supplier<AutomationContext> context;
 private final AutomationCommand.Sender sender;
 private final Consumer<Notice> feedback;
 private boolean sendingOwnCommand,sessionSuspended,reportedStart;
 private StashTask.State reported=StashTask.State.IDLE;
 public StashService(AutomationSettingsStore settings,AutomationCoordinator coordinator,Supplier<AutomationContext> context,AutomationCommand.Sender sender,Consumer<Notice> feedback){
  this.settings=Objects.requireNonNull(settings);task=new StashTask(coordinator);this.context=Objects.requireNonNull(context);this.sender=Objects.requireNonNull(sender);this.feedback=Objects.requireNonNull(feedback);
 }
 public AutomationSettingsStore settings(){return settings;}
 public boolean autoEnabled(){return settings.enabled(AutomationFeature.STASH)&&!sessionSuspended;}
 public boolean active(){return task.active();}public StashTask.State state(){return task.state();}public int attempts(){return task.attempts();}
 public void setEnabled(boolean enabled)throws IOException {
  // An IO failure must not turn a user's stop into continued background operations.
  if(!enabled){sessionSuspended=true;cancel("已關閉自動領取");}
  settings.setEnabled(AutomationFeature.STASH,enabled);
  if(enabled)sessionSuspended=false;
 }
 public void setPolicy(StashTask.Policy policy)throws IOException {
  if(active())cancel("設定變更，本次已停止");
  try{settings.setStashPolicy(policy);}catch(IOException failure){sessionSuspended=true;throw failure;}
 }
 public boolean manualRequest(long now){
  if(active()||!settings.writable())return false;long run=task.runId();var observed=current();
  if(run!=task.runId()||observed==null||observed.hardBlocked()||observed.blockers().contains(AutomationContext.Blocker.INVENTORY_FULL))return false;
  return request(observed,true,now);
 }
 private AutomationContext current(){try{return context.get();}catch(RuntimeException unavailable){return null;}}
 private void stopForUnavailable(){sessionSuspended=true;cancel("無法確認遊戲狀態");}
 private boolean request(AutomationContext observed,boolean manual,long now){
  if(!task.request(observed,settings.stashPolicy(),manual,now))return false;
  reportedStart=false;reported=StashTask.State.WAITING;return true;
 }
 /** Only the Fabric server GAME callback calls this; player CHAT is intentionally excluded. */
 public void onSystemMessage(String text,long now){
  var signal=StashTask.classify(text);if(signal==StashTask.Signal.NONE)return;
  if(signal==StashTask.Signal.ADDED){
   if(active()||!autoEnabled())return;long run=task.runId();var observed=current();
   if(run==task.runId()&&!active()&&autoEnabled()&&observed!=null)request(observed,false,now);return;
  }
  if(active()){
   long run=task.runId();var observed=current();if(run!=task.runId()||!active())return;
   if(observed==null){stopForUnavailable();return;}task.onSignal(signal,observed);reportTerminal();
  }
 }
 public void tick(long now){
  if(!active())return;
  long run=task.runId();var observed=current();if(run!=task.runId()||!active())return;
  if(observed==null){stopForUnavailable();return;}
  task.tick(observed,now,command->{
   boolean previous=sendingOwnCommand;sendingOwnCommand=true;
   try{return sender.send(command);}finally{sendingOwnCommand=previous;}
  });
  if(run!=task.runId())return;
  if(task.attempts()>0&&!reportedStart){reportedStart=true;feedback.accept(new Notice("已發送 /pickupstash，開始領取 Stash。",false));}
  reportTerminal();
 }
 /** The host's outbound-command event gives manual/legacy commands priority. */
 public void onOutgoingCommand(){if(!sendingOwnCommand)cancel("手動或原有指令優先");}
 /** Jerry safety stop applies even when no Stash signal/task is active. */
 public void suspend(String reason){sessionSuspended=true;cancel(reason);}
 public void cancel(String reason){if(!active())return;task.cancel();reported=task.state();feedback.accept(new Notice("Stash 已停止（"+reason+"）。",false));}
 private void reportTerminal(){
  var state=task.state();if(state==reported||state==StashTask.State.WAITING||state==StashTask.State.IDLE)return;reported=state;
  String text=switch(state){
   case COMPLETE -> "Stash 已無待領物品。";
   case BLOCKED -> "Stash 無法繼續領取：背包已滿或伺服器拒絕，已停止。";
   case TIMED_OUT -> "Stash 已逾時，已停止，等待操作。";
   case EXHAUSTED -> "Stash 已達嘗試上限，已停止，等待操作。";
   case FAILED -> "無法發送 /pickupstash，已停止，請檢查連線。";
   case CANCELLED -> "Stash 已停止：執行環境或任務權限已改變。";
   default -> throw new IllegalStateException("Not a terminal state");
  };
  feedback.accept(new Notice(text,state!=StashTask.State.COMPLETE&&state!=StashTask.State.CANCELLED));
 }
 public String status(){
  if(!settings.writable())return settings.issue();
  return switch(task.state()){
   case IDLE -> "閒置";case WAITING -> "等待／領取中（"+task.attempts()+"/"+settings.stashPolicy().maxAttempts()+"）";
   case COMPLETE -> "已無待領物品";case BLOCKED -> "背包已滿或伺服器拒絕";case CANCELLED -> "已停止";case TIMED_OUT -> "已逾時";case EXHAUSTED -> "已達嘗試上限";case FAILED -> "指令發送失敗";
  };
 }
}
