package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
public final class ComposterTaskTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final ItemSnapshot TOOL=new ItemSnapshot("HOE","minecraft:diamond_hoe","uuid","Hoe","hoe",1,List.of(),true),MATERIAL=new ItemSnapshot("BOX_OF_SEEDS","minecraft:paper","","Box","box",2,List.of(),false),AIR=new ItemSnapshot("","minecraft:air","","","air",0,List.of(),true);
 static final ComposterPolicy POLICY=new ComposterPolicy(new ComposterPolicy.Anchor(-11,72,-27,"minecraft:player_head"),Map.of("BOX_OF_SEEDS",new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,1000)),Set.of(),90,3,192,500,240,7200,true);
 static ItemSnapshot role(String name,long current,long capacity){return new ItemSnapshot("","minecraft:paper","",name,name+current,1,List.of(current+"/"+capacity),true);}
 static final class Fake implements ComposterTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());FlightAnchor pose=new FlightAnchor(new PestTargets.Point(.5,70,.5),10,20,0,TOOL,false);boolean mayFly=true,manual,cursorEmpty=true,accept=true,blocked;MenuSnapshot menu;int prepares,opens,deposits,restores,releases,disposes,closes,stops,pauses;boolean consumed;List<PestPlot> commands=new ArrayList<>();
  public AutomationContext context(){return ctx;}public PestTargets.Point position(){return pose.position();}
  public ComposterTask.Frame frame(long now){return new ComposterTask.Frame(ctx.worldId(),now,pose,new PestTargets.Point(pose.position().x(),pose.position().y()+1.62,pose.position().z()),mayFly,manual,cursorEmpty,true);}
  public List<ComposterPlan.Stack> inventory(){return consumed?List.of():List.of(new ComposterPlan.Stack(9,MATERIAL));}
  public boolean prepare(AutomationSession.Handle h,FlightAnchor original,FlightAnchor region,long now){prepares++;pose=new FlightAnchor(pose.position(),pose.yaw(),pose.pitch(),original.slot(),original.item(),true);return accept;}
  public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){commands.add(plot);return accept;}
  public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){if(blocked)return FlightMotion.BLOCKED;pose=new FlightAnchor(goal,pose.yaw(),pose.pitch(),pose.slot(),pose.item(),true);return FlightMotion.ARRIVED;}
  public ComposterTask.Open open(AutomationSession.Handle h,ComposterPolicy.Anchor anchor,long now){opens++;return accept?ComposterTask.Open.SENT:ComposterTask.Open.REJECTED;}
  public MenuSnapshot menu(){return menu;}
  public boolean deposit(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now){deposits++;return accept;}
  public void closeOwnedMenu(int id){closes++;menu=null;ctx=new AutomationContext(1,true,true,Set.of());}
  public FlightMotion restore(AutomationSession.Handle h,FlightAnchor original,long now){restores++;pose=original;return FlightMotion.ARRIVED;}
  public void release(AutomationSession.Handle h){releases++;}public void dispose(AutomationSession.Handle h){disposes++;}
  void menu(long organic){menu=new MenuSnapshot(1,7,3,"Composter",54,List.of(new MenuSnapshot.Slot(9,role("Organic Matter",organic,10000)),new MenuSnapshot.Slot(35,role("Fuel",5000,10000)),new MenuSnapshot.Slot(54,consumed?AIR:MATERIAL,true)));ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}
 }
 static final class H {Fake f=new Fake();InventoryReceipts inventory=new InventoryReceipts();CursorReceipts cursor=new CursorReceipts();PlayerPositionReceipts positions=new PlayerPositionReceipts();AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);ComposterTask task=new ComposterTask(session,positions,inventory,cursor,f);int positionPacket;void start(){check(task.request(POLICY,true,0),"explicit start");task.tick(0);}void arrive(long now,int plot){f.pose=new FlightAnchor(new PestTargets.Point(0,72,0),10,20,0,TOOL,true);positions.observe(1,++positionPacket,new PlayerPositionReceipts.Position(0,72,0),true,now);task.tick(now);}long deposit(){start();arrive(S,0);for(int i=2;i<9&&f.opens==0;i++)task.tick(i*S);check(f.opens==1,"one native machine opening");f.menu(1000);for(int i=9;i<15&&f.deposits==0;i++)task.tick(i*S);check(f.deposits==1,"one guarded full material stack");return 14*S;}}
 public static void main(String[]args){
  H h=new H();check(!h.task.request(ComposterPolicy.defaults(),true,0),"no configured consumption authority");h.f.mayFly=false;check(!h.task.request(POLICY,true,0),"legal flight mandatory");
  h=new H();h.start();check(h.f.commands.size()==1&&h.f.commands.getFirst().id()==0&&h.session.holdsFarming(),"one leased typed Barn trip under original Farming hold");h.f.pose=new FlightAnchor(new PestTargets.Point(0,72,0),10,20,0,TOOL,true);h.task.tick(S);check(h.task.state()==ComposterTask.State.TRAVEL_OUT&&h.f.opens==0,"predicted arrival is not authority");h.task.cancel(2*S);check(h.f.stops==1&&h.f.disposes==1&&h.f.restores==0,"manual cancellation never restores old view or re-arms");
  h=new H();h.start();h.arrive(S,0);for(int i=2;i<9&&h.f.opens==0;i++)h.task.tick(i*S);h.f.menu=new MenuSnapshot(1,7,0,"Composter",54,List.of());h.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.task.tick(9*S);check(h.task.state()==ComposterTask.State.WAIT_MENU&&h.f.deposits==0&&h.task.ownsMenu(),"known opening GUI may load slots later; wait without blind click or masking other blockers");h.task.cancel(10*S);
  h=new H();long at=h.deposit();h.f.consumed=true;h.f.menu(3000);h.task.tick(at);check(h.task.inserted()==0,"predicted source and resource changes do not prove deposit");
  h.inventory.observe(1,7,54,AIR);h.inventory.observe(1,7,9,h.f.menu.item(9));h.cursor.observe(1,7,AIR,at+S);h.task.tick(at+S);check(h.task.inserted()==2,"resource delta + source removal + server-applied empty cursor");
  h.task.tick(at+2*S);check(h.f.commands.size()==2&&h.f.commands.getLast().id()==0&&h.f.closes==1&&h.f.restores==0,"insufficient next materials returns PARTIAL instead of purchasing or guessing");
  h.arrive(at+3*S,0);for(int i=4;i<12&&h.task.active();i++)h.task.tick(at+i*S);check(h.task.state()==ComposterTask.State.PARTIAL&&h.f.restores==1&&h.f.stops==0&&h.f.pose.position().equals(new PestTargets.Point(.5,70,.5)),"only exact confirmed original return releases token, partial not full success");
  h=new H();at=h.deposit();h.f.consumed=true;h.f.menu(3000);h.inventory.observe(1,7,54,AIR);h.inventory.observe(1,7,9,h.f.menu.item(9));h.task.tick(at+8*S);check(h.task.state()==ComposterTask.State.TIMED_OUT&&h.f.closes==0&&h.f.restores==0&&h.f.stops==1,"missing cursor acknowledgement never repaired/closed/retried");
  h=new H();at=h.deposit();h.f.cursorEmpty=false;h.task.cancel(at+S);check(h.f.closes==0&&h.f.deposits==1,"manual cursor/GUI ownership remains with user");
  System.out.println("ComposterTaskTest: PASS (explicit source/machine, confirmed Barn arrival, guarded stack, triple server proof, honest partial exact return, missing receipt and manual cursor safety)");
 }
}
