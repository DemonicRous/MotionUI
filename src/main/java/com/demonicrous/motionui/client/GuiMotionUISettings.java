package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Global MotionUI settings kept separate from the per-screen catalog. */
public final class GuiMotionUISettings extends GuiScreen {
    private final GuiScreen parent;
    private GuiButton profileButton;
    private GuiButton closingModeButton;

    public GuiMotionUISettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        refresh();
    }

    private void refresh() {
        buttonList.clear();
        int x = width / 2 - 140;
        int y = height / 2 - 36;
        profileButton = new GuiButton(1, x, y, 280, 20, profileText());
        buttonList.add(profileButton);
        closingModeButton = new GuiButton(2, x, y + 28, 280, 20, closingModeText());
        buttonList.add(closingModeButton);
        buttonList.add(new GuiButton(3, x, y + 68, 280, 20, I18n.format("gui.done")));
    }

    private String profileText() {
        String key = "motionui.profile." + MotionUIConfig.profile().name().toLowerCase(Locale.ROOT);
        return I18n.format("motionui.settings.profile", I18n.format(key));
    }

    private String closingModeText() {
        String key = "motionui.closingMode." + MotionUIConfig.closingMode().name().toLowerCase(Locale.ROOT);
        return I18n.format("motionui.settings.closingMode", I18n.format(key));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            MotionUIConfig.cycleProfile();
            refresh();
        } else if (button.id == 2) {
            MotionUIConfig.cycleClosingMode();
            refresh();
        } else if (button.id == 3) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            if (over(profileButton, mouseX, mouseY)) {
                MotionUIConfig.cycleProfile(-1);
                refresh();
                return;
            }
            if (over(closingModeButton, mouseX, mouseY)) {
                MotionUIConfig.cycleClosingMode(-1);
                refresh();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private static boolean over(GuiButton button, int x, int y) {
        return button != null && x >= button.x && x < button.x + button.width
                && y >= button.y && y < button.y + button.height;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("motionui.settings.title"), width / 2, height / 2 - 78, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (profileButton != null && mouseX >= profileButton.x && mouseX < profileButton.x + profileButton.width
                && mouseY >= profileButton.y && mouseY < profileButton.y + profileButton.height) {
            TextTooltip.draw(fontRenderer, I18n.format("motionui.settings.profileHint.title"),
                    I18n.format("motionui.settings.profileHint"), mouseX, mouseY, width, height);
        } else if (closingModeButton != null && mouseX >= closingModeButton.x
                && mouseX < closingModeButton.x + closingModeButton.width
                && mouseY >= closingModeButton.y && mouseY < closingModeButton.y + closingModeButton.height) {
            String key = "motionui.settings.closingModeHint."
                    + MotionUIConfig.closingMode().name().toLowerCase(Locale.ROOT);
            TextTooltip.draw(fontRenderer, I18n.format(key + ".title"), I18n.format(key),
                    mouseX, mouseY, width, height);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
