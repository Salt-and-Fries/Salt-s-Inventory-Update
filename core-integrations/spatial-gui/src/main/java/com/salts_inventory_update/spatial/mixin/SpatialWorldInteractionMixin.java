package com.salts_inventory_update.spatial.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.client.CursorWorldInteraction;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialDesktopAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CursorWorldInteraction.class, remap = false)
public class SpatialWorldInteractionMixin {
    @WrapMethod(method = "continueAttackAtCursor")
    private static void salts$windowsOwnHeldInput(Minecraft mc, double x, double y, Operation<Void> original) {
        if (SpatialGuiIntegration.active() && SpatialGuiIntegration.overPanel()
                && ((SpatialDesktopAccess)(Object)SpatialGuiIntegration.desktop()).salts$isPointOverInteractiveUi(x, y)) {
            CursorWorldInteraction.stopAttack(mc);
        } else original.call(mc, x, y);
    }
    @WrapMethod(method = "pickAtCursor")
    private static HitResult salts$physicalWorldRay(Minecraft mc, double x, double y, Operation<HitResult> original) {
        return SpatialGuiIntegration.active() ? SpatialGuiIntegration.pickWorld() : original.call(mc, x, y);
    }
}
