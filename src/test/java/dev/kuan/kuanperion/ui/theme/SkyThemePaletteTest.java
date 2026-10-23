package dev.kuan.kuanperion.ui.theme;
import java.util.*;
public final class SkyThemePaletteTest {
    public static void main(String[] args) {
        var old=new HashMap<String,String>(Map.ofEntries(
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
            Map.entry("[Action] Hover","FF46515E")));
        require(SkyThemePalette.isLegacyDefault(old,false),"recognize complete old default");
        old.put("uiScale","2.25");
        require(SkyThemePalette.isLegacyDefault(old,false),"scale does not customize colors");
        require(!SkyThemePalette.isLegacyDefault(old,true),"preserve rainbow preference");
        old.put("Accent","ff123456");
        require(!SkyThemePalette.isLegacyDefault(old,false),"never replace custom color");
        old.put("Accent","ffd32f2f");
        require(SkyThemePalette.isLegacyDefault(old,false),"case insensitive color");
        old.remove("Text Label");
        require(!SkyThemePalette.isLegacyDefault(old,false),"partial file is not default");
        int white=SkyThemePalette.color("Main Panel BG");
        require(white==0xFFFFFFFF,"white main surface");
        for(String label:List.of("Text","Text Dim","Text Muted","Text Label","Text Value","Accent")) {
            int color=SkyThemePalette.color(label);
            require(contrast(color,white)>=4.5,"readable text on white: "+label);
            require(contrast(color,0xFFE4F3FF)>=4.5,"readable text on blue wash: "+label);
        }
        require((SkyThemePalette.color("Gradient Blue")&255)>((SkyThemePalette.color("Gradient Blue")>>16)&255),"blue wash");
        require(((SkyThemePalette.color("Gradient Pink")>>16)&255)>((SkyThemePalette.color("Gradient Pink")>>8)&255),"pink wash");
        System.out.println("SkyThemePaletteTest: PASS (white/blue/pink, contrast, exact legacy migration/custom preservation)");
    }
    private static double lum(int color) {double sum=0;double[] weight={.2126,.7152,.0722};for(int i=0;i<3;i++){double c=((color>>(16-8*i))&255)/255.0;sum+=weight[i]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));}return sum;}
    private static double contrast(int a,int b) {double x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static void require(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
}
