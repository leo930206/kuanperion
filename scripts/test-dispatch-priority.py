#!/usr/bin/env python3
"""Actual idle dispatch/readiness bodies + real session; controlled job actors, not native-chain proof."""
from pathlib import Path
import re,subprocess,tempfile,sys
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';source=(Path(sys.argv[1]) if len(sys.argv)>1 else m/'AutomationClientBridge.java').read_text()
def body(name):
 match=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',source);assert match,name
 a=match.start();b=source.index('{',a)+1;depth=1
 while depth:depth+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods=body('tick')+'\nprivate AutomationFeature manualOwner(){return null;}\nprivate boolean screenReady(AutomationFeature f){return Minecraft.getInstance().gui.screen()==null;}' if len(sys.argv)>1 else '\n'.join(body(n) for n in ['manualOwner','screenReady','tick'])
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class DispatchPriorityTest {
 boolean farming=true;int pauses,stops;final List<AutomationFeature> starts=new ArrayList<>();final AutomationCoordinator coordinator=new AutomationCoordinator();
 AutomationContext context(){return new AutomationContext(1,farming,true,Set.of());}
 final AutomationSession session=new AutomationSession(coordinator,this::context,()->pauses++,()->{stops++;farming=false;});
 final class Work {final AutomationFeature feature;boolean pending,due=true,complete;int ticks;Runnable afterComplete=()->{};AutomationSession.Handle handle;
 Work(AutomationFeature f){feature=f;}boolean pending(){return pending;}boolean active(){return handle!=null;}
 void tick(long now){ticks++;if(feature==AutomationFeature.STASH)return;
  if(handle!=null){if(complete){session.finish(handle,true,now);handle=null;afterComplete.run();}return;}
  if(pending){if(session.active()){pending=false;return;}if(!screenReady(feature))return;pending=false;start(now,true);return;}
  if(!due||!farming||!screenReady(feature))return;start(now,false);
 }
 void start(long now,boolean manual){var h=session.begin(feature,now,60_000_000_000L);if(h==null)return;handle=h;due=false;starts.add(feature);}
 }
 Work pest=new Work(AutomationFeature.PEST),greenhouse=new Work(AutomationFeature.GREENHOUSE),composter=new Work(AutomationFeature.COMPOSTER),supercraft=new Work(AutomationFeature.SUPERCRAFT),visitor=new Work(AutomationFeature.VISITOR),book=new Work(AutomationFeature.BOOK_COMBINE),pets=new Work(AutomationFeature.PET_SELL),autoSell=new Work(AutomationFeature.AUTO_SELL),junk=new Work(AutomationFeature.JUNK),stash=new Work(AutomationFeature.STASH);
 static class Gui {Object screen;Object screen(){return screen;}}static class Minecraft{static final Minecraft I=new Minecraft();final Gui gui=new Gui();static Minecraft getInstance(){return I;}}
 METHODS
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  var h=new DispatchPriorityTest();h.autoSell.pending=true;h.tick();check(h.starts.equals(List.of(AutomationFeature.AUTO_SELL))&&h.pauses==1&&h.stash.ticks==0,"explicit lower-priority Trade request beats every automatic timer; Stash cannot cancel pending intent via outgoing event");
  h=new DispatchPriorityTest();h.book.pending=true;Minecraft.I.gui.screen=new Object();h.tick();check(h.starts.isEmpty()&&h.pauses==0,"settings-open pending request stays inert");Minecraft.I.gui.screen=null;h.tick();check(h.starts.equals(List.of(AutomationFeature.BOOK_COMBINE)),"closing UI dispatches intended Book, not an earlier automatic Pest");
  h=new DispatchPriorityTest();h.tick();h.pest.complete=true;h.tick();h.greenhouse.complete=true;h.tick();h.composter.complete=true;h.tick();h.supercraft.complete=true;h.tick();h.visitor.complete=true;h.tick();h.book.complete=true;h.tick();h.pets.complete=true;h.tick();h.autoSell.complete=true;h.tick();
  check(h.starts.equals(List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.SUPERCRAFT,AutomationFeature.VISITOR,AutomationFeature.BOOK_COMBINE,AutomationFeature.PET_SELL,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK)),"due order honors approved maintenance/Supercraft before Visitor, single owner each step");check(h.pauses==9&&h.stops==0&&h.farming,"success keeps original user Farm flag without arming or cross-cancelling");
  h=new DispatchPriorityTest();h.book.pending=true;var original=h.session.begin(AutomationFeature.PEST,System.nanoTime(),60_000_000_000L);h.tick();check(h.session.isCurrent(original)&&h.starts.isEmpty()&&!h.book.pending&&h.pauses==1,"pending intent cannot preempt a live root; competing request invalidates without replay");
  h=new DispatchPriorityTest();h.tick();h.session.invalidate();h.farming=false;h.pest.complete=true;h.tick();check(h.starts.equals(List.of(AutomationFeature.PEST))&&h.pauses==1&&!h.farming,"invalidated original Farm cannot resume or launch automatic followups");
  for(var feature:List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.SUPERCRAFT,AutomationFeature.VISITOR,AutomationFeature.BOOK_COMBINE,AutomationFeature.PET_SELL,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK)){
   h=new DispatchPriorityTest();var jobs=List.of(h.pest,h.greenhouse,h.composter,h.supercraft,h.visitor,h.book,h.pets,h.autoSell,h.junk);
   var running=jobs.stream().filter(w->w.feature==feature).findFirst().orElseThrow();running.start(System.nanoTime(),false);h.starts.clear();
   var next=feature==AutomationFeature.PEST?h.greenhouse:h.pest;for(var work:jobs)work.due=work==next;
   h.tick();check(h.starts.isEmpty()&&h.session.isCurrent(running.handle)&&h.session.holdsFarming(),"live owner cannot be preempted by earlier due job: "+feature);check(jobs.stream().allMatch(w->w.ticks==1),"each service ticks exactly once while incumbent remains active: "+feature);
   for(var work:jobs)work.ticks=0;running.complete=true;h.tick();
   check(h.starts.equals(List.of(next.feature))&&h.session.isCurrent(next.handle)&&h.session.holdsFarming(),"completed owner must hand off to already-due job before returning to legacy tick: "+feature);
   check(jobs.stream().allMatch(w->w.ticks==1)&&h.stash.ticks==2,"each service ticks exactly once even when owner completes: "+feature);
   check(h.pauses==2&&h.stops==0&&h.farming&&running.handle==null,"original manual Farm remains armed and exclusively held without restart: "+feature);
  }
  h=new DispatchPriorityTest();h.visitor.start(System.nanoTime(),false);h.visitor.complete=true;h.autoSell.pending=true;h.starts.clear();h.tick();
  check(h.starts.equals(List.of(AutomationFeature.AUTO_SELL))&&h.stash.ticks==0,"manual intent still beats all due timers after incumbent completes; Stash stays deferred");
  h=new DispatchPriorityTest();h.visitor.start(System.nanoTime(),false);h.visitor.complete=true;for(var work:List.of(h.pest,h.greenhouse,h.composter,h.supercraft,h.book,h.pets,h.autoSell,h.junk))work.due=false;h.starts.clear();h.tick();
  check(h.starts.isEmpty()&&!h.session.active()&&!h.session.holdsFarming()&&h.farming&&h.pauses==1&&h.stops==0,"no due work releases original Farm, not a forced followup queue");
  h=new DispatchPriorityTest();h.visitor.start(System.nanoTime(),false);h.visitor.complete=true;h.starts.clear();var stopped=h;h.visitor.afterComplete=()->{stopped.session.invalidate();stopped.farming=false;};h.tick();
  check(h.starts.isEmpty()&&!h.session.active()&&!h.farming&&h.pauses==1,"stop or new Farm invalidation in completion feedback cannot replay old followups");
  h=new DispatchPriorityTest();h.visitor.start(System.nanoTime(),false);h.visitor.complete=true;h.starts.clear();h.visitor.afterComplete=()->{Minecraft.I.gui.screen=new Object();};h.tick();
  check(h.starts.isEmpty()&&!h.session.active()&&h.farming,"new settings screen in completion callback wins over due work");Minecraft.I.gui.screen=null;
  h=new DispatchPriorityTest();h.visitor.start(System.nanoTime(),false);h.visitor.complete=true;h.starts.clear();var failure=new IllegalStateException("completion callback");h.visitor.afterComplete=()->{throw failure;};RuntimeException seen=null;try{h.tick();}catch(RuntimeException caught){seen=caught;}
  check(seen==failure&&h.starts.isEmpty()&&h.pest.ticks==0,"failed incumbent callback stays visible and cannot launch another task in that pass");
  System.out.println("DispatchPriorityTest: PASS (actual bridge bodies, real exclusive session, idle manual priority/Stash isolation/approved due order/nine-owner same-pass handoff; actors not native workflows)");
 }
}'''.replace('METHODS',methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
with tempfile.TemporaryDirectory(prefix='kuanperion-dispatch-') as tmp:
 p=Path(tmp);(p/'DispatchPriorityTest.java').write_text(java)
 files=[m/'automation'/(n+'.java') for n in ['AutomationSession','AutomationCoordinator','AutomationContext','AutomationFeature','FarmingHandoff','MenuContract','MenuSnapshot','ItemSnapshot']]
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,*map(str,files),str(p/'DispatchPriorityTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'dev.kuan.kuanperion.DispatchPriorityTest'],check=True)
for feature,name in [('PEST','pest'),('GREENHOUSE','greenhouse'),('COMPOSTER','composter'),('SUPERCRAFT','supercraft'),('VISITOR','visitor'),('BOOK_COMBINE','book'),('PET_SELL','pets'),('AUTO_SELL','autoSell'),('JUNK','junk')]:
 assert f'()->screenReady(AutomationFeature.{feature})' in source,name
print('DispatchReadinessWiring: PASS (all nine gameplay services use idle intent arbitration; Stash remains command-only)')
