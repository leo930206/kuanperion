package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class PestExchangeSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("pest-exchange-settings-").resolve("automation.json");Files.writeString(path,"{\"schemaVersion\":1,\"pest\":{\"clearTraps\":true,\"future\":9}}");var s=AutomationSettingsStore.load(path);
  check(s.pestPolicyValid()&&!s.pestPolicy().exchangeAfterCleanup(),"old Pest/trap/bait consent grants no whole-bag exchange");
  var bait=new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of()));var p=s.pestPolicy().withBait(bait).withExchangeAfterCleanup(true);s.setPestPolicy(p);
  check(AutomationSettingsStore.load(path).pestPolicy().equals(p)&&Files.readString(path).contains("\"future\": 9"),"exact independent exchange consent persists without losing unknown settings");
  check(p.withSweep(true).withClearTraps(false).withTrapPlot(0).withTravelPlots(2).withBait(bait).exchangeAfterCleanup(),"editing other policies preserves exchange consent");
  check(p.withExchangeAfterCleanup(false).bait().equals(bait),"exchange changes never grant or reset bait authority");
  for(String bad:List.of("null","\"true\"","1","[]","{}")){
   String json="{\"schemaVersion\":1,\"pest\":{\"exchangeAfterCleanup\":"+bad+"}}";Files.writeString(path,json);s=AutomationSettingsStore.load(path);check(!s.pestPolicyValid()&&!s.pestPolicy().exchangeAfterCleanup()&&Files.readString(path).equals(json),"malformed exchange consent fails closed without overwriting: "+bad);
  }
  System.out.println("PestExchangeSettingsTest: PASS default false, independent strict consent, unknown-key and byte preservation");
 }
}
