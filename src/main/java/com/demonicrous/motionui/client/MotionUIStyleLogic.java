package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;

/** Shared, explicit composition rules for the arrow + scale selector. */
final class MotionUIStyleLogic {
    private MotionUIStyleLogic() {}

    static MotionUIConfig.AnimationStyle toggleSlide(MotionUIConfig.AnimationStyle style, boolean sameDirection) {
        if (style == MotionUIConfig.AnimationStyle.SCALE) return MotionUIConfig.AnimationStyle.SCALE_SLIDE;
        if (style == MotionUIConfig.AnimationStyle.SCALE_SLIDE && sameDirection) return MotionUIConfig.AnimationStyle.SCALE;
        return style;
    }

    static MotionUIConfig.AnimationStyle toggleScale(MotionUIConfig.AnimationStyle style) {
        if (style == MotionUIConfig.AnimationStyle.SLIDE) return MotionUIConfig.AnimationStyle.SCALE_SLIDE;
        if (style == MotionUIConfig.AnimationStyle.SCALE_SLIDE) return MotionUIConfig.AnimationStyle.SLIDE;
        return MotionUIConfig.AnimationStyle.SCALE;
    }

    static MotionUIConfig.AnimationDirection directionFor(int id, int firstId) {
        MotionUIConfig.AnimationDirection[] values = {MotionUIConfig.AnimationDirection.UP,
                MotionUIConfig.AnimationDirection.RIGHT, MotionUIConfig.AnimationDirection.DOWN,
                MotionUIConfig.AnimationDirection.LEFT};
        return values[Math.max(0, Math.min(3, id - firstId))];
    }
}
