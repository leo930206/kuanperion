// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.regex.Pattern;
/** Read-only selected-visitor offer. A GUI button's own ID is never a required material ID. */
public final class VisitorOffer {
 public record Requirement(String name,String itemId,int quantity){
  public Requirement{if(!label(name)||itemId==null||!itemId.matches("[A-Z0-9_:-]{1,100}")||quantity<1||quantity>65536)throw new IllegalArgumentException("Invalid required item");}
 }
 private static final String POSITIVE="(?:[1-9][0-9]{0,6}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})";
 private static final Pattern QUANTITY=Pattern.compile("^(.{1,128}) x("+POSITIVE+")$");
 private static final Pattern COPPER=Pattern.compile("^\\+?(0|"+POSITIVE+") Copper$");
 private final MenuSnapshot menu;private final String visitor;private final int acceptSlot,refuseSlot,copper;
 private final List<Requirement> requirements;private final List<String> rewards;
 private VisitorOffer(MenuSnapshot menu,String visitor,int accept,int refuse,List<Requirement> needs,List<String> rewards,int copper){this.menu=menu;this.visitor=visitor;acceptSlot=accept;refuseSlot=refuse;requirements=List.copyOf(needs);this.rewards=List.copyOf(rewards);this.copper=copper;}
 public MenuSnapshot menu(){return menu;}public String visitor(){return visitor;}public int acceptSlot(){return acceptSlot;}public int refuseSlot(){return refuseSlot;}
 public List<Requirement> requirements(){return requirements;}public List<String> rewards(){return rewards;}public int copper(){return copper;}
 public static Optional<VisitorOffer> inspect(MenuSnapshot menu,String selectedVisitor,Map<String,String> exactMaterialIds){
  if(menu==null||!label(selectedVisitor)||!menu.title().equals(selectedVisitor)||menu.containerId()<=0||menu.containerSlots()<9||menu.containerSlots()>54||menu.containerSlots()%9!=0||menu.slots().size()>90||exactMaterialIds==null||exactMaterialIds.size()>8192)return Optional.empty();
  int accept=-1,refuse=-1;ItemSnapshot button=null;
  for(var slot:menu.slots()){
   if(slot.playerInventory()||slot.index()>=menu.containerSlots())continue;var item=slot.item();
   if(!item.name().equals("Accept Offer")&&!item.name().equals("Refuse Offer"))continue;
   if(item.count()!=1||!item.uuid().isEmpty()||item.fingerprint().isBlank()||item.fingerprint().length()>32768)return Optional.empty();
   if(item.name().equals("Accept Offer")){if(accept!=-1)return Optional.empty();accept=slot.index();button=item;}
   else{if(refuse!=-1)return Optional.empty();refuse=slot.index();}
  }
  if(accept<0||refuse<0||button.lore().isEmpty()||button.lore().size()>128)return Optional.empty();
  var needs=new ArrayList<Requirement>();var rewards=new ArrayList<String>();var ids=new HashSet<String>();int section=0,copper=-1;
  try{
   for(String raw:button.lore()){
    if(raw==null||raw.length()>256||raw.chars().anyMatch(Character::isISOControl))return Optional.empty();String line=raw.trim();if(line.isEmpty())continue;
    if(line.equals("Items Required:")||line.equals("Required:")){if(section!=0)return Optional.empty();section=1;continue;}
    if(line.equals("Rewards:")){if(section!=1||needs.isEmpty())return Optional.empty();section=2;continue;}
    if(section==1){
     var match=QUANTITY.matcher(line);String name=match.matches()?match.group(1):line;int count=match.matches()?Integer.parseInt(match.group(2).replace(",","")):1;
     // An unparsed quantity suffix must not become an apparent new display name.
     if(!match.matches()&&line.contains(" x"))return Optional.empty();
     String id=exactMaterialIds.get(name);var need=new Requirement(name,id,count);if(needs.size()>=8||!ids.add(need.itemId()))return Optional.empty();needs.add(need);
    }else if(section==2){
     if(rewards.size()>=64)return Optional.empty();rewards.add(line);var match=COPPER.matcher(line);
     if(match.matches()){if(copper!=-1)return Optional.empty();copper=Integer.parseInt(match.group(1).replace(",",""));if(copper>1_000_000)return Optional.empty();}
     else if(line.matches("^[+0-9.,-].*\\bCopper\\b.*")||line.equals("Copper"))return Optional.empty();
    }
   }
   return section==2&&copper>=0?Optional.of(new VisitorOffer(menu,selectedVisitor,accept,refuse,needs,rewards,copper)):Optional.empty();
  }catch(IllegalArgumentException malformed){return Optional.empty();}
 }
 private static boolean label(String s){return s!=null&&!s.isBlank()&&s.length()<=128&&s.equals(s.trim())&&!Pattern.compile("[\\p{C}§]").matcher(s).find();}
}
