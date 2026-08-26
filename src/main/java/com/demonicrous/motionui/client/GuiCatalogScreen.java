package com.demonicrous.motionui.client;

import com.demonicrous.motionui.config.MotionUIConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;

/** F8 catalog with a measured toolbar, virtualized list viewport and stable footer. */
public final class GuiCatalogScreen extends GuiScreen implements ClosingFlattenedRegion {
    private static final int ROW = 26, GEAR = 22;
    private final MotionUITooltipPanel panel = new MotionUITooltipPanel();
    private final GuiScreen parent;
    private final List<GuiCatalog.Entry> visible = new ArrayList<GuiCatalog.Entry>();
    private MotionUILayout.Rect frame, nav, toolbar, searchRect, header, list, footer;
    private MotionUITextField search;
    private GuiButton sortButton, configuredButton;
    private String mod;
    private MotionUIConfig.CatalogSort sort;
    private boolean configuredOnly;
    private int offset, lastRevision = -1;

    public GuiCatalogScreen(GuiScreen parent) {
        this.parent = parent; mod = MotionUIConfig.catalogMod; sort = MotionUIConfig.catalogSort;
        configuredOnly = MotionUIConfig.catalogConfiguredOnly; offset = MotionUIConfig.catalogScroll;
    }

    @Override public MotionUILayout.Rect closingFlattenedRegion() { return frame; }

    private boolean compact() { return frame.width < 600; }
    private int rows() { return Math.max(1, list.height / ROW); }

    private void measure() {
        frame = MotionUILayout.frame(width, height, 1040, height - MotionUILayout.S8);
        MotionUILayout.Rect inner = frame.inset(MotionUILayout.S12);
        nav = new MotionUILayout.Rect(inner.x, frame.y + 28, inner.width, MotionUILayout.TAB);
        int toolbarHeight = compact() ? MotionUILayout.CONTROL * 2 + MotionUILayout.S4 : MotionUILayout.CONTROL;
        toolbar = new MotionUILayout.Rect(inner.x, nav.bottom() + MotionUILayout.S8, inner.width, toolbarHeight);
        if (compact()) searchRect = new MotionUILayout.Rect(toolbar.x,
                toolbar.y + MotionUILayout.CONTROL + MotionUILayout.S4 + 1,
                toolbar.width, MotionUILayout.CONTROL - 2);
        else {
            int searchX = toolbar.x + 132;
            int searchRight = toolbar.right() - 138 - MotionUILayout.S4;
            searchRect = new MotionUILayout.Rect(searchX, toolbar.y + 1,
                    searchRight - searchX, MotionUILayout.CONTROL - 2);
        }
        footer = new MotionUILayout.Rect(inner.x, frame.bottom() - MotionUILayout.S12 - MotionUILayout.FOOTER,
                inner.width, MotionUILayout.FOOTER);
        header = new MotionUILayout.Rect(inner.x, toolbar.bottom() + MotionUILayout.S8, inner.width, 18);
        int availableListHeight = Math.max(ROW, footer.y - MotionUILayout.S8 - header.bottom());
        list = new MotionUILayout.Rect(inner.x, header.bottom(), inner.width,
                Math.max(ROW, availableListHeight / ROW * ROW));
    }

    @Override public void initGui() {
        measure();
        search = new MotionUITextField(3, fontRenderer, searchRect,
                I18n.format("motionui.catalog.searchHint"));
        search.setMaxStringLength(120); search.setText(MotionUIConfig.catalogSearch);
        rebuild();
    }

    private void rebuild() {
        lastRevision = GuiCatalog.revision();
        String query = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        visible.clear();
        for (GuiCatalog.Entry entry : GuiCatalog.entries()) {
            if (("all".equals(mod) || mod.equals(entry.modId))
                    && (!configuredOnly || MotionUIConfig.isConfigured(entry.className,entry.modId))
                    && (query.isEmpty() || entry.className.toLowerCase(Locale.ROOT).contains(query))) visible.add(entry);
        }
        Collections.sort(visible, comparator());
        offset = Math.max(0, Math.min(offset, Math.max(0, visible.size() - rows())));
        createButtons();
    }

