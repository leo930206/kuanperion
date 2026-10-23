package dev.kuan.kuanperion;
public final class HotkeyChordTest {
 public static void main(String[] args){
  for(int expected=0;expected<16;expected++)for(int actual=0;actual<16;actual++)require(HotkeyChord.matches(expected,actual)==(expected==actual),"exact modifiers");
  require(HotkeyChord.matches(0,16|32),"ignore caps/num lock");
  for(int code=340;code<=347;code++)require(HotkeyChord.isModifierKey(code),"modifier capture waits");
  require(!HotkeyChord.isModifierKey(70),"F is base key");
  require(HotkeyChord.parseModifiers(null,1)==1,"legacy Shift default");
  require(HotkeyChord.parseModifiers("0",1)==0,"plain key explicit");
  require(HotkeyChord.parseModifiers("7",1)==7,"three modifiers");
  require(HotkeyChord.parseModifiers("999",1)==1,"invalid persisted mask");
  var edge=new HotkeyChord.Edge();require(edge.press(true,true,true),"first press");require(!edge.press(true,true,true),"held no repeats");
  require(!edge.press(true,false,true),"modifier release no new press");edge.press(false,true,true);require(edge.press(true,true,true),"release then press");
  edge=new HotkeyChord.Edge();require(!edge.press(true,true,false),"no actions while editing/menu/focus lost");require(!edge.press(true,true,true),"held through menu close stays suppressed");
  require(HotkeyChord.prefix(7).equals("Ctrl + Alt + Shift + "),"human readable chord");
  System.out.println("HotkeyChordTest: PASS (all 16 combinations, single keys, lock flags, modifier capture, persistence, one-shot/screen suppression)");
 }
 static void require(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
}
