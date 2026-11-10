package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="reborncore.common.network.BlockPosPayload",remap=false)
public interface RebornBlockPosMixin {
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true,remap=false)
    default void salts$owner(ServerPlayer player,Predicate<AbstractContainerMenu> predicate,CallbackInfoReturnable<Boolean> ci){var allowed=TechRebornWindows.authorize(player,this,predicate);if(allowed!=null)ci.setReturnValue(allowed);}
}
