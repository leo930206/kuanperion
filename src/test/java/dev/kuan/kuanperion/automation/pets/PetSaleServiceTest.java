package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.nio.file.*;
public final class PetSaleServiceTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var dir=Files.createTempDirectory("pet-service-");try{var settings=AutomationSettingsStore.load(dir.resolve("automation.json"));var f=new PetGeorgeTaskTest.H();boolean[] closed={true};int[] factories={0};var notices=new ArrayList<StashService.Notice>();var service=new PetSaleService(settings,f.coordinator,f.session,f.items,f.cursors,f::context,()->{factories[0]++;return f;},()->closed[0],notices::add);
   service.tick(0);check(!service.autoEnabled()&&factories[0]==0&&!service.manualRequest(0),"default off/empty policy creates no driver or root");service.setPolicy(PetSalePolicy.defaults().withAllowed(Set.of(new PetSalePreflight.Kind("SLUG","EPIC"))).withMinimumPayout(100));
   closed[0]=false;f.context=new AutomationContext(1,true,true,Set.of());check(service.manualRequest(0)&&service.pending()&&factories[0]==0,"UI intent does not open a driver or play while settings is open");closed[0]=true;f.context=new AutomationContext(1,true,true,Set.of());service.tick(1_000_000_000L);check(service.active()&&factories[0]==1&&f.opens==1&&f.session.holdsFarming(),"queued intent becomes one real native-port task under original root");service.cancel(2_000_000_000L);check(!service.active()&&!f.session.active()&&f.stops==1&&notices.size()==1&&f.closes==0,"manual stop retains user GUI and reports once without rearm");service.tick(3_000_000_000L);check(factories[0]==1&&notices.size()==1,"no implicit retry or duplicate notification");
   var f2=new PetGeorgeTaskTest.H();f2.openAllowed=false;var failures=new ArrayList<StashService.Notice>();var guarded=new PetSaleService(settings,f2.coordinator,f2.session,f2.items,f2.cursors,f2::context,()->f2,()->true,failures::add);guarded.setPolicy(guarded.policy().withOnlyFarming(false));guarded.setEnabled(true);guarded.tick(0);guarded.tick(1_000_000_000L);check(f2.opens==1&&!guarded.autoEnabled()&&!guarded.active()&&failures.size()==1,"possibly sent opener failure becomes one reported suspended attempt even with automatic outside Farming enabled; no invisible retries");
   System.out.println("PetSaleServiceTest: PASS");
  }finally{try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
