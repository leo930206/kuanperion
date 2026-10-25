package dev.kuan.kuanperion.automation;
import java.util.*;
public final class CursorReceiptsTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final ItemSnapshot BAIT=new ItemSnapshot("TASTY_CHEESE","minecraft:paper","","Tasty Cheese","exact",16,List.of(),false),EMPTY=new ItemSnapshot("","minecraft:air","","","",0,List.of(),false);
 public static void main(String[]args){
  var r=new CursorReceipts();var stamp=r.stamp(1,4,0);check(r.after(stamp,S,2*S).isEmpty(),"predicted/local cursor is never a server receipt");
  r.observe(1,4,BAIT,S);check(r.after(stamp,S,2*S).orElseThrow().item().equals(BAIT),"new packet for exact world/container with immutable item");var beforeEmpty=r.stamp(1,4,2*S);check(!r.emptyAfter(beforeEmpty,2*S,2*S),"old cursor observation cannot acknowledge a new click");
  r.observe(1,4,EMPTY,3*S);check(r.emptyAfter(beforeEmpty,3*S,2*S),"fresh server empty cursor evidence");check(!r.emptyAfter(beforeEmpty,6*S,2*S),"expired empty cursor not reusable");
  var old=r.stamp(1,4,7*S);r.clear();r.observe(1,4,EMPTY,8*S);check(r.after(old,8*S,2*S).isEmpty(),"clear epoch invalidates even same world/container IDs");
  old=r.stamp(1,4,9*S);r.observe(1,5,EMPTY,10*S);check(r.after(old,10*S,2*S).isEmpty(),"another menu cannot satisfy the old cursor transaction");
  old=r.stamp(1,5,11*S);r.observe(2,5,EMPTY,12*S);check(r.after(old,12*S,2*S).isEmpty(),"world identity changes invalidate evidence");
  stamp=r.stamp(2,5,13*S);r.observe(2,5,null,14*S);check(r.after(stamp,14*S,2*S).isEmpty(),"malformed packet cannot borrow previous empty cursor");
  stamp=r.stamp(2,5,15*S);r.observe(2,5,new ItemSnapshot("TASTY_CHEESE","minecraft:paper","","","bad",0,List.of(),false),16*S);check(!r.emptyAfter(stamp,16*S,2*S),"zero-count identified non-air is not invented as valid server empty");
  stamp=r.stamp(2,5,17*S);r.observe(2,5,EMPTY,18*S);check(r.after(stamp,17*S,2*S).isEmpty(),"future observation cannot satisfy earlier tick");
  for(int i=0;i<10000;i++)r.observe(2,5,EMPTY,19*S+i);check(r.cached()==1,"bounded latest-only cursor cache, no history growth");
  System.out.println("CursorReceiptsTest: PASS (server-only sequence/freshness, exact menu/world scope, clear epochs, unknown/zero-count/future/stale rejection, bounded latest cache)");
 }
}
