package dev.kuan.kuanperion;

/** One signal per continuous configured stall; intentional pauses start a fresh interval. */
final class FarmingStallGuard {
    private static final long STALL_NANOS = 5_000_000_000L;
    private static final double MOVEMENT_SQUARED = 0.05 * 0.05;
    private boolean tracking;
    private boolean notified;
    private double anchorX, anchorZ;
    private long lastMovementAt;
    private long activeTimeout;

    boolean update(boolean eligible, double x, double z, long now) {return update(eligible,x,z,now,STALL_NANOS);}

    boolean update(boolean eligible, double x, double z, long now,long timeoutNanos) {
        if(timeoutNanos<=0)throw new IllegalArgumentException("Stall interval must be positive");
        if(activeTimeout!=timeoutNanos){reset();activeTimeout=timeoutNanos;}
        if (!eligible || !Double.isFinite(x) || !Double.isFinite(z)) {
            reset();
            return false;
        }
        double dx = x - anchorX;
        double dz = z - anchorZ;
        if (!tracking || dx * dx + dz * dz >= MOVEMENT_SQUARED) {
            tracking = true;
            notified = false;
            anchorX = x;
            anchorZ = z;
            lastMovementAt = now;
            return false;
        }
        if (!notified && now - lastMovementAt >= timeoutNanos) {
            notified = true;
            return true;
        }
        return false;
    }

    void reset() {
        tracking = false;
        notified = false;
    }
}
