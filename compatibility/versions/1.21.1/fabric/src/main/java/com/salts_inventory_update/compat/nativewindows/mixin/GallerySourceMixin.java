package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.GalleryItemSources;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native menus do not retain their painting owner; only hosted instances need a detached owner. */
@Pseudo
@Mixin(targets="com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu",remap=false)
public abstract class GallerySourceMixin extends AbstractContainerMenu implements GalleryItemSources.Owner {
    @Unique private ItemStack salts_inventory_compat$paintingSource=ItemStack.EMPTY;
    @Unique private InteractionHand salts_inventory_compat$paintingHand;
    @Unique private int salts_inventory_compat$paintingSlot=-1;
    @Unique private int salts_inventory_compat$paintingCount;
    @Unique private boolean salts_inventory_compat$paintingHosted;
    protected GallerySourceMixin(MenuType<?> type,int id){super(type,id);}
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts_inventory_compat$capturePainting(int id,Inventory inventory,CallbackInfo ci) {
        var player=inventory.player;salts_inventory_compat$paintingHand=player.getUsedItemHand();
        salts_inventory_compat$paintingSlot=salts_inventory_compat$paintingHand==InteractionHand.OFF_HAND
            ?Inventory.SLOT_OFFHAND:inventory.selected;
        salts_inventory_compat$paintingSource=inventory.getItem(salts_inventory_compat$paintingSlot);
        salts_inventory_compat$paintingCount=salts_inventory_compat$paintingSource.getCount();
    }
    @Override public int desktopGallerySourceSlot(){return salts_inventory_compat$paintingSlot;}
    @Override public ItemStack desktopGallerySourceStack(){return salts_inventory_compat$paintingSource;}
    @Override public void desktopGallerySourceHosted(){salts_inventory_compat$paintingHosted=true;}
    @Unique private boolean salts_inventory_compat$paintingSourceValid(Player player) {
        return !salts_inventory_compat$paintingSource.isEmpty()
            &&player.getUsedItemHand()==salts_inventory_compat$paintingHand
            &&player.getItemInHand(salts_inventory_compat$paintingHand)==salts_inventory_compat$paintingSource
            &&player.getInventory().getItem(salts_inventory_compat$paintingSlot)==salts_inventory_compat$paintingSource
            &&salts_inventory_compat$paintingSource.getCount()==salts_inventory_compat$paintingCount;
    }
    @Inject(method={"stillValid","method_7597","m_6875_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void salts_inventory_compat$validPainting(Player player,CallbackInfoReturnable<Boolean> cir) {
        if(salts_inventory_compat$paintingHosted&&!salts_inventory_compat$paintingSourceValid(player))
            cir.setReturnValue(false);
    }
    @Inject(method={"removed","method_7595","m_6877_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$discardRetiredPainting(Player player,CallbackInfo ci) {
        if(salts_inventory_compat$paintingHosted&&!salts_inventory_compat$paintingSourceValid(player)) {
            super.removed(player);ci.cancel();
        }
    }
}
