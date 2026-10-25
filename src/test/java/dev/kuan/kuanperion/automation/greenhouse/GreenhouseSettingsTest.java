package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class GreenhouseSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path file=Files.createTempDirectory("greenhouse-settings-").resolve("automation.json");var store=AutomationSettingsStore.load(file);check(!store.enabled(AutomationFeature.GREENHOUSE)&&store.greenhousePolicyValid()&&!store.greenhousePolicy().configured(),"missing config has no authority");
  var p=GreenhousePolicy.defaults().withPlots(Set.of(1,24)).withKinds(Set.of(GreenhouseIdentity.Kind.GLASSCORN)).withTool("HOE",new UUID(0,3).toString()).withInterval(900).withOnlyFarming(false);
  store.setGreenhousePolicy(p);store.setEnabled(AutomationFeature.GREENHOUSE,true);byte[] before=Files.readAllBytes(file);store=AutomationSettingsStore.load(file);check(Arrays.equals(before,Files.readAllBytes(file))&&store.greenhousePolicy().equals(p)&&store.enabled(AutomationFeature.GREENHOUSE),"external exact consent persists without rewriting on load");
  for(String invalid:List.of("{\"plots\":[0]}","{\"plots\":[1,1]}","{\"plots\":[\"1\"]}","{\"kinds\":[\"ANY\"]}","{\"kinds\":[\"ASHWREATH\",\"ASHWREATH\"]}","{\"onlyWhileFarming\":\"false\"}","{\"toolUuid\":\"hoe name\"}","{\"radius\":100}")){
   Files.writeString(file,"{\"schemaVersion\":1,\"features\":{\"greenhouse\":true},\"greenhouse\":"+invalid+"}");store=AutomationSettingsStore.load(file);check(!store.greenhousePolicyValid()&&!store.greenhousePolicy().configured(),"malformed greenhouse never falls back to executable defaults: "+invalid);
  }
  Files.writeString(file,"{\"schemaVersion\":1,\"future\":{\"keep\":7},\"greenhouse\":{\"future\":8}}");store=AutomationSettingsStore.load(file);store.setGreenhousePolicy(p);check(Files.readString(file).contains("\"keep\": 7")&&Files.readString(file).contains("\"future\": 8"),"unknown fields preserved");
  System.out.println("GreenhouseSettingsTest: PASS (external exact consent, off/unconfigured defaults, strict arrays/UUID/limits, preserved unknown fields)");
 }
}
