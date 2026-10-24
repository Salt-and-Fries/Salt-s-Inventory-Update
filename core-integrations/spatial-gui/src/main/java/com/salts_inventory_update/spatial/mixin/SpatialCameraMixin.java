package com.salts_inventory_update.spatial.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialOptionsAccess;
import com.salts_inventory_update.spatial.SpatialMovementTransition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;

/** Keep the renderer active, but let vanilla align the gameplay camera during movement control. */
@Mixin(value = Camera.class, priority = 500)
public class SpatialCameraMixin {
    @Shadow private Vec3 position;
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow @Final private Quaternionf rotation;
    @Unique private Vec3 salts$lastOffset;
    @Unique private Vec3 salts$startOffset;
    @Unique private float salts$lastYawOffset, salts$lastPitchOffset, salts$startYawOffset, salts$startPitchOffset;
    @Unique private int salts$revision=-1;

    @WrapMethod(method = "alignWithEntity")
    private void salts$gameplayCamera(float partialTick, Operation<Void> original) {
        boolean gameplay = SpatialGuiIntegration.gameplay();
        int revision = SpatialMovementTransition.update();
        boolean active = SpatialMovementTransition.presenting();
        boolean first = SpatialGUIClient.getEffectiveFirstPersonMode();
        if (active && revision != salts$revision && salts$lastOffset != null) {
            salts$startOffset=salts$lastOffset;
            salts$startYawOffset=salts$lastYawOffset;
            salts$startPitchOffset=salts$lastPitchOffset;
        }
        salts$revision=revision;
        boolean nativeTarget = gameplay || SpatialMovementTransition.closing();
        SpatialGuiIntegration.cameraPass = nativeTarget;
        try { original.call(partialTick); }
        finally {
            SpatialGuiIntegration.cameraPass = false;
            if (nativeTarget) SpatialGUIClient.setEffectiveFirstPersonMode(first);
        }
        var player=Minecraft.getInstance().player;
        if (player==null) {salts$lastOffset=null;salts$startOffset=null;return;}
        Vec3 eye=player.getEyePosition(partialTick);
        double progress=SpatialMovementTransition.progress();
        if (active && salts$startOffset!=null && progress<1) {
            position=eye.add(salts$startOffset).lerp(position,progress);
            yRot=Mth.rotLerp((float)progress,player.getYRot()+salts$startYawOffset,yRot);
            xRot=Mth.lerp((float)progress,player.getXRot()+salts$startPitchOffset,xRot);
            rotation.rotationYXZ((float)Math.PI-yRot*((float)Math.PI/180),-xRot*((float)Math.PI/180),0);
            SpatialGUIClient.setCameraPosition(position);
        }
        salts$lastOffset=position.subtract(eye);
        salts$lastYawOffset=Mth.wrapDegrees(yRot-player.getYRot());
        salts$lastPitchOffset=xRot-player.getXRot();
    }
}
