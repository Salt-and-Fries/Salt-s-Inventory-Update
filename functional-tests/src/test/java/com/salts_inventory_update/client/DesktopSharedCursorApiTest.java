package com.salts_inventory_update.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Public cursor operations cannot be used with a forged or expired context.
 * Successful native action commits are exercised by the in-world terminal tests. */
final class DesktopSharedCursorApiTest {
    private static sun.misc.Unsafe unsafe;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe=(sun.misc.Unsafe)field.get(null);
    }

    private static DesktopServerSessionContext<?,?> inactiveContext() throws Exception {
        Class<?> type=Class.forName("com.salts_inventory_update.server.DesktopContainerSessions$ServerSessionContext");
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        return (DesktopServerSessionContext<?,?>)constructor.newInstance(unsafe.allocateInstance(ServerPlayer.class),null,null);
    }

    @Test void cursorReadRequiresALiveServerContext() throws Exception {
        var context=inactiveContext();
        assertThrows(IllegalStateException.class,context::carriedStack);
    }

    @Test void cursorCommitRequiresALiveServerContext() throws Exception {
        var context=inactiveContext();
        assertThrows(IllegalStateException.class,()->context.setCarriedStack(ItemStack.EMPTY));
    }
}
