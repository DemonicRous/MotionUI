package com.demonicrous.motionui.client;

import com.demonicrous.motionui.animation.AnimationTransform;
import com.demonicrous.motionui.animation.AnimationEasing;
import com.demonicrous.motionui.animation.ClosingComfort;
import com.demonicrous.motionui.config.MotionUIConfig;
import com.demonicrous.motionui.core.GuiBackgroundTransformer;
import com.demonicrous.motionui.core.GuiScreenRenderTransformer;
import com.demonicrous.motionui.core.PatchDiagnostics;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Single-pass client closing animation. FULL keeps the live screen installed;
 * SIMPLIFIED detaches it immediately and presents the same live visual from the
 * HUD pass, returning game control without taking a screenshot.
 */
public final class ClosingAnimationEvents {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ClosingAnimation");
    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final boolean JEI_LOADED = Loader.isModLoaded("jei");
    private static Field guiLeftField;
    private static Field xSizeField;
    private static boolean containerFieldsResolved;

    private final LayerCompositor compositor = new LayerCompositor();
    private GuiScreen closingScreen;
    private GuiScreen presentingScreen;
    private MotionUIConfig.ClosingRule activeRule;
    private AnimationTransform presentedTransform;
    private long startedNanos;
    private boolean detached;
    private boolean finishRequested;
    private boolean allowVanillaClose;
    private boolean focusCloseBypass;
    private boolean firstFrameLogged;
    private boolean offscreenFrame;
    private boolean presentedOwnBackground;
    private float presentedOpacity;
    private float presentedJeiOpacity;
    private int presentedJeiLeft;
    private int presentedJeiRight;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void guiOpen(GuiOpenEvent event) {
        if (focusCloseBypass || allowVanillaClose) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen outgoing = minecraft.currentScreen;
        if (event.getGui() != null) {
            finishInterruptedClose();
            return;
        }
        if (outgoing == null) return;
        if (outgoing == closingScreen) {
            event.setCanceled(true);
            return;
        }

        MotionUIConfig.ClosingRule candidate = ruleFor(outgoing);
        boolean patches = patchesAvailable();
        LOGGER.info("Close request: screen={}, eligible={}, patches={}",
                outgoing.getClass().getName(), candidate != null, patches);
        if (candidate == null || !patches) return;

        event.setCanceled(true);
        closingScreen = outgoing;
        activeRule = candidate;
        startedNanos = System.nanoTime();
        detached = candidate.mode == MotionUIConfig.ClosingMode.SIMPLIFIED;
        finishRequested = false;
        firstFrameLogged = false;
        LOGGER.info("Started {} live closing animation for {} ({} ms, {} px, {}, {})",
                candidate.mode, outgoing.getClass().getName(), candidate.duration,
                candidate.offset, candidate.style, candidate.direction);

        if (detached) detachForImmediateControl(minecraft);
    }

    boolean isClosing(GuiScreen screen) {
        return screen != null && screen == closingScreen && activeRule != null;
    }

    /** Called at the start of ForgeHooksClient.drawScreen, before mod overlays. */
    void beginPresentedFrame(GuiScreen screen) {
        if (!isClosing(screen) || presentingScreen != null) return;

        if (!firstFrameLogged) {
            firstFrameLogged = true;
            LOGGER.info("Presented first live closing frame for {}", screen.getClass().getName());
        }

        double progress = progress(System.nanoTime());
        if (progress >= 1D) {
            progress = 1D;
            finishRequested = true;
        }
        double motion = ease(ClosingComfort.motionInput(progress, activeRule.duration));
        presentedTransform = AnimationTransform.comfortableClosing(
                activeRule.style, activeRule.direction, activeRule.offset,
                activeRule.duration, screen.width, screen.height, motion);
        presentedOpacity = ClosingComfort.opacity(progress, activeRule.duration);
        presentedJeiOpacity = ClosingComfort.jeiOpacity(progress, activeRule.duration);
        presentedOwnBackground = hasOpaqueCustomBackground(screen);
        long jeiBounds = jeiBounds(screen);
        presentedJeiLeft = (int) (jeiBounds >>> 32);
        presentedJeiRight = (int) jeiBounds;
        presentingScreen = screen;

        offscreenFrame = compositor.begin(screen, presentedOwnBackground);
        if (!offscreenFrame) {
            drawDimBackground(screen.width, screen.height, presentedOpacity);
            GlStateManager.pushMatrix();
            applyTransform(screen, presentedTransform);
            GuiBackgroundControl.beginSuppression();
        }
    }

