package com.demonicrous.motionui.animation;
import static org.junit.Assert.*;
import org.junit.Test;
public final class TimedAnimationTest {
    @Test public void cubicAnimationIsTimeBased(){TimedAnimation a=new TimedAnimation(100);long n=1000000000L;a.reset(0);a.start(0,20,n);assertEquals(0,a.value(n),0.001);assertTrue(a.value(n+50000000L)>10);assertEquals(20,a.value(n+100000000L),0.001);}
}
