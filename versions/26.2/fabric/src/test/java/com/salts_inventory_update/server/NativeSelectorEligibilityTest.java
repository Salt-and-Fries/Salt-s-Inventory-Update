package com.salts_inventory_update.server;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NativeSelectorEligibilityTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    static final class Candidate extends AbstractContainerMenu {
        final boolean browser;
        Candidate(boolean browser){super(MenuType.GENERIC_9x6,1);this.browser=browser;}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
    }
    private static boolean eligible(Candidate menu)throws Exception{
        var sessions=Class.forName(DesktopContainerSessions.class.getName()+"$PlayerSessions");
        var ctor=sessions.getDeclaredConstructor();ctor.setAccessible(true);
        var method=DesktopContainerSessions.class.getDeclaredMethod("isDesktopSupportedMenu",ServerPlayer.class,sessions,AbstractContainerMenu.class);
        method.setAccessible(true);return (boolean)method.invoke(null,null,ctor.newInstance(),menu);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void withHandler(DesktopServerWindowHandler<AbstractContainerMenu,Void> handler,org.junit.jupiter.api.function.Executable test)throws Throwable{
        var field=DesktopServerApi.class.getDeclaredField("WINDOW_HANDLERS");field.setAccessible(true);
        var handlers=(Map<MenuType<?>,Object>)field.get(null);Object old=handlers.remove(MenuType.GENERIC_9x6);
        try{if(handler!=null)DesktopServerApi.registerWindow((MenuType)MenuType.GENERIC_9x6,handler);test.execute();}
        finally{handlers.remove(MenuType.GENERIC_9x6);if(old!=null)handlers.put(MenuType.GENERIC_9x6,old);}
    }
    @Test void ordinaryVanillaTypeNeedsNoCustomHandler()throws Throwable{
        withHandler(null,()->assertTrue(eligible(new Candidate(false))));
    }
    @Test void sameVanillaTypeCanKeepOnlyActionSelectorNative()throws Throwable{
        withHandler(new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){return !((Candidate)menu).browser;}
        },()->{assertFalse(eligible(new Candidate(true)));assertTrue(eligible(new Candidate(false)));});
    }
    @Test void throwingEligibilityCheckFailsClosedToNative()throws Throwable{
        withHandler(new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){throw new IllegalStateException("Unavailable native source");}
        },()->assertFalse(eligible(new Candidate(true))));
    }
}
