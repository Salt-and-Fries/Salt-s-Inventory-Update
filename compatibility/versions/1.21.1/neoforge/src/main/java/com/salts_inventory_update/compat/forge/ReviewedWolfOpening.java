package com.salts_inventory_update.compat.forge;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/** Capture only a wolf menu the upstream mod has actually authorized/opened. */
public final class ReviewedWolfOpening {
    private static final ThreadLocal<Boolean> OPENING=ThreadLocal.withInitial(()->false);
    public static void opened(PlayerContainerEvent.Open event) {
        var menu=event.getContainer();
        if(OPENING.get() || !(event.getEntity() instanceof ServerPlayer player)
                || player.containerMenu!=menu || !menu.getClass().getName().equals("baguchan.revampedwolf.inventory.WolfInventoryMenu")
                || !DesktopSessions.shouldCapture(player))return;
        final Wolf wolf;
        try{var field=menu.getClass().getDeclaredField("wolf");field.setAccessible(true);wolf=(Wolf)field.get(menu);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native wolf owner changed",failure);}
        if(!wolf.isAlive() || !wolf.isOwnedBy(player) || !menu.stillValid(player))return;
        var cargo=ReviewedEntityInventories.cargo(menu,"wolfContainer");
        OPENING.set(true);
        try {
            // Finish the original vanilla-screen lifetime before announcing the
            // detached one. The same authoritative entity container is reused.
            player.closeContainer();
            if(!ReviewedEntityInventories.open("wolf",wolf,player,cargo))
                ForeignMenuAccess.invoke(wolf,"openInventory",new Class<?>[]{Player.class},player);
        } finally { OPENING.remove(); }
    }
}
