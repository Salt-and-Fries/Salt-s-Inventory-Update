package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real public registration and Core-owned lifecycle without a game client. */
final class DesktopEmbeddedSessionTest {
    private static sun.misc.Unsafe unsafe;
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(sun.misc.Unsafe)f.get(null);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static MenuType<TestMenu> type() throws Exception {
        MenuType<TestMenu> type=(MenuType<TestMenu>)unsafe.allocateInstance(MenuType.class);
        Field frozen=BuiltInRegistries.MENU.getClass().getDeclaredField("frozen");frozen.setAccessible(true);
        boolean old=frozen.getBoolean(BuiltInRegistries.MENU);frozen.setBoolean(BuiltInRegistries.MENU,false);
        try {Registry.register((Registry)BuiltInRegistries.MENU,"embedded_fixture:t"+UUID.randomUUID().toString().replace("-",""),type);}
        finally {frozen.setBoolean(BuiltInRegistries.MENU,old);}
        return type;
    }
    private static DesktopContainerSession session(TestMenu menu,int id,AtomicInteger released) throws Exception {
        var s=(DesktopContainerSession)unsafe.allocateInstance(DesktopContainerSession.class);
        set(s,"menu",menu);set(s,"sessionId",id);set(s,"sourceKey","embedded_fixture:"+id);
        set(s,"sourceLock",(DesktopItemSourceLocks.Lease)released::incrementAndGet);return s;
    }
    private static InventoryDesktopScreen screen(DesktopContainerSession... sessions) throws Exception {
        var s=(InventoryDesktopScreen)unsafe.allocateInstance(InventoryDesktopScreen.class);
        set(s,"sessions",new ArrayList<>(List.of(sessions)));return s;
    }
    private static void set(Object target,String name,Object value) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static boolean bind(InventoryDesktopScreen screen,DesktopContainerSession session) throws Exception {
        Method m=InventoryDesktopScreen.class.getDeclaredMethod("bindEmbeddedSession",DesktopContainerSession.class,DesktopContainerSession.class);
        m.setAccessible(true);return (boolean)m.invoke(screen,session,null);
    }
    @Test void registeredEmbeddedMenusAdvertiseTheirCapabilityWithoutAStandaloneRenderer() throws Exception {
        var type=type();DesktopClientApi.registerEmbeddedSession(type,s->true);
        assertTrue(DesktopClientApi.supportedMenuIds().contains(BuiltInRegistries.MENU.getKey(type).toString()));
        assertNotNull(DesktopClientApi.embeddedSessionHandler(new TestMenu(type)));
    }
    @Test void failedIntegrationRollsBackTheEmbeddedRegistrationAndCanRetry() throws Exception {
        var type=type();var menu=new TestMenu(type);
        assertThrows(IllegalStateException.class,()->DesktopRegistrationScope.run("embedded_fixture",()->{
            DesktopClientApi.registerEmbeddedSession(type,s->true);throw new IllegalStateException("fixture rollback");
        }));
        assertNull(DesktopClientApi.embeddedSessionHandler(menu));
        DesktopClientApi.registerEmbeddedSession(type,s->true);
        assertThrows(IllegalStateException.class,()->DesktopClientApi.registerEmbeddedSession(type,s->false));
    }
    @Test void acceptedSessionRetainsCoreOwnershipAndDisposesExactlyOnce() throws Exception {
        var type=type();AtomicInteger released=new AtomicInteger(),closed=new AtomicInteger();
        DesktopClientApi.registerEmbeddedSession(type,new DesktopEmbeddedSessionHandler<TestMenu>(){
            @Override public boolean opened(DesktopEmbeddedSession<TestMenu> s){return s.sessionId()==7&&s.isOpen();}
            @Override public void closed(int id){assertEquals(7,id);closed.incrementAndGet();}
        });
        var session=session(new TestMenu(type),7,released);var screen=screen(session);
        assertTrue(bind(screen,session));assertSame(session.menu(),session.embeddedView().menu());
        assertTrue(session.embeddedView().isOpen());session.close();session.close();
        assertFalse(session.embeddedView().isOpen());assertEquals(1,released.get());assertEquals(1,closed.get());
        session.embeddedView().close();
    }
    @Test void rejectedOwnerlessSessionCannotRetainItsSourceLease() throws Exception {
        var type=type();AtomicInteger released=new AtomicInteger();DesktopClientApi.registerEmbeddedSession(type,s->false);
        var session=session(new TestMenu(type),8,released);assertTrue(bind(screen(session),session));
        assertFalse(session.embeddedView().isOpen());assertEquals(1,released.get());
    }
    @Test void disposalCallbackFailureCannotLeakTheLeaseOrCallItTwice() throws Exception {
        var type=type();AtomicInteger released=new AtomicInteger(),closed=new AtomicInteger();
        DesktopClientApi.registerEmbeddedSession(type,new DesktopEmbeddedSessionHandler<TestMenu>(){
            @Override public boolean opened(DesktopEmbeddedSession<TestMenu> s){return true;}
            @Override public void closed(int id){closed.incrementAndGet();throw new IllegalStateException("fixture callback failure");}
        });
        var session=session(new TestMenu(type),9,released);assertDoesNotThrow(session::close);session.close();
        assertEquals(1,released.get());assertEquals(1,closed.get());
    }
    @Test void embeddedHitsCannotTargetPlayerOrOutsideSlots() {
        assertThrows(IllegalArgumentException.class,()->new DesktopSessionSlotHit(0,DesktopSlotHit.of(1,0,0)));
        assertThrows(IllegalArgumentException.class,()->new DesktopSessionSlotHit(1,DesktopSlotHit.of(-1,0,0)));
        assertThrows(NullPointerException.class,()->new DesktopSessionSlotHit(1,null));
    }
    @Test void geometryPolicyRollsBackAndDoesNotAffectAnUnrelatedOwner() throws Exception {
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID();Object id=identifier("geometry");
        Method register=DesktopClientApi.class.getMethod("registerGeometryResetPolicy",id.getClass(),java.util.function.Predicate.class);
        assertThrows(IllegalStateException.class,()->DesktopRegistrationScope.run("geometry_fixture",()->{
            try {register.invoke(null,id,(java.util.function.Predicate<UUID>)value->!owner.equals(value));}
            catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            assertFalse(DesktopClientApi.allowsGeometryReset(owner));assertTrue(DesktopClientApi.allowsGeometryReset(other));
            throw new IllegalStateException("fixture rollback");
        }));
        assertTrue(DesktopClientApi.allowsGeometryReset(owner));
    }
    private static Object identifier(String path) throws Exception {
        Class<?> cls;
        try {cls=Class.forName("net.minecraft.resources.Identifier");}
        catch(ClassNotFoundException legacy){cls=Class.forName("net.minecraft.resources.ResourceLocation");}
        try {return cls.getMethod("fromNamespaceAndPath",String.class,String.class).invoke(null,"embedded_fixture",path);}
        catch(NoSuchMethodException legacy){return cls.getConstructor(String.class,String.class).newInstance("embedded_fixture",path);}
    }
    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu(MenuType<?> type){super(type,1);}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
    }
}
