package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create 6.0.8's request factory returns CategoryMenu on the server (37 cells)
 * while its client factory returns RequestMenu (36). The normal client ignores
 * that mismatched snapshot. A detached session requires the actual request menu
 * on both ends; its own constructor retains the native holder and permissions. */
@Pseudo
@Mixin(targets="com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu",remap=false)
public abstract class CreateStockRequestFactoryMixin {
    @Inject(method="create",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$matchingRequestMenu(int id,Inventory inventory,@Coerce Object holder,CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if(!(inventory.player instanceof ServerPlayer player)||!DesktopSessions.shouldCapture(player))return;
        try {
            var type=BuiltInRegistries.MENU.get(ResourceLocation.fromNamespaceAndPath("create","stock_keeper_request"));
            var menu=(AbstractContainerMenu)Class.forName("com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu")
                .getConstructor(MenuType.class,int.class,Inventory.class,holder.getClass()).newInstance(type,id,inventory,holder);
            cir.setReturnValue(menu);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Create request menu factory",failure);}
    }
}
