// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Explicit material/use consent, independent of Pest capture, bait, notifications and purchases. */
public record PestSprayPolicy(String materialId,String minecraftId,String materialName,int maximumUseItems,boolean acknowledgeSacks,Set<String> protectedUuids) {
 public PestSprayPolicy {
  Objects.requireNonNull(materialId);Objects.requireNonNull(minecraftId);Objects.requireNonNull(materialName);protectedUuids=Set.copyOf(protectedUuids);
  if(!materialId.isEmpty()&&!materialId.matches("[A-Z0-9_]{1,64}")||!minecraftId.isEmpty()&&(!minecraftId.matches("minecraft:[a-z0-9_]{1,100}")||minecraftId.equals("minecraft:air"))||!materialName.isEmpty()&&!materialName.matches("[A-Za-z0-9][A-Za-z0-9 _'()\\-]{0,63}")||materialName.equalsIgnoreCase("None")||materialName.equalsIgnoreCase("Unknown")||materialName.equalsIgnoreCase("Use Selected")||maximumUseItems<0||maximumUseItems>64||protectedUuids.size()>128||protectedUuids.stream().anyMatch(s->s.isBlank()||s.length()>128))throw new IllegalArgumentException("Invalid spray consent");
 }
 public static PestSprayPolicy defaults(){return new PestSprayPolicy("","","",0,false,Set.of());}
 public boolean configured(){return !materialId.isEmpty()&&!minecraftId.isEmpty()&&!materialName.isEmpty()&&maximumUseItems>=PestSprayPlan.WORST_CASE_ITEMS&&acknowledgeSacks;}
}
