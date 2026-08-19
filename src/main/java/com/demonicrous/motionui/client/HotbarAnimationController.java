package com.demonicrous.motionui.client;

import com.demonicrous.motionui.animation.TimedAnimation;
import com.demonicrous.motionui.config.MotionUIConfig;

public final class HotbarAnimationController {
    private static final TimedAnimation ANIMATION=new TimedAnimation(100);
    private static double displayed=Double.NaN; private static int target; private static long last;
    private HotbarAnimationController() {}
    public static int adjustSelectorX(int x){
        long now=System.nanoTime();
        if(!MotionUIConfig.hotbar||Double.isNaN(displayed)||last==0||now-last>500000000L){displayed=target=x;ANIMATION.reset(x);last=now;return x;}
        displayed=ANIMATION.value(now);
        if(x!=target){target=x;ANIMATION.setDuration(MotionUIConfig.hotbarDuration);ANIMATION.start(displayed,x,now);}
        displayed=ANIMATION.value(now);last=now;return (int)Math.round(displayed);
    }
    public static void reset(){displayed=Double.NaN;last=0;}
}
