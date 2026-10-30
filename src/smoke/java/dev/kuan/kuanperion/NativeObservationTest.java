package dev.kuan.kuanperion;
/** No client, GPU, connection, profile resolution or Minecraft account; real26.2 item classes. */
public final class NativeObservationTest {
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();
  net.minecraft.server.Bootstrap.bootStrap();
  for(int i=0;i<9;i++)if(!net.minecraft.world.inventory.InventoryMenu.isHotbarSlot(36+i)||net.minecraft.world.inventory.InventoryMenu.isHotbarSlot(i))throw new AssertionError("Pinned native hotbar window range disagrees with canonical alias: "+i);
  System.out.println("NativeInventoryHotbarRange: PASS real26.2 window0 raw36..44, not inventory indices0..8");
  var profile=new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-000000000021"),"spray_fixture");
  var info=new net.minecraft.client.multiplayer.PlayerInfo(profile,false);
  var display=net.minecraft.network.chat.Component.literal("Spray: None").withStyle(net.minecraft.ChatFormatting.GREEN);
  var entry=new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry(profile.id(),profile,true,0,net.minecraft.world.level.GameType.SURVIVAL,display,true,0,null);
  info.setTabListDisplayName(entry.displayName());
  if(info.getProfile()!=profile||info.getTabListDisplayName()!=entry.displayName()||!entry.profileId().equals(profile.id())||!java.util.Objects.equals(info.getTabListDisplayName(),display.copy()))throw new AssertionError("Pinned applied display/UUID contract changed");
  var lookalike=net.minecraft.network.chat.Component.literal("Spray: None").withStyle(net.minecraft.ChatFormatting.RED);
  if(java.util.Objects.equals(lookalike,info.getTabListDisplayName())||!lookalike.getString().equals(info.getTabListDisplayName().getString()))throw new AssertionError("Equal-looking text is not full Component equality");
  System.out.println("NativeSprayTabDisplayContract: PASS real26.2 PlayerInfo/Entry/Component, no client/account/skin lookup or handler/Mixin claim");
  PestSpraySelectionGameViewTest.runFixtures();
  AutomationGameViewTest.runFixtures();
  BazaarPurchaseControllerTest.runNativeFixtures();
  System.out.println("NativeObservationTest: PASS (headless vanilla bootstrap; no Mixin/live-server claim)");
 }
}
