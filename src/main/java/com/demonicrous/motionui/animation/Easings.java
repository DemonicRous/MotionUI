package com.demonicrous.motionui.animation;
public enum Easings implements Easing {
    LINEAR { public double apply(double p) { return clamp(p); } },
    EASE_OUT_CUBIC { public double apply(double p) { double r=1-clamp(p); return 1-r*r*r; } };
    static double clamp(double p) { return Math.max(0D, Math.min(1D, p)); }
}
