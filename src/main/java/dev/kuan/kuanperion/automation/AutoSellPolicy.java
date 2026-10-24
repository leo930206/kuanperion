// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Explicit authorization and bounded per-run disposal; never authorizes via name or lore. */
public record AutoSellPolicy(Set<String> allowedIds,Set<String> protectedUuids,int maxStacks,int maxItems,int delayMillis,int timeoutSeconds,int cooldownSeconds,int thresholdPercent,boolean onlyWhileFarming){
 public AutoSellPolicy{
  allowedIds=Set.copyOf(allowedIds);protectedUuids=Set.copyOf(protectedUuids);
  if(allowedIds.size()>64||protectedUuids.size()>128||allowedIds.stream().anyMatch(id->!id.matches("[A-Z0-9_:-]{1,100}"))||protectedUuids.stream().anyMatch(id->!id.matches("[A-Za-z0-9_-]{1,80}"))||maxStacks<1||maxStacks>27||maxItems<1||maxItems>4096||delayMillis<500||delayMillis>5000||timeoutSeconds<10||timeoutSeconds>180||cooldownSeconds<10||cooldownSeconds>3600||thresholdPercent<1||thresholdPercent>100)throw new IllegalArgumentException("Invalid Auto Sell policy");
 }
 public static AutoSellPolicy defaults(){return new AutoSellPolicy(Set.of(),Set.of(),5,320,1000,60,60,85,true);}
 public InventoryProtection protection(){return new InventoryProtection(allowedIds,protectedUuids);}
 public static Set<String> parseIds(String input){if(input==null||input.isBlank())return Set.of();var ids=new TreeSet<>(Arrays.asList(input.strip().split("[\\s,]+")));if(ids.size()>64||ids.stream().anyMatch(id->!id.matches("[A-Z0-9_:-]{1,100}")))throw new IllegalArgumentException("請填大寫 SkyBlock 物品 ID，不接受名稱、萬用字元或超過64種物品");return Set.copyOf(ids);}
}
