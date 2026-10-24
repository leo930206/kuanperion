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
        MainGUIContext context = this.owner.context();
        if (context.animation.computedSidebarExpanded < 1.0f) {
            float maxTW = 0.0f;
            for (var entry : MainGUISidebar.entries()) {
                String name=entry.label();
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
        if(!nvg.modIcon(logoX,logoY,logoSize))nvg.renderSVG("/assets/kuanperion/icons/logo.svg", logoX, logoY, logoSize, logoSize, Theme.ACCENT_PRIMARY);
        if (context.animation.sidebarAnim > 0.01f) {
            nvg.save();
            nvg.globalAlpha(context.animation.sidebarAnim);
            nvg.translate((1.0f - context.animation.sidebarAnim) * -6.0f, 0.0f);
            nvg.text("Inter-Bold", UiLang.localize("Kuanperion"), textX, logoY + (logoSize - 14.0f) / 2.0f, 18.0f, Theme.ACCENT_PRIMARY);
            nvg.restore();
        }
        float sepY = context.layout.py + 56.0f;
        nvg.rect(context.layout.px + 12.0f, sepY, sbW - 24.0f, 1.0f, Theme.SEPARATOR);
        int selectedIndex = MainGUISidebar.selectedIndex(context.navigation.activeMain, context.navigation.activeFilter);
        if (selectedIndex >= 0) {
            float selectedY = MainGUISidebar.rowY(selectedIndex, context.layout.py, context.layout.ph);
            float highlightY = this.sidebarSelection.update(selectedY - context.layout.py, Theme.ANIM_TIME_MS, System.nanoTime());
            nvg.roundedRect(context.layout.px + 12, context.layout.py + highlightY + 4, 36, 36, 8, Theme.withAlpha(Theme.ACCENT_PRIMARY, 85));
        }
        for (int i = 0; i < MainGUISidebar.entries().size(); i++) {
            var entry = MainGUISidebar.entries().get(i);
            float tabY = MainGUISidebar.rowY(i, context.layout.py, context.layout.ph);
            boolean hovered = mx >= context.layout.px && mx < context.layout.px + sbW && my >= tabY && my < tabY + 44;
            int color = selectedIndex == i ? Theme.ACCENT_PRIMARY : hovered ? Theme.TEXT_VALUE : Theme.TEXT_MUTED;
            if (hovered && selectedIndex != i) nvg.roundedRect(context.layout.px + 12, tabY + 4, 36, 36, 8, Theme.withAlpha(Theme.TEXT_MUTED, 51));
            if (i == MainGUISidebar.settingsIndex()) nvg.rect(context.layout.px + 12, tabY - 8, sbW - 24, 1, Theme.SEPARATOR);
            nvg.renderSVG("/assets/kuanperion/icons/" + entry.icon() + ".svg", context.layout.px + 21, tabY + 13, 18, 18, color);
            if (context.animation.sidebarAnim > 0.01f) {
                nvg.save();
                nvg.globalAlpha(context.animation.sidebarAnim);
                nvg.translate((1 - context.animation.sidebarAnim) * -6, 0);
                nvg.text("Inter-Regular", entry.label(), textX, tabY + 16, 12, color);
                nvg.restore();
            }
            this.owner.offerHoverHelp("sidebar:" + entry.label(), entry.label(), entry.description(), context.layout.px, tabY, sbW, 44, mx, my);
        }
        float footerY = MainGUISidebar.footerY(context.layout.py, context.layout.ph);
        this.renderVersionFooter(nvg, context, sbW, footerY, mx, my);
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
        if (context.navigation.searchQuery.isEmpty() && !this.owner.isSearchEditorActiveState()) {
            nvg.text("Inter-Regular", "Search", searchX + 30.0f, searchY + (searchH - 11.0f) / 2.0f, 11.0f, context.navigation.searchMode ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
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
            if (this.owner.isSearchEditorActiveState()) {
                float caretX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, 0, this.owner.currentTextCursor()), fontSize);
                nvg.rect(caretX, searchY + (searchH - 14.0f) / 2.0f, 1.0f, 14.0f, Theme.TEXT_PRIMARY);
            }
            nvg.popScissor();
        }
        this.owner.addClickArea(searchX, searchY, searchW, searchH, this.owner::activateSearchField);
        this.owner.offerHoverHelp("search", "Search", UiLang.localize("Search all Kuanperion settings by name and jump directly to matching controls."), searchX, searchY, searchW, searchH, mx, my);
        nvg.rect(context.layout.contX, context.layout.contY + 53.0f, context.layout.contW, 1.0f, Theme.SEPARATOR);
    }

    private void renderVersionFooter(NVGRenderer nvg, MainGUIContext context, float sidebarWidth, float y, float mx, float my) {
        this.owner.clearLoginButtonBounds();
        String version = MainGUIRegistry.modVersion();
        String fullCaption = MainGUISidebar.footerText(version, true);
        float fullWidth = nvg.textWidth("Inter-Regular", fullCaption, MainGUISidebar.FOOTER_FONT);
        // Keep the short version visible while the full label cannot fit during expansion.
        String caption = MainGUISidebar.footerText(version, context.animation.sidebarAnim > .5f && fullWidth <= sidebarWidth - 24);
        float textWidth = nvg.textWidth("Inter-Regular", caption, MainGUISidebar.FOOTER_FONT);
        nvg.text("Inter-Regular", caption, MainGUISidebar.footerTextX(context.layout.px, sidebarWidth, textWidth), y + (MainGUISidebar.FOOTER_HEIGHT - MainGUISidebar.FOOTER_FONT) / 2, MainGUISidebar.FOOTER_FONT, Theme.TEXT_MUTED);
        this.owner.offerHoverHelp("sidebar:about", UiLang.localize("About"), "點擊版本資訊查看模組資訊", context.layout.px + 12, y, sidebarWidth - 24, MainGUISidebar.FOOTER_HEIGHT, mx, my);
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

    void renderSettingsNavigation(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        float y = context.layout.contY + 54, x = context.layout.contX + 20;
        String[] labels = MainGUISettingsPages.LABELS;
        int[] pages = MainGUISettingsPages.IDS;
        for (int i = 0; i < labels.length; i++) {
            int page = pages[i];
            float w = nvg.textWidth("Inter-Regular", labels[i], 12) + 28;
            boolean selected = context.navigation.activeMain == page;
            nvg.text("Inter-Regular", labels[i], x + 14, y + 12, 12, selected || mx>=x && mx<x+w && my>=y && my<y+36 ? Theme.ACCENT_PRIMARY : Theme.TEXT_MUTED);
            if (selected) nvg.roundedRect(x + 14, y + 35, w - 28, 2, 1, Theme.ACCENT_PRIMARY);
            this.owner.addClickArea(x, y, w, 36, () -> this.owner.selectSettingsPage(page));
            x += w;
        }
        nvg.rect(context.layout.contX, y + 36, context.layout.contW, 1, Theme.SEPARATOR);
    }

    void renderFilterBar(NVGRenderer nvg, float mx, float my) {
        if(!MainGUI.hasTopFilterBar(this.owner.getActiveMainTab()))return;
        MainGUIContext context = this.owner.context();
        String[] options = this.owner.currentTabFilters();
        int selectedIndex = Math.max(0, Math.min(context.navigation.activeFilter, options.length - 1));
        float barY = context.layout.contY + 53.0f + 1.0f + this.owner.settingsNavigationHeight();
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
