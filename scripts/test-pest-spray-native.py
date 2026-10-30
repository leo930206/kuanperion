#!/usr/bin/env python3
"""Real selected-spray native bodies + real one-use task capabilities, with explicit physical fixtures."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
s=(root/'src/main/java/dev/kuan/kuanperion/PestSprayController.java').read_text()
def method(name):
 m=re.search(r' (?:public|private) [^{}\n]+ '+name+r'\([^\n]*?\)\{',s);assert m,name
 e=m.end();d=1
 while d:d+=(s[e]=='{')-(s[e]=='}');e+=1
 return s[m.start():e]
java=r'''package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.Supplier;import java.util.concurrent.CompletableFuture;
public final class PestSprayNativeTest {
 static P current;static Native nativeEdge;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class P extends PestSprayTaskTest.Fixture {
  String mode="ok";int calls;
  public AutomationContext context(){return nativeEdge!=null&&nativeEdge.effectInProgress?nativeEdge.context():super.context();}PestSprayTask.Action action;
  public void requestSprayData(AutomationSession.Handle h){super.requestSprayData(h);nativeEdge.dataOwner=h;nativeEdge.dataLevel=level;nativeEdge.dataPlayer=player;}
  public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action a,long now){
   action=a;setup(this);var m=Minecraft.instance;
   switch(mode){case "thread"->m.thread=false;case "screen"->m.gui.screen=new Object();case "overlay"->m.gui.overlay=new Object();case "level"->m.level=new Object();case "player"->m.player=new Player();case "selected"->m.player.inventory.selected=8;case "cursor"->m.player.containerMenu.carried=new Stack(PestSprayPlanTest.material(1));case "inventory"->m.player.inventory.items.put(42,new Stack(PestSprayPlanTest.material(1)));case "using"->m.player.using=true;case "dead"->m.player.alive=false;case "passenger"->m.player.passenger=true;case "spectator"->m.player.spectator=true;case "sleeping"->m.player.sleeping=true;case "menu"->m.player.containerMenu=new Menu();case "position"->m.player.x+=1;case "velocity"->m.player.velocity=new Vec(.1,0,0);case "look"->m.player.yaw++;case "focus"->GLFW.focus=false;case "manual"->manual=true;case "nested"->nativeEdge.effectInProgress=true;case "throw"->m.gameMode.fail=true;case "revoke"->session.invalidate();case "physical-key"->{m.options.keyUp.unbound=false;m.options.keyUp.key="KEYSYM:87";GLFW.keys.add(87);}case "physical-mouse"->{m.options.keyAttack.unbound=false;m.options.keyAttack.key="MOUSE:0";GLFW.buttons.add(0);}case "unsupported-key"->{m.options.keyUse.unbound=false;m.options.keyUse.key="SCANCODE:12";}}
   nativeEdge.contextCalls=0;nativeEdge.late=mode.startsWith("late-")?mode.substring(5):"";
   var result=nativeEdge.sprayEffect(h,a,now);selected=m.player.inventory.selected;calls+=m.gameMode.calls;
   if(result==PestSprayTask.Effect.SENT){if(a.kind()==PestSprayTask.Kind.EQUIP)equips++;else if(a.kind()==PestSprayTask.Kind.RESTORE)restores++;else uses++;}
   return result;
  }
 }
 static class Stack{ItemSnapshot item;Stack(ItemSnapshot i){item=i;}}
 static class Inventory{Map<Integer,Stack> items=new HashMap<>();int selected;int getSelectedSlot(){return selected;}void setSelectedSlot(int v){selected=v;}}
 static class Menu{int containerId=0;Stack carried;Stack getCarried(){return carried;}}
 static class Vec{double x,y,z;Vec(double a,double b,double c){x=a;y=b;z=c;}}
 static class Player{Inventory inventory=new Inventory();Menu inventoryMenu=new Menu(),containerMenu=inventoryMenu;Stack cursor;boolean alive=true,spectator,passenger,sleeping,using;double x,y=72,z=-96;float yaw=20,pitch;Vec velocity=new Vec(0,0,0);Inventory getInventory(){return inventory;}boolean isAlive(){return alive;}boolean isSpectator(){return spectator;}boolean isPassenger(){return passenger;}boolean isSleeping(){return sleeping;}boolean isUsingItem(){return using;}double getX(){return x;}double getY(){return y;}double getZ(){return z;}float getYRot(){return yaw;}float getXRot(){return pitch;}Vec getDeltaMovement(){return velocity;}}
 static class Gui{Object screen,overlay;Object screen(){return screen;}Object overlay(){return overlay;}}
 static class Window{long handle(){return 1;}}
 static class Mode{int calls;boolean fail;void useItem(Player p,InteractionHand hand){check(hand==InteractionHand.MAIN_HAND,"only explicit main hand");calls++;if(fail)throw new IllegalStateException("possibly sent");}}
 enum InteractionHand{MAIN_HAND}
 static class KeyMapping{String key="KEYSYM:0";boolean unbound=true;boolean isUnbound(){return unbound;}String saveString(){return key;}}
 static class InputConstants{enum Type{KEYSYM,MOUSE,SCANCODE}record Key(Type type,int value){Type getType(){return type;}int getValue(){return value;}}static Key getKey(String s){var a=s.split(":");return new Key(Type.valueOf(a[0]),Integer.parseInt(a[1]));}}
 static class Options{KeyMapping keyUp=new KeyMapping(),keyDown=new KeyMapping(),keyLeft=new KeyMapping(),keyRight=new KeyMapping(),keyJump=new KeyMapping(),keyShift=new KeyMapping(),keySprint=new KeyMapping(),keyAttack=new KeyMapping(),keyUse=new KeyMapping();KeyMapping[] keyHotbarSlots=new KeyMapping[]{new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping(),new KeyMapping()};}
 static class Minecraft{static Minecraft instance;Options options=new Options();Object level;Player player=new Player();Gui gui=new Gui();Mode gameMode=new Mode();boolean thread=true;Window getWindow(){return new Window();}boolean isSameThread(){return thread;}static Minecraft getInstance(){return instance;}}
 static class GLFW{static final int GLFW_FOCUSED=1,GLFW_TRUE=1,GLFW_PRESS=1;static Set<Integer> keys=new HashSet<>(),buttons=new HashSet<>();static int glfwGetKey(long w,int k){return keys.contains(k)?1:0;}static int glfwGetMouseButton(long w,int k){return buttons.contains(k)?1:0;}static boolean focus=true;static int glfwGetWindowAttrib(long w,int a){return focus?1:0;}}
 static class AutomationGameView{record InventorySlot(int index,ItemSnapshot item){}static List<InventorySlot> inventory(Minecraft m){var list=new ArrayList<InventorySlot>();for(int i=0;i<43;i++)list.add(new InventorySlot(i,m.player.inventory.items.get(i).item));return list;}static ItemSnapshot item(Stack s,boolean equipment){return s.item;}}
 static class GardenTabReader{static List<String> read(Minecraft m){return List.of("Pests:","Spray: "+switch(current.spray.state()){case NONE->"None";case ACTIVE->"Test Material (29m)";case UNKNOWN->"Unknown";});}}
 static class Positions{PlayerPositionReceipts.Stamp stamp(long w,long n){return new PlayerPositionReceipts.Stamp(w,current.positionEpoch,current.positionSequence,n);}}
 static class HypixelItemCatalogClient{static int created;CompletableFuture<SkyblockItemCatalog> pending=new CompletableFuture<>();boolean closed,failClose;HypixelItemCatalogClient(){created++;}CompletableFuture<SkyblockItemCatalog> refresh(){return pending;}Optional<SkyblockItemCatalog> latest(){return pending.isDone()&&!pending.isCompletedExceptionally()?Optional.ofNullable(pending.getNow(null)):Optional.empty();}void close(){closed=true;if(failClose)throw new IllegalStateException("close failed");}}
 static final class Native {
  AutomationCoordinator coordinator;AutomationSession session;AutomationSession.Handle dataOwner;Object dataLevel,dataPlayer;boolean effectInProgress;HypixelItemCatalogClient catalog;CompletableFuture<?> request;Supplier<PestSprayPolicy> policy=PestSprayPlanTest::policy;String materialName="Test Material",late="";int contextCalls;Positions positions=new Positions();
  AutomationContext context(){contextCalls++;if(contextCalls==2){var m=Minecraft.instance;switch(late){case "level"->m.level=new Object();case "player"->m.player=new Player();case "inventory"->m.player.inventory.items.put(42,new Stack(PestSprayPlanTest.material(1)));case "focus"->GLFW.focus=false;case "position"->m.player.x++;case "selected"->m.player.inventory.selected=8;case "cancel"->current.task.cancel(current.now);case "game-mode"->m.gameMode=new Mode();case "sequence"->current.positionSequence++;case "epoch"->current.positionEpoch++;}}return current.context;}
  METHODS
 }
 static void setup(P p){var m=new Minecraft();Minecraft.instance=m;m.level=p.level;m.player=(Player)p.player;
  m.player=(Player)p.player;m.player.inventory.items.clear();for(var e:p.inv.entrySet())m.player.inventory.items.put(e.getKey(),new Stack(e.getValue()));m.player.inventory.selected=p.selected;m.player.cursor=new Stack(PestSprayPlanTest.AIR);m.player.inventoryMenu.carried=m.player.cursor;m.player.x=p.x;m.player.y=p.y;m.player.z=p.z;m.player.yaw=p.yaw;m.player.pitch=p.pitch;m.player.velocity=new Vec(p.vx,0,0);
 }
 static P make(String mode){var p=new P();p.player=new Player();current=p;nativeEdge=new Native();nativeEdge.coordinator=p.coordinator;nativeEdge.session=p.session;GLFW.focus=true;GLFW.keys.clear();GLFW.buttons.clear();p.mode=mode;return p;}
 static void run(String mode){var p=make(mode);p.begin();for(int i=0;i<12&&p.task.active()&&p.uses==0;i++)p.tick();}
 public static void main(String[]args){
  // setup must preserve the already frozen native player identity; replacing it is a negative case, not harness setup.
  run("ok");var p=current;check(p.task.state()==PestSprayTask.State.AWAITING&&p.calls==1&&p.equips==1,"actual native equip and one item use");check(nativeEdge.sprayEffect(p.action.owner(),p.action,p.now)==PestSprayTask.Effect.REJECTED&&p.calls==1,"one-use private capability cannot replay");
  p.acknowledge();p.decrease(3,true);p.tick();p.tick();check(p.task.state()==PestSprayTask.State.COMPLETE&&p.restores==1&&p.selected==0,"actual guarded restoration after applied proof");
  for(String mode:List.of("thread","screen","overlay","level","player","selected","cursor","inventory","using","dead","passenger","spectator","sleeping","menu","position","velocity","look","focus","manual","nested","revoke","physical-key","physical-mouse","unsupported-key","late-level","late-player","late-inventory","late-focus","late-position","late-selected","late-cancel","late-game-mode","late-sequence","late-epoch")){
   run(mode);check(!current.task.active()&&current.calls==0&&current.equips+current.restores==0,"physical gate before any effect: "+mode);
  }
  // A throwable after useItem starts is uncertain, not a reason to resend.
  p=make("ok");p.begin();while(p.equips==0&&p.task.active())p.tick();p.mode="throw";p.tick();check(p.task.state()==PestSprayTask.State.UNCERTAIN&&p.calls==1&&!nativeEdge.effectInProgress&&p.restores==0,"ambiguous use never retries/repairs and restores only local reentrancy flag");
  p=make("ok");setup(p);check(!nativeEdge.manualInput(),"unbound keys are not physical takeover");for(int i=0;i<9;i++){var key=Minecraft.instance.options.keyHotbarSlots[i];key.unbound=false;key.key="KEYSYM:"+(49+i);GLFW.keys.add(49+i);check(nativeEdge.manualInput(),"each physical hotbar key preempts input");GLFW.keys.clear();key.unbound=true;}Minecraft.instance.thread=false;check(nativeEdge.manualInput()&&nativeEdge.sprayFrame(p.now)==null,"offthread observation/poll fail closed before native reading");Minecraft.instance.thread=true;
  check(HypixelItemCatalogClient.created==0,"no startup, read or native-use catalog construction without an explicit demand");
  p=make("ok");setup(p);var h=p.session.begin(AutomationFeature.PEST_SPRAY,p.now,20*PestSprayTaskTest.S);nativeEdge.contextCalls=0;Minecraft.instance.thread=false;try{nativeEdge.requestSprayData(h);throw new AssertionError("offthread request accepted");}catch(IllegalStateException expected){}check(nativeEdge.contextCalls==0&&nativeEdge.catalog==null,"offthread demand must not even read native context or construct HTTP");Minecraft.instance.thread=true;
  nativeEdge.requestSprayData(h);var client=nativeEdge.catalog;check(nativeEdge.dataOwner.equals(h)&&nativeEdge.dataLevel==p.level&&nativeEdge.dataPlayer==p.player&&HypixelItemCatalogClient.created==1,"one original-root demand alone constructs the pinned catalog client");
  try{nativeEdge.requestSprayData(h);throw new AssertionError("duplicate demand accepted");}catch(IllegalStateException expected){}
  nativeEdge.disposeSprayData(new AutomationSession.Handle(h.lease(),h.hold(),h.generation()+1));check(!client.closed&&nativeEdge.dataOwner.equals(h),"stale handle cannot clear live lookup");nativeEdge.disposeSprayData(h);check(client.closed&&nativeEdge.catalog==null&&nativeEdge.dataOwner==null,"pending demand cancellation clears exact original owner and closes transport");
  p=make("ok");setup(p);h=p.session.begin(AutomationFeature.PEST_SPRAY,p.now,20*PestSprayTaskTest.S);nativeEdge.requestSprayData(h);client=nativeEdge.catalog;client.failClose=true;try{nativeEdge.disposeSprayData(h);throw new AssertionError("close failure lost");}catch(IllegalStateException expected){}check(nativeEdge.dataOwner==null&&nativeEdge.request==null&&nativeEdge.catalog==null,"transport cleanup failure cannot retain ownership/future");nativeEdge.shutdownData();
  p=make("ok");setup(p);h=p.session.begin(AutomationFeature.PEST_SPRAY,p.now,20*PestSprayTaskTest.S);nativeEdge.requestSprayData(h);client=nativeEdge.catalog;var data=SkyblockItemCatalog.parse("{\"success\":true,\"lastUpdated\":1,\"items\":[{\"id\":\"TEST_MATERIAL\",\"name\":\"Test Material\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8),1);client.pending.complete(data);check(nativeEdge.sprayCatalog(h).orElseThrow().get("Test Material").equals("TEST_MATERIAL"),"actual data projection returns unique official name-to-ID mapping under current original root");Minecraft.instance.level=new Object();check(nativeEdge.sprayCatalog(h).isEmpty(),"world replacement cannot reuse old-root catalog binding");nativeEdge.disposeSprayData(h);check(!client.closed&&nativeEdge.catalog==client,"completed cached public data retained without active gameplay owner");nativeEdge.shutdownData();check(client.closed&&nativeEdge.catalog==null,"world/shutdown closes even completed cached transport");
  System.out.println("PestSprayNativeTest: PASS actual native effect/frame bodies and real task capabilities; no live server claim");
 }
}'''.replace('METHODS','\n'.join(method(n) for n in ['sprayEffect','nativeFrame','manualInput','physical','requestSprayData','sprayCatalog','disposeSprayData','shutdownData','sprayFrame']))
# Keep the same Player object that the task froze at request; ordinary fixtures may mutate it, never replace it.
java=java.replace('m.player=(Player)p.player;','m.player=(Player)p.player;')
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-native-') as d:
 f=Path(d)/'PestSprayNativeTest.java';f.write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(root/'src/main/java')+':'+str(root/'src/test/java'),'-d',d,str(f)],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.automation.pest.PestSprayNativeTest'],check=True,timeout=30)
assert 'startUseItem' not in s and 'setYRot' not in s and 'setXRot' not in s and 'setPos' not in s and '.setDown(' not in s and 'sendCommand' not in s,'No broad world-input exemption, pose/key/command shortcut'
assert '.whenComplete(' not in s and '.thenAccept(' not in s,'HTTP callbacks cannot drive gameplay'

assert s.count("new HypixelItemCatalogClient()") == 1 and "new HypixelItemCatalogClient()" in method("requestSprayData"), "Only explicit root-bound catalog demand constructs HTTP"
assert {p.name for p in (root/"src/main/java").rglob("*.java") if "new PestSprayController(" in p.read_text()} == set(), "Foundation remains deliberately unexposed until service/settings/lifecycle gates exist"
