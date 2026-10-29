package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.nio.file.*;
public final class PetSalePolicyTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var p=PetSalePolicy.defaults();check(p.allowed().isEmpty()&&p.rules().allowed().isEmpty()&&p.maximumExperience()==0,"no default pet disposal permission");p=p.withAllowed(Set.of(new PetSalePreflight.Kind("SLUG","EPIC"))).withMinimumPayout(100).withInterval(30);check(p.rules().maxPets()==1&&p.withOnlyFarming(false).minimumPayout()==100,"one pet per run, edits preserve exact minimum");
  var dir=Files.createTempDirectory("pet-policy-");try{var file=dir.resolve("automation.json");var settings=AutomationSettingsStore.load(file);settings.setPetPolicy(p);check(AutomationSettingsStore.load(file).petPolicy().equals(p)&&!settings.enabled(AutomationFeature.PET_SELL),"preferences survive JAR replacement, saving a policy does not enable automation");var text=Files.readString(file).replace("\"minimumPayout\": 100","\"minimumPayout\": 0");Files.writeString(file,text);check(!AutomationSettingsStore.load(file).petPolicyValid(),"invalid price not a free/zero fallback");}finally{try(var paths=Files.walk(dir)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
  System.out.println("PetSalePolicyTest: PASS");
 }
}