    /** Called after Forge's DrawScreenEvent.Post so JEI is part of the same layer. */
    void endPresentedFrame(GuiScreen screen) {
        if (screen != presentingScreen) return;
        try {
            if (offscreenFrame) {
                compositor.end(screen, presentedTransform, presentedOpacity,
                        presentedJeiOpacity, presentedJeiLeft, presentedJeiRight);
            } else {
                GuiBackgroundControl.endSuppression();
                GlStateManager.popMatrix();
            }
        } finally {
            presentingScreen = null;
            offscreenFrame = false;
        }
    }

    void beginCustomBackground(GuiScreen screen) {
        if (screen == presentingScreen && presentedOwnBackground && offscreenFrame) {
            compositor.beginStationaryBackground();
        }
    }

    void endCustomBackground(GuiScreen screen) {
        if (screen == presentingScreen && presentedOwnBackground && offscreenFrame) {
            compositor.endStationaryBackground();
        }
    }

    /** SIMPLIFIED has no currentScreen, so its one GUI draw belongs to the HUD tail. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void overlay(RenderGameOverlayEvent.Post event) {
        if (!detached || closingScreen == null
                || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null) {
            finishInterruptedClose();
            return;
        }
        GuiScreen previous = minecraft.currentScreen;
        minecraft.currentScreen = closingScreen;
        try {
            ForgeHooksClient.drawScreen(closingScreen, -10000, -10000,
                    event.getPartialTicks());
        } finally {
            if (minecraft.currentScreen == closingScreen) minecraft.currentScreen = previous;
        }
    }

    /** FULL consumes GUI input during its short protected transition. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!detached && isClosing(event.getGui())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void keyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!detached && isClosing(event.getGui())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!detached && closingScreen != null) flushMouseMotion(Minecraft.getMinecraft());
        if (closingScreen != null && !firstFrameLogged
                && System.nanoTime() - startedNanos > 100000000L) {
            LOGGER.error("Closing render hook did not answer within 100 ms; closing {} normally",
                    closingScreen.getClass().getName());
            finishRequested = true;
        }
        if (!finishRequested) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen finished = closingScreen;
        if (detached) {
            reset();
            if (finished != null) finished.onGuiClosed();
            return;
        }
        if (minecraft.currentScreen != finished) {
            reset();
            return;
        }

        flushMouseMotion(minecraft);
        allowVanillaClose = true;
        try {
            minecraft.displayGuiScreen(null);
        } finally {
            allowVanillaClose = false;
            flushMouseMotion(minecraft);
            reset();
        }
    }

    private void detachForImmediateControl(Minecraft minecraft) {
        minecraft.currentScreen = null;
        minecraft.getSoundHandler().resumeSounds();
        focusCloseBypass = true;
        try {
            if (Display.isActive()) {
                KeyBinding.updateKeyBindState();
                minecraft.setIngameFocus();
            }
        } finally {
            focusCloseBypass = false;
        }
    }

    private void finishInterruptedClose() {
        GuiScreen interrupted = detached ? closingScreen : null;
        compositor.abort();
        reset();
        if (interrupted != null) interrupted.onGuiClosed();
    }

    private static MotionUIConfig.ClosingRule ruleFor(GuiScreen screen) {
        if (screen == null) return null;
        String name = screen.getClass().getName();
        // Thaumcraft 6 draws the research background and foreground through one
        // stateful OpenGL routine. Separating or replaying it corrupts either the
        // star field or research nodes, so prefer the native close path.
        if ("thaumcraft.client.gui.GuiResearchBrowser".equals(name)) return null;
        MotionUIConfig.ClosingRule rule = MotionUIConfig.closingRule(name, GuiCatalog.modId(name));
        if (rule.mode == MotionUIConfig.ClosingMode.DISABLED || rule.duration <= 0) return null;
        return MotionUIConfig.hasClosingRule(name)
                || screen instanceof GuiContainer && MotionUIConfig.containers ? rule : null;
    }

    private static boolean patchesAvailable() {
        return PatchDiagnostics.isApplied(GuiBackgroundTransformer.PATCH)
                && PatchDiagnostics.isApplied(GuiScreenRenderTransformer.PATCH);
    }

    private double progress(long now) {
        return Math.max(0D, Math.min(1D, (now - startedNanos)
                / (ClosingComfort.totalDurationMillis(activeRule.duration) * 1000000D)));
    }

    private double ease(double progress) {
        return AnimationEasing.closing(activeRule.easing, progress);
    }

    private static void applyTransform(GuiScreen screen, AnimationTransform transform) {
        GlStateManager.translate(transform.translateX, transform.translateY, 0D);
        if (transform.scale != 1F) {
            GlStateManager.translate(screen.width / 2D, screen.height / 2D, 0D);
            GlStateManager.scale(transform.scale, transform.scale, 1D);
            GlStateManager.translate(-screen.width / 2D, -screen.height / 2D, 0D);
        }
    }

    private static long jeiBounds(GuiScreen screen) {
        if (!JEI_LOADED || !(screen instanceof GuiContainer)) return 0L;
        try {
            resolveContainerFields();
            int guiLeft = guiLeftField.getInt(screen);
            int guiRight = guiLeft + xSizeField.getInt(screen);
            int left = Math.max(0, Math.min(screen.width, guiLeft - 4));
            int right = Math.max(0, Math.min(screen.width, guiRight + 4));
            return left < right ? (long) left << 32 | right & 0xFFFFFFFFL : 0L;
        } catch (Throwable error) {
            LOGGER.debug("Could not isolate the JEI side overlays", error);
            return 0L;
        }
    }

    private static boolean hasOpaqueCustomBackground(GuiScreen screen) {
        return false;
    }

    private static void resolveContainerFields() {
        if (containerFieldsResolved) return;
        guiLeftField = ReflectionHelper.findField(GuiContainer.class,
                "guiLeft", "field_147003_i");
        xSizeField = ReflectionHelper.findField(GuiContainer.class,
                "xSize", "field_146999_f");
        containerFieldsResolved = true;
    }

    private static void flushMouseMotion(Minecraft minecraft) {
        if (Mouse.isCreated()) {
            Mouse.getDX();
            Mouse.getDY();
        }
        minecraft.mouseHelper.deltaX = 0;
        minecraft.mouseHelper.deltaY = 0;
    }

    private void reset() {
        closingScreen = null;
        presentingScreen = null;
        activeRule = null;
        presentedTransform = null;
        startedNanos = 0L;
        detached = false;
        finishRequested = false;
        firstFrameLogged = false;
        offscreenFrame = false;
        presentedOpacity = 0F;
        presentedJeiOpacity = 0F;
        presentedOwnBackground = false;
        presentedJeiLeft = 0;
        presentedJeiRight = 0;
    }

    private static void drawDimBackground(int width, int height, float opacity) {
        if (opacity <= 0F) return;
        int topAlpha = clampAlpha(192F * opacity);
        int bottomAlpha = clampAlpha(208F * opacity);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(width, 0D, 0D).color(16, 16, 16, topAlpha).endVertex();
        buffer.pos(0D, 0D, 0D).color(16, 16, 16, topAlpha).endVertex();
        buffer.pos(0D, height, 0D).color(16, 16, 16, bottomAlpha).endVertex();
        buffer.pos(width, height, 0D).color(16, 16, 16, bottomAlpha).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static int clampAlpha(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    /** Owns one reusable full-resolution color/depth layer and its GL boundary. */
    private final class LayerCompositor {
        // LWJGL 2 validates glGet*v buffers against the largest value OpenGL may
        // return, not the four components used by GL_VIEWPORT/CURRENT_COLOR.
        private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
        private final SavedGlState saved = new SavedGlState();
        private final SavedGlState flattenSaved = new SavedGlState();
        private Framebuffer layer;
        private Framebuffer stationaryBackground;
        private int previousFramebuffer;
        private int previousMatrixMode;
        private int viewportX;
        private int viewportY;
        private int viewportWidth;
        private int viewportHeight;
        private boolean active;
        private boolean suppressBackground;
        private boolean includesBackground;
        private boolean warned;

