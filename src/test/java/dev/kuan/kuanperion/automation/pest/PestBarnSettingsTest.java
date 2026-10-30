package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.nio.file.*;import java.util.*;
public final class PestBarnSettingsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  var desk=new PestTargets.Point(12.5,72,-10.25);var b=new PestBarnPolicy(true,desk);check(b.configured()&&!PestBarnPolicy.defaults().configured(),"explicit separate Barn desk/route consent, no guessed NPC coordinates");
  check(b.withDeskText("12.5, 72, -10.25").equals(b)&&b.withDeskText(b.deskText()).equals(b)&&b.withDeskText("  ").desk()==null,"explicit UI coordinate roundtrip/clear, never capture or guess a desk");
  var tiny=b.withDeskText(".0000001 72 0");check(tiny.withDeskText(tiny.deskText()).equals(tiny),"tiny decimal coordinate formatting must roundtrip without exponent input grammar");
  for(String text:List.of("0 70","0 70 0 1","NaN 70 0","Infinity 70 0","0x1p0 70 0","48 70 0","0 2048 0","0 70 -49","1e3 70 0")){boolean denied=false;try{b.withDeskText(text);}catch(IllegalArgumentException invalid){denied=true;}check(denied,"reject malformed or non-Barn UI desk "+text);}
  var p=PestPolicy.defaults().withExchangeAfterCleanup(true).withBarn(b);check(p.withSweep(true).withTravelPlots(2).withClearTraps(true).withTrapPlot(0).withBait(PestBaitPolicy.defaults()).withExchangeAfterCleanup(false).barn().equals(b),"unrelated Pest edits preserve explicit Barn route");
  var path=Files.createTempDirectory("pest-barn-").resolve("automation.json");Files.writeString(path,"{\"schemaVersion\":1,\"pest\":{\"exchangeAfterCleanup\":true,\"barn\":{\"future\":9}}}");var s=AutomationSettingsStore.load(path);check(s.pestPolicyValid()&&!s.pestPolicy().barn().enabled(),"old Abiphone/capture consent never authorizes Barn travel/interaction");s.setPestPolicy(p);check(AutomationSettingsStore.load(path).pestPolicy().equals(p)&&Files.readString(path).contains("\"future\": 9"),"strict desk/world-local route preserves unknown fields");
  for(String bad:List.of("null","[]","{\"enabled\":\"true\"}","{\"desk\":null}","{\"desk\":{\"x\":48,\"y\":72,\"z\":0}}","{\"desk\":{\"x\":0,\"y\":\"72\",\"z\":0}}","{\"desk\":{\"x\":0,\"y\":72}}")){
   String json="{\"schemaVersion\":1,\"pest\":{\"barn\":"+bad+"}}";Files.writeString(path,json);s=AutomationSettingsStore.load(path);check(!s.pestPolicyValid()&&!s.pestPolicy().barn().enabled()&&Files.readString(path).equals(json),"invalid Barn route/coordinates deny without overwriting "+bad);
  }
  System.out.println("PestBarnSettingsTest: PASS");
 }
}
