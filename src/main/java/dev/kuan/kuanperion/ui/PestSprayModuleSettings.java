// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.pest.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;
import java.util.*;import java.io.IOException;import java.util.function.UnaryOperator;
/** Independent spray consent; capture/notifications do not enable this task or purchase materials. */
public final class PestSprayModuleSettings {
 private PestSprayModuleSettings(){}
 private static void enabled(PestSprayService s,boolean b){try{s.setEnabled(b);}catch(IOException|IllegalArgumentException e){NotificationManager.error("噴灑設定未套用",e.getMessage());}}
 private static void policy(PestSprayService s,UnaryOperator<PestSprayPolicy> edit){try{s.setPolicy(edit.apply(s.policy()),s.buying());}catch(IOException|IllegalArgumentException e){NotificationManager.error("噴灑設定未套用",e.getMessage());}}
 private static void buying(PestSprayService s,UnaryOperator<PestSprayBuying> edit){try{s.setPolicy(s.policy(),edit.apply(s.buying()));}catch(IOException|IllegalArgumentException e){NotificationManager.error("噴灑購料設定未套用",e.getMessage());}}
 private static long coins(String s){if(s==null||!s.matches("[0-9]{1,13}"))throw new IllegalArgumentException("請輸入 0～1000000000000 Coins");long n=Long.parseLong(s);if(n>1_000_000_000_000L)throw new IllegalArgumentException("預算超過上限");return n;}
 public static ModulesTab.SubTab tab(PestSprayService service){
  var core=SettingGroup.alwaysOn("Pest Spray","獨立噴灑本地塊；只暫停手動啟動中的 Farming，不移動、不自動開啟除蟲。")
   .add(new InfoSetting("開發版驗收限制",()->"五顆消耗界限為暫定，尚待當前伺服器噴嘴／成本／Sacks／選單與 Tab 驗收；不是已驗證實服功能。未驗收不發布或安裝。").multiline())
   .add(new ToggleSetting("自動噴灑（獨立授權）",service::autoEnabled,b->enabled(service,b)))
   .add(new TextSetting("噴料 SkyBlock ID","精確 ID",()->service.policy().materialId(),v->policy(service,p->new PestSprayPolicy(v.strip(),p.minecraftId(),p.materialName(),p.maximumUseItems(),p.acknowledgeSacks(),p.protectedUuids()))).withFieldWidth(250))
   .add(new TextSetting("噴料原生物品 ID","minecraft:物品名稱",()->service.policy().minecraftId(),v->policy(service,p->new PestSprayPolicy(p.materialId(),v.strip(),p.materialName(),p.maximumUseItems(),p.acknowledgeSacks(),p.protectedUuids()))).withFieldWidth(250))
   .add(new TextSetting("噴料精確英文名稱","須與唯一官方目錄名稱一致",()->service.policy().materialName(),v->policy(service,p->new PestSprayPolicy(p.materialId(),p.minecraftId(),v.strip(),p.maximumUseItems(),p.acknowledgeSacks(),p.protectedUuids()))).withFieldWidth(250))
   .add(new SliderSetting("單次材料消耗授權上限（0 停用）",0,64,()->(float)service.policy().maximumUseItems(),v->policy(service,p->new PestSprayPolicy(p.materialId(),p.minecraftId(),p.materialName(),Math.round(v),p.acknowledgeSacks(),p.protectedUuids()))).withDecimals(0))
   .add(new ToggleSetting("理解伺服器可能取用 Sacks（獨立確認）",()->service.policy().acknowledgeSacks(),v->policy(service,p->new PestSprayPolicy(p.materialId(),p.minecraftId(),p.materialName(),p.maximumUseItems(),v,p.protectedUuids()))))
   .add(new InfoSetting("執行限制",()->"需快捷列唯一 Sprayonator 與新鮮本地塊 None；不足五顆不得噴灑。確認材料變動、GAME 與 ACTIVE 後才還原工具及釋放原暫停。未知／手動／逾時即停止，不重試、不重啟 Farming。").multiline())
   .add(new SliderSetting("缺料補到此庫存（0 不買）",0,64,()->(float)service.buying().targetCount(),v->buying(service,b->new PestSprayBuying(Math.round(v),b.maximumCoins()))).withDecimals(0))
   .add(new TextSetting("單次購料預算（Coins，0 不買）","0",()->Long.toString(service.buying().maximumCoins()),v->buying(service,b->new PestSprayBuying(b.targetCount(),coins(v)))).withFieldWidth(180))
   .add(new InfoSetting("購料授權",()->"材料消耗授權不代表可花錢；庫存與預算皆明確設定才可在同一流程採購一次。不移用其他任務預算、不重買、不推測退款。").multiline())
   .add(new ActionSetting("關閉選單後噴灑一次",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("噴灑未開始","請確認明確授權、手動啟動中的 Farming、Garden、工具、材料與新鮮本地塊回條。");}))
   .add(new ActionSetting("停止本次噴灑",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Pest Spray","噴灑與獨立材料／購料授權",service::autoEnabled,b->enabled(service,b),List.of(core));
 }
}
