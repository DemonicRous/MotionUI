package com.demonicrous.motionui.client;

/** Marks a translucent MotionUI panel that must close as one composited surface. */
interface ClosingFlattenedRegion {
    MotionUILayout.Rect closingFlattenedRegion();
}
