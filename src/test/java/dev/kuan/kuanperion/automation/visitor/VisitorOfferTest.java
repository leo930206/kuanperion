package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class VisitorOfferTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final Map<String,String> IDS=Map.of("Enchanted Wheat","ENCHANTED_WHEAT","Enchanted Carrot","ENCHANTED_CARROT");
 static ItemSnapshot button(String name,List<String> lore){return new ItemSnapshot("WRONG_SCANNER_ID","minecraft:green_terracotta","",name,"fp:"+name+lore,1,lore,false);}
 static MenuSnapshot menu(List<String> lore){return new MenuSnapshot(1,7,2,"Librarian",54,List.of(new MenuSnapshot.Slot(17,button("Accept Offer",lore)),new MenuSnapshot.Slot(35,button("Refuse Offer",List.of()))));}
 static List<String> lore(){return List.of("Items Required:","Enchanted Wheat x3","Enchanted Carrot x1,024","","Rewards:","+25 Copper","+1,000 Farming XP","Click to give!");}
 static VisitorOffer read(){return VisitorOffer.inspect(menu(lore()),"Librarian",IDS).orElseThrow();}
 public static void main(String[] args){
  var offer=read();check(offer.visitor().equals("Librarian")&&offer.acceptSlot()==17&&offer.refuseSlot()==35,"semantic buttons, not source's fixed29/33");
  check(offer.requirements().get(0).itemId().equals("ENCHANTED_WHEAT")&&offer.requirements().get(0).quantity()==3&&offer.requirements().get(1).quantity()==1024,"exact independent name-to-ID mapping, never use scanner item's ID");
  check(offer.copper()==25&&offer.rewards().contains("+1,000 Farming XP"),"typed copper and immutable remaining rewards; no inferred profit");
  check(VisitorOffer.inspect(menu(lore()),"librarian",IDS).isEmpty()&&VisitorOffer.inspect(menu(lore()),"Farmer",IDS).isEmpty(),"exact selected visitor title only");
  check(VisitorOffer.inspect(menu(lore()),"Librarian",Map.of()).isEmpty(),"unknown required ID denies entire offer, not zero-price fallback");
  var missingRewards=new ArrayList<>(lore());missingRewards.remove("Rewards:");check(VisitorOffer.inspect(menu(missingRewards),"Librarian",IDS).isEmpty(),"no guessing section boundary");
  for(String bad:List.of("Enchanted Wheat x0","Enchanted Wheat x01","Enchanted Wheat x1,02","Enchanted Wheat x-1","Enchanted Wheat x1.2","Enchanted Wheat x65537","Unknown x3","Enchanted Wheat x999999999999999999999")){
   var l=new ArrayList<>(lore());l.set(1,bad);check(VisitorOffer.inspect(menu(l),"Librarian",IDS).isEmpty(),"invalid whole requirement rejected: "+bad);
  }
  for(String copper:List.of("+0 Copper","+25 Copper","25 Copper")){
   var l=new ArrayList<>(lore());l.set(5,copper);check(VisitorOffer.inspect(menu(l),"Librarian",IDS).isPresent(),"bounded integer copper: "+copper);
  }
  for(String copper:List.of("+1,00 Copper","+25.5 Copper","+1000001 Copper","+999999999999999999999 Copper","+25 Copper more")){
   var l=new ArrayList<>(lore());l.set(5,copper);check(VisitorOffer.inspect(menu(l),"Librarian",IDS).isEmpty(),"malformed copper not silently omitted: "+copper);
  }
  var dup=new ArrayList<>(lore());dup.add(2,"Enchanted Wheat x2");check(VisitorOffer.inspect(menu(dup),"Librarian",IDS).isEmpty(),"duplicate requirements are ambiguous");
  var alias=new HashMap<>(IDS);alias.put("Enchanted Carrot","ENCHANTED_WHEAT");check(VisitorOffer.inspect(menu(lore()),"Librarian",alias).isEmpty(),"aliases cannot authorize double consumption of same ID");
  var dupCopper=new ArrayList<>(lore());dupCopper.add("+25 Copper");check(VisitorOffer.inspect(menu(dupCopper),"Librarian",IDS).isEmpty(),"even same duplicate copper rejects");
  var dye=new ArrayList<>(lore());dye.add("Copper Dye");check(VisitorOffer.inspect(menu(dye),"Librarian",IDS).orElseThrow().rewards().contains("Copper Dye"),"rare reward text is not a malformed numeric copper amount or invented profit");
  var dupSection=new ArrayList<>(lore());dupSection.add("Items Required:");check(VisitorOffer.inspect(menu(dupSection),"Librarian",IDS).isEmpty(),"section replay rejected");
  var base=menu(lore());var slots=new ArrayList<>(base.slots());slots.add(new MenuSnapshot.Slot(29,button("Accept Offer",lore())));check(VisitorOffer.inspect(new MenuSnapshot(1,7,2,"Librarian",54,slots),"Librarian",IDS).isEmpty(),"ambiguous accept roles rejected");
  check(VisitorOffer.inspect(new MenuSnapshot(1,7,2,"Librarian",54,List.of(new MenuSnapshot.Slot(17,button("Accept Offer",lore()),true),base.slots().get(1))),"Librarian",IDS).isEmpty(),"player item cannot impersonate external role");
  check(VisitorOffer.inspect(new MenuSnapshot(1,7,2,"Librarian",54,List.of(new MenuSnapshot.Slot(70,button("Accept Offer",lore())),base.slots().get(1))),"Librarian",IDS).isEmpty(),"out-of-container role cannot authorize");
  var singleton=List.of("Items Required:","Enchanted Wheat","Rewards:","+1 Copper");check(VisitorOffer.inspect(menu(singleton),"Librarian",IDS).orElseThrow().requirements().get(0).quantity()==1,"explicit grammar permits one item without x suffix");
  var huge=new ArrayList<String>();for(int i=0;i<129;i++)huge.add("x");check(VisitorOffer.inspect(menu(huge),"Librarian",IDS).isEmpty(),"bounded lore projection");
  try{offer.requirements().clear();throw new AssertionError("mutable requirements");}catch(UnsupportedOperationException expected){}
  System.out.println("VisitorOfferTest: PASS (exact selected title/semantic external roles, explicit IDs, strict quantities/sections/copper, unknown/duplicate/player/bounds deny, immutable read-only facts)");
 }
}
