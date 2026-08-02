package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.DesktopMouseHandoff;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerHandoffMixin implements DesktopMouseHandoff {
    @Unique
    private static final int SALTS_INVENTORY_UPDATE$HANDOFF_GRACE_FRAMES = 4;

    @Shadow
    private boolean ignoreFirstMove;

    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Unique
    private int salts_inventory_update$desktopHandoffFrames;

    @Override
    public void salts_inventory_update$beginDesktopMouseHandoff() {
        this.salts_inventory_update$desktopHandoffFrames = SALTS_INVENTORY_UPDATE$HANDOFF_GRACE_FRAMES;
        this.salts_inventory_update$clearDesktopHandoffMovement();
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void salts_inventory_update$rebaseDesktopHandoffMovement(long window, double x, double y, CallbackInfo ci) {
        if (this.salts_inventory_update$desktopHandoffFrames > 0) {
            this.ignoreFirstMove = true;
        }
    }

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void salts_inventory_update$discardDesktopHandoffMovement(CallbackInfo ci) {
        if (this.salts_inventory_update$desktopHandoffFrames <= 0) {
            return;
        }

        this.salts_inventory_update$clearDesktopHandoffMovement();
        this.salts_inventory_update$desktopHandoffFrames--;
    }

    @Unique
    private void salts_inventory_update$clearDesktopHandoffMovement() {
        this.accumulatedDX = 0.0D;
        this.accumulatedDY = 0.0D;
        this.smoothTurnX.reset();
        this.smoothTurnY.reset();
    }
}
