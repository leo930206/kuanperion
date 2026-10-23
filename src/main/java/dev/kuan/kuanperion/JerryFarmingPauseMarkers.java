package dev.kuan.kuanperion;

import java.math.BigDecimal;
import java.util.function.LongSupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Persistent client-only positions; manual Farming resume or a wall-clock timeout clears them. */
final class JerryFarmingPauseMarkers {
    record Marker(String scope, double x, double y, double z, long createdAtMillis) {}
    static final BigDecimal DEFAULT_MAX_SECONDS = BigDecimal.valueOf(300);
    private static final String KEY = "jerryFarmingPauseMarker";
    private static final int MAX_MARKERS = 4096;
    private final List<Marker> markers = new ArrayList<>();

    private final LongSupplier clock;

    JerryFarmingPauseMarkers() { this(System::currentTimeMillis); }
    JerryFarmingPauseMarkers(LongSupplier clock) { this.clock = clock; }

    static BigDecimal parseMaxSeconds(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?")) return null;
        try {
            BigDecimal seconds = new BigDecimal(value);
            return seconds.signum() > 0 ? seconds.stripTrailingZeros() : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    boolean expire(BigDecimal maxSeconds) {
        if (maxSeconds == null || maxSeconds.signum() <= 0) return false;
        BigDecimal now = BigDecimal.valueOf(clock.getAsLong());
        BigDecimal lifetimeMillis = maxSeconds.movePointRight(3);
        return markers.removeIf(marker -> now.subtract(BigDecimal.valueOf(marker.createdAtMillis()))
                .compareTo(lifetimeMillis) >= 0);
    }

    boolean isEmpty() { return markers.isEmpty(); }

    boolean add(String scope, double x, double y, double z) {
        return add(scope, x, y, z, clock.getAsLong());
    }

    private boolean add(String scope, double x, double y, double z, long createdAtMillis) {
        if (!valid(scope, x, y, z) || markers.size() >= MAX_MARKERS) return false;
        if (markers.stream().anyMatch(marker -> marker.scope().equals(scope)
                && marker.x() == x && marker.y() == y && marker.z() == z)) return false;
        return markers.add(new Marker(scope, x, y, z, createdAtMillis));
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
                        Double.parseDouble(properties.getProperty(prefix + "z", "NaN")),
                        Long.parseLong(properties.getProperty(prefix + "createdAtMillis", Long.toString(clock.getAsLong()))));
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
            properties.setProperty(prefix + "createdAtMillis", Long.toString(marker.createdAtMillis()));
        }
    }

    private static boolean valid(String scope, double x, double y, double z) {
        return scope != null && !scope.isBlank() && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
