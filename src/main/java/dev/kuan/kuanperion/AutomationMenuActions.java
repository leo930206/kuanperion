// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
/** Typed 26.2 boundary; never sends a blind/stale click. Workflow authorization precedes this gate. */
public final class AutomationMenuActions {
 private static boolean ownedInput;
 public static boolean ownsInventoryInput(){return ownedInput;}
 private AutomationMenuActions(){}
 public static boolean click(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,GuardedMenuAction.Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  // Player-inventory mutations require an explicit authorization policy, not a generic GUI click.
  if(ticket==null||ticket.click()==MenuContract.Click.THROW||ticket.observed().slot(ticket.slot()).playerInventory())return false;
  return execute(coordinator,lease,contract,ticket,space,observe,menus,now);
 }
 public static boolean inventoryAction(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,GuardedMenuAction.Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  if(ticket==null||rules==null||!ticket.observed().slot(ticket.slot()).playerInventory()||!rules.permits(ticket.observed().item(ticket.slot())))return false;
  if(ticket.click()==MenuContract.Click.THROW&&(lease==null||lease.owner()!=AutomationFeature.JUNK))return false;
  return execute(coordinator,lease,contract,ticket,space,observe,menus,now);
 }
 private static boolean execute(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,GuardedMenuAction.Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  return GuardedMenuAction.execute(coordinator,lease,contract,ticket,space,observe,menus,request->{
   var mc=Minecraft.getInstance();if(mc.player==null||mc.gameMode==null||!mc.player.containerMenu.getCarried().isEmpty())return false;
   var input=switch(request.click()){case PICKUP->ContainerInput.PICKUP;case QUICK_MOVE->ContainerInput.QUICK_MOVE;case THROW->ContainerInput.THROW;};
   int button=request.click()==MenuContract.Click.THROW?1:0;
   ownedInput=true;try{mc.gameMode.handleContainerInput(request.observed().containerId(),request.slot(),button,input,mc.player);return true;}finally{ownedInput=false;}
  },now);
 }
}
