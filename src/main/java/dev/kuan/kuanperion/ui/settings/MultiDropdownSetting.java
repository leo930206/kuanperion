// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiDropdownSetting
implements Setting {
    public static final float CHIP_H = 28.0f;
    public static final float CHIP_PAD_X = 10.0f;
    public static final float CHIP_GAP = 6.0f;
    public static final float CHIP_FONT_SZ = 12.0f;
    private final String name;
    private final String rawName;
    private final List<String> options;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private Supplier<Boolean> visibility = () -> true;

    public static float chipWidth(String option) {
        return (float)option.length() * 6.24f + 20.0f;
    }

    public int rowCount(float availableWidth) {
        float width = Math.max(1.0f, availableWidth);
        float used = 0.0f;
        int rows = 1;
        for (String option : this.options) {
            float chip = MultiDropdownSetting.chipWidth(option);
            if (used > 0.0f && used + 6.0f + chip > width) {
                ++rows;
                used = chip;
                continue;
            }
            used += (used > 0.0f ? 6.0f : 0.0f) + chip;
        }
        return rows;
    }

    public MultiDropdownSetting(String name, List<String> options, Supplier<Integer> getter, Consumer<Integer> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.options = options.stream().map(UiLang::localize).toList();
        this.getter = getter;
        this.setter = setter;
    }

    public List<String> getOptions() {
        return this.options;
    }

    public boolean isSelected(int index) {
        return (this.getter.get() & 1 << index) != 0;
    }

    public void toggleOption(int index) {
        this.setter.accept(this.getter.get() ^ 1 << index);
    }

    public float estimateTotalWidth() {
        float total = 0.0f;
        for (int i = 0; i < this.options.size(); ++i) {
            total += (float)this.options.get(i).length() * 6.24f + 20.0f;
            if (i <= 0) continue;
            total += 6.0f;
        }
        return total;
    }

    public MultiDropdownSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.MULTI_DROPDOWN;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
