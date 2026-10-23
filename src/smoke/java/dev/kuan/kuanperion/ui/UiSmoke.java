package dev.kuan.kuanperion.ui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
public final class UiSmoke implements ClientModInitializer {
    private java.lang.reflect.Method mouseMove;
    private int ticks,phase=-1;private MainGUI ui;private boolean waiting,phaseValidated;
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        try{
            if(phase<0){
                if(mc.gui.overlay()!=null||!(mc.gui.screen() instanceof TitleScreen))return;
                ui=new MainGUI();mc.setScreenAndShow(ui);phase=0;ticks=0;System.out.println("UI_SMOKE_OPEN");return;
            }
            boolean expanded = phase >= 13 && phase <= 16 || phase == 21;
            var frame = ui.context().layout;
            double cursorX = expanded ? (frame.px+20)*MainGUI.uiScale/frame.pr * mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth() : 1;
            double cursorY = expanded ? (frame.py+20)*MainGUI.uiScale/frame.pr * mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight() : 1;
            // Feed the vanilla cursor callback directly: background macOS windows may ignore GLFW cursor warps.
            // This exercises real hover/layout animation without forcing sidebarAnim or moving the user's pointer.
            if(mouseMove==null){mouseMove=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);mouseMove.setAccessible(true);}
            mouseMove.invoke(mc.mouseHandler,mc.getWindow().handle(),cursorX,cursorY);
            if(expanded && Math.abs(ui.scaleMouseX(mc.mouseHandler.getScaledXPos(mc.getWindow()))-(frame.px+20))>1)throw new AssertionError("Retina hover coordinate conversion");
            if(waiting||++ticks<35)return;
            if(expanded && ui.context().animation.sidebarAnim<.95f && ticks<140)return;
            ticks=0;
            String[] names={"01-overview","02-farming","03-mining","04-jerry","05-colors","06-profiles","07-keybinds","08-settings","09-search","10-reopened","11-pest-inline","12-selected-targets","13-color-presets","14-farming-sidebar","15-mining-sidebar","16-other-sidebar","17-setting-sidebar","18-setting-colors","19-setting-profiles","20-setting-keybinds","21-setting-interface","22-about-sidebar"};
            if(phase>=names.length){System.out.println("UI_SMOKE_PASS");mc.stop();return;}
            System.out.println("UI_SMOKE_STATE "+phase+" main="+ui.getActiveMainTab()+" module="+(ui.getActiveModuleSubTab()==null?"none":ui.getActiveModuleSubTab().name())+" live="+(mc.gui.screen()==ui));
            if(!phaseValidated) {
            if(phase==0) { testSidebarRouting(); testSettingsAutomation(mc); testModIcon(); testSafety(mc); testTheme(mc); testPreferenceReload(mc); testSelectedModels(); }
            if(phase==1) testToggleClick();
            if(phase==2 || phase==13) {
                var layout=ui.context().layout;
                float sidebarWidth=60+ui.context().animation.sidebarAnim*(ui.context().animation.computedSidebarExpanded-60);
                String caption=MainGUISidebar.footerText(MainGUIRegistry.modVersion(),phase==13);
                float width=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer().textWidth("Inter-Regular",caption,MainGUISidebar.FOOTER_FONT);
                if(width>sidebarWidth-24 || Math.abs(MainGUISidebar.footerTextX(layout.px,sidebarWidth,width)+width/2-(layout.px+sidebarWidth/2))>.01f)throw new AssertionError("footer not fitted/centered");
                System.out.println("UI_SMOKE_CENTERED_FOOTER_PASS expanded="+(phase==13)+" caption="+caption);
            }
            if(phase==9) testSettingsShortcut(mc);
            if(phase==10) testInlineDuration();
            if(phase==11) testTargetRows();
            if(phase==12) testColorPresets();
            if(expanded && ui.context().animation.sidebarAnim < .95f) throw new AssertionError("sidebar did not expand under real mouse hover");
            if(phase>=13 && phase<=15) assertGroup(phase-13);
            if(phase==16 && ui.getActiveMainTab()!=4) throw new AssertionError("gear did not open interface settings");
            if(phase>=17 && phase<=20) {
                clickSettingsPage(phase-16);
                if(ui.getActiveMainTab()!=phase-16) throw new AssertionError("Setting subpage mouse route failed");
            }
            if(phase==21) {
                var layout=ui.context().layout;
                String caption=MainGUISidebar.footerLines(MainGUIRegistry.modVersion()).get(0);
                float width=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer().textWidth("Inter-Regular",caption,MainGUISidebar.FOOTER_FONT);
                if(width>ui.context().animation.computedSidebarExpanded-24)throw new AssertionError("single-line footer does not fit");
                click(layout.px+130,MainGUISidebar.footerY(layout.py,layout.ph)+11);
                if(ui.getActiveMainTab()!=5 || !ui.currentTabName().equals("關於"))throw new AssertionError("version caption did not open About");
                System.out.println("UI_SMOKE_VERSION_FOOTER_PASS single line fits / expanded text hit / About preserved");
            }
                phaseValidated=true;ticks=20;return; // Allow page/sidebar transitions to settle before capture.
            }
            phaseValidated=false;
            final String shot=names[phase];
            Screenshot.grab(mc.gameDirectory,shot+".png",mc.gameRenderer.mainRenderTarget(),1,message->System.out.println("UI_SMOKE_CAPTURE "+shot));
            phase++;
            switch(phase){
                case 13 -> clickSidebar(0);
                case 14 -> clickSidebar(1);
                case 15 -> clickSidebar(2);
                case 16 -> clickSidebar(3);
                case 1-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.get(0));
                case 2-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow());
                case 3-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Jerry 偵測")).findFirst().orElseThrow());
                case 4-> switchTab(1);
                case 5-> switchTab(2);
                case 6->{switchTab(3);testChordCapture();}
                case 7->{((dev.kuan.kuanperion.ui.settings.KeybindSetting)dev.kuan.kuanperion.KuanperionClient.uiKeybinds().get(2)).resetToDefault();switchTab(4);}
                case 8->{ui.activateSearchField();ui.setSearchQuery("通知");}
                case 10->{var pest=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Pest 通知")).findFirst().orElseThrow();var group=pest.groups().get(0);var setting=group.getSettings().stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.InlineToggleSetting).findFirst().orElseThrow();ui.openSearchResult(0,pest,group,setting);}
                case 12->{switchTab(1);ui.setActiveFilterIndex(2);ui.setActiveScroll(0,0);ui.refreshContext();}
                case 11->{var mining=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow();ui.openModuleDetailFromCard(mining);ui.selectModuleCategory(2);}
                case 9->{ui.onClose();if(mc.gui.screen()==ui)throw new AssertionError("close did not dismiss");ui=new MainGUI();mc.setScreenAndShow(ui);}
            }
        }catch(Throwable error){System.out.println("UI_SMOKE_FAIL");error.printStackTrace();mc.stop();}
    }
    private void clickSidebar(int index) {
        var layout=ui.context().layout;
        click(layout.px+(ui.context().animation.sidebarAnim>.95f?100:30),MainGUISidebar.rowY(index,layout.py,layout.ph)+22);
    }
    private void assertGroup(int index) {
        String[][] expected={{"Farming","Pest 通知","Visitor 通知"},{"Mining"},{"Jerry 偵測","Discord Webhook"}};
        var sections=ui.moduleSectionsForFilter();
        if(ui.getActiveMainTab()!=0 || ui.getActiveModuleSubTab()!=null || sections.size()!=1 || !sections.get(0).subtabs().stream().map(dev.kuan.kuanperion.ui.settings.ModulesTab.SubTab::name).toList().equals(java.util.List.of(expected[index]))) throw new AssertionError("wrong feature group: "+index);
    }
    private void clickSettingsPage(int page) {
        String[] labels={"顏色","設定檔","快捷鍵","介面設定"};
        float x=ui.contentX()+20;
        var renderer=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer();
        for(int i=0;i<page-1;i++) x+=renderer.textWidth("Inter-Regular",labels[i],12)+28;
        click(x+20,ui.contentY()+72);
    }
    private void testSidebarRouting() throws Exception {
        for(int i=0;i<3;i++) {
            ui.setSearchQuery("stale-search");ui.setActiveScroll(50,60);clickSidebar(i);assertGroup(i);
            if(!ui.searchQuery.isEmpty() || ui.getActiveScrollY()!=0)throw new AssertionError("group navigation kept search/scroll");
            var subtab=ui.moduleSectionsForFilter().get(0).subtabs().get(0);
            ui.openModuleDetailFromCard(subtab);
            if(ui.context().navigation.activeFilter!=i+1)throw new AssertionError("detail lost group");
            var actions=MainGUI.class.getDeclaredField("actions");actions.setAccessible(true);((MainGUIActions)actions.get(ui)).exitModuleDetail();assertGroup(i);
        }
        for(var section:MainGUIRegistry.MODULE_SECTIONS) for(var subtab:section.subtabs()) {
            var group=subtab.groups().get(0);
            ui.openSearchResult(0,subtab,group,group.getSettings().isEmpty()?null:group.getSettings().get(0));
            if(ui.context().navigation.activeFilter!=MainGUIRegistry.moduleFilterFor(subtab))throw new AssertionError("cross-group search route wrong");
        }
        clickSidebar(3);
        if(ui.getActiveMainTab()!=4 || ui.getActiveModuleSubTab()!=null)throw new AssertionError("gear kept feature detail");
        for(int page=1;page<=4;page++) {
            switchTab(page);
            if(MainGUISidebar.selectedIndex(ui.getActiveMainTab(),0)!=3 || ui.settingsNavigationHeight()!=37 || !ui.currentTabName().equals("Setting"))throw new AssertionError("Setting page not grouped");
        }
        var layout=ui.context().layout;click(layout.px+30,MainGUISidebar.footerY(layout.py,layout.ph)+11);
        if(ui.getActiveMainTab()!=5)throw new AssertionError("visible collapsed version did not open About");
        var lines=MainGUISidebar.footerLines(MainGUIRegistry.modVersion());
        if(!lines.equals(java.util.List.of("Mod Version "+MainGUIRegistry.modVersion()+"")))throw new AssertionError("wrong footer metadata");
        clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_SIDEBAR_PASS four visible routes / group membership / detail-back / cross-group search / Setting pages / collapsed footer / metadata");
    }
    @SuppressWarnings("unchecked")
    private java.util.List<String> selected() throws Exception {
        var f=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("TARGET_BLOCKS");f.setAccessible(true);return (java.util.List<String>)f.get(null);
    }
    private void testSettingsAutomation(Minecraft mc) throws Exception {
        if(ui.isPauseScreen())throw new AssertionError("settings must not pause singleplayer world");
        if(!(mc instanceof dev.kuan.kuanperion.mixin.MinecraftAttackAccess))throw new AssertionError("26.2 held attack bridge Mixin missing");
        ((dev.kuan.kuanperion.mixin.MinecraftAttackAccess)mc).kuanperion$setMissTime(0);
        Class<?> client=dev.kuan.kuanperion.KuanperionClient.class;
        var other=new net.minecraft.client.gui.screens.TitleScreen();
        for(String name:new String[]{"isFarmingPauseScreen","isMiningPauseScreen"}){
            var method=client.getDeclaredMethod(name,net.minecraft.client.gui.screens.Screen.class);method.setAccessible(true);
            if((Boolean)method.invoke(null,ui) || (Boolean)method.invoke(null,new Object[]{null}))throw new AssertionError("own settings opened/closed creates smart-pause buffer: "+name);
            if(!(Boolean)method.invoke(null,other))throw new AssertionError("other GUI protection removed: "+name);
        }
        for(String name:new String[]{"FARMING_GUI_PAUSE_GUARD","MINING_GUI_PAUSE_GUARD"}){
            var field=client.getDeclaredField(name);field.setAccessible(true);var guard=field.get(null);var reset=guard.getClass().getDeclaredMethod("reset");reset.setAccessible(true);reset.invoke(guard);
        }
        System.out.println("UI_SMOKE_SETTINGS_AUTOMATION_PASS non-pausing UI / applied26.2 Mixin accessor / real Farming+Mining wrappers / other GUI guard");
    }
    private void testModIcon() throws Exception {
        dev.kuan.kuanperion.renderer.NanoVGManager.beginFrame(ui.width,ui.height);
        try{
            var renderer=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer();
            if(!renderer.modIcon(0,0,34))throw new AssertionError("bundled avatar PNG did not render");
            var field=renderer.getClass().getDeclaredField("modIconHandle");field.setAccessible(true);int first=field.getInt(renderer);
            if(first<=0 || !renderer.modIcon(0,0,34) || field.getInt(renderer)!=first)throw new AssertionError("avatar texture not reused");
        }finally{dev.kuan.kuanperion.renderer.NanoVGManager.endFrame();}
        System.out.println("UI_SMOKE_AVATAR_PASS bundled Fabric PNG / nearest texture / cached handle");
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
        click(row[0]+row[2]-130,UiLayoutMetrics.inputTop(row[1],row[3])+11);
        if(ui.activeText!=setting || setting.isOn()!=before)throw new AssertionError("duration click toggled checkbox / did not focus editor");
        if(ui.activeEditorH!=22)throw new AssertionError("notification input not compact default height");
        if(Math.abs(ui.activeEditorY+ui.activeEditorH/2-(row[1]+(row[3]-5)/2))>.01f)throw new AssertionError("input not full-card centered");
        ui.textBuf=new StringBuilder("2.5");ui.commitText();
        if(!setting.getValue().equals("2.5"))throw new AssertionError("duration did not save");
        setting.setValue("");
        if(!setting.getValue().isEmpty())throw new AssertionError("blank should preserve placeholder state");
        click(row[0]+row[2]-40,UiLayoutMetrics.checkboxTop(row[1],row[3])+19);
        if(setting.isOn()==before||!setting.getValue().isEmpty())throw new AssertionError("checkbox click edited time / failed toggle");
        var threshold=(dev.kuan.kuanperion.ui.settings.SliderSetting)ui.getActiveModuleSubTab().groups().get(0).getSettings().stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.SliderSetting).findFirst().orElseThrow();
        var numberRow=rowFor(threshold);float innerW=numberRow[2]-32;
        click(numberRow[0]+16+innerW-innerW/3+33,UiLayoutMetrics.inputTop(numberRow[1],numberRow[3])+11);
        if(ui.activeSliderField!=threshold || ui.activeEditorH!=22 || Math.abs(ui.activeEditorY+11-(numberRow[1]+(numberRow[3]-5)/2))>.01f)throw new AssertionError("threshold input not centered/enlarged/clickable");
        ui.commitText();
        var values=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(!values.get("pestNotificationScreenSeconds").getAsString().equals("3"))throw new AssertionError("blank duration needs safe default");
        System.out.println("UI_SMOKE_INLINE_DURATION_PASS actual mouse hit regions / editor commit / blank placeholder / independent toggle");
    }
    private void testColorPresets() throws Exception {
        var subtab=MainGUIRegistry.COLORS_SUBTABS.get(1);
        if(ui.flatSubtabsForCurrentFilter().size()!=1 || ui.flatSubtabsForCurrentFilter().get(0)!=subtab)throw new AssertionError("color preset tab unreachable");
        var slots=subtab.groups().get(0).getSettings();
        if(slots.size()!=5)throw new AssertionError("quick slots not exactly five");
        var first=(dev.kuan.kuanperion.ui.settings.ColorPresetSetting)slots.get(0);
        float x=ui.contentX()+20,w=ui.contentW()-40,y=ui.contentScrollTop()+51;
        float saveX=x+w-16-60,centerY=y+27.5f;
        int original=dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY;
        var empty=(dev.kuan.kuanperion.ui.settings.ColorPresetSetting)slots.get(4);
        if(empty.occupied())throw new AssertionError("use a clean smoke sandbox; fifth slot must be empty");
        click(x+40,centerY+240);if(empty.occupied())throw new AssertionError("empty load unexpectedly saved");
        boolean existing=first.occupied();
        click(saveX,centerY);if(existing)click(saveX,centerY);
        if(!first.occupied())throw new AssertionError("save button missed");
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=0xFF66C3EF;
        click(saveX,centerY); // Only arm overwrite.
        click(x+40,centerY); // Loading must still restore original and cancel confirmation.
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=original)throw new AssertionError("overwrite was not confirmed / click-load missed");
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=0xFF66C3EF;
        click(saveX,centerY);click(saveX,centerY);
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=original;
        dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS=345;UiLayoutMetrics.setInputHeight(24);
        click(x+40,centerY);
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=0xFF66C3EF || dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS!=345 || UiLayoutMetrics.inputHeight()!=24)throw new AssertionError("colors load changed layout or failed");
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=original;
        dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS=250;UiLayoutMetrics.reset();
        dev.kuan.kuanperion.ui.theme.Theme.saveTheme();
        System.out.println("UI_SMOKE_COLOR_PRESETS_PASS real empty/save/load/overwrite/confirm clicks + exactly five + layout isolation");
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

    private void testPreferenceReload(Minecraft mc) throws Exception {
        var cls=dev.kuan.kuanperion.KuanperionClient.class;
        var save=cls.getDeclaredMethod("saveConfig");save.setAccessible(true);
        var load=cls.getDeclaredMethod("loadConfig");load.setAccessible(true);
        save.invoke(null);
        var file=mc.gameDirectory.toPath().resolve("config/kuanperion.properties");
        byte[] original=java.nio.file.Files.readAllBytes(file);
        var prefs=new java.util.Properties();
        try(var in=java.nio.file.Files.newInputStream(file)){prefs.load(in);}
        prefs.setProperty("scanRadius","7");prefs.setProperty("actionDelayTicks","9");
        prefs.setProperty("farmingCrop","CARROT");prefs.setProperty("farmingAutoSetHome","true");
        prefs.setProperty("hotkey.mining.modifiers","7");prefs.setProperty("hotkey.farming.modifiers","0");
        prefs.setProperty("targets","minecraft:stone,minecraft:diamond_ore");
        prefs.setProperty("pestNotificationThreshold","4");
        try {
            try(var out=java.nio.file.Files.newOutputStream(file)){prefs.store(out,"upgrade fixture");}
            byte[] before=java.nio.file.Files.readAllBytes(file);
            load.invoke(null);
            if(!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(file)))throw new AssertionError("ordinary config load rewrites file");
            save.invoke(null);
            var after=new java.util.Properties();try(var in=java.nio.file.Files.newInputStream(file)){after.load(in);}
            for(String key:java.util.List.of("scanRadius","actionDelayTicks","farmingCrop","farmingAutoSetHome","hotkey.mining.modifiers","hotkey.farming.modifiers","targets","pestNotificationThreshold"))
                if(!prefs.getProperty(key).equals(after.getProperty(key)))throw new AssertionError("preference lost on reload: "+key);
            if(!after.getProperty("miningEnabled").equals("false") || !after.getProperty("farmingEnabled").equals("false"))throw new AssertionError("unsafe automatic resume");
        } finally {java.nio.file.Files.write(file,original);load.invoke(null);}
        System.out.println("UI_SMOKE_PREFERENCES_PASS config load byte preservation / nondefault Farming Mining notifications modifiers / safe runtime state");
    }

    private void testTheme(Minecraft mc) throws Exception {
        var saved=dev.kuan.kuanperion.ui.theme.Theme.exportJson();
        var file=mc.gameDirectory.toPath().resolve("config/kuanperion-theme.json");
        var mapField=dev.kuan.kuanperion.ui.theme.SkyThemePalette.class.getDeclaredField("LEGACY");mapField.setAccessible(true);
        var legacy=new com.google.gson.JsonObject();
        ((java.util.Map<String,String>)mapField.get(null)).forEach(legacy::addProperty);
        legacy.addProperty("uiScale",2.25);legacy.addProperty("textScale",1.2);legacy.addProperty("animSpeed",450);
        legacy.addProperty("HUD Title","FF010203");
        String original=legacy.toString();java.nio.file.Files.writeString(file,original);
        dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(!java.nio.file.Files.readString(file).equals(original))throw new AssertionError("loading rewrote saved legacy theme");
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=(int)Long.parseLong(legacy.get("Accent").getAsString(),16))throw new AssertionError("legacy color replaced");
        if(dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE!=2.25f || dev.kuan.kuanperion.ui.theme.Theme.HUD_TITLE!=0xFF010203)throw new AssertionError("upgrade overwrote preference");
        var inputOld=com.google.gson.JsonParser.parseString(saved).getAsJsonObject();
        inputOld.addProperty("themeSchema",2);inputOld.addProperty("inputHeight",16);
        java.nio.file.Files.writeString(file,inputOld.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(UiLayoutMetrics.inputHeight()!=16 || !java.nio.file.Files.readString(file).equals(inputOld.toString()))throw new AssertionError("upgrade overwrote input height/file");
        inputOld.addProperty("inputHeight",18);java.nio.file.Files.writeString(file,inputOld.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(UiLayoutMetrics.inputHeight()!=18)throw new AssertionError("old custom input height overwritten");
        inputOld.addProperty("themeSchema",3);inputOld.addProperty("inputHeight",16);java.nio.file.Files.writeString(file,inputOld.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(UiLayoutMetrics.inputHeight()!=16)throw new AssertionError("explicit new compact input overwritten");
        var skyField=dev.kuan.kuanperion.ui.theme.SkyThemePalette.class.getDeclaredField("PREVIOUS_SKY");skyField.setAccessible(true);
        var previousSky=new com.google.gson.JsonObject();
        ((java.util.Map<String,String>)skyField.get(null)).forEach(previousSky::addProperty);
        previousSky.addProperty("themeSchema",1);previousSky.addProperty("gradientEnabled",true);previousSky.addProperty("uiScale",2.25);
        java.nio.file.Files.writeString(file,previousSky.toString());dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        if(!java.nio.file.Files.readString(file).equals(previousSky.toString()) || dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=(int)Long.parseLong(previousSky.get("Accent").getAsString(),16))throw new AssertionError("previous sky preset overwritten");
        if(dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE!=2.25f)throw new AssertionError("sky load reset UI scale");
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
        System.out.println("UI_SMOKE_THEME_PASS legacy file byte preservation / stored input height / custom preservation / presets / gradient persistence");
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
