package com.salts_inventory_update.internal.desktop;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Internal, opt-in protection for an inventory item that is the live backing
 * source of a detached desktop session. With no registered resolver every
 * method is a no-op, which keeps loaders without an integration unchanged.
 */
public final class DesktopItemSourceLocks {
    private static final Map<MenuType<?>, Resolver> RESOLVERS = new IdentityHashMap<>();
    private static final Map<Player, Map<SourceKey, ActiveLock>> ACTIVE = new WeakHashMap<>();
    private static final Lease NOOP_LEASE = () -> { };

    private DesktopItemSourceLocks() {
    }

    @FunctionalInterface
    public interface Resolver {
        @Nullable Source resolve(Player player, AbstractContainerMenu menu);
    }

    public static final class Source {
        private final int inventorySlot;
        private final String identity;
        private final ItemStack fingerprint;
        private final Predicate<ItemStack> identityMatcher;

        public Source(int inventorySlot, String identity, ItemStack fingerprint) {
            this(inventorySlot, identity, fingerprint, fingerprintMatcher(fingerprint));
        }

        public Source(int inventorySlot, String identity, ItemStack fingerprint, Predicate<ItemStack> identityMatcher) {
            if (inventorySlot < 0 || identity == null || identity.isEmpty() || fingerprint.isEmpty() || identityMatcher == null) {
                throw new IllegalArgumentException("Invalid desktop item source");
            }
            this.inventorySlot = inventorySlot;
            this.identity = identity;
            this.fingerprint = fingerprint.copy();
            this.identityMatcher = identityMatcher;
        }

        private static Predicate<ItemStack> fingerprintMatcher(ItemStack fingerprint) {
            ItemStack expected = fingerprint.copy();
            return stack -> ItemStack.isSameItemSameTags(expected, stack);
        }
    }

    @FunctionalInterface
    public interface Lease extends AutoCloseable {
        @Override
        void close();
    }

    public static synchronized void register(MenuType<?> menuType, Resolver resolver) {
        if (menuType == null || resolver == null) {
            throw new IllegalArgumentException("Desktop item source registration must be complete");
        }
        RESOLVERS.put(menuType, resolver);
    }

    public static Lease acquire(Player player, AbstractContainerMenu menu) {
        Resolver resolver;
        synchronized (DesktopItemSourceLocks.class) {
            resolver = RESOLVERS.get(menuType(menu));
        }
        if (resolver == null) {
            return NOOP_LEASE;
        }

        Source source;
        try {
            source = resolver.resolve(player, menu);
        } catch (RuntimeException ignored) {
            return NOOP_LEASE;
        }
        if (source == null || source.inventorySlot >= player.getInventory().getContainerSize()) {
            return NOOP_LEASE;
        }

        SourceKey key = new SourceKey(source.inventorySlot, source.identity);
        synchronized (DesktopItemSourceLocks.class) {
            Map<SourceKey, ActiveLock> playerLocks = ACTIVE.computeIfAbsent(player, ignored -> new HashMap<>());
            ActiveLock active = playerLocks.get(key);
            if (active == null) {
                playerLocks.put(key, new ActiveLock(source.fingerprint.copy(), source.identityMatcher, 1));
            } else {
                active.references++;
            }
        }
        return new ActiveLease(player, key);
    }