    private Comparator<GuiCatalog.Entry> comparator() {
        return new Comparator<GuiCatalog.Entry>() {
            @Override public int compare(GuiCatalog.Entry a, GuiCatalog.Entry b) {
                int result = 0;
                if (sort == MotionUIConfig.CatalogSort.CONFIGURED)
                    result = Boolean.compare(MotionUIConfig.isConfigured(b.className,b.modId), MotionUIConfig.isConfigured(a.className,a.modId));
                else if (sort == MotionUIConfig.CatalogSort.OBSERVED)
                    result = Boolean.compare(b.observed, a.observed);
                if (result == 0 && sort != MotionUIConfig.CatalogSort.NAME)
                    result = a.modId.compareToIgnoreCase(b.modId);
                return result != 0 ? result : a.className.compareToIgnoreCase(b.className);
            }
        };
    }

    private void createButtons() {
        buttonList.clear(); MotionUINavigation.add(buttonList, nav, 0);
        int modWidth = compact() ? Math.min(140, toolbar.width / 3) : 128;
        buttonList.add(new MotionUIButton(1, toolbar.x, toolbar.y, modWidth, MotionUILayout.CONTROL, modText()));
        if (compact()) {
            int starWidth = 30;
            int sortLeft = toolbar.x + modWidth + MotionUILayout.S4;
            int sortWidth = toolbar.right() - sortLeft - starWidth - MotionUILayout.S4;
            sortButton = new MotionUIButton(4, sortLeft, toolbar.y, sortWidth, MotionUILayout.CONTROL, sortText());
            configuredButton = new MotionUIStarButton(5, toolbar.right() - starWidth, toolbar.y, starWidth,
                    MotionUILayout.CONTROL,
                    configuredOnly);
        } else {
            sortButton = new MotionUIButton(4, toolbar.right() - 138, toolbar.y, 104, MotionUILayout.CONTROL,
                    sortText());
            configuredButton = new MotionUIStarButton(5, toolbar.right() - 30, toolbar.y, 30,
                    MotionUILayout.CONTROL, configuredOnly);
        }
        buttonList.add(sortButton); buttonList.add(configuredButton);
        buttonList.add(new MotionUIButton(2, footer.right() - 88, footer.y, 88, footer.height,
                I18n.format("gui.done"), MotionUIButton.Variant.PRIMARY));
        if(!"all".equals(mod)&&!"unknown".equals(mod))buttonList.add(new MotionUIButton(6,footer.right()-184,footer.y,92,footer.height,I18n.format("motionui.catalog.editMod")));
        for (int i = 0; i < rows() && offset + i < visible.size(); i++)
            buttonList.add(new MotionUIGearButton(1000 + i, list.right() - GEAR - 2,
                    list.y + 2 + i * ROW, GEAR));
    }

