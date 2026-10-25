// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.io.IOException;
import java.util.function.*;
/** NPC disposal orchestration; Bazaar is a separate forthcoming driver, not a fake option. */
public final class AutoSellService {
 private static final long SECOND=1000000000L;
 private final AutomationSettingsStore settings;private final AutoSellTask task;private final AutoSellTask.Port port;
 private final BooleanSupplier allowAutomatic;private final Consumer<StashService.Notice> feedback;
 private boolean sessionSuspended,hasAttempt,hasPolled,reportedStart;private long lastAttempt,lastPoll;
 private AutoSellTask.State reported=AutoSellTask.State.IDLE;
 public AutoSellService(AutomationSettingsStore settings,AutomationSession session,InventoryReceipts receipts,AutoSellTask.Port port,BooleanSupplier allowAutomatic,Consumer<StashService.Notice> feedback){this.settings=settings;this.port=port;this.allowAutomatic=allowAutomatic;this.feedback=feedback;task=new AutoSellTask(session,receipts,port);}
 public boolean autoEnabled(){return settings.enabled(AutomationFeature.AUTO_SELL)&&!sessionSuspended;}
 public boolean active(){return task.active();}public AutoSellPolicy policy(){return settings.autoSellPolicy();}
 public void setEnabled(boolean enabled)throws IOException{if(!enabled){sessionSuspended=true;cancel(System.nanoTime());}settings.setEnabled(AutomationFeature.AUTO_SELL,enabled);if(enabled)sessionSuspended=false;}
 public void setPolicy(AutoSellPolicy policy)throws IOException{cancel(System.nanoTime());try{settings.setAutoSellPolicy(policy);}catch(IOException failure){sessionSuspended=true;throw failure;}}
 public boolean manualRequest(long now){return settings.writable()&&(!hasAttempt||now-lastAttempt>=SECOND)&&request(true,now);}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;lastAttempt=now;hasAttempt=true;reportedStart=false;reported=AutoSellTask.State.OPENING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(!autoEnabled()||!allowAutomatic.getAsBoolean()||hasPolled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;
  lastPoll=now;hasPolled=true;var p=policy();
  if(p.allowedIds().isEmpty()||hasAttempt&&now-lastAttempt>=0&&now-lastAttempt<p.cooldownSeconds()*SECOND)return;
  var inventory=port.inventory();if(inventory.isEmpty())return;long occupied=inventory.stream().filter(i->i.count()>0).count();
  if(occupied*100<inventory.size()*p.thresholdPercent())return;
  if(request(false,now)){task.tick(now);report();}
 }
 /** Explicit safety stop also suppresses idle automatic dispatch until the user re-enables it. */
 public void suspend(long now){sessionSuspended=true;cancel(now);}
 public void cancel(long now){task.cancel(now);report();}
 public void onSystemMessage(String message,long now){
  if(!active()||!message.replaceAll("(?i)§[0-9a-fk-orx]","").strip().equals("You've reached the daily limit of coins you may earn from NPC shops."))return;
  sessionSuspended=true;cancel(now);try{settings.setEnabled(AutomationFeature.AUTO_SELL,false);}catch(IOException ignored){}
  feedback.accept(new StashService.Notice("NPC 今日出售金額已達上限，已停止並停用自動出售。",true));
 }
 private void report(){
  var state=task.state();
  if(state==AutoSellTask.State.WAITING_MENU&&!reportedStart){reportedStart=true;feedback.accept(new StashService.Notice("已發送 /boostercookie，準備 NPC 出售。",false));}
  if(state==reported||task.active()||state==AutoSellTask.State.IDLE)return;reported=state;
  String message=switch(state){
   case COMPLETE->"NPC 出售流程完成，已收到 "+task.soldItems()+" 個物品的伺服器移出確認。";
   case CANCELLED->"NPC 出售已停止，未繼續操作選單。";
   case TIMED_OUT->"NPC 出售逾時，已停止，等待操作。";
   case FAILED->"NPC 出售未完成：選單、指令或物品驗證失敗，已停止。";
   default->throw new IllegalStateException("Not terminal");
  };feedback.accept(new StashService.Notice(message,state!=AutoSellTask.State.COMPLETE&&state!=AutoSellTask.State.CANCELLED));
 }
 public String status(){return settings.writable()?task.state().name()+" · 已確認 "+task.soldItems()+" 個物品":settings.issue();}
}
