package dev.kuan.kuanperion;
import java.util.*;
import net.minecraft.core.component.DataComponents;import net.minecraft.nbt.CompoundTag;import net.minecraft.network.chat.Component;import net.minecraft.world.item.Items;import net.minecraft.world.item.component.CustomData;import net.minecraft.world.item.component.ItemLore;
/** Real26.2 native component comparison; no client, network, account, skin resolution or item mutation in gameplay. */
public final class PestSpraySelectionGameViewTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void runFixtures(){
  var tool=AutomationGameViewTest.fixture(Items.BLAZE_ROD,1);var data=new CompoundTag();data.putString("id","SPRAYONATOR");data.putString("uuid","spray-fixture");tool.set(DataComponents.CUSTOM_DATA,CustomData.of(data));tool.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Other detail"),Component.literal("Selected Material: None"))));
  String initial=PestSpraySelectionGameView.invariant(tool);check(!initial.isEmpty(),"known source tool with unique lore");var selected=tool.copy();selected.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Other detail"),Component.literal("Selected Material: Test Material"))));check(initial.equals(PestSpraySelectionGameView.invariant(selected))&&!AutomationGameView.item(tool,true).fingerprint().equals(AutomationGameView.item(selected,true).fingerprint()),"ONLY selected lore can change while raw receipt fingerprints remain distinguishable");
  var hidden=selected.copy();var changed=data.copy();changed.putString("unknown_selection_nbt","different");hidden.set(DataComponents.CUSTOM_DATA,CustomData.of(changed));check(!initial.equals(PestSpraySelectionGameView.invariant(hidden)),"do not silently mask an unverified custom-data key");
  var other=selected.copy();other.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Changed other detail"),Component.literal("Selected Material: Test Material"))));check(!initial.equals(PestSpraySelectionGameView.invariant(other)),"all unrelated lore remains exact");
  other=selected.copy();other.set(DataComponents.CUSTOM_NAME,Component.literal("Other name"));check(!initial.equals(PestSpraySelectionGameView.invariant(other)),"names and other native components cannot drift");
  other=selected.copy();other.setCount(2);check(PestSpraySelectionGameView.invariant(other).isEmpty(),"no stacked/ambiguous source tool");
  System.out.println("NativeSpraySelectionInvariant: PASS real26.2 components; only known selected lore masked, custom-data/UUID/name/unrelated lore stay exact; not server acceptance");
 }
}
