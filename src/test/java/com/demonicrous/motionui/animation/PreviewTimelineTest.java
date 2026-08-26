package com.demonicrous.motionui.animation;
import static org.junit.Assert.*;
import org.junit.Test;
public final class PreviewTimelineTest{
 @Test public void replayUsesElapsedTimeAndClamps(){PreviewTimeline t=new PreviewTimeline(100,1000);assertEquals(0D,t.progress(1000),0D);assertEquals(.5D,t.progress(50001000),.00001D);assertEquals(1D,t.progress(200001000),0D);}
 @Test public void scrubIsStableUntilResumed(){PreviewTimeline t=new PreviewTimeline(100,0);t.scrub(.75D);assertEquals(.75D,t.progress(Long.MAX_VALUE),0D);t.resume(1000000000L);assertFalse(t.isScrubbing());assertEquals(.75D,t.progress(1000000000L),.00001D);}
 @Test public void zeroDurationCompletes(){PreviewTimeline t=new PreviewTimeline(0,10);assertEquals(1D,t.progress(10),0D);}
}
