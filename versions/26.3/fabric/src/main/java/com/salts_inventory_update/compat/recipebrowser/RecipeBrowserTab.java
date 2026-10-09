package com.salts_inventory_update.compat.recipebrowser;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public record RecipeBrowserTab(String uid, Component title, @Nullable RecipeBrowserEntry icon, RecipeBrowserTabKind kind) {
    public RecipeBrowserTab(String uid, Component title, @Nullable RecipeBrowserEntry icon) {
        this(uid, title, icon, RecipeBrowserTabKind.INGREDIENTS);
    }
}
