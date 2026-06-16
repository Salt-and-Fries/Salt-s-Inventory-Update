package com.salts_inventory_update.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;

public final class DesktopContainerSessions {
    private static final int MAX_SESSIONS = 16;
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
        ServerPlayNetworking.registerGlobalReceiver(DesktopCloseSessionPayload.TYPE, (payload, context) ->
            context.server().execute(() -> closeSession(context.player(), payload.sessionId(), true))
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

    public static OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = sessions(player);
        String sourceKey = sourceKeyForProvider(player, provider);
        if (sourceKey == null) {
            sourceKey = PENDING_USE_TARGETS.get(player.getUUID());
        }

        if (sessions.closeBySourceKey(player, sourceKey, true)) {
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
        sessions.add(player, session);
        DesktopDebug.log(
            "server capture menu player={} session={} container={} type={} title={} source={}",
            player.getName().getString(),
            session.sessionId,
            menu.containerId,
            session.menuTypeId,
            provider.getDisplayName().getString(),
            session.sourceKey
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

        ContainerInput[] inputs = ContainerInput.values();
        if (payload.inputOrdinal() < 0 || payload.inputOrdinal() >= inputs.length) {
            DesktopDebug.warn("server click dropped player={} session={} reason=bad-input ordinal={}", player.getName().getString(), payload.sessionId(), payload.inputOrdinal());
            return;
        }

        ContainerInput input = inputs[payload.inputOrdinal()];
        if (payload.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
            DesktopDebug.trace("server click player-menu player={} slot={} button={} input={}", player.getName().getString(), payload.slotIndex(), payload.button(), input);
            clickMenu(player, sessions, player.inventoryMenu, payload.slotIndex(), payload.button(), input);
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server click dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }

        DesktopDebug.trace("server click session player={} session={} slot={} button={} input={}", player.getName().getString(), payload.sessionId(), payload.slotIndex(), payload.button(), input);
        clickMenu(player, sessions, session.menu, payload.slotIndex(), payload.button(), input);
        session.menu.broadcastChanges();
        syncCarried(player, sessions);
    }

    private static void clickMenu(ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input) {
        if (slotIndex < AbstractContainerMenu.SLOT_CLICKED_OUTSIDE || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server click ignored player={} menu={} slot={} reason=out-of-range", player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        menu.setCarried(sessions.carried.copy());
        menu.clicked(slotIndex, button, input, player);
        sessions.carried = menu.getCarried().copy();
        player.inventoryMenu.setCarried(sessions.carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(sessions.carried.copy());
        }
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        send(player, new DesktopCarriedPayload(sessions.carried.copy()));
    }

    private static void closeSession(ServerPlayer player, int sessionId, boolean notifyClient) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            DesktopDebug.log("server close request player={} session={} notify={}", player.getName().getString(), sessionId, notifyClient);
            sessions.close(player, sessionId, notifyClient);
        }
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
        private boolean ready;
        private int nextSessionId = 1;
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

            for (Session session : List.copyOf(this.sessions.values())) {
                if (sourceKey.equals(session.sourceKey)) {
                    this.close(player, session.sessionId, notifyClient);
                    return true;
                }
            }

            return false;
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
            this.carried = ItemStack.EMPTY;
        }

        private void tick(ServerPlayer player) {
            for (Session session : List.copyOf(this.sessions.values())) {
                if (!session.menu.stillValid(player)) {
                    DesktopDebug.log("server session invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                    this.close(player, session.sessionId, true);
                } else {
                    session.menu.broadcastChanges();
                }
            }
        }

        private void broadcastAll(ServerPlayer player) {
            for (Session session : this.sessions.values()) {
                session.menu.broadcastChanges();
            }
            syncCarried(player, this);
        }
    }

    private record Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
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
