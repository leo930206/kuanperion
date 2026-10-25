package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.visitor.*;import net.minecraft.world.item.*;import net.minecraft.core.component.DataComponents;import net.minecraft.network.chat.Component;import net.minecraft.world.item.component.ItemLore;import java.util.*;
public final class AutomationVisitorViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void runFixtures(){
  check(VisitorGameView.inventory(null).isEmpty()&&VisitorGameView.offer(null,1,"Librarian",Map.of()).isEmpty(),"no native player/world means no projected authorization");
  check(!VisitorGameView.queue(null).known()&&VisitorGameView.targets(null,VisitorQueue.parse(List.of("Visitors: (1)","Librarian"))).isEmpty(),"no offline invented queue or guessed target");
  var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
  check(inventory.getContainerSize()==43&&inventory.getNonEquipmentItems().size()==36&&net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND==40&&net.minecraft.world.entity.player.Inventory.SLOT_BODY_ARMOR==41&&net.minecraft.world.entity.player.Inventory.SLOT_SADDLE==42,"pinned26.2 inventory includes offhand, body and saddle; no old41-slot projection");
  var accept=AutomationGameViewTest.fixture(Items.WHEAT,1);accept.set(DataComponents.CUSTOM_NAME,Component.literal("Accept Offer"));accept.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Items Required:"),Component.literal("Enchanted Wheat x1,024"),Component.literal("Rewards:"),Component.literal("+25 Copper"),Component.literal("Copper Dye"),Component.literal("Click to give!"))));
  var data=new net.minecraft.nbt.CompoundTag();data.putString("id","WRONG_SCANNER_ID");accept.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
  var refuse=AutomationGameViewTest.fixture(Items.COAL,1);refuse.set(DataComponents.CUSTOM_NAME,Component.literal("Refuse Offer"));
  var menu=new MenuSnapshot(1,7,2,"Librarian",54,List.of(new MenuSnapshot.Slot(10,AutomationGameView.item(accept,false)),new MenuSnapshot.Slot(22,AutomationGameView.item(refuse,false))));
  var offer=VisitorOffer.inspect(menu,"Librarian",Map.of("Enchanted Wheat","ENCHANTED_WHEAT")).orElseThrow();check(offer.acceptSlot()==10&&offer.refuseSlot()==22&&offer.requirements().get(0).itemId().equals("ENCHANTED_WHEAT")&&offer.requirements().get(0).quantity()==1024&&offer.copper()==25&&offer.rewards().contains("Copper Dye"),"native component/lore exact semantic offer, never scanner-ID or fixed-slot inference");
  System.out.println("AutomationVisitorViewTest: PASS (native26.2 item/lore/ID projection and all43 inventory slots, exact role/material separation; no live offer/submit claim)");
 }
}
