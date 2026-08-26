package com.demonicrous.motionui.animation;

import static com.demonicrous.motionui.config.MotionUIConfig.AnimationDirection.*;
import static com.demonicrous.motionui.config.MotionUIConfig.AnimationStyle.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class AnimationTransformTest {
    @Test public void legacySlideUpStartsBelowAndSettles() {
        AnimationTransform start = AnimationTransform.opening(SLIDE, UP, 10, 0D);
        AnimationTransform end = AnimationTransform.opening(SLIDE, UP, 10, 1D);
        assertEquals(10F, start.translateY, .001F);
        assertEquals(0F, end.translateY, .001F);
        assertEquals(1F, start.scale, .001F);
    }

    @Test public void allDirectionsProduceExpectedOpeningOrigin() {
        assertEquals(-8F, AnimationTransform.opening(SLIDE, DOWN, 8, 0D).translateY, .001F);
        assertEquals(8F, AnimationTransform.opening(SLIDE, LEFT, 8, 0D).translateX, .001F);
        assertEquals(-8F, AnimationTransform.opening(SLIDE, RIGHT, 8, 0D).translateX, .001F);
    }

    @Test public void scaleSlideCombinesBothComponents() {
        AnimationTransform start = AnimationTransform.opening(SCALE_SLIDE, RIGHT, 12, 0D);
        assertEquals(-12F, start.translateX, .001F);
        assertEquals(AnimationTransform.MIN_SCALE, start.scale, .001F);
    }

}
