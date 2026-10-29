package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
public final class ComposterSacksFlowTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H implements ComposterSacksFlow.Port {
  final AutomationCoordinator coordinator=new AutomationCoordinator();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();
  AutomationContext context=new AutomationContext(1,true,true,Set.of());final AutomationSession session=new AutomationSession(coordinator,()->context,()->{},()->{});final AutomationSession.Handle parent=session.begin(AutomationFeature.COMPOSTER,0,120*S);
  final Object originalLevel=new Object();Object level=originalLevel,identity=new Object();MenuSnapshot menu=ComposterSacksViewTest.main(7,1000);Map<Integer,ItemSnapshot> inventory=new HashMap<>();int clicks;long now=S;boolean available=true,empty=true;GuardedMenuAction.Result send=GuardedMenuAction.Result.SENT;ComposterSacksFlow flow;
  H(){for(int i=0;i<43;i++)inventory.put(i,ComposterSacksViewTest.AIR);var is=items.stamp(1);var cs=cursors.openingStamp(1,0);packets();flow=new ComposterSacksFlow(coordinator,session,parent,items,cursors,is,cs,this,()->now,ComposterObservation.Kind.ORGANIC,10000);context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}
  public boolean available(){return available;}public Object level(){return level;}public Object menuIdentity(){return identity;}public MenuSnapshot menu(){return menu;}public Map<Integer,ItemSnapshot> inventory(){return inventory;}public boolean cursorEmpty(){return empty;}public AutomationContext context(){return context;}
  public GuardedMenuAction.Result click(MenuContract c,MenuContract.Ticket t,Supplier<MenuSnapshot> proof,long at){return GuardedMenuAction.perform(coordinator,parent.lease(),c,t,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,()->context,proof,request->{clicks++;return send==GuardedMenuAction.Result.SENT;},at);}
  void packets(){if(menu!=null){for(var s:menu.slots())items.observe(1,menu.containerId(),s.index(),s.item());cursors.observe(1,menu.containerId(),ComposterSacksViewTest.AIR,now);}}
  void detail(){now=2*S;menu=ComposterSacksViewTest.detail(8);identity=new Object();packets();}
  void result(){now=3*S;menu=ComposterSacksViewTest.main(9,7000);identity=new Object();packets();}
  void begin(){check(flow.begin(now)&&clicks==1&&session.isCurrent(parent),"source main navigation in original parent");}
 }
 public static void main(String[]args){
  var h=new H();h.begin();check(h.flow.ownsScreen(),"old exact main during navigation owns no new authority");h.detail();h.session.poll(h.now);h.flow.tick(h.now);check(h.clicks==2&&h.flow.state()==ComposterSacksFlow.State.WAIT_RETURN,"fresh exact detail executes one Sacks action");h.result();h.session.poll(h.now);h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.COMPLETE&&h.flow.increase()==6000&&h.clicks==2&&h.session.isCurrent(h.parent),"new independent main resource receipt, same parent, no close or Farm release");h.flow.tick(4*S);check(h.clicks==2,"no replay");
  h=new H();h.items.clear();check(!h.flow.begin(h.now)&&h.clicks==0,"predicted main not enough");
  h=new H();h.cursors.clear();check(!h.flow.begin(h.now)&&h.clicks==0,"physical cursor alone not enough");
  h=new H();h.begin();h.detail();h.items.clear();h.flow.tick(h.now);check(h.clicks==1,"predicted detail not enough");h.now=12*S;h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.FAILED,"no consumption sent and no blind retries");
  h=new H();h.begin();h.detail();h.flow.tick(h.now);h.result();h.cursors.clear();h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.WAIT_RETURN,"missing cursor cannot confirm");h.now=13*S;h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.UNCERTAIN&&h.clicks==2,"unknown consumption never retried");
  h=new H();h.begin();h.detail();h.flow.tick(h.now);h.result();h.inventory.put(42,ComposterSacksViewTest.item("Changed equipment",List.of()));h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.UNCERTAIN,"all43 including equipment frozen");
  h=new H();h.begin();h.detail();h.session.cancel(h.now);h.flow.tick(h.now);check(h.clicks==1&&h.flow.state()==ComposterSacksFlow.State.FAILED,"manual/root cancellation before any consumption");
  h=new H();h.begin();h.detail();h.flow.tick(h.now);h.result();h.level=new Object();h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.UNCERTAIN,"world identity cannot complete");
  h=new H();h.begin();h.detail();h.flow.tick(h.now);h.result();h.menu=ComposterSacksViewTest.main(9,1000);h.packets();h.flow.tick(h.now);check(h.flow.state()==ComposterSacksFlow.State.WAIT_RETURN,"no gain not success");
  System.out.println("ComposterSacksFlowTest: PASS");
 }
}
