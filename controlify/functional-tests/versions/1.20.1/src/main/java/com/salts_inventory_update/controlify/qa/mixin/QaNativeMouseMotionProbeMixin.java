package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class QaNativeMouseMotionProbeMixin {
    @Inject(method="onMove",at=@At("RETURN"))
    private void saltsQA$observeMouseMotion(long window,double x,double y,CallbackInfo callback){
        ControlifyGameplayHarness.observeNativeMouseMotion(window,x,y);
    }
}
