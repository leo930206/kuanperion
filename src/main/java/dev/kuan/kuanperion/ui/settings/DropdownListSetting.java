// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DropdownListSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final List<String> allOptions;
    private final DropdownSetting addPicker;
    private final Supplier<List<String>> getter;
    private final Consumer<List<String>> setter;
    private Supplier<Boolean> visibility = () -> true;

    public DropdownListSetting(String name, List<String> allOptions, Supplier<List<String>> getter, Consumer<List<String>> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.allOptions = List.copyOf(allOptions);
        int[] pendingIdx = new int[]{0};
        this.addPicker = new DropdownSetting("", allOptions, () -> pendingIdx[0], i -> {
            pendingIdx[0] = i;
        });
        this.getter = getter;
        this.setter = setter;
    }

    public List<String> getValues() {
        return new ArrayList<String>((Collection)this.getter.get());
    }

    public void addValue(String v) {
        List<String> vals = this.getValues();
        if (!vals.contains(v)) {
            vals.add(v);
            this.setter.accept(vals);
        }
    }

    public void removeValue(int index) {
        List<String> vals = this.getValues();
        if (index >= 0 && index < vals.size()) {
            vals.remove(index);
            this.setter.accept(vals);
        }
    }

    public void moveUp(int index) {
        List<String> vals = this.getValues();
        if (index > 0 && index < vals.size()) {
            String tmp = vals.get(index - 1);
            vals.set(index - 1, vals.get(index));
            vals.set(index, tmp);
            this.setter.accept(vals);
        }
    }

    public void moveDown(int index) {
        List<String> vals = this.getValues();
        if (index >= 0 && index < vals.size() - 1) {
            String tmp = vals.get(index + 1);
            vals.set(index + 1, vals.get(index));
            vals.set(index, tmp);
            this.setter.accept(vals);
        }
    }

    public List<String> getAllOptions() {
        return this.allOptions;
    }

    public DropdownSetting getAddPicker() {
        return this.addPicker;
    }

    public DropdownListSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.DROPDOWN_LIST;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
