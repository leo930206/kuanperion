// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.DropdownChevron;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownListSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.InfoSetting;
import dev.kuan.kuanperion.ui.settings.KeybindSetting;
import dev.kuan.kuanperion.ui.settings.ListSetting;
import dev.kuan.kuanperion.ui.settings.MultiDropdownSetting;
import dev.kuan.kuanperion.ui.settings.PositionSetting;
import dev.kuan.kuanperion.ui.settings.RangeSliderSetting;
import dev.kuan.kuanperion.ui.settings.SectionSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SliderSetting;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import dev.kuan.kuanperion.ui.settings.ToggleSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;
import java.util.List;

final class MainGUISettingRowRenderer {
    private final MainGUI owner;

    MainGUISettingRowRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void render(NVGRenderer nvg, Setting setting, float x, float y, float w, float h, float mx, float my) {
        if (setting instanceof SectionSetting) {
            SectionSetting section = (SectionSetting)setting;
            this.renderSection(nvg, section, x, y, w, h);
            return;
        }
        float cardH = h - 5.0f;
        this.owner.offerHoverHelp("setting:" + setting.getRawName(), setting.getName(), setting.getDescription(), x, y, w, cardH, mx, my);
        nvg.roundedRect(x, y, w, cardH, 7.0f, Theme.CARD_BG);
        nvg.rectOutlineSolid(x, y, w, cardH, 7.0f, 1.0f, Theme.subtleBorder());
        float innerPad = 16.0f;
        float innerX = x + innerPad;
        float innerW = w - innerPad * 2.0f;
        float descriptionH=this.owner.settingDescriptionHeight(setting,w);
        cardH-=descriptionH;
        float midY = y + (h - UiLayoutMetrics.ROW_GAP) / 2.0f;
        List<String> labelLines = this.owner.wrapSettingLabelForRow(nvg, setting, w);
        float labelFontSize = this.owner.settingLabelFontSizeForRow(setting);
        float labelLineStep = this.owner.settingLabelLineStepForRow(setting);
        float labelBlockH = Math.max(0,labelLines.size()-1)*labelLineStep+labelFontSize;
        switch (setting.getType()) {
            case TOGGLE: {
                this.renderToggle(nvg, (ToggleSetting)setting, innerX, innerW, y, cardH, UiLayoutMetrics.checkboxTop(y,h), labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case SLIDER: {
                if(setting instanceof SelectionActionSetting selection)this.renderSelectionAction(nvg,selection,innerX,innerW,y,cardH,h,labelLines,labelFontSize,labelLineStep,labelBlockH);
                else this.renderSlider(nvg, (SliderSetting)setting, innerX, innerW, y, cardH, midY, labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case RANGE_SLIDER: {
                this.renderRangeSlider(nvg, (RangeSliderSetting)setting, innerX, innerW, y, cardH, midY, labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case TEXT: {
                if(setting instanceof InlineToggleSetting combined){
                    this.renderText(nvg,combined,innerX,InlineRowLayout.durationContentWidth(innerW),y,cardH,h,labelLines,labelFontSize,labelLineStep,labelBlockH);
                    this.owner.renderPillControl(nvg,innerX+innerW-38,UiLayoutMetrics.checkboxTop(y,h),combined.isOn(),combined);
                }else this.renderText(nvg, (TextSetting)setting, innerX, innerW, y, cardH, h, labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case INFO: {
                if(setting instanceof ColorPresetSetting preset)ColorPresetRow.render(nvg,preset,innerX,y,innerW,h);
                else if(setting instanceof BlockTargetSetting target)this.renderBlockTarget(nvg,target,innerX,innerW,y,cardH,mx,my);
                else this.renderInfo(nvg, (InfoSetting)setting, innerX, innerW, y, cardH, labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case LIST: {
                this.renderList(nvg, (ListSetting)setting, innerX, innerW, y, labelLines, labelFontSize, labelLineStep, labelBlockH, mx, my);
                break;
            }
            case DROPDOWN_LIST: {
                this.renderDropdownList(nvg, (DropdownListSetting)setting, innerX, innerW, y, labelLines, labelFontSize, labelLineStep, labelBlockH, mx, my);
                break;
            }
            case DROPDOWN: {
                this.renderDropdown(nvg, (DropdownSetting)setting, innerX, innerW, y, cardH, labelLines, labelFontSize, labelLineStep, labelBlockH, mx, my);
                break;
            }
            case MULTI_DROPDOWN: {
                this.renderMultiDropdown(nvg, (MultiDropdownSetting)setting, innerX, innerW, y, cardH, labelLines, labelFontSize, labelLineStep, labelBlockH);
                break;
            }
            case ACTION: {
                this.renderAction(nvg, (ActionSetting)setting, innerX, innerW, y, cardH, mx, my);
                break;
            }
            case COLOR: {
                this.renderColor(nvg, (ColorSetting)setting, innerX, innerW, y, cardH, labelLines, labelFontSize, labelLineStep, labelBlockH, mx, my);
                break;
            }
            case POSITION: {
                this.renderPosition(nvg, (PositionSetting)setting, innerX, innerW, y, cardH, midY, labelLines, labelFontSize, labelLineStep, labelBlockH, mx, my);
                break;
            }
            case KEYBIND: {
                this.renderKeybind(nvg, (KeybindSetting)setting, innerX, innerW, y, cardH, labelLines, labelFontSize, labelLineStep, labelBlockH);
            }
        }
        if(descriptionH>0){
            var lines=this.owner.wrapTextForWidth(nvg,setting.getDescription(),"Inter-Regular",10.5f,this.owner.settingDescriptionWidth(setting,w));
            for(int i=0;i<lines.size();i++)nvg.text("Inter-Regular",lines.get(i),innerX,y+cardH-UiLayoutMetrics.TITLE_TOP+UiLayoutMetrics.descriptionGap()+i*13*MainGUI.uiTextScale,10.5f,Theme.TEXT_MUTED);
        }
    }

    private void renderSection(NVGRenderer nvg, SectionSetting setting, float x, float y, float w, float h) {
        float titleY = y + 7.0f;
        nvg.text("Inter-Bold", setting.getName().toUpperCase(), x + 4.0f, titleY, 10.0f, Theme.ACCENT_PRIMARY);
        if (!setting.getDescription().isBlank()) {
            nvg.text("Inter-Regular", setting.getDescription(), x + 4.0f, titleY + 10.0f + UiLayoutMetrics.descriptionGap(), UiLayoutMetrics.DESCRIPTION_FONT, Theme.TEXT_SECONDARY);
        }
        nvg.rect(x + 4.0f, y + h - 7.0f, Math.max(0.0f, w - 8.0f), 1.0f, Theme.SEPARATOR);
    }

    private void renderSelectionAction(NVGRenderer nvg,SelectionActionSetting setting,float x,float w,float y,float h,float rowHeight,List<String> labels,float font,float lineStep,float labelH) {
        for(int i=0;i<labels.size();i++)nvg.text("Inter-Regular",labels.get(i),x,y+(h-labelH)/2+i*lineStep,font,Theme.TEXT_LABEL);
        float fieldH=UiLayoutMetrics.inputHeight(),fieldX=InlineRowLayout.countFieldX(x,w),fieldY=UiLayoutMetrics.inputTop(y,rowHeight);
        nvg.roundedRect(fieldX,fieldY,60,fieldH,4,Theme.BG_FIELD);
        boolean active=this.owner.isActiveSliderEditor(setting);
        nvg.rectOutline(fieldX,fieldY,60,fieldH,4,1,active?Theme.ACCENT_PRIMARY:Theme.BORDER_DEFAULT);
        if(active)this.owner.setEditorBounds(fieldX,fieldY,60,fieldH);
        nvg.textCentered("Inter-Mono",active?this.owner.currentTextBuffer():Integer.toString(Math.round(setting.getValue())),fieldX,fieldY,60,fieldH,UiLayoutMetrics.inputFont(),Theme.TEXT_VALUE);
        if(active && System.currentTimeMillis()/530%2==0) {
            String value=this.owner.currentTextBuffer();float size=UiLayoutMetrics.inputFont();
            float start=fieldX+(60-nvg.textWidth("Inter-Mono",value,size))/2;
            float caret=start+nvg.textWidth("Inter-Mono",this.owner.safeTextSlice(value,0,this.owner.currentTextCursor()),size);
            nvg.rect(Math.max(fieldX+4,Math.min(fieldX+56,caret)),fieldY+3,1,fieldH-6,Theme.TEXT_PRIMARY);
        }
        float buttonY=UiLayoutMetrics.controlTop(y,rowHeight,24),buttonX=fieldX+72,buttonW=InlineRowLayout.countButtonWidth(w);
        nvg.roundedRect(buttonX,buttonY,buttonW,24,5,Theme.withAlpha(Theme.ACCENT_PRIMARY,.12f));
        nvg.rectOutline(buttonX,buttonY,buttonW,24,5,1,Theme.ACCENT_PRIMARY);
        nvg.textCentered("Inter-Regular","開始選擇目標方塊",buttonX,buttonY,buttonW,24,11.5f,Theme.ACCENT_PRIMARY);
    }

    private void renderBlockTarget(NVGRenderer nvg,BlockTargetSetting setting,float x,float w,float y,float h,float mx,float my) {
        nvg.roundedRect(x,y+(h-36)/2,36,36,6,Theme.BG_FIELD);
        if(!nvg.blockIcon(setting.blockId(),x+4,y+(h-28)/2,28))nvg.renderSVG("/assets/kuanperion/icons/modules.svg",x+6,y+(h-24)/2,24,24,Theme.TEXT_MUTED);
        nvg.text("Inter-Bold",setting.getName(),x+48,y+10,11,Theme.TEXT_MUTED);
        boolean clipped=this.owner.pushContentLocalScissor(nvg,x+48,y+27,Math.max(20,w-184),24);
        nvg.text("Inter-Regular",setting.getValue(),x+48,y+28,13,Theme.TEXT_VALUE);
        this.owner.popContentLocalScissor(nvg,clipped);
        String[] labels={"↑","↓","×"};boolean[] enabled={setting.canMoveUp(),setting.canMoveDown(),true};
        for(int i=0;i<3;i++){
            float bx=x+w-116+i*40,by=y+(h-30)/2;
            nvg.roundedRect(bx,by,36,30,5,Theme.ELEMENT_BG);
            nvg.rectOutline(bx,by,36,30,5,1,Theme.subtleBorder());
            nvg.textCentered("Inter-Regular",labels[i],bx,by,36,30,14,enabled[i]?Theme.TEXT_VALUE:Theme.disabledText());
        }
    }

    private void renderToggle(NVGRenderer nvg, ToggleSetting setting, float x, float w, float y, float cardH, float checkboxY, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        this.owner.renderPillControl(nvg, x + w - 38.0f, checkboxY, setting.getValue(), setting);
    }

    private void renderSlider(NVGRenderer nvg, SliderSetting setting, float x, float w, float y, float cardH, float midY, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float sliderAreaW = w / 3.0f;
        float sliderX = x + w - sliderAreaW;
        float valueBoxW = 66.0f;
        float valueBoxH = UiLayoutMetrics.inputHeight();
        float valueBoxX = sliderX;
        float valueBoxY = midY - valueBoxH / 2.0f;
        nvg.roundedRect(valueBoxX, valueBoxY, valueBoxW, valueBoxH, 7.0f, Theme.ELEMENT_BG);
        boolean active = this.owner.isActiveSliderEditor(setting);
        nvg.rectOutline(valueBoxX, valueBoxY, valueBoxW, valueBoxH, 7.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        if (active) {
            this.owner.setEditorBounds(valueBoxX, valueBoxY, valueBoxW, valueBoxH);
        }
        String valueText = active ? this.owner.currentTextBuffer() : this.owner.formatSliderValue(setting);
        float textPad = 6.0f;
        float fieldTextSize = UiLayoutMetrics.inputFont();
        float valueWidth = nvg.textWidth("Inter-Mono", valueText, fieldTextSize);
        float drawX = valueBoxX + (valueBoxW - valueWidth) / 2.0f;
        float visibleTextW = valueBoxW - textPad * 2.0f;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, valueBoxX + textPad, valueBoxY + 1.0f, visibleTextW, valueBoxH - 2.0f);
        if (active && this.owner.textHasSelection()) {
            int selStart = this.owner.currentTextSelectionStart();
            int selEnd = this.owner.currentTextSelectionEnd();
            float selX = drawX + nvg.textWidth("Inter-Mono", this.owner.safeTextSlice(valueText, 0, selStart), fieldTextSize);
            float selW = nvg.textWidth("Inter-Mono", this.owner.safeTextSlice(valueText, selStart, selEnd), fieldTextSize);
            nvg.roundedRect(selX, valueBoxY + 4.0f, Math.max(1.0f, selW), valueBoxH - 8.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
        }
        if (valueText.isEmpty() && !active) {
            nvg.text("Inter-Mono", "-", valueBoxX + textPad, valueBoxY + (valueBoxH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_VALUE);
        } else {
            nvg.text("Inter-Mono", valueText, drawX, valueBoxY + (valueBoxH - fieldTextSize) / 2.0f, fieldTextSize, active ? Theme.TEXT_PRIMARY : Theme.TEXT_VALUE);
        }
        if (active && System.currentTimeMillis() / 530L % 2L == 0L) {
            String prefix = this.owner.safeTextSlice(valueText, 0, this.owner.currentTextCursor());
            float prefixW = nvg.textWidth("Inter-Mono", prefix, fieldTextSize);
            nvg.rect(drawX + prefixW, valueBoxY + 4.0f, 1.0f, valueBoxH - 8.0f, Theme.TEXT_PRIMARY);
        }
        this.owner.popContentLocalScissor(nvg, clipped);
        float trackX = sliderX + valueBoxW + 14.0f;
        float trackW = sliderAreaW - valueBoxW - 14.0f;
        float fill = Math.max(0.0f, Math.min(1.0f, (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin())));
        nvg.roundedRect(trackX, midY - 2.0f, trackW, 4.0f, 2.0f, Theme.PILL_TRACK);
        if (fill > 0.0f) {
            nvg.horizontalGradient(trackX, midY - 2.0f, trackW * fill, 4.0f, 2.0f, Theme.SLIDER_LEFT, Theme.ACCENT_PRIMARY);
        }
        nvg.circle(trackX + trackW * fill, midY, 7.0f, Theme.ACCENT_PRIMARY);
        nvg.circle(trackX + trackW * fill, midY, 5.0f, 0xFFFFFFFF);
    }

    private void renderRangeSlider(NVGRenderer nvg, RangeSliderSetting setting, float x, float w, float y, float cardH, float midY, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float sliderAreaW = w / 3.0f;
        float sliderX = x + w - sliderAreaW;
        float valueBoxW = 120.0f;
        float valueBoxH = UiLayoutMetrics.inputHeight();
        float valueBoxX = sliderX;
        float valueBoxY = midY - valueBoxH / 2.0f;
        nvg.roundedRect(valueBoxX, valueBoxY, valueBoxW, valueBoxH, 7.0f, Theme.ELEMENT_BG);
        nvg.rectOutline(valueBoxX, valueBoxY, valueBoxW, valueBoxH, 7.0f, 1.0f, Theme.BORDER_DEFAULT);
        String valueText = this.owner.formatRangeSliderValue(setting);
        float fieldTextSize = UiLayoutMetrics.inputFont();
        float valueWidth = nvg.textWidth("Inter-Mono", valueText, fieldTextSize);
        nvg.text("Inter-Mono", valueText, valueBoxX + (valueBoxW - valueWidth) / 2.0f, valueBoxY + (valueBoxH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_VALUE);
        float trackX = sliderX + valueBoxW + 14.0f;
        float trackW = sliderAreaW - valueBoxW - 14.0f;
        float lowerFill = Math.max(0.0f, Math.min(1.0f, (setting.getLowerValue() - setting.getMin()) / (setting.getMax() - setting.getMin())));
        float upperFill = Math.max(0.0f, Math.min(1.0f, (setting.getUpperValue() - setting.getMin()) / (setting.getMax() - setting.getMin())));
        float lowerX = trackX + trackW * lowerFill;
        float upperX = trackX + trackW * upperFill;
        nvg.roundedRect(trackX, midY - 2.0f, trackW, 4.0f, 2.0f, Theme.PILL_TRACK);
        if (upperX > lowerX) {
            nvg.horizontalGradient(lowerX, midY - 2.0f, upperX - lowerX, 4.0f, 2.0f, Theme.SLIDER_LEFT, Theme.ACCENT_PRIMARY);
        }
        nvg.circle(lowerX, midY, 7.0f, Theme.ACCENT_PRIMARY);
        nvg.circle(lowerX, midY, 5.0f, 0xFFFFFFFF);
        nvg.circle(upperX, midY, 7.0f, Theme.ACCENT_PRIMARY);
        nvg.circle(upperX, midY, 5.0f, 0xFFFFFFFF);
    }

    private void renderText(NVGRenderer nvg, TextSetting setting, float x, float w, float y, float cardH, float rowHeight, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        boolean active = this.owner.isActiveTextEditor(setting);
        if (setting.isMultiline()) {
            this.renderMultilineText(nvg, setting, x, w, y, cardH, active, labelLines, labelFontSize, labelLineStep, labelBlockH);
            return;
        }
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float fieldW = this.owner.textInlineFieldWidthForRow(w);
        float fieldX = x + w - fieldW;
        float fieldH = UiLayoutMetrics.inputHeight();
        float fieldY = UiLayoutMetrics.inputTop(y,rowHeight);
        nvg.roundedRect(fieldX, fieldY, fieldW, fieldH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutline(fieldX, fieldY, fieldW, fieldH, 7.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        if (active) {
            this.owner.setEditorBounds(fieldX, fieldY, fieldW, fieldH);
        }
        float textPad = 6.0f;
        float fieldTextSize = UiLayoutMetrics.inputFont();
        String display = active ? this.owner.currentTextBuffer() : setting.getValue();
        float visibleTextW = fieldW - textPad * 2.0f;
        float textOffset = active ? this.owner.inlineTextScrollOffsetFor(nvg, "Inter-Regular", display, this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
        float drawX = fieldX + textPad - textOffset;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, fieldX + textPad, fieldY + 1.0f, visibleTextW, fieldH - 2.0f);
        if (active && this.owner.textHasSelection()) {
            int selStart = this.owner.currentTextSelectionStart();
            int selEnd = this.owner.currentTextSelectionEnd();
            float selX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, 0, selStart), fieldTextSize);
            float selW = nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, selStart, selEnd), fieldTextSize);
            nvg.roundedRect(selX, fieldY + 4.0f, Math.max(1.0f, selW), fieldH - 8.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
        }
        if (display.isEmpty() && (!active || setting instanceof InlineToggleSetting)) {
            nvg.text("Inter-Regular", UiLang.localize(setting.getPlaceholder()), fieldX + textPad, fieldY + (fieldH - fieldTextSize) / 2.0f, fieldTextSize, Theme.disabledText());
        } else {
            nvg.text("Inter-Regular", display, drawX, fieldY + (fieldH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
        }
        if (active && System.currentTimeMillis() / 530L % 2L == 0L) {
            String prefix = this.owner.safeTextSlice(display, 0, this.owner.currentTextCursor());
            float caretX = drawX + nvg.textWidth("Inter-Regular", prefix, fieldTextSize);
            nvg.rect(caretX, fieldY + 3.0f, 1.0f, fieldH - 6.0f, Theme.TEXT_PRIMARY);
        }
        this.owner.popContentLocalScissor(nvg, clipped);
    }

    private void renderInfo(NVGRenderer nvg, InfoSetting setting, float x, float w, float y, float cardH, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + 10.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float valueY = textY + labelBlockH + 6.0f;
        float valueH = Math.max(24.0f, cardH - (valueY - y) - 10.0f);
        nvg.roundedRect(x, valueY, w, valueH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutlineSolid(x, valueY, w, valueH, 7.0f, 1.0f, Theme.subtleBorder());
        String value = setting.getValue();
        float textPad = 8.0f;
        float fontSize = 12.0f;
        float lineStep = fontSize + 4.0f;
        float textMaxWidth = Math.max(1.0f, w - textPad * 2.0f);
        List<String> valueLines = setting.isMultiline() ? this.owner.wrapTextForWidth(nvg, value, "Inter-Regular", fontSize, textMaxWidth) : List.of(value);
        boolean clipped = this.owner.pushContentLocalScissor(nvg, x + textPad, valueY + 3.0f, textMaxWidth, valueH - 6.0f);
        for (int i = 0; i < valueLines.size(); ++i) {
            nvg.text("Inter-Regular", valueLines.get(i), x + textPad, valueY + 6.0f + (float)i * lineStep, fontSize, Theme.TEXT_VALUE);
        }
        this.owner.popContentLocalScissor(nvg, clipped);
    }

    private void renderMultilineText(NVGRenderer nvg, TextSetting setting, float x, float w, float y, float cardH, boolean active, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        int lineIndex;
        float textY = y + 10.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float fieldY = textY + labelBlockH + 6.0f;
        float fieldH = 12.0f + (float)setting.getVisibleLines() * 14.0f - 2.0f;
        nvg.roundedRect(x, fieldY, w, fieldH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutline(x, fieldY, w, fieldH, 7.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        if (active) {
            this.owner.setEditorBounds(x, fieldY, w, fieldH);
        }
        String display = active ? this.owner.currentTextBuffer() : setting.getValue();
        String[] lines = display.split("\n", -1);
        int startLine = active ? this.owner.getMultilineViewportStart(setting, display, this.owner.currentTextCursor()) : 0;
        int maxLines = Math.max(1, setting.getVisibleLines());
        float textPadX = 6.0f;
        float textPadY = 6.0f;
        float fieldTextSize = 12.0f;
        float textMaxWidth = Math.max(1.0f, w - textPadX * 2.0f);
        boolean clipped = this.owner.pushContentLocalScissor(nvg, x + textPadX, fieldY + 1.0f, textMaxWidth, fieldH - 2.0f);
        for (int i = 0; i < maxLines && (lineIndex = startLine + i) < lines.length; ++i) {
            nvg.text("Inter-Regular", lines[lineIndex], x + textPadX, fieldY + textPadY + (float)i * 14.0f, fieldTextSize, Theme.TEXT_PRIMARY);
        }
        if (display.isEmpty() && !active) {
            nvg.text("Inter-Regular", UiLang.localize(setting.getPlaceholder()), x + textPadX, fieldY + textPadY, fieldTextSize, Theme.TEXT_DIM);
        }
        this.owner.popContentLocalScissor(nvg, clipped);
    }

    private void renderList(NVGRenderer nvg, ListSetting setting, float x, float w, float y, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH, float mx, float my) {
        float textY = y + 9.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_PRIMARY);
        }
        List<String> values = setting.getValues();
        int rowCount = Math.max(1, values.size());
        float rowY = textY + labelBlockH + 6.0f;
        float fieldW = w - 48.0f - 12.0f;
        for (int i = 0; i < rowCount; ++i) {
            boolean hasValue = i < values.size();
            boolean active = this.owner.isActiveListEditor(setting, i);
            String display = hasValue ? values.get(i) : "";
            float fieldTextSize = UiLayoutMetrics.inputFont();
            nvg.roundedRect(x, rowY, fieldW, 32.0f, 7.0f, Theme.BG_FIELD);
            nvg.rectOutline(x, rowY, fieldW, 32.0f, 7.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
            if (active) {
                this.owner.setEditorBounds(x, rowY, fieldW, 32.0f);
            }
            float textPad = 6.0f;
            float visibleTextW = fieldW - textPad * 2.0f;
            float textOffset = active ? this.owner.inlineTextScrollOffsetFor(nvg, "Inter-Regular", this.owner.currentTextBuffer(), this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
            float drawX = x + textPad - textOffset;
            boolean clipped = this.owner.pushContentLocalScissor(nvg, x + textPad, rowY + 1.0f, visibleTextW, 30.0f);
            if (active) {
                int selStart = this.owner.currentTextSelectionStart();
                int selEnd = this.owner.currentTextSelectionEnd();
                if (this.owner.textHasSelection()) {
                    float selX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(this.owner.currentTextBuffer(), 0, selStart), fieldTextSize);
                    float selW = nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(this.owner.currentTextBuffer(), selStart, selEnd), fieldTextSize);
                    nvg.roundedRect(selX, rowY + 4.0f, Math.max(1.0f, selW), 24.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
                }
                nvg.text("Inter-Regular", this.owner.currentTextBuffer(), drawX, rowY + (32.0f - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
                if (System.currentTimeMillis() / 530L % 2L == 0L) {
                    String prefix = this.owner.safeTextSlice(this.owner.currentTextBuffer(), 0, this.owner.currentTextCursor());
                    float caretX = drawX + nvg.textWidth("Inter-Regular", prefix, fieldTextSize);
                    nvg.rect(caretX, rowY + 5.0f, 1.0f, 22.0f, Theme.TEXT_PRIMARY);
                }
            } else if (display.isEmpty()) {
                nvg.text("Inter-Regular", UiLang.localize(setting.getPlaceholder()), x + textPad, rowY + (32.0f - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_DIM);
            } else {
                nvg.text("Inter-Regular", display, x + textPad, rowY + (32.0f - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
            }
            this.owner.popContentLocalScissor(nvg, clipped);
            float plusX = x + fieldW + 6.0f;
            float minusX = plusX + 24.0f + 6.0f;
            this.owner.renderListActionButtonControl(nvg, plusX, rowY, 24.0f, "+", mx >= plusX && mx <= plusX + 24.0f && my >= rowY && my <= rowY + 32.0f, true);
            this.owner.renderListActionButtonControl(nvg, minusX, rowY, 24.0f, "-", mx >= minusX && mx <= minusX + 24.0f && my >= rowY && my <= rowY + 32.0f, hasValue);
            rowY += 38.0f;
        }
    }

    private void renderDropdownList(NVGRenderer nvg, DropdownListSetting setting, float x, float w, float y, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH, float mx, float my) {
        float textY = y + 9.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_PRIMARY);
        }
        List<String> values = setting.getValues();
        float rowY = textY + labelBlockH + 6.0f;
        float itemH = 32.0f;
        float gap = 6.0f;
        float actW = 24.0f;
        float fieldTextSize = UiLayoutMetrics.inputFont();
        float textPad = 6.0f;
        float itemFieldW = w - actW * 3.0f - gap * 3.0f;
        for (int i = 0; i < values.size(); ++i) {
            String display = i + 1 + ".  " + values.get(i);
            nvg.roundedRect(x, rowY, itemFieldW, itemH, 7.0f, Theme.BG_FIELD);
            nvg.rectOutline(x, rowY, itemFieldW, itemH, 7.0f, 1.0f, Theme.BORDER_DEFAULT);
            boolean clipped = this.owner.pushContentLocalScissor(nvg, x + textPad, rowY + 1.0f, itemFieldW - textPad * 2.0f, itemH - 2.0f);
            nvg.text("Inter-Regular", display, x + textPad, rowY + (itemH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
            this.owner.popContentLocalScissor(nvg, clipped);
            float upX = x + itemFieldW + gap;
            float downX = upX + actW + gap;
            float minusX = downX + actW + gap;
            this.owner.renderListActionButtonControl(nvg, upX, rowY, actW, "\u2191", mx >= upX && mx <= upX + actW && my >= rowY && my <= rowY + itemH, i > 0);
            this.owner.renderListActionButtonControl(nvg, downX, rowY, actW, "\u2193", mx >= downX && mx <= downX + actW && my >= rowY && my <= rowY + itemH, i < values.size() - 1);
            this.owner.renderListActionButtonControl(nvg, minusX, rowY, actW, "\u2212", mx >= minusX && mx <= minusX + actW && my >= rowY && my <= rowY + itemH, true);
            rowY += itemH + gap;
        }
        DropdownSetting picker = setting.getAddPicker();
        float dropW = w - actW - gap;
        boolean open = this.owner.isOpenDropdown(picker);
        nvg.roundedRect(x, rowY, dropW, itemH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutline(x, rowY, dropW, itemH, 7.0f, 1.0f, open ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        boolean clippedDrop = this.owner.pushContentLocalScissor(nvg, x + textPad, rowY + 1.0f, dropW - 30.0f - textPad, itemH - 2.0f);
        nvg.text("Inter-Regular", picker.getSelectedOption(), x + textPad, rowY + (itemH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
        this.owner.popContentLocalScissor(nvg, clippedDrop);
        nvg.rect(x + dropW - 30.0f, rowY + 6.0f, 1.0f, itemH - 12.0f, Theme.SEPARATOR);
        DropdownChevron.renderDown(nvg, x + dropW - 30.0f, rowY, 30.0f, itemH, Theme.TEXT_MUTED);
        if (open) {
            this.owner.setDropdownOverlayBounds(x, rowY + itemH, dropW, itemH);
        }
        float addX = x + dropW + gap;
        this.owner.renderListActionButtonControl(nvg, addX, rowY, actW, "+", mx >= addX && mx <= addX + actW && my >= rowY && my <= rowY + itemH, true);
    }

    private void renderDropdown(NVGRenderer nvg, DropdownSetting setting, float x, float w, float y, float cardH, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH, float mx, float my) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float boxW = 130.0f;
        float boxH = 32.0f;
        float boxX = x + w - boxW;
        float boxY = y + (cardH - boxH) / 2.0f;
        boolean open = this.owner.isOpenDropdown(setting);
        this.owner.renderDropdownActionButtonsControl(nvg, setting, boxX, boxY, boxH, mx, my);
        nvg.roundedRect(boxX, boxY, boxW, boxH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutline(boxX, boxY, boxW, boxH, 7.0f, 1.0f, open ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        float textPad = 6.0f;
        float fieldTextSize = UiLayoutMetrics.inputFont();
        boolean clipped = this.owner.pushContentLocalScissor(nvg, boxX + textPad, boxY + 1.0f, boxW - 30.0f - textPad, boxH - 2.0f);
        nvg.text("Inter-Regular", setting.getSelectedOption(), boxX + textPad, boxY + (boxH - fieldTextSize) / 2.0f, fieldTextSize, Theme.TEXT_PRIMARY);
        this.owner.popContentLocalScissor(nvg, clipped);
        nvg.rect(boxX + boxW - 30.0f, boxY + 6.0f, 1.0f, boxH - 12.0f, Theme.SEPARATOR);
        DropdownChevron.renderDown(nvg, boxX + boxW - 30.0f, boxY, 30.0f, boxH, Theme.TEXT_MUTED);
        if (open) {
            this.owner.setDropdownOverlayBounds(boxX, boxY + boxH, boxW, boxH);
        }
    }

    private void renderAction(NVGRenderer nvg, ActionSetting setting, float x, float w, float y, float cardH, float mx, float my) {
        float buttonH = 32.0f;
        float buttonY = y + (cardH - buttonH) / 2.0f;
        boolean hovered = mx >= x && mx <= x + w && my >= buttonY && my <= buttonY + buttonH;
        int buttonBg = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
        int buttonBorder = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.subtleBorder();
        int buttonText = hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
        nvg.roundedRect(x, buttonY, w, buttonH, 7.0f, buttonBg);
        nvg.rectOutline(x, buttonY, w, buttonH, 7.0f, 1.0f, buttonBorder);
        nvg.textCentered("Inter-Regular", UiLang.localize(setting.getName()), x, buttonY, w, buttonH, 12.5f, buttonText);
    }

    private void renderColor(NVGRenderer nvg, ColorSetting setting, float x, float w, float y, float cardH, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH, float mx, float my) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float swatchW = 24.0f;
        float swatchH = 24.0f;
        float swatchX = x + w - swatchW;
        float swatchY = y + (cardH - swatchH) / 2.0f;
        String hex = String.format("#%06X", setting.getValue() & 0xFFFFFF);
        float hexW = nvg.textWidth("Inter-Mono", hex, 11.0f);
        nvg.text("Inter-Mono", hex, swatchX - hexW - 7.0f, swatchY + (swatchH - 11.0f) / 2.0f, 11.0f, Theme.TEXT_MUTED);
        boolean active = this.owner.isActiveColorSetting(setting);
        if (mx >= swatchX && mx <= swatchX + swatchW && my >= swatchY && my <= swatchY + swatchH) {
            this.owner.setHoveredColorSetting(setting);
        }
        nvg.roundedRect(swatchX, swatchY, swatchW, swatchH, 5.0f, setting.getValue() | 0xFF000000);
        nvg.rectOutline(swatchX, swatchY, swatchW, swatchH, 5.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.subtleBorder());
    }

    private void renderPosition(NVGRenderer nvg, PositionSetting setting, float x, float w, float y, float cardH, float midY, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH, float mx, float my) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_PRIMARY);
        }
        float boxH = 32.0f;
        float boxY = y + (cardH - boxH) / 2.0f;
        float captureW = 56.0f;
        float actionGap = 6.0f;
        float actionW = 24.0f;
        List<PositionSetting.ActionButton> actions = setting.getActionButtons();
        float actionStripW = actions.isEmpty() ? 0.0f : (float)actions.size() * actionW + (float)actions.size() * actionGap;
        float captureX = x + w - actionStripW - captureW;
        float pillW = 36.0f;
        float pillH = 21.0f;
        float valueGap = 6.0f;
        float valueW = 46.0f;
        float zX = captureX - valueGap - valueW;
        float yX = zX - valueGap - valueW;
        float xX = yX - valueGap - valueW;
        float pillX = xX - valueGap - pillW - 10.0f;
        float highlightW = 52.0f;
        nvg.textRight("Inter-Regular", UiLang.localize("Highlight"), pillX - highlightW - 8.0f, midY - labelFontSize / 2.0f, highlightW, labelFontSize, Theme.TEXT_PRIMARY);
        this.owner.renderPillControl(nvg, pillX, midY - pillH / 2.0f, setting.isHighlighted(), setting);
        float[] boxX = new float[]{xX, yX, zX};
        double[] values = new double[]{setting.getX(), setting.getY(), setting.getZ()};
        for (int i = 0; i < 3; ++i) {
            boolean active = this.owner.isActivePositionEditor(setting, i);
            nvg.roundedRect(boxX[i], boxY, valueW, boxH, 7.0f, Theme.BG_FIELD);
            nvg.rectOutline(boxX[i], boxY, valueW, boxH, 7.0f, 1.0f, active ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
            if (active) {
                this.owner.setEditorBounds(boxX[i], boxY, valueW, boxH);
            }
            String display = active ? this.owner.currentTextBuffer() : String.format("%.1f", values[i]);
            float textPad = 6.0f;
            float fieldTextSize = UiLayoutMetrics.inputFont();
            float visibleTextW = valueW - textPad * 2.0f;
            float textOffset = active ? this.owner.inlineTextScrollOffsetApproxFor(display, this.owner.currentTextCursor(), fieldTextSize, visibleTextW) : 0.0f;
            float drawX = boxX[i] + textPad - textOffset;
            boolean clipped = this.owner.pushContentLocalScissor(nvg, boxX[i] + textPad, boxY + 1.0f, visibleTextW, boxH - 2.0f);
            if (active && this.owner.textHasSelection()) {
                int selStart = this.owner.currentTextSelectionStart();
                int selEnd = this.owner.currentTextSelectionEnd();
                float selX = drawX + nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, 0, selStart), fieldTextSize);
                float selW = nvg.textWidth("Inter-Regular", this.owner.safeTextSlice(display, selStart, selEnd), fieldTextSize);
                nvg.roundedRect(selX, boxY + 4.0f, Math.max(1.0f, selW), boxH - 8.0f, 3.0f, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f));
            }
            nvg.text("Inter-Regular", display, drawX, boxY + (boxH - fieldTextSize) / 2.0f, fieldTextSize, active ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            if (active && System.currentTimeMillis() / 530L % 2L == 0L) {
                String prefix = this.owner.safeTextSlice(display, 0, this.owner.currentTextCursor());
                float offset = nvg.textWidth("Inter-Regular", prefix, fieldTextSize);
                nvg.rect(drawX + offset, boxY + 4.0f, 1.0f, boxH - 8.0f, Theme.TEXT_PRIMARY);
            }
            this.owner.popContentLocalScissor(nvg, clipped);
        }
        boolean hovered = mx >= captureX && mx <= captureX + captureW && my >= boxY && my <= boxY + boxH;
        int buttonBg = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
        int buttonBorder = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.subtleBorder();
        int buttonText = hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
        nvg.roundedRect(captureX, boxY, captureW, boxH, 7.0f, buttonBg);
        nvg.rectOutline(captureX, boxY, captureW, boxH, 7.0f, 1.0f, buttonBorder);
        nvg.textCentered("Inter-Regular", UiLang.localize("Capture"), captureX, boxY, captureW, boxH, 11.5f, buttonText);
        float actionX = captureX + captureW + actionGap;
        for (PositionSetting.ActionButton action : actions) {
            boolean actionHovered = mx >= actionX && mx <= actionX + actionW && my >= boxY && my <= boxY + boxH;
            this.owner.renderListActionButtonControl(nvg, actionX, boxY, actionW, action.label(), actionHovered, action.isEnabled());
            actionX += actionW + actionGap;
        }
    }

    private void renderMultiDropdown(NVGRenderer nvg, MultiDropdownSetting setting, float x, float w, float y, float cardH, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + 12.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float chipH = 28.0f;
        float chipGap = 6.0f;
        float chipFontSz = 12.0f;
        float chipY = textY + labelBlockH + 8.0f;
        List<String> options = setting.getOptions();
        float cx = x;
        for (int i = 0; i < options.size(); ++i) {
            boolean sel;
            String optionText = UiLang.localize(options.get(i));
            float chipWidth = MultiDropdownSetting.chipWidth(optionText);
            if (cx > x && cx + chipWidth > x + w) {
                cx = x;
                chipY += chipH + chipGap;
            }
            int bg = (sel = setting.isSelected(i)) ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
            int border = sel ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.5f) : Theme.BORDER_DEFAULT;
            int textColor = sel ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
            nvg.roundedRect(cx, chipY, chipWidth, chipH, 5.0f, bg);
            nvg.rectOutline(cx, chipY, chipWidth, chipH, 5.0f, 1.0f, border);
            nvg.textCentered("Inter-Regular", optionText, cx, chipY, chipWidth, chipH, chipFontSz, textColor);
            cx += chipWidth + chipGap;
        }
    }

    private void renderKeybind(NVGRenderer nvg, KeybindSetting setting, float x, float w, float y, float cardH, List<String> labelLines, float labelFontSize, float labelLineStep, float labelBlockH) {
        float textY = y + (cardH - labelBlockH) / 2.0f;
        for (int i = 0; i < labelLines.size(); ++i) {
            nvg.text("Inter-Regular", labelLines.get(i), x, textY + (float)i * labelLineStep, labelFontSize, Theme.TEXT_LABEL);
        }
        float buttonH = 32.0f;
        float buttonY = y + (cardH - buttonH) / 2.0f;
        float resetW = 60.0f;
        float bindW = InlineRowLayout.keybindWidth(w);
        float gap = 8.0f;
        float resetX = x + w - resetW;
        float bindX = resetX - gap - bindW;
        boolean listening = this.owner.isAwaitingKeybindCapture(setting);
        int bindBg = listening ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.16f) : Theme.BG_FIELD;
        int bindBorder = listening ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT;
        int bindText = listening ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
        nvg.roundedRect(bindX, buttonY, bindW, buttonH, 7.0f, bindBg);
        nvg.rectOutline(bindX, buttonY, bindW, buttonH, 7.0f, 1.0f, bindBorder);
        String bindTextValue = listening ? "按住修飾鍵，再按主鍵…" : setting.getBoundKeyName();
        float textPad = 8.0f;
        boolean clipped = this.owner.pushContentLocalScissor(nvg, bindX + textPad, buttonY + 1.0f, bindW - textPad * 2.0f, buttonH - 2.0f);
        float bindFont=Math.min(12,Math.max(8,12*(bindW-textPad*2)/Math.max(1,nvg.textWidth("Inter-Regular",bindTextValue,12))));
        nvg.textCentered("Inter-Regular", bindTextValue, bindX + textPad, buttonY, bindW - textPad * 2.0f, buttonH, bindFont, bindText);
        this.owner.popContentLocalScissor(nvg, clipped);
        int resetBg = Theme.ELEMENT_BG;
        int resetBorder = setting.isDefault() ? Theme.BORDER_DEFAULT : Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.28f);
        int resetText = setting.isDefault() ? Theme.TEXT_MUTED : Theme.TEXT_VALUE;
        nvg.roundedRect(resetX, buttonY, resetW, buttonH, 7.0f, resetBg);
        nvg.rectOutline(resetX, buttonY, resetW, buttonH, 7.0f, 1.0f, resetBorder);
        nvg.textCentered("Inter-Regular", UiLang.localize("Reset"), resetX, buttonY, resetW, buttonH, 12.0f, resetText);
    }
}
