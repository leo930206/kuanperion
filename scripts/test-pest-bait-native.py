#!/usr/bin/env python3
"""Run the production narrow native bait method body against actual private transfer proofs and physical fixtures."""
from pathlib import Path
import subprocess,tempfile,sys,re
root=Path(__file__).resolve().parent.parent
jdk=Path(sys.argv[1]) if len(sys.argv)>1 else Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home'
s=(root/'src/main/java/dev/kuan/kuanperion/AutomationMenuActions.java').read_text()
start=s.index(' static boolean sendPestBaitNative(');brace=s.index('{',start);depth=1;end=brace+1
while depth:
 depth+=(s[end]=='{')-(s[end]=='}');end+=1
body=s[start:end]
assert re.search(r'^ static boolean sendPestBaitNative\(',s,re.M),'No public generic carried-item path'
assert {p.name for p in (root/'src/main/java').rglob('*.java') if 'AutomationMenuActions.sendPestBaitNative(' in p.read_text()}=={'PestFlightController.java'}
harness=r'''
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public class PestBaitNativeBodyTest {
 static PestBaitButtonTransferTest.H h;static PestBaitButtonTransfer.Action action;
 static void reset(String step){h=new PestBaitButtonTransferTest.H();if(step.equals("return"))h.deposited();else if(!step.equals("pickup"))h.picked();var m=new Minecraft();Minecraft.instance=m;m.level=h.level;m.player.containerMenu.identity=h.identity;m.frame=h.frame();for(int i=0;i<43;i++)m.player.inventory.items.put(i,new Stack(h.inv.get(i)));for(var slot:h.menu.slots()){int raw=slot.index()<27?slot.index():slot.index()<54?slot.index()-27+9:slot.index()-54;m.player.containerMenu.slots.add(new NativeSlot(slot.index(),slot.playerInventory()?m.player.inventory:m.external,raw,new Stack(slot.item())));}m.player.containerMenu.carried=new Stack(h.carried);action=h.proof.arm(h.frame(),h.now).orElseThrow();}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static boolean send(){return AutomationMenuActions.sendPestBaitNative(h.owner,action,h.frame(),h.now);}
 public static void main(String[]args){
  for(String step:List.of("pickup","deposit")){reset(step);check(send()&&Minecraft.instance.gameMode.calls==1&&!AutomationMenuActions.ownedInput,"valid source-style "+step);check(!send()&&Minecraft.instance.gameMode.calls==1,"private action is one-use");}
  reset("deposit");check(send()&&h.proof.sent(action,GuardedMenuAction.Result.SENT,h.now),"deposit actual private proof");Minecraft.instance.gameMode.calls=0;h.now=3*PestBaitButtonTransferTest.S;h.carried=PestBaitButtonPlanTest.bait(12);h.refresh(h.marker);h.cursors.observe(1,7,h.carried,h.now);h.traps=new PestTrapObservation(Set.of(),true,Set.of(),true);h.proof.poll(h.frame(),h.now);
  // Build the native return fixture from the proof's already accepted remainder without reacquiring a root.
  var m=Minecraft.instance;m.frame=h.frame();m.player.containerMenu.carried=new Stack(h.carried);for(var slot:h.menu.slots())m.player.containerMenu.slots.get(slot.index()).stack=new Stack(slot.item());for(int i=0;i<43;i++)m.player.inventory.items.put(i,new Stack(h.inv.get(i)));action=h.proof.arm(h.frame(),h.now).orElseThrow();check(send()&&m.gameMode.calls==1,"owned remainder returns to exact original empty main slot");
  for(String mode:List.of("thread","level","identity","container","cursor","inventory","alias","external","fake","inactive","placement","capacity","nested","revoked","stale","late-placement","late-alias","late-inventory","late-cursor")){
   reset("deposit");m=Minecraft.instance;var slot=m.player.containerMenu.slots.get(13);
   switch(mode){case "thread"->m.thread=false;case "level"->m.level=new Object();case "identity"->m.player.containerMenu.identity=new Object();case "container"->m.player.containerMenu.containerId=8;case "cursor"->m.player.containerMenu.carried=new Stack(h.marker);case "inventory"->m.player.inventory.items.put(42,new Stack(PestBaitButtonPlanTest.bait(1)));case "external"->slot.container=m.player.inventory;case "fake"->slot.fake=true;case "inactive"->slot.active=false;case "placement"->slot.allowed=false;case "capacity"->slot.capacity=1;case "nested"->AutomationMenuActions.ownedInput=true;case "revoked"->h.session.cancel(h.now);case "stale"->m.frame=null;case "alias"->{reset("pickup");m=Minecraft.instance;m.player.containerMenu.slots.get(27).raw=10;}case "late-placement"->{var late=m;h.read=()->{h.read=()->h.ctx;late.player.containerMenu.slots.get(13).allowed=false;return h.ctx;};}case "late-alias"->{reset("pickup");m=Minecraft.instance;var late=m;h.read=()->{h.read=()->h.ctx;late.player.containerMenu.slots.get(27).raw=10;return h.ctx;};}case "late-inventory"->{var late=m;h.read=()->{h.read=()->h.ctx;late.player.inventory.items.put(42,new Stack(PestBaitButtonPlanTest.bait(1)));return h.ctx;};}case "late-cursor"->{var late=m;h.read=()->{h.read=()->h.ctx;late.player.containerMenu.carried=new Stack(h.marker);return h.ctx;};}}
   check(!send()&&m.gameMode.calls==0,mode+" cannot send a native click");AutomationMenuActions.ownedInput=false;
  }
  reset("deposit");Minecraft.instance.gameMode.failure=true;boolean threw=false;try{send();}catch(IllegalStateException expected){threw=true;}check(threw&&Minecraft.instance.gameMode.calls==1&&!AutomationMenuActions.ownedInput,"possibly-sent native error restores ownership and is not retried");
  System.out.println("PestBaitNativeBodyTest: PASS (actual sender body + real private proofs; physical fixtures, not live server)");
 }
}
enum ContainerInput{PICKUP,QUICK_MOVE,THROW}
final class Stack{final ItemSnapshot snapshot;Stack(ItemSnapshot s){snapshot=s;}boolean isEmpty(){return snapshot.count()==0;}int getCount(){return snapshot.count();}}
final class NativeSlot{int index,raw,capacity=64;boolean active=true,fake,allowed=true;Object container;Stack stack;NativeSlot(int i,Object c,int r,Stack s){index=i;container=c;raw=r;stack=s;}int getContainerSlot(){return raw;}Stack getItem(){return stack;}boolean isActive(){return active;}boolean isFake(){return fake;}boolean mayPlace(Stack item){return allowed;}boolean mayPickup(NativePlayer p){return allowed;}int getMaxStackSize(Stack item){return capacity;}}
final class NativeInventory{Map<Integer,Stack> items=new HashMap<>();int getContainerSize(){return 43;}Stack getItem(int i){return items.get(i);}}
final class NativeMenu{int containerId=7;Object identity;List<NativeSlot> slots=new ArrayList<>();Stack carried;Stack getCarried(){return carried;}}
final class NativePlayer{final NativeInventory inventory=new NativeInventory();final NativeMenu containerMenu=new NativeMenu();NativeInventory getInventory(){return inventory;}}
final class NativeMode{int calls;boolean failure;void handleContainerInput(int id,int slot,int button,ContainerInput input,NativePlayer p){if(!AutomationMenuActions.ownedInput||id!=7||button!=0||input!=ContainerInput.PICKUP)throw new AssertionError("wrong effect");calls++;if(failure)throw new IllegalStateException("possibly sent");}}
final class Minecraft{static Minecraft instance;boolean thread=true;Object level,external=new Object();NativePlayer player=new NativePlayer();NativeMode gameMode=new NativeMode();PestBaitButtonTransfer.Frame frame;static Minecraft getInstance(){return instance;}boolean isSameThread(){return thread;}}
final class AutomationGameView{static MenuSnapshot menu(Minecraft m,long w){return m.frame==null?null:m.frame.menu();}static ItemSnapshot item(Stack s,boolean protection){var x=s.snapshot;return new ItemSnapshot(x.skyblockId(),x.minecraftId(),x.uuid(),x.name(),x.fingerprint(),x.count(),x.lore(),x.count()==0||protection||x.protectedItem());}}
'''
# The production identity is the actual menu object; the fixture models its immutable identity field.
body=body.replace('menu!=frame.menuIdentity()','menu.identity!=frame.menuIdentity()')
with tempfile.TemporaryDirectory(prefix='kuanperion-bait-native-body-') as d:
 t=Path(d);(t/'PestBaitNativeBodyTest.java').write_text(harness)
 (t/'AutomationMenuActions.java').write_text('package dev.kuan.kuanperion.automation.pest;import dev.kuan.kuanperion.automation.*;import java.util.*;final class AutomationMenuActions{static boolean ownedInput;'+body+'}')
 cp=str(root/'src/main/java')+':'+str(root/'src/test/java')
 subprocess.run([str(jdk/'bin/javac'),'--release','21','-encoding','UTF-8','-sourcepath',cp,'-d',d,str(t/'PestBaitNativeBodyTest.java'),str(t/'AutomationMenuActions.java')],check=True,capture_output=True,text=True,timeout=90)
 r=subprocess.run([str(jdk/'bin/java'),'-cp',d,'dev.kuan.kuanperion.automation.pest.PestBaitNativeBodyTest'],capture_output=True,text=True,timeout=30)
 assert r.returncode==0,r.stdout+r.stderr
 print(r.stdout,end='')
