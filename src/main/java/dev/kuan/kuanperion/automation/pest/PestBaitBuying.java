// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.BazaarAcquisitionPlan;import java.util.*;
/** Separate optional stock purchase. The same immutable positive coin cap funds the entire original Pest run. */
public record PestBaitBuying(String minecraftId,int stackLimit,int targetCount,long maximumCoins){
 public PestBaitBuying{Objects.requireNonNull(minecraftId);if(!minecraftId.isEmpty()&&(!minecraftId.matches("minecraft:[a-z0-9_]{1,100}")||minecraftId.equals("minecraft:air"))||stackLimit<1||stackLimit>64||targetCount<0||targetCount>stackLimit||maximumCoins<0||maximumCoins>1_000_000_000_000L)throw new IllegalArgumentException("Invalid bait purchase bounds");}
 public static PestBaitBuying defaults(){return new PestBaitBuying("",64,0,0);}
 public PestBaitBuying withMinecraftId(String id){return new PestBaitBuying(id,stackLimit,targetCount,maximumCoins);}
 public PestBaitBuying withStackLimit(int n){return new PestBaitBuying(minecraftId,n,targetCount,maximumCoins);}
 public PestBaitBuying withTargetCount(int n){return new PestBaitBuying(minecraftId,stackLimit,n,maximumCoins);}
 public PestBaitBuying withMaximumCoins(long n){return new PestBaitBuying(minecraftId,stackLimit,targetCount,n);}
 public boolean configured(){return !minecraftId.isEmpty()&&targetCount>0&&maximumCoins>0;}
 public BazaarAcquisitionPlan.Rules rules(String baitId){return configured()?new BazaarAcquisitionPlan.Rules(Map.of(baitId,new BazaarAcquisitionPlan.Material(minecraftId,stackLimit,maximumCoins)),maximumCoins,targetCount):BazaarAcquisitionPlan.Rules.defaults();}
}
