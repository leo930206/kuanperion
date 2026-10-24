package dev.kuan.kuanperion.automation;
import java.util.*;
public final class JunkTaskTest {
 static final long S=1000000000L;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot item(int n,boolean protect){return new ItemSnapshot("SEEDS","minecraft:wheat_seeds","","Seeds","seed-meta",n,List.of(),protect);}
 static ItemSnapshot empty(){return new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);}
 static MenuSnapshot menu(ItemSnapshot value){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<46;i++)slots.add(new MenuSnapshot.Slot(i,i==9?value:empty(),i>=9));return new MenuSnapshot(1,0,1,"Inventory",9,slots);}
 static final class Fake implements JunkTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());JunkPolicy.Location location=new JunkPolicy.Location("overworld",10,70,10);MenuSnapshot visible;int opens,drops,closes;boolean accepted=true,fullInventory;
  public AutomationContext context(){return ctx;}public JunkPolicy.Location location(){return location;}public MenuSnapshot menu(){return visible;}
  public List<ItemSnapshot> inventory(){var items=new ArrayList<ItemSnapshot>(Collections.nCopies(36,fullInventory?item(64,false):empty()));items.set(9,item(64,false));return items;}
  public boolean open(AutomationSession.Handle handle){opens++;visible=JunkTaskTest.menu(item(64,false));ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));return accepted;}
  public boolean drop(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now){check(ticket.click()==MenuContract.Click.THROW&&rules.permits(ticket.observed().item(ticket.slot())),"authorized stack THROW only");drops++;return accepted;}
  public void closeOwnedMenu(int id){if(visible!=null&&visible.containerId()==id){closes++;visible=null;ctx=new AutomationContext(1,true,true,Set.of());}}
 }
 static JunkPolicy policy(){return new JunkPolicy(Set.of("SEEDS"),Set.of(),3,192,1000,60,60,85,true,new JunkPolicy.Location("overworld",10,70,10),1.5);}
 public static void main(String[]args){
  var f=new Fake();var c=new AutomationCoordinator();int[] hooks={0,0};var session=new AutomationSession(c,f::context,()->hooks[0]++,()->hooks[1]++);var receipts=new InventoryReceipts();var task=new JunkTask(session,receipts,f);
  check(!task.request(JunkPolicy.defaults(),true,0)&&f.opens==0&&hooks[0]==0,"no IDs/no anchor never opens or holds");
  f.location=new JunkPolicy.Location("overworld",12,70,10);check(!task.request(policy(),true,0),"outside configured drop radius refused");
  f.location=new JunkPolicy.Location("other",10,70,10);check(!task.request(policy(),true,0),"wrong dimension refused");
  f.location=new JunkPolicy.Location("overworld",10,70,10);f.ctx=new AutomationContext(1,true,false,Set.of());check(!task.request(policy(),true,0),"only Garden");f.ctx=new AutomationContext(1,true,true,Set.of());
  check(task.request(policy(),true,0)&&hooks[0]==1,"explicit authorization starts hold");task.tick(0);check(f.opens==1&&f.drops==0,"open own inventory before any drop");session.poll(S);task.tick(S);check(f.drops==1,"one guarded stack drop");task.tick(2*S);check(f.drops==1&&task.active(),"no local prediction as receipt/retry");
  receipts.observe(1,0,10,empty());task.tick(3*S);check(f.drops==1&&task.removedItems()==0,"wrong slot not receipt");
  receipts.observe(1,0,9,empty());f.visible=menu(empty());task.tick(4*S);task.tick(5*S);check(task.state()==JunkTask.State.COMPLETE&&task.removedItems()==64&&f.closes==1&&!session.holdsFarming()&&hooks[1]==0,"server decrease then close and resume original hold");
  check(task.request(policy(),true,10*S),"start again");task.tick(10*S);f.location=new JunkPolicy.Location("overworld",10,72,10);task.tick(11*S);check(!task.active()&&f.drops==1&&hooks[1]==1,"moving outside anchor cancels before drop");
  f.location=policy().anchor();f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,20*S),"manual priority fixture");task.tick(20*S);session.cancel(21*S);int closes=f.closes;task.tick(22*S);check(!task.active()&&f.closes==closes,"user owns GUI after cancellation; don't close it");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,30*S),"timeout fixture");task.tick(30*S);task.tick(90*S);check(task.state()==JunkTask.State.TIMED_OUT,"bounded wait");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,100*S),"protected-item fixture");task.tick(100*S);f.visible=menu(item(64,true));task.tick(101*S);check(task.state()==JunkTask.State.COMPLETE&&f.drops==1,"protection beats whitelist");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,110*S),"unknown GUI fixture");task.tick(110*S);f.visible=new MenuSnapshot(1,4,1,"Inventory",9,menu(item(64,false)).slots());task.tick(111*S);check(!task.active()&&f.drops==1,"same title but external container rejected");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,120*S),"offhand protection fixture");task.tick(120*S);var offhand=new ArrayList<MenuSnapshot.Slot>(menu(empty()).slots());offhand.set(45,new MenuSnapshot.Slot(45,item(64,false),true));f.visible=new MenuSnapshot(1,0,1,"Inventory",9,offhand);task.tick(121*S);check(task.state()==JunkTask.State.COMPLETE&&f.drops==1,"offhand excluded even if metadata protection absent");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;check(task.request(policy(),true,130*S),"focus fixture");f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));int opens=f.opens;task.tick(130*S);check(f.opens==opens,"background must not open inventory");f.ctx=new AutomationContext(1,true,true,Set.of());task.cancel(131*S);
  f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.INVENTORY_FULL));f.visible=null;check(task.request(policy(),true,140*S),"disposal permitted for full inventory");task.tick(140*S);f.accepted=false;task.tick(141*S);check(task.state()==JunkTask.State.FAILED&&task.removedItems()==0,"rejected native drop never counted");f.accepted=true;
  f.ctx=new AutomationContext(1,true,true,Set.of());f.visible=null;var bounded=new JunkPolicy(Set.of("SEEDS"),Set.of(),1,64,1000,60,60,85,true,policy().anchor(),1.5);check(task.request(bounded,true,150*S),"bounded fixture");task.tick(150*S);int drops=f.drops;task.tick(151*S);receipts.observe(1,0,9,empty());f.visible=menu(item(64,false));task.tick(152*S);task.tick(153*S);check(task.state()==JunkTask.State.COMPLETE&&f.drops==drops+1&&task.removedItems()==64,"never exceed exact stack/item cap even if more candidates remain");
  for(double d:new double[]{Double.NaN,Double.POSITIVE_INFINITY,31_000_000}){try{new JunkPolicy.Location("overworld",d,70,0);throw new AssertionError("bad coordinate accepted");}catch(IllegalArgumentException expected){}}
  System.out.println("JunkTaskTest: PASS (exact IDs/anchor/Garden, protected THROW, server-only receipt, bounded attempts, manual ownership, dimension/movement/GUI guards)");
 }
}
