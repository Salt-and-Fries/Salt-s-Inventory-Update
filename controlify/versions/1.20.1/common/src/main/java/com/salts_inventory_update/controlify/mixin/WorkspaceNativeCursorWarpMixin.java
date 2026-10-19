package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=VirtualMouseHandler.class,remap=false)
public abstract class WorkspaceNativeCursorWarpMixin {
    @Redirect(method="setMousePosition",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"),remap=false)
    private void saltsControlify$recordKnownWarp(long window,double x,double y) {
        NativeCursorWarpGuard.nativeWarp(window,x,y,() -> GLFW.glfwSetCursorPos(window,x,y));
    }
    @Redirect(method={"enableVirtualMouse","disableVirtualMouse","onScreenChanged"},at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwSetInputMode(JII)V"),remap=false)
    private void saltsControlify$recordNativeCursorMode(long window,int mode,int value) {
        NativeCursorWarpGuard.nativeCursorMode(window,mode,value,() -> GLFW.glfwSetInputMode(window,mode,value));
    }
}
