// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.config.ConfigProfileManager;
import dev.kuan.kuanperion.config.ThemeProfileManager;
import dev.kuan.kuanperion.notification.NotificationManager;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.ui.theme.ThemePreset;
import dev.kuan.kuanperion.util.UiLang;
import java.util.List;
import net.minecraft.client.Minecraft;

final class MainGUIProfilesPanel {
    private final MainGUI owner;

    MainGUIProfilesPanel(MainGUI owner) {
        this.owner = owner;
    }

    void commitRename() {
        boolean renamed;
        if (!this.owner.profileRenameFocus) {
            return;
        }
        boolean isConfig = this.owner.profileRenameTarget == 0;
        String oldName = this.owner.profileRenameOriginal;
        String newName = this.owner.profileRenameInput.trim();
        this.owner.cancelProfileRename();
        if (newName.isBlank()) {
            return;
        }
        boolean bl = renamed = isConfig ? ConfigProfileManager.rename(oldName, newName) : ThemeProfileManager.rename(oldName, newName);
        if (renamed) {
            String title = isConfig ? UiLang.localize("Config Renamed") : UiLang.localize("Theme Renamed");
            NotificationManager.success(title, "\"" + oldName + "\" renamed to \"" + newName + "\"");
        } else {
            String title = isConfig ? UiLang.localize("Config Rename Failed") : UiLang.localize("Theme Rename Failed");
            NotificationManager.error(title, "\"" + newName + "\" could not be used");
        }
    }

