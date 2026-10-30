package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSpraySelectionTaskTest {
 static final long S=1_000_000_000L;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class F implements PestSpraySelectionTask.Port {
  long now=S,world=1;int selected=4;Object level=new Object(),player=new Object(),menuIdentity;Map<Integer,ItemSnapshot> inv=new HashMap<>(PestSprayPlanTest.inventory());ItemSnapshot cursor=PestSprayPlanTest.AIR;MenuSnapshot menu;String invariant="all-components-except-known-selected-lore";boolean manual,safe=true,air=true,claim=true,uncertain,earlyAck;int opens,clicks,closes,stops;double x=0,z=-96;float yaw=20;long positionEpoch=1,positionSequence;
  AutomationContext context=new AutomationContext(1,true,true,Set.of());final AutomationCoordinator coordinator=new AutomationCoordinator();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();final AutomationSession session=new AutomationSession(coordinator,()->context,()->{},()->stops++);final AutomationSession.Handle owner;final PestSpraySelectionTask task=new PestSpraySelectionTask(session,coordinator,items,cursors,this);PestSpraySelectionTask.Action last;
  F(){this(true);}
  F(boolean acquire){inv.put(4,PestSprayToolTest.tool("SPRAYONATOR","Other detail","Selected Material: None"));owner=acquire?session.begin(AutomationFeature.PEST_SPRAY,now,35*S):null;if(acquire)check(owner!=null&&session.holdsFarming(),"original independently armed root");}
  public AutomationContext context(){return context;}public boolean manualInput(){return manual;}
  PestSprayTask.Frame motion(){return motion(now);}
  PestSprayTask.Frame motion(long at){return new PestSprayTask.Frame(world,at,level,player,inv,cursor,selected,menu==null,false,x,72,z,0,0,0,yaw,0,new PestSprayObservation(PestSprayObservation.State.NONE),positionEpoch,positionSequence,null);}
  public PestSpraySelectionTask.Frame selectionFrame(long at){return new PestSpraySelectionTask.Frame(motion(at),menuIdentity,menu,invariant,safe,air&&menu==null);}
  void render(){var overrides=new HashMap<Integer,ItemSnapshot>();overrides.put(13,PestSpraySelectionViewTest.button("Click to select!"));for(int i=0;i<36;i++)overrides.put(27+i,inv.get(i<27?i+9:i-27));menu=PestSpraySelectionViewTest.menu("Sprayonator",overrides);context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}
  void opening(){menuIdentity=new Object();render();for(var s:menu.slots())items.observe(1,7,s.index(),s.item());cursors.observe(1,7,PestSprayPlanTest.AIR,now);}
  public PestSprayTask.Effect selectionEffect(AutomationSession.Handle h,PestSpraySelectionTask.Action action,long at){
   last=action;if(earlyAck&&action.kind()==PestSpraySelectionTask.Kind.CLICK)task.onSystemMessage(1,"Sprayonator! Your selected material is now Test Material!",at);
   if(!claim||!action.claim(h,selectionFrame(at),at))return PestSprayTask.Effect.REJECTED;
   switch(action.kind()){case OPEN->{opens++;opening();}case CLICK->clicks++;case CLOSE->{closes++;menu=null;menuIdentity=null;context=new AutomationContext(1,true,true,Set.of());}}
   return uncertain?PestSprayTask.Effect.UNCERTAIN:PestSprayTask.Effect.SENT;
  }
  void begin(){check(task.begin(owner,PestSprayPlanTest.policy(),PestSprayTool.read(4,inv.get(4)).orElseThrow(),motion(),now),"valid child uses original root");}
  void tick(){now+=200_000_000L;task.tick(now);}void clicked(){begin();for(int i=0;i<8&&clicks==0&&task.active();i++)tick();check(clicks==1&&task.state()==PestSpraySelectionTask.State.AWAITING,"one proven button click "+task.state());}
  void selected(boolean applied,boolean game){var old=inv.get(4);inv=new HashMap<>(inv);inv.put(4,new ItemSnapshot(old.skyblockId(),old.minecraftId(),old.uuid(),old.name(),"selection-updated",1,List.of("Other detail","Selected Material: Test Material"),old.protectedItem()));render();if(applied){items.observe(1,0,40,inv.get(4));items.observe(1,7,58,inv.get(4));cursors.observe(1,7,PestSprayPlanTest.AIR,now);}if(game)task.onSystemMessage(1,"Sprayonator! Your selected material is now Test Material!",now);}
 }
 public static void main(String[]args){
  var f=new F();f.clicked();check(f.session.isCurrent(f.owner)&&f.session.holdsFarming()&&f.opens==1&&f.clicks==1,"child never acquires/releases/restarts the parent root");f.selected(false,true);f.tick();check(f.task.state()==PestSpraySelectionTask.State.AWAITING&&f.closes==0,"GAME plus predicted tool is not applied metadata proof");f.items.observe(1,0,40,f.inv.get(4));f.items.observe(1,7,58,f.inv.get(4));f.cursors.observe(1,7,PestSprayPlanTest.AIR,f.now);f.tick();f.tick();check(f.task.state()==PestSpraySelectionTask.State.COMPLETE&&f.closes==1&&f.session.isCurrent(f.owner)&&f.session.holdsFarming()&&f.stops==0,"selection settles/owned-close retains original root for spray use");check(!f.last.claim(f.owner,f.selectionFrame(f.now),f.now),"spent action cannot replay");
  for(String mode:List.of("manual","world","world-id","player","pose","look","correction","cursor","inventory","metadata","menu")){
   f=new F();f.clicked();switch(mode){case "manual"->f.manual=true;case "world"->f.level=new Object();case "world-id"->f.world=2;case "player"->f.player=new Object();case "pose"->f.x++;case "look"->f.yaw++;case "correction"->f.positionSequence++;case "cursor"->f.cursor=PestSprayPlanTest.material(1);case "inventory"->f.inv.put(10,PestSprayPlanTest.material(1));case "metadata"->f.invariant="hidden-NBT-changed";case "menu"->f.menuIdentity=new Object();}f.tick();check(!f.task.active()&&f.closes==0&&f.clicks==1&&f.session.isCurrent(f.owner),"takeover/unknown never closes/repairs/finishes parent "+mode);
  }
  f=new F();f.air=false;check(!f.task.begin(f.owner,PestSprayPlanTest.policy(),PestSprayTool.read(4,f.inv.get(4)).orElseThrow(),f.motion(),f.now)&&f.opens==0,"no rotate or entity/block attack to open selection");
  f=new F();f.clicked();f.selected(true,false);for(int i=0;i<60&&f.task.active();i++)f.tick();check(f.task.state()==PestSpraySelectionTask.State.TIMED_OUT&&f.clicks==1&&f.closes==0,"no GAME ack means bounded stop, never repeat click");
  f=new F();f.clicked();f.selected(true,true);f.inv.put(9,PestSprayPlanTest.material(11));f.render();f.tick();check(!f.task.active()&&f.closes==0,"selection is not material-consumption or purchase consent");
  f=new F();f.uncertain=true;f.begin();f.tick();check(f.task.state()==PestSpraySelectionTask.State.UNCERTAIN&&f.opens==1&&f.clicks==0&&f.closes==0,"possibly-sent opener never retried or unknown menu closed");
  f=new F();f.clicked();f.selected(true,true);f.items.clear();f.items.observe(1,0,4,f.inv.get(4));f.items.observe(1,7,58,f.inv.get(4));for(int i=0;i<60&&f.task.active();i++)f.tick();check(f.task.state()==PestSpraySelectionTask.State.TIMED_OUT,"fake raw inventory index4 cannot replace canonical window0 slot40");
  f=new F();f.clicked();f.selected(true,false);f.task.onSystemMessage(1,"SPRAYONATOR! Your selected material is now Test Material!",f.now);f.tick();f.tick();check(f.task.state()==PestSpraySelectionTask.State.COMPLETE,"source GAME prefix is case-insensitive while canonical material identity remains exact");
  f=new F();f.world=2;check(!f.task.begin(f.owner,PestSprayPlanTest.policy(),PestSprayTool.read(4,f.inv.get(4)).orElseThrow(),f.motion(),f.now),"child cannot bind another frame world to the current root");
  System.out.println("PestSpraySelectionTaskTest: PASS source-style original-root selection/receipts/close; provisional GUI/native metadata protocol, not live proof");
 }
}
