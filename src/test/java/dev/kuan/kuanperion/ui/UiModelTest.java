package dev.kuan.kuanperion.ui;
import dev.kuan.kuanperion.ui.settings.*;
import java.util.*;
public final class UiModelTest {
    public static void main(String[] args) throws Exception {
        boolean[] notify={false};String[] seconds={""};
        var combined=new InlineToggleSetting("螢幕通知","顯示時間",()->seconds[0],v->seconds[0]=v,()->notify[0],v->notify[0]=v);
        require(combined.getValue().isEmpty() && combined.getPlaceholder().equals("顯示時間"),"blank duration placeholder");
        combined.setValue("2.5");require(seconds[0].equals("2.5")&&!notify[0],"duration editing does not toggle");
        combined.toggle();require(notify[0]&&seconds[0].equals("2.5"),"toggle does not edit duration");
        int[] count={2},starts={0};
        var selection=new SelectionActionSetting("選擇種類數量",()->(float)count[0],v->count[0]=Math.round(v),()->starts[0]++);
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
