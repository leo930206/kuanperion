package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.nio.file.*;
import java.util.*;
public final class PestServiceTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("pest-service-").resolve("automation.json");var settings=AutomationSettingsStore.load(path);var f=new PestTaskTest.Fake();var session=new AutomationSession(new AutomationCoordinator(),f::context,()->{},()->f.stops++);var notices=new ArrayList<StashService.Notice>();boolean[] gui={true};var service=new PestService(settings,session,f,()->gui[0],notices::add);
  check(!service.autoEnabled()&&!Files.exists(path),"new auto off; constructor no writes/effects");service.tick(0);check(f.prepares==0,"no hidden auto from old notification setting");
  service.setEnabled(true);check(AutomationSettingsStore.load(path).enabled(AutomationFeature.PEST),"external persistence independent of jar");service.cancel(0);check(service.autoEnabled(),"idle lifecycle invalidation must not disable future Pest dispatch when user starts Farming");gui[0]=false;service.tick(PestTaskTest.S);check(!service.active(),"never automatic-start while user editing GUI");gui[0]=true;service.tick(2*PestTaskTest.S);check(service.active()&&f.prepares==1,"enabled threshold dispatches real typed task");
  service.setEnabled(false);check(!service.active()&&!service.autoEnabled()&&f.stops==1,"disable cleans input and revokes hold");
  var p=new PestPolicy(4,90,10,8,32,120,false);service.setPolicy(p);check(AutomationSettingsStore.load(path).pestPolicy().equals(p),"all policy limits persisted");
  Files.writeString(path,"{\"schemaVersion\":1,\"features\":{\"pest\":true},\"pest\":{\"threshold\":0},\"future\":{\"keep\":1}}");var corrupt=AutomationSettingsStore.load(path);var bad=new PestService(corrupt,session,f,()->true,notices::add);check(!bad.autoEnabled()&&!bad.manualRequest(10*PestTaskTest.S),"invalid action limits fail closed, not actionable defaults");
  var path2=path.resolveSibling("second.json");var current=AutomationSettingsStore.load(path2);var manual=new PestService(current,session,f,()->false,notices::add);check(manual.manualRequest(20*PestTaskTest.S),"explicit manual start works with auto off and own GUI open");manual.tick(20*PestTaskTest.S);manual.cancel(21*PestTaskTest.S);check(!manual.active()&&notices.stream().noneMatch(n->n.text().contains("完成")),"cancellation never reports completion");
  System.out.println("PestServiceTest: PASS (independent off defaults, external atomic policy, GUI-safe dispatch, explicit manual request, disable/invalid-settings safety)");
 }
}
