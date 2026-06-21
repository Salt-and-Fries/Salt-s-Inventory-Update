package com.salts_inventory_update.api.client.desktop;

import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public interface DesktopInputContext<T extends AbstractContainerMenu, S> extends DesktopWindowContext<T, S> {
    boolean shiftDown();

    boolean ctrlDown();

    boolean altDown();

    boolean mouseButtonDown(int button);

    boolean sendMenuButton(int buttonId);

    boolean sendRename(String name);

    boolean clickSlot(int menuSlotId, int button, ContainerInput input);

    boolean quickMoveSlot(int menuSlotId);

    boolean toggleRecipeBook();

    boolean setRecipeBookSearch(String search);

    boolean sendPayload(Identifier channel, byte[] data);

    default boolean sendCustomPayload(Identifier channel, byte[] data) {
        return this.sendPayload(channel, data);
    }

    <P> boolean sendPayload(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec);
}
