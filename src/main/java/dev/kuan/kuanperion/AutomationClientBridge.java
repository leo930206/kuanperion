// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;

import dev.kuan.kuanperion.automation.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Thin 26.2 adapter. New tasks cannot write legacy movement/toggles or use arbitrary commands. */
public final class AutomationClientBridge {
 public record LegacyState(boolean farmingArmed,boolean mining,boolean recovering,boolean selecting,String island){}
 private final Supplier<LegacyState> legacy;private final Function<Minecraft,List<String>> sidebar;
 private final InventoryReceipts receipts=new InventoryReceipts();
 private final AutomationCoordinator coordinator=new AutomationCoordinator();private final AutomationSession session;
 private final StashService stash;private Object levelIdentity;private long worldGeneration;
 public AutomationClientBridge(Path configDir,Supplier<LegacyState> legacy,Function<Minecraft,List<String>> sidebar,Consumer<StashService.Notice> feedback,Runnable pauseFarm,Runnable safeStopFarm){
  this.legacy=legacy;this.sidebar=sidebar;
  stash=new StashService(AutomationSettingsStore.load(configDir.resolve("kuanperion/automation.json")),coordinator,this::observe,this::send,feedback);
  session=new AutomationSession(coordinator,this::observe,pauseFarm,safeStopFarm);
 }
 public StashService stash(){return stash;}
 public void tick(){stash.tick(System.nanoTime());}
 public void beforeLegacyTick(){session.poll(System.nanoTime());}
 public InventoryReceipts receipts(){return receipts;}
 public void serverSlot(int container,int slot,net.minecraft.world.item.ItemStack stack){if(!session.active())return;receipts.observe(currentWorldId(),container,slot,AutomationGameView.item(stack,false));}
 public AutomationSession session(){return session;}
 public AutomationCoordinator coordinator(){return coordinator;}
 public boolean holdsFarming(){return session.holdsFarming();}
 public void invalidateFarmSession(){session.invalidate();}
 public void onServerGameMessage(String text){stash.onSystemMessage(text,System.nanoTime());}
 public void onOutgoingCommand(){stash.onOutgoingCommand();session.cancel(System.nanoTime());}
 public void worldChanged(){stash.cancel("切換世界");session.invalidate();receipts.clear();}
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
  List<String> lines=flags.island()==null&&mc.level!=null&&mc.player!=null?sidebar.apply(mc):List.of();
  String address=mc.getCurrentServer()==null?"":mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
  boolean official=address.matches("(?:[a-z0-9-]+\\.)*hypixel\\.net(?::[0-9]+)?");
  boolean skyblock=official&&(flags.island()!=null||lines.stream().anyMatch(line->line.toUpperCase(Locale.ROOT).contains("SKYBLOCK")));
  if(!skyblock)blockers.add(AutomationContext.Blocker.OUTSIDE_SKYBLOCK);
  boolean garden="GARDEN".equals(flags.island())||flags.island()==null&&lines.stream().anyMatch(line->line.toLowerCase(Locale.ROOT).contains("garden")||line.toLowerCase(Locale.ROOT).contains("plot"));
  return new AutomationContext(worldGeneration,flags.farmingArmed(),garden,blockers);
 }
 private boolean send(AutomationCommand command){
  if(!observe().canIssueCommand())return false;
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return false;
  mc.player.connection.sendCommand(command.wire());return true;
 }
}
