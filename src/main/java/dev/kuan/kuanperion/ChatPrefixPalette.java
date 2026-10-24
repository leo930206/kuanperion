package dev.kuan.kuanperion;
public final class ChatPrefixPalette {
 public static final int BRACKET=0xAAAAAA;
 private ChatPrefixPalette(){}
 public static int colorAt(int index,int length){
  double progress=length<=1?0:Math.clamp(index/(double)(length-1),0,1);
  int start=progress<=.5?0x13A9F2:0xFFFFFF,end=progress<=.5?0xFFFFFF:0xE64478;
  // Concentrate the white highlight near the center; keep more blue/pink across the name.
  double t=progress<=.5?Math.pow(progress*2,2):1-Math.pow((1-progress)*2,2);
  int r=(int)(((start>>16)&255)*(1-t)+((end>>16)&255)*t);
  int g=(int)(((start>>8)&255)*(1-t)+((end>>8)&255)*t);
  int b=(int)((start&255)*(1-t)+(end&255)*t);
  return r<<16|g<<8|b;
 }
}
