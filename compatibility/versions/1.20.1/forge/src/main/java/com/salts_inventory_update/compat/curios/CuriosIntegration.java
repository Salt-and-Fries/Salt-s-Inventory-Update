package com.salts_inventory_update.compat.curios;

import java.util.UUID;
import java.util.function.Supplier;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import top.theillusivec4.curios.common.network.NetworkHandler;
import net.minecraftforge.registries.DeferredRegister;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncRender;

/** Common registration and bounded, authenticated actions; no Core implementation dependency. */
public final class CuriosIntegration {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "salts_inventory_compat");
    public static final Supplier<MenuType<CuriosDesktopMenu>> MENU = MENUS.register("curios_accessories",
        () -> IForgeMenuType.create(CuriosDesktopMenu::new));
    public static final ResourceLocation COSMETICS = id("curios_cosmetics");
    public static final ResourceLocation VISIBILITY = id("curios_visibility");
    private CuriosIntegration() { }
    static ResourceLocation id(String path) { return new ResourceLocation("salts_inventory_compat", path); }

    static SimpleChannel network;
    public static void register(IEventBus bus) {
        MENUS.register(bus);
        network = NetworkRegistry.ChannelBuilder.named(id("curios_desktop"))
            .networkProtocolVersion(() -> "1").clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals).simpleChannel();
        network.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((payload, buffer) -> buffer.writeUUID(payload.owner()))
            .decoder(buffer -> new Open(buffer.readUUID()))
            .consumerMainThread((payload, supplied) -> {
                var context = supplied.get(); var player = context.getSender();
                if (player != null && DesktopSessions.shouldCapture(player)
                    && !DesktopSessions.hasOpenSessionMatching(player, menu -> menu instanceof CuriosDesktopMenu))
                    open(player, payload.owner(), false);
                context.setPacketHandled(true);
            }).add();
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
                    return ((CuriosDesktopMenu) menu).accessory(slot).backing;
                }
                @Override public int physicalIndex(AbstractContainerMenu menu, Slot slot) {
                    return ((CuriosDesktopMenu) menu).accessory(slot).accessoryIndex;
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
            var slot = context.menu().accessory(context.menu().getSlot(slotId));
            if (!slot.displayed() || !slot.canToggleRender()) return;
            var group = CuriosDesktopMenu.group(context.player(), slot.group.id());
            if (group == null || slot.accessoryIndex >= group.getRenders().size()) return;
            boolean value = !group.getRenders().get(slot.accessoryIndex);
            group.getRenders().set(slot.accessoryIndex, value);
            NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(context::player),
                new SPacketSyncRender(context.player().getId(), slot.group.id(), slot.accessoryIndex, value));
            context.broadcastChanges();
        });
    }
    public record Open(UUID owner) { }
}
