// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.pest.PestSprayTool;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
/** Native identity invariant. Only the unique source-known Selected Material LORE line is masked.
 * CUSTOM_DATA, UUID, names, flags and every other component/lore line stay exact. Unknown NBT changes fail closed. */
final class PestSpraySelectionGameView {
 static String invariant(ItemStack stack){
  if(stack==null||PestSprayTool.read(0,AutomationGameView.item(stack,true)).isEmpty())return "";
  var result=new TreeMap<String,String>();result.put("native-item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());result.put("count",Integer.toString(stack.getCount()));
  for(var component:stack.getComponents()){
   var type=component.type();var key=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);if(key==null)return "";
   if(type==DataComponents.LORE){var lore=stack.get(DataComponents.LORE);var lines=new ArrayList<String>();for(var line:lore.lines())lines.add(PestSprayTool.isSelectionLine(line.getString())?"<selected-material>":line.toString());result.put(key.toString(),lines.toString());}
   else result.put(key.toString(),component.value().toString());
  }
  String value=result.toString();return value.length()<=65536?value:"";
 }
 private PestSpraySelectionGameView(){}
}
