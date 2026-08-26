package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Configured-only filter using the supplied star silhouette in outlined and filled states. */
final class MotionUIStarButton extends MotionUIButton {
    private static final ResourceLocation STAR = new ResourceLocation("motionui", "textures/gui/icons/star.png");
    private static final ResourceLocation STAR_ACTIVE = new ResourceLocation("motionui", "textures/gui/icons/star_active.png");
    private final boolean active;

    MotionUIStarButton(int id, int x, int y, int width, int height, boolean active) {
        super(id, x, y, width, height, "");
        this.active = active;
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return active ? 2 : super.getHoverState(mouseOver);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        int color = !enabled ? 0xFF706878 : active ? 0xFFFFE080 : hovered ? 0xFFFFFFB0 : 0xFFE8DFF0;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.enableBlend();
        GlStateManager.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F,
                (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        mc.getTextureManager().bindTexture(active ? STAR_ACTIVE : STAR);
        drawScaledCustomSizeModalRect(x + (width - 16) / 2, y + (height - 16) / 2,
                0F, 0F, 32, 32, 16, 16, 32F, 32F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (!blend) GlStateManager.disableBlend();
    }
}
