// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.EnumSet;
import java.util.function.Supplier;
/** One gameplay task and a revocable hold on the existing manually armed Farming session. */
public final class AutomationSession {
 public record Handle(AutomationCoordinator.Lease lease,FarmingHandoff.Token hold,long generation){}
 private final AutomationCoordinator coordinator;private final Supplier<AutomationContext> observe;private final Runnable pauseFarm,safeStopFarm;
 private final FarmingHandoff farm=new FarmingHandoff();private long generation=1;private Handle active;private MenuContract expectedMenu;private Supplier<MenuSnapshot> menus;private int claimedContainer=-1;private long menuRevision;
 private record Transition(MenuSnapshot origin,MenuContract from,MenuContract next,long started,long timeout){}
 private Transition transition;
 private record ScreenSequence(java.util.function.BooleanSupplier owns,long started,long timeout){}
 private ScreenSequence screens;
 /** Read-only native sequence ownership (e.g. Bazaar quantity sign). Never action authority.
  * The workflow must prove each concrete screen; every native action retains its independent guards. */
 public boolean expectScreenSequence(Handle handle,java.util.function.BooleanSupplier owns,long now,long timeout){
  if(!isCurrent(handle)||owns==null||timeout<=0||timeout>10_000_000_000L)return false;
  expectedMenu=null;menus=null;claimedContainer=-1;transition=null;screens=new ScreenSequence(owns,now,timeout);menuRevision++;return true;
 }

