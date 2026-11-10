package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.common.screen.BuiltScreenHandler",remap=false)
public abstract class RebornSyncMixin {
    @Inject(method="sendContentUpdatePacketToListener",at=@At("HEAD"),remap=false) private void salts$begin(@Coerce Object listener,CallbackInfo ci){TechRebornWindows.beginSync((AbstractContainerMenu)(Object)this);}
    @Inject(method="sendContentUpdatePacketToListener",at=@At("RETURN"),remap=false) private void salts$end(@Coerce Object listener,CallbackInfo ci){TechRebornWindows.endSync();}
    @Redirect(method="sendContentUpdatePacketToListener",at=@At(value="INVOKE",target="Lreborncore/common/screen/ServerPlayerEntityScreenHandlerHelper;getServerPlayerEntity",remap=false),remap=false)
    private Optional<ServerPlayer> salts$owner(@Coerce Object listener){return TechRebornWindows.listener(listener);}
}
