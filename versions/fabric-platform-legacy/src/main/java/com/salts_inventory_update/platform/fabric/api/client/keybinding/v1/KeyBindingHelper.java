package com.salts_inventory_update.platform.fabric.api.client.keybinding.v1;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class KeyBindingHelper {
    private KeyBindingHelper() {
    }

    public static KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    public static InputConstants.Key getBoundKeyOf(KeyMapping keyMapping) {
        return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(keyMapping);
    }
}
