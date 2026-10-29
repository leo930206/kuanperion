// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.math.BigDecimal;import java.util.*;import java.util.regex.Pattern;
/** Provisional supplied-Aether BazaarBuySession/BazaarUtils roles (U-BZ-BUY/PRICE), not live compatibility. */
public final class BazaarPurchaseView {
 private static final String NUMBER="(?:[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";
 private static final Pattern BUY=Pattern.compile("(?:buy )?("+NUMBER+")x (.{1,96})",Pattern.CASE_INSENSITIVE),AMOUNT=Pattern.compile("amount: ("+NUMBER+")x",Pattern.CASE_INSENSITIVE);
 // Additional explicit provisional adapter assumption: Price is the total, not Per unit or public/API estimate.
 private static final Pattern PRICE=Pattern.compile("price: ("+NUMBER+"(?:\\.[0-9]{1,2})?) coins",Pattern.CASE_INSENSITIVE),EXTRA=Pattern.compile("(?i)\\b(?:costs?|fees?|tax(?:es)?|bits|gems|motes|essence)\\b");
 public record Quote(MenuContract contract,MenuContract.Ticket ticket,BigDecimal total){}
 public static Optional<Quote> inspect(MenuSnapshot menu,BazaarAcquisitionPlan.Request request){
  if(request==null||!complete(menu))return Optional.empty();String title=normal(menu.title());
  // Never inherit upstream's permissive buy-anyway override or timer-only warning clearance.
  if(title.contains("warning")||title.contains("alert"))return Optional.empty();
  for(int i=0;i<menu.containerSlots();i++){var item=menu.item(i);if(item.minecraftId().equals("minecraft:barrier")&&(normal(item.name()).matches(".*(?:warning|alert|wait|second).*")))return Optional.empty();}
  boolean dialog=title.contains("confirm instant buy")||title.contains("confirm purchase");
  boolean quantity=menu.containerSlots()>16&&normal(menu.item(16).name()).equals("custom amount");
  Quote result=null;
  for(int i=0;i<menu.containerSlots();i++){
   var item=menu.item(i);if(item.count()==0||item.minecraftId().equals("minecraft:barrier"))continue;String name=normal(item.name());
   boolean fullName=matchesBuy(name,request),custom=name.equals("custom amount"),generic=Set.of("confirm","buy","buy instantly","proceed").contains(name)||name.startsWith("confirm ");
   boolean position=dialog&&(i==13&&(fullName||custom)||generic)||!dialog&&quantity&&i==(request.quantity()==1?10:request.quantity()==64?12:-1)&&fullName||!dialog&&!quantity&&i==13&&custom;
   if(!position)continue;
   if(!item.uuid().isBlank()||item.protectedItem()||!item.skyblockId().isEmpty()&&!item.skyblockId().equals(request.productId()))return Optional.empty();
   int named=0,amount=0,activation=0;BigDecimal total=null;
   for(String raw:item.lore()){
    String line=normal(raw);if(line.equals(normal(request.displayName())))named++;
    var a=AMOUNT.matcher(line);if(a.matches()){try{if(Integer.parseInt(a.group(1).replace(",",""))!=request.quantity())return Optional.empty();}catch(NumberFormatException invalid){return Optional.empty();}amount++;}
    else if(line.startsWith("amount:"))return Optional.empty();
    if(line.equals("click to buy now!"))activation++;
    var p=PRICE.matcher(line);if(p.matches()){if(total!=null)return Optional.empty();total=new BigDecimal(p.group(1).replace(",",""));}
    else if(line.startsWith("price:")||EXTRA.matcher(line).find()&&!line.equals(normal(request.displayName())))return Optional.empty();
   }
   if(named>1||amount>1||activation>1||custom&&(named!=1||amount!=1||activation!=1)||generic&&!fullName&&(named!=1||amount!=1)||total==null||!request.authorizes(request.productId(),request.quantity(),total))return Optional.empty();
   var contract=new MenuContract(Set.of(menu.title()),menu.containerSlots());var quote=new Quote(contract,contract.plan(menu,i,MenuContract.Click.PICKUP),total);
   if(result!=null)return Optional.empty();result=quote;
  }
  return Optional.ofNullable(result);
 }
 private static boolean matchesBuy(String name,BazaarAcquisitionPlan.Request request){var m=BUY.matcher(name);if(!m.matches()||!m.group(2).equals(normal(request.displayName())))return false;try{return Integer.parseInt(m.group(1).replace(",",""))==request.quantity();}catch(NumberFormatException overflow){return false;}}
 private static String normal(String text){return text.replaceAll("(?i)§[0-9a-fk-orx]","").trim().toLowerCase(Locale.ROOT);}
 private static boolean complete(MenuSnapshot menu){
  if(menu==null||menu.containerId()<1||menu.containerId()>255||menu.title().isBlank()||menu.title().length()>256||menu.containerSlots()<18||menu.containerSlots()>54||menu.containerSlots()%9!=0||menu.slots().size()!=menu.containerSlots()+36)return false;
  for(int i=0;i<menu.slots().size();i++){var slot=menu.slot(i);if(slot==null||slot.playerInventory()!=(i>=menu.containerSlots()))return false;var x=slot.item();if(x.count()>64||x.name().length()>256||x.lore().size()>128||x.lore().stream().anyMatch(l->l.length()>512)||x.count()==0&&(!x.minecraftId().equals("minecraft:air")||!x.skyblockId().isBlank()||!x.uuid().isBlank()))return false;}
  return true;
 }
 private BazaarPurchaseView(){}
}