    void render(NVGRenderer nvg, float mx, float my) {
        LayoutCursor cursor;
        float scrollTop = this.owner.contentScrollTop();
        float scrollH = this.owner.contentScrollHeight();
        if (!this.owner.suppressNestedContentScissor) {
            nvg.pushScissor(this.owner.contX, scrollTop, this.owner.contW, scrollH);
        }
        float gx = this.owner.contX + 20.0f;
        float gw = this.owner.contW - 40.0f;
        float y = scrollTop - this.owner.scrollY;
        float tot = 0.0f;
        if (this.owner.showConfigProfiles()) {
            cursor = this.renderSection(nvg, mx, my, gx, gw, y += 14.0f, tot += 14.0f, true, UiLang.localize("Config Profiles"));
            y = cursor.y();
            tot = cursor.tot();
        }
        if (this.owner.showThemeProfiles()) {
            cursor = this.renderSection(nvg, mx, my, gx, gw, y += 14.0f, tot += 14.0f, false, UiLang.localize("Theme Profiles"));
            y = cursor.y();
            tot = cursor.tot();
        }
        this.owner.maxScrollY = Math.max(0.0f, tot - scrollH + 16.0f);
        this.owner.targetScrollY = Math.max(0.0f, Math.min(this.owner.maxScrollY, this.owner.targetScrollY));
        if (!this.owner.suppressNestedContentScissor) {
            nvg.popScissor();
        }
        if (this.owner.maxScrollY > 0.0f) {
            float ratio = scrollH / (tot + 16.0f);
            float thumbH = Math.max(30.0f, scrollH * ratio);
            float trackT = scrollTop + 2.0f;
            float trackH = scrollH - 4.0f;
            this.owner.sbMainTrackT = trackT;
            this.owner.sbMainTrackH = trackH;
            this.owner.sbMainThumbH = thumbH;
            float thumbY = MainGUI.scrollbarThumbY(trackT, trackH, thumbH, this.owner.scrollY, this.owner.maxScrollY);
            nvg.roundedRect(this.owner.contX + this.owner.contW - 6.0f, thumbY, 4.0f, thumbH, 2.0f, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.5f));
        }
    }

    private LayoutCursor renderSection(NVGRenderer nvg, float mx, float my, float gx, float gw, float y, float tot, boolean isConfig, String title) {
        List<String> profiles;
        this.owner.renderSectionHeader(nvg, title, gx, y, gw);
        y += 26.0f;
        tot += 26.0f;
        y += 10.0f;
        tot += 10.0f;
        if (!isConfig) {
            float presetsHeight = this.renderThemePresets(nvg, mx, my, gx, gw, y);
            y += presetsHeight;
            tot += presetsHeight;
        }
        nvg.text("Inter-Bold", UiLang.localize("Save Profile"), gx, y, 10.0f, Theme.TEXT_MUTED);
        tot += 16.0f;
        float fieldW = gw - 76.0f;
        float fieldH = 32.0f;
        boolean nameFocused = this.owner.isProfileNameFocused(isConfig);
        nvg.roundedRect(gx, y += 16.0f, fieldW, fieldH, 7.0f, Theme.BG_FIELD);
        nvg.rectOutline(gx, y, fieldW, fieldH, 7.0f, 1.0f, nameFocused ? Theme.ACCENT_PRIMARY : Theme.BORDER_DEFAULT);
        String nameDisp = this.owner.profileNameInput.isEmpty() && !nameFocused ? UiLang.localize("Profile name...") : this.owner.profileNameInput;
        int nameColor = this.owner.profileNameInput.isEmpty() && !nameFocused ? Theme.TEXT_MUTED : Theme.TEXT_LABEL;
        boolean clipProfileName = this.owner.pushContentLocalScissor(nvg, gx + 10.0f, y + 1.0f, fieldW - 20.0f, fieldH - 2.0f);
        nvg.text("Inter-Regular", nameDisp, gx + 10.0f, y + (fieldH - 12.0f) / 2.0f, 12.0f, nameColor);
        this.owner.popContentLocalScissor(nvg, clipProfileName);
        if (nameFocused && System.currentTimeMillis() / 530L % 2L == 0L) {
            float cursorX = gx + 10.0f + nvg.textWidth("Inter-Regular", this.owner.profileNameInput, 12.0f);
            nvg.rect(cursorX, y + 8.0f, 1.0f, fieldH - 16.0f, Theme.TEXT_LABEL);
        }
        int target = isConfig ? 0 : 1;
        float fieldY = y;
        this.owner.addClickArea(gx, y, fieldW, fieldH, () -> {
            this.owner.commitText();
            this.owner.commitColor();
            this.owner.openDd = null;
            this.owner.profileNameTarget = target;
            this.owner.profileNameFocus = true;
        });
        float saveBtnX = gx + fieldW + 8.0f;
        float saveBtnW = 60.0f;
        boolean saveHov = mx >= saveBtnX && mx <= saveBtnX + saveBtnW && my >= fieldY && my <= fieldY + fieldH;
        int saveBg = saveHov ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
        int saveBrd = saveHov ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.withAlpha(-1, 0.06f);
        int saveTxt = saveHov ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
        nvg.roundedRect(saveBtnX, y, saveBtnW, fieldH, 7.0f, saveBg);
        nvg.rectOutline(saveBtnX, y, saveBtnW, fieldH, 7.0f, 1.0f, saveBrd);
        nvg.textCentered("Inter-Regular", UiLang.localize("Save"), saveBtnX, y, saveBtnW, fieldH, 12.5f, saveTxt);
        this.owner.offerHoverHelp("profile-save:" + (isConfig ? "config" : "theme"), UiLang.localize("Save"), UiLang.localize(isConfig ? "Saves the current client configuration as a named profile." : "Saves the current menu and HUD theme as a named profile."), saveBtnX, y, saveBtnW, fieldH, mx, my);
        String capturedName = this.owner.profileNameInput;
        this.owner.addClickArea(saveBtnX, y, saveBtnW, fieldH, () -> {
            if (!capturedName.isBlank()) {
                if (isConfig) {
                    ConfigProfileManager.save(capturedName);
                    NotificationManager.success(UiLang.localize("Config Saved"), "\"" + capturedName + "\" saved successfully");
                } else {
                    ThemeProfileManager.save(capturedName);
                    NotificationManager.success(UiLang.localize("Theme Saved"), "\"" + capturedName + "\" saved successfully");
                }
                this.owner.profileNameInput = "";
                this.owner.profileNameFocus = false;
            }
        });
        tot += fieldH + 18.0f;
        nvg.text("Inter-Bold", UiLang.localize("Saved Profiles"), gx, y += fieldH + 18.0f, 10.0f, Theme.TEXT_MUTED);
        y += 14.0f;
        tot += 14.0f;
        List<String> list = profiles = isConfig ? ConfigProfileManager.list() : ThemeProfileManager.list();
        if (profiles.isEmpty()) {
            float emptyH = 42.0f;
            nvg.roundedRect(gx, y, gw, emptyH, 7.0f, Theme.CARD_BG);
            nvg.rectOutline(gx, y, gw, emptyH, 7.0f, 1.0f, Theme.withAlpha(-1, 0.06f));
            nvg.textCentered("Inter-Regular", UiLang.localize("No saved profiles yet."), gx, y, gw, emptyH, 11.0f, Theme.TEXT_MUTED);
            y += emptyH + 4.0f;
            tot += emptyH + 4.0f;
        } else {
            float rowH = 44.0f;
            float btnW = 60.0f;
            float btnH = 26.0f;
            float btnGap = 5.0f;
            for (String name : profiles) {
                float rowY = y;
                nvg.roundedRect(gx, rowY, gw, rowH, 7.0f, Theme.CARD_BG);
                nvg.rectOutline(gx, rowY, gw, rowH, 7.0f, 1.0f, Theme.withAlpha(-1, 0.06f));
                float btnX = gx + gw - 3.0f * (btnW + btnGap) - btnGap;
                float btnY = rowY + (rowH - btnH) / 2.0f;
                String profileName = name;
                float nameX = gx + 10.0f;
                float nameY = rowY + 7.0f;
                float nameW = Math.max(40.0f, btnX - nameX - 10.0f);
                float nameH = rowH - 14.0f;
                boolean renaming = this.owner.isProfileRenameFocused(isConfig, profileName);
                if (renaming) {
                    nvg.roundedRect(nameX, nameY, nameW, nameH, 5.0f, Theme.BG_FIELD);
                    nvg.rectOutline(nameX, nameY, nameW, nameH, 5.0f, 1.0f, Theme.ACCENT_PRIMARY);
                    boolean clipRename = this.owner.pushContentLocalScissor(nvg, nameX + 8.0f, nameY, nameW - 16.0f, nameH);
                    nvg.text("Inter-Regular", this.owner.profileRenameInput, nameX + 8.0f, rowY + (rowH - 12.0f) / 2.0f, 12.0f, Theme.TEXT_LABEL);
                    this.owner.popContentLocalScissor(nvg, clipRename);
                    if (System.currentTimeMillis() / 530L % 2L == 0L) {
                        float cursorX = nameX + 8.0f + nvg.textWidth("Inter-Regular", this.owner.profileRenameInput, 12.0f);
                        nvg.rect(Math.min(cursorX, nameX + nameW - 8.0f), nameY + 7.0f, 1.0f, nameH - 14.0f, Theme.TEXT_LABEL);
                    }
                } else {
                    boolean nameHovered = mx >= nameX && mx <= nameX + nameW && my >= nameY && my <= nameY + nameH;
                    int rowNameColor = nameHovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_LABEL;
                    boolean clipName = this.owner.pushContentLocalScissor(nvg, nameX + 4.0f, rowY, nameW - 8.0f, rowH);
                    nvg.text("Inter-Regular", name, nameX + 4.0f, rowY + (rowH - 12.0f) / 2.0f, 12.0f, rowNameColor);
                    this.owner.popContentLocalScissor(nvg, clipName);
                }
                this.owner.addClickArea(nameX, nameY, nameW, nameH, () -> this.owner.beginProfileRename(isConfig, profileName));
                int index = 0;
                while (index < 3) {
                    int borderColor;
                    int bgColor;
                    boolean isDelete;
                    float actionX = btnX + (float)index * (btnW + btnGap);
                    boolean hovered = mx >= actionX && mx <= actionX + btnW && my >= btnY && my <= btnY + btnH;
                    String label = switch (index) {
                        case 0 -> UiLang.localize("Load");
                        case 1 -> UiLang.localize("Export");
                        default -> UiLang.localize("Delete");
                    };
                    boolean bl = isDelete = index == 2;
                    bgColor = isDelete ? (hovered ? Theme.withAlpha(-3394765, 0.2f) : Theme.ELEMENT_BG) : (hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG);
                    borderColor = isDelete ? (hovered ? Theme.withAlpha(-3394765, 0.45f) : Theme.withAlpha(-1, 0.06f)) : (hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.withAlpha(-1, 0.06f));
                    int textColor = isDelete ? (hovered ? -30584 : Theme.TEXT_VALUE) : (hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE);
                    nvg.roundedRect(actionX, btnY, btnW, btnH, 5.0f, bgColor);
                    nvg.rectOutline(actionX, btnY, btnW, btnH, 5.0f, 1.0f, borderColor);
                    nvg.textCentered("Inter-Regular", label, actionX, btnY, btnW, btnH, 11.5f, textColor);
                    String help = switch (index) {
                        case 0 -> {
                            if (isConfig) {
                                yield "Loads this saved configuration profile.";
                            }
                            yield "Loads this saved theme profile.";
                        }
                        case 1 -> "Copies this profile as JSON so it can be backed up or shared.";
                        default -> "Deletes this saved profile.";
                    };
                    this.owner.offerHoverHelp("profile-action:" + (isConfig ? "config:" : "theme:") + profileName + ":" + index, label, UiLang.localize(help), actionX, btnY, btnW, btnH, mx, my);
                    int action = index++;
                    this.owner.addClickArea(actionX, btnY, btnW, btnH, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        switch (action) {
                            case 0: {
                                if (isConfig) {
                                    if (ConfigProfileManager.load(profileName)) {
                                        this.owner.refreshContext();
                                        NotificationManager.success(UiLang.localize("Config Loaded"), "\"" + profileName + "\" loaded successfully");
                                        break;
                                    }
                                    NotificationManager.error(UiLang.localize("Config Load Failed"), "\"" + profileName + "\" could not be loaded");
                                    break;
                                }
                                ThemeProfileManager.load(profileName);
                                NotificationManager.success(UiLang.localize("Theme Loaded"), "\"" + profileName + "\" loaded successfully");
                                break;
                            }
                            case 1: {
                                String json = isConfig ? ConfigProfileManager.exportJsonSanitized(profileName) : ThemeProfileManager.exportJson(profileName);
                                mc.keyboardHandler.setClipboard(json);
                                NotificationManager.info(UiLang.localize("Copied to Clipboard"), "\"" + profileName + "\" exported");
                                break;
                            }
                            case 2: {
                                if (isConfig) {
                                    ConfigProfileManager.delete(profileName);
                                    NotificationManager.warning(UiLang.localize("Config Deleted"), "\"" + profileName + "\" was deleted");
                                    break;
                                }
                                ThemeProfileManager.delete(profileName);
                                NotificationManager.warning(UiLang.localize("Theme Deleted"), "\"" + profileName + "\" was deleted");
                            }
                        }
                    });
                }
                y += rowH + 5.0f;
                tot += rowH + 5.0f;
            }
        }
        tot += 10.0f;
        float importW = gw;
        float importH = 32.0f;
        boolean importHovered = mx >= gx && mx <= gx + importW && my >= (y += 10.0f) && my <= y + importH;
        int importBg = importHovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
        int importBrd = importHovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.withAlpha(-1, 0.06f);
        int importTxt = importHovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
        nvg.roundedRect(gx, y, importW, importH, 7.0f, importBg);
        nvg.rectOutline(gx, y, importW, importH, 7.0f, 1.0f, importBrd);
        nvg.textCentered("Inter-Regular", UiLang.localize("Import from Clipboard"), gx, y, importW, importH, 12.5f, importTxt);
        this.owner.addClickArea(gx, y, importW, importH, () -> {
            String json = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (json != null && !json.isBlank()) {
                String importName;
                String string = importName = this.owner.profileNameInput.isBlank() ? "imported" : this.owner.profileNameInput;
                if (isConfig) {
                    ConfigProfileManager.importFromClipboard(importName, json);
                } else {
                    ThemeProfileManager.importJson(importName, json);
                }
            }
        });
        return new LayoutCursor(y += importH + 16.0f, tot += importH + 16.0f);
    }

    private float renderThemePresets(NVGRenderer nvg, float mx, float my, float gx, float gw, float y) {
        nvg.text("Inter-Bold", UiLang.localize("Colour Presets"), gx, y, 10.0f, Theme.TEXT_MUTED);
        ThemePreset[] presets = ThemePreset.values();
        float gap = 8.0f;
        float buttonH = 32.0f;
        float minButtonW = nvg.textWidth("Inter-Regular", UiLang.localize("Default Colours"), 12.5f) + 24.0f;
        for (ThemePreset preset : presets) {
            minButtonW = Math.max(minButtonW, nvg.textWidth("Inter-Regular", UiLang.localize(preset.label()), 12.5f) + 24.0f);
        }
        int columns = Math.clamp((long)((int)((gw + gap) / (minButtonW + gap))), (int)1, (int)(presets.length + 1));
        float buttonW = (gw - gap * (float)(columns - 1)) / (float)columns;
        for (int i = 0; i <= presets.length; ++i) {
            ThemePreset preset;
            preset = i < presets.length ? presets[i] : null;
            String label = UiLang.localize(preset == null ? "Default Colours" : preset.label());
            float buttonX = gx + (float)(i % columns) * (buttonW + gap);
            float buttonY = y + 16.0f + (float)(i / columns) * (buttonH + gap);
            boolean hovered = mx >= buttonX && mx <= buttonX + buttonW && my >= buttonY && my <= buttonY + buttonH;
            int bg = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.15f) : Theme.ELEMENT_BG;
            int border = hovered ? Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.35f) : Theme.withAlpha(-1, 0.06f);
            int text = hovered ? Theme.ACCENT_PRIMARY : Theme.TEXT_VALUE;
            nvg.roundedRect(buttonX, buttonY, buttonW, buttonH, 7.0f, bg);
            nvg.rectOutline(buttonX, buttonY, buttonW, buttonH, 7.0f, 1.0f, border);
            nvg.textCentered("Inter-Regular", label, buttonX, buttonY, buttonW, buttonH, 12.5f, text);
            this.owner.addClickArea(buttonX, buttonY, buttonW, buttonH, () -> {
                this.owner.commitText();
                this.owner.commitColor();
                if (preset == null) {
                    Theme.resetColorsToDefaults();
                } else {
                    preset.apply();
                }
                Theme.saveTheme();
            });
        }
        int rows = (presets.length + columns) / columns;
        return 16.0f + (float)rows * (buttonH + gap) - gap + 18.0f;
    }

    private record LayoutCursor(float y, float tot) {
    }
}
