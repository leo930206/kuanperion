package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayToolTest {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static ItemSnapshot tool(String id,String...lore){return new ItemSnapshot(id,"minecraft:blaze_rod","spray-uuid","Named tool","tool-fingerprint",1,List.of(lore),true);}
 public static void main(String[]args){
  for(String id:List.of("SPRAYONATOR","JUICY_SPRAYONATOR","SALTY_SPRAYONATOR")){
   var item=tool(id,"Selected Material: Test Material");var t=PestSprayTool.read(4,item).orElseThrow();check(t.slot()==4&&t.item().equals(item)&&t.selectedMaterial().equals("Test Material")&&t.matchesMaterial("test material"),"source exact ID/lore and immutable actual snapshot "+id);
  }
  check(PestSprayTool.read(4,tool("SPRAYONATOR","§aSelected   material :  Test   Material ")).orElseThrow().matchesMaterial("Test Material"),"source formatting/whitespace normalization, not native product inference");
  check(PestSprayTool.read(4,tool("SPRAYONATOR","Selected Material: None")).orElseThrow().selectedMaterial().isEmpty(),"source explicit None is not a configured material");
  for(String id:List.of("","SUPER_SPRAYONATOR","INFINI_VACUUM","TEST_SPRAYONATOR"))check(PestSprayTool.read(4,tool(id,"Selected Material: Test Material")).isEmpty(),"no name-only or ID-substring tool match "+id);
  for(var lore:List.of(List.of("No known selection"),List.of("Selected Material:"),List.of("Selected Material: Unknown"),List.of("Selected Material: Use Selected"),List.of("Selected Material: Test Material","Selected Material: Test Material"),List.of("Selected Material: Test Material\nCost: 1 Coins"),List.of("Selected Material: "+"x".repeat(65))))check(PestSprayTool.read(4,tool("SPRAYONATOR",lore.toArray(String[]::new))).isEmpty(),"missing/ambiguous/control/oversize material is unknown: "+lore);
  check(PestSprayTool.read(-1,tool("SPRAYONATOR","Selected Material: Test Material")).isEmpty()&&PestSprayTool.read(9,tool("SPRAYONATOR","Selected Material: Test Material")).isEmpty(),"hotbar-only existing tool");
  var x=tool("SPRAYONATOR","Selected Material: Test Material");check(PestSprayTool.read(4,new ItemSnapshot(x.skyblockId(),x.minecraftId(),x.uuid(),x.name(),x.fingerprint(),2,x.lore(),true)).isEmpty(),"stack count must be one");
  System.out.println("PestSprayToolTest: PASS source-marked pure tool/lore facts only; no native/runtime exposure or live material mapping");
 }
}
