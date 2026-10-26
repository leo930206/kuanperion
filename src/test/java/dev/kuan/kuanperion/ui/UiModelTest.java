package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.*;
import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.greenhouse.*;
import dev.kuan.kuanperion.automation.composter.*;
import dev.kuan.kuanperion.automation.visitor.*;
import java.util.*;
public final class UiModelTest {
    public static void main(String[] args) throws Exception {
        testPestPendingUi();
        testMaintenancePendingUi();
        var recipe=SupercraftModuleSettings.parseRecipe("ENCHANTED_WHEAT;1;64;WHEAT=160");
        require(recipe.outputId().equals("ENCHANTED_WHEAT")&&recipe.inputsPerCraft().equals(Map.of("WHEAT",160)),"explicit exact recipe UI, not a display-name guess");
        for(String bad:List.of("","WHEAT;1;64;WHEAT=1","A;1;64;WHEAT=1,WHEAT=2","A;1;64;WHEAT=1.5","A;1;64;WHEAT=0","A;1;64;WHEAT=1,","A;1;64;WHEAT=1;barn","A;65;64;WHEAT=1"))try{SupercraftModuleSettings.parseRecipe(bad);throw new AssertionError("invalid Supercraft recipe accepted");}catch(IllegalArgumentException expected){}
        var supplies=ComposterModuleSettings.parseMaterials("BOX=1000",dev.kuan.kuanperion.automation.composter.ComposterObservation.Kind.ORGANIC,Map.of());
        require(supplies.get("BOX").units()==1000,"explicit unit value, no inferred conversion");
        require(ComposterModuleSettings.parseMaterials("",dev.kuan.kuanperion.automation.composter.ComposterObservation.Kind.ORGANIC,supplies).isEmpty(),"clear material list removes consent");
        for(String bad:List.of("Box=1","BOX=0","BOX=1000000001","BOX=1,BOX=2","BOX; barn=1","BOX=1.2","BOX=1,"))try{ComposterModuleSettings.parseMaterials(bad,dev.kuan.kuanperion.automation.composter.ComposterObservation.Kind.ORGANIC,Map.of());throw new AssertionError("invalid Composter materials");}catch(IllegalArgumentException expected){}
        require(GreenhouseModuleSettings.parsePlots("1, 24, 2").equals(Set.of(1,2,24)),"Greenhouse explicit bounded plot parser");
        require(GreenhouseModuleSettings.parsePlots(" ").isEmpty(),"blank plots is unconfigured, not all plots");
        for(String bad:List.of("0","25","1,1","1,","1; barn","1\n2","-1"))try{GreenhouseModuleSettings.parsePlots(bad);throw new AssertionError("invalid plot input accepted");}catch(IllegalArgumentException expected){}

        require(java.util.Arrays.equals(MainGUISettingsPages.IDS,new int[]{4,1,3}),"only Interface Colors Keybinds remain");
        require(MainGUISettingsPages.normalize(2)==4,"old profile route goes to interface");
        for(float width:new float[]{350,600,900}){
            float bx=InlineRowLayout.keybindBindX(12,width),bw=InlineRowLayout.keybindWidth(width),cx=InlineRowLayout.keybindClearX(12,width);
            require(bx+bw+8==cx&&cx+52+8==12+width-60,"bind / clear / reset have independent nonoverlapping hit regions");
        }
        require(MainGUISidebar.contains(180,120,20,20,600,194,1),"expanded sidebar owns covered content");
        require(!MainGUISidebar.contains(214,120,20,20,600,194,1),"right edge belongs to content");
        require(!MainGUISidebar.contains(180,120,20,20,600,194,0),"collapsed sidebar leaves content interactive");
        require(MainGUISidebar.contains(100,120,20,20,600,194,.5f),"animated width owns only visible sidebar");
        require(!MainGUISidebar.contains(160,120,20,20,600,194,.5f),"animation does not block uncovered content");
        var singleTarget=new BlockTargetSetting("順位 2",()->"minecraft:prismarine",()->"Prismarine",()->{},()->{},()->{},()->true,()->false);
        require(singleTarget.displayLabel().equals("順位 2: Prismarine"),"priority and localized block on one line");
        require(dev.kuan.kuanperion.ui.theme.ThemePreset.values().length==6,"exactly six built-in palettes");
        for(var preset:dev.kuan.kuanperion.ui.theme.ThemePreset.values()) {
            require(preset.label().matches("[A-Za-z ]+"),"built-in names stay English");
            require(dev.kuan.kuanperion.util.UiLang.localize(preset.label()).equals(preset.label()),"renderer localization must also keep preset name English");
            var json=com.google.gson.JsonParser.parseString(preset.json()).getAsJsonObject();
            require(json.get("Main Panel BG").getAsString().equals("FFFFFFFF"),"built-in themes keep a white main canvas");
            require(!json.has("uiScale")&&!json.has("textScale")&&!json.has("animTimeMs"),"color presets must not overwrite layout");
            require(json.size()==29,"each preset defines 25 colors and four gradient/schema fields");
        }
        for(float width:new float[]{150,300,600,1000}) {
            int columns=ThemePresetRow.columns(width);float buttonW=ThemePresetRow.buttonWidth(width);
            require(columns>=1&&columns<=6&&columns*buttonW+(columns-1)*8<=width+.01f,"six preset buttons wrap without overflow");
            for(int i=0;i<6;i++)require(ThemePresetRow.buttonIndex(10+i%columns*(buttonW+8)+buttonW/2,20+i/columns*48+20,10,20,width)==i,"preset hit regions follow current layout rather than stale frame click areas");
            require(ThemePresetRow.buttonIndex(10+buttonW/2,62,10,20,width)==-1,"vertical gap does not apply preset");
        }

        require(MainGUISidebar.entries().stream().map(MainGUISidebar.Entry::label).toList().equals(List.of("Farming","Mining","Trade","Other","Setting")),"Trade between Mining and Other");
        for(int i=0;i<4;i++) {
            var entry=MainGUISidebar.entries().get(i);
            require(entry.mainTab()==0 && entry.moduleFilter()==i+1,"feature sidebar routes to matching module group");
            require(MainGUISidebar.selectedIndex(0,i+1)==i,"feature highlight matches group");
        }
        for(int page=1;page<=4;page++) require(MainGUISidebar.selectedIndex(page,0)==4,"all settings pages share one gear");
        require(MainGUISidebar.selectedIndex(5,0)==-1,"About belongs to footer, not settings");
        require(MainGUISidebar.footerLines("2.0.8").equals(List.of("Mod Version 2.0.8")),"single bottom version line without duplicate name");
        require(MainGUISidebar.footerLines("9.1.0").get(0).equals("Mod Version 9.1.0"),"footer is not hardcoded");
        require(MainGUISidebar.footerY(20,660)==656,"footer is a compact bottom caption");
        require(MainGUISidebar.rowY(4,20,660)+44<=MainGUISidebar.footerY(20,660),"Setting cannot overlap caption");
        require(MainGUISidebar.footerContains(160,667,20,20,660,194,1),"expanded version caption opens About across its text width");
        require(MainGUISidebar.footerContains(50,667,20,20,660,60,0),"visible collapsed version opens About");
        require(!MainGUISidebar.footerContains(50,650,20,20,660,194,1),"old footer icon area above caption is not clickable");
        require(!MainGUISidebar.footerContains(31,667,20,20,660,194,1),"caption left padding excluded");
        require(!MainGUISidebar.footerContains(203,667,20,20,660,194,1),"caption right padding excluded");
        require(MainGUISidebar.footerText("2.0.9",false).equals("2.0.9"),"collapsed shows only version");
        require(MainGUISidebar.rowY(4,20,660)+44==MainGUISidebar.footerY(20,660),"Setting/footer spacing reduced by eight pixels");
        for(float width:new float[]{60,100,194}) require(MainGUISidebar.footerTextX(20,width,30)+15==20+width/2,"caption horizontally centered at every width");
        for(float rowHeight:new float[]{50,55,68,94}) {
            float top=UiLayoutMetrics.checkboxTop(100,rowHeight);
            require(Math.abs(top+10.5f-(100+(rowHeight-5)/2))<.001f,"checkbox centered in complete card, with or without wrapped description");
        }
        var metadata=com.google.gson.JsonParser.parseString(new String(UiModelTest.class.getResourceAsStream("/fabric.mod.json").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        require(dev.kuan.kuanperion.renderer.NVGRenderer.MOD_ICON_PATH.equals("/"+metadata.get("icon").getAsString()),"sidebar uses same avatar asset as Fabric mod icon");
        try(var png=UiModelTest.class.getResourceAsStream(dev.kuan.kuanperion.renderer.NVGRenderer.MOD_ICON_PATH)){
            var icon=javax.imageio.ImageIO.read(png);require(icon!=null&&icon.getWidth()==icon.getHeight()&&icon.getWidth()>=8,"mod avatar is valid square PNG");
        }
        require(UiLayoutMetrics.inputHeight()==22,"compact input default");
        require(UiLayoutMetrics.descriptionTop(13.5f)-UiLayoutMetrics.TITLE_TOP-13.5f==6,"uniform six-pixel description gap");
        require(UiLayoutMetrics.inputFont()==12,"larger input font default");
        require(UiLayoutMetrics.inputTop(100,68)+UiLayoutMetrics.inputHeight()/2==131.5f,"input centered against full card");
        require(UiLayoutMetrics.descriptionGap()==6,"group and setting share description gap");
        var helpToggle=new ToggleSetting("工具偵測",()->false,v->{});var metrics=new MainGUITextLayout();
        float controlH=metrics.settingHeight(helpToggle,600)-5-metrics.descriptionHeight(helpToggle,600);
        require(controlH==2*UiLayoutMetrics.TITLE_TOP+metrics.settingLabelFontSize(helpToggle),"title no longer vertically centers far above description");
        UiLayoutMetrics.setInputHeight(Float.NaN);require(UiLayoutMetrics.inputHeight()==22,"invalid height resets default");
        UiLayoutMetrics.setInputHeight(100);require(UiLayoutMetrics.inputHeight()==24,"input height bounded");UiLayoutMetrics.reset();
        require(UiLayoutMetrics.migratedInputHeight(2,16)==16,"upgrade preserves stored input height");
        require(UiLayoutMetrics.migratedInputHeight(2,18)==18,"custom old height retained");
        require(UiLayoutMetrics.migratedInputHeight(3,16)==16,"explicit new height retained");
        var paletteDir=java.nio.file.Files.createTempDirectory("preset-ui-");
        try{
            var palette=com.google.gson.JsonParser.parseString(dev.kuan.kuanperion.ui.theme.ThemePreset.SKY_BLOSSOM.json()).getAsJsonObject();
            var store=new dev.kuan.kuanperion.ui.theme.ColorPresetStore(paletteDir);int[] applied={0};
            var preset=new ColorPresetSetting(1,store,()->palette,j->applied[0]++);
            require(!preset.load()&&applied[0]==0,"empty slot no-op");
            require(!preset.isVisible(),"empty palette collection has no numbered rows");
            require(preset.save(1000)==ColorPresetSetting.SaveResult.SAVED,"first save");
            require(preset.isVisible()&&!preset.previewColors().isEmpty(),"saved palette row has swatches");
            for(float rowWidth:new float[]{220,350,600,1000}) {
                require(ColorPresetRow.height(preset,rowWidth)==50,"saved palette stays one row at every width");
                require(ColorPresetRow.visibleCount(preset,rowWidth-24)==Math.min(preset.previewColors().size(),ColorPresetRow.columns(rowWidth-24)),"only fitting colors are previewed");
            }
            require(ColorPresetRow.visibleCount(preset,300)<preset.previewColors().size(),"overflow swatches are hidden, never wrapped");
            require(ColorPresetRow.columns(100)==0,"no forced swatch can overlap narrow control area");
            require(preset.load()&&applied[0]==1,"one-click load");
            require(preset.save(2000)==ColorPresetSetting.SaveResult.CONFIRM,"overwrite confirmation");
            require(preset.save(8000)==ColorPresetSetting.SaveResult.CONFIRM,"confirmation expires");
            require(preset.save(8100)==ColorPresetSetting.SaveResult.SAVED,"second click saves");
            preset.delete();require(!preset.isVisible()&&preset.previewColors().isEmpty(),"delete hides row without touching current theme");
        }finally{try(var paths=java.nio.file.Files.walk(paletteDir)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}}
        boolean[] notify={false};String[] seconds={""};
        var combined=new InlineToggleSetting("螢幕通知","顯示時間",()->seconds[0],v->seconds[0]=v,()->notify[0],v->notify[0]=v);
        require(metrics.descriptionWidth(combined,600)+12+50+combined.fieldWidth(600-24-50)<=600-24,"description reserves full centered input column");
        require(combined.getValue().isEmpty() && combined.getPlaceholder().equals("顯示時間"),"blank duration placeholder");
        combined.setValue("2.5");require(seconds[0].equals("2.5")&&!notify[0],"duration editing does not toggle");
        combined.toggle();require(notify[0]&&seconds[0].equals("2.5"),"toggle does not edit duration");
        int[] count={2},starts={0},slots={7};
        var selection=new SelectionActionSetting("選擇種類數量",()->(float)count[0],v->count[0]=Math.round(v),()->starts[0]++,()->slots[0]);
        for(float width:new float[]{400,600,1000})require(12+metrics.descriptionWidth(selection,width)<=InlineRowLayout.countFieldX(12,width-24)-12,"selection description stays left of count/action controls");
        selection.setValue(9);require(count[0]==7&&selection.getMax()==7&&starts[0]==0,"count clamps without starting selection");selection.start();require(starts[0]==1,"paired action callback");
        slots[0]=1;require(selection.getMax()==1&&selection.getValue()==1,"remaining slots update live");
        slots[0]=0;require(selection.getMin()==0&&selection.getMax()==0&&selection.getValue()==0&&!selection.canStart(),"full list capacity zero");
        selection.start();require(starts[0]==1,"full list start disabled");
        slots[0]=10;selection.setValue(100);require(count[0]==10&&selection.getMax()==10,"empty allows ten");
        selection.setValue(Float.NaN);require(count[0]==10,"ignore nonfinite");
        var hidden=new BlockTargetSetting("優先序 1",()->"minecraft:stone",()->"石頭",()->{},()->{},()->{},()->false,()->false).visibleWhen(()->false);
        require(!hidden.isVisible(),"unselected target has no phantom row");
        require(!SettingDescriptionCatalog.describe(new ToggleSetting("工具偵測",()->false,v->{})).equals("工具偵測"),"real help text not repeated label");
        require(new MainGUITextLayout().estimateWrappedLineCount("漢".repeat(15),100,10)>=2,"CJK help reserves full-width rows");
        float[] number={3};
        var slider=new SliderSetting("半徑",1,8,()->number[0],v->number[0]=v).withDecimals(0);
        slider.setValue(100); require(number[0]==8,"upper bound");slider.setValue(-1);require(number[0]==1,"lower bound");slider.setValue(Float.NaN);require(number[0]==1,"reject non-finite slider");
        boolean[] enabled={false};var toggle=new ToggleSetting("通知",()->enabled[0],v->enabled[0]=v);toggle.toggle();require(enabled[0],"live toggle");toggle.toggle();require(!enabled[0],"toggle off");
        var group=SettingGroup.of("模組","說明",()->enabled[0],v->enabled[0]=v).add(slider);group.toggle();require(enabled[0]&&group.getSettings().get(0)==slider,"group binding");
        require(MainGUIInlineTextEditor.moveCursorWord("one two",7,-1)==4,"word left");
        require(MainGUIInlineTextEditor.moveCursorWord("one two",0,1)==4,"word right");
        require(MainGUIInlineTextEditor.moveCursorWord("one two",-99,-1)==0,"negative caret");
        require(MainGUIInlineTextEditor.moveCursorWord(null,5,-1)==0,"null text");
        require(MainGUIInlineTextEditor.getLineStart("甲乙\n丙丁",4)==3,"Chinese line start");
        require(UiTextWrap.wrap("甲乙丙丁",2,String::length).equals(List.of("甲乙","丙丁")),"first long CJK token wraps");
        require(UiTextWrap.wrap("abcdef gh",3,String::length).equals(List.of("abc","def","gh")),"long word wraps before spaces");
        var animation=new SelectionAnimation();animation.reset(0,1_000_000_000L);
        float middle=animation.update(100,250,1_050_000_000L);require(middle>0&&middle<100,"selection spring midpoint");
        require(Math.abs(animation.update(100,250,2_050_000_000L)-100)<.01,"selection spring completion");
        var offModule=new ModulesTab.SubTab("Mining","test",()->false,v->{},List.of(group));
        var editable=MainGUI.class.getDeclaredMethod("isConfigurationEditable",ModulesTab.SubTab.class);editable.setAccessible(true);
        require((Boolean)editable.invoke(null,offModule),"can configure a stopped feature");
        var layout=new MainGUILayout().computeFrameLayout(960,640,2,1.5f,60);
        require(layout.pw==980&&layout.ph==610&&layout.contW==920,"reference panel geometry");
        require(UiScalePolicy.effective(320,240,3)<=.5f,"small-window fit");
        require(UiScalePolicy.effective(1920,1280,1.5f)==1.5f,"normal scale retained");
        require(Float.isFinite(UiScalePolicy.effective(1920,1280,Float.NaN)),"malformed scale");
        require(!MainGUI.hasTopFilterBar(1)&&!MainGUI.hasTopFilterBar(2),"no color/profile subfilter strips");
        var numberField=new TextSetting("number","",()->"300",v->{}).numeric();
        require(numberField.fieldWidth(600)==66&&numberField.centered(),"numeric field matches threshold width and alignment");
        require(new TextSetting("webhook","",()->"",v->{}).withFieldWidth(390).fieldWidth(600)==390,"wide webhook field");
        for(int[] size:new int[][]{{320,240},{1100,760},{1920,1080},{640,480},{1920,1080}}){
            float scale=UiScalePolicy.effective(size[0],size[1],1.5f);
            var frame=new MainGUILayout().computeFrameLayout(size[0],size[1],1,scale,60);
            require(Math.abs(frame.px+frame.pw/2-frame.physW/scale/2)<.01f && Math.abs(frame.py+frame.ph/2-frame.physH/scale/2)<.01f,"resize roundtrip always centers");
        }
        for(float width:new float[]{360,600,1000}){
            require(ColorPresetRow.saveX(12,width)>12,"reserved swatch area");
            require(ColorPresetRow.saveX(12,width)+ColorPresetRow.BUTTON_W+ColorPresetRow.BUTTON_GAP==ColorPresetRow.deleteX(12,width),"save/delete gap");
            require(ColorPresetRow.columns(width)*(ColorPresetRow.SWATCH+ColorPresetRow.SWATCH_GAP)-ColorPresetRow.SWATCH_GAP<=ColorPresetRow.saveX(0,width)-16,"swatches do not overlap controls");
        }
        System.out.println("UiModelTest: PASS (live bindings, bounds, Chinese text, word navigation, original spring animation, geometry, adaptive scale)");
    }
    private static void testPestPendingUi() throws Exception {
        var path=java.nio.file.Files.createTempDirectory("pest-pending-ui-");
        try {
            var context=new dev.kuan.kuanperion.automation.AutomationContext(1,true,true,Set.of());
            var session=new dev.kuan.kuanperion.automation.AutomationSession(new dev.kuan.kuanperion.automation.AutomationCoordinator(),()->context,()->{throw new AssertionError("UI must not pause Farming");},()->{throw new AssertionError("UI must not stop Farming");});
            var port=new dev.kuan.kuanperion.automation.pest.PestTask.Port(){
                public dev.kuan.kuanperion.automation.AutomationContext context(){return context;}
                public dev.kuan.kuanperion.automation.pest.PestTask.Frame frame(long now){throw new AssertionError("UI must not inspect/replay flight target");}
                public boolean prepare(dev.kuan.kuanperion.automation.AutomationSession.Handle h,dev.kuan.kuanperion.automation.pest.PestIdentity.Vacuum v,dev.kuan.kuanperion.automation.pest.PestTask.Anchor a,int r,long n){throw new AssertionError("no flight prepare from UI");}
                public dev.kuan.kuanperion.automation.pest.PestTask.Motion move(dev.kuan.kuanperion.automation.AutomationSession.Handle h,dev.kuan.kuanperion.automation.pest.PestTargets.Point p,double t,long n){throw new AssertionError("no UI movement");}
                public dev.kuan.kuanperion.automation.pest.PestTask.Capture capture(dev.kuan.kuanperion.automation.AutomationSession.Handle h,dev.kuan.kuanperion.automation.pest.PestTargets.Target t,dev.kuan.kuanperion.automation.pest.PestIdentity.Vacuum v,long n){throw new AssertionError("no UI capture");}
                public void release(dev.kuan.kuanperion.automation.AutomationSession.Handle h){throw new AssertionError("no UI input ownership");}
                public dev.kuan.kuanperion.automation.pest.PestTask.Motion restore(dev.kuan.kuanperion.automation.AutomationSession.Handle h,dev.kuan.kuanperion.automation.pest.PestTask.Anchor a,long n){throw new AssertionError("no UI restore");}
            };
            var store=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path.resolve("automation.json"));
            var service=new dev.kuan.kuanperion.automation.pest.PestService(store,session,port,()->false,notice->{throw new AssertionError("queue/cancel is not gameplay completion");});
            var tab=PestModuleSettings.tab(service,SettingGroup.alwaysOn("Notifications",""));
            var actions=tab.groups().getFirst().getSettings().stream().filter(ActionSetting.class::isInstance).map(ActionSetting.class::cast).toList();
            require(actions.size()==2&&!actions.get(1).isVisible(),"no phantom Pest stop action");actions.getFirst().execute();
            require(service.pending()&&actions.get(1).isVisible()&&!service.active()&&!session.active()&&service.status().contains("30"),"real Pest UI queues without effects and exposes stop/status");
            actions.get(1).execute();require(!service.pending()&&!actions.get(1).isVisible()&&!service.autoEnabled()&&!java.nio.file.Files.exists(path.resolve("automation.json")),"actual stop callback cancels queue without persisted runtime intent");
        } finally {java.nio.file.Files.delete(path);}
    }
    private static <P> P contextOnlyPort(Class<P> type,AutomationContext context){
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{if(method.getName().equals("context"))return context;throw new AssertionError("settings invoked native port: "+method.getName());}));
    }
    private static void assertPendingUi(ModulesTab.SubTab tab,java.util.function.BooleanSupplier pending,java.util.function.BooleanSupplier active,java.util.function.Supplier<String> status){
        var actions=tab.groups().getFirst().getSettings().stream().filter(ActionSetting.class::isInstance).map(ActionSetting.class::cast).toList();
        var run=actions.stream().filter(a->a.getRawName().startsWith("關閉選單後")).findFirst().orElseThrow();
        var stop=actions.stream().filter(a->a.getRawName().startsWith("停止本次")).findFirst().orElseThrow();
        require(!pending.getAsBoolean()&&!active.getAsBoolean()&&!stop.isVisible(),"idle maintenance has no phantom stop");run.execute();
        require(pending.getAsBoolean()&&!active.getAsBoolean()&&stop.isVisible()&&status.get().contains("30"),"real maintenance UI queues, exposes status and stop without native effects");stop.execute();
        require(!pending.getAsBoolean()&&!active.getAsBoolean()&&!stop.isVisible(),"real maintenance stop callback clears pending intent");
    }
    private static void testMaintenancePendingUi() throws Exception {
        var dir=java.nio.file.Files.createTempDirectory("maintenance-pending-ui-");
        try {
            var context=new AutomationContext(1,true,true,Set.of());
            var session=new AutomationSession(new AutomationCoordinator(),()->context,()->{throw new AssertionError("settings paused Farm");},()->{throw new AssertionError("settings stopped Farm");});
            var path=dir.resolve("automation.json");var store=AutomationSettingsStore.load(path);
            store.setGreenhousePolicy(GreenhousePolicy.defaults().withPlots(Set.of(1)).withKinds(Set.of(GreenhouseIdentity.Kind.ASHWREATH)).withTool("FARMING_HOE",new UUID(0,9).toString()));
            store.setComposterPolicy(ComposterPolicy.defaults().withAnchor(new ComposterPolicy.Anchor(-11,72,-27,"minecraft:player_head")).withMaterials(Map.of("BOX_OF_SEEDS",new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,1000))));
            store.setVisitorPolicy(new VisitorPolicy(new VisitorValuation.Rules(Set.of("Librarian"),Set.of("WHEAT"),1000,0,1),1,1,240,60,true));
            store.setAutoSellPolicy(new AutoSellPolicy(Set.of("WHEAT"),Set.of(),2,128,1000,60,60,85,true));
            store.setJunkPolicy(new JunkPolicy(Set.of("SEEDS"),Set.of(),3,192,1000,60,60,85,true,new JunkPolicy.Location("overworld",10,70,10),1.5));
            byte[] original=java.nio.file.Files.readAllBytes(path);java.util.function.Consumer<StashService.Notice> notice=n->{throw new AssertionError("queue/cancel is not gameplay completion");};
            var greenhouse=new GreenhouseService(store,session,new PlayerPositionReceipts(),new HarvestReceipts(),contextOnlyPort(GreenhouseTask.Port.class,context),()->false,notice);
            assertPendingUi(GreenhouseModuleSettings.tab(greenhouse),greenhouse::pending,greenhouse::active,greenhouse::status);
            var composter=new ComposterService(store,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),contextOnlyPort(ComposterTask.Port.class,context),()->false,Optional::empty,notice);
            assertPendingUi(ComposterModuleSettings.tab(composter),composter::pending,composter::active,composter::status);
            var visitor=new VisitorService(store,session,new PlayerPositionReceipts(),new InventoryReceipts(),new CursorReceipts(),contextOnlyPort(VisitorTask.Port.class,context),()->false,notice);
            assertPendingUi(VisitorModuleSettings.tab(visitor,SettingGroup.alwaysOn("Notifications","")),visitor::pending,visitor::active,visitor::status);
            var hub=new AutomationContext(1,true,false,Set.of());
            var sell=new AutoSellService(store,session,new InventoryReceipts(),contextOnlyPort(AutoSellTask.Port.class,hub),()->false,notice);
            var junk=new JunkService(store,session,new InventoryReceipts(),contextOnlyPort(JunkTask.Port.class,context),()->false,notice);
            var stash=new StashService(store,new AutomationCoordinator(),()->context,command->{throw new AssertionError("Trade settings sent Stash command");},notice);
            var trade=AutomationModuleSettings.trade(stash,sell,junk);
            assertPendingUi(trade.stream().filter(t->t.name().equals("Auto Sell")).findFirst().orElseThrow(),sell::pending,sell::active,sell::status);
            assertPendingUi(trade.stream().filter(t->t.name().equals("Junk")).findFirst().orElseThrow(),junk::pending,junk::active,junk::status);
            require(!session.active()&&!session.holdsFarming()&&!greenhouse.autoEnabled()&&!composter.autoEnabled()&&!visitor.autoEnabled()&&!sell.autoEnabled()&&!junk.autoEnabled()&&java.util.Arrays.equals(original,java.nio.file.Files.readAllBytes(path)),"queue/cancel neither acquires ownership nor enables/persists runtime intent");
        } finally {try(var paths=java.nio.file.Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(p);}}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
