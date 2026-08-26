package com.demonicrous.motionui.client;

import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

/** Shared three-tab navigation with deterministic fractional track rounding. */
final class MotionUINavigation {
    private MotionUINavigation() {}

    static void add(List<GuiButton> buttons, MotionUILayout.Rect bounds, int selected) {
        MotionUILayout.Rect[] tabs = MotionUILayout.tracks(bounds, 3, MotionUILayout.S4);
        buttons.add(new MotionUITabButton(20, tabs[0].x, tabs[0].y, tabs[0].width,
                I18n.format("motionui.catalog.tab.catalog"), selected == 0));
        buttons.add(new MotionUITabButton(21, tabs[1].x, tabs[1].y, tabs[1].width,
                I18n.format("motionui.catalog.tab.settings"), selected == 1));
        buttons.add(new MotionUITabButton(22, tabs[2].x, tabs[2].y, tabs[2].width,
                I18n.format("motionui.catalog.tab.diagnostics"), selected == 2));
    }
}
