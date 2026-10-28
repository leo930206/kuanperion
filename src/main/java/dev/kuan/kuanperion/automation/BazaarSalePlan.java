// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.math.BigDecimal;
import java.util.*;

/** Explicit whole-commodity disposal plan and gross estimate, never a native sale or net-fee assumption. */
public final class BazaarSalePlan {
 private static final long MAX_COINS=1_000_000_000_000L;
 /** The approved native exemplar pins metadata/name/lore, not just a raw ID or whatever happens to be present. */
 public record Material(ItemSnapshot exemplar,int stackLimit,BigDecimal minimumUnitNet) {
  public Material {
   if(exemplar==null||!exemplar.identified()||exemplar.protectedItem()||!exemplar.uuid().isBlank()||!id(exemplar.skyblockId())||!exemplar.minecraftId().matches("minecraft:[a-z0-9_]{1,100}")||exemplar.minecraftId().equals("minecraft:air")||stackLimit<1||stackLimit>64||exemplar.count()>stackLimit||exemplar.fingerprint().length()>65536||exemplar.name().length()>512||exemplar.lore().size()>128||exemplar.lore().stream().anyMatch(l->l.length()>512)||!money(minimumUnitNet))throw new IllegalArgumentException("Invalid approved commodity");
   minimumUnitNet=minimumUnitNet.stripTrailingZeros();
  }
 }
 public record Rules(Map<String,Material> materials,int maximumProducts,int maximumStacks,int maximumItems) {
  public Rules {
   materials=Map.copyOf(materials);
   if(materials.size()>64||materials.entrySet().stream().anyMatch(e->!id(e.getKey())||!e.getKey().equals(e.getValue().exemplar().skyblockId()))||maximumProducts<1||maximumProducts>27||maximumStacks<1||maximumStacks>27||maximumItems<1||maximumItems>4096)throw new IllegalArgumentException("Invalid sale limits");
  }
  public static Rules defaults(){return new Rules(Map.of(),3,5,320);}
 }
 public static final class Sale {
  private final String productId;private final Material material;private final int quantity;private final List<Integer> slots;private final long gross;private final BigDecimal minimum;
  private Sale(String id,Material material,int quantity,List<Integer> slots,long gross,BigDecimal minimum){productId=id;this.material=material;this.quantity=quantity;this.slots=List.copyOf(slots);this.gross=gross;this.minimum=minimum;}
  public String productId(){return productId;}public Material material(){return material;}public int quantity(){return quantity;}public List<Integer> slots(){return slots;}public long estimatedGrossCoins(){return gross;}public BigDecimal minimumNetCoins(){return minimum;}
  /** Necessary configured bounds only. Never proves GUI semantics, tax, custody, ownership or permission to click. */
  public boolean withinConfiguredBounds(String id,int quantity,BigDecimal actualGuiNet){return productId.equals(id)&&this.quantity==quantity&&money(actualGuiNet)&&actualGuiNet.compareTo(minimum)>=0;}
 }
 public static final class Plan {
  private final List<Sale> sales;private final Map<Integer,ItemSnapshot> baseline;private final long gross;
  private Plan(List<Sale> sales,Map<Integer,ItemSnapshot> baseline,long gross){this.sales=List.copyOf(sales);this.baseline=Map.copyOf(baseline);this.gross=gross;}
  public List<Sale> sales(){return sales;}public Map<Integer,ItemSnapshot> baseline(){return baseline;}public long estimatedGrossCoins(){return gross;}
  public boolean stillMatches(Map<Integer,ItemSnapshot> current){return baseline.equals(current);}
 }
 public static Optional<Plan> plan(Map<Integer,ItemSnapshot> inventory,Rules rules,BazaarMarket market,long nowMillis) {
  if(rules==null||rules.materials().isEmpty()||market==null||!complete(inventory))return Optional.empty();
  var sales=new ArrayList<Sale>();int stacks=0,items=0;long gross=0;
  for(var entry:new TreeMap<>(rules.materials()).entrySet()) {
   String id=entry.getKey();var material=entry.getValue();var slots=new ArrayList<Integer>();int quantity=0;
   for(int slot=0;slot<43;slot++) {
    var item=inventory.get(slot);if(item.count()==0||!item.skyblockId().equals(id))continue;
    // Server-side bulk selection must never choose a protected/hotbar/equipment/variant same-ID stack.
    if(slot<9||slot>=36||item.protectedItem()||!item.uuid().isBlank()||item.count()>material.stackLimit()||!sameIdentity(item,material.exemplar()))return Optional.empty();
    quantity+=item.count();slots.add(slot);
   }
   if(quantity==0)continue;
   stacks+=slots.size();items+=quantity;
   if(stacks>rules.maximumStacks()||items>rules.maximumItems()||sales.size()>=rules.maximumProducts())return Optional.empty();
   var minimum=material.minimumUnitNet().multiply(BigDecimal.valueOf(quantity));if(minimum.compareTo(BigDecimal.valueOf(MAX_COINS))>0)return Optional.empty();
   BazaarMarket.Estimate estimate;
   try{estimate=market.estimateSell(id,quantity,nowMillis).orElse(null);}catch(RuntimeException unavailable){return Optional.empty();}
   if(estimate==null||estimate.coins()<1||estimate.coins()>MAX_COINS||BigDecimal.valueOf(estimate.coins()).compareTo(minimum)<0)return Optional.empty();
   try{gross=Math.addExact(gross,estimate.coins());}catch(ArithmeticException overflow){return Optional.empty();}
   if(gross>MAX_COINS)return Optional.empty();sales.add(new Sale(id,material,quantity,slots,estimate.coins(),minimum));
  }
  if(sales.isEmpty())return Optional.empty();sales.sort(Comparator.comparingInt(s->s.slots().get(0)));
  return Optional.of(new Plan(sales,inventory,gross));
 }
 private static boolean complete(Map<Integer,ItemSnapshot> inventory) {
  if(inventory==null||inventory.size()!=43)return false;var identities=new HashSet<String>();
  for(int slot=0;slot<43;slot++){var item=inventory.get(slot);if(item==null||item.count()>64||item.count()==0&&(!item.minecraftId().equals("minecraft:air")||!item.skyblockId().isEmpty()||!item.uuid().isEmpty())||item.count()>0&&item.minecraftId().equals("minecraft:air")||!item.uuid().isBlank()&&!identities.add(item.uuid().replace("-","").toLowerCase(Locale.ROOT)))return false;}
  return true;
 }
 private static boolean sameIdentity(ItemSnapshot a,ItemSnapshot b){return a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.lore().equals(b.lore());}
 private static boolean id(String value){return value!=null&&value.matches("[A-Z0-9_:-]{1,100}");}
 private static boolean money(BigDecimal value){return value!=null&&value.precision()<=32&&value.scale()>=-12&&value.scale()<=12&&value.signum()>0&&value.compareTo(BigDecimal.valueOf(MAX_COINS))<=0;}
 private BazaarSalePlan(){}
}
