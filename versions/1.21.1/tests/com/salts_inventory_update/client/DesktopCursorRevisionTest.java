package com.salts_inventory_update.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DesktopCursorRevisionTest {
    private static sun.misc.Unsafe unsafe;

    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }

    @Test
    void acknowledgesNativeCursorRevisionsWithoutChangingSlots() throws Exception {
        checkCursorRevision(0, 2);
        checkCursorRevision(32766, 0);
    }

    private static void checkCursorRevision(int initialRevision, int expectedRevision) throws Exception {
        InventoryMenu serverMenu = menu(), clientMenu = menu();
        serverMenu.setItem(9, initialRevision, ItemStack.EMPTY);
        clientMenu.setItem(9, initialRevision, new ItemStack(Items.SAND, 7));
        ServerPlayer server = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        RecordingListener recorder = (RecordingListener) unsafe.allocateInstance(RecordingListener.class);
        server.connection = recorder;
        Constructor<?> constructor = Class.forName("net.minecraft.server.level.ServerPlayer$1")
            .getDeclaredConstructor(ServerPlayer.class);
        constructor.setAccessible(true);
        ContainerSynchronizer synchronizer = (ContainerSynchronizer) constructor.newInstance(server);

        HeadlessMinecraft minecraft = (HeadlessMinecraft) unsafe.allocateInstance(HeadlessMinecraft.class);
        minecraft.tutorial = (QuietTutorial) unsafe.allocateInstance(QuietTutorial.class);
        LocalPlayer player = (LocalPlayer) unsafe.allocateInstance(LocalPlayer.class);
        set(player, "inventoryMenu", clientMenu);
        player.containerMenu = clientMenu;
        minecraft.player = player;
        ClientPacketListener receiver = (ClientPacketListener) unsafe.allocateInstance(ClientPacketListener.class);
        set(receiver, "minecraft", minecraft);

        synchronizer.sendSlotChange(serverMenu, 10, ItemStack.EMPTY);
        receiver.handleContainerSetSlot((ClientboundContainerSetSlotPacket) recorder.last);
        synchronizer.sendCarriedChange(serverMenu, new ItemStack(Items.DIRT, 32));
        receiver.handleContainerSetSlot((ClientboundContainerSetSlotPacket) recorder.last);
        assertNotEquals(serverMenu.getStateId(), clientMenu.getStateId(), "Vanilla does not acknowledge the cursor revision");

        DesktopContainerClient.applyCarriedUpdate(minecraft, serverMenu.getStateId(), new ItemStack(Items.DIRT, 32));
        assertEquals(expectedRevision, clientMenu.getStateId());
        assertEquals(serverMenu.getStateId(), clientMenu.getStateId());
        assertEquals(7, clientMenu.getSlot(9).getItem().getCount());
        assertEquals(Items.SAND, clientMenu.getSlot(9).getItem().getItem());
        assertEquals(32, clientMenu.getCarried().getCount());

        // A later right-click changes only the cursor count; that revision must converge too.
        synchronizer.sendCarriedChange(serverMenu, new ItemStack(Items.DIRT, 31));
        receiver.handleContainerSetSlot((ClientboundContainerSetSlotPacket) recorder.last);
        DesktopContainerClient.applyCarriedUpdate(minecraft, serverMenu.getStateId(), new ItemStack(Items.DIRT, 31));
        assertEquals(serverMenu.getStateId(), clientMenu.getStateId());
        assertEquals(31, clientMenu.getCarried().getCount());
        assertEquals(7, clientMenu.getSlot(9).getItem().getCount());
    }

    private static InventoryMenu menu() throws Exception {
        InventoryMenu menu = (InventoryMenu) unsafe.allocateInstance(InventoryMenu.class);
        NonNullList<Slot> slots = NonNullList.create();
        SimpleContainer container = new SimpleContainer(46);
        for (int i = 0; i < 46; i++) slots.add(new Slot(container, i, 0, 0));
        set(menu, "slots", slots);
        menu.setCarried(ItemStack.EMPTY);
        return menu;
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

    private static final class RecordingListener extends ServerGamePacketListenerImpl {
        private Packet<?> last;
        RecordingListener() { super(null, null, null, null); }
        @Override public void send(Packet<?> packet) { this.last = packet; }
    }

    private static final class QuietTutorial extends Tutorial {
        QuietTutorial() { super(null, null); }
        @Override public void onGetItem(ItemStack stack) { }
    }

    private static final class HeadlessMinecraft extends Minecraft {
        private Tutorial tutorial;
        HeadlessMinecraft() { super(null); }
        @Override public boolean isSameThread() { return true; }
        @Override public Tutorial getTutorial() { return this.tutorial; }
    }
}
