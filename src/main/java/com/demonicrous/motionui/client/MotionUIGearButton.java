package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Pixel-grid adaptation of the supplied outlined SVG gear. */
final class MotionUIGearButton extends MotionUIButton {
    private static final ResourceLocation GEAR = new ResourceLocation("motionui", "textures/gui/icons/gear.png");
    MotionUIGearButton(int id, int x, int y, int size) {
        super(id, x, y, size, size, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        int color = !enabled ? 0xFF706878 : hovered ? 0xFFFFFFB0 : 0xFFE8DFF0;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.enableBlend();
        GlStateManager.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F,
                (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        mc.getTextureManager().bindTexture(GEAR);
        drawScaledCustomSizeModalRect(x + (width - 16) / 2, y + (height - 16) / 2,
                0F, 0F, 32, 32, 16, 16, 32F, 32F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (!blend) GlStateManager.disableBlend();
    }
}
