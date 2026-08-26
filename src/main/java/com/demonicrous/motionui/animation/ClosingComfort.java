package com.demonicrous.motionui.animation;

/** Overlapped motion/fade timeline with a protected frame budget and soft boundaries. */
public final class ClosingComfort {
    public static final double FADE_START_MOTION = .60D;
    private static final int MIN_MOTION_MS = 160;
    private static final int MIN_FADE_MS = 120;

    private ClosingComfort() {
    }

    public static int totalDurationMillis(int configuredMotionMillis) {
        int motion = motionDurationMillis(configuredMotionMillis);
        int fadeEnd = fadeStartMillis(motion) + fadeDurationMillis(motion);
        return Math.max(motion, fadeEnd);
    }

    /** Returns 0..1 across the protected motion duration. */
    public static double motionInput(double overallProgress, int configuredMotionMillis) {
        double elapsed = clamp(overallProgress)
                * totalDurationMillis(configuredMotionMillis);
        return clamp(elapsed / motionDurationMillis(configuredMotionMillis));
    }

    /** Fades the main GUI only after 60% of the motion phase has elapsed. */
    public static float opacity(double overallProgress, int configuredMotionMillis) {
        int motion = motionDurationMillis(configuredMotionMillis);
        double elapsed = clamp(overallProgress) * totalDurationMillis(configuredMotionMillis);
        double fade = clamp((elapsed - fadeStartMillis(motion))
                / (totalDurationMillis(configuredMotionMillis) - fadeStartMillis(motion)));
        return (float) (1D - smootherstep(fade));
    }

    /** Fades the separately cached JEI region before the main movement completes. */
    public static float jeiOpacity(double overallProgress, int configuredMotionMillis) {
        int motion = motionDurationMillis(configuredMotionMillis);
        double elapsed = clamp(overallProgress) * totalDurationMillis(configuredMotionMillis);
        double fade = clamp(elapsed / fadeStartMillis(motion));
        return (float) (1D - smootherstep(fade));
    }

    public static double fadeStartOverallProgress(int configuredMotionMillis) {
        int motion = motionDurationMillis(configuredMotionMillis);
        return fadeStartMillis(motion) / (double) totalDurationMillis(configuredMotionMillis);
    }

    /** Keeps the GUI and stationary dim on the same fade curve. */
    public static float backgroundOpacity(double overallProgress, int configuredMotionMillis) {
        return opacity(overallProgress, configuredMotionMillis);
    }

    public static double smootherstep(double value) {
        double progress = clamp(value);
        return progress * progress * progress
                * (progress * (progress * 6D - 15D) + 10D);
    }

    private static int motionDurationMillis(int configured) {
        return Math.max(MIN_MOTION_MS, configured);
    }

    private static int fadeStartMillis(int motionDuration) {
        return (int) Math.round(motionDuration * FADE_START_MOTION);
    }

    private static int fadeDurationMillis(int motionDuration) {
        return Math.max(MIN_FADE_MS,
                motionDuration - fadeStartMillis(motionDuration));
    }

    private static double clamp(double value) {
        return Math.max(0D, Math.min(1D, value));
    }
}
