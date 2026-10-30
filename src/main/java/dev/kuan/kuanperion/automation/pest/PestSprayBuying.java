// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Independent purchase consent. Consuming spray material does not authorize spending Coins. */
public record PestSprayBuying(int targetCount,long maximumCoins){
 public PestSprayBuying{if(targetCount<0||targetCount>64||maximumCoins<0||maximumCoins>1_000_000_000_000L)throw new IllegalArgumentException("Invalid spray purchase bounds");}
 public static PestSprayBuying defaults(){return new PestSprayBuying(0,0);}
 public boolean configured(){return targetCount>=PestSprayPlan.WORST_CASE_ITEMS&&maximumCoins>0;}
 public BazaarAcquisitionPlan.Rules rules(PestSprayPolicy p){return configured()&&p!=null&&p.configured()?new BazaarAcquisitionPlan.Rules(Map.of(p.materialId(),new BazaarAcquisitionPlan.Material(p.minecraftId(),64,maximumCoins)),maximumCoins,targetCount):BazaarAcquisitionPlan.Rules.defaults();}
}
