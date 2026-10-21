#!/usr/bin/env python3
"""Test real Pumpkin/Melon startup and paired lane/return states with fake game boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
root=Path(__file__).resolve().parent.parent
base=root/'src/main/java/dev/kuan/skyblockassistant'
source=(base/'KuanSkyBlockAssistantClient.java').read_text()
def declarations(pattern):
    result=[]
    for match in re.finditer(pattern,source):
        start=match.start();opening=source.index('{',start);depth=1;end=opening+1
        while depth:
            depth+=(source[end]=='{')-(source[end]=='}');end+=1
        result.append(source[start:end])
    assert result, f'Missing lane-start integration: {pattern}'
    return result
methods='\n'.join(block for name in ['beginPumpkinMelonFromLane','tickPumpkinMelon','pumpkinMelonLaneMarkerDetected','beginPumpkinMelonStep','pumpkinMelonStepDone'] for block in declarations(r'    private static [^\n]+ '+name+r'\('))
enums='\n'.join(declarations(r'    private enum (?:PumpkinMelonState|DirectionKey)\s*\{'))
probe=declarations(r'    private static boolean probeStartingCrop\(')[0]
assert 'beginPumpkinMelonFromLane(player, cropBlockId)' in probe, 'Use actual detected Pumpkin/Melon block, not combined crop enum'
assert 'pumpkinMelonStartRight = false;' in declarations(r'    private static void resetFarmingRuntime\(')[0], 'Do not reuse last session direction'
harness=r'''package dev.kuan.skyblockassistant;
public class PumpkinMelonStartTest {
    static final double FARMING_ONE_BLOCK_DISTANCE=1.0;
    static final int FARMING_BLOCKED_STEP_TICKS=10,PUMPKIN_MELON_SECOND_MARKER_MIN_TICKS=10;
    static PumpkinMelonState pumpkinMelonState=PumpkinMelonState.INACTIVE;
    static boolean pumpkinMelonStartRight,pumpkinMelonMarkerCleared;
    static double pumpkinMelonStepStartX,pumpkinMelonStepStartZ,pumpkinMelonStepLastX,pumpkinMelonStepLastZ,pumpkinMelonStepTargetDistance;
    static int pumpkinMelonStepStalledTicks,pumpkinMelonStepTicks;
    static class LocalPlayer { double x,z;LocalPlayer(double x) { this.x=x; } double getX(){return x;} double getZ(){return z;} }
    static class Minecraft { boolean marker,start,end;DirectionKey direction;boolean attacking,jumping; }
    static class FarmingBlockProfile { boolean rowDetection=true,startDetection=true,endDetection=true;String startBlock="start",endBlock="end"; }
    static void applyFarmingInput(Minecraft mc,DirectionKey direction,boolean attack,boolean jump) { mc.direction=direction;mc.attacking=attack;mc.jumping=jump; }
    static String findFarmingMarkerBlock(Minecraft mc,LocalPlayer p) { return mc.marker ? "row" : null; }
    static boolean isConfiguredBlockNear(Minecraft mc,LocalPlayer p,String id) { return id.equals("end") ? mc.end : mc.start; }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static final FarmingBlockProfile profile=new FarmingBlockProfile();
    static void tick(Minecraft mc,LocalPlayer p) { tickPumpkinMelon(mc,p,profile); }
    static void expect(Minecraft mc,PumpkinMelonState state,DirectionKey direction,boolean attack,String why) {
        check(pumpkinMelonState==state && mc.direction==direction && mc.attacking==attack && !mc.jumping,why+": "+pumpkinMelonState+"/"+mc.direction);
    }
    static void secondMarker(Minecraft mc,LocalPlayer p,PumpkinMelonState state) {
        mc.marker=false;
        for(int i=0;i<9;i++) tick(mc,p);
        check(pumpkinMelonState==state,"do not turn before second marker");
        mc.marker=true;tick(mc,p);mc.marker=false;
    }
    static LocalPlayer start(Minecraft mc,String block,double x,boolean right) {
        LocalPlayer p=new LocalPlayer(x);pumpkinMelonStartRight=!right;pumpkinMelonMarkerCleared=true;
        beginPumpkinMelonFromLane(p,block);
        check(pumpkinMelonStartRight==right && !pumpkinMelonMarkerCleared,"capture lane direction and reset marker latch");
        tick(mc,p);expect(mc,PumpkinMelonState.START_FORWARD_ONE,DirectionKey.FORWARD,true,"preserve initial one-block preparation");
        p.x-=1;tick(mc,p);
        expect(mc,right?PumpkinMelonState.HARVEST_RIGHT:PumpkinMelonState.HARVEST_LEFT,right?DirectionKey.RIGHT:DirectionKey.LEFT,true,"join correct full harvest phase after preparation");
        return p;
    }
    public static void main(String[] args) {
        String[] crops={"minecraft:pumpkin","minecraft:carved_pumpkin","minecraft:melon"};
        for(String crop:crops) {
            boolean melon=crop.equals("minecraft:melon");
            int[] left=melon?new int[]{125,119,113}:new int[]{143,137,131};
            int[] right=melon?new int[]{122,116,110}:new int[]{140,134,128};
            for(int column:left) for(double part:new double[]{0,0.001,0.5,0.999999}) {
                check(!PumpkinMelonLaneStart.startsRight(crop,column+part),"left column including decimals");start(new Minecraft(),crop,column+part,false);
            }
            for(int column:right) for(double part:new double[]{0,0.001,0.5,0.999999}) {
                check(PumpkinMelonLaneStart.startsRight(crop,column+part),"right column including decimals");start(new Minecraft(),crop,column+part,true);
                check(!PumpkinMelonLaneStart.startsRight(crop,Math.nextDown((double)column)),"do not round neighboring column upward");
                check(!PumpkinMelonLaneStart.startsRight(crop,column+1.0),"upper boundary excluded");
            }
        }
        check(!PumpkinMelonLaneStart.startsRight("minecraft:pumpkin",122.5),"melon coordinates cannot reverse Pumpkin");
        check(!PumpkinMelonLaneStart.startsRight("minecraft:melon",140.5),"pumpkin coordinates cannot reverse Melon");
        for(String block:new String[]{"minecraft:wheat","minecraft:red_mushroom","minecraft:stone",""})
            check(!PumpkinMelonLaneStart.startsRight(block,140.5),"other crops unaffected");
        check(!PumpkinMelonLaneStart.startsRight(null,140.5),"null block safe");
        for(double x:new double[]{-140.5,0,139.99,141,200,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            check(!PumpkinMelonLaneStart.startsRight("minecraft:pumpkin",x),"unlisted/invalid position retains original direction");
        // Start in the middle of the rightward half: use its own turn, then enter leftward half.
        Minecraft mc=new Minecraft();LocalPlayer p=start(mc,"minecraft:pumpkin",140.5,true);
        tick(mc,p);check(pumpkinMelonMarkerCleared,"clear initial marker latch");
        mc.marker=true;tick(mc,p);expect(mc,PumpkinMelonState.RIGHT_EXTRA_ONE,DirectionKey.RIGHT,true,"right marker enters rightward turn logic");
        mc.marker=false;p.z-=1;tick(mc,p);expect(mc,PumpkinMelonState.RIGHT_FORWARD_TO_SECOND_MARKER,DirectionKey.FORWARD,true,"right extra step");
        secondMarker(mc,p,PumpkinMelonState.RIGHT_FORWARD_TO_SECOND_MARKER);
        expect(mc,PumpkinMelonState.LEFT_ONE,DirectionKey.LEFT,true,"right turn uses its counter-step");
        p.z+=1;tick(mc,p);expect(mc,PumpkinMelonState.RIGHT_FORWARD_ONE,DirectionKey.FORWARD,true,"right turn final preparation");
        p.x-=1;tick(mc,p);expect(mc,PumpkinMelonState.HARVEST_LEFT,DirectionKey.LEFT,true,"right turn resumes leftward harvest, not permanent input inversion");
        tick(mc,p);mc.marker=true;tick(mc,p);expect(mc,PumpkinMelonState.LEFT_EXTRA_ONE,DirectionKey.LEFT,true,"left half retains original turn");
        mc.marker=false;p.z+=1;tick(mc,p);expect(mc,PumpkinMelonState.LEFT_FORWARD_TO_SECOND_MARKER,DirectionKey.FORWARD,true,"left extra step");
        secondMarker(mc,p,PumpkinMelonState.LEFT_FORWARD_TO_SECOND_MARKER);
        expect(mc,PumpkinMelonState.RIGHT_ONE,DirectionKey.RIGHT,true,"left turn counter-step");
        p.z-=1;tick(mc,p);expect(mc,PumpkinMelonState.LEFT_FORWARD_ONE,DirectionKey.FORWARD,true,"left turn final preparation");
        p.x-=1;tick(mc,p);expect(mc,PumpkinMelonState.HARVEST_RIGHT,DirectionKey.RIGHT,true,"full loop alternates back to right");
        // Terminal markers must keep their existing return path even on a right-start lane.
        mc=new Minecraft();p=start(mc,"minecraft:melon",122.5,true);mc.end=true;tick(mc,p);
        expect(mc,PumpkinMelonState.END_RIGHT_ONE,DirectionKey.RIGHT,false,"right start honors end marker and stops attack");
        mc.end=false;p.z-=1;tick(mc,p);expect(mc,PumpkinMelonState.RETURN_BACK_TO_START,DirectionKey.BACK,false,"end leads to return path");
        mc.start=true;tick(mc,p);expect(mc,PumpkinMelonState.RETURN_BACK_TWO,DirectionKey.BACK,true,"start marker enters existing two-block return");
        mc.start=false;p.x+=1;tick(mc,p);check(pumpkinMelonState==PumpkinMelonState.RETURN_BACK_TWO,"keep full two-block return distance");
        p.x+=1;tick(mc,p);expect(mc,PumpkinMelonState.RETURN_LEFT_ONE,DirectionKey.LEFT,true,"return left step unchanged");
        p.z+=1;tick(mc,p);expect(mc,PumpkinMelonState.RETURN_FORWARD_ONE,DirectionKey.FORWARD,true,"return forward step unchanged");
        p.x-=1;tick(mc,p);expect(mc,PumpkinMelonState.HARVEST_LEFT,DirectionKey.LEFT,true,"full return starts original first lane, not startup right override");
        mc=new Minecraft();p=new LocalPlayer(134.5);beginPumpkinMelonFromLane(p,"minecraft:pumpkin");
        for(int i=0;i<10;i++) tick(mc,p);
        expect(mc,PumpkinMelonState.HARVEST_RIGHT,DirectionKey.RIGHT,true,"blocked initial step still respects selected direction");
        // Merely moving across a configured X column during harvest must never re-select direction.
        p.x=143.5;tick(mc,p);expect(mc,PumpkinMelonState.HARVEST_RIGHT,DirectionKey.RIGHT,true,"startup choice is latched, not recalculated every tick");
        System.out.println("PumpkinMelonStartTest: PASS (all configured columns/decimals, crop isolation, one-block bootstrap, both full turns, terminal return, latched direction, legacy fallback)");
    }
    // METHODS
    // ENUMS
}
'''.replace('    // METHODS',methods).replace('    // ENUMS',enums)
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'PumpkinMelonStartTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(base/'PumpkinMelonLaneStart.java'),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.skyblockassistant.PumpkinMelonStartTest'],check=True)
