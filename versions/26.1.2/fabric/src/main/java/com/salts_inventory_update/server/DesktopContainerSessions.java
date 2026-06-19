package com.salts_inventory_update.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;

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
        ServerPlayNetworking.registerGlobalReceiver(DesktopReadyPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setReady(context.player(), payload.ready()))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopClickPayload.TYPE, (payload, context) ->
            context.server().execute(() -> click(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopQuickMovePayload.TYPE, (payload, context) ->
            context.server().execute(() -> quickMove(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopButtonPayload.TYPE, (payload, context) ->
            context.server().execute(() -> button(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopRenamePayload.TYPE, (payload, context) ->
            context.server().execute(() -> rename(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCloseSessionPayload.TYPE, (payload, context) ->
            context.server().execute(() -> closeSession(context.player(), payload.sessionId(), true))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionPinPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionPin(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionVisibility(context.player(), payload))
        );
        ServerTickEvents.END_SERVER_TICK.register(DesktopContainerSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player));
    }

    public static boolean shouldCapture(ServerPlayer player) {
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

    public static OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        return openMenuSession(player, provider, null, true, false, true);
    }

    private static OptionalInt openMenuSession(
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

        int columns = horse.getInventoryColumns();
        int sessionId = nextSessionId(player);
        HorseInventoryMenu menu = new HorseInventoryMenu(sessionId, player.getInventory(), container, horse, columns);
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

    public static void openNautilusSession(ServerPlayer player, AbstractNautilus nautilus, Container container) {
        PlayerSessions sessions = sessions(player);
        String sourceKey = sourceKeyForEntity(player, nautilus.getId());
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close nautilus player={} source={}", player.getName().getString(), sourceKey);
            return;
        }

        int columns = nautilus.getInventoryColumns();
        int sessionId = nextSessionId(player);
        NautilusInventoryMenu menu = new NautilusInventoryMenu(sessionId, player.getInventory(), container, nautilus, columns);
        sessions.add(player, new Session(
            sessionId,
            menu,
            nautilus.getDisplayName(),
            DesktopPackets.SPECIAL_NAUTILUS,
            nautilus.getId(),
            columns,
            -1,
            sourceKey
        ));
        DesktopDebug.log("server capture nautilus player={} session={} entity={} columns={}", player.getName().getString(), sessionId, nautilus.getId(), columns);
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
        sessions.ready = ready;
        DesktopDebug.log("server ready player={} ready={} sessions={}", player.getName().getString(), ready, sessions.sessions.size());
        if (!ready) {
            sessions.closeAll(player, false);
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

        ContainerInput input;
        try {
            input = ContainerInput.valueOf(payload.inputName());
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

    private static void quickMove(ServerPlayer player, DesktopQuickMovePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=not-ready", player.getName().getString(), payload.sourceSessionId());
            return;
        }

        if (!sessions.carried.isEmpty()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} sourceSlot={} reason=carried-not-empty carried={}", player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex(), sessions.carried);
            syncCarried(player, sessions);
            return;
        }

        SlotSource source = resolveSlot(player, sessions, payload.sourceSessionId(), payload.sourceSlotIndex());
        if (source == null) {
            return;
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
        boolean clicked;
        if (session.menu instanceof CrafterMenu crafterMenu) {
            int slotId = payload.buttonId() & ~CRAFTER_SLOT_STATE_ENABLED_FLAG;
            boolean enabled = (payload.buttonId() & CRAFTER_SLOT_STATE_ENABLED_FLAG) != 0;
            clicked = slotId >= 0 && slotId < CRAFTER_INPUT_SLOT_COUNT;
            if (clicked) {
                Slot slot = crafterMenu.getSlot(slotId);
                clicked = enabled || (!slot.hasItem() && crafterMenu.getCarried().isEmpty());
                if (clicked) {
                    crafterMenu.setSlotState(slotId, enabled);
                }
            }
        } else if (session.menu instanceof BeaconMenu beaconMenu) {
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

    private static boolean applyBeaconButton(BeaconMenu menu, int buttonId) {
        int primaryId = buttonId & BEACON_EFFECT_ID_MASK;
        int secondaryId = buttonId >>> BEACON_SECONDARY_EFFECT_SHIFT & BEACON_EFFECT_ID_MASK;
        Holder<MobEffect> primary = BeaconMenu.decodeEffect(primaryId);
        Holder<MobEffect> secondary = BeaconMenu.decodeEffect(secondaryId);
        if (!menu.hasPayment() || !canSelectBeaconPrimary(menu, primary) || !canSelectBeaconSecondary(menu, primary, secondary)) {
            return false;
        }

        menu.updateEffects(Optional.of(primary), Optional.ofNullable(secondary));
        return true;
    }

    private static boolean canSelectBeaconPrimary(BeaconMenu menu, @Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return false;
        }

        int unlockedTiers = Math.min(menu.getLevels(), Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.size()));
        for (int tier = 0; tier < unlockedTiers; tier++) {
            if (BeaconBlockEntity.BEACON_EFFECTS.get(tier).contains(effect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSelectBeaconSecondary(BeaconMenu menu, Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary) {
        if (secondary == null) {
            return true;
        }
        if (menu.getLevels() < 4) {
            return false;
        }
        if (primary.equals(secondary)) {
            return true;
        }
        return BeaconBlockEntity.BEACON_EFFECTS.size() > 3 && BeaconBlockEntity.BEACON_EFFECTS.get(3).contains(secondary);
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

        return defaultPlayerTargets(player, source.slot);
    }

    private static List<net.minecraft.world.inventory.Slot> defaultPlayerTargets(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot) {
        boolean sourceIsPlayerInventory = isPlayerInventorySlot(player, sourceSlot);
        int sourceContainerSlot = sourceSlot.getContainerSlot();
        if (sourceIsPlayerInventory && sourceContainerSlot >= 0 && sourceContainerSlot < 9) {
            return mainInventorySlots(player);
        }
        if (sourceIsPlayerInventory && sourceContainerSlot >= 9 && sourceContainerSlot < 36) {
            return hotbarSlots(player);
        }

        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        slots.addAll(mainInventorySlots(player));
        slots.addAll(hotbarSlots(player));
        return slots;
    }

    private static boolean moveSlotStack(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets) {
        if (!sourceSlot.isActive() || sourceSlot.isFake() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
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
            sourceSlot.setByPlayer(ItemStack.EMPTY, original);
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
            if (target == sourceSlot || !target.isActive() || target.isFake() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(moving, target.getItem())) {
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
            if (target == sourceSlot || !target.isActive() || target.isFake() || target.hasItem()) {
                continue;
            }
            target.safeInsert(moving);
        }
    }

    private static List<net.minecraft.world.inventory.Slot> containerSlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!isPlayerInventorySlot(player, slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static List<net.minecraft.world.inventory.Slot> mainInventorySlots(ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (isPlayerInventorySlot(player, slot) && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36) {
                slots.add(slot);
            }
        }
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

    private static void clickMenu(int debugId, ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input, ItemStack clientCarried) {
        if (slotIndex < AbstractContainerMenu.SLOT_CLICKED_OUTSIDE || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} slot={} reason=out-of-range", debugId, player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        ItemStack slotBefore = serverSlotStack(menu, slotIndex);
        ItemStack carriedBefore = sessions.carried.copy();
        ItemStack menuCarriedBefore = menu.getCarried().copy();
        ItemStack effectiveCarried = player.hasInfiniteMaterials() ? clientCarried.copy() : sessions.carried.copy();
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
            player.hasInfiniteMaterials()
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

    private static void syncCraftingResultSlot(ServerPlayer player, Session session) {
        if (!(session.menu instanceof AbstractCraftingMenu craftingMenu)) {
            return;
        }

        net.minecraft.world.inventory.Slot resultSlot = craftingMenu.getResultSlot();
        int slotIndex = session.menu.slots.indexOf(resultSlot);
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

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
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
            session.menu.setCarried(this.carried.copy());
            session.menu.setSynchronizer(new SessionSynchronizer(player, session));
            DesktopDebug.log("server session add player={} session={} title={} count={}", player.getName().getString(), session.sessionId, session.title.getString(), this.sessions.size());
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
        }

        private void close(ServerPlayer player, int sessionId, boolean notifyClient) {
            Session session = this.sessions.remove(sessionId);
            if (session == null) {
                DesktopDebug.trace("server close ignored player={} session={} reason=missing", player.getName().getString(), sessionId);
                return;
            }

            DesktopDebug.log("server session close player={} session={} title={} notify={}", player.getName().getString(), sessionId, session.title.getString(), notifyClient);
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
                }
            }
            this.reopenDormantGhosts(player);
        }

        private void broadcastAll(ServerPlayer player) {
            for (Session session : this.sessions.values()) {
                session.menu.broadcastChanges();
                syncMerchantOffers(player, session);
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
    }

    private record SlotSource(int sessionId, AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, @Nullable Session session) {
    }

    private record DormantGhostSource(String sourceKey) {
    }

    private static final class SessionSynchronizer implements ContainerSynchronizer {
        private final ServerPlayer player;
        private final Session session;

        private SessionSynchronizer(ServerPlayer player, Session session) {
            this.player = player;
            this.session = session;
        }

        @Override
        public void sendInitialData(AbstractContainerMenu menu, java.util.List<ItemStack> stacks, ItemStack carried, int[] dataSlots) {
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

        @Override
        public RemoteSlot createSlot() {
            return new TrackingRemoteSlot();
        }
    }

    private static final class TrackingRemoteSlot implements RemoteSlot {
        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public void force(ItemStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public void receive(HashedStack stack) {
            this.stack = ItemStack.EMPTY;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return ItemStack.matches(this.stack, stack);
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
        if (source == null || !source.dimension().equals(player.level().dimension().identifier().toString())) {
            return null;
        }

        ServerLevel level = player.level();
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
        double range = Math.max(player.blockInteractionRange(), 8.0D);
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
        return "entity:" + player.level().dimension().identifier() + ":" + entityId;
    }

    private static String sourceKeyForBlock(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().identifier().toString();
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connectedPos = ChestBlock.getConnectedBlockPos(pos, state);
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
