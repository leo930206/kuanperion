// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.pets.PetSalePreflight;import java.util.*;import net.minecraft.core.component.DataComponents;import net.minecraft.nbt.*;import net.minecraft.world.item.ItemStack;
/** Read-only item/component projection. Does not access a client/world/profile service or perform a sale. */
public final class PetGameView {
 private PetGameView(){}
 public static Optional<PetSalePreflight.Pet> pet(ItemStack stack,int slot,boolean equipment){
  if(stack==null||stack.isEmpty()||slot<0||slot>42)return Optional.empty();
  try{
   var item=AutomationGameView.item(stack,equipment);if(!item.skyblockId().equals("PET")||!item.minecraftId().equals("minecraft:player_head")||item.count()!=1)return Optional.empty();var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)return Optional.empty();var raw=custom.copyTag();
   if(raw.contains("ExtraAttributes")&&!raw.keySet().equals(Set.of("ExtraAttributes")))return Optional.empty();var a=raw.contains("ExtraAttributes")?raw.getCompoundOrEmpty("ExtraAttributes"):raw;
   var allowed=Set.of("id","uuid","petInfo","timestamp","locked","favorite","soulbound","coop_soulbound","donated_museum");if(!allowed.containsAll(a.keySet())||!(a.get("petInfo") instanceof StringTag))return Optional.empty();
   if(a.contains("timestamp")){var timestamp=a.get("timestamp");if(timestamp instanceof StringTag){String value=a.getStringOr("timestamp","");if(value.length()>128||value.codePoints().anyMatch(Character::isISOControl))return Optional.empty();}else if(!(timestamp instanceof ByteTag||timestamp instanceof ShortTag||timestamp instanceof IntTag||timestamp instanceof LongTag))return Optional.empty();}
   var metadata=PetSalePreflight.metadata(a.getStringOr("petInfo",""),item.uuid());return metadata.map(m->new PetSalePreflight.Pet(slot,item,m));
  }catch(RuntimeException unknown){return Optional.empty();}
 }
}
