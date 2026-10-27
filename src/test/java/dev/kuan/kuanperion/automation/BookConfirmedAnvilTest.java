package dev.kuan.kuanperion.automation;
import java.util.*;
/** Screenshot-supported roles; later result authority still requires consumed inputs and fresh receipts. */
public final class BookConfirmedAnvilTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[]args){
  var layout=BookAnvilView.confirmedLayout();
  check(layout.title().equals("Anvil")&&layout.size()==54&&layout.firstInput()==29&&layout.secondInput()==33&&layout.preview()==13&&layout.combine()==22,"user-confirmed six-row geometry, not old fixture inputs20/24");
  check(layout.resultSlots().equals(Set.of(13,22)),"only known preview/combine roles are candidates, neither is assumed to be an actual post-combine result");
  var f=new BookCombineTaskTest.Fake();f.visible=true;
  var planner=new BookCombinePlanner(Map.of("harvesting",5),Set.of());var plan=planner.select(new ArrayList<>(f.inventory.values())).orElseThrow();
  var books=new HashMap<Integer,BookCombinePlanner.Book>();books.put(13,BookCombineTaskTest.book(-1,5,"result",1));
  var slots=new ArrayList<>(f.menu().slots());slots.set(13,new MenuSnapshot.Slot(13,books.get(13).item(),false));
  var menu=new MenuSnapshot(1,4,1,"Anvil",54,slots);var v=new BookAnvilView(layout,menu,books);
  check(v.resultSlot(planner,plan)==13,"expected result can be in proved preview role after inputs consumed");
  books.put(22,BookCombineTaskTest.book(-1,5,"second",1));slots.set(22,new MenuSnapshot.Slot(22,books.get(22).item(),false));
  v=new BookAnvilView(layout,new MenuSnapshot(1,4,2,"Anvil",54,slots),books);check(v.resultSlot(planner,plan)==-1,"two candidate books are ambiguous, never pick one");
  books.remove(22);slots.set(22,new MenuSnapshot.Slot(22,BookCombineTaskTest.button("Cost: 0 Exp Levels"),false));books.put(29,BookCombineTaskTest.book(-1,4,"one",1));slots.set(29,new MenuSnapshot.Slot(29,books.get(29).item(),false));
  v=new BookAnvilView(layout,new MenuSnapshot(1,4,3,"Anvil",54,slots),books);check(v.resultSlot(planner,plan)==-1,"unchanged preview with remaining input is not a result");
  books.remove(29);slots.set(29,new MenuSnapshot.Slot(29,BookCombineTaskTest.empty(),false));slots.set(22,new MenuSnapshot.Slot(22,new ItemSnapshot("","minecraft:lime_terracotta","","Combine Items","user-tooltip",1,List.of("Combine the items in the slots to the","left and right below.","Click to combine!"),false),false));
  v=new BookAnvilView(layout,new MenuSnapshot(1,4,4,"Anvil",54,slots),books);check(v.freeCombine()==null,"supplied gear tooltip does not state zero cost; absent fee is never free");
  System.out.println("BookConfirmedAnvilTest: PASS (confirmed inputs29/33, preview13/button22, conditional unambiguous result, no free-cost inference)");
 }
}
