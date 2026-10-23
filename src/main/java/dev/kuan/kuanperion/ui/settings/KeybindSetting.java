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
    private final int defaultModifiers;
    private final java.util.function.IntSupplier modifiers;
    private final java.util.function.BiPredicate<InputConstants.Key,Integer> bindingSetter;
    private Supplier<Boolean> visibility = () -> true;

    public KeybindSetting(String name, KeyMapping keyMapping) {
        this(name,keyMapping,0,()->0,(key,mods)->{keyMapping.setKey(key);KeyMapping.resetMapping();return true;});
    }
    public KeybindSetting(String name,KeyMapping keyMapping,int defaultModifiers,java.util.function.IntSupplier modifiers,java.util.function.BiPredicate<InputConstants.Key,Integer> setter) {
        this.rawName=name;this.name=UiLang.localize(name);this.keyMapping=keyMapping;
        this.defaultModifiers=defaultModifiers;this.modifiers=modifiers;this.bindingSetter=setter;
    }
    public int getModifiers(){return modifiers.getAsInt();}

    public KeyMapping getKeyMapping() {
        return this.keyMapping;
    }

    public String getBoundKeyName() {
        return dev.kuan.kuanperion.HotkeyChord.prefix(getModifiers())+this.keyMapping.getTranslatedKeyMessage().getString();
    }

    public boolean isDefault() {
        return this.keyMapping.isDefault() && getModifiers()==defaultModifiers;
    }

    public void setBoundKey(InputConstants.Key key) { setBoundKey(key,getModifiers()); }
    public void setBoundKey(InputConstants.Key key,int modifiers) {
        if(bindingSetter.test(key,modifiers & dev.kuan.kuanperion.HotkeyChord.MASK))Minecraft.getInstance().options.save();
    }
    public void resetToDefault() { setBoundKey(keyMapping.getDefaultKey(),defaultModifiers); }
    public void clearBinding() { setBoundKey(InputConstants.UNKNOWN,0); }

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
