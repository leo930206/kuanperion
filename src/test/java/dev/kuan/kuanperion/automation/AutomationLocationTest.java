package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationLocationTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[]args){
  check(AutomationLocation.inspect(List.of("SKYBLOCK","⏣ The Garden ☘ x3","Plot - 14"),"GARDEN").garden(),"current native Garden and agreeing API");
  check(!AutomationLocation.inspect(List.of("HYPIXEL","⏣ Main Lobby"),"GARDEN").skyblock(),"stale API alone never authorizes new-world gameplay");
  check(!AutomationLocation.inspect(List.of("SKYBLOCK","⏣ Hub","Visit the Garden"),"GARDEN").garden(),"native Hub overrides stale Garden API and a Garden mention");
  check(!AutomationLocation.inspect(List.of("SKYBLOCK","⏣ Hub","Plot - 14"),"GARDEN").garden(),"conflicting area/plot rows fail closed");
  check(AutomationLocation.inspect(List.of("§eSKYBLOCK","§a⏣ §bThe Garden §e☘ x3"),null).garden(),"native proof works without optional API");
  check(AutomationLocation.inspect(List.of("SKYBLOCK","Plot - 24"),null).garden(),"exact bounded plot row can prove Garden");
  check(!AutomationLocation.inspect(List.of("SKYBLOCK","Plot - 25"),null).garden(),"not arbitrary plot-like text");
  check(!AutomationLocation.inspect(List.of("SKYBLOCK","⏣ The Garden"),"HUB").garden(),"API/native contradiction never issues Garden actions");
  check(!AutomationLocation.inspect(List.of("NOT SKYBLOCK","⏣ The Garden"),null).skyblock(),"exact native SkyBlock header");
  check(!AutomationLocation.inspect(Collections.nCopies(65,"SKYBLOCK"),"GARDEN").skyblock(),"bounded native input, no stale API fallback");
  check(!AutomationLocation.inspect(List.of("SKYBLOCK","⏣ The Garden","⏣ Hub"),null).garden(),"two contradictory areas rejected");
  System.out.println("AutomationLocationTest: PASS (native world proof, stale API/Hub/mentions/conflicts rejected, optional API fallback, exact bounded plot/title parsing)");
 }
}
