package dev.kuan.kuanperion;
import java.util.List;
import dev.kuan.kuanperion.automation.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
public final class AutomationGameViewTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemStack fixture(Item item,int count){
  var components=DataComponentMap.builder().set(DataComponents.ITEM_NAME,Component.literal("Fixture")).set(DataComponents.MAX_STACK_SIZE,64).build();
  return new ItemStack(Holder.direct(item,components),count,DataComponentPatch.EMPTY);
 }
 static net.minecraft.world.item.component.ResolvableProfile patchedProfile(com.mojang.authlib.GameProfile profile,net.minecraft.world.entity.player.PlayerSkin.Patch patch){
  try{var ctor=Class.forName("net.minecraft.world.item.component.ResolvableProfile$Static").getDeclaredConstructor(com.mojang.datafixers.util.Either.class,net.minecraft.world.entity.player.PlayerSkin.Patch.class);ctor.setAccessible(true);return (net.minecraft.world.item.component.ResolvableProfile)ctor.newInstance(com.mojang.datafixers.util.Either.left(profile),patch);}catch(ReflectiveOperationException invalid){throw new AssertionError("Pinned26.2 native profile fixture unavailable",invalid);}
 }
 static void testBooks(){
  var stack=fixture(Items.ENCHANTED_BOOK,1);stack.set(DataComponents.ITEM_NAME,Component.literal("Enchanted Book"));
  var attributes=new CompoundTag();attributes.putString("id","ENCHANTED_BOOK");attributes.putString("uuid","one");attributes.putString("origin","same");
  var enchants=new CompoundTag();enchants.putInt("harvesting",4);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));
  var unchanged=stack.get(DataComponents.CUSTOM_DATA).copyTag();var a=AutomationGameView.book(stack,9,false);check(a!=null&&a.enchantments().equals(java.util.Map.of("harvesting",4)),"exact native book enchantment map");check(stack.get(DataComponents.CUSTOM_DATA).copyTag().equals(unchanged),"book projection never mutates original NBT");
  attributes.putString("uuid","two");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));var b=AutomationGameView.book(stack,10,false);
  var planner=new BookCombinePlanner(java.util.Map.of("harvesting",5),java.util.Set.of());var plan=planner.select(java.util.List.of(a,b)).orElseThrow();
  check(a.compatibility().equals(b.compatibility()),"UUID difference is not discarded protection but can pair otherwise equal items");
  enchants.putInt("harvesting",5);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(planner.isExpectedResult(plan,AutomationGameView.book(stack,-1,false)),"native preview result preserves remaining data and advances exact level");
  attributes.putString("origin","changed");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(!planner.isExpectedResult(plan,AutomationGameView.book(stack,-1,false)),"native preview with metadata loss/change rejected before spending");
  enchants.putFloat("harvesting",4.5f);attributes.put("enchantments",enchants);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(attributes));check(AutomationGameView.book(stack,10,false)==null,"fractional NBT levels cannot truncate into an authorized book");
  check(AutomationGameView.book(fixture(Items.ENCHANTED_BOOK,1),10,false)==null,"display/vanilla book without authoritative NBT not inferred");
  System.out.println("BookGameViewTest: PASS (native integer NBT, metadata/UUID projection, immutable original, exact preview and no name/lore fallback)");
 }
 static void testPest(){
  var vacuum=fixture(Items.PAPER,1);var data=new CompoundTag();data.putString("id","INFINI_VACUUM");data.putString("uuid","my-vacuum");vacuum.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
  vacuum.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Ability: Vacuum RIGHT CLICK"),Component.literal("Suck pests within 12.5 blocks"))));
  var before=vacuum.getComponents().toString();check(PestGameView.vacuum(vacuum,0).orElseThrow().range()==12.5,"native exact ID/lore Pest vacuum");check(vacuum.getComponents().toString().equals(before),"Pest observation does not alter tool");
  var selected=PestGameView.vacuum(vacuum,0).orElseThrow();vacuum.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Ability: Vacuum RIGHT CLICK"),Component.literal("Suck pests within 12.5 blocks"),Component.literal("Vacuum Bag: 1"))));
  check(dev.kuan.kuanperion.automation.pest.PestIdentity.sameVacuum(selected,AutomationGameView.item(vacuum,true)),"real UUID-bound vacuum survives native bag lore update");
  data.putString("uuid","other-vacuum");vacuum.set(DataComponents.CUSTOM_DATA,CustomData.of(data));check(!dev.kuan.kuanperion.automation.pest.PestIdentity.sameVacuum(selected,AutomationGameView.item(vacuum,true)),"same-ID different native UUID not substituted");
  data.putString("id","TURBOMAX_VACUUM");vacuum.set(DataComponents.CUSTOM_DATA,CustomData.of(data));check(PestGameView.vacuum(vacuum,0).isEmpty(),"native non-VACUUM-category item excluded");
  var head=fixture(Items.PLAYER_HEAD,1);String hash="70a1e836bf1968b2eaa4837227a19204f17295d870ee9e754bd6b6d60ddbed3c";
  String encoded=java.util.Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/"+hash+"\"}}}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var props=com.google.common.collect.ArrayListMultimap.<String,com.mojang.authlib.properties.Property>create();props.put("textures",new com.mojang.authlib.properties.Property("textures",encoded));
  var profile=new com.mojang.authlib.GameProfile(new java.util.UUID(0,1),"fixture",new com.mojang.authlib.properties.PropertyMap(props));head.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(profile));
  check(PestGameView.knownHead(head),"native profile property identifies exact curated Pest skin without resolving/fetching");
  head.set(DataComponents.CUSTOM_NAME,Component.literal("Decoration"));check(!PestGameView.knownHead(head),"custom-named skull not treated as Pest marker");head.remove(DataComponents.CUSTOM_NAME);
  var patch=new net.minecraft.world.entity.player.PlayerSkin.Patch(java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.of(net.minecraft.world.entity.player.PlayerModelType.SLIM));head.set(DataComponents.PROFILE,patchedProfile(profile,patch));check(!PestGameView.knownHead(head),"native skin patch overrides are not inferred from the partial profile texture");head.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(profile));
  props.put("textures",new com.mojang.authlib.properties.Property("textures",encoded));profile=new com.mojang.authlib.GameProfile(new java.util.UUID(0,1),"fixture",new com.mojang.authlib.properties.PropertyMap(props));head.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(profile));check(!PestGameView.knownHead(head),"multiple skin properties are ambiguous");
  var context=new AutomationContext(1,true,true,java.util.Set.of());check(!PestGameView.read(null,context,0).complete(),"no world is not a valid empty/cleared Pest scan");
  var snapshot=new PestGameView.Snapshot(1,100,dev.kuan.kuanperion.automation.pest.PestTabObservation.parse(List.of("Pests: 0")),List.of(),List.of(),0,true);check(snapshot.fresh(1,100)&&snapshot.fresh(1,2000000100L)&&!snapshot.fresh(2,100)&&!snapshot.fresh(1,99)&&!snapshot.fresh(1,2000000101L),"Pest frame has bounded read-time freshness and world identity, not a kill receipt");
  var trap=PestGameView.trapMarker(77,new java.util.UUID(0,77),"§aMouse Trap #2",new net.minecraft.world.phys.Vec3(1.25,72,3.5));check(trap.isPresent()&&trap.get().trapId()==2&&trap.get().family().equals("Mouse Trap")&&trap.get().position().x()==1.25,"native exact color-stripped trap marker projection");
  check(PestGameView.trapMarker(77,new java.util.UUID(0,77),"Quest Mouse Trap #2",new net.minecraft.world.phys.Vec3(1,72,3)).isEmpty()&&PestGameView.trapMarker(77,new java.util.UUID(0,77),"Mouse Trap #2",new net.minecraft.world.phys.Vec3(Double.NaN,72,3)).isEmpty(),"native trap projection refuses malformed/mentioned candidates");
  System.out.println("PestGameViewTest: PASS (native hotbar ID/lore, immutable tool, exact local profile/no resolve, custom/ambiguous heads and no-world guard)");
 }
 public static void main(String[] args){
  try{var type=Class.forName("net.minecraft.client.multiplayer.ClientPacketListener");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$slot")),"server receipt Mixin actually applied");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$position")),"server position receipt Mixin actually applied");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$cursor")),"server cursor receipt Mixin actually applied");for(String name:java.util.List.of("harvestRemove","harvestEquipment","harvestBlock","harvestBlockEntity","harvestSection"))check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().endsWith("kuanperion$"+name)),"harvest receipt Mixin applied: "+name);}catch(ClassNotFoundException e){throw new AssertionError(e);}
  try{var type=Class.forName("net.minecraft.client.multiplayer.MultiPlayerGameMode");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanperion$manualInventoryInput")),"manual inventory priority Mixin applied");}catch(ClassNotFoundException e){throw new AssertionError(e);}
  try{var type=Class.forName("net.minecraft.world.entity.Entity");check(java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(m->m.getName().contains("kuanHsba$lockManualLook")),"manual camera takeover Mixin actually applied");}catch(ClassNotFoundException e){throw new AssertionError(e);}
  System.out.println("AutomationMixinsTest: PASS (receipt, inventory priority and camera takeover applied)");
  runFixtures();
 }
 static void runFixtures(){
  var stack=fixture(Items.WHEAT,12);var extra=new CompoundTag();extra.putString("id","WHEAT");extra.putString("uuid","test-uuid");var tag=new CompoundTag();tag.put("ExtraAttributes",extra);
  stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("§aExample"))));
  var item=AutomationGameView.item(stack,false);check(item.identified()&&item.skyblockId().equals("WHEAT")&&item.uuid().equals("test-uuid")&&item.count()==12&&item.lore().equals(List.of("Example")),"actual26.2 item/custom data/lore read correctly");
  check(!item.protectedItem()&&AutomationGameView.item(stack,true).protectedItem(),"equipment/hotbar protection");
  extra.putString("other_metadata","changed");tag.put("ExtraAttributes",extra);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  check(!item.fingerprint().equals(AutomationGameView.item(stack,false).fingerprint()),"metadata changes fingerprint");
  extra.putBoolean("favorite",true);tag.put("ExtraAttributes",extra);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));check(AutomationGameView.item(stack,false).protectedItem(),"server custom favorite protected");
  var tool=fixture(Items.DIAMOND_PICKAXE,1);tool.set(DataComponents.MAX_DAMAGE,1561);check(AutomationGameView.item(tool,false).protectedItem(),"damageable tool components protected without relying on name");
  var bound=fixture(Items.PAPER,1);bound.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("§8[Co-op Soulbound]"))));check(AutomationGameView.item(bound,false).protectedItem(),"wrapped soulbound lore protected conservatively");
  check(!AutomationGameView.item(fixture(Items.WHEAT,1),false).identified(),"vanilla-name spoof without server ID cannot authorize disposal");
  var uuidArray=fixture(Items.PAPER,1);var arrayData=new CompoundTag();arrayData.putString("id","EXAMPLE");arrayData.putIntArray("uuid",new int[]{1,2,3,4});uuidArray.set(DataComponents.CUSTOM_DATA,CustomData.of(arrayData));
  check(AutomationGameView.item(uuidArray,false).uuid().equals("00000001-0000-0002-0000-000300000004"),"NBT int-array UUID preserved for protected-item matching");
  arrayData.putIntArray("uuid",new int[]{1,2});uuidArray.set(DataComponents.CUSTOM_DATA,CustomData.of(arrayData));check(AutomationGameView.item(uuidArray,false).protectedItem(),"unknown UUID representation fails closed");
  check(!AutomationGameView.item(ItemStack.EMPTY,false).identified(),"empty slot not identified");
  testBooks();
  testPest();
  AutomationPositionViewTest.runFixtures();
  AutomationCursorViewTest.runFixtures();
  AutomationGreenhouseViewTest.runFixtures();
  var anchor=new dev.kuan.kuanperion.automation.pest.PestTask.Anchor(new dev.kuan.kuanperion.automation.pest.PestTargets.Point(.5,70.25,-.5),12,-5,3,new dev.kuan.kuanperion.automation.ItemSnapshot("TOOL","minecraft:diamond_hoe","uuid","","meta",1,List.of(),true),true);
  var converted=PestFlightController.flightAnchor(anchor);check(converted.position().equals(anchor.position())&&converted.item()==anchor.item()&&converted.slot()==anchor.slot()&&converted.yaw()==anchor.yaw()&&converted.pitch()==anchor.pitch()&&converted.flying()==anchor.flying(),"real Pest facade preserves every return anchor field");
  for(var motion:dev.kuan.kuanperion.automation.flight.FlightMotion.values())check(PestFlightController.pestMotion(motion).name().equals(motion.name()),"real Pest facade maps every motion exactly");
  var coordinator=new dev.kuan.kuanperion.automation.AutomationCoordinator();var session=new dev.kuan.kuanperion.automation.AutomationSession(coordinator,()->new dev.kuan.kuanperion.automation.AutomationContext(1,true,true,java.util.Set.of()),()->{},()->{});
  for(var feature:java.util.List.of(dev.kuan.kuanperion.automation.AutomationFeature.PEST,dev.kuan.kuanperion.automation.AutomationFeature.GREENHOUSE)){var core=new GardenFlightCore(coordinator,session,()->new dev.kuan.kuanperion.automation.AutomationContext(1,true,true,java.util.Set.of()),feature);check(core.owner()==null&&!session.active(),"constructing a flight primitive performs no action or task acquisition");}
  try{new GardenFlightCore(coordinator,session,()->null,dev.kuan.kuanperion.automation.AutomationFeature.AUTO_SELL);throw new AssertionError("unauthorized flight owner");}catch(IllegalArgumentException expected){}
  System.out.println("FlightFacadeTest: PASS (actual native facade motion/anchor mapping, no-op construction, only two fixed feature owners)");
  System.out.println("AutomationGameViewTest: PASS (26.2 component fixtures/NBT/lore/protection/metadata identity)");
 }
}
