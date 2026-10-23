// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.InfoSetting;
import dev.kuan.kuanperion.ui.settings.ListSetting;
import dev.kuan.kuanperion.ui.settings.MultiDropdownSetting;
import dev.kuan.kuanperion.ui.settings.PositionSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.List;

final class MainGUITextLayout {
    MainGUITextLayout() {
    }

    float descriptionWidth(Setting setting,float rowW){return settingLabelMaxWidth(setting,rowW);}
    float descriptionHeight(Setting setting,float rowW) {
        if(!SettingDescriptionCatalog.inlineHelp(setting))return 0;
                int lines=dev.kuan.kuanperion.renderer.NanoVGManager.isInitialized()
            ? wrapTextToWidth(dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer(),setting.getDescription(),"Inter-Regular",10.5f,descriptionWidth(setting,rowW)).size()
            : estimateWrappedLineCount(setting.getDescription(),descriptionWidth(setting,rowW),10.5f*MainGUI.uiTextScale);
        return UiLayoutMetrics.descriptionGap() + 13 * MainGUI.uiTextScale * lines;
    }
    float settingHeight(Setting setting, float rowW) {
        if(setting instanceof ThemePresetsSetting)return ThemePresetRow.height(rowW);
        if(setting instanceof ToggleRowSetting)return 50;
        if(setting instanceof BlockTargetSetting)return 66;
        if(setting instanceof ColorPresetSetting preset)return ColorPresetRow.height(preset,rowW);
        if(SettingDescriptionCatalog.inlineHelp(setting)) {
            float labelHeight=(wrappedSettingLabelLineCount(setting,rowW)-1)*settingLabelLineStep(setting)+settingLabelFontSize(setting);
            return 2*UiLayoutMetrics.TITLE_TOP+labelHeight+descriptionHeight(setting,rowW)+UiLayoutMetrics.ROW_GAP;
        }

        float extraLines = Math.max(0, this.wrappedSettingLabelLineCount(setting, rowW) - 1);
        return descriptionHeight(setting,rowW) + switch (setting.getType()) {
            case SettingType.SECTION -> 46.0f;
            case SettingType.TEXT -> {
                TextSetting textSetting = (TextSetting)setting;
                if (!textSetting.isMultiline()) {
                    yield 50.0f + extraLines * 14.0f;
                }
                float labelBlockH = (float)this.wrappedSettingLabelLineCount(setting, rowW) * this.settingLabelLineStep(setting);
                float fieldH = 12.0f + (float)textSetting.getVisibleLines() * 14.0f - 2.0f;
                yield 26.0f + labelBlockH + fieldH;
            }
            case SettingType.INFO -> {
                float labelBlockH = (float)this.wrappedSettingLabelLineCount(setting, rowW) * this.settingLabelLineStep(setting);
                float valueFontSize = 12.0f;
                float valueLineStep = valueFontSize + 4.0f;
                int valueLines = 1;
                if (((InfoSetting)setting).isMultiline()) {
                    float innerWidth = Math.max(1.0f, rowW - 48.0f);
                    valueLines = this.estimateWrappedLineCount(((InfoSetting)setting).getValue(), innerWidth, valueFontSize);
                }
                yield 21.0f + labelBlockH + Math.max(28.0f, (float)valueLines * valueLineStep + 12.0f);
            }
            case SettingType.LIST -> {
                float labelBlockH = (float)this.wrappedSettingLabelLineCount(setting, rowW) * this.settingLabelLineStep(setting);
                int rowCount = Math.max(1, ((ListSetting)setting).getValues().size());
                yield 9.0f + labelBlockH + 6.0f + (float)rowCount * 32.0f + (float)(rowCount - 1) * 6.0f + 15.0f;
            }
            case SettingType.DROPDOWN_LIST -> {
                float labelBlockH = (float)this.wrappedSettingLabelLineCount(setting, rowW) * this.settingLabelLineStep(setting);
                int rowCount = ((DropdownListSetting)setting).getValues().size() + 1;
                yield 9.0f + labelBlockH + 6.0f + (float)rowCount * 32.0f + (float)(rowCount - 1) * 6.0f + 15.0f;
            }
            case SettingType.ACTION -> 50.0f;
            case SettingType.MULTI_DROPDOWN -> {
                float innerWidth = Math.max(1.0f, rowW - 24.0f);
                float labelBlockH = (float)this.wrappedSettingLabelLineCount(setting, rowW) * this.settingLabelLineStep(setting);
                int rows = ((MultiDropdownSetting)setting).rowCount(innerWidth);
                yield 12.0f + labelBlockH + 8.0f + (float)rows * 28.0f + (float)(rows - 1) * 6.0f + 12.0f;
            }
            default -> 50.0f + extraLines * 14.0f;
        };
    }

