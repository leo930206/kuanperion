// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import java.util.*;import java.math.BigDecimal;
/** Explicit pet kind/XP/price authority; no original Rat/Slug defaults or name-derived price. */
public record PetSalePolicy(Set<PetSalePreflight.Kind> allowed,long maximumExperience,long minimumPayout,int intervalSeconds,boolean onlyWhileFarming,Set<String> protectedUuids){
 public PetSalePolicy{
  allowed=Set.copyOf(allowed);var rules=new PetSalePreflight.Rules(allowed,BigDecimal.valueOf(maximumExperience),1,protectedUuids);protectedUuids=rules.protectedUuids();
  if(maximumExperience<0||maximumExperience>1_000_000_000_000L||minimumPayout<1||minimumPayout>1_000_000_000_000L||intervalSeconds<20||intervalSeconds>86400)throw new IllegalArgumentException("Invalid pet sale policy");
 }
 public PetSalePreflight.Rules rules(){return new PetSalePreflight.Rules(allowed,BigDecimal.valueOf(maximumExperience),1,protectedUuids);}
 public static PetSalePolicy defaults(){return new PetSalePolicy(Set.of(),0,1,20,true,Set.of());}
 public PetSalePolicy withAllowed(Set<PetSalePreflight.Kind> value){return new PetSalePolicy(value,maximumExperience,minimumPayout,intervalSeconds,onlyWhileFarming,protectedUuids);}
 public PetSalePolicy withMaximumExperience(long value){return new PetSalePolicy(allowed,value,minimumPayout,intervalSeconds,onlyWhileFarming,protectedUuids);}
 public PetSalePolicy withMinimumPayout(long value){return new PetSalePolicy(allowed,maximumExperience,value,intervalSeconds,onlyWhileFarming,protectedUuids);}
 public PetSalePolicy withInterval(int value){return new PetSalePolicy(allowed,maximumExperience,minimumPayout,value,onlyWhileFarming,protectedUuids);}
 public PetSalePolicy withOnlyFarming(boolean value){return new PetSalePolicy(allowed,maximumExperience,minimumPayout,intervalSeconds,value,protectedUuids);}
}
