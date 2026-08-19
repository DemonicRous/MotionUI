package com.demonicrous.motionui.animation;
public final class TimedAnimation {
    private double from, to; private long start, duration; private Easing easing=Easings.EASE_OUT_CUBIC;
    public TimedAnimation(long durationMillis) { setDuration(durationMillis); }
    public void setDuration(long ms) { duration=Math.max(1L,ms)*1000000L; }
    public void start(double f,double t,long now){from=f;to=t;start=now;}
    public double value(long now){ if(start==0)return to; double p=(now-start)/(double)duration; if(p>=1){start=0;return to;} return from+(to-from)*easing.apply(p); }
    public boolean isFinished(long now){return start==0||now-start>=duration;}
    public void finish(){start=0;from=to;}
    public void reset(double value){from=to=value;start=0;}
}
