package com.salts_inventory_update.controlify.qa.mixin;
import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(InputConstants.class)
public abstract class QaInputCursorWarpTraceMixin {
    @Redirect(method="grabOrReleaseMouse",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static void saltsQA$observeInputWarp(long window,double x,double y){
        if(ControlifyGameplayHarness.tracingCursorWarp()){
            System.out.println("CONTROLIFY_QA_INPUT_CONSTANTS_WARP window="+window+" x="+x+" y="+y);
            new Throwable("QA native InputConstants cursor warp").printStackTrace();
        }
        GLFW.glfwSetCursorPos(window,x,y);
    }
}
