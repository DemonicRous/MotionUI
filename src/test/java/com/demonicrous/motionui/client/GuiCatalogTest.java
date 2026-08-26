package com.demonicrous.motionui.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;

public final class GuiCatalogTest {
    @Test
    public void resolvesModContainerHierarchyWithoutMinecraftJar() {
        Map<String, String> supers = new HashMap<String, String>();
        supers.put("example.client.GuiMachine", "example.client.GuiMachineBase");
        supers.put("example.client.GuiMachineBase",
                "net.minecraft.client.gui.inventory.GuiContainer");

        assertTrue(GuiCatalog.isGuiHierarchy("example.client.GuiMachine", supers,
                new HashSet<String>()));
    }

    @Test
    public void rejectsUnrelatedHierarchyAndCycles() {
        Map<String, String> supers = new HashMap<String, String>();
        supers.put("example.NotGui", "java.lang.Object");
        supers.put("example.CycleA", "example.CycleB");
        supers.put("example.CycleB", "example.CycleA");

        assertFalse(GuiCatalog.isGuiHierarchy("example.NotGui", supers,
                new HashSet<String>()));
        assertFalse(GuiCatalog.isGuiHierarchy("example.CycleA", supers,
                new HashSet<String>()));
    }
}
