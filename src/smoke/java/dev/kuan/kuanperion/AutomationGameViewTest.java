package dev.kuan.kuanperion;
import java.util.List;
import dev.kuan.kuanperion.automation.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
public final class AutomationGameViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemStack fixture(Item item,int count){
  var components=DataComponentMap.builder().set(DataComponents.ITEM_NAME,Component.literal("Fixture")).set(DataComponents.MAX_STACK_SIZE,64).build();
  return new ItemStack(Holder.direct(item,components),count,DataComponentPatch.EMPTY);
 }
 static void testBooks(){
  var stack=fixture(Items.ENCHANTED_BOOK,1);stack.set(DataComponents.ITEM_NAME,Component.literal("Enchanted Book"));
  var attributes=new CompoundTag();attributes.putString("id","ENCHANTED_BOOK");attributes.putString("uuid","one");attributes.putString("origin","same");
  var enchants=new CompoundTag();enchants.putInt("harvesting",4);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));
  var unchanged=stack.get(DataComponents.CUSTOM_DATA).copyTag();var a=AutomationGameView.book(stack,9,false);check(a!=null&&a.enchantments().equals(java.util.Map.of("harvesting",4)),"exact native book enchantment map");check(stack.get(DataComponents.CUSTOM_DATA).copyTag().equals(unchanged),"book projection never mutates original NBT");
  attributes.putString("uuid","two");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));var b=AutomationGameView.book(stack,10,false);
  var planner=new BookCombinePlanner(java.util.Map.of("harvesting",5),java.util.Set.of());var plan=planner.select(java.util.List.of(a,b)).orElseThrow();
  check(a.compatibility().equals(b.compatibility()),"UUID difference is not discarded protection but can pair otherwise equal items");
  enchants.putInt("harvesting",5);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(planner.isExpectedResult(plan,AutomationGameView.book(stack,-1,false)),"native preview result preserves remaining data and advances exact level");
  attributes.putString("origin","changed");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(!planner.isExpectedResult(plan,AutomationGameView.book(stack,-1,false)),"native preview with metadata loss/change rejected before spending");
  enchants.putFloat("harvesting",4.5f);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(AutomationGameView.book(stack,10,false)==null,"fractional NBT levels cannot truncate into an authorized book");
  check(AutomationGameView.book(fixture(Items.ENCHANTED_BOOK,1),10,false)==null,"display/vanilla book without authoritative NBT not inferred");
  System.out.println("BookGameViewTest: PASS (native integer NBT, metadata/UUID projection, immutable original, exact preview and no name/lore fallback)");
 }
 public static void main(String[] args){
  try{var type=Class.forName("net.minecraft.client.multiplayer.ClientPacketListener");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$slot")),"server receipt Mixin actually applied");}catch(ClassNotFoundException e){throw new AssertionError(e);}
  try{var type=Class.forName("net.minecraft.client.multiplayer.MultiPlayerGameMode");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$manualInventoryInput")),"manual inventory priority Mixin applied");}catch(ClassNotFoundException e){throw new AssertionError(e);}
  var stack=fixture(Items.WHEAT,12);var extra=new CompoundTag();extra.putString("id","WHEAT");extra.putString("uuid","test-uuid");var tag=new CompoundTag();tag.put("ExtraAttributes",extra);
  stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("§aExample"))));
  var item=AutomationGameView.item(stack,false);check(item.identified()&&item.skyblockId().equals("WHEAT")&&item.uuid().equals("test-uuid")&&item.count()==12&&item.lore().equals(List.of("Example")),"actual26.2 item/custom data/lore read correctly");
  check(!item.protectedItem()&&AutomationGameView.item(stack,true).protectedItem(),"equipment/hotbar protection");
  extra.putString("other_metadata","changed");tag.put("ExtraAttributes",extra);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  check(!item.fingerprint().equals(AutomationGameView.item(stack,false).fingerprint()),"metadata changes fingerprint");
  extra.putBoolean("favorite",true);tag.put("ExtraAttributes",extra);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));check(AutomationGameView.item(stack,false).protectedItem(),"server custom favorite protected");
  var tool=fixture(Items.DIAMOND_PICKAXE,1);tool.set(DataComponents.MAX_DAMAGE,1561);check(AutomationGameView.item(tool,false).protectedItem(),"damageable tool components protected without relying on name");
  var bound=fixture(Items.PAPER,1);bound.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("§8[Co-op Soulbound]"))));check(AutomationGameView.item(bound,false).protectedItem(),"wrapped soulbound lore protected conservatively");
  check(!AutomationGameView.item(fixture(Items.WHEAT,1),false).identified(),"vanilla-name spoof without server ID cannot authorize disposal");
  var uuidArray=fixture(Items.PAPER,1);var arrayData=new CompoundTag();arrayData.putString("id","EXAMPLE");arrayData.putIntArray("uuid",new int[]{1,2,3,4});uuidArray.set(DataComponents.CUSTOM_DATA,CustomData.of(arrayData));
  check(AutomationGameView.item(uuidArray,false).uuid().equals("00000001-0000-0002-0000-000300000004"),"NBT int-array UUID preserved for protected-item matching");
  arrayData.putIntArray("uuid",new int[]{1,2});uuidArray.set(DataComponents.CUSTOM_DATA,CustomData.of(arrayData));check(AutomationGameView.item(uuidArray,false).protectedItem(),"unknown UUID representation fails closed");
  check(!AutomationGameView.item(ItemStack.EMPTY,false).identified(),"empty slot not identified");
  testBooks();
  System.out.println("AutomationGameViewTest: PASS (26.2 component fixtures/NBT/lore/protection/metadata identity; applied packet Mixin)");
 }
}
