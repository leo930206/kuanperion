package dev.kuan.kuanperion.automation;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public final class AutomationRuntimeTest {
 static final long SECOND=1_000_000_000L;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 static AutomationContext context(long world,AutomationContext.Blocker... blockers){return new AutomationContext(world,true,true,Set.of(blockers));}
 public static void main(String[] args)throws Exception {
  ownership();stash();preferences();service();
  System.out.println("AutomationRuntimeTest: PASS (ownership/world/safety gates, bounded Stash, atomic off-by-default preferences)");
 }
 static void ownership(){
  var c=new AutomationCoordinator();var safe=context(1);
  var first=c.tryAcquire(AutomationFeature.STASH,safe,0,30*SECOND);
  check(first!=null&&c.permits(first,AutomationCoordinator.Resource.COMMAND,safe,1),"command lease issued");
  check(!c.permits(first,AutomationCoordinator.Resource.MOVEMENT,safe,1),"Stash never owns movement");
  check(c.tryAcquire(AutomationFeature.PEST,safe,1,30*SECOND)==null,"no overlapping owner or unsafe preemption");
  check(!c.permits(first,AutomationCoordinator.Resource.COMMAND,context(1,AutomationContext.Blocker.MENU),2),"user GUI blocks commands");
  check(c.permits(first,AutomationCoordinator.Resource.COMMAND,safe,3),"safe context returns without new lease");
  check(!c.permits(first,AutomationCoordinator.Resource.COMMAND,context(2),4),"world invalidates lease");
  var second=c.tryAcquire(AutomationFeature.PEST,context(2),5,SECOND);
  check(second!=null&&!c.release(first)&&c.isOwner(second,context(2),6),"stale release cannot cancel new owner");
  check(!c.isOwner(second,context(2),5+SECOND),"lease times out at exact boundary");
  for(var blocker:AutomationContext.Blocker.values()){
   var lease=c.tryAcquire(AutomationFeature.STASH,safe,10,SECOND);
   check(!c.permits(lease,AutomationCoordinator.Resource.COMMAND,context(1,blocker),11),"all blockers deny side effects: "+blocker);c.clear();
  }
  long near=Long.MAX_VALUE-SECOND;
  var lease=c.tryAcquire(AutomationFeature.STASH,safe,near,2*SECOND);
  check(c.isOwner(lease,safe,near+SECOND)&&!c.isOwner(lease,safe,near+2*SECOND),"nanoTime wrap-safe timeout");
 }
 static void stash(){
  var c=new AutomationCoordinator();var task=new StashTask(c);var p=StashTask.Policy.defaults();var safe=context(1);var commands=new ArrayList<AutomationCommand>();
  check(task.request(safe,p,false,0),"fresh automatic request accepted");
  task.tick(safe,0,command->{commands.add(command);return true;});
  check(commands.equals(List.of(AutomationCommand.PICKUP_STASH))&&task.attempts()==1,"typed actual pickup command requested");
  for(int i=0;i<50;i++)task.tick(safe,SECOND,command->{commands.add(command);return true;});
  check(commands.size()==1,"ticks cannot spam before delay");
  check(!task.request(safe,p,false,2*SECOND),"duplicate signals cannot reset limits");
  task.tick(context(1,AutomationContext.Blocker.MENU),3*SECOND,command->{throw new AssertionError("GUI side effect");});
  task.tick(safe,3*SECOND,command->{commands.add(command);return true;});
  check(commands.size()==2,"eligible retry after GUI closes");
  task.onSignal(StashTask.Signal.EMPTY,safe);task.tick(safe,30*SECOND,command->{throw new AssertionError("command after empty");});
  check(task.state()==StashTask.State.COMPLETE&&!task.active(),"empty reply releases/finishes");
  check(c.tryAcquire(AutomationFeature.PEST,safe,31*SECOND,SECOND)!=null,"Stash releases ownership on completion");c.clear();
  var limited=new StashTask.Policy(2,2,30,true);task.request(safe,limited,true,40*SECOND);
  task.tick(safe,40*SECOND,command->true);task.tick(safe,42*SECOND,command->true);task.tick(safe,44*SECOND,command->{throw new AssertionError("over attempt limit");});
  check(task.state()==StashTask.State.EXHAUSTED,"bounded attempt budget");
  task.request(safe,p,true,50*SECOND);task.tick(context(1,AutomationContext.Blocker.FOCUS),50*SECOND,command->{throw new AssertionError("background send");});
  task.tick(safe,110*SECOND,command->{throw new AssertionError("timeout side effect");});check(task.state()==StashTask.State.TIMED_OUT,"timeout even when blocked");
  for(var blocker:List.of(AutomationContext.Blocker.MINING,AutomationContext.Blocker.RECOVERY,AutomationContext.Blocker.SELECTION,AutomationContext.Blocker.DISCONNECTED,AutomationContext.Blocker.DEAD,AutomationContext.Blocker.OUTSIDE_SKYBLOCK)){
   task.request(safe,p,true,120*SECOND);task.tick(context(1,blocker),121*SECOND,command->{throw new AssertionError("legacy priority violated");});check(task.state()==StashTask.State.CANCELLED,"hard blocker cancels: "+blocker);
  }
  task.request(safe,p,true,130*SECOND);task.tick(context(2),130*SECOND,command->{throw new AssertionError("old world request replayed");});check(task.state()==StashTask.State.CANCELLED,"world change never replays request");
  task.request(safe,p,true,140*SECOND);task.tick(safe,140*SECOND,command->false);check(task.state()==StashTask.State.FAILED,"send failure is not success");
  task.request(safe,p,true,150*SECOND);task.tick(safe,150*SECOND,command->{throw new IllegalStateException("fake send failure");});check(task.state()==StashTask.State.FAILED,"throwing boundary releases task");
  task.request(safe,p,false,160*SECOND);task.tick(new AutomationContext(1,false,true,Set.of()),160*SECOND,command->{throw new AssertionError("farming stopped");});check(task.state()==StashTask.State.CANCELLED,"auto request obeys only-while-Farming");
  task.request(safe,p,true,170*SECOND);task.tick(context(1,AutomationContext.Blocker.INVENTORY_FULL),170*SECOND,command->{throw new AssertionError("full inventory pickup");});check(task.state()==StashTask.State.BLOCKED,"full inventory stops safely");
  task.request(safe,p,true,180*SECOND);task.onSignal(StashTask.Signal.EMPTY,context(2));check(task.state()==StashTask.State.CANCELLED,"new-world reply cannot complete old task");
  check(StashTask.classify("Your stash isn't holding any items or materials!")==StashTask.Signal.EMPTY,"source-known empty signal");
  check(StashTask.classify("Your items were stashed away!")==StashTask.Signal.ADDED,"source-known stash signal");
  check(StashTask.classify("Couldn't unstash your items!")==StashTask.Signal.BLOCKED,"source-known blocked signal");
  check(StashTask.classify("[VIP] Alex: Your items were stashed away!")==StashTask.Signal.NONE,"player-shaped message cannot start task");
  check(StashTask.classify("[Kuanperion] Your items were stashed away!")==StashTask.Signal.NONE,"no local feedback loop");
 }
 static void service()throws Exception {
  Path dir=Files.createTempDirectory("stash-service-");
  try{
   var store=AutomationSettingsStore.load(dir.resolve("automation.json"));var notices=new ArrayList<StashService.Notice>();
   AutomationContext[] observed={context(1)};StashService[] box=new StashService[1];var sent=new ArrayList<AutomationCommand>();
   box[0]=new StashService(store,new AutomationCoordinator(),()->observed[0],command->{sent.add(command);box[0].onOutgoingCommand();return true;},notices::add);
   var service=box[0];service.onSystemMessage("Your items were stashed away!",0);service.tick(0);check(sent.isEmpty(),"default off means no automatic command");
   service.setEnabled(true);service.tick(0);check(sent.isEmpty(),"enabling alone does not trigger/replay stash");
   service.onSystemMessage("Your items were stashed away!",SECOND);service.tick(SECOND);
   check(sent.size()==1&&service.active(),"actual service boundary sends once; own command event doesn't self-cancel");
   service.onOutgoingCommand();check(!service.active(),"manual outgoing command wins immediately");
   int n=notices.size();for(int i=0;i<10;i++)service.tick(2*SECOND);check(notices.size()==n,"no repeated terminal notification");
   service.setEnabled(false);service.onSystemMessage("Your items were stashed away!",3*SECOND);check(!service.active(),"disabled auto ignores fresh signals");
   check(service.manualRequest(5*SECOND),"explicit manual run allowed with auto off");service.tick(5*SECOND);check(sent.size()==2,"manual run uses same guarded sender");
   observed[0]=context(2);service.onSystemMessage("Your stash isn't holding any items or materials!",6*SECOND);check(!service.active(),"reply on different world cancels old task");
   observed[0]=context(2,AutomationContext.Blocker.OUTSIDE_SKYBLOCK);check(!service.manualRequest(7*SECOND),"no outside-server request");
   observed[0]=context(2);service.setEnabled(true);service.onSystemMessage("Your items were stashed away!",10*SECOND);service.tick(10*SECOND);
   Files.writeString(dir.resolve("automation.json"),"{\"schemaVersion\":1,\"external\":true}");
   try{service.setEnabled(false);throw new AssertionError("expected external edit protection");}catch(java.io.IOException expected){}
   check(!service.active()&&!service.autoEnabled(),"disable is safe in-session even if preference save fails");
  }finally{try(var stream=Files.walk(dir)){for(var path:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
 }
 static void preferences()throws Exception {
  Path dir=Files.createTempDirectory("kuanperion-automation-");
  try{
   Path path=dir.resolve("automation.json"),old=dir.resolve("kuanperion.properties");Files.writeString(old,"pestNotificationEnabled=true\nvisitorNotificationEnabled=true\n");byte[] legacy=Files.readAllBytes(old);
   var store=AutomationSettingsStore.load(path);
   for(var f:AutomationFeature.values())check(!store.enabled(f),"all new automation defaults off");
   check(!Files.exists(path),"load doesn't create or reset settings");
   store.setEnabled(AutomationFeature.STASH,true);store.setStashPolicy(new StashTask.Policy(4,5,50,false));
   var reopened=AutomationSettingsStore.load(path);check(reopened.enabled(AutomationFeature.STASH)&&reopened.stashPolicy().delaySeconds()==4&&!reopened.stashPolicy().onlyWhileFarming(),"settings survive restart");
   check(!reopened.enabled(AutomationFeature.PEST)&&Arrays.equals(legacy,Files.readAllBytes(old)),"Pest/Visitor notification properties independent and untouched");
   var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();json.addProperty("futureUnknown","keep");Files.writeString(path,json.toString());
   reopened=AutomationSettingsStore.load(path);reopened.setEnabled(AutomationFeature.VISITOR,true);
   check(JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("futureUnknown").getAsString().equals("keep"),"unknown fields survive save");
   Files.writeString(path,"{bad json");byte[] bad=Files.readAllBytes(path);var corrupt=AutomationSettingsStore.load(path);check(!corrupt.writable()&&!corrupt.enabled(AutomationFeature.STASH),"corrupt config fails closed");
   try{corrupt.setEnabled(AutomationFeature.STASH,true);throw new AssertionError("corrupt config overwritten");}catch(java.io.IOException expected){}
   check(Arrays.equals(bad,Files.readAllBytes(path)),"malformed bytes preserved");
   Files.writeString(path,"{\"schemaVersion\":99,\"features\":{\"stash\":true}}");var future=AutomationSettingsStore.load(path);check(!future.writable()&&!future.enabled(AutomationFeature.STASH),"future schema not interpreted or overwritten");
   Files.delete(path);var race=AutomationSettingsStore.load(path);Files.writeString(path,"{\"schemaVersion\":1,\"otherWindow\":true}");
   try{race.setEnabled(AutomationFeature.STASH,true);throw new AssertionError("external save overwritten");}catch(java.io.IOException expected){}
   check(Files.readString(path).contains("otherWindow"),"external edits not lost");
  }finally{try(var stream=Files.walk(dir)){for(var p:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
