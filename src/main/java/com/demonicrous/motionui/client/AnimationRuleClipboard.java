package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;

/** Session-local, immutable animation rule clipboard. */
final class AnimationRuleClipboard {
    private static MotionUIConfig.ScreenRule opening;
    private static MotionUIConfig.Policy openingPolicy;
    private static MotionUIConfig.ClosingRule closing;
    private AnimationRuleClipboard() {}

    static void copyOpening(MotionUIConfig.ScreenRule rule,MotionUIConfig.Policy policy){opening=rule;openingPolicy=policy;}
    static void copyClosing(MotionUIConfig.ClosingRule rule){closing=rule;}
    static MotionUIConfig.ScreenRule opening(){return opening;}
    static MotionUIConfig.Policy openingPolicy(){return openingPolicy;}
    static MotionUIConfig.ClosingRule closing(){return closing;}
    static boolean hasOpening(){return opening!=null;}
    static boolean hasClosing(){return closing!=null;}
}
