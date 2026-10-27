package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.FlightAnchor;import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
/** Real controller/task/session; explicit simulated server observations, no live menu-format claim. */
public final class BookMenuControllerTest {
 static final long S=1_000_000_000L;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static ItemSnapshot empty(){return new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);}
 static BookCombinePlanner.Book book(int raw,int level,String uuid){return new BookCombinePlanner.Book(raw,new ItemSnapshot("ENCHANTED_BOOK","minecraft:enchanted_book",uuid,"Enchanted Book","book-"+level+uuid,1,List.of(),false),Map.of("harvesting",level),"same-data");}
 static final BookAnvilView.Layout L=new BookAnvilView.Layout("Anvil",54,20,24,13,22,22,Set.of("minecraft:air"));
 static final BookCombineTask.Policy P=new BookCombineTask.Policy(Map.of("harvesting",5),Set.of(),500,30,true);
 static final class Game implements BookMenuController.Game {
  final Object level=new Object();Object currentLevel=level,containerIdentity=new Object();boolean visible,ready=true,emptyCursor=true,manual;int clicks,closes,id=4,revision=1,lastDeposit=-1;long x;boolean throwClick;ItemSnapshot carried=empty();
  int inventoryReads,mutateAt=Integer.MAX_VALUE;Runnable mutation;Map<Integer,BookCombinePlanner.Book> inv=new HashMap<>(),placed=new HashMap<>();Map<Integer,ItemSnapshot> extras=new HashMap<>(),icons=new HashMap<>();
  Game(){for(int i=0;i<9;i++)extras.put(i,new ItemSnapshot("TEST_TOOL","minecraft:stone","","Tool","tool-"+i,1,List.of(),true));inv.put(9,book(9,4,"one"));inv.put(10,book(10,4,"two"));icons.put(22,new ItemSnapshot("","minecraft:anvil","","Combine Items","free",1,List.of("Cost: 0 Exp Levels","Click to combine!"),false));}
  public boolean available(){return ready;}public Object level(){return currentLevel;}public Object menuIdentity(){return containerIdentity;}public boolean idle(){return !visible;}public boolean safePlayer(){return ready&&!manual;}public boolean cursorEmpty(){return emptyCursor&&carried.count()==0;}public ItemSnapshot cursor(){return carried;}
  public FlightAnchor pose(){return new FlightAnchor(new Point(x,70,0),0,0,0,empty(),false);}
  public BookInventorySnapshot inventory(){if(++inventoryReads==mutateAt&&mutation!=null)mutation.run();var items=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)items.put(i,extras.getOrDefault(i,empty()));inv.forEach((i,b)->items.put(i,b.item()));return BookInventorySnapshot.inspect(items,inv).orElseThrow();}
  public MenuSnapshot menu(long world){if(!visible)return null;var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++){int raw=i<54?-1:i<81?i-54+9:i-81;var b=i<54?placed.get(i):inv.get(raw);slots.add(new MenuSnapshot.Slot(i,b!=null?b.item():i<54?icons.getOrDefault(i,empty()):extras.getOrDefault(raw,empty()),i>=54));}return new MenuSnapshot(world,id,revision,"Anvil",54,slots);}
  public BookAnvilView anvil(long world,BookAnvilView.Layout layout){var m=menu(world);if(m==null)return null;var books=new HashMap<>(placed);inv.forEach((raw,b)->books.put(raw<9?81+raw:54+raw-9,b));return new BookAnvilView(layout,m,books);}
  public boolean click(MenuContract.Ticket t){clicks++;if(throwClick)throw new IllegalStateException("unknown native outcome");return true;}public boolean deposit(MenuContract.Ticket t,ItemSnapshot own){check(t.click()==MenuContract.Click.PICKUP&&t.slot()>=54&&t.slot()<81&&t.observed().item(t.slot()).count()==0&&own.equals(carried),"explicit empty main-slot deposit of exact own carried book");lastDeposit=t.slot();return click(t);}public void close(){closes++;visible=false;}
 }
 static final class Fixture {
  final Game g=new Game();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursor=new CursorReceipts();final long[] now={0};final int[] hooks={0,0};
  final AutomationCoordinator coordinator=new AutomationCoordinator();final AutomationSession session=new AutomationSession(coordinator,this::context,()->hooks[0]++,()->hooks[1]++);
  final BookAnvilView.Layout layout;final BookMenuController driver;final BookCombineTask task;
  Fixture(){this(L);}Fixture(BookAnvilView.Layout layout){this.layout=layout;driver=new BookMenuController(coordinator,session,this::context,items,cursor,layout,P,(h,t)->{g.visible=true;return true;},g,()->now[0]);task=new BookCombineTask(session,items,cursor,driver,layout);}
  AutomationContext context(){return new AutomationContext(1,true,true,g.visible?Set.of(AutomationContext.Blocker.MENU):Set.of());}
  void tick(long t){now[0]=t;task.tick(t);}
  void start(){check(task.request(P,true,0),"authorized pair request");tick(0);check(g.visible,"typed opening callback");}
  // Packet ItemSnapshot has intrinsic protection, not the additional hotbar-slot protection of a native menu projection.
  ItemSnapshot packet(ItemSnapshot i,boolean hotbar){return hotbar&&i.count()>0?new ItemSnapshot(i.skyblockId(),i.minecraftId(),i.uuid(),i.name(),i.fingerprint(),i.count(),i.lore(),false):i;}
  void server(long t){now[0]=t;var m=g.menu(1);for(var slot:m.slots())items.observe(1,g.id,slot.index(),packet(slot.item(),slot.index()>=81));for(int raw=0;raw<36;raw++)items.observe(1,0,raw,packet(g.inventory().inventory().get(raw),raw<9));cursor.observe(1,g.id,g.cursor(),t);g.revision++;}
  void input(int raw,int target,long t){var b=g.inv.remove(raw);g.placed.put(target,new BookCombinePlanner.Book(-1,b.item(),b.enchantments(),b.compatibility()));server(t);}
  void first(){start();server(S);tick(S);check(g.clicks==1,"first input sent once");input(9,layout.firstInput(),2*S);tick(2*S);}
  void both(){first();tick(3*S);check(g.clicks==2,"second exact input");input(10,layout.secondInput(),4*S);g.placed.put(layout.preview(),book(-1,5,"preview"));server(4*S);tick(4*S);}
 }
 public static void main(String[]args){
  if(args.length>0){regression(args[0]);return;}
  var f=new Fixture();f.both();f.tick(5*S);check(f.g.clicks==3,"free combine sent once");f.g.placed.clear();f.g.placed.put(22,book(-1,5,"result"));f.server(6*S);f.tick(6*S);f.tick(7*S);check(f.g.clicks==4,"proved result collected once");f.g.placed.clear();f.g.inv.put(9,book(9,5,"result"));f.server(8*S);f.tick(8*S);check(f.task.state()==BookCombineTask.State.COMPLETE&&f.g.closes==1&&f.hooks[1]==0,"real controller/task completes only after server result and owned close");
  f=new Fixture();f.start();f.tick(S);check(f.g.clicks==0,"physical empty/menu alone is not opening authority");f.server(2*S);f.tick(2*S);check(f.g.clicks==1,"complete opening server proof permits input");
  f=new Fixture();f.start();for(var slot:f.g.menu(1).slots())if(slot.index()!=5)f.items.observe(1,4,slot.index(),slot.item());f.cursor.observe(1,4,empty(),S);f.tick(S);check(f.g.clicks==0,"missing even empty external slot cannot borrow partial opening proof");
  f=new Fixture();f.start();f.server(S);f.g.x=1;f.tick(S);check(f.g.clicks==0&&f.g.closes==0,"pose drift cannot move books or close");
  f=new Fixture();f.start();f.server(S);f.g.extras.put(40,new ItemSnapshot("TEST","minecraft:stone","","changed","changed",1,List.of(),true));f.tick(S);check(f.g.clicks==0,"full43 protected drift denied");
  f=new Fixture();f.start();f.server(S);f.g.currentLevel=new Object();f.tick(S);check(f.g.clicks==0,"same world counter with changed native level denied");
  f=new Fixture();f.start();f.server(S);f.g.manual=true;f.tick(S);check(f.g.clicks==0,"physical manual input denies native action");
  f=new Fixture();f.start();f.server(S);f.g.icons.put(5,new ItemSnapshot("","minecraft:paper","","Fee","fee",1,List.of("Cost: 10 Coins"),false));f.server(S);f.tick(S);check(f.g.clicks==0,"unknown/priced external role refuses all input");
  f=new Fixture();f.both();f.g.icons.put(22,new ItemSnapshot("","minecraft:anvil","","Combine Items","paid",1,List.of("Cost: 1 Exp Levels","Click to combine!"),false));f.server(5*S);f.tick(5*S);check(f.g.clicks==2,"paid combine is never sent");
  f=new Fixture();f.start();f.server(S);f.g.throwClick=true;f.tick(S);f.tick(2*S);check(f.g.clicks==1&&!f.task.active()&&f.g.closes==0,"possibly-sent native exception never retries or closes");
  f=new Fixture();f.first();f.task.cancel(2*S);f.tick(3*S);check(f.g.clicks==2,"confirmed original reclaimed under same root");f.g.placed.clear();f.g.inv.put(9,book(9,4,"one"));f.server(4*S);f.tick(4*S);check(f.task.state()==BookCombineTask.State.CANCELLED&&f.g.closes==1,"server-confirmed recovery closes only owned menu");
  f=new Fixture();f.first();f.session.cancel(2*S);f.tick(3*S);check(f.g.clicks==1&&f.g.closes==0,"manual root takeover cannot reclaim or close");
  f=new Fixture();f.start();f.server(S);f.tick(S);var predicted=f.g.inv.remove(9);f.g.placed.put(20,new BookCombinePlanner.Book(-1,predicted.item(),predicted.enchantments(),predicted.compatibility()));f.tick(2*S);check(f.task.active()&&f.g.clicks==1,"bound predicted input remains observable but cannot advance without server receipt");f.server(3*S);f.tick(3*S);f.tick(4*S);check(f.g.clicks==2,"settled server input continues once, without replay");
  f=new Fixture();f.start();f.server(S);var changed=f;f.g.mutateAt=f.g.inventoryReads+3;f.g.mutation=()->changed.g.extras.put(40,new ItemSnapshot("TEST","minecraft:stone","","late","late",1,List.of(),true));f.tick(S);check(f.g.clicks==0,"full43 drift precisely at final sender refuses effect");
  f=new Fixture();f.start();f.server(S);var takeover=f;f.g.mutateAt=f.g.inventoryReads+3;f.g.mutation=()->takeover.session.invalidate();f.tick(S);check(f.g.clicks==0,"root invalidation during final inventory observation cannot send under stale owner");
  f=new Fixture();f.first();f.g.containerIdentity=new Object();f.server(3*S);f.tick(3*S);check(f.g.clicks==1&&f.g.closes==0,"replaced native menu with identical container ID is not the owned menu");
  confirmedLayout();
  regression("controlled-main");regression("controlled-deposit");regression("cancel-carried");regression("uncertain-deposit");regression("late-deposit-drift");regression("two-reclaims");regression("two-reclaims-stale-preview");regression("occupied-second");
  System.out.println("BookMenuControllerTest: PASS (real task/controller/session, simulated server opening/input/combine/result/recovery; native/current-live validation remains separate)");
 }
 static void confirmedLayout(){
  var f=new Fixture(BookAnvilView.confirmedLayout());f.g.extras.remove(0);f.both();check(f.g.placed.containsKey(29)&&f.g.placed.containsKey(33),"current screenshot inputs29/33, not fixture20/24");f.tick(5*S);check(f.g.clicks==3,"current layout exact free preview accepted");
  f.g.placed.clear();f.g.placed.put(13,book(-1,5,"result"));f.now[0]=6*S;for(var slot:f.g.menu(1).slots())if(slot.index()!=13)f.items.observe(1,4,slot.index(),f.packet(slot.item(),slot.index()>=81));f.cursor.observe(1,4,empty(),6*S);f.tick(6*S);check(f.g.clicks==3&&f.task.state()==BookCombineTask.State.WAIT_COMBINE,"known preview slot alone is not post-combine result authority");
  f.server(7*S);f.tick(7*S);f.tick(8*S);check(f.g.clicks==4&&f.task.state()==BookCombineTask.State.WAIT_RESULT,"fresh exact result in observed preview role picked up only after both input-consumption receipts");
  f.g.placed.clear();f.g.carried=book(-1,5,"result").item();f.server(9*S);f.tick(9*S);check(f.g.lastDeposit==54&&f.g.clicks==5,"result13 uses controlled raw9 main deposit, never protected hotbar");f.g.carried=empty();f.g.inv.put(9,book(9,5,"result"));f.server(10*S);f.tick(10*S);check(f.task.state()==BookCombineTask.State.COMPLETE&&f.g.closes==1&&!f.g.extras.containsKey(0),"current-layout composed success requires receipts and keeps empty hotbar");
  f=new Fixture(BookAnvilView.confirmedLayout());f.both();f.tick(5*S);f.g.placed.clear();f.g.placed.put(13,book(-1,5,"a"));f.g.placed.put(22,book(-1,5,"b"));f.server(6*S);f.tick(6*S);f.tick(7*S);check(f.g.clicks==3,"ambiguous result roles never choose one arbitrarily");
 }
 static void regression(String name){var f=new Fixture();if(name.equals("controlled-main")||name.equals("controlled-deposit")||name.equals("cancel-carried")||name.equals("uncertain-deposit")||name.equals("late-deposit-drift")){
   if(name.equals("controlled-main"))f.g.extras.remove(0);f.both();f.tick(5*S);f.g.placed.clear();f.g.placed.put(22,book(-1,5,"result"));f.server(6*S);f.tick(6*S);f.tick(7*S);check(f.g.clicks==4,"result picked up once");
   f.g.placed.clear();f.g.carried=book(-1,5,"result").item();f.server(8*S);
   if(name.equals("cancel-carried")){f.task.cancel(8*S);f.tick(9*S);check(f.g.clicks==4&&f.g.closes==0&&f.g.carried.count()==1,"manual cancellation leaves own carried item untouched, no deposit/repair/close");return;}
   if(name.equals("uncertain-deposit")){f.g.throwClick=true;f.tick(8*S);f.tick(9*S);check(f.g.clicks==5&&!f.task.active()&&f.g.closes==0&&f.g.carried.count()==1,"possibly-sent deposit failure never replays, repairs or closes");return;}
   if(name.equals("late-deposit-drift")){var target=f;f.g.mutateAt=f.g.inventoryReads+3;f.g.mutation=()->target.g.extras.put(40,new ItemSnapshot("TEST","minecraft:stone","","late","late",1,List.of(),true));f.tick(8*S);check(f.g.clicks==4&&!f.task.active()&&f.g.closes==0&&f.g.carried.count()==1,"full43 drift precisely at carried deposit leaves item untouched for manual handling");return;}
   f.tick(8*S);check(f.g.clicks==5&&f.g.lastDeposit==54,"server-owned carried book goes only to explicit raw9 empty main slot, never hotbar");
   f.g.carried=empty();f.g.inv.put(9,book(9,5,"result"));f.tick(8*S+1);check(f.task.active()&&f.g.closes==0,"predicted destination/empty cursor cannot complete transfer");f.server(9*S);f.tick(9*S);check(f.task.state()==BookCombineTask.State.COMPLETE&&f.g.closes==1,"both destination aliases and fresh server empty cursor complete controlled return");
   if(name.equals("controlled-main"))check(!f.g.extras.containsKey(0),"previously empty protected hotbar remains empty");
  }
  else if(name.equals("two-reclaims")||name.equals("two-reclaims-stale-preview")){
   f.both();f.task.cancel(4*S);f.tick(5*S);check(f.g.clicks==3,"first original picked up before combine");f.g.placed.remove(20);if(name.equals("two-reclaims"))f.g.placed.remove(13);f.g.carried=book(-1,4,"one").item();f.server(6*S);f.tick(6*S);check(f.g.lastDeposit==54,"first original returns only to its original raw9 slot");f.g.inv.put(9,book(9,4,"one"));f.g.carried=empty();f.server(7*S);f.tick(7*S);
   f.tick(8*S);check(f.g.clicks==5,"second original pickup follows proof of first return");f.g.placed.remove(24);f.g.carried=book(-1,4,"two").item();f.server(9*S);f.tick(9*S);check(f.g.lastDeposit==55,"second original returns only to original raw10 slot");f.g.inv.put(10,book(10,4,"two"));f.g.carried=empty();f.server(10*S);f.tick(10*S);if(name.equals("two-reclaims"))check(f.g.clicks==6&&f.task.state()==BookCombineTask.State.CANCELLED&&f.g.closes==1,"both original books server-proven and preview cleared before owned recovery close");else check(f.g.clicks==6&&f.task.state()==BookCombineTask.State.FAILED&&f.g.closes==0,"stale candidate preview forbids close even after both originals were returned");
  }
  else if(name.equals("occupied-second")){f.first();f.g.placed.put(24,book(-1,4,"foreign"));f.server(3*S);f.tick(3*S);check(f.g.clicks==1,"never shift a second book into an already occupied destination role");}
  else throw new IllegalArgumentException(name);
 }
}
