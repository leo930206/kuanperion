package dev.kuan.kuanperion.automation;
import java.util.List;
public final class CursorOpeningReceiptsTest {
 static final long S=1_000_000_000L;static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 public static void main(String[]args){var r=new CursorReceipts();var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),false);
  var open=r.openingStamp(1,S);check(!r.emptyAfterOpening(open,4,2*S,10*S),"physical prediction is no server opening receipt");r.observe(1,4,empty,2*S);
  check(r.emptyAfterOpening(open,4,2*S,10*S),"server opening empty for newly assigned exact container");check(!r.emptyAfterOpening(open,5,2*S,10*S),"exact target container required");
  check(!r.emptyAfterOpening(open,4,S,10*S),"future evidence denied");check(r.emptyAfterOpening(open,4,12*S,10*S)&&!r.emptyAfterOpening(open,4,12*S+1,10*S),"inclusive age boundary");
  var action=r.stamp(1,4,3*S);check(!r.emptyAfter(action,3*S,10*S),"opening evidence cannot acknowledge later item action");
  open=r.openingStamp(1,4*S);r.observe(1,4,empty,3*S);check(!r.emptyAfterOpening(open,4,4*S,10*S),"pre-request packet timestamp cannot prove opening");
  open=r.openingStamp(1,5*S);r.observe(1,4,empty,5*S);check(r.emptyAfterOpening(open,4,5*S,10*S),"new server revision at identical timestamp is valid");
  var next=r.openingStamp(1,5*S);check(!r.emptyAfterOpening(next,4,5*S,10*S),"old same-timestamp receipt cannot satisfy a new opening");
  r.clear();r.observe(1,4,empty,6*S);check(!r.emptyAfterOpening(open,4,6*S,10*S),"explicit clear invalidates opening generation even after same-ID packet");
  open=r.openingStamp(1,7*S);r.observe(2,4,empty,8*S);r.observe(1,4,empty,9*S);check(!r.emptyAfterOpening(open,4,9*S,10*S),"world away/back cannot reuse opening generation");
  open=r.openingStamp(1,10*S);r.observe(1,4,null,11*S);check(!r.emptyAfterOpening(open,4,11*S,10*S),"malformed cursor denied");r.observe(1,4,new ItemSnapshot("TEST","minecraft:paper","","","",1,List.of(),false),12*S);check(!r.emptyAfterOpening(open,4,12*S,10*S),"nonempty server observation denied");
  System.out.println("CursorOpeningReceiptsTest: PASS (server-assigned exact container, global revision/generation/time proof; no substitution for later action sequence stamps)");
 }
}
