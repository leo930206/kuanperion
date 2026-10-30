package dev.kuan.kuanperion;
/** No client, GPU, connection, profile resolution or Minecraft account; real26.2 item classes. */
public final class NativeObservationTest {
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();
  net.minecraft.server.Bootstrap.bootStrap();
  for(int i=0;i<9;i++)if(!net.minecraft.world.inventory.InventoryMenu.isHotbarSlot(36+i)||net.minecraft.world.inventory.InventoryMenu.isHotbarSlot(i))throw new AssertionError("Pinned native hotbar window range disagrees with canonical alias: "+i);
  System.out.println("NativeInventoryHotbarRange: PASS real26.2 window0 raw36..44, not inventory indices0..8");
  AutomationGameViewTest.runFixtures();
  BazaarPurchaseControllerTest.runNativeFixtures();
  System.out.println("NativeObservationTest: PASS (headless vanilla bootstrap; no Mixin/live-server claim)");
 }
}
