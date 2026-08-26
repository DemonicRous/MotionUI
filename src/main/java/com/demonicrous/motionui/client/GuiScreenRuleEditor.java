package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import com.demonicrous.motionui.animation.PreviewTimeline;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Per-GUI editor with explicit header, segmented mode, content cards and action footer. */
public final class GuiScreenRuleEditor extends GuiScreen implements ClosingFlattenedRegion {
    private final MotionUITooltipPanel panel = new MotionUITooltipPanel();
    private final GuiScreen parent;
    private final GuiCatalog.Entry entry;
    private final boolean modScope;
    private boolean opening = true, closingInherited;
    private int openingDuration, openingOffset, closingDuration, closingOffset;
    private MotionUIConfig.ScreenEasing openingEasing, closingEasing;
    private MotionUIConfig.AnimationStyle openingStyle, closingStyle;
    private MotionUIConfig.AnimationDirection openingDirection, closingDirection;
    private MotionUIConfig.ClosingMode closingMode;
    private MotionUILayout.Rect frame, modeTabs, state, summary, body, footer, parametersCard, motionCard, previewCard, scrubber;
    private MotionUIStepper durationStepper, offsetStepper;
    private PreviewTimeline preview;
    private boolean draggingScrubber;

    public GuiScreenRuleEditor(GuiScreen parent, GuiCatalog.Entry entry) {
        this(parent,entry,false);
    }

    public GuiScreenRuleEditor(GuiScreen parent, GuiCatalog.Entry entry, boolean modScope) {
        this.parent = parent; this.entry = entry; this.modScope=modScope;
        MotionUIConfig.ScreenRule openingRule = effectiveOpening();
        openingDuration = openingRule.duration; openingOffset = openingRule.offset;
        openingEasing = openingRule.easing; openingStyle = openingRule.style; openingDirection = openingRule.direction;
        MotionUIConfig.ClosingRule closingRule = effectiveClosing();
        closingDuration = closingRule.duration; closingOffset = closingRule.offset;
        closingEasing = closingRule.easing; closingStyle = closingRule.style; closingDirection = closingRule.direction;
        closingMode = closingRule.mode; closingInherited = !hasOwnClosing();
        preview=new PreviewTimeline(openingDuration,System.nanoTime());
    }

    @Override public MotionUILayout.Rect closingFlattenedRegion() { return frame; }

    @Override public void initGui() { refresh(); }

    private void measure() {
        boolean narrow = width - MotionUILayout.S16 < 536;
        frame = MotionUILayout.frame(width, height, 860, narrow ? 520 : 410);
        MotionUILayout.Rect inner = frame.inset(MotionUILayout.S12);
        modeTabs = new MotionUILayout.Rect(inner.x, frame.y + 48, inner.width, MotionUILayout.TAB);
        state = new MotionUILayout.Rect(inner.x, modeTabs.bottom() + MotionUILayout.S8, inner.width, 24);
        summary = new MotionUILayout.Rect(inner.x, state.bottom() + MotionUILayout.S8, inner.width, 20);
        footer = new MotionUILayout.Rect(inner.x, frame.bottom() - MotionUILayout.S12 - MotionUILayout.FOOTER,
                inner.width, MotionUILayout.FOOTER);
        int bodyTop = summary.bottom() + MotionUILayout.S12;
        body = new MotionUILayout.Rect(inner.x, bodyTop, inner.width,
                footer.y - MotionUILayout.S16 - bodyTop);
        previewCard = null; scrubber = null;
        if (body.width >= 720) {
            int available=body.width-MotionUILayout.S12*2;
            int controls=Math.max(210,available*34/100),motion=Math.max(170,available*27/100);
            parametersCard=new MotionUILayout.Rect(body.x,body.y,controls,body.height);
            motionCard=new MotionUILayout.Rect(parametersCard.right()+MotionUILayout.S12,body.y,motion,body.height);
            previewCard=new MotionUILayout.Rect(motionCard.right()+MotionUILayout.S12,body.y,
                    body.right()-motionCard.right()-MotionUILayout.S12,body.height);
            scrubber=new MotionUILayout.Rect(previewCard.x+10,previewCard.bottom()-16,previewCard.width-20,6);
        } else if (body.width >= 600) {
            int available = body.width - MotionUILayout.S12;
            int leftWidth = available * 3 / 5;
            parametersCard = new MotionUILayout.Rect(body.x, body.y, leftWidth, body.height);
            motionCard = new MotionUILayout.Rect(parametersCard.right() + MotionUILayout.S12, body.y,
                    available - leftWidth, body.height);
        } else {
            int half = Math.max(0, (body.height - MotionUILayout.S8) / 2);
            parametersCard = new MotionUILayout.Rect(body.x, body.y, body.width, half);
            motionCard = new MotionUILayout.Rect(body.x, body.y + half + MotionUILayout.S8,
                    body.width, body.height - half - MotionUILayout.S8);
        }
    }

