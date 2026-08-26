package com.demonicrous.motionui.config;
import static org.junit.Assert.assertEquals;
import java.util.Map;
import org.junit.Test;
public final class MotionUIConfigMigrationTest {
 @Test public void legacyClassRulesDefaultToSlideUp(){Map.Entry<String,MotionUIConfig.ScreenRule>oe=MotionUIConfig.parseScreenRule("example.LegacyGui|160|10|EASE_OUT_CUBIC");Map.Entry<String,MotionUIConfig.ClosingRule>ce=MotionUIConfig.parseClosingRule("example.LegacyGui|SIMPLIFIED|120|6|EASE_OUT_CUBIC");assertEquals(MotionUIConfig.AnimationStyle.SLIDE,oe.getValue().style);assertEquals(MotionUIConfig.AnimationDirection.UP,oe.getValue().direction);assertEquals(MotionUIConfig.AnimationStyle.SLIDE,ce.getValue().style);assertEquals(MotionUIConfig.AnimationDirection.UP,ce.getValue().direction);}
 @Test public void extendedRulesLoadStyleAndDirection(){MotionUIConfig.ScreenRule r=MotionUIConfig.parseScreenRule("example.NewGui|200|14|EASE_OUT_QUINT|SCALE_SLIDE|RIGHT").getValue();assertEquals(MotionUIConfig.AnimationStyle.SCALE_SLIDE,r.style);assertEquals(MotionUIConfig.AnimationDirection.RIGHT,r.direction);}
}
