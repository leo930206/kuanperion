// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;

final class DropdownChevron {
    private static final float HALF_WIDTH = 3.5f;
    private static final float HALF_HEIGHT = 2.25f;
    private static final float STROKE_WIDTH = 1.5f;

    private DropdownChevron() {
    }

    static void renderDown(NVGRenderer nvg, float x, float y, float width, float height, int color) {
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        nvg.line(centerX - 3.5f, centerY - 2.25f, centerX, centerY + 2.25f, 1.5f, color);
        nvg.line(centerX, centerY + 2.25f, centerX + 3.5f, centerY - 2.25f, 1.5f, color);
    }
}
