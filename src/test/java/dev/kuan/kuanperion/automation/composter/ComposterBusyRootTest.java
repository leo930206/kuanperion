package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Actual service/task/session; a borrowed preceding lease isolates dispatch, not full Pest/Composter server interaction. */
public final class ComposterBusyRootTest {
 static final long S=1_000_000_000L,DUE=7200*S;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static final class Fixture implements AutoCloseable {
  final Path dir=Files.createTempDirectory("composter-busy-");final Path file=dir.resolve("automation.json");final AutomationSettingsStore settings=AutomationSettingsStore.load(file);final ComposterTaskTest.Fake f=new ComposterTaskTest.Fake();
  final AutomationCoordinator coordinator=new AutomationCoordinator();final AutomationSession session=new AutomationSession(coordinator,f::context,()->f.pauses++,()->f.stops++);final AtomicBoolean allowed=new AtomicBoolean(true);final int[] reads={0};final AutomationSession.Handle[] started={null};final ComposterService service;
  Fixture()throws Exception{
   var port=(ComposterTask.Port)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ComposterTask.Port.class},(p,m,a)->{if(Set.of("context","frame","inventory","menu").contains(m.getName()))reads[0]++;if(m.getName().equals("prepare"))started[0]=(AutomationSession.Handle)a[0];try{return m.invoke(f,a);}catch(InvocationTargetException e){throw e.getCause();}});
   check(port.context().equals(f.ctx)&&port.frame(0).complete(),"fixture forwards actual observations");reads[0]=0;
   service=new ComposterService(settings,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),port,allowed::get,Optional::empty,n->{});service.setPolicy(ComposterTaskTest.POLICY);service.setEnabled(true);service.tick(0);
  }
  public void close()throws Exception{service.cancel(DUE+2*S);session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void run(String mode)throws Exception{try(var x=new Fixture()){
  byte[] saved=Files.readAllBytes(x.file);long at=mode.equals("not-due")?DUE-S:DUE;var preceding=x.session.begin(AutomationFeature.PEST,at-S,30*S);check(preceding!=null&&x.session.holdsFarming(),"preceding task holds original Farm");x.service.tick(at);
  if(mode.equals("busy")){check(x.reads[0]==0&&x.f.prepares==0&&x.f.commands.isEmpty()&&x.session.isCurrent(preceding),"busy preceding root must not spend Composter native preflight/poll or steal ownership");check(Arrays.equals(saved,Files.readAllBytes(x.file)),"busy deferral never rewrites preferences");return;}
  check(x.session.finish(preceding,true,at),"preceding root successfully releases original Farm");
  if(mode.equals("manual-stop")){x.session.invalidate();x.f.ctx=new AutomationContext(1,false,true,Set.of());x.service.tick(at);check(!x.service.active()&&!x.session.active()&&x.f.commands.isEmpty()&&Arrays.equals(saved,Files.readAllBytes(x.file)),"fresh manual Farm stop prevents stale due replay without overwriting preference");return;}
  if(mode.equals("not-due")){x.service.tick(at);check(!x.service.active()&&x.f.commands.isEmpty(),"not-due Composter cannot launch early because owner completed");at=DUE;}
  if(mode.equals("ui")){x.allowed.set(false);x.service.tick(at);check(!x.service.active()&&x.f.commands.isEmpty(),"settings UI wins after preceding owner release");x.allowed.set(true);}
  x.service.tick(at);check(x.service.active()&&x.f.commands.size()==1&&x.f.pauses==2&&x.f.stops==0&&x.session.holdsFarming(),"due Composter dispatches at the SAME timestamp without spending busy poll cooldown");
  var next=x.started[0];check(next!=null&&next.lease().owner()==AutomationFeature.COMPOSTER&&next.generation()==preceding.generation()&&next.hold().farmingSession()==preceding.hold().farmingSession()&&!x.session.isCurrent(preceding),"new Composter lease still belongs only to original manual Farm generation");check(Arrays.equals(saved,Files.readAllBytes(x.file)),"fresh dispatch leaves preferences unchanged");
 }}
 public static void main(String[]args)throws Exception{if(args.length>0)run(args[0]);else for(var mode:List.of("busy","handoff","manual-stop","not-due","ui"))run(mode);System.out.println("ComposterBusyRootTest: PASS (busy-root observation denial, preserved due time, current Farm/UI priority and original generation; no full live chain claim)");}
}
