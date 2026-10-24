package com.salts_inventory_update.compat.curios;

import java.util.UUID;
import java.util.function.Supplier;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncRender;

/** Common registration and bounded, authenticated actions; no Core implementation dependency. */
public final class CuriosIntegration {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "salts_inventory_compat");
    public static final Supplier<MenuType<CuriosDesktopMenu>> MENU = MENUS.register("curios_accessories",
        () -> IMenuTypeExtension.create(CuriosDesktopMenu::new));
    public static final Identifier COSMETICS = id("curios_cosmetics");
    public static final Identifier VISIBILITY = id("curios_visibility");
    private CuriosIntegration() { }
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath("salts_inventory_compat", path); }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("curios-desktop-1")
            .playToServer(Open.TYPE, Open.CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player && DesktopSessions.shouldCapture(player)
                    && !DesktopSessions.hasOpenSessionMatching(player, menu -> menu instanceof CuriosDesktopMenu))
                    open(player, payload.owner(), false);
            })));
    }
    static void open(ServerPlayer player, UUID owner, boolean cosmetics) {
        DesktopSessions.openMenuSession(player, new SimpleMenuProvider(
            (id, inventory, ignored) -> new CuriosDesktopMenu(id, inventory, owner, cosmetics),
            Component.translatable("curios.name")), null);
    }
    public static void initializeCommon() {
        var type = MENU.get();
        DesktopMenuBindings.register(type, menu -> menu.slots.size(), true,
            (player, menu, data) -> "curios:" + player.getUUID(), null, new DesktopMenuBindings.GesturePolicy() {
                @Override public Object physicalOwner(AbstractContainerMenu menu, Slot slot) {
                    return ((CuriosDesktopMenu.AccessorySlot) slot).backing;
                }
                @Override public int physicalIndex(AbstractContainerMenu menu, Slot slot) {
                    return ((CuriosDesktopMenu.AccessorySlot) slot).accessoryIndex;
                }
            });
        DesktopServerApi.registerOpeningData(type, (player, menu, buffer) -> menu.writeOpening(buffer));
        DesktopServerApi.registerWindow(type, new DesktopServerWindowHandler<CuriosDesktopMenu, Void>() {
            @Override public void tick(DesktopServerSessionContext<CuriosDesktopMenu, Void> context) {
                if (!context.menu().topologyChanged()) return;
                var old = context.menu();
                // Replacing a real session invalidates old gesture tokens before slot indices change.
                DesktopSessions.withSessionTransition(context.player(), context.sessionId(),
                    () -> open(context.player(), old.ownerWindow, old.cosmetics));
            }
        });
        DesktopServerApi.registerPayload(type, COSMETICS, context -> {
            byte[] data = context.data();
            if (data.length != 1 || data[0] < 0 || data[0] > 1 || context.menu().topologyChanged()) return;
            context.menu().cosmetics = data[0] == 1;
            context.broadcastChanges();
        });
        DesktopServerApi.registerPayload(type, VISIBILITY, context -> {
            byte[] data = context.data();
            if (data.length != 4 || context.menu().topologyChanged()) return;
            int slotId = java.nio.ByteBuffer.wrap(data).getInt();
            if (slotId < 0 || slotId >= context.menu().slots.size()) return;
            var slot = (CuriosDesktopMenu.AccessorySlot) context.menu().getSlot(slotId);
            if (!slot.displayed() || !slot.canToggleRender()) return;
            var group = CuriosDesktopMenu.group(context.player(), slot.group.id());
            if (group == null || slot.accessoryIndex >= group.getRenders().size()) return;
            boolean value = !group.getRenders().get(slot.accessoryIndex);
            group.getRenders().set(slot.accessoryIndex, value);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(context.player(),
                new SPacketSyncRender(context.player().getId(), slot.group.id(), slot.accessoryIndex, value));
            context.broadcastChanges();
        });
    }
    public record Open(UUID owner) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(id("open_curios_accessories"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = new StreamCodec<>() {
            @Override public Open decode(RegistryFriendlyByteBuf buffer) { return new Open(buffer.readUUID()); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Open payload) { buffer.writeUUID(payload.owner()); }
        };
        @Override public Type<Open> type() { return TYPE; }
    }
}
