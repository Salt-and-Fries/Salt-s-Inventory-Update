package com.salts_inventory_update.mixin.server;

import com.salts_inventory_update.internal.desktop.FurnaceCookingCounter;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes native completion without changing fuel, progress, recipes or XP bookkeeping. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceCookingCounterMixin implements FurnaceCookingCounter {
    @Unique private long salts_inventory_update$completedCooking;

    @Inject(method = "setRecipeUsed", at = @At("HEAD"))
    private void salts_inventory_update$observeCooking(RecipeHolder<?> recipe, CallbackInfo ci) {
        salts_inventory_update$completedCooking++;
    }

    @Override public long salts_inventory_update$completedCookingOperations() {
        return salts_inventory_update$completedCooking;
    }
}
