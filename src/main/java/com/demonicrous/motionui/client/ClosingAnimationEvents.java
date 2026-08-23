package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.lang.reflect.Field;
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
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

/** Replaces a null screen briefly with a non-pausing GPU snapshot after container close. */
public final class ClosingAnimationEvents {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ClosingAnimation");
    private static final boolean JEI_LOADED = Loader.isModLoaded("jei");
    private static Field guiLeftField, guiTopField, xSizeField, ySizeField;
    private static boolean fieldsResolved;

    private int texture;
    private int textureWidth;
    private int textureHeight;
    private long overlayStarted;
    private MotionUIConfig.ClosingRule activeRule;
    private boolean splitJeiOverlay;
    private int guiLeft, guiTop, guiRight, guiBottom;

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
        MotionUIConfig.ClosingRule candidate = ruleFor(outgoing);
        if (candidate == null) {
            clear();
            return;
        }
        if (capture(minecraft, outgoing)) {
            activeRule = candidate;
            if (activeRule.mode == MotionUIConfig.ClosingMode.FULL) event.setGui(new SnapshotScreen());
        }
    }

    /** Immediate-control mode keeps the game screen active and draws only the captured visual. */
    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || texture == 0
                || activeRule == null || activeRule.mode != MotionUIConfig.ClosingMode.SIMPLIFIED) {
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

    private static MotionUIConfig.ClosingRule ruleFor(GuiScreen screen) {
        if (screen == null || screen instanceof SnapshotScreen
                ) return null;
        String name=screen.getClass().getName();
        MotionUIConfig.ClosingRule rule=MotionUIConfig.closingRule(name);
        if(rule.mode==MotionUIConfig.ClosingMode.DISABLED||rule.duration<=0)return null;
        return MotionUIConfig.hasClosingRule(name)||screen instanceof GuiContainer&&MotionUIConfig.containers?rule:null;
    }

    private boolean capture(Minecraft minecraft, GuiScreen outgoing) {
        clear();
        if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) {
            return false;
        }

        texture = GlStateManager.generateTexture();
        textureWidth = minecraft.displayWidth;
        textureHeight = minecraft.displayHeight;
        prepareJeiSplit(outgoing, minecraft);
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

    private void drawSnapshot(int width, int height, float offset, float alpha, float outsideAlpha) {
        int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.pushMatrix();
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
            if (splitJeiOverlay) {
                // Preserve the original broad motion instead of leaving only a
                // floating container rectangle. JEI occupies the right-side
                // region, which stays fixed and disappears quickly.
                texturedQuad(buffer, 0, 0, guiRight, height, offset, alpha, width, height);
                texturedQuad(buffer, guiRight, 0, width, height, 0F, outsideAlpha, width, height);
            } else {
                texturedQuad(buffer, 0, 0, width, height, offset, alpha, width, height);
            }
            if (offset > 0F) {
                // Translation exposes a strip above the full-screen snapshot.
                // Fill it with the captured top edge. Both quads meet at V=1,
                // so no bright/dark seam can appear in either closing mode.
                buffer.pos(0D, offset, 0D).tex(0D, 1D).color(1F, 1F, 1F, alpha).endVertex();
                int movingRight = splitJeiOverlay ? guiRight : width;
                double movingU = movingRight / (double) width;
                buffer.pos(movingRight, offset, 0D).tex(movingU, 1D).color(1F, 1F, 1F, alpha).endVertex();
                buffer.pos(movingRight, 0D, 0D).tex(movingU, 1D).color(1F, 1F, 1F, alpha).endVertex();
                buffer.pos(0D, 0D, 0D).tex(0D, 1D).color(1F, 1F, 1F, alpha).endVertex();
            }
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

    private static void texturedQuad(BufferBuilder buffer, int left, int top, int right, int bottom,
            float offset, float alpha, int width, int height) {
        if (right <= left || bottom <= top || alpha <= 0F) return;
        double u1 = left / (double) width, u2 = right / (double) width;
        double vTop = 1D - top / (double) height, vBottom = 1D - bottom / (double) height;
        buffer.pos(left, bottom + offset, 0D).tex(u1, vBottom).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(right, bottom + offset, 0D).tex(u2, vBottom).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(right, top + offset, 0D).tex(u2, vTop).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(left, top + offset, 0D).tex(u1, vTop).color(1F, 1F, 1F, alpha).endVertex();
    }

    private float fastOverlayAlpha(double progress) {
        if (activeRule == null) return 1F;
        double elapsedMs = progress * Math.max(1, activeRule.duration);
        double p = Math.max(0D, Math.min(1D, elapsedMs / 90D));
        double remaining = 1D - p;
        return (float) (remaining * remaining * remaining);
    }

    private void prepareJeiSplit(GuiScreen screen, Minecraft minecraft) {
        if (!JEI_LOADED || !(screen instanceof GuiContainer)) return;
        try {
            resolveContainerFields();
            ScaledResolution resolution = new ScaledResolution(minecraft);
            int width = resolution.getScaledWidth(), height = resolution.getScaledHeight();
            int padding = 4;
            guiLeft = Math.max(0, guiLeftField.getInt(screen) - padding);
            guiTop = Math.max(0, guiTopField.getInt(screen) - padding);
            guiRight = Math.min(width, guiLeftField.getInt(screen) + xSizeField.getInt(screen) + padding);
            guiBottom = Math.min(height, guiTopField.getInt(screen) + ySizeField.getInt(screen) + padding);
            splitJeiOverlay = guiRight > guiLeft && guiBottom > guiTop;
        } catch (Throwable error) {
            splitJeiOverlay = false;
            LOGGER.debug("Could not isolate JEI overlay from closing snapshot", error);
        }
    }

    private static void resolveContainerFields() {
        if (fieldsResolved) return;
        guiLeftField = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
        guiTopField = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
        xSizeField = ReflectionHelper.findField(GuiContainer.class, "xSize", "field_146999_f");
        ySizeField = ReflectionHelper.findField(GuiContainer.class, "ySize", "field_147000_g");
        fieldsResolved = true;
    }

    private void clear() {
        if (texture != 0) {
            GlStateManager.deleteTexture(texture);
            texture = 0;
        }
        textureWidth = 0;
        textureHeight = 0;
        overlayStarted = 0L;
        activeRule = null;
        splitJeiOverlay = false;
        guiLeft = guiTop = guiRight = guiBottom = 0;
    }

    private double progress(long now, long started) {
        return Math.max(0D, Math.min(1D,
                (now - started) / (Math.max(1, activeRule == null ? MotionUIConfig.closingDuration : activeRule.duration) * 1000000D)));
    }

    private void drawAnimated(int width, int height, double progress) {
        double eased = 1D - Math.pow(1D - progress, 3D);
        int offset=activeRule==null?MotionUIConfig.closingOffset:activeRule.offset;
        drawSnapshot(width, height, (float) (offset * eased), (float) (1D - eased), fastOverlayAlpha(progress));
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
