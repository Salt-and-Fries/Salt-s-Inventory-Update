package com.salts_inventory_update.mixin.client;
import java.util.function.Consumer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GuiRenderState.class)
public interface AutomaticRenderStateAccessor {
    @Invoker("traverse") void salts_inventory_update$traverse(Consumer<Object> consumer,GuiRenderState.TraverseRange range);
}
