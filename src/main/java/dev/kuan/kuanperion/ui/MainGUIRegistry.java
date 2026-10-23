package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.KuanperionClient;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.ui.theme.Theme;
import java.util.*;

/** Explicit local allowlist; no ServiceLoader of upstream feature/bootstrap providers. */
public final class MainGUIRegistry {
    public static final List<ModuleSection> MODULE_SECTIONS=new ArrayList<>();
    public static final List<ModulesTab.SubTab> MODULE_SUBTABS=new ArrayList<>();
    public static final List<ModulesTab.SubTab> COLORS_SUBTABS=new ArrayList<>();
    public static final List<ModulesTab.SubTab> KEYBINDS_SUBTABS=new ArrayList<>();
    public static final List<ModulesTab.SubTab> SETTINGS_SUBTABS=new ArrayList<>();
    public static final List<ModulesTab.SubTab> PROFILES_SUBTABS=List.of(new ModulesTab.SubTab("Config","本機功能設定檔",List.of()),new ModulesTab.SubTab("Themes","本機介面主題",List.of()));
    private static boolean populated;
    private MainGUIRegistry() {}
    public static void refresh() {
        if(populated)return;
        MODULE_SECTIONS.addAll(KuanperionClient.uiSections());
        for(var section:MODULE_SECTIONS)MODULE_SUBTABS.addAll(section.subtabs());
        var colors=SettingGroup.alwaysOn("選單顏色","原始配色與色彩選擇器；即時預覽並儲存");
        for(var entry:Theme.ENTRIES)colors.add(new ColorSetting(entry.label,entry.getter,v->{entry.setter.accept(v);Theme.saveTheme();}));
        colors.add(new ToggleSetting("藍粉漸層背景",()->Theme.GRADIENT_ENABLED,v->{Theme.GRADIENT_ENABLED=v;Theme.saveTheme();}));
        colors.add(new ActionSetting("重設顏色",()->{Theme.resetColorsToDefaults();Theme.saveTheme();}));
        COLORS_SUBTABS.add(new ModulesTab.SubTab("Menu Colors","介面主題色彩",List.of(colors)));
        var keys=SettingGroup.alwaysOn("快捷鍵","點擊後按下單鍵或組合鍵，例如 F、Ctrl + F、Ctrl + Shift + F；Esc 取消");
        for(var setting:KuanperionClient.uiKeybinds())keys.add(setting);
        KEYBINDS_SUBTABS.add(new ModulesTab.SubTab("快捷鍵","與 Minecraft 控制設定同步",List.of(keys)));
        var settings=SettingGroup.alwaysOn("介面設定","只調整本機 UI，不影響 Mining／Farming 行為")
            .add(new SliderSetting("介面大小",Theme.UI_SCALE_MIN,Theme.UI_SCALE_MAX,()->Theme.UI_SCALE,v->{Theme.UI_SCALE=v;Theme.saveTheme();}).withDecimals(2))
            .add(new SliderSetting("文字大小",Theme.TEXT_SCALE_MIN,Theme.TEXT_SCALE_MAX,()->Theme.TEXT_SCALE,v->{Theme.TEXT_SCALE=v;Theme.saveTheme();}).withDecimals(2))
            .add(new SliderSetting("動畫時間",Theme.ANIM_TIME_MIN_MS,Theme.ANIM_TIME_MAX_MS,()->Theme.ANIM_TIME_MS,v->{Theme.ANIM_TIME_MS=v;Theme.saveTheme();}).withDecimals(0).withSuffix(" ms"))
            .add(new ActionSetting("重設介面",()->{Theme.resetToDefaults();Theme.saveTheme();}))
            .add(new InfoSetting("Kuanperion",()->"2.0.1 · Minecraft 26.2 · 僅本機介面"));
        SETTINGS_SUBTABS.add(new ModulesTab.SubTab("介面設定","比例、動畫與版本",List.of(settings)));
        populated=true;
    }
    public record ModuleSection(String id,String displayName,List<ModulesTab.SubTab> subtabs) {}
}
