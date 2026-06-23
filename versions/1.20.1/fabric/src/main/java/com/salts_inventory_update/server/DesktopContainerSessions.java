package com.salts_inventory_update.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadHandler;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;

public final class DesktopContainerSessions {
    private static final int MAX_SESSIONS = 16;
    private static final int DORMANT_GHOST_REOPEN_INTERVAL_TICKS = 10;
    private static final int CRAFTER_INPUT_SLOT_COUNT = 9;
    private static final int CRAFTER_SLOT_STATE_ENABLED_FLAG = 16;
    private static final int BEACON_EFFECT_ID_MASK = 0xFFFF;
    private static final int BEACON_SECONDARY_EFFECT_SHIFT = 16;
    private static final Map<UUID, PlayerSessions> PLAYERS = new LinkedHashMap<>();
    private static final Map<UUID, String> PENDING_USE_TARGETS = new LinkedHashMap<>();

    private DesktopContainerSessions() {
    }

    public static void initialize() {
        DesktopDebug.log("server desktop session networking initialized");
        register(DesktopReadyPayload.TYPE, DesktopReadyPayload::new, (player, payload) -> setReady(player, payload.ready()));
        register(DesktopClickPayload.TYPE, DesktopClickPayload::new, DesktopContainerSessions::click);
        register(DesktopQuickMovePayload.TYPE, DesktopQuickMovePayload::new, DesktopContainerSessions::quickMove);
        register(DesktopButtonPayload.TYPE, DesktopButtonPayload::new, DesktopContainerSessions::button);
        register(DesktopPlaceRecipePayload.TYPE, DesktopPlaceRecipePayload::new, DesktopContainerSessions::placeRecipe);
        register(DesktopRenamePayload.TYPE, DesktopRenamePayload::new, DesktopContainerSessions::rename);
        register(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload::new, (player, payload) -> closeSession(player, payload.sessionId(), true));
        register(DesktopSessionPinPayload.TYPE, DesktopSessionPinPayload::new, DesktopContainerSessions::setSessionPin);
        register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload::new, DesktopContainerSessions::setSessionVisibility);
        register(DesktopCustomPayload.TYPE, DesktopCustomPayload::new, DesktopContainerSessions::customPayload);
        register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload::new, DesktopContainerSessions::carried);
        register(InventorySlotPurchasePayload.TYPE, InventorySlotPurchasePayload::new, (player, payload) -> InventoryExpansion.tryPurchase(player));
        ServerTickEvents.END_SERVER_TICK.register(DesktopContainerSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player));
    }

    private static <P extends DesktopPacket> void register(ResourceLocation id, Function<FriendlyByteBuf, P> decoder, BiConsumer<ServerPlayer, P> handler) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, networkHandler, buf, sender) -> {
            P payload = decoder.apply(buf);
            server.execute(() -> handler.accept(player, payload));
        });
    }

    public static boolean shouldCapture(ServerPlayer player) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null && sessions.ready) {
            return true;
        }

        try {
            if (ServerPlayNetworking.canSend(player, DesktopOpenSessionPayload.TYPE)) {
                sessions(player).ready = true;
                DesktopDebug.warn("server desktop capture enabled player={} reason=client-can-receive", player.getName().getString());
                return true;
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            DesktopDebug.warn("server desktop capture unavailable player={} reason={}", player.getName().getString(), exception.toString());
        }

        return false;
    }

    public static void captureUseTarget(ServerPlayer player, BlockHitResult hitResult) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        String sourceKey = sourceKeyForBlock(player, hitResult.getBlockPos());
        PENDING_USE_TARGETS.put(player.getUUID(), sourceKey);
        DesktopDebug.trace("server use target player={} key={}", player.getName().getString(), sourceKey);
    }

    public static void clearUseTarget(ServerPlayer player) {
        PENDING_USE_TARGETS.remove(player.getUUID());
    }

    public static boolean hasOpenSessionForContainer(Player player, Container container) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        PlayerSessions sessions = PLAYERS.get(serverPlayer.getUUID());
        if (sessions == null || !sessions.ready) {
            return false;
        }

        for (Session session : sessions.sessions.values()) {
            Container sessionContainer = containerForMenu(session.menu);
            if (sessionContainer != null && containsContainer(sessionContainer, container)) {
                DesktopDebug.trace(
                    "server chest opener owned by desktop player={} session={} title={}",
                    serverPlayer.getName().getString(),
                    session.sessionId,
                    session.title.getString()
                );
                return true;
            }
        }

        return false;
    }

    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        return openMenuSession(player, provider, null, true, false, true);
    }

    private static @Nullable OptionalInt openMenuSession(
        ServerPlayer player,
        MenuProvider provider,
        @Nullable String forcedSourceKey,
        boolean toggleExisting,
        boolean ghostPinned,
        boolean visibleToClient
    ) {
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = sessions(player);
        String sourceKey = forcedSourceKey == null ? sourceKeyForProvider(player, provider) : forcedSourceKey;
        if (sourceKey == null) {
            sourceKey = PENDING_USE_TARGETS.get(player.getUUID());
        }

        if (toggleExisting && sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
            return OptionalInt.empty();
        }

        AbstractContainerMenu menu = provider.createMenu(nextSessionId(player), player.getInventory(), player);
        if (menu == null) {
            DesktopDebug.warn("server capture skipped player={} title={} reason=null-menu", player.getName().getString(), provider.getDisplayName().getString());
            return OptionalInt.empty();
        }

        if (!isDesktopSupportedMenu(menu)) {
            ResourceLocation menuKey = BuiltInRegistries.MENU.getKey(menu.getType());
            DesktopDebug.log(
                "server capture skipped player={} title={} menu={} menuType={} source={} reason=unsupported-menu-vanilla-fallback",
                player.getName().getString(),
                provider.getDisplayName().getString(),
                menuKey,
                menu.getType(),
                sourceKey
            );
            menu.removed(player);
            return null;
        }

        Session session = new Session(
            menu.containerId,
            menu,
            provider.getDisplayName(),
            DesktopPackets.SPECIAL_GENERIC,
            -1,
            0,
            DesktopPackets.menuTypeId(menu.getType()),
            sourceKey == null ? "" : sourceKey
        );
        session.ghostPinned = ghostPinned;
        session.visibleToClient = visibleToClient;
        sessions.add(player, session);
        DesktopDebug.log(
            "server capture menu player={} session={} container={} type={} title={} source={} ghostPinned={} visible={}",
            player.getName().getString(),
            session.sessionId,
            menu.containerId,
            session.menuTypeId,
            provider.getDisplayName().getString(),
            session.sourceKey,
            session.ghostPinned,
            session.visibleToClient
        );
        return OptionalInt.of(session.sessionId);
    }

    public static void openHorseSession(ServerPlayer player, AbstractHorse horse, Container container) {
        PlayerSessions sessions = sessions(player);
        String sourceKey = sourceKeyForEntity(player, horse.getId());
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close horse player={} source={}", player.getName().getString(), sourceKey);
            return;
        }

        int columns = horse instanceof AbstractChestedHorse chestedHorse && chestedHorse.hasChest()
            ? chestedHorse.getInventoryColumns()
            : 0;
        int sessionId = nextSessionId(player);
        HorseInventoryMenu menu = new HorseInventoryMenu(sessionId, player.getInventory(), container, horse);
        sessions.add(player, new Session(
            sessionId,
            menu,
            horse.getDisplayName(),
            DesktopPackets.SPECIAL_HORSE,
            horse.getId(),
            columns,
            -1,
            sourceKey
        ));
        DesktopDebug.log("server capture horse player={} session={} entity={} columns={}", player.getName().getString(), sessionId, horse.getId(), columns);
    }

    private static boolean isDesktopSupportedMenu(AbstractContainerMenu menu) {
        MenuType<?> type = menu.getType();
        if (isKnownVanillaDesktopMenu(type)) {
            return true;
        }

        if (DesktopServerApi.hasWindowSupport(type)) {
            return true;
        }

        return false;
    }

    private static boolean isKnownVanillaDesktopMenu(MenuType<?> type) {
        return type == MenuType.GENERIC_9x1
            || type == MenuType.GENERIC_9x2
            || type == MenuType.GENERIC_9x3
            || type == MenuType.GENERIC_9x4
            || type == MenuType.GENERIC_9x5
            || type == MenuType.GENERIC_9x6
            || type == MenuType.GENERIC_3x3
            || type == MenuType.ANVIL
            || type == MenuType.BEACON
            || type == MenuType.BLAST_FURNACE
            || type == MenuType.BREWING_STAND
            || type == MenuType.CRAFTING
            || type == MenuType.ENCHANTMENT
            || type == MenuType.FURNACE
            || type == MenuType.GRINDSTONE
            || type == MenuType.HOPPER
            || type == MenuType.LOOM
            || type == MenuType.MERCHANT
            || type == MenuType.SHULKER_BOX
            || type == MenuType.SMITHING
            || type == MenuType.SMOKER
            || type == MenuType.CARTOGRAPHY_TABLE
            || type == MenuType.STONECUTTER;
    }

    public static boolean sendMerchantOffers(ServerPlayer player, int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !sessions.sessions.containsKey(containerId)) {
            return false;
        }

        if (sessions.sessions.get(containerId).menu instanceof MerchantMenu merchantMenu) {
            merchantMenu.setOffers(offers);
            merchantMenu.setMerchantLevel(villagerLevel);
            merchantMenu.setXp(villagerXp);
            merchantMenu.setShowProgressBar(showProgress);
            merchantMenu.setCanRestock(canRestock);
        }

        send(player, new DesktopMerchantOffersPayload(containerId, offers, villagerLevel, villagerXp, showProgress, canRestock));
        DesktopDebug.trace("server merchant offers player={} session={}", player.getName().getString(), containerId);
        return true;
    }

    private static void setReady(ServerPlayer player, boolean ready) {
        PlayerSessions sessions = sessions(player);
        sessions.ready = ready && SaltsInventoryRuntime.isEnabled();
        DesktopDebug.log("server ready player={} ready={} sessions={}", player.getName().getString(), ready, sessions.sessions.size());
        if (!ready) {
            sessions.closeAll(player, false);
        } else {
            InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
            InventoryExpansion.syncToClient(player);
        }
    }

    private static void disconnect(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.remove(player.getUUID());
        if (sessions != null) {
            DesktopDebug.log("server disconnect close player={} sessions={}", player.getName().getString(), sessions.sessions.size());
            sessions.closeAll(player, false);
        }
    }

    private static void tick(MinecraftServer server) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerSessions sessions = PLAYERS.get(player.getUUID());
                if (sessions != null && sessions.ready) {
                    sessions.ready = false;
                    sessions.closeAll(player, true);
                }
            }
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSessions sessions = PLAYERS.get(player.getUUID());
            if (sessions != null && sessions.ready) {
                sessions.tick(player);
            }
        }
    }

    private static void click(ServerPlayer player, DesktopClickPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server click dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        ClickType input;
        try {
            input = ClickType.valueOf(payload.inputName());
        } catch (IllegalArgumentException exception) {
            DesktopDebug.warn("server click dropped player={} session={} reason=bad-input input={}", player.getName().getString(), payload.sessionId(), payload.inputName());
            return;
        }

        if (payload.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
            DesktopDebug.trace("server click player-menu id={} player={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
            clickMenu(payload.debugId(), player, sessions, player.inventoryMenu, payload.slotIndex(), payload.button(), input, payload.clientCarried());
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server click dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server click dropped player={} session={} reason=hidden", player.getName().getString(), payload.sessionId());
            return;
        }

        DesktopDebug.trace("server click session id={} player={} session={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.sessionId(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
        clickMenu(payload.debugId(), player, sessions, session.menu, payload.slotIndex(), payload.button(), input, payload.clientCarried());
        session.menu.broadcastChanges();
        syncCraftingResultSlot(player, session);
        syncMerchantOffers(player, session);
        syncCarried(player, sessions);
    }

    private static void carried(ServerPlayer player, DesktopCarriedPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server carried dropped player={} reason=not-ready stack={}", player.getName().getString(), payload.carried());
            return;
        }

        if (!player.getAbilities().instabuild) {
            DesktopDebug.trace("server carried dropped player={} reason=not-creative stack={} serverCarried={}", player.getName().getString(), payload.carried(), sessions.carried);
            syncCarried(player, sessions);
            return;
        }

        ItemStack carried = payload.carried().copy();
        sessions.carried = carried;
        player.inventoryMenu.setCarried(carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(carried.copy());
        }
        DesktopDebug.trace("server carried sync player={} stack={}", player.getName().getString(), sessions.carried);
        syncCarried(player, sessions);
    }

    private static void quickMove(ServerPlayer player, DesktopQuickMovePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=not-ready", player.getName().getString(), payload.sourceSessionId());
            return;
        }

        SlotSource source = resolveSlot(player, sessions, payload.sourceSessionId(), payload.sourceSlotIndex());
        if (source == null) {
            return;
        }

        ItemStack carriedBeforeQuickMove = sessions.carried.copy();
        if (!carriedBeforeQuickMove.isEmpty()) {
            DesktopDebug.trace(
                "server quick move treating carried as empty player={} sourceSession={} sourceSlot={} carried={}",
                player.getName().getString(),
                payload.sourceSessionId(),
                payload.sourceSlotIndex(),
                carriedBeforeQuickMove
            );
        }

        if (isCraftingResultSource(source)) {
            DesktopDebug.trace(
                "server quick move vanilla crafting result player={} sourceSession={} sourceSlot={}",
                player.getName().getString(),
                payload.sourceSessionId(),
                payload.sourceSlotIndex()
            );
            if (!carriedBeforeQuickMove.isEmpty()) {
                setSharedCarried(player, sessions, ItemStack.EMPTY);
            }
            clickMenu(0, player, sessions, source.menu, payload.sourceSlotIndex(), 0, ClickType.QUICK_MOVE, ItemStack.EMPTY);
            if (!carriedBeforeQuickMove.isEmpty()) {
                setSharedCarried(player, sessions, carriedBeforeQuickMove);
            }
            source.menu.broadcastChanges();
            if (source.session != null) {
                syncCraftingResultSlot(player, source.session);
                syncMerchantOffers(player, source.session);
            }
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            syncCarried(player, sessions);
            return;
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION && payload.targetSessionId() != source.sessionId) {
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (quickMoveIntoTomStorageTerminal(player, sessions, source, targetSession)) {
                return;
            }
        }

        List<net.minecraft.world.inventory.Slot> targets = quickMoveTargets(player, sessions, source, payload);
        if (targets.isEmpty()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} sourceSlot={} reason=no-targets", player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex());
            return;
        }

        boolean moved = moveSlotStack(player, source.slot, targets);
        DesktopDebug.trace(
            "server quick move player={} sourceSession={} sourceSlot={} targetKind={} targetSession={} moved={}",
            player.getName().getString(),
            payload.sourceSessionId(),
            payload.sourceSlotIndex(),
            payload.targetKind(),
            payload.targetSessionId(),
            moved
        );
        if (!moved) {
            return;
        }

        source.menu.broadcastChanges();
        if (source.session != null) {
            syncCraftingResultSlot(player, source.session);
            syncMerchantOffers(player, source.session);
        }
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
        if (!carriedBeforeQuickMove.isEmpty()) {
            syncCarried(player, sessions);
        }
    }

    private static boolean quickMoveIntoTomStorageTerminal(ServerPlayer player, PlayerSessions sessions, SlotSource source, @Nullable Session targetSession) {
        if (targetSession == null
            || !targetSession.visibleToClient
            || !targetSession.menu.stillValid(player)
            || !TomsStorageCompat.isTerminal(targetSession.menu.getType())
            || !isPlayerInventorySlot(player, source.slot)
            || !source.slot.hasItem()) {
            return false;
        }

        int sourceContainerSlot = source.slot.getContainerSlot();
        int targetSlotIndex = -1;
        for (int i = 0; i < targetSession.menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot slot = targetSession.menu.slots.get(i);
            if (isPlayerInventorySlot(player, slot) && slot.getContainerSlot() == sourceContainerSlot) {
                targetSlotIndex = i;
                break;
            }
        }
        if (targetSlotIndex < 0) {
            DesktopDebug.trace(
                "server quick move tom terminal dropped player={} sourceSlot={} targetSession={} reason=no-matching-player-slot",
                player.getName().getString(),
                sourceContainerSlot,
                targetSession.sessionId
            );
            return false;
        }

        ItemStack carriedBeforeQuickMove = sessions.carried.copy();
        if (!carriedBeforeQuickMove.isEmpty()) {
            setSharedCarried(player, sessions, ItemStack.EMPTY);
        }
        ItemStack before = targetSession.menu.slots.get(targetSlotIndex).getItem().copy();
        targetSession.menu.quickMoveStack(player, targetSlotIndex);
        if (!carriedBeforeQuickMove.isEmpty()) {
            setSharedCarried(player, sessions, carriedBeforeQuickMove);
        }
        ItemStack after = targetSession.menu.slots.get(targetSlotIndex).getItem();
        boolean moved = !ItemStack.matches(before, after);
        DesktopDebug.trace(
            "server quick move tom terminal player={} sourceSession={} sourceSlot={} targetSession={} targetSlot={} moved={} before={} after={}",
            player.getName().getString(),
            source.sessionId,
            source.slot.getContainerSlot(),
            targetSession.sessionId,
            targetSlotIndex,
            moved,
            before,
            after
        );
        if (!moved) {
            return false;
        }

        targetSession.menu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
        return true;
    }

    private static boolean isCraftingResultSource(SlotSource source) {
        return source.menu instanceof RecipeBookMenu<?> recipeMenu && source.menu.slots.indexOf(source.slot) == recipeMenu.getResultSlotIndex();
    }

    private static void button(ServerPlayer player, DesktopButtonPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server button invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        session.menu.setCarried(sessions.carried.copy());
        boolean clicked; if (session.menu instanceof BeaconMenu beaconMenu) {
            clicked = applyBeaconButton(beaconMenu, payload.buttonId());
        } else if (session.menu instanceof MerchantMenu merchantMenu) {
            clicked = payload.buttonId() >= 0 && payload.buttonId() < merchantMenu.getOffers().size();
            if (clicked) {
                merchantMenu.setSelectionHint(payload.buttonId());
                merchantMenu.tryMoveItems(payload.buttonId());
            }
        } else {
            clicked = session.menu.clickMenuButton(player, payload.buttonId());
        }
        sessions.carried = session.menu.getCarried().copy();
        player.inventoryMenu.setCarried(sessions.carried.copy());
        for (Session openSession : sessions.sessions.values()) {
            openSession.menu.setCarried(sessions.carried.copy());
        }

        DesktopDebug.trace(
            "server button player={} session={} button={} clicked={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.buttonId(),
            clicked
        );
        if (clicked) {
            session.menu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
        } else {
            syncCarried(player, sessions);
        }
    }

    private static void placeRecipe(ServerPlayer player, DesktopPlaceRecipePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (player.isSpectator()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=spectator", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server recipe place invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }
        if (!(session.menu instanceof RecipeBookMenu recipeBookMenu)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} menu={} reason=not-recipe-menu", player.getName().getString(), payload.sessionId(), payload.recipeId(), session.menu.getType());
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=no-server", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        Recipe<?> recipe = server.getRecipeManager().byKey(payload.recipeId()).orElse(null);
        if (recipe == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-recipe", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        if (!player.getRecipeBook().contains(recipe)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=not-unlocked", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.getId());
            return;
        }
        if (recipe.isIncomplete()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=incomplete", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.getId());
            return;
        }

        boolean canCraft = player.isCreative() || canCraftRecipe(recipeBookMenu, player, recipe);
        session.menu.setCarried(sessions.carried.copy());
        recipeBookMenu.handlePlacement(
            payload.useMaxItems(),
            recipe,
            player
        );
        sessions.carried = session.menu.getCarried().copy();
        player.inventoryMenu.setCarried(sessions.carried.copy());
        for (Session openSession : sessions.sessions.values()) {
            openSession.menu.setCarried(sessions.carried.copy());
        }

        DesktopDebug.trace(
            "server recipe place player={} session={} recipe={} recipeKey={} useMax={} canCraft={} carried={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.recipeId(),
            recipe.getId(),
            payload.useMaxItems(),
            canCraft,
            sessions.carried
        );

        if (!canCraft) {
            send(player, new DesktopGhostRecipePayload(session.sessionId, recipe.getId()));
        }
        session.menu.broadcastChanges();
        syncCraftingResultSlot(player, session);
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
    }

    private static boolean canCraftRecipe(RecipeBookMenu recipeBookMenu, ServerPlayer player, Recipe<?> recipe) {
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        recipeBookMenu.fillCraftSlotsStackedContents(contents);
        return contents.canCraft(recipe, null);
    }

    private static void rename(ServerPlayer player, DesktopRenamePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=hidden", player.getName().getString(), payload.sessionId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server rename invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        if (!(session.menu instanceof AnvilMenu anvilMenu)) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-anvil", player.getName().getString(), payload.sessionId());
            return;
        }

        boolean changed = anvilMenu.setItemName(payload.name());
        DesktopDebug.trace("server rename player={} session={} changed={} name={}", player.getName().getString(), payload.sessionId(), changed, payload.name());
        if (changed) {
            anvilMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
        }
    }

    private static void customPayload(ServerPlayer player, DesktopCustomPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server custom invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        DesktopServerPayloadHandler<AbstractContainerMenu> handler = DesktopServerApi.findPayloadHandler(session.menu.getType(), payload.channel());
        if (handler == null) {
            DesktopDebug.warn(
                "server custom dropped player={} session={} channel={} menu={} reason=no-handler",
                player.getName().getString(),
                payload.sessionId(),
                payload.channel(),
                session.menu.getType()
            );
            return;
        }

        DesktopDebug.trace(
            "server custom player={} session={} channel={} bytes={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.channel(),
            payload.data().length
        );
        try {
            handler.handle(new ServerPayloadContext(player, sessions, session, payload));
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server custom handler failed player={} session={} channel={} reason={}",
                player.getName().getString(),
                payload.sessionId(),
                payload.channel(),
                exception.toString()
            );
        }
    }

    private static boolean applyBeaconButton(BeaconMenu menu, int buttonId) {
        int primaryId = buttonId & BEACON_EFFECT_ID_MASK;
        int secondaryId = buttonId >>> BEACON_SECONDARY_EFFECT_SHIFT & BEACON_EFFECT_ID_MASK;
        MobEffect primary = MobEffect.byId(primaryId);
        MobEffect secondary = MobEffect.byId(secondaryId);
        if (!menu.hasPayment() || !canSelectBeaconPrimary(menu, primary) || !canSelectBeaconSecondary(menu, primary, secondary)) {
            return false;
        }

        menu.updateEffects(Optional.of(primary), Optional.ofNullable(secondary));
        return true;
    }

    private static boolean canSelectBeaconPrimary(BeaconMenu menu, @Nullable MobEffect effect) {
        if (effect == null) {
            return false;
        }

        int unlockedTiers = Math.min(menu.getLevels(), Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.length));
        for (int tier = 0; tier < unlockedTiers; tier++) {
            if (beaconTierContains(tier, effect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSelectBeaconSecondary(BeaconMenu menu, MobEffect primary, @Nullable MobEffect secondary) {
        if (secondary == null) {
            return true;
        }
        if (menu.getLevels() < 4) {
            return false;
        }
        if (primary.equals(secondary)) {
            return true;
        }
        return BeaconBlockEntity.BEACON_EFFECTS.length > 3 && beaconTierContains(3, secondary);
    }

    private static boolean beaconTierContains(int tier, MobEffect effect) {
        if (tier < 0 || tier >= BeaconBlockEntity.BEACON_EFFECTS.length) {
            return false;
        }
        for (MobEffect candidate : BeaconBlockEntity.BEACON_EFFECTS[tier]) {
            if (candidate == effect) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable SlotSource resolveSlot(ServerPlayer player, PlayerSessions sessions, int sessionId, int slotIndex) {
        AbstractContainerMenu menu;
        Session session = null;
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            menu = player.inventoryMenu;
        } else {
            session = sessions.sessions.get(sessionId);
            if (session == null) {
                DesktopDebug.trace("server quick move dropped player={} session={} reason=missing-session", player.getName().getString(), sessionId);
                return null;
            }
            if (!session.visibleToClient) {
                DesktopDebug.trace("server quick move dropped player={} session={} reason=hidden", player.getName().getString(), sessionId);
                return null;
            }
            if (!session.menu.stillValid(player)) {
                DesktopDebug.log("server quick move invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                sessions.close(player, session.sessionId, true);
                return null;
            }
            menu = session.menu;
        }

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server quick move dropped player={} session={} slot={} reason=out-of-range", player.getName().getString(), sessionId, slotIndex);
            return null;
        }

        return new SlotSource(sessionId, menu, menu.slots.get(slotIndex), session);
    }

    private static List<net.minecraft.world.inventory.Slot> quickMoveTargets(ServerPlayer player, PlayerSessions sessions, SlotSource source, DesktopQuickMovePayload payload) {
        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION && payload.targetSessionId() != source.sessionId) {
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (targetSession != null && targetSession.visibleToClient && targetSession.menu.stillValid(player)) {
                List<net.minecraft.world.inventory.Slot> targetSlots = containerSlots(targetSession.menu, player);
                if (!targetSlots.isEmpty()) {
                    return targetSlots;
                }
            }
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_HOTBAR && !isPlayerInventorySlot(player, source.slot)) {
            return hotbarSlots(player);
        }

        return defaultPlayerTargets(player, source.slot);
    }

    private static List<net.minecraft.world.inventory.Slot> defaultPlayerTargets(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot) {
        boolean sourceIsPlayerInventory = isPlayerInventorySlot(player, sourceSlot);
        boolean sourceIsMainInventory = InventoryExpansion.isMainInventorySlot(player, sourceSlot);
        int sourceContainerSlot = sourceSlot.getContainerSlot();
        if (sourceIsPlayerInventory && sourceContainerSlot >= 0 && sourceContainerSlot < 9) {
            return mainInventorySlots(player);
        }
        if (sourceIsMainInventory) {
            return hotbarSlots(player);
        }

        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        slots.addAll(mainInventorySlots(player));
        slots.addAll(hotbarSlots(player));
        return slots;
    }

    private static boolean moveSlotStack(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets) {
        if (!sourceSlot.isActive() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return false;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        ItemStack moving = sourceStack.copy();
        int originalCount = moving.getCount();
        insertIntoMatchingSlots(sourceSlot, targets, moving);
        insertIntoEmptySlots(sourceSlot, targets, moving);

        int moved = originalCount - moving.getCount();
        if (moved <= 0) {
            return false;
        }

        ItemStack taken = original.copyWithCount(moved);
        sourceStack.shrink(moved);
        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, taken);
        return true;
    }

    private static void insertIntoMatchingSlots(net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets, ItemStack moving) {
        if (!moving.isStackable()) {
            return;
        }

        for (net.minecraft.world.inventory.Slot target : targets) {
            if (moving.isEmpty()) {
                return;
            }
            if (target == sourceSlot || !target.isActive() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameTags(moving, target.getItem())) {
                continue;
            }
            target.safeInsert(moving);
        }
    }

    private static void insertIntoEmptySlots(net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets, ItemStack moving) {
        for (net.minecraft.world.inventory.Slot target : targets) {
            if (moving.isEmpty()) {
                return;
            }
            if (target == sourceSlot || !target.isActive() || target.hasItem()) {
                continue;
            }
            target.safeInsert(moving);
        }
    }

    private static List<net.minecraft.world.inventory.Slot> containerSlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!isPlayerInventorySlot(player, slot) && !InventoryExpansion.isExtraSlot(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static List<net.minecraft.world.inventory.Slot> mainInventorySlots(ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (InventoryExpansion.isMainInventorySlot(player, slot)) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.comparingInt(InventoryExpansion::storageOrder));
        return slots;
    }

    private static List<net.minecraft.world.inventory.Slot> hotbarSlots(ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (isPlayerInventorySlot(player, slot) && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static boolean isPlayerInventorySlot(ServerPlayer player, net.minecraft.world.inventory.Slot slot) {
        return slot.container == player.getInventory();
    }

    private static @Nullable Container containerForMenu(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu chestMenu) {
            return chestMenu.getContainer();
        }

        return null;
    }

    private static boolean containsContainer(Container owner, Container target) {
        return owner == target || owner instanceof CompoundContainer compoundContainer && compoundContainer.contains(target);
    }

    private static void clickMenu(int debugId, ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ClickType input, ItemStack clientCarried) {
        if (slotIndex < AbstractContainerMenu.SLOT_CLICKED_OUTSIDE || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} slot={} reason=out-of-range", debugId, player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        ItemStack slotBefore = serverSlotStack(menu, slotIndex);
        ItemStack carriedBefore = sessions.carried.copy();
        ItemStack menuCarriedBefore = menu.getCarried().copy();
        ItemStack effectiveCarried = player.getAbilities().instabuild ? clientCarried.copy() : sessions.carried.copy();
        DesktopDebug.trace(
            "server click before id={} player={} menu={} slot={} button={} input={} slotBefore={} sessionsCarried={} menuCarried={} clientCarried={} effectiveCarried={} creative={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            button,
            input,
            slotBefore,
            carriedBefore,
            menuCarriedBefore,
            clientCarried,
            effectiveCarried,
            player.getAbilities().instabuild
        );
        DesktopDebug.probe(
            "server click before id={} player={} menu={} slot={} button={} input={} slotBefore={} sessionsCarried={} menuCarried={} clientCarried={} effectiveCarried={} creative={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            button,
            input,
            slotBefore,
            carriedBefore,
            menuCarriedBefore,
            clientCarried,
            effectiveCarried,
            player.getAbilities().instabuild
        );
        menu.setCarried(effectiveCarried);
        menu.clicked(slotIndex, button, input, player);
        sessions.carried = menu.getCarried().copy();
        player.inventoryMenu.setCarried(sessions.carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(sessions.carried.copy());
        }
        DesktopDebug.trace(
            "server click after id={} player={} menu={} slot={} slotAfter={} sessionsCarried={} playerMenuCarried={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            serverSlotStack(menu, slotIndex),
            sessions.carried,
            player.inventoryMenu.getCarried()
        );
        DesktopDebug.probe(
            "server click after id={} player={} menu={} slot={} input={} slotAfter={} sessionsCarried={} playerMenuCarried={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            input,
            serverSlotStack(menu, slotIndex),
            sessions.carried,
            player.inventoryMenu.getCarried()
        );
    }

    private static ItemStack serverSlotStack(AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }

        return menu.slots.get(slotIndex).getItem().copy();
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        DesktopDebug.trace("server sync carried player={} stack={}", player.getName().getString(), sessions.carried);
        send(player, new DesktopCarriedPayload(sessions.carried.copy()));
    }

    private static void setSharedCarried(ServerPlayer player, PlayerSessions sessions, ItemStack stack) {
        ItemStack carried = stack.copy();
        sessions.carried = carried.copy();
        player.inventoryMenu.setCarried(carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(carried.copy());
        }
    }

    private static void syncCraftingResultSlot(ServerPlayer player, Session session) {
        if (!(session.menu instanceof RecipeBookMenu<?> recipeMenu)) {
            return;
        }

        int slotIndex = recipeMenu.getResultSlotIndex();
        net.minecraft.world.inventory.Slot resultSlot = session.menu.getSlot(slotIndex);
        if (slotIndex < 0) {
            return;
        }

        send(player, new DesktopSlotPayload(session.sessionId, slotIndex, session.menu.getStateId(), resultSlot.getItem().copy()));
    }

    private static void syncMerchantOffers(ServerPlayer player, Session session) {
        if (!(session.menu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        send(player, new DesktopMerchantOffersPayload(
            session.sessionId,
            merchantMenu.getOffers(),
            merchantMenu.getTraderLevel(),
            merchantMenu.getTraderXp(),
            merchantMenu.showProgressBar(),
            merchantMenu.canRestock()
        ));
    }

    private static void closeSession(ServerPlayer player, int sessionId, boolean notifyClient) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            DesktopDebug.log("server close request player={} session={} notify={}", player.getName().getString(), sessionId, notifyClient);
            sessions.close(player, sessionId, notifyClient);
        }
    }

    private static void setSessionPin(ServerPlayer player, DesktopSessionPinPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server pin dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server pin dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }

        session.ghostPinned = payload.pinMode() == DesktopPackets.PIN_MODE_GHOST_PINNED;
        DesktopDebug.trace("server pin player={} session={} ghostPinned={}", player.getName().getString(), payload.sessionId(), session.ghostPinned);
        session.dispatchPinChanged(player);
    }

    private static void setSessionVisibility(ServerPlayer player, DesktopSessionVisibilityPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server visibility invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.rememberDormantGhost(session, "visibility-invalid");
            sessions.close(player, session.sessionId, true);
            return;
        }

        sessions.setVisible(player, session, payload.visible(), true);
    }

    private static int nextSessionId(ServerPlayer player) {
        PlayerSessions sessions = sessions(player);
        int next = sessions.nextSessionId++;
        if (sessions.nextSessionId == Integer.MAX_VALUE) {
            sessions.nextSessionId = 1;
        }
        return next;
    }

    private static PlayerSessions sessions(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), uuid -> new PlayerSessions());
    }

    private static void send(ServerPlayer player, DesktopPacket payload) {
        if (ServerPlayNetworking.canSend(player, payload.id())) {
            ServerPlayNetworking.send(player, payload.id(), DesktopPackets.toBuffer(payload));
        }
    }

    private static final class PlayerSessions {
        private final LinkedHashMap<Integer, Session> sessions = new LinkedHashMap<>();
        private final LinkedHashMap<String, DormantGhostSource> dormantGhostSources = new LinkedHashMap<>();
        private boolean ready;
        private int nextSessionId = 1;
        private int dormantGhostProbeTicks;
        private ItemStack carried = ItemStack.EMPTY;

        private void add(ServerPlayer player, Session session) {
            while (this.sessions.size() >= MAX_SESSIONS) {
                Iterator<Integer> iterator = this.sessions.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                Integer sessionId = iterator.next();
                DesktopDebug.log("server session cap close player={} session={}", player.getName().getString(), sessionId);
                this.close(player, sessionId, true);
            }

            this.sessions.put(session.sessionId, session);
            if (this.carried.isEmpty()) {
                this.carried = player.inventoryMenu.getCarried().copy();
            }
            session.initializeServerHandler(player, this);
            session.menu.setCarried(this.carried.copy());
            session.menu.setSynchronizer(new SessionSynchronizer(player, session));
            session.menu.sendAllDataToRemote();
            DesktopDebug.log("server session initial sync requested player={} session={} state={} slots={}", player.getName().getString(), session.sessionId, session.menu.getStateId(), session.menu.slots.size());
            DesktopDebug.log("server session add player={} session={} title={} count={}", player.getName().getString(), session.sessionId, session.title.getString(), this.sessions.size());
            session.dispatchOpened(player, this);
        }

        private boolean closeBySourceKey(ServerPlayer player, @Nullable String sourceKey, boolean notifyClient) {
            if (sourceKey == null || sourceKey.isEmpty()) {
                return false;
            }

            boolean handled = false;
            for (Session session : List.copyOf(this.sessions.values())) {
                if (sourceKey.equals(session.sourceKey)) {
                    if (session.ghostPinned) {
                        this.setVisible(player, session, !session.visibleToClient, notifyClient);
                    } else {
                        this.close(player, session.sessionId, notifyClient);
                    }
                    handled = true;
                }
            }

            return handled;
        }

        private void setVisible(ServerPlayer player, Session session, boolean visible, boolean notifyClient) {
            if (session.visibleToClient == visible) {
                return;
            }

            session.visibleToClient = visible;
            DesktopDebug.log("server session visibility player={} session={} title={} visible={} notify={}", player.getName().getString(), session.sessionId, session.title.getString(), visible, notifyClient);
            if (notifyClient) {
                send(player, new DesktopSessionVisibilityPayload(session.sessionId, visible));
            }
            if (visible) {
                session.menu.sendAllDataToRemote();
                syncCraftingResultSlot(player, session);
                syncMerchantOffers(player, session);
                syncCarried(player, this);
            }
            session.dispatchVisibilityChanged(player, this);
        }

        private void close(ServerPlayer player, int sessionId, boolean notifyClient) {
            Session session = this.sessions.remove(sessionId);
            if (session == null) {
                DesktopDebug.trace("server close ignored player={} session={} reason=missing", player.getName().getString(), sessionId);
                return;
            }

            DesktopDebug.log("server session close player={} session={} title={} notify={}", player.getName().getString(), sessionId, session.title.getString(), notifyClient);
            session.dispatchClosed(player, this);
            session.menu.removed(player);
            if (notifyClient) {
                send(player, new DesktopSessionClosedPayload(sessionId));
            }
        }

        private void closeAll(ServerPlayer player, boolean notifyClient) {
            for (Integer sessionId : List.copyOf(this.sessions.keySet())) {
                this.close(player, sessionId, notifyClient);
            }
            this.dormantGhostSources.clear();
            this.carried = ItemStack.EMPTY;
        }

        private void tick(ServerPlayer player) {
            for (Session session : List.copyOf(this.sessions.values())) {
                if (!session.menu.stillValid(player)) {
                    DesktopDebug.log("server session invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                    this.rememberDormantGhost(session, "invalid");
                    this.close(player, session.sessionId, true);
                } else {
                    session.menu.broadcastChanges();
                    syncCraftingResultSlot(player, session);
                    syncMerchantOffers(player, session);
                    session.dispatchTick(player, this);
                }
            }
            this.reopenDormantGhosts(player);
        }

        private void broadcastAll(ServerPlayer player) {
            for (Session session : this.sessions.values()) {
                session.menu.broadcastChanges();
                syncMerchantOffers(player, session);
                session.dispatchTick(player, this);
            }
            syncCarried(player, this);
        }

        private void rememberDormantGhost(Session session, String reason) {
            if (!session.ghostPinned || !isBlockBackedSourceKey(session.sourceKey)) {
                return;
            }

            this.dormantGhostSources.put(session.sourceKey, new DormantGhostSource(session.sourceKey));
            DesktopDebug.log("server dormant ghost remember source={} session={} title={} reason={}", session.sourceKey, session.sessionId, session.title.getString(), reason);
        }

        private void reopenDormantGhosts(ServerPlayer player) {
            if (this.dormantGhostSources.isEmpty()) {
                return;
            }

            this.dormantGhostProbeTicks++;
            if (this.dormantGhostProbeTicks % DORMANT_GHOST_REOPEN_INTERVAL_TICKS != 0) {
                return;
            }

            for (DormantGhostSource dormant : List.copyOf(this.dormantGhostSources.values())) {
                if (this.hasSessionForSourceKey(dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }

                MenuProvider provider = providerForDormantGhost(player, dormant.sourceKey());
                if (provider == null) {
                    continue;
                }

                this.dormantGhostSources.remove(dormant.sourceKey());
                DesktopDebug.log("server dormant ghost reopen player={} source={} title={}", player.getName().getString(), dormant.sourceKey(), provider.getDisplayName().getString());
                openMenuSession(player, provider, dormant.sourceKey(), false, true, false);
            }
        }

        private boolean hasSessionForSourceKey(String sourceKey) {
            for (Session session : this.sessions.values()) {
                if (sourceKey.equals(session.sourceKey)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Session {
        private final int sessionId;
        private final AbstractContainerMenu menu;
        private final Component title;
        private final int specialKind;
        private final int entityId;
        private final int columns;
        private final int menuTypeId;
        private final String sourceKey;
        private boolean ghostPinned;
        private boolean visibleToClient = true;
        private @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> serverHandler;
        private @Nullable Object serverState;

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
            this.sessionId = sessionId;
            this.menu = menu;
            this.title = title;
            this.specialKind = specialKind;
            this.entityId = entityId;
            this.columns = columns;
            this.menuTypeId = menuTypeId;
            this.sourceKey = sourceKey;
        }

        private void initializeServerHandler(ServerPlayer player, PlayerSessions sessions) {
            this.serverHandler = DesktopServerApi.findWindowHandler(this.menu.getType());
            if (this.serverHandler == null) {
                return;
            }

            try {
                this.serverState = this.serverHandler.createState(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler createState failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
                this.serverState = null;
            }
        }

        private void dispatchOpened(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            try {
                this.serverHandler.opened(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler opened failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
            }
        }

        private void dispatchTick(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            try {
                this.serverHandler.tick(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler tick failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
            }
        }

        private void dispatchClosed(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            try {
                this.serverHandler.closed(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler closed failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
            }
        }

        private void dispatchVisibilityChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            try {
                this.serverHandler.visibilityChanged(new ServerSessionContext(player, sessions, this), this.visibleToClient);
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler visibility failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
            }
        }

        private void dispatchPinChanged(ServerPlayer player) {
            if (this.serverHandler == null) {
                return;
            }
            PlayerSessions sessions = sessions(player);
            try {
                this.serverHandler.pinChanged(new ServerSessionContext(player, sessions, this), this.ghostPinned);
            } catch (RuntimeException exception) {
                DesktopDebug.warn("server window handler pin failed player={} session={} title={} reason={}", player.getName().getString(), this.sessionId, this.title.getString(), exception.toString());
            }
        }
    }

    private record SlotSource(int sessionId, AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, @Nullable Session session) {
    }

    private record DormantGhostSource(String sourceKey) {
    }

    private record ServerSessionContext(
        ServerPlayer player,
        PlayerSessions sessions,
        Session session
    ) implements DesktopServerSessionContext<AbstractContainerMenu, Object> {
        @Override
        public AbstractContainerMenu menu() {
            return this.session.menu;
        }

        @Override
        public int sessionId() {
            return this.session.sessionId;
        }

        @Override
        public String sourceKey() {
            return this.session.sourceKey;
        }

        @Override
        public boolean visible() {
            return this.session.visibleToClient;
        }

        @Override
        public boolean ghostPinned() {
            return this.session.ghostPinned;
        }

        @Override
        public Object state() {
            return this.session.serverState;
        }

        @Override
        public void sendToClient(ResourceLocation channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.session.menu.broadcastChanges();
            this.sessions.carried = this.session.menu.getCarried().copy();
            this.player.inventoryMenu.setCarried(this.sessions.carried.copy());
            for (Session openSession : this.sessions.sessions.values()) {
                openSession.menu.setCarried(this.sessions.carried.copy());
            }
            this.player.inventoryMenu.broadcastChanges();
            this.sessions.broadcastAll(this.player);
            syncCarried(this.player, this.sessions);
        }
    }

    private record ServerPayloadContext(
        ServerPlayer player,
        PlayerSessions sessions,
        Session session,
        DesktopCustomPayload payload
    ) implements DesktopServerPayloadContext<AbstractContainerMenu> {
        @Override
        public AbstractContainerMenu menu() {
            return this.session.menu;
        }

        @Override
        public int sessionId() {
            return this.session.sessionId;
        }

        @Override
        public String sourceKey() {
            return this.session.sourceKey;
        }

        @Override
        public boolean visible() {
            return this.session.visibleToClient;
        }

        @Override
        public boolean ghostPinned() {
            return this.session.ghostPinned;
        }

        @Override
        public Object state() {
            return this.session.serverState;
        }

        @Override
        public ResourceLocation channel() {
            return this.payload.channel();
        }

        @Override
        public byte[] data() {
            return this.payload.data();
        }

        @Override
        public void sendToClient(ResourceLocation channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.session.menu.broadcastChanges();
            this.sessions.carried = this.session.menu.getCarried().copy();
            this.player.inventoryMenu.setCarried(this.sessions.carried.copy());
            for (Session openSession : this.sessions.sessions.values()) {
                openSession.menu.setCarried(this.sessions.carried.copy());
            }
            this.player.inventoryMenu.broadcastChanges();
            this.sessions.broadcastAll(this.player);
            syncCarried(this.player, this.sessions);
        }
    }

    private static final class SessionSynchronizer implements ContainerSynchronizer {
        private final ServerPlayer player;
        private final Session session;

        private SessionSynchronizer(ServerPlayer player, Session session) {
            this.player = player;
            this.session = session;
        }

        @Override
        public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> stacks, ItemStack carried, int[] dataSlots) {
            DesktopDebug.log("server send initial player={} session={} title={} slots={} data={}", this.player.getName().getString(), this.session.sessionId, this.session.title.getString(), stacks.size(), dataSlots.length);
            send(this.player, new DesktopOpenSessionPayload(
                this.session.sessionId,
                this.session.menuTypeId,
                this.session.specialKind,
                this.session.entityId,
                this.session.columns,
                menu.getStateId(),
                this.session.visibleToClient,
                this.session.sourceKey,
                this.session.title,
                stacks,
                carried,
                dataSlots
            ));
            send(this.player, new DesktopCarriedPayload(carried.copy()));
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            DesktopDebug.trace("server send slot player={} session={} slot={} stack={}", this.player.getName().getString(), this.session.sessionId, slot, stack);
            send(this.player, new DesktopSlotPayload(this.session.sessionId, slot, menu.getStateId(), stack.copy()));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
            PlayerSessions sessions = PLAYERS.get(this.player.getUUID());
            if (sessions != null) {
                sessions.carried = stack.copy();
            }
            DesktopDebug.trace("server send carried player={} session={} stack={}", this.player.getName().getString(), this.session.sessionId, stack);
            send(this.player, new DesktopCarriedPayload(stack.copy()));
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int dataSlotIndex, int value) {
            DesktopDebug.trace("server send data player={} session={} data={} value={}", this.player.getName().getString(), this.session.sessionId, dataSlotIndex, value);
            send(this.player, new DesktopDataPayload(this.session.sessionId, dataSlotIndex, value));
        }
    }

    private static @Nullable String sourceKeyForProvider(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof BlockEntity blockEntity) {
            return sourceKeyForBlock(player, blockEntity.getBlockPos());
        }

        return null;
    }

    private static boolean isBlockBackedSourceKey(String sourceKey) {
        return sourceKey.startsWith("block:") || sourceKey.startsWith("chest:");
    }

    private static @Nullable MenuProvider providerForDormantGhost(ServerPlayer player, String sourceKey) {
        SourceKey source = SourceKey.parse(sourceKey);
        if (source == null || !source.dimension().equals(player.level().dimension().location().toString())) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        for (BlockPos pos : source.positions()) {
            if (!canReachDormantSource(player, level, pos)) {
                continue;
            }

            if (!sourceKey.equals(sourceKeyForBlock(player, pos))) {
                continue;
            }

            MenuProvider provider = level.getBlockState(pos).getMenuProvider(level, pos);
            if (provider != null) {
                return provider;
            }
        }

        return null;
    }

    private static boolean canReachDormantSource(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        double range = Math.max(4.5D, 8.0D);
        if (player.position().distanceToSqr(target) > range * range) {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(
            eye,
            target,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private record SourceKey(String kind, String dimension, List<BlockPos> positions) {
        private static @Nullable SourceKey parse(String sourceKey) {
            int firstColon = sourceKey.indexOf(':');
            int lastColon = sourceKey.lastIndexOf(':');
            if (firstColon <= 0 || lastColon <= firstColon) {
                return null;
            }

            String kind = sourceKey.substring(0, firstColon);
            if (!kind.equals("block") && !kind.equals("chest")) {
                return null;
            }

            String dimension = sourceKey.substring(firstColon + 1, lastColon);
            String positionsPart = sourceKey.substring(lastColon + 1);
            List<BlockPos> positions = new ArrayList<>();
            for (String positionPart : positionsPart.split("\\|")) {
                BlockPos pos = parseBlockPos(positionPart);
                if (pos == null) {
                    return null;
                }
                positions.add(pos);
            }

            return positions.isEmpty() ? null : new SourceKey(kind, dimension, List.copyOf(positions));
        }
    }

    private static @Nullable BlockPos parseBlockPos(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            return null;
        }

        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String sourceKeyForEntity(ServerPlayer player, int entityId) {
        return "entity:" + player.level().dimension().location() + ":" + entityId;
    }

    private static String sourceKeyForBlock(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().location().toString();
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connectedPos = pos.relative(ChestBlock.getConnectedDirection(state));
                String first = blockPosKey(pos);
                String second = blockPosKey(connectedPos);
                if (first.compareTo(second) > 0) {
                    String swap = first;
                    first = second;
                    second = swap;
                }
                return "chest:" + dimension + ":" + first + "|" + second;
            }
        }

        return "block:" + dimension + ":" + blockPosKey(pos);
    }

    private static String blockPosKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
