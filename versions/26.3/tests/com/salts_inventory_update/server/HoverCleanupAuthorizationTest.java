package com.salts_inventory_update.server;

import com.salts_inventory_update.network.DesktopPackets.DesktopSessionReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** The one-action exception must never become a general stale-state bypass. */
class HoverCleanupAuthorizationTest {
    private static sun.misc.Unsafe unsafe;
    private static ThreadLocal<Object> scope;
    private static Method allows;
    private ServerPlayer player;
    private Object sessions;
    private TestMenu menu;

    @BeforeAll @SuppressWarnings("unchecked") static void setupReflection() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
        unsafe = (sun.misc.Unsafe) access.get(null);
        Field context = DesktopContainerSessions.class.getDeclaredField("HOVER_CLEANUP_AUTHORIZATION"); context.setAccessible(true);
        scope = (ThreadLocal<Object>) context.get(null);
        allows = java.util.Arrays.stream(DesktopContainerSessions.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("authorizedHoverCleanupState")).findFirst().orElseThrow();
        allows.setAccessible(true);
    }
    @BeforeEach void setup() throws Exception {
        player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        sessions = construct("PlayerSessions"); menu = new TestMenu();
        menu.incrementStateId();
    }
    @AfterEach void cleanup() { scope.remove(); }

    private static Object construct(String nestedClass, Object... arguments) throws Exception {
        Class<?> type = Class.forName(DesktopContainerSessions.class.getName() + "$" + nestedClass);
        Constructor<?> constructor = java.util.Arrays.stream(type.getDeclaredConstructors())
            .filter(candidate -> candidate.getParameterCount() == arguments.length).findFirst().orElseThrow();
        constructor.setAccessible(true); return constructor.newInstance(arguments);
    }
    private void grant() throws Exception {
        Object grant = construct("HoverCleanupGrant", new DesktopSessionReference(4, 55L, 0), menu, menu.getStateId());
        scope.set(construct("HoverCleanupAuthorization", player, sessions, 99L, Map.of(4, grant)));
    }
    private boolean allowed(ServerPlayer requestPlayer, Object requestSessions, long nonce, int session, long token,
        int oldState, AbstractContainerMenu requestMenu) throws Exception {
        return (boolean) allows.invoke(null, requestPlayer, requestSessions, nonce, session, token, oldState, requestMenu);
    }

    @Test void exactPreauthorizedStateMayCrossOnlyCleanupPublication() throws Exception {
        grant(); assertTrue(allowed(player, sessions, 99L, 4, 55L, 0, menu));
    }
    @Test void genuinelyStaleOrUnscopedStateIsNeverAccepted() throws Exception {
        assertFalse(allowed(player, sessions, 99L, 4, 55L, 0, menu));
        grant(); assertFalse(allowed(player, sessions, 99L, 4, 55L, 2, menu));
    }
    @Test void changedPlayerSessionConnectionOrMenuCannotReuseGrant() throws Exception {
        grant();
        assertFalse(allowed((ServerPlayer) unsafe.allocateInstance(ServerPlayer.class), sessions, 99L, 4, 55L, 0, menu));
        assertFalse(allowed(player, construct("PlayerSessions"), 99L, 4, 55L, 0, menu));
        assertFalse(allowed(player, sessions, 100L, 4, 55L, 0, menu));
        assertFalse(allowed(player, sessions, 99L, 5, 55L, 0, menu));
        assertFalse(allowed(player, sessions, 99L, 4, 56L, 0, menu));
        TestMenu replacement = new TestMenu(); replacement.incrementStateId();
        assertFalse(allowed(player, sessions, 99L, 4, 55L, 0, replacement));
    }
    @Test void anotherPublicationInvalidatesExactCleanupState() throws Exception {
        grant(); menu.incrementStateId(); assertFalse(allowed(player, sessions, 99L, 4, 55L, 0, menu));
    }
    @Test void leavingScopeRestoresOrdinaryStaleRejection() throws Exception {
        grant(); scope.remove(); assertFalse(allowed(player, sessions, 99L, 4, 55L, 0, menu));
    }

    private static class TestMenu extends AbstractContainerMenu {
        TestMenu() { super(MenuType.GENERIC_9x3, 4); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
