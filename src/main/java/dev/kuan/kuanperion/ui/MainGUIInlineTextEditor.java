// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.TextSetting;
import java.util.List;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

final class MainGUIInlineTextEditor {
    private final MainGUI owner;

    MainGUIInlineTextEditor(MainGUI owner) {
        this.owner = owner;
    }

    boolean hasActiveInlineEditor() {
        return this.owner.activeSliderField != null || this.owner.activeText != null || this.owner.activeList != null || this.owner.activePosField != null;
    }

    boolean isSearchEditorActive() {
        return this.owner.searchMode && !this.owner.profileNameFocus && !this.owner.profileRenameFocus && this.owner.activeSliderField == null && this.owner.activeText == null && this.owner.activeList == null && this.owner.activePosField == null && !this.owner.cpHexFocus;
    }

    void syncSearchQueryFromEditor() {
        this.owner.searchQuery = this.owner.textBuf.toString();
        this.owner.searchScrollY = 0.0f;
        this.owner.searchTargetScrollY = 0.0f;
        this.normalizeTextCursorState();
    }

    void focusSearchField() {
        if (!this.owner.searchMode) {
            this.owner.searchMode = true;
        }
        this.owner.textBuf = new StringBuilder(this.owner.searchQuery);
        this.owner.textCursor = this.owner.textBuf.length();
        this.clearTextSelection();
        this.normalizeTextCursorState();
    }

    boolean isPointInsideSearchField(float mx, float my) {
        return mx >= this.owner.searchBoxX && mx <= this.owner.searchBoxX + this.owner.searchBoxW && my >= this.owner.searchBoxY && my <= this.owner.searchBoxY + this.owner.searchBoxH;
    }

    void updateSearchSelectionFromMouse(float mx) {
        String value = this.owner.textBuf.toString();
        float textPad = 6.0f;
        float fontSize = 11.0f;
        float visibleTextW = this.owner.searchBoxW - 30.0f - textPad * 2.0f;
        float textOffset = this.owner.inlineTextScrollOffsetApprox(value, this.owner.textCursor, fontSize, visibleTextW);
        float startX = this.owner.searchBoxX + 30.0f + textPad - textOffset;
        this.owner.textCursor = MainGUIInlineTextEditor.cursorFromInlineText(value, fontSize, startX, mx);
    }

    boolean hasTextSelection() {
        if (this.owner.textSelectionAnchor < 0) {
            return false;
        }
        int len = this.owner.textBuf.length();
        int cursor = Math.max(0, Math.min(this.owner.textCursor, len));
        int anchor = Math.max(0, Math.min(this.owner.textSelectionAnchor, len));
        return anchor != cursor;
    }

    int textSelectionStart() {
        int len = this.owner.textBuf.length();
        int cursor = Math.max(0, Math.min(this.owner.textCursor, len));
        int anchor = this.owner.textSelectionAnchor < 0 ? cursor : Math.max(0, Math.min(this.owner.textSelectionAnchor, len));
        return Math.min(anchor, cursor);
    }

    int textSelectionEnd() {
        int len = this.owner.textBuf.length();
        int cursor = Math.max(0, Math.min(this.owner.textCursor, len));
        int anchor = this.owner.textSelectionAnchor < 0 ? cursor : Math.max(0, Math.min(this.owner.textSelectionAnchor, len));
        return Math.max(anchor, cursor);
    }

    void clearTextSelection() {
        this.owner.textSelectionAnchor = -1;
        this.owner.textSelectionDragging = false;
        this.owner.textSelectionPendingDrag = false;
        this.owner.textSelectionPressCursor = -1;
    }

    void normalizeTextCursorState() {
        int len = this.owner.textBuf.length();
        this.owner.textCursor = Math.max(0, Math.min(this.owner.textCursor, len));
        if (this.owner.textSelectionAnchor >= 0) {
            this.owner.textSelectionAnchor = Math.max(0, Math.min(this.owner.textSelectionAnchor, len));
            if (this.owner.textSelectionAnchor == this.owner.textCursor) {
                this.clearTextSelection();
            }
        }
    }

