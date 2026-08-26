package com.demonicrous.motionui.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;

/** OptiFine-safe single-line field independent from its incompatible GuiTextField replacement. */
final class MotionUITextField extends Gui {
    private final MotionUILayout.Rect bounds;
    private final FontRenderer font;
    private final String placeholder;
    private String text = "";
    private int maxLength = 120;
    private int cursor;
    private int anchor;
    private int scrollOffset;
    private int cursorCounter;
    private boolean focused;

    MotionUITextField(int ignoredId, FontRenderer font, MotionUILayout.Rect bounds, String placeholder) {
        this.bounds = bounds;
        this.font = font;
        this.placeholder = placeholder;
    }

    void setMaxStringLength(int value) {
        maxLength = Math.max(0, value);
        if (text.length() > maxLength) setText(text.substring(0, maxLength));
    }

    void setText(String value) {
        text = value == null ? "" : ChatAllowedCharacters.filterAllowedCharacters(value);
        if (text.length() > maxLength) text = text.substring(0, maxLength);
        cursor = anchor = text.length();
        ensureCursorVisible();
    }

    String getText() { return text; }
    boolean contains(int x, int y) { return bounds.contains(x, y); }
    void setFocused(boolean value) { focused = value; }
    boolean isFocused() { return focused; }
    void updateCursorCounter() { cursorCounter++; }

    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        focused = bounds.contains(mouseX, mouseY);
        if (!focused || mouseButton != 0) return false;
        String visible = visibleText();
        int local = Math.max(0, mouseX - textX());
        cursor = Math.min(text.length(), scrollOffset + font.trimStringToWidth(visible, local).length());
        anchor = cursor;
        ensureCursorVisible();
        return true;
    }

    boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (!focused) return false;
        boolean ctrl = GuiScreen.isCtrlKeyDown();
        boolean shift = GuiScreen.isShiftKeyDown();
        if (ctrl && keyCode == 30) { cursor = text.length(); anchor = 0; return true; }
        if (ctrl && keyCode == 46) { GuiScreen.setClipboardString(selectedText()); return true; }
        if (ctrl && keyCode == 45) {
            GuiScreen.setClipboardString(selectedText()); replaceSelection(""); return true;
        }
        if (ctrl && keyCode == 47) {
            replaceSelection(ChatAllowedCharacters.filterAllowedCharacters(GuiScreen.getClipboardString()));
            return true;
        }
        if (keyCode == 14) {
            if (hasSelection()) replaceSelection("");
            else if (cursor > 0) {
                text = text.substring(0, cursor - 1) + text.substring(cursor);
                cursor--; anchor = cursor;
            }
            ensureCursorVisible(); return true;
        }
        if (keyCode == 211) {
            if (hasSelection()) replaceSelection("");
            else if (cursor < text.length()) text = text.substring(0, cursor) + text.substring(cursor + 1);
            ensureCursorVisible(); return true;
        }
        if (keyCode == 203) { moveCursor(cursor - 1, shift); return true; }
        if (keyCode == 205) { moveCursor(cursor + 1, shift); return true; }
        if (keyCode == 199) { moveCursor(0, shift); return true; }
        if (keyCode == 207) { moveCursor(text.length(), shift); return true; }
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            replaceSelection(String.valueOf(typedChar)); return true;
        }
        return false;
    }

    void drawTextBox() {
        MotionUITheme.surface(this, bounds, MotionUITheme.SURFACE_RAISED,
                focused ? MotionUITheme.ACCENT : MotionUITheme.BORDER);
        int baseline = bounds.y + (bounds.height - 8) / 2;
        if (text.isEmpty() && !focused) {
            font.drawStringWithShadow(placeholder, textX(), baseline, MotionUITheme.TEXT_FAINT);
            return;
        }
        String visible = visibleText();
        font.drawStringWithShadow(visible, textX(), baseline, MotionUITheme.TEXT);
        int visibleEnd = scrollOffset + visible.length();
        if (hasSelection()) {
            int from = Math.max(scrollOffset, Math.min(cursor, anchor));
            int to = Math.min(visibleEnd, Math.max(cursor, anchor));
            if (from < to) {
                int left = textX() + font.getStringWidth(text.substring(scrollOffset, from));
                int right = textX() + font.getStringWidth(text.substring(scrollOffset, to));
                drawRect(left, baseline - 1, right, baseline + font.FONT_HEIGHT, 0x806A42A0);
                font.drawStringWithShadow(text.substring(from, to), left, baseline, MotionUITheme.TEXT);
            }
        }
        if (focused && cursorCounter / 6 % 2 == 0 && cursor >= scrollOffset && cursor <= visibleEnd) {
            int x = textX() + font.getStringWidth(text.substring(scrollOffset, cursor));
            drawRect(x, baseline - 1, x + 1, baseline + font.FONT_HEIGHT + 1, MotionUITheme.TEXT);
        }
    }

    private void moveCursor(int value, boolean selecting) {
        cursor = Math.max(0, Math.min(text.length(), value));
        if (!selecting) anchor = cursor;
        ensureCursorVisible();
    }

    private boolean hasSelection() { return cursor != anchor; }

    private String selectedText() {
        int from = Math.min(cursor, anchor), to = Math.max(cursor, anchor);
        return text.substring(from, to);
    }

    private void replaceSelection(String insertion) {
        int from = Math.min(cursor, anchor), to = Math.max(cursor, anchor);
        int capacity = Math.max(0, maxLength - (text.length() - (to - from)));
        String value = insertion.length() > capacity ? insertion.substring(0, capacity) : insertion;
        text = text.substring(0, from) + value + text.substring(to);
        cursor = anchor = from + value.length();
        ensureCursorVisible();
    }

    private int textX() { return bounds.x + 7; }
    private int availableWidth() { return Math.max(1, bounds.width - 14); }

    private void ensureCursorVisible() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, text.length()));
        if (cursor < scrollOffset) scrollOffset = cursor;
        while (scrollOffset < cursor
                && font.getStringWidth(text.substring(scrollOffset, cursor)) > availableWidth()) scrollOffset++;
        while (scrollOffset > 0
                && font.getStringWidth(text.substring(scrollOffset - 1, cursor)) <= availableWidth()) scrollOffset--;
    }

    private String visibleText() {
        return font.trimStringToWidth(text.substring(Math.min(scrollOffset, text.length())), availableWidth());
    }
}
