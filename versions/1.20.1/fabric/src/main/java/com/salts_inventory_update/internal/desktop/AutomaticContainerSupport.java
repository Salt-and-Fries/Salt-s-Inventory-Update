package com.salts_inventory_update.internal.desktop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;

/** Structural, loader-independent bridges. No per-mod/menu registration or name matching. */
public final class AutomaticContainerSupport {
    private static final java.util.Map<AbstractContainerMenu, java.util.Set<net.minecraft.world.inventory.Slot>> STORAGE =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private record Grid(Class<?> slotType, int xResidue, int yResidue) { }
    private AutomaticContainerSupport() { }
    public static void inferStorage(AbstractContainerMenu menu, Inventory inventory) {
        // A repeated homogeneous grid with no machine data is a storage candidate.
        // Isolated bait/filter/result cells are excluded rather than reordered.
        if (!((com.salts_inventory_update.mixin.accessor.AbstractContainerMenuAccessor)menu).salts_inventory_update$getDataSlots().isEmpty()) return;
        var slots = menu.slots.stream().filter(s -> s.container != inventory && s.isActive()).toList();
        var groups = new java.util.IdentityHashMap<net.minecraft.world.Container,
            java.util.Map<Grid, java.util.List<net.minecraft.world.inventory.Slot>>>();
        for (var slot : slots) groups.computeIfAbsent(slot.container, unused -> new java.util.HashMap<>())
            .computeIfAbsent(new Grid(slot.getClass(), Math.floorMod(slot.x, 18), Math.floorMod(slot.y, 18)),
                unused -> new java.util.ArrayList<>()).add(slot);
        java.util.List<net.minecraft.world.inventory.Slot> best = java.util.List.of();
        for (var grids : groups.values()) for (var group : grids.values()) if (group.size() > best.size()) best = group;
        if (best.size() < 5 || best.size() * 2 <= slots.size()) return;
        STORAGE.put(menu, java.util.Set.copyOf(best));
    }
    public static boolean isStorage(AbstractContainerMenu menu) { return STORAGE.containsKey(menu); }
    public static void release(AbstractContainerMenu menu) { STORAGE.remove(menu); }
    public static void notifyNativeOpened(ServerPlayer player, AbstractContainerMenu menu) {
        notifyNativeEvent(player, menu, "Open");
    }
    public static void notifyNativeClosed(ServerPlayer player, AbstractContainerMenu menu) {
        notifyNativeEvent(player, menu, "Close");
    }
    private static void notifyNativeEvent(ServerPlayer player, AbstractContainerMenu menu, String phase) {
        try {
            Class<?> event = Class.forName("net.minecraftforge.event.entity.player.PlayerContainerEvent$" + phase);
            Object opened = event.getConstructor(net.minecraft.world.entity.player.Player.class, AbstractContainerMenu.class)
                .newInstance(player, menu);
            Object bus = Class.forName("net.minecraftforge.common.MinecraftForge").getField("EVENT_BUS").get(null);
            Class<?> busApi = Class.forName("net.minecraftforge.eventbus.api.IEventBus");
            busApi.getMethod("post", Class.forName("net.minecraftforge.eventbus.api.Event")).invoke(bus, opened);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native loader opening event failed", failure);
        }
    }
    public static void openNormally(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer) {
        if (writer != null) {
            try {
                // The optional loader bridge is used only when the original opening
                // supplied Forge's explicit extra-data writer.
                Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
                hooks.getMethod("openScreen", ServerPlayer.class, MenuProvider.class, Consumer.class)
                    .invoke(null, player, provider, writer);
                return;
            } catch (ClassNotFoundException absentOnFabric) {
                // Fabric's extended provider writes its own opening data.
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Native loader reopening failed", failure);
            }
        }
        player.openMenu(provider);
    }
    public static boolean storageSlot(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot) {
        var slots = STORAGE.get(menu); return slots != null && slots.contains(slot);
    }
    public static boolean candidate(ServerPlayer player, AbstractContainerMenu menu) {
        return candidate(player,menu,false);
    }
    public static boolean candidate(ServerPlayer player, AbstractContainerMenu menu, boolean described) {
        MenuType<?> type = menu.getType();
        if (type == null || DesktopServerApi.hasWindowSupport(type)) return false;
        var id = BuiltInRegistries.MENU.getKey(type);
        if (id == null || id.getNamespace().equals("minecraft")) return false;
        int playerTop = Integer.MAX_VALUE, machineBottom = -1;
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot.container == player.getInventory()) playerTop = Math.min(playerTop, slot.y);
            else if (slot.x >= 0 && slot.y >= 0) machineBottom = Math.max(machineBottom, slot.y + 17);
        }
        return machineBottom > 0 && (described || playerTop != Integer.MAX_VALUE && machineBottom < playerTop);
    }
    public static Consumer<FriendlyByteBuf> openingWriter(MenuProvider provider, ServerPlayer player) {
        Method method = unique(provider.getClass(), void.class, ServerPlayer.class, FriendlyByteBuf.class);
        return method == null ? null : buffer -> invoke(method, provider, player, buffer);
    }
    public static AbstractContainerMenu create(MenuType<?> type, int id, Inventory inventory, byte[] data) {
        Method method = Arrays.stream(type.getClass().getMethods())
            .filter(m -> AbstractContainerMenu.class.isAssignableFrom(m.getReturnType()))
            .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{int.class, Inventory.class, FriendlyByteBuf.class}))
            .filter(m -> !m.isBridge()).findFirst().orElse(null);
        if (method == null) return type.create(id, inventory);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try { return (AbstractContainerMenu) invoke(method, type, id, inventory, buffer); }
        finally { buffer.release(); }
    }
    private static Method unique(Class<?> type, Class<?> result, Class<?>... params) {
        var matches = Arrays.stream(type.getMethods()).filter(m -> m.getReturnType() == result
            && Arrays.equals(m.getParameterTypes(), params) && !m.isBridge()).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }
    private static Object invoke(Method method, Object receiver, Object... values) {
        try { method.setAccessible(true); return method.invoke(receiver, values); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Native opening bridge failed", failure); }
    }
}
