package com.salts_inventory_update.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DesktopPickupAllTest {
    private static sun.misc.Unsafe unsafe;
    private PickupAllTestPlayer player;
    private Object sessions;

    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }

    @BeforeEach
    void setup() throws Exception {
        player = (PickupAllTestPlayer) unsafe.allocateInstance(PickupAllTestPlayer.class);
        player.inventory = (net.minecraft.world.entity.player.Inventory) unsafe.allocateInstance(net.minecraft.world.entity.player.Inventory.class);
        // Native item-click hooks query enabled world features even for an ordinary dirt stack.
        Object server = unsafe.allocateInstance(Class.forName("net.minecraft.client.server.IntegratedServer"));
        Class<?> worldDataType = Class.forName("net.minecraft.world.level.storage.WorldData");
        Object worldData = java.lang.reflect.Proxy.newProxyInstance(worldDataType.getClassLoader(), new Class<?>[] {worldDataType},
            (proxy, method, args) -> {
                if (method.getName().equals("enabledFeatures")) return net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS;
                throw new UnsupportedOperationException(method.getName());
            });
        set(server, "worldData", worldData);
        Object level = unsafe.allocateInstance(Class.forName("net.minecraft.server.level.ServerLevel"));
        set(level, "server", server);
        set(player, "level", level);
        InventoryMenu inventory = (InventoryMenu) unsafe.allocateInstance(InventoryMenu.class);
        inventory.setCarried(ItemStack.EMPTY);
        Field field = Player.class.getDeclaredField("inventoryMenu");
        field.setAccessible(true);
        field.set(player, inventory);
        Class<?> type = Class.forName(DesktopContainerSessions.class.getName() + "$PlayerSessions");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        sessions = constructor.newInstance();
    }

    @Test
    void emptyServerCursorPicksUpTheJustPlacedAnchorUsingNativeCallbacks() throws Exception {
        TestMenu menu = new TestMenu();
        menu.slots.get(0).set(new ItemStack(Items.DIRT, 12));
        ItemStack carried = prepare(menu, 0, 0);
        assertEquals(12, carried.getCount());
        assertEquals(12, player.inventoryMenu.getCarried().getCount());
        assertTrue(menu.slots.get(0).getItem().isEmpty());
        assertEquals(12, menu.taken);
    }

    @Test
    void loadedServerCursorDoesNotPickUpTheAnchorAgain() throws Exception {
        player.inventoryMenu.setCarried(new ItemStack(Items.DIRT, 3));
        TestMenu menu = new TestMenu();
        menu.slots.get(0).set(new ItemStack(Items.DIRT, 12));
        assertEquals(3, prepare(menu, 0, 0).getCount());
        assertEquals(12, menu.slots.get(0).getItem().getCount());
        assertEquals(0, menu.taken);
    }

    @Test
    void rightButtonAndInvalidOrReadOnlyAnchorsCannotStartPickup() throws Exception {
        TestMenu menu = new TestMenu();
        menu.slots.get(0).set(new ItemStack(Items.DIRT, 12));
        assertTrue(prepare(menu, 0, 1).isEmpty());
        assertTrue(prepare(menu, -1, 0).isEmpty());
        assertTrue(prepare(menu, 9, 0).isEmpty());
        menu.pickupAllowed = false;
        assertTrue(prepare(menu, 0, 0).isEmpty());
        assertEquals(12, menu.slots.get(0).getItem().getCount());
        assertTrue(player.inventoryMenu.getCarried().isEmpty());
        assertEquals(0, menu.taken);
    }

    @Test
    void invalidMenuCannotStartPickup() throws Exception {
        TestMenu menu = new TestMenu();
        menu.slots.get(0).set(new ItemStack(Items.DIRT, 12));
        menu.valid = false;
        assertTrue(prepare(menu, 0, 0).isEmpty());
        assertEquals(12, menu.slots.get(0).getItem().getCount());
        assertEquals(0, menu.taken);
    }

    private ItemStack prepare(TestMenu menu, int slot, int button) throws Exception {
        Method method = java.util.Arrays.stream(DesktopContainerSessions.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("preparePickupAllCarried")).findFirst().orElseThrow();
        method.setAccessible(true);
        return (ItemStack) method.invoke(null, player, sessions, menu, slot, button);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private boolean pickupAllowed = true;
        private boolean valid = true;
        private int taken;

        TestMenu() {
            super(null, 5);
            addSlot(new Slot(new SimpleContainer(1), 0, 0, 0) {
                @Override public boolean mayPickup(Player player) { return pickupAllowed; }
                @Override public void onTake(Player player, ItemStack stack) {
                    taken += stack.getCount();
                    super.onTake(player, stack);
                }
            });
            setCarried(ItemStack.EMPTY);
        }

        @Override public boolean stillValid(Player player) { return valid; }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
