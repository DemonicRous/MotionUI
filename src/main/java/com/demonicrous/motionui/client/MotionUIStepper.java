package com.demonicrous.motionui.client;

import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/** Label/value row with symmetric decrement and increment actions. */
final class MotionUIStepper extends Gui {
    private final MotionUILayout.Rect bounds;
    private final String value;
    private final int decrementId, incrementId;
    private final String decrementText, incrementText;

    MotionUIStepper(MotionUILayout.Rect bounds, String value, int decrementId, int incrementId,
            String decrementText, String incrementText) {
        this.bounds = bounds; this.value = value; this.decrementId = decrementId; this.incrementId = incrementId;
        this.decrementText = decrementText; this.incrementText = incrementText;
    }

    void addButtons(List<GuiButton> buttons) {
        int buttonWidth = Math.min(48, Math.max(34, bounds.width / 5));
        buttons.add(new MotionUIButton(decrementId, bounds.x, bounds.y, buttonWidth, bounds.height, decrementText));
        buttons.add(new MotionUIButton(incrementId, bounds.right() - buttonWidth, bounds.y,
                buttonWidth, bounds.height, incrementText));
    }

    void draw(FontRenderer font) {
        int buttonWidth = Math.min(48, Math.max(34, bounds.width / 5));
        MotionUILayout.Rect valueRect = new MotionUILayout.Rect(bounds.x + buttonWidth + MotionUILayout.S4,
                bounds.y, bounds.width - buttonWidth * 2 - MotionUILayout.S8, bounds.height);
        MotionUITheme.surface(this, valueRect, MotionUITheme.SURFACE_RAISED, MotionUITheme.BORDER_SUBTLE);
        drawCenteredString(font, value, valueRect.centerX(), valueRect.y + (valueRect.height - font.FONT_HEIGHT) / 2,
                MotionUITheme.TEXT);
    }
}