    private void refresh() {
        buttonList.clear(); measure();
        MotionUILayout.Rect[] tabs = MotionUILayout.tracks(modeTabs, 2, MotionUILayout.S4);
        buttonList.add(new MotionUITabButton(70, tabs[0].x, tabs[0].y, tabs[0].width,
                I18n.format("motionui.editor.opening"), opening));
        buttonList.add(new MotionUITabButton(71, tabs[1].x, tabs[1].y, tabs[1].width,
                I18n.format("motionui.editor.closing"), !opening));
        String stateText;
        if (opening) {
            stateText = I18n.format("motionui.editor.state",
                    I18n.format("motionui.policy." + openingPolicy().name().toLowerCase(Locale.ROOT)));
            buttonList.add(new MotionUIButton(10, state.x, state.y, state.width, state.height, stateText));
        } else {
            String value = closingInherited ? I18n.format("motionui.policy.default")
                    : I18n.format("motionui.closingMode." + closingMode.name().toLowerCase(Locale.ROOT));
            stateText = I18n.format("motionui.editor.state", value);
            buttonList.add(new MotionUIButton(20, state.x, state.y, state.width, state.height, stateText));
        }
        if (hasDetailedControls()) addDetailedControls();
        MotionUILayout.Rect[] actions = MotionUILayout.tracks(footer, 6, MotionUILayout.S4);
        buttonList.add(new MotionUIButton(80, actions[0].x, actions[0].y, actions[0].width, actions[0].height,
                I18n.format("motionui.editor.copy")));
        MotionUIButton paste=new MotionUIButton(81,actions[1].x,actions[1].y,actions[1].width,actions[1].height,I18n.format("motionui.editor.paste"));
        paste.enabled=opening?AnimationRuleClipboard.hasOpening():AnimationRuleClipboard.hasClosing();buttonList.add(paste);
        buttonList.add(new MotionUIButton(83,actions[2].x,actions[2].y,actions[2].width,actions[2].height,I18n.format(previewCard==null?"motionui.editor.preview":"motionui.editor.replay")));
        buttonList.add(new MotionUIButton(82,actions[3].x,actions[3].y,actions[3].width,actions[3].height,
                I18n.format(modScope?"motionui.editor.applyMod":"motionui.editor.applyAllMod")));
        buttonList.add(new MotionUIButton(90, actions[4].x, actions[4].y, actions[4].width, actions[4].height,
                I18n.format("motionui.editor.reset")));
        buttonList.add(new MotionUIButton(99, actions[5].x, actions[5].y, actions[5].width, actions[5].height,
                I18n.format("gui.done"), MotionUIButton.Variant.PRIMARY));
    }

