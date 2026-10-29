// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;

import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.pest.*;
import dev.kuan.kuanperion.automation.greenhouse.*;
import dev.kuan.kuanperion.automation.composter.*;
import dev.kuan.kuanperion.automation.visitor.*;
import dev.kuan.kuanperion.automation.supercraft.*;
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
 private final dev.kuan.kuanperion.automation.greenhouse.HarvestReceipts harvests=new dev.kuan.kuanperion.automation.greenhouse.HarvestReceipts();
 private PestTrapRelease.CloseTicket trapClose;private long trapCloseWorld;private int trapCloseId;
 private final CursorReceipts cursors=new CursorReceipts();
 private final PlayerPositionReceipts positions=new PlayerPositionReceipts();
 private final AutomationCoordinator coordinator=new AutomationCoordinator();private final AutomationSession session;
 private final SupercraftService supercraft;private final SupercraftMenuController supercraftMenu;
 private final VisitorService visitor;private final VisitorFlightController visitorFlight;
 private VisitorTask.CloseTicket visitorClose;private long visitorCloseWorld;private int visitorCloseId;
 private final ComposterService composter;private final ComposterFlightController composterFlight;
 private final GreenhouseService greenhouse;private final GreenhouseFlightController greenhouseFlight;
 private final PestService pest;private final PestFlightController flight;
 private final JunkService junk;private final AutoSellService autoSell;private boolean ownGameplayCommand,observedGameplayCommand;
 private final BookService book;
 private final StashService stash;private Object levelIdentity;private long worldGeneration;
 public AutomationClientBridge(Path configDir,Supplier<LegacyState> legacy,Function<Minecraft,List<String>> sidebar,Consumer<StashService.Notice> feedback,Runnable pauseFarm,Runnable safeStopFarm){
  this.feedback=feedback;this.legacy=legacy;this.sidebar=sidebar;
  var settings=AutomationSettingsStore.load(configDir.resolve("kuanperion/automation.json"));
  stash=new StashService(settings,coordinator,this::observe,this::send,feedback);
  session=new AutomationSession(coordinator,this::observe,pauseFarm,safeStopFarm);
  flight=new PestFlightController(coordinator,session,this::observe,this::sendPestTravel);
  pest=new PestService(settings,session,flight,positions,()->screenReady(AutomationFeature.PEST),feedback);
  greenhouseFlight=new GreenhouseFlightController(coordinator,session,this::observe,this::sendGreenhouseTravel);
  greenhouse=new GreenhouseService(settings,session,positions,harvests,greenhouseFlight,()->screenReady(AutomationFeature.GREENHOUSE),feedback);
  composterFlight=new ComposterFlightController(coordinator,session,this::observe,this::sendComposterTravel,receipts,cursors,this::sendBazaarAcquisition,()->BazaarPurse.read(sidebar.apply(Minecraft.getInstance())).orElse(-1));
  composter=new ComposterService(settings,session,positions,receipts,cursors,composterFlight,()->screenReady(AutomationFeature.COMPOSTER),()->ComposterGameView.capture(Minecraft.getInstance(),observe()),feedback);
  visitorFlight=new VisitorFlightController(coordinator,session,this::observe,this::sendVisitorTravel,receipts,cursors,this::sendBazaarAcquisition,()->BazaarPurse.read(sidebar.apply(Minecraft.getInstance())).orElse(-1));
  visitor=new VisitorService(settings,session,positions,receipts,cursors,visitorFlight,()->screenReady(AutomationFeature.VISITOR),feedback);
  supercraftMenu=new SupercraftMenuController(coordinator,session,this::observe,receipts,this::sendSupercraftRecipe);
  supercraft=new SupercraftService(settings,session,receipts,cursors,supercraftMenu,()->screenReady(AutomationFeature.SUPERCRAFT),feedback);
  book=new BookService(settings,session,receipts,cursors,this::observe,p->new BookMenuController(coordinator,session,this::observe,receipts,cursors,BookAnvilView.confirmedLayout(),p,this::sendBookAnvil),BookAnvilView.confirmedLayout(),()->screenReady(AutomationFeature.BOOK_COMBINE),feedback);
  autoSell=new AutoSellService(settings,session,receipts,new AutoSellTask.Port(){
   public AutomationContext context(){return observe();}
   public MenuSnapshot menu(){return AutomationGameView.menu(Minecraft.getInstance(),currentWorldId());}
   public List<ItemSnapshot> inventory(){return AutomationGameView.inventory(Minecraft.getInstance()).stream().filter(s->s.index()<36).map(AutomationGameView.InventorySlot::item).toList();}
   public boolean command(AutomationSession.Handle handle,AutomationCommand command){return sendGameplay(handle,command);}
   public boolean sell(AutomationSession.Handle handle,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection protection,long now){return session.isCurrent(handle)&&AutomationMenuActions.inventoryAction(coordinator,handle.lease(),contract,ticket,protection,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,AutomationClientBridge.this::observe,this::menu,now);}
   public void closeOwnedMenu(int id){var mc=Minecraft.getInstance();var menu=menu();if(mc.player!=null&&menu!=null&&menu.containerId()==id)mc.player.closeContainer();}
  },()->screenReady(AutomationFeature.AUTO_SELL),feedback);
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
  },()->screenReady(AutomationFeature.JUNK),feedback);
 }
 public BookService book(){return book;}
 public SupercraftService supercraft(){return supercraft;}
 public VisitorService visitor(){return visitor;}
 public PestService pest(){return pest;}
 public GreenhouseService greenhouse(){return greenhouse;}
 public ComposterService composter(){return composter;}
 public void beforeMovementTick(){
  if(!pest.active()&&!greenhouse.active()&&!composter.active()&&!visitor.active()&&!supercraft.active()&&!book.active())return;
  var context=observe();
  if(book.active()&&(Minecraft.getInstance().gui.screen() instanceof dev.kuan.kuanperion.ui.MainGUI||context.worldId()<=0||context.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU&&b!=AutomationContext.Blocker.INVENTORY_FULL)||context.blockers().contains(AutomationContext.Blocker.MENU)&&!book.ownsMenu()))book.cancel(System.nanoTime());
  if(supercraft.active()&&(Minecraft.getInstance().gui.screen() instanceof dev.kuan.kuanperion.ui.MainGUI||context.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||!context.canIssueCommand()&&!supercraft.ownsMenu()||supercraftMenu.manualInput()))supercraft.cancel(System.nanoTime());
  if(visitor.active()&&(!context.garden()||context.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||!context.canIssueCommand()&&!visitor.ownsMenu()||visitorFlight.manualInput()))visitor.cancel(System.nanoTime());
  if(composter.active()&&(!context.garden()||context.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU)||!context.canIssueCommand()&&!composter.ownsMenu()||composterFlight.manualInput()))composter.cancel(System.nanoTime());
  if(pest.active()&&(!context.canIssueCommand()&&!pest.ownsMenu()||flight.manualInput()))pest.cancel(System.nanoTime());
  if(greenhouse.active()&&(!context.canIssueCommand()||!context.garden()||greenhouseFlight.manualInput()))greenhouse.cancel(System.nanoTime());
 }

 public void onManualLook(){if(Minecraft.getInstance().gui.screen()==null){
  long now=System.nanoTime();AutomationStops.run(()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->supercraft.cancel(now),()->book.cancel(now),()->session.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now));
 }}
 public StashService stash(){return stash;}
 public AutoSellService autoSell(){return autoSell;}
 public JunkService junk(){return junk;}
 /** Idle arbitration only: an explicit deferred UI intent wins over automatic timers, never over a live root. */
 private AutomationFeature manualOwner(){
  if(pest.pending())return AutomationFeature.PEST;if(greenhouse.pending())return AutomationFeature.GREENHOUSE;if(composter.pending())return AutomationFeature.COMPOSTER;if(supercraft.pending())return AutomationFeature.SUPERCRAFT;if(visitor.pending())return AutomationFeature.VISITOR;if(book.pending())return AutomationFeature.BOOK_COMBINE;if(autoSell.pending())return AutomationFeature.AUTO_SELL;if(junk.pending())return AutomationFeature.JUNK;return null;
 }
 private boolean screenReady(AutomationFeature feature){
  if(Minecraft.getInstance().gui.screen()!=null)return false;if(session.active())return true;
  var requested=manualOwner();return requested==null||requested==feature;
 }
 public void tick(){
  long now=System.nanoTime();
  // Finish the incumbent before checking due work, irrespective of its dispatch position.
  // Snapshot once: a completed task must not start or tick again in this same pass.
  boolean pestWasActive=pest.active(),greenhouseWasActive=greenhouse.active(),composterWasActive=composter.active(),supercraftWasActive=supercraft.active();
  boolean visitorWasActive=visitor.active(),bookWasActive=book.active(),autoSellWasActive=autoSell.active(),junkWasActive=junk.active();
  if(pestWasActive)pest.tick(now);if(greenhouseWasActive)greenhouse.tick(now);if(composterWasActive)composter.tick(now);if(supercraftWasActive)supercraft.tick(now);if(visitorWasActive)visitor.tick(now);if(bookWasActive)book.tick(now);if(autoSellWasActive)autoSell.tick(now);if(junkWasActive)junk.tick(now);
  if(!pestWasActive)pest.tick(now);if(!greenhouseWasActive)greenhouse.tick(now);if(!composterWasActive)composter.tick(now);if(!supercraftWasActive)supercraft.tick(now);if(!visitorWasActive)visitor.tick(now);if(!bookWasActive)book.tick(now);
  if(manualOwner()==null)stash.tick(now);if(!autoSellWasActive)autoSell.tick(now);if(!junkWasActive)junk.tick(now);
 }
 public void beforeLegacyTick(){session.poll(System.nanoTime());}
 /** Local settings must not replace a leased task or an inventory/cursor still owned by the server. No queued replay. */
 public boolean canOpenSettings(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.gui.overlay()!=null||session.active())return false;
  return mc.player==null||mc.player.containerMenu==mc.player.inventoryMenu&&mc.player.containerMenu.getCarried().isEmpty();
 }
 public dev.kuan.kuanperion.automation.greenhouse.HarvestReceipts harvestReceipts(){return harvests;}
 public void serverHarvestPacket(net.minecraft.network.protocol.Packet<?> packet){
  var mc=Minecraft.getInstance();if(!session.active()||!mc.isSameThread()||mc.player==null||mc.level==null||packet==null)return;
  var watched=harvests.watched();if(watched.isEmpty()||!GreenhouseGameView.applied(mc,watched.get(),packet))return;
  var ref=watched.get();long world=currentWorldId(),now=System.nanoTime();
  if(ref instanceof dev.kuan.kuanperion.automation.greenhouse.GreenhouseTarget.EntityRef entity)harvests.entityChanged(world,entity.id(),now);
  else if(ref instanceof dev.kuan.kuanperion.automation.greenhouse.GreenhouseTarget.BlockRef block)harvests.blockChanged(world,block.x(),block.y(),block.z(),now);
 }
 public CursorReceipts cursorReceipts(){return cursors;}
 public void serverCursor(net.minecraft.world.item.ItemStack server){
  var mc=Minecraft.getInstance();if(!session.active()||!mc.isSameThread()||mc.player==null||mc.level==null)return;
  var menu=mc.player.containerMenu;var item=AutomationCursorView.applied(server,menu.getCarried());
  cursors.observe(currentWorldId(),menu.containerId,item,System.nanoTime());
 }
 public void serverContainerCursor(net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket packet){
  var mc=Minecraft.getInstance();if(packet==null||mc.player==null||packet.containerId()!=mc.player.containerMenu.containerId)return;
  serverCursor(packet.carriedItem());
 }
 /** HEAD records only a matching currently owned pending release; off-thread callbacks never touch the cache. */
 public void beforeServerClose(net.minecraft.network.protocol.game.ClientboundContainerClosePacket packet){
  var mc=Minecraft.getInstance();if(!mc.isSameThread())return;beforeVisitorServerClose(packet);trapClose=null;
  if(packet==null||mc.player==null||mc.level==null)return;long world=currentWorldId();var menu=AutomationGameView.menu(mc,world);
  if(menu==null||menu.containerId()!=packet.getContainerId()||!pest.ownsMenu())return;
  trapClose=pest.beforeServerClose(packet.getContainerId(),System.nanoTime());trapCloseWorld=world;trapCloseId=packet.getContainerId();
 }
 /** TAIL accepts only an applied vanilla close, never local ESC/predicted absence or an ignored close ID. */
 public void afterServerClose(net.minecraft.network.protocol.game.ClientboundContainerClosePacket packet){
  var mc=Minecraft.getInstance();if(!mc.isSameThread())return;afterVisitorServerClose(packet);var ticket=trapClose;trapClose=null;
  if(ticket==null||packet==null||mc.player==null||mc.level==null||trapCloseWorld!=currentWorldId()||trapCloseId!=packet.getContainerId()||mc.gui.screen()!=null||mc.player.containerMenu!=mc.player.inventoryMenu)return;
  pest.afterServerClose(ticket,System.nanoTime());
 }
 private void beforeVisitorServerClose(net.minecraft.network.protocol.game.ClientboundContainerClosePacket packet){
  var mc=Minecraft.getInstance();if(!mc.isSameThread())return;visitorClose=null;
  if(packet==null||mc.player==null||mc.level==null)return;long world=currentWorldId();var menu=AutomationGameView.menu(mc,world);
  if(menu==null||menu.containerId()!=packet.getContainerId()||!visitor.ownsMenu())return;
  visitorClose=visitor.beforeServerClose(packet.getContainerId(),System.nanoTime());visitorCloseWorld=world;visitorCloseId=packet.getContainerId();
 }
 private void afterVisitorServerClose(net.minecraft.network.protocol.game.ClientboundContainerClosePacket packet){
  var mc=Minecraft.getInstance();if(!mc.isSameThread())return;var ticket=visitorClose;visitorClose=null;
  if(ticket==null||packet==null||mc.player==null||mc.level==null||visitorCloseWorld!=currentWorldId()||visitorCloseId!=packet.getContainerId()||mc.gui.screen()!=null||mc.player.containerMenu!=mc.player.inventoryMenu)return;
  visitor.afterServerClose(ticket,System.nanoTime());
 }
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
 public void invalidateFarmSession(){
  long now=System.nanoTime();AutomationStops.run(()->session.invalidate(),()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->supercraft.cancel(now),()->book.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now));
 }
 public void onServerGameMessage(String text){long now=System.nanoTime();stash.onSystemMessage(text,now);autoSell.onSystemMessage(text,now);visitor.onSystemMessage(currentWorldId(),text);composter.onSystemMessage(currentWorldId(),text);}
 /** Unlike lifecycle cancellation, Jerry also suspends configured but idle automatic jobs. */
 public void suspendForJerry(){long now=System.nanoTime();AutomationStops.run(()->pest.suspend(now),()->greenhouse.suspend(now),()->composter.suspend(now),()->visitor.suspend(now),()->supercraft.suspend(now),()->book.suspend(now),()->autoSell.suspend(now),()->junk.suspend(now),()->stash.suspend("Jerry 停止動作"));}
 public void onOutgoingCommand(){if(ownGameplayCommand){observedGameplayCommand=true;return;}
  long now=System.nanoTime();AutomationStops.run(()->stash.onOutgoingCommand(),()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->supercraft.cancel(now),()->book.cancel(now),()->session.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now));
 }
 public void onManualWorldInput(){onManualInventoryInput();}
 public void onManualInventoryInput(){
  long now=System.nanoTime();AutomationStops.run(()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->supercraft.cancel(now),()->book.cancel(now),()->session.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now));
 }
 public void shutdown(){
  long now=System.nanoTime();AutomationStops.run(()->book.cancel(now),()->supercraft.cancel(now),()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->session.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now),()->stash.cancel("關閉遊戲"),()->session.invalidate(),()->visitorFlight.shutdownData(),()->supercraftMenu.shutdownData(),()->visitorClose=null,()->trapClose=null,()->receipts.clear(),()->positions.clear(),()->cursors.clear(),()->harvests.clear());
 }
 public void worldChanged(){
  long now=System.nanoTime();AutomationStops.run(()->stash.cancel("切換世界"),()->session.invalidate(),()->pest.cancel(now),()->greenhouse.cancel(now),()->composter.cancel(now),()->visitor.cancel(now),()->supercraft.cancel(now),()->book.cancel(now),()->autoSell.cancel(now),()->junk.cancel(now),()->visitorFlight.shutdownData(),()->supercraftMenu.shutdownData(),()->visitorClose=null,()->trapClose=null,()->receipts.clear(),()->positions.clear(),()->cursors.clear(),()->harvests.clear());
 }
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
 /** Existing Jerry preference records a home only; it is not a manual movement/command takeover. */
 public boolean sendAutomaticJerryHome(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.player==null)return false;
  boolean previousOwn=ownGameplayCommand,previousObserved=observedGameplayCommand;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand("sethome");return observedGameplayCommand;}
  catch(RuntimeException failure){return false;}
  finally{ownGameplayCommand=previousOwn;observedGameplayCommand=previousObserved;}
 }
 private boolean sendPestTravel(AutomationSession.Handle handle,PestPlot plot,long now){
  if(plot==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.PEST)return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(plot.command());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /"+plot.command(),false,"Pest"));return true;}finally{ownGameplayCommand=false;}
 }
 private boolean sendGreenhouseTravel(AutomationSession.Handle handle,PestPlot plot,long now){
  if(plot==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.GREENHOUSE)return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(plot.command());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /"+plot.command(),false,"Greenhouse"));return true;}finally{ownGameplayCommand=false;}
 }
 private boolean sendComposterTravel(AutomationSession.Handle handle,PestPlot plot,long now){
  if(plot==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.COMPOSTER)return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(plot.command());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /"+plot.command(),false,"Composter"));return true;}finally{ownGameplayCommand=false;}
 }
 private boolean sendVisitorTravel(AutomationSession.Handle handle,PestPlot plot,long now){
  if(plot==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.VISITOR)return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||!context.garden()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand(plot.command());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /"+plot.command(),false,"Visitor"));return true;}finally{ownGameplayCommand=false;}
 }
 /** Typed private-plan request only; never a user-entered arbitrary command or author service. */
 private boolean sendBazaarAcquisition(AutomationSession.Handle handle,BazaarAcquisitionPlan.Request request,long now){
  if(request==null||handle==null||!session.isCurrent(handle)||!Set.of(AutomationFeature.VISITOR,AutomationFeature.PEST,AutomationFeature.COMPOSTER,AutomationFeature.SUPERCRAFT).contains(handle.lease().owner())||!request.displayName().matches("[A-Za-z0-9][A-Za-z0-9 '\\-:()+]{0,95}"))return false;
  var context=observe();var mc=Minecraft.getInstance();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||mc.gui.screen()!=null||mc.player.containerMenu!=mc.player.inventoryMenu||!mc.player.containerMenu.getCarried().isEmpty()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  boolean previousOwn=ownGameplayCommand,previousObserved=observedGameplayCommand;ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand("bz "+request.displayName());return observedGameplayCommand;}finally{ownGameplayCommand=previousOwn;observedGameplayCommand=previousObserved;}
 }
 private boolean sendSupercraftRecipe(AutomationSession.Handle handle,SupercraftPlan.Recipe recipe,long now){
  if(recipe==null||!recipe.outputId().matches("[A-Z0-9_:-]{1,100}")||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.SUPERCRAFT)return false;
  var mc=Minecraft.getInstance();var context=observe();
  if(!mc.isSameThread()||mc.player==null||mc.level==null||mc.gui.screen()!=null||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand("recipe "+recipe.outputId());if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /recipe "+recipe.outputId(),false,"Supercraft"));return true;}finally{ownGameplayCommand=false;}
 }
 /** User-confirmed /anvil only. A sent command is not proof of permission or a compatible menu. */
 private boolean sendBookAnvil(AutomationSession.Handle handle,long now){
  if(handle==null||!session.isCurrent(handle)||handle.lease().owner()!=AutomationFeature.BOOK_COMBINE)return false;
  var mc=Minecraft.getInstance();var context=AutomationSession.forFeature(observe(),AutomationFeature.BOOK_COMBINE);
  if(!mc.isSameThread()||mc.player==null||mc.level==null||mc.gui.screen()!=null||mc.gui.overlay()!=null||mc.player.containerMenu!=mc.player.inventoryMenu||!mc.player.containerMenu.getCarried().isEmpty()||!coordinator.permits(handle.lease(),AutomationCoordinator.Resource.COMMAND,context,now))return false;
  ownGameplayCommand=true;observedGameplayCommand=false;
  try{mc.player.connection.sendCommand("anvil");if(!observedGameplayCommand)return false;feedback.accept(new StashService.Notice("已發送 /anvil",false,"Book Combine"));return true;}finally{ownGameplayCommand=false;}
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
