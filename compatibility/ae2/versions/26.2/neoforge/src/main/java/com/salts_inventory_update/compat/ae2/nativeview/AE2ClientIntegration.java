package com.salts_inventory_update.compat.ae2.nativeview;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.connection.ConnectionType;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;

/** Salt resolves this class only on clients, after AE registries exist. */
public final class AE2ClientIntegration {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AE2ClientIntegration.class);

    public static void initialize() {
        if (!com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin.active()) return;
        new AE2ClientIntegration().register();
    }

    private void register() {
        LOG.info("Registering {} AE desktop client definitions", MenuTypeBuilder.registeredBuilders().size());
        MenuTypeBuilder.registeredBuilders().forEach(this::registerMenu);
        AEWindow.installHeaderKeyboard();

    }

    @SuppressWarnings("unchecked")
    private void registerMenu(MenuType<?> untyped, MenuTypeBuilder<?, ?> builder) {
        var type = (MenuType<AEBaseMenu>) untyped;
        DesktopClientApi.registerWindow(type, new AEWindow(type));
        DesktopMenuFactories.register(type, (minecraft, player, id, data) -> {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess(),
                    ConnectionType.NEOFORGE);
            try {
                var owner = buffer.readUUID();
                boolean companion = buffer.readBoolean();
                var menu = builder.createDesktopMenu(id, player.getInventory(), buffer);
                if (buffer.isReadable()) {
                    throw new IllegalArgumentException("Trailing AE menu opening data");
                }
                menu.setDesktopSyncId(owner);
                menu.setDesktopToolboxCompanion(companion);

                return menu;
            } finally {
                buffer.release();
            }
        });
    }
}