    String safeSubstring(String value, int start, int end) {
        if (value == null) {
            return "";
        }
        int len = value.length();
        int safeStart = Math.max(0, Math.min(start, len));
        int safeEnd = Math.max(0, Math.min(end, len));
        if (safeEnd < safeStart) {
            int swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        if (safeStart == safeEnd) {
            return "";
        }
        return value.substring(safeStart, safeEnd);
    }

    void setTextCursor(int cursor, boolean extendSelection) {
        int clamped = Math.max(0, Math.min(cursor, this.owner.textBuf.length()));
        if (extendSelection) {
            if (!this.hasTextSelection()) {
                this.owner.textSelectionAnchor = this.owner.textCursor;
            }
        } else {
            this.clearTextSelection();
        }
        this.owner.textCursor = clamped;
        if (!extendSelection && this.owner.textSelectionAnchor >= 0 && this.owner.textSelectionAnchor == this.owner.textCursor) {
            this.clearTextSelection();
        }
    }

    void selectAllText() {
        this.owner.textSelectionAnchor = 0;
        this.owner.textCursor = this.owner.textBuf.length();
    }

    boolean deleteSelectedText() {
        if (!this.hasTextSelection()) {
            return false;
        }
        int start = this.textSelectionStart();
        int end = this.textSelectionEnd();
        this.owner.textBuf.delete(start, end);
        this.owner.textCursor = start;
        this.clearTextSelection();
        this.normalizeTextCursorState();
        return true;
    }

    void insertTextAtCursor(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (this.deleteSelectedText()) {
            this.owner.textBuf.insert(this.owner.textCursor, text);
            this.owner.textCursor += text.length();
            return;
        }
        this.owner.textBuf.insert(this.owner.textCursor, text);
        this.owner.textCursor += text.length();
        this.normalizeTextCursorState();
    }

    void updateInlineSelectionFromMouse(float mx) {
        if (!this.hasActiveInlineEditor()) {
            return;
        }
        if (this.owner.activeSliderField != null) {
            String value = this.owner.textBuf.toString();
            float fieldTextSize = 12.5f;
            float drawX = this.owner.activeEditorX + (this.owner.activeEditorW - (float)value.length() * fieldTextSize * 0.52f) / 2.0f;
            this.owner.textCursor = MainGUIInlineTextEditor.cursorFromInlineText(value, fieldTextSize, drawX, mx);
            return;
        }
        if (this.owner.activePosField != null || this.owner.activeText != null || this.owner.activeList != null) {
            String value = this.owner.textBuf.toString();
            float textPad = 6.0f;
            float fieldTextSize = 12.5f;
            float visibleTextW = this.owner.activeEditorW - textPad * 2.0f;
            float textOffset = this.owner.inlineTextScrollOffsetApprox(value, this.owner.textCursor, fieldTextSize, visibleTextW);
            float startX = this.owner.activeEditorX + textPad - textOffset;
            this.owner.textCursor = MainGUIInlineTextEditor.cursorFromInlineText(value, fieldTextSize, startX, mx);
        }
    }

    void beginInlineCaretPress(float mx, float my) {
        this.owner.textSelectionPendingDrag = true;
        this.owner.textSelectionDragging = false;
        this.owner.textSelectionPressCursor = this.owner.textCursor;
        this.owner.textSelectionPressX = mx;
        this.owner.textSelectionPressY = my;
    }

    boolean shouldBeginInlineSelectionDrag(float mx, float my) {
        if (!this.owner.textSelectionPendingDrag || !this.hasActiveInlineEditor() && !this.isSearchEditorActive()) {
            return false;
        }
        float dx = mx - this.owner.textSelectionPressX;
        float dy = my - this.owner.textSelectionPressY;
        return dx * dx + dy * dy >= 9.0f;
    }

    void setActiveEditorBounds(float x, float y, float w, float h) {
        this.owner.activeEditorBoundsSet = true;
        this.owner.activeEditorX = x;
        this.owner.activeEditorY = y;
        this.owner.activeEditorW = w;
        this.owner.activeEditorH = h;
    }

    boolean isPointInsideActiveEditor(float mx, float my) {
        return this.owner.activeEditorBoundsSet && mx >= this.owner.activeEditorX && mx <= this.owner.activeEditorX + this.owner.activeEditorW && my >= this.owner.activeEditorY && my <= this.owner.activeEditorY + this.owner.activeEditorH;
    }

    float textSettingHeight(TextSetting setting) {
        return 26.0f + this.textFieldHeight(setting);
    }

    float textFieldHeight(TextSetting setting) {
        if (!setting.isMultiline()) {
            return 20.0f;
        }
        return 12.0f + (float)setting.getVisibleLines() * 14.0f - 2.0f;
    }

    int getMultilineViewportStart(TextSetting setting, String text, int cursor) {
        int totalLines = MainGUIInlineTextEditor.splitLines(text).length;
        int maxStart = Math.max(0, totalLines - setting.getVisibleLines());
        int cursorLine = MainGUIInlineTextEditor.getCursorLine(text, cursor);
        return Math.max(0, Math.min(maxStart, cursorLine - setting.getVisibleLines() + 1));
    }

    boolean handleControlShortcut(int key) {
        KeyboardHandler keyboard = Minecraft.getInstance().keyboardHandler;
        if (key == 65) {
            if (this.hasActiveInlineEditor() || this.isSearchEditorActive()) {
                this.selectAllText();
                if (this.isSearchEditorActive()) {
                    this.syncSearchQueryFromEditor();
                }
                return true;
            }
            return false;
        }
        if (key == 67) {
            if (this.owner.hoveredColor != null) {
                keyboard.setClipboard(String.format("%08X", this.owner.hoveredColor.getValue()));
                return true;
            }
            if ((this.hasActiveInlineEditor() || this.isSearchEditorActive()) && this.hasTextSelection()) {
                keyboard.setClipboard(this.owner.textBuf.substring(this.textSelectionStart(), this.textSelectionEnd()));
                return true;
            }
            if (this.owner.activeText != null || this.owner.activeList != null || this.owner.activePosField != null || this.owner.activeSliderField != null || this.isSearchEditorActive()) {
                keyboard.setClipboard(this.owner.textBuf.toString());
                return true;
            }
            return false;
        }
        if (key != 86) {
            return false;
        }
        String clip = keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) {
            return false;
        }
        if (this.owner.hoveredColor != null) {
            try {
                String hex = clip.trim().replaceFirst("^#", "");
                int argb = hex.length() == 8 ? (int)Long.parseLong(hex, 16) : (int)(0xFF000000L | Long.parseLong(hex, 16));
                this.owner.hoveredColor.setValue(argb);
                if (this.owner.activeColor == this.owner.hoveredColor) {
                    this.owner.argbToHsv(argb);
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            return true;
        }
        if (this.owner.activeSliderField != null || this.owner.activeText != null || this.owner.activeList != null || this.owner.activePosField != null) {
            this.insertTextAtCursor(clip);
            return true;
        }
        if (this.isSearchEditorActive()) {
            this.insertTextAtCursor(clip);
            this.syncSearchQueryFromEditor();
            return true;
        }
        if (this.owner.profileNameFocus) {
            this.owner.profileNameInput = this.owner.profileNameInput + clip;
            return true;
        }
        if (this.owner.profileRenameFocus) {
            this.owner.profileRenameInput = this.owner.profileRenameInput + clip;
            return true;
        }
        return false;
    }

    boolean handleSearchEditorKeyPressed(int key, boolean ctrl, boolean shift) {
        if (!this.isSearchEditorActive()) {
            return false;
        }
        boolean hasSelection = this.hasTextSelection();
        switch (key) {
            case 259: {
                if (this.deleteSelectedText() || this.owner.textCursor <= 0) break;
                this.owner.textBuf.deleteCharAt(--this.owner.textCursor);
                break;
            }
            case 261: {
                if (this.deleteSelectedText() || this.owner.textCursor >= this.owner.textBuf.length()) break;
                this.owner.textBuf.deleteCharAt(this.owner.textCursor);
                break;
            }
            case 263: {
                int nextCursor;
                int n = nextCursor = ctrl ? MainGUIInlineTextEditor.moveCursorWord(this.owner.textBuf.toString(), this.owner.textCursor, -1) : Math.max(0, this.owner.textCursor - 1);
                if (shift) {
                    this.setTextCursor(nextCursor, true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(ctrl ? nextCursor : this.textSelectionStart(), false);
                    break;
                }
                this.setTextCursor(nextCursor, false);
                break;
            }
            case 262: {
                int nextCursor;
                int n = nextCursor = ctrl ? MainGUIInlineTextEditor.moveCursorWord(this.owner.textBuf.toString(), this.owner.textCursor, 1) : Math.min(this.owner.textBuf.length(), this.owner.textCursor + 1);
                if (shift) {
                    this.setTextCursor(nextCursor, true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(ctrl ? nextCursor : this.textSelectionEnd(), false);
                    break;
                }
                this.setTextCursor(nextCursor, false);
                break;
            }
            case 268: {
                this.setTextCursor(0, shift);
                break;
            }
            case 269: {
                this.setTextCursor(this.owner.textBuf.length(), shift);
                break;
            }
            default: {
                return false;
            }
        }
        this.syncSearchQueryFromEditor();
        return true;
    }

    boolean handleProfileNameKeyPressed(int key) {
        if (this.owner.profileRenameFocus) {
            switch (key) {
                case 259: {
                    if (this.owner.profileRenameInput.isEmpty()) break;
                    this.owner.profileRenameInput = this.owner.profileRenameInput.substring(0, this.owner.profileRenameInput.length() - 1);
                    break;
                }
                case 257:
                case 335: {
                    this.owner.commitProfileRename();
                    break;
                }
                case 261: {
                    this.owner.profileRenameInput = "";
                    break;
                }
                default: {
                    return false;
                }
            }
            return true;
        }
        if (!this.owner.profileNameFocus) {
            return false;
        }
        switch (key) {
            case 259: {
                if (this.owner.profileNameInput.isEmpty()) break;
                this.owner.profileNameInput = this.owner.profileNameInput.substring(0, this.owner.profileNameInput.length() - 1);
                break;
            }
            case 257:
            case 335: {
                this.owner.profileNameFocus = false;
                break;
            }
            case 261: {
                this.owner.profileNameInput = "";
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    boolean handleInlineEditorKeyPressed(int key, boolean ctrl, boolean shift) {
        if (!this.hasActiveInlineEditor()) {
            return false;
        }
        boolean multilineText = this.owner.activeText != null && this.owner.activeText.isMultiline();
        boolean hasSelection = this.hasTextSelection();
        switch (key) {
            case 259: {
                if (this.deleteSelectedText() || this.owner.textCursor <= 0) break;
                this.owner.textBuf.deleteCharAt(--this.owner.textCursor);
                break;
            }
            case 261: {
                if (this.deleteSelectedText() || this.owner.textCursor >= this.owner.textBuf.length()) break;
                this.owner.textBuf.deleteCharAt(this.owner.textCursor);
                break;
            }
            case 263: {
                int nextCursor;
                int n = nextCursor = ctrl ? MainGUIInlineTextEditor.moveCursorWord(this.owner.textBuf.toString(), this.owner.textCursor, -1) : Math.max(0, this.owner.textCursor - 1);
                if (shift) {
                    this.setTextCursor(nextCursor, true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(ctrl ? nextCursor : this.textSelectionStart(), false);
                    break;
                }
                this.setTextCursor(nextCursor, false);
                break;
            }
            case 262: {
                int nextCursor;
                int n = nextCursor = ctrl ? MainGUIInlineTextEditor.moveCursorWord(this.owner.textBuf.toString(), this.owner.textCursor, 1) : Math.min(this.owner.textBuf.length(), this.owner.textCursor + 1);
                if (shift) {
                    this.setTextCursor(nextCursor, true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(ctrl ? nextCursor : this.textSelectionEnd(), false);
                    break;
                }
                this.setTextCursor(nextCursor, false);
                break;
            }
            case 265: {
                if (!multilineText) {
                    return false;
                }
                if (shift) {
                    this.setTextCursor(MainGUIInlineTextEditor.moveCursorVertical(this.owner.textBuf.toString(), this.owner.textCursor, -1), true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(this.textSelectionStart(), false);
                    break;
                }
                this.setTextCursor(MainGUIInlineTextEditor.moveCursorVertical(this.owner.textBuf.toString(), this.owner.textCursor, -1), false);
                break;
            }
            case 264: {
                if (!multilineText) {
                    return false;
                }
                if (shift) {
                    this.setTextCursor(MainGUIInlineTextEditor.moveCursorVertical(this.owner.textBuf.toString(), this.owner.textCursor, 1), true);
                    break;
                }
                if (hasSelection) {
                    this.setTextCursor(this.textSelectionEnd(), false);
                    break;
                }
                this.setTextCursor(MainGUIInlineTextEditor.moveCursorVertical(this.owner.textBuf.toString(), this.owner.textCursor, 1), false);
                break;
            }
            case 268: {
                int nextCursor = multilineText ? MainGUIInlineTextEditor.getLineStart(this.owner.textBuf.toString(), this.owner.textCursor) : 0;
                this.setTextCursor(nextCursor, shift);
                break;
            }
            case 269: {
                int nextCursor = multilineText ? MainGUIInlineTextEditor.getLineEnd(this.owner.textBuf.toString(), this.owner.textCursor) : this.owner.textBuf.length();
                this.setTextCursor(nextCursor, shift);
                break;
            }
            case 257:
            case 335: {
                if (multilineText) {
                    this.insertTextAtCursor("\n");
                    break;
                }
                this.commitText();
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    boolean handleCharTyped(char ch) {
        if (this.isSearchEditorActive()) {
            this.insertTextAtCursor(String.valueOf(ch));
            this.syncSearchQueryFromEditor();
            return true;
        }
        if (this.owner.profileNameFocus) {
            this.owner.profileNameInput = this.owner.profileNameInput + ch;
            return true;
        }
        if (this.owner.profileRenameFocus) {
            this.owner.profileRenameInput = this.owner.profileRenameInput + ch;
            return true;
        }
        if (this.hasActiveInlineEditor()) {
            this.insertTextAtCursor(String.valueOf(ch));
            return true;
        }
        return false;
    }

    void commitText() {
        this.owner.profileNameFocus = false;
        if (this.owner.profileRenameFocus) {
            this.owner.commitProfileRename();
        }
        this.clearTextSelection();
        if (this.owner.activeSliderField != null) {
            try {
                float parsed = Float.parseFloat(this.owner.textBuf.toString().trim());
                this.owner.activeSliderField.setValue(parsed);
                this.owner.textBuf = new StringBuilder(this.owner.fmtSliderInput(this.owner.activeSliderField));
                this.owner.textCursor = this.owner.textBuf.length();
            }
            catch (NumberFormatException parsed) {
                // empty catch block
            }
            this.owner.activeSliderField = null;
        }
        if (this.owner.activeText != null) {
            this.owner.activeText.setValue(this.owner.textBuf.toString());
            this.owner.activeText = null;
        }
        if (this.owner.activeList != null) {
            List<String> values = this.owner.activeList.getValues();
            String value = this.owner.textBuf.toString().trim();
            if (this.owner.activeListIndex >= 0 && this.owner.activeListIndex < values.size()) {
                if (value.isEmpty()) {
                    values.remove(this.owner.activeListIndex);
                } else {
                    values.set(this.owner.activeListIndex, value);
                }
                this.owner.activeList.setValues(values);
            }
            this.owner.activeList = null;
            this.owner.activeListIndex = -1;
        }
        if (this.owner.activePosField != null) {
            try {
                double value = Double.parseDouble(this.owner.textBuf.toString());
                switch (this.owner.activePosIdx) {
                    case 0: {
                        this.owner.activePosField.setX(value);
                        break;
                    }
                    case 1: {
                        this.owner.activePosField.setY(value);
                        break;
                    }
                    case 2: {
                        this.owner.activePosField.setZ(value);
                        break;
                    }
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            this.owner.activePosField = null;
        }
        this.normalizeTextCursorState();
    }

    static boolean isWordChar(char c) {
        return !Character.isWhitespace(c);
    }

    static int moveCursorWord(String text, int cursor, int direction) {
        int pos = text == null ? 0 : Math.max(0, Math.min(cursor, text.length()));
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (direction < 0) {
            if (pos == 0) {
                return 0;
            }
            --pos;
            while (pos > 0 && !MainGUIInlineTextEditor.isWordChar(text.charAt(pos))) {
                --pos;
            }
            while (pos > 0 && MainGUIInlineTextEditor.isWordChar(text.charAt(pos - 1))) {
                --pos;
            }
            return pos;
        }
        for (pos = Math.max(0, Math.min(cursor, text.length())); pos < text.length() && MainGUIInlineTextEditor.isWordChar(text.charAt(pos)); ++pos) {
        }
        while (pos < text.length() && !MainGUIInlineTextEditor.isWordChar(text.charAt(pos))) {
            ++pos;
        }
        return pos;
    }

    static int cursorFromInlineText(String text, float fontSize, float startX, float mx) {
        float right;
        float left;
        float avgCharWidth;
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float relX = mx - startX;
        if (relX <= 0.0f) {
            return 0;
        }
        int len = text.length();
        float totalWidth = (float)len * (avgCharWidth = fontSize * 0.52f);
        if (relX >= totalWidth) {
            return len;
        }
        int lo = 0;
        int hi = len;
        while (lo < hi) {
            int mid = lo + hi >>> 1;
            float width = (float)mid * avgCharWidth;
            if (width < relX) {
                lo = mid + 1;
                continue;
            }
            hi = mid;
        }
        int idx = Math.max(0, Math.min(len, lo));
        if (idx > 0 && idx < len && relX - (left = (float)(idx - 1) * avgCharWidth) < (right = (float)idx * avgCharWidth) - relX) {
            --idx;
        }
        return Math.max(0, Math.min(len, idx));
    }

    static String[] splitLines(String text) {
        return text.split("\n", -1);
    }

    static int getLineStart(String text, int cursor) {
        int pos;
        for (pos = Math.max(0, Math.min(cursor, text.length())); pos > 0 && text.charAt(pos - 1) != '\n'; --pos) {
        }
        return pos;
    }

    static int getLineEnd(String text, int cursor) {
        int pos;
        for (pos = Math.max(0, Math.min(cursor, text.length())); pos < text.length() && text.charAt(pos) != '\n'; ++pos) {
        }
        return pos;
    }

    static int getCursorLine(String text, int cursor) {
        int pos = Math.max(0, Math.min(cursor, text.length()));
        int line = 0;
        for (int i = 0; i < pos; ++i) {
            if (text.charAt(i) != '\n') continue;
            ++line;
        }
        return line;
    }

    static int getCursorColumn(String text, int cursor) {
        return Math.max(0, Math.min(cursor, text.length()) - MainGUIInlineTextEditor.getLineStart(text, cursor));
    }

    static int moveCursorVertical(String text, int cursor, int delta) {
        int currentStart = MainGUIInlineTextEditor.getLineStart(text, cursor);
        int currentCol = cursor - currentStart;
        if (delta < 0) {
            if (currentStart == 0) {
                return cursor;
            }
            int previousEnd = currentStart - 1;
            int previousStart = MainGUIInlineTextEditor.getLineStart(text, previousEnd);
            return Math.min(previousStart + currentCol, previousEnd);
        }
        int currentEnd = MainGUIInlineTextEditor.getLineEnd(text, cursor);
        if (currentEnd >= text.length()) {
            return cursor;
        }
        int nextStart = currentEnd + 1;
        int nextEnd = MainGUIInlineTextEditor.getLineEnd(text, nextStart);
        return Math.min(nextStart + currentCol, nextEnd);
    }
}
