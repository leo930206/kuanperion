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
 public static ModulesTab.SubTab tab(PestService service,SettingGroup notifications){
  var core=SettingGroup.alwaysOn("Pest","清理 Pest，可另開啟範圍搜尋；需 Garden 飛行權限與快捷列 Vacuum。返回原位才續耕，手動操作則停止。")
   .add(new ToggleSetting("自動清理 Pest",service::autoEnabled,value->enabled(service,value)))
   .add(new ToggleSetting("只在 Farming 執行中自動除蟲",()->service.policy().onlyWhileFarming(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),value,p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))))
   .add(new SliderSetting("除蟲門檻",1,8,()->(float)service.policy().threshold(),value->policy(service,p->new PestPolicy(Math.round(value),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new SliderSetting("最遠移動範圍（格）",16,96,()->(float)service.policy().radius(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),Math.round(value),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new SliderSetting("單次數量上限",1,64,()->(float)service.policy().maxTargets(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),Math.round(value),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new SliderSetting("捕捉逾時（秒）",5,30,()->(float)service.policy().captureSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),Math.round(value),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",30,240,()->(float)service.policy().timeoutSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),Math.round(value),p.captureSeconds(),p.maxTargets(),p.radius(),p.cooldownSeconds(),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new SliderSetting("自動執行冷卻（秒）",10,3600,()->(float)service.policy().cooldownSeconds(),value->policy(service,p->new PestPolicy(p.threshold(),p.timeoutSeconds(),p.captureSeconds(),p.maxTargets(),p.radius(),Math.round(value),p.onlyWhileFarming(),p.sweep(),p.travelPlots(),p.clearTraps(),p.trapPlot()))).withDecimals(0))
   .add(new ToggleSetting("找不到 Pest 時搜尋範圍內四個位置",()->service.policy().sweep(),value->policy(service,p->p.withSweep(value))))
   .add(new SliderSetting("單次地塊傳送上限（0 為停用）",0,24,()->(float)service.policy().travelPlots(),value->policy(service,p->p.withTravelPlots(Math.round(value)))).withDecimals(0))
   .add(new ToggleSetting("清理已載入的滿載陷阱",()->service.policy().clearTraps(),value->policy(service,p->p.withClearTraps(value))))
   .add(new SliderSetting("陷阱所在地塊（-1 未設定、0 Barn）",-1,24,()->(float)service.policy().trapPlot(),value->policy(service,p->p.withTrapPlot(Math.round(value)))).withDecimals(0))
   .add(new ActionSetting("立即清理 Pest",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Pest 未開始","請確認 Pest 達門檻、Garden 允許飛行、快捷列有 Vacuum；未載入目標時需啟用範圍搜尋或設定地塊傳送上限；未載入陷阱需另設所在地塊，並避免其他動作或手動輸入。");}))
   .add(new ActionSetting("停止本次除蟲",()->service.cancel(System.nanoTime())).visibleWhen(service::active))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Pest","除蟲與獨立通知設定",service::autoEnabled,value->enabled(service,value),List.of(core,notifications));
 }
}
