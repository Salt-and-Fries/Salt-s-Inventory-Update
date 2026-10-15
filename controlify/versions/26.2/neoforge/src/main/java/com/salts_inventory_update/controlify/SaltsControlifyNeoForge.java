package com.salts_inventory_update.controlify;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** NeoForge loads this compatibility mod only on the physical client. */
@Mod(value = "salts_inventory_controlify", dist = Dist.CLIENT)
public final class SaltsControlifyNeoForge {
    public SaltsControlifyNeoForge(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(SaltsControlifyClient::initialize));
    }
}
