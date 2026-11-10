package com.salts_inventory_update.compat.tieredz;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.*;
import net.minecraft.resources.ResourceLocation;

public final class TieredZIntegration {
    private TieredZIntegration() { }
    private static final ResourceLocation TEXTURE=new ResourceLocation("tiered","textures/gui/reforging_screen.png");
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("tiered",()->
            GearMenuBridge.register(NativeMenuSupport.type("minecraft:anvil"),NativeMenuSupport.type("tiered:reforge"),
                nativeMenuClass(),"container.repair","container.reforge",true)));
    }
    private static String nativeMenuClass() {
        // Tierify is the 1.20.1 fork: it retains the tiered menu ID and artwork,
        // but moved the implementation. Do not construct the absent TieredZ class.
        return TieredZIntegration.class.getClassLoader().getResource("elocindev/tierify/screen/ReforgeScreenHandler.class")!=null
            ?"elocindev.tierify.screen.ReforgeScreenHandler":"draylar.tiered.reforge.ReforgeScreenHandler";
    }
    public static void initializeClient() {
        NativeMenuSupport.bindNative(NativeMenuSupport.type("tiered:reforge"),false);
        DesktopClientApi.onClientStarted(client->NativeMenuSupport.bindNative(NativeMenuSupport.type("tiered:reforge"),false));
        var emblem=new MockupNativeTemplate.Emblem(TEXTURE,6,7,40,36);
        NativeWindowPolish.installTemplate("tiered","minecraft:anvil",()->
            new TabbedGearTemplate(NativeBatchLayouts::anvil,false,true,TEXTURE,null,emblem));
        NativeWindowPolish.installTemplate("tiered","tiered:reforge",()->
            new TabbedGearTemplate(NativeBatchLayouts::reforge,true,false,TEXTURE,new DesktopWindowBounds(79,56,18,18),emblem));
    }
}
