#!/usr/bin/env python3
"""Execute actual private exchange click/close bodies with real task capabilities and controlled native fixtures."""
from pathlib import Path
import subprocess,tempfile,re
root=Path(__file__).resolve().parents[1];source=(root/'src/main/java/dev/kuan/kuanperion/AutomationMenuActions.java').read_text()
def extract(name):
 start=source.index(' static boolean '+name+'(');end=source.index('{',start)+1;depth=1
 while depth:depth+=(source[end]=='{')-(source[end]=='}');end+=1
 return source[start:end]
bodies=[extract(n) for n in ['sendPestExchangeNative','closePestExchangeNative']]
for name in ['sendPestExchangeNative','closePestExchangeNative']:
 assert re.search(r'^ static boolean '+name+r'\(',source,re.M),'Only package-private narrow effect gates'
 assert {p.name for p in (root/'src/main/java').rglob('*.java') if 'AutomationMenuActions.'+name+'(' in p.read_text()}=={'PestFlightController.java'},name
harness=r'''
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public class PestExchangeNativeBodyTest {
 static P p;static PestExchangeTask task;static PestExchangeTask.Action last;static PestExchangeTask.Frame lastFrame;
 static class P extends PestExchangeTaskTest.P {
  String clickMode="ok",closeMode="ok";
  public GuardedMenuAction.Result exchangeClick(AutomationSession.Handle h,PestExchangeTask.Action a,long n){setup(this);last=a;lastFrame=exchangeFrame(n);mutate(clickMode);boolean sent=AutomationMenuActions.sendPestExchangeNative(h,a,lastFrame,n);clicks+=Minecraft.instance.gameMode.calls;return sent?GuardedMenuAction.Result.SENT:GuardedMenuAction.Result.REJECTED;}
  public boolean closeExchange(AutomationSession.Handle h,PestExchangeTask.Action a,long n){setup(this);last=a;lastFrame=exchangeFrame(n);mutate(closeMode);return AutomationMenuActions.closePestExchangeNative(h,a,lastFrame,n);}
 }
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void setup(P value){p=value;var m=new Minecraft();Minecraft.instance=m;m.level=p.level;m.player.containerMenu.identity=p.identity;m.player.containerMenu.containerId=p.menu.containerId();m.gui.current=new AbstractContainerScreen<>(m.player.containerMenu);for(int i=0;i<43;i++)m.player.inventory.items.put(i,new Stack(p.inv.get(i)));for(var s:p.menu.slots()){int raw=s.index()<27?s.index():s.index()<54?s.index()-27+9:s.index()-54;m.player.containerMenu.slots.add(new Slot(s.index(),s.playerInventory()?m.player.inventory:m.external,raw,new Stack(s.item())));}m.player.containerMenu.carried=new Stack(p.cursor);}
 static void mutate(String mode){var m=Minecraft.instance;var slot=m.player.containerMenu.slots.get(13);
  switch(mode){case "thread"->m.thread=false;case "level"->m.level=new Object();case "identity"->m.player.containerMenu.identity=new Object();case "id"->m.player.containerMenu.containerId=8;case "screen"->m.gui.current=new Object();case "foreign-screen-menu"->m.gui.current=new AbstractContainerScreen<>(new Menu());case "overlay"->m.gui.cover=new Object();case "selected"->m.player.inventory.selected=4;case "cursor"->m.player.containerMenu.carried=new Stack(PestBaitButtonPlanTest.bait(1));case "inventory"->m.player.inventory.items.put(42,new Stack(PestBaitButtonPlanTest.bait(1)));case "external"->slot.container=m.player.inventory;case "fake"->slot.fake=true;case "inactive"->slot.active=false;case "pickup"->slot.allowed=false;case "slot"->slot.index=12;case "item"->slot.stack=new Stack(PestBaitButtonPlanTest.bait(1));case "nested"->AutomationMenuActions.ownedInput=true;case "revoked"->p.session.cancel(p.now);case "dead"->m.player.alive=false;case "spectator"->m.player.spectator=true;case "using"->m.player.using=true;case "mayfly"->m.player.abilities.mayfly=false;case "throw"->m.gameMode.failure=true;case "close-throw"->m.player.failure=true;
   case "late-inventory"->p.read=()->{p.read=()->p.ctx;m.player.inventory.items.put(42,new Stack(PestBaitButtonPlanTest.bait(1)));return p.ctx;};
   case "late-pickup"->p.read=()->{p.read=()->p.ctx;slot.allowed=false;return p.ctx;};
   case "late-screen"->p.read=()->{p.read=()->p.ctx;m.gui.current=new Object();return p.ctx;};
   case "late-level"->p.read=()->{p.read=()->p.ctx;m.level=new Object();return p.ctx;};
   case "late-player"->p.read=()->{p.read=()->p.ctx;m.player=new Player();return p.ctx;};
   case "late-selected"->p.read=()->{p.read=()->p.ctx;m.player.inventory.selected=4;return p.ctx;};
   case "late-cursor"->p.read=()->{p.read=()->p.ctx;m.player.containerMenu.carried=new Stack(PestBaitButtonPlanTest.bait(1));return p.ctx;};
   case "late-dead"->p.read=()->{p.read=()->p.ctx;m.player.alive=false;return p.ctx;};
   case "late-menu"->p.read=()->{p.read=()->p.ctx;p.marker=PestExchangeViewTest.role("Unknown status");p.refresh();return p.ctx;};
   case "late-cancel"->p.read=()->{p.read=()->p.ctx;task.cancel();return p.ctx;};
  }
 }
 static void ready(String mode){p=new P();p.clickMode=mode;task=PestExchangeTaskTest.begin(p);PestExchangeTaskTest.ready(p,task);p.now+=PestExchangeTaskTest.S;task.tick(p.now);AutomationMenuActions.ownedInput=false;}
 public static void main(String[]args){
  ready("ok");check(task.state()==PestExchangeTask.State.AWAITING&&p.clicks==1&&!AutomationMenuActions.ownedInput,"private production click sends exactly once");check(!AutomationMenuActions.sendPestExchangeNative(p.owner,last,lastFrame,p.now)&&Minecraft.instance.gameMode.calls==1,"cannot replay accepted action");PestExchangeTaskTest.completed(p,task);check(!AutomationMenuActions.closePestExchangeNative(p.owner,last,lastFrame,p.now)&&p.closes==1,"cannot replay settled close");
  for(String mode:List.of("thread","level","identity","id","screen","foreign-screen-menu","overlay","selected","dead","spectator","using","mayfly","cursor","inventory","external","fake","inactive","pickup","slot","item","nested","revoked","late-inventory","late-pickup","late-screen","late-level","late-player","late-selected","late-cursor","late-dead","late-menu","late-cancel")){
   ready(mode);check(!task.active()&&Minecraft.instance.gameMode.calls==0&&p.closes==0,mode+" blocks physical input/cleanup");
  }
  ready("throw");check(task.state()==PestExchangeTask.State.UNCERTAIN&&Minecraft.instance.gameMode.calls==1&&!AutomationMenuActions.ownedInput,"possibly sent native exception restores flag and never retries");
  for(String mode:List.of("thread","level","identity","id","screen","foreign-screen-menu","overlay","selected","dead","spectator","using","mayfly","cursor","inventory","nested","revoked","late-inventory","late-screen","late-level","late-player","late-selected","late-cursor","late-dead","late-menu","late-cancel","close-throw")){
   ready("ok");p.ack(true,true);task.tick(p.now);p.closeMode=mode;task.tick(p.now);check(mode.equals("nested")==AutomationMenuActions.ownedInput,"close guard preserves existing owner but restores its own effect flag: "+mode);AutomationMenuActions.ownedInput=false;check(!task.active()&&p.closes==0,mode+" blocks final physical close");
  }
  System.out.println("PestExchangeNativeBodyTest: PASS actual private click/close bodies + real one-use task proofs, controlled physical fixtures; no client/server operation");
 }
}
enum ContainerInput{PICKUP}
final class Stack{final ItemSnapshot snapshot;Stack(ItemSnapshot x){snapshot=x;}boolean isEmpty(){return snapshot.count()==0;}}
final class Slot{int index,raw;Object container;Stack stack;boolean active=true,fake,allowed=true;Slot(int i,Object c,int r,Stack s){index=i;container=c;raw=r;stack=s;}boolean isActive(){return active;}boolean isFake(){return fake;}boolean mayPickup(Player p){return allowed;}Stack getItem(){return stack;}}
final class Inventory{int selected=3;Map<Integer,Stack> items=new HashMap<>();int getContainerSize(){return 43;}int getSelectedSlot(){return selected;}Stack getItem(int i){return items.get(i);}}
final class Menu{int containerId=7;Object identity;List<Slot> slots=new ArrayList<>();Stack carried=new Stack(PestExchangeViewTest.AIR);Stack getCarried(){return carried;}}
final class AbstractContainerScreen<T>{final Menu menu;AbstractContainerScreen(Menu m){menu=m;}Menu getMenu(){return menu;}}
final class Abilities{boolean mayfly=true;}
final class Player{boolean failure,alive=true,spectator,using;final Abilities abilities=new Abilities();boolean isAlive(){return alive;}boolean isSpectator(){return spectator;}boolean isPassenger(){return false;}boolean isSleeping(){return false;}boolean isUsingItem(){return using;}Abilities getAbilities(){return abilities;}final Inventory inventory=new Inventory();Menu containerMenu=new Menu();final Menu inventoryMenu=new Menu();Inventory getInventory(){return inventory;}void closeContainer(){if(!AutomationMenuActions.ownedInput)throw new AssertionError("close input ownership");if(failure)throw new IllegalStateException("native close failure");PestExchangeNativeBodyTest.p.closes++;PestExchangeNativeBodyTest.p.menu=null;PestExchangeNativeBodyTest.p.ctx=new AutomationContext(1,true,true,Set.of());containerMenu=inventoryMenu;Minecraft.instance.gui.current=null;}}
final class Gui{Object current,cover;Object screen(){return current;}Object overlay(){return cover;}}
final class Mode{int calls;boolean failure;void handleContainerInput(int id,int slot,int button,ContainerInput input,Player p){if(!AutomationMenuActions.ownedInput||id!=7||slot!=13||button!=0||input!=ContainerInput.PICKUP)throw new AssertionError("invalid narrow input");calls++;if(failure)throw new IllegalStateException("possibly sent");}}
final class Minecraft{static Minecraft instance;boolean thread=true;Object level,external=new Object();Player player=new Player();final Gui gui=new Gui();final Mode gameMode=new Mode();static Minecraft getInstance(){return instance;}boolean isSameThread(){return thread;}}
final class AutomationGameView{static MenuSnapshot menu(Minecraft mc,long world){return PestExchangeNativeBodyTest.p.menu;}static ItemSnapshot item(Stack s,boolean protect){var x=s.snapshot;return new ItemSnapshot(x.skyblockId(),x.minecraftId(),x.uuid(),x.name(),x.fingerprint(),x.count(),x.lore(),protect||x.count()==0||x.protectedItem());}}
'''
# Only the physical menu identity expression is substituted: a controlled fixture carries its native identity field.
bodies=[s.replace('menu!=frame.menuIdentity()','menu.identity!=frame.menuIdentity()') for s in bodies]
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-exchange-native-body-') as d:
 t=Path(d);(t/'PestExchangeNativeBodyTest.java').write_text(harness)
 (t/'AutomationMenuActions.java').write_text('package dev.kuan.kuanperion.automation.pest;import dev.kuan.kuanperion.automation.*;final class AutomationMenuActions{static boolean ownedInput;'+''.join(bodies)+'}')
 r=subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(root/'src/main/java')+':'+str(root/'src/test/java'),'-d',d,str(t/'PestExchangeNativeBodyTest.java'),str(t/'AutomationMenuActions.java')],capture_output=True,text=True,timeout=90)
 assert r.returncode==0,r.stdout+r.stderr
 r=subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.automation.pest.PestExchangeNativeBodyTest'],capture_output=True,text=True,timeout=30)
 assert r.returncode==0,r.stdout+r.stderr
 print(r.stdout,end='')
