package com.demonicrous.motionui.client;

import net.minecraft.client.gui.Gui;

/** Semantic colors and surfaces for the tooltip-inspired MotionUI theme. */
final class MotionUITheme {
    static final int FRAME = 0xEC0D0712;
    static final int SURFACE = 0xD8140A1D;
    static final int SURFACE_RAISED = 0xE01A0D26;
    static final int SURFACE_SELECTED = 0xE02E1542;
    static final int BORDER = 0xC05A2478;
    static final int BORDER_SUBTLE = 0x70301848;
    static final int ACCENT = 0xFFFFD868;
    static final int FOCUS = 0xFFD07AF0;
    static final int SUCCESS = 0xFF62D890;
    static final int WARNING = 0xFFFFD868;
    static final int DANGER = 0xFFFF7070;
    static final int TEXT = 0xFFE8DFF0;
    static final int TEXT_MUTED = 0xFFAFA4B8;
    static final int TEXT_FAINT = 0xFF82768A;

    private MotionUITheme() {}

    static void surface(Gui gui, MotionUILayout.Rect rect, int fill, int border) {
        if (rect.width < 2 || rect.height < 2) return;
        gui.drawRect(rect.x, rect.y, rect.right(), rect.bottom(), border);
        gui.drawRect(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, fill);
    }

    static void card(Gui gui, MotionUILayout.Rect rect) {
        surface(gui, rect, SURFACE, BORDER_SUBTLE);
    }

    static void card(Gui gui, MotionUILayout.Rect rect, float opacity) {
        surface(gui, rect, withOpacity(SURFACE, opacity), withOpacity(BORDER_SUBTLE, Math.min(1F, opacity + .18F)));
    }

    static void card(Gui gui, MotionUILayout.Rect rect, int accent) {
        card(gui, rect);
        gui.drawRect(rect.x, rect.y, rect.x + 3, rect.bottom(), accent);
    }

    static void separator(Gui gui, int left, int right, int y) {
        gui.drawRect(left, y, right, y + 1, BORDER_SUBTLE);
    }

    static int withOpacity(int color,float opacity){int alpha=color>>>24&255;return Math.max(0,Math.min(255,Math.round(alpha*Math.max(0F,Math.min(1F,opacity)))))<<24|color&0xFFFFFF;}
}
