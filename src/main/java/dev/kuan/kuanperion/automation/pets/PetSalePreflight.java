// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.ItemSnapshot;import com.google.gson.*;import com.google.gson.stream.*;import java.io.*;import java.math.BigDecimal;import java.util.*;
/** Read-only eligibility, not a George menu/price/transfer/confirmation permission. Unknown metadata fails closed. */
public final class PetSalePreflight {
 private static final Set<String> TIERS=Set.of("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC","SPECIAL");
 private static final BigDecimal XP_LIMIT=new BigDecimal("1000000000000");
 public record Kind(String type,String rarity){public Kind{if(type==null||!type.matches("[A-Z][A-Z0-9_]{0,63}")||!TIERS.contains(rarity))throw new IllegalArgumentException("Invalid pet kind");}}
 public static final class Metadata {
  private final Kind kind;private final String uuid;private final BigDecimal experience;private final boolean protectedMetadata;
  private Metadata(Kind kind,String uuid,BigDecimal experience,boolean protectedMetadata){this.kind=kind;this.uuid=uuid;this.experience=experience;this.protectedMetadata=protectedMetadata;}
  public Kind kind(){return kind;}public String uuid(){return uuid;}public BigDecimal experience(){return experience;}public boolean protectedMetadata(){return protectedMetadata;}
 }
 public record Pet(int slot,ItemSnapshot item,Metadata metadata){public Pet{Objects.requireNonNull(item);Objects.requireNonNull(metadata);if(slot<0||slot>42||!item.identified()||!item.skyblockId().equals("PET")||!item.minecraftId().equals("minecraft:player_head")||item.count()!=1||!uuid(item.uuid()).equals(metadata.uuid()))throw new IllegalArgumentException("Invalid pet projection");}}
 public record Rules(Set<Kind> allowed,BigDecimal maximumExperience,int maxPets,Set<String> protectedUuids){
  public Rules{allowed=Set.copyOf(allowed);maximumExperience=xp(maximumExperience);protectedUuids=protectedUuids.stream().map(PetSalePreflight::uuid).collect(java.util.stream.Collectors.toUnmodifiableSet());if(allowed.size()>64||protectedUuids.size()>128||maxPets<1||maxPets>10)throw new IllegalArgumentException("Invalid pet rules");}
  public static Rules defaults(){return new Rules(Set.of(),BigDecimal.ZERO,1,Set.of());}
 }
 public static final class Plan {
  private final List<Pet> pets;private final Map<Integer,ItemSnapshot> baseline;
  private Plan(List<Pet> pets,Map<Integer,ItemSnapshot> baseline){this.pets=List.copyOf(pets);this.baseline=Map.copyOf(baseline);}public List<Pet> pets(){return pets;}public Map<Integer,ItemSnapshot> baseline(){return baseline;}
 }
 public static Optional<Plan> select(List<Pet> pets,Map<Integer,ItemSnapshot> inventory,Rules rules){
  if(pets==null||pets.size()>43||rules==null||rules.allowed().isEmpty()||!complete(inventory))return Optional.empty();var slots=new HashSet<Integer>();var ids=new HashSet<String>();
  for(var p:pets)if(p==null||!slots.add(p.slot())||!ids.add(p.metadata().uuid())||!p.item().equals(inventory.get(p.slot())))return Optional.empty();
  if(inventory.values().stream().filter(i->i.skyblockId().equals("PET")).count()!=pets.size())return Optional.empty();
  var selected=pets.stream().filter(p->p.slot()>=9&&p.slot()<36&&!p.item().protectedItem()&&!p.metadata().protectedMetadata()&&!rules.protectedUuids().contains(p.metadata().uuid())&&rules.allowed().contains(p.metadata().kind())&&p.metadata().experience().compareTo(rules.maximumExperience())<=0).sorted(Comparator.comparingInt(Pet::slot)).limit(rules.maxPets()).toList();
  return selected.isEmpty()?Optional.empty():Optional.of(new Plan(selected,inventory));
 }
 public static boolean stillMatches(Plan plan,Map<Integer,ItemSnapshot> inventory){return plan!=null&&complete(inventory)&&plan.baseline.equals(inventory);}
 private static boolean complete(Map<Integer,ItemSnapshot> inv){if(inv==null||inv.size()!=43)return false;for(int i=0;i<43;i++)if(inv.get(i)==null||inv.get(i).count()>64)return false;return true;}
 private static String uuid(String value){if(value==null||!value.matches("(?:[a-fA-F0-9]{32}|[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"))throw new IllegalArgumentException("Invalid pet identity");String normalized=value.replace("-","").toLowerCase(Locale.ROOT);if(normalized.equals("0".repeat(32)))throw new IllegalArgumentException("Missing pet identity");return normalized;}
 private static BigDecimal xp(BigDecimal value){if(value==null||value.precision()>32||value.scale()< -12||value.scale()>12||value.signum()<0||value.compareTo(XP_LIMIT)>0)throw new IllegalArgumentException("Invalid pet experience");return value.stripTrailingZeros();}
 /** Bind JSON to the raw item's authoritative UUID; optional embedded UUID cannot override it. */
 public static Optional<Metadata> metadata(String raw,String itemUuid){
  try{
   String identity=uuid(itemUuid);if(raw==null||raw.isBlank()||raw.length()>16384)return Optional.empty();var reader=new JsonReader(new StringReader(raw));reader.setStrictness(Strictness.STRICT);var o=read(reader,0,new int[]{0}).getAsJsonObject();if(reader.peek()!=JsonToken.END_DOCUMENT)return Optional.empty();
   var required=Set.of("type","tier","exp","active","candyUsed","petSoulbound","noMove","hideInfo","hideRightClick");var allowed=new HashSet<>(required);allowed.addAll(Set.of("heldItem","heldItemUuid","skin","uuid","uniqueId","extraData"));if(!o.keySet().containsAll(required)||!allowed.containsAll(o.keySet()))return Optional.empty();
   var kind=new Kind(string(o,"type"),string(o,"tier"));var experience=xp(number(o,"exp"));int candy=number(o,"candyUsed").intValueExact();if(candy<0||candy>1000)return Optional.empty();
   if(present(o,"uuid")&&!uuid(string(o,"uuid")).equals(identity))return Optional.empty();
   boolean blocked=bool(o,"active")|bool(o,"petSoulbound")|bool(o,"noMove")|bool(o,"hideInfo")|bool(o,"hideRightClick")|candy>0|kind.rarity().equals("SPECIAL");
   for(var key:List.of("heldItem","skin"))if(present(o,key)){if(!string(o,key).matches("[A-Z0-9_]{1,100}"))return Optional.empty();blocked=true;}
   for(var key:List.of("heldItemUuid","uniqueId"))if(present(o,key)){if(string(o,key).isBlank())return Optional.empty();blocked=true;}
   if(present(o,"extraData")){var extras=o.getAsJsonObject("extraData");blocked|=!extras.isEmpty();}
   return Optional.of(new Metadata(kind,identity,experience,blocked));
  }catch(RuntimeException|IOException malformed){return Optional.empty();}
 }
 private static boolean present(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull();}
 private static String string(JsonObject o,String k){var v=o.getAsJsonPrimitive(k);if(v==null||!v.isString())throw new IllegalArgumentException("Invalid pet string");return v.getAsString();}
 private static boolean bool(JsonObject o,String k){var v=o.getAsJsonPrimitive(k);if(v==null||!v.isBoolean())throw new IllegalArgumentException("Invalid pet flag");return v.getAsBoolean();}
 private static BigDecimal number(JsonObject o,String k){var v=o.getAsJsonPrimitive(k);if(v==null||!v.isNumber())throw new IllegalArgumentException("Invalid pet number");return v.getAsBigDecimal();}
 private static JsonElement read(JsonReader r,int depth,int[] tokens)throws IOException{
  if(depth>8||++tokens[0]>512)throw new IllegalArgumentException("Pet metadata bound");return switch(r.peek()){
   case BEGIN_OBJECT->{var o=new JsonObject();r.beginObject();while(r.hasNext()){String k=r.nextName();if(k.length()>64||o.size()>=32||o.has(k))throw new IllegalArgumentException("Ambiguous pet metadata");o.add(k,read(r,depth+1,tokens));}r.endObject();yield o;}
   case BEGIN_ARRAY->{var a=new JsonArray();r.beginArray();while(r.hasNext()){if(a.size()>=32)throw new IllegalArgumentException("Pet metadata bound");a.add(read(r,depth+1,tokens));}r.endArray();yield a;}
   case STRING->{String v=r.nextString();if(v.length()>256)throw new IllegalArgumentException("Pet metadata bound");yield new JsonPrimitive(v);}
   case NUMBER->{String v=r.nextString();if(v.length()>64)throw new IllegalArgumentException("Pet metadata bound");var n=new BigDecimal(v);if(n.precision()>32||n.scale()< -12||n.scale()>12)throw new IllegalArgumentException("Pet numeric bound");yield new JsonPrimitive(n);}
   case BOOLEAN->new JsonPrimitive(r.nextBoolean());case NULL->{r.nextNull();yield JsonNull.INSTANCE;}default->throw new IllegalArgumentException("Invalid pet metadata");
  };
 }
}
