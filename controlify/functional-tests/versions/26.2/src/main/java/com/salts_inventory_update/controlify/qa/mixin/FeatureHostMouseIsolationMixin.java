package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Isolates only host GLFW callbacks; native controller onMove invocations remain intact. */
@Mixin(MouseHandler.class)
public abstract class FeatureHostMouseIsolationMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target =
        "Lcom/mojang/blaze3d/platform/InputConstants;setupMouseCallbacks(Lcom/mojang/blaze3d/platform/Window;Lorg/lwjgl/glfw/GLFWCursorPosCallbackI;Lorg/lwjgl/glfw/GLFWMouseButtonCallbackI;Lorg/lwjgl/glfw/GLFWScrollCallbackI;Lorg/lwjgl/glfw/GLFWDropCallbackI;)V"), index = 1)
    private GLFWCursorPosCallbackI isolateMotion(GLFWCursorPosCallbackI original) {
        return (window, x, y) -> {
            if (ControlifyGameplayHarness.isolateHostMouseMotion()) QaFeatureProbe.isolatedHostMotion(window, x, y);
            else original.invoke(window, x, y);
        };
    }
}
