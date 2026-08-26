package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Global defaults built from the shared frame, navigation, cards and motion selector. */
public final class GuiMotionUISettings extends GuiScreen implements ClosingFlattenedRegion {
    private final MotionUITooltipPanel panel = new MotionUITooltipPanel();
    private final GuiScreen parent;
    private MotionUILayout.Rect frame, nav, profile, body, footer;
    private MotionUILayout.Rect openingCard, closingCard;
    private GuiButton profileButton, closingModeButton, debugButton;

    public GuiMotionUISettings(GuiScreen parent) { this.parent = parent; }

    @Override public MotionUILayout.Rect closingFlattenedRegion() { return frame; }

    @Override public void initGui() { refresh(); }

    private void measure() {
        boolean narrow = width - MotionUILayout.S16 < 620;
        frame = MotionUILayout.frame(width, height, 780, narrow ? 520 : 360);
        MotionUILayout.Rect inner = frame.inset(MotionUILayout.S12);
        nav = new MotionUILayout.Rect(inner.x, frame.y + 30, inner.width, MotionUILayout.TAB);
        profile = new MotionUILayout.Rect(inner.x, nav.bottom() + MotionUILayout.S8, inner.width, 26);
        footer = new MotionUILayout.Rect(inner.x, frame.bottom() - MotionUILayout.S12 - MotionUILayout.FOOTER,
                inner.width, MotionUILayout.FOOTER);
        int bodyTop = profile.bottom() + MotionUILayout.S16;
        body = new MotionUILayout.Rect(inner.x, bodyTop, inner.width,
                footer.y - MotionUILayout.S16 - bodyTop);
        MotionUILayout.Rect[] cards = MotionUILayout.twoColumnOrStack(body, MotionUILayout.S16, 290);
        openingCard = cards[0]; closingCard = cards[1];
    }

    private void refresh() {
        buttonList.clear();
        measure();
        MotionUINavigation.add(buttonList, nav, 1);
        MotionUILayout.Rect[]topControls=MotionUILayout.tracks(profile,2,MotionUILayout.S8);
        profileButton = new MotionUIButton(1, topControls[0].x, topControls[0].y, topControls[0].width, topControls[0].height, profileText());
        debugButton=new MotionUIButton(4,topControls[1].x,topControls[1].y,topControls[1].width,topControls[1].height,debugText(),MotionUIConfig.modernDebug?MotionUIButton.Variant.PRIMARY:MotionUIButton.Variant.SECONDARY);
        buttonList.add(profileButton);buttonList.add(debugButton);
        addCardControls(openingCard, true);
        addCardControls(closingCard, false);
        buttonList.add(new MotionUIButton(3, footer.x, footer.y, footer.width, footer.height,
                I18n.format("gui.done"), MotionUIButton.Variant.PRIMARY));
    }

    private void addCardControls(MotionUILayout.Rect card, boolean opening) {
        MotionUILayout.Rect pad = padBounds(card);
        MotionUIConfig.AnimationStyle style = opening ? MotionUIConfig.containerStyle : MotionUIConfig.closingStyle;
        MotionUIConfig.AnimationDirection direction = opening ? MotionUIConfig.containerDirection : MotionUIConfig.closingDirection;
        new MotionUIMotionPad(pad, opening ? 40 : 50, style, direction, !opening).addButtons(buttonList);
        if (!opening) {
            int infoRight = Math.max(card.x + 120, pad.x - MotionUILayout.S8);
            closingModeButton = new MotionUIButton(2, card.x + MotionUILayout.S12, card.y + 34,
                    Math.max(96, infoRight - card.x - MotionUILayout.S16), MotionUILayout.CONTROL, closingModeText());
            buttonList.add(closingModeButton);
        }
    }

    private MotionUILayout.Rect padBounds(MotionUILayout.Rect card) {
        int size = Math.min(104, Math.max(78, card.height - 44));
        return new MotionUILayout.Rect(card.right() - size - MotionUILayout.S12,
                card.y + (card.height - size) / 2 + 5, size, size);
    }

    private String profileText() {
        return I18n.format("motionui.settings.profile",
                I18n.format("motionui.profile." + MotionUIConfig.profile().name().toLowerCase(Locale.ROOT)));
    }

    private String closingModeText() {
        return I18n.format("motionui.settings.closingMode",
                I18n.format("motionui.closingMode." + MotionUIConfig.closingMode().name().toLowerCase(Locale.ROOT)));
    }

