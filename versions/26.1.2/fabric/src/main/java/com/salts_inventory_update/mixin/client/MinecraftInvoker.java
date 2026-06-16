package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftInvoker {
    @Invoker("startAttack")
    boolean salts_inventory_update$startAttack();

    @Invoker("continueAttack")
    void salts_inventory_update$continueAttack(boolean attacking);

    @Invoker("startUseItem")
    void salts_inventory_update$startUseItem();
}
