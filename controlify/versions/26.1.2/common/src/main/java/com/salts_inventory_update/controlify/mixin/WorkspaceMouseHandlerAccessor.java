package com.salts_inventory_update.controlify.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Releases only the synthetic pointer presses owned by the keyboard overlay. */
@Mixin(MouseHandler.class)
public interface WorkspaceMouseHandlerAccessor {
    @Invoker("onButton") void saltsControlify$onButton(long window, MouseButtonInfo button, int action);
}
