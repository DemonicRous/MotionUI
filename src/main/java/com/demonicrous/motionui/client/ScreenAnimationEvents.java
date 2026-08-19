package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Container-only opening animation. Any input completes it before vanilla handles that input. */
public final class ScreenAnimationEvents {
    private GuiScreen screen; private long opened; private boolean pushed; private MotionUIConfig.ScreenRule rule;
    @SubscribeEvent public void opened(GuiOpenEvent e){
        GuiScreen gui=e.getGui();
        if(gui!=null)GuiCatalog.observe(gui.getClass());
        boolean forced=gui!=null&&MotionUIConfig.policy(gui.getClass().getName())==MotionUIConfig.Policy.ENABLED;
        boolean container=gui instanceof GuiContainer&&MotionUIConfig.containers&&MotionUIConfig.animationAllowed(gui.getClass());
        screen=forced||container?gui:null;
        rule=screen==null?null:MotionUIConfig.rule(screen.getClass().getName());
        opened=screen==null?0:System.nanoTime();
    }
    @SubscribeEvent public void background(GuiScreenEvent.BackgroundDrawnEvent e){if(e.getGui()!=screen||pushed)return;float y=offset();if(y<=0){screen=null;return;}GlStateManager.pushMatrix();GlStateManager.translate(0,y,0);pushed=true;}
    @SubscribeEvent public void post(GuiScreenEvent.DrawScreenEvent.Post e){if(pushed){GlStateManager.popMatrix();pushed=false;}}
    @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre e){if(Mouse.getEventButton()>=0&&Mouse.getEventButtonState()||Mouse.getEventDWheel()!=0)finish(e.getGui());}
    @SubscribeEvent public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre e){if(Keyboard.getEventKeyState())finish(e.getGui());}
    private void finish(Object gui){if(gui==screen){screen=null;opened=0;}}
    private float offset(){if(rule==null||rule.duration<=0)return 0;double p=Math.max(0,Math.min(1,(System.nanoTime()-opened)/(rule.duration*1000000D)));double eased;if(rule.easing==MotionUIConfig.ScreenEasing.EASE_OUT_QUINT){double r=1-p;eased=1-r*r*r*r*r;}else if(rule.easing==MotionUIConfig.ScreenEasing.EASE_IN_OUT_CUBIC){eased=p<.5?4*p*p*p:1-Math.pow(-2*p+2,3)/2;}else{double r=1-p;eased=1-r*r*r;}return(float)(rule.offset*(1-eased));}
}
