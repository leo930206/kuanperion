// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownListSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.KeybindSetting;
import dev.kuan.kuanperion.ui.settings.ListSetting;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.MultiDropdownSetting;
import dev.kuan.kuanperion.ui.settings.PositionSetting;
import dev.kuan.kuanperion.ui.settings.RangeSliderSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.ui.settings.SliderSetting;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import dev.kuan.kuanperion.ui.settings.ToggleSetting;
import java.util.List;

final class MainGUISettingInteractionController {
    private final MainGUI owner;

    MainGUISettingInteractionController(MainGUI owner) {
        this.owner = owner;
    }

    void handleContentAreaClick(float mx, float my) {
        if (mx < this.owner.contentX() || mx > this.owner.contentX() + this.owner.contentW() || my < this.owner.contentY() || my > this.owner.contentY() + this.owner.contentH()) {
            return;
        }
        if (this.owner.searchResultsVisible()) {
            this.owner.handleSearchClickEvent(mx, my);
        } else if (this.owner.getActiveMainTab() == 0 && this.owner.getActiveModuleSubTab() != null) {
            this.handleModuleSettingsPanelClick(mx, my);
        } else if (this.owner.getActiveMainTab() == 1 || this.owner.getActiveMainTab() == 3 || this.owner.getActiveMainTab() == 4) {
            this.handleFlatContentClick(mx, my);
        }
    }

    void handleContentClick(float mx, float my) {
        this.owner.closeOpenDropdown();
        float gx = this.owner.contentX() + 20.0f;
        float gw = this.owner.contentW() - 40.0f;
        float y2 = 0.0f;
        for (SettingGroup group : this.owner.activeGroupsForInteraction()) {
            float headerSY = this.owner.contentY() - this.owner.getActiveScrollY() + (y2 += MainGUI.groupGap());
            if (!group.isAlwaysOn()) {
                float pillX = gx + gw - 52.0f;
                float pillY = headerSY + 12.5f;
                if (mx >= pillX && mx <= pillX + 38.0f && my >= pillY && my <= pillY + 21.0f) {
                    group.toggle();
                    return;
                }
            }
            y2 += 46.0f;
            if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
            y2 += 5.0f;
            if (this.owner.isPetTrackerSettingsGroup(group)) {
                float sh = this.owner.petTrackerGroupHeightValue();
                float sSY = this.owner.contentY() - this.owner.getActiveScrollY() + y2;
                if (sSY + sh > this.owner.contentY() && sSY < this.owner.contentY() + this.owner.contentH() && my >= sSY && my <= sSY + sh && this.owner.handlePetTrackerGroupInteraction(group, mx, my, gx, sSY, gw)) {
                    return;
                }
                y2 += sh;
            } else {
                for (Setting setting : group.getSettings()) {
                    if (!setting.isVisible()) continue;
                    float sh = this.owner.settingHeightFor(setting, gw);
                    float sSY = this.owner.contentY() - this.owner.getActiveScrollY() + y2;
                    if (sSY + sh > this.owner.contentY() && sSY < this.owner.contentY() + this.owner.contentH() && my >= sSY && my <= sSY + sh) {
                        this.handleSettingClick(setting, mx, my, gx, sSY, gw, sh);
                        return;
                    }
                    y2 += sh;
                }
            }
            y2 += 4.0f;
        }
    }

