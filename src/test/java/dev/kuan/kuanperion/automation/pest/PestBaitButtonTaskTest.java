package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestBaitButtonTaskTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class P implements PestBaitButtonTask.Port {
  final PestBaitButtonTransferTest.H h=new PestBaitButtonTransferTest.H();int opens,closes,clicks;boolean manual,replaced,failSend;PestBaitButtonTransfer.Step last;
  P(){h.menu=null;h.ctx=new AutomationContext(1,true,true,Set.of());}
  public AutomationContext context(){return h.ctx;}public boolean manualInput(){return manual;}
  public PestTrapRelease.Facts facts(long n){return new PestTrapRelease.Facts(1,n,0,h.traps,List.of(h.target));}
  public PestBaitButtonTransfer.Frame baitFrame(long n){return new PestBaitButtonTransfer.Frame(1,n,h.level,replaced?new Object():h.identity,h.menu,h.inv,h.carried,h.traps,List.of(h.target));}
  public PestTrapRelease.Open open(AutomationSession.Handle o,PestTrapTarget t,long n){opens++;return PestTrapRelease.Open.SENT;}
  public GuardedMenuAction.Result baitClick(AutomationSession.Handle o,PestBaitButtonTransfer.Action a,long n){check(a.claim(o,baitFrame(n),n),"fake native boundary must independently consume private action");clicks++;last=a.step();return failSend?GuardedMenuAction.Result.UNCERTAIN:GuardedMenuAction.Result.SENT;}
  public void closeOwnedMenu(int id){check(h.menu!=null&&h.menu.containerId()==id&&h.carried.count()==0,"only exact empty-cursor own close");closes++;h.menu=null;h.ctx=new AutomationContext(1,true,true,Set.of());}
  public boolean closeBaitMenu(Object identity,int id){if(identity!=h.identity||replaced)return false;closeOwnedMenu(id);return true;}
  void show(){h.now+=100_000_000L;h.refresh(h.marker);h.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));for(var s:h.menu.slots())h.items.observe(1,7,s.index(),s.item());h.cursors.observe(1,7,h.carried,h.now);}
  void received(int count,boolean source){h.now+=100_000_000L;h.carried=count==0?PestBaitButtonViewTest.AIR:PestBaitButtonPlanTest.bait(count);if(source)h.inv.put(9,count==16?PestBaitButtonViewTest.AIR:PestBaitButtonPlanTest.bait(12));h.refresh(h.marker);if(source){h.items.observe(1,0,9,h.inv.get(9));h.items.observe(1,7,27,h.inv.get(9));}h.cursors.observe(1,7,h.carried,h.now);}
 }
 static PestBaitButtonTask begin(P p){var t=new PestBaitButtonTask(p.h.session,p.h.coordinator,p.h.items,p.h.cursors,p);check(t.begin(p.h.owner,p.h.target,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of()),64,p.h.now),"begin under original Pest lease");return t;}
 static void transferring(P p,PestBaitButtonTask t){t.tick(p.h.now);check(p.opens==1,"single opener");p.show();t.tick(p.h.now);t.tick(p.h.now);check(p.last==PestBaitButtonTransfer.Step.PICKUP,"first real action picks approved source");}
 public static void main(String[]args){
  var p=new P();var t=begin(p);transferring(p,t);check(t.charged()==16,"whole withdrawn stack charged before outcome, never refunded");p.received(16,true);t.tick(p.h.now);check(p.last==PestBaitButtonTransfer.Step.DEPOSIT,"same original task deposits on nonempty role");p.h.traps=new PestTrapObservation(Set.of(),true,Set.of(),true);p.received(12,false);t.tick(p.h.now);check(p.last==PestBaitButtonTransfer.Step.RETURN,"only proven own remainder returns");p.received(0,true);t.tick(p.h.now);t.tick(p.h.now);check(t.state()==PestBaitButtonTask.State.COMPLETE&&p.closes==1&&p.clicks==3&&p.h.session.isCurrent(p.h.owner)&&t.charged()==16,"complete exact refill and close retain original root/Farming hold");
  p=new P();t=begin(p);transferring(p,t);p.received(16,true);p.manual=true;t.tick(p.h.now);check(t.state()==PestBaitButtonTask.State.CANCELLED&&p.closes==0&&p.clicks==1&&p.h.carried.count()==16,"manual takeover never repairs carried bait or closes menu");
  p=new P();t=begin(p);transferring(p,t);p.replaced=true;t.tick(p.h.now);check(!t.active()&&p.closes==0&&p.clicks==1,"same numeric container/new identity cannot inherit ownership");
  p=new P();p.failSend=true;t=begin(p);transferring(p,t);check(!t.active()&&p.opens==1&&p.clicks==1&&p.closes==0,"possibly sent input is not retried or cleaned up blindly");
  p=new P();t=begin(p);t.tick(p.h.now);p.show();p.h.inv.put(42,PestBaitButtonPlanTest.bait(1));t.tick(p.h.now);check(!t.active()&&p.clicks==0&&p.closes==0,"pre-transfer unrelated inventory drift is inert");
  p=new P();t=begin(p);p.h.session.cancel(p.h.now);t.tick(p.h.now);check(!t.active()&&p.opens==0&&p.clicks==0,"revoked original owner never opens");
  p=new P();t=begin(p);p.h.now+=41_000_000_000L;t.tick(p.h.now);check(t.state()==PestBaitButtonTask.State.TIMED_OUT&&p.opens==0,"bounded time before effects");
  System.out.println("PestBaitButtonTaskTest: PASS");
 }
}
