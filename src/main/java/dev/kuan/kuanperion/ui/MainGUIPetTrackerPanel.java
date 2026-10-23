// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.DropdownChevron;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;

final class MainGUIPetTrackerPanel {
    private final MainGUI owner;

    MainGUIPetTrackerPanel(MainGUI owner) {
        this.owner = owner;
    }

    void renderGroup(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my) {
        nvg.rect(x, y, w, 1.0f, Theme.BORDER_DEFAULT);
        Fields fields = this.resolveFields(group);
        if (fields == null) {
            return;
        }
        Layout layout = new Layout(x, y, w);
        this.renderCompactTextField(nvg, fields.petName, layout.nameX, layout.row1Y, layout.nameW, MainGUI.petFieldHeight());
        this.renderCompactDropdownField(nvg, fields.maxLevel, layout.maxLevelX, layout.row1Y, layout.quarterW, MainGUI.petFieldHeight());
        this.renderCompactDropdownField(nvg, fields.rarity, layout.rarityX, layout.row1Y, layout.quarterW, MainGUI.petFieldHeight());
        this.renderCompactTextField(nvg, fields.level1Price, layout.level1X, layout.row2Y, layout.halfW, MainGUI.petFieldHeight());
        this.renderCompactTextField(nvg, fields.maxLevelPrice, layout.maxLevelX2, layout.row2Y, layout.halfW, MainGUI.petFieldHeight());
    }

    boolean handleGroupClick(SettingGroup group, float mx, float my, float x, float y, float w) {
        Fields fields = this.resolveFields(group);
        if (fields == null) {
            return false;
        }
        Layout layout = new Layout(x, y, w);
        float fieldY1 = this.compactFieldBodyY(layout.row1Y);
        float fieldY2 = this.compactFieldBodyY(layout.row2Y);
        float fieldH = this.compactFieldBodyH(MainGUI.petFieldHeight());
        if (my >= fieldY1 && my <= fieldY1 + fieldH) {
            if (mx >= layout.nameX && mx <= layout.nameX + layout.nameW) {
                this.handleCompactTextFieldClick(fields.petName, mx, my, layout.nameX, layout.row1Y, layout.nameW, MainGUI.petFieldHeight());
                return true;
            }
            if (mx >= layout.maxLevelX && mx <= layout.maxLevelX + layout.quarterW) {
                this.handleCompactDropdownFieldClick(fields.maxLevel, layout.maxLevelX, layout.row1Y, layout.quarterW, MainGUI.petFieldHeight());
                return true;
            }
            if (mx >= layout.rarityX && mx <= layout.rarityX + layout.quarterW) {
                this.handleCompactDropdownFieldClick(fields.rarity, layout.rarityX, layout.row1Y, layout.quarterW, MainGUI.petFieldHeight());
                return true;
            }
        }
        if (my >= fieldY2 && my <= fieldY2 + fieldH) {
            if (mx >= layout.level1X && mx <= layout.level1X + layout.halfW) {
                this.handleCompactTextFieldClick(fields.level1Price, mx, my, layout.level1X, layout.row2Y, layout.halfW, MainGUI.petFieldHeight());
                return true;
            }
            if (mx >= layout.maxLevelX2 && mx <= layout.maxLevelX2 + layout.halfW) {
                this.handleCompactTextFieldClick(fields.maxLevelPrice, mx, my, layout.maxLevelX2, layout.row2Y, layout.halfW, MainGUI.petFieldHeight());
                return true;
            }
        }
        return false;
    }

