// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Non-spending supplied-Aether search10..42 / Buy Instantly10 / Custom Amount16 roles.
 * Exact title delimiters are provisional local U-BZ-BUY assumptions, never substring product matching. */
public final class BazaarNavigationView {
 public record Button(MenuContract contract,MenuContract.Ticket ticket){public int slot(){return ticket.slot();}}
 public static Optional<Button> search(MenuSnapshot m,BazaarAcquisitionPlan.Request r){
  if(!complete(m)||r==null||!normal(m.title()).contains("bazaar"))return Optional.empty();int found=-1;
  for(int i=10;i<=42&&i<m.containerSlots();i++)if(normal(m.item(i).name()).equals(normal(r.displayName()))){
   var x=m.item(i);if(!safe(x)||!x.skyblockId().isEmpty()&&!x.skyblockId().equals(r.productId())||found>=0)return Optional.empty();found=i;
  }
  return button(m,found);
 }
 public static Optional<Button> instant(MenuSnapshot m,BazaarAcquisitionPlan.Request r){return role(m,r,10,"buy instantly");}
 public static Optional<Button> custom(MenuSnapshot m,BazaarAcquisitionPlan.Request r){return role(m,r,16,"custom amount");}
 private static Optional<Button> role(MenuSnapshot m,BazaarAcquisitionPlan.Request r,int slot,String name){
  if(!complete(m)||r==null||!productTitle(m.title(),r.displayName())||slot>=m.containerSlots()||!safe(m.item(slot))||!normal(m.item(slot).name()).equals(name))return Optional.empty();
  for(int i=0;i<m.containerSlots();i++)if(i!=slot&&normal(m.item(i).name()).equals(name))return Optional.empty();return button(m,slot);
 }
 public static boolean productTitle(String title,String product){
  String t=normal(title),p=normal(product);if(t.equals(p))return true;
  for(String separator:List.of(" → "," ➜ "," ➤ "," > ")){var parts=t.split(java.util.regex.Pattern.quote(separator),-1);if(Arrays.asList(parts).contains(p)&&Arrays.stream(parts).allMatch(x->x.equals(p)||Set.of("bazaar","instant buy").contains(x)))return true;}return false;
 }
 private static boolean safe(ItemSnapshot x){return x.count()>0&&!x.minecraftId().equals("minecraft:barrier")&&!x.protectedItem()&&x.uuid().isBlank();}
 private static Optional<Button> button(MenuSnapshot m,int slot){if(slot<0)return Optional.empty();var c=new MenuContract(Set.of(m.title()),m.containerSlots());return Optional.of(new Button(c,c.plan(m,slot,MenuContract.Click.PICKUP)));}
 public static boolean complete(MenuSnapshot m){
  if(m==null||m.containerId()<1||m.containerId()>255||m.title().isBlank()||m.title().length()>256||m.containerSlots()<18||m.containerSlots()>54||m.containerSlots()%9!=0||m.slots().size()!=m.containerSlots()+36)return false;
  for(int i=0;i<m.slots().size();i++){var slot=m.slot(i);if(slot==null||slot.playerInventory()!=(i>=m.containerSlots())||slot.item().count()>64)return false;}return true;
 }
 private static String normal(String s){return s.replaceAll("(?i)§[0-9a-fk-orx]","").trim().toLowerCase(Locale.ROOT);}
 private BazaarNavigationView(){}
}
