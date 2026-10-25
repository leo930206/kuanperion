// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.composter.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;import java.io.IOException;import java.util.*;import java.util.function.UnaryOperator;
/** Inventory-source slice only. No hidden sack clicks, purchases, or invented material conversion values. */
public final class ComposterModuleSettings {
 private ComposterModuleSettings(){}
 private static void enabled(ComposterService s,boolean value){try{s.setEnabled(value);}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Composter 設定未套用",invalid.getMessage());}}
 private static void policy(ComposterService s,UnaryOperator<ComposterPolicy> edit){try{s.setPolicy(edit.apply(s.policy()));}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Composter 設定未套用",invalid.getMessage());}}
 public static Map<String,ComposterPolicy.Material> parseMaterials(String input,ComposterObservation.Kind kind,Map<String,ComposterPolicy.Material> previous){
  if(input==null||input.length()>2048)throw new IllegalArgumentException("請輸入 ITEM_ID=每件資源量，以逗號分隔");var result=new TreeMap<String,ComposterPolicy.Material>();previous.forEach((id,m)->{if(m.kind()!=kind)result.put(id,m);});
  if(!input.isBlank()){var seen=new HashSet<String>();for(var entry:input.split(",",-1)){var pair=entry.trim().split("=",-1);if(pair.length!=2||!pair[0].trim().matches("[A-Z0-9_]{1,64}")||!pair[1].trim().matches("[0-9]{1,10}")||!seen.add(pair[0].trim())||result.containsKey(pair[0].trim()))throw new IllegalArgumentException("材料 ID 需精確且不可重複，格式為 ITEM_ID=每件資源量");long units=Long.parseLong(pair[1].trim());result.put(pair[0].trim(),new ComposterPolicy.Material(kind,units));}}
  if(result.size()>32)throw new IllegalArgumentException("最多設定 32 種材料");return Map.copyOf(result);
 }
 private static String materials(ComposterService s,ComposterObservation.Kind kind){return s.policy().materials().entrySet().stream().filter(e->e.getValue().kind()==kind).sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+e.getValue().units()).collect(java.util.stream.Collectors.joining(", "));}
 public static ModulesTab.SubTab tab(ComposterService service){
  var group=SettingGroup.alwaysOn("Composter","使用背包內明確指定的材料補充；目前不自動購買或取用 Sacks。需設定 Barn 機器與每件材料的資源量，返回原位才續耕。")
   .add(new ToggleSetting("自動補充 Composter",service::autoEnabled,v->enabled(service,v)))
   .add(new ToggleSetting("只在 Farming 執行中自動補充",()->service.policy().onlyWhileFarming(),v->policy(service,p->p.withOnlyFarming(v))))
   .add(new ActionSetting("記錄準星指向的 Composter 方塊",()->{try{if(!service.captureAnchor())NotificationManager.error("Composter 位置未記錄","請在 Garden 的 Barn，將準星對準 Composter 機器方塊；需可辨識附近唯一的 Composter 標記。");}catch(IOException invalid){NotificationManager.error("Composter 設定未套用",invalid.getMessage());}}))
   .add(new InfoSetting("機器位置",()->service.policy().anchor()==null?"尚未記錄":service.policy().anchor().x()+", "+service.policy().anchor().y()+", "+service.policy().anchor().z()))
   .add(new TextSetting("Organic Matter 材料與單位值","ITEM_ID=每件資源量",()->materials(service,ComposterObservation.Kind.ORGANIC),v->policy(service,p->p.withMaterials(parseMaterials(v,ComposterObservation.Kind.ORGANIC,p.materials())))).withFieldWidth(390))
   .add(new TextSetting("Fuel 材料與單位值","ITEM_ID=每件資源量",()->materials(service,ComposterObservation.Kind.FUEL),v->policy(service,p->p.withMaterials(parseMaterials(v,ComposterObservation.Kind.FUEL,p.materials())))).withFieldWidth(390))
   .add(new SliderSetting("補充目標容量",1,100,()->(float)service.policy().targetPercent(),v->policy(service,p->p.withTargetPercent(Math.round(v)))).withDecimals(0).withSuffix("%"))
   .add(new SliderSetting("單次材料堆疊上限",1,27,()->(float)service.policy().maxStacks(),v->policy(service,p->p.withMaxStacks(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("單次材料數量上限",1,1728,()->(float)service.policy().maxItems(),v->policy(service,p->p.withMaxItems(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("補充間隔（毫秒）",500,5000,()->(float)service.policy().delayMillis(),v->policy(service,p->p.withDelay(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",90,300,()->(float)service.policy().timeoutSeconds(),v->policy(service,p->p.withTimeout(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("巡查間隔（秒）",60,86400,()->(float)service.policy().intervalSeconds(),v->policy(service,p->p.withInterval(Math.round(v)))).withDecimals(0))
   .add(new ActionSetting("立即補充 Composter",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Composter 未開始","請確認已記錄機器位置、設定材料與資源量、Garden 允許飛行，並避免其他動作或手動輸入。");}))
   .add(new ActionSetting("停止本次補充",()->service.cancel(System.nanoTime())).visibleWhen(service::active))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Composter","指定背包材料補充與返回",service::autoEnabled,v->enabled(service,v),List.of(group));
 }
}
