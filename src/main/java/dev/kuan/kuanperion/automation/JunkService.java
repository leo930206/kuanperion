// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.io.IOException;
import java.util.function.*;
/** Garden-only exact-ID disposal. Empty policy/location is inert even if enabled. */
public final class JunkService {
 private final AutomationSettingsStore settings;private final JunkTask task;private final JunkTask.Port port;private final BooleanSupplier allowAutomatic;private final Consumer<StashService.Notice> feedback;
 private boolean suspended,hasAttempt,hasPoll;private long lastAttempt,lastPoll,authorizedWorld;private JunkTask.State reported=JunkTask.State.IDLE;
 public JunkService(AutomationSettingsStore settings,AutomationSession session,InventoryReceipts receipts,JunkTask.Port port,BooleanSupplier allowAutomatic,Consumer<StashService.Notice> feedback){this.settings=settings;this.port=port;this.allowAutomatic=allowAutomatic;this.feedback=feedback;task=new JunkTask(session,receipts,port);}
 public boolean autoEnabled(){return settings.enabled(AutomationFeature.JUNK)&&!suspended;}public boolean active(){return task.active();}public JunkPolicy policy(){return settings.junkPolicy();}
 public void setEnabled(boolean enabled)throws IOException{if(!enabled){suspended=true;cancel(System.nanoTime());}settings.setEnabled(AutomationFeature.JUNK,enabled);if(enabled){suspended=false;var context=port.context();authorizedWorld=context.garden()&&policy().atAnchor(port.location())&&AutomationSession.forFeature(context,AutomationFeature.JUNK).canIssueCommand()?context.worldId():0;}}
 public void setPolicy(JunkPolicy policy)throws IOException{cancel(System.nanoTime());try{if(!java.util.Objects.equals(policy.anchor(),policy().anchor()))authorizedWorld=0;settings.setJunkPolicy(policy);}catch(IOException failure){suspended=true;throw failure;}}
 public boolean recordAnchor()throws IOException{var context=port.context();var here=port.location();if(!context.garden()||!AutomationSession.forFeature(context,AutomationFeature.JUNK).canIssueCommand()||here==null)return false;var p=policy();setPolicy(new JunkPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming(),here,p.radius()));authorizedWorld=context.worldId();return true;}
 public boolean manualRequest(long now){return settings.writable()&&(!hasAttempt||now-lastAttempt>=1_000_000_000L)&&request(true,now);}
 private boolean request(boolean manual,long now){if(!task.request(policy(),manual,now))return false;if(manual)authorizedWorld=port.context().worldId();lastAttempt=now;hasAttempt=true;reported=JunkTask.State.OPENING;return true;}
 public void tick(long now){
  if(active()){task.tick(now);report();return;}
  if(!autoEnabled()||port.context().worldId()!=authorizedWorld||!allowAutomatic.getAsBoolean()||hasPoll&&now-lastPoll>=0&&now-lastPoll<1_000_000_000L)return;
  lastPoll=now;hasPoll=true;var p=policy();if(p.allowedIds().isEmpty()||p.anchor()==null||hasAttempt&&(now-lastAttempt<0||now-lastAttempt<p.cooldownSeconds()*1_000_000_000L))return;
  var inventory=port.inventory();if(inventory.isEmpty()||inventory.stream().filter(i->i.count()>0).count()*100<inventory.size()*p.thresholdPercent())return;
  if(request(false,now)){task.tick(now);report();}
 }
 /** Explicit safety stop also suppresses idle automatic dispatch until the user re-enables it. */
 public void suspend(long now){suspended=true;cancel(now);}
 public void cancel(long now){task.cancel(now);report();}
 private void report(){var state=task.state();if(state==reported||task.active()||state==JunkTask.State.IDLE)return;reported=state;
  if(state==JunkTask.State.FAILED||state==JunkTask.State.TIMED_OUT){suspended=true;try{settings.setEnabled(AutomationFeature.JUNK,false);}catch(IOException ignored){}}
  String text=switch(state){case COMPLETE->"Junk 完成，已收到 "+task.removedItems()+" 個物品的伺服器移出確認。";case CANCELLED->"Junk 已停止，未繼續丟棄物品。";case TIMED_OUT->"Junk 逾時，已停用自動丟棄，等待操作。";case FAILED->"Junk 未完成：背包或丟棄驗證失敗，已停用自動丟棄。";default->throw new IllegalStateException("Not terminal");};feedback.accept(new StashService.Notice(text,state!=JunkTask.State.COMPLETE&&state!=JunkTask.State.CANCELLED));
 }
 public String status(){var p=policy();return !settings.writable()?settings.issue():task.state().name()+" · 已確認 "+task.removedItems()+" 個物品 · "+(p.anchor()==null?"尚未設定丟棄位置":String.format(java.util.Locale.ROOT,"位置 %.1f, %.1f, %.1f",p.anchor().x(),p.anchor().y(),p.anchor().z()))+(authorizedWorld==port.context().worldId()?" · 本次世界已授權":" · 請在本次世界重新記錄位置或手動確認");}
}
