// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIChromeRenderer;
import dev.kuan.kuanperion.ui.MainGUIContext;

final class MainGUIContentRenderer {
    private final MainGUI owner;
    private final MainGUIChromeRenderer chromeRenderer;

    MainGUIContentRenderer(MainGUI owner, MainGUIChromeRenderer chromeRenderer) {
        this.owner = owner;
        this.chromeRenderer = chromeRenderer;
    }

    void renderContent(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        if (context.navigation.activeMain == 0 && context.navigation.activeSubTab != null) {
            this.renderModuleDetailView(nvg, mx, my);
            return;
        }
        this.chromeRenderer.renderContentTopBar(nvg, mx, my);
        if (this.owner.searchResultsVisible()) {
            this.owner.renderSearchContentPanel(nvg, mx, my);
            if (this.owner.hasOpenDropdown()) {
                this.owner.renderDropdownOverlayPanel(nvg, mx, my);
            }
            if (this.owner.hasActiveColorOverlay()) {
                this.owner.renderColorOverlayPanel(nvg);
            }
            return;
        }
        if (this.owner.settingsNavigationHeight() > 0) this.chromeRenderer.renderSettingsNavigation(nvg, mx, my);
        if (context.navigation.activeMain == 0) {
            float scrollTop = this.owner.contentScrollTop();
            float scrollH = this.owner.contentScrollHeight();
            this.owner.renderModuleGridPanel(nvg, mx, my, scrollTop, scrollH);
            return;
        }
        if (context.navigation.activeMain == 1) {
            this.chromeRenderer.renderFilterBar(nvg, mx, my);
            this.owner.renderFlatContentPanel(nvg, mx, my);
            if (this.owner.hasOpenDropdown()) {
                this.owner.renderDropdownOverlayPanel(nvg, mx, my);
            }
            if (this.owner.hasActiveColorOverlay()) {
                this.owner.renderColorOverlayPanel(nvg);
            }
            return;
        }
        if (context.navigation.activeMain == 2) {
            this.chromeRenderer.renderFilterBar(nvg, mx, my);
            this.owner.renderProfileContentPanel(nvg, mx, my);
            return;
        }
        this.owner.renderFlatContentPanel(nvg, mx, my);
        if (this.owner.hasOpenDropdown()) {
            this.owner.renderDropdownOverlayPanel(nvg, mx, my);
        }
        if (this.owner.hasActiveColorOverlay()) {
            this.owner.renderColorOverlayPanel(nvg);
        }
    }

    void renderModuleDetailView(NVGRenderer nvg, float mx, float my) {
        this.owner.renderModuleTopBarPanel(nvg, mx, my);
        this.renderModuleDetailBody(nvg, mx, my);
    }

    void renderModuleDetailBody(NVGRenderer nvg, float mx, float my) {
        MainGUIContext context = this.owner.context();
        float panelTop = context.layout.contY + 53.0f + 1.0f;
        float panelH = context.layout.contH - 53.0f - 1.0f;
        this.owner.renderModuleCategoryPanelSection(nvg, mx, my, panelTop, panelH);
        this.owner.renderModuleSettingsPanelSection(nvg, mx, my, panelTop, panelH);
        if (this.owner.hasOpenDropdown()) {
            this.owner.renderDropdownOverlayPanel(nvg, mx, my);
        }
        if (this.owner.hasActiveColorOverlay()) {
            this.owner.renderColorOverlayPanel(nvg);
        }
    }
}
