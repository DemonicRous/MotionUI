package com.demonicrous.motionui.animation;
import com.demonicrous.motionui.config.MotionUIConfig.AnimationDirection;
import com.demonicrous.motionui.config.MotionUIConfig.AnimationStyle;
/** Pure transform calculation shared by opening and detached GUI-layer rendering. */
public final class AnimationTransform{
 public static final float MIN_SCALE=.94F;public final float translateX,translateY,scale;
 private AnimationTransform(float x,float y,float s){translateX=x;translateY=y;scale=s;}
 public static AnimationTransform opening(AnimationStyle s,AnimationDirection d,int o,double p){return calculate(s,d,o,1-clamp(p));}
 public static AnimationTransform closing(AnimationStyle s,AnimationDirection d,int o,double p){return calculate(s,d,o,clamp(p));}
 public static AnimationTransform comfortableClosing(AnimationStyle s,AnimationDirection d,int o,int duration,int width,int height,double p){double a=clamp(p);boolean horizontal=d==AnimationDirection.LEFT||d==AnimationDirection.RIGHT;double dimension=Math.max(1,horizontal?width:height),allowed=Math.max(1,Math.min(dimension*.025D,Math.max(1,duration)*.08D)),distance=s.hasSlide()?Math.min(Math.max(0,o),allowed)*a:0,x=0,y=0;if(d==AnimationDirection.UP)y=distance;else if(d==AnimationDirection.DOWN)y=-distance;else if(d==AnimationDirection.LEFT)x=distance;else if(d==AnimationDirection.RIGHT)x=-distance;double scaleDepth=.015D*Math.min(1D,Math.max(1,duration)/120D);float scale=s.hasScale()?(float)(1-scaleDepth*a):1F;return new AnimationTransform((float)x,(float)y,scale);}
 private static AnimationTransform calculate(AnimationStyle s,AnimationDirection d,int o,double a){float n=s.hasSlide()?(float)(Math.max(0,o)*a):0,x=0,y=0;if(d==AnimationDirection.UP)y=n;else if(d==AnimationDirection.DOWN)y=-n;else if(d==AnimationDirection.LEFT)x=n;else if(d==AnimationDirection.RIGHT)x=-n;float z=s.hasScale()?(float)(1-(1-MIN_SCALE)*a):1;return new AnimationTransform(x,y,z);}
 private static double clamp(double p){return Math.max(0,Math.min(1,p));}
}
