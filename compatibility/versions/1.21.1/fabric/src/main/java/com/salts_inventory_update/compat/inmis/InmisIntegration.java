package com.salts_inventory_update.compat.inmis;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
/** Even the three-slot Baby Backpack is storage, without classifying machine inputs as storage. */
public final class InmisIntegration {
    private InmisIntegration(){}
    public static void initializeCommon(){
        if(DesktopEnvironment.isModLoaded("inmis"))DesktopEnvironment.onServerStarting(server->register());
    }
    public static void register(){
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("inmis","backpack");
        if(BuiltInRegistries.MENU.containsKey(id))DesktopEnvironment.registerIntegration("inmis",()->
            DesktopMenuBindings.registerStorage(BuiltInRegistries.MENU.get(id),(menu,slot)->!(slot.container instanceof Inventory)));
    }
}
