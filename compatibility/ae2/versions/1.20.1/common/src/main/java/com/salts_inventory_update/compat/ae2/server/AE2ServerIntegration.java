package com.salts_inventory_update.compat.ae2.server;

import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

import com.salts_inventory_update.api.desktop.DesktopInventoryIdentity;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.desktop.DesktopItemSources;
import com.salts_inventory_update.api.server.desktop.DesktopCompanionLease;
import com.salts_inventory_update.api.server.desktop.DesktopCompanionSpec;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.menu.DesktopMenuPackets;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;

/** Loaded exclusively through Salt's descriptor; never a normal mod entrypoint. */
public final class AE2ServerIntegration {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AE2ServerIntegration.class);
    public static final net.minecraft.resources.ResourceLocation TOOLBOX_CHANNEL = com.salts_inventory_update.compat.ae2.menu.DesktopProtocol.TOOLBOX_CHANNEL;
    public static final net.minecraft.resources.ResourceLocation CPU_STATE_CHANNEL = com.salts_inventory_update.compat.ae2.menu.DesktopProtocol.CPU_STATE_CHANNEL;

    // One identity for every view of the same live portable item. Charge and
    // contained resources may change in place; replacing the source ends its lock.
    private static final Map<net.minecraft.world.item.ItemStack, String> ITEM_SOURCE_IDS = new WeakHashMap<>();

    private static DesktopItemSources.Source itemSource(net.minecraft.world.entity.player.Player player,
                                                       AbstractContainerMenu menu) {
        if (!(menu instanceof AEBaseMenu ae)
                || !(ae.getTarget() instanceof appeng.api.implementations.menuobjects.ItemMenuHost host)
                || host.getPlayer() != player) return null;
        Integer slot = host.getSlot();
        if (slot == null || slot < 0 || slot >= player.getInventory().getContainerSize()) return null;
        var source = host.getItemStack();
        if (source.isEmpty() || player.getInventory().getItem(slot) != source) return null;
        String identity;
        synchronized (ITEM_SOURCE_IDS) {
            identity = ITEM_SOURCE_IDS.computeIfAbsent(source, ignored -> UUID.randomUUID().toString());
        }
        return new DesktopItemSources.Source(slot, identity, source, current -> current == source);
    }

    private static final class ToolboxState {
        DesktopCompanionLease lease;
        net.minecraft.world.item.ItemStack source;
        int reportedSession = Integer.MIN_VALUE;
        int retryTicks;
        com.salts_inventory_update.compat.ae2.menu.DesktopCpuState cpuState;
    }

    private static appeng.menu.ToolboxMenu toolbox(AEBaseMenu menu) {
        if (menu instanceof appeng.menu.implementations.UpgradeableMenu<?> upgradeable) {
            return upgradeable.getToolbox();
        }
        if (menu instanceof appeng.menu.me.common.MEStorageMenu terminal) {
            return terminal.getToolbox();
        }
        return null;
    }

    private static void updateToolbox(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context) {
        var state = context.state();
        var toolbox = toolbox(context.menu());
        var host = toolbox == null ? null : toolbox.getMenuHost();
        var source = host == null ? null : host.getItemStack();
        if (state.lease != null && (!state.lease.isOpen() || source != state.source)) {
            state.lease.close();
            state.lease = null;
            state.retryTicks = 0;
        }
        if (state.lease == null && host != null && !source.isEmpty() && state.retryTicks-- <= 0) {
            var exactSource = source;
            var locator = appeng.menu.locator.MenuLocators.forInventorySlot(host.getSlot());
            var parentMenu = context.menu();
            state.retryTicks = 20;
            state.lease = context.acquireCompanion(new DesktopCompanionSpec(
                    DesktopInventoryIdentity.reference(exactSource),
                    new net.minecraft.world.SimpleMenuProvider((id, inventory, player) -> {
                        var companion = new appeng.menu.me.networktool.NetworkToolMenu(id, inventory, host);
                        companion.setLocator(locator);
                        companion.setDesktopToolboxCompanion(true);
                        return companion;
                    }, net.minecraft.network.chat.Component.translatable("gui.ae2.NetworkTool")),
                    player -> player == host.getPlayer() && host.getItemStack() == exactSource
                            && host.siu$isValid() && parentMenu.stillValid(player)))
                    .orElse(null);
            if (state.lease != null) {
                state.source = exactSource;
            }
        }
        int session = state.lease == null ? -1 : state.lease.sessionId();
        if (session != state.reportedSession) {
            context.sendToClient(TOOLBOX_CHANNEL, java.nio.ByteBuffer.allocate(4).putInt(session).array());
            state.reportedSession = session;

        }
    }

    private static void updateCpuState(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context, boolean force) {
        if (context.menu() instanceof appeng.menu.me.crafting.CraftingCPUMenu cpu) {
            var value = new com.salts_inventory_update.compat.ae2.menu.DesktopCpuState(
                    cpu.cpuStatusReady, cpu.cpuAvailable, cpu.hasActiveJob, cpu.cpuStatusSerial);
            if (force || !value.equals(context.state().cpuState)) {
                context.sendToClient(CPU_STATE_CHANNEL, value.encode());
                context.state().cpuState = value;
            }
        }
    }

    public static void initialize() {
        new AE2ServerIntegration().register();
    }

    private void register() {
        LOG.info("Registering {} AE desktop menu contracts", com.salts_inventory_update.compat.ae2.menu.NativeMenuRegistry.builders().size());
        com.salts_inventory_update.compat.ae2.menu.NativeMenuRegistry.builders().forEach((type,builder) -> registerMenu((MenuType<?>)type,builder));
    }

    @SuppressWarnings("unchecked")
    private void registerMenu(MenuType<?> untyped, Object untypedBuilder) {
        var type = (MenuType<AEBaseMenu>) untyped;
        DesktopItemSources.register(type, AE2ServerIntegration::itemSource);
        var builder=(com.salts_inventory_update.compat.ae2.menu.NativeDesktopBuilder)untypedBuilder;
        DesktopMenuBindings.register(type, new DesktopMenuBindings.SlotAccess() {
            @Override public int size(AbstractContainerMenu menu) { return menu.slots.size(); }
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot) {
                return slot instanceof appeng.menu.slot.FakeSlot || slot instanceof appeng.menu.slot.CraftingTermSlot;
            }
        }, true, (player, menu, bytes) -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                appeng.menu.locator.MenuLocators.writeToPacket(buffer, ((AEBaseMenu) menu).getLocator());
                byte[] locator = new byte[buffer.readableBytes()];
                buffer.readBytes(locator);
                // Different menu views have different logical topologies, while GesturePolicy still shares
                // the physical storage identity across those views.
                return (menu instanceof AEBaseMenu ae && ae.isDesktopToolboxCompanion()
                        ? "ae2:toolbox_companion:"
                        : "ae2:")
                        + net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()) + ":"
                        + player.level().dimension().location() + ":"
                        + java.util.HexFormat.of().formatHex(locator);
            } finally {
                buffer.release();
            }
        }, null, new DesktopMenuBindings.GesturePolicy() {
            @Override
            public Object physicalOwner(AbstractContainerMenu menu, Slot slot) {
                var aeMenu = (AEBaseMenu) menu;
                var toolbox = toolbox(aeMenu);
                if (aeMenu.getSlotSemantic(slot) == appeng.menu.SlotSemantics.TOOLBOX
                        && toolbox != null && toolbox.getMenuHost() != null) {
                    return toolbox.getMenuHost().getItemStack();
                }
                if (aeMenu instanceof appeng.menu.me.networktool.NetworkToolMenu
                        && aeMenu.getSlotSemantic(slot) == appeng.menu.SlotSemantics.STORAGE
                        && aeMenu.getTarget() instanceof appeng.items.contents.NetworkToolMenuHost tool) {
                    return tool.getItemStack();
                }
                if (slot instanceof AppEngSlot aeSlot) {
                    var inventory = aeSlot.getInventory();
                    return inventory instanceof ConfigMenuInventory config ? config.getDelegate() : inventory;
                }
                return slot.container;
            }

            @Override
            public int physicalIndex(AbstractContainerMenu menu, Slot slot) {
                return slot.getContainerSlot();
            }
        });
        DesktopServerApi.registerWindow(type, new DesktopServerWindowHandler<AEBaseMenu, ToolboxState>() {
            @Override
            public boolean supports(net.minecraft.server.level.ServerPlayer player, AEBaseMenu menu) {
                boolean channel=com.salts_inventory_update.compat.ae2.menu.DesktopUpdateNetwork.supports(player);
                LOG.debug("AE2 desktop support for {}: updateChannel={}, valid={}",menu.getType(),channel,menu.stillValid(player));
                return channel && menu.stillValid(player);
            }

            @Override
            public ToolboxState createState(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context) {
                return new ToolboxState();
            }

            @Override
            public void opened(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context) {
                com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets.opened(context);
                updateToolbox(context);
                updateCpuState(context, true);
                com.salts_inventory_update.compat.ae2.nativeview.RequesterDesktop.update(context);
            }

            @Override
            public void tick(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context) {
                com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets.tick(context.menu());
                updateToolbox(context);
                updateCpuState(context, false);
                com.salts_inventory_update.compat.ae2.nativeview.RequesterDesktop.update(context);
            }
            @Override public void closed(DesktopServerSessionContext<AEBaseMenu, ToolboxState> context) {
                com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets.closed(context.menu());
            }
        });
        DesktopServerApi.registerOpeningData(type, (player, menu, output) -> {

            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var identity = UUID.randomUUID();
                buffer.writeUUID(identity);
                buffer.writeBoolean(menu.isDesktopToolboxCompanion());
                builder.writeDesktopOpeningData(menu, buffer);
                if (buffer.readableBytes() > 32768) {
                    throw new IllegalArgumentException("AE menu opening data exceeds Salt's limit");
                }
                output.writeBytes(buffer);
                menu.setDesktopSyncId(identity);
            } finally {
                buffer.release();
            }
        });
        for (var channel : DesktopMenuPackets.actionChannels()) {
            DesktopServerApi.registerPayload(type, channel, request -> {
                var player = request.player();
                var menu = request.menu();
                if (!menu.stillValid(player)) {
                    request.reject("AE menu host is no longer valid");
                    return;
                }
                DesktopMenuPackets.withMenuAction(channel, request.data(), menu.containerId, packet -> {
                    // AE's native action handlers use containerMenu. Only this authenticated callback owns the scope.
                    var previous = player.containerMenu;
                    menu.setCarried(request.carriedStack());
                    player.containerMenu = menu;
                    try {
                        DesktopSessions.withSessionTransition(player, request.sessionId(), () -> packet.serverPacketData(player));
                    } finally {
                        try {
                            if (DesktopSessions.hasOpenSessionMatching(player, candidate -> candidate == menu)) {
                                request.setCarriedStack(menu.getCarried());
                            } else {
                                // A replaced native menu no longer owns this payload context.
                                // Keep the canonical cursor through Core's enclosing callback;
                                // native removed() has already cleared the old menu mirror.
                                menu.setCarried(player.inventoryMenu.getCarried().copy());
                            }
                            request.broadcastChanges();
                        } finally {
                            // Keep the committed shared cursor on this menu.
                            // Salt adopts the menu's cursor when the callback
                            // returns; clearing it here discards in-place mana
                            // container updates (and their carried tablet).
                            if (player.containerMenu == menu) player.containerMenu = previous;
                        }
                    }

                });

            });
        }
        com.salts_inventory_update.compat.ae2.nativeview.RequesterDesktop.register(type);
        com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets.register(type);
    }
}
