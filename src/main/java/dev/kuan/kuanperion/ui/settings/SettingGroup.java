// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingGroup {
    private final String name;
    private final String rawName;
    private final String description;
    private final String rawDescription;
    private final BooleanSupplier enabledGetter;
    private final Consumer<Boolean> enabledSetter;
    private final boolean alwaysOnFlag;
    private final List<Setting> settings = new ArrayList<Setting>();

    private SettingGroup(String name, String description, BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter, boolean alwaysOnFlag) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.rawDescription = description;
        this.description = UiLang.localize(description);
        this.enabledGetter = enabledGetter;
        this.enabledSetter = enabledSetter;
        this.alwaysOnFlag = alwaysOnFlag;
    }

    public static SettingGroup of(String name, String description, BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter) {
        return new SettingGroup(name, description, enabledGetter, enabledSetter, false);
    }

    public static SettingGroup alwaysOn(String name, String description) {
        return new SettingGroup(name, description, () -> true, v -> {}, true);
    }

    public SettingGroup add(Setting setting) {
        this.settings.add(setting);
        return this;
    }

    public boolean isEnabled() {
        return this.enabledGetter.getAsBoolean();
    }

    public void toggle() {
        this.enabledSetter.accept(!this.isEnabled());
    }

    public void setEnabled(boolean value) {
        this.enabledSetter.accept(value);
    }

    public String getName() {
        return this.name;
    }

    public String getRawName() {
        return this.rawName;
    }

    public String getDescription() {
        return this.description;
    }

    public String getRawDescription() {
        return this.rawDescription;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public boolean hasSettings() {
        return !this.settings.isEmpty();
    }

    public boolean isAlwaysOn() {
        return this.alwaysOnFlag;
    }
}