        boolean begin(GuiScreen screen, boolean includeBackground) {
            if (!OpenGlHelper.isFramebufferEnabled()) return false;
            Minecraft minecraft = Minecraft.getMinecraft();
            captureBoundary();
            pushMatrices();
            try {
                ensureLayer(minecraft.displayWidth, minecraft.displayHeight);
                layer.bindFramebuffer(true);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.depthMask(true);
                GlStateManager.clearColor(0F, 0F, 0F, 0F);
                GlStateManager.clearDepth(1D);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                minecraft.entityRenderer.setupOverlayRendering();
                includesBackground = includeBackground;
                suppressBackground = true;
                GuiBackgroundControl.beginSuppression();
                active = true;
                return true;
            } catch (RuntimeException error) {
                if (suppressBackground) GuiBackgroundControl.endSuppression();
                restoreBoundary();
                if (!warned) {
                    warned = true;
                    LOGGER.warn("GUI compositor unavailable; using direct motion fallback", error);
                }
                return false;
            }
        }

        void end(GuiScreen screen, AnimationTransform transform, float opacity,
                float jeiOpacity, int jeiLeft, int jeiRight) {
            if (!active) return;
            try {
                if (suppressBackground) GuiBackgroundControl.endSuppression();
                flattenMotionUiPanel(screen);
                bindPreviousTarget();
                Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
                reconcileBlendState();
                drawDimBackground(screen.width, screen.height, opacity);
                if (includesBackground && stationaryBackground != null) {
                    drawTexture(stationaryBackground.framebufferTexture, screen.width,
                            screen.height, opacity);
                }
                drawLayer(screen, transform, opacity, jeiOpacity, jeiLeft, jeiRight);
            } finally {
                restoreBoundary();
            }
        }

