// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.function.*;
/** Off-by-default interval maintenance. Failures require explicit re-enabling, never re-arm Farming. */
public final class GreenhouseService {
 private static final long SECOND=1_000_000_000L;
 private final AutomationSettingsStore settings;private final GreenhouseTask task;private final BooleanSupplier automaticAllowed;private final Consumer<StashService.Notice> feedback;
 private boolean suspended,scheduled,attempted,polled;private long lastScheduled,lastAttempt,lastPoll;private GreenhouseTask.State reported=GreenhouseTask.State.IDLE;
 public GreenhouseService(AutomationSettingsStore settings,AutomationSession session,PlayerPositionReceipts positions,HarvestReceipts harvests,GreenhouseTask.Port port,BooleanSupplier automaticAllowed,Consumer<StashService.Notice> feedback){this.settings=settings;this.automaticAllowed=automaticAllowed;this.feedback=feedback;task=new GreenhouseTask(session,positions,harvests,port);}
 public GreenhousePolicy policy(){return settings.greenhousePolicy();}
 public boolean autoEnabled(){return !suspended&&settings.enabled(AutomationFeature.GREENHOUSE)&&settings.greenhousePolicyValid()&&policy().configured();}
 public boolean active(){return task.active();}
 public void setEnabled(boolean value)throws IOException{if(value&&(!settings.greenhousePolicyValid()||!policy().configured()))throw new IllegalArgumentException("請先設定地塊、作物與工具");if(!value)cancel(System.nanoTime());settings.setEnabled(AutomationFeature.GREENHOUSE,value);suspended=!value;scheduled=false;}
 public void setPolicy(GreenhousePolicy value)throws IOException{if(active())cancel(System.nanoTime());try{settings.setGreenhousePolicy(value);scheduled=false;}catch(IOException failure){suspended=true;throw failure;}}
 public boolean manualRequest(long now){return settings.writable()&&settings.greenhousePolicyValid()&&(!attempted||now-lastAttempt>=SECOND)&&request(true,now);}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;attempted=true;lastAttempt=lastScheduled=now;scheduled=true;reported=GreenhouseTask.State.PREPARING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(!autoEnabled())return;
  if(!scheduled){scheduled=true;lastScheduled=now;return;}
  if(now-lastScheduled<0){lastScheduled=now;return;}
  if(now-lastScheduled<policy().intervalSeconds()*SECOND||!automaticAllowed.getAsBoolean()||polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  polled=true;lastPoll=now;if(request(false,now)){task.tick(now);report();}
 }
 /** Explicit safety stop also suppresses idle automatic dispatch until the user re-enables it. */
 public void suspend(long now){suspended=true;cancel(now);}
 public void cancel(long now){if(!active())return;suspended=true;task.cancel(now);report();}
 private void report(){
  var state=task.state();if(state==reported||task.active()||state==GreenhouseTask.State.IDLE)return;reported=state;
  boolean success=state==GreenhouseTask.State.COMPLETE||state==GreenhouseTask.State.PARTIAL;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.GREENHOUSE,false);}catch(IOException ignored){}}
  String text=switch(state){
   case COMPLETE->"Greenhouse 已確認收割 "+task.confirmed()+" 個目標，已返回原位。";
   case PARTIAL->"Greenhouse 已確認收割 "+task.confirmed()+" 個目標，達本次上限已返回原位；尚未完成全部巡查。";
   case CANCELLED->"Greenhouse 已停止，等待操作。";
   case TIMED_OUT->"Greenhouse 逾時，已停止並停用自動收割，等待操作。";
   default->"Greenhouse 未完成：目標、工具、路徑或伺服器回條驗證失敗，已停止並停用自動收割。";
  };feedback.accept(new StashService.Notice(text,!success&&state!=GreenhouseTask.State.CANCELLED,"Greenhouse"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.greenhousePolicyValid()?"Greenhouse 設定無效，未執行或覆寫":!policy().configured()?"請先設定地塊、作物與工具":task.state().name()+" · 已確認收割 "+task.confirmed()+" 個目標";}
}