    public static synchronized boolean isInventorySlotLocked(Player player, int inventorySlot) {
        Map<SourceKey, ActiveLock> playerLocks = ACTIVE.get(player);
        if (playerLocks == null) {
            return false;
        }
        for (Map.Entry<SourceKey, ActiveLock> entry : playerLocks.entrySet()) {
            if (entry.getKey().inventorySlot == inventorySlot && isCurrentSource(player, entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSelectedSlotLocked(Player player) {
        return isInventorySlotLocked(player, player.getInventory().selected);
    }

    public static boolean isOffhandSlotLocked(Player player) {
        return isInventorySlotLocked(player, Inventory.SLOT_OFFHAND);
    }

    public static boolean isHandLocked(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
            ? isSelectedSlotLocked(player)
            : isOffhandSlotLocked(player);
    }

    public static synchronized boolean hasLockedSourceInRange(Player player, int fromInclusive, int toExclusive) {
        Map<SourceKey, ActiveLock> playerLocks = ACTIVE.get(player);
        if (playerLocks == null) {
            return false;
        }
        for (Map.Entry<SourceKey, ActiveLock> entry : playerLocks.entrySet()) {
            int inventorySlot = entry.getKey().inventorySlot;
            if (inventorySlot >= fromInclusive
                && inventorySlot < toExclusive
                && isCurrentSource(player, entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlotLocked(Player player, @Nullable Slot slot) {
        return slot != null
            && slot.container == player.getInventory()
            && isInventorySlotLocked(player, slot.getContainerSlot());
    }

    public static synchronized boolean matchesLockedSource(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Map<SourceKey, ActiveLock> playerLocks = ACTIVE.get(player);
        if (playerLocks == null) {
            return false;
        }
        for (Map.Entry<SourceKey, ActiveLock> entry : playerLocks.entrySet()) {
            if (isCurrentSource(player, entry.getKey(), entry.getValue())
                && entry.getValue().matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean anyLockedSourceMatches(Player player, Predicate<ItemStack> predicate) {
        Map<SourceKey, ActiveLock> playerLocks = ACTIVE.get(player);
        if (playerLocks == null) {
            return false;
        }
        for (Map.Entry<SourceKey, ActiveLock> entry : playerLocks.entrySet()) {
            if (isCurrentSource(player, entry.getKey(), entry.getValue())
                && predicate.test(currentSource(player, entry.getKey()).copy())) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldRejectClick(
        Player player,
        AbstractContainerMenu menu,
        int slotIndex,
        int button,
        ClickType input,
        ItemStack carried
    ) {
        Slot clicked = slotIndex == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
            ? null
            : DesktopMenuSlots.slot(menu, slotIndex);
        if (isSlotLocked(player, clicked)) {
            return true;
        }
        if (input == ClickType.SWAP && isInventorySlotLocked(player, button)) {
            return true;
        }
        return input == ClickType.PICKUP_ALL && matchesLockedSource(player, carried);
    }

    private static @Nullable MenuType<?> menuType(AbstractContainerMenu menu) {
        try {
            return menu.getType();
        } catch (UnsupportedOperationException ignored) {
            return null;
        }
    }

    private static boolean isCurrentSource(Player player, SourceKey key, ActiveLock active) {
        if (key.inventorySlot < 0 || key.inventorySlot >= player.getInventory().getContainerSize()) {
            return false;
        }
        ItemStack current = currentSource(player, key);
        return !current.isEmpty() && active.matches(current);
    }

    private static ItemStack currentSource(Player player, SourceKey key) {
        if (key.inventorySlot < 0 || key.inventorySlot >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(key.inventorySlot);
    }

    private static synchronized void release(Player player, SourceKey key) {
        Map<SourceKey, ActiveLock> playerLocks = ACTIVE.get(player);
        if (playerLocks == null) {
            return;
        }
        ActiveLock active = playerLocks.get(key);
        if (active == null) {
            return;
        }
        if (--active.references <= 0) {
            playerLocks.remove(key);
        }
        if (playerLocks.isEmpty()) {
            ACTIVE.remove(player);
        }
    }

    private record SourceKey(int inventorySlot, String identity) {
    }

    private static final class ActiveLock {
        private final ItemStack fingerprint;
        private final Predicate<ItemStack> identityMatcher;
        private int references;

        private ActiveLock(ItemStack fingerprint, Predicate<ItemStack> identityMatcher, int references) {
            this.fingerprint = fingerprint;
            this.identityMatcher = identityMatcher;
            this.references = references;
        }

        private boolean matches(ItemStack stack) {
            try {
                return this.identityMatcher.test(stack);
            } catch (RuntimeException ignored) {
                return false;
            }
        }
    }

    private static final class ActiveLease implements Lease {
        private final Player player;
        private final SourceKey key;
        private boolean closed;

        private ActiveLease(Player player, SourceKey key) {
            this.player = player;
            this.key = key;
        }

        @Override
        public void close() {
            synchronized (DesktopItemSourceLocks.class) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                release(this.player, this.key);
            }
        }
    }
}
