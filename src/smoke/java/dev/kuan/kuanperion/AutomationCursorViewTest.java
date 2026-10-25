package dev.kuan.kuanperion;
import net.minecraft.world.item.*;import net.minecraft.core.component.DataComponents;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.item.component.CustomData;import net.minecraft.network.protocol.game.*;import java.util.*;
public final class AutomationCursorViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void runFixtures(){
  var stack=AutomationGameViewTest.fixture(Items.PAPER,16);var tag=new CompoundTag();tag.putString("id","TASTY_CHEESE");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  var packet=new ClientboundSetCursorItemPacket(stack.copy());var carried=stack.copy();var snapshot=AutomationCursorView.applied(packet.contents(),carried);check(snapshot!=null&&snapshot.skyblockId().equals("TASTY_CHEESE")&&snapshot.count()==16,"actual26.2 server cursor packet and applied native state agree");
  carried.setCount(15);check(AutomationCursorView.applied(packet.contents(),carried)==null,"a local/predicted/mismatched cursor is not this packet's applied receipt");
  packet.contents().setCount(1);check(snapshot.count()==16,"receipt snapshot immutable after native packet/stack mutation");
  check(AutomationCursorView.applied(ItemStack.EMPTY,ItemStack.EMPTY).count()==0&&AutomationCursorView.applied(null,ItemStack.EMPTY)==null&&AutomationCursorView.applied(ItemStack.EMPTY,stack)==null,"server empty and malformed/mismatched states distinguished");
  var content=new ClientboundContainerSetContentPacket(3,8,List.of(ItemStack.EMPTY),stack.copy());check(AutomationCursorView.applied(content.carriedItem(),stack.copy()).count()==16&&content.containerId()==3,"container-content cursor uses pinned packet accessor; adapter still must match current container ID");
  System.out.println("AutomationCursorViewTest: PASS (pinned26.2 cursor/content packets, raw-vs-applied equality, immutable snapshots, empty/malformed/prediction rejection)");
 }
}
