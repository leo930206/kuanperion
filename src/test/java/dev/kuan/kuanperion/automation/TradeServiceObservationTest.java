package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
/** Real service entry points must not bypass the task's unavailable-native-read boundary. */
public final class TradeServiceObservationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class NativePort implements AutoSellTask.Port,JunkTask.Port {
  final boolean junk;String fault="";int contextReads,throwContextAt,effects;MenuSnapshot visible;
  NativePort(boolean junk){this.junk=junk;}
  public AutomationContext context(){if(++contextReads==throwContextAt||fault.equals("context throw"))throw new IllegalStateException("unavailable context");return fault.equals("context null")?null:new AutomationContext(1,true,true,visible==null?Set.of():Set.of(AutomationContext.Blocker.MENU));}
  public JunkPolicy.Location location(){if(fault.equals("location throw"))throw new IllegalStateException("unavailable location");return fault.equals("location null")?null:JunkTaskTest.policy().anchor();}
  public List<ItemSnapshot> inventory(){if(fault.equals("inventory throw"))throw new IllegalStateException("unavailable inventory");if(fault.equals("inventory null"))return null;if(fault.equals("inventory element"))return Arrays.asList(AutoSellTaskTest.wheat(64),null);return Collections.nCopies(36,junk?JunkTaskTest.item(64,false):AutoSellTaskTest.wheat(64));}
  public MenuSnapshot menu(){return visible;}
  public boolean command(AutomationSession.Handle h,AutomationCommand c){effects++;return true;}
  public boolean sell(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,InventoryProtection p,long n){effects++;return true;}
  public boolean open(AutomationSession.Handle h){effects++;visible=JunkTaskTest.menu(JunkTaskTest.item(64,false));return true;}
  public boolean drop(AutomationSession.Handle h,MenuContract c,MenuContract.Ticket t,InventoryProtection p,long n){effects++;return true;}
  public void closeOwnedMenu(int id){effects++;visible=null;}
 }
 public static void main(String[]args)throws Exception{
  if(args.length==0)for(boolean junk:new boolean[]{false,true})for(String fault:List.of("context throw","context null","inventory throw","inventory null","inventory element","location throw","location null")){
   if(!junk&&fault.startsWith("location"))continue;
   var path=Files.createTempDirectory("trade-service-observation-").resolve("automation.json");var store=AutomationSettingsStore.load(path);store.setAutoSellPolicy(TradePreflightObservationTest.sellPolicy());store.setJunkPolicy(JunkTaskTest.policy());var p=new NativePort(junk);int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->hooks[0]++,()->hooks[1]++);var notices=new ArrayList<StashService.Notice>();
   var a=new AutoSellService(store,s,new InventoryReceipts(),p,()->true,notices::add);var j=new JunkService(store,s,new InventoryReceipts(),p,()->true,notices::add);
   if(junk)j.setEnabled(true);else a.setEnabled(true);var before=Files.readAllBytes(path);p.fault=fault;
   if(junk)j.tick(1);else a.tick(1);
   check(!s.active()&&!s.holdsFarming()&&!a.active()&&!j.active()&&p.effects==0&&hooks[0]==0&&hooks[1]==0,"idle failed observation stays inert: "+junk+" / "+fault);
   check(Arrays.equals(before,Files.readAllBytes(path)),"a failed read never changes saved policy");
   p.fault="";if(junk)j.tick(2_000_000_000L);else a.tick(2_000_000_000L);
   check(s.active()&&p.effects==1,"fresh healthy reads retain existing automatic dispatch");
   s.invalidate();if(junk)j.cancel(3_000_000_000L);else a.cancel(3_000_000_000L);
  }
  if(args.length==0||args[0].equals("anchor"))for(String fault:List.of("context throw","context null","location throw","location null")){
   var path=Files.createTempDirectory("junk-anchor-observation-").resolve("automation.json");var store=AutomationSettingsStore.load(path);store.setJunkPolicy(JunkTaskTest.policy());var p=new NativePort(true);p.fault=fault;var s=new AutomationSession(new AutomationCoordinator(),p::context,()->{throw new AssertionError("unexpected hold");},()->{throw new AssertionError("unexpected stop");});var j=new JunkService(store,s,new InventoryReceipts(),p,()->true,n->{});var before=Files.readAllBytes(path);
   check(!j.recordAnchor()&&Arrays.equals(before,Files.readAllBytes(path)),"failed explicit anchor read does not write: "+fault);
   check(j.status().contains("重新記錄"),"unavailable status cannot claim world authorization");
   j.setEnabled(true);check(store.enabled(AutomationFeature.JUNK),"explicit enable still persists the user's preference");j.tick(1);check(!j.active()&&!s.active()&&p.effects==0,"unavailable enable cannot authorize current world");
  }
  // Previously a third read after successful acquisition could throw before returning to the caller.
  var path=Files.createTempDirectory("junk-acquired-observation-").resolve("automation.json");var store=AutomationSettingsStore.load(path);store.setJunkPolicy(JunkTaskTest.policy());var p=new NativePort(true);p.throwContextAt=3;int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),p::context,()->hooks[0]++,()->hooks[1]++);var j=new JunkService(store,s,new InventoryReceipts(),p,()->true,n->{});
  check(j.manualRequest(0)&&j.active()&&s.holdsFarming()&&p.contextReads==2&&hooks[0]==1&&p.effects==0,"successful task hands its validated world to service without another unsafe read");p.throwContextAt=0;s.invalidate();j.cancel(1);
  System.out.println("TradeServiceObservationTest: PASS (idle faults/recovery, byte-identical preferences, read-only status/anchor, enable without world authority, no post-acquire observation)");
 }
}
