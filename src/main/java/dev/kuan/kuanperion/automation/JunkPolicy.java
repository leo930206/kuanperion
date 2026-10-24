// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Disposal requires explicit IDs AND a user-recorded Garden location; never travels or guesses. */
public record JunkPolicy(Set<String> allowedIds,Set<String> protectedUuids,int maxStacks,int maxItems,int delayMillis,int timeoutSeconds,int cooldownSeconds,int thresholdPercent,boolean onlyWhileFarming,Location anchor,double radius){
 public record Location(String dimension,double x,double y,double z){
  public Location{if(dimension==null||dimension.isBlank()||dimension.length()>150||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000||Math.abs(y)>2048)throw new IllegalArgumentException("Invalid drop location");}
 }
 public JunkPolicy{
  allowedIds=Set.copyOf(allowedIds);protectedUuids=Set.copyOf(protectedUuids);
  if(allowedIds.size()>64||protectedUuids.size()>128||allowedIds.stream().anyMatch(id->!id.matches("[A-Z0-9_:-]{1,100}"))||protectedUuids.stream().anyMatch(id->!id.matches("[A-Za-z0-9_-]{1,80}"))||maxStacks<1||maxStacks>27||maxItems<1||maxItems>4096||delayMillis<500||delayMillis>5000||timeoutSeconds<10||timeoutSeconds>180||cooldownSeconds<10||cooldownSeconds>3600||thresholdPercent<1||thresholdPercent>100||!Double.isFinite(radius)||radius<0.5||radius>3)throw new IllegalArgumentException("Invalid Junk policy");
 }
 public static JunkPolicy defaults(){return new JunkPolicy(Set.of(),Set.of(),3,192,1000,60,60,85,true,null,1.5);}
 public InventoryProtection protection(){return new InventoryProtection(allowedIds,protectedUuids);}
 public boolean atAnchor(Location current){if(anchor==null||current==null||!anchor.dimension().equals(current.dimension()))return false;double dx=current.x()-anchor.x(),dy=current.y()-anchor.y(),dz=current.z()-anchor.z();return dx*dx+dy*dy+dz*dz<=radius*radius;}
}
