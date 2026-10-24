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
 public static List<ModulesTab.SubTab> trade(StashService stash,AutoSellService sell){
  var group=SettingGroup.alwaysOn("Stash","領取待領物品；不會出售、丟棄、切換裝備或更動 Farming／Mining")
   .add(new ToggleSetting("自動領取 Stash",stash::autoEnabled,value->change(()->stash.setEnabled(value))))
   .add(new ToggleSetting("只在 Farming 執行中自動領取",()->stash.settings().stashPolicy().onlyWhileFarming(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),p.maxAttempts(),p.timeoutSeconds(),value));})))
   .add(new SliderSetting("領取間隔（秒）",2,60,()->(float)stash.settings().stashPolicy().delaySeconds(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(Math.round(value),p.maxAttempts(),p.timeoutSeconds(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("單次最多嘗試",1,30,()->(float)stash.settings().stashPolicy().maxAttempts(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),Math.round(value),p.timeoutSeconds(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("單次逾時（秒）",10,300,()->(float)stash.settings().stashPolicy().timeoutSeconds(),value->change(()->{var p=stash.settings().stashPolicy();stash.setPolicy(new StashTask.Policy(p.delaySeconds(),p.maxAttempts(),Math.round(value),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new ActionSetting("立即領取 Stash",()->{if(!stash.manualRequest(System.nanoTime()))NotificationManager.error("Stash 未開始","請確認位於 SkyBlock、背包有空位、沒有 Mining／回程／選取任務，且沒有重複執行或設定錯誤");}))
   .add(new ActionSetting("停止本次領取",()->stash.cancel("手動停止本次領取")).visibleWhen(stash::active))
   .add(new InfoSetting("執行狀態",stash::status));
  return List.of(autoSell(sell),new ModulesTab.SubTab("Stash","自動或手動領取待領物品",stash::autoEnabled,value->change(()->stash.setEnabled(value)),List.of(group)));
 }
 private static ModulesTab.SubTab autoSell(AutoSellService sell){
  var group=SettingGroup.alwaysOn("Auto Sell","僅 NPC 出售；空白名單不執行，保護物品優先排除。")
   .add(new ToggleSetting("自動 NPC 出售",sell::autoEnabled,value->change(()->sell.setEnabled(value))))
   .add(new TextSetting("物品 ID 白名單","WHEAT, CARROT_ITEM",()->String.join(", ",new java.util.TreeSet<>(sell.policy().allowedIds())),text->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(AutoSellPolicy.parseIds(text),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withFieldWidth(390))
   .add(new TextSetting("保護 UUID","逗號分隔",()->String.join(", ",new java.util.TreeSet<>(sell.policy().protectedUuids())),text->change(()->{var p=sell.policy();var ids=text.isBlank()?java.util.Set.<String>of():java.util.Set.copyOf(java.util.Arrays.asList(text.strip().split("[\\s,]+")));sell.setPolicy(new AutoSellPolicy(p.allowedIds(),ids,p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withFieldWidth(390))
   .add(new ToggleSetting("只在 Farming 執行中自動出售",()->sell.policy().onlyWhileFarming(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),value));})))
   .add(new SliderSetting("背包使用率門檻",1,100,()->(float)sell.policy().thresholdPercent(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),Math.round(value),p.onlyWhileFarming()));})).withDecimals(0).withSuffix("%"))
   .add(new SliderSetting("單次最多堆疊",1,27,()->(float)sell.policy().maxStacks(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),Math.round(value),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("單次最多物品",1,4096,()->(float)sell.policy().maxItems(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),Math.round(value),p.delayMillis(),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("操作間隔（秒）",.5f,5,()->sell.policy().delayMillis()/1000f,value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),Math.round(value*1000),p.timeoutSeconds(),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withDecimals(1))
   .add(new SliderSetting("逾時（秒）",10,180,()->(float)sell.policy().timeoutSeconds(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),Math.round(value),p.cooldownSeconds(),p.thresholdPercent(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new SliderSetting("自動執行冷卻（秒）",10,3600,()->(float)sell.policy().cooldownSeconds(),value->change(()->{var p=sell.policy();sell.setPolicy(new AutoSellPolicy(p.allowedIds(),p.protectedUuids(),p.maxStacks(),p.maxItems(),p.delayMillis(),p.timeoutSeconds(),Math.round(value),p.thresholdPercent(),p.onlyWhileFarming()));})).withDecimals(0))
   .add(new ActionSetting("立即 NPC 出售",()->{if(!sell.manualRequest(System.nanoTime()))NotificationManager.error("Auto Sell 未開始","請先設定精確物品 ID 白名單，確認有符合且未受保護的物品，並位於 SkyBlock 的安全狀態");}))
   .add(new ActionSetting("停止本次出售",()->sell.cancel(System.nanoTime())).visibleWhen(sell::active))
   .add(new InfoSetting("執行狀態",sell::status));
  return new ModulesTab.SubTab("Auto Sell","依精確白名單進行 NPC 出售",sell::autoEnabled,value->change(()->sell.setEnabled(value)),List.of(group));
 }

}
