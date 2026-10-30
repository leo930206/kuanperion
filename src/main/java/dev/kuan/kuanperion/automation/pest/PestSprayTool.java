// SPDX-License-Identifier: GPL-3.0-only
// Curated IDs and Selected Material lore concept from supplied SprayonatorItem; strict local parsing is provisional.
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.ItemSnapshot;import java.util.*;import java.util.regex.Pattern;
/** Immutable read-only source facts. Neither a matched tool nor a label authorizes item use/consumption. */
public final class PestSprayTool {
 private static final Set<String> IDS=Set.of("SPRAYONATOR","JUICY_SPRAYONATOR","SALTY_SPRAYONATOR");
 private static final Pattern MATERIAL=Pattern.compile("(?i)^selected\\s+material\\s*:\\s*(.+)$");
 public record Tool(int slot,ItemSnapshot item,String selectedMaterial){
  public Tool{Objects.requireNonNull(item);Objects.requireNonNull(selectedMaterial);if(slot<0||slot>8)throw new IllegalArgumentException("Not a hotbar tool");}
  public boolean matchesMaterial(String wanted){var normalized=name(wanted);return !selectedMaterial.isEmpty()&&normalized!=null&&selectedMaterial.equalsIgnoreCase(normalized);}
 }
 private PestSprayTool(){}
 public static Optional<Tool> read(int slot,ItemSnapshot item){
  if(slot<0||slot>8||item==null||item.count()!=1||!IDS.contains(item.skyblockId())||!item.minecraftId().matches("minecraft:[a-z0-9_]{1,100}")||item.minecraftId().equals("minecraft:air")||item.fingerprint().isBlank()||item.lore().size()>64)return Optional.empty();
  String selected=null;
  for(String raw:item.lore()){
   if(raw.length()>1024||raw.codePoints().anyMatch(Character::isISOControl))return Optional.empty();
   String line=clean(raw);var match=MATERIAL.matcher(line);
   if(!match.matches()){if(line.toLowerCase(Locale.ROOT).startsWith("selected material"))return Optional.empty();continue;}
   if(selected!=null)return Optional.empty();var value=name(match.group(1));if(value==null||Set.of("unknown","use selected").contains(value.toLowerCase(Locale.ROOT)))return Optional.empty();selected=value.equalsIgnoreCase("none")?"":value;
  }
  return selected==null?Optional.empty():Optional.of(new Tool(slot,item,selected));
 }
 private static String clean(String raw){return raw.replaceAll("(?i)§[0-9a-fk-orx]","").replaceAll("\\s+"," ").strip();}
 private static String name(String raw){if(raw==null||raw.length()>256||raw.codePoints().anyMatch(Character::isISOControl))return null;var value=clean(raw);return value.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _'()\\-]{0,63}")?value:null;}
}
