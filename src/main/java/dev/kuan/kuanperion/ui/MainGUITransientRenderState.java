// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;

final class MainGUITransientRenderState {
    ColorSetting hoveredColor;
    boolean activeEditorBoundsSet;
    float filterBarAnimX;
    float filterBarAnimW;
    float filterBarTargetX;
    float filterBarTargetW;
    boolean filterBarInited;
    float maxScrollY;
    float searchMaxScrollY;
    float profileMaxScrollY;
    float subtabMaxScrollY;
    float sbMainTrackT;
    float sbMainTrackH;
    float sbMainThumbH;
    float sbSrchTrackT;
    float sbSrchTrackH;
    float sbSrchThumbH;
    float sbProfTrackT;
    float sbProfTrackH;
    float sbProfThumbH;
    float sbSubTrackT;
    float sbSubTrackH;
    float sbSubThumbH;
    DropdownSetting openDropdown;
    ColorSetting activeColor;
    boolean suppressNestedContentScissor;
    float scrollY;
    float targetScrollY;

    MainGUITransientRenderState() {
    }
}
