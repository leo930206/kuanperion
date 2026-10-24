#!/usr/bin/env python3
"""Test production chat factories/sender with actual Minecraft Component and Style."""
from pathlib import Path
import re,subprocess,sys,tempfile
root=Path(__file__).resolve().parent.parent
source=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def extract(name):
 a=re.search(r'    private static [^\n]* '+name+r'\(',source).start();b=source.index('{',a)+1;d=1
 while d:
  d+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
constants='\n'.join(re.findall(r'    private static final int COLOR_[^\n]+',source))
methods='\n'.join(extract(n) for n in ['say','hsbaPrefix','plain','feature','success','error','warning','blockName','locationName','cropName'])
java=r'''package dev.kuan.kuanperion;
import net.minecraft.network.chat.*;
import java.util.*;
public class ChatStyleTest {
 CONSTANTS
 METHODS
 static class Minecraft {Player player=new Player();}
 static class Player {Component received;void sendSystemMessage(Component c){received=c;}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void style(Component c,int color){
  c.visit((Style s,String text)->{check(!s.isBold(),"no inherited bold on "+text);check(s.getColor()!=null&&s.getColor().getValue()==color,"requested two-tone palette on "+text);return Optional.empty();},Style.EMPTY.withBold(true));
 }
 public static void main(String[] args){
  style(plain("通知文字"),0x00D8F5);
  for(Component c:List.of(feature("Farming"),success("開啟"),error("關閉"),warning("警告"),blockName("minecraft:stone"),locationName("Garden"),cropName("Wheat")))style(c,0x0088D1);
  check(blockName("minecraft:stone").getString().equals("stone"),"block-name text unchanged");
  var mc=new Minecraft();say(mc,feature("Mining"),plain(" 一般訊息 "),success("開啟"));
  check(mc.player.received.getString().equals("[Kuanperion] Mining 一般訊息 開啟"),"prefix/text/order unchanged");
  mc.player.received.visit((Style s,String text)->{check(!s.isBold(),"entire sent message never bold");return Optional.empty();},Style.EMPTY.withBold(true));
  var prefix=hsbaPrefix();var parts=prefix.getSiblings();
  check(parts.get(0).getStyle().getColor().getValue()==0xAAAAAA&&parts.get(1).getStyle().getColor().getValue()==0x13A9F2&&parts.get(parts.size()-3).getStyle().getColor().getValue()==0xE64478,"existing gray brackets/gradient untouched");
  mc.player=null;say(mc,plain("outside world"));
  System.out.println("ChatStyleTest: PASS (actual Component/Style, two tones/all roles, inherited bold blocked, prefix/text unchanged)");
 }
}'''.replace('CONSTANTS',constants).replace('METHODS',methods)
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'ChatStyleTest.java';p.write_text(java);jdk=Path(sys.argv[1])/'bin';cp=sys.argv[2]
 subprocess.run([str(jdk/'javac'),'--release','25','-encoding','UTF-8','-cp',cp,'-d',tmp,str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+cp,'dev.kuan.kuanperion.ChatStyleTest'],check=True)
