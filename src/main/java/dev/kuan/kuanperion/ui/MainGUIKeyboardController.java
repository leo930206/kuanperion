// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUI;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

final class MainGUIKeyboardController {
    private final MainGUI owner;

    MainGUIKeyboardController(MainGUI owner) {
        this.owner = owner;
    }

    boolean keyPressed(KeyEvent input) {
        boolean shift;
        int key = input.key();
        int modifiers = input.modifiers();
        if (this.owner.handleKeybindCapture(input)) {
            return true;
        }
        if (this.owner.handleSettingsShortcut(input)) return true;
        if (key == 256) {
            return this.owner.handleEscapeKey();
        }
        boolean ctrl = (modifiers & (2 | 8)) != 0; // Ctrl or macOS Command
        boolean bl = shift = (modifiers & 1) != 0;
        if (ctrl && (key == 65 || key == 67 || key == 86) && this.owner.handleInlineControlShortcut(key)) {
            return true;
        }
        if (this.owner.handleSearchEditorKey(key, ctrl, shift)) {
            return true;
        }
        if (this.owner.handleProfileNameKey(key)) {
            return true;
        }
        if (this.owner.handleInlineEditorKey(key, ctrl, shift)) {
            return true;
        }
        return this.owner.handleOverlayHexKey(key);
    }

    boolean charTyped(CharacterEvent input) {
        if (this.owner.isAnyKeybindCaptureActive()) {
            return true;
        }
        char ch = (char)input.codepoint();
        if (this.owner.handleInlineCharTyped(ch)) {
            return true;
        }
        return this.owner.handleOverlayHexChar(ch);
    }
}
