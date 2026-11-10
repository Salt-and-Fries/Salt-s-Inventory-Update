package com.salts_inventory_update.server;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The actual native synchronizer must not expose a half-committed cursor. */
final class NativeCursorPublicationTest {
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<AbstractContainerMenu> active() throws Exception {
        Field field=DesktopContainerSessions.class.getDeclaredField("CURSOR_MUTATION_MENU");
        field.setAccessible(true);return (ThreadLocal<AbstractContainerMenu>)field.get(null);
    }

    private static ContainerSynchronizer withoutPublicationTransport() throws Exception {
        Class<?> type=Class.forName(DesktopContainerSessions.class.getName()+"$SessionSynchronizer");
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        // Deliberately no player/transport: the in-flight path must return before
        // touching them. The ordinary path is checked separately below.
        return (ContainerSynchronizer)constructor.newInstance(null,null);
    }

    @Test void nativeBroadcastInsideClickedCannotPublishTheTemporaryCursor() throws Exception {
        var menu=new Menu();var stack=new ItemStack(Items.DIRT,7);menu.setCarried(stack);
        Field ownersField=DesktopContainerSessions.class.getDeclaredField("PLAYERS");ownersField.setAccessible(true);
        @SuppressWarnings("unchecked") var owners=(Map<Object,Object>)ownersField.get(null);
        var constructor=Class.forName(DesktopContainerSessions.class.getName()+"$PlayerSessions").getDeclaredConstructor();
        constructor.setAccessible(true);Object previous=owners.put(null,constructor.newInstance());
        active().set(menu);
        try {
            assertDoesNotThrow(()->withoutPublicationTransport().sendCarriedChange(menu,stack));
            assertSame(stack,menu.getCarried());assertEquals(7,menu.getCarried().getCount());
        } finally { active().remove();if(previous==null)owners.remove(null);else owners.put(null,previous); }
    }

    @Test void unrelatedOrCommittedMenuStillUsesItsNormalPublicationPath() throws Exception {
        var menu=new Menu();active().set(new Menu());
        try {
            assertThrows(NullPointerException.class,()->withoutPublicationTransport().sendCarriedChange(menu,ItemStack.EMPTY));
        } finally { active().remove(); }
        assertThrows(NullPointerException.class,()->withoutPublicationTransport().sendCarriedChange(menu,ItemStack.EMPTY));
    }

    private static final class Menu extends AbstractContainerMenu {
        Menu(){super(null,1);}
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(Player player){return true;}
    }
}
