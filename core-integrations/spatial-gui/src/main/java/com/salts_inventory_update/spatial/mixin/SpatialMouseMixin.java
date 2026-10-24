package com.salts_inventory_update.spatial.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;

// Wrap outside upstream's priority-1100 wrapper, before it reads/remaps raw coordinates.
@Mixin(value = MouseHandler.class, priority = 1500)
public class SpatialMouseMixin {
    @WrapMethod(method = "onMove")
    private void salts$physicalGameplayMouse(long window, double x, double y, double dx, double dy, Operation<Void> original) {
        boolean previous = SpatialGuiIntegration.mousePass;
        SpatialGuiIntegration.mousePass = true;
        try { original.call(window, x, y, dx, dy); }
        finally { SpatialGuiIntegration.mousePass = previous; }
    }
}
