package com.salts_inventory_update.compat.ae2.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuLocators;
import com.salts_inventory_update.compat.ae2.menu.DesktopMenuBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Only the fork's extra desktop-opening contract. Existing native opening,
 * factories, locator codecs and initial-data strategies stay owned by AE2.
 */
@Pseudo
@Mixin(targets = "appeng.menu.implementations.MenuTypeBuilder", remap = false)
public abstract class NativeMenuTypeBuilderMixin implements DesktopMenuBuilder {
    @Shadow @Final private Class<Object> hostInterface;
    @Shadow @Final private MenuTypeBuilder.MenuFactory<AEBaseMenu, Object> factory;
    @Shadow private MenuTypeBuilder.InitialDataSerializer<Object> initialDataSerializer;
    @Shadow private MenuTypeBuilder.InitialDataDeserializer<AEBaseMenu, Object> initialDataDeserializer;

    @Override
    public void salts$writeOpeningData(AEBaseMenu menu, RegistryFriendlyByteBuf output) {
        var locator = menu.getLocator();
        if (locator == null) {
            throw new IllegalStateException("AE menu has no host locator");
        }
        MenuLocators.writeToPacket(output, locator);
        output.writeBoolean(menu.isReturnedFromSubScreen());
        if (initialDataSerializer != null) {
            Object host = locator.locate(menu.getPlayer(), hostInterface);
            if (host == null) {
                throw new IllegalStateException("Menu host is unavailable");
            }
            initialDataSerializer.serializeInitialData(host, output);
        }
    }

    @Override
    public AEBaseMenu salts$createDesktopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf input) {
        var locator = MenuLocators.readFromPacket(input);
        Object host = locator.locate(inventory.player, hostInterface);
        if (host == null) {
            // A detached model cannot close an unrelated native container.
            throw new IllegalStateException("Couldn't find AE desktop menu host at " + locator);
        }
        var menu = factory.create(containerId, inventory, host);
        menu.setLocator(locator);
        menu.setReturnedFromSubScreen(input.readBoolean());
        if (initialDataDeserializer != null) {
            initialDataDeserializer.deserializeInitialData(host, menu, input);
        }
        return menu;
    }
}
