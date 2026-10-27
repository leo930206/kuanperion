package dev.kuan.kuanperion.ui;

import java.util.List;

/** Visible navigation; existing content-page IDs remain stable for search and profiles. */
final class MainGUISidebar {
    record Entry(String label, String icon, String description, int mainTab, int moduleFilter) {}
    private static final List<Entry> ENTRIES = List.of(
        new Entry("Farming", "farming", "Farming、Pest、Greenhouse、Visitor 與 Composter", 0, 1),
        new Entry("Mining", "mining", "Mining 與目標方塊設定", 0, 2),
        new Entry("Trade", "trade", "NPC 出售、Book Combine、Junk 與 Stash；其他交易工作移植中", 0, 3),
        new Entry("Other", "modules", "Jerry 偵測與 Discord Webhook", 0, 4),
        new Entry("Setting", "settings", "介面、顏色與快捷鍵", 4, 0));
    static int settingsIndex() { return ENTRIES.size()-1; }
    static List<Entry> entries() { return ENTRIES; }
    static int selectedIndex(int page, int filter) {
        if (page == 0) return Math.max(0, Math.min(settingsIndex()-1, filter - 1));
        return page >= 1 && page <= 4 ? settingsIndex() : -1;
    }
    static float rowY(int index, float panelY, float panelHeight) {
        return index < settingsIndex() ? panelY + 64 + index * 44 : footerY(panelY, panelHeight) - 44;
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
    /** Same animated bounds for painting, hover, click and wheel ownership. */
    static boolean contains(float x,float y,float panelX,float panelY,float panelHeight,float expandedWidth,float animation) {
        float width=60+Math.clamp(animation,0,1)*(expandedWidth-60);
        return x>=panelX&&x<panelX+width&&y>=panelY&&y<=panelY+panelHeight;
    }
    private MainGUISidebar() {}
}
