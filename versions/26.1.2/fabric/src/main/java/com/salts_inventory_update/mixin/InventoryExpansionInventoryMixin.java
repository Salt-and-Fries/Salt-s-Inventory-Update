package com.salts_inventory_update.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.PlayerExtraInventory;

@Mixin(Inventory.class)
public abstract class InventoryExpansionInventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$addToExpansion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && InventoryExpansion.insertIntoExtra(this.player, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$addToExpansion(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && InventoryExpansion.insertIntoExtra(this.player, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "replaceWith", at = @At("RETURN"))
    private void salts_inventory_update$replaceExpansionInventory(Inventory other, CallbackInfo ci) {
        var source = InventoryExpansion.access(other.player);
        var target = InventoryExpansion.access(this.player);
        target.salts_inventory_update$setExtraSlotCount(source.salts_inventory_update$getExtraSlotCount());
        target.salts_inventory_update$getExtraInventory().copyFrom(source.salts_inventory_update$getExtraInventory(), true);
        InventoryExpansion.appendMissingMenuSlots(this.player.inventoryMenu, this.player);
    }

    @Redirect(
        method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private ItemEntity salts_inventory_update$insertExpansionBeforeDrop(Player player, ItemStack stack, boolean randomly) {
        InventoryExpansion.insertIntoExtra(player, stack);
        return stack.isEmpty() ? null : player.drop(stack, randomly);
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void salts_inventory_update$dropExpansionInventory(CallbackInfo ci) {
        InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().dropAll();
    }

    @Inject(method = "clearContent", at = @At("RETURN"))
    private void salts_inventory_update$clearExpansionInventory(CallbackInfo ci) {
        InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().clearContent();
    }

    @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$isExpansionInventoryEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()
            && !InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$containsExpansionStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryExpansion.isGameplayActive(this.player) && !cir.getReturnValueZ()) {
            PlayerExtraInventory extraInventory = InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory();
            cir.setReturnValue(extraInventory.hasAnyMatching(extraStack -> ItemStack.isSameItemSameComponents(extraStack, stack)));
        }
    }

    @Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$containsExpansionTag(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryExpansion.isGameplayActive(this.player) && !cir.getReturnValueZ()) {
            PlayerExtraInventory extraInventory = InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory();
            cir.setReturnValue(extraInventory.hasAnyMatching(stack -> stack.is(tag)));
        }
    }

    @Inject(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$containsExpansionPredicate(Predicate<ItemStack> predicate, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryExpansion.isGameplayActive(this.player) && !cir.getReturnValueZ()) {
            cir.setReturnValue(InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().hasAnyMatching(predicate));
        }
    }

    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void salts_inventory_update$clearOrCountExpansionItems(
        Predicate<ItemStack> predicate,
        int maxCount,
        Container craftingInventory,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!InventoryExpansion.isGameplayActive(this.player)) {
            return;
        }
        int vanillaCount = cir.getReturnValueI();
        boolean unlimited = maxCount < 0;
        int remaining = maxCount == 0 ? 0 : unlimited ? -1 : maxCount - vanillaCount;
        if (maxCount == 0 || unlimited || remaining > 0) {
            int extraCount = InventoryExpansion.access(this.player)
                .salts_inventory_update$getExtraInventory()
                .clearOrCountMatchingItems(predicate, remaining, maxCount == 0);
            cir.setReturnValue(vanillaCount + extraCount);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void salts_inventory_update$tickExpansionInventory(CallbackInfo ci) {
        if (InventoryExpansion.isGameplayActive(this.player)) {
            InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory().tick();
        }
    }

    @Inject(method = "fillStackedContents", at = @At("RETURN"))
    private void salts_inventory_update$accountExpansionRecipeItems(StackedItemContents contents, CallbackInfo ci) {
        if (!InventoryExpansion.isGameplayActive(this.player)) {
            return;
        }
        PlayerExtraInventory extraInventory = InventoryExpansion.access(this.player).salts_inventory_update$getExtraInventory();
        for (int slot = 0; slot < extraInventory.getContainerSize(); slot++) {
            contents.accountSimpleStack(extraInventory.getItem(slot));
        }
    }

}