    int wrappedSettingLabelLineCount(Setting setting, float rowW) {
        if(dev.kuan.kuanperion.renderer.NanoVGManager.isInitialized())
            return wrapTextToWidth(dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer(),UiLang.localize(setting.getName()),"Inter-Regular",settingLabelFontSize(setting),settingLabelMaxWidth(setting,rowW)).size();
        return this.estimateWrappedLineCount(UiLang.localize(setting.getName()), this.settingLabelMaxWidth(setting, rowW), this.settingLabelFontSize(setting));
    }

    float settingLabelFontSize(Setting setting) {
        return setting.getType() == SettingType.POSITION ? 13.0f : 13.5f;
    }

    float settingLabelLineStep(Setting setting) {
        return this.settingLabelFontSize(setting) + 3.0f;
    }

    float settingLabelMaxWidth(Setting setting, float rowW) {
        float innerWidth = Math.max(0.0f, rowW - 24.0f);
        if(setting instanceof SelectionActionSetting)return Math.max(60,innerWidth-InlineRowLayout.countButtonWidth(innerWidth)-12-60-16);
        if(setting instanceof InlineToggleSetting)return Math.max(60,innerWidth-50-((TextSetting)setting).fieldWidth(innerWidth-50)-12);
        return switch (setting.getType()) {
            case SettingType.TOGGLE -> Math.max(80.0f, innerWidth - 50.0f);
            case SettingType.SLIDER, SettingType.RANGE_SLIDER -> Math.max(80.0f, innerWidth - innerWidth / 3.0f - 12.0f);
            case SettingType.TEXT -> {
                TextSetting textSetting = (TextSetting)setting;
                if (textSetting.isMultiline()) {
                    yield Math.max(80.0f, innerWidth - 16.0f);
                }
                yield Math.max(80.0f, innerWidth - textSetting.fieldWidth(innerWidth) - 12.0f);
            }
            case SettingType.INFO -> Math.max(80.0f, innerWidth - 16.0f);
            case SettingType.LIST, SettingType.DROPDOWN_LIST -> Math.max(80.0f, innerWidth);
            case SettingType.DROPDOWN -> Math.max(80.0f, innerWidth - (130.0f + this.dropdownActionStripWidth((DropdownSetting)setting) + 12.0f));
            case SettingType.MULTI_DROPDOWN -> Math.max(80.0f, innerWidth - 16.0f);
            case SettingType.COLOR -> Math.max(80.0f, innerWidth - 96.0f);
            case SettingType.POSITION -> Math.max(80.0f, innerWidth - (320.0f + this.positionActionStripWidth((PositionSetting)setting)));
            case SettingType.KEYBIND -> Math.max(80.0f, innerWidth - InlineRowLayout.keybindWidth(innerWidth) - 80);
            default -> Math.max(80.0f, innerWidth - 16.0f);
        };
    }

    float dropdownActionStripWidth(DropdownSetting dropdown) {
        int count = dropdown.getIconActions().size();
        if (count == 0) {
            return 0.0f;
        }
        return 6.0f + (float)count * 32.0f + (float)(count - 1) * 6.0f;
    }

