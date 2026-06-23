package net.minecraft.client.gui;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public final class GuiGraphicsExtractor {
    private final GuiGraphics graphics;
    private final PoseAdapter pose;

    private GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
        this.pose = new PoseAdapter(graphics.pose());
    }

    public static GuiGraphicsExtractor wrap(GuiGraphics graphics) {
        return new GuiGraphicsExtractor(graphics);
    }

    public GuiGraphics unwrap() {
        return this.graphics;
    }

    public int guiWidth() {
        return this.graphics.guiWidth();
    }

    public int guiHeight() {
        return this.graphics.guiHeight();
    }

    public PoseAdapter pose() {
        return this.pose;
    }

    public void nextStratum() {
        this.graphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, Minecraft.ON_OSX);
    }

    public void blurBeforeThisStratum() {
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        this.graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics.disableScissor();
    }

    public boolean containsPointInScissor(int x, int y) {
        return this.graphics.containsPointInScissor(x, y);
    }

    public void horizontalLine(int minX, int maxX, int y, int color) {
        this.graphics.hLine(minX, maxX, y, color);
    }

    public void verticalLine(int x, int minY, int maxY, int color) {
        this.graphics.vLine(x, minY, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void fill(Object pipeline, int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int fromColor, int toColor) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, fromColor, toColor);
    }

    public void outline(int x, int y, int width, int height, int color) {
        this.graphics.renderOutline(x, y, width, height, color);
    }

    public void text(Font font, String text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void centeredText(Font font, String text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        this.graphics.drawWordWrap(font, text, x, y, width, color);
    }

    public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color, boolean dropShadow) {
        this.graphics.drawWordWrap(font, text, x, y, width, color);
    }

    public void textWithBackdrop(Font font, Component text, int x, int y, int color, int backgroundColor) {
        this.graphics.drawStringWithBackdrop(font, text, x, y, color, backgroundColor);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        this.graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int color) {
        this.graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u0, float u1, float v0, float v1) {
        this.graphics.innerBlit(texture, x, x + width, y, y + height, 0, u0, u1, v0, v1);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        this.graphics.blitSprite(sprite, x, y, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height, float alpha) {
        this.graphics.blitSprite(sprite, x, y, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        this.graphics.blitSprite(sprite, x, y, width, height, color);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height) {
        this.graphics.blitSprite(sprite, spriteWidth, spriteHeight, u, v, x, y, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, int color) {
        this.graphics.blitSprite(sprite, spriteWidth, spriteHeight, u, v, x, y, width, height, color);
    }

    public void blitSprite(Object pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        this.graphics.blit(x, y, 0, width, height, sprite);
    }

    public void blitSprite(Object pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        this.graphics.blit(x, y, 0, width, height, sprite);
    }

    public void item(ItemStack stack, int x, int y) {
        this.graphics.renderItem(stack, x, y);
    }

    public void item(ItemStack stack, int x, int y, int seed) {
        this.graphics.renderItem(stack, x, y, seed);
    }

    public void item(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        this.graphics.renderItem(entity, stack, x, y, seed);
    }

    public void fakeItem(ItemStack stack, int x, int y) {
        this.graphics.renderFakeItem(stack, x, y);
    }

    public void fakeItem(ItemStack stack, int x, int y, int seed) {
        this.graphics.renderFakeItem(stack, x, y, seed);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        this.graphics.renderItemDecorations(font, stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y, String text) {
        this.graphics.renderItemDecorations(font, stack, x, y, text);
    }

    public void book(BookModel model, ResourceLocation texture, float scale, float open, float flip, int x0, int y0, int x1, int y1) {
        Lighting.setupForEntityInInventory();
        this.graphics.pose().pushPose();
        this.graphics.pose().translate((x0 + x1) / 2.0F, y0 + 17.0F, 100.0F);
        this.graphics.pose().scale(scale, scale, -scale);
        this.graphics.pose().mulPose(Axis.YP.rotationDegrees(180.0F));
        this.graphics.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
        this.graphics.pose().translate((1.0F - open) * 0.2F, (1.0F - open) * 0.1F, (1.0F - open) * 0.25F);
        this.graphics.pose().mulPose(Axis.YP.rotationDegrees(-(1.0F - open) * 90.0F - 90.0F));
        this.graphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
        float leftFlip = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float rightFlip = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        model.setupAnim(0.0F, leftFlip, rightFlip, open);
        VertexConsumer vertexConsumer = this.graphics.bufferSource().getBuffer(model.renderType(texture));
        model.renderToBuffer(this.graphics.pose(), vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY);
        this.graphics.flush();
        this.graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    public void bannerPattern(BannerFlagModel model, DyeColor baseColor, BannerPatternLayers patterns, int x0, int y0, int x1, int y1) {
    }

    public TextureAtlasSprite getSprite(Material material) {
        return material.sprite();
    }

    public void setTooltipForNextFrame(Component tooltip, int mouseX, int mouseY) {
        this.graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> tooltip, int mouseX, int mouseY) {
        this.graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int mouseX, int mouseY) {
        this.graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> component, int mouseX, int mouseY) {
        this.graphics.renderTooltip(font, lines, component, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> component, int mouseX, int mouseY, ResourceLocation texture) {
        this.graphics.renderTooltip(font, lines, component, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY) {
        this.graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY, ResourceLocation texture) {
        this.graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY) {
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY, ResourceLocation texture) {
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    public static final class PoseAdapter {
        private final PoseStack pose;

        private PoseAdapter(PoseStack pose) {
            this.pose = pose;
        }

        public void pushMatrix() {
            this.pose.pushPose();
        }

        public void popMatrix() {
            this.pose.popPose();
        }

        public void translate(float x, float y) {
            this.pose.translate(x, y, 0.0F);
        }

        public void translate(double x, double y) {
            this.pose.translate(x, y, 0.0D);
        }

        public void scale(float x, float y) {
            this.pose.scale(x, y, 1.0F);
        }
    }
}
