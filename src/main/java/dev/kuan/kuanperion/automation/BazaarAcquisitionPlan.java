// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;
/** Read-only explicit main-inventory deficits and estimates, never a GUI order or permission to spend. */
public final class BazaarAcquisitionPlan {
 private static final long MAX_COINS=1_000_000_000_000L;
 public record Need(String productId,int targetCount){public Need{if(!id(productId)||targetCount<1||targetCount>4096)throw new IllegalArgumentException("Invalid material requirement");}}
 public record Material(String minecraftId,int stackLimit,long maximumCoins){public Material{if(minecraftId==null||!minecraftId.matches("minecraft:[a-z0-9_]{1,100}")||minecraftId.equals("minecraft:air")||stackLimit<1||stackLimit>64||maximumCoins<1||maximumCoins>MAX_COINS)throw new IllegalArgumentException("Invalid purchase allowlist");}}
 public record Rules(Map<String,Material> materials,long maximumCoins,int maximumItems){
  public Rules{materials=Map.copyOf(materials);if(materials.size()>8||materials.keySet().stream().anyMatch(k->!id(k))||maximumCoins<0||maximumCoins>MAX_COINS||maximumItems<1||maximumItems>4096)throw new IllegalArgumentException("Invalid acquisition rules");}
  public static Rules defaults(){return new Rules(Map.of(),0,128);}
 }
 public static final class Request {
  private final String productId,displayName;private final int quantity;private final Material material;private final long estimate;private final SpendingBudget budget;
  private Request(String id,String name,int quantity,Material material,long estimate,SpendingBudget budget){this.budget=budget;this.productId=id;this.displayName=name;this.quantity=quantity;this.material=material;this.estimate=estimate;}
  public String productId(){return productId;}public String displayName(){return displayName;}public int quantity(){return quantity;}public Material material(){return material;}public long estimatedCoins(){return estimate;}
  /** Necessary configured bounds only: caller still needs actual title/container/button/native/cursor proof and shared budget reservation. */
  public boolean authorizes(String id,int amount,BigDecimal actualGuiCost){return productId.equals(id)&&quantity==amount&&actualGuiCost!=null&&actualGuiCost.signum()>0&&actualGuiCost.scale()<=2&&actualGuiCost.compareTo(BigDecimal.valueOf(Math.min(material.maximumCoins(),budget.available())))<=0;}
 }
 public static final class Plan {
  private final List<Request> requests;private final Map<Integer,ItemSnapshot> baseline;private final long estimate;
  private Plan(List<Request> requests,Map<Integer,ItemSnapshot> baseline,long estimate){this.requests=List.copyOf(requests);this.baseline=Map.copyOf(baseline);this.estimate=estimate;}
  public List<Request> requests(){return requests;}public long estimatedCoins(){return estimate;}public boolean stillMatches(Map<Integer,ItemSnapshot> inventory){return baseline.equals(inventory);}
 }
 public static Optional<Plan> plan(List<Need> needs,Map<Integer,ItemSnapshot> inventory,Rules rules,BazaarMarket market,SkyblockItemCatalog catalog,SpendingBudget budget,long nowMillis){
  if(needs==null||needs.isEmpty()||needs.size()>8||rules==null||rules.materials().isEmpty()||rules.maximumCoins()==0||budget==null||budget.limit()>rules.maximumCoins()||!complete(inventory))return Optional.empty();
  var seen=new HashSet<String>();var requests=new ArrayList<Request>();int requiredSlots=0,totalItems=0;long coins=0;
  int empty=0;for(int i=9;i<36;i++)if(inventory.get(i).count()==0)empty++;
  for(var need:needs){
   if(need==null||!seen.add(need.productId()))return Optional.empty();var material=rules.materials().get(need.productId());if(material==null)return Optional.empty();
   int owned=0;for(int slot=0;slot<43;slot++){var item=inventory.get(slot);if(item.count()==0||!item.skyblockId().equals(need.productId()))continue;
    if(slot<9||slot>=36||item.protectedItem()||!item.uuid().isBlank()||!item.identified()||!item.minecraftId().equals(material.minecraftId())||item.count()>material.stackLimit())return Optional.empty();owned+=item.count();
   }
   int missing=Math.max(0,need.targetCount()-owned);if(missing==0)continue;
   totalItems+=missing;requiredSlots+=(missing+material.stackLimit()-1)/material.stackLimit();if(totalItems>rules.maximumItems()||requiredSlots>empty||market==null||catalog==null)return Optional.empty();
   String name=null;for(var entry:catalog.names().entrySet())if(entry.getValue().equals(need.productId())){if(name!=null)return Optional.empty();name=entry.getKey();}
   if(name==null||!name.matches("[A-Za-z0-9][A-Za-z0-9 '\\-:()+]{0,95}"))return Optional.empty();
   var quote=market.estimateBuy(need.productId(),missing,nowMillis).orElse(null);if(quote==null||quote.coins()<1||quote.coins()>material.maximumCoins())return Optional.empty();
   try{coins=Math.addExact(coins,quote.coins());}catch(ArithmeticException overflow){return Optional.empty();}
   if(coins>rules.maximumCoins()||coins>budget.available())return Optional.empty();requests.add(new Request(need.productId(),name,missing,material,quote.coins(),budget));
  }
  return Optional.of(new Plan(requests,inventory,coins));
 }
 private static boolean id(String value){return value!=null&&value.matches("[A-Z0-9_:-]{1,100}");}
 private static boolean complete(Map<Integer,ItemSnapshot> inventory){if(inventory==null||inventory.size()!=43)return false;var ids=new HashSet<String>();for(int i=0;i<43;i++){var x=inventory.get(i);if(x==null||x.count()>64||x.count()==0&&(!x.minecraftId().equals("minecraft:air")||!x.skyblockId().isEmpty()||!x.uuid().isEmpty())||!x.uuid().isBlank()&&!ids.add(x.uuid().replace("-","").toLowerCase(Locale.ROOT)))return false;}return true;}
 private BazaarAcquisitionPlan(){}
}