        /**
         * Some legacy GUIs (notably Thaumcraft containers) call
         * GL11.glDisable(GL_BLEND) directly. That changes the driver without
         * updating GlStateManager's cache, so a later enableBlend() can be
         * skipped and our translucent dim quad becomes an opaque black quad.
         * Toggle through GlStateManager to make its cache and the driver agree.
         */
        private void reconcileBlendState() {
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
        }

        /**
         * MotionUI panels contain nested translucent surfaces. Gui.drawRect replaces
         * destination alpha while composing those surfaces into a transparent FBO,
         * so fading the FBO would otherwise make child cards more transparent than
         * their parent. Preserve the already-composited RGB and normalize only the
         * panel coverage before applying the closing opacity.
         */
        private void flattenMotionUiPanel(GuiScreen screen) {
            if (!(screen instanceof ClosingFlattenedRegion)) return;
            MotionUILayout.Rect region = ((ClosingFlattenedRegion) screen).closingFlattenedRegion();
            if (region == null || region.width < 4 || region.height < 4) return;

            // This pass is an implementation detail of MotionUI's own panels.
            // Keep its state contract local: otherwise GlStateManager can cache
            // the temporary alpha-only mask and a later third-party GUI may be
            // composited against an opaque black layer.
            flattenSaved.capture();
            try {
                GlStateManager.disableBlend();
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);
                GlStateManager.colorMask(false, false, false, true);
                int alpha = MotionUITheme.FRAME >>> 24 & 255;
                alphaRect(region.x + 2, region.y, region.right() - 2, region.bottom(), alpha);
                alphaRect(region.x, region.y + 2, region.right(), region.bottom() - 2, alpha);
                alphaRect(region.x + 1, region.y + 1, region.right() - 1, region.bottom() - 1, alpha);
            } finally {
                flattenSaved.restore();
            }
        }

