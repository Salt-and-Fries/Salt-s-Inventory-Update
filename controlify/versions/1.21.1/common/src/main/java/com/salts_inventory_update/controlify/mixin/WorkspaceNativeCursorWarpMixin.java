package com.salts_inventory_update.controlify.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=VirtualMouseHandler.class,remap=false)
public abstract class WorkspaceNativeCursorWarpMixin {
    @WrapOperation(method="setMousePosition",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"),remap=false)
    private void saltsControlify$recordKnownWarp(long window,double x,double y,Operation<Void> original) {
        NativeCursorWarpGuard.nativeWarp(window,x,y,() -> original.call(window,x,y));
    }
}
