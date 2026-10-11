package com.salts_inventory_update.client;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.protocol.DesktopProtocol;
import com.salts_inventory_update.server.DesktopContainerSessions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the shipped API, capture gate and packet codecs on each real mapped game runtime. */
final class DesktopApiContractTest {
    private static sun.misc.Unsafe unsafe;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }

    @Test void declaredRenderersAndPerInstanceEligibilityControlAutomaticCapture() throws Exception {
        MenuType<TestMenu> type = menuType("capability");
        TestMenu menu = new TestMenu(type);
        AtomicBoolean eligible = new AtomicBoolean(true);
        DesktopServerApi.registerWindow(type, new DesktopServerWindowHandler<TestMenu, Void>() {
            @Override public boolean supports(ServerPlayer player, TestMenu candidate) { return eligible.get(); }
        });
        Object sessions = sessions();
        set(sessions, "forcedMenuIds", Set.of());
        set(sessions, "supportedMenuIds", Set.of());
        ServerPlayer player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        assertFalse(capture(player, sessions, menu), "Common registration alone keeps the original UI");
        DesktopClientApi.registerWindow(type, new DesktopWindowDefinition<TestMenu, Void>() { });
        String id = BuiltInRegistries.MENU.getKey(type).toString();
        assertTrue(DesktopClientApi.supportedMenuIds().contains(id));
        Object advertised = isLegacy() ? BuiltInRegistries.MENU.getKey(type) : id;
        set(sessions, "supportedMenuIds", Set.of(advertised));
        assertTrue(capture(player, sessions, menu));
        eligible.set(false);
        assertFalse(capture(player, sessions, menu), "A conditional layout can leave this instance on its normal UI");
        DesktopWindowDefinition<TestMenu, Void> replacement = new DesktopWindowDefinition<>() { };
        assertThrows(IllegalStateException.class, () -> DesktopClientApi.registerWindow(type, replacement));
    }

    @Test void failedProviderDoesNotAdvertiseItsPartiallyRegisteredRenderer() throws Exception {
        MenuType<TestMenu> type = menuType("rollback");
        String id = BuiltInRegistries.MENU.getKey(type).toString();
        assertThrows(IllegalStateException.class, () -> DesktopRegistrationScope.run("api_fixture", () -> {
            DesktopClientApi.registerWindow(type, new DesktopWindowDefinition<TestMenu, Void>() { });
            throw new IllegalStateException("Provider failed after registration");
        }));
        assertFalse(DesktopClientApi.supportedMenuIds().contains(id));
    }

    @Test void customOwnershipClassifiesInventoryWrappersWithoutCoordinateAssumptions() throws Exception {
        MenuType<TestMenu> type = menuType("ownership");
        TestMenu menu = new TestMenu(type);
        Slot wrappedInventory = menu.slots.get(0);
        Slot machine = menu.slots.get(1);
        DesktopMenuBindings.register(type, new DesktopMenuBindings.SlotAccess() {
            @Override public int size(AbstractContainerMenu value) { return value.slots.size(); }
            @Override public boolean isPlayerInventorySlot(AbstractContainerMenu value, Slot slot, Inventory inventory) {
                return slot == wrappedInventory;
            }
        }, false);
        Inventory inventory = (Inventory) unsafe.allocateInstance(Inventory.class);
        assertTrue(DesktopMenuBindings.isPlayerInventorySlot(menu, wrappedInventory, inventory));
        assertFalse(DesktopMenuBindings.isPlayerInventorySlot(menu, machine, inventory));
        Method collect = DesktopContainerSession.class.getDeclaredMethod("findContainerSlots", AbstractContainerMenu.class, Inventory.class);
        collect.setAccessible(true);
        assertEquals(List.of(machine), collect.invoke(null, menu, inventory));
    }

    @Test void helloAndModeRoundTripBothBoundedListsIndependently() throws Exception {
        var forced = List.of("api_fixture:forced");
        var supported = List.of("api_fixture:machine", "api_fixture:terminal");
        var hello = new DesktopHelloPayload(DesktopProtocol.VERSION, 17L, 7L, true, forced, supported);
        var mode = new DesktopModePayload(17L, 1L, false, forced, supported);
        assertEquals(hello, roundTrip(hello));
        assertEquals(mode, roundTrip(mode));
        assertThrows(IllegalArgumentException.class, () -> new DesktopHelloPayload(DesktopProtocol.VERSION, 17L,
            7L, true, forced, java.util.Collections.nCopies(DesktopProtocol.MAX_SUPPORTED_MENU_IDS + 1, "x:y")));
    }

    @Test void wireDecoderRejectsAnOversizedSupportedListBeforeAllocatingIt() throws Exception {
        FriendlyByteBuf buffer = buffer();
        try {
            buffer.writeLong(17L);
            buffer.writeVarLong(1L);
            buffer.writeBoolean(true);
            buffer.writeVarInt(0);
            buffer.writeVarInt(DesktopProtocol.MAX_SUPPORTED_MENU_IDS + 1);
            Constructor<?> decoder = DesktopModePayload.class.getDeclaredConstructor(buffer.getClass());
            decoder.setAccessible(true);
            var failure = assertThrows(InvocationTargetException.class, () -> decoder.newInstance(buffer));
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        } finally { buffer.release(); }
    }

    private static Object roundTrip(Object payload) throws Exception {
        FriendlyByteBuf buffer = buffer();
        try {
            Method write = payload.getClass().getDeclaredMethod("write", buffer.getClass());
            write.setAccessible(true);
            write.invoke(payload, buffer);
            Constructor<?> decoder = payload.getClass().getDeclaredConstructor(buffer.getClass());
            decoder.setAccessible(true);
            Object result = decoder.newInstance(buffer);
            assertEquals(0, buffer.readableBytes());
            return result;
        } finally { buffer.release(); }
    }

    @Test void companionOwnershipEnvelopeCopiesAndBoundsAuthenticatedOwnerIds() throws Exception {
        int menuId = BuiltInRegistries.MENU.getId(MenuType.GENERIC_9x1);
        int[] owners = {1, 3};
        var payload = new com.salts_inventory_update.network.DesktopMenuOpenDataPayload(7L, 20, 8L, menuId, -1,
            new byte[]{9, 4}, owners, true);
        owners[0] = 99;
        int[] returned = payload.companionOwnerIds(); returned[0] = 100;
        assertArrayEquals(new int[]{1, 3}, payload.companionOwnerIds());
        var decoded = (com.salts_inventory_update.network.DesktopMenuOpenDataPayload) roundTrip(payload);
        assertArrayEquals(new int[]{1, 3}, decoded.companionOwnerIds());
        assertArrayEquals(new byte[]{9, 4}, decoded.data());
        assertTrue(decoded.metadataOnly()); assertEquals(8L, decoded.sessionToken());
        assertThrows(IllegalArgumentException.class, () -> new com.salts_inventory_update.network.DesktopMenuOpenDataPayload(
            7L, 20, 8L, menuId, -1, new byte[0], new int[]{1, 1}, true));
        assertThrows(IllegalArgumentException.class, () -> new com.salts_inventory_update.network.DesktopMenuOpenDataPayload(
            7L, 20, 8L, menuId, -1, new byte[0], new int[]{20}, true));
        assertThrows(IllegalArgumentException.class, () -> new com.salts_inventory_update.network.DesktopMenuOpenDataPayload(
            7L, 20, 8L, menuId, -1, new byte[0], new int[0], true));
        FriendlyByteBuf buffer = buffer();
        try {
            buffer.writeLong(7L); buffer.writeVarInt(20); buffer.writeLong(8L); buffer.writeVarInt(menuId);
            buffer.writeVarInt(0); buffer.writeVarInt(0); buffer.writeVarInt(DesktopProtocol.MAX_DESKTOP_SESSIONS + 1);
            Constructor<?> reader = payload.getClass().getDeclaredConstructor(buffer.getClass()); reader.setAccessible(true);
            var error = assertThrows(InvocationTargetException.class, () -> reader.newInstance(buffer));
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
        } finally { buffer.release(); }
    }

    private static FriendlyByteBuf buffer() throws Exception {
        if (isLegacy()) return new FriendlyByteBuf(Unpooled.buffer());
        Class<?> registryBuffer = Class.forName("net.minecraft.network.RegistryFriendlyByteBuf");
        Class<?> access = Class.forName("net.minecraft.core.RegistryAccess");
        Object empty = access.getField("EMPTY").get(null);
        return (FriendlyByteBuf) registryBuffer.getConstructor(ByteBuf.class, access).newInstance(Unpooled.buffer(), empty);
    }

    @Test @SuppressWarnings("unchecked")
    void recipeSourceScanRejectsRevokedCompanionPermissionBeforeItsNextTick() throws Exception {
        ServerPlayer player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        var inventory = (Inventory) unsafe.allocateInstance(Inventory.class);
        Field inventoryField = Player.class.getDeclaredField("inventory"); inventoryField.setAccessible(true); inventoryField.set(player, inventory);
        var playerMenu = (net.minecraft.world.inventory.InventoryMenu) unsafe.allocateInstance(net.minecraft.world.inventory.InventoryMenu.class);
        Field playerMenuField = Player.class.getDeclaredField("inventoryMenu"); playerMenuField.setAccessible(true); playerMenuField.set(player, playerMenu);
        Field slots = AbstractContainerMenu.class.getDeclaredField("slots"); slots.setAccessible(true);
        slots.set(player.inventoryMenu, net.minecraft.core.NonNullList.<Slot>create());
        Object sessions = sessions();
        Class<?> sessionType = Class.forName(DesktopContainerSessions.class.getName() + "$Session");
        Object parent = unsafe.allocateInstance(sessionType), child = unsafe.allocateInstance(sessionType);
        TestMenu menu = new TestMenu(MenuType.GENERIC_9x1), parentMenu = new TestMenu(MenuType.GENERIC_9x1);
        menu.slots.get(1).set(new ItemStack(net.minecraft.world.item.Items.DIRT, 9));
        set(parent, "sessionId", 1); set(parent, "menu", parentMenu);
        set(child, "sessionId", 2); set(child, "menu", menu); set(child, "visibleToClient", true);
        Field sessionMap = sessions.getClass().getDeclaredField("sessions"); sessionMap.setAccessible(true);
        Map<Integer, Object> registered = (Map<Integer, Object>) sessionMap.get(sessions);
        registered.put(1, parent); registered.put(2, child);
        Class<?> companionType = Class.forName(DesktopContainerSessions.class.getName() + "$Companion");
        Constructor<?> companionCtor = companionType.getDeclaredConstructors()[0]; companionCtor.setAccessible(true);
        Object companion = companionCtor.newInstance(com.salts_inventory_update.api.desktop.DesktopInventoryIdentity.reference(menu), child, null);
        set(child, "companion", companion);
        AtomicBoolean permission = new AtomicBoolean(true);
        var provider = new net.minecraft.world.SimpleMenuProvider((id, inv, owner) -> menu, net.minecraft.network.chat.Component.literal("Tool"));
        var spec = new com.salts_inventory_update.api.server.desktop.DesktopCompanionSpec(
            com.salts_inventory_update.api.desktop.DesktopInventoryIdentity.reference(menu), provider, owner -> permission.get());
        Class<?> ownershipType = Class.forName(DesktopContainerSessions.class.getName() + "$CompanionOwnership");
        Constructor<?> ownershipCtor = ownershipType.getDeclaredConstructors()[0]; ownershipCtor.setAccessible(true);
        Object ownership = ownershipCtor.newInstance(player, sessions, companion, parent, spec);
        Field ownersField = companionType.getDeclaredField("owners"); ownersField.setAccessible(true);
        Map<Integer, Object> owners = (Map<Integer, Object>) ownersField.get(companion); owners.put(1, ownership);
        Method scan = List.of(DesktopContainerSessions.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("jeiTransferSourceSlots")).findFirst().orElseThrow(); scan.setAccessible(true);
        java.util.concurrent.Callable<List<Slot>> sources = () -> (List<Slot>) (scan.getParameterCount() == 3
            ? scan.invoke(null, player, sessions, Set.of()) : scan.invoke(null, player, sessions, 1, Set.of()));
        assertTrue(sources.call().contains(menu.slots.get(1)));
        permission.set(false);
        assertTrue(menu.stillValid(player), "Native menu validity alone does not grant companion permission");
        assertFalse(sources.call().contains(menu.slots.get(1)), "Revocation applies before the next lifecycle tick");
        assertEquals(9, menu.slots.get(1).getItem().getCount());
        Object secondParent = unsafe.allocateInstance(sessionType); set(secondParent, "sessionId", 3); set(secondParent, "menu", parentMenu);
        registered.put(3, secondParent);
        var secondSpec = new com.salts_inventory_update.api.server.desktop.DesktopCompanionSpec(spec.identity(), provider, owner -> true);
        Object second = ownershipCtor.newInstance(player, sessions, companion, secondParent, secondSpec); owners.put(3, second);
        assertTrue(sources.call().contains(menu.slots.get(1)), "Another independently authorized owner keeps the source usable");
        registered.remove(3);
        assertFalse(sources.call().contains(menu.slots.get(1)), "A stale owner cannot retain recipe-transfer permission");
    }

    private static boolean isLegacy() { return DesktopHelloPayload.class.getInterfaces()[0].getSimpleName().equals("DesktopPacket"); }

    private static Object sessions() throws Exception {
        Class<?> type = Class.forName(DesktopContainerSessions.class.getName() + "$PlayerSessions");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    private static boolean capture(ServerPlayer player, Object sessions, TestMenu menu) throws Exception {
        Method method = List.of(DesktopContainerSessions.class.getDeclaredMethods()).stream()
            .filter(value -> value.getName().equals("isDesktopSupportedMenu")).findFirst().orElseThrow();
        method.setAccessible(true);
        if (!isLegacy()) return (boolean) method.invoke(null, player, sessions, menu);
        Field field = DesktopContainerSessions.class.getDeclaredField("PLAYERS");
        field.setAccessible(true);
        Map<Object, Object> players = (Map<Object, Object>) field.get(null);
        Object previous = players.put(player.connection, sessions);
        try { return (boolean) method.invoke(null, player, menu); }
        finally { if (previous == null) players.remove(player.connection); else players.put(player.connection, previous); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MenuType<TestMenu> menuType(String name) throws Exception {
        MenuType<TestMenu> type = (MenuType<TestMenu>) unsafe.allocateInstance(MenuType.class);
        // Headless bootstrap freezes vanilla registries. Add fixture entries as a loader would before freezing.
        Field frozen = BuiltInRegistries.MENU.getClass().getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean previous = frozen.getBoolean(BuiltInRegistries.MENU);
        frozen.setBoolean(BuiltInRegistries.MENU, false);
        try { Registry.register((Registry) BuiltInRegistries.MENU, "api_fixture:" + name, type); }
        finally { frozen.setBoolean(BuiltInRegistries.MENU, previous); }
        return type;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu(MenuType<?> type) {
            super(type, 1);
            var storage = new SimpleContainer(2);
            addSlot(new Slot(storage, 0, 280, 5));
            addSlot(new Slot(storage, 1, -20, 180));
        }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int id) { return ItemStack.EMPTY; }
    }
}
