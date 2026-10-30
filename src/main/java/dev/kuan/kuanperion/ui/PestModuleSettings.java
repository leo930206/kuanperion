// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.pest.*;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.notification.NotificationManager;
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;
/** Core driver plus original independent notification settings; extensions arrive in later slices. */
public final class PestModuleSettings {
 private PestModuleSettings(){}
 private static void enabled(PestService service,boolean value){try{service.setEnabled(value);}catch(IOException invalid){NotificationManager.error("Pest 設定未套用",invalid.getMessage());}}
 private static void policy(PestService service,UnaryOperator<PestPolicy> edit){try{service.setPolicy(edit.apply(service.policy()));}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Pest 設定未套用",invalid.getMessage());}}
 private static long coins(String input){if(input==null||!input.matches("[0-9]{1,13}"))throw new IllegalArgumentException("請輸入 0～1000000000000 Coins");long n=Long.parseLong(input);if(n>1_000_000_000_000L)throw new IllegalArgumentException("預算過大");return n;}
 public static ModulesTab.SubTab tab(PestService service,SettingGroup notifications){
  var core=SettingGroup.alwaysOn("Pest","清理 Pest，可另開啟範圍搜尋；需 Garden 飛行權限與快捷列 Vacuum。返回原位才續耕，手動操作則停止。")
   .add(new ToggleSetting("自動清理 Pest",service::autoEnabled,value->enabled(service,value)))
   .add(new ToggleSetting("只在 Farming 執行中自動除蟲",()->service.policy().onlyWhileFarming(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),value,p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))))
   .add(new SliderSetting("除蟲門檻",1,8,()->(float)service.policy().threshold(),value->policy(service,p->new PestPolicy(Math.round(value),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new SliderSetting("最遠移動範圍（格）",16,96,()->(float)service.policy().radius(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),Math.round(value),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new SliderSetting("單次數量上限",1,64,()->(float)service.policy().maxTargets(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),Math.round(value),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new SliderSetting("捕捉逾時（秒）",5,30,()->(float)service.policy().captureSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),Math.round(value),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",30,240,()->(float)service.policy().timeoutSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),Math.round(value),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new SliderSetting("自動執行冷卻（秒）",10,3600,()->(float)service.policy().cooldownSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),Math.round(value),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot(),p.bait(),p.exchangeAfterCleanup(),p.barn()))).withDecimals(0))
   .add(new ToggleSetting("找不到 Pest 時搜尋範圍內四個位置",()->service.policy().sweep(),value->policy(service,p->p.withSweep(value))))
   .add(new SliderSetting("單次地塊傳送上限（0 為停用）",0,24,()->(float)service.policy().travelPlots(),value->policy(service,p->p.withTravelPlots(Math.round(value)))).withDecimals(0))
   .add(new ToggleSetting("清理已載入的滿載陷阱",()->service.policy().clearTraps(),value->policy(service,p->p.withClearTraps(value))))
   .add(new SliderSetting("陷阱所在地塊（-1 未設定、0 Barn）",-1,24,()->(float)service.policy().trapPlot(),value->policy(service,p->p.withTrapPlot(Math.round(value)))).withDecimals(0))
   .add(new ToggleSetting("處理缺餌陷阱（獨立授權）",()->service.policy().bait().enabled(),value->policy(service,p->p.withBait(p.bait().withEnabled(value)))))
   .add(new TextSetting("餌料物品 ID","例如 TASTY_CHEESE",()->service.policy().bait().rules().baitId(),value->policy(service,p->p.withBait(p.bait().withId(value.strip())))).withFieldWidth(240))
   .add(new SliderSetting("每個陷阱最多提取餌料（0 停用）",0,64,()->(float)service.policy().bait().rules().maximumPerTrap(),value->policy(service,p->p.withBait(p.bait().withPerTrap(Math.round(value))))).withDecimals(0))
   .add(new SliderSetting("整次最多提取餌料（0 停用）",0,1728,()->(float)service.policy().bait().rules().maximumRunItems(),value->policy(service,p->p.withBait(p.bait().withRun(Math.round(value))))).withDecimals(0))
   .add(new InfoSetting("補餌限制",()->"僅提取主背包內整疊指定餌料；超過上限不取用、不拆疊。缺料採購需另設原生 ID、庫存與整次預算，0 預算不購買。").multiline())
   .add(new TextSetting("購料原生物品 ID","minecraft:物品名稱",()->service.policy().bait().buying().minecraftId(),value->policy(service,p->p.withBait(p.bait().withBuying(p.bait().buying().withMinecraftId(value.strip()))))).withFieldWidth(240))
   .add(new SliderSetting("餌料原生堆疊上限",1,64,()->(float)service.policy().bait().buying().stackLimit(),value->policy(service,p->p.withBait(p.bait().withBuying(p.bait().buying().withStackLimit(Math.round(value)))))).withDecimals(0))
   .add(new SliderSetting("缺料時補到此庫存（0 不購料）",0,64,()->(float)service.policy().bait().buying().targetCount(),value->policy(service,p->p.withBait(p.bait().withBuying(p.bait().buying().withTargetCount(Math.round(value)))))).withDecimals(0))
   .add(new TextSetting("整次補餌購料預算（Coins，0 關閉）","0",()->Long.toString(service.policy().bait().buying().maximumCoins()),value->policy(service,p->p.withBait(p.bait().withBuying(p.bait().buying().withMaximumCoins(coins(value)))))).withFieldWidth(180))
   .add(new ToggleSetting("清理後兌換整袋 Pest（獨立授權）",()->service.policy().exchangeAfterCleanup(),value->policy(service,p->p.withExchangeAfterCleanup(value))))
   .add(new ToggleSetting("改用 Barn 實體 Phillip（獨立授權）",()->service.policy().barn().enabled(),value->policy(service,p->p.withBarn(p.barn().withEnabled(value)))))
   .add(new TextSetting("Barn 兌換桌座標 X Y Z（留空未設定）","X Y Z",()->service.policy().barn().deskText(),value->policy(service,p->p.withBarn(p.barn().withDeskText(value)))).withFieldWidth(240))
   .add(new InfoSetting("兌換限制",()->"預設 Abiphone 路線需 Phillip 聯絡人。另開 Barn 路線需明確桌面座標；跨地塊另需剩餘傳送額度，不會改設出生點。載入的 Phillip 類型／名稱／互動仍為暫定，待批次實服驗收；未知目標、選單、費用或回條即停止，不改走電話、不重試。回到本次原位才續耕。").multiline())
   .add(new ActionSetting("關閉選單後清理 Pest",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Pest 未開始","請確認 Pest 達門檻、Garden 允許飛行、快捷列有 Vacuum；未載入目標時需啟用範圍搜尋或設定地塊傳送上限；未載入陷阱需另設所在地塊，並避免其他動作或手動輸入。");}))
   .add(new ActionSetting("停止本次除蟲",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Pest","除蟲與獨立通知設定",service::autoEnabled,value->enabled(service,value),List.of(core,notifications));
 }
}
