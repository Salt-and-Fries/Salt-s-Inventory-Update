package com.salts_inventory_update.controlify.qa.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class FeatureNativeGuideDrawProbeMixin {
    @Inject(method="itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",at=@At("HEAD"))
    private void itemDecorationsBefore(Font font,ItemStack stack,int x,int y,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.itemDecorationsBefore();
    }
    @Inject(method="itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",at=@At("RETURN"))
    private void itemDecorationsAfter(Font font,ItemStack stack,int x,int y,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.itemDecorationsAfter();
    }
    @Inject(method="nextStratum",at=@At("HEAD"))
    private void nextStratum(CallbackInfo callback) { com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nextStratum(); }
    @Inject(method = "fill(IIIII)V", at = @At("HEAD"))
    private void alphabetFill(int x1,int y1,int x2,int y2,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.procedural("fill");
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.ghostFill(x1,y1,x2,y2,color);
    }
    @Inject(method = "outline(IIIII)V", at = @At("HEAD"))
    private void shapeOutline(int x,int y,int width,int height,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.procedural("outline");
    }
    @Inject(method="innerFill",at=@At("HEAD"))
    private void anyNativeShape(CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.procedural("innerFill");
    }
    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void sourceItem(ItemStack stack,int x,int y,int seed,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeItem(stack);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.ghostItem((GuiGraphicsExtractor)(Object)this,stack,x,y,seed);
    }
    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void item(ItemStack stack, int x, int y, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaFeatureProbe.tabIcon((GuiGraphicsExtractor)(Object)this,stack);
        QaVisualFeatureProbe.item((GuiGraphicsExtractor)(Object)this,stack,x,y);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeItem(stack);
    }
    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", at = @At("HEAD"))
    private void text(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfo callback) {
        if (com.salts_inventory_update.controlify.qa.QaFeatureProbe.renderingTabs) com.salts_inventory_update.controlify.qa.QaFeatureProbe.tabTextDraws++;
        QaNativeGuideProbe.text((GuiGraphicsExtractor)(Object)this, font, text, x, y);
        QaVisualFeatureProbe.alphabetText((GuiGraphicsExtractor)(Object)this,font,text,x,y,color);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeFrameText(text.getString(),x,y,color);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.artworkText(text.getString(),color,shadow);
    }
    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"))
    private void stringText(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo callback) {
        if (com.salts_inventory_update.controlify.qa.QaFeatureProbe.renderingTabs) com.salts_inventory_update.controlify.qa.QaFeatureProbe.tabTextDraws++;
        QaNativeGuideProbe.text((GuiGraphicsExtractor)(Object)this, font, Component.literal(text), x, y);
        QaVisualFeatureProbe.alphabetText((GuiGraphicsExtractor)(Object)this,font,Component.literal(text),x,y,color);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeFrameText(text,x,y,color);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.artworkText(text,color,shadow);
    }
    @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V", at = @At("HEAD"))
    private void rawBook(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height,
                         int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int color, CallbackInfo callback) {
        QaNativeGuideProbe.rawBook(texture);
        QaVisualFeatureProbe.texture((GuiGraphicsExtractor)(Object)this,texture,x,y,u,v,width,height,sourceWidth,sourceHeight,textureWidth,textureHeight);
        QaVisualFeatureProbe.alphabetAnchorTexture(texture,x,y,width,height,color);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.texture((GuiGraphicsExtractor)(Object)this,
            texture,x,y,u,v,width,height,sourceWidth,sourceHeight,textureWidth,textureHeight,color);
    }
}
