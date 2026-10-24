#!/usr/bin/env python3
"""Actual selection methods with fake mouse/world; no real game/settings."""
from pathlib import Path
import re,subprocess,sys,tempfile
root=Path(__file__).resolve().parent.parent
source=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def extract(name):
 a=re.search(r'    private static [^\n]* '+name+r'\(',source).start();b=source.index('{',a)+1;d=1
 while d:
  d+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods='\n'.join(extract(n) for n in ['remainingMiningTargetSlots','clampMiningSelectionCount','beginUiTargetSelection','handleRightClickSelection'])
java=r'''import java.util.*;
public class MiningSelectionTest {
 static final int MAX_MINING_TARGETS=10;
 static final List<String> TARGET_BLOCKS=new ArrayList<>(),messages=new ArrayList<>(),errors=new ArrayList<>();
 static List<String> saved=List.of();static boolean selectMode,miningEnabled,rightWasDown;static int miningSelectionTargetCount=1,miningSelectionAddedCount,clears;
 static class Minecraft {static final Minecraft INSTANCE=new Minecraft();Object player=new Object();Level level=new Level();Gui gui=new Gui();Object hitResult;static Minecraft getInstance(){return INSTANCE;}Window getWindow(){return new Window();}void setScreenAndShow(Object screen){gui.screen=screen;}}
 static class Gui {Object screen;Object screen(){return screen;}}
 static class Window {long handle(){return 1;}}
 static class GLFW {static final int GLFW_MOUSE_BUTTON_RIGHT=1,GLFW_PRESS=1;static boolean down;static int glfwGetMouseButton(long h,int b){return down?1:0;}}
 record BlockHitResult(String getBlockPos){}
 record BlockState(String name){boolean isAir(){return name.equals("air");}}
 static class Level {BlockState getBlockState(String p){return new BlockState(p);}}
 static String blockId(BlockState s){return s.name();}static boolean isAllowedTargetBlock(String n){return !n.equals("air")&&!n.equals("minecraft:bedrock");}
 static void clearActiveTarget(Minecraft mc){clears++;}static void saveConfig(){saved=List.copyOf(TARGET_BLOCKS);}
 static void uiInputError(String s){errors.add(s);}static String feature(String s){return s;}static String plain(String s){return s;}static String warning(String s){return s;}static String error(String s){return s;}static String success(String s){return s;}static String blockName(String s){return s;}
 static void say(Minecraft mc,String...s){messages.add(String.join("",s));}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 METHODS
 static void click(String s){var mc=Minecraft.getInstance();mc.hitResult=new BlockHitResult(s);GLFW.down=true;handleRightClickSelection(mc);GLFW.down=false;handleRightClickSelection(mc);}
 public static void main(String[] args){
  var mc=Minecraft.getInstance();TARGET_BLOCKS.addAll(List.of("A","B","C"));miningSelectionTargetCount=99;miningEnabled=true;mc.gui.screen=new Object();beginUiTargetSelection();
  check(TARGET_BLOCKS.equals(List.of("A","B","C"))&&!miningEnabled&&selectMode&&miningSelectionTargetCount==7&&clears==1&&mc.gui.screen==null,"preserve three / request capped seven / mining stopped");
  click("D");check(TARGET_BLOCKS.equals(List.of("D","A","B","C"))&&miningSelectionAddedCount==1&&selectMode,"prepend first new target");
  check(messages.get(messages.size()-1).contains("1/7"),"session progress excludes old targets");
  click("A");click("D");click("air");click("minecraft:bedrock");check(TARGET_BLOCKS.size()==4&&miningSelectionAddedCount==1,"duplicates and invalid blocks consume no slot");
  mc.gui.screen=new Object();mc.hitResult=new BlockHitResult("hidden");GLFW.down=true;handleRightClickSelection(mc);mc.gui.screen=null;handleRightClickSelection(mc);check(TARGET_BLOCKS.size()==4,"GUI and held click cannot pierce");GLFW.down=false;handleRightClickSelection(mc);
  for(String s:List.of("E","F","G","H","I","J"))click(s);
  check(!selectMode&&miningSelectionAddedCount==7&&TARGET_BLOCKS.equals(List.of("J","I","H","G","F","E","D","A","B","C")),"latest selected first / old order preserved / ten cap");
  check(saved.equals(TARGET_BLOCKS),"entire ordered list saved");
  int before=clears;miningEnabled=true;beginUiTargetSelection();check(TARGET_BLOCKS.size()==10&&!selectMode&&miningEnabled&&clears==before&&!errors.isEmpty(),"full start blocked without stopping mining");
  selectMode=true;click("overflow");check(TARGET_BLOCKS.size()==10&&!selectMode,"stale session cannot exceed ten");
  TARGET_BLOCKS.remove(9);miningSelectionTargetCount=9;beginUiTargetSelection();check(miningSelectionTargetCount==1,"delete frees one slot");click("K");check(TARGET_BLOCKS.size()==10&&TARGET_BLOCKS.get(0).equals("K")&&!selectMode,"new target takes remaining slot at top");
  TARGET_BLOCKS.clear();TARGET_BLOCKS.addAll(List.of("old1","old2"));miningSelectionTargetCount=2;beginUiTargetSelection();click("X");beginUiTargetSelection();click("Y");click("Z");
  check(TARGET_BLOCKS.equals(List.of("Z","Y","X","old1","old2"))&&!selectMode,"restart retains partial additions");
  var old=List.copyOf(TARGET_BLOCKS);mc.player=null;beginUiTargetSelection();check(TARGET_BLOCKS.equals(old)&&!selectMode,"world guard preserves targets");
  System.out.println("MiningSelectionTest: PASS (prepend/preserve, three+seven, duplicates, full/GUI guards, save, partial restart)");
 }
}'''.replace('METHODS',methods)
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'MiningSelectionTest.java';p.write_text(java);jdk=Path(sys.argv[1])/'bin'
 subprocess.run([str(jdk/'javac'),'--release','21','-d',tmp,str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'MiningSelectionTest'],check=True)
