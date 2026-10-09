package com.salts_inventory_update.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("mouseGrabbed")
    void salts_inventory_update$setMouseGrabbed(boolean mouseGrabbed);

    @Accessor("xpos")
    void salts_inventory_update$setXpos(double xpos);

    @Accessor("ypos")
    void salts_inventory_update$setYpos(double ypos);
}
