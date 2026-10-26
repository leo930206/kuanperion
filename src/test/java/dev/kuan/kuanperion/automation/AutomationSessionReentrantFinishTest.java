package dev.kuan.kuanperion.automation;
public final class AutomationSessionReentrantFinishTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
  var h=new AutomationSessionObservationTest.H();h.start(AutomationFeature.PEST);var ref=h;h.contextHook=ref::replace;
  check(!h.session.finish(h.h,true,3)&&h.session.isCurrent(h.newer)&&h.session.holdsFarming()&&h.stops==0,"old finish observation must not erase the newer root/hold");
  h=new AutomationSessionObservationTest.H();h.start(AutomationFeature.PEST);var failed=h;h.contextHook=()->{failed.replace();throw new IllegalStateException("old context unavailable after new owner");};
  check(!h.session.finish(h.h,false,3)&&h.session.isCurrent(h.newer)&&h.session.holdsFarming()&&h.stops==0,"old failing finish observation cannot erase a newer owner");
  System.out.println("AutomationSessionReentrantFinishTest: PASS (successful/failing context callbacks cannot clear or stop a newer root or its Farming hold)");
 }
}
