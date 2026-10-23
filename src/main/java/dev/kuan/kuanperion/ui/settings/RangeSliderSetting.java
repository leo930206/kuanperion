// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RangeSliderSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final float min;
    private final float max;
    private final Supplier<Float> lowerGetter;
    private final Supplier<Float> upperGetter;
    private final BiConsumer<Float, Float> setter;
    private int decimals = 1;
    private String suffix = "";
    private Supplier<Boolean> visibility = () -> true;

    public RangeSliderSetting(String name, float min, float max, Supplier<Float> lowerGetter, Supplier<Float> upperGetter, BiConsumer<Float, Float> setter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.min = min;
        this.max = max;
        this.lowerGetter = lowerGetter;
        this.upperGetter = upperGetter;
        this.setter = setter;
    }

    public float getLowerValue() {
        return this.lowerGetter.get().floatValue();
    }

    public float getUpperValue() {
        return this.upperGetter.get().floatValue();
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

    public void setLowerValue(float value) {
        this.setValues(value, this.getUpperValue());
    }

    public void setUpperValue(float value) {
        this.setValues(this.getLowerValue(), value);
    }

    public void setValues(float lower, float upper) {
        float clampedUpper;
        float clampedLower = Math.max(this.min, Math.min(this.max, lower));
        if (clampedLower > (clampedUpper = Math.max(this.min, Math.min(this.max, upper)))) {
            float swap = clampedLower;
            clampedLower = clampedUpper;
            clampedUpper = swap;
        }
        this.setter.accept(Float.valueOf(clampedLower), Float.valueOf(clampedUpper));
    }

    public RangeSliderSetting withDecimals(int decimals) {
        this.decimals = decimals;
        return this;
    }

    public RangeSliderSetting withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public RangeSliderSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.RANGE_SLIDER;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
