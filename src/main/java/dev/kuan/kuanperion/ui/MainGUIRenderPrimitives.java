// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUITextLayout;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;
import java.util.IdentityHashMap;
import java.util.List;

final class MainGUIRenderPrimitives {
    private final MainGUI owner;
    private final MainGUITextLayout textLayout;
    private final IdentityHashMap<Object, Float> pillAnim = new IdentityHashMap();

    MainGUIRenderPrimitives(MainGUI owner, MainGUITextLayout textLayout) {
        this.owner = owner;
        this.textLayout = textLayout;
    }

    void renderSectionHeader(NVGRenderer nvg, String name, float x, float y, float w) {
        name = UiLang.localize(name);
        float textW = nvg.textWidth("Inter-Bold", name, 11.0f);
        nvg.text("Inter-Bold", name, x, y + 7.5f, 11.0f, Theme.TEXT_MUTED);
        float lineX = x + textW + 10.0f;
        float lineY = y + 13.0f;
        nvg.rect(lineX, lineY, w - textW - 10.0f, 1.0f, Theme.SEPARATOR);
    }

    void renderModuleCard(NVGRenderer nvg, ModulesTab.SubTab subTab, float x, float y, float w, float hover, float mx, float my) {
        int i;
        float height = 82.0f;
        float lift = hover * 4.0f;
        boolean enabled = subTab.isEnabled();
        nvg.save();
        nvg.translate(0.0f, -lift);
        if (hover > 0.01f) {
            nvg.shadow(x, y, w, height, 7.0f, 14.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, hover * 0.18f));
        }
        nvg.roundedRect(x, y, w, height, 7.0f, Theme.CARD_BG);
        if (enabled) {
            nvg.roundedRect(x, y, w, height, 7.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.12f));
            nvg.rectOutlineSolid(x, y, w, height, 7.0f, 1.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.4f));
        } else {
            int border = hover > 0.01f ? Theme.blend(Theme.SEPARATOR, Theme.BORDER_HOVER, hover) : Theme.SEPARATOR;
            nvg.rectOutlineSolid(x, y, w, height, 7.0f, 1.0f, border);
            if (hover > 0.01f) {
                nvg.roundedRect(x, y, w, height, 7.0f, Theme.withAlpha(-1, hover * 0.04f));
            }
        }
        float pillX = x + w - 52.0f;
        float pillY = y + (height - 21.0f) / 2.0f;
        if (subTab.hasToggle()) {
            this.renderPill(nvg, pillX, pillY, enabled, (Object)subTab);
        }
        float textX = x + 16.0f;
        int nameColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_VALUE;
        int descriptionColor = enabled ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED;
        float textMaxW = Math.max(80.0f, w - 32.0f - (subTab.hasToggle() ? 62.0f : 0.0f));
        List<String> nameLines = this.textLayout.wrapTextToWidth(nvg, UiLang.localize(subTab.name()), "Inter-Bold", 13.0f, textMaxW);
        List<String> descriptionLines = this.textLayout.wrapTextToWidth(nvg, UiLang.localize(subTab.description()), "Inter-Regular", 10.0f, textMaxW);
        float descriptionY = y + 19 + 13 + UiLayoutMetrics.descriptionGap() + (float)Math.max(0, Math.min(2, nameLines.size()) - 1) * 15;
        for (i = 0; i < Math.min(2, nameLines.size()); ++i) {
            nvg.text("Inter-Bold", nameLines.get(i), textX, y + 19.0f + (float)i * 15.0f, 13.0f, nameColor);
        }
        for (i = 0; i < Math.min(2, descriptionLines.size()); ++i) {
            nvg.text("Inter-Regular", descriptionLines.get(i), textX, descriptionY + (float)i * 12.0f, 10.0f, descriptionColor);
        }
        nvg.restore();
        this.owner.offerHoverHelp("module:" + subTab.name(), UiLang.localize(subTab.name()), UiLang.localize(subTab.description()), x, y, w, height, mx, my);
        if (subTab.hasToggle()) {
            this.owner.addClickArea(pillX - 6.0f, pillY - 6.0f, 50.0f, 33.0f, subTab::toggle);
        }
        this.owner.addClickArea(x, y, w - 56.0f, height, () -> this.owner.openModuleDetailFromCard(subTab));
    }

    void renderListActionButton(NVGRenderer nvg, float x, float y, float w, String label, boolean hovered, boolean enabled) {
        int border;
        int bg;
        bg = !enabled ? Theme.withAlpha(Theme.BG_FIELD, 0.7f) : (hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.16f) : Theme.BG_FIELD);
        border = !enabled ? Theme.withAlpha(Theme.BORDER_DEFAULT, 0.6f) : (hovered ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        int text = !enabled ? Theme.disabledText() : (hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_SECONDARY);
        nvg.roundedRect(x, y, w, 32.0f, 3.0f, bg);
        nvg.rectOutline(x, y, w, 32.0f, 3.0f, 1.0f, border);
        nvg.textCentered("Inter-Bold", label, x, y + 0.5f, w, 32.0f, 12.0f, text);
    }

    void renderPill(NVGRenderer nvg, float x, float y, boolean on, Object key) {
        float t = this.pillAnimValue(key, on);
        float boxX = x + 10.0f;
        float boxY = y + 1.5f;
        nvg.roundedRect(boxX, boxY, 18.0f, 18.0f, 4.0f, Theme.blend(Theme.BG_FIELD, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.18f), t));
        nvg.rectOutline(boxX, boxY, 18.0f, 18.0f, 4.0f, 1.0f, Theme.blend(Theme.BORDER_HOVER, Theme.ACCENT_PRIMARY, t));
        if (t > 0.01f) {
            int color = Theme.withAlpha(Theme.ACCENT_PRIMARY, t);
            nvg.line(boxX + 4.0f, boxY + 9.0f, boxX + 8.0f, boxY + 13.0f, 1.8f, color);
            nvg.line(boxX + 8.0f, boxY + 13.0f, boxX + 14.0f, boxY + 5.0f, 1.8f, color);
        }
    }

    void renderDropdownActionButtons(NVGRenderer nvg, DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        float startX = this.textLayout.dropdownActionStartX(dropdown, fieldX);
        if (startX >= fieldX) {
            return;
        }
        float iconSize = 16.0f;
        for (int i = 0; i < dropdown.getIconActions().size(); ++i) {
            DropdownSetting.IconAction action = dropdown.getIconActions().get(i);
            float actionX = startX + (float)i * 38.0f;
            boolean hovered = mx >= actionX && mx <= actionX + 32.0f && my >= fieldY && my <= fieldY + fieldH;
            int bg = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.12f) : Theme.ELEMENT_BG;
            int border = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.4f) : Theme.BORDER_DEFAULT;
            int iconColor = hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_MUTED;
            nvg.roundedRect(actionX, fieldY, 32.0f, fieldH, 7.0f, bg);
            nvg.rectOutline(actionX, fieldY, 32.0f, fieldH, 7.0f, 1.0f, border);
            nvg.renderSVG(action.iconPath(), actionX + (32.0f - iconSize) / 2.0f, fieldY + (fieldH - iconSize) / 2.0f, iconSize, iconSize, iconColor);
        }
    }

    private float pillAnimValue(Object key, boolean on) {
        float target = on ? 1.0f : 0.0f;
        float current = this.pillAnim.getOrDefault(key, Float.valueOf(target)).floatValue();
        float animationFactor = Theme.animationFactor();
        current += (target - current) * Math.min(1.0f, animationFactor * 3.0f);
        this.pillAnim.put(key, Float.valueOf(current));
        return current;
    }
}
