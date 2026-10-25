package dev.kuan.kuanperion.automation.greenhouse;
import java.util.*;
public final class GreenhousePolicyTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var p=GreenhousePolicy.defaults();check(!p.configured()&&p.plots().isEmpty()&&p.kinds().isEmpty()&&p.toolUuid().isEmpty(),"no default plot/crop/tool authority");
  p=new GreenhousePolicy(Set.of(1),Set.of(GreenhouseIdentity.Kind.ASHWREATH),"FARMING_HOE",new UUID(0,100).toString(),25,128,240,600,true);check(p.configured(),"explicit exact crop/plot/tool config");
  for(int plot:new int[]{0,25,-1})try{new GreenhousePolicy(Set.of(plot),p.kinds(),p.toolId(),p.toolUuid(),25,128,240,600,true);throw new AssertionError("unbounded plot");}catch(IllegalArgumentException expected){}
  for(String uuid:List.of("tool name","; command","bad"))try{new GreenhousePolicy(p.plots(),p.kinds(),p.toolId(),uuid,25,128,240,600,true);throw new AssertionError("name is not tool UUID");}catch(IllegalArgumentException expected){}
  System.out.println("GreenhousePolicyTest: PASS (off/unconfigured authority, typed plot/crop/exact tool, bounded scan/target/deadline/interval)");
 }
}
