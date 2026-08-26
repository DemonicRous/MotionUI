package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.util.List;
import net.minecraft.client.gui.GuiButton;

/** Reusable five-action selector for slide direction and optional scale composition. */
final class MotionUIMotionPad {
    private final MotionUILayout.Rect bounds;
    private final int firstId;
    private final MotionUIConfig.AnimationStyle style;
    private final MotionUIConfig.AnimationDirection direction;
    private final boolean inward;

    MotionUIMotionPad(MotionUILayout.Rect bounds, int firstId, MotionUIConfig.AnimationStyle style,
            MotionUIConfig.AnimationDirection direction, boolean inward) {
        this.bounds = bounds; this.firstId = firstId; this.style = style; this.direction = direction;
        this.inward = inward;
    }

    void addButtons(List<GuiButton> buttons) {
        int gap = MotionUILayout.S4;
        int cell = Math.max(20, Math.min(28, (Math.min(bounds.width, bounds.height) - gap * 2) / 3));
        int grid = cell * 3 + gap * 2;
        int left = bounds.centerX() - grid / 2;
        int top = bounds.centerY() - grid / 2;
        buttons.add(direction(firstId, left + cell + gap, top, cell, MotionUIConfig.AnimationDirection.UP));
        buttons.add(direction(firstId + 1, left + (cell + gap) * 2, top + cell + gap, cell,
                MotionUIConfig.AnimationDirection.RIGHT));
        buttons.add(direction(firstId + 2, left + cell + gap, top + (cell + gap) * 2, cell,
                MotionUIConfig.AnimationDirection.DOWN));
        buttons.add(direction(firstId + 3, left, top + cell + gap, cell, MotionUIConfig.AnimationDirection.LEFT));
        buttons.add(new MotionUIDirectionButton(firstId + 4, left + cell + gap, top + cell + gap, cell,
                direction, true, style.hasScale(), false));
    }

    private MotionUIDirectionButton direction(int id, int x, int y, int size,
            MotionUIConfig.AnimationDirection candidate) {
        return new MotionUIDirectionButton(id, x, y, size, candidate, false,
                style.hasSlide() && direction == candidate, inward);
    }
}
