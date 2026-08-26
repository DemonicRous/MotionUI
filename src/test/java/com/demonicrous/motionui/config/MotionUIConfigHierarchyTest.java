package com.demonicrous.motionui.config;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public final class MotionUIConfigHierarchyTest {
    @Before public void resetConfig() { MotionUIConfig.resetForTests(); }

    @Test public void classOverridesModWhichOverridesGlobal() {
        MotionUIConfig.setModRule("example",210,18,MotionUIConfig.ScreenEasing.EASE_OUT_QUINT,
                MotionUIConfig.AnimationStyle.SCALE_SLIDE,MotionUIConfig.AnimationDirection.RIGHT);
        MotionUIConfig.ScreenRule inherited=MotionUIConfig.rule("example.Gui","example");
        assertEquals(210,inherited.duration);assertEquals(MotionUIConfig.RuleSource.MOD,MotionUIConfig.openingSource("example.Gui","example"));
        MotionUIConfig.setRule("example.Gui",90,2,MotionUIConfig.ScreenEasing.EASE_OUT_CUBIC,
                MotionUIConfig.AnimationStyle.SLIDE,MotionUIConfig.AnimationDirection.DOWN);
        assertEquals(90,MotionUIConfig.rule("example.Gui","example").duration);
        assertEquals(MotionUIConfig.RuleSource.CLASS,MotionUIConfig.openingSource("example.Gui","example"));
        MotionUIConfig.resetOpening("example.Gui");
        assertEquals(210,MotionUIConfig.rule("example.Gui","example").duration);
    }

    @Test public void modPolicyIsInheritedAndClassCanOverrideIt() {
        MotionUIConfig.setModPolicy("example",MotionUIConfig.Policy.DISABLED);
        assertFalse(MotionUIConfig.animationAllowed("example.Gui","example"));
        MotionUIConfig.setPolicy("example.Gui",MotionUIConfig.Policy.ENABLED);
        assertTrue(MotionUIConfig.animationAllowed("example.Gui","example"));
    }

    @Test public void bulkApplyCopiesValuesToEveryClass() {
        MotionUIConfig.ScreenRule source=new MotionUIConfig.ScreenRule(180,12,MotionUIConfig.ScreenEasing.EASE_IN_OUT_CUBIC,
                MotionUIConfig.AnimationStyle.SCALE,MotionUIConfig.AnimationDirection.LEFT);
        assertEquals(2,MotionUIConfig.applyOpening(Arrays.asList("a.Gui","b.Gui"),source,MotionUIConfig.Policy.ENABLED));
        assertEquals(180,MotionUIConfig.rule("a.Gui").duration);assertEquals(MotionUIConfig.AnimationStyle.SCALE,MotionUIConfig.rule("b.Gui").style);
    }

    @Test public void debugProfilesCycleAndRemainEnabled(){assertFalse(MotionUIConfig.modernDebug);MotionUIConfig.cycleDebugMode();assertEquals(MotionUIConfig.DebugProfile.COMPACT,MotionUIConfig.debugProfile());MotionUIConfig.cycleDebugProfile();assertEquals(MotionUIConfig.DebugProfile.STANDARD,MotionUIConfig.debugProfile());assertTrue(MotionUIConfig.modernDebug);}
}
