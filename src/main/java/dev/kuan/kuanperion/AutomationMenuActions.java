// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.pest.PestExchangeTask;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 /** Pest-only private action, not a general carried-item sender. Full frame is independently sampled by the flight port. */
 static boolean sendPestBaitNative(AutomationSession.Handle owner,dev.kuan.kuanperion.automation.pest.PestBaitButtonTransfer.Action action,dev.kuan.kuanperion.automation.pest.PestBaitButtonTransfer.Frame frame,long now){
  var mc=Minecraft.getInstance();
  if(ownedInput||mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||mc.gameMode==null||owner==null||owner.lease().owner()!=AutomationFeature.PEST||action==null||frame==null||frame.menu()==null||frame.world()!=owner.lease().worldId()||mc.level!=frame.level())return false;
  var request=action.ticket();var menu=mc.player.containerMenu;
  if(menu!=frame.menuIdentity()||menu.containerId!=frame.menu().containerId()||request.click()!=MenuContract.Click.PICKUP||!frame.menu().equals(request.observed())||!frame.menu().equals(AutomationGameView.menu(mc,frame.world()))||menu.slots.size()!=frame.menu().slots().size()||request.slot()<0||request.slot()>=menu.slots.size()||mc.player.getInventory().getContainerSize()!=43)return false;
  // Claim may read fallible/reentrant context; physical slot rules must be sampled AFTER it.
  if(!action.claim(owner,frame,now)||ownedInput||mc.level!=frame.level()||mc.player.containerMenu!=menu||!frame.menu().equals(AutomationGameView.menu(mc,frame.world()))||!frame.cursor().equals(AutomationGameView.item(menu.getCarried(),false)))return false;
  var slot=menu.slots.get(request.slot());boolean player=frame.menu().slot(request.slot()).playerInventory();
  if(slot.index!=request.slot()||slot.isFake()||!slot.isActive())return false;
  if(action.step()==dev.kuan.kuanperion.automation.pest.PestBaitButtonTransfer.Step.DEPOSIT){
   if(player||slot.container==mc.player.getInventory()||menu.getCarried().isEmpty()||!slot.mayPlace(menu.getCarried())||slot.getMaxStackSize(menu.getCarried())<menu.getCarried().getCount())return false;
  }else{
   int raw=request.slot()-frame.menu().containerSlots()+9;
   if(!player||raw<9||raw>=36||slot.container!=mc.player.getInventory()||slot.getContainerSlot()!=raw)return false;
   if(action.step()==dev.kuan.kuanperion.automation.pest.PestBaitButtonTransfer.Step.PICKUP){if(!menu.getCarried().isEmpty()||!slot.mayPickup(mc.player))return false;}
   else if(menu.getCarried().isEmpty()||!slot.getItem().isEmpty()||!slot.mayPlace(menu.getCarried())||slot.getMaxStackSize(menu.getCarried())<menu.getCarried().getCount())return false;
  }
  for(int i=0;i<43;i++)if(!frame.inventory().get(i).equals(AutomationGameView.item(mc.player.getInventory().getItem(i),i<9||i>=36)))return false;
  ownedInput=true;try{mc.gameMode.handleContainerInput(menu.containerId,request.slot(),0,ContainerInput.PICKUP,mc.player);return true;}finally{ownedInput=false;}
 }
 /** Narrow source-role exchange only; carried items remain rejected by the generic sender. */
 static boolean sendPestExchangeNative(AutomationSession.Handle owner,PestExchangeTask.Action action,PestExchangeTask.Frame frame,long now){
  var mc=Minecraft.getInstance();
  if(ownedInput||mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||mc.gameMode==null||mc.gui.overlay()!=null||owner==null||owner.lease().owner()!=AutomationFeature.PEST||action==null||action.kind()!=PestExchangeTask.Kind.CLICK||frame==null||frame.menu()==null||frame.world()!=owner.lease().worldId()||mc.level!=frame.level()||frame.cursor().count()!=0)return false;
  var player=mc.player;var menu=player.containerMenu;var screen=mc.gui.screen();var ticket=action.ticket();
  if(!(screen instanceof AbstractContainerScreen<?> containerScreen)||containerScreen.getMenu()!=menu||menu!=frame.menuIdentity()||menu.containerId!=frame.menu().containerId()||ticket==null||ticket.click()!=MenuContract.Click.PICKUP||!frame.menu().equals(ticket.observed())||!frame.menu().equals(AutomationGameView.menu(mc,frame.world()))||menu.slots.size()!=frame.menu().slots().size()||ticket.slot()<0||ticket.slot()>=frame.menu().containerSlots()||frame.menu().slot(ticket.slot()).playerInventory()||player.getInventory().getContainerSize()!=43)return false;
  if(!action.claim(owner,frame,now)||ownedInput||mc.level!=frame.level()||mc.player!=player||player.containerMenu!=menu||mc.gui.screen()!=screen||mc.gui.overlay()!=null||!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isSleeping()||player.isUsingItem()||!player.getAbilities().mayfly||player.getInventory().getSelectedSlot()!=frame.selected()||!menu.getCarried().isEmpty()||!frame.menu().equals(AutomationGameView.menu(mc,frame.world())))return false;
  var slot=menu.slots.get(ticket.slot());
  if(slot.index!=ticket.slot()||slot.container==player.getInventory()||slot.isFake()||!slot.isActive()||!slot.mayPickup(player)||!frame.menu().item(ticket.slot()).equals(AutomationGameView.item(slot.getItem(),false)))return false;
  for(int i=0;i<43;i++)if(!frame.inventory().get(i).equals(AutomationGameView.item(player.getInventory().getItem(i),i<9||i>=36)))return false;
  ownedInput=true;try{mc.gameMode.handleContainerInput(menu.containerId,ticket.slot(),0,ContainerInput.PICKUP,player);return true;}finally{ownedInput=false;}
 }
 /** One-use close of the exact acknowledged menu; never closes an unknown or taken-over screen. */
 static boolean closePestExchangeNative(AutomationSession.Handle owner,PestExchangeTask.Action action,PestExchangeTask.Frame frame,long now){
  var mc=Minecraft.getInstance();
  if(ownedInput||mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||mc.gui.overlay()!=null||owner==null||owner.lease().owner()!=AutomationFeature.PEST||action==null||action.kind()!=PestExchangeTask.Kind.CLOSE||frame==null||frame.menu()==null||frame.world()!=owner.lease().worldId()||mc.level!=frame.level()||frame.cursor().count()!=0)return false;
  var player=mc.player;var menu=player.containerMenu;var screen=mc.gui.screen();
  if(!(screen instanceof AbstractContainerScreen<?> containerScreen)||containerScreen.getMenu()!=menu||menu!=frame.menuIdentity()||menu.containerId!=frame.menu().containerId()||player.getInventory().getContainerSize()!=43||!frame.menu().equals(AutomationGameView.menu(mc,frame.world())))return false;
  if(!action.claim(owner,frame,now)||ownedInput||mc.level!=frame.level()||mc.player!=player||player.containerMenu!=menu||mc.gui.screen()!=screen||mc.gui.overlay()!=null||!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isSleeping()||player.isUsingItem()||!player.getAbilities().mayfly||player.getInventory().getSelectedSlot()!=frame.selected()||!menu.getCarried().isEmpty()||!frame.menu().equals(AutomationGameView.menu(mc,frame.world())))return false;
  for(int i=0;i<43;i++)if(!frame.inventory().get(i).equals(AutomationGameView.item(player.getInventory().getItem(i),i<9||i>=36)))return false;
  ownedInput=true;try{player.closeContainer();return player.containerMenu==player.inventoryMenu&&mc.gui.screen()==null&&player.containerMenu.getCarried().isEmpty();}finally{ownedInput=false;}
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
