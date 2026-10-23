// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Supplier;

public class ActionSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final Runnable action;
    private Supplier<Boolean> visibility = () -> true;

    public ActionSetting(String name, Runnable action) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.action = action;
    }

    public void execute() {
        if (this.action != null) {
            this.action.run();
        }
    }

    public ActionSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.ACTION;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
