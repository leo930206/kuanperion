package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.*;
public final class VisitorServiceTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{Path dir=Files.createTempDirectory("visitor-service-");try{
  Path path=dir.resolve("automation.json");var settings=AutomationSettingsStore.load(path);check(settings.visitorPolicyValid()&&!settings.visitorPolicy().configured()&&!settings.enabled(AutomationFeature.VISITOR),"absent settings grant no Visitor authority");
  var p=VisitorTaskTest.POLICY;settings.setVisitorPolicy(p);settings.setEnabled(AutomationFeature.VISITOR,true);check(AutomationSettingsStore.load(path).visitorPolicy().equals(p),"external policy survives new store/jar");
  var f=new VisitorTaskTest.Fake();var session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);var allowed=new AtomicBoolean(false);var notices=new ArrayList<StashService.Notice>();var s=new VisitorService(settings,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),f,allowed::get,notices::add);
  s.tick(0);check(!s.active()&&f.dataRequests==0,"automatic UI/focus gate before demand data");allowed.set(true);s.tick(VisitorTaskTest.S);check(s.active()&&f.dataRequests==1&&session.holdsFarming(),"queue threshold launches genuine inventory-only task");s.suspend(2*VisitorTaskTest.S);check(!s.active()&&!s.autoEnabled()&&f.stops==1&&!notices.isEmpty(),"Jerry suspends active standalone/original Farming hold");
  s.setEnabled(true);byte[] before=Files.readAllBytes(path);s.suspend(3*VisitorTaskTest.S);s.tick(200*VisitorTaskTest.S);check(!s.active()&&!s.autoEnabled()&&Arrays.equals(before,Files.readAllBytes(path)),"idle Jerry suspension does not write prefs or run later");s.setEnabled(true);check(s.autoEnabled(),"explicit user re-enable only");
  for(String bad:List.of("{\"threshold\":0}","{\"timeoutSeconds\":\"240\"}","{\"maximumOfferCoins\":1.5}","{\"visitors\":[\"Librarian\",\"Librarian\"]}","{\"materialIds\":[\"WHEAT\",\"WHEAT\"]}","{\"onlyWhileFarming\":1}")){
   Files.writeString(path,"{\"schemaVersion\":1,\"features\":{\"visitor\":true},\"visitor\":"+bad+"}");var invalid=AutomationSettingsStore.load(path);check(!invalid.visitorPolicyValid()&&!invalid.visitorPolicy().configured(),"malformed consent cannot fall back to executable: "+bad);
  }
  Files.writeString(path,"{\"schemaVersion\":1,\"future\":{\"keep\":7},\"visitor\":{\"newField\":true}}");var preserve=AutomationSettingsStore.load(path);preserve.setVisitorPolicy(p);String saved=Files.readString(path);check(saved.contains("newField")&&saved.contains("future"),"unknown root/section fields preserved");
  System.out.println("VisitorServiceTest: PASS (off/unconfigured, external exact policy, strict numbers/duplicates, threshold demand-only launch, active/idle Jerry suspension and explicit re-enable)");
 }finally{try(var files=Files.walk(dir)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}}
}
