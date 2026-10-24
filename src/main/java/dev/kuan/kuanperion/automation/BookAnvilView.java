// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** A declared server layout, not guessed slot numbers. No default live layout or GUI caller. */
public record BookAnvilView(Layout layout,MenuSnapshot menu,Map<Integer,BookCombinePlanner.Book> books){
 public record Layout(String title,int size,int firstInput,int secondInput,int preview,int combine,int result,Set<String> emptyMaterials){
  public Layout{emptyMaterials=Set.copyOf(emptyMaterials);if(!"Anvil".equals(title)||size!=54||firstInput==secondInput||java.util.stream.IntStream.of(firstInput,secondInput,preview,combine,result).anyMatch(i->i<0||i>=size)||Set.of(firstInput,secondInput).stream().anyMatch(i->i==preview||i==combine||i==result)||emptyMaterials.isEmpty()||!Set.of("minecraft:air","minecraft:barrier","minecraft:gray_stained_glass_pane","minecraft:red_stained_glass_pane","minecraft:lime_stained_glass_pane","minecraft:black_stained_glass_pane").containsAll(emptyMaterials))throw new IllegalArgumentException("Invalid declared anvil layout");}
  public MenuContract contract(){return new MenuContract(Set.of(title),size);}
 }
 public BookAnvilView{
  Objects.requireNonNull(layout);Objects.requireNonNull(menu);books=Map.copyOf(books);
  if(!layout.contract().matches(menu)||menu.containerId()<=0||menu.slots().size()!=layout.size()+36||books.size()>90||books.entrySet().stream().anyMatch(e->menu.slot(e.getKey())==null||!menu.item(e.getKey()).equals(e.getValue().item())||menu.slot(e.getKey()).playerInventory()!=(e.getValue().inventorySlot()>=0)||e.getValue().inventorySlot()>=0&&e.getKey()!=(e.getValue().inventorySlot()<9?layout.size()+27+e.getValue().inventorySlot():layout.size()+e.getValue().inventorySlot()-9)))throw new IllegalArgumentException("Invalid anvil observation");
  var playerBooks=books.values().stream().filter(b->b.inventorySlot()>=0).toList();if(playerBooks.stream().map(BookCombinePlanner.Book::inventorySlot).distinct().count()!=playerBooks.size())throw new IllegalArgumentException("Aliased player book slots");
 }
 public BookCombinePlanner.Book book(int slot){return books.get(slot);}
 public boolean emptyInput(int slot){var item=menu.item(slot);return item!=null&&book(slot)==null&&(item.count()==0||!item.identified()&&layout.emptyMaterials().contains(item.minecraftId()));}
 public MenuContract.Ticket inputTicket(BookCombinePlanner.Book expected){
  return books.entrySet().stream().filter(e->menu.slot(e.getKey()).playerInventory()&&e.getValue().equals(expected)).map(e->layout.contract().plan(menu,e.getKey(),MenuContract.Click.QUICK_MOVE)).filter(Objects::nonNull).findFirst().orElse(null);
 }
 /** Free combine must be explicit; a book's gear-application price is never interpreted as combine cost. */
 public MenuContract.Ticket freeCombine(){
  var item=menu.item(layout.combine());if(item==null||menu.slot(layout.combine()).playerInventory()||!item.name().equals("Combine Items")||item.count()!=1||!item.lore().contains("Click to combine!"))return null;
  int costs=0;for(String line:item.lore()){
   if(line.equals("Cost: 0 Exp Levels")||line.equals("Cost: 0 Experience Levels")){costs++;continue;}
   String lower=line.toLowerCase(Locale.ROOT);if(lower.contains("cost")||lower.contains("coins")||lower.contains("levels")||lower.contains("experience")||lower.contains("xp"))return null;
  }
  return costs==1?layout.contract().plan(menu,layout.combine(),MenuContract.Click.PICKUP):null;
 }
}
