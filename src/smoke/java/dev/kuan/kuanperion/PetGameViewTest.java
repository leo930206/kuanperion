// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.pets.PetSalePreflight;import net.minecraft.world.item.*;import net.minecraft.core.component.DataComponents;import net.minecraft.nbt.*;import net.minecraft.world.item.component.CustomData;import java.util.*;
public final class PetGameViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static final String ID="11111111111111111111111111111111";
 static String plain(){return "{\"type\":\"SLUG\",\"tier\":\"EPIC\",\"exp\":0.0,\"active\":false,\"candyUsed\":0,\"petSoulbound\":false,\"noMove\":false,\"hideInfo\":false,\"hideRightClick\":false}";}
 static ItemStack pet(String metadata){var stack=AutomationGameViewTest.fixture(Items.PLAYER_HEAD,1);var a=new CompoundTag();a.putString("id","PET");a.putString("uuid",ID);a.putString("petInfo",metadata);a.putString("timestamp","synthetic-not-a-player-record");var outer=new CompoundTag();outer.put("ExtraAttributes",a);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(outer));return stack;}
 public static void runFixtures(){
  check(PetGameView.pet(null,9,false).isEmpty(),"no phantom pet from absent native item");var stack=pet(plain());var before=stack.getComponents().toString();var p=PetGameView.pet(stack,9,false).orElseThrow();check(p.metadata().kind().equals(new PetSalePreflight.Kind("SLUG","EPIC"))&&p.metadata().experience().signum()==0&&!p.metadata().protectedMetadata()&&p.metadata().uuid().equals(ID),"native26.2 typed ID/UUID and exact petInfo flags, no display-level/name authority");check(before.equals(stack.getComponents().toString()),"projection never strips/modifies native data");
  check(PetGameView.pet(stack,40,true).orElseThrow().item().protectedItem(),"offhand/equipment protection remains visible");
  var soulbound=PetGameView.pet(pet(plain().replace("\"petSoulbound\":false","\"petSoulbound\":true")),9,false).orElseThrow();check(soulbound.metadata().protectedMetadata()&&!p.item().fingerprint().equals(soulbound.item().fingerprint()),"nested petSoulbound not missed by generic item flags; raw baseline changes with metadata");
  var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();data.getCompoundOrEmpty("ExtraAttributes").putString("unknownValuableData","do not sell");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));check(PetGameView.pet(stack,9,false).isEmpty(),"unknown raw attributes cannot hide attachment/cosmetic value");
  var malformed=pet(plain());data=malformed.get(DataComponents.CUSTOM_DATA).copyTag();data.getCompoundOrEmpty("ExtraAttributes").putInt("petInfo",0);malformed.set(DataComponents.CUSTOM_DATA,CustomData.of(data));check(PetGameView.pet(malformed,9,false).isEmpty(),"wrong NBT type cannot become default JSON");
  var falseName=AutomationGameViewTest.fixture(Items.PLAYER_HEAD,1);falseName.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("[Lvl 1] Slug"));check(PetGameView.pet(falseName,9,false).isEmpty(),"copied display name is never a pet identity");
  check(PetGameView.pet(pet(plain().replace("}",",\"uuid\":\"22222222222222222222222222222222\"}")),9,false).isEmpty(),"conflicting embedded/item UUID rejected");
  System.out.println("PetGameViewTest: PASS (native26.2 immutable raw PET/UUID/petInfo, nested soulbound/protection/equipment, unknown/wrong-type/name-only/mismatched-identity denial; no profile/network/sale)");
 }
}
