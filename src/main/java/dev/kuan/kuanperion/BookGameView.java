// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.world.Container;import net.minecraft.world.inventory.Slot;import net.minecraft.world.item.ItemStack;
/** Read-only pinned26.2 projections. No GUI action or cost assumption; layout is supplied by the caller. */
public final class BookGameView {
 private BookGameView(){}
 private static boolean available(Minecraft mc){return mc!=null&&mc.isSameThread()&&mc.player!=null&&mc.level!=null;}
 public static Optional<BookInventorySnapshot> inventory(Minecraft mc){
  if(!available(mc))return Optional.empty();
  try{var inv=mc.player.getInventory();if(inv.getContainerSize()!=43||inv.getNonEquipmentItems().size()!=36)return Optional.empty();var items=new ArrayList<ItemStack>();for(int i=0;i<43;i++)items.add(inv.getItem(i));return projectInventory(items);}catch(RuntimeException unavailable){return Optional.empty();}
 }
 static Optional<BookInventorySnapshot> projectInventory(List<ItemStack> stacks){
  try{
   var copy=List.copyOf(stacks);if(copy.size()!=43)return Optional.empty();var items=new HashMap<Integer,ItemSnapshot>();var books=new HashMap<Integer,BookCombinePlanner.Book>();
   for(int slot=0;slot<43;slot++){
    var stack=copy.get(slot);var item=AutomationGameView.item(stack,slot<9||slot>=36);items.put(slot,item);
    if(slot<36&&(item.skyblockId().equals("ENCHANTED_BOOK")||item.minecraftId().equals("minecraft:enchanted_book"))){var book=AutomationGameView.book(stack,slot,slot<9);if(book==null)return Optional.empty();books.put(slot,book);}
   }return BookInventorySnapshot.inspect(items,books);
  }catch(RuntimeException unavailable){return Optional.empty();}
 }
 public static Optional<BookAnvilView> anvil(Minecraft mc,long world,BookAnvilView.Layout layout){
  if(!available(mc)||world<=0||layout==null)return Optional.empty();
  try{return projectAnvil(layout,AutomationGameView.menu(mc,world),mc.player.containerMenu.slots,mc.player.getInventory());}catch(RuntimeException unavailable){return Optional.empty();}
 }
 static Optional<BookAnvilView> projectAnvil(BookAnvilView.Layout layout,MenuSnapshot menu,List<Slot> slots,Container player){
  try{
   if(layout==null||player==null||!layout.contract().matches(menu)||menu.slots().size()!=90||slots.size()!=90)return Optional.empty();var books=new HashMap<Integer,BookCombinePlanner.Book>();
   for(int i=0;i<90;i++){
    var nativeSlot=Objects.requireNonNull(slots.get(i));boolean own=nativeSlot.container==player;int raw=nativeSlot.getContainerSlot();var observed=menu.slot(i);
    if(nativeSlot.index!=i||observed==null||own!=observed.playerInventory()||own!=(i>=54)||raw!=(own?(i<81?i-54+9:i-81):i)||!AutomationGameView.item(nativeSlot.getItem(),own&&raw<9).equals(observed.item()))return Optional.empty();
    var item=observed.item();if(item.skyblockId().equals("ENCHANTED_BOOK")||item.minecraftId().equals("minecraft:enchanted_book")){var book=AutomationGameView.book(nativeSlot.getItem(),own?raw:-1,own&&raw<9);if(book==null)return Optional.empty();books.put(i,book);}
   }return Optional.of(new BookAnvilView(layout,menu,books));
  }catch(RuntimeException unavailable){return Optional.empty();}
 }
}
