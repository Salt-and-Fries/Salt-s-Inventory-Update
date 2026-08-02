package com.salts_inventory_update.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.PlayerExtraInventory;

@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeInventoryExpansionMixin {
    @Shadow
    protected Inventory inventory;

    @Shadow
    protected RecipeBookMenu<?, ?> menu;

    @Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$moveExpansionItemToGrid(
        Slot target,
        ItemStack template,
        int requested,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!InventoryExpansion.isGameplayActive(this.inventory.player) || this.inventory.findSlotMatchingUnusedItem(template) != -1) {
            return;
        }

        PlayerExtraInventory extra = InventoryExpansion.access(this.inventory.player).salts_inventory_update$getExtraInventory();
        int extraSlot = extra.findSlotMatchingUnusedItem(template);
        if (extraSlot < 0) {
            cir.setReturnValue(-1);
            return;
        }

        ItemStack source = extra.getItem(extraSlot);
        ItemStack taken = requested < source.getCount()
            ? extra.removeItem(extraSlot, requested)
            : extra.removeItemNoUpdate(extraSlot);
        int takenCount = taken.getCount();
        if (target.getItem().isEmpty()) {
            target.set(taken);
        } else {
            target.getItem().grow(takenCount);
        }
        cir.setReturnValue(requested - takenCount);
    }

    @Inject(method = "testClearGrid", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$testCombinedClearCapacity(CallbackInfoReturnable<Boolean> cir) {
        if (!InventoryExpansion.isGameplayActive(this.inventory.player)
            || InventoryExpansion.access(this.inventory.player).salts_inventory_update$getExtraSlotCount() == 0) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; i++) {
            if (i != this.menu.getResultSlotIndex()) {
                ItemStack stack = this.menu.getSlot(i).getItem();
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        cir.setReturnValue(InventoryExpansion.canStowRecipeGrid(this.inventory.player, stacks));
    }
}
