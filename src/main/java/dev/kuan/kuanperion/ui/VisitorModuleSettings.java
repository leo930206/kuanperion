// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.visitor.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;import java.io.IOException;import java.util.*;import java.util.function.UnaryOperator;
/** Inventory-backed scope is explicit; old Visitor notifications remain independent. */
public final class VisitorModuleSettings {
 private VisitorModuleSettings(){}
 private static void enabled(VisitorService s,boolean v){try{s.setEnabled(v);}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Visitor 設定未套用",invalid.getMessage());}}
 private static void policy(VisitorService s,UnaryOperator<VisitorPolicy> edit){try{s.setPolicy(edit.apply(s.policy()));}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Visitor 設定未套用",invalid.getMessage());}}
 private static VisitorPolicy rules(VisitorPolicy p,VisitorValuation.Rules r){return new VisitorPolicy(r,p.threshold(),p.maxVisitors(),p.timeoutSeconds(),p.cooldownSeconds(),p.onlyWhileFarming());}
 public static Set<String> parseList(String input){if(input==null||input.length()>8192)throw new IllegalArgumentException("清單過長");if(input.isBlank())return Set.of();var result=new TreeSet<String>();for(String token:input.split(";",-1)){String value=token.strip();if(value.isEmpty()||!result.add(value))throw new IllegalArgumentException("請用分號分隔，且不可留空或重複");}return Set.copyOf(result);}
 private static long coins(String input){if(input==null||!input.matches("[0-9]{1,13}"))throw new IllegalArgumentException("請輸入 0～1000000000000 Coins");long value=Long.parseLong(input);if(value>1000000000000L)throw new IllegalArgumentException("估價上限過大");return value;}
 public static ModulesTab.SubTab tab(VisitorService service,SettingGroup notifications){
  var core=SettingGroup.alwaysOn("Visitor","目前只提交背包已有的材料；依官方價格驗證整單價值，不會購料。確認伺服器收件並返回原位才續耕。")
   .add(new ToggleSetting("自動接單 Visitor",service::autoEnabled,v->enabled(service,v)))
   .add(new ToggleSetting("只在 Farming 執行中自動接單",()->service.policy().onlyWhileFarming(),v->policy(service,p->new VisitorPolicy(p.rules(),p.threshold(),p.maxVisitors(),p.timeoutSeconds(),p.cooldownSeconds(),v))))
   .add(new TextSetting("允許的 Visitor（分號分隔）","Librarian; Farmer",()->String.join("; ",new TreeSet<>(service.policy().rules().visitors())),v->policy(service,p->{var r=p.rules();return rules(p,new VisitorValuation.Rules(parseList(v),r.materialIds(),r.maximumOfferCoins(),r.maximumBuyCoins(),r.minimumCopper()));})).withFieldWidth(390))
   .add(new TextSetting("允許消耗的材料 ID（分號分隔）","ENCHANTED_WHEAT",()->String.join("; ",new TreeSet<>(service.policy().rules().materialIds())),v->policy(service,p->{var r=p.rules();return rules(p,new VisitorValuation.Rules(r.visitors(),parseList(v),r.maximumOfferCoins(),r.maximumBuyCoins(),r.minimumCopper()));})).withFieldWidth(390))
   .add(new TextSetting("整單材料估價上限（Coins）","0",()->Long.toString(service.policy().rules().maximumOfferCoins()),v->policy(service,p->{var r=p.rules();return rules(p,new VisitorValuation.Rules(r.visitors(),r.materialIds(),coins(v),r.maximumBuyCoins(),r.minimumCopper()));})).withFieldWidth(180))
   .add(new SliderSetting("最低 Copper",0,1000,()->(float)service.policy().rules().minimumCopper(),v->policy(service,p->{var r=p.rules();return rules(p,new VisitorValuation.Rules(r.visitors(),r.materialIds(),r.maximumOfferCoins(),r.maximumBuyCoins(),Math.round(v)));})).withDecimals(0))
   .add(new SliderSetting("自動接單人數門檻",1,5,()->(float)service.policy().threshold(),v->policy(service,p->new VisitorPolicy(p.rules(),Math.round(v),p.maxVisitors(),p.timeoutSeconds(),p.cooldownSeconds(),p.onlyWhileFarming()))).withDecimals(0))
   .add(new SliderSetting("單次接單上限",1,5,()->(float)service.policy().maxVisitors(),v->policy(service,p->new VisitorPolicy(p.rules(),p.threshold(),Math.round(v),p.timeoutSeconds(),p.cooldownSeconds(),p.onlyWhileFarming()))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",90,300,()->(float)service.policy().timeoutSeconds(),v->policy(service,p->new VisitorPolicy(p.rules(),p.threshold(),p.maxVisitors(),Math.round(v),p.cooldownSeconds(),p.onlyWhileFarming()))).withDecimals(0))
   .add(new SliderSetting("自動接單冷卻（秒）",30,86400,()->(float)service.policy().cooldownSeconds(),v->policy(service,p->new VisitorPolicy(p.rules(),p.threshold(),p.maxVisitors(),p.timeoutSeconds(),Math.round(v),p.onlyWhileFarming()))).withDecimals(0))
   .add(new ActionSetting("立即接單 Visitor",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Visitor 未開始","請確認清單與估價上限、Garden 飛行權限、訪客資料與背包材料，且沒有其他動作；兩次手動啟動需間隔 20 秒。");}))
   .add(new ActionSetting("停止本次接單",()->service.cancel(System.nanoTime())).visibleWhen(service::active))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Visitor","接單與獨立通知設定",service::autoEnabled,v->enabled(service,v),List.of(core,notifications));
 }
}
