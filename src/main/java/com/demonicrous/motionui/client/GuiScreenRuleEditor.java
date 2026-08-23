package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Compact per-GUI editor with real tabs and fixed header/footer regions. */
public final class GuiScreenRuleEditor extends GuiScreen {
    private static final int TAB_TOP = 54, TAB_HEIGHT = 20, CONTENT_TOP = 82;
    private final MotionUITooltipPanel panel = new MotionUITooltipPanel();
    private final GuiScreen parent;
    private final GuiCatalog.Entry entry;
    private boolean opening = true, closingInherited;
    private int openingDuration, openingOffset, closingDuration, closingOffset;
    private MotionUIConfig.ScreenEasing openingEasing, closingEasing;
    private MotionUIConfig.ClosingMode closingMode;

    public GuiScreenRuleEditor(GuiScreen parent, GuiCatalog.Entry entry) {
        this.parent = parent;
        this.entry = entry;
        MotionUIConfig.ScreenRule openingRule = MotionUIConfig.rule(entry.className);
        openingDuration = openingRule.duration;
        openingOffset = openingRule.offset;
        openingEasing = openingRule.easing;
        MotionUIConfig.ClosingRule closingRule = MotionUIConfig.closingRule(entry.className);
        closingDuration = closingRule.duration;
        closingOffset = closingRule.offset;
        closingEasing = closingRule.easing;
        closingMode = closingRule.mode;
        closingInherited = !MotionUIConfig.hasClosingRule(entry.className);
    }

    private int panelWidth() { return Math.min(420, width - 32); }
    private int panelLeft() { return (width - panelWidth()) / 2; }

    @Override
    public void initGui() { refresh(); }

    private void refresh() {
        buttonList.clear();
        int left = panelLeft(), contentWidth = panelWidth();
        int firstTabWidth = (contentWidth - 4) / 2;
        buttonList.add(new MotionUITabButton(70, left, TAB_TOP, firstTabWidth,
                I18n.format("motionui.editor.opening"), opening));
        buttonList.add(new MotionUITabButton(71, left + firstTabWidth + 4, TAB_TOP,
                contentWidth - firstTabWidth - 4, I18n.format("motionui.editor.closing"), !opening));
        if (opening) openingButtons(left, contentWidth); else closingButtons(left, contentWidth);
        int footerY = footerY();
        buttonList.add(new MotionUIButton(90, left, footerY, (contentWidth - 8) / 2, 20, I18n.format("motionui.editor.reset")));
        buttonList.add(new MotionUIButton(99, left + (contentWidth + 8) / 2, footerY, (contentWidth - 8) / 2, 20, I18n.format("gui.done")));
    }

    private boolean hasDetailedControls() {
        return opening ? openingPolicy() == MotionUIConfig.Policy.ENABLED
                : !closingInherited && closingMode != MotionUIConfig.ClosingMode.DISABLED;
    }

    private int footerY() { return CONTENT_TOP + (hasDetailedControls() ? 122 : 78); }

    private void openingButtons(int left, int contentWidth) {
        MotionUIConfig.Policy state = openingPolicy();
        buttonList.add(new MotionUIButton(10, left, CONTENT_TOP, contentWidth, 20,
                I18n.format("motionui.editor.state", I18n.format("motionui.policy." + state.name().toLowerCase(Locale.ROOT)))));
        if (state == MotionUIConfig.Policy.ENABLED) numbers(left, contentWidth, openingDuration, openingOffset, openingEasing);
    }

    private void closingButtons(int left, int contentWidth) {
        String state = closingInherited ? I18n.format("motionui.policy.default")
                : I18n.format("motionui.closingMode." + closingMode.name().toLowerCase(Locale.ROOT));
        buttonList.add(new MotionUIButton(20, left, CONTENT_TOP, contentWidth, 20, I18n.format("motionui.editor.state", state)));
        if (!closingInherited && closingMode != MotionUIConfig.ClosingMode.DISABLED)
            numbers(left, contentWidth, closingDuration, closingOffset, closingEasing);
    }

