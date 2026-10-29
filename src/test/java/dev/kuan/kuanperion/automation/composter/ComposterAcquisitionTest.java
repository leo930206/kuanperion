package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class ComposterAcquisitionTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static ComposterTaskTest.H buying(){var h=new ComposterTaskTest.H();h.f.consumed=true;h.f.acquireAllowed=true;var stock=new ComposterBuying(Map.of("BOX_OF_SEEDS",new ComposterBuying.Stock(2,new BazaarAcquisitionPlan.Material("minecraft:paper",64,1000))),1000);check(h.task.request(ComposterTaskTest.POLICY.withBuying(stock),true,0),"explicit policy");h.task.tick(0);h.arrive(S,0);for(int i=2;i<9&&h.f.opens==0;i++)h.task.tick(i*S);h.f.menu(1000);for(int i=9;i<15&&h.f.acquisitions==0;i++)h.task.tick(i*S);check(h.task.state()==ComposterTask.State.ACQUIRING&&h.f.acquisitions==1&&h.f.deposits==0,"only missing configured supplies start child, no blind refill");return h;}
 public static void main(String[]args){
  var h=buying();check(h.session.holdsFarming()&&h.f.pauses==1&&h.f.budget.limit()==1000&&h.f.closes==1,"same original Farm hold and one run budget after owned close");h.task.onSystemMessage(1,"[Bazaar] Bought 2x Box of Seeds for 100 coins!");check(h.f.messages==1,"GAME reaches child only while acquiring");h.f.acquisition=ComposterTask.Acquisition.COMPLETE;h.f.consumed=false;h.task.tick(15*S);for(int i=16;i<20&&h.f.opens<2;i++)h.task.tick(i*S);check(h.f.opens==2,"same machine reopened after verified acquisition");h.f.menu(1000);for(int i=20;i<24&&h.f.deposits==0;i++)h.task.tick(i*S);check(h.f.deposits==1&&h.f.acquisitions==1,"normal protected deposit is revalidated, not inferred from buy");h.task.cancel(24*S);
  h=buying();h.f.acquisition=ComposterTask.Acquisition.UNCERTAIN;h.task.tick(15*S);check(!h.task.active()&&h.f.stops==1&&h.f.deposits==0,"unknown spend stops without retry or Farm restart");
  h=buying();h.f.manual=true;h.task.tick(15*S);check(!h.task.active()&&h.f.cancelAcquisitions>0&&h.f.deposits==0,"manual takeover cancels acquisition before return");
  h=buying();h.f.acquisition=ComposterTask.Acquisition.COMPLETE;h.task.tick(15*S);for(int i=16;i<20&&h.f.opens<2;i++)h.task.tick(i*S);h.f.menu(1000);for(int i=20;i<26&&h.f.commands.size()<2;i++)h.task.tick(i*S);check(h.f.acquisitions==1&&h.f.deposits==0&&h.f.commands.size()==2,"no item after declared completion cannot repeat purchasing same resource");h.task.cancel(26*S);
  System.out.println("ComposterAcquisitionTest: PASS");
 }
}
