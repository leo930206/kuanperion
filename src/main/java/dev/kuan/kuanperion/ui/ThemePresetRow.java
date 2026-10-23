package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.ui.theme.ThemePreset;

/** Responsive six-palette chooser; no seventh default/reset button. */
final class ThemePresetRow {
    static int columns(float width){return Math.clamp((int)((width+8)/144),1,6);}
    static float buttonWidth(float width){int cols=columns(width);return (width-8*(cols-1))/cols;}
    static float height(float width){int rows=(6+columns(width)-1)/columns(width);return rows*48+5;}
    static void render(NVGRenderer nvg,float x,float y,float width,float mx,float my){
        int cols=columns(width);float w=buttonWidth(width);
        var presets=ThemePreset.values();
        for(int i=0;i<presets.length;i++){
            var preset=presets[i];float bx=x+i%cols*(w+8),by=y+i/cols*48;
            boolean hover=mx>=bx&&mx<bx+w&&my>=by&&my<by+40;
            nvg.roundedRect(bx,by,w,40,7,hover?Theme.BG_HOVER:Theme.CARD_BG);
            nvg.rectOutlineSolid(bx,by,w,40,7,1,hover?Theme.ACCENT_PRIMARY:Theme.subtleBorder());
            // Mini color strip previews this palette without applying it.
            int[] swatches=preset.previewColors();float chipW=Math.min(17,(w-24)/swatches.length);
            float sx=bx+(w-chipW*swatches.length)/2;
            for(int j=0;j<swatches.length;j++)nvg.roundedRect(sx+j*chipW,by+7,chipW-2,5,2,swatches[j]);
            nvg.textCentered("Inter-Regular",preset.label(),bx,by+14,w,22,11.5f,hover?Theme.ACCENT_PRIMARY:Theme.TEXT_VALUE);

        }
    }
    static int buttonIndex(float mx,float my,float x,float y,float width){
        int cols=columns(width);float w=buttonWidth(width);
        for(int i=0;i<6;i++){
            float bx=x+i%cols*(w+8),by=y+i/cols*48;
            if(mx>=bx&&mx<bx+w&&my>=by&&my<by+40)return i;
        }
        return -1;
    }
    static void click(float mx,float my,float x,float y,float width){
        int index=buttonIndex(mx,my,x,y,width);if(index<0)return;
        ThemePreset.values()[index].apply();Theme.saveTheme();
    }
    private ThemePresetRow(){}
}
