// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Explicit original-style crop/fuel stock targets, not an inferred resource conversion or GUI quote. */
public record ComposterBuying(Map<String,Stock> stock,long maximumCoins){
 public record Stock(int targetCount,BazaarAcquisitionPlan.Material material){public Stock{Objects.requireNonNull(material);if(targetCount<1||targetCount>4096)throw new IllegalArgumentException("Invalid Composter stock target");}}
 public ComposterBuying{stock=Map.copyOf(stock);if(stock.size()>2||stock.keySet().stream().anyMatch(id->!id.matches("[A-Z0-9_]{1,64}"))||maximumCoins<0||maximumCoins>1_000_000_000_000L)throw new IllegalArgumentException("Invalid Composter buying policy");}
 public static ComposterBuying defaults(){return new ComposterBuying(Map.of(),0);}
 public BazaarAcquisitionPlan.Rules rules(int maximumItems){var allowed=new TreeMap<String,BazaarAcquisitionPlan.Material>();stock.forEach((id,s)->allowed.put(id,s.material()));return new BazaarAcquisitionPlan.Rules(allowed,maximumCoins,maximumItems);}
}
