package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;import java.nio.charset.StandardCharsets;
/** Deficit/capacity/price planning only; quotes never create a purchase Offer or reserve coins. */
public final class BazaarAcquisitionPlanTest {
 static final long NOW=1_800_000_000_000L;static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
 static ItemSnapshot item(String id,int n,boolean p){return new ItemSnapshot(id,"minecraft:"+(id.equals("CARROT_ITEM")?"carrot":id.toLowerCase(Locale.ROOT)),"",id,id,n,List.of(),p);}
 static Map<Integer,ItemSnapshot> inv(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,AIR);return m;}
 static SkyblockItemCatalog catalog(){return SkyblockItemCatalog.parse(("{\"success\":true,\"lastUpdated\":"+NOW+",\"items\":[{\"id\":\"WHEAT\",\"name\":\"Wheat\"},{\"id\":\"CARROT_ITEM\",\"name\":\"Carrot\"}]}").getBytes(StandardCharsets.UTF_8),NOW);}
 static BazaarMarket market(){return new BazaarMarket(NOW,Map.of("WHEAT",new BazaarMarket.Book(List.of(new BazaarMarket.Level(100,new BigDecimal("4"))),List.of()),"CARROT_ITEM",new BazaarMarket.Book(List.of(new BazaarMarket.Level(100,new BigDecimal("2"))),List.of())));}
 static BazaarAcquisitionPlan.Rules rules(long cap){return new BazaarAcquisitionPlan.Rules(Map.of("WHEAT",new BazaarAcquisitionPlan.Material("minecraft:wheat",64,20),"CARROT_ITEM",new BazaarAcquisitionPlan.Material("minecraft:carrot",64,20)),cap,128);}
 static List<BazaarAcquisitionPlan.Need> needs(){return List.of(new BazaarAcquisitionPlan.Need("WHEAT",3),new BazaarAcquisitionPlan.Need("CARROT_ITEM",5));}
 public static void main(String[]args){
  var inv=inv();inv.put(9,item("WHEAT",1,false));var budget=new SpendingBudget(100);
  var p=BazaarAcquisitionPlan.plan(needs(),inv,rules(100),market(),catalog(),budget,NOW).orElseThrow();check(p.requests().size()==2&&p.requests().get(0).quantity()==2&&p.requests().get(1).quantity()==5&&p.estimatedCoins()==18&&budget.available()==100,"exact missing counts, full depth conservative estimate, no reservation");
  check(BazaarAcquisitionPlan.plan(needs(),inv,rules(20),market(),catalog(),budget,NOW).isEmpty(),"cannot accidentally use larger shared budget than this owning module cap");
  var r=p.requests().get(0);check(r.productId().equals("WHEAT")&&r.displayName().equals("Wheat")&&r.authorizes("WHEAT",2,new BigDecimal("19.99"))&&!r.authorizes("WHEAT",3,BigDecimal.ONE)&&!r.authorizes("CARROT_ITEM",2,BigDecimal.ONE)&&!r.authorizes("WHEAT",2,new BigDecimal("20.01")),"configured GUI ceiling is distinct from readonly API estimate");
  var held=budget.reserve(95,100);check(held!=null&&!r.authorizes("WHEAT",2,new BigDecimal("19.99")),"latest shared money hold cannot be bypassed by old estimate");budget.release(held);
  check(p.stillMatches(inv),"private immutable baseline");inv.put(40,item("STONE",1,true));check(!p.stillMatches(inv),"equipment drift invalidates planning");inv=inv();
  check(BazaarAcquisitionPlan.plan(needs(),inv,BazaarAcquisitionPlan.Rules.defaults(),market(),catalog(),budget,NOW).isEmpty(),"default zero/empty no buys");
  check(BazaarAcquisitionPlan.plan(needs(),inv,rules(10),market(),catalog(),budget,NOW).isEmpty(),"whole estimated basket budget");
  check(BazaarAcquisitionPlan.plan(needs(),inv,rules(100),market(),catalog(),new SpendingBudget(1),NOW).isEmpty(),"shared original run remaining budget");
  check(BazaarAcquisitionPlan.plan(needs(),inv,rules(100),market(),catalog(),budget,NOW+120001).isEmpty(),"stale public quote");
  check(BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need("UNKNOWN",1)),inv,rules(100),market(),catalog(),budget,NOW).isEmpty(),"unknown not guessed from display name");
  check(BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need("WHEAT",1),new BazaarAcquisitionPlan.Need("WHEAT",1)),inv,rules(100),market(),catalog(),budget,NOW).isEmpty(),"duplicate requirements rejected rather than silently double-buy");
  for(int slot:List.of(0,9,40)){var blocked=inv();blocked.put(slot,item("WHEAT",1,true));check(BazaarAcquisitionPlan.plan(needs(),blocked,rules(100),market(),catalog(),budget,NOW).isEmpty(),"protected same-ID veto: "+slot);}
  var crowded=inv();for(int i=9;i<35;i++)crowded.put(i,item("STONE",64,false));check(BazaarAcquisitionPlan.plan(needs(),crowded,rules(100),market(),catalog(),budget,NOW).isEmpty(),"two kinds need two empty main destinations, no assumed merging/hotbar");
  var full=inv();full.put(9,item("WHEAT",3,false));full.put(10,item("CARROT_ITEM",5,false));var done=BazaarAcquisitionPlan.plan(needs(),full,rules(100),market(),catalog(),budget,NOW).orElseThrow();check(done.requests().isEmpty()&&done.estimatedCoins()==0,"no deficits means no purchase/data claim");
  System.out.println("BazaarAcquisitionPlanTest: PASS (explicit native products/main deficits, protected43/duplicates/capacity, catalog IDs, depth/age/per-item/shared-run caps, immutable no-effect estimates; no GUI purchase authority)");
 }
}
