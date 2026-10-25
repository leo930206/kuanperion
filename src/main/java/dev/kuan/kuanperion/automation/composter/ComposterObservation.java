// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.math.BigDecimal;import java.util.regex.Pattern;
/** Read-only exact resource ratios. No slot fallback, click, purchase, or inference of permission. */
public final class ComposterObservation {
 public enum Kind{ORGANIC,FUEL}
 public record Resource(long current,long capacity){public Resource{if(current<0||capacity<=0||current>capacity||capacity>1_000_000_000L)throw new IllegalArgumentException("Invalid resource");}}
 public record Snapshot(MenuSnapshot menu,int organicSlot,Resource organic,int fuelSlot,Resource fuel){public Snapshot{Objects.requireNonNull(menu);Objects.requireNonNull(organic);Objects.requireNonNull(fuel);}}
 private static final String NUMBER="(?:[0-9]{1,9}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})(?:\\.[0-9]{1,3})?[kKmM]?";
 private static final Pattern RATIO=Pattern.compile("^("+NUMBER+")\\s*/\\s*("+NUMBER+")$");
 private static final MenuContract MENU=new MenuContract(Set.of("Composter"),Set.of(45,54));
 private ComposterObservation(){}
 public static Optional<Resource> resource(String line){
  if(line==null||line.length()>64)return Optional.empty();var match=RATIO.matcher(line.trim());if(!match.matches())return Optional.empty();
  try{return Optional.of(new Resource(number(match.group(1)),number(match.group(2))));}catch(RuntimeException invalid){return Optional.empty();}
 }
 private static long number(String value){var n=value.toLowerCase(Locale.ROOT).replace(",","");int multiplier=n.endsWith("k")?1000:n.endsWith("m")?1_000_000:1;if(multiplier!=1)n=n.substring(0,n.length()-1);return new BigDecimal(n).multiply(BigDecimal.valueOf(multiplier)).longValueExact();}
 public static Optional<Snapshot> inspect(MenuSnapshot menu){
  if(!MENU.matches(menu)||menu.slots().size()>90)return Optional.empty();Resource organic=null,fuel=null;int organicSlot=-1,fuelSlot=-1;
  for(var slot:menu.slots()){
   if(slot.playerInventory()||slot.index()>=menu.containerSlots())continue;var item=slot.item();boolean isOrganic=item.name().equals("Organic Matter"),isFuel=item.name().equals("Fuel");if(!isOrganic&&!isFuel)continue;
   if(item.count()!=1||item.identified()||!item.uuid().isEmpty()||item.lore().size()>128)return Optional.empty();Resource value=null;
   for(String line:item.lore()){var ratio=resource(line);if(ratio.isEmpty())continue;if(value!=null)return Optional.empty();value=ratio.get();}
   if(value==null)return Optional.empty();if(isOrganic){if(organic!=null)return Optional.empty();organic=value;organicSlot=slot.index();}else{if(fuel!=null)return Optional.empty();fuel=value;fuelSlot=slot.index();}
  }
  return organic==null||fuel==null?Optional.empty():Optional.of(new Snapshot(menu,organicSlot,organic,fuelSlot,fuel));
 }
 /** A local delta is only one condition; the task must separately require an independent new server-applied slot receipt. */
 public static boolean increased(Snapshot before,Snapshot after,Kind kind,long amount){
  if(before==null||after==null||kind==null||amount<=0||before.menu().worldId()!=after.menu().worldId()||before.menu().containerId()!=after.menu().containerId()||before.organicSlot()!=after.organicSlot()||before.fuelSlot()!=after.fuelSlot())return false;
  var old=kind==Kind.ORGANIC?before.organic():before.fuel();var current=kind==Kind.ORGANIC?after.organic():after.fuel();return old.capacity()==current.capacity()&&current.current()-old.current()==amount;
 }
}
