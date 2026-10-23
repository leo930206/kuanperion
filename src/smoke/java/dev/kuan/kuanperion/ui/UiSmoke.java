package dev.kuan.kuanperion.ui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
public final class UiSmoke implements ClientModInitializer {
    private int ticks,phase=-1;private MainGUI ui;private boolean waiting,phaseValidated;
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        try{
            if(phase<0){
                if(mc.gui.overlay()!=null||!(mc.gui.screen() instanceof TitleScreen))return;
                ui=new MainGUI();mc.setScreenAndShow(ui);phase=0;ticks=0;System.out.println("UI_SMOKE_OPEN");return;
            }
            if(waiting||++ticks<35)return;ticks=0;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(),1000,700);
            String[] names={"01-overview","02-farming","03-mining","04-jerry","05-colors","06-profiles","07-keybinds","08-settings","09-search","10-reopened","11-pest-inline","12-selected-targets"};
            if(phase>=names.length){System.out.println("UI_SMOKE_PASS");mc.stop();return;}
            System.out.println("UI_SMOKE_STATE "+phase+" main="+ui.getActiveMainTab()+" module="+(ui.getActiveModuleSubTab()==null?"none":ui.getActiveModuleSubTab().name())+" live="+(mc.gui.screen()==ui));
            if(!phaseValidated) {
            if(phase==0) { testSafety(mc); testTheme(mc); testSelectedModels(); }
            if(phase==1) testToggleClick();
            if(phase==9) testSettingsShortcut(mc);
            if(phase==10) testInlineDuration();
            if(phase==11) testTargetRows();
                phaseValidated=true;ticks=32;return; // Capture after two fresh rendered frames.
            }
            phaseValidated=false;
            final String shot=names[phase];
            Screenshot.grab(mc.gameDirectory,shot+".png",mc.gameRenderer.mainRenderTarget(),1,message->System.out.println("UI_SMOKE_CAPTURE "+shot));
            phase++;
            switch(phase){
                case 1-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.get(0));
                case 2-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow());
                case 3-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Jerry 偵測")).findFirst().orElseThrow());
                case 4-> switchTab(1);
                case 5-> switchTab(2);
                case 6->{switchTab(3);testChordCapture();}
                case 7->{((dev.kuan.kuanperion.ui.settings.KeybindSetting)dev.kuan.kuanperion.KuanperionClient.uiKeybinds().get(2)).resetToDefault();switchTab(4);}
                case 8->{ui.activateSearchField();ui.setSearchQuery("通知");}
                case 10->{var pest=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Pest 通知")).findFirst().orElseThrow();var group=pest.groups().get(0);var setting=group.getSettings().stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.InlineToggleSetting).findFirst().orElseThrow();ui.openSearchResult(0,pest,group,setting);}
                case 11->{var mining=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow();ui.openModuleDetailFromCard(mining);ui.selectModuleCategory(2);}
                case 9->{ui.onClose();if(mc.gui.screen()==ui)throw new AssertionError("close did not dismiss");ui=new MainGUI();mc.setScreenAndShow(ui);}
            }
        }catch(Throwable error){System.out.println("UI_SMOKE_FAIL");error.printStackTrace();mc.stop();}
    }
    @SuppressWarnings("unchecked")
    private java.util.List<String> selected() throws Exception {
        var f=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("TARGET_BLOCKS");f.setAccessible(true);return (java.util.List<String>)f.get(null);
    }
    private void testSelectedModels() throws Exception {
        selected().clear();
        var group=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow().groups().get(1);
        if(group.getSettings().stream().filter(dev.kuan.kuanperion.ui.settings.Setting::isVisible).count()!=1)throw new AssertionError("empty targets have phantom priority rows");
        selected().add("minecraft:stone");selected().add("minecraft:diamond_ore");
        if(group.getSettings().stream().filter(dev.kuan.kuanperion.ui.settings.Setting::isVisible).count()!=3)throw new AssertionError("selected target visibility count");
        var count=(dev.kuan.kuanperion.ui.settings.SelectionActionSetting)group.getSettings().get(0);count.setValue(2);
        dev.kuan.kuanperion.renderer.NanoVGManager.beginFrame(ui.width,ui.height);
        try{for(String id:selected())if(!dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer().blockIcon(id,0,0,24))throw new AssertionError("missing real block material icon: "+id);}
        finally{dev.kuan.kuanperion.renderer.NanoVGManager.endFrame();}
        System.out.println("UI_SMOKE_TARGET_MODELS_PASS zero phantom rows / actual count / resource-pack block icons");
    }
    private float[] rowFor(dev.kuan.kuanperion.ui.settings.Setting target) {
        var layout=ui.context().layout;float x=layout.contX+181,w=layout.contW-201;
        float y=layout.contY+129-ui.getActiveScrollY();
        for(var group:ui.activeModuleClickGroups()){
            y+=65;
            for(var setting:group.getSettings()){
                if(!setting.isVisible())continue;float h=ui.settingHeightFor(setting,w);
                if(setting==target)return new float[]{x,y,w,h};y+=h;
            }
            y+=4;
        }
        throw new AssertionError("setting row not found");
    }
    private void click(float x,float y){
        var layout=ui.context().layout;
        var event=new MouseButtonEvent(x*MainGUI.uiScale/layout.pr,y*MainGUI.uiScale/layout.pr,new MouseButtonInfo(0,0));
        ui.mouseClicked(event,false);ui.mouseReleased(event);
    }
    private void testInlineDuration() {
        var setting=(dev.kuan.kuanperion.ui.settings.InlineToggleSetting)ui.getActiveModuleSubTab().groups().get(0).getSettings().stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.InlineToggleSetting).findFirst().orElseThrow();
        var row=rowFor(setting);boolean before=setting.isOn();
        click(row[0]+row[2]-130,row[1]+22);
        if(ui.activeText!=setting || setting.isOn()!=before)throw new AssertionError("duration click toggled checkbox / did not focus editor");
        if(ui.activeEditorH!=16)throw new AssertionError("notification input not compact default height");
        ui.textBuf=new StringBuilder("2.5");ui.commitText();
        if(!setting.getValue().equals("2.5"))throw new AssertionError("duration did not save");
        setting.setValue("");
        if(!setting.getValue().isEmpty())throw new AssertionError("blank should preserve placeholder state");
        click(row[0]+row[2]-40,UiLayoutMetrics.checkboxTop(row[1],row[3])+19);
        if(setting.isOn()==before||!setting.getValue().isEmpty())throw new AssertionError("checkbox click edited time / failed toggle");
        var values=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(!values.get("pestNotificationScreenSeconds").getAsString().equals("3"))throw new AssertionError("blank duration needs safe default");
        System.out.println("UI_SMOKE_INLINE_DURATION_PASS actual mouse hit regions / editor commit / blank placeholder / independent toggle");
    }
    private void testTargetRows() throws Exception {
        var group=ui.getActiveModuleSubTab().groups().get(1);
        var first=(dev.kuan.kuanperion.ui.settings.BlockTargetSetting)group.getSettings().get(1);
        first.moveDown();if(!selected().get(1).equals("minecraft:stone"))throw new AssertionError("priority move failed");
        first.moveDown(); // now row 1 refers to diamond ore, move it back down
        var second=(dev.kuan.kuanperion.ui.settings.BlockTargetSetting)group.getSettings().get(2);
        second.remove();
        if(group.getSettings().stream().filter(dev.kuan.kuanperion.ui.settings.Setting::isVisible).count()!=2)throw new AssertionError("deleted target left phantom row");
        selected().add("minecraft:diamond_ore");
        System.out.println("UI_SMOKE_TARGET_ROWS_PASS priority controls / delete visibility / existing backing list");
    }
    private void testChordCapture() {
        var keys=dev.kuan.kuanperion.KuanperionClient.uiKeybinds();
        var farming=(dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(2);
        ui.beginKeybindCapture(farming);ui.keyPressed(new net.minecraft.client.input.KeyEvent(341,0,2));
        if(!ui.isAwaitingKeybindCapture(farming))throw new AssertionError("modifier committed without base key");
        ui.keyPressed(new net.minecraft.client.input.KeyEvent(70,0,3));
        if(farming.getModifiers()!=3)throw new AssertionError("Ctrl Shift F not captured");
        ui.beginKeybindCapture(farming);ui.keyPressed(new net.minecraft.client.input.KeyEvent(70,0,0));
        if(farming.getModifiers()!=0||farming.getBoundKeyName().contains("Shift"))throw new AssertionError("plain F forced Shift");
        ui.beginKeybindCapture(farming);ui.keyPressed(new net.minecraft.client.input.KeyEvent(70,0,7));
        if(farming.getModifiers()!=7||!farming.getBoundKeyName().contains("Ctrl + Alt + Shift"))throw new AssertionError("three modifier capture/display");
        var snapshot=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(snapshot.keySet().stream().anyMatch(k->k.startsWith("hotkey.")))throw new AssertionError("partial hotkey state leaked into feature profile");
        farming.resetToDefault();
        var mining=(dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(1);mining.resetToDefault();
        if(!mining.getBoundKeyName().equals("Shift + M") || !farming.getBoundKeyName().equals("Shift + F"))throw new AssertionError("default Latin shortcut labels");
        System.out.println("UI_SMOKE_CHORD_PASS modifier waiting / Ctrl Shift F / plain F / three modifiers / display / profile isolation");
    }

    private void testTheme(Minecraft mc) throws Exception {
        var saved=dev.kuan.kuanperion.ui.theme.Theme.exportJson();
        var file=mc.gameDirectory.toPath().resolve("config/kuanperion-theme.json");
        var mapField=dev.kuan.kuanperion.ui.theme.SkyThemePalette.class.getDeclaredField("LEGACY");mapField.setAccessible(true);
        var legacy=new com.google.gson.JsonObject();
        ((java.util.Map<String,String>)mapField.get(null)).forEach(legacy::addProperty);
        legacy.addProperty("uiScale",2.25);legacy.addProperty("textScale",1.2);legacy.addProperty("animSpeed",450);
        legacy.addProperty("HUD Title","FF010203");
        var priorBackup=file.resolveSibling("kuanperion-theme.before-sky-blossom.bak");
        String backupBefore=java.nio.file.Files.exists(priorBackup)?java.nio.file.Files.readString(priorBackup):null;
        String original=legacy.toString();java.nio.file.Files.writeString(file,original);
        dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(dev.kuan.kuanperion.ui.theme.Theme.PANEL_BG!=0xFFFFFFFF || !dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED)throw new AssertionError("old default not upgraded");
        if(dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE!=2.25f || dev.kuan.kuanperion.ui.theme.Theme.HUD_TITLE!=0xFF010203)throw new AssertionError("migration overwrote non-menu preference");
        var backup=file.resolveSibling("kuanperion-theme.before-sky-blossom.bak");
        if(!java.nio.file.Files.readString(backup).equals(backupBefore==null?original:backupBefore))throw new AssertionError("theme backup not exact");
        var skyField=dev.kuan.kuanperion.ui.theme.SkyThemePalette.class.getDeclaredField("PREVIOUS_SKY");skyField.setAccessible(true);
        var previousSky=new com.google.gson.JsonObject();
        ((java.util.Map<String,String>)skyField.get(null)).forEach(previousSky::addProperty);
        previousSky.addProperty("themeSchema",1);previousSky.addProperty("gradientEnabled",true);previousSky.addProperty("uiScale",2.25);
        java.nio.file.Files.writeString(file,previousSky.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=0xFF58B9EA || dev.kuan.kuanperion.ui.theme.Theme.TEXT_LABEL!=0xFF50AFE1)throw new AssertionError("2.0.1 sky default not lightened");
        if(dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_COVERAGE>=.64f || dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_STRENGTH>=1)throw new AssertionError("gradient not reduced");
        if(dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE!=2.25f)throw new AssertionError("sky upgrade reset UI scale");
        previousSky.addProperty("Text","FF123456");java.nio.file.Files.writeString(file,previousSky.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(dev.kuan.kuanperion.ui.theme.Theme.TEXT_PRIMARY!=0xFF123456)throw new AssertionError("custom text overwritten");
        legacy.addProperty("Accent","FF123456");java.nio.file.Files.writeString(file,legacy.toString());
        dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=0xFF123456 || dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED)throw new AssertionError("custom old theme replaced");
        dev.kuan.kuanperion.ui.theme.ThemePreset.SLATE.apply();
        if(dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED)throw new AssertionError("gradient leaked into old preset");
        dev.kuan.kuanperion.ui.theme.ThemePreset.SKY_BLOSSOM.apply();
        if(!dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED || dev.kuan.kuanperion.ui.theme.Theme.PANEL_BG!=0xFFFFFFFF)throw new AssertionError("new preset failed");
        dev.kuan.kuanperion.ui.theme.Theme.importJson(saved);
        dev.kuan.kuanperion.ui.theme.ThemePreset.SKY_BLOSSOM.apply();
        dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE=1.5f;dev.kuan.kuanperion.ui.theme.Theme.TEXT_SCALE=1;
        dev.kuan.kuanperion.ui.theme.Theme.saveTheme();
        dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED=false;
        dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(!dev.kuan.kuanperion.ui.theme.Theme.GRADIENT_ENABLED)throw new AssertionError("gradient not persisted");
        System.out.println("UI_SMOKE_THEME_PASS old default migration+backup / custom preservation / presets / gradient persistence");
    }
    private void testSettingsShortcut(Minecraft mc) {
        var key=new net.minecraft.client.input.KeyEvent(39,0,0);
        ui.activateSearchField();
        ui.keyPressed(key);
        if(mc.gui.screen()!=ui)throw new AssertionError("settings key closed a focused text editor");
        ui.keyPressed(new net.minecraft.client.input.KeyEvent(256,0,0));
        if(mc.gui.screen()!=ui)throw new AssertionError("first escape should leave search");
        ui.keyPressed(key);
        if(mc.gui.screen()==ui)throw new AssertionError("settings key did not close");
        ui=new MainGUI();mc.setScreenAndShow(ui);
        System.out.println("UI_SMOKE_SHORTCUT_PASS editor priority / escape / settings-key close");
    }
    private void testToggleClick() {
        var group=ui.getActiveModuleSubTab().groups().get(0);
        var setting=(dev.kuan.kuanperion.ui.settings.ToggleSetting)group.getSettings().get(0);
        boolean old=setting.getValue();
        var row=rowFor(setting);click(row[0]+row[2]-40,UiLayoutMetrics.checkboxTop(row[1],row[3])+19);
        if(setting.getValue()==old)throw new AssertionError("rendered row click did not change live preference");
        setting.toggle();
        var controller=new MainGUISettingInteractionController(ui);
        for(String label:new String[]{"Blue","異常狀況處理"}){
            boolean[] value={false};
            var sample=new dev.kuan.kuanperion.ui.settings.ToggleSetting(label,()->value[0],v->value[0]=v);
            float height=ui.settingHeightFor(sample,350);
            controller.handleSettingClick(sample,320,UiLayoutMetrics.checkboxTop(10,height)+19,0,10,350,height);
            if(!value[0])throw new AssertionError("full-card checkbox hit missed: "+label);
        }
        System.out.println("UI_SMOKE_CENTERED_CHECKBOX_PASS no-help / wrapped-help / lower checkbox edge / inline duration independence");
        System.out.println("UI_SMOKE_INPUT_PASS toggle live binding while feature stopped");
    }
    private void testSafety(Minecraft mc) throws Exception {
        var webhook=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("pestWebhookUrl");webhook.setAccessible(true);webhook.set(null,"https://example.invalid/secret-test-only");
        var snapshot=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(snapshot.toString().contains("secret-test")||snapshot.has("pestWebhookUrl"))throw new AssertionError("profile secret leak");
        dev.kuan.kuanperion.config.ConfigProfileManager.save("ui-smoke-profile");
        snapshot.addProperty("scanRadius","6");snapshot.addProperty("miningEnabled","true");
        if(!dev.kuan.kuanperion.KuanperionClient.applyUiConfig(snapshot))throw new AssertionError("valid profile failed");
        if(!dev.kuan.kuanperion.KuanperionClient.exportUiConfig().get("scanRadius").getAsString().equals("6"))throw new AssertionError("profile not applied");
        snapshot.addProperty("scanRadius",7);
        if(dev.kuan.kuanperion.KuanperionClient.applyUiConfig(snapshot))throw new AssertionError("invalid profile accepted");
        webhook.set(null,"");
        boolean stencil=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
        int active=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        int[] box=new int[4];org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,box);
        try{
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
            org.lwjgl.opengl.GL11.glScissor(3,4,50,60);
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE3);
            dev.kuan.kuanperion.renderer.NanoVGManager.beginFrame(ui.width,ui.height);
            dev.kuan.kuanperion.renderer.NanoVGManager.endFrame();
            if(!org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_STENCIL_TEST))throw new AssertionError("stencil state lost");
            if(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE)!=org.lwjgl.opengl.GL13.GL_TEXTURE3)throw new AssertionError("active texture lost");
            int[] after=new int[4];org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,after);
            if(!java.util.Arrays.equals(after,new int[]{3,4,50,60}))throw new AssertionError("scissor rectangle lost");
        }finally{
            if(!stencil)org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
            org.lwjgl.opengl.GL11.glScissor(box[0],box[1],box[2],box[3]);org.lwjgl.opengl.GL13.glActiveTexture(active);
        }
        var parse=dev.kuan.kuanperion.ui.theme.Theme.class.getDeclaredMethod("parseAnimationTime",float.class);parse.setAccessible(true);
        if(!Float.isFinite((Float)parse.invoke(null,Float.NaN)))throw new AssertionError("corrupt theme animation");
        System.out.println("UI_SMOKE_SAFETY_PASS profile apply/reject/secret exclusion + GL state restoration");
    }
    private void switchTab(int tab)throws Exception{
        var field=MainGUI.class.getDeclaredField("actions");field.setAccessible(true);((MainGUIActions)field.get(ui)).switchMainTab(tab);
    }
}
