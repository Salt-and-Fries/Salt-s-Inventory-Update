package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.input.WorkspaceContainerScreen;
import com.salts_inventory_update.controlify.model.ControllerScrollRepeat;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changes native cursor navigation only while Salt's workspace owns the screen. */
@Mixin(VirtualMouseHandler.class)
public abstract class WorkspaceNavigationMixin {
    @Shadow(remap = false) public abstract int getCurrentX(float partialTick);
    @Shadow(remap = false) public abstract int getCurrentY(float partialTick);
    @Shadow(remap = false) public abstract void snapToPoint(SnapPoint point, Vector2dc scale);
    @Shadow(remap = false) public abstract void preventScrollingThisTick();
    @Unique private final ControllerScrollRepeat saltsControlify$scrollRepeat = new ControllerScrollRepeat();

    @Inject(method = "handleControllerInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void saltsControlify$freezeContextualCursor(ControllerEntity controller, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof WorkspaceContainerScreen workspace && workspace.contextualRadialActive()) {
            preventScrollingThisTick();
            saltsControlify$scrollRepeat.reset();
            callback.cancel();
        }
    }

    @Inject(method = "snapInDirection", at = @At("HEAD"), cancellable = true, remap = false)
    private void saltsControlify$logicalNavigation(ScreenDirection direction, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof WorkspaceContainerScreen workspace)) return;
        var window = minecraft.getWindow();
        var scale = new Vector2d((double) window.getGuiScaledWidth() / window.getScreenWidth(),
            (double) window.getGuiScaledHeight() / window.getScreenHeight());
        double x = getCurrentX(1f) * scale.x;
        double y = getCurrentY(1f) * scale.y;
        if ((direction == ScreenDirection.UP || direction == ScreenDirection.DOWN)
                && workspace.controllerScroll(x, y, direction == ScreenDirection.UP ? -1 : 1, true)) {
            callback.cancel();
            return;
        }
        SnapPoint point = workspace.controllerNavigate(direction, x, y);
        if (point != null) {
            snapToPoint(point, scale);
            callback.cancel();
        }
    }

    @Inject(method = "handleScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private void saltsControlify$rightStickRowScroll(ControllerEntity controller, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof WorkspaceContainerScreen workspace)) {
            saltsControlify$scrollRepeat.reset();
            return;
        }
        // Own native scrolling here, so analog speed never also scrolls a second time upstream.
        preventScrollingThisTick();
        if (workspace.contextualRadialActive()) {
            saltsControlify$scrollRepeat.reset();
            callback.cancel();
            return;
        }
        var state = controller.input().orElseThrow().stateNow();
        double vertical = state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN)
            - state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP);
        int rows = saltsControlify$scrollRepeat.update(vertical, System.nanoTime() / 1_000_000L);
        if (rows != 0) {
            var window = minecraft.getWindow();
            double x = getCurrentX(1f) * (double) window.getGuiScaledWidth() / window.getScreenWidth();
            double y = getCurrentY(1f) * (double) window.getGuiScaledHeight() / window.getScreenHeight();
            // Core's existing API consumes exactly one row per call. Preserve that contract
            // while allowing a bounded elapsed-time batch at full stick or lower frame rates.
            for (int row = 0; row < Math.abs(rows); row++) {
                if (!workspace.controllerScroll(x, y, Integer.signum(rows), false)) {
                    saltsControlify$scrollRepeat.reset();
                    break;
                }
            }
        }
        callback.cancel();
    }

    @Inject(method = {"onScreenChanged", "disableVirtualMouse"}, at = @At("HEAD"), remap = false)
    private void saltsControlify$resetScrolling(CallbackInfo callback) {
        saltsControlify$scrollRepeat.reset();
    }
}
