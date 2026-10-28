package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
/** Real service/task/session, deterministic native observations; no live anvil claim. */
public final class BookServiceObservationTest {
 static final long S=1_000_000_000L;static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class H implements AutoCloseable {
  final Path dir=Files.createTempDirectory("book-observation-");final Path file=dir.resolve("automation.json");
  final AutomationSettingsStore store=AutomationSettingsStore.load(file);final BookCombineTaskTest.Fake f=new BookCombineTaskTest.Fake();
  final RuntimeException stopFailure=new IllegalStateException("safety stop failed");final List<StashService.Notice> notices=new ArrayList<>();
  boolean closed=true,screenThrows,stopThrows,factoryThrows,factoryNull;int factories,pauses,stops;
  final AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->pauses++,()->{stops++;if(stopThrows)throw stopFailure;});
  final BookService service;
  H()throws Exception{var items=new InventoryReceipts();var cursors=new CursorReceipts();f.cursor=cursors;store.setBookPolicy(BookCombineTaskTest.policy());
   service=new BookService(store,session,items,cursors,f::context,p->{factories++;if(factoryThrows)throw new IllegalStateException("factory unavailable");return factoryNull?null:f;},BookCombineTaskTest.LAYOUT,()->{if(screenThrows)throw new IllegalStateException("screen unavailable");return closed;},notices::add);
  }
  public void close()throws Exception{stopThrows=false;service.cancel(100*S);session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void run(String mode)throws Exception{try(var h=new H()){
  h.store.setEnabled(AutomationFeature.BOOK_COMBINE,true);byte[] saved=Files.readAllBytes(h.file);
  if(mode.startsWith("factory")){
   h.factoryThrows=mode.endsWith("throw");h.factoryNull=mode.endsWith("null");h.closed=false;check(h.service.manualRequest(0),"manual intent accepted before factory");h.closed=true;h.service.tick(S);
   check(!h.service.pending()&&!h.service.active()&&!h.session.active()&&h.factories==1&&h.pauses==0&&h.f.opens==0&&h.notices.size()==1,"failed factory consumes only manual intent, no acquired root/command or success");
   check(Arrays.equals(saved,Files.readAllBytes(h.file)),"factory observation fault preserves saved consent");h.factoryThrows=h.factoryNull=false;h.service.tick(2*S);check(h.service.active()&&h.f.opens==1,"fresh healthy automatic observation can dispatch later");return;
  }
  if(mode.equals("idle")){
   h.screenThrows=true;h.service.tick(0);check(h.factories==0&&!h.session.active()&&Arrays.equals(saved,Files.readAllBytes(h.file)),"unknown idle screen never creates driver or writes preferences");h.screenThrows=false;h.service.tick(S);check(h.service.active()&&h.f.opens==1,"idle observation recovery is not permanently suspended");return;
  }
  if(mode.equals("pending")){
   h.closed=false;check(h.service.manualRequest(0),"deferred manual intent");h.screenThrows=true;h.service.tick(S);check(!h.service.pending()&&!h.service.active()&&h.factories==0&&Arrays.equals(saved,Files.readAllBytes(h.file)),"unknown screen cancels only queued intent without persistence");h.screenThrows=false;h.closed=true;h.store.setEnabled(AutomationFeature.BOOK_COMBINE,false);h.service.tick(2*S);check(h.factories==0,"invalidated manual intent cannot replay on later screen close");return;
  }
  h.service.tick(0);check(h.service.active()&&h.session.holdsFarming()&&h.f.opens==1,"real task acquired original Farm and opened expected anvil");
  // An already-owned menu does not make an unavailable screen observation safe.
  h.closed=false;check(h.service.ownsMenu(),"current expected menu fixture");h.screenThrows=true;h.stopThrows=mode.equals("stop-throws");
  if(h.stopThrows){try{h.service.tick(S);throw new AssertionError("safety stop error must remain visible");}catch(RuntimeException failure){check(failure==h.stopFailure,"do not replace/swallow safety-stop failure with screen fault");}}
  else h.service.tick(S);
  check(!h.service.active()&&!h.service.pending()&&!h.service.autoEnabled()&&!h.session.active()&&!h.session.holdsFarming()&&h.stops==1&&h.f.transfers==0&&h.f.returns==0&&h.f.closed==0,"unavailable active screen revokes original task without recovery/close/retry");
  h.stopThrows=false;h.screenThrows=false;h.service.tick(50*S);check(h.f.opens==1&&h.stops==1,"failed observation never restarts work after recovery");
 }}
 public static void main(String[] args)throws Exception{if(args.length>0)run(args[0]);else for(String mode:List.of("idle","pending","active","stop-throws","factory-throw","factory-null"))run(mode);System.out.println("BookServiceObservationTest: PASS (real service/task/session; idle/pending/owned-screen and lazy factory faults, no replay/repair, stop errors visible)");}
}
