package com.demonicrous.motionui.client;

import net.minecraft.client.gui.Gui;

/** Scalable panel using the same layered border language as vanilla tooltips. */
final class MotionUITooltipPanel extends Gui {
    private static final int BACKGROUND = 0xE8100010;
    private static final int INNER_EDGE = 0x80300050;
    private static final int BORDER_TOP = 0xFF5000A0;
    private static final int BORDER_BOTTOM = 0xFF280050;

    void drawPanel(int left, int top, int right, int bottom) {
        if (right - left < 4 || bottom - top < 4) return;

        // Tooltip-shaped background: its corners remain transparent instead of
        // producing the heavy rectangular header used by the previous screen.
        drawRect(left + 2, top, right - 2, bottom, BACKGROUND);
        drawRect(left, top + 2, right, bottom - 2, BACKGROUND);
        drawRect(left + 1, top + 1, right - 1, bottom - 1, BACKGROUND);

        drawRect(left + 2, top + 1, right - 2, top + 2, BORDER_TOP);
        drawRect(left + 2, bottom - 2, right - 2, bottom - 1, BORDER_BOTTOM);
        drawGradientRect(left + 1, top + 2, left + 2, bottom - 2, BORDER_TOP, BORDER_BOTTOM);
        drawGradientRect(right - 2, top + 2, right - 1, bottom - 2, BORDER_TOP, BORDER_BOTTOM);

        drawRect(left + 2, top + 2, right - 2, top + 3, INNER_EDGE);
        drawRect(left + 2, bottom - 3, right - 2, bottom - 2, INNER_EDGE);
    }

    /** Compact read-only value field sharing the tooltip visual language. */
    void drawField(int left, int top, int right, int bottom) {
        drawRect(left + 1, top, right - 1, bottom, 0xD0100010);
        drawRect(left, top + 1, right, bottom - 1, 0xD0100010);
        drawRect(left + 1, top + 1, right - 1, top + 2, 0xA05000A0);
        drawRect(left + 1, bottom - 2, right - 1, bottom - 1, 0xA0280050);
        drawGradientRect(left, top + 1, left + 1, bottom - 1, 0xA05000A0, 0xA0280050);
        drawGradientRect(right - 1, top + 1, right, bottom - 1, 0xA05000A0, 0xA0280050);
    }
}
