package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;

/** Per-screen animation editor opened by the gear in the GUI catalog. */
public final class GuiScreenRuleEditor extends GuiScreen {
    private final GuiScreen parent; private final GuiCatalog.Entry entry;
    private int duration,offset; private MotionUIConfig.ScreenEasing easing;
    public GuiScreenRuleEditor(GuiScreen parent,GuiCatalog.Entry entry){this.parent=parent;this.entry=entry;MotionUIConfig.ScreenRule r=MotionUIConfig.rule(entry.className);duration=r.duration;offset=r.offset;easing=r.easing;}
    public void initGui(){refresh();}
    private void refresh(){buttonList.clear();int x=width/2-120,y=height/2-74;buttonList.add(new GuiButton(1,x,y,240,20,I18n.format("motionui.editor.state",I18n.format("motionui.policy."+MotionUIConfig.policy(entry.className).name().toLowerCase()))));buttonList.add(new GuiButton(2,x,y+24,38,20,"-20"));buttonList.add(new GuiButton(3,x+42,y+24,156,20,I18n.format("motionui.editor.duration",duration)));buttonList.add(new GuiButton(4,x+202,y+24,38,20,"+20"));buttonList.add(new GuiButton(5,x,y+48,38,20,"-2"));buttonList.add(new GuiButton(6,x+42,y+48,156,20,I18n.format("motionui.editor.offset",offset)));buttonList.add(new GuiButton(7,x+202,y+48,38,20,"+2"));buttonList.add(new GuiButton(8,x,y+72,240,20,I18n.format("motionui.editor.easing",I18n.format("motionui.easing."+easing.name().toLowerCase()))));buttonList.add(new GuiButton(9,x,y+104,116,20,I18n.format("motionui.editor.reset")));buttonList.add(new GuiButton(10,x+124,y+104,116,20,I18n.format("gui.done")));}
    protected void actionPerformed(GuiButton b)throws IOException{if(b.id==1)MotionUIConfig.cycle(entry.className);else if(b.id==2)duration=Math.max(0,duration-20);else if(b.id==4)duration=Math.min(600,duration+20);else if(b.id==5)offset=Math.max(0,offset-2);else if(b.id==7)offset=Math.min(48,offset+2);else if(b.id==8){MotionUIConfig.ScreenEasing[] v=MotionUIConfig.ScreenEasing.values();easing=v[(easing.ordinal()+1)%v.length];}else if(b.id==9){MotionUIConfig.resetRule(entry.className);MotionUIConfig.ScreenRule r=MotionUIConfig.rule(entry.className);duration=r.duration;offset=r.offset;easing=r.easing;}else if(b.id==10){MotionUIConfig.setRule(entry.className,duration,offset,easing);mc.displayGuiScreen(parent);return;}if(b.id>=2&&b.id<=8)MotionUIConfig.setRule(entry.className,duration,offset,easing);refresh();}
    protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,key);}
    public void drawScreen(int mx,int my,float partial){drawDefaultBackground();drawCenteredString(fontRenderer,I18n.format("motionui.editor.title"),width/2,height/2-112,0xFFFFFF);drawCenteredString(fontRenderer,entry.modId+" · "+entry.className,width/2,height/2-96,0xAAAAAA);super.drawScreen(mx,my,partial);}
    public boolean doesGuiPauseGame(){return false;}
}
