package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import net.minecraft.world.item.*;import net.minecraft.core.component.DataComponents;import net.minecraft.network.chat.Component;import net.minecraft.world.item.component.ItemLore;import java.util.*;
public final class AutomationComposterViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void runFixtures(){
  check(ComposterGameView.markerName("Composter")&&ComposterGameView.markerName("COMPOSTER")&&!ComposterGameView.markerName("My Composter")&&!ComposterGameView.markerName("Composter upgrade"),"exact native hologram labels, no contains matching");
  check(ComposterGameView.capture(null,new AutomationContext(1,true,true,Set.of())).isEmpty()&&!ComposterGameView.valid(null,new AutomationContext(1,true,true,Set.of()),new ComposterPolicy.Anchor(-11,72,-27,"minecraft:player_head")),"missing native world never records or validates a guessed machine");
  var organic=AutomationGameViewTest.fixture(Items.WHEAT,1);organic.set(DataComponents.CUSTOM_NAME,Component.literal("Organic Matter"));organic.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("1,234/40k"))));
  var fuel=AutomationGameViewTest.fixture(Items.COAL,1);fuel.set(DataComponents.CUSTOM_NAME,Component.literal("Fuel"));fuel.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("4.25k/100k"))));
  var menu=new MenuSnapshot(1,7,1,"Composter",54,List.of(new MenuSnapshot.Slot(10,AutomationGameView.item(organic,false)),new MenuSnapshot.Slot(40,AutomationGameView.item(fuel,false))));var read=ComposterObservation.inspect(menu).orElseThrow();check(read.organic().current()==1234&&read.fuel().current()==4250,"actual26.2 component/lore projections into semantic capacities, not slot numbers");
  System.out.println("AutomationComposterViewTest: PASS (native name/lore/component projection, exact labels/roles, absent-world capture refusal; no live menu or movement claim)");
 }
}
