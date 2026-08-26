package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import com.demonicrous.motionui.animation.AnimationTransform;
import com.demonicrous.motionui.animation.AnimationEasing;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Container-only opening animation. Any input completes it before vanilla handles that input. */
public final class ScreenAnimationEvents {
    private static final boolean JEI_LOADED = Loader.isModLoaded("jei");
    private GuiScreen screen; private long opened; private boolean pushed; private MotionUIConfig.ScreenRule rule;
    @SubscribeEvent public void opened(GuiOpenEvent e){
        GuiScreen gui=e.getGui();
        if(gui!=null)GuiCatalog.observe(gui.getClass());
        String name=gui==null?null:gui.getClass().getName(),mod=name==null?null:GuiCatalog.modId(name);
        boolean forced=gui!=null&&MotionUIConfig.policy(name,mod)==MotionUIConfig.Policy.ENABLED;
        boolean container=(gui instanceof GuiContainer||isJeiRecipeScreen(gui))&&MotionUIConfig.containers&&MotionUIConfig.animationAllowed(name,mod);
        screen=forced||container?gui:null;
        rule=screen==null?null:MotionUIConfig.rule(name,mod);
        opened=screen==null?0:System.nanoTime();
    }
    // JEI 1.12 draws its ingredient list from background and, in some builds,
    // post-screen events. Start after its normal-priority background handler
    // and finish before its normal-priority post handler, leaving the overlay fixed.
    @SubscribeEvent(priority=EventPriority.LOWEST) public void background(GuiScreenEvent.BackgroundDrawnEvent e){if(e.getGui()!=screen||pushed)return;double p=progress();if(p>=1){screen=null;return;}AnimationTransform t=AnimationTransform.opening(rule.style,rule.direction,rule.offset,ease(p));GlStateManager.pushMatrix();try{GlStateManager.translate(t.translateX,t.translateY,0);if(t.scale!=1F){GlStateManager.translate(e.getGui().width/2F,e.getGui().height/2F,0);GlStateManager.scale(t.scale,t.scale,1);GlStateManager.translate(-e.getGui().width/2F,-e.getGui().height/2F,0);}pushed=true;}catch(RuntimeException failure){GlStateManager.popMatrix();throw failure;}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void post(GuiScreenEvent.DrawScreenEvent.Post e){if(pushed){GlStateManager.popMatrix();pushed=false;}}
    @SubscribeEvent public void mouse(GuiScreenEvent.MouseInputEvent.Pre e){if(Mouse.getEventButton()>=0&&Mouse.getEventButtonState()||Mouse.getEventDWheel()!=0)finish(e.getGui());}
    @SubscribeEvent public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre e){if(Keyboard.getEventKeyState())finish(e.getGui());}
    private void finish(Object gui){if(gui==screen){screen=null;opened=0;}}
    private static boolean isJeiRecipeScreen(GuiScreen gui){return JEI_LOADED&&gui!=null&&gui.getClass().getName().startsWith("mezz.jei.gui.recipes.");}
    private double progress(){if(rule==null||rule.duration<=0)return 1;return Math.max(0,Math.min(1,(System.nanoTime()-opened)/(rule.duration*1000000D)));}
    private double ease(double p){return AnimationEasing.opening(rule.easing,p);}
}
