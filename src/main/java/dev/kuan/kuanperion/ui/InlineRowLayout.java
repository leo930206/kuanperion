package dev.kuan.kuanperion.ui;
public final class InlineRowLayout {
 private InlineRowLayout(){}
 public static float keybindWidth(float innerWidth){return Math.min(260,Math.max(132,innerWidth*.44f));}
 public static float countButtonWidth(float innerWidth){return Math.min(170,Math.max(110,innerWidth*.30f));}
 public static float countFieldX(float x,float width){return x+width-countButtonWidth(width)-12-60;}
 public static float durationContentWidth(float width){return width-50;}
}
