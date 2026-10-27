// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.function.*;
/** Dispatch only an explicit exact batch. UI requests expire and wait for the user's screen to close. */
public final class SupercraftService {
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final SupercraftTask.Port port;private final ManualTaskRequest manual=new ManualTaskRequest(false);private final AutomationSettingsStore settings;private final SupercraftTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private boolean suspended,attempted,polled;private long lastAttempt,lastPoll;private SupercraftTask.State reported=SupercraftTask.State.IDLE;
 public SupercraftService(AutomationSettingsStore settings,AutomationSession session,InventoryReceipts receipts,CursorReceipts cursors,SupercraftTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.session=session;this.port=port;this.settings=settings;this.automaticAllowed=automaticAllowed;this.feedback=feedback;task=new SupercraftTask(session,receipts,cursors,port);}
 public SupercraftPolicy policy(){return settings.supercraftPolicy();}public boolean autoEnabled(){return !suspended&&settings.enabled(AutomationFeature.SUPERCRAFT)&&settings.supercraftPolicyValid()&&policy().configured();}public boolean active(){return task.active();}public boolean pending(){return manual.pending();}public boolean ownsMenu(){return task.ownsMenu();}
 public void setEnabled(boolean value)throws IOException{if(value&&(!settings.supercraftPolicyValid()||!policy().configured()))throw new IllegalArgumentException("請先設定 Supercraft 配方、材料及數量上限");if(!value){suspended=true;cancel(System.nanoTime());}settings.setEnabled(AutomationFeature.SUPERCRAFT,value);suspended=!value;}
 public void setPolicy(SupercraftPolicy value)throws IOException{cancel(System.nanoTime());try{settings.setSupercraftPolicy(value);}catch(IOException failure){suspended=true;throw failure;}}
 public boolean manualRequest(long now){if(active()||pending()||!settings.writable()||!settings.supercraftPolicyValid()||!policy().configured()||attempted&&(now-lastAttempt<0||now-lastAttempt<20*SECOND))return false;return manual.offer(pendingContext(),session.active(),now);}
 private AutomationContext pendingContext(){try{return port.context();}catch(RuntimeException unavailable){return null;}}
 private boolean request(boolean manual,long now){if(!task.request(policy().task(),manual,now)){report();return false;}attempted=true;lastAttempt=now;reported=SupercraftTask.State.DATA;return true;}
 public void tick(long now){
  if(active()){if(!automaticAllowed.getAsBoolean()&&!task.ownsMenu())cancel(now);else{task.tick(now);report();}return;}
  if(pending()){
   if(manual.take(pendingContext(),session.active(),automaticAllowed.getAsBoolean(),now)){
    var previous=reported;boolean accepted=request(true,now);
    if(!accepted&&reported==previous)feedback.accept(new StashService.Notice("Supercraft 單次請求未開始：目前設定、材料或控制權條件不符；未繼續執行。",false,"Supercraft"));
   }return;
  }
  if(session.active()){polled=false;return;}
  if(!autoEnabled()||!automaticAllowed.getAsBoolean()||attempted&&(now-lastAttempt<0||now-lastAttempt<policy().intervalSeconds()*SECOND)||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(request(false,now)){task.tick(now);report();}
 }
 public void suspend(long now){suspended=true;cancel(now);}public void cancel(long now){if(manual.clear())suspended=true;if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){var state=task.state();if(state==reported||task.active()||state==SupercraftTask.State.IDLE)return;reported=state;boolean success=state==SupercraftTask.State.COMPLETE;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.SUPERCRAFT,false);}catch(IOException ignored){}}
  String text=switch(state){case COMPLETE->"Supercraft 已確認合成 "+task.confirmedOutput()+" 個，操作已完成。";case CANCELLED->"Supercraft 已停止，等待操作。";case TIMED_OUT->"Supercraft 逾時，已停止並停用自動合成；請確認背包與選單。";default->"Supercraft 未完成：配方、來源、數量、費用或伺服器回條驗證失敗，已停止並停用自動合成。";};feedback.accept(new StashService.Notice(text,!success&&state!=SupercraftTask.State.CANCELLED,"Supercraft"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.supercraftPolicyValid()?"Supercraft 設定無效，未執行或覆寫":!policy().configured()?"請先設定完整配方、材料及數量上限":pending()?"已排入單次執行，關閉選單後開始（30 秒內有效）":task.state().name()+" · 已確認 "+task.confirmedOutput()+" 個";}
}
