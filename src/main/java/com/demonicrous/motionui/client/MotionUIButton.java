package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Vanilla-looking button assembled from the left cap, a tiled centre and the
 * right cap of widgets.png. Unlike GuiButton 1.12.2, it is safe above 200 px.
 */
class MotionUIButton extends GuiButton {
    private static final int SOURCE_WIDTH = 200;
    private static final int SOURCE_HEIGHT = 20;
    private static final int CAP_WIDTH = 4;

    MotionUIButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

        mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        int state = getHoverState(hovered);
        int textureY = 46 + state * SOURCE_HEIGHT;
        drawSlicedButton(textureY);
        mouseDragged(mc, mouseX, mouseY);

        FontRenderer font = mc.fontRenderer;
        int color = packedFGColour != 0 ? packedFGColour
                : !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
        drawCenteredString(font, displayString, x + width / 2, y + (height - 8) / 2, color);
    }

    private void drawSlicedButton(int textureY) {
        int drawnHeight = Math.min(height, SOURCE_HEIGHT);
        int cap = Math.min(CAP_WIDTH, width / 2);
        drawTexturedModalRect(x, y, 0, textureY, cap, drawnHeight);

        int middleX = x + cap;
        int remaining = width - cap * 2;
        int tileWidth = SOURCE_WIDTH - CAP_WIDTH * 2;
        while (remaining > 0) {
            int part = Math.min(remaining, tileWidth);
            drawTexturedModalRect(middleX, y, CAP_WIDTH, textureY, part, drawnHeight);
            middleX += part;
            remaining -= part;
        }

        drawTexturedModalRect(x + width - cap, y,
                SOURCE_WIDTH - cap, textureY, cap, drawnHeight);
    }
}
