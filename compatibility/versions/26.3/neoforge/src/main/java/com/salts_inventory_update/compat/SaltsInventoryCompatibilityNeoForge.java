package com.salts_inventory_update.compat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@Mod("salts_inventory_compat")
public final class SaltsInventoryCompatibilityNeoForge {
    public SaltsInventoryCompatibilityNeoForge(IEventBus bus) {
        com.salts_inventory_update.compat.sophisticated.SophisticatedCompatBootstrap.initialize(bus);
        CompatibilityBootstrap.initializeCommon();
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(CompatibilityBootstrap::initializeClient));
    }
}
