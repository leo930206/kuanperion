#!/usr/bin/env python3
"""Actual command adapter guards + closed native wiring; no server travel claim."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
s=(m/'AutomationClientBridge.java').read_text();a=s.index(' private boolean sendGreenhouseTravel(');b=s.index('{',a)+1;depth=1
while depth:
 depth+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b]
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;import java.util.function.*;
public final class GreenhouseTravelAdapterTest {
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{});
 boolean ownGameplayCommand,observedGameplayCommand;List<StashService.Notice> notices=new ArrayList<>();Consumer<StashService.Notice> feedback=notices::add;
 static GreenhouseTravelAdapterTest current;
 static class Connection {List<String> commands=new ArrayList<>();boolean observed=true;void sendCommand(String s){commands.add(s);if(observed)current.observedGameplayCommand=current.ownGameplayCommand;}}
 static class Player {Connection connection=new Connection();}
 static class Minecraft {static final Minecraft M=new Minecraft();Player player=new Player();Object level=new Object();boolean same=true;static Minecraft getInstance(){return M;}boolean isSameThread(){return same;}}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var t=new GreenhouseTravelAdapterTest();current=t;var mc=Minecraft.getInstance();var h=t.session.begin(AutomationFeature.GREENHOUSE,0,120_000_000_000L);
  check(t.sendGreenhouseTravel(h,PestPlot.of(3),1)&&mc.player.connection.commands.equals(List.of("tptoplot 3"))&&t.notices.size()==1&&!t.ownGameplayCommand,"actual observed typed command and feedback; guard flag scoped");
  mc.player.connection.observed=false;check(!t.sendGreenhouseTravel(h,PestPlot.of(0),2)&&t.notices.size()==1&&!t.ownGameplayCommand,"outgoing event absent never treated as acknowledged dispatch");mc.player.connection.observed=true;
  int before=mc.player.connection.commands.size();mc.same=false;check(!t.sendGreenhouseTravel(h,PestPlot.of(1),3),"client thread only");mc.same=true;
  t.ctx=new AutomationContext(1,true,false,Set.of());check(!t.sendGreenhouseTravel(h,PestPlot.of(1),3),"native Garden proof mandatory");t.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MINING));check(!t.sendGreenhouseTravel(h,PestPlot.of(1),3),"manual Mining priority");
  t.ctx=new AutomationContext(1,true,true,Set.of());check(!t.sendGreenhouseTravel(h,null,3),"no arbitrary destination string");t.session.invalidate();check(!t.sendGreenhouseTravel(h,PestPlot.of(1),3)&&mc.player.connection.commands.size()==before,"revoked ownership no send");
  var other=t.session.begin(AutomationFeature.AUTO_SELL,4,120_000_000_000L);check(!t.sendGreenhouseTravel(other,PestPlot.of(1),5),"another owner cannot use Pest travel authority");
  System.out.println("GreenhouseTravelAdapterTest: PASS (actual production method; typed literals, event feedback, lease/Garden/thread/Mining guards)");
 }
}
'''.replace('METHOD',method)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
with tempfile.TemporaryDirectory() as temp:
 p=Path(temp);(p/'GreenhouseTravelAdapterTest.java').write_text(java)
 sources=[str(f) for f in (m/'automation').rglob('*.java')]
 gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',temp,*sources,str(p/'GreenhouseTravelAdapterTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',f'{temp}:{gson}','dev.kuan.kuanperion.GreenhouseTravelAdapterTest'],check=True)

d=(m/'GreenhouseFlightController.java').read_text();feature=(m/'automation/AutomationFeature.java').read_text();task=(m/'automation/greenhouse/GreenhouseTask.java').read_text()
assert 'new GardenFlightCore(coordinator,session,observe,AutomationFeature.GREENHOUSE)' in d
assert 'Resource.ATTACK' in d and '.startDestroyBlock(hit.getBlockPos(),hit.getDirection())' in d and '.attack(p,stand)' in d
assert 'GreenhouseGameView.target(mc(),expected)' in d and 'target.equals(live.current())' in d and 'PestIdentity.sameTool(tool.item(),' in d
assert '2.75*2.75' in d and 'flight.loaded(' in d and 'flight.clear(' in d and 'ClipContext.Block.OUTLINE' in d
assert 'harvests.after(stamp,now)' in task and 'policy.timeoutSeconds()-45' in task and 'state=State.WAITING_HARVEST' in task
for forbidden in ['.setPos(','.setDeltaMovement(','.noPhysics','mayfly=','mayfly =','HttpClient','.sendCommand(','.useItem(']:assert forbidden not in d,forbidden
assert 'greenhouse.tick(now)' in s and 'this::sendGreenhouseTravel' in s
for name in ['onManualInventoryInput','onOutgoingCommand','worldChanged','invalidateFarmSession','onManualLook','shutdown']:
 start=s.index('public void '+name+'(');end=s.index('{',start)+1;depth=1
 while depth:
  depth+=(s[end]=='{')-(s[end]=='}');end+=1
 assert 'greenhouse.cancel(' in s[start:end] and 'AutomationStops.run(' in s[start:end],name
print('GreenhouseDriverWiringTest: PASS (fixed-owner ATTACK only, live exact tool/target + loaded reach/clip/body, one attempt/server-proof, native manual/world/Farm cancellation)')
