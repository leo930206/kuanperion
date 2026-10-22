package dev.kuan.skyblockassistant;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

public final class JerryFarmingPauseMarkersTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        JerryFarmingPauseMarkers markers = new JerryFarmingPauseMarkers();
        String garden = "server:test|minecraft:overworld|GARDEN";
        String hub = "server:test|minecraft:overworld|HUB";
        String other = "server:other|minecraft:overworld|GARDEN";
        check(markers.isEmpty(), "no old markers on fresh install");
        check(markers.add(garden, 140.25, 70, -20.75), "capture exact standing position");
        check(!markers.add(garden, 140.25, 70, -20.75), "duplicate signal cannot stack identical beams");
        for (int i = 0; i < 1000; i++) check(markers.inScope(garden).size() == 1, "standing/walking/rendering near marker never clears it");
        check(markers.inScope(hub).isEmpty() && markers.inScope(other).isEmpty(), "hide in other islands/servers");
        check(!markers.resumeNear(hub, 140.25, 70, -20.75), "same coordinates in another island cannot clear");
        check(!markers.resumeNear(other, 140.25, 70, -20.75), "another server cannot clear");
        check(!markers.resumeNear(garden, 150.250001, 70, -20.75), "more than ten blocks cannot clear");
        check(!markers.resumeNear(garden, 140.25, 80.000001, -20.75), "farther than ten blocks on another floor cannot clear");
        check(markers.resumeNear(garden, 146.25, 78, -20.75) && markers.isEmpty(), "ten-block boundary is inclusive, using three-dimensional distance");
        markers.add(garden, 0, 70, 0);markers.add(garden, 30, 70, 0);markers.add(hub, 0, 70, 0);
        List<JerryFarmingPauseMarkers.Marker> snapshot = markers.inScope(garden);
        check(markers.resumeNear(garden, 0, 70, 0), "accepted Farming start acknowledges nearby marker");
        check(markers.inScope(garden).size() == 1 && markers.inScope(hub).size() == 1, "preserve distant and other-world markers");
        check(snapshot.size() == 2, "extracted marker snapshots remain immutable after clear");
        boolean immutable = false;
        try { snapshot.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "renderer cannot mutate live markers");
        markers.add("local:path with spaces|world|LOCAL", -10.125, -32.5, 0.75);
        Properties properties = new Properties();markers.save(properties);
        StringWriter text = new StringWriter();properties.store(text, "test");
        Properties disk = new Properties();disk.load(new StringReader(text.toString()));
        JerryFarmingPauseMarkers restored = new JerryFarmingPauseMarkers();restored.load(disk);
        check(restored.inScope(garden).equals(markers.inScope(garden)) && restored.inScope(hub).equals(markers.inScope(hub)), "restart retains markers for each world");
        check(restored.inScope("local:path with spaces|world|LOCAL").size() == 1, "fractional/negative positions and scoped paths survive Properties escaping");
        restored.resumeNear(garden, 30, 70, 0);Properties cleared = new Properties();restored.save(cleared);markers.load(cleared);
        check(markers.inScope(garden).isEmpty() && !markers.inScope(hub).isEmpty(), "acknowledged marker stays removed after restart");
        check(!markers.add(null, 0, 0, 0) && !markers.add(" ", 0, 0, 0), "unknown scope never creates a cross-world marker");
        check(!markers.add(garden, Double.NaN, 0, 0) && !markers.add(garden, 0, Double.POSITIVE_INFINITY, 0), "invalid coordinates rejected");
        check(!markers.resumeNear(garden, Double.NaN, 0, 0), "invalid player position cannot clear markers");
        disk.setProperty("jerryFarmingPauseMarker.0.x", "broken");restored.load(disk);
        check(restored.inScope(hub).size() == 1, "bad entry does not discard subsequent valid entries");
        disk.setProperty("jerryFarmingPauseMarkerCount", "999999999999999");restored.load(disk);
        check(restored.isEmpty(), "malformed marker count safe");
        restored.load(new Properties());check(restored.isEmpty(), "legacy configs have no markers");
        System.out.println("JerryFarmingPauseMarkersTest: PASS (manual-only acknowledgement, 10-block boundary, world isolation, multiple markers, persistence, safe malformed input)");
    }
}
