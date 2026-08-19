package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.nio.FloatBuffer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Spring motion for nearby slots and a no-travel crossfade for long jumps. */
public final class HotbarAnimationController {
    private static final int LONG_JUMP_SLOTS=3;
    private static final long CROSSFADE_NANOS=90000000L;
    // LWJGL 2 requires room for the largest possible glGetFloat result even
    // when GL_CURRENT_COLOR itself returns only four values.
    private static final FloatBuffer CAPTURE_COLOR=BufferUtils.createFloatBuffer(16),RESTORE_COLOR=BufferUtils.createFloatBuffer(16);
    private static double displayed=Double.NaN,velocity,crossfadeFrom; private static int target; private static long last,crossfadeStarted;
    private static Gui drawer; private static int baseX,y,u,v,w,h,texture; private static float red=1,green=1,blue=1,alpha=1; private static boolean pending;
    private HotbarAnimationController() {}
    public static int adjustSelectorX(int x){
        long now=System.nanoTime();
        if(!MotionUIConfig.hotbar||Double.isNaN(displayed)||last==0||now-last>500000000L){displayed=crossfadeFrom=target=x;velocity=0;crossfadeStarted=0;last=now;return x;}
        advance(now);
        if(x!=target){int slots=Math.max(1,Math.round(Math.abs(x-target)/20F));if(slots>LONG_JUMP_SLOTS){crossfadeFrom=displayed;displayed=target=x;velocity=0;crossfadeStarted=now;}else target=x;}
        advance(now);return (int)Math.floor(displayed);
    }
    private static void advance(long now){double dt=Math.min(.05,Math.max(0,(now-last)/1000000000D));last=now;double omega=4.75/(Math.max(40,MotionUIConfig.hotbarDuration)/1000D);double delta=displayed-target,c2=velocity+omega*delta,decay=Math.exp(-omega*dt);displayed=target+(delta+c2*dt)*decay;velocity=(c2-omega*(delta+c2*dt))*decay;if(Math.abs(displayed-target)<.002&&Math.abs(velocity)<.02){displayed=target;velocity=0;}}
    public static void deferSelector(Gui gui,int x,int drawY,int textureX,int textureY,int width,int height){if(!MotionUIConfig.hotbar){pending=false;gui.drawTexturedModalRect(x,drawY,textureX,textureY,width,height);return;}adjustSelectorX(x);drawer=gui;baseX=x;y=drawY;u=textureX;v=textureY;w=width;h=height;texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);CAPTURE_COLOR.clear();GL11.glGetFloat(GL11.GL_CURRENT_COLOR,CAPTURE_COLOR);red=CAPTURE_COLOR.get(0);green=CAPTURE_COLOR.get(1);blue=CAPTURE_COLOR.get(2);alpha=CAPTURE_COLOR.get(3);pending=true;}
    static void renderDeferred(){
        if(!pending||drawer==null)return;pending=false;
        int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);boolean oldBlend=GL11.glIsEnabled(GL11.GL_BLEND),oldDepth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);RESTORE_COLOR.clear();GL11.glGetFloat(GL11.GL_CURRENT_COLOR,RESTORE_COLOR);
        try{GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);GlStateManager.disableDepth();GlStateManager.bindTexture(texture);double p=crossfadeStarted==0?1:Math.min(1,(System.nanoTime()-crossfadeStarted)/(double)CROSSFADE_NANOS);if(p<1)drawAt(crossfadeFrom,(float)(1-p));drawAt(displayed,(float)p);if(p>=1)crossfadeStarted=0;}
        finally{GlStateManager.color(RESTORE_COLOR.get(0),RESTORE_COLOR.get(1),RESTORE_COLOR.get(2),RESTORE_COLOR.get(3));GlStateManager.bindTexture(oldTexture);GlStateManager.tryBlendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);if(!oldBlend)GlStateManager.disableBlend();if(oldDepth)GlStateManager.enableDepth();else GlStateManager.disableDepth();}
    }
    private static void drawAt(double x,float opacity){GlStateManager.pushMatrix();try{GlStateManager.translate((float)(x-baseX),0,0);GlStateManager.color(red,green,blue,alpha*opacity);drawer.drawTexturedModalRect(baseX,y,u,v,w,h);}finally{GlStateManager.popMatrix();}}
    public static void reset(){displayed=Double.NaN;velocity=0;last=crossfadeStarted=0;pending=false;drawer=null;}
}
