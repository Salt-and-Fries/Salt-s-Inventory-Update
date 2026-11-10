package com.salts_inventory_update.compat.forge;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

/** Register the original untyped entity menus during Forge's registry event. */
public final class ReviewedEntityMenuRegistration {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"salts_inventory_compat");
    static {
        MENUS.register(FMLJavaModLoadingContext.get().getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ReviewedWolfOpening::opened);
    }
    private ReviewedEntityMenuRegistration() { }
    public static Supplier<MenuType<AbstractContainerMenu>> register(String id,Supplier<MenuType<AbstractContainerMenu>> factory){
        return MENUS.register(id,factory);
    }
}