    private void addDetailedControls() {
        int duration = opening ? openingDuration : closingDuration;
        int offset = opening ? openingOffset : closingOffset;
        MotionUIConfig.ScreenEasing easing = opening ? openingEasing : closingEasing;
        int x = parametersCard.x + MotionUILayout.S12;
        int width = parametersCard.width - MotionUILayout.S16 - MotionUILayout.S8;
        int rowTop = parametersCard.y + 34;
        durationStepper = new MotionUIStepper(new MotionUILayout.Rect(x, rowTop, width, MotionUILayout.CONTROL),
                I18n.format("motionui.editor.duration", duration), 30, 32, "-20", "+20");
        offsetStepper = new MotionUIStepper(new MotionUILayout.Rect(x, rowTop + 30, width, MotionUILayout.CONTROL),
                I18n.format("motionui.editor.offset", offset), 33, 35, "-2", "+2");
        durationStepper.addButtons(buttonList); offsetStepper.addButtons(buttonList);
        buttonList.add(new MotionUIButton(36, x, rowTop + 60, width, MotionUILayout.CONTROL,
                I18n.format("motionui.editor.easing",
                        I18n.format("motionui.easing." + easing.name().toLowerCase(Locale.ROOT)))));
        MotionUILayout.Rect padBounds = new MotionUILayout.Rect(motionCard.x + MotionUILayout.S12,
                motionCard.y + 28, motionCard.width - MotionUILayout.S16 - MotionUILayout.S8,
                Math.max(74, motionCard.height - 54));
        MotionUIConfig.AnimationStyle style = opening ? openingStyle : closingStyle;
        MotionUIConfig.AnimationDirection direction = opening ? openingDirection : closingDirection;
        new MotionUIMotionPad(padBounds, 40, style, direction, !opening).addButtons(buttonList);
    }

