package dev.kuan.skyblockassistant;

final class GuiPauseGuard {
    private static final long RESUME_DELAY_NANOS = 1_000_000_000L;
    private boolean screenWasOpen;
    private boolean buffering;
    private long closedAtNanos;

    boolean shouldPause(boolean enabled, boolean screenOpen, long nowNanos) {
        if (!enabled) {
            reset();
            return false;
        }
        if (screenOpen) {
            screenWasOpen = true;
            buffering = false;
            return true;
        }
        if (screenWasOpen) {
            screenWasOpen = false;
            buffering = true;
            closedAtNanos = nowNanos;
        }
        if (buffering && nowNanos - closedAtNanos >= RESUME_DELAY_NANOS) buffering = false;
        return buffering;
    }

    void reset() {
        screenWasOpen = false;
        buffering = false;
        closedAtNanos = 0;
    }
}
