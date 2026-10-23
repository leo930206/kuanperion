package dev.kuan.kuanperion.ui.settings;
public final class SettingDescriptionCatalog {
    private SettingDescriptionCatalog() {}
    public static String describe(Setting setting) { return setting.getName(); }
}
