package com.demonicrous.motionui.client;

import com.demonicrous.motionui.MotionUI;
import com.demonicrous.motionui.config.MotionUIConfig;
import com.demonicrous.motionui.core.CompatibilityDiagnostics;
import com.demonicrous.motionui.core.PatchDiagnostics;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.I18n;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeVersion;
import org.lwjgl.input.Mouse;

/** Health-oriented diagnostics screen: outcome first, evidence second, raw details on demand. */
public final class GuiDiagnosticsScreen extends GuiScreen implements ClosingFlattenedRegion {
    private final MotionUITooltipPanel panel = new MotionUITooltipPanel();
    private final MotionUIScrollbar patchScrollbar = new MotionUIScrollbar();
    private final GuiScreen parent;
    private final List<HoverText> hoverTexts = new ArrayList<HoverText>();
    private MotionUILayout.Rect frame, nav, health, content, footer;
    private boolean details;
    private long copiedUntil;

    public GuiDiagnosticsScreen(GuiScreen parent) { this.parent = parent; }

    @Override public MotionUILayout.Rect closingFlattenedRegion() { return frame; }

    @Override public void initGui() { refresh(); }

    private void measure() {
        boolean narrow = width - MotionUILayout.S16 < 600;
        int preferredHeight = narrow ? (details ? 560 : 440) : (details ? 440 : 390);
        frame = MotionUILayout.frame(width, height, 820, preferredHeight);
        MotionUILayout.Rect inner = frame.inset(MotionUILayout.S12);
        nav = new MotionUILayout.Rect(inner.x, frame.y + 30, inner.width, MotionUILayout.TAB);
        health = new MotionUILayout.Rect(inner.x, nav.bottom() + MotionUILayout.S12, inner.width, 54);
        footer = new MotionUILayout.Rect(inner.x, frame.bottom() - MotionUILayout.S12 - MotionUILayout.FOOTER,
                inner.width, MotionUILayout.FOOTER);
        int contentTop = health.bottom() + MotionUILayout.S8;
        content = new MotionUILayout.Rect(inner.x, contentTop, inner.width,
                footer.y - MotionUILayout.S12 - contentTop);
    }

