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
                "draylar.tiered.reforge.ReforgeScreenHandler","container.repair","container.reforge",true)));
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
