package com.salts_inventory_update.compat.powah;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
@EventBusSubscriber(modid="salts_inventory_compat",value=Dist.CLIENT)
public final class PowahReloadEvents {
    @SubscribeEvent public static void reloaders(AddClientReloadListenersEvent event) {
        if(net.neoforged.fml.ModList.get().isLoaded("powah"))event.addListener(
            Identifier.fromNamespaceAndPath("salts_inventory_compat","powah_windows"),
            (ResourceManagerReloadListener)resources->PowahClient.reloadResources());
    }
}
