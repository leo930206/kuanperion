// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.EnumSet;
import java.util.function.Supplier;
/** Structural permission boundary. Each workflow must additionally authorize items/cost/quantity. */
public final class GuardedMenuAction {
 public enum Space {REQUIRE_FREE_SLOT,EXISTING_ITEMS_ONLY}
 @FunctionalInterface public interface Sender {boolean click(MenuContract.Ticket ticket);}
 private GuardedMenuAction(){}
 public static boolean execute(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menu,Sender sender,long now){
  var context=observe.get();var current=menu.get();
  if(context.hardBlocked()||current==null||current.worldId()!=context.worldId()||!contract.revalidate(ticket,current))return false;
  // Only a fully revalidated owned container can remove MENU. All other blockers remain.
  var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(context.blockers());blockers.remove(AutomationContext.Blocker.MENU);
  if(space==Space.EXISTING_ITEMS_ONLY)blockers.remove(AutomationContext.Blocker.INVENTORY_FULL);
  var owned=new AutomationContext(context.worldId(),context.farmingActive(),context.garden(),blockers);
  if(!coordinator.permits(lease,AutomationCoordinator.Resource.SCREEN,owned,now))return false;
  try{return sender.click(ticket);}catch(RuntimeException failure){return false;}
 }
}
