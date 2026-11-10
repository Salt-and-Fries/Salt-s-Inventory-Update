package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.client.gui.GuiBase",remap=false)
public abstract class RebornScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    protected RebornScreenMixin(AbstractContainerMenu m,Inventory i,Component t){super(m,i,t);}
    @ModifyVariable(method={"<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V","<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;II)V"},at=@At("HEAD"),argsOnly=true,remap=false) private static AbstractContainerMenu salts$menu(AbstractContainerMenu original){return TechRebornScreens.constructionMenu(original);}
    @Inject(method={"<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V","<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;II)V"},at=@At("RETURN"),remap=false) private void salts$remember(CallbackInfo ci){TechRebornScreens.remember(this);}
    @Inject(method="addHologramButton",at=@At("HEAD"),remap=false)
    private void salts$oneHologram(int x,int y,int id,@Coerce Object layer,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> ci){
        // Several native multiblock screens add this selectable on every render.
        // Keep the current one, rather than stale invisible H hit boxes after
        // a structure transitions between incomplete and formed states.
        if(TechRebornScreens.capturing())for(var child:java.util.List.copyOf(children()))
            if(child.getClass().getName().equals("reborncore.client.gui.widget.GuiButtonHologram"))removeWidget(child);
    }
    @Inject(method="hideGuiElements",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$fluidContext(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){
        Object tab=com.salts_inventory_update.compat.techreborn.RebornAccess.field(this,"selectedTab");
        if(TechRebornScreens.hosted(getMenu())&&tab!=null&&tab.getClass().getSimpleName().equals("FluidConfigGui"))ci.setReturnValue(false);
    }
    @Inject(method="drawTitle",at=@At("HEAD"),cancellable=true,remap=false) private void salts$title(GuiGraphicsExtractor g,CallbackInfo ci){if(TechRebornScreens.capturing())ci.cancel();}
}
