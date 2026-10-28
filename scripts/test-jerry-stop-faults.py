#!/usr/bin/env python3
"""Actual Bridge Jerry-stop body + real session/hold with faulting service actors; no live/server effects."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
bridge=(main/'AutomationClientBridge.java').read_text()
match=re.search(r' public void suspendForJerry\(\)\{',bridge);assert match
end=match.end();depth=1
while depth:
    depth+=(bridge[end]=='{')-(bridge[end]=='}');end+=1
method=bridge[match.start():end]
java=r'''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class JerryStopFaultsTest {
 static final List<String> NAMES=List.of("pest","greenhouse","composter","visitor","supercraft","book","autoSell","junk","stash");
 static final List<AutomationFeature> FEATURES=List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.SUPERCRAFT,AutomationFeature.BOOK_COMBINE,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK);
 final List<String> calls=new ArrayList<>();final List<RuntimeException> errors=new ArrayList<>();final Set<String> faults=new HashSet<>();int pauses,stops;
 AutomationContext context=new AutomationContext(1,true,true,Set.of());
 final RuntimeException stopError=new IllegalStateException("native stop failed");
 final AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->context,()->pauses++,()->{stops++;throw stopError;});
 AutomationSession.Handle active;String running="";
 final class Service {
  final String name;boolean enabled=true,pending=true;int invocations;
  final RuntimeException failure=new IllegalStateException("failed "+System.identityHashCode(this));
  Service(String name){this.name=name;}
  void suspend(long now){suspend("Jerry");}
  void suspend(String reason){
   invocations++;calls.add(name);enabled=false;pending=false;
   if(name.equals(running)){session.finish(active,false,System.nanoTime());return;}
   if(faults.contains(name)){errors.add(failure);throw failure;}
  }
 }
 final Service pest=new Service("pest"),greenhouse=new Service("greenhouse"),composter=new Service("composter"),visitor=new Service("visitor"),supercraft=new Service("supercraft"),book=new Service("book"),autoSell=new Service("autoSell"),junk=new Service("junk"),stash=new Service("stash");
 List<Service> services(){return List.of(pest,greenhouse,composter,visitor,supercraft,book,autoSell,junk,stash);}
 METHOD
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 void stop(){
  RuntimeException seen=null;try{suspendForJerry();}catch(RuntimeException failure){seen=failure;}
  check(calls.equals(NAMES),"one failed service must not leave later services or queued intents running: "+calls);
  for(var service:services())check(service.invocations==1&&!service.pending&&!service.enabled,"each actor suspended exactly once: "+service.name);
  if(!running.isEmpty())check(!session.active()&&!session.holdsFarming()&&stops==1&&seen==stopError,"actual original root/hold must be released and stop failure remain visible");
  else if(!faults.isEmpty()){check(seen==errors.getFirst(),"first service failure preserved exactly");check(Arrays.asList(seen.getSuppressed()).equals(errors.subList(1,errors.size())),"later failures are retained, not swallowed/replacing first");}
  else check(seen==null,"healthy global stop unchanged");
 }
 public static void main(String[] args){
  if(args.length==0)throw new AssertionError("explicit scenario required");
  var h=new JerryStopFaultsTest();String mode=args[0];
  if(mode.equals("all"))h.faults.addAll(NAMES);
  else if(mode.startsWith("root:")){h.running=mode.substring(5);int index=NAMES.indexOf(h.running);h.active=h.session.begin(FEATURES.get(index),System.nanoTime(),60_000_000_000L);check(h.active!=null&&h.session.holdsFarming()&&h.pauses==1,"real original Farm hold fixture");}
  else if(!mode.equals("healthy"))h.faults.add(mode);
  h.stop();System.out.println("JerryStopFaultsTest: PASS "+mode);
 }
}'''.replace('METHOD',method)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-jerry-stop-faults-') as tmp:
    p=Path(tmp);(p/'JerryStopFaultsTest.java').write_text(java)
    subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',tmp,*map(str,(main/'automation').rglob('*.java')),str(p/'JerryStopFaultsTest.java')],check=True)
    failures=[]
    scenarios=['healthy',*['pest','greenhouse','composter','visitor','supercraft','book','autoSell','junk','stash'],'all',*[f'root:{name}' for name in ['pest','greenhouse','composter','visitor','supercraft','book','autoSell','junk']]]
    for scenario in scenarios:
        result=subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.JerryStopFaultsTest',scenario])
        if result.returncode:failures.append(scenario)
    if failures:raise AssertionError('Jerry stop fault matrix failed: '+', '.join(failures))
print('JerryStopFaultsMatrix: PASS (actual Bridge body, all nine implemented service actors, real original-session stop faults, continued suspension and preserved errors; no native/live proof)')
