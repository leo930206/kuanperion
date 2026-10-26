package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
/** Real task/session with fake native effects: editing settings must not acquire a flight root. */
public final class PestManualQueueTest {
 static final long S=1_000_000_000L;
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class Harness {
  final PestTaskTest.Fake f=new PestTaskTest.Fake();boolean closed;int pauses;final List<StashService.Notice> notices=new ArrayList<>();
  final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->pauses++,()->f.stops++);
  final AutomationSettingsStore store;final PestService service;
  Harness(Path path){store=AutomationSettingsStore.load(path);service=new PestService(store,session,f,()->closed,notices::add);}
  void untouched(String why){check(!session.active()&&!session.holdsFarming()&&!service.active()&&pauses==0&&f.prepares==0&&f.uses==0&&f.stops==0,why);}
 }
 public static void main(String[]args)throws Exception{var dir=Files.createTempDirectory("pest-manual-queue-");try{
  var h=new Harness(dir.resolve("first.json"));check(h.service.manualRequest(0),"explicit UI request accepted with automatic Pest off");h.untouched("UI request must not acquire a root, pause Farm, switch tool, fly or capture while settings is open");
  check(!h.service.manualRequest(S),"only one pending request");h.service.tick(S);h.untouched("waiting has no effects");h.closed=true;h.service.tick(2*S);check(h.service.active()&&h.session.holdsFarming()&&h.pauses==1&&h.f.prepares==1&&!h.service.autoEnabled(),"closing settings starts one fresh manual task, never enables automatic mode");h.service.cancel(3*S);int prepares=h.f.prepares;h.service.tick(60*S);check(h.f.prepares==prepares&&!h.service.active(),"cancelled dispatched request never replays");
  for(long elapsed:new long[]{30*S,31*S,-1}){h=new Harness(dir.resolve("expire-"+elapsed+".json"));check(h.service.manualRequest(S),"queue expiry case");h.closed=true;h.service.tick(S+elapsed);h.untouched("expired/backwards-clock request cannot execute");h.service.tick(100*S);h.untouched("expired request not retained");}
  for(int mode=0;mode<4;mode++){h=new Harness(dir.resolve("cancel-"+mode+".json"));check(h.service.manualRequest(0),"queue cancellation case");switch(mode){case 0->h.service.cancel(S);case 1->h.service.suspend(S);case 2->h.service.setEnabled(false);case 3->h.service.setPolicy(new PestPolicy(2,90,10,8,32,120,false,true));}h.closed=true;h.service.tick(2*S);h.untouched("manual/world/Jerry/disable/policy cancellation drops pending authority");}
  h=new Harness(dir.resolve("world.json"));check(h.service.manualRequest(0),"queue on original world");h.f.ctx=new AutomationContext(2,true,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("changed world never inherits queued request");
  for(var blocker:AutomationContext.Blocker.values()){h=new Harness(dir.resolve("block-"+blocker+".json"));check(h.service.manualRequest(0),"queue before blocker");h.f.ctx=new AutomationContext(1,true,true,Set.of(blocker));h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("pending request cannot survive manual GUI/focus/death/recovery/Mining blocker: "+blocker);}
  h=new Harness(dir.resolve("farm-off.json"));check(h.service.manualRequest(0),"queue while original Farm armed");h.f.ctx=new AutomationContext(1,false,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("stopped Farm cannot convert pending request into standalone flight");
  h=new Harness(dir.resolve("unavailable.json"));check(h.service.manualRequest(0),"queue before unavailable context");h.f.ctx=null;h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("lost observation clears pending authority without root/effects/replay");
  h=new Harness(dir.resolve("unknown.json"));h.f.ctx=new AutomationContext(1,true,false,Set.of(AutomationContext.Blocker.DISCONNECTED));check(!h.service.manualRequest(0),"no offline/title queue masquerading as accepted task");h.untouched("unknown context remains inert");
  h=new Harness(dir.resolve("foreign.json"));var foreign=h.session.begin(AutomationFeature.AUTO_SELL,0,90*S);check(foreign!=null&&!h.service.manualRequest(S)&&h.session.isCurrent(foreign),"another current gameplay owner cannot receive a hidden pending Pest request");h.session.invalidate();
  h=new Harness(dir.resolve("fresh.json"));check(h.service.manualRequest(0),"queue with valid loaded target");h.f.targets=List.of();h.closed=true;h.service.tick(S);h.untouched("post-UI dispatch revalidates current target instead of replaying old facts");check(h.notices.size()==1,"failed fresh dispatch reports once");h.service.tick(2*S);check(h.notices.size()==1,"no failed dispatch retry or duplicate feedback");
  System.out.println("PestManualQueueTest: PASS (real task/original hold, UI deferral, exact once, expiry/clock/world, explicit cancellation/policy and fresh dispatch checks)");
 }finally{try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}}
}
