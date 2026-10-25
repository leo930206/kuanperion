package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
public final class StashSuspendTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  var path=Files.createTempDirectory("stash-jerry-suspend-").resolve("automation.json");var settings=AutomationSettingsStore.load(path);var coordinator=new AutomationCoordinator();int[] sent={0};var notices=new ArrayList<StashService.Notice>();var context=new AutomationContext(1,false,true,Set.of());var service=new StashService(settings,coordinator,()->context,c->{sent[0]++;return true;},notices::add);
  service.setPolicy(new StashTask.Policy(3,10,60,false));service.setEnabled(true);var bytes=Files.readString(path);service.suspend("Jerry 停止動作");check(!service.autoEnabled()&&settings.enabled(AutomationFeature.STASH)&&Files.readString(path).equals(bytes),"idle suspension affects runtime only, not saved latest preference");
  service.onSystemMessage("Your items were stashed away!",0);service.tick(4_000_000_000L);check(!service.active()&&sent[0]==0&&notices.isEmpty(),"later stash signals cannot silently restart idle suspended auto");
  check(service.manualRequest(5_000_000_000L),"explicit manual request remains available");service.tick(8_000_000_000L);check(service.active()&&sent[0]==1&&!service.autoEnabled(),"manual work does not re-arm auto");service.suspend("Jerry 停止動作");check(!service.active(),"separate command-only Stash lease is cancelled even without Farming session");
  service.tick(30_000_000_000L);check(sent[0]==1,"no delayed command after active suspension");service.setEnabled(true);check(service.autoEnabled(),"explicit re-enable acknowledges suspension");service.onSystemMessage("Your items were stashed away!",31_000_000_000L);service.tick(34_000_000_000L);check(sent[0]==2,"explicitly re-enabled stash can run again");service.suspend("Jerry 停止動作");
  System.out.println("StashSuspendTest: PASS (idle and active separate lease, no Farming dependency, persisted flag retained, late signals/timers blocked, explicit manual/re-enable only)");
 }
}
