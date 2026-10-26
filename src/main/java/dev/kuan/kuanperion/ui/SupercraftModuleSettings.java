// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.supercraft.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;import java.io.IOException;import java.util.*;import java.util.function.UnaryOperator;
/** Explicit inventory-only single-batch scope; no buy, maximize or guessed quantity dialog. */
public final class SupercraftModuleSettings {
 private SupercraftModuleSettings(){}
 private static void enabled(SupercraftService service,boolean value){try{service.setEnabled(value);}catch(IOException|IllegalArgumentException failure){NotificationManager.error("Supercraft 設定未套用",failure.getMessage());}}
 private static void policy(SupercraftService service,UnaryOperator<SupercraftPolicy> edit){try{service.setPolicy(edit.apply(service.policy()));}catch(IOException|IllegalArgumentException failure){NotificationManager.error("Supercraft 設定未套用",failure.getMessage());}}
 private static SupercraftPolicy task(SupercraftPolicy p,UnaryOperator<SupercraftTask.Policy> edit){return new SupercraftPolicy(edit.apply(p.task()),p.intervalSeconds());}
 private static SupercraftTask.Policy rules(SupercraftTask.Policy p,int output,int consumed,int crafts){var r=p.rules();return new SupercraftTask.Policy(p.recipe(),new SupercraftPlan.Rules(r.outputs(),r.ingredients(),output,consumed,crafts,r.protectedUuids()),p.outputCount(),p.timeoutSeconds(),p.onlyWhileFarming());}
 public static SupercraftPlan.Recipe parseRecipe(String input){if(input==null||input.length()>2048)throw new IllegalArgumentException("配方過長");String[] parts=input.split(";",-1);if(parts.length!=4)throw new IllegalArgumentException("格式：產物 ID;每次產量;堆疊上限;材料 ID=每次數量（逗號分隔）");var inputs=new TreeMap<String,Integer>();for(String entry:parts[3].split(",",-1)){String[] pair=entry.strip().split("=",-1);if(pair.length!=2||inputs.put(pair[0].strip(),number(pair[1].strip(),4096))!=null)throw new IllegalArgumentException("材料不可重複或留空");}return new SupercraftPlan.Recipe(parts[0].strip(),number(parts[1].strip(),64),number(parts[2].strip(),64),inputs);}
 private static int number(String text,int max){if(!text.matches("[0-9]{1,5}"))throw new IllegalArgumentException("請輸入範圍內整數");int value=Integer.parseInt(text);if(value>max)throw new IllegalArgumentException("超出數量上限");return value;}
 private static String format(SupercraftPlan.Recipe r){if(r==null)return "";return r.outputId()+";"+r.outputPerCraft()+";"+r.stackLimit()+";"+String.join(",",new TreeMap<>(r.inputsPerCraft()).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toList());}
 public static ModulesTab.SubTab tab(SupercraftService service){
  var group=SettingGroup.alwaysOn("Supercraft","目前僅用背包材料，執行一批已符合數量的 Advanced Supercraft；來源、費用或選單不符即停止，不會購料或猜測數量視窗。")
   .add(new ToggleSetting("自動 Supercraft",service::autoEnabled,v->enabled(service,v)))
   .add(new ToggleSetting("只在 Farming 執行中自動合成",()->service.policy().task().onlyWhileFarming(),v->policy(service,p->task(p,t->new SupercraftTask.Policy(t.recipe(),t.rules(),t.outputCount(),t.timeoutSeconds(),v)))))
   .add(new TextSetting("配方（產物;產量;堆疊;材料=數量）","ENCHANTED_WHEAT;1;64;WHEAT=160",()->format(service.policy().task().recipe()),v->policy(service,p->task(p,t->{var recipe=v.isBlank()?null:parseRecipe(v);var r=t.rules();var next=new SupercraftPlan.Rules(recipe==null?Set.of():Set.of(recipe.outputId()),recipe==null?Set.of():recipe.inputsPerCraft().keySet(),r.maxOutput(),r.maxConsumed(),r.maxCrafts(),r.protectedUuids());return new SupercraftTask.Policy(recipe,next,t.outputCount(),t.timeoutSeconds(),t.onlyWhileFarming());}))).withFieldWidth(390))
   .add(new TextSetting("本批產物數量","0",()->Integer.toString(service.policy().task().outputCount()),v->policy(service,p->task(p,t->new SupercraftTask.Policy(t.recipe(),t.rules(),number(v,4096),t.timeoutSeconds(),t.onlyWhileFarming())))).numeric())
   .add(new SliderSetting("單批產物上限",0,4096,()->(float)service.policy().task().rules().maxOutput(),v->policy(service,p->task(p,t->rules(t,Math.round(v),t.rules().maxConsumed(),t.rules().maxCrafts())))).withDecimals(0))
   .add(new SliderSetting("單批材料消耗上限",0,65536,()->(float)service.policy().task().rules().maxConsumed(),v->policy(service,p->task(p,t->rules(t,t.rules().maxOutput(),Math.round(v),t.rules().maxCrafts())))).withDecimals(0))
   .add(new SliderSetting("單批合成次數上限",0,4096,()->(float)service.policy().task().rules().maxCrafts(),v->policy(service,p->task(p,t->rules(t,t.rules().maxOutput(),t.rules().maxConsumed(),Math.round(v))))).withDecimals(0))
   .add(new SliderSetting("自動合成間隔（秒）",30,86400,()->(float)service.policy().intervalSeconds(),v->policy(service,p->new SupercraftPolicy(p.task(),Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",60,180,()->(float)service.policy().task().timeoutSeconds(),v->policy(service,p->task(p,t->new SupercraftTask.Policy(t.recipe(),t.rules(),t.outputCount(),Math.round(v),t.onlyWhileFarming())))).withDecimals(0))
   .add(new ActionSetting("關閉選單後執行一批",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Supercraft 未排入","請完成配方與上限設定；兩次執行需間隔 20 秒。關閉選單後開始，30 秒後失效。");}))
   .add(new ActionSetting("停止／取消本批合成",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Supercraft","背包材料單批合成；不符伺服器預覽即停止",service::autoEnabled,v->enabled(service,v),List.of(group));
 }
}
