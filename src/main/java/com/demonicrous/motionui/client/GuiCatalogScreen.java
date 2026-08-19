package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;

public final class GuiCatalogScreen extends GuiScreen {
    private final GuiScreen parent; private final List<GuiCatalog.Entry> visible=new ArrayList<GuiCatalog.Entry>();
    private String query="",mod="all"; private int offset;
    public GuiCatalogScreen(GuiScreen parent){this.parent=parent;}
    public void initGui(){rebuild();}
    private void rebuild(){visible.clear();for(GuiCatalog.Entry e:GuiCatalog.entries())if(("all".equals(mod)||mod.equals(e.modId))&&(query.isEmpty()||e.className.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))))visible.add(e);Collections.sort(visible,new Comparator<GuiCatalog.Entry>(){public int compare(GuiCatalog.Entry a,GuiCatalog.Entry b){int c=a.modId.compareToIgnoreCase(b.modId);return c!=0?c:a.className.compareToIgnoreCase(b.className);}});offset=Math.max(0,Math.min(offset,Math.max(0,visible.size()-rows())));buttons();}
    private int rows(){return Math.max(1,(height-92)/22);}
    private void buttons(){buttonList.clear();buttonList.add(new GuiButton(1,8,28,120,20,I18n.format("motionui.catalog.filter",mod)));buttonList.add(new GuiButton(2,width-88,28,80,20,"Done"));for(int i=0;i<rows()&&offset+i<visible.size();i++){GuiCatalog.Entry e=visible.get(offset+i);String simple=e.className.substring(e.className.lastIndexOf('.')+1);String mark=e.observed?"* ":"  ";buttonList.add(new GuiButton(100+i,8,56+i*22,width-16,20,mark+"["+e.modId+"] "+simple+" — "+MotionUIConfig.policy(e.className)));}}
    protected void actionPerformed(GuiButton b)throws IOException{if(b.id==2){mc.displayGuiScreen(parent);return;}if(b.id==1){List<String> ids=new ArrayList<String>();ids.add("all");for(GuiCatalog.Entry e:GuiCatalog.entries())if(!ids.contains(e.modId))ids.add(e.modId);Collections.sort(ids);int i=ids.indexOf(mod);mod=ids.get((i+1)%ids.size());rebuild();return;}if(b.id>=100){int i=offset+b.id-100;if(i<visible.size()){MotionUIConfig.cycle(visible.get(i).className);buttons();}}}
    protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}if(key==14&&!query.isEmpty())query=query.substring(0,query.length()-1);else if(ChatAllowedCharacters.isAllowedCharacter(c))query+=c;rebuild();}
    public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=org.lwjgl.input.Mouse.getEventDWheel();if(wheel!=0){offset+=wheel<0?1:-1;rebuild();}}
    public void drawScreen(int x,int y,float partial){drawDefaultBackground();drawCenteredString(fontRenderer,I18n.format("motionui.catalog.title"),width/2,8,0xFFFFFF);drawString(fontRenderer,I18n.format("motionui.catalog.search",query+"_"),136,34,0xFFFFFF);if(visible.isEmpty())drawCenteredString(fontRenderer,I18n.format("motionui.catalog.empty"),width/2,height/2,0xAAAAAA);super.drawScreen(x,y,partial);}
    public boolean doesGuiPauseGame(){return false;}
}
