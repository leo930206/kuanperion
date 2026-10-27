package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Actual service/task/session and existing deterministic port. No server/client UI or craft claim. */
public final class SupercraftManualIntentTest {
 static final long S=1_000_000_000L;static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class H implements AutoCloseable {
  final Path dir=Files.createTempDirectory("supercraft-manual-intent-");final Path file=dir.resolve("automation.json");final SupercraftTaskTest.Fake f=new SupercraftTaskTest.Fake();final AtomicBoolean closed=new AtomicBoolean(false);final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);final SupercraftService service;
  H()throws Exception{var store=AutomationSettingsStore.load(file);service=new SupercraftService(store,session,new InventoryReceipts(),new CursorReceipts(),f,closed::get,n->{});service.setPolicy(new SupercraftPolicy(SupercraftTaskTest.POLICY,60));}
  public void close()throws Exception{service.cancel(100*S);session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void run(String mode)throws Exception{try(var h=new H()){
  byte[] saved=Files.readAllBytes(h.file);
  if(mode.equals("busy")){var prior=h.session.begin(AutomationFeature.PEST,0,60*S);check(prior!=null&&!h.service.manualRequest(0)&&!h.service.pending()&&h.f.requests==0,"foreign live root cannot accept a delayed Supercraft intent");return;}
  if(mode.equals("standalone"))h.f.ctx=new AutomationContext(1,false,false,Set.of());
  check(h.service.manualRequest(0)&&h.service.pending()&&h.f.requests==0&&!h.session.active(),"UI offer has no data/command/hold");h.service.tick(S);check(h.service.pending()&&h.f.requests==0,"open settings keeps explicit request inert");
  switch(mode){
   case "world"->h.f.ctx=new AutomationContext(2,true,true,Set.of());
   case "farm"->h.f.ctx=new AutomationContext(1,false,true,Set.of());
   case "foreign-menu"->h.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));
   case "focus"->h.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));
   case "cancel"->h.service.cancel(2*S);
   case "policy"->h.service.setPolicy(new SupercraftPolicy(SupercraftTaskTest.POLICY,60));
   case "expiry"->{h.service.tick(30*S);h.closed.set(true);h.service.tick(31*S);check(!h.service.pending()&&h.f.requests==0,"exact30s expiry never replays");return;}
   case "clock"->{h.service.tick(-1);check(!h.service.pending()&&h.f.requests==0,"clock reversal invalidates intent");return;}
   default->{}
  }
  h.closed.set(true);h.service.tick(2*S);
  if(mode.equals("success")||mode.equals("standalone")){check(h.service.active()&&h.f.requests==1&&!h.service.pending()&&h.session.holdsFarming()==mode.equals("success")&&h.f.pauses==(mode.equals("success")?1:0),"current original-world/Farm state dispatches one exact task; manual standalone never invents Farm");return;}
  check(!h.service.pending()&&!h.service.active()&&!h.session.active()&&h.f.requests==0&&h.f.commands==0&&h.f.pauses==0&&Arrays.equals(saved,Files.readAllBytes(h.file)),"invalidated "+mode+" request never acquires/query/replays or rewrites enable consent");
 }}
 public static void main(String[]args)throws Exception{if(args.length>0)run(args[0]);else for(String mode:List.of("world","farm","busy","foreign-menu","focus","cancel","policy","expiry","clock","success","standalone"))run(mode);System.out.println("SupercraftManualIntentTest: PASS (real service/task/session, bounded original-world/Farm intent, foreign-root/menu/focus/policy/clock invalidation, one explicit standalone-safe dispatch)");}
}
