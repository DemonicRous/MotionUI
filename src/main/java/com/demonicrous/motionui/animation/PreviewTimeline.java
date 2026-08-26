package com.demonicrous.motionui.animation;

/** Frame-rate-independent replay/scrub state for the animation editor preview. */
public final class PreviewTimeline {
    private long startedNanos; private int durationMillis; private double scrubbedProgress; private boolean scrubbing;
    public PreviewTimeline(int durationMillis,long now){replay(durationMillis,now);}
    public void replay(int duration,long now){durationMillis=Math.max(0,duration);startedNanos=now;scrubbedProgress=0D;scrubbing=false;}
    public void scrub(double progress){scrubbedProgress=clamp(progress);scrubbing=true;}
    public void resume(long now){startedNanos=now-(long)(scrubbedProgress*durationMillis*1000000D);scrubbing=false;}
    public double progress(long now){if(scrubbing)return scrubbedProgress;if(durationMillis<=0)return 1D;return clamp((now-startedNanos)/(durationMillis*1000000D));}
    public boolean isScrubbing(){return scrubbing;}
    private static double clamp(double value){return Math.max(0D,Math.min(1D,value));}
}
