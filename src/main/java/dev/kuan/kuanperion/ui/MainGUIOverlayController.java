// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import java.util.List;

final class MainGUIOverlayController {
    private final MainGUI owner;

    MainGUIOverlayController(MainGUI owner) {
        this.owner = owner;
    }

    boolean dropdownContains(float mx,float my) {
        var dropdown=this.owner.currentOpenDropdown();if(dropdown==null)return false;
        float height=dropdown.getOptions().size()*30+6;
        float y=this.owner.dropdownOverlayY()+height>this.owner.panelY()+this.owner.panelH()-4
            ?this.owner.dropdownOverlayY()-height-this.owner.dropdownButtonHeight():this.owner.dropdownOverlayY();
        y=Math.max(this.owner.panelY()+4,y)+(1-this.owner.dropdownAnimAmount())*5;
        return mx>=this.owner.dropdownOverlayX()&&mx<=this.owner.dropdownOverlayX()+this.owner.dropdownOverlayW()&&my>=y&&my<=y+height;
    }

    void renderDropdownOverlay(NVGRenderer nvg, float mx, float my) {
        DropdownSetting dropdown = this.owner.currentOpenDropdown();
        if (dropdown == null) {
            return;
        }
        List<String> options = dropdown.getOptions();
        float overlayH = (float)options.size() * 30.0f + 6.0f;
        float overlayY = this.owner.dropdownOverlayY() + overlayH > this.owner.panelY() + this.owner.panelH() - 4.0f ? this.owner.dropdownOverlayY() - overlayH - this.owner.dropdownButtonHeight() : this.owner.dropdownOverlayY();
        overlayY = Math.max(this.owner.panelY() + 4.0f, overlayY);
        float slideOffset = (1.0f - this.owner.dropdownAnimAmount()) * 5.0f;
        nvg.save();
        nvg.globalAlpha(this.owner.dropdownAnimAmount());
        nvg.translate(0.0f, slideOffset);
        nvg.shadow(this.owner.dropdownOverlayX(), overlayY, this.owner.dropdownOverlayW(), overlayH, 7.0f, 16.0f, Theme.withAlpha(-16777216, 0.5f));
        nvg.roundedRect(this.owner.dropdownOverlayX(), overlayY, this.owner.dropdownOverlayW(), overlayH, 7.0f, Theme.CARD_BG);
        nvg.rectOutline(this.owner.dropdownOverlayX(), overlayY, this.owner.dropdownOverlayW(), overlayH, 7.0f, 1.0f, Theme.subtleBorder());
        float itemY = overlayY + 3.0f;
        for (int i = 0; i < options.size(); ++i) {
            boolean hovered;
            boolean selected = dropdown.getSelectedIndex() == i;
            boolean bl = hovered = mx >= this.owner.dropdownOverlayX() && mx <= this.owner.dropdownOverlayX() + this.owner.dropdownOverlayW() && my >= itemY && my <= itemY + 30.0f;
            if (selected) {
                nvg.roundedRect(this.owner.dropdownOverlayX() + 4.0f, itemY, this.owner.dropdownOverlayW() - 8.0f, 30.0f, 5.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f));
            } else if (hovered) {
                nvg.roundedRect(this.owner.dropdownOverlayX() + 4.0f, itemY, this.owner.dropdownOverlayW() - 8.0f, 30.0f, 5.0f, Theme.withAlpha(-1, 0.05f));
            }
            if (selected) {
                float checkX = this.owner.dropdownOverlayX() + this.owner.dropdownOverlayW() - 22.0f;
                float checkY = itemY + 15.0f;
                nvg.line(checkX, checkY, checkX + 3.0f, checkY + 3.0f, 1.5f, Theme.ACCENT_PRIMARY);
                nvg.line(checkX + 3.0f, checkY + 3.0f, checkX + 9.0f, checkY - 4.0f, 1.5f, Theme.ACCENT_PRIMARY);
            }
            nvg.pushScissor(this.owner.dropdownOverlayX() + 14.0f, itemY, this.owner.dropdownOverlayW() - 44.0f, 30.0f);
            nvg.text("Inter-Regular", options.get(i), this.owner.dropdownOverlayX() + 14.0f, itemY + 8.75f, 12.5f, selected ? Theme.ACCENT_PRIMARY : (hovered ? Theme.TEXT_LABEL : Theme.TEXT_VALUE));
            nvg.popScissor();
            itemY += 30.0f;
        }
        nvg.restore();
    }

    void renderColorOverlay(NVGRenderer nvg) {
        float pad = 10.0f;
        float svW = 220.0f;
        float svH = 140.0f;
        float barH = 12.0f;
        float hexH = 26.0f;
        float panelW = 240.0f;
        float panelH = 236.0f;
        float panelX = Math.max(this.owner.panelX() + 4.0f, Math.min(this.owner.panelX() + this.owner.panelW() - panelW - 4.0f, this.owner.panelX() + (this.owner.panelW() - panelW) / 2.0f));
        float panelY = Math.max(this.owner.panelY() + 4.0f, Math.min(this.owner.panelY() + this.owner.panelH() - panelH - 4.0f, this.owner.panelY() + (this.owner.panelH() - panelH) / 2.0f));
        this.owner.setColorPickerPanelBounds(panelX, panelY, panelW, panelH);
        nvg.shadow(panelX, panelY, panelW, panelH, 8.0f, 24.0f, Theme.withAlpha(-16777216, 0.7f));
        nvg.roundedRect(panelX, panelY, panelW, panelH, 8.0f, -15198176);
        nvg.rectOutline(panelX, panelY, panelW, panelH, 8.0f, 1.0f, Theme.ACCENT_PRIMARY);
        float svX = panelX + 10.0f;
        float svY = panelY + 10.0f;
        this.owner.setColorPickerSvBounds(svX, svY, 220.0f, 140.0f);
        int hueColor = MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), 1.0f, 1.0f, 255);
        nvg.horizontalGradient(svX, svY, 220.0f, 140.0f, 4.0f, -1, hueColor);
        nvg.linearGradient(svX, svY, 220.0f, 140.0f, 4.0f, 0, -16777216);
        nvg.rectOutline(svX, svY, 220.0f, 140.0f, 4.0f, 0.5f, Theme.withAlpha(-1, 30));
        float knobX = svX + this.owner.colorSaturation() * 220.0f;
        float knobY = svY + (1.0f - this.owner.colorValue()) * 140.0f;
        nvg.circle(knobX, knobY + 1.0f, 7.0f, Theme.withAlpha(-16777216, 120));
        nvg.circle(knobX, knobY, 6.0f, -1);
        nvg.circle(knobX, knobY, 4.0f, MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), 255));
        float hueBarY = svY + 140.0f + 8.0f;
        this.renderHueBar(nvg, svX, hueBarY, 220.0f, 12.0f);
        float hueIndicator = svX + this.owner.colorHue() * 220.0f;
        nvg.roundedRect(hueIndicator - 2.0f, hueBarY - 2.0f, 4.0f, 16.0f, 2.0f, Theme.withAlpha(-1, 220));
        float alphaBarY = hueBarY + 12.0f + 8.0f;
        int fullColor = MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), 255);
        this.renderAlphaBar(nvg, svX, alphaBarY, 220.0f, 12.0f, fullColor);
        float alphaIndicator = svX + this.owner.colorAlpha() * 220.0f;
        nvg.roundedRect(alphaIndicator - 2.0f, alphaBarY - 2.0f, 4.0f, 16.0f, 2.0f, Theme.withAlpha(-1, 220));
        this.owner.setColorPickerBarBounds(hueBarY, alphaBarY, 12.0f);
        float rowY = alphaBarY + 12.0f + 10.0f;
        float hexW = 176.0f;
        this.owner.setColorPickerHexBounds(svX, rowY, hexW, 26.0f);
        int currentArgb = MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), (int)(this.owner.colorAlpha() * 255.0f));
        nvg.roundedRect(svX, rowY, hexW, 26.0f, 3.0f, Theme.BG_FIELD);
        nvg.rectOutline(svX, rowY, hexW, 26.0f, 3.0f, 1.0f, this.owner.isColorHexFocused() ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        nvg.text("Inter-Regular", "#", svX + 6.0f, rowY + 7.0f, 11.0f, Theme.TEXT_SECONDARY);
        String hexText = this.owner.isColorHexFocused() ? this.owner.colorHexBuffer().toString() : String.format("%06X", currentArgb & 0xFFFFFF);
        nvg.text("Inter-Mono", hexText, svX + 16.0f, rowY + 7.0f, 11.0f, Theme.TEXT_PRIMARY);
        float swatchX = svX + hexW + 8.0f;
        nvg.roundedRect(swatchX, rowY, 36.0f, 26.0f, 3.0f, currentArgb | 0xFF000000);
        nvg.rectOutline(swatchX, rowY, 36.0f, 26.0f, 3.0f, 1.0f, Theme.BORDER_DEFAULT);
    }

    void argbToHsv(int argb) {
        float r = (float)(argb >> 16 & 0xFF) / 255.0f;
        float g = (float)(argb >> 8 & 0xFF) / 255.0f;
        float b = (float)(argb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        this.owner.setColorValue(max);
        this.owner.setColorSaturation(max == 0.0f ? 0.0f : delta / max);
        this.owner.setColorAlpha((float)(argb >> 24 & 0xFF) / 255.0f);
        if (delta == 0.0f) {
            this.owner.setColorHue(0.0f);
            return;
        }
        float hue = max == r ? (g - b) / delta % 6.0f / 6.0f : (max == g ? ((b - r) / delta + 2.0f) / 6.0f : ((r - g) / delta + 4.0f) / 6.0f);
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        this.owner.setColorHue(hue);
    }

    void updateColorFromHsv() {
        ColorSetting activeColor = this.owner.activeColorSetting();
        if (activeColor != null) {
            activeColor.setValue(MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), (int)(this.owner.colorAlpha() * 255.0f)));
        }
    }

    boolean handleColorPickerClick(float mx, float my) {
        if (mx >= this.owner.colorPickerSvX() && mx <= this.owner.colorPickerSvX() + this.owner.colorPickerSvW() && my >= this.owner.colorPickerSvY() && my <= this.owner.colorPickerSvY() + this.owner.colorPickerSvH()) {
            this.owner.setColorSaturation(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
            this.owner.setColorValue(1.0f - Math.max(0.0f, Math.min(1.0f, (my - this.owner.colorPickerSvY()) / this.owner.colorPickerSvH())));
            this.owner.setColorDragMode(1);
            this.updateColorFromHsv();
            return true;
        }
        if (mx >= this.owner.colorPickerSvX() && mx <= this.owner.colorPickerSvX() + this.owner.colorPickerSvW() && my >= this.owner.colorPickerHueBarY() && my <= this.owner.colorPickerHueBarY() + this.owner.colorPickerBarH()) {
            this.owner.setColorHue(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
            this.owner.setColorDragMode(2);
            this.updateColorFromHsv();
            return true;
        }
        if (mx >= this.owner.colorPickerSvX() && mx <= this.owner.colorPickerSvX() + this.owner.colorPickerSvW() && my >= this.owner.colorPickerAlphaBarY() && my <= this.owner.colorPickerAlphaBarY() + this.owner.colorPickerBarH()) {
            this.owner.setColorAlpha(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
            this.owner.setColorDragMode(3);
            this.updateColorFromHsv();
            return true;
        }
        if (mx >= this.owner.colorPickerHexX() && mx <= this.owner.colorPickerHexX() + this.owner.colorPickerHexW() && my >= this.owner.colorPickerHexY() && my <= this.owner.colorPickerHexY() + this.owner.colorPickerHexH()) {
            this.owner.setColorHexFocused(true);
            this.owner.colorHexBuffer().setLength(0);
            this.owner.colorHexBuffer().append(String.format("%06X", MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), 255) & 0xFFFFFF));
            return true;
        }
        if (mx >= this.owner.colorPickerPanelX() && mx <= this.owner.colorPickerPanelX() + this.owner.colorPickerPanelW() && my >= this.owner.colorPickerPanelY() && my <= this.owner.colorPickerPanelY() + this.owner.colorPickerPanelH()) {
            this.owner.setColorHexFocused(false);
            return true;
        }
        return false;
    }

    void handleColorPickerDrag(float mx, float my) {
        switch (this.owner.colorDragMode()) {
            case 1: {
                this.owner.setColorSaturation(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
                this.owner.setColorValue(1.0f - Math.max(0.0f, Math.min(1.0f, (my - this.owner.colorPickerSvY()) / this.owner.colorPickerSvH())));
                break;
            }
            case 2: {
                this.owner.setColorHue(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
                break;
            }
            case 3: {
                this.owner.setColorAlpha(Math.max(0.0f, Math.min(1.0f, (mx - this.owner.colorPickerSvX()) / this.owner.colorPickerSvW())));
                break;
            }
            default: {
                return;
            }
        }
        this.updateColorFromHsv();
    }

    void handleDropdownClick(float mx, float my) {
        int idx;
        DropdownSetting dropdown = this.owner.currentOpenDropdown();
        if (dropdown == null) {
            return;
        }
        List<String> options = dropdown.getOptions();
        float overlayH = (float)options.size() * 30.0f + 4.0f;
        float overlayY = this.owner.dropdownOverlayY() + overlayH > this.owner.panelY() + this.owner.panelH() - 4.0f ? this.owner.dropdownOverlayY() - overlayH - this.owner.dropdownButtonHeight() : this.owner.dropdownOverlayY();
        overlayY = Math.max(this.owner.panelY() + 4.0f, overlayY);
        if (mx >= this.owner.dropdownOverlayX() && mx <= this.owner.dropdownOverlayX() + this.owner.dropdownOverlayW() && my >= overlayY && my <= overlayY + overlayH && (idx = (int)((my - overlayY - 2.0f) / 30.0f)) >= 0 && idx < options.size()) {
            dropdown.setSelectedIndex(idx);
        }
        this.owner.closeOpenDropdown();
    }

    void commitColor() {
        ColorSetting activeColor = this.owner.activeColorSetting();
        if (activeColor == null) {
            return;
        }
        if (this.owner.isColorHexFocused() && this.owner.colorHexBuffer().length() == 6) {
            try {
                int rgb = (int)Long.parseLong(this.owner.colorHexBuffer().toString(), 16);
                this.argbToHsv(0xFF000000 | rgb);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        activeColor.setValue(MainGUIOverlayController.hsvToArgb(this.owner.colorHue(), this.owner.colorSaturation(), this.owner.colorValue(), (int)(this.owner.colorAlpha() * 255.0f)));
        this.owner.clearActiveColorSetting();
        this.owner.setColorHexFocused(false);
        this.owner.setColorDragMode(0);
    }

    boolean handleHexKeyPressed(int key) {
        if (!this.owner.isColorHexFocused() || this.owner.activeColorSetting() == null) {
            return false;
        }
        if (key == 259 && !this.owner.colorHexBuffer().isEmpty()) {
            this.owner.colorHexBuffer().deleteCharAt(this.owner.colorHexBuffer().length() - 1);
        }
        if (key == 257 || key == 335) {
            this.commitColor();
        }
        return true;
    }

    boolean handleHexChar(char ch) {
        if (!this.owner.isColorHexFocused() || this.owner.activeColorSetting() == null || this.owner.colorHexBuffer().length() >= 6 || !MainGUIOverlayController.isHex(ch)) {
            return false;
        }
        this.owner.colorHexBuffer().append(ch);
        if (this.owner.colorHexBuffer().length() == 6) {
            try {
                int rgb = (int)Long.parseLong(this.owner.colorHexBuffer().toString(), 16);
                this.argbToHsv(0xFF000000 | rgb);
                this.updateColorFromHsv();
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return true;
    }

    private void renderHueBar(NVGRenderer nvg, float x, float y, float w, float h) {
        int[] stops = new int[]{-65536, -256, -16711936, -16711681, -16776961, -65281, -65536};
        float segW = w / 6.0f;
        for (int i = 0; i < 6; ++i) {
            nvg.horizontalGradient(x + (float)i * segW, y, segW + 0.5f, h, 0.0f, stops[i], stops[i + 1]);
        }
        nvg.rectOutline(x, y, w, h, 2.0f, 0.5f, Theme.withAlpha(-1, 20));
    }

    private void renderAlphaBar(NVGRenderer nvg, float x, float y, float w, float h, int solidColor) {
        float sq = h / 2.0f;
        for (float bx = x; bx < x + w; bx += sq * 2.0f) {
            float s1 = Math.min(sq, x + w - bx);
            float s2 = Math.min(sq, x + w - (bx + sq));
            if (s1 > 0.0f) {
                nvg.rect(bx, y, s1, sq, -12566464);
            }
            if (s2 > 0.0f) {
                nvg.rect(bx + sq, y + sq, s2, sq, -12566464);
            }
            if (s1 > 0.0f) {
                nvg.rect(bx, y + sq, s1, sq, -10461088);
            }
            if (!(s2 > 0.0f)) continue;
            nvg.rect(bx + sq, y, s2, sq, -10461088);
        }
        nvg.horizontalGradient(x, y, w, h, 2.0f, 0, solidColor);
        nvg.rectOutline(x, y, w, h, 2.0f, 0.5f, Theme.withAlpha(-1, 20));
    }

    private static boolean isHex(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static int hsvToArgb(float h, float s, float v, int alpha) {
        float g;
        float r;
        h = (h % 1.0f + 1.0f) % 1.0f;
        int i = (int)(h * 6.0f);
        float f = h * 6.0f - (float)i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        float b = switch (i % 6) {
            case 0 -> {
                r = v;
                g = t;
                yield p;
            }
            case 1 -> {
                r = q;
                g = v;
                yield p;
            }
            case 2 -> {
                r = p;
                g = v;
                yield t;
            }
            case 3 -> {
                r = p;
                g = q;
                yield v;
            }
            case 4 -> {
                r = t;
                g = p;
                yield v;
            }
            default -> {
                r = v;
                g = p;
                yield q;
            }
        };
        alpha = Math.max(0, Math.min(255, alpha));
        return alpha << 24 | (int)(r * 255.0f) << 16 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f);
    }
}
