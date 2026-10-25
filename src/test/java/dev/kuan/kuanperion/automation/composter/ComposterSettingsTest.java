package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class ComposterSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var path=Files.createTempDirectory("composter-settings-").resolve("automation.json");var store=AutomationSettingsStore.load(path);check(!store.enabled(AutomationFeature.COMPOSTER)&&store.composterPolicyValid()&&!store.composterPolicy().configured(),"missing config no consumption authority");
  var p=ComposterTaskTest.POLICY;store.setComposterPolicy(p);store.setEnabled(AutomationFeature.COMPOSTER,true);var before=Files.readAllBytes(path);store=AutomationSettingsStore.load(path);check(store.composterPolicy().equals(p)&&Arrays.equals(before,Files.readAllBytes(path)),"exact external machine/material/units consent survives reload without writes");
  for(String bad:List.of("{\"anchor\":{\"blockId\":\"minecraft:stone\"}}","{\"materials\":{\"BOX\":{\"kind\":\"ORGANIC\",\"units\":0}}}","{\"materials\":{\"BOX\":{\"kind\":\"ANY\",\"units\":1000}}}","{\"materials\":[]}","{\"targetPercent\":101}","{\"anchor\":{\"x\":999,\"y\":72,\"z\":0,\"blockId\":\"minecraft:stone\"}}","{\"onlyWhileFarming\":\"false\"}")){
   Files.writeString(path,"{\"schemaVersion\":1,\"features\":{\"composter\":true},\"composter\":"+bad+"}");store=AutomationSettingsStore.load(path);check(!store.composterPolicyValid()&&!store.composterPolicy().configured(),"invalid consent never repaired as executable: "+bad);
  }
  Files.writeString(path,"{\"schemaVersion\":1,\"composter\":{\"future\":9}}");store=AutomationSettingsStore.load(path);store.setComposterPolicy(p);check(Files.readString(path).contains("\"future\": 9"),"unknown prefs preserved");
  System.out.println("ComposterSettingsTest: PASS (strict external machine/material/unit authority, default off, malformed deny, unknown preserved, byte-preserving reload)");
 }
}
