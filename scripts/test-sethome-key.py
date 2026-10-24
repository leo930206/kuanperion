#!/usr/bin/env python3
"""Exercise the production hotkey dispatch with fake physical key/focus boundaries."""
from pathlib import Path
import re, subprocess,sys,tempfile
root=Path(__file__).resolve().parent.parent
source=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
assert 'new KeybindSetting("/warp garden",homeKey,1,' in source,'Shortcut label must match actual command'
import json
labels=json.loads((root/'src/main/resources/assets/kuanperion/lang/en_us.json').read_text())
assert labels['key.kuanperion.home']=='/warp garden','Keep legacy key ID but display new command'
assert 'new KeyMapping("key.kuanperion.home", InputConstants.Type.KEYSYM, InputConstants.KEY_H' in source,'Do not reset saved bindings or Shift+H default'
m=re.search(r'    private static void handleShiftHotkeys\(',source);a=m.start();b=source.index('{',a)+1;depth=1
while depth:
 depth+=(source[b]=='{')-(source[b]=='}');b+=1
method=source[a:b]
m=re.search(r'    private static boolean sendHomeCommand\(',source);a=m.start();b=source.index('{',a)+1;depth=1
while depth:
 depth+=(source[b]=='{')-(source[b]=='}');b+=1
method+='\n'+source[a:b]
java=r'''package dev.kuan.kuanperion;
import java.util.*;
public class SethomeKeyTest {
 static final HotkeyChord.Edge miningHotkeyEdge=new HotkeyChord.Edge(), farmingHotkeyEdge=new HotkeyChord.Edge(),sethomeHotkeyEdge=new HotkeyChord.Edge(),homeHotkeyEdge=new HotkeyChord.Edge(),barnHotkeyEdge=new HotkeyChord.Edge();
 static int miningModifiers=1,farmingModifiers=1,sethomeModifiers=0,homeModifiers=0,barnModifiers=1,mods;
 static class Key {boolean bound,down;}
 static Key toggleMiningKey=new Key(),toggleFarmingKey=new Key(),sethomeKey=new Key(),homeKey=new Key(),barnKey=new Key();
 static boolean hotkeyDown(Key key){return key.bound&&key.down;}
 static int currentHotkeyModifiers(){return mods;}
 static int farming,mining;static List<String> commands=new ArrayList<>();
 static void toggleFarming(Minecraft mc){farming++;} static void toggleMining(Minecraft mc){mining++;}
 static List<String> feedback=new ArrayList<>();
 static String plain(String s){return s;}static String warning(String s){return s;}
 static void say(Minecraft mc,String message){feedback.add(message);}
 static class Player{Connection connection=new Connection();}
 static class Connection{boolean fail;void sendCommand(String command){if(fail)throw new IllegalStateException();commands.add(command);}}
 static class Window{long handle(){return 1;}}
 static class Gui {Object screen;Object screen(){return screen;}}
 static class Minecraft {Player player=new Player();Object level=new Object();Gui gui=new Gui();Window getWindow(){return new Window();}}
 static class GLFW {static final int GLFW_FOCUSED=1,GLFW_TRUE=1;static boolean focused=true;static int glfwGetWindowAttrib(long h,int flag){return focused?1:0;}}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 METHOD
 public static void main(String[] args){
  Minecraft mc=new Minecraft();sethomeKey.down=true;handleShiftHotkeys(mc);check(commands.isEmpty(),"unbound default never sends");
  sethomeKey.down=false;handleShiftHotkeys(mc);sethomeKey.bound=true;sethomeKey.down=true;handleShiftHotkeys(mc);
  for(int i=0;i<100;i++)handleShiftHotkeys(mc);check(commands.equals(List.of("sethome")),"one command per physical press");
  sethomeKey.down=false;handleShiftHotkeys(mc);sethomeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==2,"re-press works");
  sethomeKey.down=false;handleShiftHotkeys(mc);mods=2;sethomeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==2,"exact modifiers required");
  sethomeKey.down=false;handleShiftHotkeys(mc);sethomeModifiers=mods;sethomeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==3,"Ctrl chord works");
  for(int mode=0;mode<3;mode++){
   sethomeKey.down=false;handleShiftHotkeys(mc);
   if(mode==0)mc.gui.screen=new Object();if(mode==1)GLFW.focused=false;if(mode==2)mc.player=null;
   sethomeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==3,"GUI/focus/world suppression");
   mc.gui.screen=null;GLFW.focused=true;mc.player=new Player();handleShiftHotkeys(mc);check(commands.size()==3,"held suppressed key does not fire after context returns");
  }
  sethomeKey.down=false;mods=0;homeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==3,"home initially unbound");
  homeKey.down=false;handleShiftHotkeys(mc);homeKey.bound=true;homeKey.down=true;handleShiftHotkeys(mc);
  check(commands.size()==4&&commands.get(3).equals("warp garden")&&feedback.get(feedback.size()-1).equals("已發送 /warp garden。"),"warp garden command and feedback match, independently of sethome");
  for(int i=0;i<100;i++)handleShiftHotkeys(mc);check(commands.size()==4,"held home does not repeat");
  homeKey.down=false;handleShiftHotkeys(mc);homeModifiers=3;mods=3;homeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==5,"home chord");
  for(int mode=0;mode<3;mode++){
   homeKey.down=false;handleShiftHotkeys(mc);
   if(mode==0)mc.gui.screen=new Object();if(mode==1)GLFW.focused=false;if(mode==2)mc.player=null;
   homeKey.down=true;handleShiftHotkeys(mc);check(commands.size()==5,"home GUI/focus/world guards");
   mc.gui.screen=null;GLFW.focused=true;mc.player=new Player();handleShiftHotkeys(mc);check(commands.size()==5,"home held key suppressed after context return");
  }
  check(feedback.size()==commands.size()&&feedback.stream().allMatch(s->s.startsWith("已發送 /")),"every shortcut reports one local chat message");
  int messages=feedback.size();mc.player.connection.fail=true;
  check(!sendHomeCommand(mc,"warp garden",true)&&commands.size()==5&&feedback.size()==messages+1&&feedback.get(messages).startsWith("無法發送 /"),"never claim success when sending fails");
  homeKey.down=false;mods=1;handleShiftHotkeys(mc);mc.player.connection.fail=false;barnKey.bound=true;
  int before=commands.size();barnKey.down=true;handleShiftHotkeys(mc);
  check(commands.size()==before+1&&commands.get(before).equals("barn")&&feedback.get(feedback.size()-1).equals("已發送 /barn。"),"Shift+B sends barn and chat feedback");
  for(int i=0;i<100;i++)handleShiftHotkeys(mc);check(commands.size()==before+1,"held barn never repeats");
  for(int mode=0;mode<5;mode++){
   barnKey.down=false;handleShiftHotkeys(mc);
   if(mode==0)mc.gui.screen=new Object();if(mode==1)GLFW.focused=false;if(mode==2)mc.player=null;if(mode==3)mc.level=null;if(mode==4)mods=3;
   barnKey.down=true;handleShiftHotkeys(mc);check(commands.size()==before+1,"barn context and exact modifier guards");
   mc.gui.screen=null;GLFW.focused=true;mc.player=new Player();mc.level=new Object();mods=1;
   handleShiftHotkeys(mc);check(commands.size()==before+1,"barn held key cannot fire after context returns");
  }
  barnKey.down=false;handleShiftHotkeys(mc);barnKey.bound=false;barnKey.down=true;handleShiftHotkeys(mc);check(commands.size()==before+1,"clear barn disables it");
  check(farming==0&&mining==0,"no interference with farming/mining");
  System.out.println("SethomeKeyTest: PASS (unbound, one-shot, exact modifiers, screen/focus/world guards, no cross-feature action)");
 }
}'''.replace('METHOD',method)
jdk=Path(sys.argv[1])/'bin'
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'SethomeKeyTest.java';p.write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-d',tmp,str(root/'src/main/java/dev/kuan/kuanperion/HotkeyChord.java'),str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'dev.kuan.kuanperion.SethomeKeyTest'],check=True)
