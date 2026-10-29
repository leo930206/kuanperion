package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
/** Real navigation/controller/terminal/ledgers with controlled native edge, not a live server. */
public final class BazaarAcquisitionControllerTest {
 static final ItemSnapshot AIR=BazaarPurchaseViewTest.inventory().get(0);
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static ItemSnapshot item(String id,String mc,String name,int count,List<String> lore){return new ItemSnapshot(id,mc,"",name,name+String.join("|",lore),count,lore,false);}
 static final class H implements BazaarAcquisitionController.Game {
  final AutomationCoordinator coordinator=new AutomationCoordinator();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();
  final SpendingBudget budget=new SpendingBudget(1000);final BazaarAcquisitionPlan.Plan initial=BazaarPurchaseViewTest.plan(budget,7);
  boolean farm=true,available=true,safe=true,empty=true,sign,foreign;long now=1;int opens,nav,bought,typed,closed,committedIdentityReads;boolean replaceBeforeClose;Object level=new Object(),identity=new Object(),screen;MenuSnapshot menu;Map<Integer,ItemSnapshot> inventory=BazaarPurchaseViewTest.inventory();Runnable callback=()->{};
  AutomationContext context(){return new AutomationContext(1,farm,true,screen==null?Set.of():Set.of(AutomationContext.Blocker.MENU));}
  final AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->farm=false);final AutomationSession.Handle parent;
  final BazaarAcquisitionController flow;
  H(){this(AutomationFeature.VISITOR);}
  H(AutomationFeature owner){parent=session.begin(owner,0,120_000_000_000L);flow=new BazaarAcquisitionController(coordinator,session,this::context,items,cursors,parent,initial,budget,this,()->now);}
  public boolean available(){return available;}public boolean safe(){return safe&&!foreign;}public boolean cursorEmpty(){return empty;}public Object level(){return level;}public Object menuIdentity(){if(replaceBeforeClose&&budget.spent()>0&&++committedIdentityReads==2)identity=new Object();return identity;}public Object screenIdentity(){return screen;}public boolean sign(){return sign;}
  public Map<Integer,ItemSnapshot> inventory(){return Map.copyOf(inventory);}public MenuSnapshot menu(long world){return menu;}public long purse(){return 1000;}
  public boolean open(AutomationSession.Handle h,BazaarAcquisitionPlan.Request r,long now){opens++;return session.isCurrent(h)&&r==initial.requests().getFirst();}
  public boolean amount(Object expected,int quantity){check(screen==expected&&sign&&quantity==7,"owned exact sign quantity");typed++;screen=null;sign=false;return true;}
  public boolean close(Object expected,int id){if(replaceBeforeClose&&committedIdentityReads==1)identity=new Object();if(identity!=expected||menu==null||menu.containerId()!=id)return false;closed++;menu=null;screen=null;identity=new Object();return true;}
  public GuardedMenuAction.Result navigate(AutomationCoordinator c,AutomationCoordinator.Lease l,MenuContract contract,MenuContract.Ticket t,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){return GuardedMenuAction.perform(c,l,contract,t,GuardedMenuAction.Space.REQUIRE_FREE_SLOT,observe,menus,x->{nav++;callback.run();return true;},now);}
  public GuardedMenuAction.Result purchase(AutomationCoordinator c,AutomationCoordinator.Lease l,MenuContract contract,MenuContract.Ticket t,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){return GuardedMenuAction.perform(c,l,contract,t,GuardedMenuAction.Space.REQUIRE_FREE_SLOT,observe,menus,x->{bought++;callback.run();return true;},now);}
  void show(String title,int id,Map<Integer,ItemSnapshot> buttons){
   var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<63;i++)slots.add(new MenuSnapshot.Slot(i,i<27?buttons.getOrDefault(i,AIR):inventory.get(i<54?i-27+9:i-54),i>=27));
   menu=new MenuSnapshot(1,id,1,title,27,slots);identity=new Object();screen=new Object();sign=false;for(var slot:slots)items.observe(1,id,slot.index(),slot.item());cursors.observe(1,id,AIR,++now);
  }
  void tick(){session.poll(++now);flow.tick(now);}
  void toSign(){check(flow.begin(now),"begin under original root");check(opens==1,"one catalog-derived opener");show("Bazaar Search",2,Map.of(11,item("WHEAT","minecraft:wheat","Wheat",1,List.of())));tick();check(nav==1,"exact search click");show("Wheat",3,Map.of(10,item("","minecraft:gold_nugget","Buy Instantly",1,List.of())));tick();check(nav==2,"exact instant click");show("Wheat → Instant Buy",4,Map.of(16,item("","minecraft:oak_sign","Custom Amount",1,List.of())));tick();check(nav==3,"one nonspending sign opener");sign=true;screen=new Object();tick();check(typed==1,"one native sign submission");}
  void toPurchase(){toSign();show("Confirm Instant Buy",5,Map.of(13,item("","minecraft:gold_nugget","Custom Amount",1,List.of("Wheat","Amount: 7x","Price: 14 coins","Click to buy now!"))));tick();check(bought==1&&budget.available()==986,"one final native purchase reserves exact shared run budget");}
  void deliver(){var wheat=item("WHEAT","minecraft:wheat","Wheat",7,List.of());inventory.put(9,wheat);var slots=new ArrayList<>(menu.slots());slots.set(27,new MenuSnapshot.Slot(27,wheat,true));menu=new MenuSnapshot(1,menu.containerId(),2,menu.title(),27,slots);items.observe(1,0,9,wheat);items.observe(1,menu.containerId(),27,wheat);cursors.observe(1,menu.containerId(),AIR,++now);flow.onSystemMessage(1,"[Bazaar] Bought 7x Wheat for 14 coins!");}
 }
 public static void main(String[]args){
  var text=new String[]{"64"};var editor=new net.minecraft.client.gui.font.TextFieldHelper(()->text[0],v->text[0]=v,()->"",v->{},v->v.length()<16);
  editor.keyPressed(AutomationMenuActions.bazaarSelectAllKey());editor.keyPressed(new net.minecraft.client.input.KeyEvent(261,0,0));editor.charTyped(new net.minecraft.client.input.CharacterEvent('7'));check(text[0].equals("7"),"actual pinned26.2 native text editor replaces old amount on macOS/other platforms; no clipboard");

  var composter=new H(AutomationFeature.COMPOSTER);composter.toPurchase();composter.deliver();composter.tick();check(composter.flow.state()==BazaarAcquisitionController.State.COMPLETE&&composter.budget.spent()==14&&composter.session.isCurrent(composter.parent),"actual native-edge purchase route also preserves the original Composter root");
  H h=new H();h.toPurchase();h.deliver();h.tick();check(h.flow.state()==BazaarAcquisitionController.State.COMPLETE&&h.budget.spent()==14&&h.closed==1&&h.session.isCurrent(h.parent)&&h.session.holdsFarming()&&h.farm,"full custom amount navigation -> real terminal -> dual receipts -> owned close; same parent remains");
  h=new H();h.toPurchase();h.show("Confirm Purchase",6,Map.of(13,item("","minecraft:gold_nugget","Confirm",1,List.of("Wheat","Amount: 7x","Price: 14 coins"))));h.tick();check(h.bought==2&&h.budget.available()==986&&h.session.isCurrent(h.parent),"session binding preserves exact optional confirmation under original parent, no second reservation");h.deliver();h.tick();check(h.flow.state()==BazaarAcquisitionController.State.COMPLETE&&h.closed==1&&h.budget.spent()==14,"complete navigation+confirmation+dual receipt+frozen final-menu close");
  h=new H();h.toPurchase();h.replaceBeforeClose=true;h.deliver();h.tick();check(h.closed==0&&h.flow.state()==BazaarAcquisitionController.State.UNCERTAIN,"completion may close only originally frozen native menu, never a replacement during final reads");
  h=new H();h.toSign();h.foreign=true;h.tick();check(h.typed==1&&h.bought==0&&!h.flow.active(),"foreign GUI never inherits purchase");
  h=new H();h.toPurchase();h.flow.cancel();h.tick();check(h.bought==1&&h.budget.available()==986&&h.closed==0,"cancel never retries, refunds possible spend, repairs cursor or closes menu");
  h=new H();h.toSign();h.now=11_000_000_001L;h.tick();check(!h.flow.active()&&h.bought==0,"bounded navigation timeout");
  h=new H();h.toSign();h.inventory.put(40,item("STONE","minecraft:stone","Stone",1,List.of()));h.tick();check(!h.flow.active()&&h.bought==0,"full43 drift before purchase cancels");
  h=new H();check(h.flow.begin(h.now),"start");h.screen=new Object();h.foreign=true;h.tick();check(h.nav==0&&!h.flow.active(),"unknown overlay cannot be clicked");
  h=new H();h.toPurchase();h.level=new Object();h.deliver();h.tick();check(h.flow.state()==BazaarAcquisitionController.State.UNCERTAIN&&h.budget.spent()==0,"new native level cannot confirm old buy");
  System.out.println("BazaarAcquisitionControllerTest: PASS (actual complete custom navigation plus terminal and native receipts; controlled native game)");
 }
}
