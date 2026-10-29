package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PetSaleCustodyTest {
 static final long S=1_000_000_000L;static final ItemSnapshot AIR=PetSalePreflightTest.AIR;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H {
  AutomationContext context=new AutomationContext(1,true,true,Set.of());java.util.function.Supplier<AutomationContext> read=()->context;final AutomationCoordinator coordinator=new AutomationCoordinator();final AutomationSession session=new AutomationSession(coordinator,()->context,()->{},()->{});final AutomationSession.Handle owner=session.begin(AutomationFeature.PET_SELL,0,120*S);final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();
  final PetSalePreflight.Pet pet=PetSalePreflightTest.pet(9,PetSalePreflightTest.A,PetSalePreflightTest.plain(),false);final PetSalePreflight.Plan plan=PetSalePreflight.select(List.of(pet),PetSalePreflightTest.inventory(List.of(pet)),PetSalePreflightTest.rules()).orElseThrow();final PetSaleCustody custody=new PetSaleCustody(session,coordinator,items,cursors,()->read.get());
  final InventoryReceipts.Stamp opening=items.stamp(1);final CursorReceipts.OpeningStamp cursorOpening=cursors.openingStamp(1,0);Map<Integer,ItemSnapshot> inventory=new HashMap<>(plan.baseline());MenuSnapshot menu;int sends;
  H(){menu(AIR);packets(S);context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}
  void menu(ItemSnapshot input){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<54;i++)slots.add(new MenuSnapshot.Slot(i,i==13?input:AIR));for(int i=0;i<36;i++)slots.add(new MenuSnapshot.Slot(54+i,inventory.get(i<27?i+9:i-27),true));menu=new MenuSnapshot(1,7,1,"Offer Pets",54,slots);}
  PetSaleAttempt.Frame frame(long now){return new PetSaleAttempt.Frame(menu,inventory,true,now);}
  void packets(long now){for(var s:menu.slots())items.observe(1,menu.containerId(),s.index(),s.item());cursors.observe(1,menu.containerId(),AIR,now);}
  boolean start(){return custody.start(owner,plan,pet,13,frame(S),opening,cursorOpening,S,()->{sends++;return GuardedMenuAction.Result.SENT;});}
  void transfer(){inventory.put(9,AIR);menu(pet.item());items.observe(1,0,9,AIR);packets(2*S);custody.poll(frame(2*S),2*S);}
  PetSaleCustody.Token token(){check(start(),"one explicitly authorized deposit");transfer();return custody.token().orElseThrow();}
 }
 static void confirmation(){
  var h=new H();var token=h.token();var attempt=new PetSaleAttempt(h.session,h.coordinator,h.items,h.cursors,()->h.context);var quote=new PetSaleAttempt.Quote(h.pet,100,200);check(attempt.startCustody(h.owner,token,quote,h.frame(3*S),3*S,()->GuardedMenuAction.Result.SENT),"custody sale starts");
  h.menu=GeorgeSaleViewTest.menu("Confirm Sale",Map.of(13,h.pet.item(),11,GeorgeSaleViewTest.button("Confirm","Price: 200 Coins")));h.menu=new MenuSnapshot(1,8,1,h.menu.title(),54,h.menu.slots());h.packets(4*S);int[] sends={0};check(attempt.continueCustody(h.frame(4*S),h.cursorOpening,4*S,()->{sends[0]++;return GuardedMenuAction.Result.SENT;}),"same pending quote can continue one fresh source confirmation");check(!attempt.continueCustody(h.frame(4*S),h.cursorOpening,4*S,()->{throw new AssertionError("third click");})&&sends[0]==1,"confirmation once only");
  h.menu=GeorgeSaleViewTest.menu("Confirm Sale",Map.of());h.menu=new MenuSnapshot(1,8,2,h.menu.title(),54,h.menu.slots());h.packets(5*S);attempt.onServerSale(1,h.pet.metadata().uuid(),200,5*S);attempt.poll(h.frame(5*S),5*S);check(attempt.state()==PetSaleAttempt.State.CONFIRMED,"post-confirm actual NPC removal/cursor/payout");
 }
 public static void main(String[]args){confirmation();
  var h=new H();var token=h.token();check(token.pet().equals(h.pet)&&h.sends==1&&h.session.isCurrent(h.owner),"private token comes only from actual paired source/NPC/cursor evidence");
  var selling=h;var sale=new PetSaleAttempt(h.session,h.coordinator,h.items,h.cursors,()->selling.context);var quote=new PetSaleAttempt.Quote(h.pet,100,200);check(sale.startCustody(h.owner,token,quote,h.frame(3*S),3*S,()->GuardedMenuAction.Result.SENT),"sale accepts already-proven server custody, not stale original inventory plan");
  var other=new PetSaleAttempt(h.session,h.coordinator,h.items,h.cursors,()->selling.context);check(!other.startCustody(h.owner,token,quote,h.frame(3*S),3*S,()->{throw new AssertionError("token replay");}),"custody consumed once across sale attempts");
  h.menu(AIR);h.packets(4*S);sale.onServerSale(1,h.pet.metadata().uuid(),200,4*S);sale.poll(h.frame(4*S),4*S);check(sale.state()==PetSaleAttempt.State.CONFIRMED,"post-sale NPC removal + unchanged deposited inventory + scoped payout/cursor");
  h=new H();check(h.start(),"predicted fixture");h.inventory.put(9,AIR);h.menu(h.pet.item());h.custody.poll(h.frame(2*S),2*S);check(h.custody.token().isEmpty(),"local source/NPC prediction cannot mint custody");
  h=new H();check(h.start(),"wrong custody fixture");h.inventory.put(9,AIR);h.menu(PetSalePreflightTest.item(PetSalePreflightTest.B,false));h.items.observe(1,0,9,AIR);h.packets(2*S);h.custody.poll(h.frame(2*S),2*S);check(h.custody.token().isEmpty(),"same kind/name but other UUID not the authorized pet");
  h=new H();check(h.start(),"cursor fixture");h.transfer();token=h.custody.token().orElseThrow();h.session.cancel(3*S);var old=h;var stopped=new PetSaleAttempt(h.session,h.coordinator,h.items,h.cursors,()->old.context);check(!stopped.startCustody(h.owner,token,quote,h.frame(3*S),3*S,()->{throw new AssertionError("stopped root");}),"manual stop never revives held pet sale");
  var cancelled=new H();check(!cancelled.custody.start(cancelled.owner,cancelled.plan,cancelled.pet,13,cancelled.frame(S),cancelled.opening,cancelled.cursorOpening,S,()->{cancelled.read=()->{cancelled.custody.cancel();return cancelled.context;};return GuardedMenuAction.Result.SENT;})&&cancelled.custody.state()==PetSaleCustody.State.UNCERTAIN,"post-send context cancellation cannot be overwritten back to AWAITING");
  var next=new H();next.menu=new MenuSnapshot(1,7,1,"George",54,next.menu.slots());next.packets(S);check(next.start(),"source George transfer");next.inventory.put(9,AIR);next.menu(next.pet.item());next.menu=new MenuSnapshot(1,8,1,"Offer Pets",54,next.menu.slots());next.items.observe(1,0,9,AIR);next.packets(2*S);next.custody.poll(next.frame(2*S),2*S);check(next.custody.token().isPresent(),"source transfer may open a fresh Offer Pets container; new applied alias/NPC/cursor still mandatory");
  System.out.println("PetSaleCustodyTest: PASS");
 }
}
