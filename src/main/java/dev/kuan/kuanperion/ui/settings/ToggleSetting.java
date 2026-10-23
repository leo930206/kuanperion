// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToggleSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private Supplier<Boolean> visibility = () -> true;

    public ToggleSetting(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.getter = getter;
        this.setter = setter;
    }

    public boolean getValue() {
        return this.getter.get();
    }

    public void setValue(boolean value) {
        this.setter.accept(value);
    }

    public void toggle() {
        this.setValue(!this.getValue());
    }

    public ToggleSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibility = condition;
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
        return SettingType.TOGGLE;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
