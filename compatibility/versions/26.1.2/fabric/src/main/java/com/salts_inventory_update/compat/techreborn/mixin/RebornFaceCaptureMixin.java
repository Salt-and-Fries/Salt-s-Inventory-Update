package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.RebornAccess;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Submit the publisher's custom picture state directly, outside native capture. */
@Pseudo @Mixin(targets="reborncore.client.gui.config.elements.AbstractConfigPopupElement",remap=false)
public abstract class RebornFaceCaptureMixin {
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState",remap=false),remap=false)
    private void salts$defer(@Coerce Object state,@Coerce Object picture){if(!TechRebornScreens.capturing())RebornAccess.call(state,"addPicturesInPictureState",picture);}
}
