package com.salts_inventory_update.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Shadow
    @Final
    private Player player;

    @Shadow
    private boolean placingRecipe;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void salts_inventory_update$syncDesktopCraftingResult(Container container, CallbackInfo ci) {
        if (!this.placingRecipe && this.player instanceof ServerPlayer serverPlayer) {
            DesktopContainerSessions.syncCraftingResult(serverPlayer, (CraftingMenu) (Object) this);
        }
    }

    @Inject(method = "finishPlacingRecipe", at = @At("RETURN"))
    private void salts_inventory_update$syncPlacedRecipeResult(ServerLevel level, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            DesktopContainerSessions.syncCraftingResult(serverPlayer, (CraftingMenu) (Object) this);
        }
    }
}
