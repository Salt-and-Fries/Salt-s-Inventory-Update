package com.salts_inventory_update.compat;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
@Mod("salts_inventory_compat")
public final class SaltsInventoryCompatibilityForge {
    public SaltsInventoryCompatibilityForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        CompatibilityBootstrap.initializeCommon();
        // Mod presence is reliable after Forge completes mod construction.
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                CompatibilityBootstrap.initialize("ae2", "com.salts_inventory_update.compat.ae2.AE2Bootstrap", "initializeCommon")));
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(CompatibilityBootstrap::initializeClient));
        com.salts_inventory_update.compat.sophisticated.SophisticatedCompatBootstrap.initialize(bus);
    }
}
