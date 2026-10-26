package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationSessionPauseCallbackTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){for(boolean throwAfter:new boolean[]{false,true}){
  var coordinator=new AutomationCoordinator();AutomationSession[] ref=new AutomationSession[1];AutomationSession.Handle[] newer=new AutomationSession.Handle[1];boolean[] replace={true};int[] stopped={0};
  ref[0]=new AutomationSession(coordinator,()->new AutomationContext(1,true,true,Set.of()),()->{if(replace[0]){replace[0]=false;ref[0].invalidate();newer[0]=ref[0].begin(AutomationFeature.AUTO_SELL,1,60_000_000_000L);if(throwAfter)throw new IllegalStateException("old pause failed");}},()->stopped[0]++);
  var result=ref[0].begin(AutomationFeature.PEST,0,60_000_000_000L);
  check(result==null&&ref[0].isCurrent(newer[0])&&ref[0].holdsFarming()&&stopped[0]==0,"old pause callback must not return/cancel a newer owner's handle: "+throwAfter);
 }System.out.println("AutomationSessionPauseCallbackTest: PASS (reentrant successful/failing pause callback cannot lend or cancel a newer root)");}
}
