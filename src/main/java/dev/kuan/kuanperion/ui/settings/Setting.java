// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.settings;

import dev.kuan.kuanperion.ui.settings.SettingDescriptionCatalog;
import dev.kuan.kuanperion.ui.settings.SettingType;

public interface Setting {
    public String getName();

    default public String getRawName() {
        return this.getName();
    }

    public SettingType getType();

    default public String getDescription() {
        return SettingDescriptionCatalog.describe(this);
    }

    public boolean isVisible();
}
