package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

/** Replaces a null screen briefly with a non-pausing GPU snapshot after container close. */
public final class ClosingAnimationEvents {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ClosingAnimation");

    private int texture;
    private int textureWidth;
    private int textureHeight;
    private long overlayStarted;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void guiOpen(GuiOpenEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen outgoing = minecraft.currentScreen;

        if (event.getGui() != null) {
            clear();
            return;
        }
        // setIngameFocus() recursively calls displayGuiScreen(null) after the real close.
        // At that point outgoing is already null; keep the simplified snapshot alive.
        if (outgoing == null) return;
        if (!eligible(outgoing)) {
            clear();
            return;
        }

        if (capture(minecraft) && MotionUIConfig.closingMode() == MotionUIConfig.ClosingMode.FULL) {
            event.setGui(new SnapshotScreen());
        }
    }

    /** Immediate-control mode keeps the game screen active and draws only the captured visual. */
    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || texture == 0
                || MotionUIConfig.closingMode() != MotionUIConfig.ClosingMode.SIMPLIFIED) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null || minecraft.displayWidth != textureWidth
                || minecraft.displayHeight != textureHeight) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (overlayStarted == 0L) {
            overlayStarted = now;
            LOGGER.debug("Presented non-blocking closing snapshot");
        }
        double progress = progress(now, overlayStarted);
        if (progress >= 1D) {
            clear();
            return;
        }
        minecraft.entityRenderer.setupOverlayRendering();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        drawAnimated(resolution.getScaledWidth(), resolution.getScaledHeight(), progress);
    }

    private static boolean eligible(GuiScreen screen) {
        if (screen == null || screen instanceof SnapshotScreen
                || MotionUIConfig.closingMode() == MotionUIConfig.ClosingMode.DISABLED
                || MotionUIConfig.closingDuration <= 0) {
            return false;
        }
        MotionUIConfig.Policy policy = MotionUIConfig.policy(screen.getClass().getName());
        if (policy == MotionUIConfig.Policy.DISABLED) {
            return false;
        }
        return policy == MotionUIConfig.Policy.ENABLED
                || MotionUIConfig.closing && screen instanceof GuiContainer && MotionUIConfig.containers;
    }

    private boolean capture(Minecraft minecraft) {
        clear();
        if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) {
            return false;
        }

        texture = GlStateManager.generateTexture();
        textureWidth = minecraft.displayWidth;
        textureHeight = minecraft.displayHeight;
        try {
            GlStateManager.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    0, 0, textureWidth, textureHeight, 0);
            LOGGER.debug("Captured {}x{} closing snapshot", textureWidth, textureHeight);
            return true;
        } catch (RuntimeException error) {
            clear();
            LOGGER.debug("Closing snapshot capture failed", error);
            return false;
        } finally {
            GlStateManager.bindTexture(0);
        }
    }

    private void drawSnapshot(int width, int height, float offset, float alpha) {
        int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, offset, 0F);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1F, 1F, 1F, alpha);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(texture);

        try {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(0D, height, 0D).tex(0D, 0D).color(1F, 1F, 1F, alpha).endVertex();
            buffer.pos(width, height, 0D).tex(1D, 0D).color(1F, 1F, 1F, alpha).endVertex();
            buffer.pos(width, 0D, 0D).tex(1D, 1D).color(1F, 1F, 1F, alpha).endVertex();
            buffer.pos(0D, 0D, 0D).tex(0D, 1D).color(1F, 1F, 1F, alpha).endVertex();
            tessellator.draw();
        } finally {
            GlStateManager.bindTexture(0);
            GlStateManager.setActiveTexture(previousTextureUnit);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    private void clear() {
        if (texture != 0) {
            GlStateManager.deleteTexture(texture);
            texture = 0;
        }
        textureWidth = 0;
        textureHeight = 0;
        overlayStarted = 0L;
    }

    private static double progress(long now, long started) {
        return Math.max(0D, Math.min(1D,
                (now - started) / (MotionUIConfig.closingDuration * 1000000D)));
    }

    private void drawAnimated(int width, int height, double progress) {
        double eased = 1D - Math.pow(1D - progress, 3D);
        drawSnapshot(width, height, (float) (MotionUIConfig.closingOffset * eased), (float) (1D - eased));
    }

    private final class SnapshotScreen extends GuiScreen {
        private long firstFrame;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (texture == 0 || mc.displayWidth != textureWidth
                    || mc.displayHeight != textureHeight) {
                finish();
                return;
            }

            long now = System.nanoTime();
            if (firstFrame == 0L) {
                firstFrame = now;
                LOGGER.debug("Presented closing snapshot");
            }
            double progress = progress(now, firstFrame);
            if (progress >= 1D) {
                finish();
                return;
            }
            drawAnimated(width, height, progress);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            // AFTER_ANIMATION deliberately blocks game input until the short visual completes.
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
        }

        @Override
        public void onGuiClosed() {
            clear();
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        private void finish() {
            if (mc.currentScreen == this) {
                mc.displayGuiScreen(null);
            } else {
                clear();
            }
        }
    }
}
