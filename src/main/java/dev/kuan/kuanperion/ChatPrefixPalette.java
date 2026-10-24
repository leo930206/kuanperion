package dev.kuan.kuanperion;
public final class ChatPrefixPalette {
 public static final int BRACKET=0xAAAAAA;
 private ChatPrefixPalette(){}
 public static int colorAt(int index,int length){
  double progress=length<=1?0:Math.clamp(index/(double)(length-1),0,1);
  int start=progress<=.5?0x23B6F5:0xFFFFFF,end=progress<=.5?0xFFFFFF:0xF06C93;
  double t=progress<=.5?progress*2:(progress-.5)*2;
  int r=(int)(((start>>16)&255)*(1-t)+((end>>16)&255)*t);
  int g=(int)(((start>>8)&255)*(1-t)+((end>>8)&255)*t);
  int b=(int)((start&255)*(1-t)+(end&255)*t);
  return r<<16|g<<8|b;
 }
}
