package dev.kuan.skyblockassistant;

import java.util.Locale;

final class FarmingToolGuard {
    private String expectedTool;
    private boolean paused;

    void reset(String expectedTool) {
        this.expectedTool = expectedTool;
        paused = false;
    }

    String expectedTool() {
        return expectedTool;
    }

    boolean matches(String heldTool) {
        return expectedTool == null || (heldTool != null
                && heldTool.toLowerCase(Locale.ROOT).contains(expectedTool.toLowerCase(Locale.ROOT)));
    }

    boolean update(boolean detectionEnabled, String heldTool) {
        paused = detectionEnabled && !matches(heldTool);
        return paused;
    }

    boolean isPaused() {
        return paused;
    }
}
