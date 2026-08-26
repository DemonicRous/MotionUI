package com.demonicrous.motionui.client;

import com.demonicrous.motionui.animation.PreviewTimeline;
import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Compact-layout animation preview with replay and draggable progress. */
final class GuiAnimationPreviewScreen extends GuiScreen implements ClosingFlattenedRegion {
    private final GuiScreen parent;private final MotionUIConfig.ScreenRule opening;private final MotionUIConfig.ClosingRule closing;private final boolean closingMode;
    private MotionUILayout.Rect frame,stage,scrubber;private PreviewTimeline timeline;private boolean dragging;
    GuiAnimationPreviewScreen(GuiScreen parent,MotionUIConfig.ScreenRule opening,MotionUIConfig.ClosingRule closing,boolean closingMode){this.parent=parent;this.opening=opening;this.closing=closing;this.closingMode=closingMode;timeline=new PreviewTimeline(duration(),System.nanoTime());}
    @Override public MotionUILayout.Rect closingFlattenedRegion(){return frame;}
    @Override public void initGui(){frame=MotionUILayout.frame(width,height,520,330);stage=frame.inset(16,42,16,62);scrubber=new MotionUILayout.Rect(stage.x,stage.bottom()+12,stage.width,8);buttonList.clear();MotionUILayout.Rect footer=new MotionUILayout.Rect(frame.x+16,frame.bottom()-38,frame.width-32,22);MotionUILayout.Rect[]buttons=MotionUILayout.tracks(footer,2,8);buttonList.add(new MotionUIButton(1,buttons[0].x,buttons[0].y,buttons[0].width,buttons[0].height,I18n.format("motionui.editor.replay")));buttonList.add(new MotionUIButton(2,buttons[1].x,buttons[1].y,buttons[1].width,buttons[1].height,I18n.format("gui.done"),MotionUIButton.Variant.PRIMARY));}
    private int duration(){return closingMode?closing.duration:opening.duration;}
    @Override protected void actionPerformed(GuiButton button)throws IOException{if(button.id==1)timeline.replay(duration(),System.nanoTime());else if(button.id==2)mc.displayGuiScreen(parent);}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{if(scrubber.contains(x,y)){dragging=true;scrub(x);return;}super.mouseClicked(x,y,button);}
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed){if(dragging){scrub(x);return;}super.mouseClickMove(x,y,button,elapsed);}
    @Override protected void mouseReleased(int x,int y,int state){if(dragging){scrub(x);dragging=false;return;}super.mouseReleased(x,y,state);}
    private void scrub(int x){timeline.scrub((x-scrubber.x)/(double)Math.max(1,scrubber.width));}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,key);}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){drawDefaultBackground();new MotionUITooltipPanel().drawPanel(frame.x,frame.y,frame.right(),frame.bottom());drawCenteredString(fontRenderer,I18n.format("motionui.editor.preview"),frame.centerX(),frame.y+12,MotionUITheme.TEXT);double p=timeline.progress(System.nanoTime());AnimationPreviewRenderer.draw(this,stage,opening,closing,closingMode,p);drawRect(scrubber.x,scrubber.y,scrubber.right(),scrubber.bottom(),MotionUITheme.BORDER_SUBTLE);drawRect(scrubber.x,scrubber.y,scrubber.x+(int)(scrubber.width*p),scrubber.bottom(),MotionUITheme.ACCENT);super.drawScreen(mouseX,mouseY,partialTicks);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
