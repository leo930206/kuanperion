// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Conservative read-only estimate, not a purchase, coin reservation or submit authorization. */
public final class VisitorValuation {
 public record Rules(Set<String> visitors,Set<String> materialIds,long maximumOfferCoins,long maximumBuyCoins,int minimumCopper){
  public Rules{visitors=Set.copyOf(visitors);materialIds=Set.copyOf(materialIds);
   if(visitors.size()>128||materialIds.size()>256||visitors.stream().anyMatch(s->s.isBlank()||s.length()>128||!s.equals(s.trim())||java.util.regex.Pattern.compile("[\\p{C}§]").matcher(s).find())||materialIds.stream().anyMatch(s->!s.matches("[A-Z0-9_:-]{1,100}"))||maximumOfferCoins<0||maximumOfferCoins>1_000_000_000_000L||maximumBuyCoins<0||maximumBuyCoins>1_000_000_000_000L||minimumCopper<0||minimumCopper>1_000_000)throw new IllegalArgumentException("Invalid Visitor rules");
  }
  public static Rules defaults(){return new Rules(Set.of(),Set.of(),0,0,1);}
 }
 public record Estimate(VisitorOffer offer,long offerCoins,long purchaseCoins,Map<String,Integer> missing,long marketUpdatedAt){public Estimate{Objects.requireNonNull(offer);missing=Map.copyOf(missing);}}
 private VisitorValuation(){}
 /** Require all36 main/hotbar slots so unavailable data cannot masquerade as zero demand. */
 public static Optional<Estimate> estimate(VisitorOffer offer,Rules rules,Map<Integer,ItemSnapshot> inventory,BazaarMarket market,long now){
  if(offer==null||rules==null||market==null||inventory==null||inventory.size()!=36||!rules.visitors().contains(offer.visitor())||offer.copper()<rules.minimumCopper()||rules.maximumOfferCoins()==0)return Optional.empty();
  var available=new HashMap<String,Integer>();
  try{
   for(int slot=0;slot<36;slot++){
    var item=inventory.get(slot);if(item==null||item.count()>64)return Optional.empty();
    if(slot>=9&&item.identified()&&!item.protectedItem()&&item.uuid().isEmpty())available.merge(item.skyblockId(),item.count(),Math::addExact);
   }
   long full=0,buy=0;var missing=new LinkedHashMap<String,Integer>();
   for(var need:offer.requirements()){
    if(!rules.materialIds().contains(need.itemId()))return Optional.empty();
    var total=market.estimateBuy(need.itemId(),need.quantity(),now);if(total.isEmpty())return Optional.empty();full=Math.addExact(full,total.get().coins());
    int deficit=Math.max(0,need.quantity()-available.getOrDefault(need.itemId(),0));
    if(deficit>0){var quote=market.estimateBuy(need.itemId(),deficit,now);if(quote.isEmpty())return Optional.empty();buy=Math.addExact(buy,quote.get().coins());missing.put(need.itemId(),deficit);}
   }
   return full<=rules.maximumOfferCoins()&&buy<=rules.maximumBuyCoins()?Optional.of(new Estimate(offer,full,buy,missing,market.updatedAt())):Optional.empty();
  }catch(RuntimeException malformed){return Optional.empty();}
 }
}
