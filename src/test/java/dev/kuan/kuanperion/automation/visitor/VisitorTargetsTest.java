package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
public final class VisitorTargetsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static VisitorTargets.Fact fact(int id,VisitorTargets.Kind kind,String name,double x,double y,boolean listed){return new VisitorTargets.Fact(id,new UUID(0,id),kind,new Point(x,y,0),name,true,kind==VisitorTargets.Kind.MARKER,listed);}
 public static void main(String[]args){
  var q=VisitorQueue.parse(List.of("Visitors: (1)","Librarian"));var marker=fact(1,VisitorTargets.Kind.MARKER,"Librarian",0,73,false);var npc=fact(2,VisitorTargets.Kind.CHARACTER,"NPC",0,72,false);
  var selected=VisitorTargets.select(q,List.of(marker,npc));check(selected.size()==1&&selected.get(0).id()==2&&selected.get(0).markerId()==1&&selected.get(0).visitor().equals("Librarian"),"exact queue marker + sole nearby non-player character, stable dual UUIDs");
  check(VisitorTargets.select(q,List.of(marker)).isEmpty(),"missing character not guessed from marker");
  check(VisitorTargets.select(q,List.of(marker,fact(2,VisitorTargets.Kind.CHARACTER,"NPC",0,72,true))).isEmpty(),"listed player cannot impersonate visitor");
  check(VisitorTargets.select(q,List.of(marker,npc,fact(3,VisitorTargets.Kind.CHARACTER,"Other",1,72,false))).isEmpty(),"ambiguous character is not nearest or first match");
  check(VisitorTargets.select(q,List.of(marker,npc,fact(3,VisitorTargets.Kind.MARKER,"Librarian",1,73,false))).isEmpty(),"duplicate exact marker denies target");
  check(VisitorTargets.select(q,List.of(fact(1,VisitorTargets.Kind.MARKER,"Librarian Jr",0,73,false),npc)).isEmpty(),"no substring/name folding");
  check(VisitorTargets.select(q,List.of(marker,fact(2,VisitorTargets.Kind.CHARACTER,"NPC",4,72,false))).isEmpty(),"bounded marker association");
  check(VisitorTargets.select(q,List.of(marker,npc,npc)).isEmpty()&&VisitorTargets.select(VisitorQueue.unknown(),List.of(marker,npc)).isEmpty(),"duplicate IDs/UUID or unknown Tab do not select");
  var duplicateUuid=new VisitorTargets.Fact(3,npc.uuid(),VisitorTargets.Kind.OTHER,new Point(10,72,0),"",true,false,false);check(VisitorTargets.select(q,List.of(marker,npc,duplicateUuid)).isEmpty(),"even noncandidate identity ambiguity fails closed");
  var pair=VisitorQueue.parse(List.of("Visitors: (2)","Librarian","Farmer"));check(VisitorTargets.select(pair,List.of(marker,npc,fact(3,VisitorTargets.Kind.MARKER,"Farmer",1,73,false))).isEmpty(),"one character cannot be two queued visitors");
  check(VisitorTargets.select(q,List.of(marker,fact(2,VisitorTargets.Kind.CHARACTER,"NPC",0,74,false))).isEmpty(),"character cannot be above its hologram marker");
  var mutable=new ArrayList<>(selected);try{selected.clear();throw new AssertionError("mutable selected targets");}catch(UnsupportedOperationException expected){}
  check(VisitorTargets.select(q,Collections.nCopies(257,marker)).isEmpty(),"bounded candidate data, no partial trusted prefix");
  var visible=new VisitorTargets.Fact(1,marker.uuid(),VisitorTargets.Kind.MARKER,marker.position(),marker.name(),true,false,false);check(VisitorTargets.select(q,List.of(visible,npc)).isEmpty(),"visible renamed armor stand is not an invisible server marker");
  var dead=new VisitorTargets.Fact(2,npc.uuid(),npc.kind(),npc.position(),npc.name(),false,false,false);check(VisitorTargets.select(q,List.of(marker,dead)).isEmpty(),"removed/dead candidate not interactive");
  System.out.println("VisitorTargetsTest: PASS (complete queue + exact unique invisible marker + sole nearby living non-listed character, stable dual identity, no first/nearest/substring/player ambiguity)");
 }
}
