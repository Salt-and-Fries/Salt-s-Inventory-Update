package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.kibe.KibeWindows;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kibe 2.x uses intermediary on Fabric and named inventoryTick on NeoForge.
 * Suppress only the original feeding tick while the real item menu is open;
 * Kibe owns feeding, hunger changes and NBT again as soon as it is closed. */
@Pseudo @Mixin(targets="io.github.lucaargolo.kibe.item.CoolerBlockItem",remap=false)
public abstract class KibeCoolerTickMixin {
    @Inject(method={"method_7888","inventoryTick"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$preserveOpenMenuGuard(ItemStack stack,Level level,Entity entity,int slot,boolean selected,CallbackInfo ci){
        if(entity instanceof Player player&&KibeWindows.coolerOpen(player))ci.cancel();
    }
}
