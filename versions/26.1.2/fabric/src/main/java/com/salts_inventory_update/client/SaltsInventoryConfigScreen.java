package com.salts_inventory_update.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.SaltsInventoryRuntime;

public final class SaltsInventoryConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("config.salts_inventory_update.title");
    private static final int BACKGROUND = 0xAA000000;
    private static final int ROW_BACKGROUND = 0x66000000;
    private static final int ROW_BORDER = 0x55333333;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int DESCRIPTION_COLOR = 0xFF9EA7B3;
    private static final float DESCRIPTION_SCALE = 0.75F;
    private static final int TOP = 36;
    private static final int BOTTOM_RESERVED = 34;
    private static final int SIDE_MARGIN = 28;
    private static final int ROW_PADDING = 8;
    private static final int ROW_GAP = 6;
    private static final int CONTROL_WIDTH = 150;
    private static final int CONTROL_HEIGHT = 20;
    private static final int MIN_ROW_HEIGHT = 42;
    private static final int FOOTER_BUTTON_GAP = 8;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 22;
    private static final int SCROLLBAR_TRACK_COLOR = 0x77333333;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFB8B8B8;
    private static final int SCROLLBAR_THUMB_BORDER = 0xFF4A4A4A;

    private final @Nullable Screen previousScreen;
    private final List<OptionRow> rows = new ArrayList<>();
    private double scrollOffset;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;

    public SaltsInventoryConfigScreen(@Nullable Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.scrollOffset = 0.0D;

        this.addToggle(
            "enable_mod",
            () -> SaltsInventoryConfig.get().enableMod,
            value -> SaltsInventoryConfig.update(config -> config.enableMod = value)
        );
        this.addToggle(
            "expandable_inventory",
            () -> SaltsInventoryConfig.get().expandableInventory,
            value -> SaltsInventoryConfig.update(config -> config.expandableInventory = value)
        );
        this.addCycle(
            "window_opening_style",
            () -> Component.translatable("config.salts_inventory_update.window_opening_style." + SaltsInventoryConfig.get().windowOpeningStyle().name().toLowerCase(Locale.ROOT)),
            () -> SaltsInventoryConfig.update(config -> config.setWindowOpeningStyle(config.windowOpeningStyle().next()))
        );
        this.addToggle(
            "open_unlocked",
            () -> SaltsInventoryConfig.get().openUnlocked,
            value -> SaltsInventoryConfig.update(config -> config.openUnlocked = value)
        );
        this.addToggle(
            "allow_resizing",
            () -> SaltsInventoryConfig.get().allowResizing,
            value -> SaltsInventoryConfig.update(config -> config.allowResizing = value)
        );
        this.addToggle(
            "enable_window_snapping",
            () -> SaltsInventoryConfig.get().enableWindowSnapping,
            value -> SaltsInventoryConfig.update(config -> config.enableWindowSnapping = value)
        );
        this.addToggle(
            "reset_locked_windows",
            () -> SaltsInventoryConfig.get().resetLockedWindows,
            value -> SaltsInventoryConfig.update(config -> config.resetLockedWindows = value)
        );
        this.addToggle(
            "enable_ghost_pins",
            () -> SaltsInventoryConfig.get().enableGhostPins,
            value -> SaltsInventoryConfig.update(config -> config.enableGhostPins = value)
        );
        this.addSlider(
            "ghost_window_opacity",
            0.15D,
            0.90D,
            0.01D,
            SaltsInventoryConfig.get().ghostWindowOpacity,
            value -> SaltsInventoryConfig.update(config -> config.ghostWindowOpacity = value),
            value -> Component.translatable("config.salts_inventory_update.value.percent", Math.round(value * 100.0D))
        );
        this.addSlider(
            "e_hold_close_all_seconds",
            0.5D,
            10.0D,
            0.25D,
            SaltsInventoryConfig.get().eHoldCloseAllSeconds,
            value -> SaltsInventoryConfig.update(config -> config.eHoldCloseAllSeconds = value),
            value -> Component.translatable("config.salts_inventory_update.value.seconds", String.format(Locale.ROOT, "%.2f", value))
        );

        int footerWidth = Button.DEFAULT_WIDTH * 2 + FOOTER_BUTTON_GAP;
        int footerX = (this.width - footerWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("config.salts_inventory_update.reset_defaults"), button -> this.resetToDefaults())
            .bounds(footerX, this.height - 26, Button.DEFAULT_WIDTH, CONTROL_HEIGHT)
            .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(footerX + Button.DEFAULT_WIDTH + FOOTER_BUTTON_GAP, this.height - 26, Button.DEFAULT_WIDTH, CONTROL_HEIGHT)
            .build());
        this.updateRowPositions();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        this.extractMenuBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, LABEL_COLOR);
        this.renderRuntimeStatus(graphics);
        this.updateRowPositions();
        this.renderRows(graphics);
        this.renderScrollbar(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
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
        int listTop = this.listTop();
        int listBottom = this.listBottom();
        if (mouseY < listTop || mouseY > listBottom || this.maxScrollOffset() <= 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        this.scrollOffset = clamp(this.scrollOffset - scrollY * 18.0D, 0.0D, this.maxScrollOffset());
        this.updateRowPositions();
        return true;
    }

    @Override
    public void onClose() {
        SaltsInventoryConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(!SaltsInventoryRuntime.isEnabled() && this.previousScreen instanceof InventoryDesktopScreen ? null : this.previousScreen);
        }
    }

    private void resetToDefaults() {
        SaltsInventoryConfig.update(SaltsInventoryConfig.ConfigFile::resetToDefaults);
        if (this.minecraft != null) {
            this.minecraft.setScreen(new SaltsInventoryConfigScreen(this.previousScreen));
        }
    }

    private void addToggle(String id, BooleanGetter getter, BooleanSetter setter) {
        Button button = Button.builder(this.onOff(getter.get()), clicked -> {
            setter.set(!getter.get());
            clicked.setMessage(this.onOff(getter.get()));
        }).bounds(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT).build();
        this.rows.add(new OptionRow(id, button));
        this.addRenderableWidget(button);
    }

    private void addCycle(String id, ComponentGetter message, Runnable action) {
        Button button = Button.builder(message.get(), clicked -> {
            action.run();
            clicked.setMessage(message.get());
        }).bounds(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT).build();
        this.rows.add(new OptionRow(id, button));
        this.addRenderableWidget(button);
    }

    private void addSlider(String id, double min, double max, double step, double value, DoubleConsumer setter, DoubleFunction<Component> message) {
        ConfigSlider slider = new ConfigSlider(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT, min, max, step, value, setter, message);
        this.rows.add(new OptionRow(id, slider));
        this.addRenderableWidget(slider);
    }

    private void updateRowPositions() {
        int x = SIDE_MARGIN;
        int width = this.width - SIDE_MARGIN * 2;
        int controlX = x + width - CONTROL_WIDTH - ROW_PADDING;
        int y = this.listTop() - (int) Math.round(this.scrollOffset);
        int listTop = this.listTop();
        int listBottom = this.listBottom();
        int labelWidth = Math.max(60, width - CONTROL_WIDTH - ROW_PADDING * 3);

        for (OptionRow row : this.rows) {
            row.height = this.rowHeight(row, labelWidth);
            row.y = y;
            row.control.setX(controlX);
            row.control.setY(y + ROW_PADDING);
            row.control.setWidth(CONTROL_WIDTH);
            row.control.setHeight(CONTROL_HEIGHT);
            boolean visible = y + row.height >= listTop && y <= listBottom;
            row.control.visible = visible;
            row.control.active = visible;
            y += row.height + ROW_GAP;
        }
    }

    private void renderRows(GuiGraphicsExtractor graphics) {
        int x = SIDE_MARGIN;
        int width = this.width - SIDE_MARGIN * 2;
        int listTop = this.listTop();
        int listBottom = this.listBottom();
        int labelWidth = Math.max(60, width - CONTROL_WIDTH - ROW_PADDING * 3);

        graphics.enableScissor(0, listTop, this.width, listBottom);
        for (OptionRow row : this.rows) {
            if (row.y + row.height < listTop || row.y > listBottom) {
                continue;
            }

            graphics.fill(x, row.y, x + width, row.y + row.height, ROW_BACKGROUND);
            graphics.outline(x, row.y, width, row.height, ROW_BORDER);
            graphics.text(this.font, row.label(), x + ROW_PADDING, row.y + ROW_PADDING, LABEL_COLOR, false);
            this.renderDescription(graphics, row.description(), x + ROW_PADDING, row.y + ROW_PADDING + 13, labelWidth);
        }
        graphics.disableScissor();
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (this.maxScrollOffset() <= 0.0D) {
            return;
        }

        int x = this.scrollbarX();
        int top = this.listTop();
        int bottom = this.listBottom();
        int height = Math.max(1, bottom - top);
        int thumbHeight = this.scrollbarThumbHeight();
        int thumbY = this.scrollbarThumbY();

        graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK_COLOR);
        graphics.outline(x, top, SCROLLBAR_WIDTH, height, ROW_BORDER);
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
        graphics.outline(x + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, SCROLLBAR_THUMB_BORDER);
    }

    private void renderDescription(GuiGraphicsExtractor graphics, Component description, int x, int y, int width) {
        List<FormattedCharSequence> lines = this.font.split(description, (int) (width / DESCRIPTION_SCALE));
        graphics.pose().pushMatrix();
        graphics.pose().scale(DESCRIPTION_SCALE, DESCRIPTION_SCALE);
        int scaledX = Math.round(x / DESCRIPTION_SCALE);
        int scaledY = Math.round(y / DESCRIPTION_SCALE);
        int lineHeight = Math.round(9 / DESCRIPTION_SCALE);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(this.font, lines.get(i), scaledX, scaledY + i * lineHeight, DESCRIPTION_COLOR, false);
        }
        graphics.pose().popMatrix();
    }

    private void renderRuntimeStatus(GuiGraphicsExtractor graphics) {
        if (SaltsInventoryRuntime.isServerDesktopAvailable() || !SaltsInventoryConfig.get().enableMod) {
            return;
        }

        Component status = Component.translatable("config.salts_inventory_update.runtime_disabled.server");
        graphics.centeredText(this.font, status, this.width / 2, 26, 0xFFFFD84A);
    }

    private int rowHeight(OptionRow row, int labelWidth) {
        int descriptionLines = Math.max(1, this.font.split(row.description(), (int) (labelWidth / DESCRIPTION_SCALE)).size());
        int descriptionHeight = Math.round(descriptionLines * 9 * DESCRIPTION_SCALE);
        return Math.max(MIN_ROW_HEIGHT, ROW_PADDING + 12 + descriptionHeight + ROW_PADDING);
    }

    private int listTop() {
        return TOP + (this.runtimeStatusVisible() ? 12 : 0);
    }

    private int listBottom() {
        return this.height - BOTTOM_RESERVED;
    }

    private boolean runtimeStatusVisible() {
        return !SaltsInventoryRuntime.isServerDesktopAvailable() && SaltsInventoryConfig.get().enableMod;
    }

    private double maxScrollOffset() {
        int totalHeight = -ROW_GAP;
        int labelWidth = Math.max(60, this.width - SIDE_MARGIN * 2 - CONTROL_WIDTH - ROW_PADDING * 3);
        for (OptionRow row : this.rows) {
            totalHeight += this.rowHeight(row, labelWidth) + ROW_GAP;
        }
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
        if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
            this.scrollbarDragOffset = mouseY - thumbY;
        } else {
            this.scrollbarDragOffset = thumbHeight / 2.0D;
        }
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

    private Component onOff(boolean value) {
        return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class OptionRow {
        private final String id;
        private final AbstractWidget control;
        private int y;
        private int height;

        private OptionRow(String id, AbstractWidget control) {
            this.id = id;
            this.control = control;
        }

        private Component label() {
            return Component.translatable("config.salts_inventory_update." + this.id);
        }

        private Component description() {
            return Component.translatable("config.salts_inventory_update." + this.id + ".description");
        }
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface ComponentGetter {
        Component get();
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer setter;
        private final DoubleFunction<Component> message;

        private ConfigSlider(
            int x,
            int y,
            int width,
            int height,
            double min,
            double max,
            double step,
            double value,
            DoubleConsumer setter,
            DoubleFunction<Component> message
        ) {
            super(x, y, width, height, message.apply(clamp(value, min, max)), (clamp(value, min, max) - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            this.message = message;
            this.applyValue();
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.message.apply(this.actualValue()));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.actualValue());
        }

        private double actualValue() {
            double raw = this.min + (this.max - this.min) * this.value;
            double stepped = Math.round(raw / this.step) * this.step;
            return clamp(stepped, this.min, this.max);
        }
    }
}
