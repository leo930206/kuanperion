package dev.kuan.kuanperion.ui;
/** Shared logical-pixel defaults; configurable from interface settings and stored with the theme. */
public final class UiLayoutMetrics {
 public static final float TITLE_TOP=9,DESCRIPTION_FONT=10.5f,DESCRIPTION_LINE=13,ROW_GAP=5;
 private static float gap=6,inputHeight=16;
 private UiLayoutMetrics(){}
 public static float descriptionGap(){return gap;}
 public static float inputHeight(){return inputHeight;}
 public static float inputFont(){return Math.min(11,Math.max(9,inputHeight-6));}
 public static float descriptionTop(float titleHeight){return TITLE_TOP+titleHeight+gap;}
 public static void setDescriptionGap(float value){gap=clamp(value,6,12,6);}
 public static void setInputHeight(float value){inputHeight=clamp(value,14,24,16);}
 public static float clamp(float value,float low,float high,float fallback){return Float.isFinite(value)?Math.max(low,Math.min(high,value)):fallback;}
 public static void reset(){gap=6;inputHeight=16;}
}
