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
    public static boolean notifyNativeOpened(ServerPlayer player, AbstractContainerMenu menu) {
        return notifyNativeEvent(player, menu, "Open");
    }
    public static void notifyNativeClosed(ServerPlayer player, AbstractContainerMenu menu) { notifyNativeEvent(player,menu,"Close"); }
    private static boolean notifyNativeEvent(ServerPlayer player, AbstractContainerMenu menu, String phase) {
        try {
            Class<?> event=Class.forName("net.neoforged.neoforge.event.entity.player.PlayerContainerEvent$"+phase);
            Object value=event.getConstructor(net.minecraft.world.entity.player.Player.class,AbstractContainerMenu.class).newInstance(player,menu);
            Object bus=Class.forName("net.neoforged.neoforge.common.NeoForge").getField("EVENT_BUS").get(null);
            Method post=Arrays.stream(bus.getClass().getMethods()).filter(m->m.getName().equals("post")&&m.getParameterCount()==1&&m.getParameterTypes()[0].isInstance(value)).findFirst().orElseThrow();
            invoke(post,bus,value);return true;
        } catch(ClassNotFoundException absentOnFabric) { return false; }
        catch(ReflectiveOperationException failure) { throw new IllegalStateException("Native loader event failed",failure); }
    }
    public static void openNormally(ServerPlayer player, MenuProvider provider, Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer) {
        if(writer!=null) try {
            player.getClass().getMethod("openMenu",MenuProvider.class,Consumer.class).invoke(player,provider,writer);return;
        } catch(NoSuchMethodException absentOnFabric) { }
        catch(ReflectiveOperationException failure) { throw new IllegalStateException("Native loader reopening failed",failure); }
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
    @SuppressWarnings({"rawtypes","unchecked"})
    public static Consumer<net.minecraft.network.RegistryFriendlyByteBuf> openingWriter(MenuProvider provider,ServerPlayer player,MenuType<?> type) {
        try {
            Method data=provider.getClass().getMethod("getScreenOpeningData",ServerPlayer.class);
            Object value=invoke(data,provider,player);
            var codec=(net.minecraft.network.codec.StreamCodec)openingCodec(type);
            return buffer->codec.encode(buffer,value);
        } catch(NoSuchMethodException ordinaryProvider) { return null; }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static AbstractContainerMenu create(MenuType<?> type,int id,Inventory inventory,net.minecraft.core.RegistryAccess registries,byte[] data) {
        net.minecraft.network.RegistryFriendlyByteBuf buffer=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data),registries);
        try {
            try {
                var codec=(net.minecraft.network.codec.StreamCodec)openingCodec(type);
                Object decoded=codec.decode(buffer);
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing native opening data");
                Method factory=Arrays.stream(type.getClass().getMethods()).filter(m->m.getName().equals("create")&&m.getParameterCount()==3&&m.getParameterTypes()[0]==int.class&&m.getParameterTypes()[1]==Inventory.class&&(decoded==null||m.getParameterTypes()[2].isInstance(decoded))&&!m.isBridge()).findFirst().orElseThrow();
                return (AbstractContainerMenu)invoke(factory,type,id,inventory,decoded);
            } catch(NoSuchMethodException notFabricExtended) {
                Method factory=Arrays.stream(type.getClass().getMethods()).filter(m->m.getName().equals("create")&&m.getParameterCount()==3&&m.getParameterTypes()[0]==int.class&&m.getParameterTypes()[1]==Inventory.class&&m.getParameterTypes()[2]!=Object.class&&m.getParameterTypes()[2].isInstance(buffer)&&!m.isBridge()).findFirst().orElse(null);
                return factory==null?type.create(id,inventory):(AbstractContainerMenu)invoke(factory,type,id,inventory,buffer);
            }
        } finally { buffer.release(); }
    }
    /** Public Fabric API getter names changed with ExtendedMenuType. */
    private static Object openingCodec(MenuType<?> type) throws NoSuchMethodException {
        for (String getter : new String[]{"getPacketCodec", "getStreamCodec"}) {
            try { return invoke(type.getClass().getMethod(getter),type); }
            catch (NoSuchMethodException otherGeneration) { }
        }
        throw new NoSuchMethodException("No public extended-menu stream codec");
    }
    private static Method unique(Class<?> type, Class<?> result, Class<?>... params) {
        var matches = Arrays.stream(type.getMethods()).filter(m -> m.getReturnType() == result
            && Arrays.equals(m.getParameterTypes(), params) && !m.isBridge()).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }
    private static Object invoke(Method method, Object receiver, Object... values) {
        try { method.setAccessible(true); return method.invoke(receiver, values); }
        catch (java.lang.reflect.InvocationTargetException failure) { throw new IllegalStateException("Native opening bridge failed: " + failure.getCause(), failure.getCause()); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Native opening bridge failed", failure); }
    }
}
