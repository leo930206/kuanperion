#!/usr/bin/env python3
"""Exercise the actual beacon renderer against Fabric extraction/submit and PoseStack test doubles."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
root=Path(__file__).resolve().parent.parent
base=root/'src/main/java/dev/kuan/skyblockassistant'
client=(base/'KuanSkyBlockAssistantClient.java').read_text()
def method(name):
    match=re.search(r'    private static [^\n]+ '+name+r'\(',client)
    assert match, name
    start=match.start();opening=client.index('{',start);depth=1;end=opening+1
    while depth:
        depth+=(client[end]=='{')-(client[end]=='}');end+=1
    return client[start:end]
assert 'JerryFarmingMarkerRenderer.register(' in client
assert method('tick').index('expireJerryFarmingMarkers();') < method('tick').index('while (settingsKey'), 'Expiry runs regardless of menus, world, or automation state'
probe=method('probeStartingCrop')
assert 'if (farmingManualStartPending)' in probe
assert probe.index('clearJerryFarmingMarkersOnStart(mc);') > probe.index('farmingCropDetected = true;')
assert probe.index('clearJerryFarmingMarkersOnStart(mc);') < probe.rindex('saveConfig();')
assert 'farmingManualStartPending = true;' in method('toggleFarming')
assert 'farmingManualStartPending = false;' in method('stopFarming')
assert 'clearJerryFarmingMarkersOnStart' not in method('toggleMining')
assert client.count('clearJerryFarmingMarkersOnStart(mc);') == 1, 'Only accepted manual Farming start acknowledges markers'
assert 'JERRY_FARMING_MARKERS.load(p);' in method('loadConfig')
assert 'JERRY_FARMING_MARKERS.save(p);' in method('saveConfig')
assert 'JERRY_FARMING_MARKERS' not in method('onFarmingWorldChange'), 'Changing worlds must not delete retained beams'
renderer=(base/'JerryFarmingMarkerRenderer.java').read_text()
renderer=renderer[renderer.index('final class JerryFarmingMarkerRenderer'):].replace('final class JerryFarmingMarkerRenderer','static final class JerryFarmingMarkerRenderer',1)
harness=r'''package dev.kuan.skyblockassistant;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;
public class JerryMarkerRenderingTest {
    static long now=1000;
    static int saves;
    static final JerryFarmingPauseMarkers JERRY_FARMING_MARKERS=new JerryFarmingPauseMarkers(()->now);
    static BigDecimal jerryMarkerMaxSeconds=BigDecimal.ONE;
    static void saveConfig() { saves++; }
    // EXPIRY
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static class RenderStateDataKey<T> { static <T> RenderStateDataKey<T> create() { return new RenderStateDataKey<>(); } }
    interface FabricRenderState {
        <T> void setData(RenderStateDataKey<T> key,T value);
        <T> T getDataOrDefault(RenderStateDataKey<T> key,T fallback);
    }
    static class Vec3 { double x,y,z;Vec3(double x,double y,double z) { this.x=x;this.y=y;this.z=z; } }
    static class Camera { boolean initialized=true;Vec3 pos=new Vec3(5,10,15); }
    static class State implements FabricRenderState {
        Camera cameraRenderState=new Camera();long gameTime=123;Map<Object,Object> data=new HashMap<>();
        public <T> void setData(RenderStateDataKey<T> key,T value) { data.put(key,value); }
        @SuppressWarnings("unchecked") public <T> T getDataOrDefault(RenderStateDataKey<T> key,T fallback) { return (T)data.getOrDefault(key,fallback); }
    }
    static class Pose {
        double x,y,z;Deque<double[]> stack=new ArrayDeque<>();
        void pushPose() { stack.push(new double[]{x,y,z}); }
        void popPose() { double[] p=stack.pop();x=p[0];y=p[1];z=p[2]; }
        void translate(double x,double y,double z) { this.x+=x;this.y+=y;this.z+=z; }
    }
    static class LevelRenderContext {
        State state=new State();Pose pose=new Pose();Object collector=new Object();
        State levelState() { return state; } Pose poseStack() { return pose; } Object submitNodeCollector() { return collector; }
    }
    static class Event {
        Consumer<LevelRenderContext> callback;
        void register(Consumer<LevelRenderContext> callback) { this.callback=callback; }
        void fire(LevelRenderContext ctx) { callback.accept(ctx); }
    }
    static class LevelExtractionEvents { static final Event END_EXTRACTION=new Event(); }
    static class LevelRenderEvents { static final Event COLLECT_SUBMITS=new Event(); }
    record Draw(double x,double y,double z,int color,int height) {}
    static final List<Draw> draws=new ArrayList<>();
    static class BeaconRenderer {
        static final int MAX_RENDER_Y=2048;static final Object BEAM_LOCATION=new Object();
        static void submitBeaconBeam(Pose pose,Object collector,Object texture,float scale,float animation,int y,int height,int color,float core,float glow) {
            check(texture==BEAM_LOCATION && collector!=null && scale==1 && animation==3 && y==0 && core>0 && glow>core,"use native animated full-height beam with visible glow");
            draws.add(new Draw(pose.x,pose.y,pose.z,color,height));
        }
    }
    public static void main(String[] args) {
        JERRY_FARMING_MARKERS.add("garden",0,70,0);
        now=1999;expireJerryFarmingMarkers();check(saves==0 && !JERRY_FARMING_MARKERS.isEmpty(),"before timeout, no disk writes");
        now=2000;expireJerryFarmingMarkers();check(saves==1 && JERRY_FARMING_MARKERS.isEmpty(),"timeout clears and persists without manual Farming resume");
        expireJerryFarmingMarkers();check(saves==1,"empty marker list does not repeatedly write config");
        List<JerryFarmingPauseMarkers.Marker> live=new ArrayList<>();
        live.add(new JerryFarmingPauseMarkers.Marker("garden",100.25,70,-30.5,0));
        JerryFarmingMarkerRenderer.register(()->live);LevelRenderContext context=new LevelRenderContext();
        LevelExtractionEvents.END_EXTRACTION.fire(context);live.clear();
        LevelRenderEvents.COLLECT_SUBMITS.fire(context);
        check(draws.size()==1,"render captured frame even if live markers were acknowledged after extraction");
        check(draws.get(0).equals(new Draw(94.75,60,-46,0xFF90EE90,2048)),"light green beam at exact stop position relative to camera, with beacon half-block correction");
        check(context.pose.stack.isEmpty() && context.pose.x==0 && context.pose.y==0 && context.pose.z==0,"balanced/restored camera pose");
        draws.clear();LevelExtractionEvents.END_EXTRACTION.fire(context);LevelRenderEvents.COLLECT_SUBMITS.fire(context);
        check(draws.isEmpty(),"acknowledgement removes beam on next extracted frame");
        live.add(new JerryFarmingPauseMarkers.Marker("garden",0,70,0,0));live.add(new JerryFarmingPauseMarkers.Marker("garden",30,80,40,0));
        LevelExtractionEvents.END_EXTRACTION.fire(context);LevelRenderEvents.COLLECT_SUBMITS.fire(context);
        check(draws.size()==2 && context.pose.stack.isEmpty(),"multiple retained pause markers render independently");
        draws.clear();context.state.cameraRenderState.initialized=false;LevelRenderEvents.COLLECT_SUBMITS.fire(context);
        check(draws.isEmpty(),"uninitialized/loading camera safe");
        context.state.cameraRenderState=null;LevelRenderEvents.COLLECT_SUBMITS.fire(context);check(draws.isEmpty(),"missing camera safe");
        System.out.println("JerryMarkerRenderingTest: PASS (manual-start wiring, persisted worlds, immutable extraction snapshots, native light-green beam, camera coordinates, balanced poses)");
    }
    // RENDERER
}
'''.replace('    // RENDERER',renderer).replace('    // EXPIRY',method('expireJerryFarmingMarkers'))
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'JerryMarkerRenderingTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(base/'JerryFarmingPauseMarkers.java'),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.skyblockassistant.JerryMarkerRenderingTest'],check=True)
