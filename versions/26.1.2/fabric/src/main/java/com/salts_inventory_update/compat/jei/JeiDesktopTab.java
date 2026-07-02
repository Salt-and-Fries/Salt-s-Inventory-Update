package com.salts_inventory_update.compat.jei;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public record JeiDesktopTab(String uid, Component title, @Nullable JeiDesktopEntry icon, JeiDesktopTabKind kind) {
    public JeiDesktopTab(String uid, Component title, @Nullable JeiDesktopEntry icon) {
        this(uid, title, icon, JeiDesktopTabKind.INGREDIENTS);
    }
}
