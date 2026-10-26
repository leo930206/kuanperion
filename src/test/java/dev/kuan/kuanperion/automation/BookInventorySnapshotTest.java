package dev.kuan.kuanperion.automation;
import java.util.*;
public final class BookInventorySnapshotTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Map<Integer,ItemSnapshot> inventory(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,JunkTaskTest.empty());return m;}
 static BookInventorySnapshot snapshot(Map<Integer,ItemSnapshot> inv,Map<Integer,BookCombinePlanner.Book> books){return BookInventorySnapshot.inspect(inv,books).orElseThrow();}
 public static void main(String[]args){
  var a=BookCombinePlannerTest.book(9,4,"same",false);var b=BookCombinePlannerTest.book(10,4,"same",false);var inv=inventory();inv.put(9,a.item());inv.put(10,b.item());var books=new HashMap<Integer,BookCombinePlanner.Book>(Map.of(9,a,10,b));var s=snapshot(inv,books);
  check(new BookCombinePlanner(Map.of("harvesting",5),Set.of()).select(new ArrayList<>(s.books().values())).isPresent(),"complete immutable native projection can feed existing exact planner");
  check(s.stillMatches(inv),"unchanged complete43 baseline");inv.put(42,AutoSellTaskTest.wheat(1));check(!s.stillMatches(inv),"equipment/saddle drift not invisible to future driver");inv.put(42,JunkTaskTest.empty());
  books.clear();inv.put(9,JunkTaskTest.empty());check(s.books().size()==2&&s.inventory().get(9).equals(a.item()),"caller mutations cannot change frozen observation");
  inv=new HashMap<>(s.inventory());books=new HashMap<>(s.books());
  check(BookInventorySnapshot.inspect(inv,Map.of(9,a)).isEmpty(),"cannot omit a raw server book and pretend it is unrelated");
  check(BookInventorySnapshot.inspect(inv,Map.of(9,a,11,b)).isEmpty(),"book slot must agree with canonical inventory slot");
  inv.remove(42);check(BookInventorySnapshot.inspect(inv,books).isEmpty(),"missing protected equipment rejects incomplete projection");inv.put(43,JunkTaskTest.empty());check(BookInventorySnapshot.inspect(inv,books).isEmpty(),"size43 with noncanonical key cannot pass");
  inv=new HashMap<>(s.inventory());var duplicate=new ItemSnapshot("OTHER","minecraft:paper",a.item().uuid().toUpperCase(Locale.ROOT),"Other","other",1,List.of(),true);inv.put(40,duplicate);check(BookInventorySnapshot.inspect(inv,books).isEmpty(),"normalized duplicate identity even in protected offhand vetoes ambiguity");
  inv=new HashMap<>(s.inventory());var tooMany=new ItemSnapshot("OTHER","minecraft:paper","","Other","other",65,List.of(),false);inv.put(11,tooMany);check(BookInventorySnapshot.inspect(inv,books).isEmpty(),"impossible counts refused");
  check(BookInventorySnapshot.inspect(null,books).isEmpty()&&BookInventorySnapshot.inspect(s.inventory(),null).isEmpty(),"missing observations are not empty inventories");
  System.out.println("BookInventorySnapshotTest: PASS (complete immutable43, exact raw-book projection, canonical slots, full-equipment drift, duplicate identity/count/missing denial; data only)");
 }
}
