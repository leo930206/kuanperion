#!/usr/bin/env python3
"""Actual KuanperionClient slot projection -> real exchange task/ledger; controlled native wrappers, no Mixin/live claim."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parents[1];source=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def method(name):
 m=re.search(r'    public static void '+name+r'\([^\n]*\)\{',source);assert m,name
 e=m.end();d=1
 while d:d+=(source[e]=='{')-(source[e]=='}');e+=1
 return source[m.start():e].replace('net.minecraft.world.item.ItemStack','Stack')
java=r'''package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestExchangeCanonicalNativeTest {
 static PestExchangeCanonicalReceiptTest.P p;static Automation automation;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 record Stack(ItemSnapshot item){}static class Slot{Object container;int index,inventoryIndex;Slot(Object c,int i,int raw){container=c;index=i;inventoryIndex=raw;}int getContainerSlot(){return inventoryIndex;}}
 static class Menu{int containerId;List<Slot> slots=new ArrayList<>();Menu(int id){containerId=id;}}
 static class Player{final Object inventory=new Object();Menu inventoryMenu=new Menu(0),containerMenu=new Menu(7);Object getInventory(){return inventory;}}
 static class Minecraft{static Minecraft instance;Object level=new Object();Player player=new Player();boolean thread=true;boolean isSameThread(){return thread;}static Minecraft getInstance(){return instance;}}
 static class Automation{void serverSlot(int container,int slot,Stack stack){p.items.observe(1,container,slot,stack.item());}}
 METHODS
 static void setup(){automation=new Automation();var mc=new Minecraft();Minecraft.instance=mc;var player=mc.player;
  for(int raw=0;raw<46;raw++)player.inventoryMenu.slots.add(new Slot(raw<9?new Object():player.inventory,raw,raw<36?raw:raw<45?raw-36:40));
  for(int raw=0;raw<63;raw++)player.containerMenu.slots.add(new Slot(raw<27?new Object():player.inventory,raw,raw<27?-1:raw<54?raw-27+9:raw-54));
 }
 public static void main(String[]args){
  for(int slot=0;slot<9;slot++)for(String route:List.of("inventory","legacy-inventory","external")){
   p=new PestExchangeCanonicalReceiptTest.P(slot);setup();var t=new PestExchangeTask(p.session,p.coordinator,p.items,p.cursors,p);check(t.begin(p.owner,true,new PestIdentity.Vacuum(slot,PestExchangeTaskTest.TOOL,10),p.now),"begin owned slot");PestExchangeTaskTest.sent(p,t);
   var old=p.inv.get(slot);var next=new ItemSnapshot(old.skyblockId(),old.minecraftId(),old.uuid(),old.name(),"applied-empty-"+slot,1,List.of("Ability: Vacuum RIGHT CLICK","within 10 blocks","Vacuum Bag: 0"),true);p.inv.put(slot,next);p.ack(true,true);var stamp=p.items.stamp(1);
   if(route.equals("inventory"))observeAutomationInventorySlot(slot,new Stack(next));else if(route.equals("legacy-inventory"))observeAutomationSlot(-2,slot,new Stack(next));else observeAutomationSlot(7,54+slot,new Stack(next));
   check(p.items.after(stamp,0,36+slot)!=null&&p.items.after(stamp,7,54+slot)!=null&&next.fingerprint().equals(p.items.after(stamp,0,36+slot).fingerprint())&&next.fingerprint().equals(p.items.after(stamp,7,54+slot).fingerprint())&&p.items.after(stamp,0,slot)==null,"actual client callback uses real window alias, not inventory index "+route+"/"+slot);
   t.tick(p.now);t.tick(p.now);check(t.state()==PestExchangeTask.State.COMPLETE&&p.clicks==1&&p.closes==1&&p.session.isCurrent(p.owner),"correct native projection composes with original-root exact-once exchange "+route+"/"+slot);
  }
  System.out.println("PestExchangeCanonicalNativeTest: PASS all9 hotbar slots x3 actual client callback routes -> real task/receipts; pinned slot range tested separately");
 }
}'''.replace('METHODS','\n'.join(method(n) for n in ['observeAutomationSlot','observeAutomationInventorySlot']))
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-exchange-canonical-') as d:
 f=Path(d)/'PestExchangeCanonicalNativeTest.java';f.write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(root/'src/main/java')+':'+str(root/'src/test/java'),'-d',d,str(f)],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.automation.pest.PestExchangeCanonicalNativeTest'],check=True,timeout=30)
