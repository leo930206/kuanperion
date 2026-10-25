package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class PestTrapTravelSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var file=Files.createTempDirectory("trap-travel-settings-").resolve("automation.json");Files.writeString(file,"{\"schemaVersion\":1,\"features\":{\"pest\":true},\"pest\":{\"clearTraps\":true,\"travelPlots\":24,\"future\":7}}");var store=AutomationSettingsStore.load(file);check(store.pestPolicyValid()&&store.pestPolicy().trapPlot()==-1,"old clear+travel config never silently gets a trap destination");
  var p=store.pestPolicy().withTrapPlot(0);store.setPestPolicy(p);byte[] bytes=Files.readAllBytes(file);store=AutomationSettingsStore.load(file);check(store.pestPolicy().equals(p)&&Arrays.equals(bytes,Files.readAllBytes(file)),"explicit independent Barn trap destination survives reload with no writes");check(Files.readString(file).contains("\"future\": 7"),"unknown fields retained");
  for(String bad:List.of("25","-2","0.5","\"0\"","null")){Files.writeString(file,"{\"schemaVersion\":1,\"pest\":{\"trapPlot\":"+bad+"}}");store=AutomationSettingsStore.load(file);check(!store.pestPolicyValid()&&store.pestPolicy().trapPlot()==-1,"invalid trap plot cannot fall back to Barn: "+bad);}
  System.out.println("PestTrapTravelSettingsTest: PASS (old flags do not grant destinations, explicit Barn persistence, strict malformed/range deny, unknown fields preserved)");
 }
}
