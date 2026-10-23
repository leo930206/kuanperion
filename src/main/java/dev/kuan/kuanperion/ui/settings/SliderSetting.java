// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SliderSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final float min;
    private final float max;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private int decimals = 1;
    private String suffix = "";
    private boolean percentage = false;
    private Supplier<Boolean> visibility = () -> true;

    public SliderSetting(String name, float min, float max, Supplier<Float> getter, Consumer<Float> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.min = min;
        this.max = max;
        this.getter = getter;
        this.setter = setter;
    }

    public float getValue() {
        return this.getter.get().floatValue();
    }

    public void setValue(float value) {
        if (!Float.isFinite(value)) return;
        this.setter.accept(Float.valueOf(Math.max(this.min, Math.min(this.max, value))));
    }

    public float getMin() {
        return this.min;
    }

    public float getMax() {
        return this.max;
    }

    public int getDecimals() {
        return this.decimals;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public boolean isPercentage() {
        return this.percentage;
    }

    public SliderSetting withDecimals(int decimals) {
        this.decimals = decimals;
        return this;
    }

    public SliderSetting withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public SliderSetting asPercentage() {
        this.percentage = true;
        return this;
    }

    public SliderSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.SLIDER;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
