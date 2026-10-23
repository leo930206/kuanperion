// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUIContentViewState;
import dev.kuan.kuanperion.ui.settings.ColorSetting;
import dev.kuan.kuanperion.ui.settings.DropdownSetting;
import dev.kuan.kuanperion.ui.settings.ListSetting;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.PositionSetting;
import dev.kuan.kuanperion.ui.settings.RangeSliderSetting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import dev.kuan.kuanperion.ui.settings.SliderSetting;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import java.util.Set;

final class MainGUIContext {
    final LayoutState layout = new LayoutState();
    final NavigationState navigation = new NavigationState();
    final ScrollState scroll = new ScrollState();
    final AnimationState animation = new AnimationState();
    final EditorState editor = new EditorState();
    final OverlayState overlay = new OverlayState();
    final InteractionState interaction = new InteractionState();
    final TransitionState transition = new TransitionState();

    MainGUIContext() {
    }

    static final class LayoutState {
        float px;
        float py;
        float pw;
        float ph;
        float contX;
        float contY;
        float contW;
        float contH;
        float pr = 1.0f;
        float physW;
        float physH;
        float btnSearchX;
        float btnSearchY;
        float btnSearchW;
        float btnSearchH;
        float searchBoxX;
        float searchBoxY;
        float searchBoxW;
        float searchBoxH;
        float activeEditorX;
        float activeEditorY;
        float activeEditorW;
        float activeEditorH;
        int lastWidth = -1;
        int lastHeight = -1;
        float lastPixelRatio = -1.0f;
        float lastUiScale = -1.0f;

        LayoutState() {
        }
    }

    static final class NavigationState {
        int activeMain;
        int activeSubtab;
        int activeFilter;
        ModulesTab.SubTab activeSubTab;
        int activeCategoryIdx;
        boolean searchMode;
        String searchQuery = "";
        String profileNameInput = "";
        boolean profileNameFocus;
        int profileNameTarget;
        String profileRenameInput = "";
        String profileRenameOriginal = "";
        boolean profileRenameFocus;
        int profileRenameTarget;
        boolean suppressNestedContentScissor;

        NavigationState() {
        }
    }

    static final class ScrollState {
        float scrollY;
        float targetScrollY;
        float maxScrollY;
        float modulesOverviewScrollY;
        float modulesOverviewTargetScrollY;
        float moduleDetailScrollY;
        float moduleDetailTargetScrollY;
        float searchScrollY;
        float searchTargetScrollY;
        float searchMaxScrollY;
        float profileScrollY;
        float profileTargetScrollY;
        float profileMaxScrollY;
        float subtabScrollY;
        float subtabTargetScrollY;
        float subtabMaxScrollY;
        float sbMainTrackT;
        float sbMainTrackH;
        float sbMainThumbH;
        float sbSrchTrackT;
        float sbSrchTrackH;
        float sbSrchThumbH;
        float sbProfTrackT;
        float sbProfTrackH;
        float sbProfThumbH;
        float sbSubTrackT;
        float sbSubTrackH;
        float sbSubThumbH;
        long lastFrameTimeNanos;

        ScrollState() {
        }
    }

    static final class AnimationState {
        float alpha;
        float animScale;
        float animOffsetY;
        float sidebarAnim;
        float computedSidebarExpanded;
        float animMainSel;
        float animSubSel;
        float filterBarAnimX;
        float filterBarAnimW;
        float filterBarTargetX;
        float filterBarTargetW;
        boolean filterBarInited;
        float ddAnimAmt;

        AnimationState() {
        }
    }

    static final class EditorState {
        SliderSetting activeSliderField;
        TextSetting activeText;
        ListSetting activeList;
        int activeListIndex = -1;
        PositionSetting activePosField;
        int activePosIdx;
        String textBuffer = "";
        int textCursor;
        int textSelectionAnchor = -1;
        boolean textSelectionDragging;
        boolean textSelectionPendingDrag;
        int textSelectionPressCursor = -1;
        float textSelectionPressX;
        float textSelectionPressY;
        boolean activeEditorBoundsSet;
        boolean cpHexFocus;
        String cpHexBuffer = "";

        EditorState() {
        }
    }

    static final class OverlayState {
        DropdownSetting openDropdown;
        float dropdownX;
        float dropdownY;
        float dropdownW;
        float dropdownButtonH;
        ColorSetting hoveredColor;
        ColorSetting activeColor;
        float cpHue;
        float cpSat = 1.0f;
        float cpVal = 1.0f;
        float cpAlpha = 1.0f;
        int cpDrag;
        float cpPX;
        float cpPY;
        float cpPW;
        float cpPH;
        float cpSvX;
        float cpSvY;
        float cpSvW;
        float cpSvH;
        float cpHBarY;
        float cpABarY;
        float cpBarH;
        float cpHexX;
        float cpHexY;
        float cpHexW;
        float cpHexH;

        OverlayState() {
        }
    }

    static final class InteractionState {
        int scrollbarDragging;
        float scrollbarDragThumbOffset;
        SliderSetting dragSetting;
        RangeSliderSetting dragRangeSetting;
        float dragTrackX;
        float dragTrackW;
        float dragMin;
        float dragMax;
        int dragRangeHandle;
        boolean panelDragging;
        float dragPanelOffX;
        float dragPanelOffY;
        Set<SettingGroup> forcedOverride;

        InteractionState() {
        }
    }

    static final class TransitionState {
        MainGUIContentViewState fromState;
        MainGUIContentViewState toState;
        long startNanos;

        TransitionState() {
        }
    }
}
