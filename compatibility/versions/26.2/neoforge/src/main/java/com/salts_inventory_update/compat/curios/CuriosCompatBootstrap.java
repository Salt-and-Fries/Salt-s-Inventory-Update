package com.salts_inventory_update.compat.curios;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.salts_inventory_update.compat.CompatibilityLog;

/** The optional native implementation is resolved only after its supported dependency is present. */
public final class CuriosCompatBootstrap {
    private CuriosCompatBootstrap() { }
    public static void initialize(IEventBus bus) {
        var mod = ModList.get().getModContainerById("curios");
        if (mod.isEmpty()) return;
        if (!mod.get().getModInfo().getVersion().toString().equals("16.0.0+26.2")) {
            CompatibilityLog.log("Curios desktop adapter supports 16.0.0+26.2; native UI retained for {}", mod.get().getModInfo().getVersion());
            return;
        }
        CuriosIntegration.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.salts_inventory_update.compat.CompatibilityBootstrap.initialize("curios",
                "com.salts_inventory_update.compat.curios.CuriosIntegration", "initializeCommon")));
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
            com.salts_inventory_update.compat.CompatibilityBootstrap.initialize("curios",
                "com.salts_inventory_update.compat.curios.CuriosClientIntegration", "initialize")));
    }
}
