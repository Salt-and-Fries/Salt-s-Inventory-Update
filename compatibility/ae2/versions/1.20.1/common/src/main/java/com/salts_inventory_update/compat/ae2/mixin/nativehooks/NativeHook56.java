package com.salts_inventory_update.compat.ae2.mixin.nativehooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
@Pseudo
@Mixin(targets = "appeng.menu.me.crafting.CraftAmountMenu", priority = 2000, remap = false)
public abstract class NativeHook56 { }
