package dev.kuan.kuanperion.automation;
public final class PlayerPositionReceiptsTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  var r=new PlayerPositionReceipts();var p=new PlayerPositionReceipts.Position(10.5,75,-20.25);var first=r.stamp(1,100);
  check(r.since(first,100,1000).isEmpty(),"no local position/snapshot as server receipt");r.observe(1,8,p,true,101);
  check(r.since(first,102,1000).orElseThrow().position().equals(p),"new absolute server packet in the same world");
  var next=r.stamp(1,103);check(r.since(next,104,1000).isEmpty(),"old packet cannot satisfy later command");r.observe(1,8,p,true,105);check(r.since(next,106,1000).isEmpty(),"duplicate teleport ID is not a new acknowledgement");
  r.observe(1,-72,p,true,107);check(r.since(next,108,1000).orElseThrow().teleportId()==-72,"protocol ID opaque, not assumed positive/monotonic");
  r.observe(1,3,p,false,109);check(r.since(next,110,1000).isEmpty(),"relative position depends on local prediction and clears previous proof");
  r.observe(1,4,p,true,111);check(r.since(next,110,1000).isEmpty()&&r.since(next,1112,1000).isEmpty(),"future/stale receipt rejected");
  var old=r.stamp(1,120);r.clear();r.observe(1,5,p,true,121);check(r.since(old,122,1000).isEmpty(),"cleared epoch invalidates old request even with same numeric world");
  var world=r.stamp(1,125);r.observe(2,6,p,true,126);check(r.since(world,127,1000).isEmpty(),"world switch invalidates request");
  var current=r.stamp(2,128);r.observe(2,7,null,true,129);check(r.since(current,130,1000).isEmpty(),"missing position never leaves usable old proof");
  for(double x:new double[]{Double.NaN,Double.POSITIVE_INFINITY,30_000_001})try{new PlayerPositionReceipts.Position(x,70,0);throw new AssertionError("bad position accepted");}catch(IllegalArgumentException expected){}
  var bounded=r.stamp(2,200);for(int id=100;id<180;id++)r.observe(2,id,p,true,201+id);check(r.cachedIds()<=32&&r.since(bounded,400,1000).isPresent(),"bounded replay cache with no history growth");
  System.out.println("PlayerPositionReceiptsTest: PASS (server-only absolute evidence, sequence/world/epoch/freshness, opaque IDs, replay/relative/malformed rejection, bounded memory)");
 }
}
