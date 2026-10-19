package com.salts_inventory_update.controlify.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Balances only virtual presses owned by the keyboard overlay. */
@Mixin(MouseHandler.class)
public interface WorkspaceMouseHandlerAccessor {
    @Invoker("onPress") void saltsControlify$releaseButton(long window,int button,int action,int modifiers);
}
