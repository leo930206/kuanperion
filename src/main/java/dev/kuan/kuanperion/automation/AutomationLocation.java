// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
import java.util.regex.Pattern;
/** Fresh native scoreboard proof is mandatory; an optional API may veto, never invent a location. */
public record AutomationLocation(boolean skyblock,boolean garden){
 private static final Pattern FORMATTING=Pattern.compile("(?i)§[0-9a-fk-orx]|\\p{Cf}");
 private static final Pattern PLOT=Pattern.compile("(?i)Plot\\s*[-:]\\s*(?:[1-9]|1[0-9]|2[0-4])");
 public static AutomationLocation inspect(List<String> lines,String apiIsland){
  if(lines==null||lines.size()>64)return new AutomationLocation(false,false);
  boolean header=false,gardenArea=false,foreignArea=false,plot=false;
  for(String raw:lines){
   if(raw==null||raw.length()>512)return new AutomationLocation(false,false);
   String line=FORMATTING.matcher(raw).replaceAll("").strip();
   String upper=line.toUpperCase(Locale.ROOT);if(upper.equals("SKYBLOCK")||upper.equals("SKYBLOCK CO-OP"))header=true;
   if(PLOT.matcher(line).matches())plot=true;
   if(line.startsWith("⏣")){
    String area=line.substring(1).strip();String lower=area.toLowerCase(Locale.ROOT);String prefix=lower.startsWith("the garden")?"the garden":lower.startsWith("garden")?"garden":"";
    String tail=area.substring(prefix.length()).stripLeading();
    if(!prefix.isEmpty()&&(tail.isEmpty()||!Character.isLetterOrDigit(tail.codePointAt(0))))gardenArea=true;else foreignArea=true;
   }
  }
  boolean agrees=apiIsland==null||apiIsland.equals("GARDEN");
  return new AutomationLocation(header,header&&agrees&&!foreignArea&&(gardenArea||plot));
 }
}
