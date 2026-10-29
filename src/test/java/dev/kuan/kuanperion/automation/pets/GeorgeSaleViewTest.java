package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class GeorgeSaleViewTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final PetSalePreflight.Pet PET=PetSalePreflightTest.pet(9,PetSalePreflightTest.A,PetSalePreflightTest.plain(),false);
 static ItemSnapshot button(String name,String...lore){return new ItemSnapshot("","minecraft:lime_terracotta","",name,name+Arrays.toString(lore),1,List.of(lore),false);}
 static MenuSnapshot menu(String title,Map<Integer,ItemSnapshot> changed){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)slots.add(new MenuSnapshot.Slot(i,changed.getOrDefault(i,PetSalePreflightTest.AIR),i>=54));return new MenuSnapshot(1,7,1,title,54,slots);}
 public static void main(String[]args){
  var m=menu("Offer Pets",Map.of(13,PET.item(),22,button("Sell Pet","You will receive: 200 Coins")));var offer=GeorgeSaleView.offer(m,PET,100).orElseThrow();check(offer.quote().payout()==200&&offer.ticket().slot()==22,"source semantic role plus explicit exact quote and selected UUID");check(GeorgeSaleView.offer(m,PET,201).isEmpty(),"minimum is not a market estimate");check(GeorgeSaleView.offer(menu("Offer Pets",Map.of(13,PetSalePreflightTest.item(PetSalePreflightTest.B,false),22,m.item(22))),PET,100).isEmpty(),"display name cannot replace selected pet identity");
  check(GeorgeSaleView.offer(menu("Offer Pets",Map.of(13,PET.item(),22,button("Sell Pet","Price: 200 Coins","Price: 201 Coins"))),PET,100).isEmpty(),"ambiguous quote");check(GeorgeSaleView.offer(menu("Offer Pets",Map.of(13,PET.item(),22,m.item(22),23,m.item(22))),PET,100).isEmpty(),"duplicate sale role");
  var confirm=menu("Confirm Sale",Map.of(13,PET.item(),11,button("Confirm","Price: 200 Coins")));check(GeorgeSaleView.confirmation(confirm,offer.quote()).isPresent(),"source confirm continuation retains same pet and quote");check(GeorgeSaleView.confirmation(menu("Confirm Sale",Map.of(13,PET.item(),11,button("Confirm","Price: 199 Coins"))),offer.quote()).isEmpty(),"changed payout is not original quote");
  check(GeorgeSaleView.saleMessage("You sold [Lvl 1] Slug to George for 200 coins!",offer.quote()),"provisional exact pending pet/name/payout GAME grammar");check(!GeorgeSaleView.saleMessage("[Player] You sold [Lvl 1] Slug to George for 200 coins!",offer.quote())&&!GeorgeSaleView.saleMessage("You sold [Lvl 1] Slug to George for 201 coins!",offer.quote()),"player prefix/wrong payout refused; adapter must correlate private pending custody");
  check(!GeorgeSaleView.main(menu("George",Map.of(5,button("Details","Cost: 10 Gems")))),"do not deposit an authorized pet into a menu advertising an unapproved fee");
  for(String fee:List.of("Cost: 10 Gems","Fee: 1 Coins","Tax: 1%","Warning: unknown transaction")){
   check(GeorgeSaleView.offer(menu("Offer Pets",Map.of(13,PET.item(),22,button("Sell Pet","Price: 200 Coins",fee))),PET,100).isEmpty(),"payout never authorizes fee/currency/warning: "+fee);
   check(GeorgeSaleView.offer(menu("Offer Pets",Map.of(13,PET.item(),22,m.item(22),5,button("Details",fee))),PET,100).isEmpty(),"a fee on another external info slot is not invisible");
   check(GeorgeSaleView.confirmation(menu("Confirm Sale",Map.of(13,PET.item(),11,button("Confirm","Price: 200 Coins",fee))),offer.quote()).isEmpty(),"confirmation cannot add hidden cost");
  }
  System.out.println("GeorgeSaleViewTest: PASS provisional original-source roles/local price+GAME grammar, not live compatibility");
 }
}
