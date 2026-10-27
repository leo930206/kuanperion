package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Actual service/task/session; a borrowed Pest lease isolates dispatch ownership, not a live Pest flight claim. */
public final class GreenhouseBusyRootTest {
 static final long S=1_000_000_000L,DUE=600*S;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static final class Fixture implements AutoCloseable {
  final Path dir=Files.createTempDirectory("greenhouse-busy-");final Path file=dir.resolve("automation.json");final AutomationSettingsStore settings=AutomationSettingsStore.load(file);final GreenhouseTaskTest.Fake f=new GreenhouseTaskTest.Fake();
  final AutomationCoordinator coordinator=new AutomationCoordinator();final AutomationSession session=new AutomationSession(coordinator,f::context,()->f.pauses++,()->f.stops++);final AtomicBoolean allowed=new AtomicBoolean(true);final int[] reads={0};final AutomationSession.Handle[] started={null};final GreenhouseService service;
  Fixture()throws Exception{
   var port=(GreenhouseTask.Port)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{GreenhouseTask.Port.class},(p,m,a)->{if(Set.of("context","frame").contains(m.getName()))reads[0]++;if(m.getName().equals("prepare"))started[0]=(AutomationSession.Handle)a[0];try{return m.invoke(f,a);}catch(InvocationTargetException e){throw e.getCause();}});
   check(port.context().equals(f.ctx)&&port.frame(0).complete(),"fixture forwards actual observations");reads[0]=0;
   service=new GreenhouseService(settings,session,new PlayerPositionReceipts(),new HarvestReceipts(),port,allowed::get,n->{});service.setPolicy(GreenhouseTaskTest.POLICY);service.setEnabled(true);service.tick(0);
  }
  public void close()throws Exception{service.cancel(DUE+2*S);session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void run(String mode)throws Exception{try(var x=new Fixture()){
  byte[] saved=Files.readAllBytes(x.file);long at=mode.equals("not-due")?DUE-S:DUE;var pest=x.session.begin(AutomationFeature.PEST,at-S,30*S);check(pest!=null&&x.session.holdsFarming(),"original Farm held by Pest root");x.service.tick(at);
  if(mode.equals("busy")){check(x.reads[0]==0&&x.f.prepares==0&&x.f.commands.isEmpty()&&x.session.isCurrent(pest),"busy Pest root must not spend Greenhouse native preflight/poll or steal ownership");check(Arrays.equals(saved,Files.readAllBytes(x.file)),"busy deferral never rewrites preferences");return;}
  check(x.session.finish(pest,true,at),"preceding root successfully releases original Farm");
  if(mode.equals("manual-stop")){x.session.invalidate();x.f.ctx=new AutomationContext(1,false,true,Set.of());x.service.tick(at);check(!x.service.active()&&!x.session.active()&&x.f.commands.isEmpty()&&Arrays.equals(saved,Files.readAllBytes(x.file)),"fresh Farm-off state cannot replay due work or rewrite enable bit");return;}
  if(mode.equals("not-due")){x.service.tick(at);check(!x.service.active()&&x.f.commands.isEmpty(),"busy interval does not force work before its due time");at=DUE;}
  if(mode.equals("ui")){x.allowed.set(false);x.service.tick(at);check(!x.service.active()&&x.f.commands.isEmpty(),"user settings screen retains priority after owner release");x.allowed.set(true);}
  x.service.tick(at);check(x.service.active()&&x.f.commands.size()==1&&x.f.pauses==2&&x.f.stops==0&&x.session.holdsFarming(),"due Greenhouse may dispatch immediately at the SAME timestamp, no artificial one-second busy poll penalty");
  var next=x.started[0];check(next!=null&&next.lease().owner()==AutomationFeature.GREENHOUSE&&next.generation()==pest.generation()&&next.hold().farmingSession()==pest.hold().farmingSession()&&!x.session.isCurrent(pest),"new fixed-feature lease holds only the same original manual Farm generation");check(Arrays.equals(saved,Files.readAllBytes(x.file)),"dispatch changes runtime ownership, not preferences");
 }}
 public static void main(String[]args)throws Exception{if(args.length>0)run(args[0]);else for(var mode:List.of("busy","handoff","manual-stop","not-due","ui"))run(mode);System.out.println("GreenhouseBusyRootTest: PASS (no busy-root preflight, due timestamp preserved, fresh manual/UI priority and original Farm generation; no live chain claim)");}
}
