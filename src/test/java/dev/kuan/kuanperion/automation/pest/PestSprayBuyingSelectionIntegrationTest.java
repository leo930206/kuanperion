package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayBuyingSelectionIntegrationTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class B extends PestSpraySelectionIntegrationTest.P {
  int buys,polls,buyerCancels;SpendingBudget cap;
  B(){inv.put(9,PestSprayPlanTest.AIR);}
  public boolean beginSprayAcquisition(AutomationSession.Handle h,PestSprayPolicy p,PestSprayBuying b,SpendingBudget budget,long n){check(h.equals(actualRoot)&&session.holdsFarming()&&b.maximumCoins()==1000,"original parent root and separate consent");buys++;cap=budget;return true;}
  public PestSprayTask.Acquisition pollSprayAcquisition(AutomationSession.Handle h,long n){check(h.equals(actualRoot)&&cap.limit()==1000,"original exact budget/root retained");if(++polls<2)return PestSprayTask.Acquisition.RUNNING;inv=new HashMap<>(inv);inv.put(9,PestSprayPlanTest.material(16));items.observe(1,0,9,inv.get(9));return PestSprayTask.Acquisition.COMPLETE;}
  public void cancelSprayAcquisition(){buyerCancels++;}
  @Override public PestSprayTask.Effect sprayEffect(AutomationSession.Handle h,PestSprayTask.Action a,long n){if(a.kind()!=PestSprayTask.Kind.USE)return super.sprayEffect(h,a,n);if(!a.claim(h,motion(n),n))return PestSprayTask.Effect.REJECTED;uses++;inv=new HashMap<>(inv);inv.put(9,PestSprayPlanTest.material(13));items.observe(1,0,9,inv.get(9));parent.onSystemMessage(1,"Sprayonator! You sprayed plot 1!",n);spray=new PestSprayObservation(PestSprayObservation.State.ACTIVE);applyTab();return PestSprayTask.Effect.SENT;}
 }
 public static void main(String[]args){
  var b=new B();check(b.parent.request(PestSprayPlanTest.policy(),new PestSprayBuying(16,1000),b.now),"missing material + None-selected tool may enter separately authorized original-root buyer");for(int i=0;i<60&&b.parent.active();i++)b.tick();check(b.parent.state()==PestSprayTask.State.COMPLETE&&b.buys==1&&b.opens==1&&b.clicks==1&&b.closes==1&&b.uses==1&&b.equips==1&&b.restores==1&&b.buyerCancels==1&&b.demands==1&&b.disposals==1&&b.stops==0&&!b.session.active(),"buy -> canonical target -> fresh None -> equip -> actual selection child/paired metadata/GAME/close -> fresh None -> use/receipts -> restore under one root "+b.parent.state());
  b=new B();check(b.parent.request(PestSprayPlanTest.policy(),new PestSprayBuying(16,1000),b.now),"manual test setup");for(int i=0;i<30&&b.parent.active()&&b.clicks==0;i++)b.tick();b.manual=true;for(int i=0;i<4&&b.parent.active();i++)b.tick();check(!b.parent.active()&&b.buys==1&&b.uses==0&&b.restores==0&&b.stops==1&&b.buyerCancels==1,"manual takeover after purchase never repeats buy, uses material or repairs/restores over user");
  System.out.println("PestSprayBuyingSelectionIntegrationTest: PASS real original parent/selection/receipts and controlled buying edge; not live server");
 }
}
