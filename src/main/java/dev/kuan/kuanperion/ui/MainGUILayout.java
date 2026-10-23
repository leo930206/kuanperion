// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUIContext;

final class MainGUILayout {
    private static final float PANEL_MARGIN = 40.0f;
    private static final float MAX_PANEL_W = 1060.0f;
    private static final float MAX_PANEL_H = 660.0f;

    MainGUILayout() {
    }

    MainGUIContext.LayoutState computeFrameLayout(int width, int height, float pixelRatio, float uiScale, float sidebarWidth) {
        MainGUIContext.LayoutState layout = new MainGUIContext.LayoutState();
        layout.pr = pixelRatio;
        layout.physW = (float)width * pixelRatio;
        layout.physH = (float)height * pixelRatio;
        float canvasW = layout.physW / uiScale;
        float canvasH = layout.physH / uiScale;
        layout.pw = Math.min(1060.0f, canvasW - 40.0f);
        layout.ph = Math.min(660.0f, canvasH - 40.0f);
        layout.px = (canvasW - layout.pw) / 2.0f;
        layout.py = (canvasH - layout.ph) / 2.0f;
        layout.contX = layout.px + sidebarWidth;
        layout.contY = layout.py;
        layout.contW = layout.pw - sidebarWidth;
        layout.contH = layout.ph;
        return layout;
    }

    float contentScrollTop(float contY, boolean hasTopFilterBar, float topBarHeight, float filterBarHeight) {
        return contY + topBarHeight + 1.0f + (hasTopFilterBar ? filterBarHeight + 1.0f : 0.0f);
    }

    float contentScrollHeight(float contY, float contH, boolean hasTopFilterBar, float topBarHeight, float filterBarHeight) {
        return contH - (this.contentScrollTop(contY, hasTopFilterBar, topBarHeight, filterBarHeight) - contY);
    }
}
