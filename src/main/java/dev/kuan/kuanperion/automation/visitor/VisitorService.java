// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.function.*;
/** Explicit inventory-only automatic dispatch; failures require user re-enabling, never re-arm Farming. */
public final class VisitorService {
 private static final long SECOND=1_000_000_000L;
 private final AutomationSession session;private final VisitorTask.Port port;private final ManualTaskRequest manual=new ManualTaskRequest(true);
 private final AutomationSettingsStore settings;private final VisitorTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private boolean suspended,attempted,polled;private long lastAttempt,lastPoll;private VisitorTask.State reported=VisitorTask.State.IDLE;
 public VisitorService(AutomationSettingsStore settings,AutomationSession session,PlayerPositionReceipts positions,InventoryReceipts receipts,CursorReceipts cursors,VisitorTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.settings=settings;this.session=session;this.port=port;this.automaticAllowed=automaticAllowed;this.feedback=feedback;task=new VisitorTask(session,positions,receipts,cursors,port);}
 public VisitorPolicy policy(){return settings.visitorPolicy();}public boolean autoEnabled(){return !suspended&&settings.enabled(AutomationFeature.VISITOR)&&settings.visitorPolicyValid()&&policy().configured();}public boolean active(){return task.active();}public boolean pending(){return manual.pending();}public boolean ownsMenu(){return task.ownsMenu();}
 public void setEnabled(boolean value)throws IOException{manual.clear();if(value&&(!settings.visitorPolicyValid()||!policy().configured()))throw new IllegalArgumentException("請先設定 Visitor、材料 ID 與整單估價上限");if(!value)cancel(System.nanoTime());settings.setEnabled(AutomationFeature.VISITOR,value);suspended=!value;}
 public void setPolicy(VisitorPolicy value)throws IOException{cancel(System.nanoTime());try{settings.setVisitorPolicy(value);}catch(IOException failure){suspended=true;throw failure;}}
 public boolean manualRequest(long now){
  if(active()||pending()||!settings.writable()||!settings.visitorPolicyValid()||!policy().configured()||attempted&&(now-lastAttempt<0||now-lastAttempt<20*SECOND))return false;
  if(automaticAllowed.getAsBoolean())return request(true,now);
  return manual.offer(pendingContext(),session.active(),now);
 }
 private AutomationContext pendingContext(){try{return port.context();}catch(RuntimeException unavailable){return null;}}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now)){report();return false;}attempted=true;lastAttempt=now;reported=VisitorTask.State.DATA;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(pending()){
   if(manual.take(pendingContext(),session.active(),automaticAllowed.getAsBoolean(),now)){
    var previous=reported;boolean accepted=request(true,now);if(accepted)task.tick(now);report();
    if(!accepted&&reported==previous)feedback.accept(new StashService.Notice("Visitor 單次請求未開始：目前設定、目標、材料、Garden 或控制權條件不符；未繼續執行。",false,"Visitor"));
   }return;
  }
  if(!autoEnabled()||!automaticAllowed.getAsBoolean()||attempted&&(now-lastAttempt<0||now-lastAttempt<policy().cooldownSeconds()*SECOND)||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(request(false,now)){task.tick(now);report();}
 }
 public void onSystemMessage(long world,String message){task.onSystemMessage(world,message);}
 public VisitorTask.CloseTicket beforeServerClose(int id,long now){return task.beforeServerClose(id,now);}public boolean afterServerClose(VisitorTask.CloseTicket ticket,long now){return task.afterServerClose(ticket,now);}
 public void suspend(long now){suspended=true;cancel(now);}public void cancel(long now){if(manual.clear())suspended=true;if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){var state=task.state();if(state==reported||task.active()||state==VisitorTask.State.IDLE)return;reported=state;boolean success=state==VisitorTask.State.COMPLETE||state==VisitorTask.State.PARTIAL;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.VISITOR,false);}catch(IOException ignored){}}
  String text=switch(state){case COMPLETE->"Visitor 已確認完成 "+task.confirmed()+" 筆，已返回原位。";case PARTIAL->"Visitor 已確認完成 "+task.confirmed()+" 筆、略過 "+task.skipped()+" 筆，已返回原位；未完成所有訪客。";case CANCELLED->"Visitor 已停止，等待操作。";case TIMED_OUT->"Visitor 逾時，已停止並停用自動接單，等待操作。";default->"Visitor 未完成：目標、估價、材料或伺服器回條驗證失敗，已停止並停用自動接單。";};feedback.accept(new StashService.Notice(text,!success&&state!=VisitorTask.State.CANCELLED,"Visitor"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.visitorPolicyValid()?"Visitor 設定無效，未執行或覆寫":!policy().configured()?"請先設定 Visitor、材料 ID 與整單估價上限":pending()?"已排入單次執行，關閉選單後開始（30 秒內有效）":task.state().name()+" · 已確認 "+task.confirmed()+" 筆 · 略過 "+task.skipped()+" 筆";}
}
