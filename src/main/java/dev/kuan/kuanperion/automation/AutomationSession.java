// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.EnumSet;
import java.util.function.Supplier;
/** One gameplay task and a revocable hold on the existing manually armed Farming session. */
public final class AutomationSession {
 public record Handle(AutomationCoordinator.Lease lease,FarmingHandoff.Token hold,long generation){}
 private final AutomationCoordinator coordinator;private final Supplier<AutomationContext> observe;private final Runnable pauseFarm,safeStopFarm;
 private final FarmingHandoff farm=new FarmingHandoff();private long generation=1;private Handle active;private MenuContract expectedMenu;private Supplier<MenuSnapshot> menus;private int claimedContainer=-1;
 private record Transition(MenuSnapshot origin,MenuContract from,MenuContract next,long started,long timeout){}
 private Transition transition;
 public AutomationSession(AutomationCoordinator coordinator,Supplier<AutomationContext> observe,Runnable pauseFarm,Runnable safeStopFarm){this.coordinator=coordinator;this.observe=observe;this.pauseFarm=pauseFarm;this.safeStopFarm=safeStopFarm;}
 public boolean isCurrent(Handle handle){return handle!=null&&handle.equals(active);}
 public boolean active(){return active!=null;}public boolean holdsFarming(){return farm.held();}
 public Handle begin(AutomationFeature feature,long now,long lifetime){
  if(active!=null||feature==AutomationFeature.STASH)return null;
  var context=forFeature(observe.get(),feature);var lease=coordinator.tryAcquire(feature,context,now,lifetime);if(lease==null)return null;
  var hold=farm.acquire(context.worldId(),generation,context.farmingActive());active=new Handle(lease,hold,generation);
  if(hold!=null)try{pauseFarm.run();}catch(RuntimeException failure){finish(active,false,now);return null;}
  return active;
 }
 public void cancel(long now){finish(active,false,now);}
 public void clearExpectedMenu(Handle handle){if(!isCurrent(handle))return;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;}
 public void expectMenu(Handle handle,MenuContract contract,Supplier<MenuSnapshot> menus){
  if(handle==null||!handle.equals(active))throw new IllegalStateException("Not current task");this.expectedMenu=contract;this.menus=menus;claimedContainer=-1;transition=null;
 }
 /** Register before the guarded native click; permits only the old container or exact next contract. */
 public boolean transitionMenu(Handle handle,MenuSnapshot origin,MenuContract from,MenuContract next,long now,long timeout){
  if(!isCurrent(handle)||transition!=null||from==null||next==null||!java.util.Collections.disjoint(from.titles(),next.titles())||timeout<=0||timeout>10_000_000_000L||menus==null||!from.equals(expectedMenu)||!from.matches(origin)||origin.worldId()!=handle.lease().worldId()||claimedContainer>=0&&claimedContainer!=origin.containerId()||!origin.equals(menus.get()))return false;
  transition=new Transition(origin,from,next,now,timeout);return true;
 }
 public void poll(long now){
  if(active==null)return;var context=observe.get();
  if(!coordinator.isOwner(active.lease(),context,now)){finish(active,false,now);return;}
  if(transition!=null){
   long elapsed=now-transition.started();var menu=menus.get();
   if(elapsed<0||elapsed>=transition.timeout()){finish(active,false,now);return;}
   if(transition.next().matches(menu)&&menu.worldId()==context.worldId()){
    expectedMenu=transition.next();claimedContainer=menu.containerId();transition=null;return;
   }
   if(menu==null){if(context.blockers().contains(AutomationContext.Blocker.MENU))finish(active,false,now);return;}
   var origin=transition.origin();
   if(!transition.from().matches(menu)||menu.worldId()!=origin.worldId()||menu.containerId()!=origin.containerId())finish(active,false,now);
   return;
  }
  if(context.blockers().contains(AutomationContext.Blocker.MENU)||claimedContainer>=0){
   var menu=menus==null?null:menus.get();
   if(expectedMenu==null||!expectedMenu.matches(menu)||menu.worldId()!=context.worldId()||claimedContainer>=0&&claimedContainer!=menu.containerId()){finish(active,false,now);return;}
   claimedContainer=menu.containerId();
  }
 }
 public boolean finish(Handle handle,boolean success,long now){
  if(handle==null||!handle.equals(active))return false;AutomationContext context;
  try{context=java.util.Objects.requireNonNull(observe.get());}catch(RuntimeException unavailable){
   // Same root/generation only: an unavailable native context cannot leave an invisible Farming hold or lease alive.
   active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;coordinator.release(handle.lease());farm.invalidate();
   if(handle.hold()!=null&&handle.generation()==generation)safeStopFarm.run();return false;
  }
  boolean owns=coordinator.isOwner(handle.lease(),context,now);
  active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;coordinator.release(handle.lease());
  boolean resume=handle.hold()==null||farm.finish(handle.hold(),context.worldId(),generation,context.farmingActive());
  boolean completed=success&&owns&&resume;
  if(!completed&&handle.hold()!=null&&handle.generation()==generation&&context.farmingActive())safeStopFarm.run();
  return completed;
 }
 /** Called by legacy stop/new-start, Mining, recovery, Jerry stop and world invalidation. No callback recursion. */
 public void invalidate(){if(active!=null)coordinator.release(active.lease());active=null;expectedMenu=null;menus=null;claimedContainer=-1;transition=null;farm.invalidate();generation++;}
 public static AutomationContext forFeature(AutomationContext context,AutomationFeature feature){
  if(feature!=AutomationFeature.AUTO_SELL&&feature!=AutomationFeature.JUNK&&feature!=AutomationFeature.PET_SELL&&feature!=AutomationFeature.BOOK_COMBINE)return context;
  var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(context.blockers());blockers.remove(AutomationContext.Blocker.INVENTORY_FULL);
  return new AutomationContext(context.worldId(),context.farmingActive(),context.garden(),blockers);
 }
}
