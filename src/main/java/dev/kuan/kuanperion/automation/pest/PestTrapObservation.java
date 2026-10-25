// SPDX-License-Identifier: GPL-3.0-only
// Field concepts from supplied Aether PestTrapTabParser; bounded exact local parser.
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
import java.util.regex.Pattern;
/** Missing, malformed and conflicting fields remain unknown, not empty completed work. */
public record PestTrapObservation(Set<Integer> full,boolean fullKnown,Set<Integer> empty,boolean emptyKnown) {
 private static final Pattern COLORS=Pattern.compile("§[0-9a-fk-orx]",Pattern.CASE_INSENSITIVE);
 public PestTrapObservation {
  full=Set.copyOf(full);empty=Set.copyOf(empty);
  for(var ids:List.of(full,empty))if(ids.size()>24||ids.stream().anyMatch(id->id<1||id>24))throw new IllegalArgumentException("Invalid trap IDs");
  if(!fullKnown&&!full.isEmpty()||!emptyKnown&&!empty.isEmpty())throw new IllegalArgumentException("Unknown trap field with values");
 }
 public static PestTrapObservation unknown(){return new PestTrapObservation(Set.of(),false,Set.of(),false);}
 public static PestTrapObservation parse(List<String> entries){
  if(entries==null||entries.size()>128)return unknown();
  Set<Integer> full=Set.of(),empty=Set.of();boolean seenFull=false,seenEmpty=false,badFull=false,badEmpty=false;int count=0,total=0;
  for(String entry:entries){
   if(entry==null||entry.length()>4096||(total+=entry.length())>65536)return unknown();
   for(String raw:entry.split("\\R",-1)){
    if(++count>256||raw.length()>512)return unknown();
    String line=COLORS.matcher(raw).replaceAll("").replaceAll("[\\p{Zs}]"," ").replaceAll("\\p{Cf}","").strip();
    int colon=line.indexOf(':');if(colon<0)continue;String heading=line.substring(0,colon).strip(),value=line.substring(colon+1).strip();
    if(heading.equalsIgnoreCase("Full Traps")){
     var ids=ids(value);if(ids==null||seenFull&&!full.equals(ids))badFull=true;else full=ids;seenFull=true;
    }else if(heading.equalsIgnoreCase("No Bait")){
     var ids=ids(value);if(ids==null||seenEmpty&&!empty.equals(ids))badEmpty=true;else empty=ids;seenEmpty=true;
    }
   }
  }
  boolean f=seenFull&&!badFull,e=seenEmpty&&!badEmpty;return new PestTrapObservation(f?full:Set.of(),f,e?empty:Set.of(),e);
 }
 private static Set<Integer> ids(String value){
  if(value.equalsIgnoreCase("None"))return Set.of();
  var ids=new HashSet<Integer>();var parts=value.split(",",-1);if(parts.length>24)return null;
  for(String part:parts){String token=part.strip();if(!token.matches("#(?:[1-9]|1[0-9]|2[0-4])")||!ids.add(Integer.parseInt(token.substring(1))))return null;}
  return Set.copyOf(ids);
 }
}
