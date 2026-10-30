package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSpraySelectionViewTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static ItemSnapshot button(String... lore){return new ItemSnapshot("TEST_MATERIAL","minecraft:paper","","Test Material","selection-button"+Arrays.toString(lore),1,List.of(lore),false);}
 static MenuSnapshot menu(String title,Map<Integer,ItemSnapshot> overrides){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<63;i++)slots.add(new MenuSnapshot.Slot(i,overrides.getOrDefault(i,PestSprayPlanTest.AIR),i>=27));return new MenuSnapshot(1,7,1,title,27,slots);}
 public static void main(String[]args){
  var p=PestSprayPlanTest.policy();var b=button("Click to select!");var m=menu("Sprayonator",Map.of(13,b));var role=PestSpraySelectionView.read(m,p).orElseThrow();check(role.slot()==13&&role.ticket().click()==MenuContract.Click.PICKUP&&role.contract().revalidate(role.ticket(),m),"exact configured native material role, never fixed guessed slot");
  for(String title:List.of("Chest","My Sprayonator Storage","sprayonator"))check(PestSpraySelectionView.read(menu(title,Map.of(13,b)),p).isEmpty(),"no broad title match");
  for(var bad:List.of(button("Selected!"),button("Click to select!","Cost: 0 Coins"),button("Click to select!","Price: 1"),button("Click to select!","$1"),button("Click to select!","Buy from Bazaar"),button("Click to select!\nCost: 1 Coins"),button("Click to select!","Withdraw from Sacks")))check(PestSpraySelectionView.read(menu("Sprayonator",Map.of(13,bad)),p).isEmpty(),"unknown/cost/contradictory/stack-transfer role cannot grant selection");
  check(PestSpraySelectionView.read(menu("Sprayonator",Map.of(13,b,14,b)),p).isEmpty()&&PestSpraySelectionView.read(menu("Sprayonator",Map.of(27,b)),p).isEmpty(),"duplicate and player-slot fallback refused");
  var fake=new ItemSnapshot("OTHER_MATERIAL","minecraft:paper","","Test Material","fake",1,b.lore(),false);check(PestSpraySelectionView.read(menu("Sprayonator",Map.of(13,fake)),p).isEmpty(),"same displayed name cannot override explicit material ID");
  var inv=new HashMap<>(PestSprayPlanTest.inventory());inv.put(4,PestSprayToolTest.tool("SPRAYONATOR","Selected Material: None"));check(PestSprayPlan.select(p,inv).isEmpty()&&PestSprayPlan.prepare(p,inv).isPresent(),"preparation is NOT permission to use a wrong/empty selected material");
  System.out.println("PestSpraySelectionViewTest: PASS provisional exact no-cost source-style roles and prepare/use separation");
 }
}
