package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

/** Dark tooltip-inspired button that remains safe above vanilla's 200 px width. */
class MotionUIButton extends GuiButton {
    enum Variant { PRIMARY, SECONDARY, QUIET, DANGER }
    private final Variant variant;

    MotionUIButton(int id, int x, int y, int width, int height, String text) {
        this(id, x, y, width, height, text, Variant.SECONDARY);
    }

    MotionUIButton(int id, int x, int y, int width, int height, String text, Variant variant) {
        super(id, x, y, width, height, text);
        this.variant = variant;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

        int state = getHoverState(hovered);
        int normalFill = variant == Variant.PRIMARY ? 0xD032193C : variant == Variant.DANGER ? 0xD036161E
                : variant == Variant.QUIET ? 0x70100818 : MotionUITheme.SURFACE_RAISED;
        int normalEdge = variant == Variant.PRIMARY ? 0xD0A05A30 : variant == Variant.DANGER ? 0xD0903038
                : variant == Variant.QUIET ? MotionUITheme.BORDER_SUBTLE : MotionUITheme.BORDER;
        int fill = !enabled ? 0xB0121018 : state == 2 ? MotionUITheme.SURFACE_SELECTED : normalFill;
        int edge = !enabled ? 0x80403048 : state == 2 ? MotionUITheme.FOCUS : normalEdge;
        drawRect(x, y, x + width, y + height, 0xD0080610);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, edge);
        drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, edge);
        drawRect(x + 1, y + 1, x + 2, y + height - 1, edge);
        drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, edge);
        mouseDragged(mc, mouseX, mouseY);

        FontRenderer font = mc.fontRenderer;
        int color = packedFGColour != 0 ? packedFGColour : !enabled ? 0x777080
                : variant == Variant.PRIMARY ? MotionUITheme.ACCENT : hovered ? 0xFFFFF2C0 : MotionUITheme.TEXT;
        drawCenteredString(font, displayString, x + width / 2, y + (height - 8) / 2, color);
    }
}
