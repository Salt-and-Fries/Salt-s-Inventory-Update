package com.salts_inventory_update.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.debug.DesktopDebug;

public final class ForceContainersConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("config.salts_inventory_update.force_containers_as_windows.title");
    private static final int BACKGROUND = 0xAA000000;
    private static final int ROW_BACKGROUND = 0x66000000;
    private static final int ROW_BORDER = 0x55333333;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int DESCRIPTION_COLOR = 0xFF9EA7B3;
    private static final int TOP = 42;
    private static final int BOTTOM_RESERVED = 34;
    private static final int SIDE_MARGIN = 28;
    private static final int ROW_PADDING = 8;
    private static final int ROW_GAP = 4;
    private static final int ROW_HEIGHT = 38;
    private static final int TOGGLE_WIDTH = 62;
    private static final int CONTROL_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 22;
    private static final int SCROLLBAR_TRACK_COLOR = 0x77333333;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFB8B8B8;
    private static final int SCROLLBAR_THUMB_BORDER = 0xFF4A4A4A;

    private final Screen previousScreen;
    private final List<MenuRow> rows = new ArrayList<>();
    private double scrollOffset;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;

    public ForceContainersConfigScreen(Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.scrollOffset = 0.0D;

        List<MenuEntry> entries = menuEntries();
        for (MenuEntry entry : entries) {
            Button button = Button.builder(this.toggleText(entry.id()), clicked -> {
                boolean enabled = !SaltsInventoryConfig.isForcedContainerWindow(entry.id());
                SaltsInventoryConfig.setForcedContainerWindow(entry.id(), enabled);
                InventoryDesktopScreen.syncForcedContainerScreens();
                clicked.setMessage(this.toggleText(entry.id()));
                DesktopDebug.log("client force container config menu={} enabled={}", entry.id(), enabled);
            }).bounds(0, 0, TOGGLE_WIDTH, CONTROL_HEIGHT).build();
            this.rows.add(new MenuRow(entry, button));
            this.addRenderableWidget(button);
        }

        int footerWidth = Button.DEFAULT_WIDTH * 2 + 8;
        int footerX = (this.width - footerWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("config.salts_inventory_update.force_containers_as_windows.clear"), button -> this.clearForced())
            .bounds(footerX, this.height - 26, Button.DEFAULT_WIDTH, CONTROL_HEIGHT)
            .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(footerX + Button.DEFAULT_WIDTH + 8, this.height - 26, Button.DEFAULT_WIDTH, CONTROL_HEIGHT)
            .build());
        this.updateRowPositions();
    }

    @Override
    public void render(GuiGraphics rawGraphics, int mouseX, int mouseY, float tickProgress) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.wrap(rawGraphics);
        this.renderMenuBackground(rawGraphics);
        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, LABEL_COLOR);
        graphics.centeredText(this.font, Component.translatable("config.salts_inventory_update.force_containers_as_windows.subtitle", this.rows.size()), this.width / 2, 27, DESCRIPTION_COLOR);
        this.updateRowPositions();
        this.renderRows(graphics);
        this.renderScrollbar(graphics);
        super.render(rawGraphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.isOverScrollbar(event.x(), event.y())) {
            this.beginScrollbarDrag(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingScrollbar) {
            this.scrollToThumbTop(event.y() - this.scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY < this.listTop() || mouseY > this.listBottom() || this.maxScrollOffset() <= 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        this.scrollOffset = clamp(this.scrollOffset - scrollY * 18.0D, 0.0D, this.maxScrollOffset());
        this.updateRowPositions();
        return true;
    }

    @Override
    public void onClose() {
        SaltsInventoryConfig.save();
        InventoryDesktopScreen.syncForcedContainerScreens();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    private void clearForced() {
        SaltsInventoryConfig.update(config -> config.forcedContainerWindows.clear());
        InventoryDesktopScreen.syncForcedContainerScreens();
        for (MenuRow row : this.rows) {
            row.control.setMessage(this.toggleText(row.entry.id()));
        }
        DesktopDebug.log("client force container config cleared");
    }

    private Component toggleText(String id) {
        return Component.literal(SaltsInventoryConfig.isForcedContainerWindow(id) ? "[x]" : "[ ]");
    }

    private static List<MenuEntry> menuEntries() {
        List<MenuEntry> entries = new ArrayList<>();
        for (MenuType<?> menuType : BuiltInRegistries.MENU) {
            Identifier id = BuiltInRegistries.MENU.getKey(menuType);
            if (id != null) {
                entries.add(new MenuEntry(id.toString(), friendlyName(id), id.getNamespace()));
            }
        }
        entries.sort(Comparator.comparing(MenuEntry::namespace).thenComparing(MenuEntry::name).thenComparing(MenuEntry::id));
        return entries;
    }

    private static String friendlyName(Identifier id) {
        String path = id.getPath().replace('/', ' ').replace('_', ' ').replace('-', ' ');
        String[] words = path.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }

    private void updateRowPositions() {
        int x = SIDE_MARGIN;
        int width = this.width - SIDE_MARGIN * 2;
        int controlX = x + width - TOGGLE_WIDTH - ROW_PADDING;
        int y = this.listTop() - (int) Math.round(this.scrollOffset);
        int listTop = this.listTop();
        int listBottom = this.listBottom();

        for (MenuRow row : this.rows) {
            row.y = y;
            row.control.setX(controlX);
            row.control.setY(y + ROW_PADDING);
            row.control.setWidth(TOGGLE_WIDTH);
            row.control.setHeight(CONTROL_HEIGHT);
            boolean visible = y + ROW_HEIGHT >= listTop && y <= listBottom;
            row.control.visible = visible;
            row.control.active = visible;
            y += ROW_HEIGHT + ROW_GAP;
        }
    }

    private void renderRows(GuiGraphicsExtractor graphics) {
        int x = SIDE_MARGIN;
        int width = this.width - SIDE_MARGIN * 2;
        int listTop = this.listTop();
        int listBottom = this.listBottom();
        int labelWidth = Math.max(60, width - TOGGLE_WIDTH - ROW_PADDING * 3);

        graphics.enableScissor(0, listTop, this.width, listBottom);
        for (MenuRow row : this.rows) {
            if (row.y + ROW_HEIGHT < listTop || row.y > listBottom) {
                continue;
            }

            graphics.fill(x, row.y, x + width, row.y + ROW_HEIGHT, ROW_BACKGROUND);
            graphics.outline(x, row.y, width, ROW_HEIGHT, ROW_BORDER);
            graphics.text(this.font, row.entry.name(), x + ROW_PADDING, row.y + ROW_PADDING, LABEL_COLOR, false);
            this.renderDescription(graphics, row.entry.id(), x + ROW_PADDING, row.y + ROW_PADDING + 13, labelWidth);
        }
        graphics.disableScissor();
    }

    private void renderDescription(GuiGraphicsExtractor graphics, String text, int x, int y, int width) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);
        if (!lines.isEmpty()) {
            graphics.text(this.font, lines.get(0), x, y, DESCRIPTION_COLOR, false);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (this.maxScrollOffset() <= 0.0D) {
            return;
        }

        int x = this.scrollbarX();
        int top = this.listTop();
        int bottom = this.listBottom();
        int thumbHeight = this.scrollbarThumbHeight();
        int thumbY = this.scrollbarThumbY();

        graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK_COLOR);
        graphics.outline(x, top, SCROLLBAR_WIDTH, Math.max(1, bottom - top), ROW_BORDER);
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
        graphics.outline(x + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, SCROLLBAR_THUMB_BORDER);
    }

    private int listTop() {
        return TOP;
    }

    private int listBottom() {
        return this.height - BOTTOM_RESERVED;
    }

    private double maxScrollOffset() {
        int totalHeight = this.rows.isEmpty() ? 0 : this.rows.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        return Math.max(0.0D, totalHeight - Math.max(1, this.listBottom() - this.listTop()));
    }

    private int scrollbarX() {
        return this.width - SIDE_MARGIN + (SIDE_MARGIN - SCROLLBAR_WIDTH) / 2;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (this.maxScrollOffset() <= 0.0D) {
            return false;
        }

        int x = this.scrollbarX();
        return mouseX >= x && mouseX <= x + SCROLLBAR_WIDTH && mouseY >= this.listTop() && mouseY <= this.listBottom();
    }

    private void beginScrollbarDrag(double mouseY) {
        int thumbY = this.scrollbarThumbY();
        int thumbHeight = this.scrollbarThumbHeight();
        this.scrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight ? mouseY - thumbY : thumbHeight / 2.0D;
        this.draggingScrollbar = true;
        this.scrollToThumbTop(mouseY - this.scrollbarDragOffset);
    }

    private void scrollToThumbTop(double thumbTop) {
        double maxScroll = this.maxScrollOffset();
        if (maxScroll <= 0.0D) {
            this.scrollOffset = 0.0D;
            return;
        }

        int top = this.listTop();
        int travel = Math.max(1, this.listBottom() - top - this.scrollbarThumbHeight());
        this.scrollOffset = clamp((thumbTop - top) / travel * maxScroll, 0.0D, maxScroll);
        this.updateRowPositions();
    }

    private int scrollbarThumbHeight() {
        double maxScroll = this.maxScrollOffset();
        int viewportHeight = Math.max(1, this.listBottom() - this.listTop());
        if (maxScroll <= 0.0D) {
            return viewportHeight;
        }

        int totalHeight = viewportHeight + (int) Math.ceil(maxScroll);
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, viewportHeight * viewportHeight / Math.max(1, totalHeight));
    }

    private int scrollbarThumbY() {
        double maxScroll = this.maxScrollOffset();
        int top = this.listTop();
        if (maxScroll <= 0.0D) {
            return top;
        }

        int travel = Math.max(0, this.listBottom() - top - this.scrollbarThumbHeight());
        return top + (int) Math.round(this.scrollOffset / maxScroll * travel);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record MenuEntry(String id, String name, String namespace) {
    }

    private static final class MenuRow {
        private final MenuEntry entry;
        private final AbstractWidget control;
        private int y;

        private MenuRow(MenuEntry entry, AbstractWidget control) {
            this.entry = entry;
            this.control = control;
        }
    }
}
