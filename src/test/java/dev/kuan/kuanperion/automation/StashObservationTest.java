package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
/** Real Stash service/task/coordinator; deterministic reads/sender, no server pickup claim. */
public final class StashObservationTest {
 static final long S=1_000_000_000L;static final String ADDED="Your items were stashed away!",EMPTY="Your stash isn't holding any items or materials!",BLOCKED="Couldn't unstash your items!";
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final class H implements AutoCloseable {
  final Path dir=Files.createTempDirectory("stash-observation-");final Path file=dir.resolve("automation.json");final AutomationSettingsStore store=AutomationSettingsStore.load(file);
  final AutomationCoordinator coordinator=new AutomationCoordinator();final AutomationContext context=new AutomationContext(1,true,true,Set.of());final List<StashService.Notice> notices=new ArrayList<>();
  final RuntimeException feedbackFailure=new IllegalStateException("feedback failed");String fault="",outcome="accept";Runnable readHook,sendHook;int reads,commands;boolean feedbackThrows;final StashService service;
  H()throws Exception{store.setEnabled(AutomationFeature.STASH,true);service=new StashService(store,coordinator,this::observe,c->{check(c==AutomationCommand.PICKUP_STASH,"typed command only");commands++;String response=outcome;outcome="accept";var hook=sendHook;sendHook=null;if(hook!=null)hook.run();if(response.equals("throw"))throw new IllegalStateException("send uncertain");return !response.equals("reject");},n->{notices.add(n);if(feedbackThrows)throw feedbackFailure;});}
  AutomationContext observe(){reads++;String fail=fault;var hook=readHook;readHook=null;if(hook!=null)hook.run();if(fail.equals("throw"))throw new IllegalStateException("context unavailable");return fail.equals("null")?null:context;}
  void released(long now){var other=coordinator.tryAcquire(AutomationFeature.PEST,context,now,10*S);check(other!=null,"original Stash command lease actually released, not only UI flag cleared");check(coordinator.release(other),"only this probe's own lease released");}
  public void close()throws Exception{feedbackThrows=false;readHook=sendHook=null;fault="";service.cancel("fixture cleanup");coordinator.clear();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 static void idle(String fault)throws Exception{try(var h=new H()){
  byte[] saved=Files.readAllBytes(h.file);h.fault=fault;check(!h.service.manualRequest(0),"unknown manual preflight refuses before task/command");h.service.onSystemMessage(ADDED,0);
  check(!h.service.active()&&h.service.autoEnabled()&&h.commands==0&&h.notices.isEmpty()&&Arrays.equals(saved,Files.readAllBytes(h.file)),"idle unavailable reads are inert and do not rewrite or silently suspend consent");h.released(S);
  h.fault="";h.service.onSystemMessage(ADDED,2*S);h.service.tick(2*S);check(h.service.active()&&h.commands==1,"later independent healthy event can start fresh work");
 }}
 static void active(String fault,String route,boolean sent)throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"active fixture");if(sent)h.service.tick(0);int before=h.commands,count=h.notices.size();byte[] saved=Files.readAllBytes(h.file);h.fault=fault;
  if(route.equals("tick"))h.service.tick(S);else h.service.onSystemMessage(route.equals("empty")?EMPTY:BLOCKED,S);
  check(!h.service.active()&&h.service.state()==StashTask.State.CANCELLED&&!h.service.autoEnabled()&&h.commands==before&&h.notices.size()==count+1,"unknown active "+route+" stops rather than sending/completing/holding indefinitely");
  check(h.store.enabled(AutomationFeature.STASH)&&Arrays.equals(saved,Files.readAllBytes(h.file)),"active read fault suppresses runtime only, saved latest preference preserved");h.released(S+1);
  h.fault="";h.service.onSystemMessage(ADDED,2*S);h.service.tick(2*S);check(h.commands==before&&!h.service.active()&&h.notices.size()==count+1,"healthy read alone never replays failed active work");
  check(h.service.manualRequest(3*S),"explicit manual remains available after runtime suspension");h.service.tick(3*S);check(h.commands==before+1&&!h.service.autoEnabled(),"manual does not rearm automatic pickup");h.service.cancel("explicit stop");h.service.setEnabled(true);h.service.onSystemMessage(ADDED,4*S);h.service.tick(4*S);check(h.commands==before+2,"explicit re-enable starts a fresh run, not previous cooldown");
 }}
 static void reentrantRead(String route,String fault)throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"old request");h.fault=fault;h.readHook=()->{h.service.cancel("context callback");h.fault="";check(h.service.manualRequest(S),"new explicit request inside observation");};
  if(route.equals("tick"))h.service.tick(S);else h.service.onSystemMessage(EMPTY,S);
  check(h.service.active()&&h.service.attempts()==0&&h.commands==0&&h.notices.stream().noneMatch(n->n.text().contains("開始領取")||n.text().contains("已無待領")),"old "+route+" observation must not dispatch, cancel or finish a newer run");
  h.service.tick(2*S);check(h.commands==1&&h.service.active()&&h.service.attempts()==1,"only the next fresh tick can send for new run");
 }}
 static void reentrantSender(String outcome)throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"sender old run");h.outcome=outcome;h.sendHook=()->{h.service.cancel("sender callback");check(h.service.manualRequest(0),"sender creates distinct explicit run");};h.service.tick(0);
  check(h.commands==1&&h.service.active()&&h.service.attempts()==0&&h.notices.stream().noneMatch(n->n.text().contains("開始領取")),"old sender "+outcome+" must not count/finish/report the new run");
  h.service.tick(S/2);check(h.commands==2&&h.service.attempts()==1&&h.service.active(),"new run's first command has its own timer");
  h.service.onOutgoingCommand();check(!h.service.active(),"trusted own-send flag always cleared, even after uncertainty/reentrancy");h.released(S);
 }}
 static void newRunTimer()throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"timer initial");h.service.tick(0);h.service.onSystemMessage(EMPTY,1);check(!h.service.active(),"first run completed");
  check(h.service.manualRequest(S/2),"new independent request");h.service.tick(S/2);check(h.commands==2&&h.service.attempts()==1,"first command cannot inherit previous run's everSent/lastSent delay");
 }}
 static void nestedSender()throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"outer sender setup");h.sendHook=()->{h.service.cancel("replace old run");check(h.service.manualRequest(0),"nested explicit run");h.service.tick(0);h.service.onOutgoingCommand();};h.service.tick(0);
  check(h.commands==2&&h.service.active()&&h.service.attempts()==1,"inner own sender must restore outer trusted scope; old result cannot change new attempts");
  h.service.onOutgoingCommand();check(!h.service.active(),"outer finally restores untrusted/manual command priority");h.released(S);
 }}
 static void feedback()throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"feedback setup");h.service.tick(0);h.fault="throw";h.feedbackThrows=true;
  try{h.service.tick(S);throw new AssertionError("cancel feedback failure must remain visible");}catch(RuntimeException failure){check(failure==h.feedbackFailure,"do not swallow original feedback failure with context catch");}
  check(!h.service.active()&&!h.service.autoEnabled()&&h.commands==1,"state revoked before failing notification");h.released(S+1);
 }}
 static void disabledDuringRead()throws Exception{try(var h=new H()){
  h.readHook=()->h.service.suspend("new safety stop");h.service.onSystemMessage(ADDED,0);check(!h.service.active()&&h.commands==0&&!h.service.autoEnabled(),"automatic consent rechecked after context callback suspension");
 }}
 static void foreignOwner(boolean sender)throws Exception{try(var h=new H()){
  check(h.service.manualRequest(0),"foreign-owner old run");AutomationCoordinator.Lease[] foreign={null};Runnable replace=()->{h.service.cancel("manual foreign owner");foreign[0]=h.coordinator.tryAcquire(AutomationFeature.PEST,h.context,0,10*S);check(foreign[0]!=null,"new foreign root acquired only after original cancellation");};
  if(sender){h.sendHook=replace;h.outcome="reject";h.service.tick(0);}else{h.service.tick(0);h.readHook=replace;h.fault="throw";h.service.tick(1);}
  check(h.service.state()==StashTask.State.CANCELLED&&!h.service.active()&&h.coordinator.isOwner(foreign[0],h.context,S),"old Stash result/read cannot fail or release a new foreign root");
 }}
 static void ignored()throws Exception{try(var h=new H()){
  h.fault="throw";h.service.onSystemMessage("[Player] Your items were stashed away!",0);h.service.onSystemMessage("ordinary message",0);check(h.reads==0&&h.commands==0&&!h.service.active(),"untrusted/non-signal chat never consults context or starts work");
  h.fault="";check(h.service.manualRequest(0),"already active signal setup");h.reads=0;h.fault="throw";h.service.onSystemMessage(ADDED,S);check(h.reads==0&&h.service.active()&&h.service.attempts()==0,"another ADDED cannot reset or preflight an existing run");
 }}
 public static void main(String[] args)throws Exception{
  if(args.length>0)switch(args[0]){case "idle"->idle("throw");case "active"->active("throw","tick",true);case "read"->reentrantRead("tick","");case "signal"->reentrantRead("signal","");case "send"->reentrantSender("accept");case "send-reject"->reentrantSender("reject");case "send-throw"->reentrantSender("throw");case "timer"->newRunTimer();case "nested"->nestedSender();case "feedback"->feedback();case "disabled"->disabledDuringRead();case "ignored"->ignored();default->throw new AssertionError(args[0]);}
  else{for(String fault:List.of("null","throw")){idle(fault);for(String route:List.of("tick","empty","blocked"))for(boolean sent:new boolean[]{false,true})active(fault,route,sent);}for(String route:List.of("tick","signal"))for(String fault:List.of("","null","throw"))reentrantRead(route,fault);for(String result:List.of("accept","reject","throw"))reentrantSender(result);newRunTimer();nestedSender();feedback();disabledDuringRead();foreignOwner(false);foreignOwner(true);ignored();}
  System.out.println("StashObservationTest: PASS (actual service/task/coordinator; idle/active read faults, exact lease release, no replay, protected run reentrancy, fresh per-run timer, visible feedback failures)");
 }
}
