package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** QA-only observation of actual native transitions during the keyboard modal stages. */
@Mixin(Minecraft.class)
public abstract class QaScreenTransitionMixin {
    @Shadow public Screen screen;
    @Inject(method="setScreen",at=@At("HEAD"))
    private void traceKeyboardScreen(Screen next,CallbackInfo callback){
        if(!ControlifyGameplayHarness.tracingKeyboard())return;
        System.out.println("CONTROLIFY_QA_KEYBOARD_SCREEN "+(screen==null?"null":screen.getClass().getSimpleName())+" -> "+(next==null?"null":next.getClass().getSimpleName())
            +" active="+WorkspaceSearchKeyboard.active()+" waiting="+WorkspaceSearchKeyboard.waitingNeutral());
        new Throwable("QA native keyboard screen transition").printStackTrace();
    }
}
