// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pets.*;import dev.kuan.kuanperion.automation.flight.FlightAnchor;import dev.kuan.kuanperion.automation.pest.PestTargets;
import java.util.*;import java.util.function.*;import net.minecraft.client.Minecraft;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
/** Native George boundary. Only an original PET_SELL root, exact selected PET or already-authorized GUI role may be clicked. */
public final class PetGeorgeController implements PetGeorgeTask.Port {
 @FunctionalInterface public interface Opener{boolean open(AutomationSession.Handle h,long now);}
 private final AutomationCoordinator coordinator;private final AutomationSession session;private final Supplier<AutomationContext> observe;private final Opener opener;private AutomationSession.Handle owner;private Object level;
 public PetGeorgeController(AutomationCoordinator c,AutomationSession s,Supplier<AutomationContext> observe,Opener opener){coordinator=c;session=s;this.observe=observe;this.opener=opener;}
 private Minecraft mc(){return Minecraft.getInstance();}
 public AutomationContext context(){return observe.get();}
 public boolean available(){var m=mc();return m!=null&&m.isSameThread()&&m.player!=null&&m.level!=null&&m.gameMode!=null&&m.gui.overlay()==null&&m.player.isAlive()&&!m.player.isSpectator()&&!m.player.isPassenger()&&!m.player.isSleeping()&&!m.player.isUsingItem()&&!manualInput()&&(m.gui.screen()==null||m.gui.screen() instanceof AbstractContainerScreen<?> screen&&screen.getMenu()==m.player.containerMenu);}
 private boolean manualInput(){
  var m=mc();if(m.gui.screen()!=null)return false;if(m.options==null||m.getWindow()==null)return true;
  for(var mapping:List.of(m.options.keyUp,m.options.keyDown,m.options.keyLeft,m.options.keyRight,m.options.keyJump,m.options.keyShift,m.options.keySprint,m.options.keyAttack,m.options.keyUse)){
   if(mapping.isUnbound())continue;var key=com.mojang.blaze3d.platform.InputConstants.getKey(mapping.saveString());long window=m.getWindow().handle();
   if(key.getType()==com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM){if(key.getValue()<0||org.lwjgl.glfw.GLFW.glfwGetKey(window,key.getValue())==org.lwjgl.glfw.GLFW.GLFW_PRESS)return true;}
   else if(key.getType()==com.mojang.blaze3d.platform.InputConstants.Type.MOUSE){if(org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,key.getValue())==org.lwjgl.glfw.GLFW.GLFW_PRESS)return true;}else return true;
  }return false;
 }
 public Object level(){return mc().level;}public Object menuIdentity(){return mc().player==null?null:mc().player.containerMenu;}
 public Object pose(){var p=mc().player;return p==null?null:new FlightAnchor(new PestTargets.Point(p.getX(),p.getY(),p.getZ()),p.getYRot(),p.getXRot(),p.getInventory().getSelectedSlot(),AutomationGameView.item(p.getMainHandItem(),true),p.getAbilities().flying);}
 public boolean idle(){return available()&&mc().gui.screen()==null&&mc().player.containerMenu==mc().player.inventoryMenu&&mc().player.containerMenu.getCarried().isEmpty();}
 public PetSaleAttempt.Frame frame(long now){var m=mc();return new PetSaleAttempt.Frame(AutomationGameView.menu(m,context().worldId()),VisitorGameView.inventory(m),available()&&m.player.containerMenu.getCarried().isEmpty(),now);}
 public List<PetSalePreflight.Pet> pets(){
  if(!available()||mc().player.getInventory().getContainerSize()!=43)return List.of();var pets=new ArrayList<PetSalePreflight.Pet>();var inv=mc().player.getInventory();for(int i=0;i<43;i++)PetGameView.pet(inv.getItem(i),i,i<9||i>=36).ifPresent(pets::add);return List.copyOf(pets);
 }
 public boolean open(AutomationSession.Handle h,long now){
  if(h==null||h.lease().owner()!=AutomationFeature.PET_SELL||!session.isCurrent(h)||!idle()||!coordinator.permits(h.lease(),AutomationCoordinator.Resource.COMMAND,AutomationSession.forFeature(context(),AutomationFeature.PET_SELL),now))return false;owner=h;level=mc().level;return opener.open(h,now);
 }
 private boolean owns(AutomationSession.Handle h){return available()&&h!=null&&h.equals(owner)&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&h.lease().owner()==AutomationFeature.PET_SELL&&mc().level==level;}
 static boolean exactPet(PetSalePreflight.Pet expected,net.minecraft.world.item.ItemStack raw){
  if(expected==null)return false;
  var projected=PetGameView.pet(raw,expected.slot(),false).orElse(null);return projected!=null&&!projected.item().protectedItem()&&!projected.metadata().protectedMetadata()&&projected.item().equals(expected.item())&&projected.metadata().uuid().equals(expected.metadata().uuid())&&projected.metadata().kind().equals(expected.metadata().kind())&&projected.metadata().experience().compareTo(expected.metadata().experience())==0;
 }
 public GuardedMenuAction.Result click(AutomationSession.Handle h,PetSalePreflight.Pet pet,MenuContract contract,MenuContract.Ticket ticket,Supplier<MenuSnapshot> proof,long now){
  if(!owns(h)||pet==null||pet.item().protectedItem()||pet.metadata().protectedMetadata()||ticket==null||proof==null||ticket.click()==MenuContract.Click.THROW||!mc().player.containerMenu.getCarried().isEmpty())return GuardedMenuAction.Result.REJECTED;
  var nativeMenu=mc().player.containerMenu;var current=AutomationGameView.menu(mc(),h.lease().worldId());if(!Objects.equals(current,ticket.observed())||!contract.revalidate(ticket,current)||ticket.slot()<0||ticket.slot()>=nativeMenu.slots.size())return GuardedMenuAction.Result.REJECTED;var slot=current.slot(ticket.slot());
  if(slot.playerInventory()){
   int expected=current.containerSlots()+pet.slot()-9;if(pet.slot()<9||pet.slot()>=36||ticket.slot()!=expected||ticket.click()!=MenuContract.Click.QUICK_MOVE||!exactPet(pet,nativeMenu.slots.get(ticket.slot()).getItem()))return GuardedMenuAction.Result.REJECTED;
  }else if(ticket.slot()==13){
   if(ticket.click()!=MenuContract.Click.QUICK_MOVE||!exactPet(pet,nativeMenu.slots.get(13).getItem()))return GuardedMenuAction.Result.REJECTED;
  }else{
   var item=slot.item();if(ticket.click()!=MenuContract.Click.PICKUP||item.count()!=1||item.protectedItem()||item.identified()||!item.uuid().isEmpty()||!Set.of("Sell Pet","Accept","Confirm","Offer","Yes").contains(item.name()))return GuardedMenuAction.Result.REJECTED;
  }
  return GuardedMenuAction.perform(coordinator,h.lease(),contract,ticket,GuardedMenuAction.Space.EXISTING_ITEMS_ONLY,this::context,proof,t->owns(h)&&mc().player.containerMenu==nativeMenu&&AutomationMenuActions.sendNative(t),now);
 }
 public void dispose(AutomationSession.Handle h){if(h!=null&&h.equals(owner)){owner=null;level=null;}}
 public boolean close(Object identity,int container){
  if(!owns(owner)||identity==null||mc().player.containerMenu!=identity||mc().player.containerMenu.containerId!=container||!mc().player.containerMenu.getCarried().isEmpty())return false;mc().player.closeContainer();return mc().gui.screen()==null&&mc().player.containerMenu==mc().player.inventoryMenu;
 }
}
