package com.salts_inventory_update.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.PlayerExtraInventory;

@Mixin(Inventory.class)
public abstract class InventoryExpansionInventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$addToExpansion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !stack.isEmpty() && InventoryExpansion.insertIntoExtra(this.player, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$addToExpansion(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !stack.isEmpty() && InventoryExpansion.insertIntoExtra(this.player, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void salts_inventory_update$dropExpansionInventory(CallbackInfo ci) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().dropAll();
    }

    @Inject(method = "clearContent", at = @At("RETURN"))
    private void salts_inventory_update$clearExpansionInventory(CallbackInfo ci) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().clearContent();
    }

    @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$isExpansionInventoryEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        if (cir.getReturnValueZ() && !InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$containsExpansionStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        if (!cir.getReturnValueZ()) {
            PlayerExtraInventory extraInventory = InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory();
            cir.setReturnValue(extraInventory.hasAnyMatching(extraStack -> ItemStack.isSameItemSameTags(extraStack, stack)));
        }
    }

    @Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$containsExpansionTag(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        if (!cir.getReturnValueZ()) {
            PlayerExtraInventory extraInventory = InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory();
            cir.setReturnValue(extraInventory.hasAnyMatching(stack -> stack.is(tag)));
        }
    }

    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$clearOrCountExpansionItems(
        Predicate<ItemStack> predicate,
        int maxCount,
        Container craftingInventory,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }
        int vanillaCount = cir.getReturnValueI();
        int remaining = maxCount == 0 ? 0 : maxCount - vanillaCount;
        if (maxCount == 0 || remaining > 0) {
            int extraCount = InventoryExpansion.access(this.player)
                .salts_inventory_update$getExtraInventory()
                .clearOrCountMatchingItems(predicate, remaining, maxCount == 0);
            cir.setReturnValue(vanillaCount + extraCount);
        }
    }
}
