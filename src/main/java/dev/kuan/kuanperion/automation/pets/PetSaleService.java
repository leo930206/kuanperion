// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.io.IOException;import java.util.*;import java.util.function.*;
/** Default-off, lazy one-pet runs. Failed or recovered sales require explicit re-enabling; no automatic custody retry. */
public final class PetSaleService {
 private static final long S=1_000_000_000L;private final AutomationSettingsStore settings;private final AutomationCoordinator coordinator;private final AutomationSession session;private final InventoryReceipts items;private final CursorReceipts cursors;private final Supplier<AutomationContext> context;private final Supplier<PetGeorgeTask.Port> ports;private final BooleanSupplier screenClosed;private final Consumer<StashService.Notice> feedback;private final ManualTaskRequest manual=new ManualTaskRequest(false);
 private PetGeorgeTask task;private boolean suspended,attempted,reported,polled;private long lastAttempt,lastPoll;
 public PetSaleService(AutomationSettingsStore settings,AutomationCoordinator coordinator,AutomationSession session,InventoryReceipts items,CursorReceipts cursors,Supplier<AutomationContext> context,Supplier<PetGeorgeTask.Port> ports,BooleanSupplier screenClosed,Consumer<StashService.Notice> feedback){this.settings=settings;this.coordinator=coordinator;this.session=session;this.items=items;this.cursors=cursors;this.context=context;this.ports=ports;this.screenClosed=screenClosed;this.feedback=feedback;}
 public PetSalePolicy policy(){return settings.petPolicy();}private boolean configured(){return settings.writable()&&settings.petPolicyValid()&&!policy().allowed().isEmpty();}
 public boolean active(){return task!=null&&task.active();}public boolean pending(){return manual.pending();}public boolean ownsMenu(){return active()&&task.ownsMenu();}public boolean autoEnabled(){return !suspended&&configured()&&settings.enabled(AutomationFeature.PET_SELL);}
 private AutomationContext current(){try{var c=context.get();return c==null?null:AutomationSession.forFeature(c,AutomationFeature.PET_SELL);}catch(RuntimeException unavailable){return null;}}
 private Boolean closed(){try{return screenClosed.getAsBoolean();}catch(RuntimeException unavailable){return null;}}
 private boolean cooling(long now){return attempted&&(now-lastAttempt<0||now-lastAttempt<policy().intervalSeconds()*S);}
 public void setPolicy(PetSalePolicy value)throws IOException{Objects.requireNonNull(value);cancel(System.nanoTime());try{settings.setPetPolicy(value);}catch(IOException failure){suspended=true;throw failure;}}
 public void setEnabled(boolean value)throws IOException{manual.clear();if(!value){suspended=true;cancel(System.nanoTime());}else if(!configured())throw new IllegalArgumentException("請先指定寵物種類、稀有度、經驗上限與最低售價");settings.setEnabled(AutomationFeature.PET_SELL,value);suspended=!value;}
 public boolean manualRequest(long now){return !active()&&!pending()&&configured()&&!cooling(now)&&manual.offer(current(),session.active(),now);}
 private boolean request(boolean user,long now){
  if(!configured()||session.active())return false;var c=current();var p=policy();if(c==null||!c.canIssueCommand()||!user&&p.onlyWhileFarming()&&!c.farmingActive())return false;
  PetGeorgeTask.Port port;try{port=Objects.requireNonNull(ports.get());}catch(RuntimeException unavailable){return false;}
  var next=new PetGeorgeTask(coordinator,session,items,cursors,port);boolean started=next.request(p.rules(),p.minimumPayout(),now);if(!started&&next.state()==PetGeorgeTask.State.IDLE)return false;task=next;attempted=true;lastAttempt=now;reported=false;if(!started)report();return true;
 }
 public void tick(long now){
  if(active()){var ready=closed();if(ready==null||!ready&&!ownsMenu())cancel(now);else{task.tick(now);report();}return;}
  if(pending()){var ready=closed();if(ready==null){manual.clear();return;}if(manual.take(current(),session.active(),ready,now)&&!request(true,now))feedback.accept(new StashService.Notice("Pet Sell 未開始：沒有符合授權的寵物，或目前狀態不允許操作。",false,"Pet Sell"));return;}
  if(!autoEnabled()||!Boolean.TRUE.equals(closed())||cooling(now))return;if(session.active()){polled=false;return;}if(polled&&now-lastPoll>=0&&now-lastPoll<S)return;polled=true;lastPoll=now;request(false,now);
 }
 public PetGeorgeTask.CloseTicket beforeServerClose(int container,long now){return active()?task.beforeServerClose(container,now):null;}
 public void afterServerClose(PetGeorgeTask.CloseTicket ticket,long now){if(active())task.afterServerClose(ticket,now);}
 public void onSystemMessage(long world,String text,long now){if(active())task.onSystemMessage(world,text,now);}
 public void cancel(long now){boolean queued=manual.clear();if(queued||active())suspended=true;if(active()){task.cancel(now);report();}}
 public void suspend(long now){suspended=true;cancel(now);}
 private void report(){
  if(task==null||task.active()||reported)return;reported=true;boolean success=task.state()==PetGeorgeTask.State.COMPLETE;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.PET_SELL,false);}catch(IOException ignored){}}
  String text=success?"Pet Sell 已確認售出一隻授權寵物。":task.state()==PetGeorgeTask.State.RECOVERED?"Pet Sell 未取得符合最低價的報價，已確認取回原寵物；已停用自動賣寵。":task.state()==PetGeorgeTask.State.CANCELLED?"Pet Sell 已停止，等待操作。":"Pet Sell 未完成：選單、報價、物品或伺服器回條驗證失敗；已停用自動賣寵。";
  if(task.manualRecoveryRequired())text+=" 請手動確認 George 選單與原寵物。";feedback.accept(new StashService.Notice(text,!success&&task.state()!=PetGeorgeTask.State.CANCELLED&&task.state()!=PetGeorgeTask.State.RECOVERED,"Pet Sell"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.petPolicyValid()?"賣寵設定無效，未執行或覆寫":!configured()?"請先指定寵物種類與稀有度":pending()?"關閉選單後處理一隻寵物（30 秒內有效）":task==null?"IDLE":task.state().name()+(task.manualRecoveryRequired()?" · 請手動確認原寵物":"");}
}
