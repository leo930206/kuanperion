// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.function.*;import java.util.Optional;
/** Off-by-default interval maintenance. Failures require explicit re-enabling, never re-arm Farming. */
public final class ComposterService {
 private static final long SECOND=1_000_000_000L;
 private final Supplier<Optional<ComposterPolicy.Anchor>> capture;private final AutomationSettingsStore settings;private final ComposterTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private boolean suspended,scheduled,attempted,polled;private long lastScheduled,lastAttempt,lastPoll;private ComposterTask.State reported=ComposterTask.State.IDLE;
 public ComposterService(AutomationSettingsStore settings,AutomationSession session,PlayerPositionReceipts positions,InventoryReceipts inventory,CursorReceipts cursor,ComposterTask.Port port,BooleanSupplier automaticAllowed,Supplier<Optional<ComposterPolicy.Anchor>> capture,Consumer<StashService.Notice> feedback){this.capture=capture;this.settings=settings;this.automaticAllowed=automaticAllowed;this.feedback=feedback;task=new ComposterTask(session,positions,inventory,cursor,port);}
 public boolean ownsMenu(){return task.ownsMenu();}
 public boolean captureAnchor()throws IOException{var anchor=capture.get();if(anchor.isEmpty())return false;setPolicy(policy().withAnchor(anchor.get()));return true;}
 public ComposterPolicy policy(){return settings.composterPolicy();}
 public boolean autoEnabled(){return !suspended&&settings.enabled(AutomationFeature.COMPOSTER)&&settings.composterPolicyValid()&&policy().configured();}
 public boolean active(){return task.active();}
 public void setEnabled(boolean value)throws IOException{if(value&&(!settings.composterPolicyValid()||!policy().configured()))throw new IllegalArgumentException("請先設定機器位置與材料");if(!value)cancel(System.nanoTime());settings.setEnabled(AutomationFeature.COMPOSTER,value);suspended=!value;scheduled=false;}
 public void setPolicy(ComposterPolicy value)throws IOException{if(active())cancel(System.nanoTime());try{settings.setComposterPolicy(value);scheduled=false;}catch(IOException failure){suspended=true;throw failure;}}
 public boolean manualRequest(long now){return settings.writable()&&settings.composterPolicyValid()&&(!attempted||now-lastAttempt>=SECOND)&&request(true,now);}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;attempted=true;lastAttempt=lastScheduled=now;scheduled=true;reported=ComposterTask.State.PREPARING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(!autoEnabled())return;
  if(!scheduled){scheduled=true;lastScheduled=now;return;}
  if(now-lastScheduled<0){lastScheduled=now;return;}
  if(now-lastScheduled<policy().intervalSeconds()*SECOND||!automaticAllowed.getAsBoolean()||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(request(false,now)){task.tick(now);report();}
 }
 public void cancel(long now){if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){
  var state=task.state();if(state==reported||task.active()||state==ComposterTask.State.IDLE)return;reported=state;
  boolean success=state==ComposterTask.State.COMPLETE||state==ComposterTask.State.PARTIAL;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.COMPOSTER,false);}catch(IOException ignored){}}
  String text=switch(state){
   case COMPLETE->"Composter 已確認補充 "+task.inserted()+" 個材料，已返回原位。";
   case PARTIAL->"Composter 已確認補充 "+task.inserted()+" 個材料，已返回原位；庫存不足或達本次上限，尚未補充到目標容量。";
   case CANCELLED->"Composter 已停止，等待操作。";
   case TIMED_OUT->"Composter 逾時，已停止並停用自動補充，等待操作。";
   default->"Composter 未完成：材料、選單、路徑或伺服器回條驗證失敗，已停止並停用自動補充。";
  };feedback.accept(new StashService.Notice(text,!success&&state!=ComposterTask.State.CANCELLED,"Composter"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.composterPolicyValid()?"Composter 設定無效，未執行或覆寫":!policy().configured()?"請先設定機器位置與材料":task.state().name()+" · 已確認補充 "+task.inserted()+" 個材料";}
}
