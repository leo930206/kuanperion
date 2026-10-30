// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.function.*;
/** Independent source-cadence scheduler. Never arms Farming or replays a failed/expired manual intent. */
public final class PestSprayService {
 private static final long SECOND=1_000_000_000L,POLL=750_000_000L,COOLDOWN=10*SECOND;
 private final AutomationSettingsStore settings;private final AutomationSession session;private final PestSprayTask.Port port;private final PestSprayTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private final ManualTaskRequest manual=new ManualTaskRequest(true);private boolean suspended,attempted,polled;private long lastAttempt,lastPoll;private PestSprayTask.State reported=PestSprayTask.State.IDLE;
 public PestSprayService(AutomationSettingsStore settings,AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts items,CursorReceipts cursors,PestSprayTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.settings=settings;this.session=session;this.port=port;this.automaticAllowed=automaticAllowed;this.feedback=feedback;task=new PestSprayTask(session,coordinator,items,cursors,port);}
 public PestSprayPolicy policy(){return settings.pestSprayPolicy();}public PestSprayBuying buying(){return settings.pestSprayBuying();}
 public boolean autoEnabled(){return !suspended&&settings.enabled(AutomationFeature.PEST_SPRAY)&&settings.pestSprayPolicyValid()&&policy().configured();}
 public boolean active(){return task.active();}public boolean pending(){return manual.pending();}public boolean ownsMenu(){return task.ownsMenu();}
 public void onSystemMessage(long world,String text,long now){task.onSystemMessage(world,text,now);}
 public void setEnabled(boolean value)throws IOException{manual.clear();if(value&&(!settings.pestSprayPolicyValid()||!policy().configured()))throw new IllegalArgumentException("請先設定精確噴料 ID／原生 ID／名稱、消耗上限並確認 Sacks 風險");if(!value)cancel(System.nanoTime());try{settings.setEnabled(AutomationFeature.PEST_SPRAY,value);}catch(IOException failure){suspended=true;throw failure;}suspended=!value;}
 public void setPolicy(PestSprayPolicy p,PestSprayBuying b)throws IOException{cancel(System.nanoTime());try{settings.setPestSprayPolicy(p,b);}catch(IOException failure){suspended=true;throw failure;}}
 private AutomationContext context(){try{return port.context();}catch(RuntimeException unavailable){return null;}}
 public boolean manualRequest(long now){
  if(active()||pending()||session.active()||!settings.writable()||!settings.pestSprayPolicyValid()||!policy().configured()||attempted&&(now-lastAttempt<0||now-lastAttempt<SECOND))return false;
  var c=context();if(c==null||!c.farmingActive()||!c.garden()||!c.canIssueCommand())return false;
  if(automaticAllowed.getAsBoolean())return request(now);
  return manual.offer(c,session.active(),now);
 }
 private boolean request(long now){if(!task.request(policy(),buying(),now)){report();return false;}attempted=true;lastAttempt=now;reported=task.state();return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(pending()){
   if(manual.take(context(),session.active(),automaticAllowed.getAsBoolean(),now)){
    if(request(now)){task.tick(now);report();}else feedback.accept(new StashService.Notice("噴灑未開始：需手動啟動的 Farming、Garden、快捷列工具、授權材料及新鮮地塊回條；不重試。",false,"Pest Spray"));
   }return;
  }
  // Do not inspect an incumbent's temporary hotbar/pose, consume its due poll, or construct public HTTP.
  if(session.active()){polled=false;return;}
  if(!autoEnabled()||!automaticAllowed.getAsBoolean())return;
  if(attempted&&(now-lastAttempt<0||now-lastAttempt<COOLDOWN))return;
  if(polled&&now-lastPoll>=0&&now-lastPoll<POLL)return;
  polled=true;lastPoll=now;if(request(now)){task.tick(now);report();}
 }
 public void suspend(long now){suspended=true;cancel(now);}
 public void cancel(long now){if(manual.clear())suspended=true;if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){
  var state=task.state();if(state==reported||task.active()||state==PestSprayTask.State.IDLE)return;reported=state;boolean success=state==PestSprayTask.State.COMPLETE;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.PEST_SPRAY,false);}catch(IOException ignored){}}
  String text=success?"已確認本地塊噴灑、材料變動與工具還原；僅釋放原有 Farming 暫停。":state==PestSprayTask.State.CANCELLED?"噴灑已停止，等待操作。":"噴灑未完成：材料、地塊、選單或伺服器回條不符，已停用自動噴灑；不重試。";
  feedback.accept(new StashService.Notice(text,!success&&state!=PestSprayTask.State.CANCELLED,"Pest Spray"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.pestSprayPolicyValid()?"噴灑設定無效，未覆寫原檔":!policy().configured()?"請先設定精確噴料身分、上限及 Sacks 授權":pending()?"已排入單次噴灑，關閉選單後開始（30 秒內有效）":task.state().name();}
}
