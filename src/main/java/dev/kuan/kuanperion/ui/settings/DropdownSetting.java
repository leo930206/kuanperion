// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingType;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DropdownSetting
implements Setting {
    private final String name;
    private final String rawName;
    private final List<String> options;
    private final List<IconAction> iconActions = new ArrayList<IconAction>();
    private final Supplier<Integer> indexGetter;
    private final Consumer<Integer> indexSetter;
    private Supplier<Boolean> visibility = () -> true;

    public DropdownSetting(String name, List<String> options, Supplier<Integer> indexGetter, Consumer<Integer> indexSetter) {
        this.rawName = name;
        this.name = UiLang.localize(name);
        this.options = options;
        this.indexGetter = indexGetter;
        this.indexSetter = indexSetter;
    }

    public List<String> getOptions() {
        return this.options.stream().map(UiLang::localize).toList();
    }

    public List<IconAction> getIconActions() {
        return this.iconActions;
    }

    public int getSelectedIndex() {
        return this.indexGetter.get();
    }

    public void setSelectedIndex(int index) {
        this.indexSetter.accept(index);
    }

    public String getSelectedOption() {
        if (this.options.isEmpty()) {
            return "";
        }
        int index = this.getSelectedIndex();
        if (index < 0 || index >= this.options.size()) {
            return UiLang.localize((String)this.options.getFirst());
        }
        return UiLang.localize(this.options.get(index));
    }

    public DropdownSetting addIconAction(String iconPath, Runnable action) {
        this.iconActions.add(new IconAction(iconPath, action));
        return this;
    }

    public DropdownSetting visibleWhen(Supplier<Boolean> condition) {
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
        return SettingType.DROPDOWN;
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }

    public record IconAction(String iconPath, Runnable action) {
        public void execute() {
            if (this.action != null) {
                this.action.run();
            }
        }
    }
}
