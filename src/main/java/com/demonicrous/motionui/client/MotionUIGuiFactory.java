package com.demonicrous.motionui.client;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
public final class MotionUIGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft minecraftInstance){}
    public boolean hasConfigGui(){return true;}
    public GuiScreen createConfigGui(GuiScreen parentScreen){return new GuiCatalogScreen(parentScreen);}
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories(){return null;}
}
