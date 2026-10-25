// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.supercraft.*;import dev.kuan.kuanperion.automation.flight.FlightAnchor;import dev.kuan.kuanperion.automation.pest.PestTargets;import java.util.*;import java.util.concurrent.CompletableFuture;import java.util.function.Supplier;import java.util.regex.Pattern;import net.minecraft.client.Minecraft;import com.mojang.blaze3d.platform.InputConstants;import org.lwjgl.glfw.GLFW;
/** Fixed-owner, inventory-only adapter. Not wired to a service/UI; current live schema/quantity gates remain pending. No movement, quantity guessing, purchase, cursor repair or startup fetching. */
public final class SupercraftMenuController implements SupercraftTask.Port {
 @FunctionalInterface public interface RecipeSender{boolean send(AutomationSession.Handle handle,SupercraftPlan.Recipe recipe,long now);}
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final InventoryReceipts receipts;private final RecipeSender sender;
 private HypixelItemCatalogClient catalogClient;private CompletableFuture<?> catalogRequest;private AutomationSession.Handle dataOwner;private Object dataLevel;private InventoryReceipts.Stamp openStamp;private SupercraftPlan.Recipe activeRecipe;private String expectedTitle;private int boundContainer=-1;private boolean requested,opened,attempted,sent;private long requestedAt;
 public SupercraftMenuController(AutomationCoordinator coordinator,AutomationSession session,Supplier<AutomationContext> observe,InventoryReceipts receipts,RecipeSender sender){this.coordinator=Objects.requireNonNull(coordinator);this.session=Objects.requireNonNull(session);this.observe=Objects.requireNonNull(observe);this.receipts=Objects.requireNonNull(receipts);this.sender=Objects.requireNonNull(sender);}
 private Minecraft mc(){return Minecraft.getInstance();}public AutomationContext context(){var game=mc();return game==null||!game.isSameThread()?new AutomationContext(0,false,false,Set.of(AutomationContext.Blocker.DISCONNECTED)):observe.get();}public Map<Integer,ItemSnapshot> inventory(){return VisitorGameView.inventory(mc());}public MenuSnapshot menu(){var game=mc();return game==null||!game.isSameThread()?null:AutomationGameView.menu(game,context().worldId());}
 private boolean valid(AutomationSession.Handle h){var mc=mc();if(mc==null||!mc.isSameThread()||mc.player==null||mc.level==null||h==null||h.lease().owner()!=AutomationFeature.SUPERCRAFT||!session.isCurrent(h))return false;var c=context();return c.worldId()==h.lease().worldId()&&coordinator.isOwner(h.lease(),c,System.nanoTime());}
 private boolean owns(AutomationSession.Handle h){return valid(h)&&h.equals(dataOwner)&&dataLevel==mc().level;}
 public SupercraftTask.Frame frame(long now){
  var mc=mc();var c=context();var p=mc==null?null:mc.player;var zero=new PestTargets.Point(0,0,0);var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  if(p==null||mc.level==null||!mc.isSameThread())return new SupercraftTask.Frame(c.worldId(),now,new FlightAnchor(zero,0,0,0,empty,false),true,false,false);
  var pose=new FlightAnchor(new PestTargets.Point(p.getX(),p.getY(),p.getZ()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().flying);
  return new SupercraftTask.Frame(c.worldId(),now,pose,manualInput(),p.containerMenu.getCarried().isEmpty(),!p.isSpectator()&&!p.isPassenger()&&!p.isUsingItem()&&!p.isSleeping());
 }
 public boolean manualInput(){var mc=mc();if(mc==null||mc.options==null||mc.getWindow()==null)return true;if(mc.gui.screen()!=null)return false;
  for(var mapping:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint,mc.options.keyAttack,mc.options.keyUse)){
   if(mapping.isUnbound())continue;var key=InputConstants.getKey(mapping.saveString());long window=mc.getWindow().handle();
   if(key.getType()==InputConstants.Type.KEYSYM){if(key.getValue()<0||GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS)return true;}
   else if(key.getType()==InputConstants.Type.MOUSE){if(GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS)return true;}else return true;
  }return false;
 }
 public void requestCatalog(AutomationSession.Handle h){
  long now=System.nanoTime();if(!valid(h)||!context().canIssueCommand()||dataOwner!=null||requested&&(now-requestedAt<0||now-requestedAt<20_000_000_000L))throw new IllegalStateException("Supercraft data request not authorized");
  dataOwner=h;dataLevel=mc().level;requested=true;requestedAt=now;opened=attempted=sent=false;activeRecipe=null;expectedTitle=null;openStamp=null;boundContainer=-1;
  if(catalogClient==null)catalogClient=new HypixelItemCatalogClient();catalogRequest=catalogClient.refresh();
 }
 public Optional<Map<String,String>> catalog(){return dataOwner!=null&&owns(dataOwner)&&catalogClient!=null?catalogClient.latest().map(SkyblockItemCatalog::names):Optional.empty();}
 public boolean recipe(AutomationSession.Handle h,SupercraftPlan.Recipe recipe,long now){
  if(recipe==null||!owns(h)||opened||catalog().isEmpty()||!coordinator.permits(h.lease(),AutomationCoordinator.Resource.COMMAND,context(),now))return false;
  var matches=catalog().get().entrySet().stream().filter(e->recipe.outputId().equals(e.getValue())).map(Map.Entry::getKey).toList();if(matches.size()!=1)return false;
  activeRecipe=recipe;expectedTitle=matches.getFirst()+" Recipe";openStamp=receipts.stamp(h.lease().worldId());opened=true;return sender.send(h,recipe,now);
 }
 public GuardedMenuAction.Result craft(AutomationSession.Handle h,SupercraftPreview preview,long now){
  var mc=mc();if(!owns(h)||preview==null||!opened||attempted||mc.gameMode==null||mc.player.isSpectator()||mc.player.isPassenger()||mc.player.isUsingItem()||mc.player.isSleeping()||!mc.player.containerMenu.getCarried().isEmpty()||manualInput()||!preview.plan().recipe().equals(activeRecipe)||context().blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU))return GuardedMenuAction.Result.REJECTED;
  var current=menu();var names=catalog();if(current==null||names.isEmpty()||!current.equals(preview.menu())||!current.title().equals(expectedTitle)||!SupercraftPlan.stillMatches(preview.plan(),inventory())||SupercraftPreview.inspect(preview.plan(),current,names.get()).isEmpty())return GuardedMenuAction.Result.REJECTED;
  // Include empty slots: a local prediction must not hide a server-provided fee/unknown control.
  for(int slot=0;slot<54;slot++)if(!Objects.equals(receipts.after(openStamp,current.containerId(),slot),current.item(slot)))return GuardedMenuAction.Result.REJECTED;
  if(!freeGrid(preview,names.get(),mc.player.containerMenu.getSlot(preview.resultSlot()).getItem().getMaxStackSize()))return GuardedMenuAction.Result.REJECTED;
  if(boundContainer>=0&&boundContainer!=current.containerId())return GuardedMenuAction.Result.REJECTED;boundContainer=current.containerId();attempted=true;
  var outcome=AutomationMenuActions.purchaseClick(coordinator,h.lease(),preview.contract(),preview.ticket(),this::context,this::menu,System.nanoTime());sent=outcome==GuardedMenuAction.Result.SENT;return outcome;
 }
 private static final Pattern MONEY=Pattern.compile("(?i)(?:\\b(?:costs?|fees?|price|charges?)\\b|(?<![A-Za-z])(?:coins?|bits|motes|essence)(?![A-Za-z])|[$€£¥⏣])");
 /** Declared free-recipe shape: exact ingredient grid, selected result, neutral controls, no priced/unknown extra roles. No slot numbers are guessed. */
 static boolean freeGrid(SupercraftPreview preview,Map<String,String> ids,int outputStackLimit){
  if(preview==null||ids==null||outputStackLimit!=preview.plan().recipe().stackLimit())return false;var totals=new HashMap<String,Integer>();var menu=preview.menu();
  for(int i=0;i<menu.containerSlots();i++){
   var slot=menu.slot(i);if(slot==null||slot.playerInventory())return false;var item=slot.item();if(item.count()==0){if(!item.minecraftId().equals("minecraft:air")||!item.skyblockId().isEmpty()||!item.uuid().isEmpty())return false;continue;}
   if(item.count()>64||item.name().length()>256||item.lore().size()>128||item.lore().stream().anyMatch(l->l.length()>512)||MONEY.matcher(item.name()+" "+String.join(" ",item.lore())).find())return false;
   if(i==preview.acceptSlot()||i==preview.resultSlot())continue;if(!item.uuid().isEmpty())return false;
   boolean back=item.minecraftId().equals("minecraft:arrow")&&item.name().equals("Go Back")&&item.count()==1&&item.lore().stream().allMatch(l->l.isBlank()||Set.of("To Recipe Book","To Recipe Search","To SkyBlock Menu").contains(l));
   boolean close=item.minecraftId().equals("minecraft:barrier")&&item.name().equals("Close")&&item.count()==1&&item.lore().stream().allMatch(String::isBlank);if(back||close)continue;
   if(preview.plan().recipe().inputsPerCraft().containsKey(item.skyblockId())){if(item.protectedItem()||!item.identified()||!item.skyblockId().equals(ids.get(item.name())))return false;totals.merge(item.skyblockId(),item.count(),Integer::sum);continue;}
   boolean pane=item.minecraftId().matches("minecraft:[a-z_]+_stained_glass_pane")&&item.name().isBlank()&&item.lore().stream().allMatch(String::isBlank)&&item.skyblockId().isEmpty();
   boolean table=item.minecraftId().equals("minecraft:crafting_table")&&Set.of("Craft Item","Recipe","Crafting Table").contains(item.name())&&item.count()==1&&item.skyblockId().isEmpty();
   if(!pane&&!table)return false;
  }return totals.equals(preview.plan().recipe().inputsPerCraft());
 }
 public void closeOwnedMenu(int id){
  var h=dataOwner;if(!owns(h)||!sent||id!=boundContainer||expectedTitle==null)return;var mc=mc();var m=menu();var c=context();if(m==null||m.containerId()!=id||!m.title().equals(expectedTitle)||m.containerSlots()!=54||!mc.player.containerMenu.getCarried().isEmpty()||c.blockers().stream().anyMatch(b->b!=AutomationContext.Blocker.MENU))return;
  var blockers=EnumSet.noneOf(AutomationContext.Blocker.class);blockers.addAll(c.blockers());blockers.remove(AutomationContext.Blocker.MENU);var owned=new AutomationContext(c.worldId(),c.farmingActive(),c.garden(),blockers);
  if(coordinator.permits(h.lease(),AutomationCoordinator.Resource.SCREEN,owned,System.nanoTime()))mc.player.closeContainer();
 }
 public void dispose(AutomationSession.Handle h){
  if(h==null||!h.equals(dataOwner))return;var game=mc();if(game==null||!game.isSameThread())throw new IllegalStateException("Client thread required");dataOwner=null;dataLevel=null;activeRecipe=null;expectedTitle=null;openStamp=null;boundContainer=-1;opened=attempted=sent=false;
  if(catalogRequest!=null&&!catalogRequest.isDone()){catalogClient.close();catalogClient=null;}catalogRequest=null;
 }
 public void shutdownData(){var game=mc();if(game!=null&&!game.isSameThread())throw new IllegalStateException("Client thread required");if(catalogClient!=null)catalogClient.close();catalogClient=null;catalogRequest=null;dataOwner=null;dataLevel=null;activeRecipe=null;expectedTitle=null;openStamp=null;boundContainer=-1;opened=attempted=sent=false;}
}
