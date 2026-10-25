// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.io.IOException;
import java.util.function.*;
/** Independent off-by-default Pest task, on-demand observations and no automatic retry after failure. */
public final class PestService {
 private static final long SECOND=1_000_000_000L;
 private final AutomationSettingsStore settings;private final PestTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private long lastAttempt,lastPoll;private boolean attempted,polled,suspended;private PestTask.State reported=PestTask.State.IDLE;
 public PestService(AutomationSettingsStore settings,AutomationSession session,PestTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this(settings,session,port,new PlayerPositionReceipts(),automaticAllowed,feedback);}
 public PestService(AutomationSettingsStore settings,AutomationSession session,PestTask.Port port,PlayerPositionReceipts receipts,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.settings=settings;task=new PestTask(session,port,receipts);this.automaticAllowed=automaticAllowed;this.feedback=feedback;}
 public PestPolicy policy(){return settings.pestPolicy();}public boolean autoEnabled(){return settings.enabled(AutomationFeature.PEST)&&settings.pestPolicyValid()&&!suspended;}public boolean active(){return task.active();}
 public void setEnabled(boolean value)throws IOException{if(!value){suspended=true;cancel(System.nanoTime());}settings.setEnabled(AutomationFeature.PEST,value);if(value)suspended=false;}
 public void setPolicy(PestPolicy policy)throws IOException{if(active())cancel(System.nanoTime());try{settings.setPestPolicy(policy);}catch(IOException failure){suspended=true;throw failure;}}
 public boolean manualRequest(long now){return settings.writable()&&settings.pestPolicyValid()&&(!attempted||now-lastAttempt>=SECOND)&&request(true,now);}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;attempted=true;lastAttempt=now;reported=PestTask.State.PREPARING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(!autoEnabled()||!automaticAllowed.getAsBoolean()||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(attempted&&now-lastAttempt>=0&&now-lastAttempt<policy().cooldownSeconds()*SECOND)return;
  if(request(false,now)){task.tick(now);report();}
 }
 public void cancel(long now){if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){
  var state=task.state();if(state==reported||task.active()||state==PestTask.State.IDLE)return;reported=state;
  boolean success=state==PestTask.State.COMPLETE;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.PEST,false);}catch(IOException ignored){}}
  String text=success?"已確認 Pest 數量減少 "+task.confirmedDecreases()+" 次，已返回原位。":switch(state){
   case INCOMPLETE->"已返回原位，但範圍內未找到剩餘 Pest，已停用自動除蟲，等待操作。";
   case CANCELLED->"Pest 已停止，等待操作。";
   case TIMED_OUT->"Pest 逾時，已停止並停用自動除蟲，等待操作。";
   default->"Pest 未完成：目標、路徑、飛行權限或返回驗證失敗，已停止並停用自動除蟲。";
  };feedback.accept(new StashService.Notice(text,!success&&state!=PestTask.State.CANCELLED,"Pest"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.pestPolicyValid()?"Pest 設定無效，未執行或覆寫":task.state().name()+" · 已確認數量減少 "+task.confirmedDecreases()+" 次";}
}
