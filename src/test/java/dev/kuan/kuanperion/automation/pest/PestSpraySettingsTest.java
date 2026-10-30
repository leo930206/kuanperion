package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class PestSpraySettingsTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("spray-policy-").resolve("automation.json");Files.writeString(path,"{\"schemaVersion\":1,\"features\":{\"pest\":true},\"pest\":{\"threshold\":3},\"pestSpray\":{\"future\":9,\"buying\":{\"futureBuy\":7}}}");var s=AutomationSettingsStore.load(path);
  check(s.pestSprayPolicyValid()&&!s.pestSprayPolicy().configured()&&!s.pestSprayBuying().configured()&&!s.enabled(AutomationFeature.PEST_SPRAY),"Pest capture/notifications never grant spray, material, Sacks or Coins consent");
  var p=PestSprayPlanTest.policy();var b=new PestSprayBuying(16,1000);s.setPestSprayPolicy(p,b);s.setEnabled(AutomationFeature.PEST_SPRAY,true);var loaded=AutomationSettingsStore.load(path);check(loaded.pestSprayPolicyValid()&&loaded.pestSprayPolicy().equals(p)&&loaded.pestSprayBuying().equals(b)&&loaded.enabled(AutomationFeature.PEST)&&loaded.enabled(AutomationFeature.PEST_SPRAY)&&loaded.pestPolicy().threshold()==3,"independent atomic policy and cap roundtrip, original Pest preserved");check(Files.readString(path).contains("\"future\": 9")&&Files.readString(path).contains("\"futureBuy\": 7"),"unknown nested fields retained");
  for(String bad:List.of("null","[]","{\"acknowledgeSacks\":\"true\"}","{\"maximumUseItems\":65}","{\"materialId\":\"bad id\"}","{\"protectedUuids\":[1]}","{\"buying\":null}","{\"buying\":{\"targetCount\":65}}","{\"buying\":{\"maximumCoins\":\"1000\"}}","{\"buying\":{\"maximumCoins\":1000000000001}}")){
   String json="{\"schemaVersion\":1,\"features\":{\"pestSpray\":true},\"pestSpray\":"+bad+"}";Files.writeString(path,json);s=AutomationSettingsStore.load(path);check(!s.pestSprayPolicyValid()&&!s.pestSprayPolicy().configured()&&!s.pestSprayBuying().configured()&&Files.readString(path).equals(json),"invalid independent consent fails closed without overwriting original bytes: "+bad);
  }
  System.out.println("PestSpraySettingsTest: PASS");
 }
}
