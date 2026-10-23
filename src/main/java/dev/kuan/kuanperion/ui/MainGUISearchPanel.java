// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIRegistry;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.List;

final class MainGUISearchPanel {
    private static final float RESULT_H = 54.0f;
    private static final float RESULT_GAP = 6.0f;
    private static final float MODULE_GAP = 10.0f;
    private final MainGUI owner;
    private final List<SearchResult> searchResults = new ArrayList<SearchResult>();

    MainGUISearchPanel(MainGUI owner) {
        this.owner = owner;
    }

    void render(NVGRenderer nvg, float mx, float my) {
        float gx = this.owner.contX + 20.0f;
        float gw = this.owner.contW - 40.0f;
        float resultsTop = this.owner.contY + 53.0f + 1.0f;
        float resultsH = this.owner.contH - 53.0f - 1.0f;
        this.buildResults(this.owner.searchQuery.toLowerCase());
        float y = resultsTop + 10.0f - this.owner.searchScrollY;
        float total = 10.0f;
        nvg.pushScissor(this.owner.contX, resultsTop, this.owner.contW, resultsH);
        ModulesTab.SubTab lastSubtab = null;
        for (SearchResult result : this.searchResults) {
            if (result.subtab() != lastSubtab) {
                if (lastSubtab != null) {
                    y += 10.0f;
                    total += 10.0f;
                }
                this.renderModuleHeader(nvg, result, gx, y, gw);
                y += 46.0f;
                total += 46.0f;
                y += 5.0f;
                total += 5.0f;
                lastSubtab = result.subtab();
            }
            if (y + 54.0f > resultsTop && y < resultsTop + resultsH) {
                this.renderResult(nvg, result, gx, y, gw, mx, my);
                this.owner.addClickArea(gx, y, gw, 54.0f, () -> this.owner.openSearchResult(result.mainTab(), result.subtab(), result.group(), result.setting()));
            }
            y += 60.0f;
            total += 60.0f;
        }
        if (this.searchResults.isEmpty()) {
            nvg.textCentered("Inter-Regular", UiLang.localize("No results for") + " \"" + this.owner.searchQuery + "\"", this.owner.contX, y + 16.0f, this.owner.contW, 20.0f, 11.0f, Theme.TEXT_DIM);
        }
        this.owner.searchMaxScrollY = Math.max(0.0f, total - resultsH + 16.0f);
        nvg.popScissor();
        if (this.owner.searchMaxScrollY > 0.0f) {
            float ratio = resultsH / (total + 16.0f);
            float thumbH = Math.max(30.0f, resultsH * ratio);
            float trackT = resultsTop + 10.0f;
            float trackH = resultsH - 20.0f;
            this.owner.sbSrchTrackT = trackT;
            this.owner.sbSrchTrackH = trackH;
            this.owner.sbSrchThumbH = thumbH;
            float thumbY = MainGUI.scrollbarThumbY(trackT, trackH, thumbH, this.owner.searchScrollY, this.owner.searchMaxScrollY);
            nvg.roundedRect(this.owner.contX + this.owner.contW - 6.0f, thumbY, 4.0f, thumbH, 2.0f, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.5f));
        }
    }

    void handleClick(float mx, float my) {
        float gx = this.owner.contX + 20.0f;
        float gw = this.owner.contW - 40.0f;
        if (mx < gx || mx > gx + gw) {
            return;
        }
        float resultsTop = this.owner.contY + 53.0f + 1.0f;
        float y = resultsTop + 10.0f - this.owner.searchScrollY;
        ModulesTab.SubTab lastSubtab = null;
        for (SearchResult result : this.searchResults) {
            if (result.subtab() != lastSubtab) {
                if (lastSubtab != null) {
                    y += 10.0f;
                }
                y += 51.0f;
                lastSubtab = result.subtab();
            }
            if (my >= y && my <= y + 54.0f) {
                this.owner.openSearchResult(result.mainTab(), result.subtab(), result.group(), result.setting());
                return;
            }
            y += 60.0f;
        }
    }

    private void renderModuleHeader(NVGRenderer nvg, SearchResult result, float x, float y, float w) {
        nvg.roundedRect(x, y, w, 46.0f, 6.0f, Theme.BG_SECONDARY);
        nvg.roundedRect(x, y + 10.0f, 3.0f, 26.0f, 2.0f, Theme.ACCENT_PRIMARY);
        nvg.text("Inter-Bold", result.subtab().name(), x + 12.0f, y + 9.0f, 13.0f, Theme.TEXT_PRIMARY);
        String context = UiLang.localize(result.sourceLabel()) + " > " + result.subtab().name();
        nvg.textRight("Inter-Regular", context, x, y + 9.0f, w - 8.0f, 10.0f, Theme.TEXT_DIM);
    }

    private void renderResult(NVGRenderer nvg, SearchResult result, float x, float y, float w, float mx, float my) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + 54.0f;
        int background = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.12f) : Theme.BG_SECONDARY;
        int border = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.55f) : Theme.BORDER_DEFAULT;
        nvg.roundedRect(x, y, w, 54.0f, 7.0f, background);
        nvg.rectOutline(x, y, w, 54.0f, 7.0f, 1.0f, border);
        String title = result.setting() == null ? (result.group() == null ? result.subtab().name() : result.group().getName()) : result.setting().getName();
        this.owner.offerHoverHelp("search-result:" + result.mainTab() + ":" + result.subtab().name() + ":" + title, UiLang.localize(title), MainGUISearchPanel.resultTooltip(result), x, y, w, 54.0f, mx, my);
        nvg.text("Inter-Bold", UiLang.localize(title), x + 14.0f, y + 10.0f, 12.5f, hovered ? Theme.TEXT_PRIMARY : Theme.TEXT_VALUE);
        String context = result.setting() == null ? UiLang.localize("Module settings") : (result.group() == null ? result.subtab().name() : result.group().getName());
        nvg.text("Inter-Regular", UiLang.localize(context), x + 14.0f, y + 31.0f, 10.5f, Theme.TEXT_MUTED);
        nvg.textRight("Inter-Regular", "\u2192", x, y + 16.0f, w - 14.0f, 18.0f, hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_DIM);
    }

    private void buildResults(String query) {
        this.searchResults.clear();
        for (MainGUIRegistry.ModuleSection section : MainGUIRegistry.MODULE_SECTIONS) {
            this.addResults(this.searchResults, 0, section.displayName(), section.subtabs(), query);
        }
        this.addResults(this.searchResults, 1, UiLang.localize("Colors"), MainGUIRegistry.COLORS_SUBTABS, query);
        this.addResults(this.searchResults, 3, UiLang.localize("Keybinds"), MainGUIRegistry.KEYBINDS_SUBTABS, query);
        this.addResults(this.searchResults, 4, UiLang.localize("Settings"), MainGUIRegistry.SETTINGS_SUBTABS, query);
    }

    private void addResults(List<SearchResult> out, int mainTab, String sourceLabel, List<ModulesTab.SubTab> subtabs, String query) {
        for (ModulesTab.SubTab subtab : subtabs) {
            boolean subtabMatched = MainGUISearchPanel.matchesQuery(subtab.name(), null, subtab.description(), query);
            boolean addedForSubtab = false;
            for (SettingGroup group : subtab.groups()) {
                boolean groupMatched = MainGUISearchPanel.matchesQuery(group.getName(), group.getRawName(), group.getDescription(), query);
                boolean addedForGroup = false;
                for (Setting setting : group.getSettings()) {
                    if (!setting.isVisible() || !subtabMatched && !groupMatched && !MainGUISearchPanel.matchesQuery(setting.getName(), setting.getRawName(), setting.getDescription(), query)) continue;
                    out.add(new SearchResult(mainTab, sourceLabel, subtab, group, setting));
                    addedForGroup = true;
                    addedForSubtab = true;
                }
                if (!groupMatched || addedForGroup) continue;
                out.add(new SearchResult(mainTab, sourceLabel, subtab, group, null));
                addedForSubtab = true;
            }
            if (!subtabMatched || addedForSubtab) continue;
            out.add(new SearchResult(mainTab, sourceLabel, subtab, null, null));
        }
    }

    private static String resultTooltip(SearchResult result) {
        if (result.setting() != null) {
            return result.setting().getDescription();
        }
        if (result.group() != null) {
            return result.group().getDescription();
        }
        return result.subtab().description();
    }

    private static boolean matchesQuery(String localized, String raw, String tooltip, String query) {
        return MainGUISearchPanel.containsIgnoreCase(localized, query) || MainGUISearchPanel.containsIgnoreCase(raw, query) || MainGUISearchPanel.containsIgnoreCase(tooltip, query);
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private record SearchResult(int mainTab, String sourceLabel, ModulesTab.SubTab subtab, SettingGroup group, Setting setting) {
    }
}
