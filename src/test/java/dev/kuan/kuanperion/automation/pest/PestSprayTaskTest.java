package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayTaskTest {
 static final long S=1_000_000_000L;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class Fixture implements PestSprayTask.Port {
  long now=S,positionEpoch=1,positionSequence;Object level=new Object(),player=new Object();AutomationContext context=new AutomationContext(1,true,true,Set.of());boolean manual,menu,using,acquireUnarmed;int selected=0,pauses,stops,equips,uses,restores;double x=0,y=72,z=-96,vx=0;float yaw=20,pitch=0;
  Map<Integer,ItemSnapshot> inv=PestSprayPlanTest.inventory();PestSprayObservation spray=new PestSprayObservation(PestSprayObservation.State.NONE);final InventoryReceipts receipts=new InventoryReceipts();final AutomationCoordinator coordinator=new AutomationCoordinator();
  final AutomationSession session=new AutomationSession(coordinator,()->{if(acquireUnarmed){acquireUnarmed=false;return new AutomationContext(1,false,true,Set.of());}return context;},()->pauses++,()->{stops++;context=new AutomationContext(1,false,true,Set.of());});
  final PestSprayTask task=new PestSprayTask(session,coordinator,receipts,this);PestSprayTask.Action last;boolean claim=true,uncertain,earlyAck,afterClaimAck,reentrantCancel,catalogMissing,catalogWrong;int dataRequests,disposals,contextReads,hookAt;boolean revokeDemand;Runnable contextHook;Runnable beforeClaim,readHook;
  public void requestSprayData(AutomationSession.Handle h){check(session.isCurrent(h),"public data demand must belong to original root");dataRequests++;if(revokeDemand)session.invalidate();}public Optional<Map<String,String>> sprayCatalog(AutomationSession.Handle h){return catalogMissing?Optional.empty():Optional.of(Map.of("Test Material",catalogWrong?"DIFFERENT_ID":"TEST_MATERIAL"));}public void disposeSprayData(AutomationSession.Handle h){disposals++;}
  public AutomationContext context(){if(++contextReads==hookAt&&contextHook!=null){var r=contextHook;contextHook=null;r.run();}if(readHook!=null){var r=readHook;readHook=null;r.run();}return context;}public boolean manualInput(){return manual;}
  public PestSprayTask.Frame sprayFrame(long at){return new PestSprayTask.Frame(1,at,level,player,inv,PestSprayPlanTest.AIR,selected,!menu,using,x,y,z,vx,0,0,yaw,pitch,spray,positionEpoch,positionSequence);}
  public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action a,long at){
   last=a;if(earlyAck&&a.kind()==PestSprayTask.Kind.USE)task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",at);if(beforeClaim!=null){var r=beforeClaim;beforeClaim=null;r.run();}
   if(!claim||!a.claim(h,sprayFrame(at),at))return PestSprayTask.Effect.REJECTED;
   if(a.kind()==PestSprayTask.Kind.EQUIP){selected=4;equips++;}else if(a.kind()==PestSprayTask.Kind.RESTORE){selected=0;restores++;}else{uses++;if(afterClaimAck)task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",at);if(reentrantCancel)task.cancel(at);}
   return uncertain?PestSprayTask.Effect.UNCERTAIN:PestSprayTask.Effect.SENT;
  }
  void begin(){check(task.request(PestSprayPlanTest.policy(),now),"valid independent request should acquire root");}
  void tick(){now+=200_000_000L;task.tick(now);}void readyUse(){begin();for(int i=0;i<8&&uses==0&&task.active();i++)tick();check(uses==1,"settle/equip/use path must actually send one use: "+task.state());}
  void decrease(int n,boolean applied){inv=new HashMap<>(inv);inv.put(9,PestSprayPlanTest.material(12-n));if(applied)receipts.observe(1,0,9,PestSprayPlanTest.material(12-n));}
  void acknowledge(){task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",now);spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);}
 }
 public static void main(String[]args){
  check(!AutomationFeature.PEST_SPRAY.implemented()&&AutomationFeature.PEST_SPRAY.requiresGarden()&&AutomationFeature.PEST_SPRAY.resources().equals(Set.of(AutomationCoordinator.Resource.HOTBAR,AutomationCoordinator.Resource.USE)),"unexposed fixed Garden foundation declares only real effect resources, no false finished-module flag");
  var f=new Fixture();f.readyUse();check(f.pauses==1&&f.session.holdsFarming()&&f.task.reservedItems()==5&&f.task.state()==PestSprayTask.State.AWAITING,"same independent root and worst case charged before use");
  f.acknowledge();f.decrease(3,false);f.tick();check(f.task.state()==PestSprayTask.State.AWAITING&&f.restores==0,"prediction plus message plus Tab alone cannot complete");f.receipts.observe(1,0,9,f.inv.get(9));f.tick();f.tick();check(f.task.state()==PestSprayTask.State.COMPLETE&&!f.session.active()&&!f.session.holdsFarming()&&f.stops==0&&f.restores==1&&f.selected==0&&f.context.farmingActive()&&f.dataRequests==1&&f.disposals==1,"applied decrease+post-action GAME+new active field then exact original tool restore; never rearm Farm");check(!f.last.claim(f.last.owner(),f.sprayFrame(f.now),f.now),"capability cannot replay after completion");
  f=new Fixture();f.manual=true;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.pauses==0,"physical input blocks root acquisition");
  f=new Fixture();f.context=new AutomationContext(1,false,true,Set.of());check(!f.task.request(PestSprayPlanTest.policy(),f.now),"only manually armed Farming");
  f=new Fixture();var other=f.session.begin(AutomationFeature.BOOK_COMBINE,f.now,20*S);check(other!=null&&!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.session.isCurrent(other),"never preempt incumbent");
  f=new Fixture();f.vx=.2;f.begin();for(int i=0;i<20&&f.task.active();i++)f.tick();check(f.task.state()==PestSprayTask.State.TIMED_OUT&&f.equips+f.uses==0&&f.stops==1,"bounded natural settle, no velocity/movement intervention");
  for(int mode=0;mode<7;mode++){
   f=new Fixture();f.readyUse();switch(mode){case 0->f.manual=true;case 1->f.level=new Object();case 2->f.player=new Object();case 3->f.x=2;case 4->f.yaw++;case 5->f.menu=true;case 6->f.context=new AutomationContext(1,true,false,Set.of());}f.tick();check(!f.task.active()&&f.uses==1&&f.restores==0&&f.stops==1,"late manual/world/player/position/look/menu/Garden stops without repair "+mode);
  }
  for(int mode=0;mode<5;mode++){
   f=new Fixture();f.readyUse();f.decrease(1,true);f.spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);
   if(mode==0)f.task.onSystemMessage(2,"Sprayonator! You sprayed plot 1!",f.now);if(mode==1)f.task.onSystemMessage(1,"Sprayonator! You sprayed plot 2!",f.now);if(mode==2)f.task.onSystemMessage(1,"[Player] Sprayonator! You sprayed plot 1!",f.now);if(mode==3)f.task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!\nforged",f.now);
   if(mode==4){f.task.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",f.now);f.spray=PestSprayObservation.unknown();}
   for(int i=0;i<50&&f.task.active();i++)f.tick();check(f.task.state()==PestSprayTask.State.TIMED_OUT&&f.uses==1&&f.restores==0,"wrong world/plot/player text/control/unknown active cannot substitute for scoped proof "+mode);
  }
  f=new Fixture();f.earlyAck=true;f.readyUse();f.decrease(1,true);f.spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);for(int i=0;i<50&&f.task.active();i++)f.tick();check(f.task.state()==PestSprayTask.State.TIMED_OUT,"message received before private native claim is ignored");
  f=new Fixture();f.readyUse();f.acknowledge();f.decrease(6,true);f.tick();check(f.task.state()==PestSprayTask.State.UNCERTAIN&&f.restores==0,"over-limit stops no retry/refund");
  f=new Fixture();f.readyUse();f.task.onSystemMessage(1,"You don't have enough Test Material!",f.now);f.tick();check(f.task.state()==PestSprayTask.State.FAILED&&f.uses==1,"missing material does not infer permission to buy/retry");
  f=new Fixture();f.readyUse();f.acknowledge();f.task.onSystemMessage(1,"You cannot spray here!",f.now);f.tick();check(f.task.state()==PestSprayTask.State.UNCERTAIN,"contradictory acknowledgements never success");
  f=new Fixture();f.uncertain=true;f.begin();while(f.task.active())f.tick();check(f.task.state()==PestSprayTask.State.UNCERTAIN&&f.uses==0&&f.equips==1,"ambiguous equip not repeated or repaired");
  f=new Fixture();f.claim=false;f.begin();while(f.task.active())f.tick();check(f.task.state()==PestSprayTask.State.FAILED&&f.uses+f.equips==0,"port saying nothing cannot create authority");
  f=new Fixture();f.reentrantCancel=true;f.readyUse();check(f.task.state()==PestSprayTask.State.CANCELLED&&!f.session.active()&&f.stops==1,"reentrant manual cancellation cannot be overwritten by old native result");
  f=new Fixture();f.begin();var live=f;f.beforeClaim=()->live.inv=new HashMap<>(PestSprayPlanTest.inventory()); // same data remains allowed
  while(f.uses==0&&f.task.active())f.tick();check(f.uses==1,"snapshot equality permits same immutable data, not object identity guesses");
  f=new Fixture();f.catalogWrong=true;f.begin();while(f.task.active())f.tick();check(f.task.state()==PestSprayTask.State.FAILED&&f.equips+f.uses==0&&f.disposals==1,"same displayed name is not authority for an ID contradicted by official unique catalog");
  f=new Fixture();f.catalogMissing=true;f.begin();for(int i=0;i<70&&f.task.active();i++)f.tick();check(f.task.state()==PestSprayTask.State.TIMED_OUT&&f.equips+f.uses==0&&f.dataRequests==1&&f.disposals==1,"one demanded lookup, bounded wait, no blind use or retry when unavailable");
  f=new Fixture();f.afterClaimAck=true;f.readyUse();f.decrease(1,true);f.spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);f.tick();f.tick();check(f.task.state()==PestSprayTask.State.COMPLETE,"post-claim in-flight GAME acknowledgement survives return from send");
  f=new Fixture();f.readyUse();f.selected=0;f.tick();check(f.task.state()==PestSprayTask.State.CANCELLED&&f.restores==0,"actual hotbar takeover cancels without restoring over user choice");
  f=new Fixture();f.readyUse();f.positionSequence++;f.tick();check(f.task.state()==PestSprayTask.State.CANCELLED,"even a relative/zero-distance server position correction invalidates in-place use");
  f=new Fixture();f.readyUse();f.positionEpoch++;f.tick();check(f.task.state()==PestSprayTask.State.CANCELLED,"cleared position receipt epoch cannot be mistaken for no movement");
  f=new Fixture();var rebound=f;f.hookAt=2;f.contextHook=()->{rebound.task.cancel(rebound.now);rebound.session.invalidate();rebound.context=new AutomationContext(1,true,true,Set.of());check(rebound.task.request(PestSprayPlanTest.policy(),rebound.now),"explicit new manual generation can acquire after old cancellation");};check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&f.task.active()&&f.task.state()==PestSprayTask.State.SETTLING&&f.session.active()&&f.session.holdsFarming()&&f.pauses==2&&f.stops==1,"old failed post-acquire read must not cancel the newer accepted root");f.task.cancel(f.now);
  f=new Fixture();f.revokeDemand=true;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&!f.task.active()&&f.disposals==1,"revocation during data demand cannot leave un-tickable ACQUIRING state or leak demand");
  f=new Fixture();f.selected=4;f.readyUse();f.acknowledge();f.decrease(1,true);f.tick();check(f.task.state()==PestSprayTask.State.READY_RESTORE,"original slot already tool still needs settled final verification");f.inv=new HashMap<>(f.inv);f.inv.put(42,PestSprayPlanTest.material(1));f.tick();check(f.task.state()==PestSprayTask.State.FAILED&&f.restores==0,"no-op restore is not a shortcut past full43 final verification");
  f=new Fixture();f.acquireUnarmed=true;check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&!f.task.active()&&!f.session.active()&&!f.session.holdsFarming()&&f.pauses==0&&f.equips+f.uses==0,"must acquire an actual original Farming hold, not rely on a preflight/post-acquire armed flag");
  System.out.println("PestSprayTaskTest: PASS independent bounded in-place selected-material model; no native/server compatibility claim");
 }
}
