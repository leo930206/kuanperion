package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
/** Headless geometry; real glyph wrapping and click offsets are separately checked by isolated UiSmoke. */
public final class GroupHeaderLayoutTest {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void main(String[]args){float saved=MainGUI.uiTextScale;float gap=UiLayoutMetrics.descriptionGap();try{
  MainGUI.uiTextScale=1;UiLayoutMetrics.setDescriptionGap(6);var layout=new MainGUITextLayout();var shortText=SettingGroup.alwaysOn("Short","Short help");var longText=SettingGroup.alwaysOn("Book Combine","每次只合成一組授權的同級單附魔書；工具、武器、快捷列及受保護物品不動。僅接受明確零經驗費用，未知格式即停止。");
  check(layout.groupHeaderHeight(shortText,500)==46,"one-line defaults preserve original height");
  float narrow=layout.groupHeaderHeight(longText,220),wide=layout.groupHeaderHeight(longText,650);check(narrow>wide&&wide>=46,"long help grows with actual available width instead of a fixed46px crop");
  check(layout.groupHeaderDescriptionWidth(longText,220)==196,"same twelve-pixel side padding");
  float top=layout.groupHeaderDescriptionTop();check(top==28.5f,"default title/gap positions unchanged");
  MainGUI.uiTextScale=1.4f;check(layout.groupHeaderHeight(longText,220)>narrow&&layout.groupHeaderDescriptionTop()>top,"font scale affects wrapping and vertical geometry");
  UiLayoutMetrics.setDescriptionGap(12);check(layout.groupHeaderDescriptionTop()==9+13.5f*1.4f+12,"configured help gap is measured, not hidden padding");
  System.out.println("GroupHeaderLayoutTest: PASS (long-help dynamic width/height, default46 retained, title/font/gap metrics; real native glyph/hit/scroll smoke separate)");
 }finally{MainGUI.uiTextScale=saved;UiLayoutMetrics.setDescriptionGap(gap);}}
}
