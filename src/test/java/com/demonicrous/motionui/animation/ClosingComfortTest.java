package com.demonicrous.motionui.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class ClosingComfortTest {
    private static final int MOTION_MS = 120;

    @Test
    public void guiAndBackgroundStartFadingTogether() {
        assertEquals(1F, ClosingComfort.opacity(0D, MOTION_MS), .001F);
        assertEquals(ClosingComfort.backgroundOpacity(.1D, MOTION_MS),
                ClosingComfort.opacity(.1D, MOTION_MS), .001F);
        assertEquals(ClosingComfort.backgroundOpacity(.5D, MOTION_MS),
                ClosingComfort.opacity(.5D, MOTION_MS), .001F);
        double fadeStart = ClosingComfort.fadeStartOverallProgress(MOTION_MS);
        assertEquals(1F, ClosingComfort.opacity(fadeStart, MOTION_MS), .001F);
        assertTrue(ClosingComfort.opacity(fadeStart + .1D, MOTION_MS) < 1F);
    }

    @Test
    public void motionAndFadeOverlapWithoutHardBoundary() {
        double motionEnd = 160D / ClosingComfort.totalDurationMillis(MOTION_MS);
        assertEquals(1D, ClosingComfort.motionInput(motionEnd, MOTION_MS), .001D);
        assertTrue(ClosingComfort.opacity(motionEnd, MOTION_MS) > 0F);
        assertTrue(ClosingComfort.opacity(motionEnd, MOTION_MS) < 1F);
    }

    @Test
    public void opacityFadeIsSmoothAndMonotonic() {
        assertEquals(1F, ClosingComfort.opacity(0D, MOTION_MS), .001F);
        assertEquals(0F, ClosingComfort.opacity(1D, MOTION_MS), .001F);
        float previous = 1F;
        for (int index = 1; index <= 100; index++) {
            float next = ClosingComfort.opacity(index / 100D, MOTION_MS);
            assertTrue(next <= previous + .0001F);
            previous = next;
        }
        assertTrue(ClosingComfort.opacity(.999D, MOTION_MS) < .001F);
    }

    @Test
    public void jeiFadeStartsImmediatelyAndFinishesAtMainFadeBoundary() {
        double mainFadeStart = ClosingComfort.fadeStartOverallProgress(MOTION_MS);
        assertEquals(1F, ClosingComfort.jeiOpacity(0D, MOTION_MS), .001F);
        assertTrue(ClosingComfort.jeiOpacity(mainFadeStart * .25D, MOTION_MS) < 1F);
        assertEquals(0F, ClosingComfort.jeiOpacity(mainFadeStart, MOTION_MS), .001F);

        float previous = 1F;
        for (int index = 1; index <= 100; index++) {
            float next = ClosingComfort.jeiOpacity(
                    mainFadeStart * index / 100D, MOTION_MS);
            assertTrue(next <= previous + .0001F);
            previous = next;
        }
    }

    @Test
    public void backgroundFadeMatchesTheDelayedGuiFade() {
        double fadeStart = ClosingComfort.fadeStartOverallProgress(MOTION_MS);
        assertEquals(1F, ClosingComfort.backgroundOpacity(0D, MOTION_MS), .001F);
        assertEquals(1F, ClosingComfort.backgroundOpacity(fadeStart, MOTION_MS), .001F);
        assertEquals(0F, ClosingComfort.backgroundOpacity(1D, MOTION_MS), .001F);
        assertEquals(ClosingComfort.opacity(.8D, MOTION_MS),
                ClosingComfort.backgroundOpacity(.8D, MOTION_MS), .001F);
    }

    @Test
    public void shortSettingsStillReceiveReadableMotionAndFadePhases() {
        assertTrue(ClosingComfort.totalDurationMillis(1) >= 200);
        assertEquals(1D, ClosingComfort.motionInput(1D, 1), .001D);
        assertEquals(0F, ClosingComfort.opacity(1D, 1), .001F);
    }

    @Test
    public void smootherstepHasNearlyMotionlessEndpoints() {
        assertTrue(ClosingComfort.smootherstep(.001D) < .000001D);
        assertTrue(1D - ClosingComfort.smootherstep(.999D) < .000001D);
    }
}