 public AutomationSession(AutomationCoordinator coordinator,Supplier<AutomationContext> observe,Runnable pauseFarm,Runnable safeStopFarm){this.coordinator=coordinator;this.observe=observe;this.pauseFarm=pauseFarm;this.safeStopFarm=safeStopFarm;}
 public boolean isCurrent(Handle handle){return handle!=null&&handle.equals(active);}
 public boolean active(){return active!=null;}public boolean holdsFarming(){return farm.held();}
 public Handle begin(AutomationFeature feature,long now,long lifetime){
  if(active!=null||feature==AutomationFeature.STASH)return null;
  long observedGeneration=generation;AutomationContext context;
  try{context=forFeature(java.util.Objects.requireNonNull(observe.get()),feature);}catch(RuntimeException unavailable){return null;}
  if(active!=null||generation!=observedGeneration)return null;
  var lease=coordinator.tryAcquire(feature,context,now,lifetime);if(lease==null)return null;
  var hold=farm.acquire(context.worldId(),generation,context.farmingActive());var created=new Handle(lease,hold,generation);active=created;
  if(hold!=null)try{pauseFarm.run();}catch(RuntimeException failure){finish(created,false,now);return null;}
  return isCurrent(created)?created:null;
 }
 public void cancel(long now){finish(active,false,now);}
 public void clearExpectedMenu(Handle handle){if(!isCurrent(handle))return;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;screens=null;menuRevision++;}
 public void expectMenu(Handle handle,MenuContract contract,Supplier<MenuSnapshot> menus){
  if(handle==null||!handle.equals(active))throw new IllegalStateException("Not current task");this.expectedMenu=contract;this.menus=menus;claimedContainer=-1;transition=null;screens=null;menuRevision++;
 }
 /** Register before the guarded native click; permits only the old container or exact next contract. */
 public boolean transitionMenu(Handle handle,MenuSnapshot origin,MenuContract from,MenuContract next,long now,long timeout){
  if(!isCurrent(handle)||transition!=null||from==null||next==null||!java.util.Collections.disjoint(from.titles(),next.titles())||timeout<=0||timeout>10_000_000_000L||menus==null||!from.equals(expectedMenu)||!from.matches(origin)||origin.worldId()!=handle.lease().worldId()||claimedContainer>=0&&claimedContainer!=origin.containerId())return false;
  var reader=menus;var contract=expectedMenu;long binding=menuRevision;var current=observeMenu(handle,reader,now);
  if(!isCurrent(handle)||menuRevision!=binding||transition!=null||menus!=reader||expectedMenu!=contract||!origin.equals(current))return false;
  transition=new Transition(origin,from,next,now,timeout);menuRevision++;return true;
 }
 /** A failed/stale observation may revoke only the root that requested it, not a newer reentrant owner/binding. */
 public void poll(long now){
  var handle=active;if(handle==null)return;AutomationContext context;
  try{context=java.util.Objects.requireNonNull(observe.get());}catch(RuntimeException unavailable){finish(handle,false,now);return;}
  if(!isCurrent(handle))return;
  if(!coordinator.isOwner(handle.lease(),context,now)){finish(handle,false,now);return;}
  var sequence=screens;
  if(sequence!=null){
   long revision=menuRevision,elapsed=now-sequence.started();boolean owned=false;
   try{owned=elapsed>=0&&elapsed<sequence.timeout()&&sequence.owns().getAsBoolean();}catch(RuntimeException unavailable){}
   if(isCurrent(handle)&&menuRevision==revision&&screens==sequence&&!owned)finish(handle,false,now);
   return;
  }
  var flow=transition;var reader=menus;var contract=expectedMenu;long binding=menuRevision;
  if(flow!=null){
   long elapsed=now-flow.started();if(elapsed<0||elapsed>=flow.timeout()){finish(handle,false,now);return;}
   var menu=observeMenu(handle,reader,now);
   if(!isCurrent(handle)||menuRevision!=binding||transition!=flow||menus!=reader||expectedMenu!=contract)return;
   if(flow.next().matches(menu)&&menu.worldId()==context.worldId()){expectedMenu=flow.next();claimedContainer=menu.containerId();transition=null;screens=null;menuRevision++;return;}
   if(menu==null){if(context.blockers().contains(AutomationContext.Blocker.MENU))finish(handle,false,now);return;}
   var origin=flow.origin();if(!flow.from().matches(menu)||menu.worldId()!=origin.worldId()||menu.containerId()!=origin.containerId())finish(handle,false,now);return;
  }
  if(context.blockers().contains(AutomationContext.Blocker.MENU)||claimedContainer>=0){
   var menu=observeMenu(handle,reader,now);
   if(!isCurrent(handle)||menuRevision!=binding||transition!=flow||menus!=reader||expectedMenu!=contract)return;
   if(contract==null||!contract.matches(menu)||menu.worldId()!=context.worldId()||claimedContainer>=0&&claimedContainer!=menu.containerId()){finish(handle,false,now);return;}
   if(claimedContainer!=menu.containerId()){claimedContainer=menu.containerId();menuRevision++;}
  }
 }
 private MenuSnapshot observeMenu(Handle handle,Supplier<MenuSnapshot> reader,long now){
  try{return reader==null?null:reader.get();}catch(RuntimeException unavailable){finish(handle,false,now);return null;}
 }
 public boolean finish(Handle handle,boolean success,long now){
  if(handle==null||!handle.equals(active))return false;AutomationContext context;
  try{context=java.util.Objects.requireNonNull(observe.get());}catch(RuntimeException unavailable){
   if(!isCurrent(handle))return false;
   // Same root/generation only: an unavailable native context cannot leave an invisible Farming hold or lease alive.
   active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;screens=null;menuRevision++;coordinator.release(handle.lease());farm.invalidate();
   if(handle.hold()!=null&&handle.generation()==generation)safeStopFarm.run();return false;
  }
  if(!isCurrent(handle))return false;
  boolean owns=coordinator.isOwner(handle.lease(),context,now);
  active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;screens=null;menuRevision++;coordinator.release(handle.lease());
  boolean resume=handle.hold()==null||farm.finish(handle.hold(),context.worldId(),generation,context.farmingActive());
  boolean completed=success&&owns&&resume;
  if(!completed&&handle.hold()!=null&&handle.generation()==generation&&context.farmingActive())safeStopFarm.run();
  return completed;
 }
 /** Called by legacy stop/new-start, Mining, recovery, Jerry stop and world invalidation. No callback recursion. */
 public void invalidate(){if(active!=null)coordinator.release(active.lease());active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;screens=null;menuRevision++;farm.invalidate();generation++;}
 public static AutomationContext forFeature(AutomationContext context,AutomationFeature feature){
  if(feature!=AutomationFeature.AUTO_SELL&&feature!=AutomationFeature.JUNK&&feature!=AutomationFeature.PET_SELL&&feature!=AutomationFeature.BOOK_COMBINE)return context;
  var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(context.blockers());blockers.remove(AutomationContext.Blocker.INVENTORY_FULL);
  return new AutomationContext(context.worldId(),context.farmingActive(),context.garden(),blockers);
 }
}