    private String debugText(){String value=MotionUIConfig.modernDebug?I18n.format("motionui.debug.profile."+MotionUIConfig.debugProfile().name().toLowerCase(Locale.ROOT)):I18n.format("options.off");return I18n.format("motionui.settings.modernF3",value);}

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 20 || button.id == 3) mc.displayGuiScreen(parent);
        else if (button.id == 22) mc.displayGuiScreen(new GuiDiagnosticsScreen(parent));
        else if (button.id == 1) { MotionUIConfig.cycleProfile(); refresh(); }
        else if (button.id == 2) { MotionUIConfig.cycleClosingMode(); refresh(); }
        else if(button.id==4){MotionUIConfig.cycleDebugMode();refresh();}
        else if (button.id >= 40 && button.id <= 44) { changeMotion(button.id, true); refresh(); }
        else if (button.id >= 50 && button.id <= 54) { changeMotion(button.id, false); refresh(); }
    }

    private void changeMotion(int id, boolean opening) {
        int first = opening ? 40 : 50;
        MotionUIConfig.AnimationStyle style = opening ? MotionUIConfig.containerStyle : MotionUIConfig.closingStyle;
        MotionUIConfig.AnimationDirection direction = opening ? MotionUIConfig.containerDirection : MotionUIConfig.closingDirection;
        if (id == first + 4) style = MotionUIStyleLogic.toggleScale(style);
        else {
            MotionUIConfig.AnimationDirection selected = MotionUIStyleLogic.directionFor(id, first);
            style = MotionUIStyleLogic.toggleSlide(style, direction == selected);
            direction = selected;
        }
        if (opening) MotionUIConfig.setContainerMotion(style, direction);
        else MotionUIConfig.setClosingMotion(style, direction);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            if (over(profileButton, mouseX, mouseY)) { MotionUIConfig.cycleProfile(-1); refresh(); return; }
            if (over(closingModeButton, mouseX, mouseY)) { MotionUIConfig.cycleClosingMode(-1); refresh(); return; }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        panel.drawPanel(frame.x, frame.y, frame.right(), frame.bottom());
        drawCenteredString(fontRenderer, I18n.format("motionui.settings.title"), frame.centerX(), frame.y + 10,
                MotionUITheme.TEXT);
        drawMotionCard(openingCard, true);
        drawMotionCard(closingCard, false);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (over(profileButton, mouseX, mouseY)) {
            TextTooltip.draw(fontRenderer, I18n.format("motionui.settings.profileHint.title"),
                    I18n.format("motionui.settings.profileHint"), mouseX, mouseY, width, height);
        } else if (over(closingModeButton, mouseX, mouseY)) {
            String key = "motionui.settings.closingModeHint."
                    + MotionUIConfig.closingMode().name().toLowerCase(Locale.ROOT);
            TextTooltip.draw(fontRenderer, I18n.format(key + ".title"), I18n.format(key),
                    mouseX, mouseY, width, height);
        } else if(over(debugButton,mouseX,mouseY)){
            TextTooltip.draw(fontRenderer,I18n.format("motionui.settings.modernF3Hint.title"),I18n.format("motionui.settings.modernF3Hint"),mouseX,mouseY,width,height);
        }
    }

    private void drawMotionCard(MotionUILayout.Rect card, boolean opening) {
        MotionUITheme.card(this, card);
        String title = I18n.format(opening ? "motionui.editor.opening" : "motionui.editor.closing");
        drawString(fontRenderer, "§e" + title, card.x + MotionUILayout.S12, card.y + 10, MotionUITheme.TEXT);
        MotionUILayout.Rect pad = padBounds(card);
        int infoX = card.x + MotionUILayout.S12;
        int infoTop = card.y + (opening ? 40 : 64);
        MotionUIConfig.AnimationStyle style = opening ? MotionUIConfig.containerStyle : MotionUIConfig.closingStyle;
        int duration = opening ? MotionUIConfig.containerDuration : MotionUIConfig.closingDuration;
        int offset = opening ? MotionUIConfig.containerOffset : MotionUIConfig.closingOffset;
        drawString(fontRenderer, I18n.format("motionui.style." + style.name().toLowerCase(Locale.ROOT)),
                infoX, infoTop, MotionUITheme.TEXT);
        drawString(fontRenderer, duration + " ms  ·  " + offset + " px", infoX, infoTop + 16,
                MotionUITheme.TEXT_MUTED);
        String hint = I18n.format("motionui.settings.padHint");
        int hintWidth = Math.max(60, pad.x - infoX - MotionUILayout.S8);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(hint, hintWidth), infoX,
                card.bottom() - 18, MotionUITheme.TEXT_FAINT);
    }

    private static boolean over(GuiButton button, int x, int y) {
        return button != null && x >= button.x && x < button.x + button.width
                && y >= button.y && y < button.y + button.height;
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
