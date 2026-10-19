package com.salts_inventory_update.controlify.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Wrap outside Controlify's callback so native hover still receives an exact warp echo. */
@Mixin(InputConstants.class)
public abstract class WorkspaceMouseCallbackMixin {
    @ModifyVariable(method="setupMouseCallbacks",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static GLFWCursorPosCallbackI saltsControlify$wrapNativeMouseMotion(GLFWCursorPosCallbackI callback) {
        return NativeCursorWarpGuard.wrap(callback);
    }
}
