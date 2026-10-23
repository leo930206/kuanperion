package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.ColorPresetSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.notification.NotificationManager;

final class ColorPresetRow {
 static final float BUTTON_W=64,BUTTON_GAP=8,SWATCH=18,SWATCH_GAP=8;
 private ColorPresetRow(){}
 static float deleteX(float x,float width){return x+width-BUTTON_W;}
 static float saveX(float x,float width){return deleteX(x,width)-BUTTON_GAP-BUTTON_W;}
 static int columns(float width){return Math.max(1,(int)((width-2*BUTTON_W-BUTTON_GAP-16+SWATCH_GAP)/(SWATCH+SWATCH_GAP)));}
 static float height(ColorPresetSetting setting,float rowWidth){
  int cols=columns(rowWidth-24),rows=Math.max(1,(setting.previewColors().size()+cols-1)/cols);
  return Math.max(50,18+rows*SWATCH+(rows-1)*SWATCH_GAP+UiLayoutMetrics.ROW_GAP);
 }
 static void render(NVGRenderer nvg,ColorPresetSetting setting,float x,float y,float width,float rowHeight,float mx,float my){
  float h=rowHeight-UiLayoutMetrics.ROW_GAP,buttonY=y+(h-28)/2;
  var colors=setting.previewColors();int cols=columns(width),rows=Math.max(1,(colors.size()+cols-1)/cols);
  float gridH=rows*SWATCH+(rows-1)*SWATCH_GAP,top=y+(h-gridH)/2;
  for(int i=0;i<colors.size();i++){
   float sx=x+(i%cols)*(SWATCH+SWATCH_GAP),sy=top+(i/cols)*(SWATCH+SWATCH_GAP);
   nvg.roundedRect(sx,sy,SWATCH,SWATCH,4,colors.get(i)|0xFF000000);
   nvg.rectOutline(sx,sy,SWATCH,SWATCH,4,1,Theme.BORDER_DEFAULT);
  }
  button(nvg,saveX(x,width),buttonY,setting.saveLabel(System.currentTimeMillis()),mx,my);
  button(nvg,deleteX(x,width),buttonY,"刪除",mx,my);
 }
 private static void button(NVGRenderer nvg,float x,float y,String label,float mx,float my){
  boolean hover=mx>=x&&mx<=x+BUTTON_W&&my>=y&&my<=y+28;
  nvg.roundedRect(x,y,BUTTON_W,28,5,hover?Theme.ACTION_BTN_HOVER:Theme.ACTION_BTN_BG);
  nvg.rectOutline(x,y,BUTTON_W,28,5,1,hover?Theme.ACCENT_PRIMARY:Theme.BORDER_DEFAULT);
  nvg.textCentered("Inter-Regular",label,x,y,BUTTON_W,28,11,Theme.TEXT_VALUE);
 }
 static void click(ColorPresetSetting setting,float mx,float my,float x,float y,float width,float rowHeight){
  float buttonY=UiLayoutMetrics.controlTop(y,rowHeight,28);
  try{
   if(mx>=saveX(x,width)){
    if(my<buttonY||my>buttonY+28)return;
    if(mx>=deleteX(x,width)){
     setting.delete();NotificationManager.success("配色收藏","已刪除配色");
    }else if(mx<=saveX(x,width)+BUTTON_W){
     var result=setting.save(System.currentTimeMillis());
     if(result==ColorPresetSetting.SaveResult.CONFIRM)NotificationManager.warning("確認儲存","5 秒內再點一次儲存，覆寫此配色");
     else NotificationManager.success("配色收藏","已儲存目前配色");
    }
   }else if(setting.load())NotificationManager.success("配色收藏","已載入配色");
  }catch(java.io.IOException|RuntimeException error){NotificationManager.error("配色收藏","操作失敗，原配色保留；請檢查配色檔案與寫入權限");}
 }
}
