package com.salts_inventory_update.server;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ScopedUseTargetTest {
    private static sun.misc.Unsafe unsafe;
    private ServerPlayer first,second;
    @BeforeAll static void bootstrap()throws Exception{
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);unsafe=(sun.misc.Unsafe)field.get(null);
    }
    private static ServerPlayer player()throws Exception{
        var p=(ServerPlayer)unsafe.allocateInstance(ServerPlayer.class);
        p.connection=(ServerGamePacketListenerImpl)unsafe.allocateInstance(ServerGamePacketListenerImpl.class);return p;
    }
    @BeforeEach void setup()throws Exception{first=player();second=player();}
    @SuppressWarnings("unchecked") private static Map<Object,String> targets()throws Exception{
        var field=DesktopContainerSessions.class.getDeclaredField("PENDING_USE_TARGETS");field.setAccessible(true);return (Map<Object,String>)field.get(null);
    }
    private static String pending(ServerPlayer p)throws Exception{
        var values=targets();return values.get(values.containsKey(p)?p:p.connection);
    }
    private static Object scoped(ServerPlayer p,String key,Supplier<?> action){
        try{var m=DesktopContainerSessions.class.getDeclaredMethod("withPendingUseTarget",ServerPlayer.class,String.class,Supplier.class);m.setAccessible(true);return m.invoke(null,p,key,action);}
        catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static String read(ServerPlayer p){try{return pending(p);}catch(Exception e){throw new AssertionError(e);}}
    @AfterEach void cleanup()throws Exception{for(var p:List.of(first,second)){targets().remove(p);targets().remove(p.connection);}}
    @Test void successfulCallbackCannotLeakItsBlockToTheNextItemMenu(){
        assertEquals("native-result",scoped(first,"block:first",()->{assertEquals("block:first",read(first));return "native-result";}));
        assertNull(read(first));
        scoped(first,null,()->{assertNull(read(first));return null;});
    }
    @Test void nativeExceptionAlwaysReleasesThePendingTarget(){
        var expected=new IllegalStateException("native opener failed");
        assertSame(expected,assertThrows(IllegalStateException.class,()->scoped(first,"block:first",()->{throw expected;})));
        assertNull(read(first));
    }
    @Test void nestedOpenRestoresTheOuterSourceAndThenClearsIt(){
        scoped(first,"block:outer",()->{scoped(first,"block:inner",()->{assertEquals("block:inner",read(first));return null;});assertEquals("block:outer",read(first));return null;});
        assertNull(read(first));
    }
    @Test void twoPlayersAndAnUncapturedNestedActionCannotBorrowEachOthersSource(){
        scoped(first,"block:first",()->{scoped(second,"block:second",()->{assertEquals("block:first",read(first));assertEquals("block:second",read(second));return null;});
            scoped(first,null,()->{assertNull(read(first));return null;});assertEquals("block:first",read(first));assertNull(read(second));return null;});
        assertNull(read(first));assertNull(read(second));
    }
}
