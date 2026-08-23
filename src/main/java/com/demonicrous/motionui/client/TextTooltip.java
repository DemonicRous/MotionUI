package com.demonicrous.motionui.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;

/** Vanilla-style tooltip for explanatory text, without the item-name line gap. */
final class TextTooltip extends Gui {
    private static final TextTooltip INSTANCE = new TextTooltip();
    private static final int PADDING_X = 7;
    private static final int PADDING_Y = 6;

    private TextTooltip() {}

    static void draw(FontRenderer font, String title, String body, int mouseX, int mouseY,
            int screenWidth, int screenHeight) {
        int wrapWidth = Math.max(100, Math.min(300, screenWidth - 32));
        List<String> lines = new ArrayList<String>();
        lines.addAll(font.listFormattedStringToWidth(title, wrapWidth));
        lines.addAll(font.listFormattedStringToWidth(body, wrapWidth));
        INSTANCE.drawLines(font, lines, mouseX, mouseY, screenWidth, screenHeight);
    }

    private void drawLines(FontRenderer font, List<String> lines, int mouseX, int mouseY,
            int screenWidth, int screenHeight) {
        if (lines.isEmpty()) return;
        int textWidth = 0;
        for (String line : lines) textWidth = Math.max(textWidth, font.getStringWidth(line));
        int textHeight = lines.size() * 10 - 1;
        int left = mouseX + 12;
        int top = mouseY - 12;
        if (left + textWidth + PADDING_X + 4 > screenWidth)
            left = mouseX - 12 - textWidth - PADDING_X * 2;
        if (left - PADDING_X < 4) left = 4 + PADDING_X;
        if (top + textHeight + PADDING_Y + 4 > screenHeight)
            top = screenHeight - textHeight - PADDING_Y - 4;
        if (top - PADDING_Y < 4) top = 4 + PADDING_Y;

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        zLevel = 300F;
        try {
            int background = 0xF0100010;
            int borderTop = 0x505000FF;
            int borderBottom = 0x5028007F;
            int boxLeft = left - PADDING_X, boxRight = left + textWidth + PADDING_X;
            int boxTop = top - PADDING_Y, boxBottom = top + textHeight + PADDING_Y;
            drawGradientRect(boxLeft + 1, boxTop, boxRight - 1, boxBottom, background, background);
            drawGradientRect(boxLeft, boxTop + 1, boxRight, boxBottom - 1, background, background);
            drawGradientRect(boxLeft + 1, boxTop + 1, boxLeft + 2, boxBottom - 1, borderTop, borderBottom);
            drawGradientRect(boxRight - 2, boxTop + 1, boxRight - 1, boxBottom - 1, borderTop, borderBottom);
            drawGradientRect(boxLeft + 1, boxTop + 1, boxRight - 1, boxTop + 2, borderTop, borderTop);
            drawGradientRect(boxLeft + 1, boxBottom - 2, boxRight - 1, boxBottom - 1, borderBottom, borderBottom);
            int y = top;
            for (String line : lines) {
                font.drawStringWithShadow(line, left, y, 0xFFFFFFFF);
                y += 10;
            }
        } finally {
            zLevel = 0F;
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
    }
}
