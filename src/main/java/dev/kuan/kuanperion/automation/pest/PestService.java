// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.io.IOException;
import java.util.function.*;
/** Independent off-by-default Pest task, on-demand observations and no automatic retry after failure. */
public final class PestService {
 private static final long SECOND=1_000_000_000L;
 private final AutomationSettingsStore settings;private final PestTask task;private final AutomationSession session;private final PestTask.Port port;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private boolean pending,pendingFarming;private long pendingAt,pendingWorld;
 private long lastAttempt,lastPoll;private boolean attempted,polled,suspended;private PestTask.State reported=PestTask.State.IDLE;
 public PestService(AutomationSettingsStore settings,AutomationSession session,PestTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this(settings,session,port,new PlayerPositionReceipts(),automaticAllowed,feedback);}
 public PestService(AutomationSettingsStore settings,AutomationSession session,PestTask.Port port,PlayerPositionReceipts receipts,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.settings=settings;this.session=session;this.port=port;task=new PestTask(session,port,receipts);this.automaticAllowed=automaticAllowed;this.feedback=feedback;}
 public PestTrapRelease.CloseTicket beforeServerClose(int id,long now){return task.beforeServerClose(id,now);}
 public boolean afterServerClose(PestTrapRelease.CloseTicket ticket,long now){return task.afterServerClose(ticket,now);}
 public boolean ownsMenu(){return task.ownsMenu();}
 public PestPolicy policy(){return settings.pestPolicy();}public boolean autoEnabled(){return settings.enabled(AutomationFeature.PEST)&&settings.pestPolicyValid()&&!suspended;}public boolean active(){return task.active();}public boolean pending(){return pending;}
 public void setEnabled(boolean value)throws IOException{pending=false;if(!value){suspended=true;cancel(System.nanoTime());}settings.setEnabled(AutomationFeature.PEST,value);if(value)suspended=false;}
 public void setPolicy(PestPolicy policy)throws IOException{cancel(System.nanoTime());try{settings.setPestPolicy(policy);}catch(IOException failure){suspended=true;throw failure;}}
 /** A settings click is intent only; do not acquire a flight/Farming root until the user's screen closes. */
 public boolean manualRequest(long now){
  if(active()||pending||!settings.writable()||!settings.pestPolicyValid()||attempted&&(now-lastAttempt<0||now-lastAttempt<SECOND))return false;
  if(automaticAllowed.getAsBoolean())return request(true,now);
  var context=pendingContext();if(session.active()||context==null||!context.canIssueCommand()||!context.garden())return false;
  pending=true;pendingAt=now;pendingWorld=context.worldId();pendingFarming=context.farmingActive();return true;
 }
 private AutomationContext pendingContext(){try{return port.context();}catch(RuntimeException unavailable){return null;}}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;attempted=true;lastAttempt=now;reported=PestTask.State.PREPARING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(pending){
   var context=pendingContext();
   if(now-pendingAt<0||now-pendingAt>=30*SECOND||context==null||!context.canIssueCommand()||!context.garden()||context.worldId()!=pendingWorld||context.farmingActive()!=pendingFarming||session.active()){pending=false;return;}
   if(!automaticAllowed.getAsBoolean())return;pending=false;
   if(request(true,now)){task.tick(now);report();}else feedback.accept(new StashService.Notice("Pest 單次請求未開始：目前目標、工具、Garden 或控制權條件不符；未繼續執行。",false,"Pest"));return;
  }
  if(!autoEnabled()||!automaticAllowed.getAsBoolean()||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(attempted&&now-lastAttempt>=0&&now-lastAttempt<policy().cooldownSeconds()*SECOND)return;
  if(request(false,now)){task.tick(now);report();}
 }
 /** Explicit safety stop also suppresses idle automatic dispatch until the user re-enables it. */
 public void suspend(long now){suspended=true;cancel(now);}
 public void cancel(long now){if(pending)suspended=true;pending=false;if(!active())return;suspended=true;task.cancel(now);report();}
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
 public String status(){return !settings.writable()?settings.issue():!settings.pestPolicyValid()?"Pest 設定無效，未執行或覆寫":pending?"已排入單次執行，關閉選單後開始（30 秒內有效）":task.state().name()+" · 已確認數量減少 "+task.confirmedDecreases()+" 次";}
}
