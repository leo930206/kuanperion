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
 /** Strict authoritative book preflight; no name/lore parsing to invent enchantments. */
 public static BookCombinePlanner.Book book(ItemStack stack,int inventorySlot,boolean equipment){
  var item=item(stack,equipment);if(!item.identified()||!item.skyblockId().equals("ENCHANTED_BOOK")||!item.minecraftId().equals("minecraft:enchanted_book"))return null;
  var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)return null;
  var raw=custom.copyTag();var attributes=raw.contains("ExtraAttributes")?raw.getCompoundOrEmpty("ExtraAttributes"):raw;
  var enchants=attributes.getCompoundOrEmpty("enchantments");if(enchants.isEmpty()||enchants.size()>32)return null;
  var levels=new TreeMap<String,Integer>();
  for(String key:enchants.keySet()){
   var tag=enchants.get(key);
   if(!(tag instanceof net.minecraft.nbt.IntTag||tag instanceof net.minecraft.nbt.ShortTag||tag instanceof net.minecraft.nbt.ByteTag||tag instanceof net.minecraft.nbt.LongTag))return null;
   long value=enchants.getLongOr(key,0);if(value<1||value>10)return null;levels.put(key,(int)value);
  }
  // Work only on copies. UUID protection remains on ItemSnapshot; never remove arbitrary attributes.
  attributes.remove("uuid");attributes.remove("enchantments");if(raw.contains("ExtraAttributes"))raw.put("ExtraAttributes",attributes);
  var metadata=new TreeMap<String,String>();metadata.put("skyblock-data",raw.toString());
  for(var component:stack.getComponents()){
   var type=component.type();
   // Server-generated book name/lore can change with the enchantment level. Custom names are excluded by the planner.
   if(type==DataComponents.CUSTOM_DATA||type==DataComponents.LORE||type==DataComponents.CUSTOM_NAME||type==DataComponents.ITEM_NAME)continue;
   var key=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);if(key==null)return null;metadata.put(key.toString(),component.value().toString());
  }
  try{return new BookCombinePlanner.Book(inventorySlot,item,levels,metadata.toString());}catch(IllegalArgumentException malformed){return null;}
 }
 public static List<InventorySlot> inventory(Minecraft mc){
  if(mc==null||mc.player==null)return List.of();var result=new ArrayList<InventorySlot>();
  var inv=mc.player.getInventory();for(int i=0;i<inv.getContainerSize();i++)result.add(new InventorySlot(i,item(inv.getItem(i),i<9||i>=36)));
  return List.copyOf(result);
 }
 /** Only the player's vanilla inventory, including protected offhand; never a server lookalike. */
 public static MenuSnapshot junkMenu(Minecraft mc,long world){
  if(mc==null||mc.player==null||!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)||mc.player.containerMenu!=mc.player.inventoryMenu)return null;
  var menu=menu(mc,world);return menu!=null&&menu.containerId()==0&&menu.containerSlots()==9&&menu.slots().size()==46?menu:null;
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
