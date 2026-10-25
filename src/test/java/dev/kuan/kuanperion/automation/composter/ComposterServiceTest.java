package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class ComposterServiceTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("composter-service-").resolve("automation.json");var settings=AutomationSettingsStore.load(path);var f=new ComposterTaskTest.Fake();var session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);var notices=new ArrayList<StashService.Notice>();var service=new ComposterService(settings,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),f,()->true,Optional::empty,notices::add);
  check(!service.autoEnabled()&&!service.captureAnchor()&&!service.manualRequest(0),"no guessed default anchor or material");try{service.setEnabled(true);throw new AssertionError("enabled unconfigured");}catch(IllegalArgumentException expected){}
  service.setPolicy(ComposterTaskTest.POLICY);service.setEnabled(true);check(settings.enabled(AutomationFeature.COMPOSTER)&&!settings.enabled(AutomationFeature.GREENHOUSE),"enable writes only the Composter persisted feature bit");service.tick(0);service.tick(7199_000_000_000L);check(!service.active()&&f.commands.isEmpty(),"no immediate startup/deferred task replay");service.tick(7200_000_000_000L);check(service.active()&&session.holdsFarming()&&f.commands.size()==1,"explicit interval due holds original Farm only");settings.setEnabled(AutomationFeature.GREENHOUSE,true);service.cancel(7201_000_000_000L);check(!service.active()&&!service.autoEnabled()&&!settings.enabled(AutomationFeature.COMPOSTER)&&f.stops==1&&notices.size()==1&&settings.enabled(AutomationFeature.GREENHOUSE),"cancel releases native keys/token and disables own automation");service.tick(99999_000_000_000L);check(f.commands.size()==1,"no retry after failure/cancel");
  System.out.println("ComposterServiceTest: PASS (off/unconfigured/native capture refusal, explicit deferred schedule, original hold, cancel disables/no retry)");
 }
}
