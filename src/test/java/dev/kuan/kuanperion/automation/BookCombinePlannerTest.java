package dev.kuan.kuanperion.automation;
import java.util.*;
public final class BookCombinePlannerTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static BookCombinePlanner.Book book(int slot,int level,String metadata,boolean locked){return new BookCombinePlanner.Book(slot,new ItemSnapshot("ENCHANTED_BOOK","minecraft:enchanted_book","uuid-"+slot,"Enchanted Book","book-"+slot,1,List.of("not authority"),locked),Map.of("harvesting",level),metadata);}
 public static void main(String[]args){
  var a=book(9,4,"same-other-metadata",false);var b=book(10,4,"same-other-metadata",false);var input=List.of(b,a);
  check(new BookCombinePlanner(Map.of(),Set.of()).select(input).isEmpty(),"unconfigured enchantments never authorize consumption");
  var planner=new BookCombinePlanner(Map.of("harvesting",5),Set.of());var plan=planner.select(input).orElseThrow();check(plan.first().inventorySlot()==9&&plan.second().inventorySlot()==10&&plan.outputLevel()==5,"deterministic exact equal-level pair");
  check(planner.select(List.of(a,book(11,5,"same-other-metadata",false))).isEmpty(),"different levels / maximum output level never combined");
  check(planner.select(List.of(a,book(11,4,"different-origin-or-attributes",false))).isEmpty(),"extra metadata mismatch not silently discarded");
  check(planner.select(List.of(a,book(11,4,"same-other-metadata",true))).isEmpty(),"locked/soulbound/tool protection wins");
  check(planner.select(List.of(a,book(1,4,"same-other-metadata",false))).isEmpty(),"hotbar never consumed");
  check(new BookCombinePlanner(Map.of("harvesting",5),Set.of("UUID10")).select(input).isEmpty(),"normalized UUID protection wins");
  var dual=new BookCombinePlanner.Book(11,b.item(),Map.of("harvesting",4,"cultivating",4),b.compatibility());check(planner.select(List.of(a,dual)).isEmpty(),"multi-enchantment books never reduced to one line of lore");
  check(planner.select(List.of(a,a)).isEmpty(),"same physical item cannot be both inputs");
  check(planner.stillMatches(plan,input),"fresh pair retained");check(!planner.stillMatches(plan,List.of(a,book(10,3,b.compatibility(),false))),"changed level invalidates selection before GUI transfer");
  var output=book(-1,5,"same-other-metadata",false);check(planner.isExpectedResult(plan,output),"output requires exact upgraded enchantment and preserved remaining metadata");check(!planner.isExpectedResult(plan,book(-1,5,"different",false)),"output metadata loss cannot count as success");check(!planner.isExpectedResult(plan,book(-1,4,a.compatibility(),false)),"preview/input cannot masquerade as result");
  var renamedBook=new BookCombinePlanner.Book(11,new ItemSnapshot("ENCHANTED_BOOK","minecraft:enchanted_book","","Personal Name","renamed",1,List.of(),false),Map.of("harvesting",4),a.compatibility());check(planner.select(List.of(a,renamedBook)).isEmpty(),"custom-named books are excluded rather than silently losing the name");
  var renamedFake=new BookCombinePlanner.Book(11,new ItemSnapshot("PAPER","minecraft:paper","","Enchanted Book","fake",1,List.of("Harvesting IV"),false),Map.of("harvesting",4),a.compatibility());check(planner.select(List.of(a,renamedFake)).isEmpty(),"name/lore/claimed enchantment alone not book identity");
  System.out.println("BookCombinePlannerTest: PASS (exact metadata/levels, protected IDs/UUIDs/hotbar, no multi-enchant/duplicate/name inference, preserved result proof)");
 }
}
