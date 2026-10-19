package com.salts_inventory_update.controlify;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Native Forge entry point, using the published Controlify: Forgified API. */
@Mod("salts_inventory_controlify")
public final class SaltsControlifyForge {
    public SaltsControlifyForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.install();
    }
    private static final class Client {
        static void install() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(
                    (FMLClientSetupEvent event) -> event.enqueueWork(SaltsControlifyClient::initialize));
        }
    }
}
