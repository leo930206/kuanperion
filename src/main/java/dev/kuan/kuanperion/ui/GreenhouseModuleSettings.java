// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.AutomationGameView;import dev.kuan.kuanperion.automation.greenhouse.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;import java.io.IOException;import java.util.*;import java.util.function.UnaryOperator;import net.minecraft.client.Minecraft;
/** Explicit plot/crop/exact-tool consent. No guessed plot, tool by display name, or enable-on-migration. */
public final class GreenhouseModuleSettings {
 private GreenhouseModuleSettings(){}
 private static void enabled(GreenhouseService s,boolean v){try{s.setEnabled(v);}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Greenhouse 設定未套用",invalid.getMessage());}}
 private static void policy(GreenhouseService s,UnaryOperator<GreenhousePolicy> edit){try{s.setPolicy(edit.apply(s.policy()));}catch(IOException|IllegalArgumentException invalid){NotificationManager.error("Greenhouse 設定未套用",invalid.getMessage());}}
 public static Set<Integer> parsePlots(String input){
  if(input==null||input.length()>96)throw new IllegalArgumentException("請輸入 1～24 地塊編號，以逗號分隔");if(input.isBlank())return Set.of();var result=new TreeSet<Integer>();for(String token:input.split(",",-1)){if(!token.trim().matches("[0-9]{1,2}"))throw new IllegalArgumentException("請輸入 1～24 地塊編號，以逗號分隔");int id=Integer.parseInt(token.trim());if(id<1||id>24||!result.add(id))throw new IllegalArgumentException("地塊需介於 1～24，且不可重複");}return Set.copyOf(result);
 }
 public static ModulesTab.SubTab tab(GreenhouseService service){
  var core=SettingGroup.alwaysOn("Greenhouse","依設定的地塊收割成熟作物；需 Garden 飛行權限與指定工具，返回原位才續耕，手動操作則停止。")
   .add(new ToggleSetting("自動收割 Greenhouse",service::autoEnabled,v->enabled(service,v)))
   .add(new ToggleSetting("只在 Farming 執行中自動收割",()->service.policy().onlyWhileFarming(),v->policy(service,p->p.withOnlyFarming(v))))
   .add(new TextSetting("Greenhouse 地塊","1, 2",()->service.policy().plots().stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")),v->policy(service,p->p.withPlots(parsePlots(v)))).withFieldWidth(260));
  String[] labels={"Ashwreath","Turtellini","Glasscorn"};int index=0;
  for(var kind:GreenhouseIdentity.Kind.values()){String name=labels[index++];core.add(new ToggleSetting(name,()->service.policy().kinds().contains(kind),v->policy(service,p->{var kinds=EnumSet.noneOf(GreenhouseIdentity.Kind.class);kinds.addAll(p.kinds());if(v)kinds.add(kind);else kinds.remove(kind);return p.withKinds(kinds);})));}
  core.add(new ActionSetting("使用目前手持工具",()->{var mc=Minecraft.getInstance();if(!mc.isSameThread()||mc.player==null){NotificationManager.error("工具未設定","請先進入遊戲並手持收割工具");return;}var item=AutomationGameView.item(mc.player.getMainHandItem(),true);if(!item.identified()||item.count()!=1||item.uuid().isEmpty()){NotificationManager.error("工具未設定","請手持具有 SkyBlock ID 與 UUID 的單一收割工具");return;}policy(service,p->p.withTool(item.skyblockId(),item.uuid()));}))
   .add(new InfoSetting("指定工具",()->service.policy().toolId().isEmpty()?"尚未設定":service.policy().toolId()))
   .add(new ActionSetting("清空指定工具",()->policy(service,p->p.withTool("",""))))
   .add(new SliderSetting("掃描半徑（格）",4,25,()->(float)service.policy().radius(),v->policy(service,p->p.withRadius(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("單次收割上限",1,256,()->(float)service.policy().maxTargets(),v->policy(service,p->p.withMaxTargets(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("整體逾時（秒）",90,300,()->(float)service.policy().timeoutSeconds(),v->policy(service,p->p.withTimeout(Math.round(v)))).withDecimals(0))
   .add(new SliderSetting("巡查間隔（秒）",60,86400,()->(float)service.policy().intervalSeconds(),v->policy(service,p->p.withInterval(Math.round(v)))).withDecimals(0))
   .add(new ActionSetting("關閉選單後收割 Greenhouse",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Greenhouse 未開始","請確認地塊、作物與工具已設定、Garden 允許飛行、快捷列有指定工具，並避免其他動作或手動輸入。");}))
   .add(new ActionSetting("停止本次收割",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Greenhouse","成熟作物收割與返回",service::autoEnabled,v->enabled(service,v),List.of(core));
 }
}
