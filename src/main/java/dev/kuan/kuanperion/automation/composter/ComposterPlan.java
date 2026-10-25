// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** One full explicitly permitted ordinary material stack; never name-based, hotbar, cursor splitting or overfilling. */
public final class ComposterPlan {
 public record Stack(int slot,ItemSnapshot item){public Stack{if(slot<0||slot>35)throw new IllegalArgumentException("Invalid inventory slot");Objects.requireNonNull(item);}}
 public record Deposit(int inventorySlot,ItemSnapshot item,ComposterObservation.Kind kind,int amount,long increase){}
 private ComposterPlan(){}
 public static boolean supplied(ComposterPolicy policy,ComposterObservation.Resource resource){return resource.current()>=(resource.capacity()*policy.targetPercent()+99)/100;}
 public static Optional<Deposit> select(ComposterPolicy policy,ComposterObservation.Kind kind,ComposterObservation.Resource resource,List<Stack> inventory,int itemsRemaining){
  if(policy==null||kind==null||resource==null||inventory==null||!policy.configured()||itemsRemaining<=0||inventory.size()>36||inventory.stream().map(Stack::slot).distinct().count()!=inventory.size()||supplied(policy,resource))return Optional.empty();
  long capacity=(resource.capacity()*policy.targetPercent()+99)/100-resource.current();
  return inventory.stream().filter(stack->stack.slot()>=9).sorted(Comparator.comparingInt(Stack::slot)).flatMap(stack->{
   var item=stack.item();var material=policy.materials().get(item.skyblockId());
   if(material==null||material.kind()!=kind||!item.identified()||item.protectedItem()||!item.uuid().isEmpty()||item.count()<1||item.count()>64||item.count()>itemsRemaining||policy.protectedUuids().contains(item.uuid()))return java.util.stream.Stream.empty();
   long increase=material.units()*item.count();return increase<=capacity?java.util.stream.Stream.of(new Deposit(stack.slot(),item,kind,item.count(),increase)):java.util.stream.Stream.empty();
  }).findFirst();
 }
}
