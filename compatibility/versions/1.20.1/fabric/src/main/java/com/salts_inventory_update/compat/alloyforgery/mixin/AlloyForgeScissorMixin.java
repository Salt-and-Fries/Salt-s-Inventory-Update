package com.salts_inventory_update.compat.alloyforgery.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Owo's transformed rectangles must use the active render target's pixel height. */
@Pseudo
@Mixin(targets="io.wispforest.owo.ui.util.ScissorStack",remap=false)
abstract class AlloyForgeScissorMixin {
    @Redirect(method="applyState",at=@At(value="INVOKE",target="Lorg/lwjgl/opengl/GL11;glScissor(IIII)V"),remap=false)
    private static void salts$activeTargetClip(int x,int y,int width,int height){
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        GL11.glScissor(x,y+viewport[3]-Minecraft.getInstance().getWindow().getHeight(),width,height);
    }
}
