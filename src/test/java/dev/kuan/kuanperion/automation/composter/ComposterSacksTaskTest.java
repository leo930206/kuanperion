package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class ComposterSacksTaskTest {
 static final long S=1_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static ComposterTaskTest.H start(long cap){var h=new ComposterTaskTest.H();h.f.consumed=true;h.f.sacksAllowed=true;var p=ComposterTaskTest.POLICY.withMaterials(Map.of()).withTargetPercent(100).withSacksUnits(cap);check(h.task.request(p,true,0),"explicit Sacks-only consent with known machine");h.task.tick(0);h.arrive(S,0);for(int i=2;i<9&&h.f.opens==0;i++)h.task.tick(i*S);h.f.menu(1000);for(int i=9;i<15&&h.f.sacksStarts==0;i++)h.task.tick(i*S);return h;}
 public static void main(String[]args){
  var h=start(10000);check(h.task.state()==ComposterTask.State.SACKS&&h.f.sacksStarts==1&&h.f.sacksCeiling==10000&&h.f.closes==0&&h.session.holdsFarming(),"one capacity-charged source under same hold, no client close");
  h.f.sacksResult=ComposterTask.Acquisition.COMPLETE;h.f.menu(7000);h.task.tick(15*S);check(h.task.sacksInserted()==6000&&h.task.state()==ComposterTask.State.FILLING,"verified Sacks returned menu rebinds normal task");for(int i=16;i<20&&h.f.commands.size()<2;i++)h.task.tick(i*S);check(h.f.sacksStarts==1&&h.f.commands.size()==2,"no retry same pool; remaining run ceiling cannot cover fuel");h.task.cancel(20*S);
  h=start(9999);check(h.f.sacksStarts==0&&h.f.commands.size()==2,"insufficient worst-case capacity budget never navigates Sacks");h.task.cancel(20*S);
  h=start(20000);h.f.sacksResult=ComposterTask.Acquisition.UNCERTAIN;h.task.tick(15*S);check(!h.task.active()&&h.f.sacksCancels>0&&h.f.closes==0&&h.f.stops==1,"unknown Sacks consumption retains GUI and never re-arms");
  h=start(20000);h.f.manual=true;h.task.tick(15*S);check(!h.task.active()&&h.f.sacksCancels>0&&h.f.restores==0,"manual takeover stops before next source");
  System.out.println("ComposterSacksTaskTest: PASS");
 }
}
