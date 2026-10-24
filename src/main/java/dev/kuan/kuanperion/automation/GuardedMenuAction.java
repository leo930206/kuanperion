// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.EnumSet;
import java.util.function.Supplier;
/** Structural permission boundary. Each workflow must additionally authorize items/cost/quantity. */
public final class GuardedMenuAction {
 public enum Space {REQUIRE_FREE_SLOT,EXISTING_ITEMS_ONLY}
 public enum Result {REJECTED,SENT,UNCERTAIN}
 @FunctionalInterface public interface Sender {boolean click(MenuContract.Ticket ticket);}
 private GuardedMenuAction(){}
 public static boolean execute(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menu,Sender sender,long now){
  return perform(coordinator,lease,contract,ticket,space,observe,menu,sender,now)==Result.SENT;
 }
 /** Distinguishes a rejected guard from a native failure after a possibly irreversible send. */
 public static Result perform(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menu,Sender sender,long now){
  var context=observe.get();var current=menu.get();
  if(context.hardBlocked()||current==null||current.worldId()!=context.worldId()||!contract.revalidate(ticket,current))return Result.REJECTED;
  // Only a fully revalidated owned container can remove MENU. All other blockers remain.
  var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(context.blockers());blockers.remove(AutomationContext.Blocker.MENU);
  if(space==Space.EXISTING_ITEMS_ONLY)blockers.remove(AutomationContext.Blocker.INVENTORY_FULL);
  var owned=new AutomationContext(context.worldId(),context.farmingActive(),context.garden(),blockers);
  if(!coordinator.permits(lease,AutomationCoordinator.Resource.SCREEN,owned,now))return Result.REJECTED;
  try{return sender.click(ticket)?Result.SENT:Result.REJECTED;}catch(RuntimeException failure){return Result.UNCERTAIN;}
 }
}
