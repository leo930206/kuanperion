package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;
/** Provisional supplied-Aether menu fixtures, not current-server screenshots or live purchases. */
public final class BazaarPurchaseViewTest {
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","air",0,List.of(),false);
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static Map<Integer,ItemSnapshot> inventory(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,AIR);return m;}
 public static BazaarAcquisitionPlan.Plan plan(SpendingBudget budget,int amount){
  var material=new BazaarAcquisitionPlan.Material("minecraft:wheat",64,1000);
  var market=new BazaarMarket(1000,Map.of("WHEAT",new BazaarMarket.Book(List.of(new BazaarMarket.Level(10000,new BigDecimal("2"))),List.of(new BazaarMarket.Level(10000,BigDecimal.ONE)))));
  return BazaarAcquisitionPlan.plan(List.of(new BazaarAcquisitionPlan.Need("WHEAT",amount)),inventory(),new BazaarAcquisitionPlan.Rules(Map.of("WHEAT",material),1000,4096),market,SkyblockItemCatalog.parse("{\"success\":true,\"lastUpdated\":1000,\"items\":[{\"id\":\"WHEAT\",\"name\":\"Wheat\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8),1000),budget,1000).orElseThrow();
 }
 static ItemSnapshot button(String name,List<String> lore){return new ItemSnapshot("","minecraft:gold_nugget","",name,name+String.join("|",lore),1,lore,false);}
 public static MenuSnapshot menu(String title,int slot,String name,List<String> lore){
  var s=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<63;i++)s.add(new MenuSnapshot.Slot(i,AIR,i>=27));
  s.set(slot,new MenuSnapshot.Slot(slot,button(name,lore)));if(!title.toLowerCase(Locale.ROOT).contains("confirm"))s.set(16,new MenuSnapshot.Slot(16,button("Custom Amount",List.of())));
  return new MenuSnapshot(1,2,4,title,27,s);
 }
 static MenuSnapshot replace(MenuSnapshot m,int slot,ItemSnapshot item){var s=new ArrayList<>(m.slots());s.set(slot,new MenuSnapshot.Slot(slot,item,slot>=27));return new MenuSnapshot(m.worldId(),m.containerId(),m.stateId(),m.title(),m.containerSlots(),s);}
 public static void main(String[]args){
  var budget=new SpendingBudget(1000);var plan=plan(budget,64);var request=plan.requests().getFirst();
  var m=menu("Wheat",12,"Buy 64x Wheat",List.of("Per unit: 2 coins","Price: 128 coins"));
  var v=BazaarPurchaseView.inspect(m,request).orElseThrow();check(v.ticket().slot()==12&&v.total().equals(new BigDecimal("128"))&&v.contract().revalidate(v.ticket(),m),"legacy64 slot12 role + exact GUI total, not per-unit/API estimate");
  check(request.usesBudget(budget)&&!request.usesBudget(new SpendingBudget(1000)),"request keeps exact shared budget identity");
  var one=plan(new SpendingBudget(1000),1).requests().getFirst();check(BazaarPurchaseView.inspect(menu("Wheat",10,"Buy 1x Wheat",List.of("Price: 2 coins")),one).isPresent(),"legacy preset1 slot10");
  var custom=plan(new SpendingBudget(1000),7).requests().getFirst();var customMenu=menu("Confirm Instant Buy",13,"Custom Amount",List.of("Wheat","Amount: 7x","Price: 14.25 coins","Click to buy now!"));
  check(BazaarPurchaseView.inspect(customMenu,custom).orElseThrow().total().equals(new BigDecimal("14.25")),"legacy Custom Amount13 exact item/count/activation");
  for(String title:List.of("Confirm Instant Buy","Confirm Purchase"))for(String name:List.of("Confirm","Buy","Buy Instantly","Buy 64x Wheat"))check(BazaarPurchaseView.inspect(menu(title,13,name,List.of("Wheat","Amount: 64x","Price: 128 coins","Click to buy now!")),request).isPresent(),"explicit product/count protects generic original confirm role");
  for(var lore:List.of(List.of("Per unit: 2 coins"),List.of("Price: 128 coins","Price: 128 coins"),List.of("Price: 128 coins","Cost: 150 coins"),List.of("Price: 0 coins"),List.of("Price: -1 coins"),List.of("Price: 1e2 coins"),List.of("Price: 1,28 coins"),List.of("Price: 128.001 coins"),List.of("Price: 1001 coins"),List.of("Price: 128 coins","Fee: 1 coins")))check(BazaarPurchaseView.inspect(menu("Wheat",12,"Buy 64x Wheat",lore),request).isEmpty(),"missing/ambiguous/unknown fees/invalid/overlimit GUI total refuses: "+lore);
  for(String name:List.of("Buy 63x Wheat","Buy 64x Enchanted Wheat","Buy 64x Wheat Seeds","Buy 64x Wheat!"))check(BazaarPurchaseView.inspect(menu("Wheat",12,name,List.of("Price: 128 coins")),request).isEmpty(),"no name/count substring: "+name);
  check(BazaarPurchaseView.inspect(menu("Wheat",11,"Buy 64x Wheat",List.of("Price: 128 coins")),request).isEmpty(),"legacy slot candidate does not slide blindly");
  check(BazaarPurchaseView.inspect(menu("Confirm Instant Buy",13,"Confirm",List.of("Price: 128 coins")),request).isEmpty(),"generic confirm without product/count never authorizes");
  check(BazaarPurchaseView.inspect(menu("Confirm Instant Buy",13,"Custom Amount",List.of("Wheat","Amount: 7x","Price: 14 coins")),custom).isEmpty(),"missing Click to buy now stays non-actionable");
  check(BazaarPurchaseView.inspect(menu("Bazaar Warning",13,"Buy 64x Wheat",List.of("Price: 128 coins")),request).isEmpty(),"no automatic original buy-anyway warning override");
  var duplicate=replace(menu("Confirm Instant Buy",13,"Buy 64x Wheat",List.of("Price: 128 coins")),14,button("Confirm",List.of("Wheat","Amount: 64x","Price: 128 coins")));check(BazaarPurchaseView.inspect(duplicate,request).isEmpty(),"multiple confirmed roles refuse");
  var wrongId=new ItemSnapshot("CARROT_ITEM","minecraft:carrot","","Buy 64x Wheat","wrong",1,List.of("Price: 128 coins"),false);check(BazaarPurchaseView.inspect(replace(m,12,wrongId),request).isEmpty(),"native raw product conflict refuses");
  var partial=new MenuSnapshot(1,2,4,m.title(),27,m.slots().subList(0,62));check(BazaarPurchaseView.inspect(partial,request).isEmpty(),"complete native menu aliases mandatory");
  var stale=new MenuSnapshot(1,2,5,m.title(),27,m.slots());check(!v.contract().revalidate(v.ticket(),stale),"quote is bound to exact revision");
  var held=budget.reserve(900,1000);check(BazaarPurchaseView.inspect(m,request).isEmpty(),"new shared budget hold invalidates old quote");budget.release(held);
  System.out.println("BazaarPurchaseViewTest: PASS (explicit Aether legacy roles and exact provisional GUI total, product/count/budget/ambiguity guards; not current-server validation)");
 }
}
