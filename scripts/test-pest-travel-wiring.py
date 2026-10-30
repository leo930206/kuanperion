#!/usr/bin/env python3
"""Actual command adapter guards + closed native wiring; no server travel claim."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
s=(m/'AutomationClientBridge.java').read_text();a=s.index(' private boolean sendPestTravel(');b=s.index('{',a)+1;depth=1
while depth:
 depth+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b]
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.*;
public final class PestTravelAdapterTest {
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{});
 boolean ownGameplayCommand,observedGameplayCommand;List<StashService.Notice> notices=new ArrayList<>();Consumer<StashService.Notice> feedback=notices::add;
 static PestTravelAdapterTest current;
 static class Connection {List<String> commands=new ArrayList<>();boolean observed=true;void sendCommand(String s){commands.add(s);if(observed)current.observedGameplayCommand=current.ownGameplayCommand;}}
 static class Player {Connection connection=new Connection();}
 static class Minecraft {static final Minecraft M=new Minecraft();Player player=new Player();Object level=new Object();boolean same=true;static Minecraft getInstance(){return M;}boolean isSameThread(){return same;}}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var t=new PestTravelAdapterTest();current=t;var mc=Minecraft.getInstance();var h=t.session.begin(AutomationFeature.PEST,0,120_000_000_000L);
  check(t.sendPestTravel(h,PestPlot.of(3),1)&&mc.player.connection.commands.equals(List.of("tptoplot 3"))&&t.notices.size()==1&&!t.ownGameplayCommand,"actual observed typed command and feedback; guard flag scoped");
  mc.player.connection.observed=false;check(!t.sendPestTravel(h,PestPlot.of(0),2)&&t.notices.size()==1&&!t.ownGameplayCommand,"outgoing event absent never treated as acknowledged dispatch");mc.player.connection.observed=true;
  int before=mc.player.connection.commands.size();mc.same=false;check(!t.sendPestTravel(h,PestPlot.of(1),3),"client thread only");mc.same=true;
  t.ctx=new AutomationContext(1,true,false,Set.of());check(!t.sendPestTravel(h,PestPlot.of(1),3),"native Garden proof mandatory");t.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MINING));check(!t.sendPestTravel(h,PestPlot.of(1),3),"manual Mining priority");
  t.ctx=new AutomationContext(1,true,true,Set.of());check(!t.sendPestTravel(h,null,3),"no arbitrary destination string");t.session.invalidate();check(!t.sendPestTravel(h,PestPlot.of(1),3)&&mc.player.connection.commands.size()==before,"revoked ownership no send");
  var other=t.session.begin(AutomationFeature.AUTO_SELL,4,120_000_000_000L);check(!t.sendPestTravel(other,PestPlot.of(1),5),"another owner cannot use Pest travel authority");
  System.out.println("PestTravelAdapterTest: PASS (actual production method; typed literals, event feedback, lease/Garden/thread/Mining guards)");
 }
}
'''.replace('METHOD',method)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
with tempfile.TemporaryDirectory() as temp:
 p=Path(temp);(p/'PestTravelAdapterTest.java').write_text(java)
 sources=[str(f) for f in (m/'automation').rglob('*.java')]
 gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',temp,*sources,str(p/'PestTravelAdapterTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',f'{temp}:{gson}','dev.kuan.kuanperion.PestTravelAdapterTest'],check=True)
d=(m/'PestFlightController.java').read_text();task=(m/'automation/pest/PestTask.java').read_text();ui=(m/'ui/PestModuleSettings.java').read_text()
assert 'this::sendPestTravel' in s and 'new PestService(settings,session,flight,positions,' in s
assert 'facts.tab().plots(),facts.tab().plotsKnown()' in d
assert 'allowed(h,now,AutomationCoordinator.Resource.COMMAND)&&travel.send(h,plot,now)' in d
assert 'travel.arrival(handle,now)' in task and 'visited.size()>=policy.travelPlots()' in task
assert 'withTravelPlots(Math.round(value))' in ui
for line in ui.splitlines():
 if 'new PestPolicy(' in line:assert 'p.travelPlots()' in line and 'p.trapPlot()' in line and 'p.barn()' in line,'other policy edits must not erase explicit travel/trap authority'
print('PestTravelWiringTest: PASS (native guarded adapter + shared server receipt cache, observed plots, explicit persistent opt-in, bounded visits)')
