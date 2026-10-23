// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.MainGUIModuleSection;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.theme.Theme;
import java.util.List;

final class MainGUIModuleOverviewRenderer {
    private final MainGUI owner;

    MainGUIModuleOverviewRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void render(NVGRenderer nvg, float mx, float my, float scrollTop, float scrollH) {
        MainGUIContext context = this.owner.context();
        float gridX = context.layout.contX + 20.0f;
        float gridW = context.layout.contW - 40.0f;
        float cardW = (gridW - 20.0f) / 3.0f;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, context.layout.contX, scrollTop, context.layout.contW, scrollH);
        float y = scrollTop - context.scroll.scrollY;
        float total = 0.0f;
        for (MainGUIModuleSection section : this.owner.moduleSectionsForFilter()) {
            if (section.subtabs().isEmpty()) continue;
            float gap = total == 0.0f ? 13.200001f : 22.0f;
            total += gap;
            this.owner.renderSectionHeader(nvg, section.name(), gridX, y += gap, gridW);
            y += 26.0f;
            total += 26.0f;
            y += 10.0f;
            total += 10.0f;
            List<ModulesTab.SubTab> subtabs = section.subtabs();
            for (int i = 0; i < subtabs.size(); i += 3) {
                int idx;
                for (int col = 0; col < 3 && (idx = i + col) < subtabs.size(); ++col) {
                    ModulesTab.SubTab subTab = subtabs.get(idx);
                    float cardX = gridX + (float)col * (cardW + 10.0f);
                    float hover = this.owner.moduleCardHoverProgress(subTab);
                    boolean inCard = mx >= cardX && mx < cardX + cardW && my >= y && my < y + 82.0f;
                    hover += ((inCard ? 1.0f : 0.0f) - hover) * Math.min(1.0f, Theme.animationFactor() * 7.0f);
                    this.owner.setModuleCardHoverProgress(subTab, hover);
                    if (!(y + 82.0f > scrollTop) || !(y < scrollTop + scrollH)) continue;
                    this.owner.renderModuleCardPanel(nvg, subTab, cardX, y, cardW, hover, mx, my);
                }
                y += 92.0f;
                total += 92.0f;
            }
            total -= 10.0f;
            y -= 10.0f;
            y += 14.0f;
            total += 14.0f;
        }
        this.owner.popContentLocalScissor(nvg, clipped);
        this.owner.renderMainScrollbar(nvg, total, scrollTop, scrollH, context.layout.contX + context.layout.contW - 6.0f);
    }
}
