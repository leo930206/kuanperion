// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Supplier;

public final class SectionSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final String description;
    private Supplier<Boolean> visibility = () -> true;

    public SectionSetting(String name, String description) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.description = description == null ? "" : UiLang.localize(description);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public SectionSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.SECTION;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
