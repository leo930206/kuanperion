// SPDX-License-Identifier: GPL-3.0-only
// Role concept from supplied PestExchangeManager; exact local grammar remains provisional pending batch acceptance.
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Source-marked nonempty role only; no command, inventory or completion authority. */
public final class PestExchangeView {
 public enum Status {READY,ALREADY_EXCHANGED}
 public static final Set<String> TITLES=Set.of("Pesthunter","Phillip","Pesthunter Phillip");
 public static final class Role {
  private final MenuSnapshot menu;private final int slot;private final Status status;
  private Role(MenuSnapshot m,int i,Status s){menu=m;slot=i;status=s;}
  public MenuSnapshot menu(){return menu;}public int slot(){return slot;}public Status status(){return status;}public ItemSnapshot marker(){return menu.item(slot);}
  public MenuContract contract(){return new MenuContract(Set.of(menu.title()),menu.containerSlots());}
  public MenuContract.Ticket ticket(){return status==Status.READY?contract().plan(menu,slot,MenuContract.Click.PICKUP):null;}
 }
 private PestExchangeView(){}
 public static Optional<Role> read(MenuSnapshot menu){
  if(menu==null||menu.worldId()<1||menu.containerId()<1||menu.containerId()>255||!TITLES.contains(menu.title())||menu.containerSlots()<9||menu.containerSlots()>54||menu.containerSlots()%9!=0||menu.slots().size()!=menu.containerSlots()+36)return Optional.empty();
  int found=-1;
  for(int i=0;i<menu.slots().size();i++){
   var slot=menu.slot(i);if(slot==null||slot.playerInventory()!=(i>=menu.containerSlots())||slot.item().count()>64)return Optional.empty();
   if(i<menu.containerSlots()&&Set.of("Empty Vacuum Bag","Vacuum Bag").contains(slot.item().name())){if(found>=0)return Optional.empty();found=i;}
  }
  if(found<0)return Optional.empty();var item=menu.item(found);
  if(item.count()!=1||item.protectedItem()||item.identified()||!item.uuid().isEmpty()||item.minecraftId().equals("minecraft:air")||item.fingerprint().isBlank()||item.lore().size()>128)return Optional.empty();
  int ready=0,already=0;
  for(String line:item.lore()){
   if(line.length()>512||line.codePoints().anyMatch(Character::isISOControl)||line.toLowerCase(Locale.ROOT).matches(".*\\b(?:coins?|bits|gems?|costs?|fees?|tax(?:es)?|buy|purchase|pay|sacks|withdraw)\\b.*"))return Optional.empty();
   if(Set.of("Click to empty your Vacuum Bag!","Click to empty your Vacuum Bag","Click to empty!").contains(line))ready++;
   else if(line.startsWith("Click to "))return Optional.empty();
   if(line.contains("exchanged enough Pests"))already++;
  }
  if(ready==1&&already==0&&!item.minecraftId().equals("minecraft:barrier"))return Optional.of(new Role(menu,found,Status.READY));
  return ready==0&&already==1?Optional.of(new Role(menu,found,Status.ALREADY_EXCHANGED)):Optional.empty();
 }
}
