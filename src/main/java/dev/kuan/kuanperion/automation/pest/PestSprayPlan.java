// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Prepared or selected-material path. No implicit selection, purchasing, stack moves or guessed Sacks receipt. */
public final class PestSprayPlan {
 /** PROVISIONAL five-item bound from x3/x5 modes; verify base/all-mode cost before enabling this foundation. */
 public static final int WORST_CASE_ITEMS=5;
 private final PestSprayPolicy policy;private final PestSprayTool.Tool tool;private final Map<Integer,ItemSnapshot> baseline;private final Set<Integer> sources;
 private PestSprayPlan(PestSprayPolicy p,PestSprayTool.Tool t,Map<Integer,ItemSnapshot> inv,Set<Integer> s){policy=p;tool=t;baseline=Map.copyOf(inv);sources=Set.copyOf(s);}
 public PestSprayTool.Tool tool(){return tool;}public Map<Integer,ItemSnapshot> baseline(){return baseline;}public int reservedItems(){return WORST_CASE_ITEMS;}
 public boolean stillMatches(Map<Integer,ItemSnapshot> inv){return complete(inv)&&baseline.equals(inv);}
 public static Optional<PestSprayPlan> select(PestSprayPolicy p,Map<Integer,ItemSnapshot> inv){return prepare(p,inv,true);}
 /** Preparation permits only a guarded selection workflow, never USE before an exact selected plan is rebuilt. */
 public static Optional<PestSprayPlan> prepare(PestSprayPolicy p,Map<Integer,ItemSnapshot> inv){return prepare(p,inv,false);}
 private static Optional<PestSprayPlan> prepare(PestSprayPolicy p,Map<Integer,ItemSnapshot> inv,boolean selectedOnly){
  if(p==null||!p.configured()||!complete(inv))return Optional.empty();PestSprayTool.Tool tool=null;
  for(int i=0;i<9;i++){var match=PestSprayTool.read(i,inv.get(i));if(match.isPresent()&&(!selectedOnly||match.get().selectedMaterial().equals(p.materialName()))){if(tool!=null)return Optional.empty();tool=match.get();}}
  if(tool==null)return Optional.empty();var protection=new InventoryProtection(Set.of(p.materialId()),p.protectedUuids());var sources=new HashSet<Integer>();ItemSnapshot identity=null;int total=0;
  for(int i=0;i<43;i++){
   var x=inv.get(i);if(!x.skyblockId().equals(p.materialId()))continue;
   // The server chooses the consumption source, not this client. A single protected/variant copy blocks use.
   if(i<9||i>=36||!protection.permits(x)||!x.uuid().isEmpty()||!x.minecraftId().equals(p.minecraftId())||!x.name().equals(p.materialName())||identity!=null&&!sameKind(identity,x))return Optional.empty();
   identity=x;sources.add(i);total+=x.count();
  }
  return total<WORST_CASE_ITEMS?Optional.empty():Optional.of(new PestSprayPlan(p,tool,inv,sources));
 }
 /** A pending prediction may be observed, but cannot complete without the independent receipt check. */
 public boolean possibleAfter(Map<Integer,ItemSnapshot> after){return delta(after)>=0;}
 private int delta(Map<Integer,ItemSnapshot> after){
  if(!complete(after))return -1;int decrease=0;
  for(int i=0;i<43;i++){var old=baseline.get(i);var next=after.get(i);if(old.equals(next))continue;
   if(!sources.contains(i)||next.count()>=old.count()||next.count()>0&&(!sameKind(old,next)||next.protectedItem()))return -1;
   decrease+=old.count()-next.count();
  }return decrease<=WORST_CASE_ITEMS?decrease:-1;
 }
 /** Every changed slot must be an authorized decrement with a new applied window-0 receipt. */
 public boolean receipt(Map<Integer,ItemSnapshot> after,InventoryReceipts receipts,InventoryReceipts.Stamp stamp){
  if(delta(after)<1||receipts==null||stamp==null)return false;
  for(int i:sources)if(!baseline.get(i).equals(after.get(i))&&!raw(after.get(i),receipts.after(stamp,0,i)))return false;
  return true;
 }
 public static boolean complete(Map<Integer,ItemSnapshot> inv){if(inv==null||inv.size()!=43)return false;for(int i=0;i<43;i++){var x=inv.get(i);if(x==null||x.count()>64||x.count()==0&&!air(x)||x.count()>0&&(x.minecraftId().equals("minecraft:air")||x.fingerprint().isBlank()))return false;}return true;}
 public static boolean air(ItemSnapshot x){return x!=null&&x.count()==0&&x.minecraftId().equals("minecraft:air")&&x.skyblockId().isEmpty()&&x.uuid().isEmpty();}
 private static boolean sameKind(ItemSnapshot a,ItemSnapshot b){return a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.lore().equals(b.lore());}
 public static boolean raw(ItemSnapshot a,ItemSnapshot b){return a!=null&&b!=null&&a.count()==b.count()&&sameKind(a,b);}
}
