// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.notification.NotificationManager;
import java.io.IOException;
import java.util.List;

/** Expose completed drivers only; no decorative toggles that do not run a workflow. */
public final class AutomationModuleSettings {
 private AutomationModuleSettings(){}
 @FunctionalInterface private interface Change {void run()throws IOException;}
 private static void change(Change action){try{action.run();}catch(IOException|IllegalArgumentException error){NotificationManager.error("自動化設定未套用",error.getMessage()==null?"原設定保留":error.getMessage());}}
 public static List<ModulesTab.SubTab> trade(StashService stash){
  var group=SettingGroup.alwaysOn("Stash","領取待領物品；不會出售、丟棄、切換裝備或更動 Farming／Mining")
   .add(new ToggleSetting("自動領取 Stash",stash::autoEnabled,value->change(()->stash.setEnabled(value))))
   .add(new ToggleSetting("只在 Farming 執行中自動領取",()->stash.settings().stashPolicy().onlyWhileFarming(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),p.maxAttempts(),p.timeoutSeconds(),value));})))
   .add(new SliderSetting("領取間隔（秒）",2,60,()->(float)stash.settings().stashPolicy().delaySeconds(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(Math.round(value),p.maxAttempts(),p.timeoutSeconds(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("單次最多嘗試",1,30,()->(float)stash.settings().stashPolicy().maxAttempts(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),Math.round(value),p.timeoutSeconds(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("單次逾時（秒）",10,300,()->(float)stash.settings().stashPolicy().timeoutSeconds(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),p.maxAttempts(),Math.round(value),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new ActionSetting("立即領取 Stash",()->{if(!stash.manualRequest(System.nanoTime()))NotificationManager.error("Stash 未開始","請確認位於 SkyBlock、背包有空位、沒有 Mining／回程／選取任務，且沒有重複執行或設定錯誤");}))
   .add(new ActionSetting("停止本次領取",()->stash.cancel("手動停止本次領取")).visibleWhen(stash::active))
   .add(new InfoSetting("執行狀態",stash::status));
  return List.of(new ModulesTab.SubTab("Stash","自動或手動領取待領物品",stash::autoEnabled,value->change(()->stash.setEnabled(value)),List.of(group)));
 }
}
