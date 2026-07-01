package com.salts_inventory_update.compat.jei;

import net.minecraft.network.chat.Component;

public record JeiRecipeCategory(String uid, Component title, int width, int height, Object category) {
}
