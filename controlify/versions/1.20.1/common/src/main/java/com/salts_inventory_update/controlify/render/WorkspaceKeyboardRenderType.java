package com.salts_inventory_update.controlify.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Keeps Controlify 2.x's native keyboard texture and matches its actual vertex format. */
public final class WorkspaceKeyboardRenderType {
    private static final Map<ResourceLocation,RenderType> TYPES=new HashMap<>();
    private WorkspaceKeyboardRenderType() { }
    public static RenderType textured(ResourceLocation texture){
        return TYPES.computeIfAbsent(texture,key->new RenderType("salts_inventory_controlify:keyboard",
            DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,786432,false,false,()->{
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderTexture(0,key);
                RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();
            },()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();}){});
    }
}
