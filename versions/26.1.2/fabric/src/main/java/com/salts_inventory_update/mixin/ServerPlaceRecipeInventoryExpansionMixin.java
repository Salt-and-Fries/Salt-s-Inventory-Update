package com.salts_inventory_update.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.PlayerExtraInventory;

@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeInventoryExpansionMixin {
    @Shadow @Final private Inventory inventory;
    @Shadow @Final private List<Slot> inputGridSlots;

    @Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$moveExpansionItemToGrid(
        Slot target,
        Holder<Item> item,
        int requested,
        CallbackInfoReturnable<Integer> cir
    ) {
        ItemStack existing = target.getItem();
        if (!InventoryExpansion.isGameplayActive(this.inventory.player)
            || this.inventory.findSlotMatchingCraftingIngredient(item, existing) != Inventory.NOT_FOUND_INDEX) {
            return;
        }
        PlayerExtraInventory extra = InventoryExpansion.access(this.inventory.player).salts_inventory_update$getExtraInventory();
        int extraSlot = extra.findSlotMatchingCraftingIngredient(item, existing);
        if (extraSlot < 0) {
            cir.setReturnValue(-1);
            return;
        }
        ItemStack source = extra.getItem(extraSlot);
        ItemStack taken = requested < source.getCount()
            ? extra.removeItem(extraSlot, requested)
            : extra.removeItemNoUpdate(extraSlot);
        int takenCount = taken.getCount();
        if (existing.isEmpty()) {
            target.set(taken);
        } else {
            existing.grow(takenCount);
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
        for (Slot slot : this.inputGridSlots) {
            if (slot.hasItem()) {
                stacks.add(slot.getItem());
            }
        }
        cir.setReturnValue(InventoryExpansion.canStowRecipeGrid(this.inventory.player, stacks));
    }
}

