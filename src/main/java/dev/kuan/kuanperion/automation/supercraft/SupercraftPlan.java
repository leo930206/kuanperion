// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Read-only quantity/material preflight. A Recipe is data, NOT proof of an actual GUI recipe or permission to click. */
public final class SupercraftPlan {
 public record Recipe(String outputId,int outputPerCraft,int stackLimit,Map<String,Integer> inputsPerCraft){
  public Recipe{inputsPerCraft=Map.copyOf(inputsPerCraft);if(!id(outputId)||outputPerCraft<1||outputPerCraft>64||stackLimit<1||stackLimit>64||outputPerCraft>stackLimit||inputsPerCraft.isEmpty()||inputsPerCraft.size()>9||inputsPerCraft.containsKey(outputId)||inputsPerCraft.entrySet().stream().anyMatch(e->!id(e.getKey())||e.getValue()<1||e.getValue()>4096))throw new IllegalArgumentException("Invalid Supercraft recipe data");}
 }
 public record Rules(Set<String> outputs,Set<String> ingredients,int maxOutput,int maxConsumed,int maxCrafts,Set<String> protectedUuids){
  public Rules{outputs=Set.copyOf(outputs);ingredients=Set.copyOf(ingredients);protectedUuids=Set.copyOf(protectedUuids);if(outputs.size()>64||ingredients.size()>256||outputs.stream().anyMatch(s->!id(s))||ingredients.stream().anyMatch(s->!id(s))||maxOutput<0||maxOutput>4096||maxConsumed<0||maxConsumed>65536||maxCrafts<0||maxCrafts>4096||protectedUuids.size()>128||protectedUuids.stream().anyMatch(s->s.isBlank()||s.length()>128))throw new IllegalArgumentException("Invalid Supercraft authorization");}
  public static Rules defaults(){return new Rules(Set.of(),Set.of(),0,0,0,Set.of());}
 }
 /** Private construction: only a complete checked inventory can create an immutable plan. */
 public static final class Plan {
  private final Recipe recipe;private final int crafts,outputCount;private final Map<String,Integer> inputs;private final Map<Integer,ItemSnapshot> baseline;
  private Plan(Recipe recipe,int crafts,int output,Map<String,Integer> inputs,Map<Integer,ItemSnapshot> inventory){this.recipe=recipe;this.crafts=crafts;outputCount=output;this.inputs=Map.copyOf(inputs);baseline=Map.copyOf(inventory);}
  public Recipe recipe(){return recipe;}public int crafts(){return crafts;}public int outputCount(){return outputCount;}public Map<String,Integer> inputs(){return inputs;}public Map<Integer,ItemSnapshot> baseline(){return baseline;}
 }
 private SupercraftPlan(){}
 public static Optional<Plan> select(Recipe recipe,int outputCount,Rules rules,Map<Integer,ItemSnapshot> inventory){
  if(recipe==null||rules==null||!complete(inventory)||outputCount<=0||outputCount>rules.maxOutput()||!rules.outputs().contains(recipe.outputId())||!rules.ingredients().containsAll(recipe.inputsPerCraft().keySet())||outputCount%recipe.outputPerCraft()!=0)return Optional.empty();
  int crafts=outputCount/recipe.outputPerCraft();if(crafts>rules.maxCrafts())return Optional.empty();var needs=new TreeMap<String,Integer>();long total=0;
  for(var e:recipe.inputsPerCraft().entrySet()){long amount=(long)e.getValue()*crafts;total+=amount;if(amount>65536||total>rules.maxConsumed())return Optional.empty();needs.put(e.getKey(),(int)amount);}
  var protection=new InventoryProtection(rules.ingredients(),rules.protectedUuids());var available=new HashMap<String,Integer>();long capacity=0;
  for(int slot=0;slot<43;slot++){
   var item=inventory.get(slot);
   // The server chooses which matching inputs to consume; never discount protected copies and hope it picks another stack.
   if(item.count()>0&&needs.containsKey(item.skyblockId())){if(slot<9||slot>=36||!item.uuid().isEmpty()||!protection.permits(item))return Optional.empty();available.merge(item.skyblockId(),item.count(),Integer::sum);}
   if(slot<9||slot>=36)continue;
   if(item.count()==0)capacity+=recipe.stackLimit();
   else if(item.skyblockId().equals(recipe.outputId())){if(!item.identified()||item.protectedItem()||!item.uuid().isEmpty()||item.count()>recipe.stackLimit())return Optional.empty();capacity+=recipe.stackLimit()-item.count();}
  }
  if(capacity<outputCount||needs.entrySet().stream().anyMatch(e->available.getOrDefault(e.getKey(),0)<e.getValue()))return Optional.empty();
  return Optional.of(new Plan(recipe,crafts,outputCount,needs,inventory));
 }
 public static boolean stillMatches(Plan plan,Map<Integer,ItemSnapshot> current){return plan!=null&&complete(current)&&plan.baseline().equals(current);}
 static boolean complete(Map<Integer,ItemSnapshot> inventory){
  if(inventory==null||inventory.size()!=43)return false;for(int slot=0;slot<43;slot++){var item=inventory.get(slot);if(item==null||item.count()>64||item.count()==0&&(!item.minecraftId().equals("minecraft:air")||!item.skyblockId().isEmpty()||!item.uuid().isEmpty()))return false;}return true;
 }
 private static boolean id(String id){return id!=null&&id.matches("[A-Z0-9_:-]{1,100}");}
}
