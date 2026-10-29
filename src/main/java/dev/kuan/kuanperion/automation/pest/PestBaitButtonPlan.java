// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Full owned source stack is the consumption ceiling. A desired count cannot limit a native left click. */
public final class PestBaitButtonPlan {
 public record Rules(String baitId,int maximumPerTrap,int maximumRunItems,Set<String> protectedUuids){
  public Rules{Objects.requireNonNull(baitId);protectedUuids=Set.copyOf(protectedUuids);if(!baitId.isEmpty()&&!baitId.matches("[A-Z0-9_]{1,64}")||maximumPerTrap<0||maximumPerTrap>64||maximumRunItems<0||maximumRunItems>1728||protectedUuids.size()>128||protectedUuids.stream().anyMatch(s->s.isBlank()||s.length()>128))throw new IllegalArgumentException("Invalid bait ceiling");}
  public static Rules defaults(){return new Rules("",0,0,Set.of());}
  public boolean configured(){return !baitId.isEmpty()&&maximumPerTrap>0&&maximumRunItems>0;}
 }
 private final Map<Integer,ItemSnapshot> baseline;private final int source;
 private PestBaitButtonPlan(Map<Integer,ItemSnapshot> inventory,int source){baseline=Map.copyOf(inventory);this.source=source;}
 public Map<Integer,ItemSnapshot> baseline(){return baseline;}public int source(){return source;}public ItemSnapshot item(){return baseline.get(source);}public int maximumConsumption(){return item().count();}
 public boolean stillMatches(Map<Integer,ItemSnapshot> inventory){return complete(inventory)&&baseline.equals(inventory);}
 public static Optional<PestBaitButtonPlan> select(Rules rules,Map<Integer,ItemSnapshot> inventory,int remainingRun){
  if(rules==null||!rules.configured()||remainingRun<1||remainingRun>rules.maximumRunItems()||!complete(inventory))return Optional.empty();var protection=new InventoryProtection(Set.of(rules.baitId()),rules.protectedUuids());
  for(int i=9;i<36;i++){var x=inventory.get(i);if(protection.permits(x)&&x.uuid().isEmpty()&&x.count()>=1&&x.count()<=rules.maximumPerTrap()&&x.count()<=remainingRun)return Optional.of(new PestBaitButtonPlan(inventory,i));}return Optional.empty();
 }
 private static boolean complete(Map<Integer,ItemSnapshot> inventory){
  if(inventory==null||inventory.size()!=43)return false;
  for(int i=0;i<43;i++){var x=inventory.get(i);if(x==null||x.count()>64||x.count()==0&&(!x.minecraftId().equals("minecraft:air")||!x.skyblockId().isEmpty()||!x.uuid().isEmpty()))return false;}return true;
 }
}
