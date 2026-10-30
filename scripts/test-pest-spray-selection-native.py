#!/usr/bin/env python3
"""Actual selection controller/menu-sender bodies + real private task actions, controlled physical wrappers.
The shared native frame and component-invariant adapters have separate tests; no applied-Mixin/live-server claim.
"""
from pathlib import Path
import re,subprocess,tempfile,sys
root=Path(__file__).resolve().parents[1];base=root/'src/main/java/dev/kuan/kuanperion'
def method(file,name):
 s=(base/file).read_text();m=re.search(r' (?:public |private )?(?:static )?[^{}\n]+ '+name+r'\([^\n]*?\)\{',s);assert m,(file,name)
 end=m.end();depth=1
 while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
 text=s[m.start():end]
 for old,new in [('dev.kuan.kuanperion.mixin.MinecraftAttackAccess','MinecraftAttackAccess'),('dev.kuan.kuanperion.mixin.SprayCarriedItemAccess','SprayCarriedItemAccess'),('net.minecraft.world.phys.HitResult','HitResult'),('net.minecraft.core.component.DataComponents','DataComponents')]:text=text.replace(old,new)
 return text
java=r'''package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
public final class PestSpraySelectionNativeTest {
 static P current;static Native edge;static int swings,syncs,clicks,closes,resets;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class P extends PestSpraySelectionTaskTest.F {
  String mode="ok";PestSpraySelectionTask.Action action;
  P(){player=new Player();level=new Level();}
  @Override public AutomationContext context(){return edge!=null&&edge.effectInProgress?edge.context():context;}
  @Override void render(){super.render();if(menuIdentity instanceof Menu m)m.snapshot=menu;}
  @Override void opening(){menuIdentity=new Menu(7);render();for(var s:menu.slots())items.observe(1,7,s.index(),s.item());cursors.observe(1,7,PestSprayPlanTest.AIR,now);}
  @Override public PestSprayTask.Effect selectionEffect(AutomationSession.Handle h,PestSpraySelectionTask.Action a,long at){
   action=a;setup(this);var m=Minecraft.instance;var p=m.player;
   switch(mode){case "thread"->m.thread=false;case "hit-block"->m.hitResult=new HitResult(HitResult.Type.BLOCK);case "hit-entity"->m.hitResult=new HitResult(HitResult.Type.ENTITY);case "cooldown"->m.missTime=1;case "hands-busy"->p.busy=true;case "spectator"->m.gameMode.spectator=true;case "destroying"->m.gameMode.destroying=true;case "disabled-item"->p.inventory.items.get(4).enabled=false;case "cannot-attack"->p.cannotAttack=true;case "piercing"->p.inventory.items.get(4).piercing=true;case "focus"->GLFW.focus=false;case "physical-key"->m.manual=true;case "cursor"->p.containerMenu.carried=new Stack(PestSprayPlanTest.material(1));case "throw-sync"->m.gameMode.throwSync=true;case "throw-click"->m.gameMode.throwClick=true;case "throw-close"->p.throwClose=true;}
   if(a.kind()==PestSpraySelectionTask.Kind.CLICK){var slot=p.containerMenu.slots.get(13);switch(mode){case "slot-fake"->slot.fake=true;case "slot-inactive"->slot.active=false;case "slot-denied"->slot.pickup=false;case "slot-index"->slot.index=14;case "slot-player"->slot.container=p.inventory;}}
   edge.contextCalls=0;edge.late=mode.startsWith("late-")?mode.substring(5):"";edge.afterSync=mode.startsWith("sync-")?mode.substring(5):"";
   var result=edge.selectionEffect(h,a,at);
   if(result==PestSprayTask.Effect.SENT)switch(a.kind()){case OPEN->{opens++;opening();}case CLICK->this.clicks++;case CLOSE->{this.closes++;menu=null;menuIdentity=null;context=new AutomationContext(1,true,true,Set.of());}}
   return result;
  }
 }
 interface MinecraftAttackAccess{int kuanperion$getMissTime();}interface SprayCarriedItemAccess{void kuanperion$syncSprayCarriedItem();}
 enum InteractionHand{MAIN_HAND}enum ContainerInput{PICKUP}
 static class DataComponents{static final Object PIERCING_WEAPON=new Object();}
 static class Stack{ItemSnapshot item;boolean enabled=true,piercing;Stack(ItemSnapshot i){item=i;}boolean isEmpty(){return item.count()==0;}boolean isItemEnabled(Object flags){return enabled;}boolean has(Object type){return piercing;}}
 static class Inventory{Map<Integer,Stack> items=new HashMap<>();int selected=4;int getSelectedSlot(){return selected;}Stack getItem(int i){return items.get(i);}}
 static class Slot{int index;Object container;Stack item;boolean fake,active=true,pickup=true;Slot(int i,Object c,Stack x){index=i;container=c;item=x;}boolean isFake(){return fake;}boolean isActive(){return active;}boolean mayPickup(Player p){return pickup;}Stack getItem(){return item;}}
 static class Menu{int containerId;List<Slot> slots=new ArrayList<>();Stack carried=new Stack(PestSprayPlanTest.AIR);MenuSnapshot snapshot;Menu(int id){containerId=id;}Stack getCarried(){return carried;}}
 static class Level{Object enabledFeatures(){return this;}}
 static class Player{Inventory inventory=new Inventory();Menu inventoryMenu=new Menu(0),containerMenu=inventoryMenu;boolean alive=true,spectator,passenger,sleeping,using,busy,cannotAttack,throwClose;double x=0,y=72,z=-96;float yaw=20,pitch=0;long epoch=1,sequence;String invariant;
  Inventory getInventory(){return inventory;}boolean isAlive(){return alive;}boolean isSpectator(){return spectator;}boolean isPassenger(){return passenger;}boolean isSleeping(){return sleeping;}boolean isUsingItem(){return using;}boolean isHandsBusy(){return busy;}Stack getItemInHand(InteractionHand hand){return inventory.getItem(inventory.selected);}boolean cannotAttackWithItem(Stack s,int kind){return cannotAttack;}void resetAttackStrengthTicker(){resets++;}void swing(InteractionHand h){swings++;}void closeContainer(){closes++;check(AutomationMenuActions.ownedInput,"only narrow private action owns the close input");if(throwClose)throw new IllegalStateException("possibly sent close");containerMenu=inventoryMenu;Minecraft.instance.gui.screen=null;}}
 static class Mode implements SprayCarriedItemAccess{boolean spectator,destroying,throwSync,throwClick;boolean isSpectator(){return spectator;}boolean isDestroying(){return destroying;}public void kuanperion$syncSprayCarriedItem(){syncs++;if(throwSync)throw new IllegalStateException("possibly sent slot sync");mutate(edge.afterSync);}void handleContainerInput(int id,int slot,int button,ContainerInput input,Player p){clicks++;check(AutomationMenuActions.ownedInput&&id==7&&slot==13&&button==0&&input==ContainerInput.PICKUP,"exact ordinary no-carried menu click only");if(throwClick)throw new IllegalStateException("possibly sent click");}}
 static class HitResult{enum Type{MISS,BLOCK,ENTITY}Type type;HitResult(Type t){type=t;}Type getType(){return type;}}
 static class AbstractContainerScreen<T>{Menu menu;AbstractContainerScreen(Menu m){menu=m;}Menu getMenu(){return menu;}}
 static class Gui{Object screen,overlay;Object screen(){return screen;}Object overlay(){return overlay;}}
 static class Window{long handle(){return 1;}}
 static class Minecraft implements MinecraftAttackAccess{static Minecraft instance;Level level;Player player;Mode gameMode=new Mode();Gui gui=new Gui();HitResult hitResult=new HitResult(HitResult.Type.MISS);boolean thread=true,manual;int missTime;boolean isSameThread(){return thread;}Window getWindow(){return new Window();}public int kuanperion$getMissTime(){return missTime;}static Minecraft getInstance(){return instance;}}
 static class GLFW{static final int GLFW_FOCUSED=1,GLFW_TRUE=1;static boolean focus=true;static int glfwGetWindowAttrib(long w,int a){return focus?1:0;}}
 static class AutomationGameView{static MenuSnapshot menu(Minecraft m,long world){return m.gui.screen instanceof AbstractContainerScreen<?> s&&s.getMenu()==m.player.containerMenu?m.player.containerMenu.snapshot:null;}static ItemSnapshot item(Stack s,boolean equipment){return s.item;}}
 static class PestSpraySelectionGameView{static String invariant(Stack s){return Minecraft.instance.player.invariant;}}
 static class AutomationMenuActions{static boolean ownedInput;static boolean ownsInventoryInput(){return ownedInput;}
 MENU_METHOD
 }
 static class Native {
  AutomationCoordinator coordinator;AutomationSession session;AutomationSession.Handle dataOwner;Object dataLevel,dataPlayer;String materialName="Test Material",late="",afterSync="";int contextCalls;boolean effectInProgress;
  AutomationContext context(){if(++contextCalls==2)mutate(late);return current.context;}boolean manualInput(){return Minecraft.instance.manual;}
  PestSprayTask.Frame nativeFrame(long world,long now,String material){var m=Minecraft.instance;var p=m.player;var inv=new HashMap<Integer,ItemSnapshot>();for(var e:p.inventory.items.entrySet())inv.put(e.getKey(),e.getValue().item);return new PestSprayTask.Frame(world,now,m.level,p,inv,p.containerMenu.carried.item,p.inventory.selected,m.gui.screen==null&&m.gui.overlay==null&&p.containerMenu==p.inventoryMenu&&GLFW.focus,p.using,p.x,p.y,p.z,0,0,0,p.yaw,p.pitch,new PestSprayObservation(PestSprayObservation.State.NONE),p.epoch,p.sequence,null);}
 METHODS
 }
 static void mutate(String what){var m=Minecraft.instance;var p=m.player;switch(what){case "level"->m.level=new Level();case "player"->m.player=new Player();case "game-mode"->m.gameMode=new Mode();case "position"->p.x++;case "look"->p.yaw++;case "inventory"->p.inventory.items.put(42,new Stack(PestSprayPlanTest.material(1)));case "invariant"->p.invariant="unknown-NBT-change";case "cursor"->p.containerMenu.carried=new Stack(PestSprayPlanTest.material(1));case "focus"->GLFW.focus=false;case "physical-key"->m.manual=true;case "selected"->p.inventory.selected=8;case "correction"->p.sequence++;case "menu"->m.gui.screen=new Object();case "cancel"->current.task.cancel();}}
 static void setup(P f){var m=new Minecraft();Minecraft.instance=m;m.level=(Level)f.level;m.player=(Player)f.player;var p=m.player;p.inventory=new Inventory();for(var e:f.inv.entrySet())p.inventory.items.put(e.getKey(),new Stack(e.getValue()));p.inventory.selected=f.selected;p.invariant=f.invariant;p.x=f.x;p.z=f.z;p.yaw=f.yaw;p.epoch=f.positionEpoch;p.sequence=f.positionSequence;p.containerMenu=f.menu==null?p.inventoryMenu:(Menu)f.menuIdentity;p.containerMenu.carried=new Stack(f.cursor);p.containerMenu.slots.clear();if(f.menu!=null){p.containerMenu.snapshot=f.menu;for(var slot:f.menu.slots())p.containerMenu.slots.add(new Slot(slot.index(),slot.playerInventory()?p.inventory:new Object(),new Stack(slot.item())));m.gui.screen=new AbstractContainerScreen<>(p.containerMenu);}GLFW.focus=true;}
 static P make(String mode){current=new P();edge=new Native();edge.coordinator=current.coordinator;edge.session=current.session;edge.dataOwner=current.owner;edge.dataLevel=current.level;edge.dataPlayer=current.player;current.mode=mode;swings=syncs=clicks=closes=resets=0;return current;}
 static void clicked(P p){p.begin();for(int i=0;i<8&&p.clicks==0&&p.task.active();i++)p.tick();}
 public static void main(String[]args){
  var p=make("ok");clicked(p);check(p.task.state()==PestSpraySelectionTask.State.AWAITING&&syncs==1&&swings==1&&clicks==1&&resets==1,"normal carried sync+MISS swing then one private material-role click");p.selected(true,true);p.tick();p.tick();check(p.task.state()==PestSpraySelectionTask.State.COMPLETE&&closes==1&&p.session.isCurrent(p.owner)&&p.session.holdsFarming()&&!AutomationMenuActions.ownedInput&&!edge.effectInProgress,"native acknowledged close leaves original root for parent use");check(edge.selectionEffect(p.owner,p.action,p.now)==PestSprayTask.Effect.REJECTED&&closes==1,"private native capability cannot replay after child complete");
  for(String mode:List.of("thread","hit-block","hit-entity","cooldown","hands-busy","spectator","destroying","disabled-item","cannot-attack","piercing","focus","physical-key","cursor","late-level","late-player","late-game-mode","late-position","late-look","late-inventory","late-invariant","late-cursor","late-focus","late-physical-key","late-selected","late-correction","late-menu","late-cancel")){
   p=make(mode);p.begin();p.tick();check(!p.task.active()&&swings==0&&clicks==0&&closes==0&&syncs==0,"no physical effect after rejected source-style opener: "+mode);
  }
  for(String mode:List.of("sync-level","sync-player","sync-game-mode","sync-position","sync-look","sync-inventory","sync-invariant","sync-cursor","sync-focus","sync-physical-key","sync-selected","sync-correction","sync-menu","sync-cancel","throw-sync")){
   p=make(mode);p.begin();p.tick();check(!p.task.active()&&p.task.state()==(mode.equals("sync-cancel")?PestSpraySelectionTask.State.CANCELLED:PestSpraySelectionTask.State.UNCERTAIN)&&syncs==1&&swings==0&&clicks==0&&closes==0,"late sync outcome never retries, swings after takeover or closes unknown menu: "+mode+" state="+p.task.state()+" syncs="+syncs+" swings="+swings);
  }
  for(String mode:List.of("slot-fake","slot-inactive","slot-denied","slot-index","slot-player")){
   p=make("ok");p.begin();while(p.task.state()!=PestSpraySelectionTask.State.READY&&p.task.active())p.tick();p.mode=mode;p.tick();check(!p.task.active()&&clicks==0&&closes==0&&swings==1,"native physical role gate: "+mode);
  }
  p=make("ok");p.begin();while(p.task.state()!=PestSpraySelectionTask.State.READY&&p.task.active())p.tick();p.mode="throw-click";p.tick();check(p.task.state()==PestSpraySelectionTask.State.UNCERTAIN&&clicks==1&&!AutomationMenuActions.ownedInput,"possibly sent menu click is never repeated, owned flag released");
  p=make("ok");clicked(p);p.selected(true,true);p.tick();p.mode="throw-close";p.tick();check(p.task.state()==PestSpraySelectionTask.State.UNCERTAIN&&closes==1&&!AutomationMenuActions.ownedInput&&!edge.effectInProgress,"possibly sent close is not success/retry and both native flags are released");
  System.out.println("PestSpraySelectionNativeTest: PASS actual controller/menu-sender bodies + real private capabilities; MISS-only sync/swing, physical/late/sync/menu/uncertain gates; controlled wrappers, no Mixin/live claim");
 }
}'''.replace('METHODS','\n'.join(method('PestSprayController.java',n) for n in ['selectionFrame','nativeSelectionFrame','selectionAllowed','airSwingReady','selectionEffect'])).replace('MENU_METHOD',method('AutomationMenuActions.java','sendPestSpraySelectionNative'))
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-selection-native-') as d:
 f=Path(d)/'PestSpraySelectionNativeTest.java';f.write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',(str(root/'build/classes')+':' if '--built' in sys.argv else '')+str(gson),'-sourcepath',(str(root/'src/main/java')+':' if '--built' not in sys.argv else '')+str(root/'src/test/java'),'-d',d,str(f)],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 if '--built' in sys.argv:assert not any((root/'build/classes'/c.relative_to(d)).exists() for c in Path(d).rglob('*.class')),'No production bytecode may be shadowed'
 subprocess.run([str(jdk/'java'),'-cp',d+':'+(str(root/'build/classes')+':' if '--built' in sys.argv else '')+str(gson),'dev.kuan.kuanperion.automation.pest.PestSpraySelectionNativeTest'],check=True,timeout=30)
