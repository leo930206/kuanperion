package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.*;
import java.util.*;
public final class UiModelTest {
    public static void main(String[] args) throws Exception {
        require(MainGUISidebar.entries().stream().map(MainGUISidebar.Entry::label).toList().equals(List.of("Farming","Mining","Other","Setting")),"exactly four sidebar entries");
        for(int i=0;i<3;i++) {
            var entry=MainGUISidebar.entries().get(i);
            require(entry.mainTab()==0 && entry.moduleFilter()==i+1,"feature sidebar routes to matching module group");
            require(MainGUISidebar.selectedIndex(0,i+1)==i,"feature highlight matches group");
        }
        for(int page=1;page<=4;page++) require(MainGUISidebar.selectedIndex(page,0)==3,"all settings pages share one gear");
        require(MainGUISidebar.selectedIndex(5,0)==-1,"About belongs to footer, not settings");
        require(MainGUISidebar.footerLines("2.0.8").equals(List.of("Mod Version: 2.0.8")),"single bottom version line without duplicate name");
        require(MainGUISidebar.footerLines("9.1.0").get(0).equals("Mod Version: 9.1.0"),"footer is not hardcoded");
        require(MainGUISidebar.footerY(20,660)==656,"footer is a compact bottom caption");
        require(MainGUISidebar.rowY(3,20,660)+44<=MainGUISidebar.footerY(20,660),"Setting cannot overlap caption");
        require(MainGUISidebar.footerContains(160,667,20,20,660,194,1),"expanded version caption opens About across its text width");
        require(!MainGUISidebar.footerContains(50,667,20,20,660,60,0),"collapsed footer has no invisible old K hit target");
        require(!MainGUISidebar.footerContains(50,650,20,20,660,194,1),"old footer icon area above caption is not clickable");
        require(!MainGUISidebar.footerContains(31,667,20,20,660,194,1),"caption left padding excluded");
        require(!MainGUISidebar.footerContains(203,667,20,20,660,194,1),"caption right padding excluded");
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
            require(preset.save(1000)==ColorPresetSetting.SaveResult.SAVED,"first save");
            require(preset.load()&&applied[0]==1,"one-click load");
            require(preset.save(2000)==ColorPresetSetting.SaveResult.CONFIRM,"overwrite confirmation");
            require(preset.save(8000)==ColorPresetSetting.SaveResult.CONFIRM,"confirmation expires");
            require(preset.save(8100)==ColorPresetSetting.SaveResult.SAVED,"second click saves");
        }finally{try(var paths=java.nio.file.Files.walk(paletteDir)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}}
        boolean[] notify={false};String[] seconds={""};
        var combined=new InlineToggleSetting("螢幕通知","顯示時間",()->seconds[0],v->seconds[0]=v,()->notify[0],v->notify[0]=v);
        require(metrics.descriptionWidth(combined,600)+12+50+metrics.textInlineFieldWidth(600-32-50)<=600-32,"description reserves full centered input column");
        require(combined.getValue().isEmpty() && combined.getPlaceholder().equals("顯示時間"),"blank duration placeholder");
        combined.setValue("2.5");require(seconds[0].equals("2.5")&&!notify[0],"duration editing does not toggle");
        combined.toggle();require(notify[0]&&seconds[0].equals("2.5"),"toggle does not edit duration");
        int[] count={2},starts={0};
        var selection=new SelectionActionSetting("選擇種類數量",()->(float)count[0],v->count[0]=Math.round(v),()->starts[0]++);
        for(float width:new float[]{400,600,1000})require(16+metrics.descriptionWidth(selection,width)<=InlineRowLayout.countFieldX(16,width-32)-16,"selection description stays left of count/action controls");
        selection.setValue(9);require(count[0]==5&&starts[0]==0,"count clamps without starting selection");selection.start();require(starts[0]==1,"paired action callback");
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
        require(layout.pw==1060&&layout.ph==660&&layout.contW==1000,"reference panel geometry");
        require(UiScalePolicy.effective(320,240,3)<=.5f,"small-window fit");
        require(UiScalePolicy.effective(1920,1280,1.5f)==1.5f,"normal scale retained");
        require(Float.isFinite(UiScalePolicy.effective(1920,1280,Float.NaN)),"malformed scale");
        System.out.println("UiModelTest: PASS (live bindings, bounds, Chinese text, word navigation, original spring animation, geometry, adaptive scale)");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
