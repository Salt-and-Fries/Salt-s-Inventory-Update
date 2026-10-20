package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaPlaytestProbe;
import com.salts_inventory_update.controlify.qa.QaStatusEffectLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.salts_inventory_update.controlify.WorkspaceEffectsTray",remap=false)
public abstract class FeatureEffectsTrayProbeMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true,remap=false)
    private static void before(CallbackInfo callback) {
        if (QaStatusEffectLayouts.active()) { callback.cancel(); return; }
        QaPlaytestProbe.inTray=true;QaPlaytestProbe.trayTexts=0;
    }
    @Inject(method="render",at=@At("RETURN"),remap=false)
    private static void after(CallbackInfo callback) {
        if (QaPlaytestProbe.trayTexts>0) QaPlaytestProbe.trayFrames++;
        QaPlaytestProbe.inTray=false;
    }
}