        private void alphaRect(int left, int top, int right, int bottom, int alpha) {
            if (right <= left || bottom <= top) return;
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(right, top, 0D).color(255, 255, 255, alpha).endVertex();
            buffer.pos(left, top, 0D).color(255, 255, 255, alpha).endVertex();
            buffer.pos(left, bottom, 0D).color(255, 255, 255, alpha).endVertex();
            buffer.pos(right, bottom, 0D).color(255, 255, 255, alpha).endVertex();
            Tessellator.getInstance().draw();
        }

        void abort() {
            if (!active) return;
            if (suppressBackground) GuiBackgroundControl.endSuppression();
            restoreBoundary();
        }

        private void ensureLayer(int width, int height) {
            if (layer == null) {
                layer = new Framebuffer(width, height, true);
                layer.setFramebufferColor(0F, 0F, 0F, 0F);
                layer.setFramebufferFilter(GL11.GL_LINEAR);
            } else if (layer.framebufferWidth != width || layer.framebufferHeight != height) {
                layer.createBindFramebuffer(width, height);
                layer.setFramebufferColor(0F, 0F, 0F, 0F);
                layer.setFramebufferFilter(GL11.GL_LINEAR);
            }
            if (stationaryBackground == null) {
                stationaryBackground = new Framebuffer(width, height, true);
                stationaryBackground.setFramebufferFilter(GL11.GL_LINEAR);
            } else if (stationaryBackground.framebufferWidth != width
                    || stationaryBackground.framebufferHeight != height) {
                stationaryBackground.createBindFramebuffer(width, height);
                stationaryBackground.setFramebufferFilter(GL11.GL_LINEAR);
            }
        }

        void beginStationaryBackground() {
            // Keep Thaumcraft in the framebuffer and matrix context it expects.
            // Its translucent star textures expect an opaque destination, so
            // establish that destination without rebinding or resetting matrices.
            if (!active || !includesBackground) return;
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthMask(true);
            GlStateManager.clearColor(0F, 0F, 0F, 1F);
            GlStateManager.clearDepth(1D);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        }

        void endStationaryBackground() {
            if (!active || !includesBackground || stationaryBackground == null) return;
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(stationaryBackground.framebufferTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                    layer.framebufferWidth, layer.framebufferHeight);
            GlStateManager.bindTexture(0);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthMask(true);
            GlStateManager.clearColor(0F, 0F, 0F, 0F);
            GlStateManager.clearDepth(1D);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
        }

        private void captureBoundary() {
            previousFramebuffer = GL11.glGetInteger(GL_FRAMEBUFFER_BINDING);
            viewport.clear();
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            viewportX = viewport.get(0);
            viewportY = viewport.get(1);
            viewportWidth = viewport.get(2);
            viewportHeight = viewport.get(3);
            previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            saved.capture();
        }

        private void pushMatrices() {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
        }

        private void bindPreviousTarget() {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            GlStateManager.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
        }

        private void restoreBoundary() {
            try {
                bindPreviousTarget();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(previousMatrixMode);
                saved.restore();
            } finally {
                active = false;
                suppressBackground = false;
                includesBackground = false;
            }
        }

