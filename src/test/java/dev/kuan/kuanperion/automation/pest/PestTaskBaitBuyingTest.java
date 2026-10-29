package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestTaskBaitBuyingTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class P extends PestTaskBaitTest.P {
  int buys,cancels;boolean delivered,uncertain;AutomationSession.Handle parent;SpendingBudget budget;
  P(){h.inv.put(9,PestBaitButtonViewTest.AIR);}
  public boolean beginBaitAcquisition(AutomationSession.Handle root,PestBaitPolicy policy,SpendingBudget coins,int remaining,long n){check(root.lease().owner()==AutomationFeature.PEST&&h.session.isCurrent(root)&&h.session.holdsFarming(),"purchase keeps original Pest root/Farming hold");if(parent!=null)check(parent.equals(root)&&budget==coins,"another trap cannot mint a fresh parent or budget");parent=root;budget=coins;buys++;return true;}
  public PestTask.Acquisition pollBaitAcquisition(AutomationSession.Handle root,long n){check(root.equals(parent),"same original buyer");if(uncertain)return PestTask.Acquisition.UNCERTAIN;if(!delivered)return PestTask.Acquisition.RUNNING;h.inv.put(9,PestBaitButtonPlanTest.bait(16));return PestTask.Acquisition.COMPLETE;}
  public boolean ownsBaitAcquisitionScreen(){return parent!=null;}
  public void cancelBaitAcquisition(){cancels++;}
 }
 static PestPolicy policy(){var p=PestTaskBaitTest.policy();return p.withBait(p.bait().withBuying(new PestBaitBuying("minecraft:paper",64,16,1000)));}
 static PestTask start(P p){var t=PestTaskBaitTest.task(p);check(t.request(policy(),true,p.h.now),"explicit budgeted shortage may start without existing bait");for(int i=0;i<6&&p.buys==0;i++){p.h.now+=100_000_000L;t.tick(p.h.now);}check(p.buys==1&&p.opens==0,"buy shortage before opening trap, not after picking up anything");return t;}
 public static void main(String[]args){
  var p=new P();var t=PestTaskBaitTest.task(p);check(!t.request(PestTaskBaitTest.policy(),true,p.h.now)&&p.buys==0,"no separate purchase consent means no shortage task or spend");t=start(p);var original=p.parent;var budget=p.budget;p.delivered=true;p.h.now+=100_000_000L;t.tick(p.h.now);PestTaskBaitTest.opened(p,t);check(p.parent.equals(original)&&p.budget==budget&&p.buys==1&&p.h.session.isCurrent(original),"delivered bait returns to same parent and guarded existing transfer");
  p.received(16,true);t.tick(p.h.now);p.h.traps=new PestTrapObservation(Set.of(),true,Set.of(),true);p.received(12,false);t.tick(p.h.now);p.received(0,true);for(int i=0;i<12&&t.active();i++){p.h.now+=100_000_000L;t.tick(p.h.now);}check(t.state()==PestTask.State.COMPLETE&&p.buys==1&&p.closes==1&&!p.h.session.holdsFarming(),"buy/refill/own remainder/original return complete as one bounded run");
  p=new P();t=start(p);p.uncertain=true;p.h.now+=100_000_000L;t.tick(p.h.now);check(t.state()==PestTask.State.FAILED&&p.buys==1&&p.clicks==0&&p.opens==0&&p.cancels>0,"uncertain purchase stops without retry, trap interaction or Farm restart");
  p=new P();t=start(p);p.manual=true;t.tick(p.h.now);check(t.state()==PestTask.State.CANCELLED&&p.buys==1&&p.opens==0&&p.cancels>0,"manual takeover cancels buyer before another action");
  System.out.println("PestTaskBaitBuyingTest: PASS");
 }
}
