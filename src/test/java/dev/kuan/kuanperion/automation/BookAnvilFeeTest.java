package dev.kuan.kuanperion.automation;
import java.util.*;
public final class BookAnvilFeeTest {
 static final List<String> CURRENT=List.of("Combine the items in the slots to the","left and right below.","","Cost","0 Exp Levels","Your XP: 398","","Click to combine!");
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static BookAnvilView view(List<String> lore){var f=new BookCombineTaskTest.Fake();f.visible=true;f.icons.put(22,new ItemSnapshot("","minecraft:lime_terracotta","","Combine Items","current-fee",1,lore,false));return f.view();}
 public static void main(String[]args){
  check(view(CURRENT).freeCombine()!=null,"2026-10-27 book+book screenshot: separate Cost / 0 Exp Levels / non-cost Your XP info");
  check(view(List.of("Cost: 0 Exp Levels","Click to combine!")).freeCombine()!=null,"legacy explicit zero remains supported");
  check(view(List.of("Cost: 0 Experience Levels","Click to combine!")).freeCombine()!=null,"legacy long zero format remains supported");
  check(view(List.of("Cost","0 Exp Levels","Your XP: 0","Click to combine!")).freeCombine()!=null,"zero current XP is not a fee or authority to spend");
  for(var lore:List.of(List.of("Click to combine!"),List.of("Cost","1 Exp Levels","Your XP: 398","Click to combine!"),List.of("Cost","0 Coins","Click to combine!"),List.of("Cost","0 Exp Levels","Cost: 0 Exp Levels","Click to combine!"),List.of("Cost","0 Exp Levels","Your XP: 398","Your XP: 398","Click to combine!"),List.of("Cost","0 Exp Levels","Your XP: -1","Click to combine!"),List.of("Cost","0 Exp Levels","Your XP: 1.5","Click to combine!"),List.of("Cost","0 Exp Levels","Your XP: 999999999999","Click to combine!"),List.of("Cost","0 Exp Levels","Price: 10 Bits","Click to combine!"),List.of("Cost","0 Exp Levels","Requires 1 Token","Click to combine!"),List.of("Cost","0 Exp Levels","Click to combine!","Click to combine!"),List.of("Cost: 0 Exp Levels","free donation: 100","Click to combine!")))check(view(lore).freeCombine()==null,"unknown, positive, duplicate or malformed fee/info denied: "+lore);
  System.out.println("BookAnvilFeeTest: PASS (actual screenshot multiline zero/Your-XP informational grammar, legacy zero, positive/unknown/duplicate/missing rejection)");
 }
}
