package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class GreenhouseServiceTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  var file=Files.createTempDirectory("greenhouse-service-").resolve("automation.json");var settings=AutomationSettingsStore.load(file);var f=new GreenhouseTaskTest.Fake();var session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);var notices=new ArrayList<StashService.Notice>();var service=new GreenhouseService(settings,session,new PlayerPositionReceipts(),new HarvestReceipts(),f,()->true,notices::add);
  service.tick(0);check(!service.autoEnabled()&&!service.active()&&f.commands.isEmpty(),"default no actions");try{service.setEnabled(true);throw new AssertionError("enabled unconfigured");}catch(IllegalArgumentException expected){}
  service.setPolicy(GreenhouseTaskTest.POLICY);service.setEnabled(true);service.tick(0);service.tick(599_000_000_000L);check(!service.active(),"no immediate automatic work on launch/enable");service.tick(600_000_000_000L);check(service.active()&&f.commands.size()==1&&session.holdsFarming(),"interval due explicit configured native task only");service.cancel(601_000_000_000L);check(!service.active()&&!service.autoEnabled()&&!settings.enabled(AutomationFeature.GREENHOUSE)&&f.stops==1&&notices.size()==1,"manual cancel disposes original hold and disables only Greenhouse");service.tick(2_000_000_000_000L);check(f.commands.size()==1,"no retries after cancellation");
  service.setEnabled(true);service.tick(3_000_000_000_000L);check(!service.active(),"re-enable initializes a fresh interval, not stale pending job");
  Files.writeString(file,"{\"schemaVersion\":1,\"features\":{\"greenhouse\":true},\"greenhouse\":{\"plots\":[0]}}");settings=AutomationSettingsStore.load(file);service=new GreenhouseService(settings,session,new PlayerPositionReceipts(),new HarvestReceipts(),f,()->true,notices::add);check(!service.autoEnabled()&&!service.manualRequest(4_000_000_000_000L),"malformed external config never starts or silently repairs");
  System.out.println("GreenhouseServiceTest: PASS (off/unconfigured defaults, delayed explicit opt-in, shared hold, cancel disables/no retry, invalid config)");
 }
}
