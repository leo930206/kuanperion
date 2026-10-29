package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationScreenBindingTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H {
  boolean shown,valid=true,farm=true;int stopped;Runnable hook;
  AutomationCoordinator c=new AutomationCoordinator();
  AutomationContext context(){return new AutomationContext(1,farm,true,shown?Set.of(AutomationContext.Blocker.MENU):Set.of());}
  AutomationSession s=new AutomationSession(c,this::context,()->{},()->{stopped++;farm=false;});
  AutomationSession.Handle h=s.begin(AutomationFeature.VISITOR,0,100_000_000_000L),next;
  boolean observe(){if(hook!=null){var r=hook;hook=null;r.run();}return valid;}
  void bind(long now){check(s.expectScreenSequence(h,this::observe,now,10_000_000_000L),"binding");}
 }
 public static void main(String[]args){
  H h=new H();h.bind(1);h.shown=true;h.s.poll(2);check(h.s.isCurrent(h.h),"explicit owned menu or sign allowed without replacing root");
  h.valid=false;h.s.poll(3);check(!h.s.active()&&h.stopped==1,"unowned screen revokes original hold");
  h=new H();h.bind(1);h.s.poll(10_000_000_001L);check(!h.s.active(),"bounded ownership expires even without MENU");
  h=new H();h.bind(1);h.s.expectMenu(h.h,new MenuContract(Set.of("Visitor"),27),()->null);h.shown=true;h.s.poll(2);check(!h.s.active(),"regular menu binding replaces screen sequence");
  h=new H();h.bind(1);h.s.clearExpectedMenu(h.h);h.shown=true;h.s.poll(2);check(!h.s.active(),"clearing removes screen sequence too");
  h=new H();h.bind(1);var replaced=h;h.hook=()->{replaced.s.invalidate();replaced.shown=false;replaced.next=replaced.s.begin(AutomationFeature.COMPOSTER,2,50_000_000_000L);};h.valid=false;h.s.poll(3);check(h.s.isCurrent(h.next)&&h.stopped==0,"stale screen reader cannot cancel newer root");
  h=new H();h.bind(1);var rebound=h;h.hook=()->rebound.bind(3);h.valid=false;h.s.poll(3);check(h.s.isCurrent(h.h),"stale reader cannot cancel new binding of same root");h.s.poll(4);check(!h.s.active(),"new binding validated next poll");
  h=new H();check(!h.s.expectScreenSequence(h.h,()->true,0,10_000_000_001L),"cannot grant unbounded screen permission");
  h=new H();h.bind(1);h.hook=()->{throw new IllegalStateException("screen unavailable");};h.s.poll(3);check(!h.s.active()&&h.stopped==1,"unknown screen stops original run");
  h=new H();h.bind(1);h.s.invalidate();check(!h.s.expectScreenSequence(h.h,()->true,2,1),"stale owner cannot rebind");
  System.out.println("AutomationScreenBindingTest: PASS");
 }
}
