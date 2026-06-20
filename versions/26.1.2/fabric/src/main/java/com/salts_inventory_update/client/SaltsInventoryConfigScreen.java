package com.salts_inventory_update.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class SaltsInventoryConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("config.salts_inventory_update.title");
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 24;

    private final @Nullable Screen previousScreen;
    private Button expandableInventoryButton;
    private Button windowOpeningStyleButton;
    private Button openUnlockedButton;
    private Button allowResizingButton;

    public SaltsInventoryConfigScreen(@Nullable Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        int x = (this.width - BUTTON_WIDTH) / 2;
        int y = Math.max(40, this.height / 6 + 8);

        this.expandableInventoryButton = this.addRenderableWidget(Button.builder(this.expandableInventoryMessage(), button -> {
            SaltsInventoryConfig.update(config -> config.expandableInventory = !config.expandableInventory);
            this.refreshMessages();
        }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.windowOpeningStyleButton = this.addRenderableWidget(Button.builder(this.windowOpeningStyleMessage(), button -> {
            SaltsInventoryConfig.update(config -> config.setWindowOpeningStyle(config.windowOpeningStyle().next()));
            this.refreshMessages();
        }).bounds(x, y + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.openUnlockedButton = this.addRenderableWidget(Button.builder(this.openUnlockedMessage(), button -> {
            SaltsInventoryConfig.update(config -> config.openUnlocked = !config.openUnlocked);
            this.refreshMessages();
        }).bounds(x, y + BUTTON_GAP * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.allowResizingButton = this.addRenderableWidget(Button.builder(this.allowResizingMessage(), button -> {
            SaltsInventoryConfig.update(config -> config.allowResizing = !config.allowResizing);
            this.refreshMessages();
        }).bounds(x, y + BUTTON_GAP * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds((this.width - Button.DEFAULT_WIDTH) / 2, y + BUTTON_GAP * 5, Button.DEFAULT_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        this.extractMenuBackground(graphics);
        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public void onClose() {
        SaltsInventoryConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    private void refreshMessages() {
        this.expandableInventoryButton.setMessage(this.expandableInventoryMessage());
        this.windowOpeningStyleButton.setMessage(this.windowOpeningStyleMessage());
        this.openUnlockedButton.setMessage(this.openUnlockedMessage());
        this.allowResizingButton.setMessage(this.allowResizingMessage());
    }

    private Component expandableInventoryMessage() {
        return option("config.salts_inventory_update.expandable_inventory", onOff(SaltsInventoryConfig.get().expandableInventory));
    }

    private Component windowOpeningStyleMessage() {
        return option(
            "config.salts_inventory_update.window_opening_style",
            Component.translatable("config.salts_inventory_update.window_opening_style." + SaltsInventoryConfig.get().windowOpeningStyle().name().toLowerCase())
        );
    }

    private Component openUnlockedMessage() {
        return option("config.salts_inventory_update.open_unlocked", onOff(SaltsInventoryConfig.get().openUnlocked));
    }

    private Component allowResizingMessage() {
        return option("config.salts_inventory_update.allow_resizing", onOff(SaltsInventoryConfig.get().allowResizing));
    }

    private static Component option(String key, Component value) {
        return Component.translatable("config.salts_inventory_update.option", Component.translatable(key), value);
    }

    private static Component onOff(boolean value) {
        return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }
}
