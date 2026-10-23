// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;

final class MainGUIFlatPanelRenderer {
    private final MainGUI owner;

    MainGUIFlatPanelRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void render(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        float scrollTop = this.owner.contentScrollTop();
        float scrollH = this.owner.contentScrollHeight();
        float groupX = context.layout.contX + 20.0f;
        float groupW = context.layout.contW - 40.0f;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, context.layout.contX, scrollTop, context.layout.contW, scrollH);
        float y = scrollTop - context.scroll.scrollY;
        float total = 0.0f;
        for (ModulesTab.SubTab subTab : this.owner.flatSubtabsForCurrentFilter()) {
            for (SettingGroup group : subTab.groups()) {
                total += 14.0f;
                this.owner.renderFlatGroupLabelPanel(nvg, group, groupX, y += 14.0f, groupW, mx, my);
                y += 32.0f;
                total += 32.0f;
                if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
                y += 5.0f;
                total += 5.0f;
                if (this.owner.isPetTrackerSettingsGroup(group)) {
                    float groupHeight = this.owner.petTrackerGroupHeightValue();
                    if (y + groupHeight > scrollTop && y < scrollTop + scrollH) {
                        this.owner.renderPetTrackerGroupPanel(nvg, group, groupX, y, groupW, mx, my);
                    }
                    y += groupHeight;
                    total += groupHeight;
                } else {
                    for (Setting setting : group.getSettings()) {
                        if (!setting.isVisible()) continue;
                        float settingHeight = this.owner.settingHeightFor(setting, groupW);
                        if (y + settingHeight > scrollTop && y < scrollTop + scrollH) {
                            this.owner.renderSettingRow(nvg, setting, groupX, y, groupW, settingHeight, mx, my);
                        }
                        y += settingHeight;
                        total += settingHeight;
                    }
                }
                y += 4.0f;
                total += 4.0f;
            }
        }
        this.owner.popContentLocalScissor(nvg, clipped);
        this.owner.renderMainScrollbar(nvg, total, scrollTop, scrollH, context.layout.contX + context.layout.contW - 6.0f);
    }
}
