package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationMenuTransitionTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static MenuSnapshot menu(String name,int id){return new MenuSnapshot(1,id,1,name,54,List.of());}
 public static void main(String[] args){
  var safe=new AutomationContext(1,true,true,Set.of());AutomationContext[] context={safe};MenuSnapshot[] visible={null};int[] stops={0};
  var session=new AutomationSession(new AutomationCoordinator(),()->context[0],()->{},()->stops[0]++);
  var from=new MenuContract(Set.of("Bazaar ➜ Wheat"),54);var to=new MenuContract(Set.of("Wheat ➜ Instant Buy"),54);
  var h=session.begin(AutomationFeature.VISITOR,0,100_000_000_000L);session.expectMenu(h,from,()->visible[0]);visible[0]=menu("Bazaar ➜ Wheat",4);context[0]=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(1);
  check(!session.transitionMenu(h,visible[0],from,from,2,5_000_000_000L),"same contract cannot bypass same-title-container ownership");
  check(!session.transitionMenu(h,visible[0],from,new MenuContract(Set.of("Bazaar ➜ Wheat","Wheat ➜ Instant Buy"),54),2,5_000_000_000L),"overlapping aliases cannot adopt a foreign same-title container");
  check(session.transitionMenu(h,visible[0],from,to,2,5_000_000_000L),"explicit transition registered before own click");
  session.poll(3);check(session.active(),"old exact container allowed while server processes click");
  visible[0]=null;context[0]=safe;session.poll(4);check(session.active(),"bounded close/open gap allowed");
  visible[0]=menu("Wheat ➜ Instant Buy",5);context[0]=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(5);check(session.active(),"only contracted next menu can be claimed");
  visible[0]=menu("Wheat ➜ Instant Buy",6);session.poll(6);check(!session.active()&&stops[0]==1,"after transition new same-title container is not re-adopted");
  context[0]=safe;visible[0]=null;h=session.begin(AutomationFeature.VISITOR,10,100_000_000_000L);session.expectMenu(h,from,()->visible[0]);visible[0]=menu("Bazaar ➜ Wheat",7);context[0]=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(11);
  check(!session.transitionMenu(h,menu("Bazaar ➜ Wheat",99),from,to,12,100),"unobserved origin not accepted");
  check(session.transitionMenu(h,visible[0],from,to,12,100),"second transition");visible[0]=menu("Inventory",8);session.poll(13);check(!session.active()&&stops[0]==2,"unknown GUI interrupts; no guessing");
  context[0]=safe;visible[0]=null;h=session.begin(AutomationFeature.VISITOR,20,100_000_000_000L);session.expectMenu(h,from,()->visible[0]);visible[0]=menu("Bazaar ➜ Wheat",9);context[0]=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(21);check(session.transitionMenu(h,visible[0],from,to,22,100),"timeout fixture");session.poll(122);check(!session.active()&&stops[0]==3,"transition deadline never extends owner lifetime");
  context[0]=safe;visible[0]=null;h=session.begin(AutomationFeature.VISITOR,200,100_000_000_000L);session.expectMenu(h,from,()->visible[0]);visible[0]=menu("Bazaar ➜ Wheat",9);context[0]=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(201);check(session.transitionMenu(h,visible[0],from,to,202,100),"foreign screen fixture");visible[0]=null;session.poll(203);check(!session.active(),"non-container GUI is not a close/open gap");
  check(!session.transitionMenu(h,menu("Bazaar ➜ Wheat",9),from,to,204,100),"stale owner cannot schedule transition");
  System.out.println("AutomationMenuTransitionTest: PASS (explicit before-click transition, bounded gap, exact old/new identity, unknown/manual/stale/deadline rejection)");
 }
}
