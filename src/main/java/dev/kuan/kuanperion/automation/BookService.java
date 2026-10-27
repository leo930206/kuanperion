// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.io.IOException;import java.util.Objects;import java.util.function.*;
/** One exact authorized pair per attempt; no startup driver, paid combine or silent recovery/retry. */
public final class BookService {
 private static final long SECOND=1_000_000_000L,COOLDOWN=20*SECOND;
 private final AutomationSettingsStore settings;private final AutomationSession session;private final InventoryReceipts receipts;private final CursorReceipts cursors;
 private final Supplier<AutomationContext> context;private final Function<BookCombineTask.Policy,BookCombineTask.Port> ports;private final BookAnvilView.Layout layout;private final BooleanSupplier screenClosed;private final Consumer<StashService.Notice> feedback;
 private final ManualTaskRequest manual=new ManualTaskRequest(false);private BookCombineTask task;private boolean suspended,attempted,polled,reported;private long lastAttempt,lastPoll;
 public BookService(AutomationSettingsStore settings,AutomationSession session,InventoryReceipts receipts,CursorReceipts cursors,Supplier<AutomationContext> context,Function<BookCombineTask.Policy,BookCombineTask.Port> ports,BookAnvilView.Layout layout,BooleanSupplier screenClosed,Consumer<StashService.Notice> feedback){
  this.settings=Objects.requireNonNull(settings);this.session=Objects.requireNonNull(session);this.receipts=Objects.requireNonNull(receipts);this.cursors=Objects.requireNonNull(cursors);this.context=Objects.requireNonNull(context);this.ports=Objects.requireNonNull(ports);this.layout=Objects.requireNonNull(layout);this.screenClosed=Objects.requireNonNull(screenClosed);this.feedback=Objects.requireNonNull(feedback);
 }
 public BookCombineTask.Policy policy(){return settings.bookPolicy();}
 private boolean configured(){return settings.writable()&&settings.bookPolicyValid()&&!policy().ceilings().isEmpty();}
 public boolean autoEnabled(){return !suspended&&configured()&&settings.enabled(AutomationFeature.BOOK_COMBINE);}
 public boolean active(){return task!=null&&task.active();}public boolean pending(){return manual.pending();}public boolean ownsMenu(){return active()&&task.ownsMenu();}
 public void setEnabled(boolean value)throws IOException{
  manual.clear();if(!value){suspended=true;cancel(System.nanoTime());}else if(!configured())throw new IllegalArgumentException("請先設定合書附魔 ID 與最高等級");
  settings.setEnabled(AutomationFeature.BOOK_COMBINE,value);suspended=!value;
 }
 public void setPolicy(BookCombineTask.Policy value)throws IOException{Objects.requireNonNull(value);cancel(System.nanoTime());try{settings.setBookPolicy(value);}catch(IOException failure){suspended=true;throw failure;}}
 private AutomationContext current(){try{var c=context.get();return c==null?null:AutomationSession.forFeature(c,AutomationFeature.BOOK_COMBINE);}catch(RuntimeException unavailable){return null;}}
 private boolean cooling(long now){return attempted&&(now-lastAttempt<0||now-lastAttempt<COOLDOWN);}
 public boolean manualRequest(long now){if(active()||pending()||!configured()||cooling(now))return false;return manual.offer(current(),session.active(),now);}
 private boolean request(boolean user,long now){
  if(!configured()||session.active())return false;var c=current();if(c==null||!c.canIssueCommand()||!user&&policy().onlyWhileFarming()&&!c.farmingActive())return false;
  var p=policy();var port=Objects.requireNonNull(ports.apply(p));var next=new BookCombineTask(session,receipts,cursors,port,layout);
  if(!next.request(p,user,now))return false;task=next;attempted=true;lastAttempt=now;reported=false;task.tick(now);report();return true;
 }
 public void tick(long now){
  if(active()){if(!screenClosed.getAsBoolean()&&!ownsMenu())cancel(now);else{task.tick(now);report();}return;}
  if(pending()){if(manual.take(current(),session.active(),screenClosed.getAsBoolean(),now)&&!request(true,now))feedback.accept(new StashService.Notice("Book Combine 未開始：找不到符合授權且未受保護的同級書，或目前狀態不允許操作。",false,"Book Combine"));return;}
  if(!autoEnabled()||!screenClosed.getAsBoolean()||cooling(now))return;
  if(session.active()){polled=false;return;}
  if(polled&&now-lastPoll>=0&&now-lastPoll<SECOND)return;polled=true;lastPoll=now;request(false,now);
 }
 /** Global/manual stops revoke the task without reclaim/deposit/close; recovery is explicitly manual. */
 public void cancel(long now){boolean queued=manual.clear();if(queued||active())suspended=true;if(active()){task.interrupt(now);report();}}
 public void suspend(long now){suspended=true;cancel(now);}
 private void report(){
  if(task==null||task.active()||reported)return;reported=true;boolean success=task.state()==BookCombineTask.State.COMPLETE;
  if(!success){suspended=true;try{settings.setEnabled(AutomationFeature.BOOK_COMBINE,false);}catch(IOException ignored){}}
  String text=success?"Book Combine 已確認合成並取回一本書。":task.state()==BookCombineTask.State.CANCELLED?"Book Combine 已停止，等待操作。":"Book Combine 未完成：選單、預覽、費用或伺服器回條不符；已停用自動合書，請手動確認物品。";
  feedback.accept(new StashService.Notice(text,!success&&task.state()!=BookCombineTask.State.CANCELLED,"Book Combine"));
 }
 public String status(){return !settings.writable()?settings.issue():!settings.bookPolicyValid()?"合書設定無效，未執行或覆寫":!configured()?"請先設定附魔 ID 與最高等級；目前僅接受明確零經驗費用":pending()?"關閉選單後合成一組（30 秒內有效）":task==null?"IDLE":task.state().name()+(task.manualRecoveryRequired()?" · 請手動確認書本與選單":"");}
}
