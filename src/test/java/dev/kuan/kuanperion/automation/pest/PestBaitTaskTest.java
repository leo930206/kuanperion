package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Real lease/session/menu guard + synthetic declared direct-item capacity; no native/live format assumption. */
public final class PestBaitTaskTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}static final long S=1_000_000_000L;
 static final class F implements PestBaitTask.Port {
  long now,world=1;boolean visible,manual,noBait=true,dropPackets,unknownLayout,closeWorks=true;int opens,clicks,closes,stops,capacity=2;Map<Integer,ItemSnapshot> inv=PestBaitTransferTest.inventory();ItemSnapshot cursor=PestBaitTransferTest.AIR,bait=PestBaitTransferTest.AIR;Runnable pending;GuardedMenuAction.Result outcome=GuardedMenuAction.Result.SENT;
  final PestTrapTarget target=new PestTrapTarget(4,UUID.fromString("00000000-0000-0000-0000-000000000001"),1,"Pest Trap",new PestTargets.Point(0,70,0));
  InventoryReceipts items=new InventoryReceipts();CursorReceipts cursors=new CursorReceipts();AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->stops++);AutomationSession.Handle handle=session.begin(AutomationFeature.PEST,0,120*S);PestBaitTask task=new PestBaitTask(session,items,cursors,this);
  public AutomationContext context(){return new AutomationContext(world,true,true,visible?Set.of(AutomationContext.Blocker.MENU):Set.of());}public boolean manualInput(){return manual;}public Map<Integer,ItemSnapshot> inventory(){return inv;}public ItemSnapshot cursor(){return cursor;}
  public MenuSnapshot menu(){return !visible?null:PestBaitTransferTest.menu(inv,bait);}
  public PestTrapRelease.Facts facts(long now){return new PestTrapRelease.Facts(world,now,0,PestBaitTransferTest.traps(noBait),List.of(target));}
  public Optional<PestBaitTransfer.Layout> layout(PestTrapTarget target,MenuSnapshot menu){return unknownLayout?Optional.empty():Optional.of(new PestBaitTransfer.Layout("Pest Trap #1",27,13,capacity));}
  public PestTrapRelease.Open open(AutomationSession.Handle h,PestTrapTarget target,long n){opens++;visible=true;return PestTrapRelease.Open.SENT;}
  public GuardedMenuAction.Result transfer(AutomationSession.Handle h,PestBaitTransfer.Plan plan,PestBaitTransfer.Step step,MenuContract.Ticket ticket,long n){
   check(h.equals(handle),"same original root, no nested Farm acquisition");if(outcome!=GuardedMenuAction.Result.SENT)return outcome;
   return GuardedMenuAction.perform(coordinator,h.lease(),plan.layout().contract(),ticket,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,this::context,this::menu,t->{clicks++;switch(step){
    case PICKUP->{check(t.slot()==28&&cursor.count()==0,"exact protected source");cursor=inv.get(10);inv.put(10,PestBaitTransferTest.AIR);pending=()->{items.observe(world,7,28,inv.get(10));cursors.observe(world,7,cursor,now);};}
    case DEPOSIT->{check(t.slot()==13&&cursor.count()==8,"exact declared direct-item capacity before transfer");bait=PestBaitTransferTest.count(cursor,2);cursor=PestBaitTransferTest.count(cursor,6);noBait=false;pending=()->{items.observe(world,7,13,bait);cursors.observe(world,7,cursor,now);};}
    case RETURN->{check(t.slot()==28&&inv.get(10).count()==0&&cursor.count()==6,"owned remainder to original empty slot only");inv.put(10,cursor);cursor=PestBaitTransferTest.AIR;pending=()->{items.observe(world,7,28,inv.get(10));cursors.observe(world,7,cursor,now);};}
   }return true;},n);
  }
  public void closeOwnedMenu(int id){check(id==7&&cursor.count()==0,"close only exact container with physically empty cursor");closes++;if(closeWorks)visible=false;}
  void start(){check(task.begin(handle,target,new PestBaitTransfer.Rules("PLANT_MATTER",2,8,Set.of()),now),"explicit empty-trap subtask");}
  void tick(){now+=S/10;if(pending!=null&&!dropPackets){var p=pending;pending=null;p.run();}session.poll(now);task.tick(now);}
  void finish(){for(int i=0;i<350&&task.active();i++)tick();}
 }
 public static void main(String[]args){
  F f=new F();f.start();f.finish();check(f.task.state()==PestBaitTask.State.COMPLETE&&f.clicks==3&&f.closes==1&&f.inv.get(10).count()==6&&f.cursor.count()==0,"exact pickup/deposit/server proof/owned return/NoBait/close sequence");check(f.session.isCurrent(f.handle)&&f.session.holdsFarming()&&f.stops==0,"subtask must not release original Farm before parent capture/return");
  f=new F();check(!f.task.begin(f.handle,f.target,PestBaitTransfer.Rules.defaults(),0)&&f.opens==0,"off/default bait authority cannot open GUI");f.inv.put(10,PestBaitTransferTest.AIR);check(!f.task.begin(f.handle,f.target,new PestBaitTransfer.Rules("PLANT_MATTER",2,8,Set.of()),0)&&f.opens==0,"missing protected material preflight before GUI");
  f=new F();f.unknownLayout=true;f.start();f.finish();check(f.clicks==0&&f.closes==0&&f.task.state()!=PestBaitTask.State.COMPLETE,"unknown actual role/capacity never moves an item or closes GUI");
  f=new F();f.dropPackets=true;f.start();f.finish();check(f.clicks==1&&f.closes==0&&f.task.state()!=PestBaitTask.State.COMPLETE&&f.cursor.count()==8,"prediction without source/cursor receipts stops holding own item; no repair/retry/close");
  f=new F();f.start();while(f.clicks==0)f.tick();f.manual=true;f.tick();check(!f.task.active()&&f.clicks==1&&f.closes==0,"manual takeover beats delayed acknowledgement");
  f=new F();f.outcome=GuardedMenuAction.Result.UNCERTAIN;f.start();f.finish();check(f.task.state()==PestBaitTask.State.FAILED&&f.clicks==0&&f.closes==0,"possibly sent outcome terminal without replay");
  f=new F();f.closeWorks=false;f.start();f.finish();check(f.task.state()==PestBaitTask.State.FAILED&&f.clicks==3&&f.closes==1&&f.session.holdsFarming(),"failed owned close cannot claim complete or release original hold");
  f=new F();f.start();f.tick();f.session.invalidate();f.visible=false;var newer=f.session.begin(AutomationFeature.AUTO_SELL,f.now,60*S);check(newer!=null,"new owner");f.task.tick(f.now);check(!f.task.active()&&f.session.isCurrent(newer)&&f.closes==0,"stale subtask cannot cancel/close new owner's context");
  f=new F();f.start();while(f.task.state()!=PestBaitTask.State.TRANSFER)f.tick();f.capacity=8;f.tick();check(!f.task.active()&&f.clicks==0&&f.closes==0,"changed capacity must stop BEFORE picking up an oversized source stack");
  f=new F();f.start();while(f.clicks==0)f.tick();f.capacity=8;f.tick();check(!f.task.active()&&f.clicks==1&&f.closes==0,"capacity must be re-proved after pickup and BEFORE deposit, not merely reject over-consumption afterwards");
  f=new F();f.start();while(f.clicks==0)f.tick();f.noBait=false;f.tick();check(!f.task.active()&&f.clicks==1&&f.closes==0,"another refill's newer NoBait update preempts this deposit");
  f=new F();f.start();while(f.task.state()!=PestBaitTask.State.CLOSING)f.tick();f.cursors.clear();f.tick();check(f.task.state()==PestBaitTask.State.FAILED&&f.closes==0,"lost settled cursor scope prevents even an owned close");
  System.out.println("PestBaitTaskTest: PASS (same original Pest/Farm root, explicit material/target/declared capacity, ordered guarded transfer + server receipt proof + exact owned close; manual/unknown/revoked/uncertain failures never repair/retry or release parent hold)");
 }
}
