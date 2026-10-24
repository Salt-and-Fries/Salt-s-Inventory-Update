package com.salts_inventory_update.spatial.mixin;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialOptionsAccess;
import com.salts_inventory_update.spatial.SpatialMovementTransition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Options;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Options.class, priority = 1500)
public abstract class SpatialOptionsMixin implements SpatialOptionsAccess {
    @Accessor("cameraType") public abstract CameraType salts$perspective();
    @WrapMethod(method = "getCameraType")
    private CameraType salts$gameplayPerspective(Operation<CameraType> original) {
        if(SpatialGuiIntegration.gameplay())return CameraType.FIRST_PERSON;
        return SpatialMovementTransition.closing()?salts$perspective():original.call();
    }
}
