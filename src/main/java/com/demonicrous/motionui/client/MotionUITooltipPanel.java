package com.demonicrous.motionui.client;

import net.minecraft.client.gui.Gui;

/** Scalable panel using the same layered border language as vanilla tooltips. */
final class MotionUITooltipPanel extends Gui {
    private static final int BACKGROUND = MotionUITheme.FRAME;
    private static final int INNER_EDGE = MotionUITheme.BORDER_SUBTLE;
    private static final int BORDER_TOP = 0xFF6826A8;
    private static final int BORDER_BOTTOM = 0xFF341054;

    void drawPanel(int left, int top, int right, int bottom) {
        drawPanel(left,top,right,bottom,1F);
    }

    void drawPanel(int left, int top, int right, int bottom,float opacity) {
        if (right - left < 4 || bottom - top < 4) return;

        int background=MotionUITheme.withOpacity(BACKGROUND,opacity);
        int innerEdge=MotionUITheme.withOpacity(INNER_EDGE,Math.min(1F,opacity+.18F));
        int borderTop=MotionUITheme.withOpacity(BORDER_TOP,Math.min(1F,opacity+.25F));
        int borderBottom=MotionUITheme.withOpacity(BORDER_BOTTOM,Math.min(1F,opacity+.25F));

        // Tooltip-shaped background: its corners remain transparent instead of
        // producing the heavy rectangular header used by the previous screen.
        drawRect(left + 2, top, right - 2, bottom, background);
        drawRect(left, top + 2, right, bottom - 2, background);
        drawRect(left + 1, top + 1, right - 1, bottom - 1, background);

        drawRect(left + 2, top + 1, right - 2, top + 2, borderTop);
        drawRect(left + 2, bottom - 2, right - 2, bottom - 1, borderBottom);
        drawGradientRect(left + 1, top + 2, left + 2, bottom - 2, borderTop, borderBottom);
        drawGradientRect(right - 2, top + 2, right - 1, bottom - 2, borderTop, borderBottom);

        drawRect(left + 2, top + 2, right - 2, top + 3, innerEdge);
        drawRect(left + 2, bottom - 3, right - 2, bottom - 2, innerEdge);
    }

    /** Compact read-only value field sharing the tooltip visual language. */
    void drawField(int left, int top, int right, int bottom) {
        MotionUITheme.surface(this, new MotionUILayout.Rect(left, top, right - left, bottom - top),
                MotionUITheme.SURFACE, MotionUITheme.BORDER);
    }
}
