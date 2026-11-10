package com.salts_inventory_update.compat.nativewindows.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 21.11's original guide boxes a char, which vanilla text arguments now reject. */
@Pseudo
@Mixin(targets="fuzs.easyanvils.client.gui.components.FormattingGuideWidget",remap=false)
public abstract class EasyAnvilsFormattingArgumentMixin {
    @ModifyArg(method="<init>(IILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)V",at=@At(value="INVOKE",
        target="Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
        remap=true),index=1,remap=false)
    private Object[] salts$formattingCharacterAsText(Object[] arguments) {
        if(arguments.length>0&&arguments[0] instanceof Character value) {
            Object[] normalized=arguments.clone();normalized[0]=value.toString();return normalized;
        }
        return arguments;
    }
}
