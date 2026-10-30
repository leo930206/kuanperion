package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestExchangeViewTest {
 static final ItemSnapshot AIR=PestBaitButtonViewTest.AIR;
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static ItemSnapshot role(String...lore){return new ItemSnapshot("","minecraft:hopper","","Empty Vacuum Bag","exchange-role"+Arrays.toString(lore),1,List.of(lore),false);}
 static MenuSnapshot menu(String title,Map<Integer,ItemSnapshot> changed){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<63;i++)slots.add(new MenuSnapshot.Slot(i,changed.getOrDefault(i,AIR),i>=27));return new MenuSnapshot(1,7,1,title,27,slots);}
 static MenuSnapshot menu(ItemSnapshot role){return menu("Pesthunter",Map.of(13,role));}
 public static void main(String[]args){
  var ready=role("Click to empty your Vacuum Bag!");var m=menu(ready);var r=PestExchangeView.read(m).orElseThrow();check(r.slot()==13&&r.status()==PestExchangeView.Status.READY,"source role and click hint");check(r.ticket().slot()==13&&r.ticket().click()==MenuContract.Click.PICKUP&&r.contract().revalidate(r.ticket(),m),"only exact one-role PICKUP");
  check(PestExchangeView.read(menu(role("You have exchanged enough Pests recently!"))).orElseThrow().status()==PestExchangeView.Status.ALREADY_EXCHANGED,"source cooldown is known skip, not a fresh exchange");
  for(String title:List.of("Phillip","Pesthunter Phillip"))check(PestExchangeView.read(menu(title,Map.of(13,ready))).isPresent(),"explicit provisional source title "+title);
  for(String title:List.of("Chest","My Phillip Storage","pesthunter"))check(PestExchangeView.read(menu(title,Map.of(13,ready))).isEmpty(),"no broad title substring "+title);
  for(String bad:List.of("Cost: 1 Coins","Cost: 0 Coins","Buy now","10 Bits","Gem fee","Click to empty","Click to empty your Vacuum Bag!\nYou have exchanged enough Pests","x".repeat(513)))check(PestExchangeView.read(menu(role("Click to empty your Vacuum Bag!",bad))).isEmpty(),"unknown/cost/contradiction "+bad.substring(0,Math.min(30,bad.length())));
  check(PestExchangeView.read(menu(role("Unknown bag status"))).isEmpty(),"missing activation/skip remains unknown");
  check(PestExchangeView.read(menu("Pesthunter",Map.of(13,ready,14,ready))).isEmpty(),"duplicate external roles veto");
  check(PestExchangeView.read(menu("Pesthunter",Map.of(27,ready))).isEmpty(),"player alias is not NPC role");
  check(PestExchangeView.read(menu("Pesthunter",Map.of())).isEmpty(),"guessed empty slot grants nothing");
  var malformed=new MenuSnapshot(1,7,1,"Pesthunter",27,m.slots().subList(0,62));check(PestExchangeView.read(malformed).isEmpty(),"partial menu refused");
  var protectedRole=new ItemSnapshot("","minecraft:hopper","","Empty Vacuum Bag","role",1,ready.lore(),true);check(PestExchangeView.read(menu(protectedRole)).isEmpty(),"protected role not effect authority");
  var identified=new ItemSnapshot("SKYMART_VACUUM","minecraft:hopper","uuid","Empty Vacuum Bag","role",1,ready.lore(),false);check(PestExchangeView.read(menu(identified)).isEmpty(),"real item cannot masquerade as role");
  System.out.println("PestExchangeViewTest: PASS source-marked exact role states; no native/server claim");
 }
}
