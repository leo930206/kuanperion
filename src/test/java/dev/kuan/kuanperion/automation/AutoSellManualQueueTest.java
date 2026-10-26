package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
public final class AutoSellManualQueueTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H {
  final AutoSellTaskTest.Fake f=new AutoSellTaskTest.Fake();boolean closed;int pauses,stops;final List<StashService.Notice> notices=new ArrayList<>();
  final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->pauses++,()->stops++);
  final AutomationSettingsStore store;final AutoSellService service;
  H(Path path)throws Exception{store=AutomationSettingsStore.load(path);store.setAutoSellPolicy(new AutoSellPolicy(Set.of("WHEAT"),Set.of(),2,128,1000,60,60,85,true));service=new AutoSellService(store,session,new InventoryReceipts(),f,()->closed,notices::add);}
  void untouched(String m){check(!service.active()&&!session.active()&&!session.holdsFarming()&&pauses==0&&stops==0&&f.commands==0&&f.sales==0&&f.closed==0,m);}
 }
 public static void main(String[]args)throws Exception{var dir=Files.createTempDirectory("autosell-manual-");try{
  var h=new H(dir.resolve("first.json"));check(h.service.manualRequest(0),"configured manual UI intent accepted");h.untouched("manual AutoSell must not acquire a root, pause Farm, send a command, open inventory or dispose while settings is open");check(!h.service.manualRequest(S),"duplicate refused");h.service.tick(S);h.untouched("waiting inert");
  h.closed=true;h.service.tick(2*S);check(h.service.active()&&h.session.holdsFarming()&&h.pauses==1&&!h.service.autoEnabled(),"close dispatches one fresh task without automatic enable");h.service.cancel(3*S);int count=h.f.commands;h.service.tick(9999*S);check(!h.service.active()&&h.f.commands==count,"cancelled request never replays");
  for(long delta:new long[]{30*S,31*S,-1}){h=new H(dir.resolve("expiry-"+delta+".json"));check(h.service.manualRequest(S),"queue expiry");h.closed=true;h.service.tick(S+delta);h.untouched("expired/backwards clock");h.service.tick(100*S);h.untouched("expired request not retained");}
  for(int mode=0;mode<4;mode++){h=new H(dir.resolve("cancel-"+mode+".json"));check(h.service.manualRequest(0),"queue cancellation");switch(mode){case 0->h.service.cancel(S);case 1->h.service.suspend(S);case 2->h.service.setEnabled(false);case 3->h.service.setPolicy(h.service.policy());}h.closed=true;h.service.tick(2*S);h.untouched("stop/Jerry/disable/policy change revokes pending intent");}
  for(var blocker:AutomationContext.Blocker.values()){if(blocker==AutomationContext.Blocker.INVENTORY_FULL)continue;h=new H(dir.resolve("block-"+blocker+".json"));check(h.service.manualRequest(0),"queue blocker");h.f.ctx=new AutomationContext(1,true,true,Set.of(blocker));h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("foreign GUI/focus/death/Mining/recovery cancels intent: "+blocker);}
  h=new H(dir.resolve("full.json"));h.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.INVENTORY_FULL));check(h.service.manualRequest(0),"disposal intentionally permits full inventory only");h.untouched("full inventory still no UI effects");h.closed=true;h.service.tick(S);check(h.service.active()&&h.pauses==1,"full-inventory context uses existing disposal feature guard");h.service.cancel(2*S);
  h=new H(dir.resolve("world.json"));check(h.service.manualRequest(0),"queue original world");h.f.ctx=new AutomationContext(2,true,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("changed world never inherits request");
  h=new H(dir.resolve("farm.json"));check(h.service.manualRequest(0),"queue original Farm");h.f.ctx=new AutomationContext(1,false,true,Set.of());h.closed=true;h.service.tick(S);h.untouched("stopped Farm cannot turn queued task into standalone work");
  h=new H(dir.resolve("null.json"));check(h.service.manualRequest(0),"queue context");h.f.ctx=null;h.service.tick(S);h.f.ctx=new AutomationContext(1,true,true,Set.of());h.closed=true;h.service.tick(2*S);h.untouched("unavailable context revokes intent");
  h=new H(dir.resolve("foreign.json"));var other=h.session.begin(AutomationFeature.PEST,0,90*S);check(other!=null&&!h.service.manualRequest(S)&&h.session.isCurrent(other),"another owner cannot gain hidden queued disposal");h.session.invalidate();
  h=new H(dir.resolve("hub.json"));h.f.ctx=new AutomationContext(1,false,false,Set.of());check(h.service.manualRequest(0),"standalone NPC sale may be requested in known SkyBlock Hub, not only Garden");h.untouched("Hub queue is still inert");h.closed=true;h.service.tick(S);check(h.service.active()&&!h.session.holdsFarming()&&h.pauses==0&&h.f.commands==1,"Hub manual dispatch has no invented Farming hold");h.service.cancel(2*S);check(h.stops==0,"standalone completion never changes Farming");
  System.out.println("AutoSellManualQueueTest: PASS (real task/session, no UI disposal/command/hold, fresh one-shot dispatch, full inventory exception, expiry/world/Farm/blocker/cancel/policy safety)");
 }finally{try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}}
}
