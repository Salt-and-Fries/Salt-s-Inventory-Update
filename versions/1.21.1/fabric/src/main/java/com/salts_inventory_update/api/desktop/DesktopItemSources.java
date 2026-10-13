package com.salts_inventory_update.api.desktop;

import java.util.function.Predicate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;

/** Opt-in protection for items backing detached inventory sessions.
 * Core acquires and releases reference-counted locks with each session.
 * Resolvers must be pure and identify an item in the supplied player's inventory.
 */
public final class DesktopItemSources {
    private DesktopItemSources() { }
    @FunctionalInterface public interface Resolver {
        @Nullable Source resolve(Player player, AbstractContainerMenu menu);
    }
    public record Source(int inventorySlot, String identity, ItemStack fingerprint, Predicate<ItemStack> identityMatcher) {
        public Source(int inventorySlot, String identity, ItemStack fingerprint) {
            this(inventorySlot, identity, fingerprint, matcher(fingerprint));
        }
        public Source { fingerprint = fingerprint.copy(); }
        @Override public ItemStack fingerprint() { return fingerprint.copy(); }
        private static Predicate<ItemStack> matcher(ItemStack fingerprint) {
            ItemStack expected = fingerprint.copy();
            return stack -> ItemStack.isSameItemSameComponents(expected, stack);
        }
    }
    public static void register(MenuType<?> type, Resolver resolver) {
        DesktopItemSourceLocks.register(type, (player, menu) -> {
            Source value = resolver.resolve(player, menu);
            return value == null ? null : new DesktopItemSourceLocks.Source(value.inventorySlot(), value.identity(), value.fingerprint(), value.identityMatcher());
        });
    }
    public static boolean isInventorySlotLocked(Player player, int slot) { return DesktopItemSourceLocks.isInventorySlotLocked(player, slot); }
    public static boolean isSelectedSlotLocked(Player player) { return DesktopItemSourceLocks.isSelectedSlotLocked(player); }
    public static boolean isOffhandSlotLocked(Player player) { return DesktopItemSourceLocks.isOffhandSlotLocked(player); }
    public static boolean isHandLocked(Player player, InteractionHand hand) { return DesktopItemSourceLocks.isHandLocked(player, hand); }
    public static boolean isSlotLocked(Player player, @Nullable Slot slot) { return DesktopItemSourceLocks.isSlotLocked(player, slot); }
    public static boolean hasLockedSourceInRange(Player player, int from, int to) { return DesktopItemSourceLocks.hasLockedSourceInRange(player, from, to); }
    public static boolean matchesLockedSource(Player player, ItemStack stack) { return DesktopItemSourceLocks.matchesLockedSource(player, stack); }
    public static boolean anyLockedSourceMatches(Player player, Predicate<ItemStack> predicate) { return DesktopItemSourceLocks.anyLockedSourceMatches(player, predicate); }
    public static boolean shouldRejectClick(Player player, AbstractContainerMenu menu, int slot, int button, ClickType type, ItemStack carried) {
        return DesktopItemSourceLocks.shouldRejectClick(player, menu, slot, button, type, carried);
    }
}
