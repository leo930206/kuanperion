package dev.kuan.kuanperion.automation;
import java.util.*;
/** Native reads are observations, not permission to acquire a task or change Farming. */
public final class TradePreflightObservationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class NativePort implements AutoSellTask.Port,JunkTask.Port {
  String fault="";int effects,reads;boolean farm=true;
  public AutomationContext context(){reads++;if(fault.equals("context throw"))throw new IllegalStateException("unavailable context");return fault.equals("context null")?null:new AutomationContext(1,farm,true,Set.of());}
  public JunkPolicy.Location location(){reads++;if(fault.equals("location throw"))throw new IllegalStateException("unavailable position");return fault.equals("location null")?null:JunkTaskTest.policy().anchor();}
  public List<ItemSnapshot> inventory(){reads++;if(fault.equals("inventory throw"))throw new IllegalStateException("unavailable inventory");if(fault.equals("inventory null"))return null;if(fault.equals("inventory element"))return Arrays.asList(AutoSellTaskTest.wheat(64),null);return List.of(AutoSellTaskTest.wheat(64),JunkTaskTest.item(64,false));}
  public MenuSnapshot menu(){return null;}
  public boolean command(AutomationSession.Handle h,AutomationCommand c){effects++;return true;}
  public boolean sell(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,InventoryProtection p,long n){effects++;return true;}
  public boolean open(AutomationSession.Handle h){effects++;return true;}
  public boolean drop(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,InventoryProtection p,long n){effects++;return true;}
  public void closeOwnedMenu(int id){effects++;}
 }
 static AutoSellPolicy sellPolicy(){return new AutoSellPolicy(Set.of("WHEAT"),Set.of(),5,320,1000,60,60,85,true);}
 public static void main(String[]args){
  for(boolean junk:new boolean[]{false,true})for(boolean manual:new boolean[]{false,true})for(String fault:List.of("context throw","context null","inventory throw","inventory null","inventory element","location throw","location null")){
   if(!junk&&fault.startsWith("location"))continue;
   var p=new NativePort();p.fault=fault;int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->hooks[0]++,()->hooks[1]++);
   var a=new AutoSellTask(s,new InventoryReceipts(),p);var j=new JunkTask(s,new InventoryReceipts(),p);
   boolean accepted=junk?j.request(JunkTaskTest.policy(),manual,0):a.request(sellPolicy(),manual,0);
   check(!accepted&&!s.active()&&!s.holdsFarming()&&!a.active()&&!j.active()&&hooks[0]==0&&hooks[1]==0&&p.effects==0,"failed preflight is inert: "+junk+" / "+manual+" / "+fault);
  }
  for(boolean junk:new boolean[]{false,true}){
   var p=new NativePort();int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->hooks[0]++,()->hooks[1]++);
   var a=new AutoSellTask(s,new InventoryReceipts(),p);var j=new JunkTask(s,new InventoryReceipts(),p);
   check(junk?j.request(JunkTaskTest.policy(),true,0):a.request(sellPolicy(),true,0),"healthy explicit preflight starts");
   int reads=p.reads;p.fault="context throw";
   check(!(junk?j.request(JunkTaskTest.policy(),true,1):a.request(sellPolicy(),true,1))&&p.reads==reads&&s.active()&&s.holdsFarming()&&hooks[0]==1&&hooks[1]==0&&p.effects==0,"duplicate request never reads/changes active task");
   p.fault="";s.invalidate();
  }
  // A session callback failure must remain visible, not be mistaken for an inert read failure.
  var p=new NativePort();AutomationSession[] ref=new AutomationSession[1];int[] stops={0};
  ref[0]=new AutomationSession(new AutomationCoordinator(),p::context,()->{throw new IllegalStateException("pause failed");},()->{stops[0]++;throw new IllegalStateException("stop failed");});
  try{new AutoSellTask(ref[0],new InventoryReceipts(),p).request(sellPolicy(),true,0);throw new AssertionError("safety stop exception swallowed");}catch(IllegalStateException expected){check(expected.getMessage().equals("stop failed"),"stop error not replaced");}
  check(!ref[0].active()&&!ref[0].holdsFarming()&&stops[0]==1&&p.effects==0,"stop exception follows ownership revocation");
  System.out.println("TradePreflightObservationTest: PASS (manual/automatic read faults, complete-list validation, duplicate inertness, safety callback errors visible)");
 }
}
