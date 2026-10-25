package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;
import java.nio.file.*;
public final class PestTravelSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("pest-travel-config-");Path p=root.resolve("automation.json");
  Files.writeString(p,"{\"schemaVersion\":1,\"features\":{\"pest\":true},\"pest\":{\"sweep\":true},\"future\":17}");
  var store=AutomationSettingsStore.load(p);check(store.enabled(AutomationFeature.PEST)&&store.pestPolicy().travelPlots()==0,"existing enabled automation does not gain travel authorization");
  store.setPestPolicy(store.pestPolicy().withTravelPlots(4));var loaded=AutomationSettingsStore.load(p);check(loaded.pestPolicy().travelPlots()==4&&loaded.pestPolicy().withSweep(false).travelPlots()==4&&Files.readString(p).contains("\"future\": 17"),"explicit travel opt-in persists externally, other edits retain it, unknown fields survive");
  for(String bad:new String[]{"-1","25","1.5","\"2\"","true","null"}){String raw="{\"schemaVersion\":1,\"pest\":{\"travelPlots\":"+bad+"}}";Files.writeString(p,raw);loaded=AutomationSettingsStore.load(p);check(!loaded.pestPolicyValid()&&Files.readString(p).equals(raw),"invalid travel authority fails closed and stays untouched: "+bad);}
  System.out.println("PestTravelSettingsTest: PASS (off by default even for enabled old configs, exact 0..24 integer authorization, atomic roundtrip and unknown preservation)");
 }
}
