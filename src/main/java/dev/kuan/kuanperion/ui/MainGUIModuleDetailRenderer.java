// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.SelectionAnimation;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;
import java.util.List;

final class MainGUIModuleDetailRenderer {
    private final MainGUI owner;
    private final SelectionAnimation categorySelection = new SelectionAnimation();
    private ModulesTab.SubTab categorySubTab;

    MainGUIModuleDetailRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void renderTopBar(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        String moduleName = this.owner.moduleCardNameFor(this.owner.getActiveModuleSubTab());
        float totalHeight = 53.0f;
        String backLabel = "\u2190 " + UiLang.localize("Back");
        float backTextWidth = nvg.textWidth("Inter-Regular", backLabel, 11.0f);
        float backX = context.layout.contX + 20.0f;
        float textCenterY = context.layout.contY + (totalHeight - 11.0f) / 2.0f;
        boolean backHovered = mx >= context.layout.contX + 14.0f && mx < context.layout.contX + 14.0f + backTextWidth + 16.0f && my >= context.layout.contY && my < context.layout.contY + totalHeight;
        float buttonBg = backHovered ? 0.08f : 0.04f;
        nvg.roundedRect(context.layout.contX + 14.0f, context.layout.contY + (totalHeight - 26.0f) / 2.0f, backTextWidth + 16.0f, 26.0f, 5.0f, Theme.withAlpha(Theme.TEXT_PRIMARY, buttonBg));
        nvg.text("Inter-Regular", backLabel, backX + 2.0f, textCenterY, 11.0f, backHovered ? Theme.TEXT_PRIMARY : Theme.TEXT_VALUE);
        this.owner.addClickArea(context.layout.contX + 14.0f, context.layout.contY, backTextWidth + 16.0f, totalHeight, this.owner::exitModuleDetailView);
        float sep1X = context.layout.contX + 20.0f + backTextWidth + 14.0f;
        nvg.rect(sep1X, context.layout.contY + 15.0f, 1.0f, totalHeight - 30.0f, Theme.SEPARATOR);
        float nameX = sep1X + 12.0f;
        float nameWidth = nvg.textWidth("Inter-Bold", moduleName, 13.0f);
        nvg.text("Inter-Bold", moduleName, nameX, context.layout.contY + (totalHeight - 13.0f) / 2.0f, 13.0f, Theme.TEXT_PRIMARY);
        float sep2X = nameX + nameWidth + 12.0f;
        nvg.rect(sep2X, context.layout.contY + 15.0f, 1.0f, totalHeight - 30.0f, Theme.SEPARATOR);
        float descY = context.layout.contY + 20.5f;
        nvg.text("Inter-Regular", UiLang.localize("Module settings"), sep2X + 12.0f, descY, 12.0f, Theme.TEXT_MUTED);
        nvg.rect(context.layout.contX, context.layout.contY + totalHeight, context.layout.contW, 1.0f, Theme.SEPARATOR);
    }

