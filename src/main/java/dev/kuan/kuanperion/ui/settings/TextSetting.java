// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final String placeholder;
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private boolean multiline = false;
    private float fieldWidth = 130;
    private boolean centered;
    public TextSetting withFieldWidth(float width) { fieldWidth=width;return this; }
    public TextSetting numeric() { fieldWidth=66;centered=true;return this; }
    public float fieldWidth(float available) { return Math.min(fieldWidth,Math.max(40,available-(fieldWidth>130?160:100))); }
    public boolean centered() { return centered; }

    private int visibleLines = 4;
    private Supplier<Boolean> visibility = () -> true;

    public TextSetting(String name, String placeholder, Supplier<String> getter, Consumer<String> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.placeholder = UiLang.localize(placeholder);
        this.getter = getter;
        this.setter = setter;
    }

    public String getValue() {
        return this.getter.get();
    }

    public void setValue(String value) {
        this.setter.accept(value);
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public int getVisibleLines() {
        return this.visibleLines;
    }

    public TextSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibility = condition;
        return this;
    }

    public TextSetting multiline() {
        return this.multiline(4);
    }

    public TextSetting multiline(int visibleLines) {
        this.multiline = true;
        this.visibleLines = Math.max(2, visibleLines);
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getRawName() {
        return this.rawName;
    }

    @Override
    public SettingType getType() {
        return SettingType.TEXT;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
