package com.demonicrous.motionui.client;
import static org.junit.Assert.*;
import org.junit.Test;
public final class ModernDebugFpsSamplerTest{
 @Test public void reportsAverageAndLowFromFrameTimes(){ModernDebugOverlayEvents.FpsSampler s=new ModernDebugOverlayEvents.FpsSampler();s.sample(1L);s.sample(10000001L);s.sample(20000001L);assertEquals("100 / 100",s.summary());}
 @Test public void emptySamplerHasNoFabricatedValue(){assertEquals("\u2014",new ModernDebugOverlayEvents.FpsSampler().summary());}
}