    void renderCategoryPanel(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        boolean skipAll;
        MainGUIContext context = this.owner.context();
        ModulesTab.SubTab activeSubTab = this.owner.getActiveModuleSubTab();
        float categoryX = context.layout.contX;
        float categoryW = 160.0f;
        boolean subtabEnabled = this.owner.isModuleConfigurationEditable(activeSubTab);
        nvg.rect(categoryX + categoryW, panelTop, 1.0f, panelH, Theme.SEPARATOR);
        nvg.text("Inter-Bold", UiLang.localize("Categories").toUpperCase(), categoryX + 16.0f, panelTop + 18.0f, 9.0f, Theme.withAlpha(Theme.TEXT_MUTED, 185));
        float itemHeight = 36.0f;
        float itemsStart = panelTop + 44.0f;
        List<SettingGroup> groups = activeSubTab.groups();
        boolean bl = skipAll = groups.size() == 1;
        if (skipAll && this.owner.getActiveCategoryIndex() == 0) {
            this.owner.setActiveCategoryIndex(1);
            context = this.owner.context();
        }
        int totalItems = skipAll ? groups.size() : groups.size() + 1;
        int selectedRow = skipAll ? Math.max(0, this.owner.getActiveCategoryIndex() - 1) : this.owner.getActiveCategoryIndex();
        long nowNanos = System.nanoTime();
        if (this.categorySubTab != activeSubTab) {
            this.categorySelection.reset((float)selectedRow * itemHeight, nowNanos);
            this.categorySubTab = activeSubTab;
        }
        float highlightY = this.categorySelection.update((float)selectedRow * itemHeight, Theme.ANIM_TIME_MS, nowNanos);
        nvg.roundedRect(categoryX + 8.0f, itemsStart + highlightY + 3.0f, categoryW - 16.0f, 30.0f, 6.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, subtabEnabled ? 0.12f : 0.06f));
        for (int i = 0; i < totalItems; ++i) {
            boolean isAll = !skipAll && i == 0;
            int groupIndex = skipAll ? i : i - 1;
            String label = isAll ? UiLang.localize("All") : UiLang.localize(groups.get(groupIndex).getName());
            Object animKey = this.owner.moduleCategoryAnimationKey(isAll ? null : groups.get(groupIndex), isAll);
            float itemY = itemsStart + (float)i * itemHeight;
            boolean selected = this.owner.getActiveCategoryIndex() == (skipAll ? i + 1 : i);
            float hover = this.owner.moduleCategoryHoverProgress(animKey);
            boolean inItem = mx >= categoryX + 6.0f && mx < categoryX + categoryW - 6.0f && my >= itemY && my < itemY + itemHeight;
            hover += ((!selected && inItem ? 1.0f : 0.0f) - hover) * Math.min(1.0f, Theme.animationFactor() * 7.0f);
            this.owner.setModuleCategoryHoverProgress(animKey, hover);
            if (selected) {
                nvg.text("Inter-Bold", label, categoryX + 18.0f, itemY + 12.0f, 11.0f, subtabEnabled ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_DIM, 190));
            } else {
                if (hover > 0.01f) {
                    nvg.roundedRect(categoryX + 8.0f, itemY + 3.0f, categoryW - 16.0f, 30.0f, 6.0f, Theme.withAlpha(-1, hover * 0.05f));
                }
                int textColor = subtabEnabled ? (hover > 0.01f ? Theme.blend(Theme.TEXT_MUTED, Theme.TEXT_VALUE, hover) : Theme.TEXT_MUTED) : Theme.withAlpha(Theme.TEXT_DIM, 165);
                nvg.text("Inter-Regular", label, categoryX + 18.0f, itemY + 12.0f, 11.0f, textColor);
            }
            if (!subtabEnabled) continue;
            int categoryIndex = skipAll ? i + 1 : i;
            this.owner.addClickArea(categoryX + 6.0f, itemY, categoryW - 12.0f, itemHeight, () -> this.owner.selectModuleCategory(categoryIndex));
        }
    }

    void renderSettingsPanel(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        MainGUIContext context = this.owner.context();
        ModulesTab.SubTab activeSubTab = this.owner.getActiveModuleSubTab();
        float rightX = context.layout.contX + 160.0f + 1.0f;
        float rightW = context.layout.contW - 160.0f - 1.0f;
        boolean subtabEnabled = this.owner.isModuleConfigurationEditable(activeSubTab);
        String moduleName = this.owner.moduleCardNameFor(activeSubTab);
        String moduleDescription = UiLang.localize(activeSubTab.description());
        float groupX = rightX + 20.0f;
        float groupW = rightW - 40.0f;
        nvg.text("Inter-Bold", moduleName, rightX + 20.0f, panelTop + 16.0f, 15.0f, Theme.TEXT_PRIMARY);
        nvg.text("Inter-Regular", moduleDescription, rightX + 20.0f, panelTop + 37.0f, 10.0f, Theme.TEXT_MUTED);
        if (activeSubTab.hasToggle()) {
            float pillX = groupX + groupW - 52.0f;
            float pillY = panelTop + 26.5f;
            this.owner.renderPillControl(nvg, pillX, pillY, activeSubTab.isEnabled(), (Object)activeSubTab);
            this.owner.addClickArea(pillX - 6.0f, pillY - 6.0f, 48.0f, 33.0f, activeSubTab::toggle);
        }
        nvg.rect(rightX, panelTop + 74.0f, rightW, 1.0f, Theme.SEPARATOR);
        int maxCategoryIndex = activeSubTab.groups().size();
        if (this.owner.getActiveCategoryIndex() > maxCategoryIndex) {
            this.owner.setActiveCategoryIndex(0);
        }
        boolean showAll = this.owner.getActiveCategoryIndex() == 0;
        List<SettingGroup> renderGroups = showAll ? activeSubTab.groups() : List.of(activeSubTab.groups().get(this.owner.getActiveCategoryIndex() - 1));
        float settingsTop = panelTop + 74.0f + 1.0f;
        float settingsH = panelH - 74.0f - 1.0f;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, rightX, settingsTop, rightW, settingsH);
        context = this.owner.context();
        float y = settingsTop - context.scroll.scrollY;
        float total = 0.0f;
        for (SettingGroup group : renderGroups) {
            y += 14.0f;
            total += 14.0f;
            if (y + 46.0f > settingsTop && y < settingsTop + settingsH) {
                this.owner.renderGroupHeaderPanel(nvg, group, groupX, y, groupW, mx, my, subtabEnabled);
            }
            y += 46.0f;
            total += 46.0f;
            if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
            y += 5.0f;
            total += 5.0f;
            if (this.owner.isPetTrackerSettingsGroup(group)) {
                float groupHeight = this.owner.petTrackerGroupHeightValue();
                if (y + groupHeight > settingsTop && y < settingsTop + settingsH) {
                    this.owner.renderPetTrackerGroupPanel(nvg, group, groupX, y, groupW, mx, my);
                    if (!subtabEnabled) {
                        float cardH = groupHeight - 5.0f;
                        nvg.roundedRect(groupX, y, groupW, cardH, 7.0f, Theme.withAlpha(Theme.PANEL_BG, 0.42f));
                    }
                }
                y += groupHeight;
                total += groupHeight;
            } else {
                for (Setting setting : group.getSettings()) {
                    if (!setting.isVisible()) continue;
                    float settingHeight = this.owner.settingHeightFor(setting, groupW);
                    if (y + settingHeight > settingsTop && y < settingsTop + settingsH) {
                        this.owner.renderSettingRow(nvg, setting, groupX, y, groupW, settingHeight, mx, my);
                        if (!subtabEnabled) {
                            float cardH = settingHeight - 5.0f;
                            nvg.roundedRect(groupX, y, groupW, cardH, 7.0f, Theme.withAlpha(Theme.PANEL_BG, 0.42f));
                        }
                    }
                    y += settingHeight;
                    total += settingHeight;
                }
            }
            y += 4.0f;
            total += 4.0f;
        }
        this.owner.popContentLocalScissor(nvg, clipped);
        this.owner.renderMainScrollbar(nvg, total, settingsTop, settingsH, rightX + rightW - 6.0f);
    }
}
