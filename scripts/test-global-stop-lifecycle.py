#!/usr/bin/env python3
"""Execute real Bridge stop bodies + original-session ownership using controlled actors; no native/live claim."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
bridge=(main/'AutomationClientBridge.java').read_text()
names=['onManualLook','invalidateFarmSession','onOutgoingCommand','onManualWorldInput','onManualInventoryInput','shutdown','worldChanged']
def extract(name):
    match=re.search(r' public void '+name+r'\([^)]*\)\{',bridge);assert match,name
    end=match.end();depth=1
    while depth:
        depth+=(bridge[end]=='{')-(bridge[end]=='}');end+=1
    return bridge[match.start():end]
java=r'''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class GlobalStopLifecycleTest {
 static final List<String> JOBS=List.of("pest","spray","greenhouse","composter","visitor","supercraft","book","pets","autoSell","junk");
 static final List<AutomationFeature> FEATURES=List.of(AutomationFeature.PEST,AutomationFeature.PEST_SPRAY,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.SUPERCRAFT,AutomationFeature.BOOK_COMBINE,AutomationFeature.PET_SELL,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK);
 final List<String> calls=new ArrayList<>();final List<RuntimeException> errors=new ArrayList<>();final Set<String> faults=new HashSet<>();final Map<String,RuntimeException> failures=new HashMap<>();
 final RuntimeException stopFailure=new IllegalStateException("native original-Farm stop failed");int pauses,stops,menuCloses;boolean stopThrows,ownGameplayCommand,observedGameplayCommand;String running="";
 AutomationContext context=new AutomationContext(1,true,true,Set.of());AutomationSession.Handle handle;
 final AutomationSession raw=new AutomationSession(new AutomationCoordinator(),()->context,()->pauses++,()->{stops++;if(stopThrows){errors.add(stopFailure);throw stopFailure;}context=new AutomationContext(1,false,true,Set.of());});
 void step(String name,Runnable body){calls.add(name);body.run();if(faults.contains(name)){var error=failures.computeIfAbsent(name,k->new IllegalStateException(k));errors.add(error);throw error;}}
 final class Session {
  void cancel(long now){step("session.cancel",()->raw.cancel(System.nanoTime()));}
  void invalidate(){step("session.invalidate",raw::invalidate);}
 }
 final Session session=new Session();
 final class Service {
  final String name;boolean pending=true;
  Service(String name){this.name=name;}
  void cancel(long now){step(name,()->{pending=false;if(name.equals(running)&&raw.isCurrent(handle)){if(name.equals("autoSell")||name.equals("junk"))menuCloses++;raw.finish(handle,false,System.nanoTime());}});}
  void cancel(String reason){step("stash",()->pending=false);}
  void onOutgoingCommand(){step("stash.outgoing",()->pending=false);}
 }
 final Service pest=new Service("pest"),spray=new Service("spray"),greenhouse=new Service("greenhouse"),composter=new Service("composter"),visitor=new Service("visitor"),supercraft=new Service("supercraft"),book=new Service("book"),pets=new Service("pets"),autoSell=new Service("autoSell"),junk=new Service("junk"),stash=new Service("stash");
 List<Service> services(){return List.of(pest,spray,greenhouse,composter,visitor,supercraft,book,pets,autoSell,junk);}
 final class Data {final String name;boolean cleared;Data(String name){this.name=name;}void clear(){step(name,()->cleared=true);}void shutdownData(){clear();}}
 final Data sprayController=new Data("sprayData"),visitorFlight=new Data("visitorData"),supercraftMenu=new Data("supercraftData"),receipts=new Data("items"),positions=new Data("positions"),sprayTabs=new Data("sprayTabs"),cursors=new Data("cursor"),harvests=new Data("harvests");
 Object petClose,visitorClose=new Object(),trapClose=new Object();
 static final class Minecraft {static final Minecraft INSTANCE=new Minecraft();final Gui gui=new Gui();static Minecraft getInstance(){return INSTANCE;}}
 static final class Gui {Object screen;Object screen(){return screen;}}
 METHODS
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static List<String> expected(String hook){
  var normal=new ArrayList<>(List.of("pest","spray","greenhouse","composter","visitor","supercraft","book","pets","session.cancel","autoSell","junk"));
  return switch(hook){
   case "onManualLook","onManualInventoryInput","onManualWorldInput"->normal;
   case "onOutgoingCommand"->{normal.add(0,"stash.outgoing");yield normal;}
   case "invalidateFarmSession"->{var out=new ArrayList<String>();out.add("session.invalidate");out.addAll(JOBS);yield out;}
   case "worldChanged"->{var out=new ArrayList<String>(List.of("stash","session.invalidate"));out.addAll(JOBS);out.addAll(List.of("sprayData","visitorData","supercraftData","items","positions","sprayTabs","cursor","harvests"));yield out;}
   case "shutdown"->List.of("book","pets","supercraft","pest","spray","greenhouse","composter","visitor","session.cancel","autoSell","junk","stash","session.invalidate","sprayData","visitorData","supercraftData","items","positions","sprayTabs","cursor","harvests");
   default->throw new AssertionError(hook);
  };
 }
 void invoke(String hook){switch(hook){case "onManualLook"->onManualLook();case "onManualInventoryInput"->onManualInventoryInput();case "onManualWorldInput"->onManualWorldInput();case "onOutgoingCommand"->onOutgoingCommand();case "invalidateFarmSession"->invalidateFarmSession();case "worldChanged"->worldChanged();case "shutdown"->shutdown();default->throw new AssertionError(hook);}}
 void start(String owner){running=owner;handle=raw.begin(FEATURES.get(JOBS.indexOf(owner)),System.nanoTime(),60_000_000_000L);check(handle!=null&&raw.holdsFarming()&&pauses==1,"original real Farming hold fixture");}
 void run(String hook,String fault,String owner){
  Minecraft.INSTANCE.gui.screen=null;if(!owner.isEmpty()){start(owner);stopThrows=true;}
  if(fault.equals("all"))faults.addAll(expected(hook));else if(!fault.isEmpty())faults.add(fault);
  RuntimeException seen=null;try{invoke(hook);}catch(RuntimeException failure){seen=failure;}
  check(calls.equals(expected(hook)),hook+" skips/duplicates later callbacks after "+fault+" / "+owner+": "+calls);
  for(var service:services())check(!service.pending,"stale manual intent: "+hook+" / "+service.name);
  if(hook.equals("onOutgoingCommand")||hook.equals("worldChanged")||hook.equals("shutdown"))check(!stash.pending,"Stash stop must not be skipped");
  check(!raw.active()&&!raw.holdsFarming()&&menuCloses==0,"never retain a root or close a manually taken-over Trade menu");
  if(!owner.isEmpty())check(stops==(hook.equals("worldChanged")||hook.equals("invalidateFarmSession")?0:1),"original stop invoked at most once; invalidation never rearms Farm");
  if(errors.isEmpty())check(seen==null,"healthy stop must not throw");else check(seen==errors.getFirst()&&Arrays.asList(seen.getSuppressed()).equals(errors.subList(1,errors.size())),"preserve exact first/later failures without swallowing or replacing");
  if(hook.equals("worldChanged")||hook.equals("shutdown")){
   check(sprayController.cleared&&visitorFlight.cleared&&supercraftMenu.cleared&&receipts.cleared&&positions.cleared&&sprayTabs.cleared&&cursors.cleared&&harvests.cleared,"data/receipt clearing survives earlier failures");
   check(visitorClose==null&&trapClose==null,"both stale close tickets cleared");
  }
 }
 static int count,failed;static final List<String> reasons=new ArrayList<>();
 static void scenario(String hook,String fault,String owner){count++;try{new GlobalStopLifecycleTest().run(hook,fault,owner);}catch(AssertionError e){failed++;if(reasons.size()<8)reasons.add(e.getMessage());}}
 public static void main(String[] args){
  var hooks=List.of("onManualLook","onManualInventoryInput","onManualWorldInput","onOutgoingCommand","invalidateFarmSession","worldChanged","shutdown");
  for(String hook:hooks){scenario(hook,"","");for(String fault:expected(hook))scenario(hook,fault,"");scenario(hook,"all","");for(String owner:JOBS){scenario(hook,"",owner);scenario(hook,"all",owner);}}
  var trusted=new GlobalStopLifecycleTest();trusted.start("book");trusted.ownGameplayCommand=true;trusted.onOutgoingCommand();check(trusted.observedGameplayCommand&&trusted.calls.isEmpty()&&trusted.raw.isCurrent(trusted.handle)&&trusted.services().stream().allMatch(s->s.pending),"trusted typed sender path unchanged");trusted.raw.invalidate();count++;
  var screen=new GlobalStopLifecycleTest();screen.start("pest");Minecraft.INSTANCE.gui.screen=new Object();screen.onManualLook();check(screen.calls.isEmpty()&&screen.raw.isCurrent(screen.handle),"mouse in a known-open screen is not world look takeover");Minecraft.INSTANCE.gui.screen=null;screen.raw.invalidate();count++;
  for(String hook:List.of("worldChanged","shutdown")){var twice=new GlobalStopLifecycleTest();twice.run(hook,"","");twice.calls.clear();twice.invoke(hook);check(twice.calls.equals(expected(hook))&&!twice.raw.active()&&!twice.raw.holdsFarming()&&twice.stops==0&&twice.menuCloses==0,"repeated cleanup does not invent work/holds/stops or touch menus");count++;}
  if(failed>0)throw new AssertionError("Global stop failed "+failed+" of "+count+" scenarios: "+reasons);
  System.out.println("GlobalStopLifecycleTest: PASS "+count+" actual Bridge-body / real session actor scenarios; no native/live claim");
 }
}'''.replace('METHODS','\n'.join(map(extract,names)))
client=(main/'KuanperionClient.java').read_text()
needle='ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {'
start=client.index(needle)+len(needle);end=start;depth=1
while depth:
    depth+=(client[end]=='{')-(client[end]=='}');end+=1
callback=client[start:end-1].replace('dev.kuan.kuanperion.renderer.NanoVGManager','Renderer')
render=r'''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class StopRendererCallbackTest {
 static int stopped,destroyed;static RuntimeException stopFailure,rendererFailure;
 static final class Automation {void shutdown(){stopped++;if(stopFailure!=null)throw stopFailure;}}
 static final class Renderer {static void destroy(){destroyed++;if(rendererFailure!=null)throw rendererFailure;}}
 final Automation automation=new Automation();void stop(){CALLBACK}
 public static void main(String[] args){
  for(int mode=0;mode<4;mode++){
   stopped=destroyed=0;stopFailure=(mode&1)!=0?new IllegalStateException("automation"):null;rendererFailure=(mode&2)!=0?new IllegalArgumentException("renderer"):null;
   RuntimeException seen=null;try{new StopRendererCallbackTest().stop();}catch(RuntimeException failure){seen=failure;}
   if(stopped!=1||destroyed!=1)throw new AssertionError("renderer cleanup skipped after automation failure; mode="+mode);
   RuntimeException first=stopFailure!=null?stopFailure:rendererFailure;
   if(seen!=first||mode==3&&!Arrays.equals(seen.getSuppressed(),new Throwable[]{rendererFailure}))throw new AssertionError("exact original/later cleanup errors lost");
  }
  System.out.println("StopRendererCallbackTest: PASS actual ClientLifecycleEvents closure, four cleanup/error combinations; no GUI/native renderer claim");
 }
}'''.replace('CALLBACK',callback)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-global-stop-') as tmp:
    p=Path(tmp);(p/'GlobalStopLifecycleTest.java').write_text(java);(p/'StopRendererCallbackTest.java').write_text(render)
    subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',tmp,*map(str,(main/'automation').rglob('*.java')),str(p/'GlobalStopLifecycleTest.java'),str(p/'StopRendererCallbackTest.java')],check=True)
    failures=[]
    for case in ['GlobalStopLifecycleTest','StopRendererCallbackTest']:
        if subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.'+case]).returncode:failures.append(case)
    if failures:raise AssertionError('Lifecycle stop behavior failed: '+', '.join(failures))
