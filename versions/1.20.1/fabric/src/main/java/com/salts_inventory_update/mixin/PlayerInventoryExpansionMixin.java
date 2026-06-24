package com.salts_inventory_update.mixin;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.InventoryExpansionAccess;
import com.salts_inventory_update.inventory.PlayerExtraInventory;
import com.salts_inventory_update.debug.DesktopDebug;

@Mixin(Player.class)
public abstract class PlayerInventoryExpansionMixin implements InventoryExpansionAccess {
    @Unique
    private static int salts_inventory_update$probeLogs;

    @Unique
    private PlayerExtraInventory salts_inventory_update$extraInventory;
    @Unique
    private int salts_inventory_update$extraSlotCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void salts_inventory_update$initializeExpansionInventory(Level level, BlockPos blockPos, float yRot, GameProfile gameProfile, CallbackInfo ci) {
        this.salts_inventory_update$extraInventory().resize(this.salts_inventory_update$extraSlotCount);
        if (salts_inventory_update$probeLogs < 6) {
            salts_inventory_update$probeLogs++;
            Player player = (Player) (Object) this;
            DesktopDebug.probe(
                "mixin PlayerInventoryExpansionMixin applied player={} class={} extraSlots={} inventoryCreated={} level={}",
                player.getName().getString(),
                player.getClass().getName(),
                this.salts_inventory_update$extraSlotCount,
                this.salts_inventory_update$extraInventory != null,
                level == null ? "null" : level.dimension().location()
            );
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void salts_inventory_update$readExpansionInventory(CompoundTag input, CallbackInfo ci) {
        InventoryExpansion.load((Player) (Object) this, input);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void salts_inventory_update$saveExpansionInventory(CompoundTag output, CallbackInfo ci) {
        InventoryExpansion.save((Player) (Object) this, output);
    }

    @Override
    public int salts_inventory_update$getExtraSlotCount() {
        return this.salts_inventory_update$extraSlotCount;
    }

    @Override
    public void salts_inventory_update$setExtraSlotCount(int slotCount) {
        this.salts_inventory_update$extraSlotCount = InventoryExpansion.clampSlotCount(slotCount);
        this.salts_inventory_update$extraInventory().resize(this.salts_inventory_update$extraSlotCount);
        Player player = (Player) (Object) this;
        if (player.inventoryMenu != null) {
            InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        }
    }

    @Override
    public PlayerExtraInventory salts_inventory_update$getExtraInventory() {
        return this.salts_inventory_update$extraInventory();
    }

    @Unique
    private PlayerExtraInventory salts_inventory_update$extraInventory() {
        if (this.salts_inventory_update$extraInventory == null) {
            this.salts_inventory_update$extraInventory = new PlayerExtraInventory((Player) (Object) this);
        }
        return this.salts_inventory_update$extraInventory;
    }
}