    private String modText() { return I18n.format("motionui.catalog.mod", mod); }
    private String sortText() { return I18n.format("motionui.catalog.sort." + sort.name().toLowerCase(Locale.ROOT)); }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 2) { save(); mc.displayGuiScreen(parent); }
        else if (button.id == 21) { save(); mc.displayGuiScreen(new GuiMotionUISettings(this)); }
        else if (button.id == 22) { save(); mc.displayGuiScreen(new GuiDiagnosticsScreen(this)); }
        else if (button.id == 1) cycleMod(1);
        else if (button.id == 4) cycleSort(1);
        else if (button.id == 5) { configuredOnly = !configuredOnly; offset = 0; rebuild(); }
        else if(button.id==6){GuiCatalog.Entry representative=null;for(GuiCatalog.Entry item:GuiCatalog.entries())if(mod.equals(item.modId)){representative=item;break;}if(representative!=null){save();mc.displayGuiScreen(new GuiScreenRuleEditor(this,representative,true));}}
        else if (button.id >= 1000) {
            int index = offset + button.id - 1000;
            if (index < visible.size()) { save(); mc.displayGuiScreen(new GuiScreenRuleEditor(this, visible.get(index))); }
        }
    }

    private void cycleMod(int direction) {
        List<String> ids = new ArrayList<String>(); ids.add("all");
        for (GuiCatalog.Entry entry : GuiCatalog.entries()) if (!ids.contains(entry.modId)) ids.add(entry.modId);
        if (ids.size() > 1) Collections.sort(ids.subList(1, ids.size()));
        int index = ids.indexOf(mod); if (index < 0) index = 0;
        mod = ids.get((index + (direction < 0 ? ids.size() - 1 : 1)) % ids.size());
        offset = 0; rebuild();
    }

    private void cycleSort(int direction) {
        MotionUIConfig.CatalogSort[] values = MotionUIConfig.CatalogSort.values();
        sort = values[(sort.ordinal() + (direction < 0 ? values.length - 1 : 1)) % values.length];
        offset = 0; rebuild();
    }

    private void save() {
        MotionUIConfig.saveCatalogState(search == null ? "" : search.getText(), mod, sort, configuredOnly, offset);
    }

    @Override public void onGuiClosed() { save(); }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { save(); mc.displayGuiScreen(parent); return; }
        if (search.textboxKeyTyped(typedChar, keyCode)) { offset = 0; rebuild(); }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        boolean overSearch = search.contains(mouseX, mouseY);
        search.setFocused(overSearch);
        if (overSearch) search.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1) {
            GuiButton hit = buttonAt(mouseX, mouseY);
            if (hit != null) {
                if (hit.id == 1) { cycleMod(-1); return; }
                if (hit.id == 4) { cycleSort(-1); return; }
                if (hit.id == 5) { configuredOnly = !configuredOnly; offset = 0; rebuild(); return; }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private GuiButton buttonAt(int x, int y) {
        for (int i = buttonList.size() - 1; i >= 0; i--) {
            GuiButton button = buttonList.get(i);
            if (button.visible && button.enabled && over(button, x, y)) return button;
        }
        return null;
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int wheel = Mouse.getEventDWheel();
        if (wheel != 0) { offset += wheel < 0 ? 3 : -3; rebuild(); }
    }

    @Override public void updateScreen() {
        search.updateCursorCounter(); if (lastRevision != GuiCatalog.revision()) rebuild();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground(); panel.drawPanel(frame.x, frame.y, frame.right(), frame.bottom());
        drawCenteredString(fontRenderer, I18n.format("motionui.catalog.title"), frame.centerX(), frame.y + 9,
                MotionUITheme.TEXT);
        search.drawTextBox();
        drawColumnHeader(); MotionUITheme.card(this, list);
        int hoveredRow = list.contains(mouseX, mouseY) ? (mouseY - list.y) / ROW : -1;
        for (int i = 0; i < rows() && offset + i < visible.size(); i++) drawRow(i, hoveredRow == i);
        if (visible.isEmpty()) drawCenteredString(fontRenderer, I18n.format("motionui.catalog.empty"),
                list.centerX(), list.centerY(), MotionUITheme.TEXT_MUTED);
        String count = visible.isEmpty() ? "0 / 0"
                : (offset + 1) + "–" + Math.min(visible.size(), offset + rows()) + " / " + visible.size();
        drawString(fontRenderer, count, footer.x, footer.y + (footer.height - fontRenderer.FONT_HEIGHT) / 2,
                MotionUITheme.TEXT_MUTED);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawHoverTooltip(mouseX, mouseY, hoveredRow);
    }

    private void drawColumnHeader() {
        MotionUITheme.surface(this, header, MotionUITheme.SURFACE_RAISED, MotionUITheme.BORDER_SUBTLE);
        drawString(fontRenderer, I18n.format("motionui.catalog.column.gui"), header.x + 7, header.y + 5,
                MotionUITheme.TEXT_FAINT);
        if (!compact()) {
            MotionUILayout.Rect openingColumn = openingColumn();
            MotionUILayout.Rect closingColumn = closingColumn();
            drawCenteredString(fontRenderer, I18n.format("motionui.catalog.column.opening"),
                    openingColumn.centerX(), header.y + 5, MotionUITheme.TEXT_FAINT);
            drawCenteredString(fontRenderer, I18n.format("motionui.catalog.column.closing"),
                    closingColumn.centerX(), header.y + 5, MotionUITheme.TEXT_FAINT);
        }
    }

    private void drawRow(int row, boolean hovered) {
        GuiCatalog.Entry entry = visible.get(offset + row); int y = list.y + row * ROW;
        int fill = hovered ? MotionUITheme.SURFACE_SELECTED : (row & 1) == 0 ? 0x8A160B20 : 0x8A1B0D27;
        drawRect(list.x + 1, y + 1, list.right() - 1, Math.min(list.bottom(), y + ROW), fill);
        MotionUITheme.separator(this, list.x + 1, list.right() - 1, Math.min(list.bottom() - 1, y + ROW - 1));
        String identity = (entry.observed ? "§a●" : "§7○") + " §b" + entry.modId + "§r  " + shortName(entry.className);
        MotionUILayout.Rect openingColumn = openingColumn();
        MotionUILayout.Rect closingColumn = closingColumn();
        int identityWidth = Math.max(24, openingColumn.x - list.x - MotionUILayout.S12);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(identity, identityWidth),
                list.x + 7, y + 9, MotionUITheme.TEXT);
        drawCenteredString(fontRenderer, opening(entry), openingColumn.centerX(), y + 9,
                MotionUITheme.TEXT);
        drawCenteredString(fontRenderer, closing(entry), closingColumn.centerX(), y + 9,
                MotionUITheme.TEXT);
    }

    private void drawHoverTooltip(int mouseX, int mouseY, int hoveredRow) {
        if (over(sortButton, mouseX, mouseY)) {
            String key = "motionui.catalog.sortHint." + sort.name().toLowerCase(Locale.ROOT);
            TextTooltip.draw(fontRenderer, I18n.format("motionui.catalog.sortHint.title"), I18n.format(key),
                    mouseX, mouseY, width, height); return;
        }
        if (over(configuredButton, mouseX, mouseY)) {
            String key = configuredOnly ? "motionui.catalog.configuredHint.on" : "motionui.catalog.configuredHint.off";
            TextTooltip.draw(fontRenderer, I18n.format("motionui.catalog.configuredHint.title"), I18n.format(key),
                    mouseX, mouseY, width, height); return;
        }
        if (hoveredRow >= 0 && hoveredRow < rows() && offset + hoveredRow < visible.size()
                && mouseX >= openingColumn().x) {
            GuiCatalog.Entry entry = visible.get(offset + hoveredRow);
            TextTooltip.draw(fontRenderer, "§e§l" + shortName(entry.className) + "§r", tooltip(entry),
                    mouseX, mouseY, width, height);
        }
    }

    private static boolean over(GuiButton button, int x, int y) {
        return button != null && x >= button.x && x < button.x + button.width
                && y >= button.y && y < button.y + button.height;
    }

    private MotionUILayout.Rect closingColumn() {
        int width = compact() ? 78 : 150;
        int right = list.right() - GEAR - MotionUILayout.S8;
        return new MotionUILayout.Rect(right - width, list.y, width, list.height);
    }

    private MotionUILayout.Rect openingColumn() {
        int width = compact() ? 58 : 150;
        MotionUILayout.Rect closing = closingColumn();
        return new MotionUILayout.Rect(closing.x - MotionUILayout.S8 - width, list.y, width, list.height);
    }

    private String opening(GuiCatalog.Entry entry) {
        String name=entry.className;MotionUIConfig.Policy policy = MotionUIConfig.policy(name,entry.modId);
        MotionUIConfig.RuleSource source=MotionUIConfig.openingSource(name,entry.modId);
        if (compact()) return policy == MotionUIConfig.Policy.DISABLED ? "§cO:×"
                : source!=MotionUIConfig.RuleSource.GLOBAL ? "§eO:●" : "§7O:↳";
        if (policy == MotionUIConfig.Policy.DISABLED) return "§c" + I18n.format("motionui.catalog.open.disabled");
        if (source!=MotionUIConfig.RuleSource.GLOBAL)
            return "§e" + I18n.format("motionui.catalog.open.custom");
        return "§7" + I18n.format("motionui.catalog.open.inherit", profile());
    }

    private String closing(GuiCatalog.Entry entry) {
        String name=entry.className;MotionUIConfig.ClosingRule rule = MotionUIConfig.closingRule(name,entry.modId);
        boolean inherited=MotionUIConfig.closingSource(name,entry.modId)==MotionUIConfig.RuleSource.GLOBAL;
        String mode = I18n.format("motionui.closingMode." + rule.mode.name().toLowerCase(Locale.ROOT));
        if (compact()) return (!inherited ? "§eC:" : "§7C:↳")
                + (rule.mode == MotionUIConfig.ClosingMode.DISABLED ? "×"
                : rule.mode == MotionUIConfig.ClosingMode.SIMPLIFIED ? "S" : "F");
        return !inherited ? "§e" + I18n.format("motionui.catalog.close.custom", mode)
                : "§7" + I18n.format("motionui.catalog.close.inherit", mode);
    }

    private List<String> tooltip(GuiCatalog.Entry entry) {
        String name=entry.className;MotionUIConfig.ScreenRule opening = MotionUIConfig.rule(name,entry.modId);
        MotionUIConfig.ClosingRule closing = MotionUIConfig.closingRule(name,entry.modId);
        boolean openingInherited = MotionUIConfig.openingSource(name,entry.modId)==MotionUIConfig.RuleSource.GLOBAL;
        boolean closingInherited = MotionUIConfig.closingSource(name,entry.modId)==MotionUIConfig.RuleSource.GLOBAL;
        List<String> lines = new ArrayList<String>();
        lines.add(section("motionui.editor.opening", openingInherited));
        lines.add(I18n.format("motionui.catalog.tooltip.style",
                I18n.format("motionui.style." + opening.style.name().toLowerCase(Locale.ROOT))));
        lines.add(I18n.format("motionui.catalog.tooltip.duration", opening.duration));
        lines.add(I18n.format("motionui.catalog.tooltip.offset", opening.offset));
        lines.add("");
        lines.add(section("motionui.editor.closing", closingInherited));
        lines.add(I18n.format("motionui.catalog.tooltip.mode",
                I18n.format("motionui.closingMode." + closing.mode.name().toLowerCase(Locale.ROOT))));
        lines.add(I18n.format("motionui.catalog.tooltip.style",
                I18n.format("motionui.style." + closing.style.name().toLowerCase(Locale.ROOT))));
        lines.add(I18n.format("motionui.catalog.tooltip.duration", closing.duration));
        lines.add(I18n.format("motionui.catalog.tooltip.offset", closing.offset));
        return lines;
    }

    private String section(String key, boolean inherited) {
        return "§e" + I18n.format(key) + " §8· "
                + I18n.format(inherited ? "motionui.catalog.tooltip.inherited"
                : "motionui.catalog.tooltip.custom");
    }

    private String profile() {
        return I18n.format("motionui.profile." + MotionUIConfig.profile().name().toLowerCase(Locale.ROOT));
    }

    private static String shortName(String name) {
        int separator = name.lastIndexOf('.'); return separator < 0 ? name : name.substring(separator + 1);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
