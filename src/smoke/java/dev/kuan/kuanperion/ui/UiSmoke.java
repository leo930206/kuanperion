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
                ui=new MainGUI();mc.setScreenAndShow(ui);phase=0;ticks=0;if(Boolean.getBoolean("kuanperion.smoke.hoverOnly")){ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Mining")).findFirst().orElseThrow());phase=29;}if(Boolean.getBoolean("kuanperion.smoke.capacityOnly")){setCapacityFixture(true);phase=32;}System.out.println("UI_SMOKE_OPEN");return;
            }
            boolean expanded = phase >= 13 && phase <= 16 || phase == 21 || phase==30;
            var frame = ui.context().layout;
            double cursorX = expanded ? (frame.px+20)*MainGUI.uiScale/frame.pr * mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth() : 1;
            double cursorY = expanded ? (frame.py+20)*MainGUI.uiScale/frame.pr * mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight() : 1;
            if(phase==30){cursorX=(frame.px+(ticks<70?20:120))*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth();cursorY=(MainGUISidebar.rowY(3,frame.py,frame.ph)+22)*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();}
            if(phase==31){cursorX=(frame.contX+(ticks<15?300:60))*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth();cursorY=(frame.contY+130)*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();}
            if(phase==29){cursorX=(frame.contX+220)*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth();cursorY=(frame.contY+150)*MainGUI.uiScale/frame.pr*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();}
            // Feed the vanilla cursor callback directly: background macOS windows may ignore GLFW cursor warps.
            // This exercises real hover/layout animation without forcing sidebarAnim or moving the user's pointer.
            if(mouseMove==null){mouseMove=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);mouseMove.setAccessible(true);}
            mouseMove.invoke(mc.mouseHandler,mc.getWindow().handle(),cursorX,cursorY);
            if(expanded && Math.abs(ui.scaleMouseX(mc.mouseHandler.getScaledXPos(mc.getWindow()))-(frame.px+(phase==30&&ticks>=70?120:20)))>1)throw new AssertionError("Retina hover coordinate conversion");
            if(phase==30&&ticks%25==0)System.out.println("UI_HOVER_DIAG tick="+ticks+" main="+ui.getActiveMainTab()+" anim="+ui.context().animation.sidebarAnim+" pointer="+ui.scaleMouseX(mc.mouseHandler.getScaledXPos(mc.getWindow()))+","+ui.scaleMouseY(mc.mouseHandler.getScaledYPos(mc.getWindow()))+" panel="+ui.panelX()+","+ui.panelY()+" hit="+ui.isSidebarExpandedOverContent(frame.px+120,MainGUISidebar.rowY(3,frame.py,frame.ph)+22));
            if(waiting||++ticks<35)return;
            if(expanded && ui.context().animation.sidebarAnim<.95f && ticks<140)return;
            if(phase==30&&ticks<105)return; // Enter collapsed rail, let it expand, then probe its covered card for 35 frames.
            ticks=0;
            String[] names={"01-overview","02-farming","03-mining","04-jerry","05-colors","06-auto-persistence","07-keybinds","08-settings","09-search","10-reopened","11-pest-inline","12-selected-targets","13-color-presets","14-farming-sidebar","15-mining-sidebar","16-other-sidebar","17-setting-sidebar","18-setting-colors","19-setting-interface-route","20-setting-keybinds","21-setting-interface","22-about-sidebar","23-resize-small","24-resize-restored","25-resize-large","26-webhook-wide","27-jerry-row","28-visitor-help","29-search-caret","30-hover-no-tooltip","31-sidebar-hover","32-module-hover","33-mining-full","34-mining-seven-slots","35-trade","36-stash","37-npc-sell","38-junk"};
            if(phase>=names.length){System.out.println("UI_SMOKE_PASS");mc.stop();return;}
            System.out.println("UI_SMOKE_STATE "+phase+" main="+ui.getActiveMainTab()+" module="+(ui.getActiveModuleSubTab()==null?"none":ui.getActiveModuleSubTab().name())+" live="+(mc.gui.screen()==ui));
            if(!phaseValidated) {
            if(phase==0) { testChatBodyStyles(); testSettingsCommand(mc); testSidebarRouting(); testStashUi(mc); testAutoSellUi(); testJunkUi(); testPestUi(); testGreenhouseUi(); testComposterUi(); testVisitorUi(); testSupercraftUi(); testJerryHomeGuard(mc); testSettingsAutomation(mc); testModIcon(); testSafety(mc); dev.kuan.kuanperion.AutomationGameViewTest.main(new String[0]); testTheme(mc); testPreferenceReload(mc); testNewUiFeatures(mc); testSelectedModels(); if(Boolean.getBoolean("kuanperion.smoke.chatOnly")){System.out.println("UI_SMOKE_PASS");mc.stop();return;} }
            if(phase==34)assertGroup(2);
            if(phase==35 && !ui.getActiveModuleSubTab().name().equals("Stash"))throw new AssertionError("Stash detail route");
            if(phase==1) testToggleClick();
            if(phase==4)testBuiltInPaletteClicks();
            if(phase==6)testKeybindClearButtons();
            if(phase==32 || phase==33) {
                assertMiningDetail("phase "+phase);
                var targets=ui.getActiveModuleSubTab().groups().get(1);
                var count=(dev.kuan.kuanperion.ui.settings.SelectionActionSetting)targets.getSettings().get(0);
                int size=selected().size();
                if(count.getMax()!=10-size || targets.getSettings().stream().filter(dev.kuan.kuanperion.ui.settings.Setting::isVisible).count()!=size+1)throw new AssertionError("capacity/ten target visibility");
                var inputRow=rowFor(count);
                if(inputRow[1]<ui.contentScrollTop() || inputRow[1]+inputRow[3]>ui.context().layout.py+ui.context().layout.ph-10)throw new AssertionError("capacity input is offscreen, click would not be real");
                if(phase==32){
                    if(count.canStart()||count.getValue()!=0)throw new AssertionError("full list must disable selection");
                    var row=rowFor(count);float fieldX=InlineRowLayout.countFieldX(row[0]+12,row[2]-24);
                    click(fieldX+30,UiLayoutMetrics.inputTop(row[1],row[3])+11);
                    if(ui.activeSliderField==count)throw new AssertionError("full count field is still editable");
                }else{
                    float fieldX=InlineRowLayout.countFieldX(inputRow[0]+12,inputRow[2]-24);
                    click(fieldX+30,UiLayoutMetrics.inputTop(inputRow[1],inputRow[3])+11);
                    if(ui.activeSliderField!=count)throw new AssertionError("available count field not editable");
                    ui.textBuf=new StringBuilder("99");ui.commitText();if(count.getValue()!=7 || !count.canStart() || !count.getDescription().contains("1～7"))throw new AssertionError("three saved allow seven additions only");}
                System.out.println("UI_SMOKE_MINING_CAPACITY_PASS saved="+size+" available="+count.getMax());
            }
            if(phase==30) {
                for(var section:ui.moduleSectionsForFilter())for(var sub:section.subtabs())if(ui.moduleCardHoverProgress(sub)>.01f)throw new AssertionError("hover pierced expanded sidebar");
                float old=ui.targetScrollY;ui.handleMouseScroll(frame.px+120,MainGUISidebar.rowY(3,frame.py,frame.ph)+22,-1);
                if(old!=ui.targetScrollY)throw new AssertionError("wheel pierced sidebar");
                click(frame.px+120,MainGUISidebar.rowY(3,frame.py,frame.ph)+22);
                if(ui.getActiveModuleSubTab()!=null || ui.context().navigation.activeFilter!=4)throw new AssertionError("click pierced sidebar into Jerry card");
                System.out.println("UI_SMOKE_TOP_LAYER_PASS sidebar hover/click/wheel cannot reach covered card");
            }
            if(phase==31 && ui.moduleCardHoverProgress(MainGUIRegistry.MODULE_SUBTABS.get(0))<.5f)throw new AssertionError("uncovered module lost hover feedback");
            if(phase==2 || phase==13) {
                var layout=ui.context().layout;
                float sidebarWidth=60+ui.context().animation.sidebarAnim*(ui.context().animation.computedSidebarExpanded-60);
                String caption=MainGUISidebar.footerText(MainGUIRegistry.modVersion(),phase==13);
                float width=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer().textWidth("Inter-Regular",caption,MainGUISidebar.FOOTER_FONT);
                if(width>sidebarWidth-24 || Math.abs(MainGUISidebar.footerTextX(layout.px,sidebarWidth,width)+width/2-(layout.px+sidebarWidth/2))>.01f)throw new AssertionError("footer not fitted/centered");
                System.out.println("UI_SMOKE_CENTERED_FOOTER_PASS expanded="+(phase==13)+" caption="+caption);
            }
            if(phase>=22) {
                var layout=ui.context().layout;
                if(Math.abs(layout.px+layout.pw/2-layout.physW/MainGUI.uiScale/2)>.1f || Math.abs(layout.py+layout.ph/2-layout.physH/MainGUI.uiScale/2)>.1f)throw new AssertionError("runtime resize off center");
            }
            if(phase==26) {
                var row=(dev.kuan.kuanperion.ui.settings.ToggleRowSetting)ui.getActiveModuleSubTab().groups().get(0).getSettings().get(0);
                boolean[] before=new boolean[4];for(int i=0;i<4;i++)before[i]=row.choices().get(i).getValue();
                var bounds=rowFor(row);float cell=(bounds[2]-24)/4;
                for(int i=0;i<4;i++){click(bounds[0]+12+cell*(i+.5f),bounds[1]+20);if(row.choices().get(i).getValue()==before[i])throw new AssertionError("Jerry cell click missed");}
                for(int i=0;i<4;i++)row.choices().get(i).setValue(before[i]);
                System.out.println("UI_SMOKE_JERRY_ROW_PASS four independent real cell clicks");
            }
            if(phase==27) {
                var setting=ui.getActiveModuleSubTab().groups().stream().flatMap(g->g.getSettings().stream()).filter(s->s.getName().equals("通知門檻")).findFirst().orElseThrow();
                var bounds=rowFor(setting);var metrics=new MainGUITextLayout();
                var renderer=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer();
                int lines=metrics.wrapTextToWidth(renderer,setting.getDescription(),"Inter-Regular",10.5f,metrics.descriptionWidth(setting,bounds[2])).size();
                float expected=2*UiLayoutMetrics.TITLE_TOP+metrics.settingLabelFontSize(setting)+UiLayoutMetrics.descriptionGap()+13*MainGUI.uiTextScale*lines+UiLayoutMetrics.ROW_GAP;
                if(Math.abs(ui.settingHeightFor(setting,bounds[2])-expected)>.01f)throw new AssertionError("unused estimated description whitespace");
            }
            if(phase==9) testSettingsShortcut(mc);
            if(phase==10) testInlineDuration();
            if(phase==11) testTargetRows();
            if(phase==12) testColorPresets();
            if(expanded && ui.context().animation.sidebarAnim < .95f) throw new AssertionError("sidebar did not expand under real mouse hover");
            if(phase>=13 && phase<=15) assertGroup(phase==15?3:phase-13);
            if(phase==16 && ui.getActiveMainTab()!=4) throw new AssertionError("gear did not open interface settings");
            if(phase>=17 && phase<=20) {
                int page=MainGUISettingsPages.normalize(phase-16);
                clickSettingsPage(page);
                if(ui.getActiveMainTab()!=page) throw new AssertionError("Setting subpage mouse route failed");
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
                case 22 -> org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(),820,600);
                case 23 -> org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(),1100,760);
                case 24 -> org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(),1440,900);
                case 25 -> {org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(),1100,760);ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Discord Webhook")).findFirst().orElseThrow());}
                case 26 -> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Jerry 偵測")).findFirst().orElseThrow());
                case 27 -> {var visitor=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Visitor")).findFirst().orElseThrow();var group=visitor.groups().stream().filter(g->g.getSettings().stream().anyMatch(s->s.getName().equals("通知門檻"))).findFirst().orElseThrow();var threshold=group.getSettings().stream().filter(s->s.getName().equals("通知門檻")).findFirst().orElseThrow();ui.openSearchResult(0,visitor,group,threshold);}
                case 28 -> {switchTab(1);ui.activateSearchField();ui.setSearchQuery("");ui.textBuf=new StringBuilder();ui.textCursor=0;}
                case 30 -> {clickSidebar(3);assertGroup(3);System.out.println("UI_HOVER_NAV main="+ui.getActiveMainTab()+" panel="+ui.panelX()+","+ui.panelY()+" anim="+ui.context().animation.sidebarAnim);}
                case 31 -> clickSidebar(0);
                case 32 -> {setCapacityFixture(true);}
                case 33 -> {setCapacityFixture(false);}
                case 34 -> clickSidebar(2);
                case 37 -> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Junk")).findFirst().orElseThrow());
                case 36 -> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Auto Sell")).findFirst().orElseThrow());
                case 35 -> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Stash")).findFirst().orElseThrow());
                case 29 -> {ui.setSearchMode(false);ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Mining")).findFirst().orElseThrow());ui.selectModuleCategory(2);}
                case 13 -> clickSidebar(0);
                case 14 -> clickSidebar(1);
                case 15 -> clickSidebar(3);
                case 16 -> clickSidebar(4);
                case 1-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.get(0));
                case 2-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow());
                case 3-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Jerry 偵測")).findFirst().orElseThrow());
                case 4-> switchTab(1);
                case 5-> switchTab(4);
                case 6->{switchTab(3);testChordCapture();}
                case 7->{((dev.kuan.kuanperion.ui.settings.KeybindSetting)dev.kuan.kuanperion.KuanperionClient.uiKeybinds().get(2)).resetToDefault();switchTab(4);}
                case 8->{ui.activateSearchField();ui.setSearchQuery("通知");}
                case 10->{var pest=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Pest")).findFirst().orElseThrow();var group=pest.groups().get(1);var setting=group.getSettings().stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.InlineToggleSetting).findFirst().orElseThrow();ui.openSearchResult(0,pest,group,setting);}
                case 12->{switchTab(1);ui.setActiveFilterIndex(0);ui.setActiveScroll(0,0);ui.refreshContext();if(ui.getActiveMainTab()!=1)throw new AssertionError("color transition did not select page immediately");System.out.println("UI_COLOR_TRANSITION main="+ui.getActiveMainTab());}
                case 11->{var mining=MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow();ui.openModuleDetailFromCard(mining);ui.selectModuleCategory(2);}
                case 9->{ui.onClose();if(mc.gui.screen()==ui)throw new AssertionError("close did not dismiss");ui=new MainGUI();mc.setScreenAndShow(ui);}
            }
        }catch(Throwable error){Screenshot.grab(mc.gameDirectory,"failure.png",mc.gameRenderer.mainRenderTarget(),1,message->{});System.out.println("UI_SMOKE_FAIL");error.printStackTrace();mc.stop();}
    }
    private void clickSidebar(int index) {
        var layout=ui.context().layout;
        click(layout.px+(ui.context().animation.sidebarAnim>.95f?100:30),MainGUISidebar.rowY(index,layout.py,layout.ph)+22);
    }
    private void assertGroup(int index) {
        String[][] expected={{"Farming","Pest","Greenhouse","Visitor","Composter"},{"Mining"},{"Auto Sell","Junk","Stash"},{"Jerry 偵測","Discord Webhook","Supercraft"}};
        var sections=ui.moduleSectionsForFilter();
        if(ui.getActiveMainTab()!=0 || ui.getActiveModuleSubTab()!=null || sections.size()!=1 || !sections.get(0).subtabs().stream().map(dev.kuan.kuanperion.ui.settings.ModulesTab.SubTab::name).toList().equals(java.util.List.of(expected[index]))) throw new AssertionError("wrong feature group: "+index+" actual="+sections.stream().map(s->s.subtabs().stream().map(dev.kuan.kuanperion.ui.settings.ModulesTab.SubTab::name).toList()).toList());
    }
    private void clickSettingsPage(int page) {
        String[] labels=MainGUISettingsPages.LABELS;
        int[] pages=MainGUISettingsPages.IDS;
        float x=ui.contentX()+20;
        var renderer=dev.kuan.kuanperion.renderer.NanoVGManager.getRenderer();
        for(int i=0;i<java.util.stream.IntStream.range(0,pages.length).filter(j->pages[j]==page).findFirst().orElseThrow();i++) x+=renderer.textWidth("Inter-Regular",labels[i],12)+28;
        click(x+20,ui.contentY()+72);
    }
    private void testSidebarRouting() throws Exception {
        for(int i=0;i<4;i++) {
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
        clickSidebar(4);
        if(ui.getActiveMainTab()!=4 || ui.getActiveModuleSubTab()!=null)throw new AssertionError("gear kept feature detail");
        for(int page:MainGUISettingsPages.IDS) {
            switchTab(page);
            if(MainGUISidebar.selectedIndex(ui.getActiveMainTab(),0)!=4 || ui.settingsNavigationHeight()!=37 || !ui.currentTabName().equals("Setting"))throw new AssertionError("Setting page not grouped");
        }
        var layout=ui.context().layout;click(layout.px+30,MainGUISidebar.footerY(layout.py,layout.ph)+11);
        if(ui.getActiveMainTab()!=5)throw new AssertionError("visible collapsed version did not open About");
        var lines=MainGUISidebar.footerLines(MainGUIRegistry.modVersion());
        if(!lines.equals(java.util.List.of("Mod Version "+MainGUIRegistry.modVersion()+"")))throw new AssertionError("wrong footer metadata");
        clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_SIDEBAR_PASS five visible routes / group membership / detail-back / cross-group search / Setting pages / collapsed footer / metadata");
    }
    private void testAutoSellUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);
        var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var sell=bridge.autoSell();
        if(sell.autoEnabled()||sell.active()||!sell.policy().allowedIds().isEmpty())throw new AssertionError("new disposal must be off/empty");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Auto Sell")).findFirst().orElseThrow();
        ui.openModuleDetailFromCard(sub);var rows=sub.groups().get(0).getSettings();
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(1)).setValue("WHEAT, CARROT_ITEM");
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(5)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");
        var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.AUTO_SELL)||saved.autoSellPolicy().maxStacks()!=2||!saved.autoSellPolicy().allowedIds().equals(java.util.Set.of("WHEAT","CARROT_ITEM"))||saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.STASH))throw new AssertionError("NPC policy/enable persist independently");
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(9)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(10)).execute();
        if(sell.active()||rows.get(11).isVisible())throw new AssertionError("offline NPC command must not start");
        sell.setPolicy(dev.kuan.kuanperion.automation.AutoSellPolicy.defaults());sell.setEnabled(false);
        clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_NPC_PASS real Auto Sell page/callbacks, persisted exact IDs/limits/default off, no offline command, no Stash interference");
    }
    private void testJunkUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);
        var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var junk=bridge.junk();
        if(junk.autoEnabled()||junk.active()||!junk.policy().allowedIds().isEmpty()||junk.policy().anchor()!=null)throw new AssertionError("Junk must start off/empty/no location");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Junk")).findFirst().orElseThrow();ui.openModuleDetailFromCard(sub);var rows=sub.groups().get(0).getSettings();
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(1)).setValue("SEEDS");
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(8)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(3)).execute();
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(13)).execute();
        if(junk.active()||junk.policy().anchor()!=null||rows.get(14).isVisible())throw new AssertionError("offline Junk must not record anchor/open inventory/drop");
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.JUNK)||saved.junkPolicy().maxStacks()!=2||!saved.junkPolicy().allowedIds().equals(java.util.Set.of("SEEDS"))||bridge.autoSell().autoEnabled()||bridge.stash().autoEnabled())throw new AssertionError("Junk UI preferences not independent/persistent");
        if(net.minecraft.world.inventory.InventoryMenu.SHIELD_SLOT!=45||net.minecraft.world.inventory.InventoryMenu.INV_SLOT_END!=36)throw new AssertionError("native inventory shape changed");
        junk.setPolicy(dev.kuan.kuanperion.automation.JunkPolicy.defaults());junk.setEnabled(false);clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_JUNK_PASS real settings/independent persistence/offline anchor and drop refusal/26.2 offhand shape/default restoration");
    }
    private void testPestUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);
        var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var pest=bridge.pest();
        if(pest.autoEnabled()||pest.active())throw new AssertionError("Pest automation must default off independently of notifications");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Pest")).findFirst().orElseThrow();ui.openModuleDetailFromCard(sub);
        var rows=sub.groups().get(0).getSettings();var notify=(dev.kuan.kuanperion.ui.settings.ToggleSetting)sub.groups().get(1).getSettings().get(0);boolean old=notify.getValue();
        notify.setValue(true);if(pest.autoEnabled())throw new AssertionError("notification flag enabled automation");
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);notify.setValue(false);
        if(!pest.autoEnabled())throw new AssertionError("notification-off disabled independent automation");
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(2)).setValue(3);
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(8)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(9)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(10)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(11)).setValue(0);
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(2)).setValue(3);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(12)).execute();
        if(pest.active()||rows.get(13).isVisible())throw new AssertionError("offline Pest request must not fly/use tools");
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.PEST)||saved.pestPolicy().threshold()!=3||!saved.pestPolicy().sweep()||saved.pestPolicy().travelPlots()!=2||!saved.pestPolicy().clearTraps()||saved.pestPolicy().trapPlot()!=0||bridge.autoSell().autoEnabled()||bridge.junk().autoEnabled())throw new AssertionError("Pest settings not independent/external/persistent");
        pest.setEnabled(false);pest.setPolicy(dev.kuan.kuanperion.automation.pest.PestPolicy.defaults());notify.setValue(old);clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_PEST_PASS real page/callbacks, independent notification and automation flags, external policy, no offline flight/use, safe default restoration");
    }
    private void testGreenhouseUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);
        var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var service=bridge.greenhouse();
        if(service.autoEnabled()||service.active()||service.policy().configured())throw new AssertionError("Greenhouse must start off/unconfigured");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Greenhouse")).findFirst().orElseThrow();ui.openModuleDetailFromCard(sub);var rows=sub.groups().get(0).getSettings();
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);if(service.autoEnabled())throw new AssertionError("unconfigured enable must fail closed");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(2)).setValue("1, 24");
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(3)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(6)).execute();if(!service.policy().toolId().isEmpty())throw new AssertionError("offline cannot capture a guessed tool");
        service.setPolicy(service.policy().withTool("FARMING_HOE",new java.util.UUID(0,9).toString()));
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(10)).setValue(3);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(13)).execute();
        if(service.active()||rows.get(14).isVisible())throw new AssertionError("offline Greenhouse must not move/travel/attack");
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.GREENHOUSE)||!saved.greenhousePolicy().plots().equals(java.util.Set.of(1,24))||!saved.greenhousePolicy().kinds().equals(java.util.Set.of(dev.kuan.kuanperion.automation.greenhouse.GreenhouseIdentity.Kind.ASHWREATH))||saved.greenhousePolicy().maxTargets()!=3||bridge.pest().autoEnabled()||bridge.junk().autoEnabled())throw new AssertionError("Greenhouse UI preferences not independent/external");
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(8)).execute();if(service.policy().configured()||service.autoEnabled())throw new AssertionError("clearing exact tool consent disables effective automation");
        service.setEnabled(false);service.setPolicy(dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy.defaults());clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_GREENHOUSE_PASS real Farming module/callbacks, explicit plot/crop/tool persistence, no offline travel/attack, safe default restoration");
    }
    private void testComposterUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var service=bridge.composter();
        if(service.autoEnabled()||service.active()||service.policy().configured())throw new AssertionError("Composter must default off/no machine/no materials");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Composter")).findFirst().orElseThrow();ui.openModuleDetailFromCard(sub);var rows=sub.groups().get(0).getSettings();
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(2)).execute();if(service.policy().anchor()!=null)throw new AssertionError("offline machine capture guessed a position");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(4)).setValue("BOX_OF_SEEDS=1000");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(5)).setValue("VOLTA=2000");
        service.setPolicy(service.policy().withAnchor(new dev.kuan.kuanperion.automation.composter.ComposterPolicy.Anchor(-11,72,-27,"minecraft:player_head")));
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(7)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(12)).execute();
        if(service.active()||rows.get(13).isVisible())throw new AssertionError("offline Composter cannot fly/open/consume");
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.COMPOSTER)||saved.composterPolicy().materials().size()!=2||saved.composterPolicy().maxStacks()!=2||bridge.greenhouse().autoEnabled()||bridge.pest().autoEnabled())throw new AssertionError("Composter settings not independent/external");
        service.setEnabled(false);service.setPolicy(dev.kuan.kuanperion.automation.composter.ComposterPolicy.defaults());clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_COMPOSTER_PASS real Farming module/callbacks, explicit machine/material/unit persistence, no offline flight/consume, safe default restoration");
    }
    private void testVisitorUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var service=bridge.visitor();
        if(service.autoEnabled()||service.active()||service.policy().configured())throw new AssertionError("Visitor must default off/no material or visitor authority");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Visitor")).findFirst().orElseThrow();ui.openModuleDetailFromCard(sub);var rows=sub.groups().get(0).getSettings();
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);if(service.autoEnabled())throw new AssertionError("notification/defaults cannot authorize Visitor");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(2)).setValue("Librarian; Farmer");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(3)).setValue("ENCHANTED_WHEAT");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)rows.get(4)).setValue("2000");
        ((dev.kuan.kuanperion.ui.settings.SliderSetting)rows.get(6)).setValue(2);
        ((dev.kuan.kuanperion.ui.settings.ToggleSetting)rows.get(0)).setValue(true);
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)rows.get(10)).execute();if(service.active()||rows.get(11).isVisible())throw new AssertionError("offline Visitor must not lookup/travel/interact/consume");
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.VISITOR)||!saved.visitorPolicy().rules().visitors().equals(java.util.Set.of("Librarian","Farmer"))||!saved.visitorPolicy().rules().materialIds().equals(java.util.Set.of("ENCHANTED_WHEAT"))||saved.visitorPolicy().rules().maximumOfferCoins()!=2000||saved.visitorPolicy().threshold()!=2||bridge.greenhouse().autoEnabled()||bridge.composter().autoEnabled())throw new AssertionError("Visitor UI settings not independent/external");
        var controllerField=bridge.getClass().getDeclaredField("visitorFlight");controllerField.setAccessible(true);var controller=controllerField.get(bridge);for(String name:java.util.List.of("catalog","prices")){var f=controller.getClass().getDeclaredField(name);f.setAccessible(true);if(f.get(controller)!=null)throw new AssertionError("offline UI must not initialize public network client");}
        byte[] before=java.nio.file.Files.readAllBytes(path);bridge.suspendForJerry();if(service.autoEnabled()||!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(path)))throw new AssertionError("idle Jerry Visitor suspension changes saved preferences");
        service.setEnabled(false);service.setPolicy(dev.kuan.kuanperion.automation.visitor.VisitorPolicy.defaults());clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_VISITOR_PASS real Farming module/callbacks, inventory-only explicit authority/external persistence, notification-independent, no offline network/gameplay, idle Jerry suspension");
    }
    private void testSupercraftUi() throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var service=bridge.supercraft();
        if(service.autoEnabled()||service.active()||service.pending()||service.policy().configured())throw new AssertionError("Supercraft defaults grant no authority");
        clickSidebar(3);var tab=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Supercraft")).findFirst().orElseThrow();if(!MainGUIRegistry.MODULE_SECTIONS.stream().filter(v->v.id().equals("other")).findFirst().orElseThrow().subtabs().contains(tab))throw new AssertionError("Supercraft must be Other module");ui.openModuleDetailFromCard(tab);
        var group=tab.groups().getFirst();var recipe=(dev.kuan.kuanperion.ui.settings.TextSetting)group.getSettings().stream().filter(v->v.getName().equals("配方（產物;產量;堆疊;材料=數量）")).findFirst().orElseThrow();recipe.setValue("ENCHANTED_WHEAT;1;64;WHEAT=160");
        var count=(dev.kuan.kuanperion.ui.settings.TextSetting)group.getSettings().stream().filter(v->v.getName().equals("本批產物數量")).findFirst().orElseThrow();count.setValue("1");
        for(var setting:group.getSettings())if(setting instanceof dev.kuan.kuanperion.ui.settings.SliderSetting slider){switch(slider.getName()){case "單批產物上限"->slider.setValue(1);case "單批材料消耗上限"->slider.setValue(160);case "單批合成次數上限"->slider.setValue(1);}}
        if(!service.policy().configured())throw new AssertionError("Supercraft UI recipe/caps callbacks must configure exact batch");((dev.kuan.kuanperion.ui.settings.ToggleSetting)group.getSettings().getFirst()).setValue(true);
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");var saved=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);if(!saved.enabled(dev.kuan.kuanperion.automation.AutomationFeature.SUPERCRAFT)||!saved.supercraftPolicy().equals(service.policy()))throw new AssertionError("Supercraft exact policy must persist outside jar");
        var controllerField=bridge.getClass().getDeclaredField("supercraftMenu");controllerField.setAccessible(true);var controller=controllerField.get(bridge);var catalog=controller.getClass().getDeclaredField("catalogClient");catalog.setAccessible(true);if(catalog.get(controller)!=null||bridge.session().active())throw new AssertionError("offline UI must not fetch catalog/acquire root");
        var run=(dev.kuan.kuanperion.ui.settings.ActionSetting)group.getSettings().stream().filter(v->v.getName().equals("關閉選單後執行一批")).findFirst().orElseThrow();run.execute();if(!service.pending()||service.active())throw new AssertionError("explicit UI action must defer, not replace settings");
        byte[] before=java.nio.file.Files.readAllBytes(path);bridge.suspendForJerry();if(service.pending()||service.autoEnabled()||service.active()||!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(path)))throw new AssertionError("Jerry must clear pending and suspend idle automatic craft without settings rewrite");
        if(catalog.get(controller)!=null||bridge.session().active())throw new AssertionError("no offline command/data/gameplay");service.setEnabled(false);service.setPolicy(dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy.defaults());clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_SUPERCRAFT_PASS real Other/callbacks/external exact policy, pending action/Jerry cancellation, no offline data/gameplay");
    }
    private void testJerryHomeGuard(Minecraft mc) throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");byte[] before=java.nio.file.Files.readAllBytes(path);
        if(mc.player!=null||bridge.sendAutomaticJerryHome()||bridge.session().active()||bridge.holdsFarming())throw new AssertionError("offline trusted Jerry record-only command must not send or acquire work");
        if(!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(path)))throw new AssertionError("record-only sender must not mutate persisted settings");
        var closed=new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(7);if(closed.getContainerId()!=7)throw new AssertionError("pinned26.2 close packet identity");bridge.beforeServerClose(closed);bridge.afterServerClose(closed);
        bridge.suspendForJerry();
        if(bridge.session().active()||!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(path)))throw new AssertionError("idle Jerry global stop must not acquire a lease or rewrite latest settings");
        System.out.println("UI_SMOKE_JERRY_GLOBAL_STOP_PASS native offline bridge suspends all eight services without preference/lease side effects");
        System.out.println("UI_SMOKE_JERRY_HOME_GUARD_PASS actual native bridge refuses no-player send without lease/preference side effects");
    }
    private void testStashUi(Minecraft mc) throws Exception {
        var field=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("automation");field.setAccessible(true);
        var bridge=(dev.kuan.kuanperion.AutomationClientBridge)field.get(null);var stash=bridge.stash();
        var path=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("kuanperion/automation.json");
        if(stash.autoEnabled()||stash.active()||java.nio.file.Files.exists(path))throw new AssertionError("fresh automation must be off and not save on startup");
        var sub=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Stash")).findFirst().orElseThrow();
        ui.openModuleDetailFromCard(sub);var settings=sub.groups().get(0).getSettings();
        var auto=(dev.kuan.kuanperion.ui.settings.ToggleSetting)settings.get(0);
        auto.setValue(true);
        if(!stash.autoEnabled()||stash.active())throw new AssertionError("enable must persist without starting any commands");
        var delay=(dev.kuan.kuanperion.ui.settings.SliderSetting)settings.get(2);delay.setValue(7);
        var reloaded=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(!reloaded.enabled(dev.kuan.kuanperion.automation.AutomationFeature.STASH)||reloaded.stashPolicy().delaySeconds()!=7)throw new AssertionError("real UI changes not persisted");
        ((dev.kuan.kuanperion.ui.settings.ActionSetting)settings.get(5)).execute();
        if(stash.active())throw new AssertionError("offline client must refuse manual Stash without queueing");
        if(settings.get(6).isVisible())throw new AssertionError("idle cancel should be hidden");
        auto.setValue(false);delay.setValue(3);
        reloaded=dev.kuan.kuanperion.automation.AutomationSettingsStore.load(path);
        if(reloaded.enabled(dev.kuan.kuanperion.automation.AutomationFeature.STASH)||reloaded.stashPolicy().delaySeconds()!=3)throw new AssertionError("restored stash settings not persisted");
        clickSidebar(0);assertGroup(0);
        System.out.println("UI_SMOKE_STASH_PASS Trade/detail/settings persistence/default off/no offline command/no runtime auto-start");
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
        var settings=ui.getActiveModuleSubTab().groups().stream().flatMap(g->g.getSettings().stream()).toList();
        var setting=(dev.kuan.kuanperion.ui.settings.InlineToggleSetting)settings.stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.InlineToggleSetting).findFirst().orElseThrow();
        var row=rowFor(setting);boolean before=setting.isOn();
        click(row[0]+row[2]-100,UiLayoutMetrics.inputTop(row[1],row[3])+11);
        if(ui.activeText!=setting || setting.isOn()!=before)throw new AssertionError("duration click toggled checkbox / did not focus editor");
        if(ui.activeEditorH!=22)throw new AssertionError("notification input not compact default height");
        if(Math.abs(ui.activeEditorY+ui.activeEditorH/2-(row[1]+(row[3]-5)/2))>.01f)throw new AssertionError("input not full-card centered");
        ui.textBuf=new StringBuilder("2.5");ui.commitText();
        if(!setting.getValue().equals("2.5"))throw new AssertionError("duration did not save");
        setting.setValue("");
        if(!setting.getValue().isEmpty())throw new AssertionError("blank should preserve placeholder state");
        click(row[0]+row[2]-40,UiLayoutMetrics.checkboxTop(row[1],row[3])+19);
        if(setting.isOn()==before||!setting.getValue().isEmpty())throw new AssertionError("checkbox click edited time / failed toggle");
        var threshold=(dev.kuan.kuanperion.ui.settings.SliderSetting)settings.stream().filter(s->s instanceof dev.kuan.kuanperion.ui.settings.SliderSetting&&s.getName().equals("通知門檻")).findFirst().orElseThrow();
        var numberRow=rowFor(threshold);float innerW=numberRow[2]-24;
        click(numberRow[0]+12+innerW-innerW/3+33,UiLayoutMetrics.inputTop(numberRow[1],numberRow[3])+11);
        if(ui.activeSliderField!=threshold || ui.activeEditorH!=22 || Math.abs(ui.activeEditorY+11-(numberRow[1]+(numberRow[3]-5)/2))>.01f)throw new AssertionError("threshold input not centered/enlarged/clickable");
        ui.commitText();
        var values=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(!values.get("pestNotificationScreenSeconds").getAsString().equals("3"))throw new AssertionError("blank duration needs safe default");
        System.out.println("UI_SMOKE_INLINE_DURATION_PASS actual mouse hit regions / editor commit / blank placeholder / independent toggle");
    }
    private float[] paletteRow(dev.kuan.kuanperion.ui.settings.Setting target){
        float x=ui.contentX()+20,w=ui.contentW()-40,y=ui.contentScrollTop()+51;
        for(var item:MainGUIRegistry.COLORS_SUBTABS.get(0).groups().get(0).getSettings()){
            if(!item.isVisible())continue;float h=ui.settingHeightFor(item,w);
            if(item==target)return new float[]{x,y,w,h};y+=h;
        }
        throw new AssertionError("palette row hidden");
    }
    private void addPalette(dev.kuan.kuanperion.ui.settings.Setting add){
        float[] row=paletteRow(add);click(row[0]+row[2]/2,UiLayoutMetrics.controlTop(row[1],row[3],32)+16);
    }
    private void testColorPresets() throws Exception {
        var subtab=MainGUIRegistry.COLORS_SUBTABS.get(0);
        if(ui.flatSubtabsForCurrentFilter().size()!=3 || ui.flatSubtabsForCurrentFilter().get(0)!=subtab)throw new AssertionError("palette collection unreachable");
        var slots=subtab.groups().get(0).getSettings();
        if(slots.size()!=6)throw new AssertionError("five persistent slots plus add action");
        var add=slots.get(5);
        if(slots.stream().filter(dev.kuan.kuanperion.ui.settings.ColorPresetSetting.class::isInstance).anyMatch(dev.kuan.kuanperion.ui.settings.Setting::isVisible))throw new AssertionError("fresh collection must be empty");
        var first=(dev.kuan.kuanperion.ui.settings.ColorPresetSetting)slots.get(0);
        int original=dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY;
        addPalette(add);if(!first.isVisible())throw new AssertionError("add button missed");
        if(first.previewColors().size()!=dev.kuan.kuanperion.ui.theme.Theme.ENTRIES.size() || first.previewColors().get(0)!=original || first.previewColors().get(1)!=dev.kuan.kuanperion.ui.theme.Theme.ACCENT_SECONDARY)throw new AssertionError("saved swatch order not menu order");
        float[] row=paletteRow(first);
        if(row[3]!=50 || ColorPresetRow.visibleCount(first,row[2]-24)>=first.previewColors().size())throw new AssertionError("palette preview must be a single truncated row at this window width");
        float x=row[0]+12,w=row[2]-24,cy=UiLayoutMetrics.controlTop(row[1],row[3],28)+14;
        float saveX=ColorPresetRow.saveX(x,w)+32,deleteX=ColorPresetRow.deleteX(x,w)+32;
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=0xFF66C3EF;
        click(saveX,cy);click(x+10,cy);
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=original)throw new AssertionError("save overwrote without confirmation / swatch load missed");
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=0xFF66C3EF;click(saveX,cy);click(saveX,cy);
        if(first.previewColors().get(0)!=0xFF66C3EF)throw new AssertionError("save did not refresh saved swatches");
        dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=original;
        dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS=345;UiLayoutMetrics.setInputHeight(24);
        click(x+10,cy);
        if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=0xFF66C3EF || dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS!=345 || UiLayoutMetrics.inputHeight()!=24)throw new AssertionError("palette load changed non-color preference");
        click(deleteX,cy);if(first.isVisible() || dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=0xFF66C3EF)throw new AssertionError("delete did not remove row / changed active colors");
        addPalette(add);dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY=original;addPalette(add);
        // Fill the collection using its actual add control; then leave two examples for screenshots.
        for(int i=2;i<5;i++)addPalette(add);
        if(add.isVisible())throw new AssertionError("sixth palette can be added");
        for(int i=2;i<5;i++)((dev.kuan.kuanperion.ui.settings.ColorPresetSetting)slots.get(i)).delete();
        if(!add.isVisible())throw new AssertionError("delete did not restore add control");
        dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS=250;UiLayoutMetrics.reset();
        dev.kuan.kuanperion.ui.theme.Theme.saveTheme();
        ui.setActiveScroll(0,0);ui.refreshContext();
        System.out.println("UI_SMOKE_COLOR_PRESETS_PASS empty/add/swatch order/save/confirm/load/delete/five-limit/re-add/independent theme layout");
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

    private void testBuiltInPaletteClicks() {
        var subtabs=ui.flatSubtabsForCurrentFilter();
        if(subtabs.size()!=3 || !subtabs.get(0).name().equals("配色收藏") || !subtabs.get(1).name().equals("Colour Presets") || !subtabs.get(2).name().equals("選單顏色"))throw new AssertionError("palette order");
        if(MainGUIRegistry.SETTINGS_SUBTABS.stream().flatMap(t->t.groups().stream()).flatMap(g->g.getSettings().stream()).anyMatch(a->a instanceof dev.kuan.kuanperion.ui.settings.InfoSetting))throw new AssertionError("redundant interface info row remains");
        float x=ui.contentX()+20,w=ui.contentW()-40,y=ui.contentScrollTop()-ui.scrollY;
        float rowY=0;
        outer:for(var tab:subtabs)for(var group:tab.groups()){
            y+=51;
            for(var setting:group.getSettings()){
                if(!setting.isVisible())continue;
                if(setting instanceof dev.kuan.kuanperion.ui.settings.ThemePresetsSetting){rowY=y;break outer;}
                y+=ui.settingHeightFor(setting,w);
            }
            y+=4;
        }
        String before=dev.kuan.kuanperion.ui.theme.Theme.exportJson();
        float scale=dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE,text=dev.kuan.kuanperion.ui.theme.Theme.TEXT_SCALE,anim=dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS,height=UiLayoutMetrics.inputHeight();
        int cols=ThemePresetRow.columns(w);float bw=ThemePresetRow.buttonWidth(w);
        var presets=dev.kuan.kuanperion.ui.theme.ThemePreset.values();
        for(int i=0;i<presets.length;i++){
            click(x+(i%cols)*(bw+8)+bw/2,rowY+i/cols*48+20);
            int expected=(int)Long.parseLong(com.google.gson.JsonParser.parseString(presets[i].json()).getAsJsonObject().get("Accent").getAsString(),16);
            if(dev.kuan.kuanperion.ui.theme.Theme.ACCENT_PRIMARY!=expected)throw new AssertionError("preset button missed "+presets[i].label());
            if(scale!=dev.kuan.kuanperion.ui.theme.Theme.UI_SCALE||text!=dev.kuan.kuanperion.ui.theme.Theme.TEXT_SCALE||anim!=dev.kuan.kuanperion.ui.theme.Theme.ANIM_TIME_MS||height!=UiLayoutMetrics.inputHeight())throw new AssertionError("built-in colors changed UI layout");
        }
        dev.kuan.kuanperion.ui.theme.Theme.importJson(before);dev.kuan.kuanperion.ui.theme.Theme.saveTheme();
        System.out.println("UI_SMOKE_BUILTINS_PASS six actual buttons/order/English names/layout retained/interface info removed");
    }

    private void testKeybindClearButtons() {
        float x=ui.contentX()+32,w=ui.contentW()-64,y=ui.contentScrollTop()+51;
        for(var item:MainGUIRegistry.KEYBINDS_SUBTABS.get(0).groups().get(0).getSettings()){
            var key=(dev.kuan.kuanperion.ui.settings.KeybindSetting)item;
            float h=ui.settingHeightFor(key,w+24),cy=y+(h-5)/2;
            click(InlineRowLayout.keybindClearX(x,w)+26,cy);
            if(!key.getKeyMapping().isUnbound()||key.getModifiers()!=0||!key.getBoundKeyName().equals("Not Bound"))throw new AssertionError("clear did not unbind "+key.getName());
            click(x+w-30,cy);if(!key.isDefault())throw new AssertionError("Reset after Clear failed "+key.getName());
            y+=h;
        }
        System.out.println("UI_SMOKE_CLEAR_KEYS_PASS all six clear/reset buttons / modifiers zero / default restoration");
    }

    private void testNewUiFeatures(Minecraft mc) throws Exception {
        var client=dev.kuan.kuanperion.KuanperionClient.class;
        if(java.util.Arrays.stream(net.minecraft.client.sounds.SoundEngine.class.getDeclaredMethods()).noneMatch(m->m.getName().contains("automationVolume")))throw new AssertionError("26.2 sound gain Mixin not applied");
        if(MainGUIRegistry.PROFILES_SUBTABS.size()!=0||java.util.Arrays.stream(MainGUISettingsPages.IDS).anyMatch(id->id==2))throw new AssertionError("profile page still exposed");
        var farmSettings=MainGUIRegistry.MODULE_SUBTABS.get(0).groups().get(0).getSettings();
        var mineSettings=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Mining")).findFirst().orElseThrow().groups().get(0).getSettings();
        var farmMute=(dev.kuan.kuanperion.ui.settings.ToggleSetting)farmSettings.stream().filter(t->t.getName().equals("開啟 Farming 後關閉遊戲音量")).findFirst().orElseThrow();
        var mineMute=(dev.kuan.kuanperion.ui.settings.ToggleSetting)mineSettings.stream().filter(t->t.getName().equals("開啟 Mining 後關閉遊戲音量")).findFirst().orElseThrow();
        boolean oldFarm=farmMute.getValue(),oldMine=mineMute.getValue();
        farmMute.setValue(true);mineMute.setValue(false);if(!farmMute.getValue()||mineMute.getValue())throw new AssertionError("independent mute switches");
        farmMute.setValue(false);mineMute.setValue(true);if(farmMute.getValue()||!mineMute.getValue())throw new AssertionError("reverse independent mute switches");
        farmMute.setValue(oldFarm);mineMute.setValue(oldMine);
        var stall=(dev.kuan.kuanperion.ui.settings.InlineToggleSetting)farmSettings.stream().filter(t->t.getName().equals("一段時間都沒動偵測")).findFirst().orElseThrow();
        String oldSeconds=stall.getValue();stall.setValue("8.25");if(!stall.getValue().equals("8.25")||stall.fieldWidth(500)!=66||!stall.centered())throw new AssertionError("custom centered compact stall input");stall.setValue(oldSeconds);
        var prefix=client.getDeclaredMethod("hsbaPrefix");prefix.setAccessible(true);
        var component=(net.minecraft.network.chat.Component)prefix.invoke(null);
        if(!component.getString().equals("[Kuanperion] "))throw new AssertionError("chat prefix");
        var chars=component.getSiblings();
        if(chars.stream().anyMatch(c->c.getStyle().isBold()) || chars.get(0).getStyle().getColor().getValue()!=0xAAAAAA || chars.get(1).getStyle().getColor().getValue()!=0x13A9F2 || chars.get(chars.size()-3).getStyle().getColor().getValue()!=0xE64478 || chars.get(chars.size()-2).getStyle().getColor().getValue()!=0xAAAAAA)throw new AssertionError("nonbold gradient endpoints");
        var keys=dev.kuan.kuanperion.KuanperionClient.uiKeybinds();
        if(keys.size()!=6 || !((dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(3)).isDefault() || !((dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(4)).isDefault())throw new AssertionError("home/sethome defaults");
        String[] defaults={"Shift + S","Shift + G","Shift + B"};
        for(int i=0;i<3;i++)if(!((dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(i+3)).getBoundKeyName().equals(defaults[i]))throw new AssertionError("home/barn default chord "+i);
        var home=(dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(4);
        var sethome=(dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(3);
        ui.beginKeybindCapture(home);ui.keyPressed(new net.minecraft.client.input.KeyEvent(341,0,2));ui.keyPressed(new net.minecraft.client.input.KeyEvent(72,0,2));
        if(!home.getName().equals("/warp garden")||home.getModifiers()!=2 || !home.getBoundKeyName().equals("Ctrl + H"))throw new AssertionError("home chord capture failed");
        sethome.setBoundKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.h"),2);
        if(!sethome.isDefault())throw new AssertionError("duplicate home/sethome chord accepted");
        home.resetToDefault();
        var barn=(dev.kuan.kuanperion.ui.settings.KeybindSetting)keys.get(5);
        barn.setBoundKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.g"),1);
        if(!barn.getBoundKeyName().equals("Shift + B"))throw new AssertionError("barn duplicate home chord accepted");
        home.setBoundKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.b"),1);
        if(!home.getBoundKeyName().equals("Shift + G"))throw new AssertionError("home duplicate barn chord accepted");
        barn.setBoundKey(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.j"),4);
        if(!barn.getBoundKeyName().equals("Alt + J"))throw new AssertionError("barn custom chord not saved");
        var savedKeys=java.nio.file.Files.readString(mc.gameDirectory.toPath().resolve("options.txt"));
        if(!savedKeys.contains("key_key.kuanperion.barn:key.keyboard.j"))throw new AssertionError("barn key not outside jar in options.txt");
        barn.resetToDefault();
        var jerry=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Jerry 偵測")).findFirst().orElseThrow();
        var row=(dev.kuan.kuanperion.ui.settings.ToggleRowSetting)jerry.groups().get(0).getSettings().get(0);
        if(row.choices().size()!=4 || row.choices().stream().anyMatch(t->!t.getName().endsWith(" Jerry")))throw new AssertionError("one four-color row");
        var pest=MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Pest")).findFirst().orElseThrow().groups().get(1).getSettings().stream().filter(t->t.getName().equals("通知門檻")).findFirst().orElseThrow();
        if(pest.getDescription().contains("Visitor"))throw new AssertionError("cross-feature threshold help");
        var marker=jerry.groups().get(0).getSettings().stream().filter(t->t.getName().startsWith("光柱")).findFirst().orElseThrow();
        String previous=((dev.kuan.kuanperion.ui.settings.TextSetting)marker).getValue();
        ((dev.kuan.kuanperion.ui.settings.TextSetting)marker).setValue("0");
        if(!((dev.kuan.kuanperion.ui.settings.TextSetting)marker).getValue().equals("0"))throw new AssertionError("zero beam config");
        ((dev.kuan.kuanperion.ui.settings.TextSetting)marker).setValue(previous);
        var transition=MainGUI.class.getDeclaredField("transitionRenderer");transition.setAccessible(true);
        var sync=MainGUITransitionRenderer.class.getDeclaredMethod("syncContentTransition");sync.setAccessible(true);
        ui.activateSearchField();
        for(String query:new String[]{"","a","ab","abc","通知"}) {
            ui.setSearchQuery(query);ui.refreshContext();sync.invoke(transition.get(ui));
            if(ui.context().transition.fromState!=null)throw new AssertionError("typing restarted content slide");
        }
        ui.setSearchQuery("");ui.setSearchMode(false);ui.refreshContext();
        System.out.println("UI_SMOKE_13_FEATURES_PASS nonbold gradient / fourth shortcut / one Jerry row / zero beam / threshold help / stable search transition");
    }

    private void testChatBodyStyles() throws Exception {
        var cls=dev.kuan.kuanperion.KuanperionClient.class;
        String[] names={"plain","feature","success","error","warning","blockName","locationName","cropName"};
        for(String name:names){
            var method=cls.getDeclaredMethod(name,String.class);method.setAccessible(true);
            var component=(net.minecraft.network.chat.Component)method.invoke(null,"樣本 Sample");
            final int expected=name.equals("plain")?0x00D8F5:0x0088D1;
            component.visit((net.minecraft.network.chat.Style style,String text)->{if(style.isBold()||style.getColor()==null||style.getColor().getValue()!=expected)throw new AssertionError("chat role palette/no-bold: "+name);return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY.withBold(true));
        }
        var prefix=cls.getDeclaredMethod("hsbaPrefix");prefix.setAccessible(true);
        ((net.minecraft.network.chat.Component)prefix.invoke(null)).visit((net.minecraft.network.chat.Style style,String text)->{if(style.isBold())throw new AssertionError("prefix inherits bold");return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY.withBold(true));
        System.out.println("UI_SMOKE_CHAT_STYLE_PASS actual private factories/all eight roles/no inherited bold/prefix retained");
    }

    private void testSettingsCommand(Minecraft mc) throws Exception {
        var process=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredMethod("tickSettingsCommand",Minecraft.class);process.setAccessible(true);
        for(String command:new String[]{"kuanperion","Kuanperion","KUANPERION","kUaNpErIoN"}){
            var before=mc.gui.screen();
            if(net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.invoker().allowSendCommandMessage(command))throw new AssertionError("local command leaked to server");
            if(mc.gui.screen()!=before)throw new AssertionError("opened before ChatScreen finished closing");
            mc.setScreenAndShow(null);process.invoke(null,mc);
            if(!(mc.gui.screen() instanceof MainGUI))throw new AssertionError("command did not open same settings UI");
            ui=(MainGUI)mc.gui.screen();process.invoke(null,mc);
            if(mc.gui.screen()!=ui)throw new AssertionError("command repeated next tick");
        }
        for(String other:new String[]{"home","barn","sethome","kuanperion other","kuanperionx"})if(!net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.invoker().allowSendCommandMessage(other))throw new AssertionError("unrelated server command cancelled");
        System.out.println("UI_SMOKE_SETTINGS_COMMAND_PASS registered Fabric event / mixed case / deferred open / server command isolation");
    }

    private void assertMiningDetail(String stage) {
        var sub=ui.getActiveModuleSubTab();
        if(ui.getActiveMainTab()!=0 || sub==null || !sub.name().equals("Mining"))throw new AssertionError("Mining route changed at "+stage+": main="+ui.getActiveMainTab()+" filter="+ui.context().navigation.activeFilter+" module="+(sub==null?"none":sub.name()));
    }
    private void setCapacityFixture(boolean full) throws Exception {
        selected().clear();selected().addAll(java.util.List.of("minecraft:stone","minecraft:diamond_ore","minecraft:coal_ore"));
        if(full)selected().addAll(java.util.List.of("minecraft:iron_ore","minecraft:gold_ore","minecraft:cobblestone","minecraft:dirt","minecraft:sand","minecraft:gravel","minecraft:oak_log"));
        ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(t->t.name().equals("Mining")).findFirst().orElseThrow());
        ui.selectModuleCategory(2);
        assertMiningDetail("fixture before count");
        var count=(dev.kuan.kuanperion.ui.settings.SelectionActionSetting)ui.getActiveModuleSubTab().groups().get(1).getSettings().get(0);count.setValue(99);
        assertMiningDetail("fixture after count");
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
        prefs.setProperty("hotkey.mining.modifiers","7");prefs.setProperty("hotkey.farming.modifiers","0");prefs.setProperty("hotkey.home.modifiers","7");
        prefs.setProperty("hotkey.sethome.modifiers","0");prefs.setProperty("hotkey.barn.modifiers","4");
        prefs.setProperty("targets","minecraft:stone,minecraft:diamond_ore,minecraft:coal_ore,minecraft:iron_ore,minecraft:gold_ore,minecraft:cobblestone,minecraft:dirt,minecraft:sand,minecraft:gravel,minecraft:oak_log");prefs.setProperty("miningSelectionTargetCount","0");
        prefs.setProperty("pestNotificationThreshold","4");
        prefs.setProperty("farmingMuteSoundEnabled","true");prefs.setProperty("miningMuteSoundEnabled","false");
        prefs.setProperty("farmingStallDetectionEnabled","false");prefs.setProperty("farmingStallSeconds","8.25");
        try {
            try(var out=java.nio.file.Files.newOutputStream(file)){prefs.store(out,"upgrade fixture");}
            byte[] before=java.nio.file.Files.readAllBytes(file);
            load.invoke(null);
            if(!java.util.Arrays.equals(before,java.nio.file.Files.readAllBytes(file)))throw new AssertionError("ordinary config load rewrites file");
            save.invoke(null);
            var after=new java.util.Properties();try(var in=java.nio.file.Files.newInputStream(file)){after.load(in);}
            for(String key:java.util.List.of("scanRadius","actionDelayTicks","farmingCrop","farmingAutoSetHome","hotkey.mining.modifiers","hotkey.farming.modifiers","hotkey.home.modifiers","hotkey.sethome.modifiers","hotkey.barn.modifiers","targets","miningSelectionTargetCount","pestNotificationThreshold","farmingMuteSoundEnabled","miningMuteSoundEnabled","farmingStallDetectionEnabled","farmingStallSeconds"))
                if(!prefs.getProperty(key).equals(after.getProperty(key)))throw new AssertionError("preference lost on reload: "+key);
            if(selected().size()!=10)throw new AssertionError("ten saved targets truncated on reload");
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
