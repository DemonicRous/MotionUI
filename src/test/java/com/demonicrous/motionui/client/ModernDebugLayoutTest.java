package com.demonicrous.motionui.client;
import static org.junit.Assert.*;
import org.junit.Test;
public final class ModernDebugLayoutTest{
 @Test public void columnsStayInsideNarrowScreen(){MotionUILayout.Rect[]c=ModernDebugLayout.columns(320,292,292);assertTrue(c[0].x>=0);assertTrue(c[1].right()<=320);assertTrue(c[0].right()<c[1].x);}
 @Test public void preferredWidthsArePreservedWhenTheyFit(){MotionUILayout.Rect[]c=ModernDebugLayout.columns(800,240,260);assertEquals(240,c[0].width);assertEquals(260,c[1].width);assertEquals(792,c[1].right());}
}
