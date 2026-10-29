// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.regex.*;
/** U-GEORGE source roles/input13; bounded price and GAME text are additional provisional local grammars. */
public final class GeorgeSaleView {
 public record Offer(MenuContract contract,MenuContract.Ticket ticket,PetSaleAttempt.Quote quote){}
 private static final String N="(?:[0-9]{1,15}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,4})";
 private static final Pattern PRICE=Pattern.compile("^(?:You will receive|Sell Price|Price|Offer): ("+N+") [Cc]oins$");
 public static boolean main(MenuSnapshot m){return BazaarNavigationView.complete(m)&&Set.of("George","Sell Pets","Pet Collector","Offer Pets").contains(m.title())&&safeRoles(m);}
 public static Optional<Offer> offer(MenuSnapshot menu,PetSalePreflight.Pet pet,long minimum){return main(menu)?parse(menu,pet,minimum,Set.of("Sell Pet","Accept","Confirm","Offer")):Optional.empty();}
 public static Optional<Offer> confirmation(MenuSnapshot menu,PetSaleAttempt.Quote original){
  if(original==null||!BazaarNavigationView.complete(menu)||!Set.of("Confirm Sale","Are you sure?","Are You Sure?").contains(menu.title()))return Optional.empty();
  return parse(menu,original.pet(),original.minimumPayout(),Set.of("Confirm","Yes","Accept")).filter(x->x.quote().payout()==original.payout());
 }
 private static boolean safeRoles(MenuSnapshot menu){
  // A visible payout never grants permission for a separate fee, currency charge or warning.
  for(int i=0;i<menu.containerSlots();i++)if(i!=13){var lore=menu.item(i).lore();if(lore.size()>128)return false;for(var line:lore)if(line.length()>512||line.toLowerCase(Locale.ROOT).matches(".*\\b(?:costs?|fees?|tax(?:es)?|bits|gems|warning)\\b.*"))return false;}
  return true;
 }
 private static Optional<Offer> parse(MenuSnapshot menu,PetSalePreflight.Pet pet,long minimum,Set<String> names){
  if(pet==null||minimum<=0||!PetSaleAttempt.applied(pet.item(),menu.item(13))||!safeRoles(menu))return Optional.empty();
  int found=-1;long payout=-1;
  for(int i=0;i<menu.containerSlots();i++){
   var x=menu.item(i);if(i==13||!names.contains(x.name()))continue;
   if(found>=0||x.count()!=1||x.protectedItem()||x.identified()||!x.uuid().isEmpty()||x.minecraftId().equals("minecraft:barrier")||x.lore().size()>128)return Optional.empty();found=i;
   for(String line:x.lore()){if(line.length()>512)return Optional.empty();var match=PRICE.matcher(line);if(!match.matches())continue;if(payout!=-1)return Optional.empty();try{payout=Long.parseLong(match.group(1).replace(",",""));}catch(NumberFormatException invalid){return Optional.empty();}}
  }
  if(found<0||payout<minimum||payout>1_000_000_000_000_000L)return Optional.empty();var contract=new MenuContract(Set.of(menu.title()),menu.containerSlots());return Optional.of(new Offer(contract,contract.plan(menu,found,MenuContract.Click.PICKUP),new PetSaleAttempt.Quote(pet,minimum,payout)));
 }
 /** No UUID is fabricated from text: only the single already-proven native pending custody may correlate this notice. */
 public static boolean saleMessage(String text,PetSaleAttempt.Quote pending){
  if(text==null||text.length()>512||pending==null)return false;String name=Pattern.quote(pending.pet().item().name());
  var match=Pattern.compile("^You sold "+name+" to George for ("+N+") coins!$").matcher(text);if(!match.matches())return false;
  try{return Long.parseLong(match.group(1).replace(",",""))==pending.payout();}catch(NumberFormatException invalid){return false;}
 }
 private GeorgeSaleView(){}
}
