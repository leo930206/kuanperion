#!/usr/bin/env python3
"""Run extracted production settings-attack integration with a simulated26.2 GUI miss blocker."""
from pathlib import Path
import subprocess,sys,tempfile,re
r=Path(__file__).resolve().parents[1];s=(r/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def method(name):
 m=re.search(r'    private static [^\n]+ '+name+r'\(',s);assert m,'Missing production method: '+name
 start=m.start();end=s.index('{',start)+1;depth=1
 while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
 return s[start:end].replace('dev.kuan.kuanperion.ui.MainGUI','MainGUI').replace('dev.kuan.kuanperion.mixin.MinecraftAttackAccess','MinecraftAttackAccess')
body=method('tickFarmingSettingsAttack')
tick=method('tick');assert tick.count('tickFarmingSettingsAttack(mc);')==1 and tick.index('tickFarmingSettingsAttack(mc);')<tick.index('tickFarmingStallNotification(mc, System.nanoTime());')
selection=method('handleRightClickSelection');assert selection.index('mc.gui.screen() != null')<selection.index('if (down && !rightWasDown') and 'rightWasDown = down; return;' in selection
h=r'''package dev.kuan.kuanperion;
public class SettingsAutomationTest {
 static boolean farmingEnabled=true,miningEnabled,farmingAutoRecovering,farmingSettingsWasOpen;
 interface MinecraftAttackAccess{void kuanperion$setMissTime(int ticks);void kuanperion$continueAttack(boolean attack);}
 static class MainGUI{}
 static class Key{boolean down=true;boolean isDown(){return down;}}
 static class Options{Key keyAttack=new Key();}
 static class Gui{Object current=new MainGUI(),overlay;Object screen(){return current;}Object overlay(){return overlay;}}
 static class GameMode{int stops;void stopDestroyBlock(){stops++;}}
 static class Player{boolean alive=true;boolean isAlive(){return alive;}}
 static class Minecraft implements MinecraftAttackAccess{
  Player player=new Player();Object level=new Object();GameMode gameMode=new GameMode();Options options=new Options();Gui gui=new Gui();boolean paused;int missTime=10000,harvests,stops,calls,resets;
  boolean isPaused(){return paused;}
  public void kuanperion$setMissTime(int ticks){missTime=ticks;resets++;}
  public void kuanperion$continueAttack(boolean down){check(missTime==0,"must clear vanilla GUI miss blocker before harvesting");calls++;if(down)harvests++;else stops++;}
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Minecraft reset(){farmingEnabled=true;miningEnabled=farmingAutoRecovering=farmingSettingsWasOpen=false;return new Minecraft();}
 static void excluded(java.util.function.Consumer<Minecraft> change,String reason){Minecraft m=reset();change.accept(m);tickFarmingSettingsAttack(m);check(m.calls==0&&m.resets==0,"do not bypass "+reason);}
 public static void main(String[] args){
  Minecraft m=reset();for(int i=0;i<10;i++){m.missTime=10000;tickFarmingSettingsAttack(m);}check(m.harvests==10&&m.calls==10,"one held block harvest per active settings tick");
  m.gui.current=null;m.missTime=10000;tickFarmingSettingsAttack(m);check(m.missTime==0&&m.calls==10&&!farmingSettingsWasOpen,"closing settings clears last GUI blocker without extra attack");
  m.missTime=7;tickFarmingSettingsAttack(m);check(m.missTime==7,"normal game cooldown not overridden");
  m=reset();m.options.keyAttack.down=false;tickFarmingSettingsAttack(m);check(m.stops==1&&m.harvests==0,"planned waits/wrong-tool release stop vanilla harvesting");
  m=reset();tickFarmingSettingsAttack(m);m.gui.current=new Object();m.missTime=10000;tickFarmingSettingsAttack(m);check(m.missTime==10000&&m.calls==1&&!farmingSettingsWasOpen&&m.gameMode.stops==1,"other GUIs abort owned harvest without cooldown bypass");
  m=reset();tickFarmingSettingsAttack(m);farmingEnabled=false;tickFarmingSettingsAttack(m);tickFarmingSettingsAttack(m);check(m.calls==1&&m.gameMode.stops==1,"manual off aborts owned harvest once");
  m=reset();tickFarmingSettingsAttack(m);miningEnabled=true;tickFarmingSettingsAttack(m);check(m.gameMode.stops==0,"never abort new Mining owner");
  excluded(x->farmingEnabled=false,"disabled farming");excluded(x->miningEnabled=true,"Mining");excluded(x->farmingAutoRecovering=true,"recovery");
  excluded(x->x.player=null,"no player");excluded(x->x.level=null,"no world");excluded(x->x.options=null,"no options");excluded(x->x.gameMode=null,"no game mode");excluded(x->x.player.alive=false,"death");excluded(x->x.paused=true,"paused game");excluded(x->x.gui.overlay=new Object(),"loading overlay");
  excluded(x->x.gui.current=null,"normal gameplay");excluded(x->x.gui.current=new Object(),"backpack/chat/other UI");tickFarmingSettingsAttack(null);
  System.out.println("SettingsAutomationTest: PASS (active own UI / vanilla GUI miss blocker / hold-release / close cleanup / one-per-tick / other states unchanged)");
 }
 //METHOD
}
'''.replace(' //METHOD',body)
with tempfile.TemporaryDirectory() as t:
 p=Path(t)/'SettingsAutomationTest.java';p.write_text(h);j=Path(sys.argv[1])/'bin'
 subprocess.run([str(j/'javac'),'--release','25','-encoding','UTF-8','-d',t,str(p)],check=True)
 subprocess.run([str(j/'java'),'-cp',t,'dev.kuan.kuanperion.SettingsAutomationTest'],check=True)
