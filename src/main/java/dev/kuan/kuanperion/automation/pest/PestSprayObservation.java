// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;import java.util.regex.Pattern;
/** Cached Tab facts only, never an item-use grant. Strict ACTIVE timer grammar is provisional. */
public record PestSprayObservation(State state) {
 public enum State {UNKNOWN,NONE,ACTIVE}
 public PestSprayObservation{Objects.requireNonNull(state);}
 public static PestSprayObservation unknown(){return new PestSprayObservation(State.UNKNOWN);}
 public static PestSprayObservation parse(List<String> entries,String material){
  if(entries==null||entries.size()>128||material==null||material.length()>64||material.isBlank())return unknown();
  boolean scoped=false,seen=false;State result=State.UNKNOWN;int size=0,count=0;
  for(var entry:entries){
   if(entry==null||entry.length()>4096||(size+=entry.length())>65536)return unknown();
   for(var raw:entry.split("\\R",-1)){
    if(++count>256||raw.length()>512||raw.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\t'))return unknown();
    var line=raw.replaceAll("(?i)§[0-9a-fk-orx]","").replace('\u00a0',' ').strip();if(line.isEmpty())continue;
    if(line.matches("(?i)^Pests\\s*:.*$")){scoped=true;continue;}if(!scoped)continue;
    int colon=line.indexOf(':');if(colon<0){scoped=false;continue;}String field=line.substring(0,colon).strip().toLowerCase(Locale.ROOT),value=line.substring(colon+1).strip();
    if(field.equals("spray")){
     if(seen)return unknown();seen=true;
     if(value.equalsIgnoreCase("None"))result=State.NONE;
     else if(value.equalsIgnoreCase(material)||active(value,material))result=State.ACTIVE;
    }else if(!Set.of("alive","plot","plots","cooldown","bonus","repellent").contains(field))scoped=false;
   }
  }
  return new PestSprayObservation(seen?result:State.UNKNOWN);
 }
 private static boolean active(String value,String material){
  var match=Pattern.compile("(?i)"+Pattern.quote(material)+" \\((?:(0|[1-5]?[0-9]|60)m(?: (0|[1-5]?[0-9])s)?|(0|[1-5]?[0-9])s)\\)").matcher(value);if(!match.matches())return false;
  int minutes=match.group(1)==null?0:Integer.parseInt(match.group(1)),seconds=match.group(2)!=null?Integer.parseInt(match.group(2)):match.group(3)!=null?Integer.parseInt(match.group(3)):0;return minutes*60+seconds>=1&&minutes*60+seconds<=3600;
 }
}
