package dev.kuan.kuanperion.ui.settings;
import java.util.Map;
/** Explanations of the existing behavior, not additional automation. */
public final class SettingDescriptionCatalog {
 private SettingDescriptionCatalog(){}
 private static final Map<String,String> HELP=Map.ofEntries(
  Map.entry("漸層範圍","越小白色留白越多；只縮小角落色彩範圍，不改主底色。"),
  Map.entry("漸層濃度","越小藍粉越淡；0 為不顯示，1 保留色票原始透明度。"),
  Map.entry("說明間距","群組與各設定共用的標題／說明間隔；預設 6，可增大。"),
  Map.entry("輸入框高度","調整數字與單行文字框高度；不影響快捷鍵按鈕或多行文字區。"),
  Map.entry("工具偵測","拿錯農耕工具時暫停動作；換回對應工具後繼續，不會自動換工具。"),
  Map.entry("智慧暫停","背包、聊天室等其他選單暫停，關閉後緩衝 0.5 秒；Kuanperion 設定介面不暫停。"),
  Map.entry("異常狀況處理","啟用原有掉落／換世界回復流程；非預期停滯滿 5 秒可通知 Discord，需設定 Webhook。"),
  Map.entry("偵測半徑","搜尋玩家周圍的目標方塊，範圍 1～8 格；越大需檢查的方塊越多。"),
  Map.entry("轉動速度","每次轉向目標的角度上限，範圍 1～40°；數字越大，轉向越快。"),
  Map.entry("挖礦延遲","兩次挖礦動作間的等待，範圍 1～20 ticks；20 ticks 約為 1 秒。"),
  Map.entry("選擇種類數量","輸入 1～5，開始後在遊戲內右鍵選取不同方塊；只顯示已選取項目。"),
  Map.entry("通知門檻","數量達到此門檻才通知；Pest 為 1～8，Visitor 為 1～5。"),
  Map.entry("Discord 訊息通知","將此功能通知送到你設定的共用 Webhook；不提供遠端控制。"),
  Map.entry("遊戲內聊天室通知","只在遊戲聊天室顯示此功能通知，不改變其他通知通道。"),
  Map.entry("遊戲內螢幕通知","在畫面中央顯示通知；時間單位為秒，可填正小數，留空使用 3 秒。"),
  Map.entry("出現 Jerry 後自動 sethome","偵測到已勾選顏色的 Jerry 時，立即發送 /sethome；不受通知通道或停止動作開關影響。"),
  Map.entry("出現 Jerry 是否停下動作","偵測到已勾選顏色的 Jerry 時，停止目前 Mining／Farming；不自動追蹤或擊殺。"),
  Map.entry("光柱最久存在時間（秒）","Jerry 造成 Farming 停機後的光柱期限；0 秒不顯示光柱；預設 300 秒，離線也計時。"),
  Map.entry("Webhook 網址","所有 Discord 通知共用，僅在啟用時使用。"),
  Map.entry("藍粉漸層背景","開啟左下淡藍、右上淡粉的柔和漸層；關閉後保留主面板底色。")
 );
 public static String describe(Setting setting){
  if(setting instanceof ToggleRowSetting)return setting.getName();
  if(setting instanceof BlockTargetSetting)return "已選取方塊；排列即挖礦優先序，可移動或移除，不會新增空白項目。";
  if(setting.getType()==SettingType.KEYBIND)return "先按住所需 Ctrl／Shift／Alt／Command，再按主鍵；也可直接按單鍵。Esc 取消，Delete／Backspace 清除。";
  return HELP.getOrDefault(setting.getRawName(),setting.getName());
 }
 public static boolean inlineHelp(Setting setting){return !setting.getDescription().equals(setting.getName())&&(setting.getType()==SettingType.TOGGLE||setting.getType()==SettingType.SLIDER||setting.getType()==SettingType.TEXT);}
}
