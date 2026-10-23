// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeybindSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final KeyMapping keyMapping;
    private Supplier<Boolean> visibility = () -> true;

    public KeybindSetting(String name, KeyMapping keyMapping) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.keyMapping = keyMapping;
    }

    public KeyMapping getKeyMapping() {
        return this.keyMapping;
    }

    public String getBoundKeyName() {
        return this.keyMapping.getTranslatedKeyMessage().getString();
    }

    public boolean isDefault() {
        return this.keyMapping.isDefault();
    }

    public void setBoundKey(InputConstants.Key key) {
        this.keyMapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    public void resetToDefault() {
        this.setBoundKey(this.keyMapping.getDefaultKey());
    }

    public void clearBinding() {
        this.setBoundKey(InputConstants.UNKNOWN);
    }

    public KeybindSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.KEYBIND;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
