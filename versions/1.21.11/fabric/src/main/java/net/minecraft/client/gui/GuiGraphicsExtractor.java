package net.minecraft.client.gui;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GuiGraphicsExtractor {
    private final GuiGraphics graphics;

    private GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
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

    public Matrix3x2fStack pose() {
        return this.graphics.pose();
    }

    public void nextStratum() {
        this.graphics.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.graphics.blurBeforeThisStratum();
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

    public void fill(RenderPipeline pipeline, int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(pipeline, minX, minY, maxX, maxY, color);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int fromColor, int toColor) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, fromColor, toColor);
    }

    public void fill(RenderPipeline pipeline, TextureSetup textureSetup, int x, int y, int width, int height) {
        this.graphics.fill(pipeline, textureSetup, x, y, width, height);
    }

    public void outline(int x, int y, int width, int height, int color) {
        this.graphics.renderOutline(x, y, width, height, color);
    }

    public void textHighlight(int minX, int minY, int maxX, int maxY, boolean active) {
        this.graphics.textHighlight(minX, minY, maxX, maxY, active);
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
        this.graphics.drawWordWrap(font, text, x, y, width, color, dropShadow);
    }

    public void textWithBackdrop(Font font, Component text, int x, int y, int color, int backgroundColor) {
        this.graphics.drawStringWithBackdrop(font, text, x, y, color, backgroundColor);
    }

    public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        this.graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
    }

    public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(pipeline, texture, x, y, u, v, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int color) {
        this.graphics.blit(pipeline, texture, x, y, u, v, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight, color);
    }

    public void blit(Identifier texture, int x, int y, int width, int height, float u0, float u1, float v0, float v1) {
        this.graphics.blit(texture, x, y, width, height, u0, u1, v0, v1);
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        this.graphics.blitSprite(pipeline, sprite, x, y, width, height);
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, float alpha) {
        this.graphics.blitSprite(pipeline, sprite, x, y, width, height, alpha);
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
        this.graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height) {
        this.graphics.blitSprite(pipeline, sprite, spriteWidth, spriteHeight, u, v, x, y, width, height);
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, int color) {
        this.graphics.blitSprite(pipeline, sprite, spriteWidth, spriteHeight, u, v, x, y, width, height, color);
    }

    public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        this.graphics.blitSprite(pipeline, sprite, x, y, width, height);
    }

    public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        this.graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
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

    public void map(MapRenderState state) {
        this.graphics.submitMapRenderState(state);
    }

    public void entity(EntityRenderState state, float scale, Vector3fc translation, Quaternionfc rotation, Quaternionfc overrideCameraAngle, int x0, int y0, int x1, int y1) {
        this.graphics.submitEntityRenderState(
            state,
            scale,
            new Vector3f(translation),
            rotation == null ? new Quaternionf() : new Quaternionf(rotation),
            overrideCameraAngle == null ? null : new Quaternionf(overrideCameraAngle),
            x0,
            y0,
            x1,
            y1
        );
    }

    public void book(BookModel model, Identifier texture, float scale, float open, float flip, int x0, int y0, int x1, int y1) {
        this.graphics.submitBookModelRenderState(model, texture, scale, open, flip, x0, y0, x1, y1);
    }

    public void bannerPattern(BannerFlagModel model, DyeColor baseColor, BannerPatternLayers patterns, int x0, int y0, int x1, int y1) {
        this.graphics.submitBannerPatternRenderState(model, baseColor, patterns, x0, y0, x1, y1);
    }

    public void setTooltipForNextFrame(Component tooltip, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> tooltip, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> component, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(font, lines, component, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> component, int mouseX, int mouseY, Identifier texture) {
        this.graphics.setTooltipForNextFrame(font, lines, component, mouseX, mouseY, texture);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY, Identifier texture) {
        this.graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY, texture);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY) {
        this.graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY, Identifier texture) {
        this.graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, texture);
    }

    public void tooltip(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner positioner, Identifier texture) {
        this.graphics.renderTooltip(font, components, mouseX, mouseY, positioner, texture);
    }

    public TextureAtlasSprite getSprite(Material material) {
        return this.graphics.getSprite(material);
    }
}
