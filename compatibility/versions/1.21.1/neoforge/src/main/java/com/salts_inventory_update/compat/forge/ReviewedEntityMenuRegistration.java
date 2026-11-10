package com.salts_inventory_update.compat.forge;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.neoforge.registries.DeferredRegister;

/** Register the original untyped entity menus during Forge's registry event. */
public final class ReviewedEntityMenuRegistration {
    private ReviewedEntityMenuRegistration() { }
    public static Supplier<MenuType<AbstractContainerMenu>> register(String id,Supplier<MenuType<AbstractContainerMenu>> factory){
        return com.salts_inventory_update.compat.platform.LoaderMenus.registerEntity(id,factory);
    }
}
