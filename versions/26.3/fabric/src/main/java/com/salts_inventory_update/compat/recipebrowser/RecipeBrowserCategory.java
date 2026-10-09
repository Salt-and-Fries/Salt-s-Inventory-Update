package com.salts_inventory_update.compat.recipebrowser;

import net.minecraft.network.chat.Component;

public record RecipeBrowserCategory(String uid, Component title, int width, int height, Object category) {
}
