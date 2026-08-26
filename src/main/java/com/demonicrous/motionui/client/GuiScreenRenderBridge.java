package com.demonicrous.motionui.client;

import net.minecraft.client.gui.GuiScreen;

/** Descriptor-stable entry points injected into ForgeHooksClient by the coremod. */
public final class GuiScreenRenderBridge {
    private static volatile ClosingAnimationEvents closingAnimations;

    private GuiScreenRenderBridge() {
    }

    public static void install(ClosingAnimationEvents events) {
        closingAnimations = events;
    }

    public static int adjustMouseCoordinate(GuiScreen screen, int coordinate) {
        ClosingAnimationEvents events = closingAnimations;
        return events != null && events.isClosing(screen) ? -10000 : coordinate;
    }

    public static void beginFrame(GuiScreen screen) {
        ClosingAnimationEvents events = closingAnimations;
        if (events != null) events.beginPresentedFrame(screen);
    }

    public static void endFrame(GuiScreen screen) {
        ClosingAnimationEvents events = closingAnimations;
        if (events != null) events.endPresentedFrame(screen);
    }

}
