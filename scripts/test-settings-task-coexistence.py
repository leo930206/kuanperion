#!/usr/bin/env python3
"""Execute actual settings entry and read-only task gate, not a reimplementation of either."""
from pathlib import Path
import re, subprocess, tempfile
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
def body(source,name):
 m=re.search(r'\b(?:public|private) (?:static )?[^{}\n()]+ '+name+r'\(',source);assert m,name
 a=m.start();b=source.index('{',a)+1;depth=1
 while depth:depth+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
def run(name,java,files=()):
 with tempfile.TemporaryDirectory(prefix='kuanperion-settings-task-') as temp:
  p=Path(temp);(p/(name+'.java')).write_text(java)
  subprocess.run([str(jdk/'javac'),'--release','21','-d',temp,*map(str,files),str(p/(name+'.java'))],check=True)
  subprocess.run([str(jdk/'java'),'-cp',temp,name],check=True)
client=(main/'KuanperionClient.java').read_text()
toggle=body(client,'toggleSettingsScreen').replace('dev.kuan.kuanperion.ui.MainGUI','OwnScreen')
run('SettingsTaskEntryTest',r'''
public final class SettingsTaskEntryTest {
 static class Screen {int closed;void onClose(){closed++;}}
 static class OwnScreen extends Screen {}
 static class Gui {Screen current;Screen screen(){return current;}}
 static class Minecraft {Gui gui=new Gui();int opened;void setScreenAndShow(Screen s){gui.current=s;opened++;}}
 static class Bridge {boolean allowed=true;int checks;boolean canOpenSettings(){checks++;return allowed;}}
 static Bridge automation;static int notices;static String plain(String s){return s;}
 static void say(Minecraft m,String... text){notices++;}
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 METHODS
 public static void main(String[]args){
  var mc=new Minecraft();automation=new Bridge();automation.allowed=false;
  var server=new Screen();mc.gui.current=server;toggleSettingsScreen(mc);
  check(mc.opened==0&&mc.gui.current==server&&server.closed==0&&notices==1,"settings must not replace active task/server menu; refusal reports once without closing/clicking/cancelling it");
  automation.allowed=true;toggleSettingsScreen(mc);check(mc.opened==1&&mc.gui.current instanceof OwnScreen,"idle settings opens normally");
  var own=mc.gui.current;automation.allowed=false;int checks=automation.checks;toggleSettingsScreen(mc);
  check(own.closed==1&&automation.checks==checks&&notices==1,"closing existing settings never consults or cancels a task");
  automation=null;mc.gui.current=null;toggleSettingsScreen(mc);check(mc.opened==2,"title/bootstrap settings remains available");
  System.out.println("SettingsTaskEntryTest: PASS (actual hotkey/command toggle, explicit refusal preserves current screen, close/title unchanged)");
 }
}'''.replace('METHODS',toggle))
bridge=(main/'AutomationClientBridge.java').read_text()
gate=body(bridge,'canOpenSettings')
files=[main/'automation'/(name+'.java') for name in ['AutomationSession','AutomationContext','AutomationCoordinator','AutomationFeature','FarmingHandoff','MenuContract','MenuSnapshot','ItemSnapshot']]
run('SettingsTaskGateTest',r'''
import java.util.*;import dev.kuan.kuanperion.automation.*;
public final class SettingsTaskGateTest {
 AutomationContext context=new AutomationContext(1,true,true,Set.of());int stops;
 AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->context,()->{},()->stops++);
 static class Item {boolean empty=true;boolean isEmpty(){return empty;}}
 static class Menu {Item cursor=new Item();Item getCarried(){return cursor;}}
 static class Player {Menu inventoryMenu=new Menu(),containerMenu=inventoryMenu;}
 static class Gui {Object overlay;Object overlay(){return overlay;}}
 static class Minecraft {static Minecraft instance=new Minecraft();static Minecraft getInstance(){return instance;}Player player=new Player();Object level=new Object();Gui gui=new Gui();boolean same=true;boolean isSameThread(){return same;}}
 METHODS
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
  var a=new SettingsTaskGateTest();var mc=Minecraft.getInstance();
  check(a.canOpenSettings(),"ordinary manually armed Farming is not blocked");
  a.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MINING));check(a.canOpenSettings(),"existing Mining settings behavior is unchanged");
  a.context=new AutomationContext(1,true,true,Set.of());
  for(var feature:List.of(AutomationFeature.PEST_SPRAY,AutomationFeature.PET_SELL,AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK,AutomationFeature.SUPERCRAFT,AutomationFeature.BOOK_COMBINE)){
   var h=a.session.begin(feature,1,1000);check(h!=null&&!a.canOpenSettings()&&a.session.isCurrent(h)&&a.session.holdsFarming()&&a.stops==0,"read-only gate preserves original root/hold: "+feature);
   check(a.session.finish(h,true,2)&&a.canOpenSettings(),"no queued settings replay after root completes: "+feature);
  }
  mc.player.containerMenu=new Menu();check(!a.canOpenSettings(),"foreign or formerly owned server menu cannot be replaced even with no task");mc.player.containerMenu=mc.player.inventoryMenu;
  mc.player.inventoryMenu.cursor.empty=false;check(!a.canOpenSettings(),"never close an inventory carrying an item");mc.player.inventoryMenu.cursor.empty=true;
  mc.gui.overlay=new Object();check(!a.canOpenSettings(),"loading overlay not replaced");mc.gui.overlay=null;
  mc.same=false;check(!a.canOpenSettings(),"off-thread gate denies");mc.same=true;
  mc.player=null;mc.level=null;check(a.canOpenSettings(),"title screen still permits local configuration");
  check(a.stops==0&&!a.session.active(),"checking settings never stops/restarts Farm or acquires a lease");
  System.out.println("SettingsTaskGateTest: PASS (actual read-only native gate, all ten gameplay roots, cursor/container/overlay/thread, legacy Farm/Mining/title preserved)");
 }
}'''.replace('METHODS',gate),files)
tick=body(client,'tick')
assert 'toggleSettingsScreen(mc)' in tick and 'toggleSettingsScreen(mc)' in body(client,'tickSettingsCommand')
assert 'setScreen' not in gate and '.cancel(' not in gate and '.finish(' not in gate and '.invalidate(' not in gate
print('SettingsTaskCoexistence: PASS (same guarded entry for key/command, no deferred replay, native item/command boundaries retained; no live server claim)')
