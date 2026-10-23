package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.ColorPresetSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.notification.NotificationManager;

/** Separate load area and explicit save/confirm button. No file I/O in rendering. */
final class ColorPresetRow {
 private ColorPresetRow(){}
 static float height(){return Math.max(60,2*UiLayoutMetrics.TITLE_TOP+24*MainGUI.uiTextScale+UiLayoutMetrics.descriptionGap()+UiLayoutMetrics.ROW_GAP);}
 static float buttonX(float x,float width){return x+width-120;}
 static void render(NVGRenderer nvg,ColorPresetSetting setting,float x,float y,float width,float rowHeight){
  float h=rowHeight-UiLayoutMetrics.ROW_GAP,buttonX=buttonX(x,width),buttonY=y+(h-28)/2;
  nvg.text("Inter-Bold",setting.getName(),x,y+9,13.5f,Theme.TEXT_LABEL);
  nvg.text("Inter-Regular",setting.getValue(),x,y+UiLayoutMetrics.descriptionTop(13.5f*MainGUI.uiTextScale),10.5f,Theme.TEXT_MUTED);
  nvg.roundedRect(buttonX,buttonY,120,28,5,Theme.ACTION_BTN_BG);
  nvg.rectOutline(buttonX,buttonY,120,28,5,1,Theme.BORDER_DEFAULT);
  nvg.textCentered("Inter-Regular",setting.saveLabel(System.currentTimeMillis()),buttonX,buttonY,120,28,11,Theme.TEXT_VALUE);
 }
 static void click(ColorPresetSetting setting,float mx,float my,float x,float y,float width,float rowHeight){
  float buttonX=buttonX(x,width),buttonY=UiLayoutMetrics.controlTop(y,rowHeight,28);
  try{
   if(mx>=buttonX){
    if(my<buttonY || my>buttonY+28)return;
    var result=setting.save(System.currentTimeMillis());
    if(result==ColorPresetSetting.SaveResult.CONFIRM)NotificationManager.warning("確認覆寫", "5 秒內再點一次右側按鈕；不會自動載入");
    else NotificationManager.success(setting.getName(),"已儲存目前顏色與漸層");
   }else if(setting.load())NotificationManager.success(setting.getName(),"已載入配色");
   else NotificationManager.info(setting.getName(),"此槽尚未儲存，請先點右側儲存");
  }catch(java.io.IOException|RuntimeException error){NotificationManager.error(setting.getName(),"操作失敗，原配色保留；請檢查配色檔案與寫入權限");}
 }
}
