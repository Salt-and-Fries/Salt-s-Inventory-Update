package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = "salts_inventory_controlify_qa", dist = Dist.CLIENT)
public final class QaClientInitializer {
    public QaClientInitializer(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            if (Boolean.getBoolean("salts_inventory_controlify.qa")) ControlifyGameplayHarness.initialize();
        }));
    }
}
