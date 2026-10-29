// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
import java.util.function.Supplier;

/** Actual proof/session/receipt models; declared menus/notices, never a live George compatibility claim. */
public final class PetSaleAttemptTest {
 static final long S=1_000_000_000L;
 static final ItemSnapshot AIR=PetSalePreflightTest.AIR;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static final class H {
  AutomationContext context=new AutomationContext(1,true,true,Set.of());int pauses,stops,sends;Supplier<AutomationContext> read=()->context;
  final AutomationCoordinator coordinator=new AutomationCoordinator();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();
  final AutomationSession session=new AutomationSession(coordinator,()->read.get(),()->pauses++,()->stops++);
  final PetSalePreflight.Pet pet=PetSalePreflightTest.pet(9,PetSalePreflightTest.A,PetSalePreflightTest.plain(),false);
  final PetSalePreflight.Pet other=PetSalePreflightTest.pet(10,PetSalePreflightTest.B,PetSalePreflightTest.plain(),false);
  final Map<Integer,ItemSnapshot> baseline=PetSalePreflightTest.inventory(List.of(pet,other));
  final PetSalePreflight.Plan plan=PetSalePreflight.select(List.of(pet,other),baseline,PetSalePreflightTest.rules()).orElseThrow();
  AutomationSession.Handle owner=session.begin(AutomationFeature.PET_SELL,0,60*S);
  final PetSaleAttempt attempt=new PetSaleAttempt(session,coordinator,items,cursors,()->read.get());
  PetSaleAttempt.Frame frame(Map<Integer,ItemSnapshot> inv,long now){return frame(inv,now,AIR);}
  PetSaleAttempt.Frame frame(Map<Integer,ItemSnapshot> inv,long now,ItemSnapshot displayed){
   var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<27;i++)slots.add(new MenuSnapshot.Slot(i,i==13?displayed:AIR));
   for(int i=0;i<36;i++)slots.add(new MenuSnapshot.Slot(27+i,inv.get(i<27?i+9:i-27),true));
   return new PetSaleAttempt.Frame(new MenuSnapshot(1,7,1,"Declared sale fixture",27,slots),inv,true,now);
  }
  PetSaleAttempt.Quote quote(){return new PetSaleAttempt.Quote(pet,100,200);}
  boolean start(Supplier<GuardedMenuAction.Result> send){return attempt.start(owner,plan,quote(),frame(baseline,0),0,()->{sends++;return send.get();});}
  boolean start(){return start(()->GuardedMenuAction.Result.SENT);}
  Map<Integer,ItemSnapshot> sold(){var inv=new HashMap<>(baseline);inv.put(9,AIR);return inv;}
  void receipt(boolean vanilla,boolean alias,boolean cursor){if(vanilla)items.observe(1,0,9,AIR);if(alias)items.observe(1,7,27,AIR);if(cursor)cursors.observe(1,7,AIR,S);}
  void notice(){attempt.onServerSale(1,pet.metadata().uuid(),200,S);}
  void confirmed(){receipt(true,true,true);notice();attempt.poll(frame(sold(),S),S);}
 }
 static void success(){var h=new H();check(h.start(),"declared guarded send accepted");h.confirmed();check(h.attempt.state()==PetSaleAttempt.State.CONFIRMED,"exact one pet loss, declared payout, both server aliases and empty cursor");check(h.session.isCurrent(h.owner)&&h.session.holdsFarming()&&h.stops==0&&h.sends==1,"proof cannot finish parent/restart Farming/close menus/retry");check(!h.start()&&h.sends==1,"one irreversible attempt per proof instance");}
 static void missing(){for(String part:List.of("vanilla","alias","cursor","notice","physical-cursor","menu-pet")){
  var h=new H();check(h.start(),"missing fixture");h.receipt(!part.equals("vanilla"),!part.equals("alias"),!part.equals("cursor"));if(!part.equals("notice"))h.notice();
  var f=h.frame(h.sold(),S,part.equals("menu-pet")?h.pet.item():AIR);if(part.equals("physical-cursor"))f=new PetSaleAttempt.Frame(f.menu(),f.inventory(),false,S);
  h.attempt.poll(f,S);check(h.attempt.state()!=PetSaleAttempt.State.CONFIRMED,"missing "+part+" cannot prove sale");
 }}
 static void unrelated(){for(int slot:new int[]{0,10,11,36,42}){var h=new H();check(h.start(),"unrelated fixture");h.receipt(true,true,true);h.notice();var inv=h.sold();inv.put(slot,slot==10?AIR:h.pet.item());h.attempt.poll(h.frame(inv,S),S);check(h.attempt.state()==PetSaleAttempt.State.UNCERTAIN,"other pet/protected/hotbar/equipment/unrelated changes never count as this one sale");}}
 static void notices(){for(String bad:List.of("uuid","world","price","before","future")){var h=new H();check(h.start(),"notice fixture");h.receipt(true,true,true);h.attempt.onServerSale(bad.equals("world")?2:1,bad.equals("uuid")?h.other.metadata().uuid():h.pet.metadata().uuid(),bad.equals("price")?201:200,bad.equals("before")?-1:bad.equals("future")?20*S:S);h.attempt.poll(h.frame(h.sold(),S),S);check(h.attempt.state()!=PetSaleAttempt.State.CONFIRMED,"unmatched or stale "+bad+" notice is not payout proof");}}
 static void initial(){
  var first=new H();var changed=new HashMap<>(first.baseline);changed.put(10,AIR);check(!first.attempt.start(first.owner,first.plan,first.quote(),first.frame(changed,0),0,()->{first.sends++;return GuardedMenuAction.Result.SENT;})&&first.sends==0,"full immutable preflight must still match before send");
  var h=new H();h.session.invalidate();h.owner=h.session.begin(AutomationFeature.BOOK_COMBINE,0,60*S);check(!h.start()&&h.sends==0,"only a Pet owner, not another Trade root");
  h=new H();var wrong=new PetSaleAttempt.Quote(PetSalePreflightTest.pet(11,"33333333333333333333333333333333",PetSalePreflightTest.plain(),false),100,200);check(!h.attempt.start(h.owner,h.plan,wrong,h.frame(h.baseline,0),0,()->GuardedMenuAction.Result.SENT),"pet outside private selected plan cannot be substituted");
  for(long[] money:new long[][]{{0,200},{201,200},{100,0},{1,Long.MAX_VALUE}}){boolean refused=false;try{new PetSaleAttempt.Quote(h.pet,money[0],money[1]);}catch(IllegalArgumentException expected){refused=true;}check(refused,"explicit nonzero minimum/exact bounded quote");}
 }
 static void outcomes(){for(var result:GuardedMenuAction.Result.values()){var h=new H();boolean sent=h.start(()->result);check(sent==(result==GuardedMenuAction.Result.SENT),"typed native outcome");if(result!=GuardedMenuAction.Result.SENT){check(h.attempt.state()==(result==GuardedMenuAction.Result.REJECTED?PetSaleAttempt.State.REJECTED:PetSaleAttempt.State.UNCERTAIN),"do not relabel uncertain send");h.confirmed();check(h.attempt.state()!=PetSaleAttempt.State.CONFIRMED,"later receipt cannot rescue rejected/ambiguous attempt");}}
  var h=new H();check(!h.start(()->{throw new IllegalStateException("native uncertain");})&&h.attempt.state()==PetSaleAttempt.State.UNCERTAIN,"throwing native result is not safe retry");
 }
 static void beforeSend(){var h=new H();check(h.start(()->{h.receipt(true,true,true);h.notice();return GuardedMenuAction.Result.SENT;}),"pre-send fixture");h.attempt.poll(h.frame(h.sold(),S),S);check(h.attempt.state()==PetSaleAttempt.State.AWAITING,"reentrant/pre-send messages and receipts cannot prove delivery");h.confirmed();check(h.attempt.state()==PetSaleAttempt.State.CONFIRMED,"new post-send evidence can confirm");}
 static void root(){for(String fault:List.of("null","throw","world","focus","expired","backwards","new-owner")){var h=new H();check(h.start(),"root fixture");h.receipt(true,true,true);h.notice();long now=S;
  switch(fault){case "null"->h.read=()->null;case "throw"->h.read=()->{throw new IllegalStateException("unavailable");};case "world"->h.context=new AutomationContext(2,true,true,Set.of());case "focus"->h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));case "expired"->now=15*S;case "backwards"->now=-1;case "new-owner"->{h.session.invalidate();h.owner=h.session.begin(AutomationFeature.PEST,0,60*S);}}
  h.attempt.poll(h.frame(h.sold(),now),now);check(h.attempt.state()==PetSaleAttempt.State.UNCERTAIN&&h.stops==0,"lost "+fault+" proof cannot release/stop/re-arm parent");if(fault.equals("new-owner"))check(h.session.isCurrent(h.owner),"never touch replacement owner");
 }}
 static void reentrant(){for(boolean duringSend:new boolean[]{false,true}){var h=new H();AutomationSession.Handle[] newer={null};Runnable replace=()->{h.session.invalidate();newer[0]=h.session.begin(AutomationFeature.PEST,0,60*S);};
  if(duringSend){check(!h.start(()->{replace.run();return GuardedMenuAction.Result.SENT;}),"old native return cannot authorize a replacement root");}
  else{check(h.start(),"read reentrancy fixture");h.receipt(true,true,true);h.notice();h.read=()->{h.read=()->h.context;replace.run();return h.context;};h.attempt.poll(h.frame(h.sold(),S),S);}
  check(h.attempt.state()==PetSaleAttempt.State.UNCERTAIN&&h.session.isCurrent(newer[0])&&h.stops==0,"old outcome cannot mutate new owner");
 }}
 static void frames(){
  for(String route:List.of("old","future","partial","container","alias","wrong-receipt")){
   var h=new H();check(h.start(),"frame fixture fresh");h.receipt(true,true,true);h.notice();var inv=h.sold();if(route.equals("partial"))inv.remove(42);
   var f=h.frame(inv,S);if(route.equals("old"))f=new PetSaleAttempt.Frame(f.menu(),f.inventory(),true,-1);if(route.equals("future"))f=new PetSaleAttempt.Frame(f.menu(),f.inventory(),true,2*S);
   if(route.equals("container"))f=new PetSaleAttempt.Frame(new MenuSnapshot(1,8,1,f.menu().title(),27,f.menu().slots()),f.inventory(),true,S);
   if(route.equals("alias")){var slots=new ArrayList<>(f.menu().slots());slots.set(27,new MenuSnapshot.Slot(27,h.pet.item(),true));f=new PetSaleAttempt.Frame(new MenuSnapshot(1,7,1,f.menu().title(),27,slots),f.inventory(),true,S);}
   if(route.equals("wrong-receipt"))h.items.observe(1,0,9,new ItemSnapshot("","minecraft:air","","","different components",0,List.of(),true));
   h.attempt.poll(f,S);check(h.attempt.state()!=PetSaleAttempt.State.CONFIRMED,"bad "+route+" frame/receipt cannot prove payout");
  }
  var h=new H();check(h.start(),"revision fixture");h.receipt(true,true,true);h.notice();var f=h.frame(h.sold(),S);f=new PetSaleAttempt.Frame(new MenuSnapshot(1,7,2,f.menu().title(),27,f.menu().slots()),f.inventory(),true,S);h.attempt.poll(f,S);check(h.attempt.state()==PetSaleAttempt.State.CONFIRMED,"legitimate server revision does not require stale pre-send state ID");
 }
 static void passiveIdentity(){
  var coordinator=new AutomationCoordinator();var context=new AutomationContext(1,false,true,Set.of());check(!coordinator.holdsIdentity(null),"empty passive query");
  var lease=coordinator.tryAcquire(AutomationFeature.PET_SELL,context,0,S);check(coordinator.holdsIdentity(lease),"exact active identity");
  var changed=new AutomationCoordinator.Lease(lease.token(),AutomationFeature.PEST,lease.worldId(),lease.started(),lease.lifetime(),lease.resources());check(!coordinator.holdsIdentity(changed),"same token alone cannot substitute changed owner metadata");
  check(!coordinator.isOwner(lease,context,S)&&!coordinator.holdsIdentity(lease),"validated expiry remains in existing isOwner, not the passive API");
  var next=coordinator.tryAcquire(AutomationFeature.STASH,context,S,S);check(!coordinator.holdsIdentity(lease)&&coordinator.holdsIdentity(next)&&coordinator.isOwner(next,context,S),"old identity query never invalidates current lease");
 }
 static void leaseRevocation(String route){
  var h=new H();AutomationCoordinator.Lease[] newer={null};var foreignContext=new AutomationContext(2,false,false,Set.of());
  Runnable replace=()->{h.coordinator.clear();newer[0]=h.coordinator.tryAcquire(AutomationFeature.STASH,foreignContext,0,60*S);check(newer[0]!=null,"independent replacement acquired");};
  if(route.equals("lease-start")){replace.run();check(!h.start()&&h.sends==0,"a stale session handle cannot send without the original coordinator lease");}
  else if(route.equals("lease-read")){h.read=()->{replace.run();return h.context;};check(!h.start()&&h.sends==0,"lease identity must be rechecked after fallible/reentrant context read");}
  else{check(h.start(),"revoked delivery fixture");h.receipt(true,true,true);h.notice();replace.run();h.attempt.poll(h.frame(h.sold(),S),S);check(h.attempt.state()==PetSaleAttempt.State.UNCERTAIN,"coordinator revocation cannot confirm under a stale session handle");}
  check(h.coordinator.isOwner(newer[0],foreignContext,S),"an old-world proof must not validate/invalidate the replacement lease");
 }
 static void readReentrancy(String route){
  var h=new H();
  if(route.equals("start-cancel")){h.read=()->{h.attempt.cancel();return h.context;};check(!h.start()&&h.sends==0&&h.attempt.state()==PetSaleAttempt.State.REJECTED,"a cancelled preflight cannot invoke a native send afterward");}
  else if(route.equals("start-nested")){h.read=()->{h.read=()->h.context;check(h.start(),"new explicit nested attempt");return h.context;};check(!h.start()&&h.sends==1&&h.attempt.state()==PetSaleAttempt.State.AWAITING,"old preflight cannot overwrite/re-send accepted nested attempt");}
  else{check(h.start(),"cancel-during-poll fixture");h.receipt(true,true,true);h.notice();h.read=()->{h.attempt.cancel();return h.context;};h.attempt.poll(h.frame(h.sold(),S),S);check(h.attempt.state()==PetSaleAttempt.State.UNCERTAIN,"a cancelled proof cannot become confirmed after a late read returns");}
 }
 public static void main(String[] args){if(args.length>0){if(args[0].startsWith("lease-"))leaseRevocation(args[0]);else readReentrancy(args[0]);return;}success();missing();unrelated();notices();initial();outcomes();beforeSend();root();reentrant();frames();passiveIdentity();for(String r:List.of("lease-start","lease-read","lease-poll"))leaseRevocation(r);for(String r:List.of("start-cancel","start-nested","poll-cancel"))readReentrancy(r);check(AutomationFeature.PET_SELL.implemented(),"native George task/service/Trade wiring now exists; this model alone is not native/live evidence");System.out.println("PetSaleAttemptTest: PASS (exact selected pet/quote, original root, unchanged43 slots, paired fresh applied receipts/cursor/declared sale notice; not native George compatibility)");}
}
