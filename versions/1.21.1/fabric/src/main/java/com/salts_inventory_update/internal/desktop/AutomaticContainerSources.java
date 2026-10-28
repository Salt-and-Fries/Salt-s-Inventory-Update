package com.salts_inventory_update.internal.desktop;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Instance-owned source inference, used only after automatic eligibility is established. */
public final class AutomaticContainerSources {
    private static final Map<AbstractContainerMenu, Source> SOURCES = Collections.synchronizedMap(new WeakHashMap<>());
    private record Source(Player owner, BlockEntity block, int inventorySlot, ItemStack backing) { }
    private AutomaticContainerSources() { }
    public static String bind(ServerPlayer owner, AbstractContainerMenu menu, MenuProvider provider) {
        ItemStack backing = itemField(menu, owner);
        if (backing == null) backing = itemField(provider, owner);
        if (backing != null) {
            for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
                if (owner.getInventory().getItem(i) == backing) {
                    SOURCES.put(menu, new Source(owner, null, i, backing));
                    return "automatic-item:" + owner.getUUID() + ":" + i;
                }
            }
        }
        BlockEntity block = provider instanceof BlockEntity be ? be : null;
        if (block == null) for (var slot : menu.slots) if (slot.container instanceof BlockEntity be) { block = be; break; }
        if (block != null) SOURCES.put(menu, new Source(owner, block, -1, ItemStack.EMPTY));
        return null;
    }
    public static void bindClient(Player owner, AbstractContainerMenu menu, String key) {
        if (!key.startsWith("automatic-item:" + owner.getUUID() + ":")) return;
        try {
            int slot = Integer.parseInt(key.substring(key.lastIndexOf(':') + 1));
            if (slot >= 0 && slot < owner.getInventory().getContainerSize())
                SOURCES.put(menu, new Source(owner, null, slot, owner.getInventory().getItem(slot)));
        } catch (NumberFormatException ignored) { }
    }
    private static ItemStack itemField(Object instance, Player owner) {
        for (Class<?> type = instance.getClass(); type != null && type != AbstractContainerMenu.class && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != ItemStack.class) continue;
                try {
                    if (!field.trySetAccessible()) continue;
                    ItemStack stack = (ItemStack)field.get(instance);
                    if (stack == null || stack.isEmpty()) continue;
                    for (int i = 0; i < owner.getInventory().getContainerSize(); i++)
                        if (owner.getInventory().getItem(i) == stack) return stack;
                } catch (IllegalAccessException | SecurityException ignored) { }
            }
        }
        return null;
    }
    public static boolean valid(AbstractContainerMenu menu) {
        Source source = SOURCES.get(menu); if (source == null) return true;
        if (source.block() != null) return source.block().hasLevel() && !source.block().isRemoved()
            && source.block().getLevel() == source.owner().level()
            && source.block().getLevel().hasChunkAt(source.block().getBlockPos())
            && Container.stillValidBlockEntity(source.block(), source.owner());
        ItemStack current = source.owner().getInventory().getItem(source.inventorySlot());
        return source.owner() instanceof ServerPlayer ? current == source.backing()
            : !current.isEmpty() && current.is(source.backing().getItem());
    }
    public static boolean locks(Player owner, int inventorySlot) {
        synchronized (SOURCES) {
            for (var entry : SOURCES.entrySet()) {
                Source source = entry.getValue();
                if (source.owner() == owner && source.inventorySlot() == inventorySlot && valid(entry.getKey())) return true;
            }
        }
        return false;
    }
    /** Native item inventories may persist their contents into their locked backing stack on each slot write. */
    public static boolean permitsBackingTagUpdate(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack before) {
        Source source = SOURCES.get(menu);
        ItemStack current = slot.getItem();
        return source != null && source.inventorySlot() >= 0 && slot.container == source.owner().getInventory()
            && slot.getContainerSlot() == source.inventorySlot() && current == source.backing()
            && !current.isEmpty() && current.is(before.getItem()) && current.getCount() == before.getCount();
    }
    public static void release(AbstractContainerMenu menu) {
        SOURCES.remove(menu);
        AutomaticContainerSupport.release(menu);
    }
}
