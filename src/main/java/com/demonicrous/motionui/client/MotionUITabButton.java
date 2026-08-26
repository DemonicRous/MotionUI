package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/** Flat navigation tab: selected state is expressed by a content underline. */
final class MotionUITabButton extends MotionUIButton {
    private final boolean selected;

    MotionUITabButton(int id, int x, int y, int width, String text, boolean selected) {
        super(id, x, y, width, 20, text);
        this.selected = selected;
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return selected ? 2 : super.getHoverState(mouseOver);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int fill = selected ? MotionUITheme.SURFACE_SELECTED : hovered ? MotionUITheme.SURFACE_RAISED : 0x40100818;
        drawRect(x, y, x + width, y + height - 2, fill);
        drawRect(x, y + height - 2, x + width, y + height, selected ? MotionUITheme.ACCENT
                : hovered ? MotionUITheme.FOCUS : MotionUITheme.BORDER_SUBTLE);
        if (selected) {
            drawRect(x, y, x + 1, y + height - 2, 0x805A2678);
            drawRect(x + width - 1, y, x + width, y + height - 2, 0x805A2678);
        }
        mouseDragged(mc, mouseX, mouseY);
        FontRenderer font = mc.fontRenderer;
        int color = !enabled ? 0x777080 : selected ? MotionUITheme.ACCENT
                : hovered ? 0xFFF6E8FF : MotionUITheme.TEXT;
        drawCenteredString(font, displayString, x + width / 2, y + (height - 8) / 2, color);
    }
}
