package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("salts_inventory_controlify_qa")
public final class QaClientInitializer {
    public QaClientInitializer() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent event) ->
                    event.enqueueWork(() -> {
                        if (Boolean.getBoolean("salts_inventory_controlify.qa")) ControlifyGameplayHarness.initialize();
                    }));
        }
    }
}
