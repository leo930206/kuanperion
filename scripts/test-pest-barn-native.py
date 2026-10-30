#!/usr/bin/env python3
"""Execute unchanged native Barn adapter bodies with real private capabilities and controlled Minecraft edges.
This is NOT a real client/NPC/server test; NPC grammar remains provisional.
"""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parents[1];m=r/'src/main/java/dev/kuan/kuanperion';s=(m/'PestBarnGameView.java').read_text()
for required in ['RemotePlayer','"Phillip"','action.claim(','action.matchesSnapshot(','System.nanoTime()','flight.allowed(','flight.manualInput()','gameMode.interact(','EntityHitResult']:
 assert required in s,required
for forbidden in ['ArmorStand','sendCommand(','new Hypixel','setDeltaMovement(','.setDown(','.setPos(','startUseItem(','session.begin(','session.finish(','setCarried(','Thread.sleep(']:
 assert forbidden not in s,forbidden
# Strip only imports of controlled native dependencies; execute every actual adapter method unchanged.
native=re.sub(r'import (?:net\.minecraft|com\.mojang|org\.lwjgl)\.[^;]+;','',s)
flight_source=(m/'PestFlightController.java').read_text()
def flight_method(name):
 match=re.search(r' (?:public|private) [^{}\n]+ '+name+r'\(',flight_source);assert match,name
 a=match.start();b=flight_source.index('{',a)+1;depth=1
 while depth:depth+=(flight_source[b]=='{')-(flight_source[b]=='}');b+=1
 return flight_source[a:b]
