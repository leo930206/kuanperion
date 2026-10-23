// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

final class MainGUIMouseController {
    private final MainGUI owner;

    MainGUIMouseController(MainGUI owner) {
        this.owner = owner;
    }

    boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        float mx = this.owner.scaleMouseX(click.x());
        float my = this.owner.scaleMouseY(click.y());
        if (this.owner.handleKeybindMouseButtonCapture(click.button())) {
            return true;
        }
        if(this.owner.isSidebarExpandedOverContent(mx,my)) {
            if(click.button()==0)this.owner.tryHandleSidebarNavigationClick(mx,my,Minecraft.getInstance());
            return true;
        }
        if (click.button() == 1) {
            this.owner.handleRightClickEvent(mx, my);
            return true;
        }
        if (click.button() != 0) {
            return true;
        }
        if (this.owner.hasActiveColorOverlay()) {
            if (this.owner.handleColorPickerClickEvent(mx, my)) {
                return true;
            }
            this.owner.commitColor();
        }
        if (this.owner.hasInlineEditorFocus() && !this.owner.isPointInsideActiveEditorState(mx, my)) {
            this.owner.commitText();
        }
        if (this.owner.isOutsidePanel(mx, my)) {
            this.owner.handleOutsidePanelClick();
            return true;
        }
        if (this.owner.tryStartContentScrollbarDrag(mx, my)) {
            return true;
        }
        if (this.owner.tryStartSubtabScrollbarDrag(mx, my)) {
            return true;
        }
        if (this.owner.isSearchButtonHit(mx, my)) {
            this.owner.handleSearchButtonClick();
            return true;
        }
        if (this.owner.hasOpenDropdown()) {
            this.owner.handleDropdownClickEvent(mx, my);
            return true;
        }
        if (this.owner.isPointInsideSearchFieldState(mx, my)) {
            this.owner.handleSearchFieldClick(mx, my);
            return true;
        }
        this.owner.collapseSearchIfNeeded();
        if (this.owner.tryBeginPanelDrag(mx, my)) {
            return true;
        }
        if (this.owner.tryHandleSidebarNavigationClick(mx, my, Minecraft.getInstance())) {
            return true;
        }
        if (this.owner.isSidebarExpandedOverContent(mx, my)) {
            return true;
        }
        if (this.owner.runClickAreas(mx, my)) {
            return true;
        }
        this.owner.handleContentAreaClick(mx, my);
        return true;
    }

    boolean mouseDragged(MouseButtonEvent click, double dX, double dY) {
        float my;
        float mx = this.owner.scaleMouseX(click.x());
        if (this.owner.dragPanel(mx, my = this.owner.scaleMouseY(click.y()))) {
            return true;
        }
        if (this.owner.dragSlider(mx)) {
            return true;
        }
        if (this.owner.dragRangeSlider(mx)) {
            return true;
        }
        if (this.owner.dragActiveColorOverlay(mx, my)) {
            return true;
        }
        if (this.owner.updateActiveTextSelection(mx)) {
            return true;
        }
        if (this.owner.tryBeginActiveTextSelection(click.button(), mx, my)) {
            return true;
        }
        return this.owner.dragScrollbar(my);
    }

    boolean mouseReleased(MouseButtonEvent click) {
        this.owner.finishMouseInteractions();
        return false;
    }

    boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return this.owner.handleMouseScroll(this.owner.scaleMouseX(mx), this.owner.scaleMouseY(my), (float)sy);
    }
}
