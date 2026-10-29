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
  if(!menuButton(ticket))return false;
  return execute(coordinator,lease,contract,ticket,space,observe,menus,now);
 }
 public static boolean inventoryAction(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,GuardedMenuAction.Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  if(ticket==null||rules==null||!ticket.observed().slot(ticket.slot()).playerInventory()||!rules.permits(ticket.observed().item(ticket.slot())))return false;
  if(ticket.click()==MenuContract.Click.THROW&&(lease==null||lease.owner()!=AutomationFeature.JUNK))return false;
  return execute(coordinator,lease,contract,ticket,space,observe,menus,now);
 }
 /** A rejected guard and a possibly-sent native failure must not look identical to a spend ledger. */
 public static GuardedMenuAction.Result purchaseClick(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  if(!menuButton(ticket))return GuardedMenuAction.Result.REJECTED;
  return GuardedMenuAction.perform(coordinator,lease,contract,ticket,GuardedMenuAction.Space.REQUIRE_FREE_SLOT,observe,menus,AutomationMenuActions::sendNative,now);
 }
 private static boolean menuButton(MenuContract.Ticket ticket){return ticket!=null&&ticket.observed()!=null&&ticket.observed().slot(ticket.slot())!=null&&ticket.click()!=MenuContract.Click.THROW&&!ticket.observed().slot(ticket.slot()).playerInventory();}
 private static boolean execute(AutomationCoordinator coordinator,AutomationCoordinator.Lease lease,MenuContract contract,MenuContract.Ticket ticket,GuardedMenuAction.Space space,Supplier<AutomationContext> observe,Supplier<MenuSnapshot> menus,long now){
  return GuardedMenuAction.execute(coordinator,lease,contract,ticket,space,observe,menus,AutomationMenuActions::sendNative,now);
 }
 /** Only the Book controller may use this narrow carried-item path after its own source/cursor/server/root proof. */
 static boolean depositBookNative(MenuContract.Ticket request,ItemSnapshot ownBook){
   var mc=Minecraft.getInstance();
   if(ownedInput||mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||mc.gameMode==null||request==null||request.observed()==null||ownBook==null||request.click()!=MenuContract.Click.PICKUP||request.slot()<54||request.slot()>=81)return false;
   if(!ownBook.identified()||ownBook.protectedItem()||ownBook.count()!=1||!ownBook.skyblockId().equals("ENCHANTED_BOOK")||!ownBook.minecraftId().equals("minecraft:enchanted_book")||!ownBook.name().equals("Enchanted Book"))return false;
   var menu=mc.player.containerMenu;var current=AutomationGameView.menu(mc,request.observed().worldId());
   if(menu.containerId!=request.observed().containerId()||menu.slots.size()!=90||!request.observed().equals(current)||!ownBook.equals(AutomationGameView.item(menu.getCarried(),false)))return false;
   var slot=menu.slots.get(request.slot());
   if(slot.index!=request.slot()||slot.container!=mc.player.getInventory()||slot.getContainerSlot()!=request.slot()-54+9||!slot.getItem().isEmpty()||!slot.isActive()||slot.isFake()||!slot.mayPlace(menu.getCarried())||slot.getMaxStackSize(menu.getCarried())<1)return false;
   ownedInput=true;try{mc.gameMode.handleContainerInput(menu.containerId,request.slot(),0,ContainerInput.PICKUP,mc.player);return true;}finally{ownedInput=false;}
 }
 /** Exact native sign identity after owned Custom Amount; only decimal quantities, no clipboard/raw commands. */
 static net.minecraft.client.input.KeyEvent bazaarSelectAllKey(){return new net.minecraft.client.input.KeyEvent(65,0,net.minecraft.client.input.InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);}
 static boolean enterBazaarAmountNative(Object expected,int quantity){
  var mc=Minecraft.getInstance();
  if(ownedInput||quantity<1||quantity>4096||mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||mc.gui.screen()!=expected||!(expected instanceof net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen sign)||!mc.player.containerMenu.getCarried().isEmpty())return false;
  ownedInput=true;
  try{
   sign.keyPressed(bazaarSelectAllKey());
   sign.keyPressed(new net.minecraft.client.input.KeyEvent(261,0,0));
   for(char digit:Integer.toString(quantity).toCharArray())sign.charTyped(new net.minecraft.client.input.CharacterEvent(digit));
   if(mc.gui.screen()!=expected)return false;sign.onClose();return mc.gui.screen()!=expected;
  }finally{ownedInput=false;}
 }
 static boolean sendNative(MenuContract.Ticket request){
   var mc=Minecraft.getInstance();if(mc.player==null||mc.gameMode==null||!mc.player.containerMenu.getCarried().isEmpty())return false;
   var input=switch(request.click()){case PICKUP->ContainerInput.PICKUP;case QUICK_MOVE->ContainerInput.QUICK_MOVE;case THROW->ContainerInput.THROW;};
   int button=request.click()==MenuContract.Click.THROW?1:0;
   ownedInput=true;try{mc.gameMode.handleContainerInput(request.observed().containerId(),request.slot(),button,input,mc.player);return true;}finally{ownedInput=false;}
 }
}
