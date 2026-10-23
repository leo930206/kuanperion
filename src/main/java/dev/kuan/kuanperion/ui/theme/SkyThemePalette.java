package dev.kuan.kuanperion.ui.theme;
import java.util.Map;
/** Local palette and conservative old-default detection. No Fabric/GL initialization. */
public final class SkyThemePalette {
    private SkyThemePalette() {}
    private static final Map<String,Integer> COLORS=Map.ofEntries(
        Map.entry("Accent",0xFF1474A6),
        Map.entry("Accent 2",0xFFE4A3C7),
        Map.entry("Panel BG",0xFFF5FAFE),
        Map.entry("Hover BG",0xFFE2F1FC),
        Map.entry("Input BG",0xFFF0F7FC),
        Map.entry("Card BG",0xF2FFFFFF),
        Map.entry("Element BG",0xFFEAF4FB),
        Map.entry("Sidebar BG",0xF2F6FBFF),
        Map.entry("Main Panel BG",0xFFFFFFFF),
        Map.entry("Border",0xFF9EB9CE),
        Map.entry("Separator",0xFFCADDEB),
        Map.entry("Text",0xFF17384E),
        Map.entry("Text Dim",0xFF4B6579),
        Map.entry("Text Muted",0xFF526D80),
        Map.entry("Text Label",0xFF284B63),
        Map.entry("Text Value",0xFF1474A6),
        Map.entry("Group Active",0xFFD9ECFA),
        Map.entry("[Toggle] Track",0xFFCADDEB),
        Map.entry("[Toggle] Knob",0xFF688DA8),
        Map.entry("[Slider] Left",0xFFB8DAEF),
        Map.entry("[Dropdown] BG",0xFFEDF6FC),
        Map.entry("[Action] BG",0xFFEAF4FB),
        Map.entry("[Action] Hover",0xFFD9EDFC),
        Map.entry("Gradient Blue",0xB37BC6F5),
        Map.entry("Gradient Pink",0x99ECB0CE));
    private static final Map<String,String> LEGACY=Map.ofEntries(
        Map.entry("Accent","FFD32F2F"),
        Map.entry("Accent 2","FFB71C1C"),
        Map.entry("Panel BG","FF2A3038"),
        Map.entry("Hover BG","FF38414D"),
        Map.entry("Input BG","FF323A45"),
        Map.entry("Card BG","FF2B333D"),
        Map.entry("Element BG","FF36404B"),
        Map.entry("Sidebar BG","FF171C23"),
        Map.entry("Main Panel BG","FF20262D"),
        Map.entry("Border","FF667383"),
        Map.entry("Separator","FF4A5563"),
        Map.entry("Text","FFF5F7FA"),
        Map.entry("Text Dim","FFC3CBD5"),
        Map.entry("Text Muted","FF9AA6B2"),
        Map.entry("Text Label","FFF0F3F6"),
        Map.entry("Text Value","FFD2D9E1"),
        Map.entry("Group Active","FFB8C4D1"),
        Map.entry("[Toggle] Track","FF46515E"),
        Map.entry("[Toggle] Knob","FFB4BEC9"),
        Map.entry("[Slider] Left","FF4FA3D1"),
        Map.entry("[Dropdown] BG","FF36404B"),
        Map.entry("[Action] BG","FF36404B"),
        Map.entry("[Action] Hover","FF46515E"));
    public static int color(String name) {
        Integer color=COLORS.get(name);
        if(color==null)throw new IllegalArgumentException("Unknown theme token: "+name);
        return color;
    }
    public static boolean isLegacyDefault(Map<String,String> colors,boolean rainbow) {
        if(rainbow)return false;
        for(var entry:LEGACY.entrySet())
            if(!entry.getValue().equalsIgnoreCase(colors.get(entry.getKey())))return false;
        return true;
    }
}
