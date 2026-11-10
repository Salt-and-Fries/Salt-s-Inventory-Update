package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.tradingpost.TradingPostWindows;
import com.salts_inventory_update.compat.tradingpost.TradingPostPackets;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native-only screens retain seven rows. Only the explicit SIU template uses four. */
@Pseudo
@Mixin(targets="fuzs.tradingpost.client.gui.screens.inventory.TradingPostScreen",remap=false)
public abstract class TradingPostScreenMixin implements DesktopNativeTextInput {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){TradingPostWindows.remember((AbstractContainerScreen<?>)(Object)this);}
    @ModifyConstant(method={"render","method_25394","m_88315_"},constant=@Constant(intValue=7),require=0,remap=false)
    private int salts$fourOffers(int original){return TradingPostWindows.hosted(this)?4:original;}
    @Redirect(method="refreshSearchResults",at=@At(value="INVOKE",target="Lfuzs/puzzleslib/api/network/v3/NetworkHandler;sendToServer(Lfuzs/puzzleslib/api/network/v3/ServerboundMessage;)V",remap=false),remap=false)
    private void salts$clearOwningPayment(@Coerce Object network,@Coerce Object packet){
        // Rebuilding the client search tree is a server update, not a new user
        // clear request. Interactive searches still take the authenticated path.
        if(TradingPostPackets.applying())return;
        try{Class.forName("fuzs.puzzleslib.api.network.v3.NetworkHandler")
            .getMethod("sendToServer",Class.forName("fuzs.puzzleslib.api.network.v3.ServerboundMessage")).invoke(network,packet);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    @Override public boolean desktopNativeTextInputFocused(){return TradingPostWindows.textFocused(this);}
}
