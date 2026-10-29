#!/usr/bin/env python3
"""Actual native Sacks adapter bodies + real flow/session/receipts; substituted native edge only."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;s=(r/'src/main/java/dev/kuan/kuanperion/ComposterFlightController.java').read_text()
def method(name):
 m=re.search(r'\b(?:public|private) [^\n{}]+ '+name+r'\(',s);assert m,name
 a=m.start();b=s.index('{',a)+1;d=1
 while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
methods='\n'.join(method(n) for n in ['beginSacks','pollSacks','sacksIncrease','ownsSacksScreen','cancelSacks']).replace('net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>','Screen<?>')
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import java.util.*;import java.util.function.*;
public class ComposterFlightController {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}static final long S=1_000_000_000L;
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","air",0,List.of(),true);
 static ComposterFlightController current;long time=S;boolean same=true;int clicks;
 static class Menu{Menu getCarried(){return this;}boolean isEmpty(){return true;}}static class Player {Menu containerMenu=new Menu();boolean isAlive(){return true;}boolean isSpectator(){return false;}boolean isPassenger(){return false;}boolean isSleeping(){return false;}boolean isUsingItem(){return false;}}
 static class Screen<T>{Object menu;Screen(Object m){menu=m;}Object getMenu(){return menu;}}class Gui{Object overlay(){return null;}Object screen(){return screen;}}
 Object level=new Object(),gameMode=new Object(),screen;Player player=new Player();Gui gui=new Gui();boolean isSameThread(){return same;}ComposterFlightController mc(){return this;}
 final AutomationCoordinator coordinator=new AutomationCoordinator();AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext context(){return ctx;}final AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->{});final InventoryReceipts purchaseItems=new InventoryReceipts();final CursorReceipts purchaseCursors=new CursorReceipts();
 class Flight {AutomationSession.Handle owner(){return machineOwner;}Object levelIdentity(){return level;}}Flight flight=new Flight();AutomationSession.Handle machineOwner,sacksOwner;InventoryReceipts.Stamp machineOpening;CursorReceipts.OpeningStamp machineCursorOpening;ComposterSacksFlow sacks;long sacksGain;MenuSnapshot observed;
 MenuSnapshot menu(){return observed;}static class VisitorGameView{static Map<Integer,ItemSnapshot> inventory(Object mc){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,AIR);return m;}}
 static class AutomationMenuActions{static boolean sendNative(MenuContract.Ticket ticket){current.clicks++;return true;}}
 METHODS
 static ItemSnapshot item(String n,String...lore){return new ItemSnapshot("","minecraft:paper","",n,n+Arrays.toString(lore),1,List.of(lore),false);}
 void menu(int id,String title,Map<Integer,ItemSnapshot> changed){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)slots.add(new MenuSnapshot.Slot(i,changed.getOrDefault(i,AIR),i>=54));observed=new MenuSnapshot(1,id,1,title,54,slots);player.containerMenu=new Menu();screen=new Screen<>(player.containerMenu);for(var x:slots)purchaseItems.observe(1,id,x.index(),x.item());purchaseCursors.observe(1,id,AIR,time);}
 void mainMenu(int id,int value){menu(id,"Composter",Map.of(39,item("Organic Matter",value+"/10000"),41,item("Fuel","5000/10000")));}
 AutomationSession.Handle start(){machineOwner=session.begin(AutomationFeature.COMPOSTER,0,120*S);machineOpening=purchaseItems.stamp(1);machineCursorOpening=purchaseCursors.openingStamp(1,0);mainMenu(7,1000);ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));return machineOwner;}
 public static void main(String[]args){var t=new ComposterFlightController();current=t;var h=t.start();t.same=false;check(!t.beginSacks(h,ComposterObservation.Kind.ORGANIC,10000,t.time)&&t.clicks==0,"offthread guard");t.same=true;check(t.beginSacks(h,ComposterObservation.Kind.ORGANIC,10000,t.time)&&t.clicks==1&&t.ownsSacksScreen(),"actual native port starts real flow");t.time=2*S;t.menu(8,"Organic Matter",Map.of(11,item("Fill from Sacks","Click to fill!")));check(t.pollSacks(h,t.time)==ComposterTask.Acquisition.RUNNING&&t.clicks==2,"actual guarded native fill");t.time=3*S;t.mainMenu(9,7000);check(t.pollSacks(h,t.time)==ComposterTask.Acquisition.COMPLETE&&t.sacksIncrease()==6000&&t.sacks==null&&t.sacksOwner==null&&t.session.isCurrent(h),"verified gain transfers to task without releasing parent");t.cancelSacks();check(t.sacksIncrease()==0,"cleanup clears result");System.out.println("ComposterSacksNativeBodyTest: PASS actual adapter/real flow/session/receipts with controlled native boundary, no current GUI claim");}
}'''.replace('METHODS',methods.replace('System::nanoTime','()->time'))
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-composter-sacks-native-') as tmp:
 f=Path(tmp)/'ComposterFlightController.java';f.write_text(java);sources=list((r/'src/main/java/dev/kuan/kuanperion/automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,sources),str(f)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.ComposterFlightController'],check=True)
opened=method('open');assert opened.index('machineOpening=purchaseItems.stamp(')<opened.index('startDestroyBlock(')
assert opened.index('machineCursorOpening=purchaseCursors.openingStamp(')<opened.index('startDestroyBlock(')
assert 'ComposterSacksView' not in opened and 'Thread.sleep' not in s
print('ComposterSacksOpeningWiring: PASS stamps before native machine open, no timing-only proof or blind refill click')
