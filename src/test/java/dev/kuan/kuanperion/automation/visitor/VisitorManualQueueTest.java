package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class VisitorManualQueueTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H {
  final VisitorTaskTest.Fake f=new VisitorTaskTest.Fake();boolean closed;final List<StashService.Notice> notices=new ArrayList<>();
  final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);
  final AutomationSettingsStore store;final VisitorService service;
  H(Path path)throws Exception{store=AutomationSettingsStore.load(path);store.setVisitorPolicy(VisitorTaskTest.POLICY);service=new VisitorService(store,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),f,()->closed,notices::add);}
  void untouched(String m){check(!service.active()&&!session.active()&&!session.holdsFarming()&&f.pauses==0&&f.stops==0&&f.prepares==0&&f.commands.isEmpty()&&f.dataRequests==0,m);}
 }
 public static void main(String[]args)throws Exception{var dir=Files.createTempDirectory("visitor-manual-");try{
  var h=new H(dir.resolve("first.json"));check(h.service.manualRequest(0),"configured manual UI intent accepted");h.untouched("manual Visitor must not acquire a root, pause Farm, fetch data, send travel, move, or submit while settings is open");check(!h.service.manualRequest(S),"duplicate intent refused");h.service.tick(S);h.untouched("waiting is inert");
  h.closed=true;h.service.tick(2*S);check(h.service.active()&&h.session.holdsFarming()&&h.f.pauses==1&&!h.service.autoEnabled(),"close dispatches one fresh manual task without enabling automatic mode");h.service.cancel(3*S);int count=h.f.dataRequests;h.service.tick(9999*S);check(!h.service.active()&&h.f.dataRequests==count,"cancelled request never replays");
  for(long delta:new long[]{30*S,31*S,-1}){h=new H(dir.resolve("expiry-"+delta+".json"));check(h.service.manualRequest(S),"queue expiry");h.closed=true;h.service.tick(S+delta);h.untouched("expired/backwards clock cannot dispatch");h.service.tick(100*S);h.untouched("no retained expiry intent");}
  for(int mode=0;mode<4;mode++){h=new H(dir.resolve("cancel-"+mode+".json"));check(h.service.manualRequest(0),"queue cancellation");switch(mode){case 0->h.service.cancel(S);case 1->h.service.suspend(S);case 2->h.service.setEnabled(false);case 3->h.service.setPolicy(h.service.policy());}h.closed=true;h.service.tick(2*S);h.untouched("stop/Jerry/disable/policy change revokes pending authority");}
  for(var blocker:AutomationContext.Blocker.values()){h=new H(dir.resolve("block-"+blocker+".json"));check(h.service.manualRequest(0),"queue before blocker");h.f.ctx=new AutomationContext(1,true,true,Set.of(blocker));h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("manual GUI/focus/death/Mining/recovery blocker clears intent: "+blocker);}
  h=new H(dir.resolve("world.json"));check(h.service.manualRequest(0),"queue original world");h.f.ctx=new AutomationContext(2,true,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("world change never inherits intent");
  h=new H(dir.resolve("farm.json"));check(h.service.manualRequest(0),"queue original Farm");h.f.ctx=new AutomationContext(1,false,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("manual stop cannot turn queued task into standalone work");
  h=new H(dir.resolve("null.json"));check(h.service.manualRequest(0),"queue context");h.f.ctx=null;h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("unavailable context loses authority without replay");
  h=new H(dir.resolve("foreign.json"));var other=h.session.begin(AutomationFeature.AUTO_SELL,0,90*S);check(other!=null&&!h.service.manualRequest(S)&&h.session.isCurrent(other),"cannot queue across another gameplay owner");h.session.invalidate();
  h=new H(dir.resolve("fresh.json"));check(h.service.manualRequest(0),"queue before flight capability changes");h.f.mayFly=false;h.closed=true;h.service.tick(S);h.untouched("dispatch revalidates current flight permission, never replays queued native facts");check(h.notices.size()==1,"failed fresh preflight reports once");h.service.tick(2*S);check(h.notices.size()==1,"no failed dispatch retry or duplicate feedback");
  System.out.println("VisitorManualQueueTest: PASS (real task/original hold, UI deferral, fresh exact-once dispatch, expiry/world/Farm/blocker/stop/policy safety)");
 }finally{try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}}
}
