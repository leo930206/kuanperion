// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
/** Read-only 26.2 observations. No display-text-based authority to sell or drop an item. */
public final class AutomationGameView {
 private AutomationGameView(){}
 public record InventorySlot(int index,ItemSnapshot item){}
 public static ItemSnapshot item(ItemStack stack,boolean equipmentSlot){
  if(stack==null||stack.isEmpty())return new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
  var data=stack.get(DataComponents.CUSTOM_DATA);var tag=data==null?new CompoundTag():data.copyTag();
  var attributes=tag.contains("ExtraAttributes")?tag.getCompoundOrEmpty("ExtraAttributes"):tag;
  String id=attributes.getStringOr("id",""),uuid=attributes.getStringOr("uuid","");
  if(uuid.isBlank()){
   var parts=attributes.getIntArray("uuid").orElse(null);
   if(parts!=null&&parts.length==4)uuid=new java.util.UUID(((long)parts[0]<<32)|(parts[1]&0xffffffffL),((long)parts[2]<<32)|(parts[3]&0xffffffffL)).toString();
  }
  var loreComponent=stack.get(DataComponents.LORE);
  List<String> lore=loreComponent==null?List.of():loreComponent.lines().stream().map(c->StringUtil.stripColor(c.getString())).toList();
  boolean protectedItem=equipmentSlot||attributes.contains("uuid")&&uuid.isBlank()||stack.has(DataComponents.TOOL)||stack.has(DataComponents.MAX_DAMAGE)
   ||List.of("locked","favorite","soulbound","coop_soulbound","donated_museum").stream().anyMatch(attributes::contains)
   ||lore.stream().anyMatch(l->l.toUpperCase(Locale.ROOT).matches(".*\\b(SOULBOUND|FAVORITE|LOCKED)\\b.*")||l.contains("🔒"));
  String fingerprint=stack.getComponents().toString();
  return new ItemSnapshot(id,BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),uuid,StringUtil.stripColor(stack.getHoverName().getString()),fingerprint,stack.getCount(),lore,protectedItem);
 }
 public static List<InventorySlot> inventory(Minecraft mc){
  if(mc==null||mc.player==null)return List.of();var result=new ArrayList<InventorySlot>();
  var inv=mc.player.getInventory();for(int i=0;i<inv.getContainerSize();i++)result.add(new InventorySlot(i,item(inv.getItem(i),i<9||i>=36)));
  return List.copyOf(result);
 }
 public static MenuSnapshot menu(Minecraft mc,long world){
  if(mc==null||mc.player==null||world<=0||!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen))return null;
  var menu=screen.getMenu();if(menu!=mc.player.containerMenu)return null;
  var result=new ArrayList<MenuSnapshot.Slot>();int external=0;
  for(var slot:menu.slots){boolean playerInventory=slot.container==mc.player.getInventory();if(!playerInventory)external++;
   result.add(new MenuSnapshot.Slot(slot.index,item(slot.getItem(),playerInventory&&(slot.getContainerSlot()<9||slot.getContainerSlot()>=36)||!playerInventory&&menu==mc.player.inventoryMenu),playerInventory));}
  return new MenuSnapshot(world,menu.containerId,menu.getStateId(),StringUtil.stripColor(screen.getTitle().getString()),external,result);
 }
}
