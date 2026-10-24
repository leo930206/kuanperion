#!/usr/bin/env python3
"""Exercise the production startup tool selector against a fake 36-slot inventory."""
from pathlib import Path
import re,subprocess,sys,tempfile
r=Path(__file__).resolve().parent.parent;src=r/'src/main/java/dev/kuan/kuanperion';code=(src/'KuanperionClient.java').read_text()
def method(name):
 m=re.search(r'    private static [^\n]+ '+name+r'\(',code);assert m,'Missing startup hotbar selector: '+name
 start=m.start();end=code.index('{',start)+1;depth=1
 while depth:depth+=(code[end]=='{')-(code[end]=='}');end+=1
 return code[start:end]
selector=method('selectStartingFarmingTool');probe=method('probeStartingCrop')
assert probe.index('FARMING_TOOL_GUARD.reset(expectedTool)')<probe.index('selectStartingFarmingTool(player)')<probe.index(' 工具錯誤：')
assert 'if (!selectStartingFarmingTool(player))' in probe
assert 'if (farmingToolDetectionEnabled && expectedTool != null)' in probe
assert 'selectStartingFarmingTool' not in method('pauseFarmingForWrongTool'),'runtime manual tool changes must still pause, not fight the user'
constants='\n'.join(re.findall(r'    private static final String (?:SUNFLOWER_TOOL_NAME|WILD_ROSE_TOOL_NAME) = [^;]+;',code))
java=r'''package dev.kuan.kuanperion;
import java.util.*;
public class FarmingStartToolTest {
 static final FarmingToolGuard FARMING_TOOL_GUARD=new FarmingToolGuard();
 static class Component{String value;Component(String s){value=s;}String getString(){return value;}}
 static class Stack{String name;Stack(String s){name=s;}Component getHoverName(){return new Component(name);}}
 static class Inventory{Stack[] items=new Stack[36];int selected=3,changes,reads;Inventory(){Arrays.setAll(items,i->new Stack(""));}Stack getItem(int i){reads++;return items[i];}void setSelectedSlot(int i){if(i<0||i>8)throw new AssertionError("not hotbar");selected=i;changes++;}}
 static class LocalPlayer{Inventory inventory=new Inventory();Inventory getInventory(){return inventory;}Stack getMainHandItem(){return inventory.items[inventory.selected];}}
 static class StringUtil{static String stripColor(String s){return s.replaceAll("§.","");}}
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 CONSTANTS
 METHODS
 public static void main(String[]args){
  for(String block:List.of("wheat","carrots","potatoes","sugar_cane","cactus","cocoa","nether_wart","red_mushroom","brown_mushroom","melon","pumpkin","sunflower","rose_bush")){
   String expected=expectedToolForCropBlock("minecraft:"+block);check(expected!=null,"known crop");
   for(int slot=0;slot<9;slot++){
    FARMING_TOOL_GUARD.reset(expected);var p=new LocalPlayer();p.inventory.items[slot]=new Stack("§aBlessed "+expected+" Mk. III");
    check(selectStartingFarmingTool(p)&&p.inventory.selected==slot,"select correct tool for "+block+" at "+slot);
    check(FARMING_TOOL_GUARD.matches(p.getMainHandItem().getHoverName().getString()),"selection visible to main hand");
   }
   var p=new LocalPlayer();p.inventory.items[3]=new Stack(expected.toUpperCase(Locale.ROOT));p.inventory.items[0]=new Stack(expected);FARMING_TOOL_GUARD.reset(expected);
   check(selectStartingFarmingTool(p)&&p.inventory.selected==3&&p.inventory.changes==0&&p.inventory.reads==0,"keep current correct tool, never switch needlessly");
   p=new LocalPlayer();p.inventory.items[9]=new Stack(expected);p.inventory.items[35]=new Stack(expected);FARMING_TOOL_GUARD.reset(expected);
   check(!selectStartingFarmingTool(p)&&p.inventory.selected==3&&p.inventory.changes==0&&p.inventory.reads==9,"no match: scan only hotbar and preserve selection");
  }
  var p=new LocalPlayer();p.inventory.items[8]=new Stack("Pumpkin Dicer");p.inventory.items[0]=new Stack("Melon Dicer");FARMING_TOOL_GUARD.reset("Pumpkin Dicer");check(selectStartingFarmingTool(p)&&p.inventory.selected==8,"combined Pumpkin/Melon crop still uses corresponding tool");
  p=new LocalPlayer();p.inventory.items[0]=new Stack("Pumpkin Dicer");p.inventory.items[8]=new Stack("Pumpkin Dicer");check(selectStartingFarmingTool(p)&&p.inventory.selected==0,"deterministic first matching slot");
  FARMING_TOOL_GUARD.reset(null);p=new LocalPlayer();check(selectStartingFarmingTool(p)&&p.inventory.changes==0,"no inferred tool requirement");
  System.out.println("FarmingStartToolTest: PASS (all crops/all9 slots, keep correct hand, no inventory scan/mutation, precise startup wiring, runtime manual priority)");
 }
}
'''.replace('CONSTANTS',constants).replace('METHODS',selector+'\n'+method('expectedToolForCropBlock'))
jdk=Path(sys.argv[1]) if len(sys.argv)>1 else Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home'
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'FarmingStartToolTest.java';p.write_text(java)
 subprocess.run([str(jdk/'bin/javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(src/'FarmingToolGuard.java'),str(p)],check=True)
 subprocess.run([str(jdk/'bin/java'),'-cp',tmp,'dev.kuan.kuanperion.FarmingStartToolTest'],check=True)
