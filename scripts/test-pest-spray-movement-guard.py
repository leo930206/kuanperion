#!/usr/bin/env python3
"""Execute actual Bridge pre-movement guard against controlled screen/physical service observations.
Global stop tests separately compose actual original-session holds and faults. No native input effects here.
"""
from pathlib import Path
import subprocess,tempfile,re
r=Path(__file__).resolve().parents[1];s=(r/'src/main/java/dev/kuan/kuanperion/AutomationClientBridge.java').read_text();a=s.index(' public void beforeMovementTick(){');b=s.index('{',a)+1;depth=1
while depth:depth+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b].replace('dev.kuan.kuanperion.ui.MainGUI','OwnSettings')
java=r'''package dev.kuan.kuanperion.automation;
import java.util.*;
public class SprayPreMovementGuardTest {
 static class OwnSettings{}static class Gui{Object screen;Object screen(){return screen;}}static class Minecraft{static final Minecraft I=new Minecraft();Gui gui=new Gui();static Minecraft getInstance(){return I;}}
 static class Job{boolean active,owned;int cancels;boolean active(){return active;}boolean ownsMenu(){return owned;}void cancel(long n){active=false;cancels++;}}
 static class Controller{boolean physical;boolean manualInput(){return physical;}}
 Job pest=new Job(),spray=new Job(),greenhouse=new Job(),composter=new Job(),visitor=new Job(),supercraft=new Job(),book=new Job(),pets=new Job();Controller flight=new Controller(),sprayController=new Controller(),greenhouseFlight=new Controller(),composterFlight=new Controller(),visitorFlight=new Controller(),supercraftMenu=new Controller();
 AutomationContext context=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return context;}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  for(String mode:new String[]{"safe","owned-menu","foreign-menu","own-settings","physical","garden","focus","dead","mining","recovery","selection","full","disconnected"}){
   var h=new SprayPreMovementGuardTest();h.spray.active=true;Minecraft.I.gui.screen=null;boolean allowed=false;
   switch(mode){case "safe"->allowed=true;case "owned-menu"->{allowed=true;h.spray.owned=true;Minecraft.I.gui.screen=new Object();h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}case "foreign-menu"->{Minecraft.I.gui.screen=new Object();h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}case "own-settings"->{h.spray.owned=true;Minecraft.I.gui.screen=new OwnSettings();}case "physical"->h.sprayController.physical=true;case "garden"->h.context=new AutomationContext(1,true,false,Set.of());default->{var b=switch(mode){case "focus"->AutomationContext.Blocker.FOCUS;case "dead"->AutomationContext.Blocker.DEAD;case "mining"->AutomationContext.Blocker.MINING;case "recovery"->AutomationContext.Blocker.RECOVERY;case "selection"->AutomationContext.Blocker.SELECTION;case "full"->AutomationContext.Blocker.INVENTORY_FULL;default->AutomationContext.Blocker.DISCONNECTED;};h.context=new AutomationContext(1,true,true,Set.of(b));}}
   h.beforeMovementTick();check(h.spray.cancels==(allowed?0:1),"actual pre-movement screen/physical guard: "+mode);check(h.pest.cancels+h.greenhouse.cancels+h.composter.cancels+h.visitor.cancels+h.supercraft.cancels+h.book.cancels+h.pets.cancels==0,"independent spray guard cannot cancel idle other services: "+mode);
  }
  System.out.println("SprayPreMovementGuardTest: PASS actual Bridge guard over own/foreign/owned UI, physical and lifecycle blockers; controlled actors, no native/live claim");
 }
}'''.replace('METHOD',method)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-movement-') as d:
 f=Path(d)/'SprayPreMovementGuardTest.java';f.write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-sourcepath',str(r/'src/main/java'),'-d',d,str(f)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',d,'dev.kuan.kuanperion.automation.SprayPreMovementGuardTest'],check=True)
