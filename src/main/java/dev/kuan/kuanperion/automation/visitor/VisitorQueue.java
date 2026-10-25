// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import java.util.*;import java.util.regex.Pattern;
/** Bounded native Tab facts, never authority to interact. Incomplete/ambiguous is not an empty queue. */
public record VisitorQueue(int count,List<Entry> entries,boolean known){
 public enum Rarity{UNKNOWN,COMMON,UNCOMMON,RARE,EPIC,LEGENDARY,MYTHIC,DIVINE,SPECIAL,VERY_SPECIAL}
 public record Entry(String name,Rarity rarity,boolean firstTime){public Entry{Objects.requireNonNull(rarity);if(!validName(name))throw new IllegalArgumentException("Invalid visitor name");}}
 private static final Pattern HEADER=Pattern.compile("^Visitors:\\s*(?:\\(([0-5])\\)|([0-5]))$");
 private static final Pattern RARITY=Pattern.compile("(?i)^(.+) \\(([A-Z ]+)\\)$");
 public VisitorQueue{entries=List.copyOf(entries);if(known?(count<0||count>5||entries.size()!=count||entries.stream().map(Entry::name).distinct().count()!=count):count!=-1||!entries.isEmpty())throw new IllegalArgumentException("Invalid visitor queue");}
 public static VisitorQueue unknown(){return new VisitorQueue(-1,List.of(),false);}
 public static VisitorQueue parse(List<String> rawEntries){
  if(rawEntries==null||rawEntries.size()>128)return unknown();var lines=new ArrayList<String>();int total=0;
  for(String entry:rawEntries){
   if(entry==null||entry.length()>4096||(total+=entry.length())>65536)return unknown();
   for(String raw:entry.split("\\R",-1)){if(raw.length()>512||lines.size()>=256)return unknown();String line=raw.replaceAll("(?i)§[0-9a-fk-orx]","").replaceAll("\\p{Zs}"," ").trim();if(Pattern.compile("[\\p{C}§]").matcher(line).find())return unknown();lines.add(line);}
  }
  int start=-1,count=-1;
  for(int i=0;i<lines.size();i++)if(lines.get(i).startsWith("Visitors:")){var match=HEADER.matcher(lines.get(i));if(start>=0||!match.matches())return unknown();start=i;count=Integer.parseInt(match.group(1)!=null?match.group(1):match.group(2));}
  if(start<0)return unknown();var entries=new ArrayList<Entry>();var unique=new HashSet<String>();
  try{
   for(int i=start+1;i<lines.size();i++){
    String line=lines.get(i);if(line.isEmpty()||line.contains(":"))break;if(entries.size()>=count)return unknown();
    boolean first=line.endsWith(" NEW!")||line.startsWith("NEW! ");if(line.endsWith(" NEW!"))line=line.substring(0,line.length()-5).trim();else if(line.startsWith("NEW! "))line=line.substring(5).trim();
    Rarity rarity=Rarity.UNKNOWN;var match=RARITY.matcher(line);if(match.matches()){line=match.group(1);rarity=Rarity.valueOf(match.group(2).toUpperCase(Locale.ROOT).replace(' ','_'));if(rarity==Rarity.UNKNOWN)return unknown();}
    var entry=new Entry(line,rarity,first);if(!unique.add(entry.name().toLowerCase(Locale.ROOT)))return unknown();entries.add(entry);
   }
   return entries.size()==count?new VisitorQueue(count,entries,true):unknown();
  }catch(IllegalArgumentException malformed){return unknown();}
 }
 private static boolean validName(String name){return name!=null&&name.equals(name.trim())&&name.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} '\\-.,]{0,95}");}
}
