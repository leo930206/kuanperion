// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContentViewState;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.MainGUITransientRenderState;
import dev.kuan.kuanperion.ui.theme.Theme;

final class MainGUITransitionRenderer {
    private final MainGUI owner;

    MainGUITransitionRenderer(MainGUI owner) {
        this.owner = owner;
    }

    void syncContentTransition() {
        MainGUIContext.TransitionState transition = this.owner.context().transition;
        MainGUIContentViewState current = this.owner.snapshotContentState();
        if (transition.toState == null) {
            transition.toState = current;
            return;
        }
        if (!transition.toState.searchQuery().equals(current.searchQuery())) {
            // Typing updates results in-place; it is not page navigation.
            transition.fromState = null;
            transition.toState = current;
            return;
        }
        if (!transition.toState.equals((Object)current)) {
            transition.fromState = transition.toState;
            transition.toState = current;
            transition.startNanos = System.nanoTime();
        }
    }

    void renderContentWithTransition(NVGRenderer nvg, float mx, float my) {
        MainGUIContext.TransitionState transition = this.owner.context().transition;
        MainGUIContentViewState fromState = transition.fromState;
        MainGUIContentViewState toState = transition.toState;
        if (fromState == null || toState == null) {
            this.owner.renderContentPanel(nvg, mx, my);
            return;
        }
        float durationMs = Math.max(1.0f, Theme.ANIM_TIME_MS);
        float elapsedMs = Math.max(0.0f, (float)(System.nanoTime() - transition.startNanos) / 1000000.0f);
        float rawT = Math.max(0.0f, Math.min(1.0f, elapsedMs / durationMs));
        float t = rawT * rawT * (3.0f - 2.0f * rawT);
        float strongEaseT = rawT * rawT * rawT * (rawT * (rawT * 6.0f - 15.0f) + 10.0f);
        if (rawT >= 1.0f) {
            transition.fromState = null;
            this.owner.renderContentPanel(nvg, mx, my);
            return;
        }
        float outgoingOffsetY = -22.0f * t;
        float incomingOffsetY = 22.0f * (1.0f - t);
        float clipInset = 1.0f;
        float fullClipX = this.owner.contentX() + clipInset;
        float fullClipY = this.owner.contentY() + clipInset;
        float fullClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
        float fullClipH = Math.max(0.0f, this.owner.contentH() - clipInset * 2.0f);
        boolean fromModuleDetail = this.isModuleDetailState(fromState);
        boolean toModuleDetail = this.isModuleDetailState(toState);
        boolean categoryOnlyTransition = this.isCategoryOnlyTransition(fromState, toState);
        boolean filterOnlyTransition = this.isFilterOnlyTransition(fromState, toState);
        if (categoryOnlyTransition) {
            this.renderModuleDetailTopBarForState(nvg, mx, my, toState, true);
            float bodyClipX = this.owner.contentX() + clipInset;
            float bodyClipY = this.owner.contentY() + 53.0f + 1.0f + clipInset;
            float bodyClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
            float bodyClipH = Math.max(0.0f, this.owner.contentH() - 53.0f - 1.0f - clipInset * 2.0f);
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            this.renderContentForState(nvg, mx, my, fromState, false, true, true);
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            nvg.globalAlpha(t);
            this.renderContentForState(nvg, mx, my, toState, true, true, true);
            nvg.popScissor();
            nvg.restore();
            return;
        }
        if (filterOnlyTransition) {
            float bodyClipX = this.owner.contentX() + clipInset;
            float bodyClipY = this.owner.contentY() + 53.0f + 1.0f + 36.0f + 1.0f + clipInset;
            float bodyClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
            float bodyClipH = Math.max(0.0f, this.owner.contentH() - 53.0f - 1.0f - 36.0f - 1.0f - clipInset * 2.0f);
            float headerClipX = this.owner.contentX() + clipInset;
            float headerClipY = this.owner.contentY() + clipInset;
            float headerClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
            float headerClipH = Math.max(0.0f, bodyClipY - this.owner.contentY() - clipInset);
            int direction = Integer.compare(toState.activeFilter(), fromState.activeFilter());
            if (direction == 0) {
                direction = 1;
            }
            float bodySlideX = this.owner.contentW() * (float)direction;
            nvg.save();
            nvg.pushScissor(headerClipX, headerClipY, headerClipW, headerClipH);
            this.owner.renderContentTopBarPanel(nvg, mx, my);
            this.owner.renderFilterBarPanel(nvg, mx, my);
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            nvg.translate(-bodySlideX * strongEaseT, 0.0f);
            this.renderFilterBodyForState(nvg, mx, my, fromState, false);
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            nvg.translate(bodySlideX * (1.0f - strongEaseT), 0.0f);
            this.renderFilterBodyForState(nvg, mx, my, toState, true);
            nvg.popScissor();
            nvg.restore();
            return;
        }
        if (fromModuleDetail || toModuleDetail) {
            float bodyClipX = this.owner.contentX() + clipInset;
            float bodyClipY = this.owner.contentY() + 53.0f + 1.0f + clipInset;
            float bodyClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
            float bodyClipH = Math.max(0.0f, this.owner.contentH() - 53.0f - 1.0f - clipInset * 2.0f);
            float headerClipX = this.owner.contentX() + clipInset;
            float headerClipY = this.owner.contentY() + clipInset;
            float headerClipW = Math.max(0.0f, this.owner.contentW() - clipInset * 2.0f);
            float headerClipH = Math.max(0.0f, bodyClipY - this.owner.contentY() - clipInset);
            int direction = toModuleDetail ? 1 : -1;
            float bodySlideX = this.owner.contentW() * (float)direction;
            nvg.save();
            nvg.pushScissor(headerClipX, headerClipY, headerClipW, headerClipH);
            nvg.translate(bodySlideX * strongEaseT, 0.0f);
            if (fromModuleDetail) {
                this.renderModuleDetailTopBarForState(nvg, mx, my, fromState, false);
            } else {
                this.renderContentTopBarForState(nvg, mx, my, fromState, false);
            }
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(headerClipX, headerClipY, headerClipW, headerClipH);
            nvg.translate(-bodySlideX * (1.0f - strongEaseT), 0.0f);
            if (toModuleDetail) {
                this.renderModuleDetailTopBarForState(nvg, mx, my, toState, true);
            } else {
                this.renderContentTopBarForState(nvg, mx, my, toState, true);
            }
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            nvg.translate(bodySlideX * strongEaseT, 0.0f);
            if (fromModuleDetail) {
                this.renderModuleDetailBodyForState(nvg, mx, my, fromState, false);
            } else {
                this.renderModulesOverviewBodyForState(nvg, mx, my, fromState, false);
            }
            nvg.popScissor();
            nvg.restore();
            nvg.save();
            nvg.pushScissor(bodyClipX, bodyClipY, bodyClipW, bodyClipH);
            nvg.translate(-bodySlideX * (1.0f - strongEaseT), 0.0f);
            if (toModuleDetail) {
                this.renderModuleDetailBodyForState(nvg, mx, my, toState, true);
            } else {
                this.renderModulesOverviewBodyForState(nvg, mx, my, toState, true);
            }
            nvg.popScissor();
            nvg.restore();
            return;
        }
        nvg.save();
        nvg.pushScissor(fullClipX, fullClipY, fullClipW, fullClipH);
        nvg.translate(0.0f, outgoingOffsetY);
        nvg.globalAlpha(1.0f - t);
        this.renderContentForState(nvg, mx, my, fromState, false, false, true);
        nvg.popScissor();
        nvg.restore();
        nvg.save();
        nvg.pushScissor(fullClipX, fullClipY, fullClipW, fullClipH);
        nvg.translate(0.0f, incomingOffsetY);
        nvg.globalAlpha(t);
        this.renderContentForState(nvg, mx, my, toState, true, false, true);
        nvg.popScissor();
        nvg.restore();
    }

