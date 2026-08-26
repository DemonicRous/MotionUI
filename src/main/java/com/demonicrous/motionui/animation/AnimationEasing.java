package com.demonicrous.motionui.animation;

import com.demonicrous.motionui.config.MotionUIConfig;

/** Shared easing implementation used by live transitions and editor previews. */
public final class AnimationEasing {
    private AnimationEasing() {}

    public static double opening(MotionUIConfig.ScreenEasing easing, double value) {
        double p = clamp(value);
        if (easing == MotionUIConfig.ScreenEasing.EASE_OUT_QUINT) {
            double remaining = 1D - p;
            return 1D - remaining * remaining * remaining * remaining * remaining;
        }
        if (easing == MotionUIConfig.ScreenEasing.EASE_IN_OUT_CUBIC)
            return p < .5D ? 4D * p * p * p : 1D - Math.pow(-2D * p + 2D, 3D) / 2D;
        double remaining = 1D - p;
        return 1D - remaining * remaining * remaining;
    }

    public static double closing(MotionUIConfig.ScreenEasing easing, double value) {
        double soft = ClosingComfort.smootherstep(clamp(value));
        if (easing == MotionUIConfig.ScreenEasing.EASE_OUT_QUINT) return 1D - Math.pow(1D - soft, 1.30D);
        if (easing == MotionUIConfig.ScreenEasing.EASE_IN_OUT_CUBIC) return soft;
        return 1D - Math.pow(1D - soft, 1.15D);
    }

    private static double clamp(double value) { return Math.max(0D, Math.min(1D, value)); }
}
