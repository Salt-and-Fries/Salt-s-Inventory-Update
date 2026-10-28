package com.salts_inventory_update.client.automatic;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** One native render, then any number of source fragments. Restores the caller's actual framebuffer. */
final class NativeFrameBuffer implements AutoCloseable {
    private TextureTarget target;
    DesktopWindowBounds bounds;
    double scale;
    int pixelHeight;
    void capture(DesktopRenderContext<?,?> context, AutomaticNativeWindow.State state,
                 AutomaticNativeScope.Frame frame, DesktopWindowBounds sourceBounds, int mouseX,int mouseY) {
        var mc=context.minecraft();var outer=context.nativeGraphics();outer.flush();
        int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4],scissor=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);
        boolean clipped=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var shader=RenderSystem.getShader();int texture=RenderSystem.getShaderTexture(0);float[] color=RenderSystem.getShaderColor().clone();
        var model=RenderSystem.getModelViewStack();model.pushPose();
        bounds=sourceBounds;scale=Math.min(mc.getWindow().getGuiScale(),Math.min(4096d/bounds.width(),4096d/bounds.height()));
        int width=Math.max(1,(int)Math.ceil(bounds.width()*scale));pixelHeight=Math.max(1,(int)Math.ceil(bounds.height()*scale));
        try {
            RenderSystem.disableScissor();
            if(target==null||target.width!=width||target.height!=pixelHeight){if(target!=null)target.destroyBuffers();target=new TextureTarget(width,pixelHeight,true,Minecraft.ON_OSX);target.setClearColor(0,0,0,0);}
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,bounds.width(),bounds.height(),0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            model.setIdentity();model.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();RenderSystem.setShaderColor(1,1,1,1);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());graphics.pose().translate(frame.offsetX(),frame.offsetY(),0);
            state.capturing=true;
            AutomaticNativeScope.call(frame,()->{state.screen.render(graphics,mouseX,mouseY,mc.getFrameTime());graphics.flush();return null;});
        } finally {
            state.capturing=false;
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            model.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            if(clipped)RenderSystem.enableScissor(scissor[0],scissor[1],scissor[2],scissor[3]);else RenderSystem.disableScissor();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.setShaderTexture(0,texture);if(shader!=null)RenderSystem.setShader(()->shader);
        }
    }
    void paint(GuiGraphics graphics,DesktopWindowBounds source,int x,int y) {
        if(target==null)return;graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,target.getColorTextureId());
        RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1,1,1,1);
        float u0=(source.x()-bounds.x())/(float)bounds.width(),u1=(source.x()+source.width()-bounds.x())/(float)bounds.width();
        float v0=1-(source.y()-bounds.y())/(float)bounds.height(),v1=1-(source.y()+source.height()-bounds.y())/(float)bounds.height();
        var matrix=graphics.pose().last().pose();var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        b.vertex(matrix,x,y+source.height(),0).uv(u0,v1).endVertex();b.vertex(matrix,x+source.width(),y+source.height(),0).uv(u1,v1).endVertex();
        b.vertex(matrix,x+source.width(),y,0).uv(u1,v0).endVertex();b.vertex(matrix,x,y,0).uv(u0,v0).endVertex();BufferUploader.drawWithShader(b.end());
        RenderSystem.defaultBlendFunc();
    }
    @Override public void close(){if(target!=null){target.destroyBuffers();target=null;}}
}