    private boolean hasDetailedControls() {
        return opening ? openingPolicy() == MotionUIConfig.Policy.ENABLED
                : !closingInherited && closingMode != MotionUIConfig.ClosingMode.DISABLED;
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) {
            MotionUIConfig.Policy value = openingPolicy();
            if (value == MotionUIConfig.Policy.DEFAULT) setOpeningPolicy(MotionUIConfig.Policy.ENABLED);
            else if (value == MotionUIConfig.Policy.ENABLED) setOpeningPolicy(MotionUIConfig.Policy.DISABLED);
            else resetOpening();
        } else if (button.id == 20) {
            if (closingInherited) { closingInherited = false; closingMode = MotionUIConfig.ClosingMode.DISABLED; }
            else if (closingMode == MotionUIConfig.ClosingMode.DISABLED) closingMode = MotionUIConfig.ClosingMode.SIMPLIFIED;
            else if (closingMode == MotionUIConfig.ClosingMode.SIMPLIFIED) closingMode = MotionUIConfig.ClosingMode.FULL;
            else closingInherited = true;
            saveClosing();
        } else if (button.id == 70 || button.id == 71) opening = button.id == 70;
        else if (button.id >= 30 && button.id <= 44) change(button.id);
        else if(button.id==80)copyCurrent();
        else if(button.id==81)pasteCurrent();
        else if(button.id==82)applyToMod();
        else if(button.id==83){if(previewCard==null){mc.displayGuiScreen(new GuiAnimationPreviewScreen(this,effectivePreviewOpening(),effectivePreviewClosing(),!opening));return;}replayPreview();}
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
            if (id >= 40 && id <= 43) {
                MotionUIConfig.AnimationDirection selected = MotionUIStyleLogic.directionFor(id, 40);
                openingStyle = MotionUIStyleLogic.toggleSlide(openingStyle, openingDirection == selected);
                openingDirection = selected;
            }
            if (id == 44) openingStyle = MotionUIStyleLogic.toggleScale(openingStyle);
            saveOpening();
        } else {
            if (id == 30) closingDuration = Math.max(0, closingDuration - 20);
            if (id == 32) closingDuration = Math.min(600, closingDuration + 20);
            if (id == 33) closingOffset = Math.max(0, closingOffset - 2);
            if (id == 35) closingOffset = Math.min(48, closingOffset + 2);
            if (id == 36) closingEasing = next(closingEasing);
            if (id >= 40 && id <= 43) {
                MotionUIConfig.AnimationDirection selected = MotionUIStyleLogic.directionFor(id, 40);
                closingStyle = MotionUIStyleLogic.toggleSlide(closingStyle, closingDirection == selected);
                closingDirection = selected;
            }
            if (id == 44) closingStyle = MotionUIStyleLogic.toggleScale(closingStyle);
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

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(previewCard!=null&&scrubber.contains(mouseX,mouseY)){draggingScrubber=true;scrubPreview(mouseX);return;}
        if (mouseButton == 1) {
            GuiButton button = buttonAt(mouseX, mouseY);
            if (button != null && reverse(button.id)) { refresh(); return; }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override protected void mouseClickMove(int mouseX,int mouseY,int clickedMouseButton,long timeSinceLastClick){if(draggingScrubber){scrubPreview(mouseX);return;}super.mouseClickMove(mouseX,mouseY,clickedMouseButton,timeSinceLastClick);}
    @Override protected void mouseReleased(int mouseX,int mouseY,int state){if(draggingScrubber){scrubPreview(mouseX);draggingScrubber=false;return;}super.mouseReleased(mouseX,mouseY,state);}
    private void scrubPreview(int mouseX){preview.scrub((mouseX-scrubber.x)/(double)Math.max(1,scrubber.width));}

    private GuiButton buttonAt(int x, int y) {
        for (int i = buttonList.size() - 1; i >= 0; i--) {
            GuiButton button = buttonList.get(i);
            if (button.visible && button.enabled && x >= button.x && x < button.x + button.width
                    && y >= button.y && y < button.y + button.height) return button;
        }
        return null;
    }

    private boolean reverse(int id) {
        if (id == 10) {
            MotionUIConfig.Policy value = openingPolicy();
            if (value == MotionUIConfig.Policy.DEFAULT) setOpeningPolicy(MotionUIConfig.Policy.DISABLED);
            else if (value == MotionUIConfig.Policy.DISABLED) setOpeningPolicy(MotionUIConfig.Policy.ENABLED);
            else resetOpening();
            return true;
        }
        if (id == 20) {
            if (closingInherited) { closingInherited = false; closingMode = MotionUIConfig.ClosingMode.FULL; }
            else if (closingMode == MotionUIConfig.ClosingMode.FULL) closingMode = MotionUIConfig.ClosingMode.SIMPLIFIED;
            else if (closingMode == MotionUIConfig.ClosingMode.SIMPLIFIED) closingMode = MotionUIConfig.ClosingMode.DISABLED;
            else closingInherited = true;
            saveClosing(); return true;
        }
        if (id == 36) {
            if (opening) {
                openingEasing = previous(openingEasing);
                saveOpening();
            } else { closingEasing = previous(closingEasing); saveClosing(); }
            return true;
        }
        if (id >= 40 && id <= 44) { change(id); return true; }
        return false;
    }

    private void resetCurrent() {
        if (opening) {
            resetOpening();
            MotionUIConfig.ScreenRule rule = effectiveOpening();
            openingDuration = rule.duration; openingOffset = rule.offset; openingEasing = rule.easing;
            openingStyle = rule.style; openingDirection = rule.direction;
        } else {
            resetClosing();
            MotionUIConfig.ClosingRule rule = effectiveClosing();
            closingInherited = true; closingMode = rule.mode; closingDuration = rule.duration;
            closingOffset = rule.offset; closingEasing = rule.easing; closingStyle = rule.style;
            closingDirection = rule.direction;
        }
    }

    private void saveClosing() {
        if (closingInherited) resetClosing();
        else if(modScope)MotionUIConfig.setModClosingRule(entry.modId,closingMode,closingDuration,closingOffset,closingEasing,closingStyle,closingDirection);
        else MotionUIConfig.setClosingRule(entry.className, closingMode, closingDuration, closingOffset,closingEasing, closingStyle, closingDirection);
        replayPreview();
    }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        panel.drawPanel(frame.x, frame.y, frame.right(), frame.bottom());
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.title"), frame.centerX(), frame.y + 8,
                MotionUITheme.TEXT);
        String identity = modScope ? I18n.format("motionui.editor.modIdentity",entry.modId) : entry.modId + " · " + entry.className;
        drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(identity, frame.width - 32),
                frame.centerX(), frame.y + 24, MotionUITheme.TEXT_MUTED);
        MotionUITheme.surface(this, summary, MotionUITheme.SURFACE, MotionUITheme.BORDER_SUBTLE);
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.effective",
                opening ? openingEffective() : closingEffective()), summary.centerX(),
                summary.y + (summary.height - fontRenderer.FONT_HEIGHT) / 2, MotionUITheme.TEXT_MUTED);
        if (hasDetailedControls()) {
            drawParametersCard(); drawMotionCard(); if(previewCard!=null)drawPreview();
        } else {
            MotionUITheme.card(this, body);
            drawCenteredString(fontRenderer, I18n.format(hintKey()), body.centerX(),
                    body.centerY() - fontRenderer.FONT_HEIGHT / 2, MotionUITheme.TEXT_MUTED);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        GuiButton hovered = buttonAt(mouseX, mouseY);
        if (hovered != null && hovered.id >= 40 && hovered.id <= 44) {
            boolean scale = hovered.id == 44;
            TextTooltip.draw(fontRenderer,
                    I18n.format(scale ? "motionui.editor.scalePad.title" : "motionui.editor.directionPad.title"),
                    I18n.format(scale ? "motionui.editor.scalePad.hint" : "motionui.editor.directionPad.hint"),
                    mouseX, mouseY, width, height);
        }
    }

    private void drawParametersCard() {
        MotionUITheme.card(this, parametersCard);
        drawString(fontRenderer, "§e" + I18n.format("motionui.editor.parameters"),
                parametersCard.x + MotionUILayout.S12, parametersCard.y + 10, MotionUITheme.TEXT);
        durationStepper.draw(fontRenderer); offsetStepper.draw(fontRenderer);
    }

    private void drawMotionCard() {
        MotionUITheme.card(this, motionCard);
        drawCenteredString(fontRenderer, I18n.format("motionui.editor.motionPad"), motionCard.centerX(),
                motionCard.y + 10, MotionUITheme.TEXT);
        MotionUIConfig.AnimationStyle style = opening ? openingStyle : closingStyle;
        drawCenteredString(fontRenderer, I18n.format("motionui.style." + style.name().toLowerCase(Locale.ROOT)),
                motionCard.centerX(), motionCard.bottom() - 17,
                style == MotionUIConfig.AnimationStyle.SCALE_SLIDE ? MotionUITheme.ACCENT : MotionUITheme.TEXT_MUTED);
    }

    private String hintKey() {
        return opening && openingPolicy() == MotionUIConfig.Policy.DISABLED || !opening && !closingInherited
                ? "motionui.editor.disabledHint" : "motionui.editor.inheritedHint";
    }

    private MotionUIConfig.Policy openingPolicy() {
        MotionUIConfig.Policy value = modScope?MotionUIConfig.modPolicy(entry.modId):MotionUIConfig.ownPolicy(entry.className);
        return value == MotionUIConfig.Policy.DEFAULT && hasOwnOpening()
                ? MotionUIConfig.Policy.ENABLED : value;
    }

    private String openingEffective() {
        MotionUIConfig.Policy value = openingPolicy();
        if (value == MotionUIConfig.Policy.DISABLED) return I18n.format("motionui.policy.disabled");
        MotionUIConfig.ScreenRule rule = effectiveOpening();
        return (value == MotionUIConfig.Policy.DEFAULT ? "↳ " : "")
                + I18n.format("motionui.style." + rule.style.name().toLowerCase(Locale.ROOT))
                + " · " + rule.duration + " ms · " + rule.offset + " px";
    }

    private String closingEffective() {
        MotionUIConfig.ClosingRule rule = effectiveClosing();
        return (closingInherited ? "↳ " : "")
                + I18n.format("motionui.closingMode." + rule.mode.name().toLowerCase(Locale.ROOT)) + " · "
                + I18n.format("motionui.style." + rule.style.name().toLowerCase(Locale.ROOT))
                + " · " + rule.duration + " ms · " + rule.offset + " px";
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private MotionUIConfig.ScreenRule effectiveOpening(){return modScope?MotionUIConfig.modRule(entry.modId):MotionUIConfig.rule(entry.className,entry.modId);}
    private MotionUIConfig.ClosingRule effectiveClosing(){return modScope?MotionUIConfig.modClosingRule(entry.modId):MotionUIConfig.closingRule(entry.className,entry.modId);}
    private boolean hasOwnOpening(){return modScope?MotionUIConfig.hasModRule(entry.modId):MotionUIConfig.hasRule(entry.className);}
    private boolean hasOwnClosing(){return modScope?MotionUIConfig.hasModClosingRule(entry.modId):MotionUIConfig.hasClosingRule(entry.className);}
    private void setOpeningPolicy(MotionUIConfig.Policy value){if(modScope)MotionUIConfig.setModPolicy(entry.modId,value);else MotionUIConfig.setPolicy(entry.className,value);}
    private void saveOpening(){if(modScope)MotionUIConfig.setModRule(entry.modId,openingDuration,openingOffset,openingEasing,openingStyle,openingDirection);else MotionUIConfig.setRule(entry.className,openingDuration,openingOffset,openingEasing,openingStyle,openingDirection);replayPreview();}
    private void resetOpening(){if(modScope)MotionUIConfig.resetModOpening(entry.modId);else MotionUIConfig.resetOpening(entry.className);}
    private void resetClosing(){if(modScope)MotionUIConfig.resetModClosing(entry.modId);else MotionUIConfig.resetClosing(entry.className);}
    private void replayPreview(){preview.replay(opening?openingDuration:closingDuration,System.nanoTime());}
    private void copyCurrent(){if(opening)AnimationRuleClipboard.copyOpening(new MotionUIConfig.ScreenRule(openingDuration,openingOffset,openingEasing,openingStyle,openingDirection),openingPolicy());else AnimationRuleClipboard.copyClosing(new MotionUIConfig.ClosingRule(closingMode,closingDuration,closingOffset,closingEasing,closingStyle,closingDirection));}
    private void pasteCurrent(){if(opening&&AnimationRuleClipboard.hasOpening()){MotionUIConfig.ScreenRule r=AnimationRuleClipboard.opening();openingDuration=r.duration;openingOffset=r.offset;openingEasing=r.easing;openingStyle=r.style;openingDirection=r.direction;setOpeningPolicy(AnimationRuleClipboard.openingPolicy());saveOpening();}else if(!opening&&AnimationRuleClipboard.hasClosing()){MotionUIConfig.ClosingRule r=AnimationRuleClipboard.closing();closingInherited=false;closingMode=r.mode;closingDuration=r.duration;closingOffset=r.offset;closingEasing=r.easing;closingStyle=r.style;closingDirection=r.direction;saveClosing();}}
    private Collection<String> modClasses(){Collection<String>names=new ArrayList<String>();for(GuiCatalog.Entry item:GuiCatalog.entries())if(entry.modId.equals(item.modId))names.add(item.className);return names;}
    private void applyToMod(){if(modScope){if(opening)saveOpening();else saveClosing();return;}Collection<String>names=modClasses();if(opening)MotionUIConfig.applyOpening(names,new MotionUIConfig.ScreenRule(openingDuration,openingOffset,openingEasing,openingStyle,openingDirection),openingPolicy());else MotionUIConfig.applyClosing(names,new MotionUIConfig.ClosingRule(closingMode,closingDuration,closingOffset,closingEasing,closingStyle,closingDirection));}
    private void drawPreview(){AnimationPreviewRenderer.draw(this,previewCard,effectivePreviewOpening(),effectivePreviewClosing(),!opening,preview.progress(System.nanoTime()));drawCenteredString(fontRenderer,I18n.format("motionui.editor.preview"),previewCard.centerX(),previewCard.y+8,MotionUITheme.TEXT);double p=preview.progress(System.nanoTime());drawRect(scrubber.x,scrubber.y,scrubber.right(),scrubber.bottom(),MotionUITheme.BORDER_SUBTLE);drawRect(scrubber.x,scrubber.y,scrubber.x+(int)(scrubber.width*p),scrubber.bottom(),MotionUITheme.ACCENT);}
    private MotionUIConfig.ScreenRule effectivePreviewOpening(){return new MotionUIConfig.ScreenRule(openingDuration,openingOffset,openingEasing,openingStyle,openingDirection);}
    private MotionUIConfig.ClosingRule effectivePreviewClosing(){return new MotionUIConfig.ClosingRule(closingMode,closingDuration,closingOffset,closingEasing,closingStyle,closingDirection);}
}
