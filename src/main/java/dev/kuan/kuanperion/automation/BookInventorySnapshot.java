// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Complete immutable native observation, not a book-transfer/combine permission. */
public record BookInventorySnapshot(Map<Integer,ItemSnapshot> inventory,Map<Integer,BookCombinePlanner.Book> books) {
 public BookInventorySnapshot {
  inventory=Map.copyOf(inventory);books=Map.copyOf(books);
  if(inventory.size()!=43||books.size()>36)throw new IllegalArgumentException("Incomplete book inventory");
  var identities=new HashSet<String>();int projected=0;
  for(int slot=0;slot<43;slot++){
   var item=inventory.get(slot);if(item==null||item.count()>64)throw new IllegalArgumentException("Invalid canonical inventory");
   if(!item.uuid().isBlank()&&!identities.add(item.uuid().replace("-","").toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Duplicate inventory identity");
   if(slot<36&&(item.skyblockId().equals("ENCHANTED_BOOK")||item.minecraftId().equals("minecraft:enchanted_book"))){
    var book=books.get(slot);
    if(book==null||book.inventorySlot()!=slot||!book.item().equals(item)||!item.identified()||!item.skyblockId().equals("ENCHANTED_BOOK")||!item.minecraftId().equals("minecraft:enchanted_book")||book.enchantments().isEmpty())throw new IllegalArgumentException("Unknown native book");
    projected++;
   }
  }
  if(projected!=books.size())throw new IllegalArgumentException("Unexpected book projection");
 }
 public static Optional<BookInventorySnapshot> inspect(Map<Integer,ItemSnapshot> inventory,Map<Integer,BookCombinePlanner.Book> books){try{return Optional.of(new BookInventorySnapshot(inventory,books));}catch(RuntimeException unavailable){return Optional.empty();}}
 public boolean stillMatches(Map<Integer,ItemSnapshot> current){return inventory.equals(current);}
}
