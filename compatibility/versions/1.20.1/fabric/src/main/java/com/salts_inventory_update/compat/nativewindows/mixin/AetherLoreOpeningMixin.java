package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Forge's no-data convenience overload supplies an empty writer. Use the registered Aether factory data. */
@Pseudo @Mixin(targets="com.aetherteam.aether.item.miscellaneous.LoreBookItem",remap=false)
public abstract class AetherLoreOpeningMixin {
    @Redirect(method={"use","m_7203_","method_7836"},at=@At(value="INVOKE",target="Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;)V"),require=0,remap=false)
    private void salts$loreOpening(ServerPlayer player,MenuProvider provider){
        if(DesktopSessions.shouldCapture(player)&&DesktopSessions.openMenuSession(player,provider,null)!=null)return;
        try{Class.forName("net.minecraftforge.network.NetworkHooks").getMethod("openScreen",ServerPlayer.class,MenuProvider.class).invoke(null,player,provider);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native Aether Lore opening",failure);}
    }
}
