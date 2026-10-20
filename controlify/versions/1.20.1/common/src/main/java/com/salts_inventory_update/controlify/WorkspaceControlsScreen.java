package com.salts_inventory_update.controlify;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import org.slf4j.LoggerFactory;

/** Native controller-accessible options for the two wheel triggers. */
public final class WorkspaceControlsScreen extends Screen {
    private final Screen parent;
    public WorkspaceControlsScreen(Screen parent) {
        super(Component.translatable("salts_inventory_controlify.controls.title"));
        this.parent = parent;
    }
    @Override protected void init() {
        Button windows = wheel(false), recipes = wheel(true);
        int left = Math.max(4, width / 2 - 100), top = Math.max(30, height / 2 - 36);
        windows.setX(left); windows.setY(top); recipes.setX(left); recipes.setY(top + 26);
        addRenderableWidget(windows); addRenderableWidget(recipes);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(left, top + 60, 200, 20).build());
    }
    private Button wheel(boolean recipe) {
        String key = recipe ? "recipe" : "windows";
        return Button.builder(message(recipe), button -> {
            var config = SaltsControlifyClient.preferences();
            try {
                if (recipe) config.setToggleRecipeWheel(!config.toggleRecipeWheel());
                else config.setToggleWindowWheel(!config.toggleWindowWheel());
            } catch (IOException failure) {
                LoggerFactory.getLogger("salts_inventory_controlify").warn("Could not save wheel controls", failure);
            }
            com.salts_inventory_update.controlify.input.ControlifyInputBridge.cancelWheelToggles();
            button.setMessage(message(recipe));
        }).width(200).build();
    }
    private static Component message(boolean recipe) {
        var config = SaltsControlifyClient.preferences();
        boolean toggle = recipe ? config.toggleRecipeWheel() : config.toggleWindowWheel();
        return Component.translatable("salts_inventory_controlify.controls." + (recipe ? "recipe" : "windows"),
                Component.translatable("salts_inventory_controlify.controls." + (toggle ? "toggle" : "hold")));
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        super.render(graphics, x, y, partialTick);
    }
}
