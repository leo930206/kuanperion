// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.util.*;
/** Exact book metadata preflight. Does not open an anvil, consume books or infer authority from lore. */
public final class BookCombinePlanner {
 public record Book(int inventorySlot,ItemSnapshot item,Map<String,Integer> enchantments,String compatibility){
  public Book{
   Objects.requireNonNull(item);Objects.requireNonNull(compatibility);enchantments=Map.copyOf(enchantments);
   if(inventorySlot< -1||inventorySlot>35||enchantments.size()>32||enchantments.entrySet().stream().anyMatch(e->!key(e.getKey())||e.getValue()<1||e.getValue()>10))throw new IllegalArgumentException("Invalid book metadata");
  }
 }
 public record Plan(Book first,Book second,String enchantment,int inputLevel){
  public Plan{Objects.requireNonNull(first);Objects.requireNonNull(second);if(!key(enchantment)||inputLevel<1||inputLevel>9)throw new IllegalArgumentException("Invalid combine plan");}
  public int outputLevel(){return inputLevel+1;}
 }
 private final Map<String,Integer> ceilings;private final InventoryProtection protection;
 public BookCombinePlanner(Map<String,Integer> ceilings,Set<String> protectedUuids){
  this.ceilings=Map.copyOf(ceilings);
  if(ceilings.size()>64||ceilings.entrySet().stream().anyMatch(e->!key(e.getKey())||e.getValue()<1||e.getValue()>10))throw new IllegalArgumentException("Invalid enchantment authorization");
  protection=new InventoryProtection(Set.of("ENCHANTED_BOOK"),protectedUuids);
 }
 public Optional<Plan> select(List<Book> inventory){
  if(inventory==null||inventory.size()>36)return Optional.empty();
  var sorted=inventory.stream().filter(Objects::nonNull).sorted(Comparator.comparingInt(Book::inventorySlot)).toList();
  if(sorted.stream().map(Book::inventorySlot).distinct().count()!=sorted.size())return Optional.empty();
  for(int i=0;i<sorted.size();i++)for(int j=i+1;j<sorted.size();j++){
   var a=sorted.get(i);var b=sorted.get(j);if(!eligible(a)||!eligible(b)||!a.enchantments().equals(b.enchantments())||!a.compatibility().equals(b.compatibility()))continue;
   String id=a.enchantments().keySet().iterator().next();int level=a.enchantments().get(id);
   return Optional.of(new Plan(a,b,id,level));
  }
  return Optional.empty();
 }
 public boolean stillMatches(Plan plan,List<Book> current){return authorized(plan)&&current!=null&&current.stream().filter(plan.first()::equals).count()==1&&current.stream().filter(plan.second()::equals).count()==1;}
 public boolean isExpectedResult(Plan plan,Book result){
  return authorized(plan)&&identity(result)&&result.compatibility().equals(plan.first().compatibility())&&result.enchantments().equals(Map.of(plan.enchantment(),plan.outputLevel()));
 }
 private boolean authorized(Plan p){return p!=null&&p.first().inventorySlot()!=p.second().inventorySlot()&&eligible(p.first())&&eligible(p.second())&&p.first().compatibility().equals(p.second().compatibility())&&p.first().enchantments().equals(Map.of(p.enchantment(),p.inputLevel()))&&p.first().enchantments().equals(p.second().enchantments());}
 private boolean eligible(Book book){
  if(!identity(book)||book.inventorySlot()<9||book.enchantments().size()!=1)return false;
  var entry=book.enchantments().entrySet().iterator().next();return entry.getValue()<ceilings.getOrDefault(entry.getKey(),0);
 }
 private boolean identity(Book book){return book!=null&&protection.permits(book.item())&&book.item().name().equals("Enchanted Book")&&book.item().minecraftId().equals("minecraft:enchanted_book")&&book.item().count()==1&&!book.compatibility().isBlank()&&book.compatibility().length()<=16384;}
 private static boolean key(String id){return id!=null&&id.matches("[a-z][a-z0-9_]{0,63}");}
}
