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
 public static void main(String[] args){
  try{var type=Class.forName("net.minecraft.client.multiplayer.ClientPacketListener");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$slot")),"server receipt Mixin actually applied");}catch(ClassNotFoundException e){throw new AssertionError(e);}
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
  check(!AutomationGameView.item(ItemStack.EMPTY,false).identified(),"empty slot not identified");
  System.out.println("AutomationGameViewTest: PASS (26.2 component fixtures/NBT/lore/protection/metadata identity; applied packet Mixin)");
 }
}
