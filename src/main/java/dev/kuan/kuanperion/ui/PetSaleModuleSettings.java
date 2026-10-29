// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.pets.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;import java.util.*;import java.io.IOException;import java.util.function.UnaryOperator;
public final class PetSaleModuleSettings {
 private PetSaleModuleSettings(){}
 private static void enabled(PetSaleService service,boolean v){try{service.setEnabled(v);}catch(IOException|IllegalArgumentException e){NotificationManager.error("Pet Sell 設定未套用",e.getMessage());}}
 private static void policy(PetSaleService service,UnaryOperator<PetSalePolicy> change){try{service.setPolicy(change.apply(service.policy()));}catch(IOException|IllegalArgumentException e){NotificationManager.error("Pet Sell 設定未套用",e.getMessage());}}
 public static Set<PetSalePreflight.Kind> parseKinds(String input){if(input==null||input.length()>4096)throw new IllegalArgumentException("寵物授權格式過長");var kinds=new HashSet<PetSalePreflight.Kind>();if(!input.isBlank())for(var entry:input.split("[,;]",-1)){var parts=entry.strip().split(":",-1);if(parts.length!=2||!kinds.add(new PetSalePreflight.Kind(parts[0],parts[1])))throw new IllegalArgumentException("格式：種類:稀有度，逗號分隔且不可重複");}if(kinds.size()>64)throw new IllegalArgumentException("最多 64 種授權");return Set.copyOf(kinds);}
 public static long number(String input){if(input==null||!input.strip().matches("[0-9]{1,13}"))throw new IllegalArgumentException("請輸入正整數");return Long.parseLong(input.strip());}
 private static String kinds(PetSalePolicy p){return String.join(", ",p.allowed().stream().map(k->k.type()+":"+k.rarity()).sorted().toList());}
 public static ModulesTab.SubTab tab(PetSaleService service){
  var group=SettingGroup.alwaysOn("Pet Sell","每次經 George 處理一隻明確授權的寵物；核對種類、稀有度、經驗與最低售價。綁定、裝備、收藏、附件與受保護寵物不動；未知成交結果停止並等待操作。")
   .add(new ToggleSetting("自動賣寵物",service::autoEnabled,v->enabled(service,v)))
   .add(new TextSetting("授權寵物種類與稀有度","SLUG:EPIC, RAT:COMMON",()->kinds(service.policy()),v->policy(service,p->p.withAllowed(parseKinds(v)))).withFieldWidth(390))
   .add(new TextSetting("最高經驗值（0 僅全新寵物）","0",()->Long.toString(service.policy().maximumExperience()),v->policy(service,p->p.withMaximumExperience(number(v)))).withFieldWidth(180))
   .add(new TextSetting("最低實收 Coins","1",()->Long.toString(service.policy().minimumPayout()),v->policy(service,p->p.withMinimumPayout(number(v)))).withFieldWidth(180))
   .add(new ToggleSetting("只在 Farming 執行中自動賣寵",()->service.policy().onlyWhileFarming(),v->policy(service,p->p.withOnlyFarming(v))))
   .add(new SliderSetting("自動執行間隔（秒）",20,86400,()->(float)service.policy().intervalSeconds(),v->policy(service,p->p.withInterval(Math.round(v)))).withDecimals(0))
   .add(new ActionSetting("關閉選單後賣一隻寵物",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Pet Sell 未排入","請先設定授權與最低價，確認目前安全且沒有其他任務，並等待設定的執行間隔。");}))
   .add(new ActionSetting("停止本次賣寵",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Pet Sell","依明確授權、最低價與伺服器成交回條賣寵",service::autoEnabled,v->enabled(service,v),List.of(group));
 }
}
