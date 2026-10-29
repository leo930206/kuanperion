package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PetGeorgeServerCloseTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static PetGeorgeTaskTest.H selling(){var h=new PetGeorgeTaskTest.H();h.start();h.task.tick(3*S);check(h.task.state()==PetGeorgeTask.State.SELLING,"pending exact sale");return h;}
 static void closed(PetGeorgeTaskTest.H h){h.menu=null;h.identity=new Object();h.context=new AutomationContext(1,true,true,Set.of());}
 static void delivery(PetGeorgeTaskTest.H h){for(int i=0;i<43;i++)h.items.observe(1,0,i,h.inventory.get(i));h.cursors.observe(1,0,PetSalePreflightTest.AIR,4*S);h.task.onSystemMessage(1,"You sold [Lvl 1] Slug to George for 200 coins!",4*S);}
 public static void main(String[]args){
  var h=selling();var ticket=h.task.beforeServerClose(7,3*S);check(ticket!=null,"ticket only while exact private native sale scope is open");closed(h);h.task.afterServerClose(ticket,3*S);h.task.tick(4*S);check(h.task.state()==PetGeorgeTask.State.SELLING&&h.closes==0,"applied server close is not a sale receipt or permission to restart Farm");delivery(h);h.task.tick(4*S);check(h.task.state()==PetGeorgeTask.State.COMPLETE&&!h.session.active()&&h.stops==0&&h.closes==0,"scoped GAME, all43 fresh canonical after sale and empty window0 cursor can confirm actual server-close path");
  h=selling();closed(h);delivery(h);h.task.tick(4*S);check(h.task.state()!=PetGeorgeTask.State.COMPLETE,"local GUI absence plus receipt lookalikes has no applied server-close ticket");
  h=selling();ticket=h.task.beforeServerClose(7,3*S);closed(h);h.task.afterServerClose(ticket,3*S);delivery(h);h.items.clear();h.task.tick(4*S);check(h.task.state()==PetGeorgeTask.State.SELLING,"missing canonical inventory cannot prove a sold rather than parked/returned pet");h.task.cancel(5*S);
  h=selling();check(h.task.beforeServerClose(8,3*S)==null,"ignored or foreign close ID not ticket");ticket=h.task.beforeServerClose(7,3*S);h.task.cancel(3*S);closed(h);h.task.afterServerClose(ticket,4*S);delivery(h);h.task.tick(4*S);check(h.task.state()==PetGeorgeTask.State.CANCELLED&&h.stops==1,"old ticket cannot revive cancelled sale");
  var r=new CursorReceipts();r.observe(1,7,PetSalePreflightTest.AIR,0);var ordinary=r.stamp(1,7,0);var passive=r.observationStamp(1,0);r.observe(1,7,PetSalePreflightTest.AIR,S);check(r.emptyAfter(ordinary,S,S),"passive cross-window stamp must not invalidate the direct-container proof");check(!r.emptyAfterWindowZero(passive,S,S),"nonzero menu not a closed inventory cursor");r.observe(1,0,PetSalePreflightTest.AIR,2*S);check(r.emptyAfterWindowZero(passive,2*S,S)&&!r.emptyAfterOpening(passive,0,2*S,S),"window0 has a separate explicit query, old opening contract unchanged");r.clear();r.observe(1,0,PetSalePreflightTest.AIR,3*S);check(!r.emptyAfterWindowZero(passive,3*S,S),"clear invalidates closed-window query too");
  System.out.println("PetGeorgeServerCloseTest: PASS");
 }
}
