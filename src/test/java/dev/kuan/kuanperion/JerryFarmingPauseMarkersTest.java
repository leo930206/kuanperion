package dev.kuan.kuanperion;

import java.math.BigDecimal;
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
        long[] now = {1_000_000L};
        JerryFarmingPauseMarkers timed = new JerryFarmingPauseMarkers(() -> now[0]);
        BigDecimal huge = new BigDecimal("999999999999999999999999999999999999999999999999999999999999999");
        check(JerryFarmingPauseMarkers.parseMaxSeconds(huge.toPlainString()).compareTo(huge) == 0, "no configured maximum, even beyond long seconds");
        check(JerryFarmingPauseMarkers.parseMaxSeconds("0.125").compareTo(new BigDecimal("0.125")) == 0, "fractional seconds supported");
        for (String invalid : new String[]{"", "0", "-1", "NaN", "Infinity", "1..5", "1e9", " 5", "5 "})
            check(JerryFarmingPauseMarkers.parseMaxSeconds(invalid) == null, "invalid duration rejected: " + invalid);
        check(JerryFarmingPauseMarkers.parseMaxSeconds(null) == null, "missing duration uses default");
        timed.add(garden, 0, 70, 0);now[0] += 124;
        check(!timed.expire(new BigDecimal("0.125")), "beam lives until duration reached");
        now[0]++;
        check(timed.expire(new BigDecimal("0.125")) && timed.isEmpty(), "expires exactly at fractional duration without any resume");
        timed.add(garden, 0, 70, 0);now[0] += 1000;timed.add(hub, 0, 70, 0);
        now[0] += 1000;
        check(timed.expire(new BigDecimal("2")) && timed.inScope(garden).isEmpty() && timed.inScope(hub).size() == 1, "each marker has its own creation time, expiry crosses world boundaries");
        Properties timedDisk = new Properties();timed.save(timedDisk);
        JerryFarmingPauseMarkers reloaded = new JerryFarmingPauseMarkers(() -> now[0]);reloaded.load(timedDisk);
        now[0] += 1000;
        check(reloaded.expire(new BigDecimal("2")) && reloaded.isEmpty(), "offline time counts; restart does not reset countdown");
        Properties legacyMarker = new Properties();legacyMarker.setProperty("jerryFarmingPauseMarkerCount", "1");
        legacyMarker.setProperty("jerryFarmingPauseMarker.0.scope", garden);
        for (String axis : new String[]{"x", "y", "z"}) legacyMarker.setProperty("jerryFarmingPauseMarker.0." + axis, "0");
        reloaded.load(legacyMarker);check(reloaded.inScope(garden).get(0).createdAtMillis()==now[0], "legacy marker begins its timeout at migration");
        reloaded.save(legacyMarker);now[0] += 2000;reloaded.load(legacyMarker);
        check(reloaded.expire(new BigDecimal("2")), "migrated timestamp survives restart");
        now[0] = Long.MAX_VALUE - 100;
        timed = new JerryFarmingPauseMarkers(() -> now[0]);timed.add(garden, 0, 0, 0);now[0] += 100;
        check(!timed.expire(huge) && !timed.isEmpty(), "large lifetime never overflows into immediate expiry");
        check(timed.resumeNear(garden, 0, 0, 0) && timed.isEmpty(), "manual Farming start still clears early");
        now[0] = 1000;timed.add(garden, 0, 0, 0);now[0] = 500;
        check(!timed.expire(BigDecimal.ONE), "backward wall clock does not prematurely expire marker");
        now[0] = 2000;check(timed.expire(BigDecimal.ONE), "original creation time remains the timeout origin");
        System.out.println("JerryFarmingPauseMarkersTest: PASS (manual-only acknowledgement, 10-block boundary, world isolation, multiple markers, persistence, unbounded timeout, fractional boundaries, offline expiry, safe malformed input)");
    }
}
