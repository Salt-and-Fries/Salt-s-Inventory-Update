package com.salts_inventory_update.client.gui;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.model.BookModel;
import com.salts_inventory_update.client.model.object.banner.BannerFlagModel;
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

import com.salts_inventory_update.SaltsInventoryUpdate;

public final class GuiGraphicsExtractor {
    private static final Set<String> LOGGED_TEXTURE_LOOKUPS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, LegacySprite> LEGACY_SPRITES = createLegacySprites();
    private static final Method INNER_BLIT = findInnerBlit();

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
        return true;
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
        this.graphics.drawString(font, text, x, y, color);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        logTextureLookup("texture", texture);
        this.graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        logTextureLookup("texture", texture);
        this.graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        logTextureLookup("texture", texture);
        this.graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    public void blit(Object pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int color) {
        logTextureLookup("texture", texture);
        this.graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u0, float u1, float v0, float v1) {
        logTextureLookup("texture", texture);
        if (INNER_BLIT != null) {
            try {
                INNER_BLIT.invoke(this.graphics, texture, x, x + width, y, y + height, 0, u0, u1, v0, v1);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        this.graphics.blit(texture, x, y, 0.0F, 0.0F, width, height, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        LegacySprite legacy = legacySprite(sprite);
        if (legacy != null) {
            legacy.blit(this.graphics, x, y, width, height);
            return;
        }
        logTextureLookup("sprite", sprite);
        this.graphics.blit(sprite, x, y, 0, 0, width, height, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height, float alpha) {
        LegacySprite legacy = legacySprite(sprite);
        if (legacy != null) {
            legacy.blit(this.graphics, x, y, width, height);
            return;
        }
        logTextureLookup("sprite", sprite);
        this.graphics.blit(sprite, x, y, 0, 0, width, height, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        LegacySprite legacy = legacySprite(sprite);
        if (legacy != null) {
            legacy.blit(this.graphics, x, y, width, height);
            return;
        }
        logTextureLookup("sprite", sprite);
        this.graphics.blit(sprite, x, y, 0, 0, width, height, width, height);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height) {
        LegacySprite legacy = legacySprite(sprite);
        if (legacy != null) {
            legacy.blit(this.graphics, u, v, x, y, width, height);
            return;
        }
        logTextureLookup("sprite", sprite);
        this.graphics.blit(sprite, x, y, width, height, u, v, width, height, spriteWidth, spriteHeight);
    }

    public void blitSprite(Object pipeline, ResourceLocation sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, int color) {
        LegacySprite legacy = legacySprite(sprite);
        if (legacy != null) {
            legacy.blit(this.graphics, u, v, x, y, width, height);
            return;
        }
        logTextureLookup("sprite", sprite);
        this.graphics.blit(sprite, x, y, width, height, u, v, width, height, spriteWidth, spriteHeight);
    }

    public void blitSprite(Object pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        logAtlasSpriteLookup(sprite);
        this.graphics.blit(x, y, 0, width, height, sprite);
    }

    public void blitSprite(Object pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        logAtlasSpriteLookup(sprite);
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
        this.graphics.renderFakeItem(stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        this.graphics.renderItemDecorations(font, stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y, String text) {
        this.graphics.renderItemDecorations(font, stack, x, y, text);
    }

    public void book(BookModel model, ResourceLocation texture, float scale, float open, float flip, int x0, int y0, int x1, int y1) {
        logTextureLookup("texture", texture);
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
        model.renderToBuffer(this.graphics.pose(), vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.graphics.flush();
        this.graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    public void bannerPattern(BannerFlagModel model, DyeColor baseColor, Object patterns, int x0, int y0, int x1, int y1) {
    }

    public TextureAtlasSprite getSprite(Material material) {
        TextureAtlasSprite sprite = material.sprite();
        logAtlasSpriteLookup(sprite);
        return sprite;
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
        logTextureLookup("tooltip-texture", texture);
        this.graphics.renderTooltip(font, lines, component, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY) {
        this.graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    public void setTooltipForNextFrame(Font font, Component tooltip, int mouseX, int mouseY, ResourceLocation texture) {
        logTextureLookup("tooltip-texture", texture);
        this.graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY) {
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY, ResourceLocation texture) {
        logTextureLookup("tooltip-texture", texture);
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static void logTextureLookup(String kind, ResourceLocation id) {
        String key = kind + ":" + id;
        if (!LOGGED_TEXTURE_LOOKUPS.add(key)) {
            return;
        }

        if (id.getPath().startsWith("textures/atlas/")) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-textures] {} runtime-atlas {}", kind, id);
            return;
        }

        boolean found = resourceExists(id);
        if (found) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-textures] {} found {}", kind, id);
            return;
        }

        if (isTexturePath(id)) {
            SaltsInventoryUpdate.LOGGER.warn("[desktop-textures] {} MISSING {}", kind, id);
            return;
        }

        ResourceLocation guiSpriteTexture = new ResourceLocation(
            id.getNamespace(),
            "textures/gui/sprites/" + id.getPath() + ".png"
        );
        boolean guiSpriteFound = resourceExists(guiSpriteTexture);
        SaltsInventoryUpdate.LOGGER.warn(
            "[desktop-textures] {} MISSING {}; gui-sprite candidate {} {}",
            kind,
            id,
            guiSpriteTexture,
            guiSpriteFound ? "FOUND" : "missing"
        );
    }

    private static void logAtlasSpriteLookup(TextureAtlasSprite sprite) {
        ResourceLocation atlas = sprite.atlasLocation();
        ResourceLocation name = sprite.contents().name();
        String key = "atlas-sprite:" + atlas + ":" + name;
        if (LOGGED_TEXTURE_LOOKUPS.add(key)) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-textures] atlas-sprite found atlas={} sprite={}", atlas, name);
        }
    }

    private static boolean resourceExists(ResourceLocation id) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
        } catch (RuntimeException exception) {
            SaltsInventoryUpdate.LOGGER.warn("[desktop-textures] lookup failed for {}", id, exception);
            return false;
        }
    }

    private static boolean isTexturePath(ResourceLocation id) {
        String path = id.getPath();
        return path.startsWith("textures/") || path.endsWith(".png");
    }

    private static LegacySprite legacySprite(ResourceLocation id) {
        LegacySprite sprite = LEGACY_SPRITES.get(id);
        if (sprite == null) {
            return null;
        }

        String key = "legacy-sprite:" + id;
        if (LOGGED_TEXTURE_LOOKUPS.add(key)) {
            SaltsInventoryUpdate.LOGGER.info(
                "[desktop-textures] sprite legacy {} -> {} @ {},{} {}x{}",
                id,
                sprite.texture,
                sprite.u,
                sprite.v,
                sprite.width,
                sprite.height
            );
        }
        return sprite;
    }

    private static Map<ResourceLocation, LegacySprite> createLegacySprites() {
        Map<ResourceLocation, LegacySprite> sprites = new HashMap<>();

        ResourceLocation widgets = new ResourceLocation("minecraft", "textures/gui/widgets.png");
        ResourceLocation stonecutter = new ResourceLocation("minecraft", "textures/gui/container/stonecutter.png");
        ResourceLocation cartography = new ResourceLocation("minecraft", "textures/gui/container/cartography_table.png");
        ResourceLocation anvil = new ResourceLocation("minecraft", "textures/gui/container/anvil.png");
        ResourceLocation enchanting = new ResourceLocation("minecraft", "textures/gui/container/enchanting_table.png");
        ResourceLocation beacon = new ResourceLocation("minecraft", "textures/gui/container/beacon.png");
        ResourceLocation brewing = new ResourceLocation("minecraft", "textures/gui/container/brewing_stand.png");
        ResourceLocation creativeTabs = new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tabs.png");

        add(sprites, "hud/hotbar_offhand_left", widgets, 24, 22, 29, 24);
        add(sprites, "hud/hotbar_offhand_right", widgets, 53, 22, 29, 24);

        add(sprites, "container/stonecutter/scroller", stonecutter, 176, 0, 12, 15);
        add(sprites, "container/stonecutter/scroller_disabled", stonecutter, 188, 0, 12, 15);
        add(sprites, "container/stonecutter/recipe", stonecutter, 0, 166, 16, 18);
        add(sprites, "container/stonecutter/recipe_selected", stonecutter, 18, 166, 16, 18);
        add(sprites, "container/stonecutter/recipe_highlighted", stonecutter, 36, 166, 16, 18);

        add(sprites, "container/cartography_table/map", cartography, 176, 0, 66, 66);
        add(sprites, "container/cartography_table/scaled_map", cartography, 176, 66, 66, 66);
        add(sprites, "container/cartography_table/duplicated_map", cartography, 176, 132, 50, 66);
        add(sprites, "container/cartography_table/error", cartography, 228, 132, 28, 21);
        add(sprites, "container/cartography_table/locked", cartography, 1, 166, 10, 14);

        add(sprites, "container/anvil/text_field", anvil, 0, 166, 110, 16);
        add(sprites, "container/anvil/text_field_disabled", anvil, 0, 182, 110, 16);
        add(sprites, "container/anvil/error", anvil, 176, 0, 28, 21);

        add(sprites, "container/enchanting_table/enchantment_slot", enchanting, 0, 166, 108, 19);
        add(sprites, "container/enchanting_table/enchantment_slot_disabled", enchanting, 0, 185, 108, 19);
        add(sprites, "container/enchanting_table/enchantment_slot_highlighted", enchanting, 0, 204, 108, 19);
        for (int level = 0; level < 3; level++) {
            add(sprites, "container/enchanting_table/level_" + (level + 1), enchanting, level * 16, 223, 16, 16);
            add(sprites, "container/enchanting_table/level_" + (level + 1) + "_disabled", enchanting, level * 16, 239, 16, 16);
        }

        add(sprites, "container/beacon/button", beacon, 0, 219, 22, 22);
        add(sprites, "container/beacon/button_selected", beacon, 22, 219, 22, 22);
        add(sprites, "container/beacon/button_disabled", beacon, 44, 219, 22, 22);
        add(sprites, "container/beacon/button_highlighted", beacon, 66, 219, 22, 22);
        add(sprites, "container/beacon/confirm", beacon, 90, 220, 18, 18);
        add(sprites, "container/beacon/cancel", beacon, 112, 220, 18, 18);

        add(sprites, "container/brewing_stand/brew_progress", brewing, 176, 0, 9, 28);
        add(sprites, "container/brewing_stand/fuel_length", brewing, 176, 29, 18, 4);
        add(sprites, "container/brewing_stand/bubbles", brewing, 185, 0, 12, 29);

        add(sprites, "container/creative_inventory/scroller", creativeTabs, 232, 0, 12, 15);
        add(sprites, "container/creative_inventory/scroller_disabled", creativeTabs, 244, 0, 12, 15);
        for (int index = 1; index <= 7; index++) {
            int x = (index - 1) * 26;
            add(sprites, "container/creative_inventory/tab_top_unselected_" + index, creativeTabs, x, 0, 26, 32);
            add(sprites, "container/creative_inventory/tab_top_selected_" + index, creativeTabs, x, 32, 26, 32);
            add(sprites, "container/creative_inventory/tab_bottom_unselected_" + index, creativeTabs, x, 64, 26, 32);
            add(sprites, "container/creative_inventory/tab_bottom_selected_" + index, creativeTabs, x, 96, 26, 32);
        }

        return Map.copyOf(sprites);
    }

    private static void add(Map<ResourceLocation, LegacySprite> sprites, String id, ResourceLocation texture, int u, int v, int width, int height) {
        sprites.put(new ResourceLocation("minecraft", id), new LegacySprite(texture, u, v, width, height, 256, 256));
    }

    private static Method findInnerBlit() {
        try {
            Method method = GuiGraphics.class.getDeclaredMethod(
                "innerBlit",
                ResourceLocation.class,
                int.class,
                int.class,
                int.class,
                int.class,
                int.class,
                float.class,
                float.class,
                float.class,
                float.class
            );
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private record LegacySprite(ResourceLocation texture, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        void blit(GuiGraphics graphics, int x, int y, int targetWidth, int targetHeight) {
            graphics.blit(this.texture, x, y, targetWidth, targetHeight, this.u, this.v, this.width, this.height, this.textureWidth, this.textureHeight);
        }

        void blit(GuiGraphics graphics, int sourceX, int sourceY, int x, int y, int targetWidth, int targetHeight) {
            graphics.blit(this.texture, x, y, targetWidth, targetHeight, this.u + sourceX, this.v + sourceY, targetWidth, targetHeight, this.textureWidth, this.textureHeight);
        }
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
