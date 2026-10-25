// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.ItemSnapshot;
import net.minecraft.world.item.ItemStack;
/** Called only from a server packet TAIL: local prediction by itself cannot manufacture a receipt. */
public final class AutomationCursorView {
 private AutomationCursorView(){}
 public static ItemSnapshot applied(ItemStack server,ItemStack carried){
  if(server==null||carried==null)return null;
  try{var fromServer=AutomationGameView.item(server,false);var current=AutomationGameView.item(carried,false);return fromServer.equals(current)?fromServer:null;}
  catch(RuntimeException malformed){return null;}
 }
}
