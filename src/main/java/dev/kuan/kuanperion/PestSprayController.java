// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;
import java.util.*;import java.util.concurrent.CompletableFuture;import java.util.function.Supplier;
import com.mojang.blaze3d.platform.InputConstants;import net.minecraft.client.KeyMapping;import net.minecraft.client.Minecraft;import net.minecraft.world.InteractionHand;import org.lwjgl.glfw.GLFW;
/** Fixed in-place item-use adapter. No flight core, world-input exemption, physical key writes or async gameplay. */
public final class PestSprayController implements PestSprayTask.Port {
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final Supplier<PestSprayPolicy> policy;private final PlayerPositionReceipts positions;
 private HypixelItemCatalogClient catalog;private CompletableFuture<?> request;private AutomationSession.Handle dataOwner;private Object dataLevel,dataPlayer;private String materialName="";private boolean effectInProgress;
 public PestSprayController(AutomationCoordinator c,AutomationSession s,PlayerPositionReceipts positions,Supplier<AutomationContext> observe,Supplier<PestSprayPolicy> policy){coordinator=c;session=s;this.positions=positions;this.observe=observe;this.policy=policy;}
 public AutomationContext context(){return observe.get();}
 public PestSprayTask.Frame sprayFrame(long now){if(!Minecraft.getInstance().isSameThread())return null;var c=context();return nativeFrame(c.worldId(),now,dataOwner==null?policy.get().materialName():materialName);}
 private PestSprayTask.Frame nativeFrame(long world,long now,String material){
  var mc=Minecraft.getInstance();var p=mc.player;if(!mc.isSameThread()||p==null||mc.level==null||mc.getWindow()==null)return null;
  var inventory=new HashMap<Integer,ItemSnapshot>();for(var slot:AutomationGameView.inventory(mc))if(inventory.put(slot.index(),slot.item())!=null)return null;
  var velocity=p.getDeltaMovement();var positionStamp=positions.stamp(world,now);
  boolean idle=mc.gui.screen()==null&&mc.gui.overlay()==null&&p.containerMenu==p.inventoryMenu&&p.containerMenu.containerId==0&&p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isSleeping()&&GLFW.glfwGetWindowAttrib(mc.getWindow().handle(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_TRUE;
  return new PestSprayTask.Frame(world,now,mc.level,p,inventory,AutomationGameView.item(p.containerMenu.getCarried(),true),p.getInventory().getSelectedSlot(),idle,p.isUsingItem(),p.getX(),p.getY(),p.getZ(),velocity.x,velocity.y,velocity.z,p.getYRot(),p.getXRot(),PestSprayObservation.parse(GardenTabReader.read(mc),material),positionStamp.epoch(),positionStamp.sequence());
 }
 public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action action,long now){
  var mc=Minecraft.getInstance();var level=mc.level;var player=mc.player;var gameMode=mc.gameMode;
  if(!mc.isSameThread()||player==null||level==null||mc.gameMode==null||effectInProgress||h==null||action==null||!h.equals(action.owner())||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!h.equals(dataOwner)||level!=dataLevel||player!=dataPlayer)return PestSprayTask.Effect.REJECTED;
  boolean effectStarted=false;effectInProgress=true;
  try{
   var c=context();var resource=action.kind()==PestSprayTask.Kind.USE?AutomationCoordinator.Resource.USE:AutomationCoordinator.Resource.HOTBAR;
   if(!session.isCurrent(h)||!c.canIssueCommand()||!c.garden()||!c.farmingActive()||manualInput()||!coordinator.permits(h.lease(),resource,c,now))return PestSprayTask.Effect.REJECTED;
   var before=nativeFrame(h.lease().worldId(),now,materialName);if(!action.claim(h,before,now))return PestSprayTask.Effect.REJECTED;
   // claim may read a fallible context. Re-sample physical identities/pose/all43/cursor/Tab AFTER it.
   boolean manual=manualInput();var physical=nativeFrame(h.lease().worldId(),now,materialName);
   if(!mc.isSameThread()||mc.level!=level||mc.player!=player||mc.gameMode!=gameMode||!session.isCurrent(h)||!h.equals(dataOwner)||!coordinator.permits(h.lease(),resource,c,now)||manual||!action.matchesSnapshot(physical))return PestSprayTask.Effect.REJECTED;
   effectStarted=true;
   if(action.kind()==PestSprayTask.Kind.USE)gameMode.useItem(player,InteractionHand.MAIN_HAND);
   else player.getInventory().setSelectedSlot(action.targetSlot());
   return PestSprayTask.Effect.SENT;
  }catch(RuntimeException unavailable){return effectStarted?PestSprayTask.Effect.UNCERTAIN:PestSprayTask.Effect.REJECTED;}
  finally{effectInProgress=false;}
 }
 public boolean manualInput(){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.options==null||mc.getWindow()==null)return true;
  for(var key:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint,mc.options.keyAttack,mc.options.keyUse))if(physical(key))return true;
  for(var key:mc.options.keyHotbarSlots)if(physical(key))return true;return false;
 }
 private boolean physical(KeyMapping mapping){
  if(mapping==null)return true;if(mapping.isUnbound())return false;var key=InputConstants.getKey(mapping.saveString());long window=Minecraft.getInstance().getWindow().handle();
  if(key.getType()==InputConstants.Type.KEYSYM)return key.getValue()<0||GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS;
  if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;return true;
 }
 public void requestSprayData(AutomationSession.Handle h){
  var mc=Minecraft.getInstance();var level=mc.level;var player=mc.player;if(!mc.isSameThread())throw new IllegalStateException("Client thread required");var c=context();var p=policy.get();
  if(!mc.isSameThread()||h==null||h.lease().owner()!=AutomationFeature.PEST_SPRAY||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||!c.canIssueCommand()||!c.garden()||!c.farmingActive()||h.lease().worldId()!=c.worldId()||level==null||player==null||mc.level!=level||mc.player!=player||manualInput()||dataOwner!=null||!p.configured())throw new IllegalStateException("Spray catalog demand not authorized");
  dataOwner=h;dataLevel=level;dataPlayer=player;materialName=p.materialName();if(catalog==null)catalog=new HypixelItemCatalogClient();request=catalog.refresh();
 }
 public Optional<Map<String,String>> sprayCatalog(AutomationSession.Handle h){
  var mc=Minecraft.getInstance();if(!mc.isSameThread()||h==null||!h.equals(dataOwner)||!session.isCurrent(h)||mc.level!=dataLevel||mc.player!=dataPlayer||catalog==null)return Optional.empty();return catalog.latest().map(SkyblockItemCatalog::names);
 }
 public void disposeSprayData(AutomationSession.Handle h){
  if(h==null||!h.equals(dataOwner))return;dataOwner=null;dataLevel=dataPlayer=null;materialName="";
  var pending=request!=null&&!request.isDone()?catalog:null;request=null;if(pending!=null){catalog=null;pending.close();}
 }
 public void shutdownData(){var old=catalog;catalog=null;request=null;dataOwner=null;dataLevel=dataPlayer=null;materialName="";if(old!=null)old.close();}
}
