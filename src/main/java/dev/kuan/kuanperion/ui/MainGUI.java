// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import dev.kuan.kuanperion.notification.NotificationManager;
import dev.kuan.kuanperion.renderer.UiRenderQueue;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.renderer.NVGScreen;
import dev.kuan.kuanperion.renderer.NanoVGManager;
import dev.kuan.kuanperion.ui.MainGUIActions;
import dev.kuan.kuanperion.ui.MainGUIChromeRenderer;
import dev.kuan.kuanperion.ui.MainGUIContentRenderer;
import dev.kuan.kuanperion.ui.MainGUIContentViewState;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.MainGUIFlatPanelRenderer;
import dev.kuan.kuanperion.ui.MainGUIInlineTextEditor;
import dev.kuan.kuanperion.ui.MainGUIKeyboardController;
import dev.kuan.kuanperion.ui.MainGUILayout;
import dev.kuan.kuanperion.ui.MainGUIModuleDetailRenderer;
import dev.kuan.kuanperion.ui.MainGUIModuleOverviewRenderer;
import dev.kuan.kuanperion.ui.MainGUIModuleSection;
import dev.kuan.kuanperion.ui.MainGUIMouseController;
import dev.kuan.kuanperion.ui.MainGUIOverlayController;
import dev.kuan.kuanperion.ui.MainGUIPetTrackerPanel;
import dev.kuan.kuanperion.ui.MainGUIProfilesPanel;
import dev.kuan.kuanperion.ui.MainGUIRegistry;
import dev.kuan.kuanperion.ui.MainGUIRenderPrimitives;
import dev.kuan.kuanperion.ui.MainGUISearchPanel;
import dev.kuan.kuanperion.ui.MainGUISettingInteractionController;
import dev.kuan.kuanperion.ui.MainGUISettingRowRenderer;
import dev.kuan.kuanperion.ui.MainGUITextLayout;
import dev.kuan.kuanperion.ui.MainGUITransientRenderState;
import dev.kuan.kuanperion.ui.MainGUITransitionRenderer;
import dev.kuan.kuanperion.ui.settings.ActionSetting;
import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.KeybindSetting;
import dev.kuan.kuanperion.ui.settings.ListSetting;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.PositionSetting;
import dev.kuan.kuanperion.ui.settings.RangeSliderSetting;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.settings.SliderSetting;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import dev.kuan.kuanperion.ui.theme.Theme;
import dev.kuan.kuanperion.util.UiLang;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class MainGUI
extends NVGScreen {
    static final float SIDEBAR_W = 60.0f;
    static final float SIDEBAR_EXPANDED = 194.0f;
    static final float TOP_BAR_H = 53.0f;
    static final float FILTER_BAR_H = 36.0f;
    private static final String[] TAB_NAMES = new String[]{"Modules", "Colors", "Config", "Keybinds", "Settings"};
    private static final String[] COLOR_FILTERS = new String[]{"All", "Menu Colors", "配色收藏"};
    private static final String[] CONFIG_FILTERS = new String[]{"All", "Config Profiles", "Theme Profiles"};
    private static final String[] SINGLE_FILTER = new String[]{"All"};
    static final float RADIUS = 10.0f;
    static final float ITEM_PAD = 20.0f;
    private static final float GROUP_GAP = 12.0f;
    static final float HEADER_H = 46.0f;
    static final float HEADER_TO_FIRST_SETTING_GAP = 5.0f;
    static final float PILL_H = 21.0f;
    static final float FLAT_LABEL_H = 32.0f;
    static final float DROPDOWN_FIELD_W = 130.0f;
    static final float DROPDOWN_FIELD_H = 32.0f;
    static final float DROPDOWN_ACTION_W = 32.0f;
    static final float DROPDOWN_ACTION_GAP = 6.0f;
    static final int CARD_COLS = 3;
    static final float CARD_HGAP = 10.0f;
    static final float CARD_VGAP = 10.0f;
    static final float CARD_H = 82.0f;
    static final float SECT_GAP = 22.0f;
    static final float SECT_H = 26.0f;
    static final float SECT_SEP = 10.0f;
    static final float MODULE_CAT_W = 160.0f;
    static final float MOD_HEADER_H = 74.0f;
    private static final float PET_FIELD_H = 42.0f;
    private static final float PET_ROW_GAP = 8.0f;
    private static final float PET_COL_GAP = 8.0f;
    private float px;
    private float py;
    private float pw;
    private float ph;
    float contX;
    float contY;
    float contW;
    float contH;
    private float pr = 1.0f;
    private float physW;
    private float physH;
    private float alpha = 0.0f;
    private float animScale = 0.96f;
    private float animOffsetY = 120.0f;
    public static float uiScale = 1.5f;
    public static float uiTextScale = 1.0f;
    private String hoverHelpCandidateKey;
    private String hoverHelpCandidateTitle;
    private String hoverHelpCandidateDescription;
    private String hoverHelpLastKey;
    private long hoverHelpStartedAt;
    private static final long HOVER_HELP_DELAY_MS = 260L;
    private float sidebarAnim = 0.0f;
    private float computedSidebarExpanded = 0.0f;
    private int activeMain = 0;
    private int activeSubtab = 0;
    private float animMainSel = 0.0f;
    private float animSubSel = 0.0f;
    boolean searchMode = false;
    String searchQuery = "";
    float searchScrollY = 0.0f;
    float searchTargetScrollY = 0.0f;
    float searchMaxScrollY = 0.0f;
    float searchBoxX;
    float searchBoxY;
    float searchBoxW;
    float searchBoxH;
    private int activeFilter = 1;
    private float filterBarAnimX = 0.0f;
    private float filterBarAnimW = 0.0f;
    private float filterBarTargetX = 0.0f;
    private float filterBarTargetW = 0.0f;
    private boolean filterBarInited = false;
    String profileNameInput = "";
    boolean profileNameFocus = false;
    int profileNameTarget = 0;
    String profileRenameInput = "";
    String profileRenameOriginal = "";
    boolean profileRenameFocus = false;
    int profileRenameTarget = 0;
    private float profileScrollY = 0.0f;
    private float profileTargetScrollY = 0.0f;
    private float profileMaxScrollY = 0.0f;
    private float loginButtonX;
    private float loginButtonY;
    private float loginButtonW;
    private float loginButtonH;
    private final List<ClickArea> clickAreas = new ArrayList<ClickArea>();
    private float btnSearchX;
    private float btnSearchY;
    private float btnSearchW;
    private float btnSearchH;
    float scrollY = 0.0f;
    float targetScrollY = 0.0f;
    float maxScrollY = 0.0f;
    private float modulesOverviewScrollY = 0.0f;
    private float modulesOverviewTargetScrollY = 0.0f;
    private float moduleDetailScrollY = 0.0f;
    private float moduleDetailTargetScrollY = 0.0f;
    private long lastFrameTimeNanos = System.nanoTime();
    private int sbDragging = 0;
    private float sbDragThumbOffset = 0.0f;
    float sbMainTrackT;
    float sbMainTrackH;
    float sbMainThumbH;
    float sbSrchTrackT;
    float sbSrchTrackH;
    float sbSrchThumbH;
    private float sbProfTrackT;
    private float sbProfTrackH;
    private float sbProfThumbH;
    private float sbSubTrackT;
    private float sbSubTrackH;
    private float sbSubThumbH;
    private float subtabScrollY = 0.0f;
    private float subtabTargetScrollY = 0.0f;
    private float subtabMaxScrollY = 0.0f;
    private SliderSetting dragSetting;
    private RangeSliderSetting dragRangeSetting;
    private float dragTrackX;
    private float dragTrackW;
    private float dragMin;
    private float dragMax;
    private int dragRangeHandle = 0;
    SliderSetting activeSliderField;
    TextSetting activeText;
    ListSetting activeList;
    int activeListIndex = -1;
    PositionSetting activePosField;
    KeybindSetting activeKeybindCapture;
    int activePosIdx;
    StringBuilder textBuf = new StringBuilder();
    int textCursor = 0;
    int textSelectionAnchor = -1;
    boolean textSelectionDragging = false;
    boolean textSelectionPendingDrag = false;
    int textSelectionPressCursor = -1;
    float textSelectionPressX = 0.0f;
    float textSelectionPressY = 0.0f;
    static final float TEXT_SINGLE_FIELD_H = 20.0f;
    static final float TEXT_MULTI_LINE_STEP = 14.0f;
    static final float TEXT_MULTI_PAD_Y = 6.0f;
    static final float TEXT_INLINE_FIELD_H = 32.0f;
    static final float TEXT_INLINE_FIELD_GAP = 12.0f;
    static final float LIST_ITEM_H = 32.0f;
    static final float LIST_ITEM_GAP = 6.0f;
    static final float LIST_ACTION_W = 24.0f;
    static final float SLIDER_FIELD_TRACK_GAP = 14.0f;
    boolean activeEditorBoundsSet = false;
    float activeEditorX;
    float activeEditorY;
    float activeEditorW;
    float activeEditorH;
    DropdownSetting openDd;
    private float ddX;
    private float ddY;
    private float ddW;
    private float ddButtonH;
    private float ddAnimAmt = 0.0f;
    static final float DD_ITEM_H = 30.0f;
    ColorSetting hoveredColor;
    ColorSetting activeColor;
    private float cpHue = 0.0f;
    private float cpSat = 1.0f;
    private float cpVal = 1.0f;
    private float cpAlpha = 1.0f;
    private int cpDrag = 0;
    static final int CP_SV = 1;
    static final int CP_HUE = 2;
    static final int CP_ALPHA = 3;
    boolean cpHexFocus = false;
    private final StringBuilder cpHexBuf = new StringBuilder();
    private float cpPX;
    private float cpPY;
    private float cpPW;
    private float cpPH;
    private float cpSvX;
    private float cpSvY;
    private float cpSvW;
    private float cpSvH;
    private float cpHBarY;
    private float cpABarY;
    private float cpBarH;
    private float cpHexX;
    private float cpHexY;
    private float cpHexW;
    private float cpHexH;
    private final IdentityHashMap<ModulesTab.SubTab, Float> cardHoverAnim = new IdentityHashMap();
    private ModulesTab.SubTab activeSubTab = null;
    private int activeCategoryIdx = 0;
    private final IdentityHashMap<Object, Float> catHoverAnim = new IdentityHashMap();
    private final IdentityHashMap<Object, Float> subTabBarAnim = new IdentityHashMap();
    boolean suppressNestedContentScissor = false;
    private boolean panelDragging = false;
    private float dragPanelOffX;
    private float dragPanelOffY;
    private final Set<SettingGroup> forcedOverride = new HashSet<SettingGroup>();
    private final MainGUIContext context = new MainGUIContext();
    private final MainGUILayout layoutHelper = new MainGUILayout();
    private final MainGUIActions actions = new MainGUIActions(this, this.context);
    private final MainGUITextLayout textLayout = new MainGUITextLayout();
    private final MainGUIRenderPrimitives renderPrimitives = new MainGUIRenderPrimitives(this, this.textLayout);
    private final MainGUIChromeRenderer chromeRenderer = new MainGUIChromeRenderer(this);
    private final MainGUIContentRenderer contentRenderer = new MainGUIContentRenderer(this, this.chromeRenderer);
    private final MainGUIModuleOverviewRenderer moduleOverviewRenderer = new MainGUIModuleOverviewRenderer(this);
    private final MainGUIFlatPanelRenderer flatPanelRenderer = new MainGUIFlatPanelRenderer(this);
    private final MainGUIModuleDetailRenderer moduleDetailRenderer = new MainGUIModuleDetailRenderer(this);
    private final MainGUISettingRowRenderer settingRowRenderer = new MainGUISettingRowRenderer(this);
    private final MainGUIOverlayController overlayController = new MainGUIOverlayController(this);
    private final MainGUISettingInteractionController settingInteractionController = new MainGUISettingInteractionController(this);
    private final MainGUITransitionRenderer transitionRenderer = new MainGUITransitionRenderer(this);
    private final MainGUIPetTrackerPanel petTrackerPanel = new MainGUIPetTrackerPanel(this);
    private final MainGUIMouseController mouseController = new MainGUIMouseController(this);
    private final MainGUIKeyboardController keyboardController = new MainGUIKeyboardController(this);
    private final MainGUISearchPanel searchPanel = new MainGUISearchPanel(this);
    private final MainGUIProfilesPanel profilesPanel = new MainGUIProfilesPanel(this);
    private final MainGUIInlineTextEditor inlineTextEditor = new MainGUIInlineTextEditor(this);
    private final LaunchTarget launchTarget;
    private boolean launchTargetApplied;
    private boolean frameStatePrepared;
    private boolean renderedQueuedFrame;
    private float frameMouseX;
    private float frameMouseY;
    private float frameDeltaTime;
    static final float SB_H_PAD = 12.0f;
    static final float SB_PILL = 36.0f;
    static final float SB_LOGO_H = 56.0f;
    static final float SB_SEP_GAP = 8.0f;
    static final float SB_ROW_PAD = 4.0f;
    static final float SB_BOT_PAD = 8.0f;

    void offerHoverHelp(String key, String title, String description, float x, float y, float w, float h, float mx, float my) {
        // Sidebar owns its original icon/text hover; cards own exactly one outline.
        if(key!=null && (key.startsWith("sidebar:") || key.startsWith("module:")))return;
        // No tooltip overlays anywhere. Other controls retain their subtle outline feedback.
        if (mx >= x && mx <= x+w && my >= y && my <= y+h && NanoVGManager.isInitialized()) {
            NanoVGManager.getRenderer().rectOutlineSolid(x,y,w,h,6,.9f,Theme.withAlpha(Theme.ACCENT_PRIMARY,.42f));
        }
    }

    private void renderHoverHelp(NVGRenderer nvg, float mx, float my) { /* Deliberately disabled globally. */ }

    public MainGUI() {
        this((LaunchTarget)null);
    }

    public MainGUI(LaunchTarget launchTarget) {
        super("Kuanperion");
        this.launchTarget = launchTarget;
    }

    @Override
    protected void initNVG() {
        Minecraft client = Minecraft.getInstance();
        MainGUIRegistry.refresh();
        try {
            RenderTarget rt = client.gameRenderer.mainRenderTarget();
            this.pr = (float)rt.width / (float)this.width;
        }
        catch (Exception ignored) {
            this.pr = 1.0f;
        }
        this.applyFrameLayout(true, 0.0f, 0.0f);
        this.activeSliderField = null;
        this.activeText = null;
        this.activeList = null;
        this.activeListIndex = -1;
        this.openDd = null;
        this.dragSetting = null;
        this.lastFrameTimeNanos = System.nanoTime();
        this.refreshContext();
        this.applyLaunchTargetIfNeeded();
    }

    private List<ModulesTab.SubTab> subtabs() {
        return switch (this.activeMain) {
            case 0 -> MainGUIRegistry.MODULE_SUBTABS;
            case 1 -> MainGUIRegistry.COLORS_SUBTABS;
            case 2 -> MainGUIRegistry.PROFILES_SUBTABS;
            case 3 -> MainGUIRegistry.KEYBINDS_SUBTABS;
            case 4 -> MainGUIRegistry.SETTINGS_SUBTABS;
            case 5 -> MainGUIRegistry.ABOUT_SUBTABS;
            default -> MainGUIRegistry.PROFILES_SUBTABS;
        };
    }

    private List<SettingGroup> groups() {
        if (this.activeMain == 2) {
            return List.of();
        }
        List<ModulesTab.SubTab> subs = this.subtabs();
        if (subs.isEmpty()) {
            return List.of();
        }
        if (this.activeSubtab >= subs.size()) {
            this.activeSubtab = 0;
        }
        return subs.get(this.activeSubtab).groups();
    }

    static boolean hasTopFilterBar(int mainTab) {
        return false;
    }

    private boolean hasTopFilterBar() {
        return MainGUI.hasTopFilterBar(this.activeMain);
    }

    float contentScrollTop() {
        return this.layoutHelper.contentScrollTop(this.contY, this.hasTopFilterBar(), 53.0f, 36.0f) + this.settingsNavigationHeight();
    }

    float contentScrollHeight() {
        return this.contH - (this.contentScrollTop() - this.contY);
    }

    float settingsNavigationHeight() { return this.activeMain >= 1 && this.activeMain <= 4 ? 37 : 0; }

    void selectSettingsPage(int page) { this.actions.switchMainTab(page); }

    void selectSidebarEntry(int index) {
        var entry = MainGUISidebar.entries().get(index);
        if (entry.mainTab() == 0) {
            // Switching groups shares the module renderer, but must clear stale detail/search/editors.
            this.commitText();
            this.commitColor();
            this.closeOpenDropdown();
            this.actions.clearSearch();
            this.activeMain = 0;
            this.activeFilter = entry.moduleFilter();
            this.activeSubtab = 0;
            this.activeSubTab = null;
            this.activeCategoryIdx = 0;
            this.setActiveScroll(0, 0);
            this.setModulesOverviewScroll(0, 0);
            this.setModuleDetailScroll(0, 0);
            this.refreshContext();
        } else {
            if (this.activeMain < 1 || this.activeMain > 4) this.actions.switchMainTab(4);
        }
    }

    private void resetActiveContentScroll() {
        this.actions.resetActiveContentScroll();
    }

    private void syncActiveBodyScrollState() {
        this.actions.syncActiveBodyScrollState();
    }

    private void enterModuleDetail(ModulesTab.SubTab subTab) {
        this.actions.enterModuleDetail(subTab);
    }

    private void exitModuleDetail() {
        this.actions.exitModuleDetail();
    }

    boolean pushContentLocalScissor(NVGRenderer nvg, float x, float y, float w, float h) {
        if (this.suppressNestedContentScissor) {
            return false;
        }
        nvg.pushScissor(x, y, w, h);
        return true;
    }

    void popContentLocalScissor(NVGRenderer nvg, boolean clipped) {
        if (clipped) {
            nvg.popScissor();
        }
    }

    void addClickArea(float x, float y, float w, float h, Runnable action) {
        this.clickAreas.add(new ClickArea(x, y, w, h, () -> {
            try { action.run(); }
            catch (IllegalArgumentException | IllegalStateException error) {
                NotificationManager.error("操作未完成", "請確認輸入格式與本機檔案權限");
            }
        }));
    }

    void openModuleDetailFromCard(ModulesTab.SubTab subTab) {
        this.enterModuleDetail(subTab);
    }

    void openSearchResult(int mainTab, ModulesTab.SubTab subTab, SettingGroup group, Setting setting) {
        this.actions.openSearchResult(mainTab, subTab, group, setting);
    }

    void ensureSearchGroupVisible(SettingGroup group) {
        if (group != null && !group.isAlwaysOn() && !this.showChildren(group)) {
            this.forcedOverride.add(group);
        }
    }

    private List<ModulesTab.SubTab> flatSubtabsForFilter() {
        return this.subtabs();
    }

    boolean showConfigProfiles() { return true; }
    boolean showThemeProfiles() { return true; }

    boolean isProfileNameFocused(boolean isConfig) {
        return this.profileNameFocus && this.profileNameTarget == (isConfig ? 0 : 1);
    }

    boolean isProfileRenameFocused(boolean isConfig, String name) {
        return this.profileRenameFocus && this.profileRenameTarget == (isConfig ? 0 : 1) && this.profileRenameOriginal.equals(name);
    }

    void beginProfileRename(boolean isConfig, String name) {
        this.commitText();
        this.commitColor();
        this.openDd = null;
        this.profileNameFocus = false;
        this.profileRenameTarget = isConfig ? 0 : 1;
        this.profileRenameOriginal = name;
        this.profileRenameInput = name;
        this.profileRenameFocus = true;
    }

    void cancelProfileRename() {
        this.profileRenameFocus = false;
        this.profileRenameOriginal = "";
        this.profileRenameInput = "";
    }

    void commitProfileRename() {
        this.profilesPanel.commitRename();
    }

    private List<Section> sectionsForFilter() {
        List<Section> availableSections = this.availableModuleSections();
        if (availableSections.size() <= 1) {
            return availableSections;
        }
        int filterIndex = this.normalizedActiveFilterIndex();
        int sectionIndex = filterIndex - 1;
        if (sectionIndex < 0 || sectionIndex >= availableSections.size()) {
            return availableSections;
        }
        return List.of(availableSections.get(sectionIndex));
    }

    private List<Section> availableModuleSections() {
        ArrayList<Section> sections = new ArrayList<Section>(MainGUIRegistry.MODULE_SECTIONS.size());
        for (MainGUIRegistry.ModuleSection section : MainGUIRegistry.MODULE_SECTIONS) {
            sections.add(new Section(section.displayName(), new ArrayList<ModulesTab.SubTab>(section.subtabs())));
        }
        return sections;
    }

    private boolean showChildren(SettingGroup group) {
        if (group.isAlwaysOn()) {
            return true;
        }
        boolean forced = this.forcedOverride.contains(group);
        return group.isEnabled() != forced;
    }

    private boolean isPetTrackerGroup(SettingGroup group) {
        return group.getRawName().startsWith("Pet XP Tracker");
    }

    private int normalizedActiveSubtabIndex() {
        List<ModulesTab.SubTab> subs = this.subtabs();
        if (subs.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(this.activeSubtab, subs.size() - 1));
    }

    private int normalizedActiveCategoryIdx() {
        if (this.activeMain != 0 || this.activeSubTab == null) {
            return this.activeCategoryIdx;
        }
        int maxCatIdx = this.activeSubTab.groups().size();
        int normalized = Math.max(0, Math.min(this.activeCategoryIdx, maxCatIdx));
        if (maxCatIdx == 1 && normalized == 0) {
            return 1;
        }
        return normalized;
    }

    MainGUIContentViewState snapshotContentState() {
        int filterIndex = this.normalizedActiveFilterIndex();
        return new MainGUIContentViewState(this.activeMain, this.normalizedActiveSubtabIndex(), filterIndex, this.activeSubTab, this.normalizedActiveCategoryIdx(), this.searchQuery);
    }

    void applyContentState(MainGUIContentViewState state) {
        this.activeMain = state.activeMain();
        this.activeSubtab = state.activeSubtab();
        this.activeFilter = state.activeFilter();
        this.activeFilter = this.normalizedActiveFilterIndex();
        this.activeSubTab = state.activeSubTab();
        this.activeCategoryIdx = state.activeCategoryIdx();
        this.searchQuery = state.searchQuery();
    }

    private Setting findSetting(SettingGroup group, String name) {
        for (Setting setting : group.getSettings()) {
            if (!setting.getRawName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    private float renderPetTrackerHeaderButtons(NVGRenderer nvg, SettingGroup group, float right, float btnY, float btnH, float mx, float my) {
        boolean hovered;
        float bx;
        float btnW;
        long trackerCount = this.currentPetTrackerGroupCount();
        boolean canAdd = true;
        boolean canRemove = trackerCount > 1L;
        float gap = 6.0f;
        if (canRemove) {
            btnW = 68.0f;
            bx = right - btnW;
            hovered = mx >= bx && mx <= bx + btnW && my >= btnY && my <= btnY + btnH;
            nvg.roundedRect(bx, btnY, btnW, btnH, 4.0f, hovered ? Theme.BG_HOVER : Theme.BG_FIELD);
            nvg.rectOutline(bx, btnY, btnW, btnH, 4.0f, 1.0f, Theme.BORDER_DEFAULT);
            nvg.textCentered("Inter-Regular", UiLang.localize("Remove"), bx, btnY, btnW, btnH, 11.0f, Theme.TEXT_PRIMARY);
            this.clickAreas.add(new ClickArea(bx, btnY, btnW, btnH, () -> {
                Setting remove = this.findSetting(group, "Remove Pet");
                if (remove instanceof ActionSetting) {
                    ActionSetting action = (ActionSetting)remove;
                    action.execute();
                }
            }));
            right = bx - gap;
        }
        if (canAdd) {
            btnW = 56.0f;
            bx = right - btnW;
            hovered = mx >= bx && mx <= bx + btnW && my >= btnY && my <= btnY + btnH;
            nvg.roundedRect(bx, btnY, btnW, btnH, 4.0f, hovered ? Theme.BG_HOVER : Theme.BG_FIELD);
            nvg.rectOutline(bx, btnY, btnW, btnH, 4.0f, 1.0f, Theme.BORDER_DEFAULT);
            nvg.textCentered("Inter-Regular", UiLang.localize("Add"), bx, btnY, btnW, btnH, 11.0f, Theme.TEXT_PRIMARY);
            this.clickAreas.add(new ClickArea(bx, btnY, btnW, btnH, () -> {
                Setting add = this.findSetting(group, "Add Pet");
                if (add instanceof ActionSetting) {
                    ActionSetting action = (ActionSetting)add;
                    action.execute();
                }
            }));
            right = bx - gap;
        }
        return right;
    }

    private long currentPetTrackerGroupCount() {
        if (this.activeSubTab != null) {
            return this.activeSubTab.groups().stream().filter(this::isPetTrackerGroup).count();
        }
        return this.groups().stream().filter(this::isPetTrackerGroup).count();
    }

    private boolean hasActiveInlineEditor() {
        return this.inlineTextEditor.hasActiveInlineEditor();
    }

    private boolean showSearchResults() {
        return this.searchQuery.length() >= 2;
    }

    private boolean isSearchEditorActive() {
        return this.inlineTextEditor.isSearchEditorActive();
    }

    private void syncSearchQueryFromEditor() {
        this.inlineTextEditor.syncSearchQueryFromEditor();
    }

    private void focusSearchField() {
        this.inlineTextEditor.focusSearchField();
    }

    private boolean isPointInsideSearchField(float mx, float my) {
        return this.inlineTextEditor.isPointInsideSearchField(mx, my);
    }

    private void updateSearchSelectionFromMouse(float mx) {
        this.inlineTextEditor.updateSearchSelectionFromMouse(mx);
    }

    private boolean hasTextSelection() {
        return this.inlineTextEditor.hasTextSelection();
    }

    private int textSelectionStart() {
        return this.inlineTextEditor.textSelectionStart();
    }

    private int textSelectionEnd() {
        return this.inlineTextEditor.textSelectionEnd();
    }

    private void clearTextSelection() {
        this.inlineTextEditor.clearTextSelection();
    }

    private void normalizeTextCursorState() {
        this.inlineTextEditor.normalizeTextCursorState();
    }

    private String safeSubstring(String value, int start, int end) {
        return this.inlineTextEditor.safeSubstring(value, start, end);
    }

    private void setTextCursor(int cursor, boolean extendSelection) {
        this.inlineTextEditor.setTextCursor(cursor, extendSelection);
    }

    void selectAllText() {
        this.inlineTextEditor.selectAllText();
    }

    private boolean deleteSelectedText() {
        return this.inlineTextEditor.deleteSelectedText();
    }

    private void insertTextAtCursor(String text) {
        this.inlineTextEditor.insertTextAtCursor(text);
    }

    private static boolean isWordChar(char c) {
        return MainGUIInlineTextEditor.isWordChar(c);
    }

    private static int moveCursorWord(String text, int cursor, int direction) {
        return MainGUIInlineTextEditor.moveCursorWord(text, cursor, direction);
    }

    private static int cursorFromInlineText(String text, float fontSize, float startX, float mx) {
        return MainGUIInlineTextEditor.cursorFromInlineText(text, fontSize, startX, mx);
    }

    private void updateInlineSelectionFromMouse(float mx) {
        this.inlineTextEditor.updateInlineSelectionFromMouse(mx);
    }

    private void beginInlineCaretPress(float mx, float my) {
        this.inlineTextEditor.beginInlineCaretPress(mx, my);
    }

    private boolean shouldBeginInlineSelectionDrag(float mx, float my) {
        return this.inlineTextEditor.shouldBeginInlineSelectionDrag(mx, my);
    }

    private void setActiveEditorBounds(float x, float y, float w, float h) {
        this.inlineTextEditor.setActiveEditorBounds(x, y, w, h);
    }

    private boolean isPointInsideActiveEditor(float mx, float my) {
        return this.inlineTextEditor.isPointInsideActiveEditor(mx, my);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mx, int my, float dt) {}

    @Override
    public void removed() { commitText(); commitColor(); Theme.saveTheme(); UiRenderQueue.clear(); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        this.prepareFrameState(mx, my, dt);
        UiRenderQueue.enqueue(this::renderQueuedFrame);
    }

    public void renderAfterGameRenderer(float dt) {
        if (this.renderedQueuedFrame) {
            this.renderedQueuedFrame = false;
            return;
        }
        if (!this.frameStatePrepared) {
            Minecraft client = Minecraft.getInstance();
            int mx = (int)client.mouseHandler.getScaledXPos(client.getWindow());
            int my = (int)client.mouseHandler.getScaledYPos(client.getWindow());
            this.prepareFrameState(mx, my, dt);
        }
        this.renderPreparedFrame(this.frameMouseX, this.frameMouseY, this.frameDeltaTime);
        this.frameStatePrepared = false;
    }

    private void renderQueuedFrame() {
        if (Minecraft.getInstance().gui.screen() != this) {
            this.frameStatePrepared = false;
            return;
        }
        if (!this.frameStatePrepared) {
            Minecraft client = Minecraft.getInstance();
            int mx = (int)client.mouseHandler.getScaledXPos(client.getWindow());
            int my = (int)client.mouseHandler.getScaledYPos(client.getWindow());
            this.prepareFrameState(mx, my, this.frameDeltaTime);
        }
        this.renderPreparedFrame(this.frameMouseX, this.frameMouseY, this.frameDeltaTime);
        this.frameStatePrepared = false;
        this.renderedQueuedFrame = true;
    }

    private void prepareFrameState(int mx, int my, float dt) {
        MainGUIRegistry.refresh();
        uiScale = UiScalePolicy.effective(this.width * this.pr, this.height * this.pr, Theme.UI_SCALE);
        uiTextScale = Theme.TEXT_SCALE;
        float prevPhysW = this.physW;
        float prevPhysH = this.physH;
        try {
            RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            this.pr = (float)rt.width / (float)this.width;
        }
        catch (Exception ignored) {
            this.pr = 1.0f;
        }
        this.syncFrameLayoutForWindowSize(prevPhysW, prevPhysH);
        float pmx = (float)mx * this.pr / uiScale;
        float pmy = (float)my * this.pr / uiScale;
        long nowNanos = System.nanoTime();
        long dtNanos = Math.max(0L, Math.min(100000000L, nowNanos - this.lastFrameTimeNanos));
        this.lastFrameTimeNanos = nowNanos;
        float as = Theme.animationFactor();
        float animT = Math.min(1.0f, as * 2.5f);
        this.alpha += (1.0f - this.alpha) * as;
        this.animScale += (1.0f - this.animScale) * (as * 1.1f);
        this.animOffsetY += (0.0f - this.animOffsetY) * (as * 1.1f);
        this.animMainSel += ((float)this.activeMain - this.animMainSel) * animT;
        this.animSubSel += ((float)this.activeSubtab - this.animSubSel) * animT;
        float expandedW = this.computedSidebarExpanded > 0.0f ? this.computedSidebarExpanded : 194.0f;
        float sbCurW = 60.0f + this.sidebarAnim * (expandedW - 60.0f);
        boolean sbHov = pmx >= this.px && pmx < this.px + sbCurW && pmy >= this.py && pmy <= this.py + this.ph;
        this.sidebarAnim += ((sbHov ? 1.0f : 0.0f) - this.sidebarAnim) * Math.min(1.0f, as * 0.5f);
        float scrollSpeed = MainGUI.scrollAnimStep(dtNanos);
        this.scrollY += (this.targetScrollY - this.scrollY) * scrollSpeed;
        this.scrollY = Math.max(0.0f, Math.min(this.maxScrollY, this.scrollY));
        this.searchScrollY += (this.searchTargetScrollY - this.searchScrollY) * scrollSpeed;
        this.searchScrollY = Math.max(0.0f, Math.min(this.searchMaxScrollY, this.searchScrollY));
        this.profileScrollY += (this.profileTargetScrollY - this.profileScrollY) * scrollSpeed;
        this.profileScrollY = Math.max(0.0f, Math.min(this.profileMaxScrollY, this.profileScrollY));
        this.subtabScrollY += (this.subtabTargetScrollY - this.subtabScrollY) * scrollSpeed;
        this.subtabScrollY = Math.max(0.0f, Math.min(this.subtabMaxScrollY, this.subtabScrollY));
        this.syncActiveBodyScrollState();
        if (this.openDd != null) {
            this.ddAnimAmt = Math.min(1.0f, this.ddAnimAmt + animT);
        }
        this.activeEditorBoundsSet = false;
        this.frameMouseX = pmx;
        this.frameMouseY = pmy;
        this.frameDeltaTime = dt;
        this.frameStatePrepared = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderPreparedFrame(float pmx, float pmy, float dt) {
        if (!NanoVGManager.isInitialized()) NanoVGManager.init();
        NanoVGManager.beginFrame(this.width, this.height);
        NVGRenderer nvg = NanoVGManager.getRenderer();
        try {
            nvg.save();
            nvg.scale(uiScale / this.pr, uiScale / this.pr);
            nvg.setTextScale(uiTextScale);
            float canvasW = this.physW / uiScale;
            float canvasH = this.physH / uiScale;
            nvg.rect(0.0f, 0.0f, canvasW, canvasH, Theme.withAlpha(-16777216, (int)(this.alpha * 170.0f)));
            float scx = canvasW / 2.0f;
            float scy = canvasH / 2.0f;
            nvg.save();
            nvg.translate(scx, scy);
            nvg.scale(this.animScale, this.animScale);
            nvg.translate(-scx, -scy);
            nvg.translate(0.0f, this.animOffsetY);
            nvg.globalAlpha(this.alpha);
            nvg.shadow(this.px, this.py, this.pw, this.ph, 10.0f, 26.0f, Theme.panelShadow());
            nvg.roundedRect(this.px, this.py, this.pw, this.ph, 10.0f, Theme.PANEL_BG);
            if (Theme.GRADIENT_ENABLED) nvg.cornerWash(this.px, this.py, this.pw, this.ph, 10.0f, Theme.GRADIENT_BLUE, Theme.GRADIENT_PINK, Theme.GRADIENT_COVERAGE, Theme.GRADIENT_STRENGTH);
            nvg.rectOutline(this.px, this.py, this.pw, this.ph, 10.0f, 1.0f, Theme.BORDER_DEFAULT);
            nvg.restore();
            nvg.save();
            nvg.translate(0.0f, this.animOffsetY);
            nvg.globalAlpha(this.alpha);
            this.renderPanel(nvg, pmx, pmy);
            nvg.restore();
            nvg.restore();
            this.renderOptionalFeatureHud(nvg, dt);
        }
        finally {
            NanoVGManager.endFrame();
        }
    }

    private void renderPanel(NVGRenderer nvg, float mx, float my) {
        this.refreshContext();
        this.clickAreas.clear();
        this.hoveredColor = null;
        this.hoverHelpCandidateKey = null;
        this.hoverHelpCandidateTitle = null;
        this.hoverHelpCandidateDescription = null;
        this.transitionRenderer.syncContentTransition();
        this.transitionRenderer.renderContentWithTransition(nvg, mx, my);
        this.renderSidebar(nvg, mx, my);
        this.renderHoverHelp(nvg, mx, my);
    }

    private void syncContentTransition() {
        this.transitionRenderer.syncContentTransition();
    }

    private void renderContentWithTransition(NVGRenderer nvg, float mx, float my) {
        this.transitionRenderer.renderContentWithTransition(nvg, mx, my);
    }

    private void renderSidebar(NVGRenderer nvg, float mx, float my) {
        this.chromeRenderer.renderSidebar(nvg, mx, my);
    }

    private void renderContentTopBar(NVGRenderer nvg, float mx, float my) {
        this.chromeRenderer.renderContentTopBar(nvg, mx, my);
    }

    private void renderFilterBar(NVGRenderer nvg, float mx, float my) {
        this.chromeRenderer.renderFilterBar(nvg, mx, my);
    }

    private void renderContent(NVGRenderer nvg, float mx, float my) {
        this.contentRenderer.renderContent(nvg, mx, my);
    }

    private void renderModuleDetailView(NVGRenderer nvg, float mx, float my) {
        this.contentRenderer.renderModuleDetailView(nvg, mx, my);
    }

    private void renderModuleDetailBody(NVGRenderer nvg, float mx, float my) {
        this.contentRenderer.renderModuleDetailBody(nvg, mx, my);
    }

    private void renderModuleGrid(NVGRenderer nvg, float mx, float my, float scrollTop, float scrollH) {
        this.moduleOverviewRenderer.render(nvg, mx, my, scrollTop, scrollH);
    }

    private void renderFlatContent(NVGRenderer nvg, float mx, float my) {
        this.flatPanelRenderer.render(nvg, mx, my);
    }

    private void renderFlatGroupLabel(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my) {
        float lineRight;
        int nameCol = group.isEnabled() && !group.isAlwaysOn() ? Theme.GROUP_ACTIVE : Theme.TEXT_MUTED;
        nvg.text("Inter-Bold", group.getName(), x, y + 11.0f, 10.0f, nameCol);
        float textW = nvg.textWidth("Inter-Bold", group.getName(), 10.0f);
        float f = lineRight = group.isAlwaysOn() ? x + w : x + w - 50.0f;
        if (this.isPetTrackerGroup(group)) {
            float buttonsLeft = this.renderPetTrackerHeaderButtons(nvg, group, x + w - 12.0f, y + 5.0f, 22.0f, mx, my);
            lineRight = Math.min(lineRight, buttonsLeft - 8.0f);
        }
        nvg.rect(x + textW + 8.0f, y + 16.0f, lineRight - (x + textW + 8.0f), 1.0f, Theme.SEPARATOR);
        if (!group.isAlwaysOn()) {
            this.renderPill(nvg, x + w - 38.0f, y + 5.5f, group.isEnabled(), group);
        }
    }

    void renderSectionHeader(NVGRenderer nvg, String name, float x, float y, float w) {
        this.renderPrimitives.renderSectionHeader(nvg, name, x, y, w);
    }

    private static String moduleCardName(ModulesTab.SubTab sub) {
        return UiLang.localize(sub.name());
    }

    private static boolean isConfigurationEditable(ModulesTab.SubTab sub) {
        // Kuanperion separates editable preferences from the runtime Start/Stop switch.
        return sub != null;
    }

    private void renderModuleCard(NVGRenderer nvg, ModulesTab.SubTab sub, float x, float y, float w, float hov, float mx, float my) {
        this.renderPrimitives.renderModuleCard(nvg, sub, x, y, w, hov, mx, my);
    }

    private void renderModuleTopBar(NVGRenderer nvg, float mx, float my) {
        this.moduleDetailRenderer.renderTopBar(nvg, mx, my);
    }

    private void renderModuleCategoryPanel(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        this.moduleDetailRenderer.renderCategoryPanel(nvg, mx, my, panelTop, panelH);
    }

    private void renderModuleSettingsPanel(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        this.moduleDetailRenderer.renderSettingsPanel(nvg, mx, my, panelTop, panelH);
    }

    private float subTabBarAnimValue(Object key, boolean active) {
        float target = active ? 1.0f : 0.0f;
        float current = this.subTabBarAnim.getOrDefault(key, Float.valueOf(target)).floatValue();
        float animationFactor = Theme.animationFactor();
        current += (target - current) * Math.min(0.05f, animationFactor * 3.0f);
        this.subTabBarAnim.put(key, Float.valueOf(current));
        return current;
    }

    private void renderGroupHeader(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my, boolean interactive) {
        nvg.roundedRect(x, y, w, 46.0f, 6.0f, Theme.BG_SECONDARY);
        float lerp = this.subTabBarAnimValue(group, group.isEnabled() && !group.isAlwaysOn() || group.isAlwaysOn() && interactive);
        nvg.rectOutline(x, y, w, 46.0f, 6.0f, 0.8f, Theme.blend(Theme.SEPARATOR, Theme.withAlpha(Theme.ACCENT_PRIMARY, 0.22f), lerp));
        int titleColor = interactive ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_DIM, 210);
        int descColor = interactive ? Theme.TEXT_SECONDARY : Theme.withAlpha(Theme.TEXT_DIM, 170);
        nvg.text("Inter-Regular", UiLang.localize(group.getName()), x + 12.0f, y + UiLayoutMetrics.TITLE_TOP, 13.5f, titleColor);
        nvg.text("Inter-Regular", UiLang.localize(group.getDescription()), x + 12.0f, y + UiLayoutMetrics.descriptionTop(13.5f), UiLayoutMetrics.DESCRIPTION_FONT, descColor);
        this.offerHoverHelp("group:" + group.getRawName(), group.getName(), group.getDescription(), x, y, w, 46.0f, mx, my);
        if (this.isPetTrackerGroup(group)) {
            this.renderPetTrackerHeaderButtons(nvg, group, x + w - 12.0f, y + 12.0f, 22.0f, mx, my);
        }
        if (!group.isAlwaysOn() && !group.isEnabled() && this.forcedOverride.contains(group)) {
            nvg.text("Inter-Regular", "\u25bc", x + w - 68.0f, y + 17.5f, 9.0f, Theme.withAlpha(Theme.TEXT_SECONDARY, 150));
        }
        if (!group.isAlwaysOn()) {
            float pillX = x + w - 52.0f;
            float pillY = y + 12.5f;
            this.renderPill(nvg, pillX, pillY, group.isEnabled(), group);
            if (interactive) {
                this.clickAreas.add(new ClickArea(pillX - 6.0f, pillY - 6.0f, 48.0f, 33.0f, group::toggle));
            }
        }
        if (!interactive) {
            nvg.roundedRect(x, y, w, 46.0f, 6.0f, Theme.withAlpha(Theme.PANEL_BG, 0.34f));
        }
    }

    void renderSettingRow(NVGRenderer nvg, Setting setting, float x, float y, float w, float h, float mx, float my) {
        this.settingRowRenderer.render(nvg, setting, x, y, w, h, mx, my);
    }

    private float petTrackerGroupHeight() {
        return 92.0f;
    }

    static float petFieldHeight() {
        return 42.0f;
    }

    static float petRowGap() {
        return 8.0f;
    }

    static float petColGap() {
        return 8.0f;
    }

    private void renderPetTrackerGroup(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my) {
        this.petTrackerPanel.renderGroup(nvg, group, x, y, w, mx, my);
    }

    private boolean handlePetTrackerGroupClick(SettingGroup group, float mx, float my, float x, float y, float w) {
        return this.petTrackerPanel.handleGroupClick(group, mx, my, x, y, w);
    }

    private void renderListActionButton(NVGRenderer nvg, float x, float y, float w, String label, boolean hovered, boolean enabled) {
        this.renderPrimitives.renderListActionButton(nvg, x, y, w, label, hovered, enabled);
    }

    private void renderPill(NVGRenderer nvg, float x, float y, boolean on, Object key) {
        this.renderPrimitives.renderPill(nvg, x, y, on, key);
    }

    private void renderDropdownOverlay(NVGRenderer nvg, float mx, float my) {
        this.overlayController.renderDropdownOverlay(nvg, mx, my);
    }

    private void renderColorOverlay(NVGRenderer nvg) {
        this.overlayController.renderColorOverlay(nvg);
    }

    float settingH(Setting s, float rowW) {
        return this.textLayout.settingHeight(s, rowW);
    }

    private int wrappedSettingLabelLineCount(Setting setting, float rowW) {
        return this.textLayout.wrappedSettingLabelLineCount(setting, rowW);
    }

    private float settingLabelFontSize(Setting setting) {
        return this.textLayout.settingLabelFontSize(setting);
    }

    private float settingLabelLineStep(Setting setting) {
        return this.textLayout.settingLabelLineStep(setting);
    }

    private float settingLabelMaxWidth(Setting setting, float rowW) {
        return this.textLayout.settingLabelMaxWidth(setting, rowW);
    }

    private float dropdownActionStripWidth(DropdownSetting dropdown) {
        return this.textLayout.dropdownActionStripWidth(dropdown);
    }

    private float dropdownActionStartX(DropdownSetting dropdown, float fieldX) {
        return this.textLayout.dropdownActionStartX(dropdown, fieldX);
    }

    private int dropdownActionIndexAt(DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        return this.textLayout.dropdownActionIndexAt(dropdown, fieldX, fieldY, fieldH, mx, my);
    }

    private void renderDropdownActionButtons(NVGRenderer nvg, DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        this.renderPrimitives.renderDropdownActionButtons(nvg, dropdown, fieldX, fieldY, fieldH, mx, my);
    }

    private float textInlineFieldWidth(float innerWidth) {
        return this.textLayout.textInlineFieldWidth(innerWidth);
    }

    private float inlineTextScrollOffset(NVGRenderer nvg, String font, String text, int cursor, float fontSize, float visibleWidth) {
        return this.textLayout.inlineTextScrollOffset(nvg, font, text, cursor, fontSize, visibleWidth);
    }

    float inlineTextScrollOffsetApprox(String text, int cursor, float fontSize, float visibleWidth) {
        return this.textLayout.inlineTextScrollOffsetApprox(text, cursor, fontSize, visibleWidth);
    }

    private int estimateWrappedLineCount(String text, float maxWidth, float fontSize) {
        return this.textLayout.estimateWrappedLineCount(text, maxWidth, fontSize);
    }

    private List<String> wrapSettingLabel(NVGRenderer nvg, Setting setting, float rowW) {
        return this.textLayout.wrapSettingLabel(nvg, setting, rowW);
    }

    List<String> wrapTextForWidth(NVGRenderer nvg, String text, String fontName, float fontSize, float maxWidth) {
        return this.textLayout.wrapTextToWidth(nvg, text, fontName, fontSize, maxWidth);
    }

    private int fittingPrefixLength(NVGRenderer nvg, String text, String fontName, float fontSize, float maxWidth) {
        return this.textLayout.fittingPrefixLength(nvg, text, fontName, fontSize, maxWidth);
    }

    private static float clampScroll(float value, float maxScroll) {
        return Math.max(0.0f, Math.min(maxScroll, value));
    }

    private static float scrollAnimStep(long dtNanos) {
        float dtMs = Math.max(0.0f, (float)dtNanos / 1000000.0f);
        if (dtMs <= 0.0f) {
            return 0.0f;
        }
        float durationMs = Math.max(1.0f, Theme.ANIM_TIME_MS);
        return 1.0f - (float)Math.exp(-dtMs / (durationMs / 3.0f));
    }

    static float scrollbarThumbY(float trackT, float trackH, float thumbH, float scrollVal, float maxScroll) {
        if (maxScroll <= 0.0f || trackH <= thumbH) {
            return trackT;
        }
        float thumbY = trackT + scrollVal / maxScroll * (trackH - thumbH);
        return Math.max(trackT, Math.min(trackT + trackH - thumbH, thumbY));
    }

    private static boolean hitScrollbarTrack(float mx, float my, float trackT, float trackH, float barX) {
        return mx >= barX - 6.0f && mx <= barX + 10.0f && my >= trackT && my <= trackT + trackH;
    }

    private static boolean hitScrollbarThumb(float mx, float my, float trackT, float trackH, float thumbH, float scrollVal, float maxScroll, float barX) {
        if (!MainGUI.hitScrollbarTrack(mx, my, trackT, trackH, barX)) {
            return false;
        }
        float thumbY = MainGUI.scrollbarThumbY(trackT, trackH, thumbH, scrollVal, maxScroll);
        return my >= thumbY && my <= thumbY + thumbH;
    }

    private void setScrollbarScroll(int dragMode, float value, boolean immediate) {
        switch (dragMode) {
            case 1: {
                this.targetScrollY = MainGUI.clampScroll(value, this.maxScrollY);
                if (!immediate) break;
                this.scrollY = this.targetScrollY;
                break;
            }
            case 2: {
                this.searchTargetScrollY = MainGUI.clampScroll(value, this.searchMaxScrollY);
                if (!immediate) break;
                this.searchScrollY = this.searchTargetScrollY;
                break;
            }
            case 3: {
                this.profileTargetScrollY = MainGUI.clampScroll(value, this.profileMaxScrollY);
                if (!immediate) break;
                this.profileScrollY = this.profileTargetScrollY;
                break;
            }
            case 4: {
                this.subtabTargetScrollY = MainGUI.clampScroll(value, this.subtabMaxScrollY);
                if (!immediate) break;
                this.subtabScrollY = this.subtabTargetScrollY;
            }
        }
    }

    private void startScrollbarDrag(int dragMode, float mx, float my, float trackT, float trackH, float thumbH, float scrollVal, float maxScroll, float barX) {
        if (!MainGUI.hitScrollbarTrack(mx, my, trackT, trackH, barX) || maxScroll <= 0.0f) {
            return;
        }
        this.sbDragging = dragMode;
        float thumbY = MainGUI.scrollbarThumbY(trackT, trackH, thumbH, scrollVal, maxScroll);
        boolean thumbHit = MainGUI.hitScrollbarThumb(mx, my, trackT, trackH, thumbH, scrollVal, maxScroll, barX);
        this.sbDragThumbOffset = thumbHit ? my - thumbY : thumbH * 0.5f;
        this.updateScrollbarDrag(my, thumbHit);
    }

    private void updateScrollbarDrag(float my, boolean immediate) {
        float maxScroll;
        float thumbH;
        float trackH;
        float trackT;
        switch (this.sbDragging) {
            case 1: {
                trackT = this.sbMainTrackT;
                trackH = this.sbMainTrackH;
                thumbH = this.sbMainThumbH;
                maxScroll = this.maxScrollY;
                break;
            }
            case 2: {
                trackT = this.sbSrchTrackT;
                trackH = this.sbSrchTrackH;
                thumbH = this.sbSrchThumbH;
                maxScroll = this.searchMaxScrollY;
                break;
            }
            case 3: {
                trackT = this.sbProfTrackT;
                trackH = this.sbProfTrackH;
                thumbH = this.sbProfThumbH;
                maxScroll = this.profileMaxScrollY;
                break;
            }
            case 4: {
                trackT = this.sbSubTrackT;
                trackH = this.sbSubTrackH;
                thumbH = this.sbSubThumbH;
                maxScroll = this.subtabMaxScrollY;
                break;
            }
            default: {
                return;
            }
        }
        float range = trackH - thumbH;
        if (range <= 0.0f || maxScroll <= 0.0f) {
            return;
        }
        float thumbTop = Math.max(trackT, Math.min(trackT + range, my - this.sbDragThumbOffset));
        float scrollValue = (thumbTop - trackT) / range * maxScroll;
        this.setScrollbarScroll(this.sbDragging, scrollValue, immediate);
    }

    private float textSettingHeight(TextSetting setting) {
        return this.inlineTextEditor.textSettingHeight(setting);
    }

    private float textFieldHeight(TextSetting setting) {
        return this.inlineTextEditor.textFieldHeight(setting);
    }

    private static String[] splitLines(String text) {
        return MainGUIInlineTextEditor.splitLines(text);
    }

    private static int getLineStart(String text, int cursor) {
        return MainGUIInlineTextEditor.getLineStart(text, cursor);
    }

    private static int getLineEnd(String text, int cursor) {
        return MainGUIInlineTextEditor.getLineEnd(text, cursor);
    }

    private static int getCursorLine(String text, int cursor) {
        return MainGUIInlineTextEditor.getCursorLine(text, cursor);
    }

    private static int getCursorColumn(String text, int cursor) {
        return MainGUIInlineTextEditor.getCursorColumn(text, cursor);
    }

    private static int moveCursorVertical(String text, int cursor, int delta) {
        return MainGUIInlineTextEditor.moveCursorVertical(text, cursor, delta);
    }

    int getMultilineViewportStart(TextSetting setting, String text, int cursor) {
        return this.inlineTextEditor.getMultilineViewportStart(setting, text, cursor);
    }

    private String fmtSlider(SliderSetting ss) {
        float v = ss.getValue();
        String sf = ss.getSuffix() != null ? ss.getSuffix() : "";
        return ss.getDecimals() == 0 ? (int)v + sf : String.format("%." + ss.getDecimals() + "f", Float.valueOf(v)) + sf;
    }

    private String fmtRangeSlider(RangeSliderSetting rs) {
        String sf;
        String string = sf = rs.getSuffix() != null ? rs.getSuffix() : "";
        if (rs.getDecimals() == 0) {
            return Math.round(rs.getLowerValue()) + sf + " - " + Math.round(rs.getUpperValue()) + sf;
        }
        return String.format("%." + rs.getDecimals() + "f", Float.valueOf(rs.getLowerValue())) + sf + " - " + String.format("%." + rs.getDecimals() + "f", Float.valueOf(rs.getUpperValue())) + sf;
    }

    String fmtSliderInput(SliderSetting ss) {
        float v = ss.getValue();
        return ss.getDecimals() == 0 ? String.valueOf(Math.round(v)) : String.format(Locale.US, "%." + ss.getDecimals() + "f", Float.valueOf(v));
    }

    void argbToHsv(int argb) {
        this.overlayController.argbToHsv(argb);
    }

    private void updateColorFromHsv() {
        this.overlayController.updateColorFromHsv();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return this.mouseController.mouseClicked(click, doubled);
    }

    private void handleRightClick(float mx, float my) {
        if (mx < this.contX || mx > this.contX + this.contW || my < this.contY || my > this.contY + this.contH) {
            return;
        }
        if (this.activeMain == 1 || this.activeMain >= 3) {
            return;
        }
        float gx = this.contX + 20.0f;
        float gw = this.contW - 40.0f;
        float y2 = 0.0f;
        for (SettingGroup group : this.groups()) {
            float headerSY = this.contY - this.scrollY + (y2 += 12.0f);
            if (my >= headerSY && my <= headerSY + 46.0f && mx >= gx && mx <= gx + gw) {
                if (group.isAlwaysOn()) {
                    return;
                }
                if (this.forcedOverride.contains(group)) {
                    this.forcedOverride.remove(group);
                } else {
                    this.forcedOverride.add(group);
                }
                return;
            }
            y2 += 46.0f;
            if (!this.showChildren(group) || !group.hasSettings()) continue;
            y2 += 5.0f;
            if (this.isPetTrackerGroup(group)) {
                y2 += this.petTrackerGroupHeight();
            } else {
                for (Setting s : group.getSettings()) {
                    if (!s.isVisible()) continue;
                    y2 += this.settingH(s, gw);
                }
            }
            y2 += 4.0f;
        }
    }

    private boolean handleColorPickerClick(float mx, float my) {
        return this.overlayController.handleColorPickerClick(mx, my);
    }

    private void handleColorPickerDrag(float mx, float my) {
        this.overlayController.handleColorPickerDrag(mx, my);
    }

    private void handleDropdownClick(float mx, float my) {
        this.overlayController.handleDropdownClick(mx, my);
    }

    private void handleContentClick(float mx, float my) {
        this.settingInteractionController.handleContentClick(mx, my);
    }

    private void handleFlatContentClick(float mx, float my) {
        this.settingInteractionController.handleFlatContentClick(mx, my);
    }

    private void handleModuleSettingsPanelClick(float mx, float my) {
        this.settingInteractionController.handleModuleSettingsPanelClick(mx, my);
    }

    private void focusListItem(ListSetting list, int index) {
        List<String> values = list.getValues();
        if (index < 0 || index >= values.size()) {
            return;
        }
        this.activeList = list;
        this.activeListIndex = index;
        this.activeText = null;
        this.textBuf = new StringBuilder(values.get(index));
        this.textCursor = this.textBuf.length();
        this.clearTextSelection();
    }

    private void addListItem(ListSetting list, int insertIndex) {
        List<String> values = list.getValues();
        int target = Math.max(0, Math.min(insertIndex, values.size()));
        values.add(target, "");
        list.setValues(values);
        this.focusListItem(list, target);
    }

    private void removeListItem(ListSetting list, int index) {
        List<String> values = list.getValues();
        if (index < 0 || index >= values.size()) {
            return;
        }
        values.remove(index);
        list.setValues(values);
        if (this.activeList == list) {
            this.activeList = null;
            this.activeListIndex = -1;
        }
    }

    private void focusSliderField(SliderSetting slider) {
        this.activeSliderField = slider;
        this.activeText = null;
        this.activeList = null;
        this.activeListIndex = -1;
        this.activePosField = null;
        this.textBuf = new StringBuilder(this.fmtSliderInput(slider));
        this.textCursor = this.textBuf.length();
        this.clearTextSelection();
    }

    void handleSettingClick(Setting setting, float mx, float my, float x, float y, float w, float h) {
        this.settingInteractionController.handleSettingClick(setting, mx, my, x, y, w, h);
    }

    private void updateSlider(float mx) {
        this.settingInteractionController.updateSlider(mx);
    }

    private void updateRangeSlider(float mx) {
        this.settingInteractionController.updateRangeSlider(mx);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dX, double dY) {
        return this.mouseController.mouseDragged(click, dX, dY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        return this.mouseController.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return this.mouseController.mouseScrolled(mx, my, sx, sy);
    }

    boolean handleSettingsShortcut(KeyEvent input) {
        if (isAnyKeybindCaptureActive() || hasActiveInlineEditor() || isSearchEditorActive()
                || profileNameFocus || profileRenameFocus || cpHexFocus) return false;
        if (!dev.kuan.kuanperion.KuanperionClient.matchesSettingsKey(input)) return false;
        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.keyboardController.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return this.keyboardController.charTyped(input);
    }

    void commitText() {
        this.inlineTextEditor.commitText();
    }

    void commitColor() {
        this.overlayController.commitColor();
    }

    private void renderSearchContent(NVGRenderer nvg, float mx, float my) {
        this.searchPanel.render(nvg, mx, my);
    }

    private void handleSearchClick(float mx, float my) {
        this.searchPanel.handleClick(mx, my);
    }

    void handleSearchClickEvent(float mx, float my) {
        this.handleSearchClick(mx, my);
    }

    private void renderProfileContent(NVGRenderer nvg, float mx, float my) {
        this.profilesPanel.render(nvg, mx, my);
    }

    void refreshContext() {
        this.activeFilter = this.normalizedActiveFilterIndex();
        this.context.layout.px = this.px;
        this.context.layout.py = this.py;
        this.context.layout.pw = this.pw;
        this.context.layout.ph = this.ph;
        this.context.layout.contX = this.contX;
        this.context.layout.contY = this.contY;
        this.context.layout.contW = this.contW;
        this.context.layout.contH = this.contH;
        this.context.layout.pr = this.pr;
        this.context.layout.physW = this.physW;
        this.context.layout.physH = this.physH;
        this.context.layout.btnSearchX = this.btnSearchX;
        this.context.layout.btnSearchY = this.btnSearchY;
        this.context.layout.btnSearchW = this.btnSearchW;
        this.context.layout.btnSearchH = this.btnSearchH;
        this.context.layout.searchBoxX = this.searchBoxX;
        this.context.layout.searchBoxY = this.searchBoxY;
        this.context.layout.searchBoxW = this.searchBoxW;
        this.context.layout.searchBoxH = this.searchBoxH;
        this.context.layout.activeEditorX = this.activeEditorX;
        this.context.layout.activeEditorY = this.activeEditorY;
        this.context.layout.activeEditorW = this.activeEditorW;
        this.context.layout.activeEditorH = this.activeEditorH;
        this.context.navigation.activeMain = this.activeMain;
        this.context.navigation.activeSubtab = this.activeSubtab;
        this.context.navigation.activeFilter = this.activeFilter;
        this.context.navigation.activeSubTab = this.activeSubTab;
        this.context.navigation.activeCategoryIdx = this.activeCategoryIdx;
        this.context.navigation.searchMode = this.searchMode;
        this.context.navigation.searchQuery = this.searchQuery;
        this.context.navigation.profileNameInput = this.profileNameInput;
        this.context.navigation.profileNameFocus = this.profileNameFocus;
        this.context.navigation.profileNameTarget = this.profileNameTarget;
        this.context.navigation.profileRenameInput = this.profileRenameInput;
        this.context.navigation.profileRenameOriginal = this.profileRenameOriginal;
        this.context.navigation.profileRenameFocus = this.profileRenameFocus;
        this.context.navigation.profileRenameTarget = this.profileRenameTarget;
        this.context.navigation.suppressNestedContentScissor = this.suppressNestedContentScissor;
        this.context.scroll.scrollY = this.scrollY;
        this.context.scroll.targetScrollY = this.targetScrollY;
        this.context.scroll.maxScrollY = this.maxScrollY;
        this.context.scroll.modulesOverviewScrollY = this.modulesOverviewScrollY;
        this.context.scroll.modulesOverviewTargetScrollY = this.modulesOverviewTargetScrollY;
        this.context.scroll.moduleDetailScrollY = this.moduleDetailScrollY;
        this.context.scroll.moduleDetailTargetScrollY = this.moduleDetailTargetScrollY;
        this.context.scroll.searchScrollY = this.searchScrollY;
        this.context.scroll.searchTargetScrollY = this.searchTargetScrollY;
        this.context.scroll.searchMaxScrollY = this.searchMaxScrollY;
        this.context.scroll.profileScrollY = this.profileScrollY;
        this.context.scroll.profileTargetScrollY = this.profileTargetScrollY;
        this.context.scroll.profileMaxScrollY = this.profileMaxScrollY;
        this.context.scroll.subtabScrollY = this.subtabScrollY;
        this.context.scroll.subtabTargetScrollY = this.subtabTargetScrollY;
        this.context.scroll.subtabMaxScrollY = this.subtabMaxScrollY;
        this.context.scroll.sbMainTrackT = this.sbMainTrackT;
        this.context.scroll.sbMainTrackH = this.sbMainTrackH;
        this.context.scroll.sbMainThumbH = this.sbMainThumbH;
        this.context.scroll.sbSrchTrackT = this.sbSrchTrackT;
        this.context.scroll.sbSrchTrackH = this.sbSrchTrackH;
        this.context.scroll.sbSrchThumbH = this.sbSrchThumbH;
        this.context.scroll.sbProfTrackT = this.sbProfTrackT;
        this.context.scroll.sbProfTrackH = this.sbProfTrackH;
        this.context.scroll.sbProfThumbH = this.sbProfThumbH;
        this.context.scroll.sbSubTrackT = this.sbSubTrackT;
        this.context.scroll.sbSubTrackH = this.sbSubTrackH;
        this.context.scroll.sbSubThumbH = this.sbSubThumbH;
        this.context.scroll.lastFrameTimeNanos = this.lastFrameTimeNanos;
        this.context.animation.alpha = this.alpha;
        this.context.animation.animScale = this.animScale;
        this.context.animation.animOffsetY = this.animOffsetY;
        this.context.animation.sidebarAnim = this.sidebarAnim;
        this.context.animation.computedSidebarExpanded = this.computedSidebarExpanded;
        this.context.animation.animMainSel = this.animMainSel;
        this.context.animation.animSubSel = this.animSubSel;
        this.context.animation.filterBarAnimX = this.filterBarAnimX;
        this.context.animation.filterBarAnimW = this.filterBarAnimW;
        this.context.animation.filterBarTargetX = this.filterBarTargetX;
        this.context.animation.filterBarTargetW = this.filterBarTargetW;
        this.context.animation.filterBarInited = this.filterBarInited;
        this.context.animation.ddAnimAmt = this.ddAnimAmt;
        this.context.editor.activeSliderField = this.activeSliderField;
        this.context.editor.activeText = this.activeText;
        this.context.editor.activeList = this.activeList;
        this.context.editor.activeListIndex = this.activeListIndex;
        this.context.editor.activePosField = this.activePosField;
        this.context.editor.activePosIdx = this.activePosIdx;
        this.context.editor.textBuffer = this.textBuf.toString();
        this.context.editor.textCursor = this.textCursor;
        this.context.editor.textSelectionAnchor = this.textSelectionAnchor;
        this.context.editor.textSelectionDragging = this.textSelectionDragging;
        this.context.editor.textSelectionPendingDrag = this.textSelectionPendingDrag;
        this.context.editor.textSelectionPressCursor = this.textSelectionPressCursor;
        this.context.editor.textSelectionPressX = this.textSelectionPressX;
        this.context.editor.textSelectionPressY = this.textSelectionPressY;
        this.context.editor.activeEditorBoundsSet = this.activeEditorBoundsSet;
        this.context.editor.cpHexFocus = this.cpHexFocus;
        this.context.editor.cpHexBuffer = this.cpHexBuf.toString();
        this.context.overlay.openDropdown = this.openDd;
        this.context.overlay.dropdownX = this.ddX;
        this.context.overlay.dropdownY = this.ddY;
        this.context.overlay.dropdownW = this.ddW;
        this.context.overlay.dropdownButtonH = this.ddButtonH;
        this.context.overlay.hoveredColor = this.hoveredColor;
        this.context.overlay.activeColor = this.activeColor;
        this.context.overlay.cpHue = this.cpHue;
        this.context.overlay.cpSat = this.cpSat;
        this.context.overlay.cpVal = this.cpVal;
        this.context.overlay.cpAlpha = this.cpAlpha;
        this.context.overlay.cpDrag = this.cpDrag;
        this.context.overlay.cpPX = this.cpPX;
        this.context.overlay.cpPY = this.cpPY;
        this.context.overlay.cpPW = this.cpPW;
        this.context.overlay.cpPH = this.cpPH;
        this.context.overlay.cpSvX = this.cpSvX;
        this.context.overlay.cpSvY = this.cpSvY;
        this.context.overlay.cpSvW = this.cpSvW;
        this.context.overlay.cpSvH = this.cpSvH;
        this.context.overlay.cpHBarY = this.cpHBarY;
        this.context.overlay.cpABarY = this.cpABarY;
        this.context.overlay.cpBarH = this.cpBarH;
        this.context.overlay.cpHexX = this.cpHexX;
        this.context.overlay.cpHexY = this.cpHexY;
        this.context.overlay.cpHexW = this.cpHexW;
        this.context.overlay.cpHexH = this.cpHexH;
        this.context.interaction.scrollbarDragging = this.sbDragging;
        this.context.interaction.scrollbarDragThumbOffset = this.sbDragThumbOffset;
        this.context.interaction.dragSetting = this.dragSetting;
        this.context.interaction.dragRangeSetting = this.dragRangeSetting;
        this.context.interaction.dragTrackX = this.dragTrackX;
        this.context.interaction.dragTrackW = this.dragTrackW;
        this.context.interaction.dragMin = this.dragMin;
        this.context.interaction.dragMax = this.dragMax;
        this.context.interaction.dragRangeHandle = this.dragRangeHandle;
        this.context.interaction.panelDragging = this.panelDragging;
        this.context.interaction.dragPanelOffX = this.dragPanelOffX;
        this.context.interaction.dragPanelOffY = this.dragPanelOffY;
        this.context.interaction.forcedOverride = this.forcedOverride;
    }

    MainGUIContext context() {
        return this.actions.context();
    }

    int getActiveMainTab() {
        return this.activeMain;
    }

    ModulesTab.SubTab getActiveModuleSubTab() {
        return this.activeSubTab;
    }

    float getActiveScrollY() {
        return this.scrollY;
    }

    float getActiveTargetScrollY() {
        return this.targetScrollY;
    }

    float getModulesOverviewScrollY() {
        return this.modulesOverviewScrollY;
    }

    float getModulesOverviewTargetScrollY() {
        return this.modulesOverviewTargetScrollY;
    }

    float getModuleDetailScrollY() {
        return this.moduleDetailScrollY;
    }

    float getModuleDetailTargetScrollY() {
        return this.moduleDetailTargetScrollY;
    }

    boolean isShowingSearchResults() {
        return this.showSearchResults();
    }

    void setActiveScroll(float scrollY, float targetScrollY) {
        this.scrollY = scrollY;
        this.targetScrollY = targetScrollY;
    }

    void setModulesOverviewScroll(float scrollY, float targetScrollY) {
        this.modulesOverviewScrollY = scrollY;
        this.modulesOverviewTargetScrollY = targetScrollY;
    }

    void setModuleDetailScroll(float scrollY, float targetScrollY) {
        this.moduleDetailScrollY = scrollY;
        this.moduleDetailTargetScrollY = targetScrollY;
    }

    void setActiveModuleSubTab(ModulesTab.SubTab subTab) {
        this.activeSubTab = subTab;
    }

    void setActiveCategoryIndex(int categoryIndex) {
        this.activeCategoryIdx = categoryIndex;
    }

    void setActiveMainTab(int mainTab) {
        this.activeMain = mainTab;
    }

    void setActiveSubtabIndex(int subtabIndex) {
        this.activeSubtab = subtabIndex;
    }

    void setProfileScroll(float scrollY, float targetScrollY) {
        this.profileScrollY = scrollY;
        this.profileTargetScrollY = targetScrollY;
    }

    void setSearchScroll(float scrollY, float targetScrollY) {
        this.searchScrollY = scrollY;
        this.searchTargetScrollY = targetScrollY;
    }

    void setSearchMode(boolean searchMode) {
        this.searchMode = searchMode;
    }

    void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    void setActiveFilterIndex(int filterIndex) {
        this.activeFilter = filterIndex;
    }

    void setFilterBarInitialized(boolean initialized) {
        this.filterBarInited = initialized;
    }

    void clearInlineTextSelection() {
        this.clearTextSelection();
    }

    void setComputedSidebarExpanded(float width) {
        this.computedSidebarExpanded = width;
    }

    String currentTabName() {
        if (this.activeMain == 0) return MainGUISidebar.entries().get(MainGUISidebar.selectedIndex(0, this.activeFilter)).label();
        return this.activeMain == 5 ? UiLang.localize("About") : "Setting";
    }

    String currentTabDescription() {
        if (this.activeMain == 0) return MainGUISidebar.entries().get(MainGUISidebar.selectedIndex(0, this.activeFilter)).description();
        if (this.activeMain == 5) return "版本與模組資訊";
        return UiLang.localize(TAB_NAMES[this.activeMain]);
    }

    String[] currentTabFilters() {
        return this.filtersForMainTab(this.activeMain);
    }

    private int normalizedActiveFilterIndex() {
        String[] filters = this.filtersForMainTab(this.activeMain);
        if (filters.length == 0) {
            return 0;
        }
        return Math.max(this.activeMain == 0 ? 1 : 0, Math.min(this.activeFilter, filters.length - 1));
    }

    private String[] filtersForMainTab(int mainTab) {
        return switch (mainTab) {
            case 0 -> this.moduleFilterOptions();
            case 1 -> this.localizedStrings(COLOR_FILTERS);
            case 2 -> this.localizedStrings(CONFIG_FILTERS);
            case 3, 4 -> this.localizedStrings(SINGLE_FILTER);
            default -> this.localizedStrings(SINGLE_FILTER);
        };
    }

    private String[] localizedStrings(String[] values) {
        String[] localized = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            localized[i] = UiLang.localize(values[i]);
        }
        return localized;
    }

    private String[] moduleFilterOptions() {
        List<Section> sections = this.availableModuleSections();
        if (sections.isEmpty()) {
            return SINGLE_FILTER;
        }
        if (sections.size() == 1) {
            return new String[]{UiLang.localize(((Section)((Object)sections.getFirst())).name())};
        }
        String[] options = new String[sections.size() + 1];
        options[0] = UiLang.localize("All");
        for (int i = 0; i < sections.size(); ++i) {
            options[i + 1] = UiLang.localize(sections.get(i).name());
        }
        return options;
    }

    void setSearchBoxBounds(float x, float y, float w, float h) {
        this.searchBoxX = x;
        this.searchBoxY = y;
        this.searchBoxW = w;
        this.searchBoxH = h;
        this.refreshContext();
    }

    private void applyLaunchTargetIfNeeded() {
        if (this.launchTargetApplied || this.launchTarget == null) {
            return;
        }
        this.launchTargetApplied = true;
        this.activeMain = Math.max(0, this.launchTarget.mainTab());
        this.activeFilter = this.activeMain == 0 ? 1 : 0;
        this.activeSubtab = 0;
        if (!this.launchTarget.openModuleDetail() || this.launchTarget.moduleName() == null || this.activeMain != 0) {
            return;
        }
        ModulesTab.SubTab targetSubTab = this.findModuleSubTabByName(this.launchTarget.moduleName());
        if (targetSubTab == null) {
            return;
        }
        this.activeFilter = MainGUIRegistry.moduleFilterFor(targetSubTab);
        this.activeSubTab = targetSubTab;
        this.activeCategoryIdx = 0;
        this.scrollY = 0.0f;
        this.targetScrollY = 0.0f;
        this.modulesOverviewScrollY = 0.0f;
        this.modulesOverviewTargetScrollY = 0.0f;
        this.moduleDetailScrollY = 0.0f;
        this.moduleDetailTargetScrollY = 0.0f;
    }

    private ModulesTab.SubTab findModuleSubTabByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (ModulesTab.SubTab subTab : MainGUIRegistry.MODULE_SUBTABS) {
            if (!subTab.name().equalsIgnoreCase(name) && !UiLang.localize(subTab.name()).equalsIgnoreCase(name)) continue;
            return subTab;
        }
        return null;
    }

    boolean isSearchEditorActiveState() {
        return this.isSearchEditorActive();
    }

    String currentTextBuffer() {
        return this.textBuf.toString();
    }

    int currentTextCursor() {
        return this.textCursor;
    }

    boolean textHasSelection() {
        return this.hasTextSelection();
    }

    int currentTextSelectionStart() {
        return this.textSelectionStart();
    }

    int currentTextSelectionEnd() {
        return this.textSelectionEnd();
    }

    String safeTextSlice(String value, int start, int end) {
        return this.safeSubstring(value, start, end);
    }

    float inlineTextScrollOffsetFor(NVGRenderer nvg, String font, String text, int cursor, float fontSize, float visibleWidth) {
        return this.inlineTextScrollOffset(nvg, font, text, cursor, fontSize, visibleWidth);
    }

    void activateSearchField() {
        if (!this.searchMode) {
            this.commitText();
            this.commitColor();
            this.openDd = null;
            this.focusSearchField();
        }
    }

    void setFilterBarTarget(float x, float w) {
        this.filterBarTargetX = x;
        this.filterBarTargetW = w;
        this.refreshContext();
    }

    void setFilterBarAnimation(float x, float w) {
        this.filterBarAnimX = x;
        this.filterBarAnimW = w;
        this.refreshContext();
    }

    void onFilterSelected(int filterIndex) {
        this.activeFilter = Math.max(0, Math.min(filterIndex, this.currentTabFilters().length - 1));
        this.resetActiveContentScroll();
    }

    boolean searchResultsVisible() {
        return this.showSearchResults();
    }

    boolean hasOpenDropdown() {
        return this.openDd != null;
    }

    boolean hasActiveColorOverlay() {
        return this.activeColor != null;
    }

    void renderSearchContentPanel(NVGRenderer nvg, float mx, float my) {
        this.renderSearchContent(nvg, mx, my);
    }

    void renderProfileContentPanel(NVGRenderer nvg, float mx, float my) {
        this.renderProfileContent(nvg, mx, my);
    }

    void renderContentPanel(NVGRenderer nvg, float mx, float my) {
        this.renderContent(nvg, mx, my);
    }

    void renderContentTopBarPanel(NVGRenderer nvg, float mx, float my) {
        this.renderContentTopBar(nvg, mx, my);
    }

    void renderFilterBarPanel(NVGRenderer nvg, float mx, float my) {
        this.renderFilterBar(nvg, mx, my);
    }

    void renderModuleGridPanel(NVGRenderer nvg, float mx, float my, float scrollTop, float scrollH) {
        this.renderModuleGrid(nvg, mx, my, scrollTop, scrollH);
    }

    void renderFlatContentPanel(NVGRenderer nvg, float mx, float my) {
        this.renderFlatContent(nvg, mx, my);
    }

    void renderDropdownOverlayPanel(NVGRenderer nvg, float mx, float my) {
        this.renderDropdownOverlay(nvg, mx, my);
    }

    void renderColorOverlayPanel(NVGRenderer nvg) {
        this.renderColorOverlay(nvg);
    }

    void renderModuleTopBarPanel(NVGRenderer nvg, float mx, float my) {
        this.renderModuleTopBar(nvg, mx, my);
    }

    void renderModuleDetailBodyPanel(NVGRenderer nvg, float mx, float my) {
        this.renderModuleDetailBody(nvg, mx, my);
    }

    void renderModuleCategoryPanelSection(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        this.renderModuleCategoryPanel(nvg, mx, my, panelTop, panelH);
    }

    void renderModuleSettingsPanelSection(NVGRenderer nvg, float mx, float my, float panelTop, float panelH) {
        this.renderModuleSettingsPanel(nvg, mx, my, panelTop, panelH);
    }

    List<MainGUIModuleSection> moduleSectionsForFilter() {
        ArrayList<MainGUIModuleSection> result = new ArrayList<MainGUIModuleSection>();
        for (Section section : this.sectionsForFilter()) {
            result.add(new MainGUIModuleSection(UiLang.localize(section.name()), section.subtabs()));
        }
        return result;
    }

    float moduleCardHoverProgress(ModulesTab.SubTab subTab) {
        return this.cardHoverAnim.getOrDefault((Object)subTab, Float.valueOf(0.0f)).floatValue();
    }

    void setModuleCardHoverProgress(ModulesTab.SubTab subTab, float progress) {
        this.cardHoverAnim.put(subTab, Float.valueOf(progress));
    }

    void renderModuleCardPanel(NVGRenderer nvg, ModulesTab.SubTab subTab, float x, float y, float w, float hover, float mx, float my) {
        this.renderModuleCard(nvg, subTab, x, y, w, hover, mx, my);
    }

    List<ModulesTab.SubTab> flatSubtabsForCurrentFilter() {
        return this.flatSubtabsForFilter();
    }

    void renderFlatGroupLabelPanel(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my) {
        this.renderFlatGroupLabel(nvg, group, x, y, w, mx, my);
    }

    boolean shouldShowChildren(SettingGroup group) {
        return this.showChildren(group);
    }

    boolean isPetTrackerSettingsGroup(SettingGroup group) {
        return this.isPetTrackerGroup(group);
    }

    float petTrackerGroupHeightValue() {
        return this.petTrackerGroupHeight();
    }

    void renderPetTrackerGroupPanel(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my) {
        this.renderPetTrackerGroup(nvg, group, x, y, w, mx, my);
    }

    float settingHeightFor(Setting setting, float width) {
        return this.settingH(setting, width);
    }

    float moduleSettingsWidth() {
        float rightW = this.contW - 160.0f - 1.0f;
        return rightW - 40.0f;
    }

    float moduleSettingsHeight() {
        return this.contH - 53.0f - 1.0f - 74.0f - 1.0f;
    }

    void renderMainScrollbar(NVGRenderer nvg, float totalContentHeight, float scrollTop, float scrollH, float barX) {
        this.maxScrollY = Math.max(0.0f, totalContentHeight - scrollH + 16.0f);
        this.targetScrollY = Math.max(0.0f, Math.min(this.maxScrollY, this.targetScrollY));
        if (this.maxScrollY <= 0.0f) {
            return;
        }
        float ratio = scrollH / (totalContentHeight + 16.0f);
        float thumbH = Math.max(30.0f, scrollH * ratio);
        float trackT = scrollTop + 2.0f;
        float trackH = scrollH - 4.0f;
        this.sbMainTrackT = trackT;
        this.sbMainTrackH = trackH;
        this.sbMainThumbH = thumbH;
        float thumbY = MainGUI.scrollbarThumbY(trackT, trackH, thumbH, this.scrollY, this.maxScrollY);
        nvg.roundedRect(barX, thumbY, 4.0f, thumbH, 2.0f, Theme.withAlpha(Theme.TEXT_SECONDARY, 0.5f));
    }

    String moduleCardNameFor(ModulesTab.SubTab subTab) {
        return MainGUI.moduleCardName(subTab);
    }

    boolean isModuleConfigurationEditable(ModulesTab.SubTab subTab) {
        return MainGUI.isConfigurationEditable(subTab);
    }

    void exitModuleDetailView() {
        this.exitModuleDetail();
    }

    int getActiveCategoryIndex() {
        return this.activeCategoryIdx;
    }

    Object moduleCategoryAnimationKey(SettingGroup group, boolean isAll) {
        return isAll ? this : group;
    }

    float moduleCategoryHoverProgress(Object key) {
        return this.catHoverAnim.getOrDefault(key, Float.valueOf(0.0f)).floatValue();
    }

    void setModuleCategoryHoverProgress(Object key, float progress) {
        this.catHoverAnim.put(key, Float.valueOf(progress));
    }

    void selectModuleCategory(int categoryIndex) {
        this.activeCategoryIdx = categoryIndex;
        this.scrollY = 0.0f;
        this.targetScrollY = 0.0f;
    }

    void renderGroupHeaderPanel(NVGRenderer nvg, SettingGroup group, float x, float y, float w, float mx, float my, boolean interactive) {
        this.renderGroupHeader(nvg, group, x, y, w, mx, my, interactive);
    }

    void renderPillControl(NVGRenderer nvg, float x, float y, boolean on, Object key) {
        this.renderPill(nvg, x, y, on, key);
    }

    List<String> wrapSettingLabelForRow(NVGRenderer nvg, Setting setting, float rowW) {
        return this.wrapSettingLabel(nvg, setting, rowW);
    }

    float settingLabelFontSizeForRow(Setting setting) {
        return this.settingLabelFontSize(setting);
    }

    float settingLabelLineStepForRow(Setting setting) {
        return this.settingLabelLineStep(setting);
    }

    int wrappedSettingLabelLineCountForRow(Setting setting, float rowW) {
        return this.wrappedSettingLabelLineCount(setting, rowW);
    }

    boolean isActiveSliderEditor(SliderSetting setting) {
        return this.activeSliderField == setting;
    }

    boolean isActiveTextEditor(TextSetting setting) {
        return this.activeText == setting;
    }

    boolean isActiveListEditor(ListSetting setting, int index) {
        return this.activeList == setting && this.activeListIndex == index;
    }

    boolean isOpenDropdown(DropdownSetting setting) {
        return this.openDd == setting;
    }

    boolean isActiveColorSetting(ColorSetting setting) {
        return this.activeColor == setting;
    }

    boolean isActivePositionEditor(PositionSetting setting, int index) {
        return this.activePosField == setting && this.activePosIdx == index;
    }

    void setHoveredColorSetting(ColorSetting setting) {
        this.hoveredColor = setting;
    }

    void setDropdownOverlayBounds(float x, float y, float w, float buttonH) {
        this.ddX = x;
        this.ddY = y;
        this.ddW = w;
        this.ddButtonH = buttonH;
    }

    void setEditorBounds(float x, float y, float w, float h) {
        this.setActiveEditorBounds(x, y, w, h);
    }

    String formatSliderValue(SliderSetting setting) {
        return this.fmtSlider(setting);
    }

    String formatRangeSliderValue(RangeSliderSetting setting) {
        return this.fmtRangeSlider(setting);
    }

    void renderDropdownActionButtonsControl(NVGRenderer nvg, DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        this.renderDropdownActionButtons(nvg, dropdown, fieldX, fieldY, fieldH, mx, my);
    }

    void renderListActionButtonControl(NVGRenderer nvg, float x, float y, float w, String label, boolean hovered, boolean enabled) {
        this.renderListActionButton(nvg, x, y, w, label, hovered, enabled);
    }

    float settingDescriptionWidth(Setting setting,float rowW){return textLayout.descriptionWidth(setting,rowW);}
    float settingDescriptionHeight(Setting setting,float rowW) {return textLayout.descriptionHeight(setting,rowW);}

    float textInlineFieldWidthForRow(float innerWidth) {
        return this.textInlineFieldWidth(innerWidth);
    }

    float inlineTextScrollOffsetApproxFor(String text, int cursor, float fontSize, float visibleWidth) {
        return this.inlineTextScrollOffsetApprox(text, cursor, fontSize, visibleWidth);
    }

    int clickAreaCount() {
        return this.clickAreas.size();
    }

    private void syncFrameLayoutForWindowSize(float prevPhysW, float prevPhysH) {
        boolean sizeChanged;
        if (this.dragSetting == null && this.dragRangeSetting == null) {
            uiScale = UiScalePolicy.effective(this.width * this.pr, this.height * this.pr, Theme.UI_SCALE);
            uiTextScale = Theme.TEXT_SCALE;
        }
        boolean bl = sizeChanged = this.context.layout.lastWidth != this.width || this.context.layout.lastHeight != this.height || Math.abs(this.context.layout.lastPixelRatio - this.pr) > 0.001f || Math.abs(this.context.layout.lastUiScale - uiScale) > 0.001f;
        if (!sizeChanged) {
            this.physW = (float)this.width * this.pr;
            this.physH = (float)this.height * this.pr;
            return;
        }
        this.applyFrameLayout(false, prevPhysW, prevPhysH);
    }

    private void applyFrameLayout(boolean forceCenter, float prevPhysW, float prevPhysH) {
        MainGUIContext.LayoutState frameLayout = this.layoutHelper.computeFrameLayout(this.width, this.height, this.pr, uiScale, 60.0f);
        float oldPx = this.px;
        float oldPy = this.py;
        float oldPw = this.pw;
        float oldPh = this.ph;
        this.pw = frameLayout.pw;
        this.ph = frameLayout.ph;
        this.physW = frameLayout.physW;
        this.physH = frameLayout.physH;
        this.px = frameLayout.px;
        this.py = frameLayout.py;
        this.contX = this.px + 60.0f;
        this.contY = this.py;
        this.contW = this.pw - 60.0f;
        this.contH = this.ph;
        this.context.layout.lastWidth = this.width;
        this.context.layout.lastHeight = this.height;
        this.context.layout.lastPixelRatio = this.pr;
        this.context.layout.lastUiScale = uiScale;
        this.refreshContext();
    }

    void trimClickAreas(int clickCount) {
        while (this.clickAreas.size() > clickCount) {
            this.clickAreas.remove(this.clickAreas.size() - 1);
        }
    }

    void clearTransitionOverlaysForPassiveRender() {
        this.openDd = null;
        this.activeColor = null;
    }

    void setSuppressNestedContentScissorState(boolean suppress) {
        this.suppressNestedContentScissor = suppress;
    }

    MainGUITransientRenderState captureTransientRenderState() {
        MainGUITransientRenderState state = new MainGUITransientRenderState();
        state.hoveredColor = this.hoveredColor;
        state.activeEditorBoundsSet = this.activeEditorBoundsSet;
        state.filterBarAnimX = this.filterBarAnimX;
        state.filterBarAnimW = this.filterBarAnimW;
        state.filterBarTargetX = this.filterBarTargetX;
        state.filterBarTargetW = this.filterBarTargetW;
        state.filterBarInited = this.filterBarInited;
        state.maxScrollY = this.maxScrollY;
        state.searchMaxScrollY = this.searchMaxScrollY;
        state.profileMaxScrollY = this.profileMaxScrollY;
        state.subtabMaxScrollY = this.subtabMaxScrollY;
        state.sbMainTrackT = this.sbMainTrackT;
        state.sbMainTrackH = this.sbMainTrackH;
        state.sbMainThumbH = this.sbMainThumbH;
        state.sbSrchTrackT = this.sbSrchTrackT;
        state.sbSrchTrackH = this.sbSrchTrackH;
        state.sbSrchThumbH = this.sbSrchThumbH;
        state.sbProfTrackT = this.sbProfTrackT;
        state.sbProfTrackH = this.sbProfTrackH;
        state.sbProfThumbH = this.sbProfThumbH;
        state.sbSubTrackT = this.sbSubTrackT;
        state.sbSubTrackH = this.sbSubTrackH;
        state.sbSubThumbH = this.sbSubThumbH;
        state.openDropdown = this.openDd;
        state.activeColor = this.activeColor;
        state.suppressNestedContentScissor = this.suppressNestedContentScissor;
        state.scrollY = this.scrollY;
        state.targetScrollY = this.targetScrollY;
        return state;
    }

    void restoreTransitionRenderState(MainGUITransientRenderState state, boolean interactive, boolean restoreScroll) {
        if (!interactive) {
            this.hoveredColor = state.hoveredColor;
            this.activeEditorBoundsSet = state.activeEditorBoundsSet;
            this.filterBarAnimX = state.filterBarAnimX;
            this.filterBarAnimW = state.filterBarAnimW;
            this.filterBarTargetX = state.filterBarTargetX;
            this.filterBarTargetW = state.filterBarTargetW;
            this.filterBarInited = state.filterBarInited;
            this.maxScrollY = state.maxScrollY;
            this.searchMaxScrollY = state.searchMaxScrollY;
            this.profileMaxScrollY = state.profileMaxScrollY;
            this.subtabMaxScrollY = state.subtabMaxScrollY;
            this.sbMainTrackT = state.sbMainTrackT;
            this.sbMainTrackH = state.sbMainTrackH;
            this.sbMainThumbH = state.sbMainThumbH;
            this.sbSrchTrackT = state.sbSrchTrackT;
            this.sbSrchTrackH = state.sbSrchTrackH;
            this.sbSrchThumbH = state.sbSrchThumbH;
            this.sbProfTrackT = state.sbProfTrackT;
            this.sbProfTrackH = state.sbProfTrackH;
            this.sbProfThumbH = state.sbProfThumbH;
            this.sbSubTrackT = state.sbSubTrackT;
            this.sbSubTrackH = state.sbSubTrackH;
            this.sbSubThumbH = state.sbSubThumbH;
        }
        this.openDd = state.openDropdown;
        this.activeColor = state.activeColor;
        this.suppressNestedContentScissor = state.suppressNestedContentScissor;
        if (restoreScroll) {
            this.scrollY = state.scrollY;
            this.targetScrollY = state.targetScrollY;
        }
    }

    static float groupGap() {
        return 12.0f;
    }

    float contentX() {
        return this.contX;
    }

    float contentY() {
        return this.contY;
    }

    float contentW() {
        return this.contW;
    }

    float contentH() {
        return this.contH;
    }

    List<SettingGroup> activeGroupsForInteraction() {
        return this.groups();
    }

    List<SettingGroup> activeModuleClickGroups() {
        if (this.activeSubTab == null) {
            return List.of();
        }
        int maxCatIdx = this.activeSubTab.groups().size();
        if (this.activeCategoryIdx > maxCatIdx) {
            this.activeCategoryIdx = 0;
        }
        return this.activeCategoryIdx == 0 ? this.activeSubTab.groups() : List.of(this.activeSubTab.groups().get(this.activeCategoryIdx - 1));
    }

    boolean isActiveModuleConfigurationEditable() {
        return this.activeSubTab != null && MainGUI.isConfigurationEditable(this.activeSubTab);
    }

    boolean handlePetTrackerGroupInteraction(SettingGroup group, float mx, float my, float x, float y, float w) {
        return this.handlePetTrackerGroupClick(group, mx, my, x, y, w);
    }

    void focusListEditorItem(ListSetting list, int index) {
        this.focusListItem(list, index);
    }

    void insertListEditorItem(ListSetting list, int insertIndex) {
        this.addListItem(list, insertIndex);
    }

    void deleteListEditorItem(ListSetting list, int index) {
        this.removeListItem(list, index);
    }

    void focusSliderEditorField(SliderSetting slider) {
        this.focusSliderField(slider);
    }

    void activateTextSettingEditor(TextSetting setting) {
        this.activeText = setting;
        this.textBuf = new StringBuilder(setting.getValue());
        this.textCursor = this.textBuf.length();
        this.clearTextSelection();
    }

    void focusPositionEditorField(PositionSetting setting, int index, double value, float mx, float my) {
        this.activePosField = setting;
        this.activePosIdx = index;
        this.textBuf = new StringBuilder(String.format("%.1f", value));
        this.textCursor = this.textBuf.length();
        this.clearTextSelection();
        this.beginInlineCaretPress(mx, my);
    }

    void beginInlineFieldCaretPress(float x, float y, float w, float h, String value, float fontSize, float startX, float mx, float my) {
        this.setActiveEditorBounds(x, y, w, h);
        this.textCursor = MainGUIInlineTextEditor.cursorFromInlineText(value, fontSize, startX, mx);
        this.clearTextSelection();
        this.beginInlineCaretPress(mx, my);
    }

    int dropdownActionIndexFor(DropdownSetting dropdown, float fieldX, float fieldY, float fieldH, float mx, float my) {
        return this.dropdownActionIndexAt(dropdown, fieldX, fieldY, fieldH, mx, my);
    }

    void openDropdownOverlayFor(DropdownSetting dropdown, float x, float y, float w, float buttonH) {
        this.openDd = dropdown;
        this.ddAnimAmt = 0.0f;
        this.ddX = x;
        this.ddY = y;
        this.ddW = w;
        this.ddButtonH = buttonH;
    }

    void activateColorSetting(ColorSetting setting) {
        this.activeColor = setting;
        this.argbToHsv(setting.getValue());
        this.cpHexFocus = false;
        this.cpHexBuf.setLength(0);
        this.cpDrag = 0;
    }

    void startSliderDrag(SliderSetting setting, float trackX, float trackW) {
        this.activeSliderField = null;
        this.dragSetting = setting;
        this.dragTrackX = trackX;
        this.dragTrackW = trackW;
        this.dragMin = setting.getMin();
        this.dragMax = setting.getMax();
    }

    void startRangeSliderDrag(RangeSliderSetting setting, float trackX, float trackW, float mx) {
        this.activeSliderField = null;
        this.dragSetting = null;
        this.dragRangeSetting = setting;
        this.dragTrackX = trackX;
        this.dragTrackW = trackW;
        this.dragMin = setting.getMin();
        this.dragMax = setting.getMax();
        float lowerT = (setting.getLowerValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float upperT = (setting.getUpperValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float lowerX = trackX + trackW * lowerT;
        float upperX = trackX + trackW * upperT;
        this.dragRangeHandle = Math.abs(mx - lowerX) <= Math.abs(mx - upperX) ? 1 : 2;
    }

    boolean hasDraggedSliderSetting() {
        return this.dragSetting != null;
    }

    boolean hasDraggedRangeSliderSetting() {
        return this.dragRangeSetting != null;
    }

    void applySliderDragValue(float mx) {
        if (this.dragSetting == null) {
            return;
        }
        float t = Math.max(0.0f, Math.min(1.0f, (mx - this.dragTrackX) / this.dragTrackW));
        this.dragSetting.setValue(this.dragMin + t * (this.dragMax - this.dragMin));
    }

    void applyRangeSliderDragValue(float mx) {
        if (this.dragRangeSetting == null || this.dragRangeHandle == 0) {
            return;
        }
        float t = Math.max(0.0f, Math.min(1.0f, (mx - this.dragTrackX) / this.dragTrackW));
        float value = this.dragMin + t * (this.dragMax - this.dragMin);
        if (this.dragRangeHandle == 1) {
            this.dragRangeSetting.setLowerValue(value);
        } else {
            this.dragRangeSetting.setUpperValue(value);
        }
    }

    float scaleMouseX(double rawX) {
        return (float)rawX * this.pr / uiScale;
    }

    float scaleMouseY(double rawY) {
        return (float)rawY * this.pr / uiScale;
    }

    void handleRightClickEvent(float mx, float my) {
        this.handleRightClick(mx, my);
    }

    boolean handleColorPickerClickEvent(float mx, float my) {
        return this.handleColorPickerClick(mx, my);
    }

    void handleDropdownClickEvent(float mx, float my) {
        this.handleDropdownClick(mx, my);
    }

    boolean hasInlineEditorFocus() {
        return this.hasActiveInlineEditor();
    }

    boolean isPointInsideActiveEditorState(float mx, float my) {
        return this.isPointInsideActiveEditor(mx, my);
    }

    boolean isOutsidePanel(float mx, float my) {
        return mx < this.px || mx > this.px + this.pw || my < this.py || my > this.py + this.ph;
    }

    void handleOutsidePanelClick() {
        if (this.searchMode) {
            this.actions.clearSearch();
        }
        this.commitText();
    }

    boolean tryStartContentScrollbarDrag(float mx, float my) {
        float sbX = this.contX + this.contW - 6.0f;
        if (this.showSearchResults() && this.searchMaxScrollY > 0.0f && MainGUI.hitScrollbarTrack(mx, my, this.sbSrchTrackT, this.sbSrchTrackH, sbX)) {
            this.startScrollbarDrag(2, mx, my, this.sbSrchTrackT, this.sbSrchTrackH, this.sbSrchThumbH, this.searchScrollY, this.searchMaxScrollY, sbX);
            return true;
        }
        if (!this.showSearchResults() && this.maxScrollY > 0.0f && MainGUI.hitScrollbarTrack(mx, my, this.sbMainTrackT, this.sbMainTrackH, sbX)) {
            this.startScrollbarDrag(1, mx, my, this.sbMainTrackT, this.sbMainTrackH, this.sbMainThumbH, this.scrollY, this.maxScrollY, sbX);
            return true;
        }
        return false;
    }

    boolean tryStartSubtabScrollbarDrag(float mx, float my) {
        float subSbX = this.px + 60.0f - 5.0f;
        if (this.subtabMaxScrollY > 0.0f && MainGUI.hitScrollbarTrack(mx, my, this.sbSubTrackT, this.sbSubTrackH, subSbX)) {
            this.startScrollbarDrag(4, mx, my, this.sbSubTrackT, this.sbSubTrackH, this.sbSubThumbH, this.subtabScrollY, this.subtabMaxScrollY, subSbX);
            return true;
        }
        return false;
    }

    boolean isSearchButtonHit(float mx, float my) {
        return mx >= this.btnSearchX && mx <= this.btnSearchX + this.btnSearchW && my >= this.btnSearchY && my <= this.btnSearchY + this.btnSearchH;
    }

    void handleSearchButtonClick() {
        boolean bl = this.searchMode = !this.searchMode;
        if (!this.searchMode) {
            this.actions.clearSearch();
        } else {
            this.commitText();
            this.commitColor();
            this.openDd = null;
            this.focusSearchField();
        }
    }

    boolean isPointInsideSearchFieldState(float mx, float my) {
        return this.isPointInsideSearchField(mx, my);
    }

    void handleSearchFieldClick(float mx, float my) {
        if (!this.searchMode) {
            this.commitText();
            this.commitColor();
            this.openDd = null;
            this.focusSearchField();
        } else if (!this.isSearchEditorActive()) {
            this.focusSearchField();
        }
        float textPad = 6.0f;
        float fontSize = 11.0f;
        float visibleTextW = this.searchBoxW - 30.0f - textPad * 2.0f;
        float textOffset = this.inlineTextScrollOffsetApprox(this.textBuf.toString(), this.textCursor, fontSize, visibleTextW);
        float startX = this.searchBoxX + 30.0f + textPad - textOffset;
        this.textCursor = MainGUI.cursorFromInlineText(this.textBuf.toString(), fontSize, startX, mx);
        this.clearTextSelection();
        this.beginInlineCaretPress(mx, my);
    }

    void collapseSearchIfNeeded() {
        if (this.searchMode) {
            this.searchMode = false;
            this.clearTextSelection();
            this.normalizeTextCursorState();
        }
    }

    boolean tryBeginPanelDrag(float mx, float my) {
        return false; // The settings panel is always anchored to the window center.
    }

    void setLoginButtonBounds(float x, float y, float w, float h) {
        this.loginButtonX = x;
        this.loginButtonY = y;
        this.loginButtonW = w;
        this.loginButtonH = h;
    }

    void clearLoginButtonBounds() {
        this.loginButtonW = 0.0f;
        this.loginButtonH = 0.0f;
    }

    boolean tryHandleSidebarNavigationClick(float mx, float my, Minecraft minecraft) {
        float sbClickW = 60 + this.sidebarAnim * ((this.computedSidebarExpanded > 0 ? this.computedSidebarExpanded : 194) - 60);
        for (int i = 0; i < MainGUISidebar.entries().size(); i++) {
            float y = MainGUISidebar.rowY(i, this.py, this.ph);
            if (mx >= this.px && mx < this.px + sbClickW && my >= y && my < y + 44) {
                this.selectSidebarEntry(i);
                return true;
            }
        }
        if (MainGUISidebar.footerContains(mx, my, this.px, this.py, this.ph, sbClickW, this.sidebarAnim)) {
            this.actions.switchMainTab(5);
            return true;
        }
        return false;
    }

    boolean isSidebarExpandedOverContent(float mx, float my) {
        float sbClickW = 60.0f + this.sidebarAnim * ((this.computedSidebarExpanded > 0.0f ? this.computedSidebarExpanded : 194.0f) - 60.0f);
        return this.sidebarAnim > 0.01f && mx >= this.px && mx < this.px + sbClickW && my >= this.py && my <= this.py + this.ph;
    }

    private void renderOptionalFeatureHud(NVGRenderer nvg, float dt) {
        NotificationManager.render(nvg, this.width, this.height);
    }

    boolean runClickAreas(float mx, float my) {
        for (ClickArea area : this.clickAreas) {
            if (!(mx >= area.x()) || !(mx <= area.x() + area.w()) || !(my >= area.y()) || !(my <= area.y() + area.h())) continue;
            area.action().run();
            return true;
        }
        return false;
    }

    void handleContentAreaClick(float mx, float my) {
        this.settingInteractionController.handleContentAreaClick(mx, my);
    }

    boolean dragPanel(float mx, float my) {
        if (!this.panelDragging) {
            return false;
        }
        this.px = Math.max(0.0f, Math.min(this.physW / uiScale - this.pw, mx - this.dragPanelOffX));
        this.py = Math.max(0.0f, Math.min(this.physH / uiScale - this.ph, my - this.dragPanelOffY));
        this.contX = this.px + 60.0f;
        this.contY = this.py;
        return true;
    }

    boolean dragSlider(float mx) {
        return this.settingInteractionController.dragSlider(mx);
    }

    boolean dragRangeSlider(float mx) {
        return this.settingInteractionController.dragRangeSlider(mx);
    }

    boolean dragActiveColorOverlay(float mx, float my) {
        if (this.activeColor == null || this.cpDrag == 0) {
            return false;
        }
        this.handleColorPickerDrag(mx, my);
        return true;
    }

    boolean updateActiveTextSelection(float mx) {
        if (!this.hasActiveInlineEditor() && !this.isSearchEditorActive() || !this.textSelectionDragging) {
            return false;
        }
        if (this.isSearchEditorActive()) {
            this.updateSearchSelectionFromMouse(mx);
            this.syncSearchQueryFromEditor();
        } else {
            this.updateInlineSelectionFromMouse(mx);
        }
        return true;
    }

    boolean tryBeginActiveTextSelection(int button, float mx, float my) {
        if (!((this.hasActiveInlineEditor() || this.isSearchEditorActive()) && button == 0 && (this.hasActiveInlineEditor() && this.isPointInsideActiveEditor(mx, my) || this.isSearchEditorActive() && this.isPointInsideSearchField(mx, my)) && this.shouldBeginInlineSelectionDrag(mx, my))) {
            return false;
        }
        this.textSelectionDragging = true;
        this.textSelectionPendingDrag = false;
        this.textSelectionAnchor = Math.max(0, Math.min(this.textSelectionPressCursor, this.textBuf.length()));
        if (this.isSearchEditorActive()) {
            this.updateSearchSelectionFromMouse(mx);
            this.syncSearchQueryFromEditor();
        } else {
            this.updateInlineSelectionFromMouse(mx);
        }
        return true;
    }

    boolean dragScrollbar(float my) {
        if (this.sbDragging == 0) {
            return false;
        }
        this.updateScrollbarDrag(my, true);
        return true;
    }

    void finishMouseInteractions() {
        this.dragSetting = null;
        this.dragRangeSetting = null;
        this.dragRangeHandle = 0;
        this.panelDragging = false;
        this.cpDrag = 0;
        this.sbDragging = 0;
        this.sbDragThumbOffset = 0.0f;
        this.textSelectionDragging = false;
        this.textSelectionPendingDrag = false;
        this.textSelectionPressCursor = -1;
        this.normalizeTextCursorState();
    }

    boolean handleMouseScroll(float mx, float my, float scrollYDelta) {
        if (mx < this.contX || mx > this.contX + this.contW || my < this.contY || my > this.contY + this.contH) {
            return false;
        }
        float delta = scrollYDelta * 20.0f;
        if (this.showSearchResults()) {
            this.searchTargetScrollY = Math.max(0.0f, Math.min(this.searchMaxScrollY, this.searchTargetScrollY - delta));
        } else {
            this.targetScrollY = Math.max(0.0f, Math.min(this.maxScrollY, this.targetScrollY - delta));
            this.syncActiveBodyScrollState();
        }
        return true;
    }

    boolean handleEscapeKey() {
        if (this.activeKeybindCapture != null) {
            this.activeKeybindCapture = null;
            return true;
        }
        if (this.searchMode || !this.searchQuery.isEmpty()) {
            this.actions.clearSearch();
            return true;
        }
        if (this.profileRenameFocus) {
            this.cancelProfileRename();
            return true;
        }
        if (this.profileNameFocus) {
            this.profileNameFocus = false;
            return true;
        }
        if (this.activeSubTab != null) {
            this.exitModuleDetail();
            return true;
        }
        this.commitText();
        this.commitColor();
        if (this.openDd != null) {
            this.openDd = null;
            return true;
        }
        this.onClose();
        return true;
    }

    boolean handleInlineControlShortcut(int key) {
        return this.inlineTextEditor.handleControlShortcut(key);
    }

    boolean handleSearchEditorKey(int key, boolean ctrl, boolean shift) {
        return this.inlineTextEditor.handleSearchEditorKeyPressed(key, ctrl, shift);
    }

    boolean handleProfileNameKey(int key) {
        return this.inlineTextEditor.handleProfileNameKeyPressed(key);
    }

    boolean handleInlineEditorKey(int key, boolean ctrl, boolean shift) {
        return this.inlineTextEditor.handleInlineEditorKeyPressed(key, ctrl, shift);
    }

    boolean handleOverlayHexKey(int key) {
        return this.overlayController.handleHexKeyPressed(key);
    }

    boolean handleInlineCharTyped(char ch) {
        if (this.activeKeybindCapture != null) {
            return true;
        }
        return this.inlineTextEditor.handleCharTyped(ch);
    }

    boolean isAwaitingKeybindCapture(KeybindSetting setting) {
        return this.activeKeybindCapture == setting;
    }

    void beginKeybindCapture(KeybindSetting setting) {
        this.activeKeybindCapture = setting;
    }

    void clearKeybindCapture() {
        this.activeKeybindCapture = null;
    }

    boolean isAnyKeybindCaptureActive() {
        return this.activeKeybindCapture != null;
    }

    boolean handleKeybindCapture(KeyEvent input) {
        if (this.activeKeybindCapture == null) {
            return false;
        }
        if (dev.kuan.kuanperion.HotkeyChord.isModifierKey(input.key())) return true;
        KeybindSetting setting = this.activeKeybindCapture;
        this.activeKeybindCapture = null;
        if (input.key() == 256) {
            return true;
        }
        if ((input.key() == 261 || input.key() == 259) && (input.modifiers() & dev.kuan.kuanperion.HotkeyChord.MASK) == 0) {
            setting.clearBinding();
            return true;
        }
        if (input.key() == -1) {
            setting.setBoundKey(InputConstants.Type.SCANCODE.getOrCreate(input.scancode()),input.modifiers());
            return true;
        }
        setting.setBoundKey(InputConstants.Type.KEYSYM.getOrCreate(input.key()),input.modifiers());
        return true;
    }

    boolean handleKeybindMouseButtonCapture(int button) {
        if (this.activeKeybindCapture == null) {
            return false;
        }
        if (button == 0) {
            this.activeKeybindCapture = null;
            return false;
        }
        KeybindSetting setting = this.activeKeybindCapture;
        this.activeKeybindCapture = null;
        setting.setBoundKey(InputConstants.Type.MOUSE.getOrCreate(button),dev.kuan.kuanperion.KuanperionClient.currentHotkeyModifiers());
        return true;
    }

    boolean handleOverlayHexChar(char ch) {
        return this.overlayController.handleHexChar(ch);
    }

    float panelX() {
        return this.px;
    }

    float panelY() {
        return this.py;
    }

    float panelW() {
        return this.pw;
    }

    float panelH() {
        return this.ph;
    }

    DropdownSetting currentOpenDropdown() {
        return this.openDd;
    }

    void closeOpenDropdown() {
        this.openDd = null;
    }

    float dropdownOverlayX() {
        return this.ddX;
    }

    float dropdownOverlayY() {
        return this.ddY;
    }

    float dropdownOverlayW() {
        return this.ddW;
    }

    float dropdownButtonHeight() {
        return this.ddButtonH;
    }

    float dropdownAnimAmount() {
        return this.ddAnimAmt;
    }

    ColorSetting activeColorSetting() {
        return this.activeColor;
    }

    void clearActiveColorSetting() {
        this.activeColor = null;
    }

    float colorHue() {
        return this.cpHue;
    }

    void setColorHue(float hue) {
        this.cpHue = hue;
    }

    float colorSaturation() {
        return this.cpSat;
    }

    void setColorSaturation(float saturation) {
        this.cpSat = saturation;
    }

    float colorValue() {
        return this.cpVal;
    }

    void setColorValue(float value) {
        this.cpVal = value;
    }

    float colorAlpha() {
        return this.cpAlpha;
    }

    void setColorAlpha(float alpha) {
        this.cpAlpha = alpha;
    }

    int colorDragMode() {
        return this.cpDrag;
    }

    void setColorDragMode(int dragMode) {
        this.cpDrag = dragMode;
    }

    boolean isColorHexFocused() {
        return this.cpHexFocus;
    }

    void setColorHexFocused(boolean focused) {
        this.cpHexFocus = focused;
    }

    StringBuilder colorHexBuffer() {
        return this.cpHexBuf;
    }

    void setColorPickerPanelBounds(float x, float y, float w, float h) {
        this.cpPX = x;
        this.cpPY = y;
        this.cpPW = w;
        this.cpPH = h;
    }

    float colorPickerPanelX() {
        return this.cpPX;
    }

    float colorPickerPanelY() {
        return this.cpPY;
    }

    float colorPickerPanelW() {
        return this.cpPW;
    }

    float colorPickerPanelH() {
        return this.cpPH;
    }

    void setColorPickerSvBounds(float x, float y, float w, float h) {
        this.cpSvX = x;
        this.cpSvY = y;
        this.cpSvW = w;
        this.cpSvH = h;
    }

    float colorPickerSvX() {
        return this.cpSvX;
    }

    float colorPickerSvY() {
        return this.cpSvY;
    }

    float colorPickerSvW() {
        return this.cpSvW;
    }

    float colorPickerSvH() {
        return this.cpSvH;
    }

    void setColorPickerBarBounds(float hueY, float alphaY, float barH) {
        this.cpHBarY = hueY;
        this.cpABarY = alphaY;
        this.cpBarH = barH;
    }

    float colorPickerHueBarY() {
        return this.cpHBarY;
    }

    float colorPickerAlphaBarY() {
        return this.cpABarY;
    }

    float colorPickerBarH() {
        return this.cpBarH;
    }

    void setColorPickerHexBounds(float x, float y, float w, float h) {
        this.cpHexX = x;
        this.cpHexY = y;
        this.cpHexW = w;
        this.cpHexH = h;
    }

    float colorPickerHexX() {
        return this.cpHexX;
    }

    float colorPickerHexY() {
        return this.cpHexY;
    }

    float colorPickerHexW() {
        return this.cpHexW;
    }

    float colorPickerHexH() {
        return this.cpHexH;
    }

    public record LaunchTarget(int mainTab, String moduleName, boolean openModuleDetail) {
        public static LaunchTarget bootstrapAuthentication() {
            return new LaunchTarget(0, "Authentication", true);
        }
    }

    private record ClickArea(float x, float y, float w, float h, Runnable action) {
    }

    private record Section(String name, List<ModulesTab.SubTab> subtabs) {
    }
}
