package com.salts_inventory_update.controlify;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import org.slf4j.LoggerFactory;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
/** Native controller-accessible options for the two wheel triggers. */
public final class WorkspaceControlsScreen extends OptionsSubScreen {
    public WorkspaceControlsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("salts_inventory_controlify.controls.title"));
    }
    @Override protected void addOptions() {
        list.addBig(wheel(false));
        list.addBig(wheel(true));
    }
    private Button wheel(boolean recipe) {
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

}
