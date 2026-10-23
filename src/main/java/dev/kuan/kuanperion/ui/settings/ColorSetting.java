// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private Supplier<Boolean> visibilityCondition = null;

    public ColorSetting(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.getter = getter;
        this.setter = setter;
    }

    public ColorSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public int getValue() {
        return this.getter.get();
    }

    public void setValue(int v) {
        this.setter.accept(v);
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
        return SettingType.COLOR;
    }

    @Override
    public boolean isVisible() {
        return this.visibilityCondition == null || this.visibilityCondition.get() != false;
    }
}