    private void numbers(int left, int contentWidth, int duration, int offset, MotionUIConfig.ScreenEasing easing) {
        int row = CONTENT_TOP + 42, small = 46;
        buttonList.add(new MotionUIButton(30, left, row, small, 20, "-20"));
        buttonList.add(new MotionUIButton(32, left + contentWidth - small, row, small, 20, "+20"));
        buttonList.add(new MotionUIButton(33, left, row + 26, small, 20, "-2"));
        buttonList.add(new MotionUIButton(35, left + contentWidth - small, row + 26, small, 20, "+2"));
        buttonList.add(new MotionUIButton(36, left, row + 52, contentWidth, 20,
                I18n.format("motionui.editor.easing", I18n.format("motionui.easing." + easing.name().toLowerCase(Locale.ROOT)))));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) {
            MotionUIConfig.Policy state = openingPolicy();
            if (state == MotionUIConfig.Policy.DEFAULT) MotionUIConfig.setPolicy(entry.className, MotionUIConfig.Policy.ENABLED);
            else if (state == MotionUIConfig.Policy.ENABLED) MotionUIConfig.setPolicy(entry.className, MotionUIConfig.Policy.DISABLED);
            else MotionUIConfig.resetOpening(entry.className);
        } else if (button.id == 20) {
            if (closingInherited) { closingInherited = false; closingMode = MotionUIConfig.ClosingMode.DISABLED; }
            else if (closingMode == MotionUIConfig.ClosingMode.DISABLED) closingMode = MotionUIConfig.ClosingMode.SIMPLIFIED;
            else if (closingMode == MotionUIConfig.ClosingMode.SIMPLIFIED) closingMode = MotionUIConfig.ClosingMode.FULL;
            else closingInherited = true;
            saveClosing();
        } else if (button.id == 70 || button.id == 71) {
            opening = button.id == 70;
        } else if (button.id >= 30 && button.id <= 36) change(button.id);
        else if (button.id == 90) resetCurrent();
        else if (button.id == 99) { mc.displayGuiScreen(parent); return; }
        refresh();
    }

    private void change(int id) {
        if (opening) {
            if (id == 30) openingDuration = Math.max(0, openingDuration - 20);
            if (id == 32) openingDuration = Math.min(600, openingDuration + 20);
            if (id == 33) openingOffset = Math.max(0, openingOffset - 2);
            if (id == 35) openingOffset = Math.min(48, openingOffset + 2);
            if (id == 36) openingEasing = next(openingEasing);
            MotionUIConfig.setRule(entry.className, openingDuration, openingOffset, openingEasing);
        } else {
            if (id == 30) closingDuration = Math.max(0, closingDuration - 20);
            if (id == 32) closingDuration = Math.min(600, closingDuration + 20);
            if (id == 33) closingOffset = Math.max(0, closingOffset - 2);
            if (id == 35) closingOffset = Math.min(48, closingOffset + 2);
            if (id == 36) closingEasing = next(closingEasing);
            saveClosing();
        }
    }

    private static MotionUIConfig.ScreenEasing next(MotionUIConfig.ScreenEasing value) {
        MotionUIConfig.ScreenEasing[] values = MotionUIConfig.ScreenEasing.values();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static MotionUIConfig.ScreenEasing previous(MotionUIConfig.ScreenEasing value) {
        MotionUIConfig.ScreenEasing[] values = MotionUIConfig.ScreenEasing.values();
        return values[(value.ordinal() + values.length - 1) % values.length];
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            GuiButton button = buttonAt(mouseX, mouseY);
            if (button != null && reverse(button.id)) {
                refresh();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private GuiButton buttonAt(int mouseX, int mouseY) {
        for (GuiButton button : buttonList)
            if (button.visible && button.enabled && mouseX >= button.x && mouseX < button.x + button.width
                    && mouseY >= button.y && mouseY < button.y + button.height) return button;
        return null;
    }

    private boolean reverse(int id) {
        if (id == 10) {
            MotionUIConfig.Policy state = openingPolicy();
            if (state == MotionUIConfig.Policy.DEFAULT) MotionUIConfig.setPolicy(entry.className, MotionUIConfig.Policy.DISABLED);
            else if (state == MotionUIConfig.Policy.DISABLED) MotionUIConfig.setPolicy(entry.className, MotionUIConfig.Policy.ENABLED);
            else MotionUIConfig.resetOpening(entry.className);
            return true;
        }
        if (id == 20) {
            if (closingInherited) { closingInherited = false; closingMode = MotionUIConfig.ClosingMode.FULL; }
            else if (closingMode == MotionUIConfig.ClosingMode.FULL) closingMode = MotionUIConfig.ClosingMode.SIMPLIFIED;
            else if (closingMode == MotionUIConfig.ClosingMode.SIMPLIFIED) closingMode = MotionUIConfig.ClosingMode.DISABLED;
            else closingInherited = true;
            saveClosing();
            return true;
        }
        if (id == 36) {
            if (opening) { openingEasing = previous(openingEasing); MotionUIConfig.setRule(entry.className, openingDuration, openingOffset, openingEasing); }
            else { closingEasing = previous(closingEasing); saveClosing(); }
            return true;
        }
        return false;
    }

    private void resetCurrent() {
        if (opening) {
            MotionUIConfig.resetOpening(entry.className);
            MotionUIConfig.ScreenRule rule = MotionUIConfig.rule(entry.className);
            openingDuration = rule.duration; openingOffset = rule.offset; openingEasing = rule.easing;
        } else {
            MotionUIConfig.resetClosing(entry.className);
            MotionUIConfig.ClosingRule rule = MotionUIConfig.closingRule(entry.className);
            closingInherited = true; closingMode = rule.mode;
            closingDuration = rule.duration; closingOffset = rule.offset; closingEasing = rule.easing;
        }
    }

    private void saveClosing() {
        if (closingInherited) MotionUIConfig.resetClosing(entry.className);
        else MotionUIConfig.setClosingRule(entry.className, closingMode, closingDuration, closingOffset, closingEasing);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.title"), width / 2, 10, 0xFFFFFF);
        String identity = entry.modId + " · " + entry.className;
        drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(identity, width - 24), width / 2, 30, 0xAAAAAA);
        int left = panelLeft(), right = left + panelWidth();
        int footer = footerY();
        panel.drawPanel(left - 10, TAB_TOP - 10, right + 10, footer + 30);
        drawRect(left, TAB_TOP + TAB_HEIGHT, right, footer - 6, 0x48100010);
        String effective = I18n.format("motionui.editor.effective", opening ? openingEffective() : closingEffective());
        drawSectionLabel(left, right, CONTENT_TOP + 26, effective);
        if (hasDetailedControls()) drawValueFields(left, opening ? openingDuration : closingDuration,
                opening ? openingOffset : closingOffset);
        if (showHint()) drawCenteredString(fontRenderer, I18n.format(hintKey()), width / 2, CONTENT_TOP + 58, 0x8C9AA8);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawValueFields(int left, int duration, int offset) {
        int contentWidth = panelWidth(), small = 46, middle = contentWidth - small * 2 - 8;
        int fieldLeft = left + small + 4, row = CONTENT_TOP + 42;
        panel.drawField(fieldLeft, row, fieldLeft + middle, row + 20);
        panel.drawField(fieldLeft, row + 26, fieldLeft + middle, row + 46);
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.duration", duration),
                fieldLeft + middle / 2, row + 6, 0xE8E0F0);
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.offset", offset),
                fieldLeft + middle / 2, row + 32, 0xE8E0F0);
    }

    private void drawSectionLabel(int left, int right, int y, String text) {
        int halfText = fontRenderer.getStringWidth(text) / 2;
        int center = (left + right) / 2;
        int lineY = y + 4;
        drawRect(left + 12, lineY, Math.max(left + 12, center - halfText - 8), lineY + 1, 0x70500070);
        drawRect(Math.min(right - 12, center + halfText + 8), lineY, right - 12, lineY + 1, 0x70500070);
        drawCenteredString(fontRenderer, text, center, y, 0xC8C0D0);
    }

    private boolean showHint() { return opening ? openingPolicy() != MotionUIConfig.Policy.ENABLED : closingInherited || closingMode == MotionUIConfig.ClosingMode.DISABLED; }
    private String hintKey() { return opening && openingPolicy() == MotionUIConfig.Policy.DISABLED || !opening && !closingInherited ? "motionui.editor.disabledHint" : "motionui.editor.inheritedHint"; }
    private MotionUIConfig.Policy openingPolicy() { MotionUIConfig.Policy p = MotionUIConfig.policy(entry.className); return p == MotionUIConfig.Policy.DEFAULT && MotionUIConfig.hasRule(entry.className) ? MotionUIConfig.Policy.ENABLED : p; }
    private String openingEffective() { MotionUIConfig.Policy p = openingPolicy(); if (p == MotionUIConfig.Policy.DISABLED) return I18n.format("motionui.policy.disabled"); MotionUIConfig.ScreenRule r = MotionUIConfig.rule(entry.className); return (p == MotionUIConfig.Policy.DEFAULT ? "↳ " : "") + r.duration + " ms · " + r.offset + " px"; }
    private String closingEffective() { MotionUIConfig.ClosingRule r = MotionUIConfig.closingRule(entry.className); return (closingInherited ? "↳ " : "") + I18n.format("motionui.closingMode." + r.mode.name().toLowerCase(Locale.ROOT)) + " · " + r.duration + " ms · " + r.offset + " px"; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
