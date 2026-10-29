package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.nio.file.*;
public final class ComposterBuyingTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var nativeItem=new BazaarAcquisitionPlan.Material("minecraft:wheat_seeds",64,1000);var buying=new ComposterBuying(Map.of("BOX_OF_SEEDS",new ComposterBuying.Stock(2,nativeItem)),1000);
  var p=ComposterPolicy.defaults().withMaterials(Map.of("BOX_OF_SEEDS",new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,25000))).withBuying(buying);
  check(p.withTargetPercent(80).buying().equals(buying)&&p.withInterval(600).buying().equals(buying),"other policy edits preserve purchasing consent");
  check(ComposterPolicy.defaults().buying().maximumCoins()==0&&ComposterPolicy.defaults().buying().stock().isEmpty(),"old/new defaults have zero purchasing authority");
  try{ComposterPolicy.defaults().withBuying(buying);throw new AssertionError("buying unauthorized machine material");}catch(IllegalArgumentException expected){}
  Path dir=Files.createTempDirectory("composter-buying-");try{var path=dir.resolve("automation.json");var store=AutomationSettingsStore.load(path);store.setComposterPolicy(p);check(AutomationSettingsStore.load(path).composterPolicy().equals(p),"purchasing choices survive JAR/config reload");}finally{try(var paths=Files.walk(dir)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
  for(int bad:List.of(0,4097))try{new ComposterBuying.Stock(bad,nativeItem);throw new AssertionError("unbounded stock");}catch(IllegalArgumentException expected){}
  System.out.println("ComposterBuyingTest: PASS");
 }
}
