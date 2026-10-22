package dev.kuan.skyblockassistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Persistent client-only positions; only an accepted manual Farming start can acknowledge them. */
final class JerryFarmingPauseMarkers {
    record Marker(String scope, double x, double y, double z) {}
    private static final String KEY = "jerryFarmingPauseMarker";
    private static final int MAX_MARKERS = 4096;
    private final List<Marker> markers = new ArrayList<>();

    boolean isEmpty() { return markers.isEmpty(); }

    boolean add(String scope, double x, double y, double z) {
        if (!valid(scope, x, y, z) || markers.size() >= MAX_MARKERS) return false;
        Marker marker = new Marker(scope, x, y, z);
        if (markers.contains(marker)) return false;
        return markers.add(marker);
    }

    List<Marker> inScope(String scope) {
        if (scope == null) return List.of();
        return markers.stream().filter(marker -> marker.scope().equals(scope)).toList();
    }

    boolean resumeNear(String scope, double x, double y, double z) {
        if (!valid(scope, x, y, z)) return false;
        return markers.removeIf(marker -> {
            if (!marker.scope().equals(scope)) return false;
            double dx = x - marker.x(), dy = y - marker.y(), dz = z - marker.z();
            return dx * dx + dy * dy + dz * dz <= 100.0;
        });
    }

    void load(Properties properties) {
        markers.clear();
        int count;
        try { count = Integer.parseInt(properties.getProperty(KEY + "Count", "0")); }
        catch (NumberFormatException ignored) { return; }
        for (int i = 0; i < Math.min(count, MAX_MARKERS); i++) {
            String prefix = KEY + "." + i + ".";
            try {
                add(properties.getProperty(prefix + "scope"),
                        Double.parseDouble(properties.getProperty(prefix + "x", "NaN")),
                        Double.parseDouble(properties.getProperty(prefix + "y", "NaN")),
                        Double.parseDouble(properties.getProperty(prefix + "z", "NaN")));
            } catch (NumberFormatException ignored) { /* Ignore a malformed entry, not the remaining markers. */ }
        }
    }

    void save(Properties properties) {
        properties.setProperty(KEY + "Count", Integer.toString(markers.size()));
        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            String prefix = KEY + "." + i + ".";
            properties.setProperty(prefix + "scope", marker.scope());
            properties.setProperty(prefix + "x", Double.toString(marker.x()));
            properties.setProperty(prefix + "y", Double.toString(marker.y()));
            properties.setProperty(prefix + "z", Double.toString(marker.z()));
        }
    }

    private static boolean valid(String scope, double x, double y, double z) {
        return scope != null && !scope.isBlank() && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
