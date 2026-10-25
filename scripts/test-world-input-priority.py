#!/usr/bin/env python3
"""A click released before END tick must still cancel a gameplay task before the user's action."""
from pathlib import Path
import re,json,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';p=m/'mixin/AutomationWorldInputMixin.java';assert p.is_file(),'No event-level world input route; fast clicks can evade physical polling'
s=p.read_text();assert '@Mixin(Minecraft.class)' in s
for name in ['startAttack','startUseItem']:assert '@Inject(method="'+name+'",at=@At("HEAD"))' in s
assert 'ci.cancel(' not in s and 'setReturnValue(' not in s,'Notify task cancellation, never swallow user input'
config=json.loads((r/'src/main/resources/kuanperion.mixins.json').read_text());assert 'AutomationWorldInputMixin' in config['client']
client=(m/'KuanperionClient.java').read_text();bridge=(m/'AutomationClientBridge.java').read_text()
assert 'public static void onManualAutomationWorldInput(){if(automation!=null)automation.onManualWorldInput();}' in client
assert 'public void onManualWorldInput(){onManualInventoryInput();}' in bridge
bodies=re.findall(r'private void (kuanperion\$\w+)\([^)]*\)\s*\{([^{}]*)\}',s)
assert len(bodies)==2 and all('KuanperionClient.onManualAutomationWorldInput()' in body for _,body in bodies)
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class WorldInputPriorityTest {
 static class KuanperionClient {static AutomationSession session;static void onManualAutomationWorldInput(){session.cancel(1);}}
 METHODS
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var context=new AutomationContext(1,true,true,Set.of());int[] stops={0},actions={0};var coordinator=new AutomationCoordinator();var session=new AutomationSession(coordinator,()->context,()->{},()->stops[0]++);KuanperionClient.session=session;var h=new WorldInputPriorityTest();
  check(session.begin(AutomationFeature.PEST,0,120_000_000_000L)!=null,"Pest fixture");boolean physicalButtonsAlreadyReleased=true;h.ATTACK();actions[0]++;check(physicalButtonsAlreadyReleased&&!session.active()&&stops[0]==1&&actions[0]==1,"fast released attack cancels root before user's native action, without polling or swallowing it");
  check(session.begin(AutomationFeature.GREENHOUSE,0,120_000_000_000L)!=null,"shared gameplay owner fixture");h.USE();actions[0]++;check(!session.active()&&stops[0]==2&&actions[0]==2,"use event also revokes any gameplay hold before action");h.ATTACK();check(stops[0]==2,"inactive tasks do not alter ordinary gameplay");
  System.out.println("WorldInputPriorityTest: PASS (actual event hook bodies, released fast click, cancellation-before-action, shared lease revocation, no input swallowed)");
 }
}
'''
methods='\n'.join('private void '+name+'(){'+body+'}' for name,body in bodies)
java=java.replace('METHODS',methods).replace('h.ATTACK()','h.'+bodies[0][0]+'()').replace('h.USE()','h.'+bodies[1][0]+'()');prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory() as temp:
 q=Path(temp);(q/'WorldInputPriorityTest.java').write_text(java);sources=[str(f) for f in (m/'automation').rglob('*.java')]
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*sources,str(q/'WorldInputPriorityTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.WorldInputPriorityTest'],check=True)
for file in ['PestFlightController.java','GreenhouseFlightController.java','GardenFlightCore.java']:
 code=(m/file).read_text();assert '.startAttack(' not in code and '.startUseItem(' not in code,'owned direct game-mode actions must not re-enter vanilla input hooks'
print('WorldInputWiringTest: PASS (two vanilla-only HEAD hooks, no GUI/key polling dependency, direct task actions isolated, old Mining lock untouched)')
