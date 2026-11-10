package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets={"com.aetherteam.protect_your_moa.inventory.menu.MoaInventoryMenu","tallestegg.guardvillagers.entities.GuardContainer","baguchan.revampedwolf.inventory.WolfInventoryMenu","com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu"},remap=false)
public abstract class ReviewedEntityMenuTypeMixin extends AbstractContainerMenu {
    protected ReviewedEntityMenuTypeMixin(MenuType<?> type,int id){super(type,id);}
    @Override public MenuType<?> getType(){return ReviewedEntityInventories.typeFor(getClass().getName());}
}
