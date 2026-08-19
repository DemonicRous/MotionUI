package com.demonicrous.motionui;

import com.demonicrous.motionui.client.*;
import com.demonicrous.motionui.config.MotionUIConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.FMLCommonHandler;

@Mod(modid=MotionUI.MOD_ID,name="MotionUI",version=MotionUI.VERSION,acceptedMinecraftVersions="[1.12.2]",dependencies="required-after:forge@[14.23.5.2847,)",guiFactory="com.demonicrous.motionui.client.MotionUIGuiFactory",clientSideOnly=true)
public final class MotionUI {
    public static final String MOD_ID="motionui",VERSION="@VERSION@";
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent e){MotionUIConfig.load(e.getSuggestedConfigurationFile());ScreenAnimationEvents screens=new ScreenAnimationEvents();MotionUIClientEvents client=new MotionUIClientEvents();MinecraftForge.EVENT_BUS.register(screens);MinecraftForge.EVENT_BUS.register(client);FMLCommonHandler.instance().bus().register(client);}
    @Mod.EventHandler public void init(FMLInitializationEvent e){GuiCatalog.scanAsync();}
}
