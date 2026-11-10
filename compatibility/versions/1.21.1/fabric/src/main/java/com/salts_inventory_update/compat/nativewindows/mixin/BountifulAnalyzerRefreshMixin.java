package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The publisher's slot callback checks Minecraft.screen, which is the desktop. */
@Pseudo
@Mixin(targets="io.ejekta.bountiful.client.AnalyzerScreen",remap=false)
public abstract class BountifulAnalyzerRefreshMixin {
    @Unique private ItemStack salts$analyzedDecree = ItemStack.EMPTY;
    @Shadow(remap=false) public abstract void refreshWidgets();

    @Inject(method="onDrawForeground",at=@At("HEAD"),remap=false)
    private void salts$refreshHostedDecree(CallbackInfo ci) {
        // Only the owning hidden native screen participates. Ordinary native
        // screens keep their original callback, and equal snapshots do no work.
        if (DesktopNativeRenderTarget.current().isEmpty()) return;
        var menu = ((AbstractContainerScreen<?>)(Object)this).getMenu();
        if (menu.slots.isEmpty()) return;
        ItemStack decree = menu.getSlot(0).getItem();
        if (ItemStack.matches(salts$analyzedDecree, decree)) return;
        refreshWidgets();
        salts$analyzedDecree = decree.copy();
    }
}
