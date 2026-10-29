#!/usr/bin/env python3
"""Execute actual flight-task finish bodies with real Farm holds and explicit failure/input boundaries."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion'
def method(s,name):
 a=s.index(' private void '+name+'(');b=s.index('{',a)+1;d=1
 while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
classes=[]
for name,path in [('Pest','pest/PestTask.java'),('Greenhouse','greenhouse/GreenhouseTask.java'),('Composter','composter/ComposterTask.java'),('Visitor','visitor/VisitorTask.java')]:
 s=(main/'automation'/path).read_text();finish=method(s,'finish')
 classes.append('static class '+name+' extends Fixture {'+finish+' void end(State terminal){finish(terminal,2);}}')
java='''package dev.kuan.kuanperion.automation;
import java.util.*;
public class CompletionBoundaryTest {
 enum State{COMPLETE,PARTIAL,CANCELLED,FAILED}static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Clear {boolean failure;void clear(){}void reset(){}void cancel(){if(failure)throw new IllegalStateException("child cleanup");}}
 static class Fixture {
  State state;Clear travel=new Clear(),trap=new Clear(),bait=new Clear(),harvests=new Clear(),submission=new Clear();Object closeTicket,target;int stops;boolean failRelease,failDispose,failAcquisition,failSacks;List<String> order=new ArrayList<>();
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->ctx,()->{},()->{stops++;order.add("stop");});AutomationSession.Handle handle=session.begin(AutomationFeature.PEST,1,1_000_000_000L);
  class Port {void cancelBaitAcquisition(){order.add("bait-acquisition");if(failAcquisition)throw new IllegalStateException("bait acquisition cleanup failed");}void cancelSacks(){order.add("sacks");if(failSacks)throw new IllegalStateException("Sacks cleanup failed");}void cancelAcquisition(){order.add("acquisition");if(failAcquisition)throw new IllegalStateException("acquisition cleanup failed");}void release(AutomationSession.Handle h){order.add("release");if(failRelease)throw new IllegalStateException("release failed");}void dispose(AutomationSession.Handle h){order.add("dispose");if(failDispose)throw new IllegalStateException("dispose failed");}void afterStop(AutomationSession.Handle h){order.add("physical");check(!session.active(),"post-stop physical boundary");}}
  Port port=new Port();void end(State terminal){throw new AssertionError();}
 }
 CLASSES
 static List<String> expected(Fixture f,String... steps){var values=new ArrayList<String>();if(f instanceof Pest)values.add("bait-acquisition");if(f instanceof Visitor||f instanceof Composter)values.add("acquisition");if(f instanceof Composter)values.add("sacks");values.addAll(List.of(steps));return values;}
 public static void main(String[]args){
  var baitFailure=new Pest();baitFailure.bait.failure=true;baitFailure.end(State.COMPLETE);check(baitFailure.state==State.FAILED&&baitFailure.stops==1&&baitFailure.order.equals(expected(baitFailure,"release","dispose","stop","physical")),"bait child cleanup failure must still release physical input before stopping original Farming");

  for(var childFailure:List.of(new Pest(),new Visitor(),new Composter())){childFailure.failAcquisition=true;childFailure.end(State.COMPLETE);check(childFailure.state==State.FAILED&&childFailure.stops==1&&childFailure.order.equals(expected(childFailure,"release","dispose","stop","physical")),"child cancellation failure must still release physical input and dispose before original Farm stop");}

  var sacksFailure=new Composter();sacksFailure.failSacks=true;sacksFailure.end(State.COMPLETE);check(sacksFailure.state==State.FAILED&&sacksFailure.order.equals(expected(sacksFailure,"release","dispose","stop","physical")),"Sacks cancellation failure still releases physical input and disposes");
  for(var factory:List.<java.util.function.Supplier<Fixture>>of(Pest::new,Greenhouse::new,Composter::new,Visitor::new)){
   var success=factory.get();success.end(State.COMPLETE);check(success.state==State.COMPLETE&&success.stops==0&&!success.session.active()&&success.order.equals(expected(success,"release","dispose")),"success disposes before finishing and has no late failure work: "+success.getClass());
   var failure=factory.get();failure.failDispose=true;failure.end(State.COMPLETE);check(failure.state==State.FAILED&&failure.stops==1&&!failure.session.active()&&failure.order.equals(expected(failure,"release","dispose","stop","physical")),"disposal failure must stop original Farming, not release then merely relabel FAILED: "+failure.getClass());
   var release=factory.get();release.failRelease=true;release.end(State.COMPLETE);check(release.state==State.FAILED&&release.stops==1&&release.order.equals(expected(release,"release","dispose","stop","physical")),"release exception still disposes and preserves post-stop physical input: "+release.getClass());
   var cancel=factory.get();cancel.end(State.CANCELLED);check(cancel.state==State.CANCELLED&&cancel.stops==1&&cancel.order.equals(expected(cancel,"release","dispose","stop","physical")),"cancel preserves terminal reason and physical input after legacy stop: "+cancel.getClass());
  }
  System.out.println("CompletionBoundaryTest: PASS (all four actual task finish bodies, real original Farm holds, pre-release disposal and failure-only post-stop physical-input order)");
 }
}'''.replace('CLASSES','\n'.join(classes))
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-completion-') as tmp:
 p=Path(tmp);(p/'CompletionBoundaryTest.java').write_text(java);sources=list((main/'automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,sources),str(p/'CompletionBoundaryTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.automation.CompletionBoundaryTest'],check=True)
