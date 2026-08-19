package com.demonicrous.motionui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public final class MotionUIClientEvents {
    private final KeyBinding catalog=new KeyBinding("key.motionui.catalog",Keyboard.KEY_F8,"key.categories.motionui");
    public MotionUIClientEvents(){ClientRegistry.registerKeyBinding(catalog);}
    @SubscribeEvent public void key(InputEvent.KeyInputEvent e){if(catalog.isPressed())Minecraft.getMinecraft().displayGuiScreen(new GuiCatalogScreen(Minecraft.getMinecraft().currentScreen));}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&catalog.isPressed())Minecraft.getMinecraft().displayGuiScreen(new GuiCatalogScreen(Minecraft.getMinecraft().currentScreen));}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){HotbarAnimationController.reset();}
}
