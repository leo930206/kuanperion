package dev.kuan.kuanperion.automation.visitor;
import java.util.*;
public final class VisitorQueueTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  var q=VisitorQueue.parse(List.of("Garden","§eVisitors: (2)","§aLibrarian (UNCOMMON) NEW!","Farmer","Next Visitor: 1m 2s"));check(q.known()&&q.count()==2&&q.entries().get(0).name().equals("Librarian")&&q.entries().get(0).rarity()==VisitorQueue.Rarity.UNCOMMON&&q.entries().get(0).firstTime()&&q.entries().get(1).rarity()==VisitorQueue.Rarity.UNKNOWN,"bounded exact section/name metadata");
  check(VisitorQueue.parse(List.of("Visitors: 0","Next Visitor: 10s")).known(),"known empty is distinct from unknown");
  for(var lines:List.of(List.of("Other: 2","Librarian","Farmer"),List.of("Visitors: (2)","Librarian",""),List.of("Visitors: (1)","Librarian","Farmer","Next Visitor: 1s"),List.of("Visitors: (2)","Librarian","Librarian"),List.of("Visitors: (1)","Librarian","Visitors: (1)","Librarian"),List.of("Visitors: (6)","Farmer"),List.of("Visitors: (-1)"),List.of("Visitors: (1)","Librarian (FAKE)"),List.of("Visitors: (1)","Libr\u200barian"),List.of("Visitors: (1)","Player: Librarian")))check(!VisitorQueue.parse(lines).known(),"incomplete/conflicting/spoofed section is unknown: "+lines);
  check(VisitorQueue.parse(List.of("Visitors: (2)\nLibrarian\nFarmer\nNext Visitor: 1s")).known(),"bounded multiline native entries");
  check(!VisitorQueue.parse(Collections.nCopies(129,"x")).known()&&!VisitorQueue.parse(List.of("x".repeat(4097))).known(),"input bounds, no truncated known queue");
  try{q.entries().clear();throw new AssertionError("mutable queue");}catch(UnsupportedOperationException expected){}
  System.out.println("VisitorQueueTest: PASS (exact bounded section/count/name metadata, complete unique queue only, unknown is not empty, no foreign/duplicate/oversized inference)");
 }
}
