package com.salts_inventory_update.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the production screen without opening a game window or connecting a server. */
final class DesktopSlotGesturesTest {
    private static sun.misc.Unsafe unsafe;

    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
    }

    @Test
    void earlierPlacementReplyDoesNotEraseTheNextClick() throws Exception {
        InventoryDesktopScreen screen = screen(32);
        TestMenu menu = new TestMenu(1);
        press(screen, hit(menu, 0));
        Object pending = get(screen, "pendingSlotClick");

        screen.setSharedCarried(new ItemStack(Items.DIRT, 31));
        assertSame(pending, get(screen, "pendingSlotClick"));
        invoke(screen, "completePendingSlotClick");
        assertNull(get(screen, "pendingSlotClick"));
        assertEquals(2, get(screen, "nextDebugClickId"), "The release must still dispatch its click");
    }

    @Test
    void countReplyPreservesDragAndLimitsTargetsToTheRemainingItems() throws Exception {
        InventoryDesktopScreen screen = screen(32);
        TestMenu menu = new TestMenu(5);
        press(screen, hit(menu, 0));
        Object drag = nested("DragDistribution", leftButton(), AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE);
        for (int i = 0; i < 5; i++) invoke(drag, "add", hit(menu, i));
        set(screen, "dragDistribution", drag);

        screen.setSharedCarried(new ItemStack(Items.DIRT, 3));
        assertSame(drag, get(screen, "dragDistribution"));
        List<?> targets = (List<?>) invoke(screen, "previewDragTargets", drag);
        assertEquals(3, targets.size());
        assertEquals(0, invoke(screen, "dragPreviewRemaining", drag, targets));
    }

    @Test
    void emptyCursorCancelsPlacementAndDrag() throws Exception {
        InventoryDesktopScreen screen = screen(2);
        press(screen, hit(new TestMenu(1), 0));
        set(screen, "dragDistribution", nested("DragDistribution", leftButton(), AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE));
        screen.setSharedCarried(ItemStack.EMPTY);
        assertNull(get(screen, "pendingSlotClick"));
        assertNull(get(screen, "dragDistribution"));
    }

    @Test
    void aDifferentItemCancelsTheOldPlacement() throws Exception {
        InventoryDesktopScreen screen = screen(2);
        press(screen, hit(new TestMenu(1), 0));
        screen.setSharedCarried(new ItemStack(Items.SAND, 2));
        assertNull(get(screen, "pendingSlotClick"));
    }

    @Test
    void changedItemMetadataCancelsTheOldPlacement() throws Exception {
        InventoryDesktopScreen screen = screen(2);
        press(screen, hit(new TestMenu(1), 0));
        ItemStack renamed = new ItemStack(Items.DIRT, 2);
        var name = net.minecraft.network.chat.Component.literal("Different cursor item");
        try {
            ItemStack.class.getMethod("setHoverName", net.minecraft.network.chat.Component.class).invoke(renamed, name);
        } catch (NoSuchMethodException ignored) {
            Class<?> type = Class.forName("net.minecraft.core.component.DataComponentType");
            Object key = Class.forName("net.minecraft.core.component.DataComponents").getField("CUSTOM_NAME").get(null);
            ItemStack.class.getMethod("set", type, Object.class).invoke(renamed, key, name);
        }
        screen.setSharedCarried(renamed);
        assertNull(get(screen, "pendingSlotClick"));
    }

    @Test
    void cloneDragCanStillFillMoreSlotsThanTheCursorCount() throws Exception {
        InventoryDesktopScreen screen = screen(1);
        TestMenu menu = new TestMenu(5);
        Object drag = nested("DragDistribution", leftButton(), AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE);
        for (int i = 0; i < 5; i++) invoke(drag, "add", hit(menu, i));
        List<?> targets = (List<?>) invoke(screen, "previewDragTargets", drag);
        assertEquals(5, targets.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void movementInsideTheStartingSlotStaysAnOrdinaryClick(int buttonIndex) throws Exception {
        InventoryDesktopScreen screen = screen(32);
        List<?> hits = window(screen, new TestMenu(2), 5, 10);
        Object start = hits.get(0);
        press(screen, start, mouseButton(buttonIndex));
        Object pending = get(screen, "pendingSlotClick");

        for (double offset : new double[]{2.0D, 8.0D, 15.0D}) {
            hover(screen, start, offset);
            assertSame(pending, get(screen, "pendingSlotClick"));
            assertNull(get(screen, "dragDistribution"), "Motion within one slot must not start distribution");
            assertNull(invoke(screen, "activeDragPreview"));
            assertNull(invoke(screen, "dragSlotPreview", invoke(start, "slot")));
        }

        invoke(screen, "completePendingSlotClick");
        assertEquals(2, get(screen, "nextDebugClickId"), "Release must dispatch the ordinary placement click");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void enteringASecondSlotStartsDragAndReturningDoesNotCancelIt(int buttonIndex) throws Exception {
        InventoryDesktopScreen screen = screen(32);
        List<?> hits = window(screen, new TestMenu(2), 5, 10);
        press(screen, hits.get(0), mouseButton(buttonIndex));

        hover(screen, hits.get(1), 2.0D);
        Object drag = get(screen, "dragDistribution");
        assertNotNull(drag);
        assertEquals(2, invoke(drag, "size"));
        assertSame(drag, invoke(screen, "activeDragPreview"));
        List<?> targets = (List<?>) invoke(screen, "previewDragTargets", drag);
        assertEquals(buttonIndex == 0 ? 0 : 30, invoke(screen, "dragPreviewRemaining", drag, targets));

        hover(screen, hits.get(0), 8.0D);
        assertSame(drag, get(screen, "dragDistribution"));
        assertEquals(2, invoke(drag, "size"), "Revisiting a slot must not duplicate its target");
    }

    @Test
    void leavingTheSlotAndReturningWithoutAnotherTargetStaysAClick() throws Exception {
        InventoryDesktopScreen screen = screen(32);
        Object start = window(screen, new TestMenu(2), 5, 10).get(0);
        press(screen, start);

        // Window padding is outside every slot, but still inside the window.
        invoke(screen, "updateDragDistribution", 12.0D, (double) (int) invoke(start, "y") + 2.0D);
        assertNull(get(screen, "dragDistribution"));
        hover(screen, start, 8.0D);
        assertNull(get(screen, "dragDistribution"));
        invoke(screen, "completePendingSlotClick");
        assertEquals(2, get(screen, "nextDebugClickId"));
    }

    @Test
    void anIncompatibleSecondSlotDoesNotStartDrag() throws Exception {
        InventoryDesktopScreen screen = screen(32);
        TestMenu menu = new TestMenu(2);
        menu.slots.get(1).set(new ItemStack(Items.SAND, 1));
        List<?> hits = window(screen, menu, 5, 10);
        press(screen, hits.get(0));

        hover(screen, hits.get(1), 2.0D);
        assertNull(get(screen, "dragDistribution"));
        hover(screen, hits.get(0), 2.0D);
        assertNull(get(screen, "dragDistribution"));
        invoke(screen, "completePendingSlotClick");
        assertEquals(2, get(screen, "nextDebugClickId"));
    }

    @Test
    void anUnacceptedSecondTargetDoesNotReplaceThePendingClick() throws Exception {
        InventoryDesktopScreen screen = screen(1);
        List<?> hits = window(screen, new TestMenu(2), 5, 10);
        press(screen, hits.get(0));

        hover(screen, hits.get(1), 2.0D);
        assertNull(get(screen, "dragDistribution"), "One carried item cannot be distributed to two slots");
        assertNull(invoke(screen, "activeDragPreview"));
        invoke(screen, "completePendingSlotClick");
        assertEquals(2, get(screen, "nextDebugClickId"));
    }

    @Test
    void equalSlotIndexesInDifferentWindowsAreDistinctTargets() throws Exception {
        InventoryDesktopScreen screen = screen(32);
        Object first = window(screen, new TestMenu(1), 5, 10).get(0);
        Object second = window(screen, new TestMenu(1), 6, 100).get(0);
        press(screen, first);

        hover(screen, second, 2.0D);
        Object drag = get(screen, "dragDistribution");
        assertNotNull(drag);
        assertEquals(2, invoke(drag, "size"));
    }

    private static List<?> window(InventoryDesktopScreen screen, TestMenu menu, int sessionId, int x) throws Exception {
        DesktopContainerSession session = (DesktopContainerSession) unsafe.allocateInstance(DesktopContainerSession.class);
        set(session, "menu", menu);
        set(session, "sessionId", sessionId);
        set(session, "containerSlots", List.copyOf(menu.slots));
        Class<?> windowClass = Class.forName(InventoryDesktopScreen.class.getName() + "$InventoryWindow");
        Object window = unsafe.allocateInstance(windowClass);
        Class<?> kindClass = Class.forName(InventoryDesktopScreen.class.getName() + "$WindowKind");
        Object containerKind = java.util.Arrays.stream(kindClass.getEnumConstants())
            .filter(kind -> ((Enum<?>) kind).name().equals("CONTAINER")).findFirst().orElseThrow();
        set(window, "kind", containerKind);
        set(window, "session", session);
        set(window, "x", x);
        set(window, "y", 10);
        set(window, "width", menu.slots.size() * InventoryDesktopScreen.SLOT_SIZE + 16);
        set(window, "height", 50);
        @SuppressWarnings("unchecked")
        List<Object> windows = (List<Object>) get(screen, "windows");
        windows.add(window);

        List<Object> hits = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Object hit = invoke(screen, "slotAt", (double) (int) invoke(window, "contentX") + i * InventoryDesktopScreen.SLOT_SIZE + 2.0D,
                (double) (int) invoke(window, "contentY") + 2.0D);
            assertNotNull(hit);
            assertSame(menu.slots.get(i), invoke(hit, "slot"));
            hits.add(hit);
        }
        return hits;
    }

    private static void hover(InventoryDesktopScreen screen, Object hit, double offset) throws Exception {
        invoke(screen, "updateDragDistribution", (double) (int) invoke(hit, "x") + offset, (double) (int) invoke(hit, "y") + offset);
    }

    private static InventoryDesktopScreen screen(int count) throws Exception {
        InventoryDesktopScreen screen = (InventoryDesktopScreen) unsafe.allocateInstance(InventoryDesktopScreen.class);
        set(screen, "windows", new ArrayList<>());
        set(screen, "sessions", new ArrayList<>());
        set(screen, "sharedCarried", new ItemStack(Items.DIRT, count));
        set(screen, "nextDebugClickId", 1);
        return screen;
    }

    private static void press(InventoryDesktopScreen screen, Object hit) throws Exception {
        press(screen, hit, leftButton());
    }

    private static void press(InventoryDesktopScreen screen, Object hit, int buttonId) throws Exception {
        Method method = java.util.Arrays.stream(InventoryDesktopScreen.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("beginPendingSlotClick")).findFirst().orElseThrow();
        Class<?> eventType = method.getParameterTypes()[1];
        Constructor<?> constructor = eventType.getDeclaredConstructors()[0];
        Class<?> buttonType = constructor.getParameterTypes()[2];
        Object button = buttonType == int.class ? buttonId
            : buttonType.getDeclaredConstructor(int.class, int.class).newInstance(buttonId, 0);
        Object event = constructor.newInstance(0.0D, 0.0D, button);
        assertEquals(true, invoke(screen, "beginPendingSlotClick", hit, event));
    }

    private static int leftButton() throws Exception {
        return mouseButton(0);
    }

    private static int mouseButton(int buttonIndex) throws Exception {
        try {
            return InputConstants.class.getField(buttonIndex == 0 ? "MOUSE_BUTTON_LEFT" : "MOUSE_BUTTON_RIGHT").getInt(null);
        } catch (NoSuchFieldException ignored) {
            return buttonIndex;
        }
    }

    private static Object hit(TestMenu menu, int index) throws Exception {
        return nested("SlotHit", menu.slots.get(index), index, 0, 0, menu, 5);
    }

    private static Object nested(String name, Object... args) throws Exception {
        Class<?> type = Class.forName(InventoryDesktopScreen.class.getName() + "$" + name);
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method method = java.util.Arrays.stream(target.getClass().getDeclaredMethods())
            .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length).findFirst().orElseThrow();
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        field(target, name).set(target, value);
    }

    private static Object get(Object target, String name) throws Exception {
        return field(target, name).get(target);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu(int slots) {
            super(null, 5);
            SimpleContainer container = new SimpleContainer(slots);
            for (int i = 0; i < slots; i++) addSlot(new Slot(container, i, i * InventoryDesktopScreen.SLOT_SIZE, 0));
        }

        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
