// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;

import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.pest.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Thin 26.2 adapter. New tasks cannot write legacy movement/toggles or use arbitrary commands. */
public final class AutomationClientBridge {
 public record LegacyState(boolean farmingArmed,boolean mining,boolean recovering,boolean selecting,String island){}
 private final Consumer<StashService.Notice> feedback;
 private final Supplier<LegacyState> legacy;private final Function<Minecraft,List<String>> sidebar;
 private final InventoryReceipts receipts=new InventoryReceipts();
 private final PlayerPositionReceipts positions=new PlayerPositionReceipts();
 private final AutomationCoordinator coordinator=new AutomationCoordinator();private final AutomationSession session;
 private final PestService pest;private final PestFlightController flight;
 private final JunkService junk;private final AutoSellService autoSell;private boolean ownGameplayCommand,observedGameplayCommand;
 private final StashService stash;private Object levelIdentity;private long worldGeneration;
 public AutomationClientBridge(Path configDir,Supplier<LegacyState> legacy,Function<Minecraft,List<String>> sidebar,Consumer<StashService.Notice> feedback,Runnable pauseFarm,Runnable safeStopFarm){
  this.feedback=feedback;this.legacy=legacy;this.sidebar=sidebar;
  var settings=AutomationSettingsStore.load(configDir.resolve("kuanperion/automation.json"));
  stash=new StashService(settings,coordinator,this::observe,this::send,feedback);
  session=new AutomationSession(coordinator,this::observe,pauseFarm,safeStopFarm);
  flight=new PestFlightController(coordinator,session,this::observe,this::sendPestTravel);
  pest=new PestService(settings,session,flight,positions,()->Minecraft.getInstance().gui.screen()==null,feedback);
  autoSell=new AutoSellService(settings,session,receipts,new AutoSellTask.Port(){
   public AutomationContext context(){return observe();}
   public MenuSnapshot menu(){return AutomationGameView.menu(Minecraft.getInstance(),currentWorldId());}
   public List<ItemSnapshot> inventory(){return AutomationGameView.inventory(Minecraft.getInstance()).stream().filter(s->s.index()<36).map(AutomationGameView.InventorySlot::item).toList();}
   public boolean command(AutomationSession.Handle handle,AutomationCommand command){return sendGameplay(handle,command);}
   public boolean sell(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection protection,long now){return session.isCurrent(handle)&&AutomationMenuActions.inventoryAction(coordinator,handle.lease(),contract,ticket,protection,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,AutomationClientBridge.this::observe,this::menu,now);}
   public void closeOwnedMenu(int id){var mc=Minecraft.getInstance();var menu=menu();if(mc.player!=null&&menu!=null&&menu.containerId()==id)mc.player.closeContainer();}
  },()->Minecraft.getInstance().gui.screen()==null,feedback);
  junk=new JunkService(settings,session,receipts,new JunkTask.Port(){
   public AutomationContext context(){return observe();}
   public JunkPolicy.Location location(){var mc=Minecraft.getInstance();return mc.level==null||mc.player==null?null:new JunkPolicy.Location(mc.level.dimension().toString(),mc.player.getX(),mc.player.getY(),mc.player.getZ());}
   public List<ItemSnapshot> inventory(){return AutomationGameView.inventory(Minecraft.getInstance()).stream().filter(s->s.index()<36).map(AutomationGameView.InventorySlot::item).toList();}
   public MenuSnapshot menu(){return AutomationGameView.junkMenu(Minecraft.getInstance(),currentWorldId());}
   public boolean open(AutomationSession.Handle handle){
    var mc=Minecraft.getInstance();if(!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.JUNK||!AutomationSession.forFeature(observe(),AutomationFeature.JUNK).canIssueCommand()||mc.player==null||mc.player.containerMenu!=mc.player.inventoryMenu||!mc.player.containerMenu.getCarried().isEmpty())return false;
    mc.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));return menu()!=null;
   }
   public boolean drop(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now){return session.isCurrent(handle)&&handle.lease().owner()==AutomationFeature.JUNK&&AutomationMenuActions.inventoryAction(coordinator,handle.lease(),contract,ticket,rules,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,AutomationClientBridge.this::observe,this::menu,now);}
   public void closeOwnedMenu(int id){var mc=Minecraft.getInstance();var menu=menu();if(mc.player!=null&&menu!=null&&menu.containerId()==id)mc.player.closeContainer();}
  },()->Minecraft.getInstance().gui.screen()==null,feedback);
 }
 public PestService pest(){return pest;}
 public void beforeMovementTick(){if(pest.active()&&(!observe().canIssueCommand()&&!pest.ownsMenu()||flight.manualInput()))pest.cancel(System.nanoTime());}
 public void onManualLook(){if(pest.active()&&Minecraft.getInstance().gui.screen()==null)pest.cancel(System.nanoTime());}
 public StashService stash(){return stash;}
 public AutoSellService autoSell(){return autoSell;}
 public JunkService junk(){return junk;}
 public void tick(){long now=System.nanoTime();pest.tick(now);stash.tick(now);autoSell.tick(now);junk.tick(now);}
 public void beforeLegacyTick(){session.poll(System.nanoTime());}
 public PlayerPositionReceipts positionReceipts(){return positions;}
 public void serverPosition(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket packet){
  if(packet==null){positions.clear();return;}var point=AutomationPositionView.absolute(packet);
  positions.observe(currentWorldId(),packet.id(),point,point!=null,System.nanoTime());
 }
 public InventoryReceipts receipts(){return receipts;}
 public void serverSlot(int container,int slot,net.minecraft.world.item.ItemStack stack){if(!session.active())return;receipts.observe(currentWorldId(),container,slot,AutomationGameView.item(stack,false));}
 public AutomationSession session(){return session;}
 public AutomationCoordinator coordinator(){return coordinator;}
 public boolean holdsFarming(){return session.holdsFarming();}
 public void invalidateFarmSession(){session.invalidate();pest.cancel(System.nanoTime());autoSell.cancel(System.nanoTime());junk.cancel(System.nanoTime());}
 public void onServerGameMessage(String text){long now=System.nanoTime();stash.onSystemMessage(text,now);autoSell.onSystemMessage(text,now);}
 public void onOutgoingCommand(){if(ownGameplayCommand){observedGameplayCommand=true;return;}stash.onOutgoingCommand();pest.cancel(System.nanoTime());session.cancel(System.nanoTime());}
 public void onManualInventoryInput(){pest.cancel(System.nanoTime());session.cancel(System.nanoTime());}
 public void worldChanged(){stash.cancel("切換世界");session.invalidate();pest.cancel(System.nanoTime());autoSell.cancel(System.nanoTime());junk.cancel(System.nanoTime());receipts.clear();positions.clear();}
 private long currentWorldId(){var level=Minecraft.getInstance().level;if(level!=levelIdentity){levelIdentity=level;worldGeneration++;}return worldGeneration;}
 public AutomationContext observe(){
  Minecraft mc=Minecraft.getInstance();var flags=legacy.get();var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);
  currentWorldId();
  if(mc.player==null||mc.level==null)blockers.add(AutomationContext.Blocker.DISCONNECTED);
  if(mc.player!=null&&!mc.player.isAlive())blockers.add(AutomationContext.Blocker.DEAD);
  if(mc.getWindow()==null||GLFW.glfwGetWindowAttrib(mc.getWindow().handle(),GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE)blockers.add(AutomationContext.Blocker.FOCUS);
  if(flags.mining())blockers.add(AutomationContext.Blocker.MINING);
  if(flags.recovering())blockers.add(AutomationContext.Blocker.RECOVERY);
  if(flags.selecting())blockers.add(AutomationContext.Blocker.SELECTION);
  if(mc.gui.screen()!=null&&!(mc.gui.screen() instanceof dev.kuan.kuanperion.ui.MainGUI))blockers.add(AutomationContext.Blocker.MENU);
  if(mc.player!=null&&mc.player.getInventory().getFreeSlot()<0)blockers.add(AutomationContext.Blocker.INVENTORY_FULL);
  List<String> lines=List.of();
  if(mc.level!=null&&mc.player!=null)try{lines=sidebar.apply(mc);}catch(RuntimeException unavailable){lines=List.of();}
  var location=AutomationLocation.inspect(lines,flags.island());
  String address=mc.getCurrentServer()==null?"":mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
  boolean official=address.matches("(?:[a-z0-9-]+\\.)*hypixel\\.net(?::[0-9]+)?");
  boolean skyblock=official&&location.skyblock();
  if(!skyblock)blockers.add(AutomationContext.Blocker.OUTSIDE_SKYBLOCK);
  boolean garden=skyblock&&location.garden();
  return new AutomationContext(worldGeneration,flags.farmingArmed(),garden,blockers);
 }
 private boolean sendPestTravel(AutomationSession.Handle handle,PestPlot plot,long now){
  if(plot==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.PEST)return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(plot.command());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /"+plot.command(),false,"Pest"));return true;}finally{ownGameplayCommand=false;}
 }
 private boolean sendGameplay(AutomationSession.Handle handle,AutomationCommand command){
  if(!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.AUTO_SELL||command!=AutomationCommand.BOOSTER_COOKIE)return false;
  var context=AutomationSession.forFeature(observe(),AutomationFeature.AUTO_SELL);
  if(!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,System.nanoTime()))return false;
  var mc=Minecraft.getInstance();if(mc.player==null)return false;ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(command.wire());return observedGameplayCommand;}finally{ownGameplayCommand=false;}
 }
 private boolean send(AutomationCommand command){
  if(command!=AutomationCommand.PICKUP_STASH)return false;
  if(!observe().canIssueCommand())return false;
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return false;
  mc.player.connection.sendCommand(command.wire());return true;
 }
}
