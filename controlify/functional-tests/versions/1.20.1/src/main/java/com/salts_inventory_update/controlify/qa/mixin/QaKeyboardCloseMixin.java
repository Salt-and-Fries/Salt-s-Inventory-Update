package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.network.DesktopPackets;
@Mixin(value=WorkspaceSearchKeyboard.class,remap=false)
public abstract class QaKeyboardCloseMixin {
    @Inject(method="onClose",at=@At("HEAD"),remap=false)
    private void traceNativeClose(CallbackInfo callback){
        if(ControlifyGameplayHarness.tracingKeyboard())new Throwable("QA native keyboard onClose active="+WorkspaceSearchKeyboard.active()+" waiting="+WorkspaceSearchKeyboard.waitingNeutral()).printStackTrace();
    }
}

@Mixin(value=DesktopContainerClient.class,remap=false)
abstract class QaRecipeHoverTraceMixin {
    @Inject(method="send",at=@At("HEAD"),remap=false)
    private static void saltsQA$observePacket(DesktopPackets.DesktopPacket payload,String label,CallbackInfoReturnable<Boolean> callback){
        if(!ControlifyGameplayHarness.tracingRecipeWheel())return;
        System.out.println("CONTROLIFY_QA_RECIPE_PACKET label="+label+" payload="+payload);
        if(!label.startsWith("recipe-hover"))new Throwable("QA recipe session mutation source").printStackTrace();
    }
    @Inject(method="endRecipeHover",at=@At("HEAD"),remap=false)
    private static void saltsQA$end(int source,CallbackInfo callback){
        if(ControlifyGameplayHarness.tracingRecipeWheel())new Throwable("QA recipe hover end source="+source).printStackTrace();
    }
    @Inject(method="recipeHoverRejected",at=@At("RETURN"),remap=false)
    private static void saltsQA$rejected(int source,CallbackInfoReturnable<Boolean> callback){
        if(ControlifyGameplayHarness.tracingRecipeWheel()&&callback.getReturnValue())new Throwable("QA actual recipe hover rejected source="+source).printStackTrace();
    }
}