    private void renderContentForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive, boolean moduleBodyOnly, boolean suppressNestedScissor) {
        MainGUIContentViewState savedState = this.owner.snapshotContentState();
        int clickCount = this.owner.clickAreaCount();
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.applyContentState(state);
        this.owner.setSuppressNestedContentScissorState(suppressNestedScissor);
        if (!interactive) {
            mx=my=-Float.MAX_VALUE;
            this.owner.clearTransitionOverlaysForPassiveRender();
        }
        if (moduleBodyOnly) {
            this.owner.renderModuleDetailBodyPanel(nvg, mx, my);
        } else {
            this.owner.renderContentPanel(nvg, mx, my);
        }
        if (!interactive) {
            this.owner.trimClickAreas(clickCount);
        }
        this.owner.restoreTransitionRenderState(transientState, interactive, false);
        this.owner.applyContentState(savedState);
    }

    private void renderContentTopBarForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive) {
        MainGUIContentViewState savedState = this.owner.snapshotContentState();
        int clickCount = this.owner.clickAreaCount();
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.applyContentState(state);
        if (!interactive) {
            mx=my=-Float.MAX_VALUE;
            this.owner.clearTransitionOverlaysForPassiveRender();
        }
        this.owner.renderContentTopBarPanel(nvg, mx, my);
        if (!interactive) {
            this.owner.trimClickAreas(clickCount);
        }
        this.owner.restoreTransitionRenderState(transientState, interactive, false);
        this.owner.applyContentState(savedState);
    }

    private void renderModuleDetailTopBarForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive) {
        MainGUIContentViewState savedState = this.owner.snapshotContentState();
        int clickCount = this.owner.clickAreaCount();
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.applyContentState(state);
        if (!interactive) {
            mx=my=-Float.MAX_VALUE;
            this.owner.clearTransitionOverlaysForPassiveRender();
        }
        this.owner.renderModuleTopBarPanel(nvg, mx, my);
        if (!interactive) {
            this.owner.trimClickAreas(clickCount);
        }
        this.owner.restoreTransitionRenderState(transientState, interactive, false);
        this.owner.applyContentState(savedState);
    }

    private void renderModuleDetailBodyForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive) {
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.setActiveScroll(this.owner.getModuleDetailScrollY(), this.owner.getModuleDetailTargetScrollY());
        this.renderContentForState(nvg, mx, my, state, interactive, true, true);
        this.owner.restoreTransitionRenderState(transientState, interactive, true);
    }

    private void renderModulesOverviewBodyForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive) {
        MainGUIContentViewState savedState = this.owner.snapshotContentState();
        int clickCount = this.owner.clickAreaCount();
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.applyContentState(state);
        this.owner.setSuppressNestedContentScissorState(true);
        this.owner.setActiveScroll(this.owner.getModulesOverviewScrollY(), this.owner.getModulesOverviewTargetScrollY());
        if (!interactive) {
            mx=my=-Float.MAX_VALUE;
            this.owner.clearTransitionOverlaysForPassiveRender();
        }
        if (this.owner.searchResultsVisible()) {
            this.owner.renderSearchContentPanel(nvg, mx, my);
        } else {
            this.owner.renderFilterBarPanel(nvg, mx, my);
            float scrollTop = this.owner.contentY() + 53.0f + 1.0f + 36.0f + 1.0f;
            float scrollH = this.owner.contentH() - 53.0f - 1.0f - 36.0f - 1.0f;
            this.owner.renderModuleGridPanel(nvg, mx, my, scrollTop, scrollH);
        }
        if (!interactive) {
            this.owner.trimClickAreas(clickCount);
        }
        this.owner.restoreTransitionRenderState(transientState, interactive, true);
        this.owner.applyContentState(savedState);
    }

    private void renderFilterBodyForState(NVGRenderer nvg, float mx, float my, MainGUIContentViewState state, boolean interactive) {
        MainGUIContentViewState savedState = this.owner.snapshotContentState();
        int clickCount = this.owner.clickAreaCount();
        MainGUITransientRenderState transientState = this.owner.captureTransientRenderState();
        this.owner.applyContentState(state);
        this.owner.setSuppressNestedContentScissorState(true);
        if (!interactive) {
            mx=my=-Float.MAX_VALUE;
            this.owner.clearTransitionOverlaysForPassiveRender();
        }
        if (this.owner.getActiveMainTab() == 0) {
            float scrollTop = this.owner.contentY() + 53.0f + 1.0f + 36.0f + 1.0f;
            float scrollH = this.owner.contentH() - 53.0f - 1.0f - 36.0f - 1.0f;
            this.owner.renderModuleGridPanel(nvg, mx, my, scrollTop, scrollH);
        } else if (this.owner.getActiveMainTab() == 2) {
            this.owner.renderProfileContentPanel(nvg, mx, my);
        } else {
            this.owner.renderFlatContentPanel(nvg, mx, my);
        }
        if (!interactive) {
            this.owner.trimClickAreas(clickCount);
        }
        this.owner.restoreTransitionRenderState(transientState, interactive, false);
        this.owner.applyContentState(savedState);
    }

    private boolean isModuleDetailState(MainGUIContentViewState state) {
        return state.activeMain() == 0 && state.activeSubTab() != null;
    }

    private boolean isCategoryOnlyTransition(MainGUIContentViewState from, MainGUIContentViewState to) {
        return from.activeMain() == 0 && to.activeMain() == 0 && from.activeSubTab() != null && from.activeSubTab() == to.activeSubTab() && from.activeSubtab() == to.activeSubtab() && from.activeFilter() == to.activeFilter() && from.searchQuery().equals(to.searchQuery()) && from.activeCategoryIdx() != to.activeCategoryIdx();
    }

    private boolean isFilterOnlyTransition(MainGUIContentViewState from, MainGUIContentViewState to) {
        return from.activeMain() == to.activeMain() && MainGUI.hasTopFilterBar(from.activeMain()) && from.activeSubTab() == null && to.activeSubTab() == null && from.activeSubtab() == to.activeSubtab() && from.searchQuery().equals(to.searchQuery()) && from.activeCategoryIdx() == to.activeCategoryIdx() && from.activeFilter() != to.activeFilter();
    }
}
