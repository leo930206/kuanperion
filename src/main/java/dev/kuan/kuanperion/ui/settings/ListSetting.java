// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ListSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final String placeholder;
    private final Supplier<List<String>> getter;
    private final Consumer<List<String>> setter;
    private Supplier<Boolean> visibility = () -> true;

    public ListSetting(String name, String placeholder, Supplier<List<String>> getter, Consumer<List<String>> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.placeholder = UiLang.localize(placeholder);
        this.getter = getter;
        this.setter = setter;
    }

    public List<String> getValues() {
        return new ArrayList<String>((Collection)this.getter.get());
    }

    public void setValues(List<String> values) {
        this.setter.accept(new ArrayList<String>(values));
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public ListSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.LIST;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
