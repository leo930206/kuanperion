// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.ui.settings.*;import dev.kuan.kuanperion.notification.NotificationManager;
import java.io.IOException;import java.util.*;import java.util.function.UnaryOperator;
/** Actual one-pair Book service; no raw GUI/command authority in settings. */
public final class BookModuleSettings {
 private BookModuleSettings(){}
 private static void enabled(BookService service,boolean value){try{service.setEnabled(value);}catch(IOException|IllegalArgumentException failure){NotificationManager.error("Book Combine 設定未套用",failure.getMessage());}}
 private static void policy(BookService service,UnaryOperator<BookCombineTask.Policy> edit){try{service.setPolicy(edit.apply(service.policy()));}catch(IOException|IllegalArgumentException failure){NotificationManager.error("Book Combine 設定未套用",failure.getMessage());}}
 public static Map<String,Integer> parseCeilings(String input){
  if(input==null||input.length()>4096)throw new IllegalArgumentException("合書授權格式過長");if(input.isBlank())return Map.of();var result=new TreeMap<String,Integer>();
  for(String entry:input.split(",",-1)){String[] pair=entry.strip().split("=",-1);if(pair.length!=2||!pair[1].strip().matches("(?:[1-9]|10)")||result.put(pair[0].strip(),Integer.parseInt(pair[1].strip()))!=null)throw new IllegalArgumentException("格式：附魔 ID=最高等級，逗號分隔且不可重複");}
  new BookCombinePlanner(result,Set.of());return Map.copyOf(result);
 }
 private static String format(Map<String,Integer> values){return String.join(", ",new TreeMap<>(values).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toList());}
 public static ModulesTab.SubTab tab(BookService service){
  var group=SettingGroup.alwaysOn("Book Combine","每次只合成一組授權的同級單附魔書；工具、武器、快捷列及受保護物品不動。僅接受明確零經驗費用，未知格式即停止。")
   .add(new ToggleSetting("自動合書",service::autoEnabled,v->enabled(service,v)))
   .add(new TextSetting("附魔 ID 與最高等級","harvesting=5",()->format(service.policy().ceilings()),v->policy(service,p->new BookCombineTask.Policy(parseCeilings(v),p.protectedUuids(),p.delayMillis(),p.timeoutSeconds(),p.onlyWhileFarming()))).withFieldWidth(390))
   .add(new TextSetting("保護 UUID","逗號分隔",()->String.join(", ",new TreeSet<>(service.policy().protectedUuids())),v->policy(service,p->new BookCombineTask.Policy(p.ceilings(),v.isBlank()?Set.of():Set.copyOf(Arrays.asList(v.strip().split("[\\s,]+"))),p.delayMillis(),p.timeoutSeconds(),p.onlyWhileFarming()))).withFieldWidth(390))
   .add(new ToggleSetting("只在 Farming 執行中自動合書",()->service.policy().onlyWhileFarming(),v->policy(service,p->new BookCombineTask.Policy(p.ceilings(),p.protectedUuids(),p.delayMillis(),p.timeoutSeconds(),v))))
   .add(new SliderSetting("操作間隔（秒）",.5f,5,()->service.policy().delayMillis()/1000f,v->policy(service,p->new BookCombineTask.Policy(p.ceilings(),p.protectedUuids(),Math.round(v*1000),p.timeoutSeconds(),p.onlyWhileFarming()))).withDecimals(1))
   .add(new SliderSetting("單組逾時（秒）",10,180,()->(float)service.policy().timeoutSeconds(),v->policy(service,p->new BookCombineTask.Policy(p.ceilings(),p.protectedUuids(),p.delayMillis(),Math.round(v),p.onlyWhileFarming()))).withDecimals(0))
   .add(new ActionSetting("關閉選單後合書一組",()->{if(!service.manualRequest(System.nanoTime()))NotificationManager.error("Book Combine 未排入","請先設定附魔 ID 與最高等級，確認目前安全且沒有其他任務；兩次執行需間隔 20 秒。");}))
   .add(new ActionSetting("停止本次合書",()->service.cancel(System.nanoTime())).visibleWhen(()->service.active()||service.pending()))
   .add(new InfoSetting("執行狀態",service::status));
  return new ModulesTab.SubTab("Book Combine","依授權合併同級書；僅明確零經驗費用",service::autoEnabled,v->enabled(service,v),List.of(group));
 }
}
