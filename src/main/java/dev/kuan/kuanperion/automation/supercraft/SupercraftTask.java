// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.FlightAnchor;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
/** One explicitly configured, already-selected inventory batch. Abstract port only: no native executor, quantity UI or automatic driver is implied. */
public final class SupercraftTask {
 public enum State{IDLE,DATA,WAIT_MENU,WAIT_RESULT,CLOSING,COMPLETE,CANCELLED,TIMED_OUT,FAILED}
 public record Policy(SupercraftPlan.Recipe recipe,SupercraftPlan.Rules rules,int outputCount,int timeoutSeconds,boolean onlyWhileFarming){
  public Policy{Objects.requireNonNull(rules);if(outputCount<0||outputCount>4096||timeoutSeconds<60||timeoutSeconds>180)throw new IllegalArgumentException("Invalid Supercraft task policy");}
  public boolean configured(){if(recipe==null||outputCount<1||outputCount>rules.maxOutput()||outputCount%recipe.outputPerCraft()!=0||outputCount/recipe.outputPerCraft()>rules.maxCrafts()||rules.maxConsumed()==0||!rules.outputs().contains(recipe.outputId())||!rules.ingredients().containsAll(recipe.inputsPerCraft().keySet()))return false;long perCraft=recipe.inputsPerCraft().values().stream().mapToLong(Integer::longValue).sum();return perCraft*(outputCount/recipe.outputPerCraft())<=rules.maxConsumed();}
  public static Policy defaults(){return new Policy(null,SupercraftPlan.Rules.defaults(),0,90,true);}
 }
 public record Frame(long world,long observedAt,FlightAnchor pose,boolean manualInput,boolean cursorEmpty,boolean complete){public Frame{Objects.requireNonNull(pose);}boolean fresh(long world,long now){return complete&&this.world==world&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L;}}
 public interface Port {
  AutomationContext context();Frame frame(long now);Map<Integer,ItemSnapshot> inventory();MenuSnapshot menu();
  void requestCatalog(AutomationSession.Handle handle);Optional<Map<String,String>> catalog();
  boolean recipe(AutomationSession.Handle handle,SupercraftPlan.Recipe recipe,long now);
  /** Native adapter must independently revalidate full43, current preview/quantity/cost, cursor and exact SCREEN lease. */
  GuardedMenuAction.Result craft(AutomationSession.Handle handle,SupercraftPreview preview,long now);
  void closeOwnedMenu(int container);default void dispose(AutomationSession.Handle handle){}
 }
 private static final long SECOND=1_000_000_000L;private final AutomationSession session;private final InventoryReceipts receipts;private final CursorReceipts cursors;private final Port port;
 private State state=State.IDLE;private Policy policy;private AutomationSession.Handle handle;private FlightAnchor original;private long world,started,phaseStart;private MenuContract contract;private int container=-1,confirmed;private Map<String,String> names;private SupercraftReceipts proof;
 public SupercraftTask(AutomationSession session,InventoryReceipts receipts,CursorReceipts cursors,Port port){this.session=Objects.requireNonNull(session);this.receipts=Objects.requireNonNull(receipts);this.cursors=Objects.requireNonNull(cursors);this.port=Objects.requireNonNull(port);}
 public State state(){return state;}public int confirmedOutput(){return confirmed;}public boolean active(){return state==State.DATA||state==State.WAIT_MENU||state==State.WAIT_RESULT||state==State.CLOSING;}
 private boolean owned(MenuSnapshot menu){return contract!=null&&contract.matches(menu)&&menu.worldId()==world&&(container<0||menu.containerId()==container);}
 public boolean ownsMenu(){return active()&&session.isCurrent(handle)&&owned(port.menu());}
 private AutomationContext context(){var c=port.context();if(!c.blockers().contains(AutomationContext.Blocker.MENU)||!owned(port.menu()))return c;var b=EnumSet.noneOf(AutomationContext.Blocker.class);b.addAll(c.blockers());b.remove(AutomationContext.Blocker.MENU);return new AutomationContext(c.worldId(),c.farmingActive(),c.garden(),b);}
 private boolean sameOrigin(Frame f){return f.pose().position().distanceSquared(original.position())<=.35*.35&&f.pose().slot()==original.slot()&&PestIdentity.sameTool(original.item(),f.pose().item())&&f.pose().flying()==original.flying()&&Math.abs(PestSteering.rotate(f.pose().yaw(),original.yaw(),30,true)-f.pose().yaw())<=1&&Math.abs(f.pose().pitch()-original.pitch())<=1;}
 public boolean request(Policy policy,boolean manual,long now){
  if(active()||policy==null||!policy.configured())return false;
  try{var c=port.context();var f=port.frame(now);
   if(!c.canIssueCommand()||!f.fresh(c.worldId(),now)||f.manualInput()||!f.cursorEmpty()||c.farmingActive()&&!c.garden()||!manual&&policy.onlyWhileFarming()&&!c.farmingActive()||SupercraftPlan.select(policy.recipe(),policy.outputCount(),policy.rules(),port.inventory()).isEmpty())return false;
   var next=session.begin(AutomationFeature.SUPERCRAFT,now,policy.timeoutSeconds()*SECOND);if(next==null)return false;
   handle=next;this.policy=policy;original=f.pose();world=c.worldId();started=phaseStart=now;contract=null;container=-1;confirmed=0;proof=null;names=null;state=State.DATA;port.requestCatalog(handle);return true;
  }catch(RuntimeException invalid){if(handle!=null)finish(State.FAILED,now);return false;}
 }
 public void tick(long now){if(active())try{run(now);}catch(RuntimeException invalid){finish(State.FAILED,now);}}
 private void run(long now){
  var c=context();var f=port.frame(now);
  if(!session.isCurrent(handle)||!c.canIssueCommand()||c.worldId()!=world||handle.hold()!=null&&(!c.farmingActive()||!c.garden())||!f.fresh(world,now)||f.manualInput()||!sameOrigin(f)){finish(State.CANCELLED,now);return;}
  if(now-started<0||now-started>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT,now);return;}
  if(state!=State.WAIT_RESULT&&!f.cursorEmpty()){finish(State.CANCELLED,now);return;}
  if(state==State.DATA){
   var data=port.catalog();if(data.isEmpty()){if(now-phaseStart>=12*SECOND)finish(State.TIMED_OUT,now);return;}
   if(data.get().size()>20000){finish(State.FAILED,now);return;}names=Map.copyOf(data.get());var outputNames=names.entrySet().stream().filter(e->policy.recipe().outputId().equals(e.getValue())).map(Map.Entry::getKey).toList();
   if(outputNames.size()!=1||outputNames.getFirst().isBlank()||outputNames.getFirst().length()>128||java.util.regex.Pattern.compile("[\\p{C}§]").matcher(outputNames.getFirst()).find()){finish(State.FAILED,now);return;}
   contract=new MenuContract(Set.of(outputNames.getFirst()+" Recipe"),54);session.expectMenu(handle,contract,port::menu);state=State.WAIT_MENU;phaseStart=now;
   if(!port.recipe(handle,policy.recipe(),now))finish(State.FAILED,now);return;
  }
  var menu=port.menu();if(menu==null||!owned(menu)){if(state==State.WAIT_MENU&&menu==null&&now-phaseStart<8*SECOND)return;finish(state==State.WAIT_MENU?State.TIMED_OUT:State.CANCELLED,now);return;}
  if(container<0)container=menu.containerId();
  if(state==State.WAIT_MENU){
   if(now-phaseStart>=8*SECOND){finish(State.TIMED_OUT,now);return;}var inventory=port.inventory();var plan=SupercraftPlan.select(policy.recipe(),policy.outputCount(),policy.rules(),inventory);if(plan.isEmpty()){finish(State.FAILED,now);return;}
   var preview=SupercraftPreview.inspect(plan.get(),menu,names);if(preview.isEmpty())return;
   proof=new SupercraftReceipts(receipts,cursors,()->session.isCurrent(handle));if(!proof.arm(plan.get(),menu,inventory,f.cursorEmpty(),now)){finish(State.FAILED,now);return;}
   state=State.WAIT_RESULT;phaseStart=now;GuardedMenuAction.Result result;try{result=port.craft(handle,preview.get(),now);}catch(RuntimeException ambiguous){result=GuardedMenuAction.Result.UNCERTAIN;}
   if(!proof.sent(result,now))finish(State.FAILED,now);return;
  }
  if(state==State.WAIT_RESULT){
   proof.poll(world,port.inventory(),f.cursorEmpty(),now);if(proof.state()==SupercraftReceipts.State.CONFIRMED){confirmed=policy.outputCount();state=State.CLOSING;return;}if(proof.state()!=SupercraftReceipts.State.AWAITING)finish(State.TIMED_OUT,now);return;
  }
  if(state==State.CLOSING){
   if(proof==null||proof.state()!=SupercraftReceipts.State.CONFIRMED){finish(State.FAILED,now);return;}port.closeOwnedMenu(container);
   var after=port.context();var pose=port.frame(now);if(port.menu()!=null||!session.isCurrent(handle)||!after.canIssueCommand()||after.worldId()!=world||!pose.fresh(world,now)||pose.manualInput()||!pose.cursorEmpty()||!sameOrigin(pose)){finish(State.FAILED,now);return;}
   session.clearExpectedMenu(handle);finish(State.COMPLETE,now);
  }
 }
 public void cancel(long now){if(active())finish(State.CANCELLED,now);}
 private void finish(State terminal,long now){
  var done=handle;handle=null;state=terminal;if(proof!=null)proof.cancel();
  // This port owns data/menu bookkeeping only, no key overlay: cleanup must succeed before releasing a successful Farm hold.
  try{port.dispose(done);}catch(RuntimeException cleanup){state=State.FAILED;}
  boolean success=terminal==State.COMPLETE&&state==terminal;try{if(!session.finish(done,success,now)&&success)state=State.FAILED;}catch(RuntimeException cleanup){state=State.FAILED;}
 }
}
