package com.demonicrous.motionui.client;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
public final class HotbarOverlayEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void afterHotbar(RenderGameOverlayEvent.Post event){if(event.getType()==RenderGameOverlayEvent.ElementType.HOTBAR)HotbarAnimationController.renderDeferred();}
}
