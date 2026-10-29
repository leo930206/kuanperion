#!/usr/bin/env python3
"""Actual onClose hook body + real root; vanilla closure is not suppressed. No client/server."""
from pathlib import Path
import json,re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent
p=r/'src/main/java/dev/kuan/kuanperion/mixin/AutomationContainerCloseMixin.java'
s=p.read_text();assert '@Mixin(AbstractContainerScreen.class)' in s and re.search(r'@Inject\(method="onClose",at=@At\("HEAD"\)\)',s)
assert 'cancellable' not in s and 'cancel()' not in s
assert 'AutomationContainerCloseMixin' in json.loads((r/'src/main/resources/kuanperion.mixins.json').read_text())['client']
a=s.index(' private void kuanperion$close(');b=s.index('{',a)+1;d=1
while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b]
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public class ContainerClosePriorityTest {
 static AutomationSession session;static AutomationSession.Handle original;static int notices,stops;static boolean owned;
 static class CallbackInfo{}static class AutomationMenuActions{static boolean ownsInventoryInput(){return owned;}}
 static class KuanperionClient{static void onManualAutomationInventoryInput(){notices++;session.cancel(2);}}
 METHOD
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void root(){session=new AutomationSession(new AutomationCoordinator(),()->new AutomationContext(1,true,true,Set.of()),()->{},()->stops++);original=session.begin(AutomationFeature.COMPOSTER,0,120_000_000_000L);}
 public static void main(String[]args){var t=new ContainerClosePriorityTest();root();t.kuanperion$close(new CallbackInfo());check(!session.isCurrent(original)&&stops==1&&notices==1,"manual/other-mod close revokes original lease before vanilla can close or reopen");root();owned=true;t.kuanperion$close(new CallbackInfo());check(session.isCurrent(original)&&notices==1,"explicit owned native input remains scoped");owned=false;t.kuanperion$close(new CallbackInfo());check(!session.active()&&stops==2,"user input resumes priority immediately after owned scope");System.out.println("ContainerClosePriorityTest: PASS actual onClose HEAD body, real original lease, vanilla remains uncancelled; no applied-Mixin claim");}
}'''.replace('METHOD',method)
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-container-close-') as tmp:
 f=Path(tmp)/'ContainerClosePriorityTest.java';f.write_text(java);sources=list((r/'src/main/java/dev/kuan/kuanperion/automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,sources),str(f)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.ContainerClosePriorityTest'],check=True)
# Pinned native bytecode confirms both Escape (Screen) and inventory-key (container) use onClose.
cp=str(p/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar')
for cls in ['net.minecraft.client.gui.screens.Screen','net.minecraft.client.gui.screens.inventory.AbstractContainerScreen']:
 code=subprocess.check_output([str(jdk/'javap'),'-c','-p','-classpath',cp,cls],text=True)
 key=code[code.index('public boolean keyPressed(net.minecraft.client.input.KeyEvent);'):]
 assert 'onClose:()V' in key.split('\n  public ',1)[0],cls
print('ContainerCloseNativeTarget: PASS pinned26.2 Escape/inventory native close path; full GUI application remains separate')
