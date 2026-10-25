// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.supercraft.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
/** Native class linkage and data-only grid checks. Never starts a game, queries a profile or crafts. */
public final class SupercraftMenuControllerTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),false);
 static ItemSnapshot item(String id,String mc,String name,int count,List<String> lore){return new ItemSnapshot(id,mc,"",name,id+name+lore,count,lore,false);}
 static final Map<String,String> IDS=Map.of("Wheat","WHEAT","Enchanted Wheat","ENCHANTED_WHEAT");
 static SupercraftPlan.Plan plan(){var inv=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)inv.put(i,AIR);for(int i=9;i<14;i++)inv.put(i,item("WHEAT","minecraft:wheat","Wheat",64,List.of()));return SupercraftPlan.select(new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,64,Map.of("WHEAT",160)),2,new SupercraftPlan.Rules(Set.of("ENCHANTED_WHEAT"),Set.of("WHEAT"),2,320,2,Set.of()),inv).orElseThrow();}
 static MenuSnapshot menu(){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<54;i++)slots.add(new MenuSnapshot.Slot(i,AIR));for(int i=10;i<15;i++)slots.set(i,new MenuSnapshot.Slot(i,item("WHEAT","minecraft:wheat","Wheat",32,List.of())));slots.set(24,new MenuSnapshot.Slot(24,item("ENCHANTED_WHEAT","minecraft:wheat","Enchanted Wheat",1,List.of())));slots.set(32,new MenuSnapshot.Slot(32,item("SCANNER_NOT_OUTPUT","minecraft:stone_pickaxe","Supercraft (2x crafts)",1,List.of("✔ 320/320 Wheat","320x from Inventory","Crafting 2 items into your inventory!","Right-Click to set amount!","Click to craft!"))));slots.set(23,new MenuSnapshot.Slot(23,item("","minecraft:crafting_table","Craft Item",1,List.of())));slots.set(45,new MenuSnapshot.Slot(45,item("","minecraft:arrow","Go Back",1,List.of("To Recipe Book"))));slots.set(49,new MenuSnapshot.Slot(49,item("","minecraft:barrier","Close",1,List.of())));var inv=plan().baseline();for(int i=0;i<36;i++)slots.add(new MenuSnapshot.Slot(54+i,inv.get(i<27?i+9:i-27),true));return new MenuSnapshot(1,7,1,"Enchanted Wheat Recipe",54,slots);}
 static SupercraftPreview preview(MenuSnapshot m){return SupercraftPreview.inspect(plan(),m,IDS).orElseThrow();}
 static MenuSnapshot replace(int index,ItemSnapshot item){var m=menu();var slots=new ArrayList<>(m.slots());slots.set(index,new MenuSnapshot.Slot(index,item));return new MenuSnapshot(1,7,2,m.title(),54,slots);}
 public static void main(String[]args)throws Exception{
  var c=new AutomationCoordinator();var s=new AutomationSession(c,()->new AutomationContext(1,false,true,Set.of()),()->{},()->{});var commands=new AtomicInteger();var nativePort=new SupercraftMenuController(c,s,()->new AutomationContext(1,false,true,Set.of()),new InventoryReceipts(),(h,r,n)->{commands.incrementAndGet();return false;});var field=nativePort.getClass().getDeclaredField("catalogClient");field.setAccessible(true);check(field.get(nativePort)==null&&commands.get()==0&&!s.active(),"native construction must not fetch/query/command/acquire any work");
  check(nativePort.catalog().isEmpty()&&nativePort.menu()==null&&nativePort.inventory().isEmpty()&&!nativePort.frame(1).complete()&&!nativePort.context().canIssueCommand(),"absent native client/world remains a blocked read-only projection, not a startup effect or guessed context");
  check(SupercraftMenuController.freeGrid(preview(menu()),IDS,64),"declared neutral roles plus exact per-craft ingredient grid and output stack limit");
  check(!SupercraftMenuController.freeGrid(preview(menu()),IDS,1),"actual native output stack limit cannot be guessed from ID");
  var shortGrid=replace(10,item("WHEAT","minecraft:wheat","Wheat",31,List.of()));check(!SupercraftMenuController.freeGrid(preview(shortGrid),IDS,64),"selected lore alone cannot hide wrong per-craft recipe grid");
  var unknown=replace(22,item("NEW_CONTROL","minecraft:paper","Unrecognized",1,List.of()));check(!SupercraftMenuController.freeGrid(preview(unknown),IDS,64),"unknown non-recipe external roles deny native craft");
  var cost=replace(22,item("","minecraft:paper","Additional Fee",1,List.of("2 Coins")));check(!SupercraftMenuController.freeGrid(preview(cost),IDS,64),"currency/cost outside the craft button is not silently ignored");
  var named=replace(10,item("WHEAT","minecraft:wheat","Custom Wheat",32,List.of()));check(!SupercraftMenuController.freeGrid(preview(named),IDS,64),"raw grid ID and exact official name must agree");
  var unique=replace(10,new ItemSnapshot("WHEAT","minecraft:wheat","unique","Wheat","metadata",32,List.of(),false));check(!SupercraftMenuController.freeGrid(preview(unique),IDS,64),"unique ingredient grid is never ordinary consumable material");
  var buy=replace(45,item("","minecraft:arrow","Buy Ingredients",1,List.of()));check(!SupercraftMenuController.freeGrid(preview(buy),IDS,64),"neutral navigation type does not authorize buy controls");
  var tightPrice=replace(24,item("ENCHANTED_WHEAT","minecraft:wheat","Enchanted Wheat",1,List.of("2Coins")));check(!SupercraftMenuController.freeGrid(preview(tightPrice),IDS,64),"currency glued to a number outside the action button is still a possible fee");
  System.out.println("SupercraftMenuControllerTest: PASS (pinned native class linkage/no startup effect, exact per-craft grid/raw ID/name/limit, unknown role/external fee/unique item denial; no live menu or craft claim)");
 }
}
