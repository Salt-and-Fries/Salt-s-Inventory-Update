package com.salts_inventory_update.server;

import java.lang.reflect.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native menus can synchronously broadcast from inside their clicked callback. */
class NativeCursorBroadcastTest {
    static Method mutate;
    static ThreadLocal<AbstractContainerMenu> active;
    static sun.misc.Unsafe unsafe;
    @BeforeAll @SuppressWarnings("unchecked") static void setup() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);unsafe=(sun.misc.Unsafe)field.get(null);
        mutate=DesktopContainerSessions.class.getDeclaredMethod("withCursorMutation",AbstractContainerMenu.class,Runnable.class);mutate.setAccessible(true);
        var context=DesktopContainerSessions.class.getDeclaredField("CURSOR_MUTATION_MENU");context.setAccessible(true);active=(ThreadLocal<AbstractContainerMenu>)context.get(null);
    }
    @AfterEach void clear(){active.remove();}
    static void run(AbstractContainerMenu menu,Runnable action) {
        try{mutate.invoke(null,menu,action);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException r)throw r;if(e.getCause() instanceof Error fatal)throw fatal;throw new IllegalStateException(e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    @Test void broadcastDuringClickDoesNotEraseTheNativeResult() throws Exception {
        var player=(ServerPlayer)unsafe.allocateInstance(ServerPlayer.class);
        var ctor=Class.forName(DesktopContainerSessions.class.getName()+"$SessionSynchronizer").getDeclaredConstructors()[0];ctor.setAccessible(true);
        var synchronizer=(ContainerSynchronizer)ctor.newInstance(player,null);
        var menu=new Menu();menu.setCarried(new ItemStack(Items.DIRT,3));
        run(menu,()->synchronizer.sendCarriedChange(menu,menu.getCarried()));
        assertEquals(3,menu.getCarried().getCount());assertEquals(Items.DIRT,menu.getCarried().getItem());
        assertNull(active.get());
    }
    @Test void nestedAndFailedOperationsRestoreTheirPreviousScope() {
        var outer=new Menu();var inner=new Menu();
        run(outer,()->{
            assertSame(outer,active.get());
            run(inner,()->assertSame(inner,active.get()));assertSame(outer,active.get());
            assertThrows(IllegalStateException.class,()->run(inner,()->{throw new IllegalStateException("native callback failed");}));
            assertSame(outer,active.get());
        });
        assertNull(active.get());
    }
    @Test void publicNativeMenuObservationIsScopedAndPlayerSpecific() throws Exception {
        var player=(ServerPlayer)unsafe.allocateInstance(ServerPlayer.class);
        var other=(ServerPlayer)unsafe.allocateInstance(ServerPlayer.class);
        var menu=new Menu();
        Class<?> sessionType=Class.forName(DesktopContainerSessions.class.getName()+"$Session");
        Object session=unsafe.allocateInstance(sessionType);
        var field=sessionType.getDeclaredField("menu");field.setAccessible(true);field.set(session,menu);
        var scoped=DesktopContainerSessions.class.getDeclaredMethod("withNetworkSession",ServerPlayer.class,sessionType,Runnable.class);scoped.setAccessible(true);
        assertTrue(com.salts_inventory_update.api.server.desktop.DesktopSessions.activeNetworkMenu(player).isEmpty());
        scoped.invoke(null,player,session,(Runnable)()->{
            assertSame(menu,com.salts_inventory_update.api.server.desktop.DesktopSessions.activeNetworkMenu(player).orElseThrow());
            assertTrue(com.salts_inventory_update.api.server.desktop.DesktopSessions.activeNetworkMenu(other).isEmpty());
        });
        assertTrue(com.salts_inventory_update.api.server.desktop.DesktopSessions.activeNetworkMenu(player).isEmpty());
    }
    static final class Menu extends AbstractContainerMenu {
        Menu(){super(MenuType.GENERIC_9x1,1);}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    }
}