route_methods='\n'.join(flight_method(n) for n in ['openExchange','dispose'])
java=r'''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
public final class PestBarnNativeTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 final Minecraft mc=new Minecraft();final AutomationCoordinator c=new AutomationCoordinator();AutomationContext ctx=new AutomationContext(1,true,true,Set.of());final AutomationSession session=new AutomationSession(c,()->ctx,()->{},()->{});
 final GardenFlightCore flight=new GardenFlightCore(session,c,()->ctx);final PestIdentity.Vacuum tool=PestIdentity.vacuum(3,TOOL).orElseThrow();final PestBarnGameView view=new PestBarnGameView(c,session,flight,()->tool);final PestBarnInteraction task=new PestBarnInteraction(session,c,view);AutomationSession.Handle h;
 static final ItemSnapshot TOOL=new ItemSnapshot("INFINI_VACUUM","minecraft:iron_hoe","vacuum-88","Vacuum","",1,List.of("Ability: Vacuum RIGHT CLICK","Capture pests within 10 blocks"),true);
 PestBarnNativeTest(){Minecraft.instance=mc;long now=System.nanoTime();h=session.begin(AutomationFeature.PEST,now-1_000_000_000L,120_000_000_000L);flight.h=h;flight.level=mc.level;mc.player.stack.item=TOOL;mc.hitResult=new EntityHitResult(mc.level.npc,mc.level.npc.getEyePosition());}
 PestBarnTarget target(){var p=mc.level.npc;var e=p.getEyePosition();return new PestBarnTarget(p.getId(),p.getUUID(),new PestTargets.Point(e.x,e.y,e.z));}
 void ready(){long now=System.nanoTime();check(task.bind(h,target(),now-200_000_000L)==PestTask.BarnReady.AIMING,"first pose is not stability proof");check(task.bind(h,target(),now)==PestTask.BarnReady.READY,"stable exact native scene");flight.calls=flight.manualCalls=mc.level.clips=0;}
 static int cases;
 static void run(String mode){var t=new PestBarnNativeTest();t.ready();var p=t.mc.player;var n=t.mc.level.npc;var originalMode=t.mc.gameMode;
  Runnable mutation=()->{};
  switch(mode){
   case "thread"->t.mc.thread=false;case "screen"->t.mc.gui.screen=new Object();case "overlay"->t.mc.gui.overlay=new Object();case "cursor"->p.inventoryMenu.carried.empty=false;case "menu"->p.containerMenu=new Container();case "dead"->p.alive=false;case "spectator"->p.spectator=true;case "passenger"->p.passenger=true;case "sleep"->p.sleep=true;case "using"->p.using=true;case "no-flight"->p.abilities.mayfly=false;case "tool"->p.stack.item=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);case "slot"->p.inventory.selected=2;case "disabled"->p.stack.enabled=false;
   case "manual"->t.flight.manual=true;case "unloaded"->t.flight.loaded=false;case "block"->t.mc.level.block=true;case "ray"->t.mc.hitResult=new HitResult(HitResult.Type.MISS);case "wrong-ray"->t.mc.hitResult=new EntityHitResult(new RemotePlayer(),n.getEyePosition());case "range"->p.z=-5;case "look"->p.yaw=1;case "position"->p.x+=.01;
   case "name"->n.name="Other";case "npc-dead"->n.alive=false;case "npc-removed"->n.removed=true;case "npc-spectator"->n.spectator=true;case "npc-uuid"->n.uuid=UUID.randomUUID();case "npc-eye"->n.y+=.01;case "npc-type"->t.mc.level.entity=new Entity();
   case "level"->t.mc.level=new Level();case "player"->t.mc.player=new Player();case "game-mode"->t.mc.gameMode=new Mode();case "root"->t.session.invalidate();case "blocker"->t.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MINING));case "world"->t.ctx=new AutomationContext(2,true,true,Set.of());case "disposed"->t.task.dispose(t.h);case "uncertain"->originalMode.fail=true;
   case "focus"->GLFW.focus=false;case "physical-hotbar"->{t.mc.options.keyHotbarSlots[0].unbound=false;t.mc.options.keyHotbarSlots[0].key="KEYSYM:49";GLFW.keys.add(49);}case "unsupported-hotbar"->{t.mc.options.keyHotbarSlots[0].unbound=false;t.mc.options.keyHotbarSlots[0].key="SCANCODE:1";}
   case "late-root"->mutation=()->t.session.invalidate();case "late-dispose"->mutation=()->t.task.dispose(t.h);case "late-player"->mutation=()->t.mc.player=new Player();case "late-mode"->mutation=()->t.mc.gameMode=new Mode();case "late-level"->mutation=()->t.mc.level=new Level();case "late-look"->mutation=()->p.yaw++;case "late-tool"->mutation=()->p.inventory.selected=1;case "late-screen"->mutation=()->t.mc.gui.screen=new Object();case "late-manual"->mutation=()->t.flight.manual=true;
  }
  if(mode.startsWith("late-")){Runnable once=mutation;t.flight.callback=()->{if(t.flight.calls==4)once.run();};}
  boolean sent=t.task.open(t.h,System.nanoTime());int expected=mode.equals("ok")||mode.equals("uncertain")?1:0;
  check(originalMode.interactions==expected&&sent==mode.equals("ok"),mode+" expected one exact effect or fail closed, calls="+originalMode.interactions+" sent="+sent);
  check(!t.task.open(t.h,System.nanoTime())&&originalMode.interactions==expected,"no retry even uncertain/disposed: "+mode);
  if(mode.equals("ok"))check(t.session.isCurrent(t.h)&&t.session.holdsFarming(),"adapter never finishes/rearms original root");
  GLFW.focus=true;GLFW.keys.clear();cases++;
 }
 public static void main(String[]args){
  for(String mode:List.of("ok","thread","screen","overlay","cursor","menu","dead","spectator","passenger","sleep","using","no-flight","tool","slot","disabled","manual","unloaded","block","ray","wrong-ray","range","look","position","name","npc-dead","npc-removed","npc-spectator","npc-uuid","npc-eye","npc-type","level","player","game-mode","root","blocker","world","disposed","uncertain","late-root","late-dispose","late-player","late-mode","late-level","late-look","late-tool","late-screen","late-manual","focus","physical-hotbar","unsupported-hotbar"))run(mode);
  var t=new PestBarnNativeTest();t.mc.player.yaw=90;for(int i=0;i<12;i++)t.task.bind(t.h,t.target(),System.nanoTime());check(t.mc.player.rotations>0&&t.mc.player.rotations<=8&&t.mc.gameMode.interactions==0,"native bounded rotations, never interact during steering");
  t=new PestBarnNativeTest();var f=PestBarnGameView.read(t.mc,t.ctx,System.nanoTime());check(f.complete()&&f.targets().size()==1,"bounded loaded exact RemotePlayer reader");t.mc.level.npc.name="Other";check(PestBarnGameView.read(t.mc,t.ctx,System.nanoTime()).targets().isEmpty(),"never infer name from label/other entity");t.mc.level.npc.name="Phillip";t.mc.level.entities=Collections.nCopies(513,new Entity());check(!PestBarnGameView.read(t.mc,t.ctx,System.nanoTime()).complete(),"overflow stays unknown, not no-target proof");t.mc.thread=false;check(!PestBarnGameView.read(t.mc,t.ctx,System.nanoTime()).complete(),"offthread reads denied");
  t=new PestBarnNativeTest();t.ready();var route=new Route(t.task,t.h);var stale=new AutomationSession.Handle(t.h.lease(),t.h.hold(),t.h.generation()+1);route.dispose(stale);check(route.openExchange(t.h,System.nanoTime())&&route.calls==0,"actual opener routes bound Barn capability, not a command");check(!route.openExchange(t.h,System.nanoTime())&&route.calls==0,"used Barn does not fall back to Abiphone");
  t=new PestBarnNativeTest();t.ready();route=new Route(t.task,t.h);route.flight.fail=true;try{route.dispose(t.h);throw new AssertionError("fixture physical cleanup must throw");}catch(IllegalStateException expected){}check(t.task.uses(t.h)&&!route.openExchange(t.h,System.nanoTime())&&route.calls==0,"actual disposal revokes private native references even after flight cleanup throws, while retaining no-fallback tombstone");
  t=new PestBarnNativeTest();route=new Route(t.task,t.h);check(route.openExchange(t.h,System.nanoTime())&&route.calls==1,"unbound original Abiphone path remains explicit one-shot route");
  System.out.println("PestBarnNativeTest: PASS "+cases+" actual adapter/private-capability guard cases + bounded rotation/loaded reader; controlled native wrappers, NOT live client/NPC proof");
 }
}
class Route{
 final PestBarnInteraction barn;final Cleanup flight=new Cleanup();AutomationSession.Handle baitDataOwner;Object latestFacts,preparedTool;int calls;interface Opener{boolean open(AutomationSession.Handle h,long n);}final Opener exchangeOpener=(h,n)->{calls++;return true;};
 Route(PestBarnInteraction b,AutomationSession.Handle h){barn=b;flight.h=h;}
 static class Cleanup{AutomationSession.Handle h;boolean fail;AutomationSession.Handle owner(){return h;}void dispose(AutomationSession.Handle p){h=null;if(fail)throw new IllegalStateException("physical cleanup");}}
 boolean exchangeNativeAllowed(AutomationSession.Handle h){return true;}boolean allowed(AutomationSession.Handle h,long n,AutomationCoordinator.Resource r){return true;}void cancelBaitAcquisition(){}void shutdownBaitData(){}
 ROUTE_METHODS
}
class Vec3{final double x,y,z;Vec3(double x,double y,double z){this.x=x;this.y=y;this.z=z;}double distanceToSqr(Vec3 p){return (x-p.x)*(x-p.x)+(y-p.y)*(y-p.y)+(z-p.z)*(z-p.z);}}
class AABB{AABB(Vec3 a,Vec3 b){}AABB inflate(double d){return this;}}
class ClipContext{enum Block{COLLIDER}enum Fluid{ANY}ClipContext(Vec3 a,Vec3 b,Block c,Fluid f,Player p){}}
class HitResult{enum Type{MISS,BLOCK,ENTITY}final Type type;HitResult(Type t){type=t;}Type getType(){return type;}}
class EntityHitResult extends HitResult{final Entity entity;final Vec3 point;EntityHitResult(Entity e,Vec3 p){super(Type.ENTITY);entity=e;point=p;}Entity getEntity(){return entity;}Vec3 getLocation(){return point;}}
enum InteractionHand{MAIN_HAND}
class Text{final String value;Text(String v){value=v;}String getString(){return value;}}
class Entity{double x=.5,y=70,z=2.5;boolean alive=true,removed,spectator;UUID uuid=UUID.randomUUID();String name="Phillip";int getId(){return 88;}UUID getUUID(){return uuid;}Text getName(){return new Text(name);}boolean isRemoved(){return removed;}boolean isAlive(){return alive;}boolean isSpectator(){return spectator;}double getX(){return x;}double getY(){return y;}double getZ(){return z;}Vec3 getEyePosition(){return new Vec3(x,y+1.62,z);}}
class RemotePlayer extends Entity{}
class Stack{boolean empty=true,enabled=true;ItemSnapshot item;boolean isEmpty(){return empty;}boolean isItemEnabled(Object o){return enabled;}}
class Container{int containerId;Stack carried=new Stack();Stack getCarried(){return carried;}}
class Inventory{int selected=3;int getSelectedSlot(){return selected;}}
class Abilities{boolean mayfly=true;}
class Player extends Entity{Player(){z=.5;}float yaw,pitch;int rotations;boolean passenger,sleep,using;Stack stack=new Stack();Container inventoryMenu=new Container(),containerMenu=inventoryMenu;Inventory inventory=new Inventory();Abilities abilities=new Abilities();Inventory getInventory(){return inventory;}Stack getMainHandItem(){return stack;}Abilities getAbilities(){return abilities;}boolean isPassenger(){return passenger;}boolean isSleeping(){return sleep;}boolean isUsingItem(){return using;}float getYRot(){return yaw;}float getXRot(){return pitch;}void setYRot(float f){if(Math.abs(yaw-f)>12.001)throw new AssertionError("unbounded yaw");yaw=f;rotations++;}void setXRot(float f){if(Math.abs(pitch-f)>12.001)throw new AssertionError("unbounded pitch");pitch=f;}}
class Level{final RemotePlayer npc=new RemotePlayer();Entity entity=npc;boolean block;int clips;List<Entity> entities=List.of(npc);Object enabledFeatures(){return this;}Entity getEntity(int id){return id==88?entity:null;}Iterable<Entity> entitiesForRendering(){return entities;}HitResult clip(ClipContext c){clips++;return new HitResult(block?HitResult.Type.BLOCK:HitResult.Type.MISS);}}
class Mode{int interactions;boolean fail;void interact(Player p,RemotePlayer e,EntityHitResult r,InteractionHand h){if(e!=r.getEntity()||h!=InteractionHand.MAIN_HAND)throw new AssertionError("wrong native typed interaction");interactions++;if(fail)throw new IllegalStateException("uncertain after send");}}
class Gui{Object screen,overlay;Object screen(){return screen;}Object overlay(){return overlay;}}
class Window{long handle(){return 1;}}
class KeyMapping{boolean unbound=true;String key="KEYSYM:0";boolean isUnbound(){return unbound;}String saveString(){return key;}}
class Options{KeyMapping[] keyHotbarSlots=new KeyMapping[9];Options(){Arrays.setAll(keyHotbarSlots,i->new KeyMapping());}}
class InputConstants{enum Type{KEYSYM,MOUSE,SCANCODE}record Key(Type type,int value){Type getType(){return type;}int getValue(){return value;}}static Key getKey(String s){var p=s.split(":");return new Key(Type.valueOf(p[0]),Integer.parseInt(p[1]));}}
class GLFW{static final int GLFW_PRESS=1,GLFW_FOCUSED=0x20001,GLFW_TRUE=1;static boolean focus=true;static Set<Integer> keys=new HashSet<>();static int glfwGetKey(long w,int k){return keys.contains(k)?1:0;}static int glfwGetMouseButton(long w,int k){return 0;}static int glfwGetWindowAttrib(long w,int a){return focus?1:0;}}
class Minecraft{static Minecraft instance;boolean thread=true;Level level=new Level();Player player=new Player();Mode gameMode=new Mode();Gui gui=new Gui();HitResult hitResult;Options options=new Options();Window window=new Window();static Minecraft getInstance(){return instance;}boolean isSameThread(){return thread;}Window getWindow(){return window;}}
class AutomationGameView{static ItemSnapshot item(Stack s,boolean b){return s.item;}}
class GardenFlightCore{final AutomationSession session;final AutomationCoordinator c;final java.util.function.Supplier<AutomationContext> context;AutomationSession.Handle h;Object level;boolean manual,loaded=true;int calls,manualCalls;Runnable callback=()->{};GardenFlightCore(AutomationSession s,AutomationCoordinator c,java.util.function.Supplier<AutomationContext> ctx){session=s;this.c=c;context=ctx;}AutomationSession.Handle owner(){return h;}Object levelIdentity(){return level;}boolean allowed(AutomationSession.Handle h,long n,AutomationCoordinator.Resource... rs){calls++;callback.run();return session.isCurrent(h)&&Arrays.stream(rs).allMatch(r->c.permits(h.lease(),r,context.get(),n));}boolean manualInput(){manualCalls++;return manual;}boolean loaded(AABB b){return loaded;}}
'''.replace('ROUTE_METHODS',route_methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-barn-native-') as d:
 p=Path(d);(p/'PestBarnGameView.java').write_text(native);(p/'PestBarnNativeTest.java').write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(r/'src/main/java'),'-d',d,str(p/'PestBarnGameView.java'),str(p/'PestBarnNativeTest.java')],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.PestBarnNativeTest'],check=True,timeout=30)
print('PestBarnNativeScope: PASS narrow loaded RemotePlayer/private action/native clock/geometry route; no current-server claim')
