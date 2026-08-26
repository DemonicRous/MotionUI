package com.demonicrous.motionui.client;

/** Render-thread switch read by the transformed vanilla GuiScreen background method. */
public final class GuiBackgroundControl {
    private static boolean suppressed;

    private GuiBackgroundControl() {
    }

    /** Called from transformed client bytecode. Must remain public and descriptor-stable. */
    public static boolean shouldSuppressBackground() {
        return suppressed;
    }

    static void beginSuppression() {
        suppressed = true;
    }

    static void endSuppression() {
        suppressed = false;
    }
}
