package com.salts_inventory_update.controlify.mixin;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Balances synthetic clicks through the same native mouse state that created them. */
@Mixin(MouseHandler.class)
public interface WorkspaceMouseHandlerAccessor {
    @Invoker("onButton") void saltsControlify$releaseButton(long window, MouseButtonInfo button, int action);
}
