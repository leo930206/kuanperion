// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
/** One selected pet per bounded original-root George run, with proven NPC custody and exact own-pet recovery. */
public final class PetGeorgeTask {
 public enum State{IDLE,OPENING,WAIT_CUSTODY,QUOTING,SELLING,WAIT_RETURN,COMPLETE,RECOVERED,CANCELLED,FAILED,UNCERTAIN}
 public interface Port {
  AutomationContext context();boolean available();Object level();Object pose();Object menuIdentity();boolean idle();List<PetSalePreflight.Pet> pets();PetSaleAttempt.Frame frame(long now);
  boolean open(AutomationSession.Handle h,long now);
  GuardedMenuAction.Result click(AutomationSession.Handle h,PetSalePreflight.Pet pet,MenuContract contract,MenuContract.Ticket ticket,Supplier<MenuSnapshot> proof,long now);
  boolean close(Object identity,int container);default void dispose(AutomationSession.Handle h){}
 }
 private static final long S=1_000_000_000L;private final AutomationCoordinator coordinator;private final AutomationSession session;private final InventoryReceipts items;private final CursorReceipts cursors;private final Port port;
 public static final class CloseTicket{
  private final AutomationSession.Handle owner;private final PetSaleAttempt attempt;private final Object identity;private final int container;private final long at;
  private CloseTicket(AutomationSession.Handle h,PetSaleAttempt attempt,Object identity,int container,long now){owner=h;this.attempt=attempt;this.identity=identity;this.container=container;at=now;}
 }
 private CloseTicket pendingClose;private boolean appliedClose;
 private State state=State.IDLE;private AutomationSession.Handle handle;private PetSalePreflight.Plan plan;private PetSalePreflight.Pet pet;private PetSaleCustody custody;private PetSaleCustody.Token token;private PetSaleAttempt sale;private PetSaleAttempt.Quote quote;private Object level,pose,boundIdentity;private int boundContainer=-1;private long minimum,started,phaseAt;private InventoryReceipts.Stamp opening,returnItems;private CursorReceipts.OpeningStamp cursorOpening,returnCursor;private PetSaleAttempt.Frame deposited;private boolean recovery;
 public PetGeorgeTask(AutomationCoordinator c,AutomationSession s,InventoryReceipts items,CursorReceipts cursors,Port port){coordinator=c;session=s;this.items=items;this.cursors=cursors;this.port=port;}
 public State state(){return state;}public boolean active(){return switch(state){case OPENING,WAIT_CUSTODY,QUOTING,SELLING,WAIT_RETURN->true;default->false;};}public boolean manualRecoveryRequired(){return recovery;}
 public boolean request(PetSalePreflight.Rules rules,long minimum,long now){
  if(active()||rules==null||minimum<1||minimum>1_000_000_000_000_000L)return false;
  try{
   if(!port.available()||!port.idle()||!AutomationSession.forFeature(port.context(),AutomationFeature.PET_SELL).canIssueCommand())return false;var frame=port.frame(now);if(frame.menu()!=null||!frame.cursorEmpty())return false;
   var selected=PetSalePreflight.select(port.pets(),frame.inventory(),rules).orElse(null);if(selected==null)return false;var h=session.begin(AutomationFeature.PET_SELL,now,120*S);if(h==null)return false;
   handle=h;plan=selected;pet=plan.pets().getFirst();this.minimum=minimum;level=port.level();pose=port.pose();started=now;pendingClose=null;appliedClose=false;boundIdentity=null;boundContainer=-1;recovery=false;token=null;sale=null;quote=null;deposited=null;custody=new PetSaleCustody(session,coordinator,items,cursors,port::context);
   opening=items.stamp(h.lease().worldId());cursorOpening=cursors.openingStamp(h.lease().worldId(),now);if(!expect(State.OPENING,now)||!root(now)||!PetSalePreflight.stillMatches(plan,port.frame(now).inventory())||!port.open(h,now)){finish(State.FAILED,now);return false;}return true;
  }catch(RuntimeException unavailable){if(handle!=null)finish(State.FAILED,now);return false;}
 }
 private boolean root(long now){
  var h=handle;if(h==null||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||!port.available()||port.level()!=level||!Objects.equals(port.pose(),pose)||now-started<0||now-started>=120*S)return false;
  var c=port.context();return handle==h&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&c!=null&&c.worldId()==h.lease().worldId()&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU||b==AutomationContext.Blocker.INVENTORY_FULL);
 }
 public boolean ownsMenu(){try{
  if(!active()||handle==null||!session.isCurrent(handle)||!coordinator.holdsIdentity(handle.lease())||port.level()!=level)return false;var m=port.frame(phaseAt).menu();if(m==null)return state==State.OPENING||state==State.SELLING;
  if(m.worldId()!=handle.lease().worldId())return false;
  if(Set.of("George","Sell Pets","Pet Collector","Offer Pets").contains(m.title()))return true;
  return state==State.SELLING&&Set.of("Confirm Sale","Are you sure?","Are You Sure?").contains(m.title());
 }catch(RuntimeException unavailable){return false;}}
 private boolean expect(State next,long now){state=next;phaseAt=now;return session.expectScreenSequence(handle,this::ownsMenu,now,10*S);}
 private boolean applied(PetSaleAttempt.Frame frame,InventoryReceipts.Stamp stamp,CursorReceipts.OpeningStamp cursor,long now){
  if(!PetSaleAttempt.fresh(frame,now)||!PetSaleAttempt.complete(frame.inventory())||!PetSaleAttempt.aliases(frame)||!frame.cursorEmpty()||!cursors.emptyAfterOpening(cursor,frame.menu().containerId(),now,S))return false;
  for(int i=0;i<frame.menu().containerSlots();i++)if(!PetSaleAttempt.applied(items.after(stamp,frame.menu().containerId(),i),frame.menu().item(i)))return false;return true;
 }
 private GuardedMenuAction.Result click(MenuContract c,MenuContract.Ticket t,PetSaleAttempt.Frame frozen,Object identity,long now){
  return port.click(handle,pet,c,t,()->{if(!root(now)||port.menuIdentity()!=identity)return null;var current=port.frame(now);return port.menuIdentity()==identity&&current.cursorEmpty()&&current.inventory().equals(frozen.inventory())&&Objects.equals(current.menu(),frozen.menu())?current.menu():null;},now);
 }
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException unavailable){finish(recovery?State.UNCERTAIN:State.FAILED,now);}}
 private void run(long now){
  if(!root(now)){finish(State.CANCELLED,now);return;}var identity=port.menuIdentity();var frame=port.frame(now);if(identity!=port.menuIdentity()){finish(State.CANCELLED,now);return;}
  if(!appliedClose&&boundIdentity!=null&&identity!=boundIdentity&&(frame.menu()==null||frame.menu().containerId()==boundContainer||state!=State.WAIT_CUSTODY&&state!=State.SELLING&&state!=State.WAIT_RETURN)){finish(State.CANCELLED,now);return;}
  if(!frame.cursorEmpty()||now-phaseAt<0||now-phaseAt>=10*S){finish(recovery?State.UNCERTAIN:State.FAILED,now);return;}
  if(state==State.OPENING){
   if(frame.menu()==null)return;if(!GeorgeSaleView.main(frame.menu())||!PetSalePreflight.stillMatches(plan,frame.inventory())||!applied(frame,opening,cursorOpening,now))return;
   var contract=new MenuContract(Set.of(frame.menu().title()),frame.menu().containerSlots());var ticket=contract.plan(frame.menu(),frame.menu().containerSlots()+pet.slot()-9,MenuContract.Click.QUICK_MOVE);if(ticket==null||!expect(State.WAIT_CUSTODY,now)){finish(State.FAILED,now);return;}
   boundIdentity=identity;boundContainer=frame.menu().containerId();recovery=true;if(!custody.start(handle,plan,pet,13,frame,opening,cursorOpening,now,()->click(contract,ticket,frame,identity,now)))finish(State.UNCERTAIN,now);return;
  }
  if(state==State.WAIT_CUSTODY){
   custody.poll(frame,now);if(custody.state()==PetSaleCustody.State.CONFIRMED){token=custody.token().orElseThrow();deposited=frame;boundIdentity=identity;boundContainer=frame.menu().containerId();expect(State.QUOTING,now);}else if(custody.state()==PetSaleCustody.State.UNCERTAIN||custody.state()==PetSaleCustody.State.REJECTED)finish(State.UNCERTAIN,now);return;
  }
  if(state==State.QUOTING){
   if(!applied(frame,opening,cursorOpening,now)||!deposited.inventory().equals(frame.inventory()))return;var offer=GeorgeSaleView.offer(frame.menu(),pet,minimum).orElse(null);
   if(offer==null){var belowMinimum=GeorgeSaleView.offer(frame.menu(),pet,1).orElse(null);if(belowMinimum!=null||now-phaseAt>=5*S)recover(frame,identity,now);return;}quote=offer.quote();sale=new PetSaleAttempt(session,coordinator,items,cursors,port::context);if(!expect(State.SELLING,now)||!sale.startCustody(handle,token,quote,frame,now,()->click(offer.contract(),offer.ticket(),frame,identity,now)))finish(State.UNCERTAIN,now);return;
  }
  if(state==State.SELLING){
   if(appliedClose){sale.pollAfterAppliedClose(frame,now);if(sale.state()==PetSaleAttempt.State.CONFIRMED){recovery=false;finish(State.COMPLETE,now);}else if(sale.state()==PetSaleAttempt.State.UNCERTAIN)finish(State.UNCERTAIN,now);return;}

   if(frame.menu()!=null){var confirmation=GeorgeSaleView.confirmation(frame.menu(),quote).orElse(null);if(confirmation!=null&&sale.continueCustody(frame,cursorOpening,now,()->click(confirmation.contract(),confirmation.ticket(),frame,identity,now))){boundIdentity=identity;boundContainer=frame.menu().containerId();}}
   sale.poll(frame,now);if(sale.state()==PetSaleAttempt.State.CONFIRMED){recovery=false;closeAndFinish(frame,identity,State.COMPLETE,now);}else if(sale.state()==PetSaleAttempt.State.UNCERTAIN||sale.state()==PetSaleAttempt.State.REJECTED)finish(State.UNCERTAIN,now);return;
  }
  if(state==State.WAIT_RETURN){
   if(!PetSaleAttempt.fresh(frame,now)||!PetSaleAttempt.complete(frame.inventory())||!PetSaleAttempt.aliases(frame)||!deposited.inventory().keySet().equals(frame.inventory().keySet()))return;int destination=-1;
   for(int i=0;i<43;i++){var old=deposited.inventory().get(i);var next=frame.inventory().get(i);if(old.equals(next))continue;if(destination>=0||i<9||i>=36||!PetSaleAttempt.air(old)||!PetSaleAttempt.applied(pet.item(),next)){finish(State.UNCERTAIN,now);return;}destination=i;}
   if(destination<0||!PetSaleAttempt.air(frame.menu().item(13))||!cursors.emptyAfterOpening(returnCursor,frame.menu().containerId(),now,S))return;
   for(var slot:frame.menu().slots())if(!slot.playerInventory()&&pet.item().uuid().equalsIgnoreCase(slot.item().uuid()))return;
   if(!PetSaleAttempt.applied(items.after(returnItems,0,destination),frame.inventory().get(destination))||!PetSaleAttempt.applied(items.after(returnItems,frame.menu().containerId(),frame.menu().containerSlots()+destination-9),frame.inventory().get(destination))||!PetSaleAttempt.applied(items.after(returnItems,frame.menu().containerId(),13),frame.menu().item(13)))return;
   recovery=false;closeAndFinish(frame,identity,State.RECOVERED,now);
  }
 }
 private void recover(PetSaleAttempt.Frame frame,Object identity,long now){
  if(!GeorgeSaleView.main(frame.menu())||!deposited.inventory().equals(frame.inventory())||!PetSaleAttempt.applied(pet.item(),frame.menu().item(13))){finish(State.UNCERTAIN,now);return;}
  var c=new MenuContract(Set.of(frame.menu().title()),frame.menu().containerSlots());var t=c.plan(frame.menu(),13,MenuContract.Click.QUICK_MOVE);if(t==null||!expect(State.WAIT_RETURN,now)||click(c,t,frame,identity,now)!=GuardedMenuAction.Result.SENT){finish(State.UNCERTAIN,now);return;}
  returnItems=items.stamp(handle.lease().worldId());returnCursor=cursors.openingStamp(handle.lease().worldId(),now);
 }
 private void closeAndFinish(PetSaleAttempt.Frame frame,Object identity,State terminal,long now){
  if(identity==null||!root(now)||!port.close(identity,frame.menu().containerId())||!root(now)){finish(State.UNCERTAIN,now);return;}var after=port.frame(now);if(after.menu()!=null||!after.cursorEmpty()||!after.inventory().equals(frame.inventory())){finish(State.UNCERTAIN,now);return;}finish(terminal,now);
 }
 public CloseTicket beforeServerClose(int container,long now){
  pendingClose=null;if(state!=State.SELLING||appliedClose||sale==null||sale.state()!=PetSaleAttempt.State.AWAITING||!root(now))return null;
  var frame=port.frame(now);if(frame.menu()==null||frame.menu().containerId()!=container||container!=boundContainer||port.menuIdentity()!=boundIdentity||!ownsMenu())return null;
  return pendingClose=new CloseTicket(handle,sale,boundIdentity,container,now);
 }
 public void afterServerClose(CloseTicket ticket,long now){
  if(ticket==null||ticket!=pendingClose)return;pendingClose=null;
  if(state!=State.SELLING||ticket.owner!=handle||ticket.attempt!=sale||ticket.container!=boundContainer||now-ticket.at<0||now-ticket.at>S||!root(now)||!port.idle()||port.menuIdentity()==ticket.identity)return;
  var frame=port.frame(now);if(frame.menu()==null&&frame.cursorEmpty()&&PetSaleAttempt.complete(frame.inventory())&&state==State.SELLING&&ticket.owner==handle&&session.isCurrent(handle))appliedClose=true;
 }
 public void onSystemMessage(long world,String text,long now){if(state==State.SELLING&&sale!=null&&GeorgeSaleView.saleMessage(text,quote))sale.onServerSale(world,pet.metadata().uuid(),quote.payout(),now);}
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){var done=handle;handle=null;pendingClose=null;level=null;pose=null;boundIdentity=null;state=terminal;var result=AutomationCompletion.finish(session,done,terminal==State.COMPLETE||terminal==State.RECOVERED,now,()->AutomationStops.run(()->{if(custody!=null)custody.cancel();},()->{if(sale!=null)sale.cancel();}),()->port.dispose(done),()->{});if(result.failed())state=State.FAILED;}
}
