package com.salts_inventory_update.client.automatic;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.mixin.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.*;
import org.joml.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
final class NativeFrameBuffer implements AutoCloseable {
    private GuiRenderState captured;
    private DesktopWindowBounds bounds;
    void capture(DesktopRenderContext<?,?> context,AutomaticNativeWindow.State state,AutomaticNativeScope.Frame frame,DesktopWindowBounds source,int mouseX,int mouseY) {
        bounds=source;captured=new GuiRenderState();
        var graphics=new GuiGraphicsExtractor(context.minecraft(),captured,mouseX,mouseY);
        graphics.pose().translate(frame.offsetX(),frame.offsetY());
        state.capturing=true;
        try { AutomaticNativeScope.call(frame,()->{state.screen.extractBackground(graphics,mouseX,mouseY,context.minecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false));state.screen.extractRenderState(graphics,mouseX,mouseY,context.minecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false));return null;}); }
        finally {state.capturing=false;}
    }
    void paint(GuiGraphicsExtractor graphics,DesktopWindowBounds source,int x,int y,DesktopWindowBounds parent) {
        if(captured==null)return;
        var transform=new Matrix3x2f(graphics.pose()).translate(x-source.x()+bounds.x(),y-source.y()+bounds.y());
        var clip=new ScreenRectangle(x,y,source.width(),source.height()).transformMaxBounds(graphics.pose());
        if(parent!=null)clip=intersect(clip,new ScreenRectangle(parent.x(),parent.y(),parent.width(),parent.height()).transformMaxBounds(graphics.pose()));
        final var outputClip=clip;
        var output=((AutomaticGraphicsCaptureAccessor)graphics).salts_inventory_update$renderState();
        ((AutomaticRenderStateAccessor)captured).salts_inventory_update$traverse(raw->{
            var node=(AutomaticRenderNodeAccessor)raw;
            each(node.salts_inventory_update$elementStates(),s->output.addGuiElement(new MovedElement(s,transform,outputClip)));
            each(node.salts_inventory_update$glyphStates(),s->output.addGlyphToCurrentLayer(new MovedElement(s,transform,outputClip)));
            each(node.salts_inventory_update$itemStates(),s->output.addItem(new GuiItemRenderState( new Matrix3x2f(transform).mul(s.pose()),s.itemStackRenderState(),s.x(),s.y(),clip(s.scissorArea(),transform,outputClip))));
            each(node.salts_inventory_update$textStates(),s->{
                var a=(AutomaticTextStateAccessor)(Object)s;
                output.addText(new GuiTextRenderState(a.salts_inventory_update$font(),a.salts_inventory_update$text(),new Matrix3x2f(transform).mul(s.pose),a.salts_inventory_update$x(),a.salts_inventory_update$y(),a.salts_inventory_update$color(),a.salts_inventory_update$backgroundColor(),a.salts_inventory_update$dropShadow(),a.salts_inventory_update$includeEmpty(),clip(s.scissor,transform,outputClip)));
            });
            each(node.salts_inventory_update$picturesInPictureStates(),s->output.addPicturesInPictureState(NativePictureStates.move(s,transform,outputClip)));
            output.up();
        },GuiRenderState.TraverseRange.ALL);
    }
    private static <T> void each(List<T> values,java.util.function.Consumer<T> action){if(values!=null)values.forEach(action);}
    static ScreenRectangle intersect(ScreenRectangle a,ScreenRectangle b){if(a==null)return b;if(b==null)return a;var c=a.intersection(b);return c==null?ScreenRectangle.empty():c;}
    static ScreenRectangle clip(ScreenRectangle s,Matrix3x2fc transform,ScreenRectangle parent){return intersect(s==null?null:s.transformMaxBounds(transform),parent);}
    private record MovedElement(GuiElementRenderState source,Matrix3x2fc transform,ScreenRectangle parent) implements GuiElementRenderState {
        public com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline(){return source.pipeline();}
        public net.minecraft.client.gui.render.TextureSetup textureSetup(){return source.textureSetup();}
        public ScreenRectangle scissorArea(){return clip(source.scissorArea(),transform,parent);}
        public ScreenRectangle bounds(){return clip(source.bounds(),transform,parent);}
        public void buildVertices(VertexConsumer target){source.buildVertices(new MovedVertices(target,transform));}
    }
    private record MovedVertices(VertexConsumer target,Matrix3x2fc transform) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){var p=transform.transformPosition(x,y,new Vector2f());target.addVertex(p.x,p.y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,a);return this;}
        public VertexConsumer setColor(int rgba){target.setColor(rgba);return this;}
        public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
        public VertexConsumer setUv3(float u,float v){target.setUv3(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
        public VertexConsumer setLineWidth(float w){target.setLineWidth(w);return this;}
    }
    public void close(){captured=null;}
}
