// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.regex.Pattern;
/** Strict read-only advanced-preview facts based on the historical2024 official screenshot grammar. Current live format still requires validation; this class cannot perform a craft. */
public final class SupercraftPreview {
 private static final String QUANTITY="(?:0|[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";
 private static final Pattern ROLE=Pattern.compile("^Supercraft(?: \\(([1-9][0-9]{0,3})x crafts\\))?$"),NEED=Pattern.compile("^[✔✓] ("+QUANTITY+")/("+QUANTITY+")(?: \\(([1-9][0-9]{0,3})x\\))? (.+)$"),SOURCE=Pattern.compile("^("+QUANTITY+")x from Inventory$"),OUTPUT=Pattern.compile("^Crafting ([1-9][0-9]{0,3}) (item|items) into your inventory!$");
 private final SupercraftPlan.Plan plan;private final MenuSnapshot menu;private final int acceptSlot,resultSlot;
 private SupercraftPreview(SupercraftPlan.Plan plan,MenuSnapshot menu,int acceptSlot,int resultSlot){this.plan=plan;this.menu=menu;this.acceptSlot=acceptSlot;this.resultSlot=resultSlot;}
 public SupercraftPlan.Plan plan(){return plan;}public MenuSnapshot menu(){return menu;}public int acceptSlot(){return acceptSlot;}public int resultSlot(){return resultSlot;}
 public MenuContract contract(){return new MenuContract(Set.of(menu.title()),54);}public MenuContract.Ticket ticket(){return contract().plan(menu,acceptSlot,MenuContract.Click.PICKUP);}
 public static Optional<SupercraftPreview> inspect(SupercraftPlan.Plan plan,MenuSnapshot menu,Map<String,String> materialIds){
  if(plan==null||menu==null||materialIds==null||menu.worldId()<=0||menu.containerId()<1||menu.containerId()>255||menu.containerSlots()!=54)return Optional.empty();
  try{
   int accept=-1,result=-1;ItemSnapshot button=null,output=null;
   for(int i=0;i<54;i++){
    var slot=menu.slot(i);if(slot==null||slot.playerInventory())return Optional.empty();var item=slot.item();if(item.count()==0)continue;
    String name=clean(item.name());if(name.startsWith("Supercraft")){
     var role=ROLE.matcher(name);if(accept>=0||!role.matches()||item.count()!=1)return Optional.empty();int crafts=role.group(1)==null?1:Integer.parseInt(role.group(1));if(crafts!=plan.crafts())return Optional.empty();accept=i;button=item;
    }
    if(item.skyblockId().equals(plan.recipe().outputId())){if(result>=0||!item.identified()||item.count()>64||item.protectedItem()||!item.uuid().isEmpty()||!item.skyblockId().equals(materialIds.get(name)))return Optional.empty();result=i;output=item;}
   }
   if(accept<0||result<0||accept==result||!menu.title().equals(clean(output.name())+" Recipe")||!lore(plan,button.lore(),materialIds))return Optional.empty();
   for(int i=0;i<36;i++){var slot=menu.slot(54+i);if(slot==null||!slot.playerInventory()||!slot.item().equals(plan.baseline().get(i<27?i+9:i-27)))return Optional.empty();}
   return Optional.of(new SupercraftPreview(plan,menu,accept,result));
  }catch(IllegalArgumentException malformed){return Optional.empty();}
 }
 private static boolean lore(SupercraftPlan.Plan plan,List<String> raw,Map<String,String> ids){
  if(raw.size()>128)return false;var requirements=new HashMap<String,Integer>();String pending=null;int pendingAmount=0;boolean output=false,right=false,click=false;
  for(String rawLine:raw){
   String line=clean(rawLine);if(line.isEmpty())continue;if(click)return false;
   if(output){
    if(line.equals("Right-Click to set amount!")&&!right){right=true;continue;}
    if(line.equals("Click to craft!")&&right){click=true;continue;}return false;
   }
   var need=NEED.matcher(line);if(need.matches()){
    if(pending!=null)return false;long available=number(need.group(1)),required=number(need.group(2));String id=ids.get(need.group(4));
    if(required<1||required>65536||available<required||need.group(3)!=null&&Integer.parseInt(need.group(3))>4096||id==null||!plan.inputs().containsKey(id)||plan.inputs().get(id)!=required||requirements.putIfAbsent(id,(int)required)!=null)return false;
    pending=id;pendingAmount=(int)required;continue;
   }
   var source=SOURCE.matcher(line);if(source.matches()){if(pending==null||number(source.group(1))!=pendingAmount)return false;pending=null;continue;}
   var produced=OUTPUT.matcher(line);if(produced.matches()){
    int amount=Integer.parseInt(produced.group(1));if(pending!=null||amount!=plan.outputCount()||!produced.group(2).equals(amount==1?"item":"items")||!requirements.equals(plan.inputs()))return false;output=true;continue;
   }
   return false; // Unknown cost/source/confirmation lines never get silently skipped.
  }
  return output&&right&&click&&pending==null&&requirements.equals(plan.inputs());
 }
 private static long number(String text){long n=Long.parseLong(text.replace(",",""));if(n>1_000_000_000L)throw new IllegalArgumentException("Oversized preview count");return n;}
 private static String clean(String text){if(text==null||text.length()>512)throw new IllegalArgumentException("Invalid preview line");String value=text.replaceAll("(?i)§[0-9a-fk-orx]","").strip();if(Pattern.compile("[\\p{C}§]").matcher(value).find())throw new IllegalArgumentException("Invalid preview control");return value;}
}
