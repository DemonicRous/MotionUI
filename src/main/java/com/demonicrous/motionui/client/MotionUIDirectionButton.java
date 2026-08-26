package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Compact D-pad cell with pixel-aligned vector icons. */
final class MotionUIDirectionButton extends MotionUIButton {
    private static final ResourceLocation ARROW = new ResourceLocation("motionui", "textures/gui/icons/arrow.png");
    private final MotionUIConfig.AnimationDirection direction;
    private final boolean scale;
    private final boolean selected;
    private final boolean inward;

    MotionUIDirectionButton(int id, int x, int y, int size,
            MotionUIConfig.AnimationDirection direction, boolean scale, boolean selected, boolean inward) {
        super(id, x, y, size, size, "");
        this.direction = direction;
        this.scale = scale;
        this.selected = selected;
        this.inward = inward;
    }

    @Override protected int getHoverState(boolean mouseOver) {
        return selected ? 2 : super.getHoverState(mouseOver);
    }

    @Override public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        int color = !enabled ? 0xFF706878 : hovered ? 0xFFFFFFB0
                : selected ? 0xFFFFE080 : 0xFFE8DFF0;
        int cx = x + width / 2, cy = y + height / 2;
        if (scale) {
            box(cx - 5, cy - 5, cx + 6, cy + 6, color);
            box(cx - 2, cy - 2, cx + 3, cy + 3, color);
            return;
        }
        drawArrowSprite(mc, cx, cy, color);
    }

    private void drawArrowSprite(Minecraft mc, int cx, int cy, int color) {
        MotionUIConfig.AnimationDirection icon = inward ? opposite(direction) : direction;
        float angle = icon == MotionUIConfig.AnimationDirection.RIGHT ? 90F
                : icon == MotionUIConfig.AnimationDirection.DOWN ? 180F
                : icon == MotionUIConfig.AnimationDirection.LEFT ? 270F : 0F;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.enableBlend();
        GlStateManager.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F,
                (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        mc.getTextureManager().bindTexture(ARROW);
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0F);
        GlStateManager.rotate(angle, 0F, 0F, 1F);
        drawScaledCustomSizeModalRect(-7, -7, 0F, 0F, 32, 32, 14, 14, 32F, 32F);
        GlStateManager.popMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (!blend) GlStateManager.disableBlend();
    }

    private static MotionUIConfig.AnimationDirection opposite(MotionUIConfig.AnimationDirection value) {
        if (value == MotionUIConfig.AnimationDirection.UP) return MotionUIConfig.AnimationDirection.DOWN;
        if (value == MotionUIConfig.AnimationDirection.RIGHT) return MotionUIConfig.AnimationDirection.LEFT;
        if (value == MotionUIConfig.AnimationDirection.DOWN) return MotionUIConfig.AnimationDirection.UP;
        return MotionUIConfig.AnimationDirection.RIGHT;
    }

    private void box(int l,int t,int r,int b,int c){drawRect(l,t,r,t+1,c);drawRect(l,b-1,r,b,c);drawRect(l,t,l+1,b,c);drawRect(r-1,t,r,b,c);}
}
