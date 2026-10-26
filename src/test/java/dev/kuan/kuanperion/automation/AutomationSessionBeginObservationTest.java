package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationSessionBeginObservationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
  if(args.length==0)for(var feature:AutomationFeature.values())for(boolean nullValue:new boolean[]{false,true}){
   int[] hooks={0,0};var s=new AutomationSession(new AutomationCoordinator(),()->{if(nullValue)return null;throw new IllegalStateException("unavailable");},()->hooks[0]++,()->hooks[1]++);
   check(s.begin(feature,0,60_000_000_000L)==null&&!s.active()&&!s.holdsFarming()&&hooks[0]==0&&hooks[1]==0,"read failure before acquire is inert: "+feature+" / "+nullValue);
  }
  // A synchronous observation may invalidate the old world without ever installing a replacement.
  for(boolean replaces:new boolean[]{false,true}){
   AutomationSession[] ref=new AutomationSession[1];AutomationSession.Handle[] newer=new AutomationSession.Handle[1];boolean[] once={true};int[] hooks={0,0};
   ref[0]=new AutomationSession(new AutomationCoordinator(),()->{if(once[0]){once[0]=false;ref[0].invalidate();if(replaces)newer[0]=ref[0].begin(AutomationFeature.JUNK,1,60_000_000_000L);}return new AutomationContext(1,true,true,Set.of());},()->hooks[0]++,()->hooks[1]++);
   check(ref[0].begin(AutomationFeature.AUTO_SELL,0,60_000_000_000L)==null,"invalidated observation cannot acquire from its stale context");
   check(replaces?ref[0].isCurrent(newer[0])&&hooks[0]==1:!ref[0].active()&&hooks[0]==0,"new generation/owner retained");check(hooks[1]==0,"no unwanted stop");
  }
  System.out.println("AutomationSessionBeginObservationTest: PASS (null/throwing observations and generation/owner invalidation before acquisition)");
 }
}
