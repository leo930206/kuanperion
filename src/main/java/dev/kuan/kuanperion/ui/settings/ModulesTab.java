// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.util.UiLang;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ModulesTab {
    private ModulesTab() {
    }

    public record SubTab(String name, String description, BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter, List<SettingGroup> groups) {
        public SubTab {
            name = UiLang.localize(name);
            description = UiLang.localize(description);
        }

        public SubTab(String name, String description, List<SettingGroup> groups) {
            this(name, description, null, null, groups);
        }

        public boolean hasToggle() {
            return this.enabledGetter != null && this.enabledSetter != null;
        }

        public boolean isEnabled() {
            return !this.hasToggle() || this.enabledGetter.getAsBoolean();
        }

        public void toggle() {
            if (this.hasToggle()) {
                this.enabledSetter.accept(!this.isEnabled());
            }
        }
    }
}
