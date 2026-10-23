#!/usr/bin/env python3
"""Exercise the production target-selection method with a fake world."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
root = Path(__file__).resolve().parent.parent
source = (root / 'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def extract(pattern):
    start = re.search(pattern, source).start()
    opening = source.index('{', start)
    depth, end = 1, opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]
methods = extract(r'    private static void mineBestTarget\(') + '\n' + extract(r'    private record Candidate\(')
harness = r'''
import java.util.*;
public class MiningPriorityTest {
    static final List<String> TARGET_BLOCKS = new ArrayList<>(List.of("one", "two", "three"));
    static final Map<BlockPos,Integer> SKIPPED_TARGETS = new HashMap<>();
    static final Map<BlockPos,String> DEPLETED_TARGETS = new HashMap<>();
    static final Map<BlockPos,BlockState> world = new LinkedHashMap<>();
    static final Set<BlockPos> hidden = new HashSet<>();
    static BlockPos activeTarget, mined;
    static String activeTargetBlockId;
    static boolean activeTargetStarted;
    static int cooldown, scanRadius = 5;
    record BlockPos(int x, int y, int z) {
        BlockPos immutable() { return this; }
        BlockPos offset(int a,int b,int c) { return new BlockPos(x+a,y+b,z+c); }
        int getX() { return x; } int getY() { return y; } int getZ() { return z; }
        static Iterable<BlockPos> betweenClosed(BlockPos a, BlockPos b) { return world.keySet(); }
    }
    record BlockState(String id) { boolean isAir() { return id.equals("air"); } }
    static class LocalPlayer {
        BlockPos blockPosition() { return new BlockPos(0,0,0); }
        double distanceToSqr(double x,double y,double z) { return x*x+y*y+z*z; }
    }
    static class Level { BlockState getBlockState(BlockPos pos) { return world.getOrDefault(pos,new BlockState("air")); } }
    static class Minecraft { LocalPlayer player = new LocalPlayer(); Level level = new Level(); Object gameMode = new Object(); }
    static String blockId(BlockState state) { return state.id(); }
    static boolean isAllowedTargetBlock(String id) { return !id.equals("bedrock") && !id.equals("air"); }
    static boolean canSeeBlockCenter(LocalPlayer p,Minecraft mc,BlockPos pos,double reach) { return !hidden.contains(pos); }
    static void clearActiveTarget(Minecraft mc) { activeTarget = null; activeTargetBlockId = null; activeTargetStarted = false; }
    // Controlled completion boundary: same block remains in progress; changed block finishes.
    static boolean mineActiveTarget(Minecraft mc,LocalPlayer p,BlockPos pos) {
        if (!Objects.equals(activeTargetBlockId, blockId(mc.level.getBlockState(pos)))) {
            DEPLETED_TARGETS.put(pos,activeTargetBlockId);
            clearActiveTarget(mc);
            return false;
        }
        mined = pos;
        return true;
    }
    static void reset() { world.clear(); hidden.clear(); SKIPPED_TARGETS.clear(); DEPLETED_TARGETS.clear(); activeTarget=null; activeTargetBlockId=null; mined=null; cooldown=0; }
    static void put(BlockPos p,String id) { world.put(p,new BlockState(id)); }
    static void check(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        Minecraft mc = new Minecraft();
        BlockPos near = new BlockPos(1,0,0), far = new BlockPos(4,0,0), third = new BlockPos(2,0,0);
        reset(); put(near,"two"); put(third,"two"); mineBestTarget(mc);
        check(near.equals(mined),"mine two when one absent");
        put(far,"one"); mineBestTarget(mc);
        check(near.equals(mined),"do not interrupt unfinished two");
        put(near,"bedrock"); mineBestTarget(mc);
        check(far.equals(mined),"after completion choose one before nearer two");
        reset(); put(near,"two"); put(third,"two"); mineBestTarget(mc);
        put(near,"one"); mineBestTarget(mc);
        check(near.equals(activeTarget) && "one".equals(activeTargetBlockId),"higher-priority replacement must not be excluded by depletion history");
        reset(); put(near,"two"); put(far,"one"); DEPLETED_TARGETS.put(far,"three"); mineBestTarget(mc);
        check(far.equals(mined),"previously depleted position can become higher-priority target");
        reset(); put(near,"two"); put(far,"one"); DEPLETED_TARGETS.put(near,"one"); mineBestTarget(mc);
        check(far.equals(mined),"lower-priority depleted representation remains ignored");
        reset(); put(near,"one"); DEPLETED_TARGETS.put(near,"one"); mineBestTarget(mc);
        check(near.equals(mined) && !DEPLETED_TARGETS.containsKey(near),"original respawn remains eligible");
        reset(); put(near,"two"); put(far,"one"); hidden.add(far); mineBestTarget(mc);
        check(near.equals(mined),"occluded priority one remains ineligible");
        reset(); put(near,"two"); put(far,"one"); SKIPPED_TARGETS.put(far,20); mineBestTarget(mc);
        check(near.equals(mined),"temporary unreachable skip remains respected");
        reset(); put(near,"one"); cooldown=2; mineBestTarget(mc);
        check(mined==null,"respect configured between-block delay");
        System.out.println("MiningPriorityTest: PASS (completion rescan, no preemption, depletion priority, respawn, visibility, skip, delay)");
    }
    // METHODS
}
'''.replace('    // METHODS', methods)
java_home = Path(sys.argv[1])
with tempfile.TemporaryDirectory() as tmp:
    path = Path(tmp)/'MiningPriorityTest.java'
    path.write_text(harness)
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-d',tmp,str(path)],check=True)
    subprocess.run([str(java_home/'bin/java'),'-cp',tmp,'MiningPriorityTest'],check=True)