        private void drawLayer(GuiScreen screen, AnimationTransform transform,
                float opacity, float jeiOpacity, int jeiLeft, int jeiRight) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(layer.framebufferTexture);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);

            boolean isolateJei = jeiRight > jeiLeft;
            int movingLeft = isolateJei ? jeiLeft : 0;
            int movingRight = isolateJei ? jeiRight : screen.width;
            if (opacity > 0F && movingRight > movingLeft) {
                GlStateManager.pushMatrix();
                applyTransform(screen, transform);
                texturedQuad(movingLeft, movingRight, screen.width, screen.height, opacity);
                GlStateManager.popMatrix();
            }
            if (isolateJei && jeiOpacity > 0F) {
                if (jeiLeft > 0) {
                    texturedQuad(0, jeiLeft, screen.width, screen.height, jeiOpacity);
                }
                if (jeiRight < screen.width) {
                    texturedQuad(jeiRight, screen.width, screen.width, screen.height, jeiOpacity);
                }
            }
            GlStateManager.bindTexture(0);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }

        private void drawTexture(int texture, int width, int height, float opacity) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(texture);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            texturedQuad(0, width, width, height, opacity);
        }

        private void texturedQuad(int left, int right, int width, int height, float opacity) {
            double u1 = left / (double) width;
            double u2 = right / (double) width;
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(left, height, 0D).tex(u1, 0D)
                    .color(opacity, opacity, opacity, opacity).endVertex();
            buffer.pos(right, height, 0D).tex(u2, 0D)
                    .color(opacity, opacity, opacity, opacity).endVertex();
            buffer.pos(right, 0D, 0D).tex(u2, 1D)
                    .color(opacity, opacity, opacity, opacity).endVertex();
            buffer.pos(left, 0D, 0D).tex(u1, 1D)
                    .color(opacity, opacity, opacity, opacity).endVertex();
            Tessellator.getInstance().draw();
        }
    }

    /** Mutable, allocation-free snapshot for states changed by the compositor. */
    private static final class SavedGlState {
        private static final int GL_BLEND_SRC_RGB = 0x80C9;
        private static final int GL_BLEND_DST_RGB = 0x80C8;
        private static final int GL_BLEND_SRC_ALPHA = 0x80CB;
        private static final int GL_BLEND_DST_ALPHA = 0x80CA;
        private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
        private final ByteBuffer colorMask = BufferUtils.createByteBuffer(16);
        private boolean blend;
        private boolean alpha;
        private boolean texture;
        private boolean depth;
        private boolean lighting;
        private boolean cull;
        private boolean scissor;
        private boolean fog;
        private boolean depthMask;
        private int blendSrcRgb;
        private int blendDstRgb;
        private int blendSrcAlpha;
        private int blendDstAlpha;
        private int depthFunc;
        private int shadeModel;
        private int activeTexture;
        private int boundTexture;
        private float red;
        private float green;
        private float blue;
        private float opacity;
        private boolean colorRed;
        private boolean colorGreen;
        private boolean colorBlue;
        private boolean colorAlpha;

        void capture() {
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            fog = GL11.glIsEnabled(GL11.GL_FOG);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            blendSrcRgb = GL11.glGetInteger(GL_BLEND_SRC_RGB);
            blendDstRgb = GL11.glGetInteger(GL_BLEND_DST_RGB);
            blendSrcAlpha = GL11.glGetInteger(GL_BLEND_SRC_ALPHA);
            blendDstAlpha = GL11.glGetInteger(GL_BLEND_DST_ALPHA);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            color.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
            red = color.get(0);
            green = color.get(1);
            blue = color.get(2);
            opacity = color.get(3);
            colorMask.clear();
            GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
            colorRed = colorMask.get(0) != 0;
            colorGreen = colorMask.get(1) != 0;
            colorBlue = colorMask.get(2) != 0;
            colorAlpha = colorMask.get(3) != 0;
        }

        void restore() {
            GlStateManager.setActiveTexture(activeTexture);
            GlStateManager.bindTexture(boundTexture);
            GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb,
                    blendSrcAlpha, blendDstAlpha);
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            // Reconcile both Minecraft's cached state and the actual driver state.
            if(fog){GlStateManager.disableFog();GlStateManager.enableFog();}
            else{GlStateManager.enableFog();GlStateManager.disableFog();}
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.depthMask(depthMask);
            GlStateManager.depthFunc(depthFunc);
            GlStateManager.shadeModel(shadeModel);
            GlStateManager.colorMask(colorRed, colorGreen, colorBlue, colorAlpha);
            GlStateManager.color(red, green, blue, opacity);
        }
    }
}
