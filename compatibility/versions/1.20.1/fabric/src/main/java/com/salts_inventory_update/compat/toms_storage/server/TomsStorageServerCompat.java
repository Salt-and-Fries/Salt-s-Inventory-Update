package com.salts_inventory_update.compat.toms_storage.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.compat.toms_storage.TomsStoragePayloads;
import com.salts_inventory_update.compat.toms_storage.TomsStoragePayloads.TerminalEntry;
import com.salts_inventory_update.compat.toms_storage.TomsStorageReflect;
import com.salts_inventory_update.compat.CompatibilityLog;

public final class TomsStorageServerCompat {
    private static boolean lifecycleHookRegistered;

    private TomsStorageServerCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized void initialize() {
        if (!TomsStorageCompat.loaded()) {
            return;
        }

        if (!lifecycleHookRegistered) {
            lifecycleHookRegistered = true;
            TomsStorageCompat.info("server compat initialize deferredUntil=server-starting");
            DesktopEnvironment.onServerStarting(server -> registerAll("server-starting"));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static synchronized void registerAll(String phase) {
        try {
            DesktopEnvironment.registerIntegration(TomsStorageCompat.MOD_ID, () -> registerMenus(phase));
        } catch (RuntimeException | LinkageError failure) {
            CompatibilityLog.LOGGER.error("Tom's Storage server registration failed", failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerMenus(String phase) {
        int count = 0;
        count += register(TomsStorageCompat.STORAGE_TERMINAL) ? 1 : 0;
        count += register(TomsStorageCompat.CRAFTING_TERMINAL) ? 1 : 0;
        count += register(TomsStorageCompat.INVENTORY_CONFIGURATOR) ? 1 : 0;
        count += register(TomsStorageCompat.LEVEL_EMITTER) ? 1 : 0;
        count += register(TomsStorageCompat.INVENTORY_LINK) ? 1 : 0;
        count += register(TomsStorageCompat.ITEM_FILTER) ? 1 : 0;
        count += register(TomsStorageCompat.TAG_ITEM_FILTER) ? 1 : 0;
        CompatibilityLog.log("Tom's Storage server compat registered phase={} menuCount={}", phase, count);
        TomsStorageCompat.info("server compat registered phase={} menuCount={}", phase, count);
        if (count < TomsStorageCompat.NATIVE_MENU_COUNT) {
            CompatibilityLog.warn("Tom's Storage server compat registered fewer menus than expected phase={} menuCount={} expected=7", phase, count);
            TomsStorageCompat.warn("server compat registered fewer menus than expected phase={} menuCount={} expected=7", phase, count);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean register(String menuId) {
        MenuType menuType = TomsStorageCompat.menu(menuId);
        if (menuType == null) {
            return false;
        }

        DesktopServerWindowHandler<?, ?> existing = DesktopServerApi.findWindowHandler(menuType);
        if (existing instanceof ServerWindowHandler) return true;
        DesktopServerApi.registerWindow(menuType, new ServerWindowHandler());
        DesktopServerApi.registerPayload(menuType, TomsStorageCompat.NBT_CHANNEL, TomsStorageServerCompat::handleNbt);
        DesktopServerApi.registerPayload(menuType, TomsStorageCompat.TERMINAL_ACTION_CHANNEL, TomsStorageServerCompat::handleTerminalAction);
        CompatibilityLog.log(
            "Tom's Storage server compat handlers registered menuId={} registryKey={} menuType={}",
            menuId,
            BuiltInRegistries.MENU.getKey(menuType),
            menuType
        );
        TomsStorageCompat.info(
            "server handlers registered menuId={} registryKey={} menuType={}",
            menuId,
            BuiltInRegistries.MENU.getKey(menuType),
            menuType
        );
        return true;
    }

    public static long syncSession(
        ServerPlayer player,
        int sessionId,
        AbstractContainerMenu menu,
        long previousHash,
        java.util.function.BiConsumer<net.minecraft.resources.ResourceLocation, byte[]> sender
    ) {
        MenuType<?> menuType = menu.getType();
        if (TomsStorageCompat.isTerminal(menuType)) {
            TerminalSnapshotData snapshot = terminalSnapshot(menu);
            if (snapshot.hash() != previousHash) {
                byte[] data = TomsStoragePayloads.writeTerminalSnapshot(RegistryAccess.EMPTY, snapshot.entries());
                sender.accept(TomsStorageCompat.TERMINAL_SNAPSHOT_CHANNEL, data);
                CompatibilityLog.log(
                    "Tom's Storage terminal snapshot sync player={} session={} menu={} entries={} bytes={} oldHash={} newHash={}",
                    player.getName().getString(),
                    sessionId,
                    BuiltInRegistries.MENU.getKey(menuType),
                    snapshot.entries().size(),
                    data.length,
                    previousHash,
                    snapshot.hash()
                );
                TomsStorageCompat.info(
                    "server terminal snapshot sync player={} session={} menu={} entries={} bytes={} oldHash={} newHash={}",
                    player.getName().getString(),
                    sessionId,
                    BuiltInRegistries.MENU.getKey(menuType),
                    snapshot.entries().size(),
                    data.length,
                    previousHash,
                    snapshot.hash()
                );
                if (TomsStoragePayloads.readTerminalSnapshot(RegistryAccess.EMPTY, data).truncated()) {
                    CompatibilityLog.warn(
                        "Tom's Storage terminal snapshot truncated player={} session={} menu={} entries={}",
                        player.getName().getString(),
                        sessionId,
                        BuiltInRegistries.MENU.getKey(menuType),
                        snapshot.entries().size()
                    );
                    TomsStorageCompat.warn(
                        "server terminal snapshot truncated player={} session={} menu={} entries={}",
                        player.getName().getString(),
                        sessionId,
                        BuiltInRegistries.MENU.getKey(menuType),
                        snapshot.entries().size()
                    );
                }
                return snapshot.hash();
            }
            return previousHash;
        }

        if (TomsStorageCompat.isMenu(menuType, TomsStorageCompat.LEVEL_EMITTER)) {
            Object tile=TomsStorageReflect.field(menu,"te");
            try {
                CompoundTag tag=new CompoundTag();
                tag.putInt("count",((Number)tile.getClass().getMethod("getCount").invoke(tile)).intValue());
                tag.putBoolean("lessThan",(Boolean)tile.getClass().getMethod("isLessThan").invoke(tile));
                byte[] data=TomsStoragePayloads.writeNbt(tag);
                long hash=hashBytes(data);
                if(hash!=previousHash)sender.accept(TomsStorageCompat.EMITTER_SNAPSHOT_CHANNEL,data);
                return hash;
            } catch(ReflectiveOperationException failure) { throw new IllegalStateException("Native emitter snapshot failed",failure); }
        }

        if (TomsStorageCompat.isMenu(menuType, TomsStorageCompat.INVENTORY_LINK)) {
            List<TomsStoragePayloads.LinkChannel> channels = TomsStorageReflect.inventoryLinkChannels(player);
            byte[] data = TomsStoragePayloads.writeLinkSnapshot(RegistryAccess.EMPTY, channels, TomsStorageReflect.inventoryLinkSelected(menu));
            Object tile=TomsStorageReflect.field(menu,"te");
            CompoundTag metadata=new CompoundTag();
            try {
                metadata.putInt("level",((Number)tile.getClass().getMethod("getBeaconLevel").invoke(tile)).intValue());
                metadata.putBoolean("remote",(Boolean)tile.getClass().getMethod("isRemote").invoke(tile));
            } catch(ReflectiveOperationException failure) { throw new IllegalStateException("Native link metadata failed",failure); }
            byte[] nativeState=TomsStoragePayloads.writeNbt(metadata);
            long hash = hashBytes(data) ^ Long.rotateLeft(hashBytes(nativeState),17);
            if (hash != previousHash) {
                sender.accept(TomsStorageCompat.LINK_SNAPSHOT_CHANNEL, data);
                sender.accept(TomsStorageCompat.LINK_METADATA_CHANNEL,nativeState);
                CompatibilityLog.log(
                    "Tom's Storage inventory link snapshot sync player={} session={} channels={} bytes={} oldHash={} newHash={}",
                    player.getName().getString(),
                    sessionId,
                    channels.size(),
                    data.length,
                    previousHash,
                    hash
                );
                return hash;
            }
            return previousHash;
        }

        if (TomsStorageCompat.isMenu(menuType, TomsStorageCompat.TAG_ITEM_FILTER)) {
            List<String> tags = TomsStorageReflect.tagFilterTags(menu);
            byte[] data = TomsStoragePayloads.writeTagSnapshot(tags);
            long hash = hashBytes(data);
            if (hash != previousHash) {
                sender.accept(TomsStorageCompat.TAG_SNAPSHOT_CHANNEL, data);
                CompatibilityLog.log(
                    "Tom's Storage tag filter snapshot sync player={} session={} tags={} bytes={} oldHash={} newHash={}",
                    player.getName().getString(),
                    sessionId,
                    tags.size(),
                    data.length,
                    previousHash,
                    hash
                );
                return hash;
            }
            return previousHash;
        }

        return previousHash;
    }

    private static void handleNbt(DesktopServerPayloadContext<AbstractContainerMenu> context) {
        CompatibilityLog.log(
            "Tom's Storage server NBT payload player={} session={} menu={} bytes={}",
            context.player().getName().getString(),
            context.sessionId(),
            BuiltInRegistries.MENU.getKey(context.menu().getType()),
            context.data().length
        );
        TomsStorageCompat.info(
            "server NBT payload player={} session={} menu={} bytes={}",
            context.player().getName().getString(),
            context.sessionId(),
            BuiltInRegistries.MENU.getKey(context.menu().getType()),
            context.data().length
        );
        boolean handled = TomsStorageReflect.receiveNbt(
            context.player(),
            context.menu(),
            TomsStoragePayloads.readNbt(context.data())
        );
        if (handled) {
            context.broadcastChanges();
        }
    }

    private static void handleTerminalAction(DesktopServerPayloadContext<AbstractContainerMenu> context) {
        if (!TomsStorageCompat.isTerminal(context.menu().getType())) {
            return;
        }

        TomsStoragePayloads.TerminalAction action = TomsStoragePayloads.readTerminalAction(RegistryAccess.EMPTY, context.data());
        CompatibilityLog.log(
            "Tom's Storage terminal action player={} session={} menu={} action={} modifier={} stack={} quantity={} bytes={}",
            context.player().getName().getString(),
            context.sessionId(),
            BuiltInRegistries.MENU.getKey(context.menu().getType()),
            action.action(),
            action.modifier(),
            action.stack(),
            action.quantity(),
            context.data().length
        );
        TomsStorageCompat.info(
            "server terminal action player={} session={} menu={} action={} modifier={} stack={} quantity={} bytes={}",
            context.player().getName().getString(),
            context.sessionId(),
            BuiltInRegistries.MENU.getKey(context.menu().getType()),
            action.action(),
            action.modifier(),
            action.stack(),
            action.quantity(),
            context.data().length
        );
        context.menu().setCarried(context.carriedStack());
        ItemStack carriedBefore = context.menu().getCarried().copy();
        boolean handled = TomsStorageReflect.invokeTerminalAction(
            context.menu(),
            action.action(),
            action.modifier(),
            action.stack(),
            action.quantity()
        );
        ItemStack carriedAfter = context.menu().getCarried().copy();
        TomsStorageCompat.info(
            "server terminal action result player={} session={} handled={} carriedBefore={} carriedAfter={}",
            context.player().getName().getString(),
            context.sessionId(),
            handled,
            carriedBefore,
            carriedAfter
        );
        if (handled) {
            context.setCarriedStack(carriedAfter);
            context.broadcastChanges();
        }
    }

    private static TerminalSnapshotData terminalSnapshot(AbstractContainerMenu menu) {
        List<TerminalEntry> entries = new ArrayList<>();
        for (Object storedStack : TomsStorageReflect.terminalStoredStacks(menu)) {
            ItemStack stack = TomsStorageReflect.storedStack(storedStack);
            long quantity = TomsStorageReflect.storedQuantity(storedStack);
            if (stack != null && !stack.isEmpty() && quantity > 0) {
                entries.add(new TerminalEntry(stack.copyWithCount(1), quantity));
            }
        }

        entries.sort(Comparator
            .comparing((TerminalEntry entry) -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString())
            .thenComparing(entry -> entry.stack().getHoverName().getString())
            .thenComparingLong(TerminalEntry::quantity)
        );

        long hash = 1469598103934665603L;
        for (TerminalEntry entry : entries) {
            hash = (hash ^ BuiltInRegistries.ITEM.getId(entry.stack().getItem())) * 1099511628211L;
            hash = (hash ^ entry.stack().getDamageValue()) * 1099511628211L;
            hash = (hash ^ (entry.stack().getTag() == null ? 0 : entry.stack().getTag().hashCode())) * 1099511628211L;
            hash = (hash ^ entry.quantity()) * 1099511628211L;
        }
        hash = (hash ^ entries.size()) * 1099511628211L;
        return new TerminalSnapshotData(entries, hash);
    }

    private static long hashBytes(byte[] data) {
        long hash = 1469598103934665603L;
        for (byte value : data) {
            hash = (hash ^ (value & 0xFF)) * 1099511628211L;
        }
        return hash;
    }

    private record TerminalSnapshotData(List<TerminalEntry> entries, long hash) {
    }

    private static final class ServerWindowHandler implements DesktopServerWindowHandler<AbstractContainerMenu, ServerWindowState> {
        @Override
        public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu) {
            return TomsStorageCompat.isTerminal(menu.getType());
        }

        @Override
        public ServerWindowState createState(DesktopServerSessionContext<AbstractContainerMenu, ServerWindowState> context) {
            return new ServerWindowState();
        }

        @Override
        public void tick(DesktopServerSessionContext<AbstractContainerMenu, ServerWindowState> context) {
            ServerWindowState state = context.state();
            state.snapshotHash = syncSession(
                context.player(),
                context.sessionId(),
                context.menu(),
                state.snapshotHash,
                context::sendToClient
            );
        }
    }

    private static final class ServerWindowState {
        private long snapshotHash = Long.MIN_VALUE;
    }
}