    void handleFlatContentClick(float mx, float my) {
        this.owner.closeOpenDropdown();
        float scrollTop = this.owner.contentScrollTop();
        float gx = this.owner.contentX() + 20.0f;
        float gw = this.owner.contentW() - 40.0f;
        float y2 = 0.0f;
        for (ModulesTab.SubTab subTab : this.owner.flatSubtabsForCurrentFilter()) {
            for (SettingGroup group : subTab.groups()) {
                float labelSY = scrollTop - this.owner.getActiveScrollY() + (y2 += 14.0f);
                if (!group.isAlwaysOn()) {
                    float pillX = gx + gw - 38.0f;
                    float pillY = labelSY + 5.5f;
                    if (mx >= pillX && mx <= pillX + 38.0f && my >= pillY && my <= pillY + 21.0f) {
                        group.toggle();
                        return;
                    }
                }
                y2 += 32.0f;
                if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
                y2 += 5.0f;
                if (this.owner.isPetTrackerSettingsGroup(group)) {
                    float sh = this.owner.petTrackerGroupHeightValue();
                    float sSY = scrollTop - this.owner.getActiveScrollY() + y2;
                    if (my >= sSY && my <= sSY + sh && mx >= gx && mx <= gx + gw && this.owner.handlePetTrackerGroupInteraction(group, mx, my, gx, sSY, gw)) {
                        return;
                    }
                    y2 += sh;
                } else {
                    for (Setting setting : group.getSettings()) {
                        if (!setting.isVisible()) continue;
                        float sh = this.owner.settingHeightFor(setting, gw);
                        float sSY = scrollTop - this.owner.getActiveScrollY() + y2;
                        if (my >= sSY && my <= sSY + sh && mx >= gx && mx <= gx + gw) {
                            this.handleSettingClick(setting, mx, my, gx, sSY, gw, sh);
                            return;
                        }
                        y2 += sh;
                    }
                }
                y2 += 4.0f;
            }
        }
    }

    void handleModuleSettingsPanelClick(float mx, float my) {
        this.owner.closeOpenDropdown();
        if (!this.owner.isActiveModuleConfigurationEditable()) {
            return;
        }
        float panelTop = this.owner.contentY() + 53.0f + 1.0f;
        float rx = this.owner.contentX() + 160.0f + 1.0f;
        float rw = this.owner.contentW() - 160.0f - 1.0f;
        float settTop = panelTop + 74.0f + 1.0f;
        float gx = rx + 20.0f;
        float gw = rw - 40.0f;
        if (mx < rx) {
            return;
        }
        float y2 = 0.0f;
        for (SettingGroup group : this.owner.activeModuleClickGroups()) {
            y2 += 14.0f;
            y2 += 46.0f;
            if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
            y2 += 5.0f;
            if (this.owner.isPetTrackerSettingsGroup(group)) {
                float sh = this.owner.petTrackerGroupHeightValue();
                float sSY = settTop - this.owner.getActiveScrollY() + y2;
                if (my >= sSY && my <= sSY + sh && mx >= gx && mx <= gx + gw && this.owner.handlePetTrackerGroupInteraction(group, mx, my, gx, sSY, gw)) {
                    return;
                }
                y2 += sh;
            } else {
                for (Setting setting : group.getSettings()) {
                    if (!setting.isVisible()) continue;
                    float sh = this.owner.settingHeightFor(setting, gw);
                    float sSY = settTop - this.owner.getActiveScrollY() + y2;
                    float cardH = sh - 5.0f;
                    if (my >= sSY && my <= sSY + cardH && mx >= gx && mx <= gx + gw) {
                        this.handleSettingClick(setting, mx, my, gx, sSY, gw, sh);
                        return;
                    }
                    y2 += sh;
                }
            }
            y2 += 4.0f;
        }
    }

