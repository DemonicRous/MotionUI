package com.demonicrous.motionui.client;

import com.demonicrous.motionui.animation.AnimationEasing;
import com.demonicrous.motionui.animation.AnimationTransform;
import com.demonicrous.motionui.animation.ClosingComfort;
import com.demonicrous.motionui.config.MotionUIConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Isolated representative GUI preview; never instantiates third-party screens. */
final class AnimationPreviewRenderer {
    private AnimationPreviewRenderer() {}

    static void draw(Gui gui,MotionUILayout.Rect bounds,MotionUIConfig.ScreenRule opening,
                     MotionUIConfig.ClosingRule closing,boolean closingMode,double progress){
        MotionUITheme.card(gui,bounds);
        MotionUILayout.Rect stage=bounds.inset(8,24,8,24);
        int dimAlpha=closingMode?(int)((1D-progress)*150D):150;
        Gui.drawRect(stage.x,stage.y,stage.right(),stage.bottom(),(dimAlpha<<24)|0x101520);
        double eased=closingMode?AnimationEasing.closing(closing.easing,progress):AnimationEasing.opening(opening.easing,progress);
        AnimationTransform transform=closingMode
                ?AnimationTransform.comfortableClosing(closing.style,closing.direction,closing.offset,closing.duration,stage.width,stage.height,eased)
                :AnimationTransform.opening(opening.style,opening.direction,opening.offset,eased);
        int cardWidth=Math.max(60,Math.min(150,stage.width-24)),cardHeight=Math.max(42,Math.min(92,stage.height-20));
        int left=stage.centerX()-cardWidth/2,top=stage.centerY()-cardHeight/2;
        float opacity=closingMode?(float)ClosingComfort.opacity(progress,closing.duration):1F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(transform.translateX,transform.translateY,0D);
        GlStateManager.translate(stage.centerX(),stage.centerY(),0D);
        GlStateManager.scale(transform.scale,transform.scale,1D);
        GlStateManager.translate(-stage.centerX(),-stage.centerY(),0D);
        int alpha=Math.max(0,Math.min(255,(int)(opacity*238F)))<<24;
        Gui.drawRect(left,top,left+cardWidth,top+cardHeight,alpha|0x24132F);
        Gui.drawRect(left+8,top+9,left+cardWidth-8,top+10,alpha|0xFFD868);
        Gui.drawRect(left+12,top+22,left+cardWidth-12,top+30,alpha|0x5A2478);
        Gui.drawRect(left+12,top+36,left+cardWidth-34,top+44,alpha|0x302040);
        GlStateManager.popMatrix();
        // Representative optional JEI side region is deliberately stationary.
        int jeiWidth=Math.max(8,Math.min(22,stage.width/7));
        int jeiAlpha=closingMode?Math.max(0,(int)((1D-Math.min(1D,progress*2.4D))*190D)):190;
        Gui.drawRect(stage.right()-jeiWidth-4,stage.y+6,stage.right()-4,stage.bottom()-6,(jeiAlpha<<24)|0x3B2548);
        Gui.drawRect(stage.right()-jeiWidth,stage.y+12,stage.right()-8,stage.y+18,(jeiAlpha<<24)|0xFFD868);
    }
}
