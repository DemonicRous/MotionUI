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
        if (left + textWidth + 4 > screenWidth) left = mouseX - 16 - textWidth;
        if (left < 4) left = 4;
        if (top + textHeight + 8 > screenHeight) top = screenHeight - textHeight - 8;
        if (top < 4) top = 4;

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        zLevel = 300F;
        try {
            int background = 0xF0100010;
            int borderTop = 0x505000FF;
            int borderBottom = 0x5028007F;
            drawGradientRect(left - 3, top - 4, left + textWidth + 3, top - 3, background, background);
            drawGradientRect(left - 3, top + textHeight + 3, left + textWidth + 3, top + textHeight + 4, background, background);
            drawGradientRect(left - 3, top - 3, left + textWidth + 3, top + textHeight + 3, background, background);
            drawGradientRect(left - 4, top - 3, left - 3, top + textHeight + 3, background, background);
            drawGradientRect(left + textWidth + 3, top - 3, left + textWidth + 4, top + textHeight + 3, background, background);
            drawGradientRect(left - 3, top - 2, left - 2, top + textHeight + 2, borderTop, borderBottom);
            drawGradientRect(left + textWidth + 2, top - 2, left + textWidth + 3, top + textHeight + 2, borderTop, borderBottom);
            drawGradientRect(left - 3, top - 3, left + textWidth + 3, top - 2, borderTop, borderTop);
            drawGradientRect(left - 3, top + textHeight + 2, left + textWidth + 3, top + textHeight + 3, borderBottom, borderBottom);
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
