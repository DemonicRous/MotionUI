package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Container-only opening animation. Any input completes it before vanilla handles that input. */
public final class ScreenAnimationEvents {
    private GuiContainer screen; private long opened; private boolean pushed;
    @SubscribeEvent public void opened(GuiOpenEvent e){if(e.getGui()!=null)GuiCatalog.observe(e.getGui().getClass());screen=e.getGui() instanceof GuiContainer&&MotionUIConfig.containers&&MotionUIConfig.animationAllowed(e.getGui().getClass())?(GuiContainer)e.getGui():null;opened=screen==null?0:System.nanoTime();}
    @SubscribeEvent public void pre(GuiScreenEvent.DrawScreenEvent.Pre e){if(e.getGui()!=screen||pushed)return;float y=offset();if(y<=0){screen=null;return;}GlStateManager.pushMatrix();GlStateManager.translate(0,y,0);pushed=true;}
    @SubscribeEvent public void post(GuiScreenEvent.DrawScreenEvent.Post e){if(pushed){GlStateManager.popMatrix();pushed=false;}}
    @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre e){finish(e.getGui());}
    @SubscribeEvent public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre e){finish(e.getGui());}
    private void finish(Object gui){if(gui==screen){screen=null;opened=0;}}
    private float offset(){if(MotionUIConfig.containerDuration<=0)return 0;double p=Math.min(1,(System.nanoTime()-opened)/(MotionUIConfig.containerDuration*1000000D));double r=1-p;return(float)(MotionUIConfig.containerOffset*r*r*r);}
}
