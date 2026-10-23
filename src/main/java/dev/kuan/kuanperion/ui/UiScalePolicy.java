package dev.kuan.kuanperion.ui;
/** The preference is retained; smaller windows use a temporary fit-to-window scale. */
final class UiScalePolicy {
    private UiScalePolicy() {}
    static float effective(float physicalWidth,float physicalHeight,float requested) {
        float preference=Float.isFinite(requested)?Math.max(.5f,Math.min(3,requested)):1.5f;
        float fit=Math.min(Math.max(1,physicalWidth)/660f,Math.max(1,physicalHeight)/440f);
        return Math.max(.1f,Math.min(preference,fit));
    }
}