    private void handleCompactTextFieldClick(TextSetting setting, float mx, float my, float x, float y, float w, float h) {
        this.owner.commitColor();
        boolean sameActive = this.owner.isActiveTextEditor(setting);
        if (!sameActive) {
            this.owner.commitText();
            this.owner.activateTextSettingEditor(setting);
        }
        float fieldY = this.compactFieldBodyY(y);
        float fieldH = this.compactFieldBodyH(h);
        float textPad = 6.0f;
        float fieldTextSize = 11.0f;
        String value = this.owner.currentTextBuffer();
        float visibleTextW = w - textPad * 2.0f;
        float textOffset = sameActive ? this.owner.inlineTextScrollOffsetApproxFor(value, this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
        float startX = x + textPad - textOffset;
        this.owner.beginInlineFieldCaretPress(x, fieldY, w, fieldH, value, fieldTextSize, startX, mx, my);
    }

    private void handleCompactDropdownFieldClick(DropdownSetting setting, float x, float y, float w, float h) {
        this.owner.commitText();
        this.owner.commitColor();
        float fieldY = this.compactFieldBodyY(y);
        float fieldH = this.compactFieldBodyH(h);
        if (this.owner.isOpenDropdown(setting)) {
            this.owner.closeOpenDropdown();
            return;
        }
        this.owner.openDropdownOverlayFor(setting, x, fieldY + fieldH, w, fieldH);
    }

    private Fields resolveFields(SettingGroup group) {
        TextSetting level1Text;
        DropdownSetting rarityDropdown;
        DropdownSetting maxLevelDropdown;
        TextSetting petNameText;
        Setting maxLevelPrice;
        block3: {
            block2: {
                Setting petName = this.findSetting(group, "Pet Name");
                Setting maxLevel = this.findSetting(group, "Max Level");
                Setting rarity = this.findSetting(group, "Rarity");
                Setting level1Price = this.findSetting(group, "Level 1 Price");
                maxLevelPrice = this.findSetting(group, "Max Level Price");
                if (!(petName instanceof TextSetting)) break block2;
                petNameText = (TextSetting)petName;
                if (!(maxLevel instanceof DropdownSetting)) break block2;
                maxLevelDropdown = (DropdownSetting)maxLevel;
                if (!(rarity instanceof DropdownSetting)) break block2;
                rarityDropdown = (DropdownSetting)rarity;
                if (!(level1Price instanceof TextSetting)) break block2;
                level1Text = (TextSetting)level1Price;
                if (maxLevelPrice instanceof TextSetting) break block3;
            }
            return null;
        }
        TextSetting maxLevelText = (TextSetting)maxLevelPrice;
        return new Fields(petNameText, maxLevelDropdown, rarityDropdown, level1Text, maxLevelText);
    }

    private Setting findSetting(SettingGroup group, String name) {
        for (Setting setting : group.getSettings()) {
            if (!setting.getRawName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    private float compactFieldBodyY(float y) {
        return y + 18.0f;
    }

    private float compactFieldBodyH(float h) {
        return h - 18.0f;
    }

    private void renderCompactTextField(NVGRenderer nvg, TextSetting setting, float x, float y, float w, float h) {
        boolean active = this.owner.isActiveTextEditor(setting);
        nvg.text("Inter-Regular", UiLang.localize(setting.getName()), x, y + 5.0f, 10.0f, Theme.TEXT_SECONDARY);
        float fieldY = this.compactFieldBodyY(y);
        float fieldH = this.compactFieldBodyH(h);
        nvg.roundedRect(x, fieldY, w, fieldH, 4.0f, Theme.BG_FIELD);
        nvg.rectOutline(x, fieldY, w, fieldH, 4.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        if (active) {
            this.owner.setEditorBounds(x, fieldY, w, fieldH);
        }
        float textPad = 6.0f;
        String value = active ? this.owner.currentTextBuffer() : setting.getValue();
        float visibleTextW = w - textPad * 2.0f;
        float textOffset = active ? this.owner.inlineTextScrollOffsetFor(nvg, "Inter-Regular", value, this.owner.currentTextCursor(), 11.0f, visibleTextW) : 0.0f;
        float drawX = x + textPad - textOffset;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, x + textPad, fieldY + 1.0f, visibleTextW, fieldH - 2.0f);
        if (active && this.owner.textHasSelection()) {
            int selectionStart = this.owner.currentTextSelectionStart();
            int selectionEnd = this.owner.currentTextSelectionEnd();
            float selectionX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(value, 0, selectionStart), 11.0f);
            float selectionW = nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(value, selectionStart, selectionEnd), 11.0f);
            nvg.roundedRect(selectionX, fieldY + 4.0f, Math.max(1.0f, selectionW), fieldH - 8.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
        }
        if (value.isEmpty() && !active) {
            nvg.text("Inter-Regular", UiLang.localize(setting.getPlaceholder()), x + textPad, fieldY + 5.0f, 11.0f, Theme.TEXT_DIM);
        } else {
            nvg.text("Inter-Regular", value, drawX, fieldY + 5.0f, 11.0f, Theme.TEXT_PRIMARY);
        }
        if (active && System.currentTimeMillis() / 530L % 2L == 0L) {
            String prefix = this.owner.safeTextSlice(this.owner.currentTextBuffer(), 0, this.owner.currentTextCursor());
            float caretX = drawX + nvg.textWidth("Inter-Regular", prefix, 11.0f);
            nvg.rect(caretX, fieldY + 4.0f, 1.0f, fieldH - 8.0f, Theme.TEXT_PRIMARY);
        }
        this.owner.popContentLocalScissor(nvg, clipped);
    }

    private void renderCompactDropdownField(NVGRenderer nvg, DropdownSetting setting, float x, float y, float w, float h) {
        boolean open = this.owner.isOpenDropdown(setting);
        nvg.text("Inter-Regular", UiLang.localize(setting.getName()), x, y + 5.0f, 10.0f, Theme.TEXT_SECONDARY);
        float fieldY = this.compactFieldBodyY(y);
        float fieldH = this.compactFieldBodyH(h);
        nvg.roundedRect(x, fieldY, w, fieldH, 4.0f, Theme.BG_FIELD);
        nvg.rectOutline(x, fieldY, w, fieldH, 4.0f, 1.0f, open ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        boolean clipped = this.owner.pushContentLocalScissor(nvg, x + 7.0f, fieldY, w - 24.0f, fieldH);
        nvg.text("Inter-Regular", UiLang.localize(setting.getSelectedOption()), x + 7.0f, fieldY + (fieldH - 13.0f) / 2.0f, 12.0f, Theme.TEXT_PRIMARY);
        this.owner.popContentLocalScissor(nvg, clipped);
        DropdownChevron.renderDown(nvg, x + w - 24.0f, fieldY, 24.0f, fieldH, Theme.TEXT_SECONDARY);
        if (open) {
            this.owner.setDropdownOverlayBounds(x, fieldY + fieldH, w, fieldH);
        }
    }

    private record Fields(TextSetting petName, DropdownSetting maxLevel, DropdownSetting rarity, TextSetting level1Price, TextSetting maxLevelPrice) {
    }

    private static final class Layout {
        private final float row1Y;
        private final float row2Y;
        private final float nameW;
        private final float quarterW;
        private final float nameX;
        private final float maxLevelX;
        private final float rarityX;
        private final float halfW;
        private final float level1X;
        private final float maxLevelX2;

        private Layout(float x, float y, float w) {
            float halfGap = MainGUI.petColGap() / 2.0f;
            this.row1Y = y + 6.0f;
            this.row2Y = this.row1Y + MainGUI.petFieldHeight() + MainGUI.petRowGap();
            this.nameW = (w - MainGUI.petColGap() * 2.0f) * 0.5f + halfGap;
            this.quarterW = (w - MainGUI.petColGap() * 2.0f - this.nameW) / 2.0f;
            this.nameX = x;
            this.maxLevelX = this.nameX + this.nameW + MainGUI.petColGap();
            this.rarityX = this.maxLevelX + this.quarterW + MainGUI.petColGap();
            this.halfW = (w - MainGUI.petColGap()) / 2.0f;
            this.level1X = x;
            this.maxLevelX2 = x + this.halfW + MainGUI.petColGap();
        }
    }
}
