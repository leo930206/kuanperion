package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.math.BigDecimal;import java.nio.charset.StandardCharsets;
public final class PestBaitBuyingTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final long NOW=1_800_000_000_000L;
 public static void main(String[]args){
  var b=new PestBaitBuying("minecraft:yellow_dye",64,16,1000);var p=new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of()),b);
  check(!PestBaitPolicy.defaults().canBuy()&&p.canBuy()&&!p.withEnabled(false).canBuy()&&!p.withPerTrap(8).canBuy(),"buying requires separate native product/count/budget consent within whole-stack consumption ceiling");
  var inv=PestBaitButtonPlanTest.inventory(0);inv.put(9,PestBaitButtonViewTest.AIR);var budget=new SpendingBudget(1000);
  var market=new BazaarMarket(NOW,Map.of("TASTY_CHEESE",new BazaarMarket.Book(List.of(new BazaarMarket.Level(100,BigDecimal.TEN)),List.of())));
  var catalog=SkyblockItemCatalog.parse(("{\"success\":true,\"lastUpdated\":"+NOW+",\"items\":[{\"id\":\"TASTY_CHEESE\",\"name\":\"Tasty Cheese\"}]}").getBytes(StandardCharsets.UTF_8),NOW);
  var plan=BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need(p.rules().baitId(),b.targetCount())),inv,b.rules(p.rules().baitId()),market,catalog,budget,NOW).orElseThrow();
  check(plan.requests().size()==1&&plan.requests().getFirst().quantity()==16&&plan.requests().getFirst().usesBudget(budget)&&budget.available()==1000,"one exact stock deficit retains parent budget identity without spending from estimates");
  var held=budget.reserve(900,1000);check(held!=null&&BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need("TASTY_CHEESE",16)),inv,b.rules("TASTY_CHEESE"),market,catalog,budget,NOW).isEmpty(),"remaining original-run cap is never reset for another trap");
  for(var bad:List.of(new PestBaitBuying("",64,16,1000),new PestBaitBuying("minecraft:yellow_dye",64,0,1000),new PestBaitBuying("minecraft:yellow_dye",64,16,0)))check(!new PestBaitPolicy(true,p.rules(),bad).canBuy(),"incomplete consent never buys");
  System.out.println("PestBaitBuyingTest: PASS");
 }
}
