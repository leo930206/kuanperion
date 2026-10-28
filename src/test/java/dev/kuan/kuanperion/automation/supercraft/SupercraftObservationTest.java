package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
/** Actual service/task/session with faulting read-only port, not a native/current-menu simulation claim. */
public final class SupercraftObservationTest {
 static final long S=1_000_000_000L;static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class P implements SupercraftTask.Port {
  final SupercraftTaskTest.Fake f=new SupercraftTaskTest.Fake();String fault="";
  void reading(String name){if(fault.equals(name+" throw"))throw new IllegalStateException(name+" unavailable");}
  public AutomationContext context(){reading("context");return fault.equals("context null")?null:f.context();}
  public SupercraftTask.Frame frame(long now){reading("frame");return fault.equals("frame null")?null:f.frame(now);}
  public Map<Integer,ItemSnapshot> inventory(){reading("inventory");return fault.equals("inventory null")?null:f.inventory();}
  public MenuSnapshot menu(){reading("menu");return f.menu();}
  public void requestCatalog(AutomationSession.Handle h){f.requestCatalog(h);reading("requestCatalog");}
  public Optional<Map<String,String>> catalog(){reading("catalog");return fault.equals("catalog null")?null:f.catalog();}
  public boolean recipe(AutomationSession.Handle h,SupercraftPlan.Recipe r,long now){return f.recipe(h,r,now);}
  public GuardedMenuAction.Result craft(AutomationSession.Handle h,SupercraftPreview p,long now){return f.craft(h,p,now);}
  public void closeOwnedMenu(int id){f.closeOwnedMenu(id);}public void dispose(AutomationSession.Handle h){f.dispose(h);}
 }
 static final class H implements AutoCloseable {
  final Path dir=Files.createTempDirectory("supercraft-observation-");final Path file=dir.resolve("automation.json");final AutomationSettingsStore store=AutomationSettingsStore.load(file);final P p=new P();
  final RuntimeException stopFailure=new IllegalStateException("safety stop failed");boolean closed=true,screenThrows,stopThrows,pauseThrows;
  final AutomationSession session=new AutomationSession(new AutomationCoordinator(),p::context,()->{p.f.pauses++;if(pauseThrows)throw new IllegalStateException("pause failed");},()->{p.f.stops++;if(stopThrows)throw stopFailure;});
  final SupercraftService service=new SupercraftService(store,session,new InventoryReceipts(),new CursorReceipts(),p,()->{if(screenThrows)throw new IllegalStateException("screen unavailable");return closed;},n->{});
  final SupercraftTask direct=new SupercraftTask(session,new InventoryReceipts(),new CursorReceipts(),p);
  H()throws Exception{store.setSupercraftPolicy(new SupercraftPolicy(SupercraftTaskTest.POLICY,60));store.setEnabled(AutomationFeature.SUPERCRAFT,true);}
  public void close()throws Exception{stopThrows=false;service.cancel(100*S);direct.cancel(100*S);session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void service(String mode)throws Exception{try(var h=new H()){
  byte[] saved=Files.readAllBytes(h.file);
  if(mode.equals("idle")){h.screenThrows=true;h.service.tick(0);check(!h.service.active()&&h.p.f.requests==0&&Arrays.equals(saved,Files.readAllBytes(h.file)),"unknown idle screen stays inert, saved consent intact");h.screenThrows=false;h.service.tick(S);check(h.service.active()&&h.p.f.requests==1,"fresh idle observation can recover");return;}
  if(mode.equals("pending")){h.closed=false;check(h.service.manualRequest(0),"queued UI intent");h.screenThrows=true;h.service.tick(S);check(!h.service.pending()&&!h.service.active()&&h.p.f.requests==0&&Arrays.equals(saved,Files.readAllBytes(h.file)),"unknown pending screen discards only intent");h.screenThrows=false;h.closed=true;h.store.setEnabled(AutomationFeature.SUPERCRAFT,false);h.service.tick(2*S);check(h.p.f.requests==0,"no delayed replay");return;}
  h.service.tick(0);check(h.service.active()&&h.p.f.requests==1&&h.session.holdsFarming(),"real active service/root");
  h.service.tick(S);h.p.f.show();h.closed=false;check(h.service.ownsMenu(),"owned recipe menu");
  if(mode.equals("known-owned")){h.service.tick(2*S);check(h.service.active()&&h.p.f.crafts==1&&h.p.f.stops==0,"known-open owned menu still advances normally");return;}
  if(mode.equals("menu-read")){h.p.fault="menu throw";check(!h.service.ownsMenu(),"unavailable ownership cannot report authority or throw");}else h.screenThrows=true;
  h.service.tick(2*S);check(!h.service.active()&&!h.session.active()&&!h.session.holdsFarming()&&!h.service.autoEnabled()&&h.p.f.stops==1&&h.p.f.closes==0&&h.p.f.crafts==0&&h.p.f.disposes==1,"active observation failure stops once without craft/close/repair");
  h.p.fault="";h.screenThrows=false;h.service.tick(100*S);check(h.p.f.requests==1&&h.p.f.stops==1,"active fault suppresses replay");
 }}
 static void task(String fault,boolean stopping)throws Exception{try(var h=new H()){
  check(h.direct.request(SupercraftTaskTest.POLICY,true,0),"task acquires hold");if(fault.equals("manual"))h.p.f.manual=true;else h.p.fault=fault;h.stopThrows=stopping;
  if(stopping){try{h.direct.tick(S);throw new AssertionError("stop callback failure must propagate");}catch(RuntimeException failure){check(failure==h.stopFailure,"preserve original stop error");}}else h.direct.tick(S);
  check(!h.direct.active()&&!h.session.active()&&!h.session.holdsFarming()&&h.p.f.stops==1&&h.p.f.disposes==1&&h.p.f.commands==0&&h.p.f.crafts==0&&h.p.f.closes==0,"failed observation releases exact original lease once without gameplay continuation: "+fault);
  h.stopThrows=false;h.p.fault="";h.direct.tick(2*S);check(h.p.f.disposes==1&&h.p.f.stops==1,"terminal failure not cleaned up twice");
 }}
 static void acquisition(String mode)throws Exception{try(var h=new H()){
  h.stopThrows=true;h.pauseThrows=mode.equals("pause");if(!h.pauseThrows)h.p.fault="requestCatalog throw";
  try{h.direct.request(SupercraftTaskTest.POLICY,true,0);throw new AssertionError("acquisition cleanup must not swallow stop error");}catch(RuntimeException failure){check(failure==h.stopFailure,"exact acquisition stop failure retained");}
  check(!h.direct.active()&&!h.session.active()&&!h.session.holdsFarming()&&h.p.f.pauses==1&&h.p.f.stops==1&&h.p.f.commands==0,"failed begin/catalog retains no invisible Farm hold");
 }}
 static void cancel()throws Exception{try(var h=new H()){check(h.direct.request(SupercraftTaskTest.POLICY,true,0),"cancel fixture");h.stopThrows=true;try{h.direct.cancel(S);throw new AssertionError("explicit stop must propagate callback failure");}catch(RuntimeException failure){check(failure==h.stopFailure,"exact cancel failure retained");}check(!h.direct.active()&&!h.session.active()&&h.p.f.stops==1&&h.p.f.disposes==1,"explicit stop terminal before error escapes");}}
 static void preflight(String fault)throws Exception{try(var h=new H()){h.p.fault=fault;byte[] saved=Files.readAllBytes(h.file);check(!h.direct.request(SupercraftTaskTest.POLICY,true,0)&&!h.direct.active()&&!h.session.active()&&h.p.f.pauses==0&&h.p.f.stops==0&&h.p.f.disposes==0&&h.p.f.requests==0&&h.p.f.commands==0&&Arrays.equals(saved,Files.readAllBytes(h.file)),"invalid preflight stays inert: "+fault);h.p.fault="";check(h.direct.request(SupercraftTaskTest.POLICY,true,S),"fresh preflight may subsequently succeed");}}
 public static void main(String[] args)throws Exception{
  if(args.length>0){switch(args[0]){case "cancel"->cancel();case "pause","catalog"->acquisition(args[0]);case "tick-stop"->task("frame throw",true);case "tick-stop-terminal"->task("manual",true);default->service(args[0]);}}
  else{for(String mode:List.of("idle","pending","active","menu-read","known-owned"))service(mode);for(String fault:List.of("context throw","context null","frame throw","frame null","inventory throw","inventory null"))preflight(fault);for(String fault:List.of("context throw","context null","frame throw","frame null","catalog throw","catalog null"))task(fault,false);task("frame throw",true);task("manual",true);acquisition("pause");acquisition("catalog");cancel();}
  System.out.println("SupercraftObservationTest: PASS (actual service/task/session observation faults, original lease release, no duplicate cleanup, stop errors visible across begin/tick/cancel)");
 }
}
