package com.salts_inventory_update.server;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativePayloadRegistrationTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void scopedNativeActionsDoNotReplaceNativeOpeningOrClaimAnExplicitWindow() {
        var type=MenuType.HOPPER;var channel=new ResourceLocation("native_payload_test","action");
        DesktopServerApi.registerNativePayload(type,channel,c->{});
        assertNotNull(DesktopServerApi.findPayloadHandler(type,channel));
        assertFalse(DesktopServerApi.hasWindowSupport(type),"Native transport must still own its opening data");
        assertThrows(RuntimeException.class,()->DesktopServerApi.registerPayload(type,channel,c->{}));
    }
    @Test void failedIntegrationRollsBackItsNativeActionAndLeavesTheChannelReusable() {
        var type=MenuType.GENERIC_3x3;var channel=new ResourceLocation("native_payload_test","rollback");
        assertThrows(IllegalStateException.class,()->DesktopRegistrationScope.run("native-payload-rollback",()->{
            DesktopServerApi.registerNativePayload(type,channel,c->{});
            throw new IllegalStateException("Simulated optional integration failure");
        }));
        assertNull(DesktopServerApi.findPayloadHandler(type,channel));
        assertDoesNotThrow(()->DesktopServerApi.registerNativePayload(type,channel,c->{}));
        assertFalse(DesktopServerApi.hasWindowSupport(type));
    }
}
