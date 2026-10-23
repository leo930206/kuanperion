package dev.kuan.kuanperion;
/** GLFW modifier policy without Minecraft/GL dependencies. */
public final class HotkeyChord {
 private HotkeyChord(){}
 public static final int MASK=15;
 public static boolean matches(int expected,int actual){return (expected&MASK)==(actual&MASK);}
 public static boolean isModifierKey(int key){return key>=340&&key<=347;}
 public static int parseModifiers(String value,int fallback){try{int n=Integer.parseInt(value);return n>=0&&n<=MASK?n:fallback;}catch(RuntimeException ignored){return fallback;}}
 public static String prefix(int modifiers){StringBuilder s=new StringBuilder();if((modifiers&2)!=0)s.append("Ctrl + ");if((modifiers&4)!=0)s.append("Alt + ");if((modifiers&1)!=0)s.append("Shift + ");if((modifiers&8)!=0)s.append("Command + ");return s.toString();}
 public static final class Edge {
  private boolean down;
  public boolean press(boolean baseDown,boolean modifierMatch,boolean allowed){boolean fire=baseDown&&!down&&modifierMatch&&allowed;down=baseDown;return fire;}
 }
}
