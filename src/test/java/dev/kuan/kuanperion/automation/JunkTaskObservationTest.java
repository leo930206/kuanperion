package dev.kuan.kuanperion.automation;
import java.util.*;
public final class JunkTaskObservationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class NativePort implements JunkTask.Port {
  String fault="";MenuSnapshot visible;int opens,drops,closes;
  public AutomationContext context(){if(fault.equals("context throw"))throw new IllegalStateException("read failed");return fault.equals("context null")?null:new AutomationContext(1,true,true,visible==null?Set.of():Set.of(AutomationContext.Blocker.MENU));}
  public JunkPolicy.Location location(){if(fault.equals("location throw"))throw new IllegalStateException("read failed");return JunkTaskTest.policy().anchor();}
  public List<ItemSnapshot> inventory(){return List.of(JunkTaskTest.item(64,false));}
  public MenuSnapshot menu(){if(fault.equals("menu throw")||fault.equals("opened menu throw")&&opens>0)throw new IllegalStateException("read failed");return visible;}
  public boolean open(AutomationSession.Handle h){opens++;visible=JunkTaskTest.menu(JunkTaskTest.item(64,false));return true;}
  public boolean drop(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,InventoryProtection p,long n){drops++;return true;}
  public void closeOwnedMenu(int id){closes++;visible=null;}
 }
 public static void main(String[]args){
  for(boolean alreadyOpen:new boolean[]{false,true})for(String fault:List.of("context throw","context null","location throw","menu throw","opened menu throw")){
   var p=new NativePort();int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->hooks[0]++,()->hooks[1]++);
   var t=new JunkTask(s,new InventoryReceipts(),p);check(t.request(JunkTaskTest.policy(),true,0),"started");
   if(alreadyOpen)t.tick(0);p.fault=fault;t.tick(1_000_000_000L);
   check(t.state()==JunkTask.State.FAILED&&!s.active()&&!s.holdsFarming()&&hooks[0]==1&&hooks[1]==1&&p.drops==0,"unavailable native read revokes original root without item disposal: "+alreadyOpen+" / "+fault);
  }
  var p=new NativePort();int[] stops={0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->{},()->{stops[0]++;throw new IllegalStateException("stop failed");});var t=new JunkTask(s,new InventoryReceipts(),p);check(t.request(JunkTaskTest.policy(),true,0),"stop failure fixture");p.fault="location throw";
  try{t.tick(1);throw new AssertionError("safety stop error swallowed");}catch(IllegalStateException expected){check(expected.getMessage().equals("stop failed"),"stop failure remains visible");}
  check(!s.active()&&!s.holdsFarming()&&!t.active()&&stops[0]==1&&p.drops==0,"stop failure follows revocation");
  System.out.println("JunkTaskObservationTest: PASS (context/position/menu faults before and after native open, no disposal/retry, original hold revoked, stop errors visible)");
 }
}
