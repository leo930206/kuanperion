// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.visitor.*;import net.minecraft.client.Minecraft;import java.util.*;
/** Read-only native projection. No interaction, packet, price lookup, profile resolution or configuration writes. */
public final class VisitorGameView {
 private VisitorGameView(){}
 public record Observation(VisitorOffer offer,Map<Integer,ItemSnapshot> inventory){public Observation{Objects.requireNonNull(offer);inventory=Map.copyOf(inventory);}}
 /** Includes pinned26.2 offhand40, body41 and saddle42, not only the36 main slots. */
 public static Map<Integer,ItemSnapshot> inventory(Minecraft mc){
  if(mc==null||!mc.isSameThread()||mc.player==null||mc.level==null)return Map.of();var inv=mc.player.getInventory();
  if(inv.getContainerSize()!=43||inv.getNonEquipmentItems().size()!=36)return Map.of();var result=new HashMap<Integer,ItemSnapshot>();
  for(int i=0;i<43;i++)result.put(i,AutomationGameView.item(inv.getItem(i),i<9||i>=36));return Map.copyOf(result);
 }
 public static Optional<Observation> offer(Minecraft mc,long world,String selectedVisitor,Map<String,String> exactMaterialIds){
  var inventory=inventory(mc);if(inventory.size()!=43||world<=0)return Optional.empty();var menu=AutomationGameView.menu(mc,world);
  return VisitorOffer.inspect(menu,selectedVisitor,exactMaterialIds).map(offer->new Observation(offer,inventory));
 }
}
