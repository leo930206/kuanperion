// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Supplied source opens a Sprayonator GUI and selects an exact material name.
 * Exact titles/activation hints/native icon identity below are PROVISIONAL local grammar, not live acceptance. */
public final class PestSpraySelectionView {
 public static final Set<String> TITLES=Set.of("Sprayonator","Juicy Sprayonator","Salty Sprayonator");
 public static final MenuContract CONTRACT=new MenuContract(TITLES,Set.of(9,18,27,36,45,54));
 public static final class Role {
  private final MenuSnapshot menu;private final int slot;
  private Role(MenuSnapshot m,int s){menu=m;slot=s;}
  public int slot(){return slot;}public MenuSnapshot menu(){return menu;}public ItemSnapshot marker(){return menu.item(slot);}
  public MenuContract contract(){return new MenuContract(Set.of(menu.title()),menu.containerSlots());}
  public MenuContract.Ticket ticket(){return contract().plan(menu,slot,MenuContract.Click.PICKUP);}
 }
 public static Optional<Role> read(MenuSnapshot m,PestSprayPolicy p){
  if(p==null||!p.configured()||!complete(m))return Optional.empty();int found=-1;
  for(int i=0;i<m.containerSlots();i++)if(m.item(i).name().equals(p.materialName())){if(found>=0)return Optional.empty();found=i;}
  if(found<0)return Optional.empty();var x=m.item(found);
  if(x.count()!=1||x.protectedItem()||!x.uuid().isEmpty()||!x.minecraftId().equals(p.minecraftId())||!x.skyblockId().isEmpty()&&!x.skyblockId().equals(p.materialId())||x.fingerprint().isBlank()||x.lore().size()>64)return Optional.empty();
  boolean activation=false;
  for(var line:x.lore()){
   if(line.length()>512||line.codePoints().anyMatch(c->Character.isISOControl(c)||Character.getType(c)==Character.CURRENCY_SYMBOL)||line.toLowerCase(Locale.ROOT).matches(".*\\b(?:coins?|bits|gems?|costs?|prices?|payments?|pay|charges?|spend|consum\\w*|fees?|tax(?:es)?|sacks|withdraw|retrieve|buy|purchas\\w*|selected)\\b.*"))return Optional.empty();
   activation|=Set.of("Click to select!","Click to select").contains(line);
  }
  return activation?Optional.of(new Role(m,found)):Optional.empty();
 }
 public static boolean complete(MenuSnapshot m){if(m==null||m.containerId()<1||m.containerId()>255||!CONTRACT.matches(m)||m.slots().size()!=m.containerSlots()+36)return false;for(int i=0;i<m.slots().size();i++){var s=m.slot(i);if(s==null||s.playerInventory()!=(i>=m.containerSlots())||s.item().count()>64)return false;}return true;}
 private PestSpraySelectionView(){}
}