    void handleSettingClick(Setting setting, float mx, float my, float x, float y, float w, float h) {
        if (setting.getType() != SettingType.TEXT && setting.getType() != SettingType.LIST && setting.getType() != SettingType.DROPDOWN_LIST) {
            this.owner.commitText();
        }
        if (setting.getType() != SettingType.COLOR) {
            this.owner.commitColor();
        }
        float ip = 16.0f;
        float ix = x + ip;
        float iw = w - ip * 2.0f;
        float cardH = h - 5.0f - this.owner.settingDescriptionHeight(setting,w);
        // Checkboxes belong to the entire card, not just the title/control region.
        if(my<y || my>y+h-UiLayoutMetrics.ROW_GAP)return;
        if(setting instanceof ToggleSetting toggle){toggle.toggle();return;}
        if(setting instanceof InlineToggleSetting combined && mx>=ix+iw-38){
            this.owner.commitText();combined.toggle();return;
        }
        if(my>y+cardH)return;
        if(setting instanceof InlineToggleSetting combined){
            this.handleTextClick(combined,mx,my,y,ix,InlineRowLayout.durationContentWidth(iw),cardH);
            return;
        }
        if(setting instanceof SelectionActionSetting selection){
            this.owner.commitText();
            float fieldH=UiLayoutMetrics.inputHeight(),fieldX=InlineRowLayout.countFieldX(ix,iw),fieldY=y+(cardH-fieldH)/2,buttonY=y+(cardH-24)/2;
            if(mx>=fieldX+72 && my>=buttonY && my<=buttonY+24)selection.start();
            else if(mx>=fieldX && mx<=fieldX+60 && my>=fieldY && my<=fieldY+fieldH){this.owner.focusSliderEditorField(selection);this.owner.setEditorBounds(fieldX,fieldY,60,fieldH);this.owner.selectAllText();}
            return;
        }
        if(setting instanceof BlockTargetSetting target){
            float startX=ix+iw-116,buttonY=y+(cardH-30)/2;
            if(my>=buttonY&&my<=buttonY+30){int index=(int)((mx-startX)/40);if(mx>=startX && index<3 && mx<=startX+index*40+36){if(index==0)target.moveUp();else if(index==1)target.moveDown();else target.remove();}}
            return;
        }
        switch (setting.getType()) {
            case TOGGLE: {
                ((ToggleSetting)setting).toggle();
                break;
            }
            case SLIDER: {
                this.handleSliderClick((SliderSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case RANGE_SLIDER: {
                this.handleRangeSliderClick((RangeSliderSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case TEXT: {
                this.handleTextClick((TextSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case INFO:
            case SECTION: {
                break;
            }
            case LIST: {
                this.handleListClick((ListSetting)setting, mx, my, y, w, ix, iw);
                break;
            }
            case DROPDOWN_LIST: {
                this.handleDropdownListClick((DropdownListSetting)setting, mx, my, y, w, ix, iw);
                break;
            }
            case DROPDOWN: {
                this.handleDropdownClick((DropdownSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case MULTI_DROPDOWN: {
                this.handleMultiDropdownClick((MultiDropdownSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case ACTION: {
                ((ActionSetting)setting).execute();
                break;
            }
            case COLOR: {
                this.handleColorClick((ColorSetting)setting);
                break;
            }
            case POSITION: {
                this.handlePositionClick((PositionSetting)setting, mx, my, y, ix, iw, cardH);
                break;
            }
            case KEYBIND: {
                this.handleKeybindClick((KeybindSetting)setting, mx, my, y, ix, iw, cardH);
            }
        }
    }

    boolean dragSlider(float mx) {
        if (!this.owner.hasDraggedSliderSetting()) {
            return false;
        }
        this.updateSlider(mx);
        return true;
    }

    boolean dragRangeSlider(float mx) {
        if (!this.owner.hasDraggedRangeSliderSetting()) {
            return false;
        }
        this.updateRangeSlider(mx);
        return true;
    }

    void updateSlider(float mx) {
        this.owner.applySliderDragValue(mx);
    }

    void updateRangeSlider(float mx) {
        this.owner.applyRangeSliderDragValue(mx);
    }

    private void handleSliderClick(SliderSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        float sliderAreaW = iw / 3.0f;
        float sliderX = ix + iw - sliderAreaW;
        float valueBoxW = 66.0f;
        float valueBoxH = UiLayoutMetrics.inputHeight();
        float valueBoxX = sliderX;
        float valueBoxY = y + (cardH - valueBoxH) / 2.0f;
        float trackX = sliderX + valueBoxW + 14.0f;
        float trackW = sliderAreaW - valueBoxW - 14.0f;
        float trackY = y + (cardH - 12.0f) / 2.0f;
        float trackH = 12.0f;
        if (mx >= valueBoxX && mx <= valueBoxX + valueBoxW && my >= valueBoxY && my <= valueBoxY + valueBoxH) {
            boolean sameActive = this.owner.isActiveSliderEditor(setting);
            String value = sameActive ? this.owner.currentTextBuffer() : this.owner.fmtSliderInput(setting);
            float textPad = 6.0f;
            float fieldTextSize = UiLayoutMetrics.inputFont();
            float startX = valueBoxX + (valueBoxW - (float)value.length() * fieldTextSize * 0.52f) / 2.0f;
            this.owner.focusSliderEditorField(setting);
            this.owner.beginInlineFieldCaretPress(valueBoxX, valueBoxY, valueBoxW, valueBoxH, value, fieldTextSize, startX, mx, my);
            return;
        }
        if (mx >= trackX && mx <= trackX + trackW && my >= trackY && my <= trackY + trackH) {
            this.owner.startSliderDrag(setting, trackX, trackW);
            this.updateSlider(mx);
        }
    }

    private void handleRangeSliderClick(RangeSliderSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        float sliderAreaW = iw / 3.0f;
        float sliderX = ix + iw - sliderAreaW;
        float valueBoxW = 120.0f;
        float trackX = sliderX + valueBoxW + 14.0f;
        float trackW = sliderAreaW - valueBoxW - 14.0f;
        float trackY = y + (cardH - 12.0f) / 2.0f;
        float trackH = 12.0f;
        if (mx >= trackX && mx <= trackX + trackW && my >= trackY && my <= trackY + trackH) {
            this.owner.startRangeSliderDrag(setting, trackX, trackW, mx);
            this.updateRangeSlider(mx);
        }
    }

    private void handleTextClick(TextSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        if (setting.isMultiline()) {
            this.owner.commitText();
            this.owner.activateTextSettingEditor(setting);
            float textY = y + 10.0f;
            float labelBlockH = (float)this.owner.wrappedSettingLabelLineCountForRow(setting, iw + 32.0f) * this.owner.settingLabelLineStepForRow(setting);
            float fieldY = textY + labelBlockH + 6.0f;
            float fieldH = 12.0f + (float)setting.getVisibleLines() * 14.0f - 2.0f;
            this.owner.setEditorBounds(ix, fieldY, iw, fieldH);
            this.owner.selectAllText();
            return;
        }
        boolean sameActive = this.owner.isActiveTextEditor(setting);
        if (!sameActive) {
            this.owner.commitText();
            this.owner.activateTextSettingEditor(setting);
        }
        float fieldW = this.owner.textInlineFieldWidthForRow(iw);
        float fieldX = ix + iw - fieldW;
        float fieldH=UiLayoutMetrics.inputHeight();
        float fieldY = y + (cardH - fieldH) / 2.0f;
        float textPad = 6.0f;
        float fieldTextSize = UiLayoutMetrics.inputFont();
        String value = this.owner.currentTextBuffer();
        float visibleTextW = fieldW - textPad * 2.0f;
        float textOffset = sameActive ? this.owner.inlineTextScrollOffsetApproxFor(value, this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
        float startX = fieldX + textPad - textOffset;
        this.owner.beginInlineFieldCaretPress(fieldX, fieldY, fieldW, fieldH, value, fieldTextSize, startX, mx, my);
    }

    private void handleListClick(ListSetting list, float mx, float my, float y, float rowW, float ix, float iw) {
        float textY = y + 9.0f;
        float labelBlockH = (float)this.owner.wrappedSettingLabelLineCountForRow(list, rowW) * this.owner.settingLabelLineStepForRow(list);
        float listY = textY + labelBlockH + 6.0f;
        List<String> values = list.getValues();
        int rowCount = Math.max(1, values.size());
        float fieldW = iw - 48.0f - 12.0f;
        for (int i = 0; i < rowCount; ++i) {
            boolean hasValue = i < values.size();
            boolean sameActive = this.owner.isActiveListEditor(list, i);
            float rowY = listY + (float)i * 38.0f;
            float plusX = ix + fieldW + 6.0f;
            float minusX = plusX + 24.0f + 6.0f;
            if (mx >= plusX && mx <= plusX + 24.0f && my >= rowY && my <= rowY + 32.0f) {
                this.owner.commitText();
                this.owner.insertListEditorItem(list, hasValue ? i + 1 : 0);
                return;
            }
            if (hasValue && mx >= minusX && mx <= minusX + 24.0f && my >= rowY && my <= rowY + 32.0f) {
                this.owner.commitText();
                this.owner.deleteListEditorItem(list, i);
                return;
            }
            if (!(mx >= ix) || !(mx <= ix + fieldW) || !(my >= rowY) || !(my <= rowY + 32.0f)) continue;
            if (hasValue) {
                if (!sameActive) {
                    this.owner.commitText();
                }
                this.owner.focusListEditorItem(list, i);
                float textPad = 6.0f;
                float fieldTextSize = UiLayoutMetrics.inputFont();
                String value = this.owner.currentTextBuffer();
                float visibleTextW = fieldW - textPad * 2.0f;
                float textOffset = sameActive ? this.owner.inlineTextScrollOffsetApproxFor(value, this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
                float startX = ix + textPad - textOffset;
                this.owner.beginInlineFieldCaretPress(ix, rowY, fieldW, 32.0f, value, fieldTextSize, startX, mx, my);
            } else {
                this.owner.commitText();
                this.owner.insertListEditorItem(list, 0);
                this.owner.beginInlineFieldCaretPress(ix, rowY, fieldW, 32.0f, this.owner.currentTextBuffer(), 12.5f, ix + 6.0f, mx, my);
            }
            return;
        }
    }

    private void handleDropdownListClick(DropdownListSetting setting, float mx, float my, float y, float w, float ix, float iw) {
        float textY = y + 9.0f;
        float labelBlockH = (float)this.owner.wrappedSettingLabelLineCountForRow(setting, w) * this.owner.settingLabelLineStepForRow(setting);
        float rowY = textY + labelBlockH + 6.0f;
        float itemH = 32.0f;
        float gap = 6.0f;
        float actW = 24.0f;
        float itemFieldW = iw - actW * 3.0f - gap * 3.0f;
        List<String> values = setting.getValues();
        for (int i = 0; i < values.size(); ++i) {
            float upX = ix + itemFieldW + gap;
            float downX = upX + actW + gap;
            float minusX = downX + actW + gap;
            if (mx >= upX && mx <= upX + actW && my >= rowY && my <= rowY + itemH && i > 0) {
                setting.moveUp(i);
                return;
            }
            if (mx >= downX && mx <= downX + actW && my >= rowY && my <= rowY + itemH && i < values.size() - 1) {
                setting.moveDown(i);
                return;
            }
            if (mx >= minusX && mx <= minusX + actW && my >= rowY && my <= rowY + itemH) {
                setting.removeValue(i);
                return;
            }
            rowY += itemH + gap;
        }
        DropdownSetting picker = setting.getAddPicker();
        float dropW = iw - actW - gap;
        float addX = ix + dropW + gap;
        if (mx >= ix && mx <= ix + dropW && my >= rowY && my <= rowY + itemH) {
            if (this.owner.isOpenDropdown(picker)) {
                this.owner.closeOpenDropdown();
            } else {
                this.owner.openDropdownOverlayFor(picker, ix, rowY + itemH, dropW, itemH);
            }
            return;
        }
        if (mx >= addX && mx <= addX + actW && my >= rowY && my <= rowY + itemH) {
            this.owner.closeOpenDropdown();
            String selected = picker.getSelectedOption();
            if (selected != null && !selected.isBlank()) {
                setting.addValue(selected);
            }
        }
    }

    private void handleDropdownClick(DropdownSetting dropdown, float mx, float my, float y, float ix, float iw, float cardH) {
        float buttonX = ix + iw - 130.0f;
        float buttonH = 32.0f;
        float buttonY = y + (cardH - buttonH) / 2.0f;
        int actionIndex = this.owner.dropdownActionIndexFor(dropdown, buttonX, buttonY, buttonH, mx, my);
        if (actionIndex >= 0) {
            dropdown.getIconActions().get(actionIndex).execute();
            return;
        }
        if (this.owner.isOpenDropdown(dropdown)) {
            this.owner.closeOpenDropdown();
            return;
        }
        this.owner.openDropdownOverlayFor(dropdown, buttonX, buttonY + buttonH, 130.0f, buttonH);
    }

    private void handleMultiDropdownClick(MultiDropdownSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        float chipH = 28.0f;
        float chipGap = 6.0f;
        float labelBlockH = (float)this.owner.wrappedSettingLabelLineCountForRow(setting, iw + 32.0f) * this.owner.settingLabelLineStepForRow(setting);
        float chipY = y + 12.0f + labelBlockH + 8.0f;
        List<String> options = setting.getOptions();
        float cx = ix;
        for (int i = 0; i < options.size(); ++i) {
            float chipWidth = MultiDropdownSetting.chipWidth(options.get(i));
            if (cx > ix && cx + chipWidth > ix + iw) {
                cx = ix;
                chipY += chipH + chipGap;
            }
            if (mx >= cx && mx <= cx + chipWidth && my >= chipY && my <= chipY + chipH) {
                setting.toggleOption(i);
                return;
            }
            cx += chipWidth + chipGap;
        }
    }

    private void handleColorClick(ColorSetting setting) {
        if (this.owner.isActiveColorSetting(setting)) {
            this.owner.commitColor();
            return;
        }
        this.owner.activateColorSetting(setting);
    }

    private void handlePositionClick(PositionSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        float buttonH = 32.0f;
        float buttonY = y + (cardH - buttonH) / 2.0f;
        float captureW = 56.0f;
        float actionGap = 6.0f;
        float actionW = 24.0f;
        List<PositionSetting.ActionButton> actions = setting.getActionButtons();
        float actionStripW = actions.isEmpty() ? 0.0f : (float)actions.size() * actionW + (float)actions.size() * actionGap;
        float captureX = ix + iw - actionStripW - captureW;
        float valueGap = 6.0f;
        float valueW = 46.0f;
        float zX = captureX - valueGap - valueW;
        float yX = zX - valueGap - valueW;
        float xX = yX - valueGap - valueW;
        float pillW = 36.0f;
        float pillX = xX - valueGap - pillW - 10.0f;
        float midY = y + cardH / 2.0f;
        if (mx >= pillX && mx <= pillX + pillW && my >= midY - 10.5f && my <= midY + 10.5f) {
            setting.setHighlighted(!setting.isHighlighted());
            return;
        }
        if (mx >= captureX && mx <= captureX + captureW && my >= buttonY && my <= buttonY + buttonH) {
            setting.capture();
            return;
        }
        float actionX = captureX + captureW + actionGap;
        for (PositionSetting.ActionButton action : actions) {
            if (mx >= actionX && mx <= actionX + actionW && my >= buttonY && my <= buttonY + buttonH) {
                action.execute();
                return;
            }
            actionX += actionW + actionGap;
        }
        float[] boxX = new float[]{xX, yX, zX};
        double[] values = new double[]{setting.getX(), setting.getY(), setting.getZ()};
        for (int i = 0; i < 3; ++i) {
            if (!(mx >= boxX[i]) || !(mx <= boxX[i] + valueW) || !(my >= buttonY) || !(my <= buttonY + buttonH)) continue;
            if (this.owner.isActivePositionEditor(setting, i)) {
                return;
            }
            this.owner.commitText();
            this.owner.focusPositionEditorField(setting, i, values[i], mx, my);
            return;
        }
    }

    private void handleKeybindClick(KeybindSetting setting, float mx, float my, float y, float ix, float iw, float cardH) {
        float buttonH = 32.0f;
        float buttonY = y + (cardH - buttonH) / 2.0f;
        float resetW = 60.0f;
        float bindW = InlineRowLayout.keybindWidth(iw);
        float gap = 8.0f;
        float resetX = ix + iw - resetW;
        float bindX = resetX - gap - bindW;
        if (mx >= resetX && mx <= resetX + resetW && my >= buttonY && my <= buttonY + buttonH) {
            setting.resetToDefault();
            this.owner.clearKeybindCapture();
            return;
        }
        if (mx >= bindX && mx <= bindX + bindW && my >= buttonY && my <= buttonY + buttonH) {
            this.owner.beginKeybindCapture(setting);
        }
    }
}
