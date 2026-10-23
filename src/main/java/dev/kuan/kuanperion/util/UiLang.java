package dev.kuan.kuanperion.util;
import java.util.Map;
/** Local UI labels only; no translation download, account or network service. */
public final class UiLang {
    private static final Map<String,String> LABELS = Map.ofEntries(
        Map.entry("Modules", "模組"), Map.entry("Colors", "顏色"), Map.entry("Config", "設定檔"),
        Map.entry("Keybinds", "快捷鍵"), Map.entry("Settings", "介面設定"), Map.entry("All", "全部"),
        Map.entry("HUD Colors", "HUD 顏色"), Map.entry("Menu Colors", "選單顏色"), Map.entry("Config Profiles", "功能設定檔"),
        Map.entry("Theme Profiles", "主題設定檔"), Map.entry("Search settings...", "搜尋設定…"),
        Map.entry("Manage and configure modules", "管理現有功能與設定"), Map.entry("Customise HUD and menu colors", "調整介面顏色"),
        Map.entry("Save and manage config and theme profiles", "儲存與管理功能／主題設定檔"),
        Map.entry("Edit Kuanperion keybinds and keep them synced with Minecraft controls", "設定快捷鍵並與 Minecraft 控制選項同步"),
        Map.entry("General client settings", "介面比例與動畫設定"), Map.entry("About", "關於"),
        Map.entry("Back", "返回"), Map.entry("Enabled", "開啟"), Map.entry("Disabled", "關閉"),
        Map.entry("Save", "儲存"), Map.entry("Load", "載入"), Map.entry("Delete", "刪除"), Map.entry("Rename", "改名"),
        Map.entry("Import", "匯入"), Map.entry("Export", "匯出"), Map.entry("Reset", "重設"),
        Map.entry("Search all Kuanperion settings by name and jump directly to matching controls.", "搜尋功能名稱並跳到對應設定。")
    );
    private UiLang() {}
    public static String localize(String value) { return value == null ? "" : LABELS.getOrDefault(value,value); }
}
