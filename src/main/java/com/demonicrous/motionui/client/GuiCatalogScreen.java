package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;

public final class GuiCatalogScreen extends GuiScreen {
    private static final int HEADER=52, ROW=24, POLICY_WIDTH=116, GEAR_WIDTH=20;
    private final GuiScreen parent;
    private final List<GuiCatalog.Entry> visible=new ArrayList<GuiCatalog.Entry>();
    private GuiTextField search; private String mod="all"; private int offset,lastRevision=-1;
    public GuiCatalogScreen(GuiScreen parent){this.parent=parent;}
    private int left(){return Math.max(8,(width-1000)/2);}
    private int right(){return Math.min(width-8,left()+1000);}
    public void initGui(){search=new GuiTextField(3,fontRenderer,left()+128,27,Math.max(80,right()-left()-338),20);search.setMaxStringLength(120);rebuild();}
    private int rows(){return Math.max(1,(height-HEADER-34)/ROW);}
    private void rebuild(){lastRevision=GuiCatalog.revision();String q=search==null?"":search.getText().toLowerCase(Locale.ROOT);visible.clear();for(GuiCatalog.Entry e:GuiCatalog.entries())if(("all".equals(mod)||mod.equals(e.modId))&&(q.isEmpty()||e.className.toLowerCase(Locale.ROOT).contains(q)))visible.add(e);Collections.sort(visible,new Comparator<GuiCatalog.Entry>(){public int compare(GuiCatalog.Entry a,GuiCatalog.Entry b){int c=a.modId.compareToIgnoreCase(b.modId);return c!=0?c:a.className.compareToIgnoreCase(b.className);}});offset=Math.max(0,Math.min(offset,Math.max(0,visible.size()-rows())));buttons();}
    private void buttons(){buttonList.clear();buttonList.add(new GuiButton(1,left(),27,120,20,I18n.format("motionui.catalog.mod",mod)));buttonList.add(new GuiButton(2,right()-74,27,74,20,I18n.format("gui.done")));for(int i=0;i<rows()&&offset+i<visible.size();i++){GuiCatalog.Entry e=visible.get(offset+i);int y=HEADER+2+i*ROW;buttonList.add(new GuiButton(100+i,right()-POLICY_WIDTH-GEAR_WIDTH-3,y,POLICY_WIDTH,20,policyText(e.className)));buttonList.add(new GuiButton(1000+i,right()-GEAR_WIDTH,y,GEAR_WIDTH,20,"⚙"));}}
    private String policyText(String name){return I18n.format("motionui.policy."+MotionUIConfig.policy(name).name().toLowerCase(Locale.ROOT));}
    protected void actionPerformed(GuiButton b)throws IOException{if(b.id==2){mc.displayGuiScreen(parent);return;}if(b.id==1){List<String> ids=new ArrayList<String>();ids.add("all");for(GuiCatalog.Entry e:GuiCatalog.entries())if(!ids.contains(e.modId))ids.add(e.modId);if(ids.size()>1)Collections.sort(ids.subList(1,ids.size()));int i=ids.indexOf(mod);mod=ids.get((i+1)%ids.size());rebuild();return;}if(b.id>=1000){int i=offset+b.id-1000;if(i<visible.size())mc.displayGuiScreen(new GuiScreenRuleEditor(this,visible.get(i)));return;}if(b.id>=100){int i=offset+b.id-100;if(i<visible.size()){MotionUIConfig.cycle(visible.get(i).className);buttons();}}}
    protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}if(search.textboxKeyTyped(c,key))rebuild();}
    protected void mouseClicked(int x,int y,int button)throws IOException{search.mouseClicked(x,y,button);super.mouseClicked(x,y,button);}
    public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=org.lwjgl.input.Mouse.getEventDWheel();if(wheel!=0){offset+=wheel<0?3:-3;rebuild();}}
    public void updateScreen(){search.updateCursorCounter();if(lastRevision!=GuiCatalog.revision())rebuild();}
    public void drawScreen(int mouseX,int mouseY,float partial){drawDefaultBackground();drawRect(0,0,width,HEADER,0xCC101820);drawCenteredString(fontRenderer,I18n.format("motionui.catalog.title"),width/2,9,0xFFFFFF);search.drawTextBox();if(search.getText().isEmpty()&&!search.isFocused())drawString(fontRenderer,I18n.format("motionui.catalog.searchHint"),left()+133,34,0x777777);for(int i=0;i<rows()&&offset+i<visible.size();i++){GuiCatalog.Entry e=visible.get(offset+i);int y=HEADER+i*ROW;drawRect(left(),y,right(),y+ROW-1,(i&1)==0?0x77242C35:0x77303943);String marker=e.observed?"§a●§r":"§7○§r";drawString(fontRenderer,marker+" §b"+e.modId+"§r  "+shortName(e.className),left()+6,y+8,0xE0E0E0);}if(visible.isEmpty())drawCenteredString(fontRenderer,I18n.format("motionui.catalog.empty"),width/2,height/2,0xAAAAAA);if(!visible.isEmpty())drawString(fontRenderer,(offset+1)+"–"+Math.min(visible.size(),offset+rows())+" / "+visible.size(),left(),height-14,0x999999);super.drawScreen(mouseX,mouseY,partial);}
    private static String shortName(String n){int i=n.lastIndexOf('.');return i<0?n:n.substring(i+1);}
    public boolean doesGuiPauseGame(){return false;}
}
