package com.salts_inventory_update.compat.ae2.mixin;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.NetworkToolItem;
import appeng.util.inv.SupplierInternalInventory;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The fork's shared tool inventory identity, using native AE2 inventory implementations. */
@Pseudo
@Mixin(targets = "appeng.items.contents.NetworkToolMenuHost", remap = false)
public abstract class NetworkToolMenuHostMixin {
    @Unique
    private static final ConcurrentMap<ItemStack, InternalInventory> salts$inventories =
            new MapMaker().weakKeys().weakValues().makeMap();

    @Shadow @Final @Mutable private SupplierInternalInventory<InternalInventory> supplierInv;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void salts$shareExactToolInventory(CallbackInfo callback) {
        var host = (ItemMenuHost<?>) (Object) this;
        supplierInv = new SupplierInternalInventory<>(new StackDependentSupplier<>(host::getItemStack,
                stack -> salts$inventories.computeIfAbsent(stack, NetworkToolItem::getInventory)));
    }
}
