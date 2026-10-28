package com.salts_inventory_update.mixin.client;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(net.minecraft.client.gui.render.state.GuiTextRenderState.class)
public interface AutomaticTextStateAccessor {
    @Accessor("font") net.minecraft.client.gui.Font salts_inventory_update$font();
    @Accessor("text") net.minecraft.util.FormattedCharSequence salts_inventory_update$text();
    @Accessor("x") int salts_inventory_update$x();
    @Accessor("y") int salts_inventory_update$y();
    @Accessor("color") int salts_inventory_update$color();
    @Accessor("backgroundColor") int salts_inventory_update$backgroundColor();
    @Accessor("dropShadow") boolean salts_inventory_update$dropShadow();
    @Accessor("includeEmpty") boolean salts_inventory_update$includeEmpty();
}
