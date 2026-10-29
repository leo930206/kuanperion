#!/usr/bin/env python3
"""Execute actual bridge movement guard against explicit service/input boundaries, not a live motion claim."""
from pathlib import Path
import subprocess,tempfile
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';s=(m/'AutomationClientBridge.java').read_text();a=s.index(' public void beforeMovementTick(');b=s.index('{',a)+1;depth=1
while depth:
 depth+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b].replace("dev.kuan.kuanperion.ui.MainGUI","OwnMenu")
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class MaintenanceInputPriorityTest {
 static class Service {boolean running,owned;int cancelled;boolean active(){return running;}boolean ownsMenu(){return owned;}void cancel(long now){if(running)cancelled++;running=false;}}
 static class Flight {boolean manual;boolean manualInput(){return manual;}}
 Service composter=new Service(),pest=new Service(),greenhouse=new Service(),visitor=new Service(),supercraft=new Service(),book=new Service(),pets=new Service();Flight composterFlight=new Flight(),flight=new Flight(),greenhouseFlight=new Flight(),visitorFlight=new Flight(),supercraftMenu=new Flight();AutomationContext context=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return context;}
 static class OwnMenu{}static class Gui{Object current;Object screen(){return current;}}static class Minecraft{static final Minecraft INSTANCE=new Minecraft();Gui gui=new Gui();static Minecraft getInstance(){return INSTANCE;}}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var h=new MaintenanceInputPriorityTest();h.composter.running=true;h.beforeMovementTick();check(h.composter.running&&h.composter.cancelled==0,"inactive Greenhouse/Pest guard must not unconditionally cancel Composter");
  h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.composter.owned=true;h.beforeMovementTick();check(h.composter.running,"registered actual owned Composter menu remains usable");
  h.composter.owned=false;h.beforeMovementTick();check(!h.composter.running&&h.composter.cancelled==1,"foreign GUI cancels before world movement");
  for(var b:AutomationContext.Blocker.values())if(b!=AutomationContext.Blocker.MENU){h.context=new AutomationContext(1,true,true,Set.of(b));h.composter.running=true;h.composter.owned=true;h.beforeMovementTick();check(!h.composter.running,"owned menu must not mask other blocker: "+b);}
  h.context=new AutomationContext(1,true,false,Set.of());h.composter.running=true;h.beforeMovementTick();check(!h.composter.running,"fresh Garden proof required before movement");
  h.context=new AutomationContext(1,true,true,Set.of());h.composter.running=true;h.composterFlight.manual=true;h.beforeMovementTick();check(!h.composter.running,"physical input wins");
  h.composterFlight.manual=false;h.greenhouse.running=true;h.beforeMovementTick();check(h.greenhouse.running,"inactive Composter cannot stop existing Greenhouse");h.pest.running=true;h.flight.manual=true;h.beforeMovementTick();check(!h.pest.running&&h.greenhouse.running,"existing Pest-specific manual guard stays isolated");
  h=new MaintenanceInputPriorityTest();h.visitor.running=true;h.beforeMovementTick();check(h.visitor.running,"Visitor running must not cross-cancel or be ignored");h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.visitor.owned=true;h.beforeMovementTick();check(h.visitor.running,"exact owned Visitor menu allowed");h.visitor.owned=false;h.beforeMovementTick();check(!h.visitor.running,"foreign menu cancels Visitor");
  for(var b:AutomationContext.Blocker.values())if(b!=AutomationContext.Blocker.MENU){h.context=new AutomationContext(1,true,true,Set.of(b));h.visitor.running=true;h.visitor.owned=true;h.beforeMovementTick();check(!h.visitor.running,"Visitor owned menu cannot mask: "+b);}h.context=new AutomationContext(1,true,false,Set.of());h.visitor.running=true;h.beforeMovementTick();check(!h.visitor.running,"Visitor Garden proof mandatory");h.context=new AutomationContext(1,true,true,Set.of());h.visitor.running=true;h.visitorFlight.manual=true;h.beforeMovementTick();check(!h.visitor.running,"Visitor physical input wins before native movement");
  h=new MaintenanceInputPriorityTest();h.supercraft.running=true;h.beforeMovementTick();check(h.supercraft.running,"inactive flight actors do not cross-cancel Supercraft");
  h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.supercraft.owned=true;h.beforeMovementTick();check(h.supercraft.running,"registered recipe menu is not foreign");h.supercraft.owned=false;h.beforeMovementTick();check(!h.supercraft.running,"foreign menu preempts craft");
  h.context=new AutomationContext(1,true,true,Set.of());h.supercraft.running=true;Minecraft.INSTANCE.gui.current=new OwnMenu();h.beforeMovementTick();check(!h.supercraft.running,"own settings screen cannot be replaced by a running craft");Minecraft.INSTANCE.gui.current=null;
  h.supercraft.running=true;h.supercraftMenu.manual=true;h.beforeMovementTick();check(!h.supercraft.running,"physical manual input beats craft");
  for(var b:AutomationContext.Blocker.values())if(b!=AutomationContext.Blocker.MENU){h.context=new AutomationContext(1,true,true,Set.of(b));h.supercraft.running=true;h.supercraft.owned=true;h.beforeMovementTick();check(!h.supercraft.running,"recipe ownership cannot mask blocker: "+b);}
  h=new MaintenanceInputPriorityTest();h.book.running=true;h.beforeMovementTick();check(h.book.running,"inactive flight tasks do not cancel Book");h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.book.owned=true;h.beforeMovementTick();check(h.book.running,"expected/owned Anvil may settle under existing receipt gate");h.book.owned=false;h.beforeMovementTick();check(!h.book.running,"foreign menu preempts Book before movement");
  h.context=new AutomationContext(1,true,true,Set.of());h.book.running=true;Minecraft.INSTANCE.gui.current=new OwnMenu();h.beforeMovementTick();check(!h.book.running,"settings never replaces active Book");Minecraft.INSTANCE.gui.current=null;
  for(var b:AutomationContext.Blocker.values())if(b!=AutomationContext.Blocker.MENU&&b!=AutomationContext.Blocker.INVENTORY_FULL){h.context=new AutomationContext(1,true,true,Set.of(b));h.book.running=true;h.book.owned=true;h.beforeMovementTick();check(!h.book.running,"Anvil ownership cannot mask blocker: "+b);}
  h.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.INVENTORY_FULL));h.book.running=true;h.beforeMovementTick();check(h.book.running,"Book full-inventory exception cannot be cancelled by unrelated movement guard");
  System.out.println("MaintenanceInputPriorityTest: PASS (actual bridge body, no cross-cancellation, exact owned GUI only, all blockers/Garden/physical priority, preserved Pest/Greenhouse guards)");
 }
}'''.replace('METHOD',method)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
with tempfile.TemporaryDirectory() as temp:
 p=Path(temp);(p/'MaintenanceInputPriorityTest.java').write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-d',temp,str(m/'automation/AutomationContext.java'),str(p/'MaintenanceInputPriorityTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp,'dev.kuan.kuanperion.MaintenanceInputPriorityTest'],check=True)
