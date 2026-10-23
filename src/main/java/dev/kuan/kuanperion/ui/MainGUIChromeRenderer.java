// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.SelectionAnimation;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;

final class MainGUIChromeRenderer {
    private final MainGUI owner;
    private final SelectionAnimation sidebarSelection = new SelectionAnimation();
    private final SelectionAnimation filterPosition = new SelectionAnimation();
    private final SelectionAnimation filterWidth = new SelectionAnimation();

    MainGUIChromeRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void renderSidebar(NVGRenderer nvg, float mx, float my) {
        int settingsColor;
        boolean settingsHovered;
        int keybindsColor;
        boolean keybindsHovered;
        int hudColor;
        MainGUIContext context = this.owner.context();
        if (context.animation.computedSidebarExpanded < 1.0f) {
            float maxTW = 0.0f;
            for (String name : new String[]{UiLang.localize("Modules"), UiLang.localize("Colors"), UiLang.localize("Config"), UiLang.localize("Keybinds"), UiLang.localize("Settings"), UiLang.localize("About")}) {
                maxTW = Math.max(maxTW, nvg.textWidth("Inter-Regular", name, 12.0f));
            }
            this.owner.setComputedSidebarExpanded(Math.max(194.0f, 72.0f + Math.max(maxTW, nvg.textWidth("Inter-Bold", "Kuanperion", 18.0f))));
            context = this.owner.context();
        }
        float sbW = 60.0f + context.animation.sidebarAnim * (context.animation.computedSidebarExpanded - 60.0f);
        float textX = context.layout.px + 12.0f + 36.0f + 10.0f;
        nvg.roundedRect(context.layout.px, context.layout.py, sbW, context.layout.ph, 10.0f, Theme.SIDEBAR_BG);
        nvg.rect(context.layout.px + 10.0f, context.layout.py, sbW - 10.0f, context.layout.ph, Theme.SIDEBAR_BG);
        nvg.rect(context.layout.px + sbW - 1.0f, context.layout.py, 1.0f, context.layout.ph, Theme.SEPARATOR);
        nvg.pushScissor(context.layout.px, context.layout.py, sbW, context.layout.ph);
        float logoSize = 34.0f;
        float logoX = context.layout.px + 12.0f + (36.0f - logoSize) / 2.0f;
        float logoY = context.layout.py + (56.0f - logoSize) / 2.0f;
        nvg.renderSVG("/assets/kuanperion/icons/logo.svg", logoX, logoY, logoSize, logoSize, Theme.ACCENT_PRIMARY);
        if (context.animation.sidebarAnim > 0.01f) {
            nvg.save();
            nvg.globalAlpha(context.animation.sidebarAnim);
            nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
            nvg.text("Inter-Bold", UiLang.localize("Kuanperion"), textX, logoY + (logoSize - 14.0f) / 2.0f, 18.0f, Theme.ACCENT_PRIMARY);
            nvg.restore();
        }
        float sepY = context.layout.py + 56.0f;
        nvg.rect(context.layout.px + 12.0f, sepY, sbW - 24.0f, 1.0f, Theme.SEPARATOR);
        int sidebarHoverColor = Theme.TEXT_VALUE;
        String[] tabIds = new String[]{"modules", "colors", "config"};
        String[] tabLabels = new String[]{UiLang.localize("Modules"), UiLang.localize("Colors"), UiLang.localize("Config")};
        float[] tabIconW = new float[]{16.0f, 16.0f, 14.0f};
        float[] tabIconH = new float[]{19.0f, 16.0f, 18.0f};
        float tabsY = sepY + 8.0f;
        float profileTabY = context.layout.py + context.layout.ph - 8.0f - 44.0f;
        float settingsTabY = profileTabY - 44.0f - 8.0f;
        float keybindsTabY = settingsTabY - 44.0f;
        float hudPositionsTabY = keybindsTabY - 44.0f;
        float selectedY = switch (context.navigation.activeMain) {
            case 3 -> keybindsTabY;
            case 4 -> settingsTabY;
            default -> tabsY + (float)context.navigation.activeMain * 44.0f;
        };
        float highlightY = this.sidebarSelection.update(selectedY - context.layout.py, Theme.ANIM_TIME_MS, System.nanoTime());
        nvg.roundedRect(context.layout.px + 12.0f, context.layout.py + highlightY + 4.0f, 36.0f, 36.0f, 8.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 85));
        for (int i = 0; i < tabLabels.length; ++i) {
            int color;
            boolean hovered;
            float tabY = tabsY + (float)i * 44.0f;
            float pillY = tabY + 4.0f;
            boolean selected = context.navigation.activeMain == i;
            boolean bl = hovered = !selected && mx >= context.layout.px && mx < context.layout.px + sbW && my >= tabY && my <= tabY + 44.0f;
            color = selected ? Theme.ACCENT_PRIMARY : (hovered ? sidebarHoverColor : Theme.TEXT_MUTED);
            if (hovered) {
                nvg.roundedRect(context.layout.px + 12.0f, pillY, 36.0f, 36.0f, 8.0f, Theme.withAlpha(Theme.TEXT_MUTED, 51));
            }
            float iconOffX = (36.0f - tabIconW[i]) / 2.0f;
            float iconOffY = (36.0f - tabIconH[i]) / 2.0f;
            nvg.renderSVG("/assets/kuanperion/icons/" + tabIds[i] + ".svg", context.layout.px + 12.0f + iconOffX, pillY + iconOffY, tabIconW[i], tabIconH[i], color);
            if (context.animation.sidebarAnim > 0.01f) {
                nvg.save();
                nvg.globalAlpha(context.animation.sidebarAnim);
                nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
                nvg.text("Inter-Regular", tabLabels[i], textX, pillY + 12.0f, 12.0f, color);
                nvg.restore();
            }
            String string = "sidebar:" + tabIds[i];
            String string2 = tabLabels[i];
            this.owner.offerHoverHelp(string, string2, switch (i) {
                case 0 -> UiLang.localize("Manage and configure modules");
                case 1 -> UiLang.localize("Customise HUD and menu colors");
                default -> UiLang.localize("Save and manage config and theme profiles");
            }, context.layout.px, tabY, sbW, 44.0f, mx, my);
        }
        nvg.rect(context.layout.px + 12.0f, hudPositionsTabY - 8.0f, sbW - 24.0f, 1.0f, Theme.SEPARATOR);
        float hudPillY = hudPositionsTabY + 4.0f;
        boolean hudHovered = mx >= context.layout.px && mx < context.layout.px + sbW && my >= hudPositionsTabY && my <= hudPositionsTabY + 44.0f;
        int n = hudColor = hudHovered ? sidebarHoverColor : Theme.TEXT_MUTED;
        if (hudHovered) {
            nvg.roundedRect(context.layout.px + 12.0f, hudPillY, 36.0f, 36.0f, 8.0f, Theme.withAlpha(Theme.TEXT_MUTED, 51));
        }
        float hudIconSize = 18.0f;
        nvg.renderSVG("/assets/kuanperion/icons/hud_positions.svg", context.layout.px + 12.0f + (36.0f - hudIconSize) / 2.0f, hudPillY + (36.0f - hudIconSize) / 2.0f, hudIconSize, hudIconSize, hudColor);
        if (context.animation.sidebarAnim > 0.01f) {
            nvg.save();
            nvg.globalAlpha(context.animation.sidebarAnim);
            nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
            nvg.text("Inter-Regular", UiLang.localize("About"), textX, hudPillY + 12.0f, 12.0f, hudColor);
            nvg.restore();
        }
        this.owner.offerHoverHelp("sidebar:hud_positions", UiLang.localize("About"), UiLang.localize("Kuanperion version and interface settings."), context.layout.px, hudPositionsTabY, sbW, 44.0f, mx, my);
        float keybindsPillY = keybindsTabY + 4.0f;
        boolean keybindsSelected = context.navigation.activeMain == 3;
        boolean bl = keybindsHovered = !keybindsSelected && mx >= context.layout.px && mx < context.layout.px + sbW && my >= keybindsTabY && my <= keybindsTabY + 44.0f;
        keybindsColor = keybindsSelected ? Theme.ACCENT_PRIMARY : (keybindsHovered ? sidebarHoverColor : Theme.TEXT_MUTED);
        if (keybindsHovered) {
            nvg.roundedRect(context.layout.px + 12.0f, keybindsPillY, 36.0f, 36.0f, 8.0f, Theme.withAlpha(Theme.TEXT_MUTED, 51));
        }
        float keybindsIconSize = 18.0f;
        nvg.renderSVG("/assets/kuanperion/icons/keybinds.svg", context.layout.px + 12.0f + (36.0f - keybindsIconSize) / 2.0f, keybindsPillY + (36.0f - keybindsIconSize) / 2.0f, keybindsIconSize, keybindsIconSize, keybindsColor);
        if (context.animation.sidebarAnim > 0.01f) {
            nvg.save();
            nvg.globalAlpha(context.animation.sidebarAnim);
            nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
            nvg.text("Inter-Regular", UiLang.localize("Keybinds"), textX, keybindsPillY + 12.0f, 12.0f, keybindsColor);
            nvg.restore();
        }
        this.owner.offerHoverHelp("sidebar:keybinds", UiLang.localize("Keybinds"), UiLang.localize("Edit Kuanperion keybinds and keep them synced with Minecraft controls."), context.layout.px, keybindsTabY, sbW, 44.0f, mx, my);
        float settingsPillY = settingsTabY + 4.0f;
        boolean settingsSelected = context.navigation.activeMain == 4;
        boolean bl2 = settingsHovered = !settingsSelected && mx >= context.layout.px && mx < context.layout.px + sbW && my >= settingsTabY && my <= settingsTabY + 44.0f;
        settingsColor = settingsSelected ? Theme.ACCENT_PRIMARY : (settingsHovered ? sidebarHoverColor : Theme.TEXT_MUTED);
        if (settingsHovered) {
            nvg.roundedRect(context.layout.px + 12.0f, settingsPillY, 36.0f, 36.0f, 8.0f, Theme.withAlpha(Theme.TEXT_MUTED, 51));
        }
        float settingsIconSize = 16.0f;
        nvg.renderSVG("/assets/kuanperion/icons/settings.svg", context.layout.px + 12.0f + (36.0f - settingsIconSize) / 2.0f, settingsPillY + (36.0f - settingsIconSize) / 2.0f, settingsIconSize, settingsIconSize, settingsColor);
        if (context.animation.sidebarAnim > 0.01f) {
            nvg.save();
            nvg.globalAlpha(context.animation.sidebarAnim);
            nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
            nvg.text("Inter-Regular", UiLang.localize("Settings"), textX, settingsPillY + 12.0f, 12.0f, settingsColor);
            nvg.restore();
        }
        this.owner.offerHoverHelp("sidebar:settings", UiLang.localize("Settings"), UiLang.localize("General client settings."), context.layout.px, settingsTabY, sbW, 44.0f, mx, my);
        nvg.rect(context.layout.px + 12.0f, profileTabY - 8.0f, sbW - 24.0f, 1.0f, Theme.SEPARATOR);
        this.renderFarmProfile(nvg, context, textX, profileTabY, mx, my);
        nvg.popScissor();
    }

    void renderContentTopBar(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        String name = this.owner.currentTabName();
        String desc = this.owner.currentTabDescription();
        float nameY = context.layout.contY + 19.0f;
        nvg.text("Inter-Bold", name, context.layout.contX + 20.0f, nameY, 15.0f, Theme.TEXT_PRIMARY);
        float nameW = nvg.textWidth("Inter-Bold", name, 15.0f);
        float sepX = context.layout.contX + 20.0f + nameW + 10.0f;
        nvg.rect(sepX, context.layout.contY + 17.0f, 1.0f, 19.0f, Theme.SEPARATOR);
        float descY = context.layout.contY + 20.5f;
        nvg.text("Inter-Regular", desc, sepX + 10.0f, descY, 12.0f, Theme.TEXT_MUTED);
        float searchW = 190.0f;
        float searchH = 30.0f;
        float searchX = context.layout.contX + context.layout.contW - 20.0f - searchW;
        float searchY = context.layout.contY + (53.0f - searchH) / 2.0f;
        this.owner.setSearchBoxBounds(searchX, searchY, searchW, searchH);
        context = this.owner.context();
        int searchBg = Theme.BG_FIELD;
        int searchOutline = context.navigation.searchMode ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT;
        nvg.roundedRect(searchX, searchY, searchW, searchH, 7.0f, searchBg);
        nvg.rectOutlineSolid(searchX, searchY, searchW, searchH, 7.0f, 1.0f, searchOutline);
        nvg.renderSVG("/assets/kuanperion/icons/search.svg", searchX + 8.0f, searchY + (searchH - 14.0f) / 2.0f, 14.0f, 14.0f, context.navigation.searchMode ? Theme.ACCENT_PRIMARY : Theme.TEXT_MUTED);
        if (context.navigation.searchQuery.isEmpty()) {
            nvg.text("Inter-Regular", UiLang.localize("Search settings..."), searchX + 30.0f, searchY + (searchH - 11.0f) / 2.0f, 11.0f, context.navigation.searchMode ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
        } else {
            String display = this.owner.isSearchEditorActiveState() ? this.owner.currentTextBuffer() : context.navigation.searchQuery;
            float textPad = 6.0f;
            float fontSize = 11.0f;
            float visibleTextW = searchW - 30.0f - textPad * 2.0f;
            float textOffset = this.owner.isSearchEditorActiveState() ? this.owner.inlineTextScrollOffsetFor(nvg, "Inter-Regular", display, this.owner.currentTextCursor(), fontSize, visibleTextW) : 0.0f;
            float drawX = searchX + 30.0f + textPad - textOffset;
            nvg.pushScissor(searchX + 30.0f + textPad, searchY + 1.0f, visibleTextW, searchH - 2.0f);
            if (this.owner.isSearchEditorActiveState() && this.owner.textHasSelection()) {
                int selStart = this.owner.currentTextSelectionStart();
                int selEnd = this.owner.currentTextSelectionEnd();
                float selX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, 0, selStart), fontSize);
                float selW = nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, selStart, selEnd), fontSize);
                nvg.roundedRect(selX, searchY + 4.0f, Math.max(1.0f, selW), searchH - 8.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
            }
            nvg.text("Inter-Regular", display, drawX, searchY + (searchH - fontSize) / 2.0f, fontSize, Theme.TEXT_PRIMARY);
            if (this.owner.isSearchEditorActiveState() && System.currentTimeMillis() / 530L % 2L == 0L) {
                float caretX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, 0, this.owner.currentTextCursor()), fontSize);
                nvg.rect(caretX, searchY + (searchH - 14.0f) / 2.0f, 1.0f, 14.0f, Theme.TEXT_PRIMARY);
            }
            nvg.popScissor();
        }
        this.owner.addClickArea(searchX, searchY, searchW, searchH, this.owner::activateSearchField);
        this.owner.offerHoverHelp("search", UiLang.localize("Search settings..."), UiLang.localize("Search all Kuanperion settings by name and jump directly to matching controls."), searchX, searchY, searchW, searchH, mx, my);
        nvg.rect(context.layout.contX, context.layout.contY + 53.0f, context.layout.contW, 1.0f, Theme.SEPARATOR);
    }

    private void renderFarmProfile(NVGRenderer nvg, MainGUIContext context, float textX, float rowY, float mx, float my) {
        this.owner.clearLoginButtonBounds();
        float pillY = rowY + 4.0f;
        nvg.renderSVG("/assets/kuanperion/icons/logo.svg", context.layout.px + 16, pillY + 5, 26, 26, Theme.ACCENT_PRIMARY);
        if (context.animation.sidebarAnim <= 0.01f) return;
        nvg.save();
        nvg.globalAlpha(context.animation.sidebarAnim);
        nvg.text("Inter-Bold", "Kuanperion", textX, pillY + 5, 11, Theme.TEXT_PRIMARY);
        nvg.text("Inter-Regular", "2.0.0 · Minecraft 26.2", textX, pillY + 21, 8, Theme.TEXT_MUTED);
        nvg.restore();
    }

    private void renderPlayedHours(NVGRenderer nvg, float textX, float cy) {
        // Account-related content intentionally not ported.
    }

    private void renderLoginButton(NVGRenderer nvg, float textX, float cy, float mx, float my) {
        this.owner.clearLoginButtonBounds();
    }

    private static String formatPlayedHours(long seconds) {
        return seconds / 3600L + "h " + seconds % 3600L / 60L + "m";
    }

    void renderFilterBar(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        String[] options = this.owner.currentTabFilters();
        int selectedIndex = Math.max(0, Math.min(context.navigation.activeFilter, options.length - 1));
        float barY = context.layout.contY + 53.0f + 1.0f;
        float textY = barY + 12.0f;
        float optionPad = 14.0f;
        float x = context.layout.contX + 20.0f;
        float[] optionX = new float[options.length];
        float[] optionW = new float[options.length];
        for (int i = 0; i < options.length; ++i) {
            float textW = nvg.textWidth("Inter-Regular", options[i], 12.0f);
            optionX[i] = x;
            optionW[i] = textW + optionPad * 2.0f;
            x += optionW[i];
        }
        float targetX = optionX[selectedIndex] + optionPad;
        float targetW = nvg.textWidth("Inter-Regular", options[selectedIndex], 12.0f);
        this.owner.setFilterBarTarget(targetX, targetW);
        long nowNanos = System.nanoTime();
        if (!context.animation.filterBarInited) {
            this.filterPosition.reset(targetX - context.layout.contX, nowNanos);
            this.filterWidth.reset(targetW, nowNanos);
            this.owner.setFilterBarInitialized(true);
        }
        this.owner.setFilterBarAnimation(context.layout.contX + this.filterPosition.update(targetX - context.layout.contX, Theme.ANIM_TIME_MS, nowNanos), this.filterWidth.update(targetW, Theme.ANIM_TIME_MS, nowNanos));
        context = this.owner.context();
        for (int i = 0; i < options.length; ++i) {
            boolean selected = context.navigation.activeFilter == i;
            nvg.text("Inter-Regular", options[i], optionX[i] + optionPad, textY, 12.0f, selected ? Theme.ACCENT_PRIMARY : Theme.TEXT_MUTED);
            int filterIndex = i;
            this.owner.addClickArea(optionX[i], barY, optionW[i], 36.0f, () -> this.owner.onFilterSelected(filterIndex));
        }
        float sepY = barY + 36.0f;
        nvg.rect(context.layout.contX, sepY, context.layout.contW, 1.0f, Theme.SEPARATOR);
        nvg.roundedRect(context.animation.filterBarAnimX, sepY - 1.0f, context.animation.filterBarAnimW, 2.0f, 1.0f, Theme.ACCENT_PRIMARY);
    }
}
