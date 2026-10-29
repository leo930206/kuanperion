// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Supplied source selects a NONEMPTY Trap Bait button. This is not an empty storage/capacity declaration.
 * Activation wording is additional provisional local grammar. A caller still needs original-root/own-cursor/action proofs. */
public final class PestBaitButtonView {
 public static final class Role {
  private final MenuSnapshot menu;private final int slot;
  private Role(MenuSnapshot menu,int slot){this.menu=menu;this.slot=slot;}
  public int slot(){return slot;}public ItemSnapshot marker(){return menu.item(slot);}public MenuSnapshot menu(){return menu;}
  public MenuContract contract(){return new MenuContract(Set.of(menu.title()),menu.containerSlots());}
  public MenuContract.Ticket ticket(){return contract().plan(menu,slot,MenuContract.Click.PICKUP);}
 }
 public static Optional<Role> initial(MenuSnapshot menu,PestTrapTarget target,PestTrapObservation traps){
  if(target==null||traps==null||!traps.emptyKnown()||!traps.fullKnown()||!traps.empty().contains(target.trapId())||traps.full().contains(target.trapId())||!complete(menu)||!Set.of(target.family(),target.label()).contains(menu.title()))return Optional.empty();
  int found=-1;for(int i=0;i<menu.containerSlots();i++)if(menu.item(i).name().equals("Trap Bait")){if(found>=0)return Optional.empty();found=i;}
  if(found<0)return Optional.empty();var x=menu.item(found);
  if(x.count()!=1||x.protectedItem()||x.identified()||!x.uuid().isEmpty()||x.fingerprint().isBlank()||x.minecraftId().equals("minecraft:barrier")||x.lore().size()>128)return Optional.empty();
  boolean activate=false;
  for(String line:x.lore()){
   if(line.length()>512||line.toLowerCase(Locale.ROOT).matches(".*\\b(?:coins?|bits|gems|costs?|fees?|tax(?:es)?|sacks|withdraw|retrieve|buy)\\b.*"))return Optional.empty();
   activate|=Set.of("Click to insert bait!","Click to insert bait","Click to add bait!","Click to add bait").contains(line);
  }
  return activate?Optional.of(new Role(menu,found)):Optional.empty();
 }
 private static boolean complete(MenuSnapshot menu){
  if(menu==null||menu.containerId()<1||menu.containerId()>255||menu.containerSlots()<9||menu.containerSlots()>54||menu.containerSlots()%9!=0||menu.slots().size()!=menu.containerSlots()+36)return false;
  for(int i=0;i<menu.slots().size();i++){var slot=menu.slot(i);if(slot==null||slot.playerInventory()!=(i>=menu.containerSlots())||slot.item().count()>64)return false;}return true;
 }
 private PestBaitButtonView(){}
}