    float dropdownActionStartX(DropdownSetting dropdown, float fieldX) {
        return fieldX - this.dropdownActionStripWidth(dropdown);
    }

    private float positionActionStripWidth(PositionSetting setting) {
        int count = setting.getActionButtons().size();
        if (count == 0) {
            return 0.0f;
        }
        return (float)count * 24.0f + (float)count * 6.0f;
    }

    int dropdownActionIndexAt(DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        float startX = this.dropdownActionStartX(dropdown, fieldX);
        if (startX >= fieldX) {
            return -1;
        }
        for (int i = 0; i < dropdown.getIconActions().size(); ++i) {
            float actionX = startX + (float)i * 38.0f;
            if (!(mx >= actionX) || !(mx <= actionX + 32.0f) || !(my >= fieldY) || !(my <= fieldY + fieldH)) continue;
            return i;
        }
        return -1;
    }

    float textInlineFieldWidth(float innerWidth) {
        return Math.min(130.0f, innerWidth);
    }

    float inlineTextScrollOffset(NVGRenderer nvg, String font, String text, int cursor, float fontSize, float visibleWidth) {
        if (text == null || text.isEmpty() || visibleWidth <= 0.0f) {
            return 0.0f;
        }
        float totalWidth = nvg.textWidth(font, text, fontSize);
        if (totalWidth <= visibleWidth) {
            return 0.0f;
        }
        int safeCursor = Math.max(0, Math.min(cursor, text.length()));
        float cursorWidth = nvg.textWidth(font, text.substring(0, safeCursor), fontSize);
        float caretPadding = Math.min(18.0f, visibleWidth * 0.35f);
        float maxOffset = totalWidth - visibleWidth;
        return Math.max(0.0f, Math.min(maxOffset, cursorWidth - (visibleWidth - caretPadding)));
    }

    float inlineTextScrollOffsetApprox(String text, int cursor, float fontSize, float visibleWidth) {
        if (text == null || text.isEmpty() || visibleWidth <= 0.0f) {
            return 0.0f;
        }
        float avgCharWidth = fontSize * 0.52f;
        float totalWidth = (float)text.length() * avgCharWidth;
        if (totalWidth <= visibleWidth) {
            return 0.0f;
        }
        int safeCursor = Math.max(0, Math.min(cursor, text.length()));
        float cursorWidth = (float)safeCursor * avgCharWidth;
        float caretPadding = Math.min(18.0f, visibleWidth * 0.35f);
        float maxOffset = totalWidth - visibleWidth;
        return Math.max(0.0f, Math.min(maxOffset, cursorWidth - (visibleWidth - caretPadding)));
    }

    int estimateWrappedLineCount(String text, float maxWidth, float fontSize) {
        if(text==null || text.isBlank() || maxWidth<=0)return 1;
        // CJK glyphs are approximately full em, not the Latin half-em estimate.
        return UiTextWrap.wrap(text,maxWidth,value->value.codePoints().mapToDouble(cp->
            fontSize*(cp>=0x2E80?1.05:Character.isWhitespace(cp)?.35:.64)).sum()).size();
    }

    List<String> wrapSettingLabel(NVGRenderer nvg, Setting setting, float rowW) {
        return this.wrapTextToWidth(nvg, UiLang.localize(setting.getName()), "Inter-Regular", this.settingLabelFontSize(setting), this.settingLabelMaxWidth(setting, rowW));
    }

    List<String> wrapTextToWidth(NVGRenderer nvg, String text, String fontName, float fontSize, float maxWidth) {
        return UiTextWrap.wrap(text,maxWidth,value->nvg.textWidth(fontName,value,fontSize));
    }

    int fittingPrefixLength(NVGRenderer nvg, String text, String fontName, float fontSize, float maxWidth) {
        int low = 1;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (nvg.textWidth(fontName, text.substring(0, mid), fontSize) <= maxWidth) {
                low = mid;
                continue;
            }
            high = mid - 1;
        }
        return Math.max(1, low);
    }
}
