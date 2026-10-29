package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class PestBaitSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("pest-bait-settings-").resolve("automation.json");Files.writeString(path,"{\"schemaVersion\":1,\"pest\":{\"clearTraps\":true,\"future\":9}}");var s=AutomationSettingsStore.load(path);check(s.pestPolicyValid()&&!s.pestPolicy().bait().enabled()&&!s.pestPolicy().bait().configured(),"existing notification/trap consent never grants bait consumption");
  var p=s.pestPolicy().withBait(new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of("protected")))).withBait(new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of("protected")),new PestBaitBuying("minecraft:yellow_dye",64,16,1000)));s.setPestPolicy(p);check(AutomationSettingsStore.load(path).pestPolicy().equals(p)&&Files.readString(path).contains("\"future\": 9"),"strict explicit bait identity/limits and existing unknown settings preserved");
  check(p.withSweep(true).withClearTraps(false).withTrapPlot(0).withTravelPlots(2).bait().equals(p.bait()),"editing other Pest settings never silently loses bait consent or limits");
  for(String bad:List.of("{\"buying\":null}","{\"buying\":{\"targetCount\":65}}","{\"buying\":{\"maximumCoins\":\"1000\"}}","null","[]","{\"enabled\":\"true\"}","{\"maximumPerTrap\":65}","{\"maximumRunItems\":1729}","{\"baitId\":\"tasty cheese\"}","{\"protectedUuids\":[1]}")){
   String json="{\"schemaVersion\":1,\"pest\":{\"bait\":"+bad+"}}";Files.writeString(path,json);s=AutomationSettingsStore.load(path);check(!s.pestPolicyValid()&&!s.pestPolicy().bait().configured()&&Files.readString(path).equals(json),"invalid bait consent fails closed without overwrite: "+bad);
  }
  System.out.println("PestBaitSettingsTest: PASS");
 }
}
