package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
public final class AutoSellServiceTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("auto-sell-service-");
  try{
   var store=AutomationSettingsStore.load(root.resolve("automation.json"));
   check(store.autoSellPolicy().allowedIds().isEmpty()&&!store.enabled(AutomationFeature.AUTO_SELL),"default authorization empty and disabled");
   var policy=new AutoSellPolicy(Set.of("WHEAT"),Set.of("protected-uuid"),2,128,1000,60,60,85,true);
   store.setAutoSellPolicy(policy);store.setEnabled(AutomationFeature.AUTO_SELL,true);
   var reopened=AutomationSettingsStore.load(root.resolve("automation.json"));check(reopened.autoSellPolicy().equals(policy)&&!reopened.enabled(AutomationFeature.STASH),"autoSell persistence independent from Stash");
   check(AutoSellPolicy.parseIds("WHEAT, CARROT_ITEM\nPOTATO_ITEM").size()==3,"strict CSV ID parsing");
   try{AutoSellPolicy.parseIds("Wheat Hoe");throw new AssertionError("display name accepted");}catch(IllegalArgumentException expected){}
   var fake=new AutoSellTaskTest.Fake();int[] stops={0};var session=new AutomationSession(new AutomationCoordinator(),fake::context,()->{},()->stops[0]++);var notices=new ArrayList<StashService.Notice>();
   boolean[] autoAllowed={false};var service=new AutoSellService(reopened,session,new InventoryReceipts(),fake,()->autoAllowed[0],notices::add);
   service.tick(0);check(!service.active()&&fake.commands==0,"automatic task cannot replace user's GUI");
   autoAllowed[0]=true;service.tick(1000000000L);check(service.active()&&fake.commands==1,"configured full inventory triggers bounded workflow");
   service.setEnabled(false);check(!service.active()&&stops[0]==1,"disable cancels safe hold");
   check(service.manualRequest(5000000000L),"manual start possible with auto off");service.tick(5000000000L);check(fake.commands==2,"manual start dispatches typed command");
   service.cancel(6000000000L);int count=notices.size();service.tick(7000000000L);check(notices.size()==count,"terminal feedback only once");
   service.setEnabled(true);var pausedBytes=Files.readString(root.resolve("automation.json"));service.suspend(8_000_000_000L);check(!service.autoEnabled()&&!service.active()&&reopened.enabled(AutomationFeature.AUTO_SELL)&&pausedBytes.equals(Files.readString(root.resolve("automation.json"))),"Jerry idle suspension does not overwrite enabled preferences");service.tick(100_000_000_000L);check(fake.commands==2,"suspended idle auto never restarts on due inventory");service.setEnabled(true);check(service.autoEnabled(),"explicit re-enable acknowledges suspension");
   System.out.println("AutoSellServiceTest: PASS (external policy/IDs, independent defaults, GUI/user priority, auto/manual dispatch and cancellation)");
  }finally{try(var files=Files.walk(root)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
