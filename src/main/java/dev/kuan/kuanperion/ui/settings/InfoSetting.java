// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Supplier;

public class InfoSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final Supplier<String> valueSupplier;
    private Supplier<Boolean> visibility = () -> true;
    private boolean multiline;

    public InfoSetting(String name, Supplier<String> valueSupplier) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.valueSupplier = valueSupplier;
    }

    public String getValue() {
        String value = this.valueSupplier.get();
        return value == null ? "" : value;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public InfoSetting multiline() {
        this.multiline = true;
        return this;
    }

    public InfoSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.INFO;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