    private void refresh() {
        buttonList.clear(); measure(); MotionUINavigation.add(buttonList, nav, 2);
        MotionUILayout.Rect[] actions = MotionUILayout.tracks(footer, 3, MotionUILayout.S8);
        buttonList.add(new MotionUIButton(1, actions[0].x, actions[0].y, actions[0].width, actions[0].height,
                I18n.format(details ? "motionui.diagnostics.less" : "motionui.diagnostics.more")));
        buttonList.add(new MotionUIButton(2, actions[1].x, actions[1].y, actions[1].width, actions[1].height,
                I18n.format("motionui.diagnostics.copy")));
        buttonList.add(new MotionUIButton(3, actions[2].x, actions[2].y, actions[2].width, actions[2].height,
                I18n.format("gui.done"), MotionUIButton.Variant.PRIMARY));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 20 || button.id == 3) mc.displayGuiScreen(parent);
        else if (button.id == 21) mc.displayGuiScreen(new GuiMotionUISettings(parent));
        else if (button.id == 1) { details = !details; refresh(); }
        else if (button.id == 2) {
            GuiScreen.setClipboardString(report());
            copiedUntil = Minecraft.getSystemTime() + 2200L;
        }
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (details && button == 0 && patchScrollbar.mousePressed(x, y)) return;
        if (button == 1) {
            for (GuiButton control : buttonList) if (control.id == 1 && over(control, x, y)) {
                details = !details; refresh(); return;
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
            long timeSinceLastClick) {
        if (details && clickedMouseButton == 0) patchScrollbar.mouseDragged(mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override protected void mouseReleased(int mouseX, int mouseY, int state) {
        patchScrollbar.mouseReleased();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (!details) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        patchScrollbar.wheel(mouseX, mouseY, wheel);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground(); hoverTexts.clear();
        panel.drawPanel(frame.x, frame.y, frame.right(), frame.bottom());
        drawCenteredString(fontRenderer, I18n.format("motionui.diagnostics.title"), frame.centerX(), frame.y + 10,
                MotionUITheme.TEXT);
        Map<String, PatchDiagnostics.Result> patches = PatchDiagnostics.snapshot();
        int warningCount = warnings(patches);
        String font = fontProvider("textures/font/unicode_page_04.png");
        boolean overridden = !font.toLowerCase(Locale.ROOT).contains("motionui");
        drawHealth(warningCount, patches.size());
        if (details) drawDetails(patches, font, overridden, mouseX, mouseY);
        else drawOverview(patches, font, overridden, warningCount);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (Minecraft.getSystemTime() < copiedUntil) drawCopiedToast();
        for (int i = hoverTexts.size() - 1; i >= 0; i--) {
            HoverText hover = hoverTexts.get(i);
            if (hover.bounds.contains(mouseX, mouseY)) {
                TextTooltip.draw(fontRenderer, hover.title, hover.text, mouseX, mouseY, width, height); break;
            }
        }
    }

    private void drawHealth(int warningCount, int total) {
        int accent = warningCount == 0 ? MotionUITheme.SUCCESS : MotionUITheme.WARNING;
        MotionUITheme.card(this, health, accent);
        int iconX = health.x + 27, iconY = health.centerY();
        drawRect(iconX - 8, iconY - 8, iconX + 8, iconY + 8, accent);
        drawCenteredString(fontRenderer, warningCount == 0 ? "✓" : "!", iconX, iconY - 4, 0xFF140A1D);

        String title = I18n.format(warningCount == 0
                ? "motionui.diagnostics.health.ready" : "motionui.diagnostics.health.warning");
        String description = I18n.format(warningCount == 0
                ? "motionui.diagnostics.ready" : "motionui.diagnostics.attention");
        drawString(fontRenderer, title, health.x + 50, health.y + 11, MotionUITheme.TEXT);
        drawEllipsized(new MotionUILayout.Rect(health.x + 50, health.y + 27,
                        health.width - 150, 14), title, description,
                health.x + 50, health.y + 29, health.width - 150, MotionUITheme.TEXT_MUTED);

        String badge = (total - warningCount) + " / " + total;
        int badgeWidth = Math.max(58, fontRenderer.getStringWidth(badge) + 18);
        MotionUILayout.Rect badgeRect = new MotionUILayout.Rect(health.right() - badgeWidth - MotionUILayout.S12,
                health.y + 15, badgeWidth, MotionUILayout.CONTROL);
        MotionUITheme.surface(this, badgeRect, MotionUITheme.SURFACE_SELECTED, accent);
        drawCenteredString(fontRenderer, badge, badgeRect.centerX(), badgeRect.y + 7, accent);
    }

    private void drawOverview(Map<String, PatchDiagnostics.Result> patches, String font,
            boolean overridden, int warningCount) {
        MotionUITheme.card(this, content);
        MotionUILayout.Rect rows = content.inset(MotionUILayout.S8);
        int gap = MotionUILayout.S4;
        int rowHeight = Math.max(34, (rows.height - gap * 2) / 3);
        drawDiagnosticRow(new MotionUILayout.Rect(rows.x, rows.y, rows.width, rowHeight),
                I18n.format("motionui.diagnostics.environment"),
                "Minecraft 1.12.2  ·  Forge " + ForgeVersion.getVersion(),
                "OptiFine: " + optifine(), MotionUITheme.SUCCESS);
        drawDiagnosticRow(new MotionUILayout.Rect(rows.x, rows.y + rowHeight + gap, rows.width, rowHeight),
                I18n.format("motionui.diagnostics.patches"),
                I18n.format("motionui.diagnostics.patchSummary", patches.size() - warningCount, patches.size()),
                I18n.format("motionui.diagnostics.patchHint"),
                warningCount == 0 ? MotionUITheme.SUCCESS : MotionUITheme.WARNING);
        drawDiagnosticRow(new MotionUILayout.Rect(rows.x, rows.y + (rowHeight + gap) * 2, rows.width,
                        rows.height - rowHeight * 2 - gap * 2),
                I18n.format("motionui.diagnostics.resources"),
                overridden ? I18n.format("motionui.diagnostics.resourceOverride")
                        : I18n.format("motionui.diagnostics.resourceNative"),
                font, overridden ? MotionUITheme.WARNING : MotionUITheme.SUCCESS);
    }

    private void drawDetails(Map<String, PatchDiagnostics.Result> patches, String font, boolean overridden,
            int mouseX, int mouseY) {
        MotionUILayout.Rect[] columns = MotionUILayout.twoColumnOrStack(content, MotionUILayout.S8, 250);
        drawFacts(columns[0], patches, font, overridden);
        drawPatchList(columns[1], patches, mouseX, mouseY);
    }

    private void drawFacts(MotionUILayout.Rect area, Map<String, PatchDiagnostics.Result> patches,
            String font, boolean overridden) {
        MotionUITheme.card(this, area);
        drawString(fontRenderer, I18n.format("motionui.diagnostics.system"),
                area.x + MotionUILayout.S12, area.y + MotionUILayout.S12, MotionUITheme.TEXT);
        int warningCount = warnings(patches);
        int y = area.y + 34;
        drawFact(area, y, I18n.format("motionui.diagnostics.minecraft"), "1.12.2"); y += 22;
        drawFact(area, y, "Forge", ForgeVersion.getVersion()); y += 22;
        drawFact(area, y, "OptiFine", optifine()); y += 22;
        drawFact(area, y, I18n.format("motionui.diagnostics.patches"),
                (patches.size() - warningCount) + " / " + patches.size()); y += 22;
        drawFact(area, y, I18n.format("motionui.diagnostics.resources"),
                overridden ? I18n.format("motionui.diagnostics.resourceOverride") : "MotionUI"); y += 22;
        drawFact(area, y, I18n.format("motionui.diagnostics.provider"), font);
    }

    private void drawFact(MotionUILayout.Rect area, int y, String label, String value) {
        if (y + fontRenderer.FONT_HEIGHT > area.bottom() - MotionUILayout.S8) return;
        drawString(fontRenderer, label, area.x + MotionUILayout.S12, y, MotionUITheme.TEXT_MUTED);
        int maxWidth = Math.max(20, area.width / 2 - MotionUILayout.S16);
        String visible = ellipsize(value, maxWidth);
        drawString(fontRenderer, visible, area.right() - MotionUILayout.S12 - fontRenderer.getStringWidth(visible),
                y, MotionUITheme.TEXT);
        MotionUITheme.separator(this, area.x + MotionUILayout.S12, area.right() - MotionUILayout.S12, y + 15);
        if (fontRenderer.getStringWidth(value) > maxWidth)
            hoverTexts.add(new HoverText(new MotionUILayout.Rect(area.x, y - 4, area.width, 20), label, value));
    }

    private void drawPatchList(MotionUILayout.Rect area, Map<String, PatchDiagnostics.Result> patches,
            int mouseX, int mouseY) {
        MotionUITheme.card(this, area);
        drawString(fontRenderer, I18n.format("motionui.diagnostics.patchDetails"),
                area.x + MotionUILayout.S12, area.y + MotionUILayout.S12, MotionUITheme.TEXT);
        int viewportTop = area.y + 32;
        int viewportHeight = Math.max(28, area.bottom() - MotionUILayout.S8 - viewportTop);
        int visibleRows = Math.max(1, viewportHeight / 32);
        MotionUILayout.Rect scrollTrack = new MotionUILayout.Rect(area.right() - MotionUILayout.S12,
                viewportTop, MotionUILayout.S4, visibleRows * 32 - MotionUILayout.S4);
        MotionUILayout.Rect wheelArea = new MotionUILayout.Rect(area.x, viewportTop,
                area.width, visibleRows * 32 - MotionUILayout.S4);
        patchScrollbar.layout(scrollTrack, wheelArea, patches.size(), visibleRows);
        int x = area.x + MotionUILayout.S12, y = viewportTop;
        int rowWidth = area.width - MotionUILayout.S16 - MotionUILayout.S12;
        int index = 0, last = patchScrollbar.value() + visibleRows;
        for (Map.Entry<String, PatchDiagnostics.Result> entry : patches.entrySet()) {
            if (index < patchScrollbar.value()) { index++; continue; }
            if (index >= last) break;
            PatchDiagnostics.Result result = entry.getValue();
            int accent = "APPLIED".equals(result.getStatus()) ? MotionUITheme.SUCCESS
                    : "FAILED".equals(result.getStatus()) ? MotionUITheme.DANGER : MotionUITheme.WARNING;
            MotionUILayout.Rect row = new MotionUILayout.Rect(x, y, rowWidth, 28);
            MotionUITheme.surface(this, row, MotionUITheme.SURFACE_RAISED, MotionUITheme.BORDER_SUBTLE);
            drawRect(row.x, row.y, row.x + 3, row.bottom(), accent);
            String patchName = patchName(entry.getKey());
            drawString(fontRenderer, ellipsize(patchName, row.width * 2 / 3), row.x + 10,
                    row.centerY() - fontRenderer.FONT_HEIGHT / 2, MotionUITheme.TEXT);
            String status = statusName(result.getStatus());
            drawString(fontRenderer, status, row.right() - 8 - fontRenderer.getStringWidth(status),
                    row.centerY() - fontRenderer.FONT_HEIGHT / 2, accent);
            String tooltipStatus = "§7" + I18n.format("motionui.diagnostics.tooltip.status",
                    statusFormat(result.getStatus()) + status);
            String tooltipDetail = "§7" + I18n.format("motionui.diagnostics.tooltip.detail",
                    "§f" + patchDetail(entry.getKey(), result));
            hoverTexts.add(new HoverText(row, "§d" + patchName, tooltipStatus + "\n" + tooltipDetail));
            y += 32;
            index++;
        }
        patchScrollbar.draw(mouseX, mouseY);
    }

    private void drawDiagnosticRow(MotionUILayout.Rect row, String title, String value,
            String detail, int accent) {
        MotionUITheme.surface(this, row, MotionUITheme.SURFACE_RAISED, MotionUITheme.BORDER_SUBTLE);
        drawRect(row.x, row.y, row.x + 3, row.bottom(), accent);
        drawString(fontRenderer, title, row.x + 12, row.y + 8, MotionUITheme.TEXT);
        drawEllipsized(new MotionUILayout.Rect(row.x + 12, row.y + 22, row.width * 2 / 3, 14),
                title, detail, row.x + 12, row.y + 24, row.width * 2 / 3, MotionUITheme.TEXT_FAINT);
        int maxValueWidth = Math.max(40, row.width / 3 - MotionUILayout.S16);
        String visible = fontRenderer.trimStringToWidth(value, maxValueWidth);
        int valueX = row.right() - MotionUILayout.S12 - fontRenderer.getStringWidth(visible);
        drawString(fontRenderer, visible, valueX, row.centerY() - 4, accent);
        if (fontRenderer.getStringWidth(value) > maxValueWidth)
            hoverTexts.add(new HoverText(row, title, value));
    }

    private void drawEllipsized(MotionUILayout.Rect hitBounds, String title, String text,
            int x, int y, int maxWidth, int color) {
        String visible = ellipsize(text, maxWidth);
        boolean trimmed = fontRenderer.getStringWidth(text) > maxWidth;
        drawString(fontRenderer, visible, x, y, color);
        if (trimmed) hoverTexts.add(new HoverText(hitBounds, title, text));
    }

    private String ellipsize(String text, int maxWidth) {
        String visible = fontRenderer.trimStringToWidth(text, maxWidth);
        if (fontRenderer.getStringWidth(text) > maxWidth && visible.length() > 1)
            visible = visible.substring(0, visible.length() - 1) + "…";
        return visible;
    }

    private void drawCopiedToast() {
        String text = I18n.format("motionui.diagnostics.copied");
        int toastWidth = Math.max(118, fontRenderer.getStringWidth(text) + 28);
        int toastHeight = 24;
        int x = frame.right() + MotionUILayout.S8;
        int y = frame.y + 30;
        if (x + toastWidth > width - MotionUILayout.S8) {
            x = frame.right() - toastWidth - MotionUILayout.S12;
            y = footer.y - toastHeight - MotionUILayout.S8;
        }
        MotionUILayout.Rect toast = new MotionUILayout.Rect(x, y, toastWidth, toastHeight);
        MotionUITheme.surface(this, toast, MotionUITheme.SURFACE_RAISED, MotionUITheme.SUCCESS);
        drawRect(toast.x, toast.y, toast.x + 3, toast.bottom(), MotionUITheme.SUCCESS);
        drawString(fontRenderer, "✓", toast.x + 10, toast.y + 8, MotionUITheme.SUCCESS);
        drawString(fontRenderer, text, toast.x + 24, toast.y + 8, MotionUITheme.TEXT);
    }

    private static String patchName(String patch) {
        String key = "motionui.diagnostics.patch." + patch;
        return I18n.hasKey(key) ? I18n.format(key) : patch;
    }

    private static String statusName(String status) {
        String key = "motionui.diagnostics.status." + status.toLowerCase(Locale.ROOT);
        return I18n.hasKey(key) ? I18n.format(key) : status;
    }

    private static String statusFormat(String status) {
        if ("APPLIED".equals(status)) return "§a";
        if ("FAILED".equals(status)) return "§c";
        if ("DISABLED".equals(status)) return "§8";
        return "§e";
    }

    private static String patchDetail(String patch, PatchDiagnostics.Result result) {
        String key = "motionui.diagnostics.patch." + patch + ".detail";
        if (I18n.hasKey(key) && "APPLIED".equals(result.getStatus())) return I18n.format(key);
        return result.getDetail();
    }

    private static int warnings(Map<String, PatchDiagnostics.Result> results) {
        int count = 0;
        for (PatchDiagnostics.Result result : results.values()) if (!"APPLIED".equals(result.getStatus())) count++;
        return count;
    }

    private static String optifine() {
        try {
            Class<?> type = Class.forName("Config", false, Launch.classLoader);
            Method method = type.getDeclaredMethod("getVersion");
            return String.valueOf(method.invoke(null));
        } catch (Throwable ignored) { return I18n.format("motionui.diagnostics.notFound"); }
    }

    private static String fontProvider(String path) {
        IResource resource = null;
        try {
            resource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("minecraft", path));
            return resource.getResourcePackName();
        } catch (Exception exception) { return I18n.format("motionui.diagnostics.unknown"); }
        finally { if (resource != null) try { resource.close(); } catch (IOException ignored) {} }
    }

    private static String report() {
        StringBuilder builder = new StringBuilder();
        builder.append("MotionUI ").append(MotionUI.VERSION).append('\n')
                .append("Minecraft 1.12.2\nForge ").append(ForgeVersion.getVersion())
                .append("\nOptiFine ").append(optifine()).append("\nProfile ").append(MotionUIConfig.profile())
                .append("\nClosing ").append(MotionUIConfig.closingMode())
                .append("\nFont glyph_sizes.bin: ").append(fontProvider("font/glyph_sizes.bin"))
                .append("\nFont unicode_page_04.png: ").append(fontProvider("textures/font/unicode_page_04.png"))
                .append("\nResource packs: ").append(Minecraft.getMinecraft().gameSettings.resourcePacks)
                .append("\nASM patches:\n");
        for (Map.Entry<String, PatchDiagnostics.Result> entry : PatchDiagnostics.snapshot().entrySet())
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().getStatus())
                    .append(" (").append(entry.getValue().getDetail()).append(")\n");
        List<String> thirdParty = CompatibilityDiagnostics.thirdPartyTransformerClassNames();
        if (!thirdParty.isEmpty()) builder.append("Third-party transformers: ").append(thirdParty).append('\n');
        return builder.toString();
    }

    private static boolean over(GuiButton button, int x, int y) {
        return button != null && x >= button.x && x < button.x + button.width
                && y >= button.y && y < button.y + button.height;
    }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private static final class HoverText {
        final MotionUILayout.Rect bounds; final String title, text;
        HoverText(MotionUILayout.Rect bounds, String title, String text) {
            this.bounds = bounds; this.title = title; this.text = text;
        }
    }
}