# No broad world-input bypass, arbitrary click or guessed pose/commands were added.
source=(base/'PestSprayController.java').read_text();assert 'startAttack(' not in source and '.setYRot(' not in source and '.setXRot(' not in source and '.setPos(' not in source and '.setDown(' not in source
mix=(base/'mixin/SprayCarriedItemAccess.java').read_text();assert '@Invoker("ensureHasSentCarriedItem")' in mix
print('PestSpraySelectionNativeBoundaries: PASS fixed source-only opener and private menu action path; no world-input exemption/pose or command shortcut')

import json
mixins=json.loads((root/'src/main/resources/kuanperion.mixins.json').read_text());assert mixins['client'].count('SprayCarriedItemAccess')==1
assert '@Accessor("missTime") int kuanperion$getMissTime();' in (base/'mixin/MinecraftAttackAccess.java').read_text()
native=subprocess.check_output([str(jdk/'javap'),'-c','-p','-classpath',str(p/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'),'net.minecraft.client.multiplayer.MultiPlayerGameMode'],text=True)
sync=native.split('private void ensureHasSentCarriedItem();',1)[1].split('\n  public ',1)[0]
assert 'carriedIndex:I' in sync and 'getSelectedSlot:()I' in sync and 'ServerboundSetCarriedItemPacket' in sync
print('SprayCarriedSyncPin: PASS real26.2 private ordinary selected-slot synchronization target/source Mixin registration; no applied-Mixin claim')
