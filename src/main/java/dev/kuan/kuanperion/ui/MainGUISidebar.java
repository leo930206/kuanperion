package dev.kuan.kuanperion.ui;

import java.util.List;

/** Visible navigation; existing content-page IDs remain stable for search and profiles. */
final class MainGUISidebar {
    record Entry(String label, String icon, String description, int mainTab, int moduleFilter) {}
    private static final List<Entry> ENTRIES = List.of(
        new Entry("Farming", "farming", "Farming、Pest 通知與 Visitor 通知", 0, 1),
        new Entry("Mining", "mining", "Mining 與目標方塊設定", 0, 2),
        new Entry("Other", "modules", "Jerry 偵測與 Discord Webhook", 0, 3),
        new Entry("Setting", "settings", "顏色、設定檔、快捷鍵與介面設定", 4, 0));
    static List<Entry> entries() { return ENTRIES; }
    static int selectedIndex(int page, int filter) {
        if (page == 0) return Math.max(0, Math.min(2, filter - 1));
        return page >= 1 && page <= 4 ? 3 : -1;
    }
    static float rowY(int index, float panelY, float panelHeight) {
        return index < 3 ? panelY + 64 + index * 44 : footerY(panelY, panelHeight) - 44;
    }
    static float footerY(float panelY, float panelHeight) { return panelY + panelHeight - 24; }
    static List<String> footerLines(String version) { return List.of(footerText(version,true)); }
    static String footerText(String version, boolean expanded) { return expanded ? "Mod Version " + version : version; }
    static float footerTextX(float panelX, float sidebarWidth, float textWidth) { return panelX + (sidebarWidth - textWidth) / 2; }
    static final float FOOTER_HEIGHT = 22, FOOTER_FONT = 9;
    static boolean footerContains(float x, float y, float panelX, float panelY, float panelHeight, float sidebarWidth, float animation) {
        float top = footerY(panelY, panelHeight);
        return x >= panelX + 12 && x < panelX + sidebarWidth - 12 && y >= top && y < top + FOOTER_HEIGHT;
    }
    private MainGUISidebar() {}
}
