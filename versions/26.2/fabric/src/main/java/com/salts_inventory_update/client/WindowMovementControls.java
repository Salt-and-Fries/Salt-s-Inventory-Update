package com.salts_inventory_update.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Shared configuration actions for the mod settings and Minecraft Controls screen. */
public final class WindowMovementControls {
    private WindowMovementControls() {}

    public static Component modeValue() {
        return Component.translatable("config.salts_inventory_update.movement_control_mode."
            + (SaltsInventoryConfig.get().toggleMovementControl ? "toggle" : "hold"));
    }

    public static void cycleMode() {
        SaltsInventoryConfig.update(config -> config.toggleMovementControl = !config.toggleMovementControl);
        WindowedInventoryClient.resetMovementControl();
    }

    public static Button modeButton() {
        return Button.builder(modeLabel(), button -> {
            cycleMode();
            button.setMessage(modeLabel());
        }).width(150).build();
    }

    private static Component modeLabel() {
        return Component.translatable("controls.salts_inventory_update.movement_control_mode", modeValue());
    }

    public static void openKeyBinds(Screen previous) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new KeyBindsScreen(previous, minecraft.options));
    }
}
