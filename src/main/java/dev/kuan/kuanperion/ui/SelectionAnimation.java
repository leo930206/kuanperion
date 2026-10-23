// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

final class SelectionAnimation {
    private double value;
    private double velocity;
    private long lastNanos;
    private boolean initialized;

    SelectionAnimation() {
    }

    float update(float target, float durationMs, long nowNanos) {
        if (!this.initialized) {
            this.reset(target, nowNanos);
            return target;
        }
        double seconds = (double)Math.max(0L, nowNanos - this.lastNanos) / 1.0E9;
        this.lastNanos = nowNanos;
        double omega = 6000.0 / Math.max(1.0, (double)durationMs);
        double offset = this.value - (double)target;
        double decay = Math.exp(-omega * seconds);
        double step = (this.velocity + omega * offset) * seconds;
        this.value = (double)target + (offset + step) * decay;
        this.velocity = (this.velocity - omega * step) * decay;
        if (Math.abs(this.value - (double)target) < 0.01 && Math.abs(this.velocity) < 0.01) {
            this.value = target;
            this.velocity = 0.0;
        }
        return (float)this.value;
    }

    void reset(float target, long nowNanos) {
        this.value = target;
        this.velocity = 0.0;
        this.lastNanos = nowNanos;
        this.initialized = true;
    }
}
