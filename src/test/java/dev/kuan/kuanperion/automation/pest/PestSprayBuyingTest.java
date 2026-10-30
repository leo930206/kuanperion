package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayBuyingTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var p=PestSprayPlanTest.policy();var b=new PestSprayBuying(16,1000);var inv=PestSprayPlanTest.inventory();inv.put(9,PestSprayPlanTest.AIR);
  check(!PestSprayBuying.defaults().configured()&&b.configured()&&!new PestSprayBuying(4,1000).configured(),"stock purchase independent positive cap and worst-case stock consent");
  check(b.rules(p).materials().get(p.materialId()).minecraftId().equals(p.minecraftId())&&b.rules(p).maximumCoins()==1000&&b.rules(p).maximumItems()==16,"fixed exact native commodity, bounded deficit, no implicit second budget");
  check(PestSprayPlan.prepare(p,inv).isEmpty(),"no stock never grants use or selection authority");
  var plan=PestSprayPlan.prepareForBuying(p,inv).orElseThrow();check(plan.tool().slot()==4&&plan.stillMatches(inv),"buy preparation preserves original full43 and exact known tool");
  var after=new HashMap<>(inv);after.put(9,PestSprayPlanTest.material(16));var receipts=new InventoryReceipts();var stamp=receipts.stamp(1);
  check(!plan.acquired(after,b,receipts,stamp),"predicted delivery not proof");receipts.observe(1,0,9,after.get(9));check(plan.acquired(after,b,receipts,stamp),"exact authorized target, canonical applied receipts and preserved nonmaterial43");
  for(int slot:List.of(0,8,36,42)){var bad=new HashMap<>(inv);bad.put(slot,PestSprayPlanTest.material(1));check(PestSprayPlan.prepareForBuying(p,bad).isEmpty(),"bad consumption source cannot be repaired by buying "+slot);}
  for(int mode=0;mode<4;mode++){var bad=new HashMap<>(after);switch(mode){case 0->bad.put(42,PestSprayPlanTest.material(1));case 1->bad.put(4,PestSprayPlanTest.AIR);case 2->bad.put(9,PestSprayPlanTest.material(15));case 3->bad.put(9,new ItemSnapshot(p.materialId(),p.minecraftId(),"",p.materialName(),"forged",16,List.of(),true));}check(!plan.acquired(bad,b,receipts,stamp),"unrelated/tool/amount/protected delta cannot qualify "+mode);}
  inv.put(9,PestSprayPlanTest.material(2));plan=PestSprayPlan.prepareForBuying(p,inv).orElseThrow();after=new HashMap<>(inv);after.put(9,PestSprayPlanTest.material(16));stamp=receipts.stamp(1);receipts.observe(1,0,9,after.get(9));check(plan.acquired(after,b,receipts,stamp),"partial stock purchases deficit, not target again");
  after.put(9,PestSprayPlanTest.AIR);after.put(10,PestSprayPlanTest.material(16));receipts.observe(1,0,9,after.get(9));receipts.observe(1,0,10,after.get(10));check(!plan.acquired(after,b,receipts,stamp),"no old stock consumption/moving disguised as acquisition");
  System.out.println("PestSprayBuyingTest: PASS");
 }
}
