package com.salts_inventory_update.spatial.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;

/** The same scoped suppression keeps Spatial GUI's cosmetic pose out of gameplay aiming. */
@Mixin(value = AvatarRenderer.class, priority = 1500)
public class SpatialAvatarMixin {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V")
    private void salts$gameplayPose(Avatar avatar, AvatarRenderState state, float tick, Operation<Void> original) {
        boolean previous = SpatialGuiIntegration.cameraPass;
        SpatialGuiIntegration.cameraPass = previous || SpatialGuiIntegration.gameplay();
        try { original.call(avatar, state, tick); }
        finally { SpatialGuiIntegration.cameraPass = previous; }
    }
}
