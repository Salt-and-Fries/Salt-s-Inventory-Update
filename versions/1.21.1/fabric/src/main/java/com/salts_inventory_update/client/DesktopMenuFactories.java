package com.salts_inventory_update.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Internal client factories for menu types that require their loader-specific opening data. */
public final class DesktopMenuFactories {
    private static final Map<MenuType<?>, Factory> FACTORIES = new LinkedHashMap<>();

    private DesktopMenuFactories() {
    }

    public static synchronized void register(MenuType<?> menuType, Factory factory) {
        FACTORIES.put(Objects.requireNonNull(menuType, "menuType"), Objects.requireNonNull(factory, "factory"));
    }

    public static synchronized @Nullable AbstractContainerMenu create(
        MenuType<?> menuType,
        Minecraft minecraft,
        LocalPlayer player,
        int containerId,
        byte[] openingData
    ) {
        Factory factory = FACTORIES.get(menuType);
        return factory == null ? null : factory.create(minecraft, player, containerId, openingData.clone());
    }

    public static synchronized boolean hasFactory(MenuType<?> menuType) {
        return FACTORIES.containsKey(menuType);
    }

    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(Minecraft minecraft, LocalPlayer player, int containerId, byte[] openingData);
    }
}
