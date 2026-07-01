package com.salts_inventory_update.client;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.FurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopResizePolicy;
import com.salts_inventory_update.api.client.desktop.DesktopSlotContext;
import com.salts_inventory_update.api.client.desktop.DesktopSlotHit;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowLookupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.desktop.DesktopPayloadCodecs;
import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.mixin.client.MenuScreensAccessor;
import com.salts_inventory_update.mixin.client.RecipeBookComponentAccessor;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class InventoryDesktopScreen extends Screen implements MenuAccess {
    private static final int TOP_BAR_HEIGHT = 16;
    static final int SLOT_SIZE = 18;
    private static final int SLOT_ITEM_SIZE = 16;
    private static final int CONTROL_SIZE = 11;
    private static final int CONTROL_GAP = 3;
    private static final int CONTROL_RIGHT_EXTRA_INSET = 2;
    private static final int CONTROL_TOP_INSET = 4;
    private static final int TITLE_LEFT_PADDING = 6;
    private static final int TITLE_TO_CONTROLS_GAP = 6;
    private static final int CONTROL_POPUP_PADDING = 4;
    private static final int INVENTORY_DEFAULT_COLUMNS = 9;
    private static final int INVENTORY_DEFAULT_VISIBLE_ROWS = 3;
    private static final int INVENTORY_MAX_AUTO_VISIBLE_ROWS = 8;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int OFFHAND_CONTAINER_SLOT = 40;
    private static final int OFFHAND_MENU_SLOT_FALLBACK = 45;
    private static final int OFFHAND_HOTBAR_GAP = 11;
    private static final int WINDOW_CONTENT_PADDING = 8;
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_BACKGROUND_TEXTURE_WIDTH = 14;
    private static final int SCROLLBAR_BACKGROUND_TEXTURE_HEIGHT = 3;
    private static final int SCROLLBAR_THUMB_WIDTH = 12;
    private static final int SCROLLBAR_THUMB_HEIGHT = 15;
    private static final int SCROLLBAR_INSET = 1;
    private static final int SCROLLBAR_RIGHT_INSET = -2;
    private static final int SCROLLBAR_RESERVED_WIDTH = SCROLLBAR_WIDTH - 2;
    private static final int RESIZE_GRIP_SIZE = 9;
    private static final int MIN_CONTAINER_WIDTH = 128;
    private static final int MIN_CONTAINER_HEIGHT = 64;
    private static final int LEGACY_MENU_SESSION = -1;
    private static final Identifier WINDOW_TEXTURE = WindowedInventoryClient.id("textures/gui/window.png");
    private static final Identifier WINDOW_CONTROLS_TEXTURE = WindowedInventoryClient.id("textures/gui/window_controls.png");
    private static final Identifier SLOT_TEXTURE = WindowedInventoryClient.id("textures/gui/slots.png");
    private static final Identifier CONTAINER_WIDGETS_TEXTURE = WindowedInventoryClient.id("textures/gui/container_widgets.png");
    private static final Identifier ANVIL_HAMMER_TEXTURE = WindowedInventoryClient.id("textures/gui/anvil_hammer.png");
    private static final Identifier SMITHING_HAMMER_TEXTURE = WindowedInventoryClient.id("textures/gui/smithing_hammer.png");
    private static final Identifier GRINDSTONE_TEXTURE = WindowedInventoryClient.id("textures/gui/grindstone_sprite.png");
    private static final Identifier STONECUTTER_CONTAINER_TEXTURE = WindowedInventoryClient.id("textures/gui/stonecutter_container.png");
    private static final Identifier LOOM_INPUTS_TEXTURE = WindowedInventoryClient.id("textures/gui/loom_inputs.png");
    private static final Identifier LOOM_OPTIONS_TEXTURE = WindowedInventoryClient.id("textures/gui/loom_options.png");
    private static final Identifier LOOM_PREVIEW_TEXTURE = WindowedInventoryClient.id("textures/gui/loom_preview.png");
    private static final Identifier CRAFTER_SLOT_TEXTURE = WindowedInventoryClient.id("textures/gui/crafter_slot.png");
    private static final Identifier CRAFTER_DISABLED_SLOT_TEXTURE = WindowedInventoryClient.id("textures/gui/crafter_disabled_slot.png");
    private static final Identifier CRAFTER_OUTPUT_DISPLAY_TEXTURE = WindowedInventoryClient.id("textures/gui/crafter_output_display.png");
    private static final Identifier CRAFTER_POWERED_REDSTONE_TEXTURE = WindowedInventoryClient.id("textures/gui/crafter_powered_redstone.png");
    private static final Identifier CRAFTER_UNPOWERED_REDSTONE_TEXTURE = WindowedInventoryClient.id("textures/gui/crafter_unpowered_redstone.png");
    private static final Identifier BEACON_UI_TEXTURE = WindowedInventoryClient.id("textures/gui/beacon_ui.png");
    private static final Identifier BREWING_UI_SLOTS_TEXTURE = WindowedInventoryClient.id("textures/gui/brewing_ui_slots.png");
    private static final Identifier CARTOGRAPHY_PLUS_TEXTURE = WindowedInventoryClient.id("textures/gui/plus.png");
    private static final Identifier LARGE_SLOT_TEXTURE = WindowedInventoryClient.id("textures/gui/large_slot.png");
    private static final Identifier CREATIVE_SEARCH_BAR_TEXTURE = WindowedInventoryClient.id("textures/gui/search_bar.png");
    private static final Identifier CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE = WindowedInventoryClient.id("textures/gui/scroll_bar_behind.png");
    private static final Identifier INCREASE_INVENTORY_BUTTON_TEXTURE = WindowedInventoryClient.id("textures/gui/increase_inventory_button.png");
    private static final Identifier MODEL_DISPLAY_TEXTURE = WindowedInventoryClient.id("textures/gui/3d_model_display.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier INVENTORY_EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
    private static final Identifier INVENTORY_EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
    private static final int WINDOW_TEXTURE_SIZE = 11;
    private static final int WINDOW_EDGE_SIZE = 5;
    private static final int CONTROL_TEXTURE_WIDTH = CONTROL_SIZE * 9;
    private static final int CONTROL_TEXTURE_HEIGHT = CONTROL_SIZE * 3;
    private static final int SLOT_TEXTURE_WIDTH = SLOT_SIZE * 2;
    private static final int SLOT_TEXTURE_HEIGHT = SLOT_SIZE;
    private static final int CONTAINER_WIDGETS_TEXTURE_WIDTH = 48;
    private static final int CONTAINER_WIDGETS_TEXTURE_HEIGHT = 30;
    private static final int ANVIL_HAMMER_TEXTURE_SIZE = 30;
    private static final int SMITHING_HAMMER_TEXTURE_WIDTH = 30;
    private static final int SMITHING_HAMMER_TEXTURE_HEIGHT = 31;
    private static final int GRINDSTONE_TEXTURE_WIDTH = 54;
    private static final int GRINDSTONE_TEXTURE_HEIGHT = 56;
    private static final int STONECUTTER_CONTAINER_TEXTURE_WIDTH = 81;
    private static final int STONECUTTER_CONTAINER_TEXTURE_HEIGHT = 56;
    private static final int LOOM_INPUTS_TEXTURE_WIDTH = 50;
    private static final int LOOM_INPUTS_TEXTURE_HEIGHT = 55;
    private static final int LOOM_OPTIONS_TEXTURE_WIDTH = 73;
    private static final int LOOM_OPTIONS_TEXTURE_HEIGHT = 58;
    private static final int LOOM_PREVIEW_TEXTURE_WIDTH = 20;
    private static final int LOOM_PREVIEW_TEXTURE_HEIGHT = 40;
    private static final int CRAFTER_SLOT_TEXTURE_SIZE = 18;
    private static final int CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE = 26;
    private static final int CRAFTER_REDSTONE_TEXTURE_SIZE = 16;
    private static final int BEACON_UI_TEXTURE_WIDTH = 216;
    private static final int BEACON_UI_TEXTURE_HEIGHT = 123;
    private static final int BREWING_UI_SLOTS_TEXTURE_WIDTH = 103;
    private static final int BREWING_UI_SLOTS_TEXTURE_HEIGHT = 60;
    private static final int CARTOGRAPHY_PLUS_TEXTURE_SIZE = 13;
    private static final int LARGE_SLOT_TEXTURE_SIZE = 26;
    private static final int LARGE_SLOT_ITEM_OFFSET = 5;
    private static final int INCREASE_INVENTORY_BUTTON_TEXTURE_WIDTH = 36;
    private static final int INCREASE_INVENTORY_BUTTON_TEXTURE_HEIGHT = 18;
    private static final int INCREASE_INVENTORY_BUTTON_FRAME_SIZE = 18;
    private static final int MODEL_DISPLAY_TEXTURE_SIZE = 3;
    private static final int SLOT_HIGHLIGHT_SIZE = 24;
    private static final int SLOT_HIGHLIGHT_OFFSET = 4;
    private static final int WIDGET_FLAME_EMPTY_X = 0;
    private static final int WIDGET_FLAME_EMPTY_Y = 0;
    private static final int WIDGET_FLAME_FULL_X = 14;
    private static final int WIDGET_FLAME_FULL_Y = 0;
    private static final int WIDGET_FLAME_WIDTH = 14;
    private static final int WIDGET_FLAME_HEIGHT = 14;
    private static final int WIDGET_ARROW_EMPTY_X = 0;
    private static final int WIDGET_ARROW_EMPTY_Y = 14;
    private static final int WIDGET_ARROW_FULL_X = 24;
    private static final int WIDGET_ARROW_FULL_Y = 14;
    private static final int WIDGET_ARROW_WIDTH = 24;
    private static final int WIDGET_ARROW_HEIGHT = 16;
    private static final int RECIPE_BOOK_WIDTH = 147;
    private static final int RECIPE_BOOK_HEIGHT = 166;
    private static final int RECIPE_BOOK_TAB_LEFT_OVERHANG = 30;
    private static final int RECIPE_BOOK_BUTTON_WIDTH = 20;
    private static final int RECIPE_BOOK_BUTTON_HEIGHT = 18;
    private static final int RECIPE_BOOK_GAP = 4;
    private static final int CHARACTER_WINDOW_WIDTH = 180;
    private static final int CHARACTER_WINDOW_HEIGHT = 136;
    private static final int CHARACTER_CONTENT_MARGIN = 6;
    private static final int CHARACTER_ARMOR_X = 0;
    private static final int CHARACTER_ARMOR_Y = 0;
    private static final int CHARACTER_MODEL_X = 22;
    private static final int CHARACTER_MODEL_Y = 0;
    private static final int CHARACTER_MODEL_WIDTH = 50;
    private static final int CHARACTER_MODEL_HEIGHT = 72;
    private static final int CHARACTER_MODEL_SCALE = 28;
    private static final int CHARACTER_STATS_X = 0;
    private static final int CHARACTER_STATS_Y = 78;
    private static final int CHARACTER_STATS_VALUE_X = 54;
    private static final int CHARACTER_STATS_LINE_HEIGHT = 10;
    private static final int CHARACTER_CRAFT_X = 88;
    private static final int CHARACTER_CRAFT_Y = 22;
    private static final int CHARACTER_CRAFT_ARROW_X = 125;
    private static final int CHARACTER_CRAFT_ARROW_Y = 31;
    private static final int CHARACTER_CRAFT_RESULT_X = 150;
    private static final int CHARACTER_CRAFT_RESULT_Y = 31;
    private static final int CHARACTER_RECIPE_BUTTON_X = CHARACTER_CRAFT_ARROW_X + 1;
    private static final int CHARACTER_RECIPE_BUTTON_Y = CHARACTER_CRAFT_RESULT_Y + SLOT_SIZE + 17;
    private static final int CHARACTER_EFFECT_GAP = 4;
    private static final int CHARACTER_EFFECT_WIDTH = 120;
    private static final int CHARACTER_EFFECT_HEIGHT = 32;
    private static final int CHARACTER_EFFECT_ICON_SIZE = 18;
    private static final int CHARACTER_EFFECT_ICON_X = 7;
    private static final int CHARACTER_EFFECT_ICON_Y = 7;
    private static final int CHARACTER_EFFECT_TEXT_X = 32;
    private static final int CHARACTER_EFFECT_NAME_Y = 7;
    private static final int CHARACTER_EFFECT_DURATION_Y = 17;
    private static final int CHARACTER_EFFECT_NAME_COLOR = 0xFFFFFFFF;
    private static final int CHARACTER_EFFECT_DURATION_COLOR = 0xFF7F7F7F;
    private static final int MOUNT_SADDLE_SLOT = 0;
    private static final int MOUNT_BODY_SLOT = 1;
    private static final int MOUNT_STORAGE_START_SLOT = 2;
    private static final int MOUNT_STORAGE_ROWS = 3;
    private static final int MOUNT_EQUIPMENT_X = 0;
    private static final int MOUNT_EQUIPMENT_Y = 9;
    private static final int MOUNT_MODEL_X = 26;
    private static final int MOUNT_MODEL_Y = 0;
    private static final int MOUNT_MODEL_WIDTH = 70;
    private static final int MOUNT_MODEL_HEIGHT = 54;
    private static final int MOUNT_MODEL_SCALE = 17;
    private static final float MOUNT_MODEL_MOUSE_SCALE = 0.25F;
    private static final int MOUNT_STORAGE_GAP = 8;
    private static final int MOUNT_STORAGE_X = MOUNT_MODEL_X + MOUNT_MODEL_WIDTH + MOUNT_STORAGE_GAP;
    private static final int MOUNT_STORAGE_Y = 0;
    private static final int MOUNT_CONTENT_HEIGHT = Math.max(MOUNT_MODEL_HEIGHT, MOUNT_STORAGE_ROWS * SLOT_SIZE);
    private static final int FURNACE_CONTENT_MARGIN = 6;
    private static final int FURNACE_CONTENT_WIDTH = 90;
    private static final int FURNACE_CONTENT_HEIGHT = 58;
    private static final int FURNACE_INPUT_SLOT = 0;
    private static final int FURNACE_FUEL_SLOT = 1;
    private static final int FURNACE_RESULT_SLOT = 2;
    private static final int FURNACE_INPUT_X = 0;
    private static final int FURNACE_INPUT_Y = 0;
    private static final int FURNACE_FUEL_X = 0;
    private static final int FURNACE_FUEL_Y = 40;
    private static final int FURNACE_RESULT_X = 72;
    private static final int FURNACE_RESULT_Y = 22;
    private static final int FURNACE_FLAME_X = 0;
    private static final int FURNACE_FLAME_Y = 22;
    private static final int FURNACE_ARROW_X = 30;
    private static final int FURNACE_ARROW_Y = 21;
    private static final int FURNACE_RECIPE_BUTTON_X = 30;
    private static final int FURNACE_RECIPE_BUTTON_Y = 40;
    private static final int CRAFTING_TABLE_CONTENT_MARGIN = 6;
    private static final int CRAFTING_TABLE_CONTENT_WIDTH = 146;
    private static final int CRAFTING_TABLE_CONTENT_HEIGHT = 54;
    private static final int CRAFTING_TABLE_GRID_COLUMNS = 3;
    private static final int CRAFTING_TABLE_GRID_ROWS = 3;
    private static final int CRAFTING_TABLE_GRID_X = 0;
    private static final int CRAFTING_TABLE_GRID_Y = 0;
    private static final int CRAFTING_TABLE_ARROW_X = 66;
    private static final int CRAFTING_TABLE_ARROW_Y = 19;
    private static final int CRAFTING_TABLE_RESULT_X = 104;
    private static final int CRAFTING_TABLE_RESULT_Y = 18;
    private static final int CRAFTING_TABLE_RECIPE_BUTTON_X = CRAFTING_TABLE_RESULT_X + SLOT_SIZE + 4;
    private static final int CRAFTING_TABLE_RECIPE_BUTTON_Y = CRAFTING_TABLE_RESULT_Y - 1;
    private static final int ANVIL_CONTENT_MARGIN = 6;
    private static final int ANVIL_CONTENT_WIDTH = 158;
    private static final int ANVIL_CONTENT_HEIGHT = 70;
    private static final int ANVIL_INPUT_SLOT = 0;
    private static final int ANVIL_ADDITIONAL_SLOT = 1;
    private static final int ANVIL_RESULT_SLOT = 2;
    private static final int ANVIL_HAMMER_X = 0;
    private static final int ANVIL_HAMMER_Y = 0;
    private static final int ANVIL_TEXT_FIELD_X = 48;
    private static final int ANVIL_TEXT_FIELD_Y = 8;
    private static final int ANVIL_TEXT_FIELD_WIDTH = 110;
    private static final int ANVIL_TEXT_FIELD_HEIGHT = 16;
    private static final int ANVIL_TEXT_X = ANVIL_TEXT_FIELD_X + 4;
    private static final int ANVIL_TEXT_Y = ANVIL_TEXT_FIELD_Y + 4;
    private static final int ANVIL_MAX_NAME_LENGTH = 50;
    private static final int ANVIL_INPUT_SLOT_X = 0;
    private static final int ANVIL_ADDITIONAL_SLOT_X = 58;
    private static final int ANVIL_RESULT_SLOT_X = 124;
    private static final int ANVIL_SLOT_Y = 50;
    private static final int ANVIL_PLUS_X = 33;
    private static final int ANVIL_PLUS_Y = 53;
    private static final int ANVIL_ARROW_X = 89;
    private static final int ANVIL_ARROW_Y = 51;
    private static final int ANVIL_ERROR_X = 86;
    private static final int ANVIL_ERROR_Y = 48;
    private static final int ANVIL_ERROR_WIDTH = 28;
    private static final int ANVIL_ERROR_HEIGHT = 21;
    private static final int ANVIL_COST_Y = 38;
    private static final int SMITHING_CONTENT_MARGIN = 6;
    private static final int SMITHING_CONTENT_WIDTH = 168;
    private static final int SMITHING_CONTENT_HEIGHT = 66;
    private static final int SMITHING_HAMMER_X = 0;
    private static final int SMITHING_HAMMER_Y = 0;
    private static final int SMITHING_LABEL_X = 40;
    private static final int SMITHING_LABEL_Y = 16;
    private static final int SMITHING_TEMPLATE_SLOT = 0;
    private static final int SMITHING_BASE_SLOT = 1;
    private static final int SMITHING_ADDITION_SLOT = 2;
    private static final int SMITHING_RESULT_SLOT = 3;
    private static final int SMITHING_TEMPLATE_SLOT_X = 8;
    private static final int SMITHING_BASE_SLOT_X = 26;
    private static final int SMITHING_ADDITION_SLOT_X = 44;
    private static final int SMITHING_RESULT_SLOT_X = 98;
    private static final int SMITHING_SLOT_Y = 48;
    private static final int SMITHING_ARROW_X = 67;
    private static final int SMITHING_ARROW_Y = 48;
    private static final int SMITHING_PREVIEW_LEFT = 126;
    private static final int SMITHING_PREVIEW_TOP = 2;
    private static final int SMITHING_PREVIEW_RIGHT = 166;
    private static final int SMITHING_PREVIEW_BOTTOM = 66;
    private static final int GRINDSTONE_CONTENT_MARGIN = 6;
    private static final int GRINDSTONE_CONTENT_WIDTH = 122;
    private static final int GRINDSTONE_CONTENT_HEIGHT = 56;
    private static final int GRINDSTONE_INPUT_SLOT = 0;
    private static final int GRINDSTONE_ADDITIONAL_SLOT = 1;
    private static final int GRINDSTONE_RESULT_SLOT = 2;
    private static final int GRINDSTONE_SPRITE_X = 0;
    private static final int GRINDSTONE_SPRITE_Y = 0;
    private static final int GRINDSTONE_INPUT_SLOT_X = 19;
    private static final int GRINDSTONE_INPUT_SLOT_Y = 4;
    private static final int GRINDSTONE_ADDITIONAL_SLOT_X = 19;
    private static final int GRINDSTONE_ADDITIONAL_SLOT_Y = 24;
    private static final int GRINDSTONE_ARROW_X = 68;
    private static final int GRINDSTONE_ARROW_Y = 21;
    private static final int GRINDSTONE_RESULT_SLOT_X = 104;
    private static final int GRINDSTONE_RESULT_SLOT_Y = 21;
    private static final int STONECUTTER_CONTENT_MARGIN = 6;
    private static final int STONECUTTER_CONTENT_WIDTH = 150;
    private static final int STONECUTTER_CONTENT_HEIGHT = 56;
    private static final int STONECUTTER_INPUT_SLOT = 0;
    private static final int STONECUTTER_RESULT_SLOT = 1;
    private static final int STONECUTTER_INPUT_SLOT_X = 0;
    private static final int STONECUTTER_INPUT_SLOT_Y = 19;
    private static final int STONECUTTER_PANEL_X = 32;
    private static final int STONECUTTER_PANEL_Y = 0;
    private static final int STONECUTTER_RECIPE_GRID_X = STONECUTTER_PANEL_X + 1;
    private static final int STONECUTTER_RECIPE_GRID_Y = STONECUTTER_PANEL_Y + 1;
    private static final int STONECUTTER_RECIPE_COLUMNS = 4;
    private static final int STONECUTTER_RECIPE_ROWS = 3;
    private static final int STONECUTTER_RECIPE_BUTTON_WIDTH = 16;
    private static final int STONECUTTER_RECIPE_BUTTON_HEIGHT = 18;
    private static final int STONECUTTER_VISIBLE_RECIPES = STONECUTTER_RECIPE_COLUMNS * STONECUTTER_RECIPE_ROWS;
    private static final int STONECUTTER_SCROLLBAR_X = STONECUTTER_PANEL_X + 68;
    private static final int STONECUTTER_SCROLLBAR_Y = STONECUTTER_PANEL_Y + 1;
    private static final int STONECUTTER_SCROLLBAR_WIDTH = 12;
    private static final int STONECUTTER_SCROLLBAR_HEIGHT = 15;
    private static final int STONECUTTER_SCROLLBAR_TRAVEL = 41;
    private static final int STONECUTTER_RESULT_SLOT_X = 124;
    private static final int STONECUTTER_RESULT_SLOT_Y = 15;
    private static final int LOOM_CONTENT_MARGIN = 6;
    private static final int LOOM_CONTENT_WIDTH = 170;
    private static final int LOOM_CONTENT_HEIGHT = 69;
    private static final int LOOM_BANNER_SLOT = 0;
    private static final int LOOM_DYE_SLOT = 1;
    private static final int LOOM_PATTERN_SLOT = 2;
    private static final int LOOM_RESULT_SLOT = 3;
    private static final int LOOM_INPUTS_X = 0;
    private static final int LOOM_INPUTS_Y = 4;
    private static final int LOOM_BANNER_SLOT_X = LOOM_INPUTS_X + 7;
    private static final int LOOM_BANNER_SLOT_Y = LOOM_INPUTS_Y + 12;
    private static final int LOOM_DYE_SLOT_X = LOOM_INPUTS_X + 27;
    private static final int LOOM_DYE_SLOT_Y = LOOM_INPUTS_Y + 12;
    private static final int LOOM_PATTERN_SLOT_X = LOOM_INPUTS_X + 17;
    private static final int LOOM_PATTERN_SLOT_Y = LOOM_INPUTS_Y + 31;
    private static final int LOOM_OPTIONS_X = 58;
    private static final int LOOM_OPTIONS_Y = 2;
    private static final int LOOM_PATTERN_GRID_X = LOOM_OPTIONS_X + 1;
    private static final int LOOM_PATTERN_GRID_Y = LOOM_OPTIONS_Y + 1;
    private static final int LOOM_PATTERN_COLUMNS = 4;
    private static final int LOOM_PATTERN_ROWS = 4;
    private static final int LOOM_PATTERN_BUTTON_SIZE = 14;
    private static final int LOOM_VISIBLE_PATTERNS = LOOM_PATTERN_COLUMNS * LOOM_PATTERN_ROWS;
    private static final int LOOM_SCROLLBAR_X = LOOM_OPTIONS_X + 60;
    private static final int LOOM_SCROLLBAR_Y = LOOM_OPTIONS_Y + 1;
    private static final int LOOM_SCROLLBAR_WIDTH = 12;
    private static final int LOOM_SCROLLBAR_HEIGHT = 15;
    private static final int LOOM_SCROLLBAR_TRAVEL = 41;
    private static final int LOOM_PREVIEW_X = 142;
    private static final int LOOM_PREVIEW_Y = -6;
    private static final int LOOM_RESULT_SLOT_X = 139;
    private static final int LOOM_RESULT_SLOT_Y = 43;
    private static final int CRAFTER_CONTENT_MARGIN = 6;
    private static final int CRAFTER_CONTENT_WIDTH = 134;
    private static final int CRAFTER_CONTENT_HEIGHT = 54;
    private static final int CRAFTER_INPUT_SLOT_COUNT = 9;
    private static final int CRAFTER_GRID_COLUMNS = 3;
    private static final int CRAFTER_GRID_ROWS = 3;
    private static final int CRAFTER_GRID_X = 0;
    private static final int CRAFTER_GRID_Y = 0;
    private static final int CRAFTER_REDSTONE_X = 76;
    private static final int CRAFTER_REDSTONE_Y = 19;
    private static final int CRAFTER_OUTPUT_X = 108;
    private static final int CRAFTER_OUTPUT_Y = 14;
    private static final int CRAFTER_OUTPUT_ITEM_OFFSET = 5;
    private static final int CRAFTER_SLOT_STATE_ENABLED_FLAG = 16;
    private static final int BEACON_CONTENT_MARGIN = 6;
    private static final int BEACON_CONTENT_WIDTH = BEACON_UI_TEXTURE_WIDTH;
    private static final int BEACON_CONTENT_HEIGHT = BEACON_UI_TEXTURE_HEIGHT;
    private static final int BEACON_BUTTON_SIZE = 22;
    private static final int BEACON_BUTTON_ICON_OFFSET = 2;
    private static final int BEACON_BUTTON_ICON_SIZE = 18;
    private static final int BEACON_PRIMARY_LABEL_CENTER_X = 54;
    private static final int BEACON_SECONDARY_LABEL_CENTER_X = 162;
    private static final int BEACON_LABEL_Y = 5;
    private static final int BEACON_PRIMARY_BUTTON_BASE_X = 46;
    private static final int BEACON_PRIMARY_BUTTON_BASE_Y = 18;
    private static final int BEACON_BUTTON_GAP = 2;
    private static final int BEACON_BUTTON_STEP_X = BEACON_BUTTON_SIZE + BEACON_BUTTON_GAP;
    private static final int BEACON_BUTTON_STEP_Y = 25;
    private static final int BEACON_SECONDARY_BUTTON_X = 139;
    private static final int BEACON_UPGRADE_BUTTON_X = 163;
    private static final int BEACON_SECONDARY_BUTTON_Y = 43;
    private static final int BEACON_PAYMENT_SLOT = 0;
    private static final int BEACON_PAYMENT_SLOT_X = 128;
    private static final int BEACON_PAYMENT_SLOT_Y = 104;
    private static final int BEACON_MATERIALS_Y = 106;
    private static final int[] BEACON_MATERIALS_X = {13, 34, 56, 79, 101};
    private static final int BEACON_CONFIRM_X = 162;
    private static final int BEACON_CONFIRM_Y = 101;
    private static final int BEACON_CANCEL_X = 188;
    private static final int BEACON_CANCEL_Y = 101;
    private static final int BEACON_EFFECT_ID_MASK = 0xFFFF;
    private static final int BEACON_SECONDARY_EFFECT_SHIFT = 16;
    private static final int BREWING_CONTENT_MARGIN = 6;
    private static final int BREWING_CONTENT_WIDTH = BREWING_UI_SLOTS_TEXTURE_WIDTH;
    private static final int BREWING_CONTENT_HEIGHT = BREWING_UI_SLOTS_TEXTURE_HEIGHT;
    private static final int BREWING_BOTTLE_0_SLOT = 0;
    private static final int BREWING_BOTTLE_1_SLOT = 1;
    private static final int BREWING_BOTTLE_2_SLOT = 2;
    private static final int BREWING_INGREDIENT_SLOT = 3;
    private static final int BREWING_FUEL_SLOT = 4;
    private static final int BREWING_FUEL_SLOT_X = 1;
    private static final int BREWING_FUEL_SLOT_Y = 2;
    private static final int BREWING_INGREDIENT_SLOT_X = 63;
    private static final int BREWING_INGREDIENT_SLOT_Y = 2;
    private static final int BREWING_BOTTLE_0_SLOT_X = 40;
    private static final int BREWING_BOTTLE_0_SLOT_Y = 36;
    private static final int BREWING_BOTTLE_1_SLOT_X = 63;
    private static final int BREWING_BOTTLE_1_SLOT_Y = 43;
    private static final int BREWING_BOTTLE_2_SLOT_X = 86;
    private static final int BREWING_BOTTLE_2_SLOT_Y = 36;
    private static final int BREWING_FUEL_LENGTH_X = 44;
    private static final int BREWING_FUEL_LENGTH_Y = 29;
    private static final int BREWING_FUEL_LENGTH_WIDTH = 18;
    private static final int BREWING_FUEL_LENGTH_HEIGHT = 4;
    private static final int BREWING_PROGRESS_X = 81;
    private static final int BREWING_PROGRESS_Y = 1;
    private static final int BREWING_PROGRESS_WIDTH = 9;
    private static final int BREWING_PROGRESS_HEIGHT = 28;
    private static final int BREWING_BUBBLES_X = 47;
    private static final int BREWING_BUBBLES_BASE_Y = 28;
    private static final int BREWING_BUBBLES_WIDTH = 12;
    private static final int BREWING_BUBBLES_HEIGHT = 29;
    private static final int[] BREWING_BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};
    private static final int CARTOGRAPHY_CONTENT_MARGIN = 6;
    private static final int CARTOGRAPHY_CONTENT_WIDTH = 174;
    private static final int CARTOGRAPHY_CONTENT_HEIGHT = 66;
    private static final int CARTOGRAPHY_MAP_SLOT = 0;
    private static final int CARTOGRAPHY_ADDITIONAL_SLOT = 1;
    private static final int CARTOGRAPHY_RESULT_SLOT = 2;
    private static final int CARTOGRAPHY_MAP_SLOT_X = 0;
    private static final int CARTOGRAPHY_MAP_SLOT_Y = 1;
    private static final int CARTOGRAPHY_ADDITIONAL_SLOT_X = 0;
    private static final int CARTOGRAPHY_ADDITIONAL_SLOT_Y = 39;
    private static final int CARTOGRAPHY_PREVIEW_X = 68;
    private static final int CARTOGRAPHY_PREVIEW_Y = 0;
    private static final int CARTOGRAPHY_PREVIEW_SIZE = 66;
    private static final int CARTOGRAPHY_INPUT_COLUMN_RIGHT = CARTOGRAPHY_MAP_SLOT_X - 1 + SLOT_SIZE;
    private static final int CARTOGRAPHY_INPUT_GAP_TOP = CARTOGRAPHY_MAP_SLOT_Y - 1 + SLOT_SIZE;
    private static final int CARTOGRAPHY_INPUT_GAP_BOTTOM = CARTOGRAPHY_ADDITIONAL_SLOT_Y - 1;
    private static final int CARTOGRAPHY_PLUS_X = CARTOGRAPHY_MAP_SLOT_X - 1 + (SLOT_SIZE - CARTOGRAPHY_PLUS_TEXTURE_SIZE) / 2;
    private static final int CARTOGRAPHY_PLUS_Y = CARTOGRAPHY_INPUT_GAP_TOP + (CARTOGRAPHY_INPUT_GAP_BOTTOM - CARTOGRAPHY_INPUT_GAP_TOP - CARTOGRAPHY_PLUS_TEXTURE_SIZE) / 2;
    private static final int CARTOGRAPHY_ARROW_X = CARTOGRAPHY_INPUT_COLUMN_RIGHT + (CARTOGRAPHY_PREVIEW_X - CARTOGRAPHY_INPUT_COLUMN_RIGHT - WIDGET_ARROW_WIDTH) / 2;
    private static final int CARTOGRAPHY_ARROW_Y = CARTOGRAPHY_INPUT_GAP_TOP + (CARTOGRAPHY_INPUT_GAP_BOTTOM - CARTOGRAPHY_INPUT_GAP_TOP - WIDGET_ARROW_HEIGHT) / 2;
    private static final int CARTOGRAPHY_RESULT_SLOT_X = 156;
    private static final int CARTOGRAPHY_RESULT_SLOT_Y = 24;
    private static final int CARTOGRAPHY_ERROR_X = 35;
    private static final int CARTOGRAPHY_ERROR_Y = 25;
    private static final int CARTOGRAPHY_ERROR_WIDTH = 28;
    private static final int CARTOGRAPHY_ERROR_HEIGHT = 21;
    private static final int CARTOGRAPHY_DUPLICATED_WIDTH = 50;
    private static final int CARTOGRAPHY_LOCKED_WIDTH = 10;
    private static final int CARTOGRAPHY_LOCKED_HEIGHT = 14;
    private static final int ENCHANTMENT_CONTENT_MARGIN = 6;
    private static final int ENCHANTMENT_CONTENT_WIDTH = 154;
    private static final int ENCHANTMENT_CONTENT_HEIGHT = 58;
    private static final int ENCHANTMENT_ITEM_SLOT = 0;
    private static final int ENCHANTMENT_LAPIS_SLOT = 1;
    private static final int ENCHANTMENT_ITEM_SLOT_X = 1;
    private static final int ENCHANTMENT_ITEM_SLOT_Y = 33;
    private static final int ENCHANTMENT_LAPIS_SLOT_X = 21;
    private static final int ENCHANTMENT_LAPIS_SLOT_Y = 33;
    private static final int ENCHANTMENT_BOOK_X = 0;
    private static final int ENCHANTMENT_BOOK_Y = 0;
    private static final int ENCHANTMENT_BOOK_WIDTH = 38;
    private static final int ENCHANTMENT_BOOK_HEIGHT = 31;
    private static final int ENCHANTMENT_BUTTON_X = 46;
    private static final int ENCHANTMENT_BUTTON_Y = 0;
    private static final int ENCHANTMENT_BUTTON_WIDTH = 108;
    private static final int ENCHANTMENT_BUTTON_HEIGHT = 19;
    private static final int MERCHANT_CONTENT_MARGIN = 6;
    private static final int MERCHANT_CONTENT_WIDTH = 236;
    private static final int MERCHANT_CONTENT_HEIGHT = 80;
    private static final int MERCHANT_VISIBLE_TRADES = 4;
    private static final int MERCHANT_TRADE_ROW_HEIGHT = 20;
    private static final int MERCHANT_TRADE_LIST_X = 0;
    private static final int MERCHANT_TRADE_LIST_Y = 0;
    private static final int MERCHANT_TRADE_LIST_WIDTH = 112;
    private static final int MERCHANT_TRADE_SCROLLBAR_X = 116;
    private static final int MERCHANT_TRADE_SCROLLBAR_WIDTH = 6;
    private static final int MERCHANT_PAYMENT_1_SLOT = 0;
    private static final int MERCHANT_PAYMENT_2_SLOT = 1;
    private static final int MERCHANT_RESULT_SLOT = 2;
    private static final int MERCHANT_PAYMENT_1_X = 135;
    private static final int MERCHANT_PAYMENT_2_X = 161;
    private static final int MERCHANT_RESULT_X = 213;
    private static final int MERCHANT_SLOT_Y = 45;
    private static final int MERCHANT_TRADE_ARROW_X = 183;
    private static final int MERCHANT_TRADE_ARROW_Y = 45;
    private static final int MERCHANT_TRADE_ARROW_WIDTH = 10;
    private static final int MERCHANT_TRADE_ARROW_HEIGHT = 9;
    private static final int MERCHANT_OUT_OF_STOCK_WIDTH = 28;
    private static final int MERCHANT_OUT_OF_STOCK_HEIGHT = 21;
    private static final int MERCHANT_DETAIL_LABEL_Y = 5;
    private static final int MERCHANT_PROGRESS_X = 134;
    private static final int MERCHANT_PROGRESS_Y = 22;
    private static final int MERCHANT_PROGRESS_WIDTH = 96;
    private static final int MERCHANT_PROGRESS_HEIGHT = 5;
    private static final int CREATIVE_CONTENT_MARGIN = 6;
    private static final int CREATIVE_TAB_WIDTH = 26;
    private static final int CREATIVE_TAB_HEIGHT = 32;
    private static final int CREATIVE_TAB_FRAME_OVERLAP = 5;
    private static final int CREATIVE_TAB_X_OFFSET = -6;
    private static final int CREATIVE_TOP_TAB_Y_OFFSET = -1;
    private static final int CREATIVE_BOTTOM_TAB_Y_OFFSET = 1;
    private static final int CREATIVE_TABS_PER_ROW = 7;
    private static final int CREATIVE_TABS_PER_PAGE = CREATIVE_TABS_PER_ROW * 2;
    private static final int CREATIVE_TAB_PAGE_BUTTON_WIDTH = 10;
    private static final int CREATIVE_TAB_PAGE_BUTTON_HEIGHT = 12;
    private static final int CREATIVE_TAB_PAGE_BUTTON_GAP = 0;
    private static final int CREATIVE_TAB_PAGE_BUTTON_TITLE_GAP = 5;
    private static final int CREATIVE_TAB_PAGE_BUTTON_Y_OFFSET = 4;
    private static final int CREATIVE_PANEL_WIDTH = 184;
    private static final int CREATIVE_PANEL_HEIGHT = 96;
    private static final int CREATIVE_CONTENT_WIDTH = CREATIVE_PANEL_WIDTH;
    private static final int CREATIVE_CONTENT_HEIGHT = CREATIVE_PANEL_HEIGHT;
    private static final int CREATIVE_GRID_COLUMNS = 9;
    private static final int CREATIVE_GRID_ROWS = 5;
    private static final int CREATIVE_GRID_X = 4;
    private static final int CREATIVE_GRID_Y = 4;
    private static final int CREATIVE_INVENTORY_GRID_Y = 4;
    private static final int CREATIVE_SEARCH_TITLE_GAP = 8;
    private static final int CREATIVE_SEARCH_CONTROLS_GAP = 6;
    private static final int CREATIVE_SEARCH_MIN_WIDTH = 24;
    private static final int CREATIVE_SEARCH_HEIGHT = 12;
    private static final int CREATIVE_SEARCH_TEXTURE_WIDTH = 3;
    private static final int CREATIVE_SEARCH_TEXTURE_HEIGHT = 12;
    private static final int CREATIVE_SCROLLBAR_X = 167;
    private static final int CREATIVE_SCROLLBAR_Y = CREATIVE_GRID_Y - 1;
    private static final int CREATIVE_SCROLLBAR_BACKGROUND_WIDTH = 14;
    private static final int CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_WIDTH = 14;
    private static final int CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_HEIGHT = 3;
    private static final int CREATIVE_SCROLLBAR_WIDTH = 12;
    private static final int CREATIVE_SCROLLBAR_HEIGHT = 15;
    private static final int CREATIVE_SCROLLBAR_INSET = 1;
    private static final int CREATIVE_SCROLLBAR_BACKGROUND_HEIGHT = CREATIVE_GRID_ROWS * SLOT_SIZE;
    private static final int CREATIVE_SCROLLBAR_TRACK_HEIGHT = CREATIVE_SCROLLBAR_BACKGROUND_HEIGHT - CREATIVE_SCROLLBAR_INSET * 2 - CREATIVE_SCROLLBAR_HEIGHT;
    private static final int CREATIVE_DELETE_SLOT_X = 167;
    private static final int CREATIVE_DELETE_SLOT_Y = CREATIVE_INVENTORY_GRID_Y;
    private static final int CREATIVE_INVENTORY_SCROLLBAR_X = CREATIVE_SCROLLBAR_X;
    private static final int CREATIVE_INVENTORY_SCROLLBAR_Y = CREATIVE_DELETE_SLOT_Y + SLOT_SIZE + 4;
    private static final int CREATIVE_INVENTORY_SCROLLBAR_BACKGROUND_HEIGHT = CREATIVE_PANEL_HEIGHT - CREATIVE_INVENTORY_SCROLLBAR_Y - 3;
    private static final int CREATIVE_TAB_ICON_OFFSET_X = 5;
    private static final int CREATIVE_TAB_ICON_OFFSET_TOP = 9;
    private static final int CREATIVE_TAB_ICON_OFFSET_BOTTOM = 7;
    private static final int CREATIVE_PICKED_STACK_SIZE = 64;
    private static final int CREATIVE_DROP_SLOT = -1;
    private static final Component CREATIVE_TITLE = Component.literal("Creative");
    private static final Component CREATIVE_DELETE_TOOLTIP = Component.translatable("inventory.binSlot");
    private static final Identifier MERCHANT_TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
    private static final Identifier MERCHANT_TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
    private static final Identifier MERCHANT_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/out_of_stock");
    private static final Identifier MERCHANT_SCROLLBAR_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier MERCHANT_SCROLLBAR_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
    private static final Identifier MERCHANT_DISCOUNT_STRIKETHROUGH_SPRITE = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");
    private static final Identifier SMITHING_TEMPLATE_ARMOR_TRIM_SPRITE = Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim");
    private static final Identifier SMITHING_TEMPLATE_NETHERITE_UPGRADE_SPRITE = Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade");
    private static final List<Identifier> SMITHING_TEMPLATE_SPRITES = List.of(SMITHING_TEMPLATE_ARMOR_TRIM_SPRITE, SMITHING_TEMPLATE_NETHERITE_UPGRADE_SPRITE);
    private static final Vector3f SMITHING_ARMOR_STAND_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Quaternionf SMITHING_ARMOR_STAND_ANGLE = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);
    private static final Identifier STONECUTTER_SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier STONECUTTER_SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier STONECUTTER_RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier STONECUTTER_RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier STONECUTTER_RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier LOOM_BANNER_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner");
    private static final Identifier LOOM_DYE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/dye");
    private static final Identifier LOOM_PATTERN_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner_pattern");
    private static final Identifier LOOM_SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller");
    private static final Identifier LOOM_SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller_disabled");
    private static final Identifier LOOM_PATTERN_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_selected");
    private static final Identifier LOOM_PATTERN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_highlighted");
    private static final Identifier LOOM_PATTERN_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern");
    private static final Identifier LOOM_ERROR_SPRITE = Identifier.withDefaultNamespace("container/loom/error");
    private static final Identifier BEACON_BUTTON_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BEACON_BUTTON_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BEACON_BUTTON_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BEACON_BUTTON_SPRITE = Identifier.withDefaultNamespace("container/beacon/button");
    private static final Identifier BEACON_CONFIRM_SPRITE = Identifier.withDefaultNamespace("container/beacon/confirm");
    private static final Identifier BEACON_CANCEL_SPRITE = Identifier.withDefaultNamespace("container/beacon/cancel");
    private static final Identifier ANVIL_TEXT_FIELD_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field");
    private static final Identifier ANVIL_TEXT_FIELD_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field_disabled");
    private static final Identifier ANVIL_ERROR_SPRITE = Identifier.withDefaultNamespace("container/anvil/error");
    private static final Identifier CREATIVE_SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier CREATIVE_SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final Identifier FABRIC_CREATIVE_BUTTONS_TEXTURE = Identifier.fromNamespaceAndPath("fabric", "textures/gui/creative_buttons.png");
    private static final Identifier[] CREATIVE_TOP_TAB_UNSELECTED_SPRITES = creativeTabSprites("tab_top_unselected");
    private static final Identifier[] CREATIVE_TOP_TAB_SELECTED_SPRITES = creativeTabSprites("tab_top_selected");
    private static final Identifier[] CREATIVE_BOTTOM_TAB_UNSELECTED_SPRITES = creativeTabSprites("tab_bottom_unselected");
    private static final Identifier[] CREATIVE_BOTTOM_TAB_SELECTED_SPRITES = creativeTabSprites("tab_bottom_selected");
    private static final Identifier CARTOGRAPHY_ERROR_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/error");
    private static final Identifier CARTOGRAPHY_SCALED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/scaled_map");
    private static final Identifier CARTOGRAPHY_DUPLICATED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/duplicated_map");
    private static final Identifier CARTOGRAPHY_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/map");
    private static final Identifier CARTOGRAPHY_LOCKED_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/locked");
    private static final Identifier BREWING_FUEL_LENGTH_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/fuel_length");
    private static final Identifier BREWING_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final Identifier BREWING_BUBBLES_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
    private static final Identifier ENCHANTING_BOOK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enchantment/enchanting_table_book.png");
    private static final Identifier ENCHANTMENT_SLOT_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot");
    private static final Identifier ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_highlighted");
    private static final Identifier ENCHANTMENT_SLOT_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_disabled");
    private static final Identifier[] ENCHANTMENT_LEVEL_SPRITES = {
        Identifier.withDefaultNamespace("container/enchanting_table/level_1"),
        Identifier.withDefaultNamespace("container/enchanting_table/level_2"),
        Identifier.withDefaultNamespace("container/enchanting_table/level_3")
    };
    private static final Identifier[] ENCHANTMENT_LEVEL_DISABLED_SPRITES = {
        Identifier.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
        Identifier.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
        Identifier.withDefaultNamespace("container/enchanting_table/level_3_disabled")
    };
    private static final int COLOR_WINDOW = 0xEE16191F;
    private static final int COLOR_WINDOW_BORDER = 0xFF7C8799;
    private static final int COLOR_TOP_BAR = 0xFF263143;
    private static final int COLOR_TOP_BAR_FOCUSED = 0xFF345372;
    private static final int COLOR_SLOT = 0xFF252A33;
    private static final int COLOR_SLOT_BORDER = 0xFF9AA3B2;
    private static final int COLOR_WINDOW_TITLE = 0xFF111111;
    private static final int COLOR_TEXT = 0xFFE8EDF5;
    private static final int COLOR_MUTED_TEXT = 0xFFB3BDCC;
    private static final int COLOR_HOTBAR_HOVER = 0x44000000;
    private static final int COLOR_DRAG_PREVIEW = 0x80FFFFFF;
    private static final int NORMAL_GUI_TINT = 0xFFFFFFFF;
    private static final int GHOST_ITEM_WASH = 0xC0D0D0D0;
    private static final int GHOST_BACKDROP = 0x40D0D0D0;
    private static int currentGuiTint = NORMAL_GUI_TINT;
    private static final Component TITLE = Component.literal("Salt's Inventory Desktop");
    private static final int WINDOW_PLACEMENT_MARGIN = 8;
    private static final int WINDOW_PLACEMENT_GAP = 8;
    private static final int WINDOW_CASCADE_OFFSET = 14;
    private static final List<WindowControl> FULL_TITLE_CONTROLS = List.of(WindowControl.FOCUS, WindowControl.PIN, WindowControl.LOCK, WindowControl.MINIMIZE, WindowControl.CLOSE);
    private static final List<WindowControl> COMPACT_TITLE_CONTROLS = List.of(WindowControl.ELLIPSIS, WindowControl.CLOSE);
    private static final List<WindowControl> POPUP_CONTROLS = List.of(WindowControl.FOCUS, WindowControl.PIN, WindowControl.LOCK, WindowControl.MINIMIZE);

    private static @Nullable InventoryDesktopScreen singleton;
    private static final Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> VANILLA_SCREEN_CONSTRUCTORS = new LinkedHashMap<>();
    private static final Set<MenuType<?>> FORCED_CONTAINER_SCREENS = new HashSet<>();
    private static Field menuScreensField;
    private static boolean internalApiDefinitionsRegistered;
    private static int nextDesktopId = 1;

    private final int desktopId;
    private final List<DesktopContainerSession> sessions = new ArrayList<>();
    private final List<InventoryWindow> windows = new ArrayList<>();
    private @Nullable LocalPlayer owner;
    private boolean hotbarOnly;
    private boolean cameraControl;
    private boolean renderingGhostWindow;
    private @Nullable InventoryWindow movingWindow;
    private @Nullable InventoryWindow pressedControlWindow;
    private @Nullable WindowControl pressedControl;
    private boolean pressedControlInPopup;
    private @Nullable InventoryWindow editingAnvilWindow;
    private @Nullable InventoryWindow editingCreativeSearchWindow;
    private @Nullable InventoryWindow popupWindow;
    private @Nullable CreativeModeTab rememberedCreativeTab;
    private int rememberedCreativeScrollRow;
    private String rememberedCreativeSearch = "";
    private int moveOffsetX;
    private int moveOffsetY;
    private @Nullable InventoryWindow resizingWindow;
    private int resizeStartMouseX;
    private int resizeStartMouseY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private @Nullable InventoryWindow scrollingCreativeWindow;
    private @Nullable InventoryWindow scrollingStorageWindow;
    private @Nullable Slot dragStartSlot;
    private @Nullable PendingSlotClick pendingSlotClick;
    private @Nullable DragDistribution dragDistribution;
    private @Nullable SlotKey lastClickedSlotKey;
    private final ScrollWheelHandler bundleScrollWheelHandler = new ScrollWheelHandler();
    private @Nullable SlotKey hoveredBundleSlotKey;
    private ItemStack sharedCarried = ItemStack.EMPTY;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;
    private int nextDebugClickId = 1;
    private @Nullable BookModel enchantmentBookModel;
    private @Nullable BannerFlagModel loomBannerFlagModel;
    private final MapRenderState cartographyMapRenderState = new MapRenderState();
    private boolean attackingWorld;
    private boolean usingWorld;
    private int recipePlacementSessionId = Integer.MIN_VALUE;

    private InventoryDesktopScreen() {
        super(TITLE);
        this.desktopId = nextDesktopId++;
        DesktopDebug.log("client singleton created id={}", this.desktopId);
    }

    public static InventoryDesktopScreen getOrCreate(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("Cannot create inventory desktop without a local player");
        }

        if (singleton != null && singleton.owner != player) {
            singleton.clearForOwnerChange("player/world changed");
            singleton = null;
        }

        if (singleton == null) {
            singleton = new InventoryDesktopScreen();
        } else {
            DesktopDebug.trace("client singleton reused id={}", singleton.desktopId);
        }

        singleton.owner = player;
        if (singleton.sharedCarried.isEmpty()) {
            singleton.sharedCarried = player.inventoryMenu.getCarried().copy();
        }
        return singleton;
    }

    public static @Nullable InventoryDesktopScreen current(Minecraft minecraft) {
        if (singleton == null || minecraft.player == null || singleton.owner != minecraft.player) {
            return null;
        }

        return singleton;
    }

    public static boolean isSingleton(InventoryDesktopScreen screen) {
        return screen == singleton;
    }

    public static void reset(Minecraft minecraft) {
        if (singleton == null) {
            return;
        }

        singleton.clearForOwnerChange("client session reset");
        DesktopDebug.log("client singleton reset id={}", singleton.desktopId);
        singleton = null;
    }

    public static void registerContainerScreens() {
        MenuScreens.ScreenConstructor constructor = (menu, inventory, title) ->
            InventoryDesktopScreen.createContainerFallbackScreen(Minecraft.getInstance(), (AbstractContainerMenu) menu, inventory, title);
        registerContainerScreen(MenuType.GENERIC_9x1, constructor);
        registerContainerScreen(MenuType.GENERIC_9x2, constructor);
        registerContainerScreen(MenuType.GENERIC_9x3, constructor);
        registerContainerScreen(MenuType.GENERIC_9x4, constructor);
        registerContainerScreen(MenuType.GENERIC_9x5, constructor);
        registerContainerScreen(MenuType.GENERIC_9x6, constructor);
        registerContainerScreen(MenuType.GENERIC_3x3, constructor);
        registerContainerScreen(MenuType.CRAFTER_3x3, constructor);
        registerContainerScreen(MenuType.ANVIL, constructor);
        registerContainerScreen(MenuType.BEACON, constructor);
        registerContainerScreen(MenuType.BLAST_FURNACE, constructor);
        registerContainerScreen(MenuType.BREWING_STAND, constructor);
        registerContainerScreen(MenuType.CRAFTING, constructor);
        registerContainerScreen(MenuType.ENCHANTMENT, constructor);
        registerContainerScreen(MenuType.FURNACE, constructor);
        registerContainerScreen(MenuType.GRINDSTONE, constructor);
        registerContainerScreen(MenuType.HOPPER, constructor);
        registerContainerScreen(MenuType.LOOM, constructor);
        registerContainerScreen(MenuType.MERCHANT, constructor);
        registerContainerScreen(MenuType.SHULKER_BOX, constructor);
        registerContainerScreen(MenuType.SMITHING, constructor);
        registerContainerScreen(MenuType.SMOKER, constructor);
        registerContainerScreen(MenuType.CARTOGRAPHY_TABLE, constructor);
        registerContainerScreen(MenuType.STONECUTTER, constructor);
        syncForcedContainerScreens();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void syncForcedContainerScreens() {
        Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> screens = getMenuScreenConstructors();
        MenuScreens.ScreenConstructor constructor = (menu, inventory, title) ->
            InventoryDesktopScreen.createContainerFallbackScreen(Minecraft.getInstance(), (AbstractContainerMenu) menu, inventory, title);

        FORCED_CONTAINER_SCREENS.removeIf(menuType -> {
            Identifier key = BuiltInRegistries.MENU.getKey(menuType);
            boolean stillForced = key != null && SaltsInventoryConfig.isForcedContainerWindow(key.toString());
            if (!stillForced && !isBuiltInDesktopScreenMenu(menuType)) {
                MenuScreens.ScreenConstructor<?, ?> original = VANILLA_SCREEN_CONSTRUCTORS.get(menuType);
                if (original == null) {
                    screens.remove(menuType);
                } else {
                    screens.put(menuType, original);
                }
            }
            return !stillForced;
        });

        for (MenuType<?> menuType : BuiltInRegistries.MENU) {
            Identifier key = BuiltInRegistries.MENU.getKey(menuType);
            if (key == null || !SaltsInventoryConfig.isForcedContainerWindow(key.toString())) {
                continue;
            }
            VANILLA_SCREEN_CONSTRUCTORS.putIfAbsent(menuType, screens.get(menuType));
            screens.put(menuType, constructor);
            if (FORCED_CONTAINER_SCREENS.add(menuType)) {
                DesktopDebug.log("client forced container screen registered menu={}", key);
            }
        }
    }

    private static boolean isBuiltInDesktopScreenMenu(MenuType<?> menuType) {
        return menuType == MenuType.GENERIC_9x1
            || menuType == MenuType.GENERIC_9x2
            || menuType == MenuType.GENERIC_9x3
            || menuType == MenuType.GENERIC_9x4
            || menuType == MenuType.GENERIC_9x5
            || menuType == MenuType.GENERIC_9x6
            || menuType == MenuType.GENERIC_3x3
            || menuType == MenuType.CRAFTER_3x3
            || menuType == MenuType.ANVIL
            || menuType == MenuType.BEACON
            || menuType == MenuType.BLAST_FURNACE
            || menuType == MenuType.BREWING_STAND
            || menuType == MenuType.CRAFTING
            || menuType == MenuType.ENCHANTMENT
            || menuType == MenuType.FURNACE
            || menuType == MenuType.GRINDSTONE
            || menuType == MenuType.HOPPER
            || menuType == MenuType.LOOM
            || menuType == MenuType.MERCHANT
            || menuType == MenuType.SHULKER_BOX
            || menuType == MenuType.SMITHING
            || menuType == MenuType.SMOKER
            || menuType == MenuType.CARTOGRAPHY_TABLE
            || menuType == MenuType.STONECUTTER;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerInternalApiDefinitions() {
        if (internalApiDefinitionsRegistered) {
            return;
        }

        DesktopWindowDefinition furnaceDefinition = new FurnaceDesktopWindowDefinition();
        SaltsInventoryDesktopApi.replaceClientWindow((MenuType) MenuType.FURNACE, furnaceDefinition);
        SaltsInventoryDesktopApi.replaceClientWindow((MenuType) MenuType.BLAST_FURNACE, furnaceDefinition);
        SaltsInventoryDesktopApi.replaceClientWindow((MenuType) MenuType.SMOKER, furnaceDefinition);
        internalApiDefinitionsRegistered = true;
    }

    private static void registerContainerScreen(MenuType<?> menuType, MenuScreens.ScreenConstructor constructor) {
        Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> screens = getMenuScreenConstructors();
        VANILLA_SCREEN_CONSTRUCTORS.putIfAbsent(menuType, screens.get(menuType));
        screens.put(menuType, constructor);
    }

    public static void registerExternalDesktopContainerScreen(MenuType<?> menuType, String owner) {
        Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> screens = getMenuScreenConstructors();
        MenuScreens.ScreenConstructor constructor = (menu, inventory, title) ->
            InventoryDesktopScreen.addLegacyContainerWindow(Minecraft.getInstance(), (AbstractContainerMenu) menu, inventory, title);
        screens.put(menuType, constructor);
        DesktopDebug.log("client external desktop screen registered owner={} menu={}", owner, BuiltInRegistries.MENU.getKey(menuType));
    }

    private static Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> getMenuScreenConstructors() {
        try {
            return MenuScreensAccessor.salts_inventory_update$getScreens();
        } catch (AssertionError accessorError) {
            return getMenuScreenConstructorsReflectively(accessorError);
        }
    }

    private static Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> getMenuScreenConstructorsReflectively(AssertionError accessorError) {
        try {
            Field field = menuScreensField;
            if (field == null) {
                field = MenuScreens.class.getDeclaredField("SCREENS");
                field.setAccessible(true);
                menuScreensField = field;
            }
            return (Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>>) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            accessorError.addSuppressed(exception);
            throw accessorError;
        }
    }

    private static @Nullable MenuType<?> safeMenuType(AbstractContainerMenu menu) {
        try {
            return menu.getType();
        } catch (UnsupportedOperationException exception) {
            return null;
        }
    }

    private static String safeMenuKey(@Nullable AbstractContainerMenu menu) {
        if (menu == null) {
            return "none";
        }
        MenuType<?> menuType = safeMenuType(menu);
        return menuType == null ? "unknown" : String.valueOf(BuiltInRegistries.MENU.getKey(menuType));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Screen createContainerFallbackScreen(
        Minecraft minecraft,
        AbstractContainerMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        MenuType<?> menuType = safeMenuType(menu);
        Identifier menuKey = menuType == null ? null : BuiltInRegistries.MENU.getKey(menuType);
        if (menuKey != null && SaltsInventoryConfig.isForcedContainerWindow(menuKey.toString())) {
            DesktopDebug.log(
                "client forced desktop window container={} menu={} title={} serverSessions={}",
                menu.containerId,
                menuKey,
                title.getString(),
                DesktopContainerClient.canUseServerSessions()
            );
            return InventoryDesktopScreen.addLegacyContainerWindow(minecraft, menu, playerInventory, title);
        }

        MenuScreens.ScreenConstructor vanillaConstructor = menuType == null ? null : VANILLA_SCREEN_CONSTRUCTORS.get(menuType);
        if (vanillaConstructor != null) {
            DesktopDebug.log(
                "client vanilla screen delegated container={} title={} serverSessions={}",
                menu.containerId,
                title.getString(),
                DesktopContainerClient.canUseServerSessions()
            );
            return (Screen) vanillaConstructor.create(menu, playerInventory, title);
        }

        if (minecraft.player != null && minecraft.player.containerMenu == menu) {
            DesktopDebug.warn("client legacy fallback window container={} title={} reason=no-vanilla-constructor-active-menu", menu.containerId, title.getString());
            return InventoryDesktopScreen.addLegacyContainerWindow(minecraft, menu, playerInventory, title);
        }

        DesktopDebug.warn("client legacy fallback window container={} title={} reason=no-vanilla-constructor-inactive-menu", menu.containerId, title.getString());
        return InventoryDesktopScreen.addLegacyContainerWindow(minecraft, menu, playerInventory, title);
    }

    public static void openOrToggleInventory(Minecraft minecraft) {
        if (!canUseDesktopInput(minecraft)) {
            return;
        }

        if (isCreativePlayer(minecraft)) {
            openOrToggleCreative(minecraft);
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        DesktopDebug.log("client request E inventory desktop={} active={}", screen.desktopId, minecraft.gui.screen() == screen);
        boolean openingInventory = !screen.hasStandaloneWindow(WindowKind.INVENTORY);
        screen.toggleWindow(WindowKind.INVENTORY);
        if (openingInventory && screen.hasStandaloneWindow(WindowKind.INVENTORY)) {
            minecraft.getTutorial().onOpenInventory();
        }
        screen.showIfNeeded(minecraft);
    }

    public static void openOrToggleCreative(Minecraft minecraft) {
        if (!canUseDesktopInput(minecraft) || !isCreativePlayer(minecraft)) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        DesktopDebug.log("client request E creative desktop={} active={}", screen.desktopId, minecraft.gui.screen() == screen);
        screen.removeStandaloneWindow(WindowKind.INVENTORY, "creative-inventory-key");
        boolean openingCreative = !screen.hasStandaloneWindow(WindowKind.CREATIVE);
        screen.toggleWindow(WindowKind.CREATIVE);
        if (openingCreative && screen.hasStandaloneWindow(WindowKind.CREATIVE)) {
            minecraft.getTutorial().onOpenInventory();
        }
        screen.showIfNeeded(minecraft);
    }

    public static boolean replaceVanillaCreativeScreen(Minecraft minecraft, @Nullable Screen incomingScreen) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        if (!(incomingScreen instanceof CreativeModeInventoryScreen) || !isCreativePlayer(minecraft)) {
            return false;
        }

        if (minecraft.gui.screen() instanceof InventoryDesktopScreen) {
            InventoryDesktopScreen screen = getOrCreate(minecraft);
            screen.removeStandaloneWindow(WindowKind.INVENTORY, "creative-screen-replace");
            screen.showWindow(WindowKind.CREATIVE);
            screen.showIfNeeded(minecraft);
        } else if (minecraft.gui.screen() == null) {
            InventoryDesktopScreen screen = getOrCreate(minecraft);
            screen.removeStandaloneWindow(WindowKind.INVENTORY, "creative-screen-replace");
            screen.showWindow(WindowKind.CREATIVE);
            screen.showIfNeeded(minecraft);
        } else {
            return false;
        }

        DesktopDebug.log("client vanilla creative screen replaced with desktop creative");
        return true;
    }

    public static void openOrToggleCharacter(Minecraft minecraft) {
        if (!canUseDesktopInput(minecraft)) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        DesktopDebug.log("client request C character desktop={} active={}", screen.desktopId, minecraft.gui.screen() == screen);
        screen.toggleWindow(WindowKind.CHARACTER);
        screen.showIfNeeded(minecraft);
    }

    public static void openHotbarOnly(Minecraft minecraft) {
        if (!SaltsInventoryRuntime.isEnabled() || minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        screen.hotbarOnly = true;
        DesktopDebug.trace("client hotbar-only show desktop={}", screen.desktopId);
        screen.showIfNeeded(minecraft);
    }

    public static void openOrAddSession(Minecraft minecraft, DesktopContainerSession session) {
        openOrAddSession(minecraft, session, true);
    }

    public static void openOrAddSession(Minecraft minecraft, DesktopContainerSession session, boolean visible) {
        if (!SaltsInventoryRuntime.isEnabled() || minecraft.player == null) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        screen.addOrReplaceSession(session, visible);
        screen.showIfNeeded(minecraft);
    }

    public static void updateDesktopCursorTarget(Minecraft minecraft) {
        if (SaltsInventoryRuntime.isEnabled() && minecraft.gui.screen() instanceof InventoryDesktopScreen screen && screen.shouldUseCursorWorldTarget()) {
            screen.updateCursorWorldTarget();
        }
    }

    public static boolean interceptRecipeBookPlacement(Minecraft minecraft, int containerId, RecipeDisplayId recipeId, boolean useMaxItems) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        InventoryDesktopScreen screen = current(minecraft);
        if (screen == null || screen.recipePlacementSessionId == Integer.MIN_VALUE) {
            return false;
        }

        int sessionId = screen.recipePlacementSessionId;
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION || sessionId == LEGACY_MENU_SESSION) {
            return false;
        }

        boolean sent = DesktopContainerClient.placeRecipe(sessionId, recipeId, useMaxItems);
        DesktopDebug.trace(
            "client recipe book place intercepted desktop={} container={} session={} recipe={} useMax={} sent={}",
            screen.desktopId,
            containerId,
            sessionId,
            recipeId,
            useMaxItems,
            sent
        );
        return true;
    }

    public static void closeAllOpenWindows(Minecraft minecraft) {
        InventoryDesktopScreen screen = current(minecraft);
        if (screen == null) {
            return;
        }

        screen.closeAllWindowsAndHide();
    }

    public static InventoryDesktopScreen addLegacyContainerWindow(
        Minecraft minecraft,
        AbstractContainerMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        InventoryDesktopScreen screen = getOrCreate(minecraft);
        screen.addLegacyContainerWindow(menu, playerInventory, title);
        DesktopDebug.log("client legacy fallback window desktop={} container={} title={}", screen.desktopId, menu.containerId, title.getString());
        return screen;
    }

    private static boolean canUseDesktopInput(Minecraft minecraft) {
        return SaltsInventoryRuntime.isEnabled()
            && minecraft.player != null
            && (minecraft.gui.screen() == null || minecraft.gui.screen() instanceof InventoryDesktopScreen);
    }

    private static boolean isCreativePlayer(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.isCreative();
    }

    public void updateSessionSlot(int sessionId, int slotIndex, int stateId, ItemStack stack) {
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            LocalPlayer player = this.player();
            if (player == null) {
                DesktopDebug.trace("client player slot update dropped desktop={} slot={} reason=no-player", this.desktopId, slotIndex);
                return;
            }

            InventoryExpansion.ensurePlayerMenuCanReadSlotCount(player, slotIndex + 1);
            AbstractContainerMenu playerMenu = player.inventoryMenu;
            if (slotIndex < 0 || slotIndex >= playerMenu.slots.size()) {
                DesktopDebug.trace("client player slot update dropped desktop={} slot={} reason=out-of-range size={}", this.desktopId, slotIndex, playerMenu.slots.size());
                return;
            }

            playerMenu.setItem(slotIndex, stateId, stack);
            DesktopDebug.trace("client player slot update desktop={} slot={} stack={}", this.desktopId, slotIndex, stack);
            return;
        }

        DesktopContainerSession session = this.session(sessionId);
        if (session == null) {
            DesktopDebug.trace("client slot update dropped desktop={} session={} slot={}", this.desktopId, sessionId, slotIndex);
            return;
        }

        session.updateSlot(slotIndex, stateId, stack);
        DesktopDebug.trace("client slot update desktop={} session={} slot={} stack={}", this.desktopId, sessionId, slotIndex, stack);
    }

    public void updateSessionData(int sessionId, int dataSlot, int value) {
        DesktopContainerSession session = this.session(sessionId);
        if (session == null) {
            DesktopDebug.trace("client data update dropped desktop={} session={} data={}", this.desktopId, sessionId, dataSlot);
            return;
        }

        session.updateData(dataSlot, value);
        DesktopDebug.trace("client data update desktop={} session={} data={} value={}", this.desktopId, sessionId, dataSlot, value);
    }

    public void setSharedCarried(ItemStack carried) {
        ItemStack copy = carried.copy();
        if (!ItemStack.matches(this.sharedCarried, copy)) {
            DesktopDebug.trace("client carried desktop={} old={} new={}", this.desktopId, this.sharedCarried, copy);
            this.clearSlotInteractionState("carried-sync");
        }

        this.sharedCarried = copy;
        this.syncSharedCarriedToMenus();
    }

    private void setCreativeSharedCarried(ItemStack carried, String reason) {
        this.setSharedCarried(carried);
        if (this.minecraft != null && isCreativePlayer(this.minecraft)) {
            boolean sent = DesktopContainerClient.syncCarried(carried);
            DesktopDebug.trace("client creative carried sync desktop={} reason={} stack={} sent={}", this.desktopId, reason, carried, sent);
        }
    }

    public void removeSession(int sessionId) {
        this.clearSlotInteractionState("session-remove");
        boolean removedSession = this.sessions.removeIf(session -> session.sessionId() == sessionId);
        boolean removedWindow = false;
        for (InventoryWindow window : List.copyOf(this.windows)) {
            if (window.session != null && window.session.sessionId() == sessionId) {
                this.saveWindowState(window, false);
                this.clearPopupStateFor(window);
                this.windows.remove(window);
                removedWindow = true;
            }
        }
        DesktopDebug.log("client session closed desktop={} session={} removedSession={} removedWindow={}", this.desktopId, sessionId, removedSession, removedWindow);
        this.closeIfEmpty();
    }

    public void setSessionVisible(int sessionId, boolean visible) {
        InventoryWindow window = this.windowForSession(sessionId);
        if (window == null) {
            DesktopDebug.trace("client visibility dropped desktop={} session={} visible={} reason=missing-window", this.desktopId, sessionId, visible);
            return;
        }

        if (visible) {
            this.promoteGhostWindow(window);
        } else if (window.pinMode == PinMode.GHOST_PINNED) {
            this.demoteGhostWindow(window, false);
        } else {
            this.closeWindow(window, "server-visibility-close");
        }
        this.closeIfEmpty();
    }

    public void applyMerchantOffers(DesktopMerchantOffersPayload payload) {
        DesktopContainerSession session = this.session(payload.sessionId());
        if (session != null) {
            session.applyMerchantOffers(payload);
            DesktopDebug.trace("client merchant offers desktop={} session={}", this.desktopId, payload.sessionId());
        }
    }

    public void refreshInventoryWindowLayout() {
        for (InventoryWindow window : this.windows) {
            if (window.kind == WindowKind.INVENTORY) {
                this.ensureInventoryWindowAutoSize(window);
            }
        }
    }

    @Override
    protected void init() {
        DesktopDebug.trace("client init desktop={} width={} height={} windows={}", this.desktopId, this.width, this.height, this.windows.size());
    }

    public boolean hasWindows() {
        return this.hasInteractiveWindows();
    }

    public boolean isHotbarOnly() {
        return this.hotbarOnly && !this.hasInteractiveWindows();
    }

    public boolean canCloseHotbarOnly() {
        return this.isHotbarOnly() && this.sharedCarried.isEmpty();
    }

    public void setCameraControl(boolean cameraControl) {
        if (this.cameraControl != cameraControl) {
            DesktopDebug.trace("client camera bypass desktop={} active={}", this.desktopId, cameraControl);
        }
        if (!this.cameraControl && cameraControl) {
            this.movingWindow = null;
            this.resizingWindow = null;
            this.editingAnvilWindow = null;
            this.editingCreativeSearchWindow = null;
            this.scrollingCreativeWindow = null;
            this.scrollingStorageWindow = null;
            this.popupWindow = null;
            this.dragStartSlot = null;
            this.stopWorldAttack();
            this.stopWorldUse();
        } else if (this.cameraControl && !cameraControl) {
            this.stopWorldAttack();
            this.stopWorldUse();
        }
        this.cameraControl = cameraControl;
    }

    public boolean isCameraControlActive() {
        return this.cameraControl;
    }

    public boolean isCreativeSearchActive() {
        return this.activeCreativeSearchWindow() != null;
    }

    public boolean isTextInputActive() {
        return this.activeCreativeSearchWindow() != null
            || this.activeAnvilEditWindow() != null
            || this.apiWantsTextInput();
    }

    @Override
    public void removed() {
        if (this.minecraft != null) {
            WindowedInventoryClient.setCameraMouseGrab(this.minecraft, false);
            WindowedInventoryClient.syncMovementKeys(this.minecraft, false);
        }
        this.stopWorldAttack();
        this.stopWorldUse();
        this.movingWindow = null;
        this.resizingWindow = null;
        this.editingAnvilWindow = null;
        this.editingCreativeSearchWindow = null;
        this.scrollingCreativeWindow = null;
        this.scrollingStorageWindow = null;
        DesktopDebug.trace("client removed from active screen desktop={} windows={} sessions={}", this.desktopId, this.windows.size(), this.sessions.size());
        super.removed();
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return this.playerMenu();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        this.updateCursorWorldTarget();
        this.tickWindowAnimations();

        if (this.attackingWorld && this.minecraft != null) {
            if (this.cameraControl) {
                CursorWorldInteraction.continueAttackAtCrosshair(this.minecraft);
            } else {
                double mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
                double mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
                CursorWorldInteraction.continueAttackAtCursor(this.minecraft, mouseX, mouseY);
            }
        }
        if (this.usingWorld && this.minecraft != null) {
            if (this.cameraControl) {
                CursorWorldInteraction.continueUseAtCrosshair(this.minecraft);
            } else {
                double mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
                double mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
                if (!this.isPointOverInteractiveUi(mouseX, mouseY)) {
                    CursorWorldInteraction.continueUseAtCursor(this.minecraft, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void onClose() {
        if (!this.sharedCarried.isEmpty()) {
            DesktopDebug.trace("client close ignored desktop={} carried={}", this.desktopId, this.sharedCarried);
            return;
        }

        this.closeAllWindowsAndHide();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        this.minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        int uiMouseX = this.cameraControl ? Integer.MIN_VALUE : mouseX;
        int uiMouseY = this.cameraControl ? Integer.MIN_VALUE : mouseY;
        for (InventoryWindow window : this.windows) {
            this.renderAttachedRecipeBook(graphics, window, uiMouseX, uiMouseY, tickProgress);
            this.renderWindow(graphics, window, uiMouseX, uiMouseY);
        }

        this.renderControlPopup(graphics, uiMouseX, uiMouseY);
        this.renderDesktopHotbarAffordances(graphics, uiMouseX, uiMouseY);
        if (!this.cameraControl) {
            this.updateBundleHover(this.slotAt(mouseX, mouseY));
            DragCarriedPreview carriedPreview = this.dragCarriedPreview();
            renderCarried(graphics, carriedPreview.stack(), mouseX, mouseY, this.minecraft, carriedPreview.countText());
            this.extractHoveredTooltip(graphics, mouseX, mouseY);
        } else {
            this.updateBundleHover(null);
        }
        this.renderDebugOverlay(graphics, uiMouseX, uiMouseY);
        InventoryKeyHoldController.extractOverlay(this.minecraft, graphics);
    }

    public static void tickPassiveGhostWindows(Minecraft minecraft) {
        InventoryDesktopScreen screen = current(minecraft);
        if (screen != null && minecraft.gui.screen() == null && screen.hasOnlyGhostWindows()) {
            screen.tickWindowAnimations();
        }
    }

    public static void extractPassiveGhostWindows(Minecraft minecraft, GuiGraphicsExtractor graphics) {
        if (!SaltsInventoryRuntime.isEnabled() || !SaltsInventoryConfig.get().enableGhostPins) {
            return;
        }

        InventoryDesktopScreen screen = current(minecraft);
        if (screen != null && minecraft.gui.screen() == null && screen.hasOnlyGhostWindows() && screen.sharedCarried.isEmpty()) {
            screen.extractGhostRenderState(graphics);
        }
    }

    private void extractGhostRenderState(GuiGraphicsExtractor graphics) {
        for (InventoryWindow window : this.windows) {
            if (window.ghosted) {
                this.renderWindow(graphics, window, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }
        }
    }

    private void tickWindowAnimations() {
        for (InventoryWindow window : this.windows) {
            if (window.minimized) {
                continue;
            }
            if (window.recipeBook != null && window.recipeBook.isVisible()) {
                window.recipeBook.tick();
            }
            if (this.apiTick(window)) {
                continue;
            }
            if (window.containerMenu() instanceof EnchantmentMenu enchantmentMenu) {
                window.enchantmentBookState().tick(enchantmentMenu);
            } else if (window.containerMenu() instanceof SmithingMenu smithingMenu) {
                window.smithingState().tick(smithingMenu);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        DesktopDebug.trace(
            "client mouse down desktop={} button={} double={} x={} y={} carried={} hotbarOnly={} windows={} hover={}",
            this.desktopId,
            event.button(),
            doubleClick,
            event.x(),
            event.y(),
            this.sharedCarried,
            this.hotbarOnly,
            this.windows.size(),
            this.describeSlotHit(this.slotAt(event.x(), event.y()))
        );
        if (this.cameraControl) {
            this.handleGameplayClick(event);
            return true;
        }

        if (!this.isContainerMouseButton(event)) {
            return false;
        }

        ControlHit popupHit = this.popupControlAt(event.x(), event.y());
        if (popupHit != null) {
            this.bringToFront(popupHit.window());
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.pressedControlWindow = popupHit.window();
                this.pressedControl = popupHit.control();
                this.pressedControlInPopup = true;
                DesktopDebug.trace("client popup control press desktop={} window={} control={}", this.desktopId, popupHit.window().debugName(), popupHit.control());
            }
            return true;
        }

        InventoryWindow recipeBookWindow = this.recipeBookWindowAt(event.x(), event.y());
        InventoryWindow window = this.windowAt(event.x(), event.y());
        if (recipeBookWindow != null && (window == null || window == recipeBookWindow)) {
            this.bringToFront(recipeBookWindow);
            this.recipeBookMouseClicked(recipeBookWindow, event, doubleClick);
            return true;
        }

        WindowControl titleControl = window == null ? null : this.titleBarControlAt(window, event.x(), event.y());
        if (this.popupWindow != null
            && !this.popupContains(event.x(), event.y())
            && !(window == this.popupWindow && titleControl == WindowControl.ELLIPSIS)) {
            this.popupWindow = null;
        }

        if (window != null) {
            this.bringToFront(window);
            if (window.ghosted) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    this.promoteGhostWindow(window);
                }
                return true;
            }
            WindowControl control = titleControl;
            if (control != null) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.pressedControlWindow = window;
                    this.pressedControl = control;
                    this.pressedControlInPopup = false;
                    DesktopDebug.trace("client control press desktop={} window={} control={}", this.desktopId, window.debugName(), control);
                }
                return true;
            }

            this.pressedControlWindow = null;
            this.pressedControl = null;
            this.pressedControlInPopup = false;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.canResizeWindow(window) && window.resizeGripAt(event.x(), event.y())) {
                this.resizingWindow = window;
                this.movingWindow = null;
                this.scrollingStorageWindow = null;
                this.popupWindow = null;
                this.resizeStartMouseX = (int) event.x();
                this.resizeStartMouseY = (int) event.y();
                this.resizeStartWidth = window.width;
                this.resizeStartHeight = window.height;
                DesktopDebug.trace("client resize start desktop={} window={} width={} height={}", this.desktopId, window.debugName(), window.width, window.height);
                return true;
            }

            boolean creativeTitleWidgetClick = window.kind == WindowKind.CREATIVE
                && (this.creativeSearchBoxContains(window, event.x(), event.y()) || this.creativeTabPageButtonAt(window, event.x(), event.y()) != null);
            if (!window.locked && window.isTopBar(event.x(), event.y()) && !creativeTitleWidgetClick) {
                this.movingWindow = window;
                this.resizingWindow = null;
                this.popupWindow = null;
                this.moveOffsetX = (int) event.x() - window.x;
                this.moveOffsetY = (int) event.y() - window.y;
                DesktopDebug.trace("client move start desktop={} window={}", this.desktopId, window.debugName());
                return true;
            }

            if (this.recipeBookButtonContains(window, event.x(), event.y())) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.toggleRecipeBook(window);
                }
                return true;
            }

            if (window.kind == WindowKind.CREATIVE) {
                return this.creativeMouseClicked(window, event, doubleClick);
            }

            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.storageScrollbarContains(window, event.x(), event.y())) {
                this.scrollingStorageWindow = window;
                this.movingWindow = null;
                this.resizingWindow = null;
                this.popupWindow = null;
                this.updateStorageScrollFromMouse(window, event.y());
                DesktopDebug.trace("client storage scrollbar drag start desktop={} window={} scroll={}", this.desktopId, window.debugName(), window.scrollRow);
                return true;
            }

            if (window.kind == WindowKind.INVENTORY && this.increaseInventoryButtonContains(window, event.x(), event.y())) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.sharedCarried.isEmpty()) {
                    DesktopContainerClient.purchaseInventorySlot();
                }
                return true;
            }

            if (this.apiMouseClicked(window, event, doubleClick)) {
                return true;
            }

            int enchantmentButton = this.enchantmentButtonAt(window, event.x(), event.y());
            if (enchantmentButton >= 0) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.enchantmentButtonClicked(window, enchantmentButton);
                }
                return true;
            }

            int merchantTrade = this.merchantTradeAt(window, event.x(), event.y());
            if (merchantTrade >= 0) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.merchantTradeClicked(window, merchantTrade);
                }
                return true;
            }

            int stonecutterRecipe = this.stonecutterRecipeAt(window, event.x(), event.y());
            if (stonecutterRecipe >= 0) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.stonecutterRecipeClicked(window, stonecutterRecipe);
                }
                return true;
            }

            int loomPattern = this.loomPatternAt(window, event.x(), event.y());
            if (loomPattern >= 0) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.loomPatternClicked(window, loomPattern);
                }
                return true;
            }

            BeaconButtonHit beaconButton = this.beaconButtonAt(window, event.x(), event.y());
            if (beaconButton != null) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.beaconButtonClicked(window, beaconButton);
                }
                return true;
            }

            if (window.containerMenu() instanceof AnvilMenu && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (this.anvilTextFieldContains(window, event.x(), event.y())) {
                    this.focusAnvilTextField(window);
                    return true;
                }
                if (this.editingAnvilWindow == window) {
                    this.editingAnvilWindow = null;
                }
            }

            if (this.crafterSlotStateClicked(window, event)) {
                return true;
            }

            SlotHit slotHit = this.slotAt(event.x(), event.y());
            if (slotHit != null) {
                this.handleSlotMouseClicked(slotHit, event, doubleClick);
            }
            return true;
        }

        SlotHit slotHit = this.slotAt(event.x(), event.y());
        if (slotHit != null) {
            this.handleSlotMouseClicked(slotHit, event, doubleClick);
            return true;
        }

        if ((this.hasWindows() || this.hotbarOnly) && !this.sharedCarried.isEmpty()) {
            this.dropCarriedOutside(event.button());
            return true;
        }

        if ((this.hasWindows() || this.hotbarOnly) && this.sharedCarried.isEmpty()) {
            this.handleWorldClick(event);
            return true;
        }

        return this.hotbarOnly || !this.sharedCarried.isEmpty();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.cameraControl) {
            return true;
        }

        if (this.pressedControl != null) {
            return true;
        }

        InventoryWindow recipeBookWindow = this.recipeBookWindowAt(event.x(), event.y());
        InventoryWindow hoveredWindow = this.windowAt(event.x(), event.y());
        if (recipeBookWindow != null && (hoveredWindow == null || hoveredWindow == recipeBookWindow)) {
            this.recipeBookMouseDragged(recipeBookWindow, event, dx, dy);
            return true;
        }

        if (this.resizingWindow != null) {
            InventoryWindow window = this.resizingWindow;
            int minWidth = this.minResizableWidth(window);
            int minHeight = this.minResizableHeight(window);
            int maxWidth = Math.max(minWidth, this.desktopWidth() - window.x);
            int maxHeight = Math.max(minHeight, this.desktopHeight() - window.y);
            window.width = clamp(this.resizeStartWidth + (int) event.x() - this.resizeStartMouseX, minWidth, maxWidth);
            window.height = clamp(this.resizeStartHeight + (int) event.y() - this.resizeStartMouseY, minHeight, maxHeight);
            this.clampStorageScroll(window);
            return true;
        }

        if (this.scrollingCreativeWindow != null) {
            this.updateCreativeScrollFromMouse(this.scrollingCreativeWindow, event.y());
            return true;
        }

        if (this.scrollingStorageWindow != null) {
            this.updateStorageScrollFromMouse(this.scrollingStorageWindow, event.y());
            return true;
        }

        if (this.movingWindow != null) {
            this.movingWindow.x = clamp((int) event.x() - this.moveOffsetX, 0, Math.max(0, this.desktopWidth() - this.movingWindow.width));
            this.movingWindow.y = clamp((int) event.y() - this.moveOffsetY, 0, Math.max(0, this.desktopHeight() - TOP_BAR_HEIGHT));
            return true;
        }

        InventoryWindow apiDragWindow = this.windowAt(event.x(), event.y());
        if (apiDragWindow != null && this.apiMouseDragged(apiDragWindow, event, dx, dy)) {
            return true;
        }

        if (this.pendingSlotClick != null) {
            this.updateDragDistribution(event.x(), event.y());
            return true;
        }

        return this.sharedCarried.isEmpty() ? this.hotbarOnly : true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        DesktopDebug.trace(
            "client mouse up desktop={} button={} x={} y={} carried={} pending={} drag={}",
            this.desktopId,
            event.button(),
            event.x(),
            event.y(),
            this.sharedCarried,
            this.describePendingClick(),
            this.dragDistribution == null ? "none" : "button=" + this.dragDistribution.button() + ",type=" + this.dragDistribution.quickCraftType() + ",slots=" + this.dragDistribution.size()
        );
        if (this.cameraControl) {
            this.pressedControlWindow = null;
            this.pressedControl = null;
            this.pressedControlInPopup = false;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.stopWorldAttack();
            } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                this.stopWorldUse();
            }
            return true;
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.pressedControl != null) {
            InventoryWindow window = this.pressedControlWindow;
            WindowControl control = this.pressedControl;
            boolean inPopup = this.pressedControlInPopup;
            this.pressedControlWindow = null;
            this.pressedControl = null;
            this.pressedControlInPopup = false;
            this.movingWindow = null;
            WindowControl releasedControl = inPopup && window != null
                ? this.popupControlAt(window, event.x(), event.y())
                : window == null ? null : this.titleBarControlAt(window, event.x(), event.y());
            if (this.windows.contains(window) && releasedControl == control) {
                this.activateControl(window, control);
            } else if (window != null) {
                DesktopDebug.trace("client control release canceled desktop={} window={} control={}", this.desktopId, window.debugName(), control);
            }
            return true;
        }

        InventoryWindow movedWindow = this.movingWindow;
        this.movingWindow = null;
        InventoryWindow resizedWindow = this.resizingWindow;
        this.resizingWindow = null;
        this.scrollingCreativeWindow = null;
        this.scrollingStorageWindow = null;
        if (movedWindow != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.saveWindowState(movedWindow);
            this.apiMoved(movedWindow);
        }
        if (resizedWindow != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (SaltsInventoryConfig.get().enableWindowSnapping) {
                this.snapResizableWindow(resizedWindow);
            } else {
                this.clampStorageScroll(resizedWindow);
            }
            this.saveWindowState(resizedWindow);
            this.apiResized(resizedWindow);
        }
        if (this.pendingSlotClick != null && this.pendingSlotClick.button() == event.button()) {
            this.completePendingSlotClick();
            return true;
        }

        InventoryWindow apiReleaseWindow = this.windowAt(event.x(), event.y());
        if (apiReleaseWindow != null && this.apiMouseReleased(apiReleaseWindow, event)) {
            return true;
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        this.dragStartSlot = null;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.stopWorldAttack();
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.stopWorldUse();
        }
        return this.windowAt(event.x(), event.y()) != null || this.hotbarOnly || !this.sharedCarried.isEmpty();
    }

    private boolean isContainerMouseButton(MouseButtonEvent event) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
            || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
            || this.isApiMiddleMouse(event)
            || this.isCreativeCloneMouse(event)
            || this.isSwapOffhandMouse(event)
            || this.hotbarMouseButton(event) >= 0;
    }

    private boolean isApiMiddleMouse(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return false;
        }

        InventoryWindow window = this.windowAt(event.x(), event.y());
        return window != null && window.apiDefinition != null && !window.minimized && !window.ghosted;
    }

    private boolean handleSlotMouseClicked(SlotHit hit, MouseButtonEvent event, boolean doubleClick) {
        boolean creativePlayerSlot = this.isCreativePlayerMenuSlot(hit);
        boolean vanillaDoubleClick = doubleClick && this.isSameLastClickedSlot(hit);
        this.lastClickedSlotKey = SlotKey.of(hit);
        DesktopDebug.trace(
            "client slot mouse branch desktop={} button={} double={} vanillaDouble={} creativePlayerSlot={} shift={} carried={} hit={}",
            this.desktopId,
            event.button(),
            doubleClick,
            vanillaDoubleClick,
            creativePlayerSlot,
            this.isShiftHeld(),
            this.sharedCarried,
            this.describeSlotHit(hit)
        );
        if (this.trySpecialSlotMouseClick(hit, event)) {
            DesktopDebug.trace("client slot mouse branch consumed special desktop={} hit={}", this.desktopId, this.describeSlotHit(hit));
            return true;
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            DesktopDebug.trace("client slot mouse branch ignored non-pickup desktop={} button={} hit={}", this.desktopId, event.button(), this.describeSlotHit(hit));
            return true;
        }

        if (vanillaDoubleClick && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.isShiftHeld() && !this.lastQuickMoved.isEmpty()) {
            DesktopDebug.trace("client slot mouse branch shift-double-move desktop={} hit={} lastQuickMoved={}", this.desktopId, this.describeSlotHit(hit), this.lastQuickMoved);
            this.quickMoveMatchingSlots(hit, this.lastQuickMoved);
            return true;
        }

        if (this.shouldQuickMove()) {
            DesktopDebug.trace("client slot mouse branch quick-move desktop={} hit={}", this.desktopId, this.describeSlotHit(hit));
            this.rememberLastQuickMoved(hit);
            this.quickMoveSlot(hit);
            return true;
        }

        if (vanillaDoubleClick && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !this.sharedCarried.isEmpty() && !isCraftingResultSlot(hit)) {
            DesktopDebug.trace("client slot mouse branch pickup-all desktop={} hit={} carried={}", this.desktopId, this.describeSlotHit(hit), this.sharedCarried);
            this.slotClicked(hit, GLFW.GLFW_MOUSE_BUTTON_LEFT, ContainerInput.PICKUP_ALL);
            return true;
        }

        if (this.beginPendingSlotClick(hit, event)) {
            DesktopDebug.trace("client slot mouse branch pending desktop={} hit={} carried={}", this.desktopId, this.describeSlotHit(hit), this.sharedCarried);
            return true;
        }

        DesktopDebug.trace("client slot mouse branch direct-pickup desktop={} hit={} carried={}", this.desktopId, this.describeSlotHit(hit), this.sharedCarried);
        this.dragStartSlot = hit.slot();
        this.slotClicked(hit, event.button(), ContainerInput.PICKUP);
        return true;
    }

    private boolean isSameLastClickedSlot(SlotHit hit) {
        return SlotKey.of(hit).equals(this.lastClickedSlotKey);
    }

    private boolean trySpecialSlotMouseClick(SlotHit hit, MouseButtonEvent event) {
        if (this.sharedCarried.isEmpty() && this.isCreativeCloneMouse(event) && hit.slot().hasItem()) {
            DesktopDebug.trace("client special slot click clone desktop={} button={} hit={}", this.desktopId, event.button(), this.describeSlotHit(hit));
            this.slotClicked(hit, event.button(), ContainerInput.CLONE);
            return true;
        }

        if (!this.sharedCarried.isEmpty() && this.isCreativeCloneMouse(event)) {
            DesktopDebug.trace("client special slot click clone-drag-start desktop={} button={} hit={} carried={}", this.desktopId, event.button(), this.describeSlotHit(hit), this.sharedCarried);
            return this.beginPendingSlotClick(hit, event);
        }

        if (!this.sharedCarried.isEmpty()) {
            return false;
        }

        if (this.isSwapOffhandMouse(event)) {
            DesktopDebug.trace("client special slot click offhand-swap desktop={} hit={}", this.desktopId, this.describeSlotHit(hit));
            this.slotClicked(hit, Inventory.SLOT_OFFHAND, ContainerInput.SWAP);
            return true;
        }

        int hotbarSlot = this.hotbarMouseButton(event);
        if (hotbarSlot >= 0) {
            DesktopDebug.trace("client special slot click hotbar-swap desktop={} hotbar={} hit={}", this.desktopId, hotbarSlot, this.describeSlotHit(hit));
            this.slotClicked(hit, hotbarSlot, ContainerInput.SWAP);
            return true;
        }

        return false;
    }

    private void dropCarriedOutside(int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT || this.sharedCarried.isEmpty()) {
            return;
        }

        DesktopDebug.trace("client carried outside click desktop={} button={} carried={}", this.desktopId, button, this.sharedCarried);
        if (this.minecraft != null && this.minecraft.gameMode != null && isCreativePlayer(this.minecraft)) {
            ItemStack dropped = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? this.sharedCarried.copy() : this.sharedCarried.copyWithCount(1);
            ItemStack remaining = this.sharedCarried.copy();
            remaining.shrink(dropped.getCount());
            DesktopDebug.trace("client creative carried outside drop desktop={} dropped={} remaining={}", this.desktopId, dropped, remaining);
            this.minecraft.gameMode.handleCreativeModeItemDrop(dropped);
            this.setCreativeSharedCarried(remaining, "outside-drop");
            return;
        }

        SlotHit outside = this.outsidePlayerMenuHit();
        if (outside != null) {
            this.slotClicked(outside, button, ContainerInput.PICKUP);
        }
    }

    private @Nullable SlotHit outsidePlayerMenuHit() {
        AbstractContainerMenu menu = this.playerMenu();
        if (menu.slots.isEmpty()) {
            return null;
        }

        return new SlotHit(
            menu.slots.get(0),
            AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
            0,
            0,
            menu,
            DesktopPackets.PLAYER_MENU_SESSION
        );
    }

    private void handleWorldClick(MouseButtonEvent event) {
        if (this.minecraft == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        DesktopDebug.trace("client cursor-world click desktop={} button={} x={} y={} alt={}", this.desktopId, event.button(), event.x(), event.y(), this.cameraControl);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.attackingWorld = true;
            CursorWorldInteraction.startAttackAtCursor(this.minecraft, event.x(), event.y());
        } else {
            this.usingWorld = true;
            CursorWorldInteraction.useAtCursor(this.minecraft, event.x(), event.y());
        }
    }

    private void handleGameplayClick(MouseButtonEvent event) {
        if (this.minecraft == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        DesktopDebug.trace("client gameplay passthrough click desktop={} button={}", this.desktopId, event.button());
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.attackingWorld = true;
            CursorWorldInteraction.startAttackAtCrosshair(this.minecraft);
        } else {
            this.usingWorld = true;
            CursorWorldInteraction.useAtCrosshair(this.minecraft);
        }
    }

    private boolean isCreativeCloneMouse(MouseButtonEvent event) {
        return this.minecraft != null
            && this.minecraft.player != null
            && this.minecraft.player.hasInfiniteMaterials()
            && this.minecraft.options.keyPickItem.matchesMouse(event);
    }

    private boolean isSwapOffhandMouse(MouseButtonEvent event) {
        return this.minecraft != null && this.minecraft.options.keySwapOffhand.matchesMouse(event);
    }

    private int hotbarMouseButton(MouseButtonEvent event) {
        if (this.minecraft == null) {
            return -1;
        }

        for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                return i;
            }
        }
        return -1;
    }

    private int quickCraftTypeFor(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY;
        }
        if (this.isCreativeCloneMouse(event)) {
            return AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE;
        }
        return -1;
    }

    private void stopWorldAttack() {
        if (this.attackingWorld && this.minecraft != null) {
            CursorWorldInteraction.stopAttack(this.minecraft);
        }
        this.attackingWorld = false;
    }

    private void stopWorldUse() {
        this.usingWorld = false;
    }

    private boolean beginPendingSlotClick(SlotHit hit, MouseButtonEvent event) {
        int quickCraftType = this.quickCraftTypeFor(event);
        if (this.sharedCarried.isEmpty() || quickCraftType < 0) {
            DesktopDebug.trace(
                "client pending slot skipped desktop={} reason={} button={} carried={} hit={}",
                this.desktopId,
                this.sharedCarried.isEmpty() ? "carried-empty" : "unsupported-button",
                event.button(),
                this.sharedCarried,
                this.describeSlotHit(hit)
            );
            return false;
        }

        this.pendingSlotClick = new PendingSlotClick(hit, event.button(), quickCraftType);
        this.dragDistribution = null;
        this.dragStartSlot = hit.slot();
        DesktopDebug.trace("client pending slot click desktop={} hit={} button={} quickCraftType={} carried={}", this.desktopId, this.describeSlotHit(hit), event.button(), quickCraftType, this.sharedCarried);
        return true;
    }

    private void updateDragDistribution(double mouseX, double mouseY) {
        PendingSlotClick pending = this.pendingSlotClick;
        if (pending == null || this.sharedCarried.isEmpty()) {
            this.clearPendingSlotClick();
            return;
        }

        SlotHit hit = this.slotAt(mouseX, mouseY);
        if (hit == null || !this.canDragDistributeTo(hit)) {
            DesktopDebug.trace(
                "client pending drag hover ignored desktop={} x={} y={} reason={} carried={} hit={}",
                this.desktopId,
                mouseX,
                mouseY,
                hit == null ? "no-slot" : "cannot-distribute",
                this.sharedCarried,
                this.describeSlotHit(hit)
            );
            return;
        }

        if (this.dragDistribution == null) {
            this.dragDistribution = new DragDistribution(pending.button(), pending.quickCraftType());
            if (this.canDragDistributeTo(pending.hit())) {
                this.dragDistribution.add(pending.hit());
            }
        }

        if (!this.canAddDragDistributionSlot(this.dragDistribution, hit)) {
            DesktopDebug.trace("client pending drag hover ignored desktop={} x={} y={} reason=carried-limit carried={} hit={}", this.desktopId, mouseX, mouseY, this.sharedCarried, this.describeSlotHit(hit));
            return;
        }

        this.dragDistribution.add(hit);
        DesktopDebug.trace("client pending drag add desktop={} size={} hit={}", this.desktopId, this.dragDistribution.size(), this.describeSlotHit(hit));
    }

    private void completePendingSlotClick() {
        PendingSlotClick pending = this.pendingSlotClick;
        DragDistribution drag = this.dragDistribution;
        this.clearPendingSlotClick();
        if (pending == null) {
            return;
        }

        if (drag != null && drag.size() > 1 && !this.sharedCarried.isEmpty()) {
            DesktopDebug.trace("client pending complete drag desktop={} dragSlots={} carried={}", this.desktopId, drag.size(), this.sharedCarried);
            this.applyDragDistribution(drag);
            return;
        }

        if (pending.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || pending.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            DesktopDebug.trace("client pending complete click desktop={} hit={} button={} carried={}", this.desktopId, this.describeSlotHit(pending.hit()), pending.button(), this.sharedCarried);
            this.slotClicked(pending.hit(), pending.button(), ContainerInput.PICKUP);
        } else {
            DesktopDebug.trace("client pending complete ignored desktop={} button={} hit={}", this.desktopId, pending.button(), this.describeSlotHit(pending.hit()));
        }
    }

    private void clearPendingSlotClick() {
        this.pendingSlotClick = null;
        this.dragDistribution = null;
        this.dragStartSlot = null;
    }

    private boolean canDragDistributeTo(SlotHit hit) {
        if (this.sharedCarried.isEmpty()) {
            return false;
        }

        Slot slot = hit.slot();
        if (!slot.isActive()
            || slot.isFake()
            || !slot.mayPlace(this.sharedCarried)
            || !hit.menu().canDragTo(slot)
            || !AbstractContainerMenu.canItemQuickReplace(slot, this.sharedCarried, true)) {
            return false;
        }

        return this.dragPlacementCapacity(hit, this.sharedCarried) > 0;
    }

    private boolean canAddDragDistributionSlot(DragDistribution drag, SlotHit hit) {
        if (drag.contains(hit) || drag.quickCraftType() == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
            return true;
        }

        return this.sharedCarried.getCount() > drag.size();
    }

    private void applyDragDistribution(DragDistribution drag) {
        if (this.applyVanillaQuickCraft(drag)) {
            return;
        }

        this.applyManualDragDistribution(drag);
    }

    private boolean applyVanillaQuickCraft(DragDistribution drag) {
        List<SlotHit> slots = drag.slots();
        if (slots.isEmpty() || !this.dragUsesSingleSession(slots)) {
            return false;
        }

        int quickcraftType = drag.quickCraftType();
        int startMask = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_START, quickcraftType);
        int continueMask = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, quickcraftType);
        int endMask = AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_END, quickcraftType);
        SlotHit first = slots.get(0);

        DesktopDebug.trace("client vanilla quick craft desktop={} button={} type={} session={} slots={}", this.desktopId, drag.button(), quickcraftType, first.sessionId(), slots.size());
        this.slotClicked(new SlotHit(first.slot(), AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, first.x(), first.y(), first.menu(), first.sessionId()), startMask, ContainerInput.QUICK_CRAFT);
        for (SlotHit hit : slots) {
            this.slotClicked(hit, continueMask, ContainerInput.QUICK_CRAFT);
        }
        this.slotClicked(new SlotHit(first.slot(), AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, first.x(), first.y(), first.menu(), first.sessionId()), endMask, ContainerInput.QUICK_CRAFT);
        return true;
    }

    private boolean dragUsesSingleSession(List<SlotHit> slots) {
        if (slots.isEmpty()) {
            return false;
        }

        int sessionId = slots.get(0).sessionId();
        for (SlotHit hit : slots) {
            if (hit.sessionId() != sessionId) {
                return false;
            }
        }
        return true;
    }

    private void applyManualDragDistribution(DragDistribution drag) {
        if (drag.quickCraftType() == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
            return;
        }

        List<SlotHit> slots = drag.slots();
        int remaining = this.sharedCarried.getCount();
        if (remaining <= 0 || slots.isEmpty()) {
            return;
        }

        Map<SlotKey, Integer> capacities = new LinkedHashMap<>();
        for (SlotHit hit : slots) {
            int capacity = this.dragPlacementCapacity(hit, this.sharedCarried);
            if (capacity > 0) {
                capacities.put(SlotKey.of(hit), capacity);
            }
        }
        if (capacities.isEmpty()) {
            return;
        }

        DesktopDebug.trace("client drag distribute desktop={} button={} slots={} carried={}", this.desktopId, drag.button(), capacities.size(), this.sharedCarried);
        if (drag.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (SlotHit hit : slots) {
                SlotKey key = SlotKey.of(hit);
                Integer capacity = capacities.get(key);
                if (capacity == null || capacity <= 0 || remaining <= 0) {
                    continue;
                }

                this.slotClicked(hit, GLFW.GLFW_MOUSE_BUTTON_RIGHT, ContainerInput.PICKUP);
                capacities.put(key, capacity - 1);
                remaining--;
            }
            return;
        }

        while (remaining > 0) {
            boolean placedAny = false;
            for (SlotHit hit : slots) {
                SlotKey key = SlotKey.of(hit);
                int capacity = capacities.getOrDefault(key, 0);
                if (capacity <= 0) {
                    continue;
                }

                this.slotClicked(hit, GLFW.GLFW_MOUSE_BUTTON_RIGHT, ContainerInput.PICKUP);
                capacities.put(key, capacity - 1);
                remaining--;
                placedAny = true;
                if (remaining <= 0) {
                    break;
                }
            }

            if (!placedAny) {
                break;
            }
        }
    }

    private int dragPlacementCapacity(SlotHit hit, ItemStack carried) {
        Slot slot = hit.slot();
        if (!slot.isActive()
            || slot.isFake()
            || !slot.mayPlace(carried)
            || !hit.menu().canDragTo(slot)
            || !AbstractContainerMenu.canItemQuickReplace(slot, carried, true)) {
            return 0;
        }

        ItemStack existing = slot.getItem();
        int max = this.creativeSlotMaxStackSize(slot, carried);
        if (existing.isEmpty()) {
            return Math.max(0, max);
        }
        if (!ItemStack.isSameItemSameComponents(existing, carried)) {
            return 0;
        }
        return Math.max(0, max - existing.getCount());
    }

    private DragCarriedPreview dragCarriedPreview() {
        DragDistribution drag = this.activeDragPreview();
        if (drag == null) {
            return new DragCarriedPreview(this.sharedCarried, null);
        }

        List<SlotHit> targets = this.previewDragTargets(drag);
        if (targets.size() <= 1) {
            return new DragCarriedPreview(this.sharedCarried, null);
        }

        int remaining = this.dragPreviewRemaining(drag, targets);
        ItemStack stack = remaining <= 0 ? this.sharedCarried.copyWithCount(1) : this.sharedCarried.copyWithCount(remaining);
        String countText = remaining <= 0 ? ChatFormatting.YELLOW + "0" : null;
        return new DragCarriedPreview(stack, countText);
    }

    private @Nullable DragSlotPreview dragSlotPreview(Slot slot) {
        DragDistribution drag = this.activeDragPreview();
        if (drag == null) {
            return null;
        }

        SlotHit hit = drag.hitFor(slot);
        if (hit == null || !this.canDragDistributeTo(hit)) {
            return null;
        }

        List<SlotHit> targets = this.previewDragTargets(drag);
        if (targets.size() <= 1) {
            return null;
        }

        return this.dragSlotPreview(hit, drag.quickCraftType(), targets.size());
    }

    private @Nullable DragDistribution activeDragPreview() {
        if (this.dragDistribution == null || this.dragDistribution.size() <= 1 || this.sharedCarried.isEmpty()) {
            return null;
        }

        return this.dragDistribution;
    }

    private List<SlotHit> previewDragTargets(DragDistribution drag) {
        List<SlotHit> targets = new ArrayList<>();
        for (SlotHit hit : drag.slots()) {
            if (this.canDragDistributeTo(hit)) {
                targets.add(hit);
            }
        }
        return targets;
    }

    private int dragPreviewRemaining(DragDistribution drag, List<SlotHit> targets) {
        if (drag.quickCraftType() == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
            return this.sharedCarried.getMaxStackSize();
        }

        int remaining = this.sharedCarried.getCount();
        int targetCount = targets.size();
        for (SlotHit hit : targets) {
            DragSlotPreview preview = this.dragSlotPreview(hit, drag.quickCraftType(), targetCount);
            if (preview == null) {
                continue;
            }

            int existing = hit.slot().getItem().isEmpty() ? 0 : hit.slot().getItem().getCount();
            remaining -= Math.max(0, preview.stack().getCount() - existing);
        }
        return Math.max(0, remaining);
    }

    private @Nullable DragSlotPreview dragSlotPreview(SlotHit hit, int quickCraftType, int targetCount) {
        ItemStack existing = hit.slot().getItem();
        int existingCount = existing.isEmpty() ? 0 : existing.getCount();
        int max = this.creativeSlotMaxStackSize(hit.slot(), this.sharedCarried);
        int previewCount = AbstractContainerMenu.getQuickCraftPlaceCount(targetCount, quickCraftType, this.sharedCarried) + existingCount;
        String countText = null;
        if (previewCount > max) {
            previewCount = max;
            countText = ChatFormatting.YELLOW.toString() + max;
        }

        return new DragSlotPreview(this.sharedCarried.copyWithCount(previewCount), countText);
    }

    private void slotClicked(SlotHit hit, int button, ContainerInput input) {
        int debugId = this.nextDebugClickId++;
        ItemStack slotBefore = hit.slot().getItem().copy();
        ItemStack carriedBefore = this.sharedCarried.copy();
        DesktopDebug.trace(
            "client slot click id={} desktop={} session={} slot={} button={} input={} menu={} slotBefore={} carriedBefore={} hit={}",
            debugId,
            this.desktopId,
            hit.sessionId(),
            hit.slotId(),
            button,
            input,
            hit.menu().containerId,
            slotBefore,
            carriedBefore,
            this.describeSlotHit(hit)
        );

        this.handleBundleSlotClick(hit, input);
        if (hit.sessionId() == LEGACY_MENU_SESSION) {
            if (this.minecraft == null || this.minecraft.player == null || this.minecraft.player.containerMenu != hit.menu()) {
                DesktopDebug.warn(
                    "client legacy click dropped menu={} playerMenu={} slot={} reason=mismatched-container",
                    hit.menu().containerId,
                    this.minecraft == null || this.minecraft.player == null ? -1 : this.minecraft.player.containerMenu.containerId,
                    hit.slotId()
                );
                return;
            }
            slotClicked(hit.menu(), hit.slotId(), button, input, this.minecraft);
            this.setSharedCarried(hit.menu().getCarried());
            DesktopDebug.trace("client slot click local result id={} slotAfter={} carriedAfter={}", debugId, hit.slot().getItem(), this.sharedCarried);
            return;
        }

        if (!DesktopContainerClient.clickSlot(debugId, hit.sessionId(), hit.slotId(), button, input, carriedBefore)) {
            if (hit.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
                slotClicked(hit.menu(), hit.slotId(), button, input, this.minecraft);
                this.setSharedCarried(hit.menu().getCarried());
                DesktopDebug.trace("client slot click fallback result id={} slotAfter={} carriedAfter={}", debugId, hit.slot().getItem(), this.sharedCarried);
            } else {
                DesktopDebug.warn("client session click dropped session={} menu={} slot={} reason=packet-send-failed", hit.sessionId(), hit.menu().containerId, hit.slotId());
            }
        } else {
            DesktopDebug.trace("client slot click sent id={} awaiting server sync session={} slot={} carriedStill={}", debugId, hit.sessionId(), hit.slotId(), this.sharedCarried);
        }
        this.notifyRecipeBooksSlotClicked(hit);
    }

    private void notifyRecipeBooksSlotClicked(SlotHit hit) {
        for (InventoryWindow window : this.windows) {
            if (window.recipeBook != null && window.recipeBook.isVisible() && window.containerMenu() == hit.menu()) {
                window.recipeBook.slotClicked(hit.slot());
            }
        }
    }

    private void updateBundleHover(@Nullable SlotHit hovered) {
        SlotKey newKey = this.isBundleSlot(hovered) ? SlotKey.of(hovered) : null;
        if (java.util.Objects.equals(this.hoveredBundleSlotKey, newKey)) {
            return;
        }

        if (this.hoveredBundleSlotKey != null) {
            SlotHit previous = this.slotHit(this.hoveredBundleSlotKey);
            if (previous != null) {
                this.clearSelectedBundleItem(previous, "hover-leave");
            }
        }
        this.hoveredBundleSlotKey = newKey;
    }

    private boolean handleBundleScroll(SlotHit hit, double scrollX, double scrollY) {
        if (!this.isBundleSlot(hit)) {
            return false;
        }

        ItemStack stack = hit.slot().getItem();
        int visibleItems = net.minecraft.world.item.BundleItem.getNumberOfItemsToShow(stack);
        if (visibleItems <= 0) {
            return false;
        }

        org.joml.Vector2i scroll = this.bundleScrollWheelHandler.onMouseScroll(scrollX, scrollY);
        int direction = scroll.y != 0 ? scroll.y : -scroll.x;
        if (direction != 0) {
            int previous = net.minecraft.world.item.BundleItem.getSelectedItemIndex(stack);
            int selected = ScrollWheelHandler.getNextScrollWheelSelection(direction, previous, visibleItems);
            if (previous != selected) {
                this.selectBundleItem(hit, stack, selected, "scroll");
            }
        }
        return true;
    }

    private void handleBundleSlotClick(SlotHit hit, ContainerInput input) {
        if (!this.isBundleSlot(hit)) {
            return;
        }
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.SWAP) {
            this.clearSelectedBundleItem(hit, "slot-click-" + input);
        }
    }

    private boolean isBundleSlot(@Nullable SlotHit hit) {
        return hit != null && hit.slot().hasItem() && hit.slot().getItem().is(ItemTags.BUNDLES);
    }

    private void clearSelectedBundleItem(SlotHit hit, String reason) {
        if (this.isBundleSlot(hit)) {
            this.selectBundleItem(hit, hit.slot().getItem(), -1, reason);
        }
    }

    private void selectBundleItem(SlotHit hit, ItemStack stack, int selectedItemIndex, String reason) {
        net.minecraft.world.item.BundleItem.toggleSelectedItem(stack, selectedItemIndex);
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundSelectBundleItemPacket(hit.slotId(), selectedItemIndex));
        }
        DesktopDebug.trace(
            "client bundle select desktop={} session={} slot={} selected={} reason={}",
            this.desktopId,
            hit.sessionId(),
            hit.slotId(),
            selectedItemIndex,
            reason
        );
    }

    private @Nullable SlotHit slotHit(SlotKey key) {
        for (InventoryWindow window : this.windows) {
            AbstractContainerMenu menu = window.containerMenu();
            if (menu == null || window.sessionId() != key.sessionId() || key.slotId() < 0 || key.slotId() >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.slots.get(key.slotId());
            return new SlotHit(slot, key.slotId(), slot.x, slot.y, menu, window.sessionId());
        }

        if (key.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
            AbstractContainerMenu menu = this.playerMenu();
            if (key.slotId() >= 0 && key.slotId() < menu.slots.size()) {
                Slot slot = menu.slots.get(key.slotId());
                return new SlotHit(slot, key.slotId(), slot.x, slot.y, menu, DesktopPackets.PLAYER_MENU_SESSION);
            }
        }
        return null;
    }

    private boolean shouldQuickMove() {
        return this.isShiftHeld();
    }

    private boolean isShiftHeld() {
        return this.minecraft != null
            && (InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    private void rememberLastQuickMoved(SlotHit hit) {
        this.lastQuickMoved = hit.slot().hasItem() ? hit.slot().getItem().copy() : ItemStack.EMPTY;
    }

    private void quickMoveMatchingSlots(SlotHit anchor, ItemStack stack) {
        if (this.minecraft == null || this.minecraft.player == null || stack.isEmpty()) {
            return;
        }

        DesktopDebug.trace(
            "client quick move matching desktop={} session={} anchorSlot={} stack={}",
            this.desktopId,
            anchor.sessionId(),
            anchor.slotId(),
            stack
        );
        for (Slot slot : anchor.menu().slots) {
            if (slot == null
                || slot.container != anchor.slot().container
                || !slot.mayPickup(this.minecraft.player)
                || !slot.hasItem()
                || !AbstractContainerMenu.canItemQuickReplace(slot, stack, true)) {
                continue;
            }

            int slotId = anchor.menu().slots.indexOf(slot);
            if (slotId >= 0) {
                this.quickMoveSlot(new SlotHit(slot, slotId, slot.x, slot.y, anchor.menu(), anchor.sessionId()));
            }
        }
    }

    private void quickMoveSlot(SlotHit hit) {
        int targetKind = DesktopPackets.QUICK_TARGET_DEFAULT;
        int targetSessionId = DesktopPackets.PLAYER_MENU_SESSION;
        InventoryWindow focusedWindow = this.focusedWindow();
        if (focusedWindow != null
            && focusedWindow.kind == WindowKind.CONTAINER
            && focusedWindow.session != null
            && focusedWindow.session.sessionId() != hit.sessionId()) {
            targetKind = DesktopPackets.QUICK_TARGET_SESSION;
            targetSessionId = focusedWindow.session.sessionId();
        } else if (hit.sessionId() != DesktopPackets.PLAYER_MENU_SESSION && !this.hasVisibleInventoryWindow()) {
            targetKind = DesktopPackets.QUICK_TARGET_HOTBAR;
        }

        DesktopDebug.trace(
            "client quick move desktop={} sourceSession={} sourceSlot={} targetKind={} targetSession={}",
            this.desktopId,
            hit.sessionId(),
            hit.slotId(),
            targetKind,
            targetSessionId
        );
        if (!DesktopContainerClient.quickMoveSlot(hit.sessionId(), hit.slotId(), targetKind, targetSessionId)
            && hit.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
            slotClicked(hit.menu(), hit.slotId(), 0, ContainerInput.QUICK_MOVE, this.minecraft);
            this.setSharedCarried(hit.menu().getCarried());
        }
    }

    private boolean hasVisibleInventoryWindow() {
        for (InventoryWindow window : this.windows) {
            if (window.kind == WindowKind.INVENTORY && !window.minimized && !window.ghosted) {
                return true;
            }
            if (window.kind == WindowKind.CREATIVE && !window.minimized && !window.ghosted) {
                CreativeModeTab selectedTab = this.selectedCreativeTab(window);
                if (selectedTab != null && this.isCreativeInventoryTab(selectedTab)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCraftingResultSlot(SlotHit hit) {
        return hit.menu() instanceof AbstractCraftingMenu craftingMenu && hit.slot() == craftingMenu.getResultSlot();
    }

    private String describePendingClick() {
        PendingSlotClick pending = this.pendingSlotClick;
        if (pending == null) {
            return "none";
        }

        return "button=" + pending.button()
            + ",type=" + pending.quickCraftType()
            + ",hit=" + this.describeSlotHit(pending.hit());
    }

    private String describeSlotHit(@Nullable SlotHit hit) {
        if (hit == null) {
            return "none";
        }

        Slot slot = hit.slot();
        return "session=" + hit.sessionId()
            + ",menu=" + hit.menu().containerId
            + ",slot=" + hit.slotId()
            + ",containerSlot=" + slot.getContainerSlot()
            + ",active=" + slot.isActive()
            + ",fake=" + slot.isFake()
            + ",mayPlaceCarried=" + (!this.sharedCarried.isEmpty() && slot.mayPlace(this.sharedCarried))
            + ",stack=" + slot.getItem();
    }

    private void enchantmentButtonClicked(InventoryWindow window, int buttonId) {
        AbstractContainerMenu menu = window.containerMenu();
        if (!(menu instanceof EnchantmentMenu enchantmentMenu) || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (!this.canSelectEnchantment(enchantmentMenu, buttonId)) {
            DesktopDebug.trace("client enchant button ignored desktop={} window={} button={} reason=disabled", this.desktopId, window.debugName(), buttonId);
            return;
        }

        DesktopDebug.trace("client enchant button desktop={} session={} button={}", this.desktopId, window.sessionId(), buttonId);
        if (window.session != null) {
            if (!DesktopContainerClient.clickButton(window.session.sessionId(), buttonId)) {
                DesktopDebug.warn("client enchant button dropped session={} button={} reason=packet-send-failed", window.session.sessionId(), buttonId);
            }
            return;
        }

        if (this.minecraft.gameMode != null
            && window.legacyMenu == menu
            && this.minecraft.player.containerMenu == menu
            && enchantmentMenu.clickMenuButton(this.minecraft.player, buttonId)) {
            this.minecraft.gameMode.handleInventoryButtonClick(enchantmentMenu.containerId, buttonId);
        }
    }

    private void merchantTradeClicked(InventoryWindow window, int tradeIndex) {
        AbstractContainerMenu menu = window.containerMenu();
        if (!(menu instanceof MerchantMenu merchantMenu) || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (tradeIndex < 0 || tradeIndex >= merchantMenu.getOffers().size()) {
            return;
        }

        window.merchantSelectedTrade = tradeIndex;
        merchantMenu.setSelectionHint(tradeIndex);
        merchantMenu.tryMoveItems(tradeIndex);
        DesktopDebug.trace("client merchant trade desktop={} session={} trade={}", this.desktopId, window.sessionId(), tradeIndex);

        if (window.session != null) {
            if (!DesktopContainerClient.clickButton(window.session.sessionId(), tradeIndex)) {
                DesktopDebug.warn("client merchant trade dropped session={} trade={} reason=packet-send-failed", window.session.sessionId(), tradeIndex);
            }
            return;
        }

        if (this.minecraft.gameMode != null && window.legacyMenu == menu && this.minecraft.player.containerMenu == menu) {
            this.minecraft.gameMode.handleInventoryButtonClick(merchantMenu.containerId, tradeIndex);
        }
    }

    private void stonecutterRecipeClicked(InventoryWindow window, int recipeIndex) {
        AbstractContainerMenu menu = window.containerMenu();
        if (!(menu instanceof StonecutterMenu stonecutterMenu) || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (recipeIndex < 0 || recipeIndex >= stonecutterMenu.getNumberOfVisibleRecipes()) {
            return;
        }

        if (!stonecutterMenu.clickMenuButton(this.minecraft.player, recipeIndex)) {
            return;
        }

        DesktopDebug.trace("client stonecutter recipe desktop={} session={} recipe={}", this.desktopId, window.sessionId(), recipeIndex);
        if (window.session != null) {
            if (!DesktopContainerClient.clickButton(window.session.sessionId(), recipeIndex)) {
                DesktopDebug.warn("client stonecutter recipe dropped session={} recipe={} reason=packet-send-failed", window.session.sessionId(), recipeIndex);
            }
            return;
        }

        if (this.minecraft.gameMode != null && window.legacyMenu == menu && this.minecraft.player.containerMenu == menu) {
            this.minecraft.gameMode.handleInventoryButtonClick(stonecutterMenu.containerId, recipeIndex);
        }
    }

    private void loomPatternClicked(InventoryWindow window, int patternIndex) {
        AbstractContainerMenu menu = window.containerMenu();
        if (!(menu instanceof LoomMenu loomMenu) || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (!this.shouldDisplayLoomPatterns(loomMenu) || patternIndex < 0 || patternIndex >= loomMenu.getSelectablePatterns().size()) {
            return;
        }

        if (!loomMenu.clickMenuButton(this.minecraft.player, patternIndex)) {
            return;
        }

        DesktopDebug.trace("client loom pattern desktop={} session={} pattern={}", this.desktopId, window.sessionId(), patternIndex);
        if (window.session != null) {
            if (!DesktopContainerClient.clickButton(window.session.sessionId(), patternIndex)) {
                DesktopDebug.warn("client loom pattern dropped session={} pattern={} reason=packet-send-failed", window.session.sessionId(), patternIndex);
            }
            return;
        }

        if (this.minecraft.gameMode != null && window.legacyMenu == menu && this.minecraft.player.containerMenu == menu) {
            this.minecraft.gameMode.handleInventoryButtonClick(loomMenu.containerId, patternIndex);
        }
    }

    private void beaconButtonClicked(InventoryWindow window, BeaconButtonHit hit) {
        AbstractContainerMenu menu = window.containerMenu();
        if (!(menu instanceof BeaconMenu beaconMenu) || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        this.syncBeaconSelection(window, beaconMenu);
        switch (hit.kind()) {
            case PRIMARY -> {
                if (!this.canSelectBeaconPrimary(beaconMenu, hit.effect())) {
                    return;
                }

                window.beaconPrimary = hit.effect();
                if (!this.canSelectBeaconSecondary(beaconMenu, window.beaconPrimary, window.beaconSecondary)) {
                    window.beaconSecondary = null;
                }
                window.beaconSelectionDirty = true;
                DesktopDebug.trace("client beacon primary desktop={} window={} effect={}", this.desktopId, window.debugName(), beaconEffectId(hit.effect()));
            }
            case SECONDARY, UPGRADE -> {
                if (!this.canSelectBeaconSecondary(beaconMenu, window.beaconPrimary, hit.effect())) {
                    return;
                }

                window.beaconSecondary = hit.effect();
                window.beaconSelectionDirty = true;
                DesktopDebug.trace("client beacon secondary desktop={} window={} effect={}", this.desktopId, window.debugName(), beaconEffectId(hit.effect()));
            }
            case CONFIRM -> {
                if (!this.canConfirmBeacon(beaconMenu, window)) {
                    return;
                }

                int buttonId = beaconButtonId(window.beaconPrimary, window.beaconSecondary);
                DesktopDebug.trace(
                    "client beacon confirm desktop={} session={} primary={} secondary={}",
                    this.desktopId,
                    window.sessionId(),
                    beaconEffectId(window.beaconPrimary),
                    beaconEffectId(window.beaconSecondary)
                );
                if (window.session != null) {
                    if (!DesktopContainerClient.clickButton(window.session.sessionId(), buttonId)) {
                        DesktopDebug.warn("client beacon confirm dropped session={} reason=packet-send-failed", window.session.sessionId());
                    }
                    return;
                }

                if (this.minecraft.getConnection() != null && window.legacyMenu == menu && this.minecraft.player.containerMenu == menu) {
                    this.minecraft.getConnection().send(new ServerboundSetBeaconPacket(
                        Optional.ofNullable(window.beaconPrimary),
                        Optional.ofNullable(window.beaconSecondary)
                    ));
                }
            }
            case CANCEL -> this.activateControl(window, WindowControl.CLOSE);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.cameraControl) {
            return this.scrollHotbar(scrollY);
        }

        SlotHit hoveredSlot = this.slotAt(x, y);
        if (hoveredSlot != null && this.handleBundleScroll(hoveredSlot, scrollX, scrollY)) {
            return true;
        }

        InventoryWindow window = this.windowAt(x, y);
        if (window != null && !window.minimized && window.kind == WindowKind.CREATIVE && this.creativeGridContains(window, x, y)) {
            if (this.scrollCreativeWindow(window, scrollY)) {
                return true;
            }
        }

        if (window != null && !window.minimized && this.apiMouseScrolled(window, x, y, scrollX, scrollY)) {
            return true;
        }

        if (window != null && !window.minimized && window.containerMenu() instanceof MerchantMenu merchantMenu && this.merchantTradeListContains(window, x, y)) {
            MerchantOffers offers = merchantMenu.getOffers();
            int maxScroll = Math.max(0, offers.size() - MERCHANT_VISIBLE_TRADES);
            if (maxScroll > 0) {
                int oldScroll = window.merchantScroll;
                window.merchantScroll = clamp(window.merchantScroll + (scrollY < 0.0 ? 1 : -1), 0, maxScroll);
                DesktopDebug.trace("client merchant scroll desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.merchantScroll, maxScroll);
                return true;
            }
        }

        if (window != null && !window.minimized && window.containerMenu() instanceof StonecutterMenu stonecutterMenu && this.stonecutterRecipePanelContains(window, x, y)) {
            int maxScroll = stonecutterMaxScroll(stonecutterMenu);
            if (maxScroll > 0) {
                int oldScroll = window.stonecutterScroll;
                window.stonecutterScroll = clamp(window.stonecutterScroll + (scrollY < 0.0 ? 1 : -1), 0, maxScroll);
                DesktopDebug.trace("client stonecutter scroll desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.stonecutterScroll, maxScroll);
                return true;
            }
        }

        if (window != null && !window.minimized && window.containerMenu() instanceof LoomMenu loomMenu && this.loomPatternPanelContains(window, x, y)) {
            int maxScroll = loomMaxScroll(loomMenu);
            if (maxScroll > 0) {
                int oldScroll = window.loomScroll;
                window.loomScroll = clamp(window.loomScroll + (scrollY < 0.0 ? 1 : -1), 0, maxScroll);
                DesktopDebug.trace("client loom scroll desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.loomScroll, maxScroll);
                return true;
            }
        }

        if (window != null && !window.minimized && this.isResizableStorageWindow(window)) {
            SlotGridLayout layout = this.storageLayout(window, this.storageSlotCount(window));
            if (layout.scrollable()) {
                int oldScroll = window.scrollRow;
                window.scrollRow = clamp(window.scrollRow + (scrollY < 0.0 ? 1 : -1), 0, layout.maxScrollRow());
                DesktopDebug.trace("client storage scroll desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.scrollRow, layout.maxScrollRow());
                return true;
            }
        }

        return this.scrollHotbar(scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.handleRecipeBookKey(event)) {
            return true;
        }

        if (this.handleCreativeHotbarLoadOrSaveKey(event)) {
            return true;
        }

        if (this.handleCreativeCatalogKey(event)) {
            return true;
        }

        if (this.handleCreativeSearchOpenKey(event)) {
            return true;
        }

        if (this.handleCreativeSearchKey(event)) {
            return true;
        }

        if (this.handleCreativeDropKey(event)) {
            return true;
        }

        if (this.handleAnvilEditKey(event)) {
            return true;
        }

        if (this.apiKeyPressed(event)) {
            return true;
        }

        if (this.minecraft.options.keyInventory.matches(event)) {
            return InventoryKeyHoldController.handleInventoryKeyAction(this.minecraft, GLFW.GLFW_PRESS, event);
        }

        if (WindowedInventoryClient.characterWindowKey().matches(event)) {
            this.toggleWindow(WindowKind.CHARACTER);
            this.showIfNeeded(this.minecraft);
            return true;
        }

        if (event.isEscape()) {
            this.onClose();
            return true;
        }

        SlotHit hoveredSlot = this.hoveredSlotForKeyboardInput();
        if (hoveredSlot != null && this.handleSlotKeyPressed(hoveredSlot, event)) {
            return true;
        }

        for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                if (hoveredSlot != null) {
                    return true;
                }
                this.selectHotbarSlot(i);
                return true;
            }
        }

        if (hoveredSlot != null && this.minecraft.options.keySwapOffhand.matches(event)) {
            return true;
        }

        this.syncMovementKey(event, true);
        return false;
    }

    private @Nullable SlotHit hoveredSlotForKeyboardInput() {
        if (this.minecraft == null) {
            return null;
        }

        double mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
        double mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
        return this.slotAt(mouseX, mouseY);
    }

    private boolean handleSlotKeyPressed(SlotHit hit, KeyEvent event) {
        if (this.minecraft == null) {
            return false;
        }

        if (this.sharedCarried.isEmpty() && this.minecraft.options.keySwapOffhand.matches(event)) {
            this.slotClicked(hit, Inventory.SLOT_OFFHAND, ContainerInput.SWAP);
            return true;
        }

        if (this.sharedCarried.isEmpty()) {
            for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                    this.slotClicked(hit, i, ContainerInput.SWAP);
                    return true;
                }
            }
        } else {
            for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                    return true;
                }
            }
            if (this.minecraft.options.keySwapOffhand.matches(event)) {
                return true;
            }
        }

        if (!hit.slot().hasItem()) {
            return false;
        }

        if (this.minecraft.options.keyPickItem.matches(event) && this.minecraft.player != null && this.minecraft.player.hasInfiniteMaterials()) {
            this.slotClicked(hit, 0, ContainerInput.CLONE);
            return true;
        }

        if (this.minecraft.options.keyDrop.matches(event)) {
            this.slotClicked(hit, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
            return true;
        }

        return false;
    }

    private boolean handleCreativeHotbarLoadOrSaveKey(KeyEvent event) {
        if (this.minecraft == null || !isCreativePlayer(this.minecraft)) {
            return false;
        }

        boolean loadPressed = this.minecraft.options.keyLoadHotbarActivator.isDown();
        boolean savePressed = this.minecraft.options.keySaveHotbarActivator.isDown();
        if (!loadPressed && !savePressed) {
            return false;
        }

        for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                CreativeModeInventoryScreen.handleHotbarLoadOrSave(this.minecraft, i, loadPressed, savePressed);
                this.refreshVisibleCreativeWindows();
                return true;
            }
        }
        return false;
    }

    private boolean handleCreativeCatalogKey(KeyEvent event) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null || !this.minecraft.player.hasInfiniteMaterials()) {
            return false;
        }

        CreativeItemHit itemHit = this.creativeItemAtCurrentMouse();
        if (itemHit == null || !this.isCreativeCatalogStackActionable(itemHit.stack())) {
            return false;
        }

        if (this.minecraft.options.keyDrop.matches(event) && this.minecraft.player.canDropItems()) {
            ItemStack dropped = itemHit.stack().copyWithCount(event.hasControlDown() ? itemHit.stack().getMaxStackSize() : 1);
            this.minecraft.player.drop(dropped, true);
            this.minecraft.gameMode.handleCreativeModeItemDrop(dropped);
            DesktopDebug.trace("client creative catalog drop desktop={} index={} stack={} ctrl={}", this.desktopId, itemHit.index(), dropped, event.hasControlDown());
            return true;
        }

        if (this.minecraft.options.keyPickItem.matches(event)) {
            this.setCreativeSharedCarried(itemHit.stack().copyWithCount(itemHit.stack().getMaxStackSize()), "catalog-clone-key");
            return true;
        }

        if (this.minecraft.options.keySwapOffhand.matches(event)) {
            this.putCreativeCatalogStackInInventorySlot(itemHit.stack(), Inventory.SLOT_OFFHAND, OFFHAND_MENU_SLOT_FALLBACK);
            return true;
        }

        for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                this.putCreativeCatalogStackInInventorySlot(itemHit.stack(), i, 36 + i);
                return true;
            }
        }
        return false;
    }

    private boolean handleCreativeSearchOpenKey(KeyEvent event) {
        if (this.minecraft == null || !isCreativePlayer(this.minecraft) || !this.minecraft.options.keyChat.matches(event)) {
            return false;
        }

        InventoryWindow window = this.topmostCreativeWindow();
        if (window == null) {
            return false;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab != null && this.isCreativeSearchTab(selectedTab) && this.editingCreativeSearchWindow == window) {
            return false;
        }

        CreativeModeTab searchTab = CreativeModeTabs.searchTab();
        if (!this.creativeTabs().contains(searchTab)) {
            return false;
        }

        this.selectCreativeTab(window, searchTab);
        window.creativeScrollRow = 0;
        this.editingCreativeSearchWindow = window;
        this.rememberCreativeWindow(window);
        return true;
    }

    private @Nullable CreativeItemHit creativeItemAtCurrentMouse() {
        if (this.minecraft == null) {
            return null;
        }

        double mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
        double mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
        return this.creativeItemAt(mouseX, mouseY);
    }

    private @Nullable InventoryWindow topmostCreativeWindow() {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.kind == WindowKind.CREATIVE && !window.minimized && !window.ghosted) {
                return window;
            }
        }
        return null;
    }

    private void putCreativeCatalogStackInInventorySlot(ItemStack source, int inventorySlot, int menuSlotId) {
        if (this.minecraft == null || this.minecraft.gameMode == null || this.minecraft.player == null || source.isEmpty()) {
            return;
        }

        ItemStack stack = source.copyWithCount(source.getMaxStackSize());
        this.minecraft.player.getInventory().setItem(inventorySlot, stack);
        this.minecraft.gameMode.handleCreativeModeItemAdd(stack, menuSlotId);
        this.minecraft.player.inventoryMenu.broadcastChanges();
        DesktopDebug.trace("client creative catalog swap desktop={} inventorySlot={} menuSlot={} stack={}", this.desktopId, inventorySlot, menuSlotId, stack);
    }

    private void refreshVisibleCreativeWindows() {
        for (InventoryWindow window : this.windows) {
            if (window.kind == WindowKind.CREATIVE) {
                this.clampCreativeScroll(window, window.creativeScrollRow);
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.handleRecipeBookChar(event)) {
            return true;
        }

        InventoryWindow creativeWindow = this.activeCreativeSearchWindow();
        if (creativeWindow != null) {
            if (!event.isAllowedChatCharacter()) {
                return false;
            }

            String addition = event.codepointAsString();
            if (!addition.isEmpty()) {
                creativeWindow.creativeSearch = creativeWindow.creativeSearch + addition;
                creativeWindow.creativeScrollRow = 0;
                this.rememberCreativeWindow(creativeWindow);
            }
            return true;
        }

        InventoryWindow window = this.activeAnvilEditWindow();
        if (window == null || !event.isAllowedChatCharacter()) {
            return this.apiCharTyped(event);
        }

        String addition = event.codepointAsString();
        if (addition.isEmpty() || window.anvilName.length() + addition.length() > ANVIL_MAX_NAME_LENGTH) {
            return true;
        }

        window.anvilName = window.anvilName + addition;
        this.submitAnvilName(window);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.handleRecipeBookKeyRelease(event)) {
            return true;
        }

        if (this.minecraft.options.keyInventory.matches(event)) {
            return InventoryKeyHoldController.handleInventoryKeyAction(this.minecraft, GLFW.GLFW_RELEASE, event);
        }

        this.syncMovementKey(event, false);
        return false;
    }

    private void syncMovementKey(KeyEvent event, boolean down) {
        if (this.minecraft.options.keyUp.matches(event)) {
            this.minecraft.options.keyUp.setDown(down);
        } else if (this.minecraft.options.keyLeft.matches(event)) {
            this.minecraft.options.keyLeft.setDown(down);
        } else if (this.minecraft.options.keyDown.matches(event)) {
            this.minecraft.options.keyDown.setDown(down);
        } else if (this.minecraft.options.keyRight.matches(event)) {
            this.minecraft.options.keyRight.setDown(down);
        } else if (this.minecraft.options.keyJump.matches(event)) {
            this.minecraft.options.keyJump.setDown(down);
        } else if (this.minecraft.options.keyShift.matches(event)) {
            this.minecraft.options.keyShift.setDown(down && this.shouldPassShiftToMovement());
        } else if (this.minecraft.options.keySprint.matches(event)) {
            this.minecraft.options.keySprint.setDown(down);
        }
    }

    private boolean shouldPassShiftToMovement() {
        return !this.hasWindows() || this.isCameraControlActive();
    }

    private boolean hasStandaloneWindow(WindowKind kind) {
        for (InventoryWindow window : this.windows) {
            if (window.kind == kind && window.session == null && window.legacyMenu == null) {
                return true;
            }
        }
        return false;
    }

    private void toggleWindow(WindowKind kind) {
        for (int i = 0; i < this.windows.size(); i++) {
            InventoryWindow window = this.windows.get(i);
            if (window.kind == kind && window.session == null && window.legacyMenu == null) {
                if (window.ghosted) {
                    this.promoteGhostWindow(window);
                } else {
                    this.closeWindow(window, "toggle");
                }
                return;
            }
        }

        this.addWindow(kind);
        this.hotbarOnly = false;
    }

    private boolean removeStandaloneWindow(WindowKind kind, String reason) {
        boolean removed = false;
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.kind == kind && window.session == null && window.legacyMenu == null) {
                this.closeWindow(window, reason);
                removed = true;
            }
        }

        return removed;
    }

    private void showWindow(WindowKind kind) {
        for (InventoryWindow window : this.windows) {
            if (window.kind == kind && window.session == null && window.legacyMenu == null) {
                this.promoteGhostWindow(window);
                this.hotbarOnly = false;
                return;
            }
        }

        this.addWindow(kind);
        this.hotbarOnly = false;
    }

    private void addWindow(WindowKind kind) {
        InventoryWindow window;
        if (kind == WindowKind.INVENTORY) {
            int inventorySlotCount = this.inventoryVirtualSlotCount();
            int totalRows = rowsForSlots(inventorySlotCount, INVENTORY_DEFAULT_COLUMNS);
            int visibleRows = Math.max(INVENTORY_DEFAULT_VISIBLE_ROWS, Math.min(INVENTORY_MAX_AUTO_VISIBLE_ROWS, Math.max(1, totalRows)));
            boolean scrollbar = totalRows > visibleRows;
            int windowWidth = storageWindowWidth(INVENTORY_DEFAULT_COLUMNS, scrollbar);
            int windowHeight = storageWindowHeight(visibleRows);
            window = new InventoryWindow(
                kind,
                Component.literal("Inventory"),
                0,
                0,
                windowWidth,
                windowHeight
            );
            this.placeOrRestoreWindow(window, WindowPlacement.CENTER);
            this.ensureInventoryWindowAutoSize(window);
        } else if (kind == WindowKind.CREATIVE) {
            int windowWidth = CREATIVE_CONTENT_MARGIN * 2 + CREATIVE_CONTENT_WIDTH;
            int windowHeight = TOP_BAR_HEIGHT + CREATIVE_CONTENT_MARGIN * 2 + CREATIVE_CONTENT_HEIGHT;
            window = new InventoryWindow(
                kind,
                CREATIVE_TITLE,
                0,
                0,
                windowWidth,
                windowHeight
            );
            this.placeOrRestoreWindow(window, WindowPlacement.CENTER);
            this.initializeCreativeWindow(window);
            this.restoreCreativeWindow(window);
        } else if (kind == WindowKind.CHARACTER) {
            window = new InventoryWindow(
                kind,
                Component.literal("Character"),
                0,
                0,
                CHARACTER_WINDOW_WIDTH,
                CHARACTER_WINDOW_HEIGHT
            );
            this.placeOrRestoreWindow(window, WindowPlacement.BOTTOM_LEFT);
        } else {
            return;
        }

        this.windows.add(window);
        this.apiOpened(window);
        this.setFocusedWindow(window);
        DesktopDebug.log("client window add desktop={} kind={} title={} windows={}", this.desktopId, kind, window.title.getString(), this.windows.size());
        this.promoteGhostWindowsForDesktopOpen(window);
    }

    private void addLegacyContainerWindow(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        List<Slot> slots = findContainerSlots(menu, playerInventory);
        int minX = minSlotX(slots);
        int minY = minSlotY(slots);
        int contentWidth = containerContentWidth(slots, minX);
        int contentHeight = containerContentHeight(slots, minY);
        int defaultWindowWidth = this.containerWindowWidth(menu, title, slots.size(), contentWidth);
        int defaultWindowHeight = containerWindowHeight(menu, slots.size(), contentHeight);
        DesktopWindowDefinition<?, ?> apiDefinition = this.apiDefinitionFor(
            menu,
            title,
            slots,
            contentWidth,
            contentHeight,
            LEGACY_MENU_SESSION,
            "",
            DesktopPackets.SPECIAL_GENERIC
        );
        DesktopWindowSetupContext<?> apiSetup = this.apiSetupContext(
            menu,
            title,
            slots,
            contentWidth,
            contentHeight,
            defaultWindowWidth,
            defaultWindowHeight,
            LEGACY_MENU_SESSION,
            ""
        );
        DesktopWindowSize apiSize = this.apiDefaultSize(apiDefinition, apiSetup, defaultWindowWidth, defaultWindowHeight);
        this.logApiWindowBuild(
            "legacy",
            menu,
            title,
            LEGACY_MENU_SESSION,
            "",
            slots.size(),
            contentWidth,
            contentHeight,
            defaultWindowWidth,
            defaultWindowHeight,
            apiSize,
            apiDefinition
        );
        InventoryWindow window = new InventoryWindow(
            WindowKind.CONTAINER,
            title,
            0,
            0,
            apiSize.width(),
            apiSize.height(),
            null,
            menu,
            slots,
            minX,
            minY
        );
        this.initializeApiWindow(window, apiDefinition, apiSetup);
        this.placeOrRestoreWindow(window, WindowPlacement.CONTAINER);
        this.windows.add(window);
        this.apiOpened(window);
        this.setFocusedWindow(window);
        this.hotbarOnly = false;
        this.setSharedCarried(menu.getCarried());
        this.promoteGhostWindowsForDesktopOpen(window);
    }

    private void addOrReplaceSession(DesktopContainerSession session, boolean visible) {
        boolean replacedSource = false;
        if (!session.sourceKey().isEmpty()) {
            for (DesktopContainerSession existing : List.copyOf(this.sessions)) {
                if (!session.sourceKey().equals(existing.sourceKey())) {
                    continue;
                }
                if (session.sessionId() == existing.sessionId()) {
                    continue;
                }

                DesktopDebug.warn(
                    "client duplicate source session desktop={} source={} oldSession={} incomingSession={} action=keep-existing",
                    this.desktopId,
                    session.sourceKey(),
                    existing.sessionId(),
                    session.sessionId()
                );
                DesktopContainerClient.closeSession(session.sessionId());
                for (InventoryWindow existingWindow : this.windows) {
                    if (existingWindow.session == existing) {
                        this.promoteGhostWindow(existingWindow);
                        break;
                    }
                }
                replacedSource = true;
            }
        }

        if (replacedSource) {
            DesktopDebug.log("client duplicate source ignored desktop={} source={} session={}", this.desktopId, session.sourceKey(), session.sessionId());
            return;
        }

        boolean replacedSession = this.sessions.removeIf(existing -> existing.sessionId() == session.sessionId());
        boolean replacedWindow = false;
        for (InventoryWindow window : List.copyOf(this.windows)) {
            if (window.session != null && window.session.sessionId() == session.sessionId()) {
                this.saveWindowState(window);
                this.clearPopupStateFor(window);
                this.apiClosed(window);
                this.windows.remove(window);
                replacedWindow = true;
            }
        }
        if (replacedSession && !replacedWindow) {
            DesktopDebug.log("client duplicate session id replaced desktop={} session={}", this.desktopId, session.sessionId());
        } else if (!replacedSession && replacedWindow) {
            DesktopDebug.warn("client stale window removed desktop={} session={}", this.desktopId, session.sessionId());
        }

        this.sessions.add(session);
        this.clearSlotInteractionState("session-add");
        session.setCarried(this.sharedCarried);
        if (session.specialKind() == DesktopPackets.SPECIAL_CAMEL || session.specialKind() == DesktopPackets.SPECIAL_LLAMA) {
            DesktopDebug.warn(
                "SIU_MOUNT_DIAG client_add_session_start desktop={} session={} special={} entityId={} columns={} visible={} source={} slots={} containerSlots={} content={}x{}",
                this.desktopId,
                session.sessionId(),
                session.specialKind(),
                session.entityId(),
                session.columns(),
                visible,
                session.sourceKey(),
                session.menu().slots.size(),
                session.containerSlots().size(),
                session.contentWidth(),
                session.contentHeight()
            );
        }

        int defaultWindowWidth = session.isMountSession()
            ? this.mountWindowWidth(session, session.title())
            : this.containerWindowWidth(session.menu(), session.title(), session.containerSlots().size(), session.contentWidth());
        int defaultWindowHeight = session.isMountSession()
            ? mountWindowHeight()
            : containerWindowHeight(session.menu(), session.containerSlots().size(), session.contentHeight());
        DesktopWindowDefinition<?, ?> apiDefinition = session.isMountSession()
            ? null
            : this.apiDefinitionFor(
                session.menu(),
                session.title(),
                session.containerSlots(),
                session.contentWidth(),
                session.contentHeight(),
                session.sessionId(),
                session.sourceKey(),
                session.specialKind()
            );
        DesktopWindowSetupContext<?> apiSetup = this.apiSetupContext(
            session.menu(),
            session.title(),
            session.containerSlots(),
            session.contentWidth(),
            session.contentHeight(),
            defaultWindowWidth,
            defaultWindowHeight,
            session.sessionId(),
            session.sourceKey()
        );
        DesktopWindowSize apiSize = this.apiDefaultSize(apiDefinition, apiSetup, defaultWindowWidth, defaultWindowHeight);
        this.logApiWindowBuild(
            "session",
            session.menu(),
            session.title(),
            session.sessionId(),
            session.sourceKey(),
            session.containerSlots().size(),
            session.contentWidth(),
            session.contentHeight(),
            defaultWindowWidth,
            defaultWindowHeight,
            apiSize,
            apiDefinition
        );
        if (session.specialKind() == DesktopPackets.SPECIAL_CAMEL || session.specialKind() == DesktopPackets.SPECIAL_LLAMA) {
            DesktopDebug.warn(
                "SIU_MOUNT_DIAG client_add_session_size desktop={} session={} special={} default={}x{} api={}x{} mountSession={}",
                this.desktopId,
                session.sessionId(),
                session.specialKind(),
                defaultWindowWidth,
                defaultWindowHeight,
                apiSize.width(),
                apiSize.height(),
                session.isMountSession()
            );
        }
        InventoryWindow window = new InventoryWindow(
            WindowKind.CONTAINER,
            session.title(),
            0,
            0,
            apiSize.width(),
            apiSize.height(),
            session
        );
        this.initializeApiWindow(window, apiDefinition, apiSetup);
        this.placeOrRestoreWindow(window, WindowPlacement.CONTAINER);
        boolean promoteHiddenGhost = !visible && window.pinMode == PinMode.GHOST_PINNED && this.hasInteractiveWindows();
        if (!visible && window.pinMode == PinMode.GHOST_PINNED && !promoteHiddenGhost) {
            window.ghosted = true;
            window.focused = false;
            window.minimized = false;
        }
        this.windows.add(window);
        this.apiOpened(window);
        if (window.ghosted) {
            this.closeIfEmpty();
        } else {
            this.setFocusedWindow(window);
            this.hotbarOnly = false;
            if (promoteHiddenGhost) {
                DesktopContainerClient.setSessionVisible(session.sessionId(), true);
            }
            this.promoteGhostWindowsForDesktopOpen(window);
        }
        DesktopDebug.log(
            "client session window add desktop={} session={} title={} visible={} ghosted={} replacedSession={} replacedWindow={} replacedSource={} windows={} sessions={}",
            this.desktopId,
            session.sessionId(),
            session.title().getString(),
            visible,
            window.ghosted,
            replacedSession,
            replacedWindow,
            replacedSource,
            this.windows.size(),
            this.sessions.size()
        );
    }

    private DesktopWindowDefinition<?, ?> apiDefinitionFor(
        AbstractContainerMenu menu,
        Component title,
        List<Slot> slots,
        int contentWidth,
        int contentHeight,
        int sessionId,
        String sourceKey,
        int specialKind
    ) {
        MenuType<?> menuType = safeMenuType(menu);
        if (menuType == null) {
            return null;
        }
        var menuKey = BuiltInRegistries.MENU.getKey(menuType);
        DesktopWindowLookupContext context = new DesktopWindowLookupContext(
            menu,
            menuType,
            title,
            sessionId,
            sourceKey,
            specialKind,
            slots,
            contentWidth,
            contentHeight
        );
        DesktopWindowDefinition<?, ?> definition = SaltsInventoryDesktopApi.findDefinition(context);
        boolean tomMenu = isTomStorageMenu(menuKey);
        if (definition != null || tomMenu) {
            DesktopDebug.log(
                "client api lookup desktop={} menu={} menuType={} title='{}' session={} source='{}' special={} slots={} content={}x{} definition={}",
                this.desktopId,
                menuKey,
                menuType,
                title.getString(),
                sessionId,
                sourceKey,
                specialKind,
                slots.size(),
                contentWidth,
                contentHeight,
                definitionName(definition)
            );
            if (tomMenu) {
                TomsStorageCompat.info(
                    "client api lookup menu={} title='{}' session={} source='{}' slots={} content={}x{} definition={}",
                    menuKey,
                    title.getString(),
                    sessionId,
                    sourceKey,
                    slots.size(),
                    contentWidth,
                    contentHeight,
                    definitionName(definition)
                );
            }
        }
        if (definition == null && tomMenu) {
            DesktopDebug.warn(
                "Tom's Storage desktop compat did not resolve a window definition menu={} title='{}' session={} source='{}' slots={} content={}x{}; Salt will render the generic fallback",
                menuKey,
                title.getString(),
                sessionId,
                sourceKey,
                slots.size(),
                contentWidth,
                contentHeight
            );
            TomsStorageCompat.warn(
                "client api lookup failed menu={} title='{}' session={} source='{}' slots={} content={}x{}; generic fallback will render",
                menuKey,
                title.getString(),
                sessionId,
                sourceKey,
                slots.size(),
                contentWidth,
                contentHeight
            );
        }
        return definition;
    }

    private void logApiWindowBuild(
        String path,
        AbstractContainerMenu menu,
        Component title,
        int sessionId,
        String sourceKey,
        int slotCount,
        int contentWidth,
        int contentHeight,
        int defaultWindowWidth,
        int defaultWindowHeight,
        DesktopWindowSize apiSize,
        @Nullable DesktopWindowDefinition<?, ?> apiDefinition
    ) {
        MenuType<?> menuType = safeMenuType(menu);
        if (menuType == null) {
            return;
        }
        var menuKey = BuiltInRegistries.MENU.getKey(menuType);
        if (apiDefinition == null && !isTomStorageMenu(menuKey)) {
            return;
        }

        DesktopDebug.log(
            "client api window build desktop={} path={} menu={} title='{}' session={} source='{}' slots={} content={}x{} default={}x{} final={}x{} definition={}",
            this.desktopId,
            path,
            menuKey,
            title.getString(),
            sessionId,
            sourceKey,
            slotCount,
            contentWidth,
            contentHeight,
            defaultWindowWidth,
            defaultWindowHeight,
            apiSize.width(),
            apiSize.height(),
            definitionName(apiDefinition)
        );
        if (isTomStorageMenu(menuKey)) {
            TomsStorageCompat.info(
                "client window build path={} menu={} title='{}' session={} source='{}' slots={} content={}x{} default={}x{} final={}x{} definition={}",
                path,
                menuKey,
                title.getString(),
                sessionId,
                sourceKey,
                slotCount,
                contentWidth,
                contentHeight,
                defaultWindowWidth,
                defaultWindowHeight,
                apiSize.width(),
                apiSize.height(),
                definitionName(apiDefinition)
            );
        }
    }

    private static boolean isTomStorageMenu(@Nullable Identifier menuKey) {
        return menuKey != null && "toms_storage".equals(menuKey.getNamespace());
    }

    private static String definitionName(@Nullable DesktopWindowDefinition<?, ?> definition) {
        return definition == null ? "none" : definition.getClass().getName();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DesktopWindowSetupContext<?> apiSetupContext(
        AbstractContainerMenu menu,
        Component title,
        List<Slot> slots,
        int contentWidth,
        int contentHeight,
        int defaultWindowWidth,
        int defaultWindowHeight,
        int sessionId,
        String sourceKey
    ) {
        return new DesktopWindowSetupContext(
            this.minecraft,
            menu,
            title,
            sessionId,
            sourceKey,
            slots,
            contentWidth,
            contentHeight,
            defaultWindowWidth,
            defaultWindowHeight
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DesktopWindowSize apiDefaultSize(
        @Nullable DesktopWindowDefinition definition,
        DesktopWindowSetupContext setup,
        int defaultWidth,
        int defaultHeight
    ) {
        if (definition == null) {
            return DesktopWindowSize.of(defaultWidth, defaultHeight);
        }

        try {
            return definition.defaultSize(setup);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api default size failed desktop={} menu={} reason={}", this.desktopId, setup.menu().getType(), exception.toString());
            return DesktopWindowSize.of(defaultWidth, defaultHeight);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initializeApiWindow(InventoryWindow window, @Nullable DesktopWindowDefinition definition, DesktopWindowSetupContext setup) {
        window.apiDefinition = definition;
        if (definition == null) {
            window.apiState = null;
            return;
        }

        try {
            window.apiState = definition.createState(setup);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api state failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            window.apiState = null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiLoadLocalState(InventoryWindow window, CompoundTag tag) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).loadLocalState(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), tag);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api loadLocalState failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompoundTag apiSaveLocalState(InventoryWindow window) {
        CompoundTag tag = new CompoundTag();
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return tag;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).saveLocalState(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), tag);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api saveLocalState failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
        return tag;
    }

    private @Nullable DesktopContainerSession session(int sessionId) {
        for (DesktopContainerSession session : this.sessions) {
            if (session.sessionId() == sessionId) {
                return session;
            }
        }
        return null;
    }

    private List<Slot> mainInventorySlots() {
        AbstractContainerMenu menu = this.playerMenu();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (InventoryExpansion.isMainInventorySlot(this.player(), slot)) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.comparingInt(InventoryExpansion::storageOrder));
        return slots;
    }

    private int inventoryVirtualSlotCount() {
        return this.mainInventorySlots().size() + (SaltsInventoryConfig.get().expandableInventory ? 1 : 0);
    }

    private List<Slot> hotbarSlots() {
        AbstractContainerMenu menu = this.playerMenu();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (this.isPlayerInventorySlot(slot)
                && slot.index >= 9
                && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < HOTBAR_SLOT_COUNT) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.comparingInt(Slot::getContainerSlot));
        return slots;
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        LocalPlayer player = this.player();
        return player != null && slot.container == player.getInventory();
    }

    private int storageSlotCount(InventoryWindow window) {
        return switch (window.kind) {
            case INVENTORY -> this.inventoryVirtualSlotCount();
            case CONTAINER -> window.containerSlots().size();
            case CHARACTER, CREATIVE -> 0;
        };
    }

    private void ensureInventoryWindowAutoSize(InventoryWindow window) {
        if (window.kind != WindowKind.INVENTORY) {
            return;
        }

        int slotCount = this.inventoryVirtualSlotCount();
        int totalRows = rowsForSlots(slotCount, INVENTORY_DEFAULT_COLUMNS);
        int visibleRows = Math.max(1, Math.min(INVENTORY_MAX_AUTO_VISIBLE_ROWS, Math.max(1, totalRows)));
        boolean scrollbar = totalRows > visibleRows;
        int desiredWidth = Math.max(this.minimumTitleBarWidth(window.title), storageWindowWidth(INVENTORY_DEFAULT_COLUMNS, scrollbar));
        int desiredHeight = storageWindowHeight(visibleRows);
        int maxWidth = Math.max(SLOT_SIZE + WINDOW_CONTENT_PADDING * 2, this.desktopWidth() - WINDOW_PLACEMENT_MARGIN * 2);
        int maxHeight = Math.max(this.minResizableHeight(), this.desktopHeight() - WINDOW_PLACEMENT_MARGIN * 2);
        int minWidth = Math.min(this.minResizableWidth(window), maxWidth);
        int minHeight = Math.min(this.minResizableHeight(), maxHeight);
        window.width = clamp(Math.max(window.width, desiredWidth), minWidth, maxWidth);
        window.height = clamp(Math.max(window.height, desiredHeight), minHeight, maxHeight);
        this.clampStorageScroll(window);
        this.clampWindowIntoDesktop(window);
    }

    private SlotGridLayout storageLayout(InventoryWindow window, int slotCount) {
        int visibleRows = Math.max(1, (window.height - TOP_BAR_HEIGHT - WINDOW_CONTENT_PADDING * 2) / SLOT_SIZE);
        int availableWidth = Math.max(SLOT_SIZE, window.width - WINDOW_CONTENT_PADDING * 2);
        int columns = Math.max(1, availableWidth / SLOT_SIZE);
        int totalRows = rowsForSlots(slotCount, columns);
        boolean scrollable = totalRows > visibleRows;
        if (scrollable) {
            int availableWithScrollbar = Math.max(SLOT_SIZE, availableWidth - SCROLLBAR_RESERVED_WIDTH);
            columns = Math.max(1, availableWithScrollbar / SLOT_SIZE);
            totalRows = rowsForSlots(slotCount, columns);
            scrollable = totalRows > visibleRows;
        }

        return new SlotGridLayout(columns, visibleRows, totalRows, Math.max(0, totalRows - visibleRows), scrollable);
    }

    private void clampStorageScroll(InventoryWindow window) {
        if (!this.isResizableStorageWindow(window)) {
            window.scrollRow = 0;
            return;
        }

        SlotGridLayout layout = this.storageLayout(window, this.storageSlotCount(window));
        window.scrollRow = clamp(window.scrollRow, 0, layout.maxScrollRow());
    }

    private void snapResizableWindow(InventoryWindow window) {
        if (window.apiDefinition != null) {
            DesktopWindowSize minSize = this.apiMinSize(window);
            DesktopWindowSize snapSize = this.apiSnapSize(window);
            int oldWidth = window.width;
            int oldHeight = window.height;
            int minWidth = this.minResizableWidth(window);
            int minHeight = this.minResizableHeight(window);
            int maxWidth = Math.max(minWidth, this.desktopWidth() - window.x);
            int maxHeight = Math.max(minHeight, this.desktopHeight() - window.y);
            int targetWidth = snapSize == null ? window.width : snapSize.width();
            int targetHeight = snapSize == null ? window.height : snapSize.height();
            window.width = clamp(targetWidth, Math.max(minWidth, minSize.width()), maxWidth);
            window.height = clamp(targetHeight, Math.max(minHeight, minSize.height()), maxHeight);
            DesktopDebug.trace(
                "client api resize snap desktop={} window={} old={}x{} new={}x{} requested={}",
                this.desktopId,
                window.debugName(),
                oldWidth,
                oldHeight,
                window.width,
                window.height,
                snapSize == null ? "none" : snapSize.width() + "x" + snapSize.height()
            );
            return;
        }

        if (!this.isResizableStorageWindow(window)) {
            return;
        }

        int slotCount = this.storageSlotCount(window);
        SlotGridLayout layout = this.storageLayout(window, slotCount);
        int columns = layout.columns();
        int visibleRows = layout.scrollable()
            ? layout.visibleRows()
            : Math.max(1, Math.min(layout.visibleRows(), layout.totalRows()));
        if (!layout.scrollable() && slotCount > 0) {
            columns = clamp(rowsForSlots(slotCount, visibleRows), 1, layout.columns());
            visibleRows = Math.max(1, rowsForSlots(slotCount, columns));
        }

        int snappedWidth = storageWindowWidth(columns, layout.scrollable());
        int snappedHeight = storageWindowHeight(visibleRows);
        int oldWidth = window.width;
        int oldHeight = window.height;
        int minWidth = this.minResizableWidth(window);
        window.width = clamp(snappedWidth, minWidth, Math.max(minWidth, this.desktopWidth() - window.x));
        window.height = clamp(snappedHeight, this.minResizableHeight(), Math.max(this.minResizableHeight(), this.desktopHeight() - window.y));
        this.clampStorageScroll(window);
        DesktopDebug.trace(
            "client resize snap desktop={} window={} old={}x{} new={}x{} columns={} rows={} scrollable={}",
            this.desktopId,
            window.debugName(),
            oldWidth,
            oldHeight,
            window.width,
            window.height,
            columns,
            visibleRows,
            layout.scrollable()
        );
    }

    private boolean canResizeWindow(InventoryWindow window) {
        return SaltsInventoryConfig.get().allowResizing
            && !this.cameraControl
            && !window.ghosted
            && !window.locked
            && !window.minimized
            && this.isResizableStorageWindow(window);
    }

    private boolean isResizableStorageWindow(InventoryWindow window) {
        if (window.kind == WindowKind.INVENTORY) {
            return true;
        }

        if (window.kind != WindowKind.CONTAINER) {
            return false;
        }

        if (window.session != null && window.session.isMountSession()) {
            return false;
        }

        AbstractContainerMenu menu = window.containerMenu();
        if (menu == null) {
            return false;
        }

        DesktopResizePolicy apiResizePolicy = this.apiResizePolicy(window);
        if (apiResizePolicy != null) {
            return apiResizePolicy == DesktopResizePolicy.STORAGE_GRID;
        }

        MenuType<?> type = safeMenuType(menu);
        if (isKnownStorageMenuType(type)) {
            return true;
        }

        if (isKnownFunctionalMenuType(type)) {
            return false;
        }

        return isDenseRegularStorageGrid(window.containerSlots());
    }

    private static boolean isKnownStorageMenuType(@Nullable MenuType<?> type) {
        return type == MenuType.GENERIC_9x1
            || type == MenuType.GENERIC_9x2
            || type == MenuType.GENERIC_9x3
            || type == MenuType.GENERIC_9x4
            || type == MenuType.GENERIC_9x5
            || type == MenuType.GENERIC_9x6
            || type == MenuType.GENERIC_3x3
            || type == MenuType.SHULKER_BOX
            || type == MenuType.HOPPER;
    }

    private static boolean isKnownFunctionalMenuType(@Nullable MenuType<?> type) {
        return type == MenuType.CRAFTER_3x3
            || type == MenuType.ANVIL
            || type == MenuType.BEACON
            || type == MenuType.BLAST_FURNACE
            || type == MenuType.BREWING_STAND
            || type == MenuType.CRAFTING
            || type == MenuType.ENCHANTMENT
            || type == MenuType.FURNACE
            || type == MenuType.GRINDSTONE
            || type == MenuType.LOOM
            || type == MenuType.MERCHANT
            || type == MenuType.SMITHING
            || type == MenuType.SMOKER
            || type == MenuType.CARTOGRAPHY_TABLE
            || type == MenuType.STONECUTTER;
    }

    private static boolean isDenseRegularStorageGrid(List<Slot> slots) {
        if (slots.isEmpty()) {
            return false;
        }

        int minX = minSlotX(slots);
        int minY = minSlotY(slots);
        int maxColumn = 0;
        int maxRow = 0;
        Set<Integer> occupied = new HashSet<>();
        for (Slot slot : slots) {
            int dx = slot.x - minX;
            int dy = slot.y - minY;
            if (dx < 0 || dy < 0 || dx % SLOT_SIZE != 0 || dy % SLOT_SIZE != 0) {
                return false;
            }

            int column = dx / SLOT_SIZE;
            int row = dy / SLOT_SIZE;
            maxColumn = Math.max(maxColumn, column);
            maxRow = Math.max(maxRow, row);
            if (!occupied.add(row * 256 + column)) {
                return false;
            }
        }

        return occupied.size() == (maxColumn + 1) * (maxRow + 1);
    }

    private int minResizableWidth(InventoryWindow window) {
        if (window.apiDefinition != null) {
            return Math.max(this.apiMinSize(window).width(), this.minimumTitleBarWidth(window.title));
        }
        return Math.max(WINDOW_CONTENT_PADDING * 2 + SLOT_SIZE + SCROLLBAR_RESERVED_WIDTH, this.minimumTitleBarWidth(window.title));
    }

    private int minResizableHeight(InventoryWindow window) {
        if (window.apiDefinition != null) {
            return this.apiMinSize(window).height();
        }
        return this.minResizableHeight();
    }

    private int minResizableHeight() {
        return TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + SLOT_SIZE;
    }

    private static int storageWindowWidth(int columns, boolean scrollbar) {
        return WINDOW_CONTENT_PADDING * 2 + columns * SLOT_SIZE + (scrollbar ? SCROLLBAR_RESERVED_WIDTH : 0);
    }

    private static int storageWindowHeight(int rows) {
        return TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + rows * SLOT_SIZE;
    }

    private int mountWindowWidth(DesktopContainerSession session, Component title) {
        int storageColumns = this.mountStorageColumns(session);
        int contentWidth = MOUNT_MODEL_X + MOUNT_MODEL_WIDTH;
        if (storageColumns > 0) {
            contentWidth = MOUNT_STORAGE_X + storageColumns * SLOT_SIZE;
        }

        return Math.max(this.minimumTitleBarWidth(title), WINDOW_CONTENT_PADDING * 2 + contentWidth);
    }

    private static int mountWindowHeight() {
        return TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + MOUNT_CONTENT_HEIGHT;
    }

    private int mountStorageColumns(DesktopContainerSession session) {
        int storageSlotCount = Math.max(0, session.containerSlots().size() - MOUNT_STORAGE_START_SLOT);
        if (storageSlotCount <= 0) {
            return 0;
        }

        int neededColumns = rowsForSlots(storageSlotCount, MOUNT_STORAGE_ROWS);
        return session.columns() > 0 ? Math.min(session.columns(), neededColumns) : neededColumns;
    }

    private int containerWindowWidth(AbstractContainerMenu menu, Component title, int slotCount, int contentWidth) {
        int titleWidth = this.minimumTitleBarWidth(this.vanillaTitleBarTitle(menu, title));
        if (menu instanceof AbstractFurnaceMenu) {
            return Math.max(titleWidth, FURNACE_CONTENT_MARGIN * 2 + FURNACE_CONTENT_WIDTH);
        }
        if (menu instanceof CraftingMenu) {
            return Math.max(titleWidth, CRAFTING_TABLE_CONTENT_MARGIN * 2 + CRAFTING_TABLE_CONTENT_WIDTH);
        }
        if (menu instanceof AnvilMenu) {
            return Math.max(titleWidth, ANVIL_CONTENT_MARGIN * 2 + ANVIL_CONTENT_WIDTH);
        }
        if (menu instanceof CrafterMenu) {
            return Math.max(titleWidth, CRAFTER_CONTENT_MARGIN * 2 + CRAFTER_CONTENT_WIDTH);
        }
        if (menu instanceof BeaconMenu) {
            return Math.max(titleWidth, BEACON_CONTENT_MARGIN * 2 + BEACON_CONTENT_WIDTH);
        }
        if (menu instanceof BrewingStandMenu) {
            return Math.max(titleWidth, BREWING_CONTENT_MARGIN * 2 + BREWING_CONTENT_WIDTH);
        }
        if (menu instanceof CartographyTableMenu) {
            return Math.max(titleWidth, CARTOGRAPHY_CONTENT_MARGIN * 2 + CARTOGRAPHY_CONTENT_WIDTH);
        }
        if (menu instanceof SmithingMenu) {
            return Math.max(titleWidth, SMITHING_CONTENT_MARGIN * 2 + SMITHING_CONTENT_WIDTH);
        }
        if (menu instanceof GrindstoneMenu) {
            return Math.max(titleWidth, GRINDSTONE_CONTENT_MARGIN * 2 + GRINDSTONE_CONTENT_WIDTH);
        }
        if (menu instanceof StonecutterMenu) {
            return Math.max(titleWidth, STONECUTTER_CONTENT_MARGIN * 2 + STONECUTTER_CONTENT_WIDTH);
        }
        if (menu instanceof LoomMenu) {
            return Math.max(titleWidth, LOOM_CONTENT_MARGIN * 2 + LOOM_CONTENT_WIDTH);
        }
        if (menu instanceof EnchantmentMenu) {
            return Math.max(titleWidth, ENCHANTMENT_CONTENT_MARGIN * 2 + ENCHANTMENT_CONTENT_WIDTH);
        }
        if (menu instanceof MerchantMenu) {
            return Math.max(titleWidth, MERCHANT_CONTENT_MARGIN * 2 + MERCHANT_CONTENT_WIDTH);
        }
        StorageGridSize defaultGrid = defaultStorageGridSize(safeMenuType(menu), slotCount);
        if (defaultGrid != null) {
            return Math.max(this.minimumTitleBarWidth(title), storageWindowWidth(defaultGrid.columns(), false));
        }

        return Math.max(Math.max(MIN_CONTAINER_WIDTH, titleWidth), WINDOW_CONTENT_PADDING * 2 + contentWidth);
    }

    private static int containerWindowHeight(AbstractContainerMenu menu, int slotCount, int contentHeight) {
        if (menu instanceof AbstractFurnaceMenu) {
            return TOP_BAR_HEIGHT + FURNACE_CONTENT_MARGIN * 2 + FURNACE_CONTENT_HEIGHT;
        }
        if (menu instanceof CraftingMenu) {
            return TOP_BAR_HEIGHT + CRAFTING_TABLE_CONTENT_MARGIN * 2 + CRAFTING_TABLE_CONTENT_HEIGHT;
        }
        if (menu instanceof AnvilMenu) {
            return TOP_BAR_HEIGHT + ANVIL_CONTENT_MARGIN * 2 + ANVIL_CONTENT_HEIGHT;
        }
        if (menu instanceof CrafterMenu) {
            return TOP_BAR_HEIGHT + CRAFTER_CONTENT_MARGIN * 2 + CRAFTER_CONTENT_HEIGHT;
        }
        if (menu instanceof BeaconMenu) {
            return TOP_BAR_HEIGHT + BEACON_CONTENT_MARGIN * 2 + BEACON_CONTENT_HEIGHT;
        }
        if (menu instanceof BrewingStandMenu) {
            return TOP_BAR_HEIGHT + BREWING_CONTENT_MARGIN * 2 + BREWING_CONTENT_HEIGHT;
        }
        if (menu instanceof CartographyTableMenu) {
            return TOP_BAR_HEIGHT + CARTOGRAPHY_CONTENT_MARGIN * 2 + CARTOGRAPHY_CONTENT_HEIGHT;
        }
        if (menu instanceof SmithingMenu) {
            return TOP_BAR_HEIGHT + SMITHING_CONTENT_MARGIN * 2 + SMITHING_CONTENT_HEIGHT;
        }
        if (menu instanceof GrindstoneMenu) {
            return TOP_BAR_HEIGHT + GRINDSTONE_CONTENT_MARGIN * 2 + GRINDSTONE_CONTENT_HEIGHT;
        }
        if (menu instanceof StonecutterMenu) {
            return TOP_BAR_HEIGHT + STONECUTTER_CONTENT_MARGIN * 2 + STONECUTTER_CONTENT_HEIGHT;
        }
        if (menu instanceof LoomMenu) {
            return TOP_BAR_HEIGHT + LOOM_CONTENT_MARGIN * 2 + LOOM_CONTENT_HEIGHT;
        }
        if (menu instanceof EnchantmentMenu) {
            return TOP_BAR_HEIGHT + ENCHANTMENT_CONTENT_MARGIN * 2 + ENCHANTMENT_CONTENT_HEIGHT;
        }
        if (menu instanceof MerchantMenu) {
            return TOP_BAR_HEIGHT + MERCHANT_CONTENT_MARGIN * 2 + MERCHANT_CONTENT_HEIGHT;
        }
        StorageGridSize defaultGrid = defaultStorageGridSize(safeMenuType(menu), slotCount);
        if (defaultGrid != null) {
            return storageWindowHeight(defaultGrid.rows());
        }

        int effectiveContentHeight = contentHeight;
        return Math.max(MIN_CONTAINER_HEIGHT, TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + effectiveContentHeight);
    }

    private static @Nullable StorageGridSize defaultStorageGridSize(@Nullable MenuType<?> type, int slotCount) {
        if (type == MenuType.GENERIC_3x3) {
            return new StorageGridSize(3, 3);
        }

        if (type == MenuType.HOPPER) {
            return new StorageGridSize(Math.max(1, slotCount), 1);
        }

        if (type == MenuType.GENERIC_9x1) {
            return new StorageGridSize(9, 1);
        }
        if (type == MenuType.GENERIC_9x2) {
            return new StorageGridSize(9, 2);
        }
        if (type == MenuType.GENERIC_9x3 || type == MenuType.SHULKER_BOX) {
            return new StorageGridSize(9, 3);
        }
        if (type == MenuType.GENERIC_9x4) {
            return new StorageGridSize(9, 4);
        }
        if (type == MenuType.GENERIC_9x5) {
            return new StorageGridSize(9, 5);
        }
        if (type == MenuType.GENERIC_9x6) {
            return new StorageGridSize(9, 6);
        }

        return null;
    }

    private int fullTitleBarWidth(Component title) {
        return TITLE_LEFT_PADDING + this.font.width(title) + TITLE_TO_CONTROLS_GAP + controlsWidth(FULL_TITLE_CONTROLS);
    }

    private int minimumTitleBarWidth(Component title) {
        return TITLE_LEFT_PADDING + this.font.width(minimumTitleText(title)) + TITLE_TO_CONTROLS_GAP + controlsWidth(COMPACT_TITLE_CONTROLS);
    }

    private static String minimumTitleText(Component title) {
        return minimumTitleText(title.getString());
    }

    private static String minimumTitleText(String title) {
        return title.isEmpty() ? "..." : title.substring(0, 1) + "...";
    }

    private static int controlsWidth(List<WindowControl> controls) {
        return controls.size() * CONTROL_SIZE + controls.size() * CONTROL_GAP + CONTROL_RIGHT_EXTRA_INSET;
    }

    private static int rowsForSlots(int slotCount, int columns) {
        if (slotCount <= 0) {
            return 0;
        }

        return (slotCount + Math.max(1, columns) - 1) / Math.max(1, columns);
    }

    private int clampedWindowX(int preferredX, int windowWidth) {
        return clamp(preferredX, 8, Math.max(8, this.desktopWidth() - windowWidth - 8));
    }

    private int clampedWindowY(int preferredY, int windowHeight) {
        return clamp(preferredY, 8, Math.max(8, this.desktopHeight() - windowHeight - 8));
    }

    private int desktopWidth() {
        if (this.width > 0) {
            return this.width;
        }

        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private int desktopHeight() {
        if (this.height > 0) {
            return this.height;
        }

        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    private void placeWindow(InventoryWindow window, WindowPlacement placement) {
        this.forceFixedWindowSize(window);
        this.clampInitialWindowSize(window);
        WindowPosition position = switch (placement) {
            case CENTER -> this.centeredWindowPosition(window.width, window.height);
            case BOTTOM_LEFT -> this.bottomLeftWindowPosition(window.width, window.height);
            case CONTAINER -> this.containerWindowPosition(window.width, window.height);
        };
        window.x = position.x();
        window.y = position.y();
        DesktopDebug.trace("client window placed desktop={} window={} placement={} x={} y={} size={}x{}", this.desktopId, window.debugName(), placement, window.x, window.y, window.width, window.height);
    }

    private void resetWindowGeometry(InventoryWindow window) {
        DesktopWindowSize defaultSize = this.defaultWindowSize(window);
        int oldX = window.x;
        int oldY = window.y;
        int oldWidth = window.width;
        int oldHeight = window.height;
        window.width = defaultSize.width();
        window.height = defaultSize.height();
        window.scrollRow = 0;
        this.placeWindow(window, this.defaultPlacement(window));
        this.clampStorageScroll(window);
        this.apiMoved(window);
        this.apiResized(window);
        DesktopDebug.trace(
            "client window geometry reset desktop={} window={} old={}x{}@{},{} new={}x{}@{},{}",
            this.desktopId,
            window.debugName(),
            oldWidth,
            oldHeight,
            oldX,
            oldY,
            window.width,
            window.height,
            window.x,
            window.y
        );
    }

    private WindowPlacement defaultPlacement(InventoryWindow window) {
        return switch (window.kind) {
            case INVENTORY, CREATIVE -> WindowPlacement.CENTER;
            case CHARACTER -> WindowPlacement.BOTTOM_LEFT;
            case CONTAINER -> WindowPlacement.CONTAINER;
        };
    }

    private DesktopWindowSize defaultWindowSize(InventoryWindow window) {
        return switch (window.kind) {
            case INVENTORY -> {
                int slotCount = this.inventoryVirtualSlotCount();
                int totalRows = rowsForSlots(slotCount, INVENTORY_DEFAULT_COLUMNS);
                int visibleRows = Math.max(INVENTORY_DEFAULT_VISIBLE_ROWS, Math.min(INVENTORY_MAX_AUTO_VISIBLE_ROWS, Math.max(1, totalRows)));
                yield DesktopWindowSize.of(storageWindowWidth(INVENTORY_DEFAULT_COLUMNS, totalRows > visibleRows), storageWindowHeight(visibleRows));
            }
            case CREATIVE -> DesktopWindowSize.of(CREATIVE_CONTENT_MARGIN * 2 + CREATIVE_CONTENT_WIDTH, TOP_BAR_HEIGHT + CREATIVE_CONTENT_MARGIN * 2 + CREATIVE_CONTENT_HEIGHT);
            case CHARACTER -> DesktopWindowSize.of(CHARACTER_WINDOW_WIDTH, CHARACTER_WINDOW_HEIGHT);
            case CONTAINER -> this.defaultContainerWindowSize(window);
        };
    }

    private DesktopWindowSize defaultContainerWindowSize(InventoryWindow window) {
        AbstractContainerMenu menu = window.containerMenu();
        if (menu == null) {
            return DesktopWindowSize.of(window.width, window.height);
        }
        if (window.session != null && window.session.isMountSession()) {
            return DesktopWindowSize.of(this.mountWindowWidth(window.session, window.title), mountWindowHeight());
        }

        List<Slot> slots = window.containerSlots();
        int contentWidth;
        int contentHeight;
        int sessionId;
        String sourceKey;
        if (window.session != null) {
            contentWidth = window.session.contentWidth();
            contentHeight = window.session.contentHeight();
            sessionId = window.session.sessionId();
            sourceKey = window.session.sourceKey();
        } else {
            int minX = minSlotX(slots);
            int minY = minSlotY(slots);
            contentWidth = containerContentWidth(slots, minX);
            contentHeight = containerContentHeight(slots, minY);
            sessionId = LEGACY_MENU_SESSION;
            sourceKey = "";
        }

        int defaultWidth = this.containerWindowWidth(menu, window.title, slots.size(), contentWidth);
        int defaultHeight = containerWindowHeight(menu, slots.size(), contentHeight);
        DesktopWindowSetupContext<?> setup = this.apiSetupContext(menu, window.title, slots, contentWidth, contentHeight, defaultWidth, defaultHeight, sessionId, sourceKey);
        return this.apiDefaultSize(window.apiDefinition, setup, defaultWidth, defaultHeight);
    }

    private void placeOrRestoreWindow(InventoryWindow window, WindowPlacement placement) {
        DesktopWindowStateStore.WindowState state = this.loadWindowState(window);
        if (state == null) {
            this.placeWindow(window, placement);
            this.syncSessionPinMode(window);
            return;
        }

        window.locked = state.locked;
        window.pinMode = this.effectivePinMode(state.pinMode());
        this.apiLoadLocalState(window, state.localState());
        if (state.width > 0 && state.height > 0) {
            window.width = state.width;
            window.height = state.height;
        }
        this.forceFixedWindowSize(window);

        this.clampInitialWindowSize(window);
        if (window.pinMode == PinMode.UNPINNED) {
            this.placeWindow(window, placement);
        } else {
            window.x = state.x;
            window.y = state.y;
            this.clampWindowIntoDesktop(window);
            DesktopDebug.trace(
                "client window restored desktop={} window={} pin={} x={} y={} size={}x{}",
                this.desktopId,
                window.debugName(),
                window.pinMode,
                window.x,
                window.y,
                window.width,
                window.height
            );
        }
        this.syncSessionPinMode(window);
    }

    private DesktopWindowStateStore.WindowState loadWindowState(InventoryWindow window) {
        String key = window.stateKey();
        if (key == null) {
            return null;
        }

        return DesktopWindowStateStore.load(this.minecraft == null ? Minecraft.getInstance() : this.minecraft, key).orElse(null);
    }

    private void saveWindowState(InventoryWindow window) {
        this.saveWindowState(window, true);
    }

    private void saveWindowState(InventoryWindow window, boolean syncSession) {
        String key = window.stateKey();
        if (key == null) {
            if (syncSession) {
                this.syncSessionPinMode(window);
            }
            return;
        }

        DesktopWindowStateStore.WindowState state = new DesktopWindowStateStore.WindowState(window.x, window.y, window.width, window.height, window.locked, window.pinMode);
        state.localState(this.apiSaveLocalState(window));
        DesktopWindowStateStore.save(
            this.minecraft == null ? Minecraft.getInstance() : this.minecraft,
            key,
            state
        );
        if (syncSession) {
            this.syncSessionPinMode(window);
        }
        DesktopDebug.trace(
            "client window state save desktop={} key={} window={} pin={} locked={} x={} y={} size={}x{}",
            this.desktopId,
            key,
            window.debugName(),
            window.pinMode,
            window.locked,
            window.x,
            window.y,
            window.width,
            window.height
        );
    }

    private void syncSessionPinMode(InventoryWindow window) {
        if (window.session != null) {
            DesktopContainerClient.setSessionPinMode(window.session.sessionId(), window.pinMode);
        }
    }

    private PinMode effectivePinMode(PinMode pinMode) {
        if (pinMode == PinMode.GHOST_PINNED && !SaltsInventoryConfig.get().enableGhostPins) {
            return PinMode.PINNED;
        }
        return pinMode;
    }

    private void clampWindowIntoDesktop(InventoryWindow window) {
        window.x = this.clampedWindowX(window.x, window.width);
        window.y = this.clampedWindowY(window.y, window.minimized ? TOP_BAR_HEIGHT : window.height);
    }

    private void forceFixedWindowSize(InventoryWindow window) {
        if (window.kind == WindowKind.CHARACTER) {
            window.width = CHARACTER_WINDOW_WIDTH;
            window.height = CHARACTER_WINDOW_HEIGHT;
        } else if (window.session != null && window.session.isMountSession()) {
            window.width = this.mountWindowWidth(window.session, window.title);
            window.height = mountWindowHeight();
        } else if (window.kind == WindowKind.CONTAINER && !this.isResizableStorageWindow(window)) {
            DesktopWindowSize fixedSize = this.defaultContainerWindowSize(window);
            window.width = fixedSize.width();
            window.height = fixedSize.height();
        }
    }

    private void clampInitialWindowSize(InventoryWindow window) {
        if (window.apiDefinition != null) {
            DesktopWindowSize minSize = this.apiMinSize(window);
            window.width = Math.max(window.width, minSize.width());
            window.height = Math.max(window.height, minSize.height());
        }

        if (!this.isResizableStorageWindow(window)) {
            return;
        }

        int maxWidth = Math.max(SLOT_SIZE + WINDOW_CONTENT_PADDING * 2, this.desktopWidth() - WINDOW_PLACEMENT_MARGIN * 2);
        int maxHeight = Math.max(TOP_BAR_HEIGHT + SLOT_SIZE + WINDOW_CONTENT_PADDING * 2, this.desktopHeight() - WINDOW_PLACEMENT_MARGIN * 2);
        int minWidth = Math.min(this.minResizableWidth(window), maxWidth);
        int minHeight = Math.min(this.minResizableHeight(window), maxHeight);
        window.width = clamp(window.width, minWidth, maxWidth);
        window.height = clamp(window.height, minHeight, maxHeight);
        this.clampStorageScroll(window);
    }

    private WindowPosition centeredWindowPosition(int windowWidth, int windowHeight) {
        return new WindowPosition(
            this.clampedWindowX((this.desktopWidth() - windowWidth) / 2, windowWidth),
            this.clampedWindowY((this.desktopHeight() - windowHeight) / 2, windowHeight)
        );
    }

    private WindowPosition bottomLeftWindowPosition(int windowWidth, int windowHeight) {
        return new WindowPosition(
            this.clampedWindowX(WINDOW_PLACEMENT_MARGIN, windowWidth),
            this.clampedWindowY(this.desktopHeight() - windowHeight - WINDOW_PLACEMENT_MARGIN, windowHeight)
        );
    }

    private WindowPosition containerWindowPosition(int windowWidth, int windowHeight) {
        return switch (SaltsInventoryConfig.get().windowOpeningStyle()) {
            case TOP_OUTSIDE -> this.topOutsideContainerWindowPosition(windowWidth, windowHeight);
            case AROUND_INVENTORY -> this.anchoredContainerWindowPosition(windowWidth, windowHeight, false);
            case VINTAGE_STORY -> this.anchoredContainerWindowPosition(windowWidth, windowHeight, true);
        };
    }

    private WindowPosition topOutsideContainerWindowPosition(int windowWidth, int windowHeight) {
        List<WindowPosition> candidates = new ArrayList<>();
        int topY = WINDOW_PLACEMENT_MARGIN;
        int centerX = (this.desktopWidth() - windowWidth) / 2;
        int rightX = this.desktopWidth() - windowWidth - WINDOW_PLACEMENT_MARGIN;
        int leftX = WINDOW_PLACEMENT_MARGIN;

        this.addPlacementCandidate(candidates, centerX, topY);

        List<Integer> rightYs = this.stackedColumnYs(rightX, windowWidth, windowHeight);
        List<Integer> leftYs = this.stackedColumnYs(leftX, windowWidth, windowHeight);
        if (!rightYs.isEmpty()) {
            this.addPlacementCandidate(candidates, rightX, rightYs.get(0));
        }
        if (!leftYs.isEmpty()) {
            this.addPlacementCandidate(candidates, leftX, leftYs.get(0));
        }

        int stackCount = Math.max(rightYs.size(), leftYs.size());
        for (int i = 1; i < stackCount; i++) {
            if (i < rightYs.size()) {
                this.addPlacementCandidate(candidates, rightX, rightYs.get(i));
            }
            if (i < leftYs.size()) {
                this.addPlacementCandidate(candidates, leftX, leftYs.get(i));
            }
        }

        for (WindowPosition candidate : candidates) {
            if (this.canPlaceWindowAt(candidate.x(), candidate.y(), windowWidth, windowHeight)) {
                return candidate;
            }
        }

        WindowPosition free = this.findFreeWindowPosition(windowWidth, windowHeight);
        if (free != null) {
            return free;
        }

        int cascade = this.windows.size() % 8 * WINDOW_CASCADE_OFFSET;
        return new WindowPosition(
            this.clampedWindowX(centerX + cascade, windowWidth),
            this.clampedWindowY(topY + cascade, windowHeight)
        );
    }

    private WindowPosition anchoredContainerWindowPosition(int windowWidth, int windowHeight, boolean vintageStory) {
        WindowBounds anchor = this.inventoryPlacementAnchor();
        List<WindowPosition> candidates = new ArrayList<>();
        if (vintageStory) {
            this.addVintageStoryPlacementCandidates(candidates, anchor, windowWidth, windowHeight);
        } else {
            this.addAroundInventoryPlacementCandidates(candidates, anchor, windowWidth, windowHeight);
        }

        for (WindowPosition candidate : candidates) {
            if (this.canPlaceWindowAt(candidate.x(), candidate.y(), windowWidth, windowHeight, anchor)) {
                return candidate;
            }
        }

        WindowPosition nearest = this.findNearestFreeWindowPosition(windowWidth, windowHeight, anchor);
        if (nearest != null) {
            return nearest;
        }

        return this.topOutsideContainerWindowPosition(windowWidth, windowHeight);
    }

    private void addAroundInventoryPlacementCandidates(List<WindowPosition> candidates, WindowBounds anchor, int windowWidth, int windowHeight) {
        int centeredX = anchor.x() + (anchor.width() - windowWidth) / 2;
        int centeredY = anchor.y() + (anchor.height() - windowHeight) / 2;
        int rightX = anchor.right() + WINDOW_PLACEMENT_GAP;
        int leftX = anchor.x() - windowWidth - WINDOW_PLACEMENT_GAP;
        int topY = anchor.y() - windowHeight - WINDOW_PLACEMENT_GAP;
        int bottomY = anchor.bottom() + WINDOW_PLACEMENT_GAP;

        this.addPlacementCandidate(candidates, centeredX, topY);
        this.addPlacementCandidate(candidates, rightX, centeredY);
        this.addPlacementCandidate(candidates, leftX, centeredY);
        this.addPlacementCandidate(candidates, rightX, topY);
        this.addPlacementCandidate(candidates, leftX, topY);
        this.addPlacementCandidate(candidates, rightX, bottomY);
        this.addPlacementCandidate(candidates, leftX, bottomY);
    }

    private void addVintageStoryPlacementCandidates(List<WindowPosition> candidates, WindowBounds anchor, int windowWidth, int windowHeight) {
        int rightX = anchor.right() + WINDOW_PLACEMENT_GAP;
        int leftX = anchor.x() - windowWidth - WINDOW_PLACEMENT_GAP;
        int middleY = anchor.y() + (anchor.height() - windowHeight) / 2;
        int topY = anchor.y();
        int bottomY = anchor.bottom() - windowHeight;

        this.addPlacementCandidate(candidates, rightX, middleY);
        this.addPlacementCandidate(candidates, rightX, topY);
        this.addPlacementCandidate(candidates, rightX, bottomY);
        this.addPlacementCandidate(candidates, leftX, middleY);
        this.addPlacementCandidate(candidates, leftX, topY);
        this.addPlacementCandidate(candidates, leftX, bottomY);
    }

    private WindowBounds inventoryPlacementAnchor() {
        for (InventoryWindow window : this.windows) {
            if (window.kind == WindowKind.INVENTORY && !window.ghosted) {
                return this.visibleWindowBounds(window);
            }
        }

        int inventorySlotCount = this.inventoryVirtualSlotCount();
        int totalRows = rowsForSlots(inventorySlotCount, INVENTORY_DEFAULT_COLUMNS);
        int visibleRows = Math.max(INVENTORY_DEFAULT_VISIBLE_ROWS, Math.min(INVENTORY_MAX_AUTO_VISIBLE_ROWS, Math.max(1, totalRows)));
        boolean scrollbar = totalRows > visibleRows;
        int inventoryWidth = Math.max(this.minimumTitleBarWidth(Component.literal("Inventory")), storageWindowWidth(INVENTORY_DEFAULT_COLUMNS, scrollbar));
        int inventoryHeight = storageWindowHeight(visibleRows);
        DesktopWindowStateStore.WindowState state = DesktopWindowStateStore
            .load(this.minecraft == null ? Minecraft.getInstance() : this.minecraft, "local:inventory")
            .orElse(null);
        if (state != null && state.pinMode() != PinMode.UNPINNED) {
            int width = state.width > 0 ? state.width : inventoryWidth;
            int height = state.height > 0 ? state.height : inventoryHeight;
            return new WindowBounds(
                this.clampedWindowX(state.x, width),
                this.clampedWindowY(state.y, height),
                width,
                height
            );
        }

        WindowPosition position = this.centeredWindowPosition(inventoryWidth, inventoryHeight);
        return new WindowBounds(position.x(), position.y(), inventoryWidth, inventoryHeight);
    }

    private @Nullable WindowPosition findNearestFreeWindowPosition(int windowWidth, int windowHeight, WindowBounds anchor) {
        int maxX = this.desktopWidth() - windowWidth - WINDOW_PLACEMENT_MARGIN;
        int maxY = this.desktopHeight() - windowHeight - WINDOW_PLACEMENT_MARGIN;
        if (maxX < WINDOW_PLACEMENT_MARGIN || maxY < WINDOW_PLACEMENT_MARGIN) {
            return null;
        }

        double anchorCenterX = anchor.x() + anchor.width() / 2.0D;
        double anchorCenterY = anchor.y() + anchor.height() / 2.0D;
        WindowPosition best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = WINDOW_PLACEMENT_MARGIN; y <= maxY; y += WINDOW_PLACEMENT_GAP) {
            for (int x = WINDOW_PLACEMENT_MARGIN; x <= maxX; x += WINDOW_PLACEMENT_GAP) {
                if (!this.canPlaceWindowAt(x, y, windowWidth, windowHeight, anchor)) {
                    continue;
                }

                double centerX = x + windowWidth / 2.0D;
                double centerY = y + windowHeight / 2.0D;
                double distance = (centerX - anchorCenterX) * (centerX - anchorCenterX)
                    + (centerY - anchorCenterY) * (centerY - anchorCenterY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new WindowPosition(x, y);
                }
            }
        }
        return best;
    }

    private void addPlacementCandidate(List<WindowPosition> candidates, int x, int y) {
        WindowPosition candidate = new WindowPosition(x, y);
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private List<Integer> stackedColumnYs(int x, int windowWidth, int windowHeight) {
        List<Integer> ys = new ArrayList<>();
        int y = WINDOW_PLACEMENT_MARGIN;
        for (int i = 0; i <= this.windows.size(); i++) {
            if (!ys.contains(y)) {
                ys.add(y);
            }

            int blockingBottom = this.blockingWindowBottom(x, y, windowWidth, windowHeight);
            if (blockingBottom < 0) {
                break;
            }

            y = blockingBottom + WINDOW_PLACEMENT_GAP;
            if (y + Math.min(windowHeight, TOP_BAR_HEIGHT) > this.desktopHeight() - WINDOW_PLACEMENT_MARGIN) {
                break;
            }
        }
        return ys;
    }

    private int blockingWindowBottom(int x, int y, int windowWidth, int windowHeight) {
        WindowBounds candidate = new WindowBounds(x, y, windowWidth, windowHeight);
        int bottom = -1;
        for (InventoryWindow window : this.windows) {
            WindowBounds bounds = this.visibleWindowBounds(window);
            if (candidate.intersectsWithGap(bounds, WINDOW_PLACEMENT_GAP)) {
                bottom = Math.max(bottom, bounds.bottom());
            }
        }
        return bottom;
    }

    private @Nullable WindowPosition findFreeWindowPosition(int windowWidth, int windowHeight) {
        int maxX = this.desktopWidth() - windowWidth - WINDOW_PLACEMENT_MARGIN;
        int maxY = this.desktopHeight() - windowHeight - WINDOW_PLACEMENT_MARGIN;
        if (maxX < WINDOW_PLACEMENT_MARGIN || maxY < WINDOW_PLACEMENT_MARGIN) {
            return null;
        }

        for (int y = WINDOW_PLACEMENT_MARGIN; y <= maxY; y += WINDOW_PLACEMENT_GAP) {
            for (int x = WINDOW_PLACEMENT_MARGIN; x <= maxX; x += WINDOW_PLACEMENT_GAP) {
                if (this.canPlaceWindowAt(x, y, windowWidth, windowHeight)) {
                    return new WindowPosition(x, y);
                }
            }
        }
        return null;
    }

    private boolean canPlaceWindowAt(int x, int y, int windowWidth, int windowHeight) {
        return this.canPlaceWindowAt(x, y, windowWidth, windowHeight, null);
    }

    private boolean canPlaceWindowAt(int x, int y, int windowWidth, int windowHeight, @Nullable WindowBounds reservedBounds) {
        if (x < WINDOW_PLACEMENT_MARGIN
            || y < WINDOW_PLACEMENT_MARGIN
            || x + windowWidth > this.desktopWidth() - WINDOW_PLACEMENT_MARGIN
            || y + windowHeight > this.desktopHeight() - WINDOW_PLACEMENT_MARGIN) {
            return false;
        }

        WindowBounds candidate = new WindowBounds(x, y, windowWidth, windowHeight);
        if (reservedBounds != null && candidate.intersectsWithGap(reservedBounds, WINDOW_PLACEMENT_GAP)) {
            return false;
        }
        for (InventoryWindow window : this.windows) {
            if (candidate.intersectsWithGap(this.visibleWindowBounds(window), WINDOW_PLACEMENT_GAP)) {
                return false;
            }
        }
        return true;
    }

    private WindowBounds visibleWindowBounds(InventoryWindow window) {
        int x = window.x;
        int y = window.y;
        int right = window.x + window.width;
        int bottom = window.y + (window.minimized ? TOP_BAR_HEIGHT : window.height);
        if (window.kind == WindowKind.CREATIVE && !window.minimized) {
            int tabsX = creativeContentX(window) + CREATIVE_TAB_X_OFFSET;
            int tabsRight = tabsX + CREATIVE_TAB_WIDTH * CREATIVE_TABS_PER_ROW;
            x = Math.min(x, tabsX);
            y = Math.min(y, creativeTopTabsY(window));
            right = Math.max(right, tabsRight);
            bottom = Math.max(bottom, creativeBottomTabsY(window) + CREATIVE_TAB_HEIGHT);
        }
        return WindowBounds.fromEdges(x, y, right, bottom);
    }

    private void closeIfEmpty() {
        if (this.hasInteractiveWindows()) {
            return;
        }

        if (this.sharedCarried.isEmpty()) {
            this.hotbarOnly = false;
            this.hideFromScreen();
        } else {
            this.hotbarOnly = true;
            this.showIfNeeded(this.minecraft == null ? Minecraft.getInstance() : this.minecraft);
        }
    }

    private void clearPopupStateFor(InventoryWindow window) {
        if (this.popupWindow == window) {
            this.popupWindow = null;
        }
        if (this.editingAnvilWindow == window) {
            this.editingAnvilWindow = null;
        }
        if (this.editingCreativeSearchWindow == window) {
            this.editingCreativeSearchWindow = null;
        }
        if (this.scrollingCreativeWindow == window) {
            this.scrollingCreativeWindow = null;
        }
        if (this.scrollingStorageWindow == window) {
            this.scrollingStorageWindow = null;
        }
        if (this.pressedControlWindow == window) {
            this.pressedControlWindow = null;
            this.pressedControl = null;
            this.pressedControlInPopup = false;
        }
    }

    private void clearSlotInteractionState(String reason) {
        if (this.pendingSlotClick != null || this.dragDistribution != null || this.dragStartSlot != null) {
            DesktopDebug.trace(
                "client clear slot interaction desktop={} reason={} pending={} drag={} dragStart={}",
                this.desktopId,
                reason,
                this.describePendingClick(),
                this.dragDistribution == null ? "none" : "button=" + this.dragDistribution.button() + ",type=" + this.dragDistribution.quickCraftType() + ",slots=" + this.dragDistribution.size(),
                this.dragStartSlot == null ? "none" : this.dragStartSlot
            );
        }
        this.pendingSlotClick = null;
        this.dragDistribution = null;
        this.dragStartSlot = null;
    }

    private void showIfNeeded(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        if (!this.hasDesktopSurface()) {
            this.hideFromScreen();
            return;
        }

        if (minecraft.gui.screen() != this) {
            DesktopDebug.log("client screen show desktop={} windows={} sessions={} hotbarOnly={}", this.desktopId, this.windows.size(), this.sessions.size(), this.hotbarOnly);
            minecraft.gui.setScreen(this);
        }
    }

    private void hideFromScreen() {
        Minecraft minecraft = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        if (minecraft.gui.screen() == this) {
            DesktopDebug.log("client screen hide desktop={} windows={} sessions={}", this.desktopId, this.windows.size(), this.sessions.size());
            minecraft.gui.setScreen(null);
        }
    }

    private boolean hasDesktopSurface() {
        return this.hotbarOnly || this.hasInteractiveWindows() || !this.sharedCarried.isEmpty();
    }

    private boolean hasInteractiveWindows() {
        for (InventoryWindow window : this.windows) {
            if (!window.ghosted) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOnlyGhostWindows() {
        return !this.windows.isEmpty() && !this.hasInteractiveWindows();
    }

    private void closeAllWindowsAndHide() {
        DesktopDebug.log("client close all desktop={} windows={} sessions={}", this.desktopId, this.windows.size(), this.sessions.size());
        this.updateBundleHover(null);
        for (InventoryWindow window : List.copyOf(this.windows)) {
            this.closeWindow(window, "close-all");
        }
        this.hotbarOnly = false;
        this.movingWindow = null;
        this.resizingWindow = null;
        this.popupWindow = null;
        this.editingAnvilWindow = null;
        this.editingCreativeSearchWindow = null;
        this.pressedControlWindow = null;
        this.pressedControl = null;
        this.pressedControlInPopup = false;
        this.scrollingCreativeWindow = null;
        this.scrollingStorageWindow = null;
        this.clearSlotInteractionState("close-all");
        this.usingWorld = false;
        this.closeIfEmpty();
    }

    private void clearForOwnerChange(String reason) {
        DesktopDebug.log("client clear desktop={} reason={} windows={} sessions={}", this.desktopId, reason, this.windows.size(), this.sessions.size());
        for (InventoryWindow window : this.windows) {
            this.rememberCreativeWindow(window);
            this.saveWindowState(window);
        }
        this.windows.clear();
        this.sessions.clear();
        this.owner = null;
        this.hotbarOnly = false;
        this.cameraControl = false;
        this.movingWindow = null;
        this.resizingWindow = null;
        this.pressedControlWindow = null;
        this.pressedControl = null;
        this.pressedControlInPopup = false;
        this.popupWindow = null;
        this.editingAnvilWindow = null;
        this.editingCreativeSearchWindow = null;
        this.scrollingCreativeWindow = null;
        this.scrollingStorageWindow = null;
        this.rememberedCreativeTab = null;
        this.rememberedCreativeScrollRow = 0;
        this.rememberedCreativeSearch = "";
        this.hoveredBundleSlotKey = null;
        this.clearSlotInteractionState("owner-change");
        this.usingWorld = false;
        this.sharedCarried = ItemStack.EMPTY;
        this.stopWorldAttack();
    }

    private void activateControl(InventoryWindow window, WindowControl control) {
        DesktopDebug.log("client control desktop={} window={} control={}", this.desktopId, window.debugName(), control);
        switch (control) {
            case CLOSE -> {
                this.closeWindow(window, "control-close");
            }
            case MINIMIZE -> {
                window.minimized = !window.minimized;
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
                this.saveWindowState(window);
            }
            case FOCUS -> {
                this.setFocusedWindow(window.focused ? null : window);
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
            }
            case PIN -> {
                window.pinMode = window.pinMode.next(SaltsInventoryConfig.get().enableGhostPins);
                if (window.pinMode != PinMode.GHOST_PINNED && window.ghosted) {
                    this.promoteGhostWindow(window);
                } else {
                    this.saveWindowState(window);
                }
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
            }
            case LOCK -> {
                boolean locking = !window.locked;
                window.locked = locking;
                this.movingWindow = null;
                if (window.locked) {
                    this.resizingWindow = null;
                }
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
                if (locking && SaltsInventoryConfig.get().resetLockedWindows) {
                    this.resetWindowGeometry(window);
                }
                this.saveWindowState(window);
            }
            case ELLIPSIS -> this.popupWindow = this.popupWindow == window ? null : window;
        }
    }

    private boolean shouldUseCursorWorldTarget() {
        return !this.cameraControl
            && (this.hasWindows() || this.hotbarOnly)
            && this.minecraft != null
            && this.minecraft.player != null
            && this.minecraft.level != null;
    }

    private void updateCursorWorldTarget() {
        if (!this.shouldUseCursorWorldTarget()) {
            return;
        }

        double mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
        double mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
        CursorWorldInteraction.updateHitResultAtCursor(this.minecraft, mouseX, mouseY);
    }

    private boolean isPointOverInteractiveUi(double mouseX, double mouseY) {
        return this.windowAt(mouseX, mouseY) != null
            || this.recipeBookWindowAt(mouseX, mouseY) != null
            || this.hotbarSlotAt(mouseX, mouseY) != null
            || this.offhandSlotAt(mouseX, mouseY) != null;
    }

    private boolean scrollHotbar(double scrollY) {
        if (scrollY == 0.0D || this.minecraft == null || this.minecraft.player == null) {
            return false;
        }

        Inventory inventory = this.minecraft.player.getInventory();
        int direction = scrollY < 0.0D ? 1 : -1;
        int selected = Math.floorMod(inventory.getSelectedSlot() + direction, HOTBAR_SLOT_COUNT);
        this.selectHotbarSlot(selected);
        return true;
    }

    private void selectHotbarSlot(int slot) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        int selected = clamp(slot, 0, HOTBAR_SLOT_COUNT - 1);
        Inventory inventory = this.minecraft.player.getInventory();
        if (inventory.getSelectedSlot() == selected) {
            return;
        }

        DesktopDebug.trace("client hotbar select desktop={} old={} new={}", this.desktopId, inventory.getSelectedSlot(), selected);
        inventory.setSelectedSlot(selected);
        this.minecraft.player.connection.getConnection().send(new ServerboundSetCarriedItemPacket(selected));
    }

    private void closeLegacyContainer(InventoryWindow window) {
        if (this.minecraft == null || this.minecraft.player == null || window.legacyMenu == null) {
            return;
        }

        DesktopDebug.log("client legacy close desktop={} container={}", this.desktopId, window.legacyMenu.containerId);
        if (this.minecraft.player.containerMenu == window.legacyMenu) {
            this.minecraft.player.connection.send(new ServerboundContainerClosePacket(window.legacyMenu.containerId));
            this.minecraft.player.containerMenu = this.minecraft.player.inventoryMenu;
        } else {
            window.legacyMenu.removed(this.minecraft.player);
        }
    }

    private void closeWindow(InventoryWindow window, String reason) {
        this.updateBundleHover(null);
        this.clearSlotInteractionState("window-close");
        this.rememberCreativeWindow(window);
        this.saveWindowState(window);
        this.clearPopupStateFor(window);
        this.editingAnvilWindow = this.editingAnvilWindow == window ? null : this.editingAnvilWindow;
        this.editingCreativeSearchWindow = this.editingCreativeSearchWindow == window ? null : this.editingCreativeSearchWindow;
        this.scrollingCreativeWindow = this.scrollingCreativeWindow == window ? null : this.scrollingCreativeWindow;
        this.scrollingStorageWindow = this.scrollingStorageWindow == window ? null : this.scrollingStorageWindow;
        this.movingWindow = this.movingWindow == window ? null : this.movingWindow;
        this.resizingWindow = this.resizingWindow == window ? null : this.resizingWindow;

        if (window.pinMode == PinMode.GHOST_PINNED && SaltsInventoryConfig.get().enableGhostPins) {
            this.demoteGhostWindow(window, true);
            DesktopDebug.log("client window ghost desktop={} window={} reason={}", this.desktopId, window.debugName(), reason);
            this.closeIfEmpty();
            return;
        }

        if (window.session != null) {
            DesktopContainerClient.closeSession(window.session.sessionId());
            this.sessions.remove(window.session);
        } else if (window.legacyMenu != null) {
            this.closeLegacyContainer(window);
        }

        this.apiClosed(window);
        this.windows.remove(window);
        DesktopDebug.log("client window remove desktop={} window={} reason={}", this.desktopId, window.debugName(), reason);
        this.closeIfEmpty();
    }

    private void demoteGhostWindow(InventoryWindow window, boolean notifyServer) {
        window.ghosted = true;
        window.minimized = false;
        window.focused = false;
        if (this.popupWindow == window) {
            this.popupWindow = null;
        }
        if (window.session != null && notifyServer) {
            DesktopContainerClient.setSessionVisible(window.session.sessionId(), false);
        }
        this.apiGhosted(window);
        this.saveWindowState(window);
    }

    private void promoteGhostWindow(InventoryWindow window) {
        boolean wasGhosted = window.ghosted;
        window.ghosted = false;
        this.bringToFront(window);
        this.setFocusedWindow(window);
        this.hotbarOnly = false;
        if (window.session != null && wasGhosted) {
            DesktopContainerClient.setSessionVisible(window.session.sessionId(), true);
        }
        if (wasGhosted) {
            this.apiUnghosted(window);
        }
        this.saveWindowState(window);
        DesktopDebug.log("client window promote desktop={} window={} wasGhosted={}", this.desktopId, window.debugName(), wasGhosted);
    }

    private void promoteGhostWindowsForDesktopOpen(InventoryWindow openedWindow) {
        if (openedWindow.ghosted) {
            return;
        }

        boolean promoted = false;
        for (InventoryWindow window : List.copyOf(this.windows)) {
            if (window != openedWindow && window.ghosted && window.pinMode == PinMode.GHOST_PINNED) {
                this.promoteGhostWindow(window);
                promoted = true;
            }
        }
        if (promoted) {
            this.bringToFront(openedWindow);
            this.setFocusedWindow(openedWindow);
        }
    }

    private @Nullable InventoryWindow windowForSession(int sessionId) {
        for (InventoryWindow window : this.windows) {
            if (window.session != null && window.session.sessionId() == sessionId) {
                return window;
            }
        }
        return null;
    }

    private void setFocusedWindow(@Nullable InventoryWindow focusedWindow) {
        for (InventoryWindow window : this.windows) {
            boolean oldFocused = window.focused;
            boolean newFocused = !window.ghosted && window == focusedWindow;
            window.focused = newFocused;
            if (oldFocused != newFocused) {
                this.apiFocusChanged(window, newFocused);
            }
        }
        DesktopDebug.trace("client focus desktop={} window={}", this.desktopId, focusedWindow == null ? "none" : focusedWindow.debugName());
    }

    private void bringToFront(InventoryWindow window) {
        if (this.windows.remove(window)) {
            this.windows.add(window);
        }
        if (this.popupWindow != null && this.popupWindow != window) {
            this.popupWindow = null;
        }
    }

    private @Nullable InventoryWindow windowAt(double mouseX, double mouseY) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.contains(mouseX, mouseY)) {
                return window;
            }
        }

        return null;
    }

    private @Nullable SlotHit slotAt(double mouseX, double mouseY) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (!window.minimized) {
                SlotHit hit = window.slotAt(this, mouseX, mouseY);
                if (hit != null) {
                    return hit;
                }
            }
        }

        Slot hotbarSlot = this.hotbarSlotAt(mouseX, mouseY);
        if (hotbarSlot != null) {
            return new SlotHit(hotbarSlot, this.playerMenu().slots.indexOf(hotbarSlot), hotbarSlotX(hotbarSlot.getContainerSlot()), hotbarY(), this.playerMenu(), DesktopPackets.PLAYER_MENU_SESSION);
        }

        Slot offhandSlot = this.offhandSlotAt(mouseX, mouseY);
        return offhandSlot == null
            ? null
            : new SlotHit(offhandSlot, this.playerMenu().slots.indexOf(offhandSlot), offhandSlotX(), hotbarY(), this.playerMenu(), DesktopPackets.PLAYER_MENU_SESSION);
    }

    private void renderWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        int visibleHeight = window.minimized ? TOP_BAR_HEIGHT : window.height;
        TitleBarLayout titleLayout = this.titleBarLayout(window);
        CreativeModeTab selectedCreativeTab = window.kind == WindowKind.CREATIVE && !window.minimized ? this.selectedCreativeTab(window) : null;
        boolean ghosted = window.ghosted;
        int previousGuiTint = currentGuiTint;
        boolean previousGhostRendering = this.renderingGhostWindow;
        if (ghosted) {
            currentGuiTint = Math.round(SaltsInventoryConfig.get().ghostWindowOpacity() * 255.0F) << 24 | 0x00FFFFFF;
            this.renderingGhostWindow = true;
        }

        try {
            if (selectedCreativeTab != null && !ghosted) {
                this.renderCreativeTabs(graphics, window, selectedCreativeTab, false);
            }

            if (ghosted) {
                this.renderGhostBackdrop(graphics, window, visibleHeight);
            } else {
                renderNineSlice(graphics, WINDOW_TEXTURE, window.x, window.y, window.width, visibleHeight);
            }
            graphics.text(this.font, titleLayout.displayTitle(), window.x + TITLE_LEFT_PADDING, window.y + 5, this.uiColor(COLOR_WINDOW_TITLE), false);
            if (selectedCreativeTab != null && !ghosted) {
                this.renderCreativeTabPageButtons(graphics, window, titleLayout, mouseX, mouseY);
            }
            if (selectedCreativeTab != null && !ghosted && this.isCreativeSearchTab(selectedCreativeTab)) {
                this.renderCreativeSearchBox(graphics, window, titleLayout);
            }
            if (!ghosted) {
                this.renderControls(graphics, window, titleLayout, mouseX, mouseY);
            }

            if (window.minimized) {
                return;
            }

            switch (window.kind) {
                case INVENTORY -> this.renderInventoryWindow(graphics, window, mouseX, mouseY);
                case CONTAINER -> this.renderContainerWindow(graphics, window, mouseX, mouseY);
                case CHARACTER -> this.renderCharacterWindow(graphics, window, mouseX, mouseY);
                case CREATIVE -> this.renderCreativeWindow(graphics, window, mouseX, mouseY);
            }

            if (!ghosted) {
                this.renderResizeGrip(graphics, window, mouseX, mouseY);
            }
            if (selectedCreativeTab != null && !ghosted) {
                this.renderCreativeTabs(graphics, window, selectedCreativeTab, true);
            }
        } finally {
            currentGuiTint = previousGuiTint;
            this.renderingGhostWindow = previousGhostRendering;
        }
    }

    private void renderAttachedRecipeBook(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY, float tickProgress) {
        RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
        if (recipeBook == null) {
            return;
        }

        this.updateRecipeBookPosition(window, recipeBook);
        recipeBook.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private @Nullable RecipeBookComponent<?> visibleRecipeBook(InventoryWindow window) {
        if (window.minimized || window.ghosted || window.recipeBook == null || !window.recipeBook.isVisible()) {
            return null;
        }

        return window.recipeBook;
    }

    private @Nullable AbstractContainerMenu vanillaRecipeBookMenu(InventoryWindow window) {
        if (window.kind == WindowKind.CHARACTER) {
            AbstractContainerMenu menu = this.playerMenu();
            return menu instanceof AbstractCraftingMenu craftingMenu ? craftingMenu : null;
        }

        AbstractContainerMenu menu = window.containerMenu();
        if (menu instanceof CraftingMenu craftingMenu) {
            return craftingMenu;
        }
        return menu instanceof AbstractFurnaceMenu furnaceMenu ? furnaceMenu : null;
    }

    private @Nullable RecipeBookComponent<?> recipeBook(InventoryWindow window) {
        if (window.recipeBook == null) {
            window.recipeBook = this.createRecipeBook(window);
            if (window.recipeBook == null) {
                return null;
            }
            window.recipeBookSyntheticWidth = Integer.MIN_VALUE;
            window.recipeBookSyntheticHeight = Integer.MIN_VALUE;
        }
        return window.recipeBook;
    }

    private @Nullable RecipeBookComponent<?> createRecipeBook(InventoryWindow window) {
        if (window.apiDefinition != null && window.containerMenu() != null) {
            RecipeBookComponent<?> recipeBook = ((DesktopWindowDefinition) window.apiDefinition).createRecipeBook(
                this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE)
            );
            if (recipeBook != null) {
                return recipeBook;
            }
        }

        AbstractContainerMenu menu = this.vanillaRecipeBookMenu(window);
        if (menu instanceof AbstractCraftingMenu craftingMenu) {
            return new CraftingRecipeBookComponent(craftingMenu);
        }
        if (menu instanceof AbstractFurnaceMenu furnaceMenu) {
            return createFurnaceRecipeBook(furnaceMenu);
        }
        return null;
    }

    private static FurnaceRecipeBookComponent createFurnaceRecipeBook(AbstractFurnaceMenu menu) {
        if (menu instanceof SmokerMenu) {
            return new FurnaceRecipeBookComponent(
                menu,
                Component.translatable("gui.recipebook.toggleRecipes.smokable"),
                List.of(
                    new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.SMOKER),
                    new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.SMOKER_FOOD)
                )
            );
        }
        if (menu instanceof BlastFurnaceMenu) {
            return new FurnaceRecipeBookComponent(
                menu,
                Component.translatable("gui.recipebook.toggleRecipes.blastable"),
                List.of(
                    new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.BLAST_FURNACE),
                    new RecipeBookComponent.TabInfo(Items.REDSTONE_ORE, RecipeBookCategories.BLAST_FURNACE_BLOCKS),
                    new RecipeBookComponent.TabInfo(Items.IRON_SHOVEL, Items.GOLDEN_LEGGINGS, RecipeBookCategories.BLAST_FURNACE_MISC)
                )
            );
        }
        return new FurnaceRecipeBookComponent(
            menu,
            Component.translatable("gui.recipebook.toggleRecipes.smeltable"),
            List.of(
                new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.FURNACE),
                new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD),
                new RecipeBookComponent.TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
                new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC)
            )
        );
    }

    private void updateRecipeBookPosition(InventoryWindow window, RecipeBookComponent<?> recipeBook) {
        RecipeBookPosition position = this.recipeBookPosition(window);
        int syntheticWidth = 2 * (position.x() + 86) + RECIPE_BOOK_WIDTH;
        int syntheticHeight = 2 * position.y() + RECIPE_BOOK_HEIGHT;
        if (window.recipeBookX != position.x()
            || window.recipeBookY != position.y()
            || window.recipeBookSyntheticWidth != syntheticWidth
            || window.recipeBookSyntheticHeight != syntheticHeight) {
            recipeBook.init(syntheticWidth, syntheticHeight, this.minecraft, false);
            window.recipeBookX = position.x();
            window.recipeBookY = position.y();
            window.recipeBookSyntheticWidth = syntheticWidth;
            window.recipeBookSyntheticHeight = syntheticHeight;
        }
    }

    private RecipeBookPosition recipeBookPosition(InventoryWindow window) {
        List<RecipeBookPosition> candidates = List.of(
            new RecipeBookPosition(window.x - RECIPE_BOOK_WIDTH - RECIPE_BOOK_GAP, window.y),
            new RecipeBookPosition(window.x + window.width + RECIPE_BOOK_GAP + RECIPE_BOOK_TAB_LEFT_OVERHANG, window.y),
            new RecipeBookPosition(window.x + RECIPE_BOOK_TAB_LEFT_OVERHANG, window.y - RECIPE_BOOK_HEIGHT - RECIPE_BOOK_GAP),
            new RecipeBookPosition(window.x + RECIPE_BOOK_TAB_LEFT_OVERHANG, window.y + window.height + RECIPE_BOOK_GAP)
        );
        for (RecipeBookPosition candidate : candidates) {
            if (this.recipeBookFits(candidate)) {
                return candidate;
            }
        }

        return new RecipeBookPosition(
            clamp(
                window.x - RECIPE_BOOK_WIDTH - RECIPE_BOOK_GAP,
                RECIPE_BOOK_TAB_LEFT_OVERHANG,
                Math.max(RECIPE_BOOK_TAB_LEFT_OVERHANG, this.desktopWidth() - RECIPE_BOOK_WIDTH)
            ),
            clamp(window.y, 0, Math.max(0, this.desktopHeight() - RECIPE_BOOK_HEIGHT))
        );
    }

    private boolean recipeBookFits(RecipeBookPosition position) {
        return recipeBookBoundsX(position) >= 0
            && position.y() >= 0
            && recipeBookBoundsX(position) + recipeBookBoundsWidth() <= this.desktopWidth()
            && position.y() + RECIPE_BOOK_HEIGHT <= this.desktopHeight();
    }

    private static int recipeBookBoundsX(RecipeBookPosition position) {
        return position.x() - RECIPE_BOOK_TAB_LEFT_OVERHANG;
    }

    private static int recipeBookBoundsWidth() {
        return RECIPE_BOOK_TAB_LEFT_OVERHANG + RECIPE_BOOK_WIDTH;
    }

    private @Nullable InventoryWindow recipeBookWindowAt(double mouseX, double mouseY) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
            if (recipeBook == null) {
                continue;
            }

            this.updateRecipeBookPosition(window, recipeBook);
            if (contains(mouseX, mouseY, window.recipeBookX - RECIPE_BOOK_TAB_LEFT_OVERHANG, window.recipeBookY, recipeBookBoundsWidth(), RECIPE_BOOK_HEIGHT)
                || recipeBook.isMouseOver(mouseX, mouseY)) {
                return window;
            }
        }
        return null;
    }

    private boolean recipeBookButtonContains(InventoryWindow window, double mouseX, double mouseY) {
        RecipeBookButtonRect rect = this.recipeBookButtonRect(window);
        return rect != null && contains(mouseX, mouseY, rect.x(), rect.y(), RECIPE_BOOK_BUTTON_WIDTH, RECIPE_BOOK_BUTTON_HEIGHT);
    }

    private @Nullable RecipeBookButtonRect recipeBookButtonRect(InventoryWindow window) {
        if (window.minimized || window.ghosted) {
            return null;
        }
        if (window.kind == WindowKind.CHARACTER && this.vanillaRecipeBookMenu(window) != null) {
            int contentX = characterContentX(window);
            int contentY = characterContentY(window);
            return new RecipeBookButtonRect(contentX + CHARACTER_RECIPE_BUTTON_X, contentY + CHARACTER_RECIPE_BUTTON_Y);
        }
        if (window.containerMenu() instanceof CraftingMenu) {
            int contentX = craftingTableContentX(window);
            int contentY = craftingTableContentY(window);
            return new RecipeBookButtonRect(contentX + CRAFTING_TABLE_RECIPE_BUTTON_X, contentY + CRAFTING_TABLE_RECIPE_BUTTON_Y);
        }
        if (window.containerMenu() instanceof AbstractFurnaceMenu) {
            int contentX = furnaceContentX(window);
            int contentY = furnaceContentY(window);
            return new RecipeBookButtonRect(contentX + FURNACE_RECIPE_BUTTON_X, contentY + FURNACE_RECIPE_BUTTON_Y);
        }
        return null;
    }

    private boolean toggleRecipeBook(InventoryWindow window) {
        RecipeBookComponent<?> recipeBook = this.recipeBook(window);
        if (recipeBook == null) {
            return false;
        }
        this.updateRecipeBookPosition(window, recipeBook);
        recipeBook.toggleVisibility();
        DesktopDebug.trace("client recipe book toggle desktop={} window={} visible={}", this.desktopId, window.debugName(), recipeBook.isVisible());
        return true;
    }

    private boolean recipeBookMouseClicked(InventoryWindow window, MouseButtonEvent event, boolean doubleClick) {
        RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
        if (recipeBook == null) {
            return false;
        }

        this.updateRecipeBookPosition(window, recipeBook);
        int previousTarget = this.recipePlacementSessionId;
        this.recipePlacementSessionId = window.sessionId();
        try {
            boolean consumed = recipeBook.mouseClicked(event, doubleClick);
            DesktopDebug.trace("client recipe book click desktop={} window={} button={} consumed={}", this.desktopId, window.debugName(), event.button(), consumed);
            return consumed;
        } finally {
            this.recipePlacementSessionId = previousTarget;
        }
    }

    private boolean recipeBookMouseDragged(InventoryWindow window, MouseButtonEvent event, double dx, double dy) {
        RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
        if (recipeBook == null) {
            return false;
        }

        this.updateRecipeBookPosition(window, recipeBook);
        return recipeBook.mouseDragged(event, dx, dy);
    }

    private boolean handleRecipeBookKey(KeyEvent event) {
        InventoryWindow textWindow = this.activeRecipeBookSearchWindow();
        if (textWindow != null) {
            RecipeBookComponent<?> recipeBook = textWindow.recipeBook;
            if (recipeBook != null) {
                recipeBook.keyPressed(event);
            }
            return true;
        }

        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
            if (recipeBook != null && recipeBook.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleRecipeBookKeyRelease(KeyEvent event) {
        InventoryWindow textWindow = this.activeRecipeBookSearchWindow();
        if (textWindow != null) {
            RecipeBookComponent<?> recipeBook = textWindow.recipeBook;
            if (recipeBook != null) {
                recipeBook.keyReleased(event);
            }
            return true;
        }

        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
            if (recipeBook != null && recipeBook.keyReleased(event)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleRecipeBookChar(CharacterEvent event) {
        InventoryWindow textWindow = this.activeRecipeBookSearchWindow();
        if (textWindow != null) {
            RecipeBookComponent<?> recipeBook = textWindow.recipeBook;
            return recipeBook != null && recipeBook.charTyped(event);
        }

        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
            if (recipeBook != null && recipeBook.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable InventoryWindow activeRecipeBookSearchWindow() {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(window);
            if (recipeBook == null) {
                continue;
            }

            EditBox searchBox = ((RecipeBookComponentAccessor) recipeBook).salts_inventory_update$getSearchBox();
            if (searchBox != null && searchBox.isFocused()) {
                return window;
            }
        }
        return null;
    }

    private void renderGhostBackdrop(GuiGraphicsExtractor graphics, InventoryWindow window, int visibleHeight) {
        graphics.fill(window.x, window.y, window.x + window.width, window.y + visibleHeight, this.uiColor(GHOST_BACKDROP));
    }

    private int uiColor(int color) {
        return this.renderingGhostWindow ? multiplyAlpha(color, currentGuiAlpha()) : color;
    }

    private static int multiplyAlpha(int color, float alphaMultiplier) {
        int alpha = color >>> 24;
        int adjustedAlpha = clamp(Math.round(alpha * alphaMultiplier), 0, 255);
        return adjustedAlpha << 24 | color & 0x00FFFFFF;
    }

    private static float currentGuiAlpha() {
        return (currentGuiTint >>> 24) / 255.0F;
    }

    private void renderControls(GuiGraphicsExtractor graphics, InventoryWindow window, TitleBarLayout layout, int mouseX, int mouseY) {
        for (ControlRect rect : layout.controls()) {
            this.renderControlButton(graphics, window, rect.control(), rect.x(), rect.y(), mouseX, mouseY);
        }
    }

    private void renderControlButton(GuiGraphicsExtractor graphics, InventoryWindow window, WindowControl control, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x, y, CONTROL_SIZE, CONTROL_SIZE);
        boolean pressed = this.pressedControlWindow == window && this.pressedControl == control && hovered;
        boolean toggled = this.isControlActive(window, control);
        int textureRow = pressed || toggled && control != WindowControl.LOCK ? 2 : hovered ? 1 : 0;
        blitRegion(
            graphics,
            WINDOW_CONTROLS_TEXTURE,
            x,
            y,
            this.controlTextureColumn(window, control) * CONTROL_SIZE,
            textureRow * CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_TEXTURE_WIDTH,
            CONTROL_TEXTURE_HEIGHT
        );
    }

    private int controlTextureColumn(InventoryWindow window, WindowControl control) {
        return switch (control) {
            case FOCUS -> 0;
            case MINIMIZE -> 1;
            case CLOSE -> 2;
            case ELLIPSIS -> 3;
            case LOCK -> window.locked ? 5 : 4;
            case PIN -> switch (window.pinMode) {
                case UNPINNED -> 6;
                case PINNED -> 7;
                case GHOST_PINNED -> 8;
            };
        };
    }

    private TitleBarLayout titleBarLayout(InventoryWindow window) {
        Component titleBarTitle = this.titleBarTitle(window);
        List<WindowControl> controls = this.fullTitleBarWidth(titleBarTitle) <= window.width ? FULL_TITLE_CONTROLS : COMPACT_TITLE_CONTROLS;
        int availableTitleWidth = Math.max(0, window.width - TITLE_LEFT_PADDING - TITLE_TO_CONTROLS_GAP - controlsWidth(controls));
        String title = this.truncatedTitle(titleBarTitle.getString(), availableTitleWidth);
        return new TitleBarLayout(title, this.controlRects(window, controls), controls == COMPACT_TITLE_CONTROLS);
    }

    private Component titleBarTitle(InventoryWindow window) {
        Component apiTitle = this.apiTitle(window);
        if (apiTitle != null) {
            return apiTitle;
        }

        AbstractContainerMenu menu = window.containerMenu();
        return menu == null ? window.title : this.vanillaTitleBarTitle(menu, window.title);
    }

    private Component vanillaTitleBarTitle(AbstractContainerMenu menu, Component fallback) {
        if (menu instanceof MerchantMenu) {
            return Component.translatable("merchant.trades");
        }
        if (menu instanceof SmithingMenu) {
            return Component.literal("Smithing");
        }
        if (menu instanceof GrindstoneMenu) {
            return Component.translatable("block.minecraft.grindstone");
        }
        if (menu instanceof StonecutterMenu) {
            return Component.translatable("block.minecraft.stonecutter");
        }
        if (menu instanceof LoomMenu) {
            return Component.translatable("block.minecraft.loom");
        }
        if (menu instanceof AnvilMenu) {
            return Component.translatable("block.minecraft.anvil");
        }
        if (menu instanceof CrafterMenu) {
            return Component.translatable("block.minecraft.crafter");
        }
        if (menu instanceof BeaconMenu) {
            return Component.translatable("block.minecraft.beacon");
        }
        if (menu instanceof BrewingStandMenu) {
            return Component.translatable("block.minecraft.brewing_stand");
        }
        if (menu instanceof CartographyTableMenu) {
            return Component.translatable("block.minecraft.cartography_table");
        }

        return fallback;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable Component apiTitle(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return null;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).title(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api title failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DesktopWindowSize apiMinSize(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return DesktopWindowSize.of(Math.max(1, window.width), Math.max(1, window.height));
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).minSize(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api min size failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return DesktopWindowSize.of(Math.max(1, window.width), Math.max(1, window.height));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable DesktopResizePolicy apiResizePolicy(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return null;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).resizePolicy(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api resize policy failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return DesktopResizePolicy.FIXED;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable DesktopWindowSize apiSnapSize(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return null;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).snapSize(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api snap size failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiOpened(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).opened(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api opened failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiClosed(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).closed(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api closed failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiMoved(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).moved(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api moved failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiResized(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).resized(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api resized failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiFocusChanged(InventoryWindow window, boolean focused) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).focusChanged(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), focused);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api focusChanged failed desktop={} window={} focused={} reason={}", this.desktopId, window.debugName(), focused, exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiGhosted(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).ghosted(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api ghosted failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void apiUnghosted(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).unghosted(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api unghosted failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiTick(InventoryWindow window) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return false;
        }

        try {
            ((DesktopWindowDefinition) window.apiDefinition).tick(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE));
            return true;
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api tick failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiAppendTooltip(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        if (window.apiDefinition == null || window.containerMenu() == null || window.minimized) {
            return false;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).appendTooltip(this.apiContext(window, graphics, mouseX, mouseY), mouseX, mouseY);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api tooltip failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiMouseClicked(InventoryWindow window, MouseButtonEvent event, boolean doubleClick) {
        if (window.apiDefinition == null || window.containerMenu() == null || window.minimized) {
            return false;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).mouseClicked(this.apiContext(window, null, (int) event.x(), (int) event.y()), event, doubleClick);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api mouseClicked failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiMouseReleased(InventoryWindow window, MouseButtonEvent event) {
        if (window.apiDefinition == null || window.containerMenu() == null || window.minimized) {
            return false;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).mouseReleased(this.apiContext(window, null, (int) event.x(), (int) event.y()), event);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api mouseReleased failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiMouseDragged(InventoryWindow window, MouseButtonEvent event, double dx, double dy) {
        if (window.apiDefinition == null || window.containerMenu() == null || window.minimized) {
            return false;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).mouseDragged(this.apiContext(window, null, (int) event.x(), (int) event.y()), event, dx, dy);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api mouseDragged failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiMouseScrolled(InventoryWindow window, double mouseX, double mouseY, double scrollX, double scrollY) {
        if (window.apiDefinition == null || window.containerMenu() == null || window.minimized) {
            return false;
        }

        try {
            return ((DesktopWindowDefinition) window.apiDefinition).mouseScrolled(this.apiContext(window, null, (int) mouseX, (int) mouseY), mouseX, mouseY, scrollX, scrollY);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api mouseScrolled failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiKeyPressed(KeyEvent event) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.apiDefinition == null || window.containerMenu() == null || window.minimized || window.ghosted) {
                continue;
            }

            try {
                if (((DesktopWindowDefinition) window.apiDefinition).keyPressed(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), event)) {
                    return true;
                }
            } catch (RuntimeException exception) {
                DesktopDebug.warn("client api keyPressed failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiCharTyped(CharacterEvent event) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.apiDefinition == null || window.containerMenu() == null || window.minimized || window.ghosted) {
                continue;
            }

            try {
                if (((DesktopWindowDefinition) window.apiDefinition).charTyped(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), event)) {
                    return true;
                }
            } catch (RuntimeException exception) {
                DesktopDebug.warn("client api charTyped failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean apiWantsTextInput() {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.apiDefinition == null || window.containerMenu() == null || window.minimized || window.ghosted) {
                continue;
            }

            try {
                if (((DesktopWindowDefinition) window.apiDefinition).wantsTextInput(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE))) {
                    return true;
                }
            } catch (RuntimeException exception) {
                DesktopDebug.warn("client api wantsTextInput failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void applyCustomPayload(DesktopCustomPayload payload) {
        InventoryWindow window = this.windowForSession(payload.sessionId());
        if (window == null || window.apiDefinition == null || window.containerMenu() == null) {
            if (isTomStorageDesktopChannel(payload.channel())) {
                DesktopDebug.warn(
                    "Tom's Storage desktop custom payload dropped desktop={} session={} channel={} bytes={} reason=no-api-window window={} api={} menu={}",
                    this.desktopId,
                    payload.sessionId(),
                    payload.channel(),
                    payload.data().length,
                    window == null ? "none" : window.debugName(),
                    window == null ? "none" : definitionName(window.apiDefinition),
                    safeMenuKey(window == null ? null : window.containerMenu())
                );
                TomsStorageCompat.warn(
                    "client custom payload dropped session={} channel={} bytes={} reason=no-api-window window={} api={} menu={}",
                    payload.sessionId(),
                    payload.channel(),
                    payload.data().length,
                    window == null ? "none" : window.debugName(),
                    window == null ? "none" : definitionName(window.apiDefinition),
                    safeMenuKey(window == null ? null : window.containerMenu())
                );
            } else {
                DesktopDebug.trace("client custom payload dropped desktop={} session={} channel={} reason=no-api-window", this.desktopId, payload.sessionId(), payload.channel());
            }
            return;
        }

        try {
            if (isTomStorageDesktopChannel(payload.channel())) {
                DesktopDebug.log(
                    "Tom's Storage desktop custom payload apply desktop={} session={} window={} menu={} channel={} bytes={} definition={}",
                    this.desktopId,
                    payload.sessionId(),
                    window.debugName(),
                    safeMenuKey(window.containerMenu()),
                    payload.channel(),
                    payload.data().length,
                    definitionName(window.apiDefinition)
                );
                TomsStorageCompat.info(
                    "client custom payload apply session={} window={} menu={} channel={} bytes={} definition={}",
                    payload.sessionId(),
                    window.debugName(),
                    safeMenuKey(window.containerMenu()),
                    payload.channel(),
                    payload.data().length,
                    definitionName(window.apiDefinition)
                );
            }
            ((DesktopWindowDefinition) window.apiDefinition).customPayload(this.apiContext(window, null, Integer.MIN_VALUE, Integer.MIN_VALUE), payload.channel(), payload.data());
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api custom payload failed desktop={} window={} channel={} reason={}", this.desktopId, window.debugName(), payload.channel(), exception.toString());
            if (isTomStorageDesktopChannel(payload.channel())) {
                TomsStorageCompat.warn(
                    "client custom payload failed session={} window={} channel={} reason={}",
                    payload.sessionId(),
                    window.debugName(),
                    payload.channel(),
                    exception.toString()
                );
            }
        }
    }

    public void applyGhostRecipe(DesktopGhostRecipePayload payload) {
        InventoryWindow window = this.windowForSession(payload.sessionId());
        if (window == null) {
            DesktopDebug.trace("client recipe ghost dropped desktop={} session={} reason=no-window", this.desktopId, payload.sessionId());
            return;
        }

        RecipeBookComponent<?> recipeBook = window.recipeBook;
        if (recipeBook == null || !recipeBook.isVisible()) {
            DesktopDebug.trace("client recipe ghost dropped desktop={} session={} window={} reason=book-hidden", this.desktopId, payload.sessionId(), window.debugName());
            return;
        }

        recipeBook.fillGhostRecipe(payload.recipeDisplay());
        DesktopDebug.trace("client recipe ghost applied desktop={} session={} window={}", this.desktopId, payload.sessionId(), window.debugName());
    }

    private static boolean isTomStorageDesktopChannel(Identifier channel) {
        return "salts_inventory_update".equals(channel.getNamespace()) && channel.getPath().contains("toms_storage");
    }

    private List<ControlRect> controlRects(InventoryWindow window, List<WindowControl> controls) {
        List<ControlRect> rects = new ArrayList<>();
        for (int i = 0; i < controls.size(); i++) {
            int fromRight = controls.size() - i;
            int x = window.x + window.width - fromRight * CONTROL_SIZE - fromRight * CONTROL_GAP - CONTROL_RIGHT_EXTRA_INSET;
            rects.add(new ControlRect(controls.get(i), x, window.y + CONTROL_TOP_INSET));
        }

        return List.copyOf(rects);
    }

    private @Nullable WindowControl titleBarControlAt(InventoryWindow window, double mouseX, double mouseY) {
        if (!window.isTopBar(mouseX, mouseY)) {
            return null;
        }

        return this.titleBarLayout(window).controlAt(mouseX, mouseY);
    }

    private void renderControlPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        InventoryWindow window = this.popupWindow;
        if (window == null || !this.windows.contains(window)) {
            return;
        }

        PopupRect popupRect = this.popupRect(window);
        if (popupRect == null) {
            return;
        }

        renderNineSlice(graphics, WINDOW_TEXTURE, popupRect.x(), popupRect.y(), popupRect.width(), popupRect.height());
        for (ControlRect rect : this.popupControlRects(popupRect)) {
            this.renderControlButton(graphics, window, rect.control(), rect.x(), rect.y(), mouseX, mouseY);
        }
    }

    private @Nullable ControlHit popupControlAt(double mouseX, double mouseY) {
        InventoryWindow window = this.popupWindow;
        if (window == null || !this.windows.contains(window)) {
            return null;
        }

        WindowControl control = this.popupControlAt(window, mouseX, mouseY);
        return control == null ? null : new ControlHit(window, control);
    }

    private @Nullable WindowControl popupControlAt(InventoryWindow window, double mouseX, double mouseY) {
        if (this.popupWindow != window) {
            return null;
        }

        PopupRect popupRect = this.popupRect(window);
        if (popupRect == null) {
            return null;
        }

        for (ControlRect rect : this.popupControlRects(popupRect)) {
            if (rect.contains(mouseX, mouseY)) {
                return rect.control();
            }
        }

        return null;
    }

    private boolean popupContains(double mouseX, double mouseY) {
        InventoryWindow window = this.popupWindow;
        if (window == null) {
            return false;
        }

        PopupRect popupRect = this.popupRect(window);
        return popupRect != null && popupRect.contains(mouseX, mouseY);
    }

    private @Nullable PopupRect popupRect(InventoryWindow window) {
        ControlRect ellipsis = this.titleBarLayout(window).controlRect(WindowControl.ELLIPSIS);
        if (ellipsis == null) {
            return null;
        }

        int width = CONTROL_POPUP_PADDING * 2 + POPUP_CONTROLS.size() * CONTROL_SIZE + (POPUP_CONTROLS.size() - 1) * CONTROL_GAP;
        int height = CONTROL_POPUP_PADDING * 2 + CONTROL_SIZE;
        int x = clamp(ellipsis.x() + CONTROL_SIZE - width, 0, Math.max(0, this.desktopWidth() - width));
        int y = ellipsis.y() + CONTROL_SIZE + 2;
        if (y + height > this.desktopHeight()) {
            y = Math.max(0, window.y - height - 2);
        }

        return new PopupRect(x, y, width, height);
    }

    private List<ControlRect> popupControlRects(PopupRect popupRect) {
        List<ControlRect> rects = new ArrayList<>();
        int x = popupRect.x() + CONTROL_POPUP_PADDING;
        int y = popupRect.y() + CONTROL_POPUP_PADDING;
        for (WindowControl control : POPUP_CONTROLS) {
            rects.add(new ControlRect(control, x, y));
            x += CONTROL_SIZE + CONTROL_GAP;
        }

        return List.copyOf(rects);
    }

    private boolean isControlActive(InventoryWindow window, WindowControl control) {
        return switch (control) {
            case FOCUS -> window.focused;
            case MINIMIZE -> window.minimized;
            case ELLIPSIS -> this.popupWindow == window;
            case PIN -> false;
            case LOCK -> false;
            case CLOSE -> false;
        };
    }

    private String truncatedTitle(String title, int availableWidth) {
        if (this.font.width(title) <= availableWidth) {
            return title;
        }

        String minimumTitle = minimumTitleText(title);
        if (this.font.width(minimumTitle) > availableWidth) {
            return minimumTitle;
        }

        String suffix = "...";
        for (int length = title.length(); length > 1; length--) {
            String candidate = title.substring(0, length) + suffix;
            if (this.font.width(candidate) <= availableWidth) {
                return candidate;
            }
        }

        return minimumTitle;
    }

    private void renderInventoryWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<Slot> inventorySlots = this.mainInventorySlots();
        SlotGridLayout layout = this.storageLayout(window, this.inventoryVirtualSlotCount());
        this.clampStorageScroll(window);
        this.renderCompactSlots(graphics, window, inventorySlots, layout, mouseX, mouseY);
        this.renderIncreaseInventoryButton(graphics, window, layout, mouseX, mouseY);
        this.renderScrollbar(graphics, window, layout);
    }

    private void initializeCreativeWindow(InventoryWindow window) {
        List<CreativeModeTab> tabs = this.creativeTabs();
        CreativeModeTab defaultTab = CreativeModeTabs.getDefaultTab();
        window.creativeSelectedTab = tabs.contains(defaultTab) ? defaultTab : tabs.isEmpty() ? null : tabs.get(0);
        window.creativeTabPage = this.creativePageForTab(tabs, window.creativeSelectedTab);
        window.creativeScrollRow = 0;
        window.creativeSearch = "";
    }

    private void restoreCreativeWindow(InventoryWindow window) {
        if (window.kind != WindowKind.CREATIVE) {
            return;
        }

        List<CreativeModeTab> tabs = this.creativeTabs();
        if (this.rememberedCreativeTab != null && tabs.contains(this.rememberedCreativeTab)) {
            window.creativeSelectedTab = this.rememberedCreativeTab;
        }
        window.creativeTabPage = this.creativePageForTab(tabs, window.creativeSelectedTab);
        window.creativeSearch = this.rememberedCreativeSearch;
        this.clampCreativeScroll(window, this.rememberedCreativeScrollRow);
        DesktopDebug.trace(
            "client creative restore desktop={} window={} tab={} search='{}' scroll={}",
            this.desktopId,
            window.debugName(),
            window.creativeSelectedTab == null ? "none" : window.creativeSelectedTab.getDisplayName().getString(),
            window.creativeSearch,
            window.creativeScrollRow
        );
    }

    private void rememberCreativeWindow(InventoryWindow window) {
        if (window.kind != WindowKind.CREATIVE) {
            return;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab != null) {
            this.rememberedCreativeTab = selectedTab;
        }
        this.rememberedCreativeSearch = window.creativeSearch;
        this.rememberedCreativeScrollRow = window.creativeScrollRow;
        DesktopDebug.trace(
            "client creative remember desktop={} window={} tab={} search='{}' scroll={}",
            this.desktopId,
            window.debugName(),
            selectedTab == null ? "none" : selectedTab.getDisplayName().getString(),
            this.rememberedCreativeSearch,
            this.rememberedCreativeScrollRow
        );
    }

    private void clampCreativeScroll(InventoryWindow window, int preferredScrollRow) {
        CreativeGridLayout layout = this.creativeGridLayout(this.creativeVisibleItems(window).size());
        window.creativeScrollRow = clamp(preferredScrollRow, 0, layout.maxScrollRow());
    }

    private void renderCreativeWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null) {
            graphics.text(this.font, "No creative tabs", window.contentX(), window.contentY(), this.uiColor(COLOR_MUTED_TEXT), false);
            return;
        }

        if (this.isCreativeInventoryTab(selectedTab)) {
            this.renderCreativeInventoryTab(graphics, window, mouseX, mouseY);
        } else {
            this.renderCreativeCatalogTab(graphics, window, mouseX, mouseY);
        }
    }

    private void renderCreativeTabs(GuiGraphicsExtractor graphics, InventoryWindow window, CreativeModeTab selectedTab, boolean selectedOnly) {
        for (CreativeModeTab tab : this.creativeTabsForPage(window)) {
            CreativeTabRect rect = this.creativeTabRect(window, tab);
            if (rect == null) {
                continue;
            }

            boolean selected = tab == selectedTab;
            if (selected != selectedOnly) {
                continue;
            }

            Identifier sprite = this.creativeTabSprite(rect, selected);
            this.blitSprite(graphics, sprite, rect.x(), rect.y(), CREATIVE_TAB_WIDTH, CREATIVE_TAB_HEIGHT);
            ItemStack icon = tab.getIconItem();
            int iconY = rect.topRow() ? rect.y() + CREATIVE_TAB_ICON_OFFSET_TOP : rect.y() + CREATIVE_TAB_ICON_OFFSET_BOTTOM;
            this.renderItemStack(graphics, icon, rect.x() + CREATIVE_TAB_ICON_OFFSET_X, iconY);
        }
    }

    private Identifier creativeTabSprite(CreativeTabRect rect, boolean selected) {
        int column = rect.column();
        int spriteColumn = column == CREATIVE_TABS_PER_ROW - 1 ? CREATIVE_TABS_PER_ROW - 2 : column;
        int index = clamp(spriteColumn + 1, 1, CREATIVE_TABS_PER_ROW) - 1;
        boolean top = rect.topRow();
        if (top) {
            return selected ? CREATIVE_TOP_TAB_SELECTED_SPRITES[index] : CREATIVE_TOP_TAB_UNSELECTED_SPRITES[index];
        }

        return selected ? CREATIVE_BOTTOM_TAB_SELECTED_SPRITES[index] : CREATIVE_BOTTOM_TAB_UNSELECTED_SPRITES[index];
    }

    private void renderCreativeTabPageButtons(GuiGraphicsExtractor graphics, InventoryWindow window, TitleBarLayout titleLayout, int mouseX, int mouseY) {
        if (this.creativeMaxTabPage(this.creativeTabs()) <= 0) {
            return;
        }

        this.renderCreativeTabPageButtonBackground(graphics, window, titleLayout);
        this.renderCreativeTabPageButton(graphics, window, titleLayout, -1, mouseX, mouseY);
        this.renderCreativeTabPageButton(graphics, window, titleLayout, 1, mouseX, mouseY);
    }

    private void renderCreativeTabPageButtonBackground(GuiGraphicsExtractor graphics, InventoryWindow window, TitleBarLayout titleLayout) {
        CreativeTabPageButtonRect previous = this.creativeTabPageButtonRect(window, titleLayout, -1);
        CreativeTabPageButtonRect next = this.creativeTabPageButtonRect(window, titleLayout, 1);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FABRIC_CREATIVE_BUTTONS_TEXTURE, previous.x(), previous.y(), 0, 12, previous.width(), previous.height(), 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FABRIC_CREATIVE_BUTTONS_TEXTURE, next.x(), next.y(), 10, 12, next.width(), next.height(), 256, 256);
    }

    private void renderCreativeTabPageButton(GuiGraphicsExtractor graphics, InventoryWindow window, TitleBarLayout titleLayout, int direction, int mouseX, int mouseY) {
        CreativeTabPageButtonRect rect = this.creativeTabPageButtonRect(window, titleLayout, direction);
        boolean enabled = this.canChangeCreativeTabPage(window, direction);
        boolean hovered = enabled && contains(mouseX, mouseY, rect.x(), rect.y(), rect.width(), rect.height());
        int u = (enabled && hovered ? 20 : 0) + (direction > 0 ? 10 : 0);
        int v = enabled ? 0 : 12;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FABRIC_CREATIVE_BUTTONS_TEXTURE, rect.x(), rect.y(), u, v, rect.width(), rect.height(), 256, 256);
    }

    private @Nullable CreativeTabPageButtonHit creativeTabPageButtonAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.kind != WindowKind.CREATIVE || window.minimized || this.creativeMaxTabPage(this.creativeTabs()) <= 0) {
            return null;
        }

        TitleBarLayout titleLayout = this.titleBarLayout(window);
        CreativeTabPageButtonRect previous = this.creativeTabPageButtonRect(window, titleLayout, -1);
        if (contains(mouseX, mouseY, previous.x(), previous.y(), previous.width(), previous.height())) {
            return new CreativeTabPageButtonHit(window, -1);
        }

        CreativeTabPageButtonRect next = this.creativeTabPageButtonRect(window, titleLayout, 1);
        return contains(mouseX, mouseY, next.x(), next.y(), next.width(), next.height())
            ? new CreativeTabPageButtonHit(window, 1)
            : null;
    }

    private CreativeTabPageButtonRect creativeTabPageButtonRect(InventoryWindow window, TitleBarLayout titleLayout, int direction) {
        int x = window.x + TITLE_LEFT_PADDING + this.font.width(titleLayout.displayTitle()) + CREATIVE_TAB_PAGE_BUTTON_TITLE_GAP;
        if (direction > 0) {
            x += CREATIVE_TAB_PAGE_BUTTON_WIDTH + CREATIVE_TAB_PAGE_BUTTON_GAP;
        }
        int y = window.y + (TOP_BAR_HEIGHT - CREATIVE_TAB_PAGE_BUTTON_HEIGHT) / 2 + CREATIVE_TAB_PAGE_BUTTON_Y_OFFSET;
        return new CreativeTabPageButtonRect(x, y, CREATIVE_TAB_PAGE_BUTTON_WIDTH, CREATIVE_TAB_PAGE_BUTTON_HEIGHT, direction);
    }

    private boolean canChangeCreativeTabPage(InventoryWindow window, int direction) {
        int maxPage = this.creativeMaxTabPage(this.creativeTabs());
        return direction < 0 ? window.creativeTabPage > 0 : window.creativeTabPage < maxPage;
    }

    private boolean changeCreativeTabPage(InventoryWindow window, int direction) {
        List<CreativeModeTab> tabs = this.creativeTabs();
        int maxPage = this.creativeMaxTabPage(tabs);
        int oldPage = window.creativeTabPage;
        window.creativeTabPage = clamp(window.creativeTabPage + direction, 0, maxPage);
        if (oldPage != window.creativeTabPage && (window.creativeSelectedTab == null || !this.isCreativeCommonTab(window.creativeSelectedTab))) {
            List<CreativeModeTab> pageSpecificTabs = this.creativePageSpecificTabs(window, tabs);
            if (!pageSpecificTabs.isEmpty()) {
                window.creativeSelectedTab = pageSpecificTabs.get(0);
                window.creativeScrollRow = 0;
                this.editingCreativeSearchWindow = null;
                this.rememberCreativeWindow(window);
            }
        }
        DesktopDebug.trace("client creative tab page desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldPage, window.creativeTabPage, maxPage);
        return oldPage != window.creativeTabPage;
    }

    private void renderCreativeSearchBox(GuiGraphicsExtractor graphics, InventoryWindow window, TitleBarLayout titleLayout) {
        CreativeSearchRect rect = this.creativeSearchRect(window, titleLayout);
        if (rect == null) {
            return;
        }

        int x = rect.x();
        int y = rect.y();
        int width = rect.width();
        blitRegion(graphics, CREATIVE_SEARCH_BAR_TEXTURE, x, y, 0, 0, 1, CREATIVE_SEARCH_HEIGHT, 1, CREATIVE_SEARCH_HEIGHT, CREATIVE_SEARCH_TEXTURE_WIDTH, CREATIVE_SEARCH_TEXTURE_HEIGHT);
        blitRegion(graphics, CREATIVE_SEARCH_BAR_TEXTURE, x + 1, y, 1, 0, width - 2, CREATIVE_SEARCH_HEIGHT, 1, CREATIVE_SEARCH_HEIGHT, CREATIVE_SEARCH_TEXTURE_WIDTH, CREATIVE_SEARCH_TEXTURE_HEIGHT);
        blitRegion(graphics, CREATIVE_SEARCH_BAR_TEXTURE, x + width - 1, y, 2, 0, 1, CREATIVE_SEARCH_HEIGHT, 1, CREATIVE_SEARCH_HEIGHT, CREATIVE_SEARCH_TEXTURE_WIDTH, CREATIVE_SEARCH_TEXTURE_HEIGHT);

        String text = this.creativeSearchVisibleText(window, width - 8);
        int textColor = this.editingCreativeSearchWindow == window ? 0xFFFFFFFF : 0xFFE8E8E8;
        graphics.text(this.font, text, x + 4, y + 2, this.uiColor(textColor), false);
        if (this.editingCreativeSearchWindow == window && (System.currentTimeMillis() / 300L) % 2L == 0L) {
            int cursorX = x + 4 + this.font.width(text);
            graphics.fill(cursorX, y + 2, cursorX + 1, y + 10, this.uiColor(textColor));
        }
    }

    private @Nullable CreativeSearchRect creativeSearchRect(InventoryWindow window) {
        return this.creativeSearchRect(window, this.titleBarLayout(window));
    }

    private @Nullable CreativeSearchRect creativeSearchRect(InventoryWindow window, TitleBarLayout titleLayout) {
        if (window.kind != WindowKind.CREATIVE || window.minimized) {
            return null;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null || !this.isCreativeSearchTab(selectedTab)) {
            return null;
        }

        int x = this.creativeTitleInlineRight(window, titleLayout) + CREATIVE_SEARCH_TITLE_GAP;
        int controlsLeft = window.x + window.width - CONTROL_RIGHT_EXTRA_INSET;
        for (ControlRect control : titleLayout.controls()) {
            controlsLeft = Math.min(controlsLeft, control.x());
        }

        int width = controlsLeft - CREATIVE_SEARCH_CONTROLS_GAP - x;
        if (width < CREATIVE_SEARCH_MIN_WIDTH) {
            return null;
        }

        return new CreativeSearchRect(x, window.y + 6, width, CREATIVE_SEARCH_HEIGHT);
    }

    private int creativeTitleInlineRight(InventoryWindow window, TitleBarLayout titleLayout) {
        int right = window.x + TITLE_LEFT_PADDING + this.font.width(titleLayout.displayTitle());
        if (this.creativeMaxTabPage(this.creativeTabs()) <= 0) {
            return right;
        }

        CreativeTabPageButtonRect next = this.creativeTabPageButtonRect(window, titleLayout, 1);
        return next.x() + next.width();
    }

    private void renderCreativeInventoryTab(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        int gridX = creativePanelX(window) + CREATIVE_GRID_X;
        int gridY = creativePanelY(window) + CREATIVE_INVENTORY_GRID_Y;
        List<Slot> slots = this.mainInventorySlots();
        for (int i = 0; i < slots.size(); i++) {
            int x = gridX + i % CREATIVE_GRID_COLUMNS * SLOT_SIZE;
            int y = gridY + i / CREATIVE_GRID_COLUMNS * SLOT_SIZE;
            this.renderSlot(graphics, slots.get(i), x, y, mouseX, mouseY);
        }

        InventoryIncreaseButtonRect increaseRect = this.creativeIncreaseInventoryButtonRect(window);
        if (increaseRect != null) {
            this.renderIncreaseInventoryButtonAt(graphics, increaseRect, mouseX, mouseY);
        }

        int deleteX = creativePanelX(window) + CREATIVE_DELETE_SLOT_X;
        int deleteY = creativePanelY(window) + CREATIVE_DELETE_SLOT_Y;
        boolean hovered = contains(mouseX, mouseY, deleteX - 1, deleteY - 1, SLOT_SIZE, SLOT_SIZE);
        renderSlotBackground(graphics, deleteX, deleteY);
        if (hovered) {
            renderSlotHighlightBack(graphics, deleteX, deleteY);
        }
        graphics.centeredText(this.font, "x", deleteX + 8, deleteY + 4, this.uiColor(0xFFFF6060));
        if (hovered) {
            renderSlotHighlightFront(graphics, deleteX, deleteY);
        }
        this.renderCreativeInventoryScrollbar(graphics, window);
    }

    private void renderCreativeCatalogTab(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<ItemStack> items = this.creativeVisibleItems(window);
        CreativeGridLayout layout = this.creativeGridLayout(items.size());
        window.creativeScrollRow = clamp(window.creativeScrollRow, 0, layout.maxScrollRow());
        int firstIndex = window.creativeScrollRow * CREATIVE_GRID_COLUMNS;
        int gridX = creativePanelX(window) + CREATIVE_GRID_X;
        int gridY = creativePanelY(window) + CREATIVE_GRID_Y;
        for (int row = 0; row < CREATIVE_GRID_ROWS; row++) {
            for (int column = 0; column < CREATIVE_GRID_COLUMNS; column++) {
                int index = firstIndex + row * CREATIVE_GRID_COLUMNS + column;
                if (index >= items.size()) {
                    continue;
                }

                int x = gridX + column * SLOT_SIZE;
                int y = gridY + row * SLOT_SIZE;
                this.renderCreativeItemSlot(graphics, items.get(index), x, y, mouseX, mouseY);
            }
        }

        this.renderCreativeScrollbar(graphics, window, layout);
    }

    private void renderCreativeItemSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        renderSlotBackground(graphics, x, y);
        if (hovered) {
            renderSlotHighlightBack(graphics, x, y);
        }
        if (!stack.isEmpty()) {
            this.renderItemStack(graphics, stack, x, y);
        }
        if (hovered) {
            renderSlotHighlightFront(graphics, x, y);
        }
    }

    private void renderCreativeScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, CreativeGridLayout layout) {
        int x = creativePanelX(window) + CREATIVE_SCROLLBAR_X;
        int y = creativePanelY(window) + CREATIVE_SCROLLBAR_Y;
        this.renderCreativeScrollbarBackground(graphics, x - 1, y, CREATIVE_SCROLLBAR_BACKGROUND_HEIGHT);
        Identifier sprite = layout.scrollable() ? CREATIVE_SCROLLER_SPRITE : CREATIVE_SCROLLER_DISABLED_SPRITE;
        int offset = layout.maxScrollRow() == 0 ? 0 : Math.round((float) window.creativeScrollRow / (float) layout.maxScrollRow() * CREATIVE_SCROLLBAR_TRACK_HEIGHT);
        this.blitSprite(graphics, sprite, x, y + CREATIVE_SCROLLBAR_INSET + offset, CREATIVE_SCROLLBAR_WIDTH, CREATIVE_SCROLLBAR_HEIGHT);
    }

    private void renderCreativeInventoryScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window) {
        int x = creativePanelX(window) + CREATIVE_INVENTORY_SCROLLBAR_X;
        int y = creativePanelY(window) + CREATIVE_INVENTORY_SCROLLBAR_Y;
        this.renderCreativeScrollbarBackground(graphics, x - 1, y, CREATIVE_INVENTORY_SCROLLBAR_BACKGROUND_HEIGHT);
        this.blitSprite(graphics, CREATIVE_SCROLLER_DISABLED_SPRITE, x, y + CREATIVE_SCROLLBAR_INSET, CREATIVE_SCROLLBAR_WIDTH, CREATIVE_SCROLLBAR_HEIGHT);
    }

    private void renderCreativeScrollbarBackground(GuiGraphicsExtractor graphics, int x, int y, int height) {
        blitRegion(
            graphics,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE,
            x,
            y,
            0,
            0,
            CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_WIDTH,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_HEIGHT
        );
        if (height > 2) {
            blitRegion(
                graphics,
                CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE,
                x,
                y + 1,
                0,
                1,
                CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
                height - 2,
                CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
                1,
                CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_WIDTH,
                CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_HEIGHT
            );
        }
        blitRegion(
            graphics,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE,
            x,
            y + height - 1,
            0,
            2,
            CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            CREATIVE_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_WIDTH,
            CREATIVE_SCROLLBAR_BACKGROUND_TEXTURE_HEIGHT
        );
    }

    private List<CreativeModeTab> creativeTabs() {
        this.refreshCreativeTabs();
        List<CreativeModeTab> tabs = new ArrayList<>(CreativeModeTabs.allTabs());
        tabs.removeIf(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY && tab.getDisplayItems().isEmpty());
        return tabs;
    }

    private List<CreativeModeTab> creativeTabsForPage(InventoryWindow window) {
        List<CreativeModeTab> tabs = this.creativeTabs();
        int maxPage = this.creativeMaxTabPage(tabs);
        window.creativeTabPage = clamp(window.creativeTabPage, 0, maxPage);
        List<CreativeModeTab> pageTabs = new ArrayList<>(this.creativeCommonTabs(tabs));
        pageTabs.addAll(this.creativePageSpecificTabs(window, tabs));
        return pageTabs;
    }

    private List<CreativeModeTab> creativePageSpecificTabs(InventoryWindow window, List<CreativeModeTab> tabs) {
        List<CreativeModeTab> pageableTabs = this.creativePageableTabs(tabs);
        int capacity = this.creativePageCapacity(tabs);
        int start = window.creativeTabPage * capacity;
        int end = Math.min(pageableTabs.size(), start + capacity);
        return start >= end ? List.of() : pageableTabs.subList(start, end);
    }

    private int creativeMaxTabPage(List<CreativeModeTab> tabs) {
        return Math.max(0, rowsForSlots(this.creativePageableTabs(tabs).size(), this.creativePageCapacity(tabs)) - 1);
    }

    private int creativePageCapacity(List<CreativeModeTab> tabs) {
        return Math.max(1, CREATIVE_TABS_PER_PAGE - this.creativeCommonTabs(tabs).size());
    }

    private int creativePageForTab(List<CreativeModeTab> tabs, @Nullable CreativeModeTab tab) {
        if (tab == null || this.isCreativeCommonTab(tab)) {
            return 0;
        }

        List<CreativeModeTab> pageableTabs = this.creativePageableTabs(tabs);
        int index = pageableTabs.indexOf(tab);
        return index < 0 ? 0 : index / this.creativePageCapacity(tabs);
    }

    private List<CreativeModeTab> creativeCommonTabs(List<CreativeModeTab> tabs) {
        List<CreativeModeTab> commonTabs = new ArrayList<>();
        for (CreativeModeTab tab : tabs) {
            if (this.isCreativeCommonTab(tab)) {
                commonTabs.add(tab);
            }
        }
        commonTabs.sort(Comparator.comparingInt((CreativeModeTab tab) -> tab.row() == CreativeModeTab.Row.TOP ? 0 : 1).thenComparingInt(CreativeModeTab::column));
        return commonTabs;
    }

    private List<CreativeModeTab> creativePageableTabs(List<CreativeModeTab> tabs) {
        List<CreativeModeTab> vanillaTabs = new ArrayList<>();
        List<CreativeModeTab> moddedTabs = new ArrayList<>();
        for (CreativeModeTab tab : tabs) {
            if (this.isCreativeCommonTab(tab)) {
                continue;
            }
            if (this.isVanillaCreativeTab(tab)) {
                vanillaTabs.add(tab);
            } else {
                moddedTabs.add(tab);
            }
        }

        vanillaTabs.sort(Comparator.comparingInt((CreativeModeTab tab) -> tab.row() == CreativeModeTab.Row.TOP ? 0 : 1).thenComparingInt(CreativeModeTab::column));
        List<CreativeModeTab> orderedTabs = new ArrayList<>(vanillaTabs.size() + moddedTabs.size());
        orderedTabs.addAll(vanillaTabs);
        orderedTabs.addAll(moddedTabs);
        return orderedTabs;
    }

    private boolean isCreativeCommonTab(CreativeModeTab tab) {
        return this.isCreativeSearchTab(tab)
            || this.isCreativeInventoryTab(tab)
            || tab.getType() == CreativeModeTab.Type.HOTBAR
            || this.isCreativeOperatorTab(tab);
    }

    private boolean isCreativeOperatorTab(CreativeModeTab tab) {
        Optional<ResourceKey<CreativeModeTab>> key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab);
        return key.isPresent() && key.get().equals(CreativeModeTabs.OP_BLOCKS);
    }

    private boolean isVanillaCreativeTab(CreativeModeTab tab) {
        Optional<ResourceKey<CreativeModeTab>> key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab);
        return key.isPresent() && "minecraft".equals(key.get().identifier().getNamespace());
    }

    private void refreshCreativeTabs() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }

        CreativeModeTabs.tryRebuildTabContents(
            this.minecraft.level.enabledFeatures(),
            this.minecraft.player.canUseGameMasterBlocks(),
            this.minecraft.level.registryAccess()
        );
    }

    private @Nullable CreativeModeTab selectedCreativeTab(InventoryWindow window) {
        List<CreativeModeTab> tabs = this.creativeTabs();
        if (tabs.isEmpty()) {
            window.creativeSelectedTab = null;
            return null;
        }

        if (window.creativeSelectedTab == null || !tabs.contains(window.creativeSelectedTab)) {
            CreativeModeTab defaultTab = this.rememberedCreativeTab != null && tabs.contains(this.rememberedCreativeTab)
                ? this.rememberedCreativeTab
                : CreativeModeTabs.getDefaultTab();
            window.creativeSelectedTab = tabs.contains(defaultTab) ? defaultTab : tabs.get(0);
            window.creativeTabPage = this.creativePageForTab(tabs, window.creativeSelectedTab);
            window.creativeScrollRow = 0;
        }

        window.creativeTabPage = clamp(window.creativeTabPage, 0, this.creativeMaxTabPage(tabs));
        return window.creativeSelectedTab;
    }

    private boolean isCreativeSearchTab(CreativeModeTab tab) {
        return tab.getType() == CreativeModeTab.Type.SEARCH;
    }

    private boolean isCreativeInventoryTab(CreativeModeTab tab) {
        return tab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    private List<ItemStack> creativeVisibleItems(InventoryWindow window) {
        CreativeModeTab tab = this.selectedCreativeTab(window);
        if (tab == null || this.isCreativeInventoryTab(tab)) {
            return List.of();
        }

        if (this.isCreativeSearchTab(tab)) {
            return this.creativeSearchItems(window);
        }

        if (tab.getType() == CreativeModeTab.Type.HOTBAR) {
            return this.creativeHotbarItems();
        }

        return this.copyStacks(tab.getDisplayItems());
    }

    private List<ItemStack> creativeHotbarItems() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>(HOTBAR_SLOT_COUNT * HOTBAR_SLOT_COUNT);
        for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SLOT_COUNT; hotbarIndex++) {
            Hotbar hotbar = this.minecraft.getHotbarManager().get(hotbarIndex);
            if (hotbar.isEmpty()) {
                for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
                    if (i == hotbarIndex) {
                        ItemStack placeholder = new ItemStack(Items.PAPER);
                        placeholder.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                        Component activator = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                        Component target = this.minecraft.options.keyHotbarSlots[hotbarIndex].getTranslatedKeyMessage();
                        placeholder.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", activator, target));
                        items.add(placeholder);
                    } else {
                        items.add(ItemStack.EMPTY);
                    }
                }
            } else {
                items.addAll(this.copyStacks(hotbar.load(this.minecraft.level.registryAccess())));
            }
        }
        return items;
    }

    private List<ItemStack> creativeSearchItems(InventoryWindow window) {
        String query = window.creativeSearch.trim();
        if (query.isEmpty()) {
            CreativeModeTab searchTab = CreativeModeTabs.searchTab();
            return this.copyStacks(searchTab.getSearchTabDisplayItems());
        }

        String lowered = query.toLowerCase(java.util.Locale.ROOT);
        List<ItemStack> results = new ArrayList<>();
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            if (query.startsWith("#")) {
                results.addAll(this.minecraft.getConnection().searchTrees().creativeTagSearch().search(lowered.substring(1)));
            } else {
                results.addAll(this.minecraft.getConnection().searchTrees().creativeNameSearch().search(lowered));
            }
        }

        if (query.startsWith("#")) {
            return results.isEmpty() ? List.of() : this.copyStacks(results);
        }

        Collection<ItemStack> candidates = results.isEmpty() ? CreativeModeTabs.searchTab().getSearchTabDisplayItems() : results;
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : candidates) {
            if (this.creativeStackMatchesSearch(stack, lowered)) {
                filtered.add(stack.copy());
            }
        }
        return filtered;
    }

    private boolean creativeStackMatchesSearch(ItemStack stack, String loweredQuery) {
        String name = stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId == null ? stack.getItem().toString().toLowerCase(java.util.Locale.ROOT) : itemId.toString().toLowerCase(java.util.Locale.ROOT);
        return name.contains(loweredQuery) || id.contains(loweredQuery);
    }

    private List<ItemStack> copyStacks(Collection<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }
        return copy;
    }

    private CreativeGridLayout creativeGridLayout(int itemCount) {
        int totalRows = rowsForSlots(itemCount, CREATIVE_GRID_COLUMNS);
        return new CreativeGridLayout(totalRows, Math.max(0, totalRows - CREATIVE_GRID_ROWS), totalRows > CREATIVE_GRID_ROWS);
    }

    private boolean creativeMouseClicked(InventoryWindow window, MouseButtonEvent event, boolean doubleClick) {
        CreativeTabPageButtonHit pageHit = this.creativeTabPageButtonAt(window, event.x(), event.y());
        if (pageHit != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.changeCreativeTabPage(window, pageHit.direction());
            return true;
        }

        CreativeTabHit tabHit = this.creativeTabAt(window, event.x(), event.y());
        if (tabHit != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.selectCreativeTab(window, tabHit.tab());
            window.creativeScrollRow = 0;
            this.editingCreativeSearchWindow = this.isCreativeSearchTab(tabHit.tab()) ? window : null;
            this.rememberCreativeWindow(window);
            DesktopDebug.trace("client creative tab desktop={} window={} tab={}", this.desktopId, window.debugName(), tabHit.tab().getDisplayName().getString());
            return true;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null) {
            return true;
        }

        if (this.isCreativeSearchTab(selectedTab)) {
            if (this.creativeSearchBoxContains(window, event.x(), event.y()) && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.editingCreativeSearchWindow = window;
                return true;
            }
            if (this.editingCreativeSearchWindow == window && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.editingCreativeSearchWindow = null;
            }
        }

        if (this.isCreativeInventoryTab(selectedTab) && this.creativeDeleteSlotContains(window, event.x(), event.y())) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.sharedCarried.isEmpty() && this.isShiftHeld()) {
                    this.clearCreativePlayerInventory();
                } else {
                    this.setCreativeSharedCarried(ItemStack.EMPTY, "delete-slot");
                }
            }
            return true;
        }

        if (this.isCreativeInventoryTab(selectedTab) && this.increaseInventoryButtonContains(window, event.x(), event.y())) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.sharedCarried.isEmpty()) {
                DesktopContainerClient.purchaseInventorySlot();
            }
            return true;
        }

        if (!this.isCreativeInventoryTab(selectedTab) && this.creativeScrollbarContains(window, event.x(), event.y())) {
            List<ItemStack> items = this.creativeVisibleItems(window);
            if (this.creativeGridLayout(items.size()).scrollable() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.scrollingCreativeWindow = window;
                this.updateCreativeScrollFromMouse(window, event.y());
            }
            return true;
        }

        CreativeItemHit itemHit = this.creativeItemAt(window, event.x(), event.y());
        if (itemHit != null) {
            if (!this.isCreativeCatalogStackActionable(itemHit.stack())) {
                return true;
            }
            if (this.isCreativeCloneMouse(event)) {
                if (!this.sharedCarried.isEmpty()) {
                    this.setCreativeSharedCarried(ItemStack.EMPTY, "catalog-clone-clear");
                } else {
                    this.setCreativeSharedCarried(itemHit.stack().copyWithCount(itemHit.stack().getMaxStackSize()), "catalog-clone-mouse");
                }
                return true;
            }
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (!this.sharedCarried.isEmpty()) {
                    this.setCreativeSharedCarried(ItemStack.EMPTY, "catalog-clear");
                    return true;
                }

                ItemStack picked = itemHit.stack().copy();
                picked.setCount(this.isShiftHeld() ? Math.min(CREATIVE_PICKED_STACK_SIZE, picked.getMaxStackSize()) : 1);
                this.setCreativeSharedCarried(picked, "catalog-pick");
            }
            return true;
        }

        SlotHit slotHit = this.creativeInventorySlotAt(window, event.x(), event.y());
        if (slotHit != null) {
            this.handleSlotMouseClicked(slotHit, event, doubleClick);
            return true;
        }

        return true;
    }

    private void selectCreativeTab(InventoryWindow window, CreativeModeTab tab) {
        List<CreativeModeTab> tabs = this.creativeTabs();
        window.creativeSelectedTab = tab;
        if (!this.isCreativeCommonTab(tab)) {
            window.creativeTabPage = this.creativePageForTab(tabs, tab);
        } else {
            window.creativeTabPage = clamp(window.creativeTabPage, 0, this.creativeMaxTabPage(tabs));
        }
    }

    private boolean isCreativeCatalogStackActionable(ItemStack stack) {
        return !stack.isEmpty() && !stack.has(DataComponents.CREATIVE_SLOT_LOCK);
    }

    private boolean scrollCreativeWindow(InventoryWindow window, double scrollY) {
        if (scrollY == 0.0D) {
            return false;
        }

        List<ItemStack> items = this.creativeVisibleItems(window);
        CreativeGridLayout layout = this.creativeGridLayout(items.size());
        if (!layout.scrollable()) {
            return false;
        }

        int oldScroll = window.creativeScrollRow;
        window.creativeScrollRow = clamp(window.creativeScrollRow + (scrollY < 0.0 ? 1 : -1), 0, layout.maxScrollRow());
        this.rememberCreativeWindow(window);
        DesktopDebug.trace("client creative scroll desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.creativeScrollRow, layout.maxScrollRow());
        return true;
    }

    private void updateCreativeScrollFromMouse(InventoryWindow window, double mouseY) {
        List<ItemStack> items = this.creativeVisibleItems(window);
        CreativeGridLayout layout = this.creativeGridLayout(items.size());
        if (!layout.scrollable()) {
            window.creativeScrollRow = 0;
            this.rememberCreativeWindow(window);
            return;
        }

        int trackTop = creativePanelY(window) + CREATIVE_SCROLLBAR_Y;
        double amount = (mouseY - trackTop - CREATIVE_SCROLLBAR_INSET - CREATIVE_SCROLLBAR_HEIGHT / 2.0D) / Math.max(1.0D, CREATIVE_SCROLLBAR_TRACK_HEIGHT);
        window.creativeScrollRow = clamp(Math.round((float) amount * layout.maxScrollRow()), 0, layout.maxScrollRow());
        this.rememberCreativeWindow(window);
    }

    private @Nullable SlotHit creativeInventorySlotAt(InventoryWindow window, double mouseX, double mouseY) {
        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null || !this.isCreativeInventoryTab(selectedTab)) {
            return null;
        }

        AbstractContainerMenu playerMenu = this.playerMenu();
        List<Slot> slots = this.mainInventorySlots();
        int gridX = creativePanelX(window) + CREATIVE_GRID_X;
        int gridY = creativePanelY(window) + CREATIVE_INVENTORY_GRID_Y;
        for (int i = 0; i < slots.size(); i++) {
            int x = gridX + i % CREATIVE_GRID_COLUMNS * SLOT_SIZE;
            int y = gridY + i / CREATIVE_GRID_COLUMNS * SLOT_SIZE;
            if (contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)) {
                Slot slot = slots.get(i);
                return new SlotHit(slot, playerMenu.slots.indexOf(slot), x, y, playerMenu, DesktopPackets.PLAYER_MENU_SESSION);
            }
        }

        return null;
    }

    private @Nullable CreativeItemHit creativeItemAt(double mouseX, double mouseY) {
        InventoryWindow window = this.windowAt(mouseX, mouseY);
        return window == null ? null : this.creativeItemAt(window, mouseX, mouseY);
    }

    private @Nullable CreativeItemHit creativeItemAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.kind != WindowKind.CREATIVE || window.minimized || !this.creativeGridContains(window, mouseX, mouseY)) {
            return null;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null || this.isCreativeInventoryTab(selectedTab)) {
            return null;
        }

        int gridX = creativePanelX(window) + CREATIVE_GRID_X;
        int gridY = creativePanelY(window) + CREATIVE_GRID_Y;
        int column = ((int) mouseX - gridX + 1) / SLOT_SIZE;
        int row = ((int) mouseY - gridY + 1) / SLOT_SIZE;
        if (column < 0 || column >= CREATIVE_GRID_COLUMNS || row < 0 || row >= CREATIVE_GRID_ROWS) {
            return null;
        }

        List<ItemStack> items = this.creativeVisibleItems(window);
        window.creativeScrollRow = clamp(window.creativeScrollRow, 0, this.creativeGridLayout(items.size()).maxScrollRow());
        int index = window.creativeScrollRow * CREATIVE_GRID_COLUMNS + row * CREATIVE_GRID_COLUMNS + column;
        if (index < 0 || index >= items.size()) {
            return null;
        }

        return new CreativeItemHit(window, items.get(index), gridX + column * SLOT_SIZE, gridY + row * SLOT_SIZE, index);
    }

    private @Nullable CreativeTabHit creativeTabAt(double mouseX, double mouseY) {
        InventoryWindow window = this.windowAt(mouseX, mouseY);
        return window == null ? null : this.creativeTabAt(window, mouseX, mouseY);
    }

    private @Nullable CreativeTabHit creativeTabAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.kind != WindowKind.CREATIVE || window.minimized) {
            return null;
        }

        for (CreativeModeTab tab : this.creativeTabsForPage(window)) {
            CreativeTabRect rect = this.creativeTabRect(window, tab);
            if (rect != null && contains(mouseX, mouseY, rect.x(), rect.y(), rect.width(), rect.height())) {
                return new CreativeTabHit(window, tab, rect);
            }
        }

        return null;
    }

    private @Nullable CreativeTabRect creativeTabRect(InventoryWindow window, CreativeModeTab tab) {
        CreativeTabPosition position = this.creativeTabPosition(window, tab);
        if (position == null) {
            return null;
        }

        boolean top = position.topRow();
        int column = position.column();
        int x = creativeContentX(window) + CREATIVE_TAB_X_OFFSET + column * CREATIVE_TAB_WIDTH;
        int y = top ? creativeTopTabsY(window) : creativeBottomTabsY(window);
        return new CreativeTabRect(x, y, CREATIVE_TAB_WIDTH, CREATIVE_TAB_HEIGHT, column, top);
    }

    private @Nullable CreativeTabPosition creativeTabPosition(InventoryWindow window, CreativeModeTab tab) {
        if (this.isCreativeCommonTab(tab)) {
            int column = tab.column();
            if (column < 0 || column >= CREATIVE_TABS_PER_ROW) {
                return null;
            }
            return new CreativeTabPosition(column, tab.row() == CreativeModeTab.Row.TOP);
        }

        List<CreativeModeTab> tabs = this.creativeTabs();
        List<CreativeModeTab> pageTabs = this.creativePageSpecificTabs(window, tabs);
        int pageIndex = pageTabs.indexOf(tab);
        if (pageIndex < 0) {
            return null;
        }

        List<CreativeTabPosition> availablePositions = this.creativeAvailableTabPositions(tabs);
        return pageIndex >= availablePositions.size() ? null : availablePositions.get(pageIndex);
    }

    private List<CreativeTabPosition> creativeAvailableTabPositions(List<CreativeModeTab> tabs) {
        Set<Integer> occupiedPositions = new HashSet<>();
        for (CreativeModeTab tab : this.creativeCommonTabs(tabs)) {
            int column = tab.column();
            if (column >= 0 && column < CREATIVE_TABS_PER_ROW) {
                occupiedPositions.add(creativeTabPositionKey(column, tab.row() == CreativeModeTab.Row.TOP));
            }
        }

        List<CreativeTabPosition> positions = new ArrayList<>();
        for (boolean top : List.of(true, false)) {
            for (int column = 0; column < CREATIVE_TABS_PER_ROW; column++) {
                if (!occupiedPositions.contains(creativeTabPositionKey(column, top))) {
                    positions.add(new CreativeTabPosition(column, top));
                }
            }
        }
        return positions;
    }

    private static int creativeTabPositionKey(int column, boolean top) {
        return (top ? 0 : CREATIVE_TABS_PER_ROW) + column;
    }

    private boolean creativeGridContains(InventoryWindow window, double mouseX, double mouseY) {
        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        int gridY = selectedTab != null && this.isCreativeInventoryTab(selectedTab) ? CREATIVE_INVENTORY_GRID_Y : CREATIVE_GRID_Y;
        return window.kind == WindowKind.CREATIVE
            && contains(
                mouseX,
                mouseY,
                creativePanelX(window) + CREATIVE_GRID_X - 1,
                creativePanelY(window) + gridY - 1,
                CREATIVE_GRID_COLUMNS * SLOT_SIZE,
                CREATIVE_GRID_ROWS * SLOT_SIZE
            );
    }

    private boolean creativeSearchBoxContains(InventoryWindow window, double mouseX, double mouseY) {
        CreativeSearchRect rect = this.creativeSearchRect(window);
        return rect != null && contains(mouseX, mouseY, rect.x(), rect.y(), rect.width(), rect.height());
    }

    private boolean creativeScrollbarContains(InventoryWindow window, double mouseX, double mouseY) {
        return window.kind == WindowKind.CREATIVE
            && contains(mouseX, mouseY, creativePanelX(window) + CREATIVE_SCROLLBAR_X - 1, creativePanelY(window) + CREATIVE_SCROLLBAR_Y, CREATIVE_SCROLLBAR_BACKGROUND_WIDTH, CREATIVE_SCROLLBAR_BACKGROUND_HEIGHT);
    }

    private boolean creativeDeleteSlotContains(InventoryWindow window, double mouseX, double mouseY) {
        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        return selectedTab != null
            && this.isCreativeInventoryTab(selectedTab)
            && contains(mouseX, mouseY, creativePanelX(window) + CREATIVE_DELETE_SLOT_X - 1, creativePanelY(window) + CREATIVE_DELETE_SLOT_Y - 1, SLOT_SIZE, SLOT_SIZE);
    }

    private boolean isCreativePlayerMenuSlot(SlotHit hit) {
        return this.minecraft != null
            && isCreativePlayer(this.minecraft)
            && hit.sessionId() == DesktopPackets.PLAYER_MENU_SESSION
            && hit.menu() == this.playerMenu()
            && hit.slotId() >= 9
            && hit.slotId() <= 45;
    }

    private int creativeSlotMaxStackSize(Slot slot, ItemStack stack) {
        return Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
    }

    private void clearCreativePlayerInventory() {
        if (this.minecraft == null || this.minecraft.gameMode == null || this.minecraft.player == null || !this.minecraft.player.hasInfiniteMaterials()) {
            return;
        }

        AbstractContainerMenu menu = this.playerMenu();
        int cleared = 0;
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            Slot slot = menu.slots.get(slotId);
            if (!this.isPlayerInventorySlot(slot) || !slot.hasItem()) {
                continue;
            }

            slot.set(ItemStack.EMPTY);
            slot.setChanged();
            this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, slotId);
            cleared++;
        }

        this.setCreativeSharedCarried(ItemStack.EMPTY, "clear-inventory");
        DesktopDebug.trace("client creative clear inventory desktop={} cleared={}", this.desktopId, cleared);
    }

    private @Nullable InventoryWindow activeCreativeSearchWindow() {
        InventoryWindow window = this.editingCreativeSearchWindow;
        if (window == null || !this.windows.contains(window) || window.minimized || window.kind != WindowKind.CREATIVE) {
            this.editingCreativeSearchWindow = null;
            return null;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null || !this.isCreativeSearchTab(selectedTab)) {
            this.editingCreativeSearchWindow = null;
            return null;
        }

        return window;
    }

    private boolean handleCreativeSearchKey(KeyEvent event) {
        InventoryWindow window = this.activeCreativeSearchWindow();
        if (window == null) {
            return false;
        }

        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!window.creativeSearch.isEmpty()) {
                int cut = window.creativeSearch.offsetByCodePoints(window.creativeSearch.length(), -1);
                window.creativeSearch = window.creativeSearch.substring(0, cut);
                window.creativeScrollRow = 0;
                this.rememberCreativeWindow(window);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            window.creativeSearch = "";
            window.creativeScrollRow = 0;
            this.rememberCreativeWindow(window);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.editingCreativeSearchWindow = null;
            return true;
        }
        if (event.isEscape()) {
            if (!window.creativeSearch.isEmpty()) {
                window.creativeSearch = "";
                window.creativeScrollRow = 0;
                this.rememberCreativeWindow(window);
            } else {
                this.editingCreativeSearchWindow = null;
            }
            return true;
        }

        return true;
    }

    private boolean handleCreativeDropKey(KeyEvent event) {
        if (this.minecraft == null || this.minecraft.gameMode == null || !isCreativePlayer(this.minecraft) || this.sharedCarried.isEmpty()) {
            return false;
        }
        if (!this.minecraft.options.keyDrop.matches(event)) {
            return false;
        }
        SlotHit hoveredSlot = this.hoveredSlotForKeyboardInput();
        if (hoveredSlot != null && hoveredSlot.slot().hasItem()) {
            return false;
        }

        ItemStack dropped = this.isControlHeld() ? this.sharedCarried.copy() : this.sharedCarried.copyWithCount(1);
        ItemStack remaining = this.sharedCarried.copy();
        remaining.shrink(dropped.getCount());
        this.minecraft.gameMode.handleCreativeModeItemDrop(dropped);
        this.setCreativeSharedCarried(remaining, "drop-key");
        return true;
    }

    private boolean isControlHeld() {
        return this.minecraft != null
            && (InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL));
    }

    private String creativeSearchVisibleText(InventoryWindow window, int maxWidth) {
        String text = window.creativeSearch;
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty() && this.font.width(text) > maxWidth) {
            text = text.substring(1);
        }
        return text;
    }

    private void renderContainerWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<Slot> slots = window.containerSlots();
        AbstractContainerMenu menu = window.containerMenu();
        int minX = window.containerMinSlotX();
        int minY = window.containerMinSlotY();
        DesktopDebug.detail(
            "client render container desktop={} window={} menu={} slots={} api={} resizable={} minimized={} ghosted={} mouse={},{}",
            this.desktopId,
            window.debugName(),
            safeMenuKey(menu),
            slots.size(),
            definitionName(window.apiDefinition),
            this.isResizableStorageWindow(window),
            window.minimized,
            window.ghosted,
            mouseX,
            mouseY
        );

        if (window.session != null && window.session.isMountSession()) {
            this.renderMountWindow(graphics, window, slots, menu, mouseX, mouseY);
            return;
        }

        if (this.renderApiWindow(graphics, window, mouseX, mouseY)) {
            return;
        }

        if (slots.isEmpty()) {
            graphics.text(this.font, "No item slots", window.contentX(), window.contentY(), this.uiColor(COLOR_MUTED_TEXT), false);
            return;
        }

        if (menu instanceof AbstractFurnaceMenu furnaceMenu) {
            this.renderFurnaceWindow(graphics, window, slots, furnaceMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof CraftingMenu craftingMenu) {
            this.renderCraftingTableWindow(graphics, window, craftingMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof AnvilMenu anvilMenu) {
            this.renderAnvilWindow(graphics, window, slots, anvilMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof CrafterMenu crafterMenu) {
            this.renderCrafterWindow(graphics, window, slots, crafterMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof BeaconMenu beaconMenu) {
            this.renderBeaconWindow(graphics, window, slots, beaconMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof BrewingStandMenu brewingMenu) {
            this.renderBrewingStandWindow(graphics, window, slots, brewingMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof CartographyTableMenu cartographyMenu) {
            this.renderCartographyTableWindow(graphics, window, slots, cartographyMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof SmithingMenu smithingMenu) {
            this.renderSmithingWindow(graphics, window, slots, smithingMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof GrindstoneMenu) {
            this.renderGrindstoneWindow(graphics, window, slots, mouseX, mouseY);
            return;
        }
        if (menu instanceof StonecutterMenu stonecutterMenu) {
            this.renderStonecutterWindow(graphics, window, slots, stonecutterMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof LoomMenu loomMenu) {
            this.renderLoomWindow(graphics, window, slots, loomMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof EnchantmentMenu enchantmentMenu) {
            this.renderEnchantmentWindow(graphics, window, slots, enchantmentMenu, mouseX, mouseY);
            return;
        }
        if (menu instanceof MerchantMenu merchantMenu) {
            this.renderMerchantWindow(graphics, window, slots, merchantMenu, mouseX, mouseY);
            return;
        }

        if (this.isResizableStorageWindow(window)) {
            SlotGridLayout layout = this.storageLayout(window, slots.size());
            this.clampStorageScroll(window);
            this.renderCompactSlots(graphics, window, slots, layout, mouseX, mouseY);
            this.renderScrollbar(graphics, window, layout);
            return;
        }

        for (Slot slot : slots) {
            int x = window.contentX() + slot.x - minX;
            int y = window.contentY() + slot.y - minY;
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean renderApiWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return false;
        }

        try {
            DesktopDebug.detail(
                "client api render start desktop={} window={} menu={} definition={} mouse={},{}",
                this.desktopId,
                window.debugName(),
                safeMenuKey(window.containerMenu()),
                definitionName(window.apiDefinition),
                mouseX,
                mouseY
            );
            ((DesktopWindowDefinition) window.apiDefinition).render(this.apiContext(window, graphics, mouseX, mouseY));
            DesktopDebug.detail(
                "client api render end desktop={} window={} menu={} definition={}",
                this.desktopId,
                window.debugName(),
                safeMenuKey(window.containerMenu()),
                definitionName(window.apiDefinition)
            );
            return true;
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api render failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return false;
        }
    }

    private void renderMountWindow(GuiGraphicsExtractor graphics, InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, int mouseX, int mouseY) {
        DesktopContainerSession session = window.session;
        if (session == null) {
            return;
        }

        int contentX = window.contentX();
        int contentY = window.contentY();
        this.renderMountSlotIfPresent(graphics, slots, MOUNT_SADDLE_SLOT, contentX + MOUNT_EQUIPMENT_X, contentY + MOUNT_EQUIPMENT_Y, mouseX, mouseY);
        this.renderMountSlotIfPresent(graphics, slots, MOUNT_BODY_SLOT, contentX + MOUNT_EQUIPMENT_X, contentY + MOUNT_EQUIPMENT_Y + SLOT_SIZE, mouseX, mouseY);

        int modelX0 = contentX + MOUNT_MODEL_X;
        int modelY0 = contentY + MOUNT_MODEL_Y;
        int modelX1 = modelX0 + MOUNT_MODEL_WIDTH;
        int modelY1 = modelY0 + MOUNT_MODEL_HEIGHT;
        renderOnePixelNineSlice(graphics, MODEL_DISPLAY_TEXTURE, modelX0, modelY0, MOUNT_MODEL_WIDTH, MOUNT_MODEL_HEIGHT);
        LivingEntity mount = session.mountEntity(this.minecraft);
        if (mount != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                modelX0,
                modelY0,
                modelX1,
                modelY1,
                MOUNT_MODEL_SCALE,
                MOUNT_MODEL_MOUSE_SCALE,
                mouseX,
                mouseY,
                mount
            );
        }

        int storageColumns = this.mountStorageColumns(session);
        for (int column = 0; column < storageColumns; column++) {
            for (int row = 0; row < MOUNT_STORAGE_ROWS; row++) {
                int slotIndex = MOUNT_STORAGE_START_SLOT + row * storageColumns + column;
                this.renderMountSlotIfPresent(
                    graphics,
                    slots,
                    slotIndex,
                    contentX + MOUNT_STORAGE_X + column * SLOT_SIZE,
                    contentY + MOUNT_STORAGE_Y + row * SLOT_SIZE,
                    mouseX,
                    mouseY
                );
            }
        }
    }

    private void renderMountSlotIfPresent(GuiGraphicsExtractor graphics, List<Slot> slots, int slotIndex, int x, int y, int mouseX, int mouseY) {
        Slot slot = mountSlot(slots, slotIndex);
        if (slot != null && slot.isActive()) {
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    private static @Nullable Slot mountSlot(List<Slot> slots, int slotIndex) {
        return slotIndex >= 0 && slotIndex < slots.size() ? slots.get(slotIndex) : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable SlotHit apiSlotAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.apiDefinition == null || window.containerMenu() == null) {
            return null;
        }

        DesktopSlotHit apiHit;
        try {
            apiHit = ((DesktopWindowDefinition) window.apiDefinition).slotAt(this.apiContext(window, null, (int) mouseX, (int) mouseY), mouseX, mouseY);
        } catch (RuntimeException exception) {
            DesktopDebug.warn("client api slotAt failed desktop={} window={} reason={}", this.desktopId, window.debugName(), exception.toString());
            return null;
        }
        if (apiHit == null) {
            return null;
        }

        AbstractContainerMenu menu = window.containerMenu();
        int slotId = apiHit.menuSlotId();
        if (slotId < 0 || slotId >= menu.slots.size()) {
            DesktopDebug.warn("client api slotAt ignored desktop={} window={} slot={} reason=out-of-range", this.desktopId, window.debugName(), slotId);
            return null;
        }

        Slot slot = menu.slots.get(slotId);
        return new SlotHit(slot, slotId, apiHit.x(), apiHit.y(), menu, window.sessionId());
    }

    private @Nullable SlotHit mountSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        SlotHit saddleHit = this.mountSlotHitAt(window, slots, menu, MOUNT_SADDLE_SLOT, window.contentX() + MOUNT_EQUIPMENT_X, window.contentY() + MOUNT_EQUIPMENT_Y, mouseX, mouseY);
        if (saddleHit != null) {
            return saddleHit;
        }

        SlotHit bodyHit = this.mountSlotHitAt(window, slots, menu, MOUNT_BODY_SLOT, window.contentX() + MOUNT_EQUIPMENT_X, window.contentY() + MOUNT_EQUIPMENT_Y + SLOT_SIZE, mouseX, mouseY);
        if (bodyHit != null) {
            return bodyHit;
        }

        if (window.session == null) {
            return null;
        }

        int storageColumns = this.mountStorageColumns(window.session);
        for (int column = 0; column < storageColumns; column++) {
            for (int row = 0; row < MOUNT_STORAGE_ROWS; row++) {
                int slotIndex = MOUNT_STORAGE_START_SLOT + row * storageColumns + column;
                SlotHit hit = this.mountSlotHitAt(
                    window,
                    slots,
                    menu,
                    slotIndex,
                    window.contentX() + MOUNT_STORAGE_X + column * SLOT_SIZE,
                    window.contentY() + MOUNT_STORAGE_Y + row * SLOT_SIZE,
                    mouseX,
                    mouseY
                );
                if (hit != null) {
                    return hit;
                }
            }
        }

        return null;
    }

    private @Nullable SlotHit mountSlotHitAt(
        InventoryWindow window,
        List<Slot> slots,
        AbstractContainerMenu menu,
        int slotIndex,
        int slotX,
        int slotY,
        double mouseX,
        double mouseY
    ) {
        Slot slot = mountSlot(slots, slotIndex);
        if (slot == null || !slot.isActive()) {
            return null;
        }
        if (!contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
            return null;
        }

        return new SlotHit(slot, menu.slots.indexOf(slot), slotX, slotY, menu, window.sessionId());
    }

    private void renderFurnaceWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        AbstractFurnaceMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = furnaceContentX(window);
        int contentY = furnaceContentY(window);

        this.renderFurnaceSlotIfPresent(graphics, slots, FURNACE_INPUT_SLOT, contentX + FURNACE_INPUT_X, contentY + FURNACE_INPUT_Y, mouseX, mouseY);
        this.renderFurnaceSlotIfPresent(graphics, slots, FURNACE_FUEL_SLOT, contentX + FURNACE_FUEL_X, contentY + FURNACE_FUEL_Y, mouseX, mouseY);
        this.renderFurnaceSlotIfPresent(graphics, slots, FURNACE_RESULT_SLOT, contentX + FURNACE_RESULT_X, contentY + FURNACE_RESULT_Y, mouseX, mouseY);

        int flameX = contentX + FURNACE_FLAME_X;
        int flameY = contentY + FURNACE_FLAME_Y;
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            flameX,
            flameY,
            WIDGET_FLAME_EMPTY_X,
            WIDGET_FLAME_EMPTY_Y,
            WIDGET_FLAME_WIDTH,
            WIDGET_FLAME_HEIGHT,
            WIDGET_FLAME_WIDTH,
            WIDGET_FLAME_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );

        int flameFill = menu.isLit() ? Math.round(clampProgress(menu.getLitProgress()) * WIDGET_FLAME_HEIGHT) : 0;
        if (flameFill > 0) {
            int clippedY = WIDGET_FLAME_HEIGHT - flameFill;
            blitRegion(
                graphics,
                CONTAINER_WIDGETS_TEXTURE,
                flameX,
                flameY + clippedY,
                WIDGET_FLAME_FULL_X,
                WIDGET_FLAME_FULL_Y + clippedY,
                WIDGET_FLAME_WIDTH,
                flameFill,
                WIDGET_FLAME_WIDTH,
                flameFill,
                CONTAINER_WIDGETS_TEXTURE_WIDTH,
                CONTAINER_WIDGETS_TEXTURE_HEIGHT
            );
        }

        int arrowX = contentX + FURNACE_ARROW_X;
        int arrowY = contentY + FURNACE_ARROW_Y;
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            arrowX,
            arrowY,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );

        int arrowFill = Math.round(clampProgress(menu.getBurnProgress()) * WIDGET_ARROW_WIDTH);
        if (arrowFill > 0) {
            blitRegion(
                graphics,
                CONTAINER_WIDGETS_TEXTURE,
                arrowX,
                arrowY,
                WIDGET_ARROW_FULL_X,
                WIDGET_ARROW_FULL_Y,
                arrowFill,
                WIDGET_ARROW_HEIGHT,
                arrowFill,
                WIDGET_ARROW_HEIGHT,
                CONTAINER_WIDGETS_TEXTURE_WIDTH,
                CONTAINER_WIDGETS_TEXTURE_HEIGHT
            );
        }

        this.renderRecipeBookButton(graphics, window, contentX + FURNACE_RECIPE_BUTTON_X, contentY + FURNACE_RECIPE_BUTTON_Y, mouseX, mouseY);
    }

    private void renderFurnaceSlotIfPresent(GuiGraphicsExtractor graphics, List<Slot> slots, int containerSlot, int x, int y, int mouseX, int mouseY) {
        Slot slot = furnaceSlot(slots, containerSlot);
        if (slot != null) {
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    private void renderContainerSlotIfPresent(GuiGraphicsExtractor graphics, List<Slot> slots, int containerSlot, int x, int y, int mouseX, int mouseY) {
        Slot slot = containerSlot(slots, containerSlot);
        if (slot != null) {
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    private void renderLargeContainerSlotIfPresent(GuiGraphicsExtractor graphics, List<Slot> slots, int containerSlot, int x, int y, int mouseX, int mouseY) {
        Slot slot = containerSlot(slots, containerSlot);
        if (slot == null) {
            return;
        }

        boolean hovered = contains(
            mouseX,
            mouseY,
            x + LARGE_SLOT_ITEM_OFFSET - 1,
            y + LARGE_SLOT_ITEM_OFFSET - 1,
            SLOT_SIZE,
            SLOT_SIZE
        );
        int itemX = x + LARGE_SLOT_ITEM_OFFSET;
        int itemY = y + LARGE_SLOT_ITEM_OFFSET;
        blitRegion(
            graphics,
            LARGE_SLOT_TEXTURE,
            x,
            y,
            0,
            0,
            LARGE_SLOT_TEXTURE_SIZE,
            LARGE_SLOT_TEXTURE_SIZE,
            LARGE_SLOT_TEXTURE_SIZE,
            LARGE_SLOT_TEXTURE_SIZE,
            LARGE_SLOT_TEXTURE_SIZE,
            LARGE_SLOT_TEXTURE_SIZE
        );
        if (hovered) {
            renderSlotHighlightBack(graphics, itemX, itemY);
        }
        if (slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), itemX, itemY, slot.index);
        }
        if (hovered) {
            renderSlotHighlightFront(graphics, itemX, itemY);
        }
    }

    private @Nullable SlotHit furnaceSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = furnaceContentX(window);
        int contentY = furnaceContentY(window);
        int[] containerSlots = {FURNACE_INPUT_SLOT, FURNACE_FUEL_SLOT, FURNACE_RESULT_SLOT};
        int[] slotXs = {contentX + FURNACE_INPUT_X, contentX + FURNACE_FUEL_X, contentX + FURNACE_RESULT_X};
        int[] slotYs = {contentY + FURNACE_INPUT_Y, contentY + FURNACE_FUEL_Y, contentY + FURNACE_RESULT_Y};

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = furnaceSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private void renderCraftingTableWindow(GuiGraphicsExtractor graphics, InventoryWindow window, CraftingMenu menu, int mouseX, int mouseY) {
        int contentX = craftingTableContentX(window);
        int contentY = craftingTableContentY(window);
        List<Slot> inputSlots = menu.getInputGridSlots();
        for (int row = 0; row < CRAFTING_TABLE_GRID_ROWS; row++) {
            for (int column = 0; column < CRAFTING_TABLE_GRID_COLUMNS; column++) {
                int slotIndex = row * CRAFTING_TABLE_GRID_COLUMNS + column;
                if (slotIndex >= inputSlots.size()) {
                    continue;
                }

                int x = contentX + CRAFTING_TABLE_GRID_X + column * SLOT_SIZE;
                int y = contentY + CRAFTING_TABLE_GRID_Y + row * SLOT_SIZE;
                this.renderSlot(graphics, inputSlots.get(slotIndex), x, y, mouseX, mouseY);
            }
        }

        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            contentX + CRAFTING_TABLE_ARROW_X,
            contentY + CRAFTING_TABLE_ARROW_Y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );
        this.renderSlot(graphics, menu.getResultSlot(), contentX + CRAFTING_TABLE_RESULT_X, contentY + CRAFTING_TABLE_RESULT_Y, mouseX, mouseY);
        this.renderRecipeBookButton(graphics, window, contentX + CRAFTING_TABLE_RECIPE_BUTTON_X, contentY + CRAFTING_TABLE_RECIPE_BUTTON_Y, mouseX, mouseY);
    }

    private @Nullable SlotHit craftingTableSlotAt(InventoryWindow window, CraftingMenu menu, double mouseX, double mouseY) {
        int contentX = craftingTableContentX(window);
        int contentY = craftingTableContentY(window);
        List<Slot> inputSlots = menu.getInputGridSlots();
        for (int row = 0; row < CRAFTING_TABLE_GRID_ROWS; row++) {
            for (int column = 0; column < CRAFTING_TABLE_GRID_COLUMNS; column++) {
                int slotIndex = row * CRAFTING_TABLE_GRID_COLUMNS + column;
                if (slotIndex >= inputSlots.size()) {
                    continue;
                }

                int slotX = contentX + CRAFTING_TABLE_GRID_X + column * SLOT_SIZE;
                int slotY = contentY + CRAFTING_TABLE_GRID_Y + row * SLOT_SIZE;
                if (contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                    Slot slot = inputSlots.get(slotIndex);
                    return new SlotHit(slot, menu.slots.indexOf(slot), slotX, slotY, menu, window.sessionId());
                }
            }
        }

        int resultX = contentX + CRAFTING_TABLE_RESULT_X;
        int resultY = contentY + CRAFTING_TABLE_RESULT_Y;
        if (contains(mouseX, mouseY, resultX - 1, resultY - 1, SLOT_SIZE, SLOT_SIZE)) {
            Slot slot = menu.getResultSlot();
            return new SlotHit(slot, menu.slots.indexOf(slot), resultX, resultY, menu, window.sessionId());
        }

        return null;
    }

    private void renderAnvilWindow(GuiGraphicsExtractor graphics, InventoryWindow window, List<Slot> slots, AnvilMenu menu, int mouseX, int mouseY) {
        int contentX = anvilContentX(window);
        int contentY = anvilContentY(window);
        this.syncAnvilNameFromInput(window, menu);

        blitRegion(
            graphics,
            ANVIL_HAMMER_TEXTURE,
            contentX + ANVIL_HAMMER_X,
            contentY + ANVIL_HAMMER_Y,
            0,
            0,
            ANVIL_HAMMER_TEXTURE_SIZE,
            ANVIL_HAMMER_TEXTURE_SIZE,
            ANVIL_HAMMER_TEXTURE_SIZE,
            ANVIL_HAMMER_TEXTURE_SIZE,
            ANVIL_HAMMER_TEXTURE_SIZE,
            ANVIL_HAMMER_TEXTURE_SIZE
        );
        this.renderAnvilTextField(graphics, window, menu, contentX, contentY);
        this.renderContainerSlotIfPresent(graphics, slots, ANVIL_INPUT_SLOT, contentX + ANVIL_INPUT_SLOT_X, contentY + ANVIL_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, ANVIL_ADDITIONAL_SLOT, contentX + ANVIL_ADDITIONAL_SLOT_X, contentY + ANVIL_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, ANVIL_RESULT_SLOT, contentX + ANVIL_RESULT_SLOT_X, contentY + ANVIL_SLOT_Y, mouseX, mouseY);
        blitRegion(
            graphics,
            CARTOGRAPHY_PLUS_TEXTURE,
            contentX + ANVIL_PLUS_X,
            contentY + ANVIL_PLUS_Y,
            0,
            0,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE
        );
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            contentX + ANVIL_ARROW_X,
            contentY + ANVIL_ARROW_Y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );

        boolean hasInput = anvilSlotHasItem(slots, ANVIL_INPUT_SLOT);
        boolean hasAdditional = anvilSlotHasItem(slots, ANVIL_ADDITIONAL_SLOT);
        boolean hasResult = anvilSlotHasItem(slots, ANVIL_RESULT_SLOT);
        if ((hasInput || hasAdditional) && !hasResult) {
            this.blitSprite(graphics,
                ANVIL_ERROR_SPRITE,
                contentX + ANVIL_ERROR_X,
                contentY + ANVIL_ERROR_Y,
                ANVIL_ERROR_WIDTH,
                ANVIL_ERROR_HEIGHT
            );
        }

        this.renderAnvilCost(graphics, menu, slots, contentX, contentY);
    }

    private void renderAnvilTextField(GuiGraphicsExtractor graphics, InventoryWindow window, AnvilMenu menu, int contentX, int contentY) {
        boolean enabled = anvilInputSlot(menu).hasItem();
        this.blitSprite(graphics,
            enabled ? ANVIL_TEXT_FIELD_SPRITE : ANVIL_TEXT_FIELD_DISABLED_SPRITE,
            contentX + ANVIL_TEXT_FIELD_X,
            contentY + ANVIL_TEXT_FIELD_Y,
            ANVIL_TEXT_FIELD_WIDTH,
            ANVIL_TEXT_FIELD_HEIGHT
        );
        if (!enabled) {
            return;
        }

        int textColor = this.editingAnvilWindow == window ? 0xFFFFFFFF : 0xFFE8E8E8;
        String text = anvilVisibleText(window.anvilName, ANVIL_TEXT_FIELD_WIDTH - 8, this.font);
        int textX = contentX + ANVIL_TEXT_X;
        int textY = contentY + ANVIL_TEXT_Y;
        graphics.text(this.font, text, textX, textY, this.uiColor(textColor), false);
        if (this.editingAnvilWindow == window && (System.currentTimeMillis() / 300L) % 2L == 0L) {
            int cursorX = textX + this.font.width(text) + 1;
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 10, this.uiColor(0xFFFFFFFF));
        }
    }

    private void renderAnvilCost(GuiGraphicsExtractor graphics, AnvilMenu menu, List<Slot> slots, int contentX, int contentY) {
        int cost = menu.getCost();
        if (cost <= 0 || !anvilSlotHasItem(slots, ANVIL_RESULT_SLOT)) {
            return;
        }

        LocalPlayer player = this.player();
        boolean tooExpensive = cost >= 40 && player != null && !player.hasInfiniteMaterials();
        Component label = tooExpensive
            ? Component.translatable("container.repair.expensive")
            : Component.translatable("container.repair.cost", cost);
        int color = tooExpensive ? 0xFFFF6060 : 0xFF80FF20;
        int x = contentX + ANVIL_INPUT_SLOT_X;
        int y = contentY + ANVIL_COST_Y;
        int width = this.font.width(label);
        graphics.fill(x - 2, y - 1, x + width + 2, y + 10, this.uiColor(0xCC3A3A3A));
        graphics.text(this.font, label, x, y, this.uiColor(color), false);
    }

    private @Nullable SlotHit anvilSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = anvilContentX(window);
        int contentY = anvilContentY(window);
        int[] containerSlots = {ANVIL_INPUT_SLOT, ANVIL_ADDITIONAL_SLOT, ANVIL_RESULT_SLOT};
        int[] slotXs = {contentX + ANVIL_INPUT_SLOT_X, contentX + ANVIL_ADDITIONAL_SLOT_X, contentX + ANVIL_RESULT_SLOT_X};
        int[] slotYs = {contentY + ANVIL_SLOT_Y, contentY + ANVIL_SLOT_Y, contentY + ANVIL_SLOT_Y};

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private boolean anvilTextFieldContains(InventoryWindow window, double mouseX, double mouseY) {
        return window.containerMenu() instanceof AnvilMenu
            && contains(
                mouseX,
                mouseY,
                anvilContentX(window) + ANVIL_TEXT_FIELD_X,
                anvilContentY(window) + ANVIL_TEXT_FIELD_Y,
                ANVIL_TEXT_FIELD_WIDTH,
                ANVIL_TEXT_FIELD_HEIGHT
            );
    }

    private void focusAnvilTextField(InventoryWindow window) {
        if (!(window.containerMenu() instanceof AnvilMenu anvilMenu)) {
            return;
        }

        this.syncAnvilNameFromInput(window, anvilMenu);
        if (!anvilInputSlot(anvilMenu).hasItem()) {
            this.editingAnvilWindow = null;
            return;
        }

        this.editingAnvilWindow = window;
        window.anvilNameDirty = true;
        DesktopDebug.trace("client anvil focus desktop={} window={} name={}", this.desktopId, window.debugName(), window.anvilName);
    }

    private @Nullable InventoryWindow activeAnvilEditWindow() {
        InventoryWindow window = this.editingAnvilWindow;
        if (window == null || !this.windows.contains(window) || window.minimized || !(window.containerMenu() instanceof AnvilMenu anvilMenu)) {
            this.editingAnvilWindow = null;
            return null;
        }

        if (!anvilInputSlot(anvilMenu).hasItem()) {
            this.editingAnvilWindow = null;
            return null;
        }

        return window;
    }

    private boolean handleAnvilEditKey(KeyEvent event) {
        InventoryWindow window = this.activeAnvilEditWindow();
        if (window == null) {
            return false;
        }

        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!window.anvilName.isEmpty()) {
                int cut = window.anvilName.offsetByCodePoints(window.anvilName.length(), -1);
                window.anvilName = window.anvilName.substring(0, cut);
                this.submitAnvilName(window);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (!window.anvilName.isEmpty()) {
                window.anvilName = "";
                this.submitAnvilName(window);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.editingAnvilWindow = null;
            return true;
        }
        if (event.isEscape()) {
            this.editingAnvilWindow = null;
            return true;
        }

        return true;
    }

    private void syncAnvilNameFromInput(InventoryWindow window, AnvilMenu menu) {
        ItemStack input = anvilInputSlot(menu).getItem();
        String inputName = input.isEmpty() ? "" : input.getHoverName().getString();
        if (input.isEmpty()) {
            window.anvilName = "";
            window.anvilLastInputName = "";
            window.anvilNameDirty = false;
            if (this.editingAnvilWindow == window) {
                this.editingAnvilWindow = null;
            }
            return;
        }

        if (!window.anvilLastInputName.equals(inputName) && (this.editingAnvilWindow != window || !window.anvilNameDirty)) {
            window.anvilName = inputName;
            window.anvilNameDirty = false;
        }
        window.anvilLastInputName = inputName;
    }

    private void submitAnvilName(InventoryWindow window) {
        if (!(window.containerMenu() instanceof AnvilMenu anvilMenu)) {
            return;
        }

        String name = window.anvilName.length() > ANVIL_MAX_NAME_LENGTH
            ? window.anvilName.substring(0, ANVIL_MAX_NAME_LENGTH)
            : window.anvilName;
        window.anvilName = name;
        window.anvilNameDirty = true;
        anvilMenu.setItemName(name);
        DesktopDebug.trace("client anvil rename desktop={} session={} name={}", this.desktopId, window.sessionId(), name);
        if (window.session != null) {
            if (!DesktopContainerClient.renameAnvil(window.session.sessionId(), name)) {
                DesktopDebug.warn("client anvil rename dropped session={} reason=packet-send-failed", window.session.sessionId());
            }
            return;
        }

        if (this.minecraft != null
            && this.minecraft.getConnection() != null
            && window.legacyMenu == anvilMenu
            && this.minecraft.player != null
            && this.minecraft.player.containerMenu == anvilMenu) {
            this.minecraft.getConnection().send(new ServerboundRenameItemPacket(name));
        }
    }

    private static String anvilVisibleText(String text, int maxWidth, net.minecraft.client.gui.Font font) {
        String visible = text;
        while (!visible.isEmpty() && font.width(visible) > maxWidth) {
            visible = visible.substring(1);
        }
        return visible;
    }

    private static Slot anvilInputSlot(AnvilMenu menu) {
        return menu.getSlot(ANVIL_INPUT_SLOT);
    }

    private static boolean anvilSlotHasItem(List<Slot> slots, int containerSlot) {
        Slot slot = containerSlot(slots, containerSlot);
        return slot != null && slot.hasItem();
    }

    private void renderCrafterWindow(GuiGraphicsExtractor graphics, InventoryWindow window, List<Slot> slots, CrafterMenu menu, int mouseX, int mouseY) {
        int contentX = crafterContentX(window);
        int contentY = crafterContentY(window);
        for (int row = 0; row < CRAFTER_GRID_ROWS; row++) {
            for (int column = 0; column < CRAFTER_GRID_COLUMNS; column++) {
                int slotId = row * CRAFTER_GRID_COLUMNS + column;
                Slot slot = slotId < slots.size() ? slots.get(slotId) : null;
                if (slot == null) {
                    continue;
                }

                int x = contentX + CRAFTER_GRID_X + column * SLOT_SIZE;
                int y = contentY + CRAFTER_GRID_Y + row * SLOT_SIZE;
                this.renderCrafterInputSlot(graphics, menu, slot, x, y, mouseX, mouseY);
            }
        }

        Identifier redstoneTexture = menu.isPowered() ? CRAFTER_POWERED_REDSTONE_TEXTURE : CRAFTER_UNPOWERED_REDSTONE_TEXTURE;
        blitRegion(
            graphics,
            redstoneTexture,
            contentX + CRAFTER_REDSTONE_X,
            contentY + CRAFTER_REDSTONE_Y,
            0,
            0,
            CRAFTER_REDSTONE_TEXTURE_SIZE,
            CRAFTER_REDSTONE_TEXTURE_SIZE,
            CRAFTER_REDSTONE_TEXTURE_SIZE,
            CRAFTER_REDSTONE_TEXTURE_SIZE,
            CRAFTER_REDSTONE_TEXTURE_SIZE,
            CRAFTER_REDSTONE_TEXTURE_SIZE
        );
        this.renderCrafterOutput(graphics, slots, contentX + CRAFTER_OUTPUT_X, contentY + CRAFTER_OUTPUT_Y, mouseX, mouseY);
    }

    private void renderCrafterInputSlot(GuiGraphicsExtractor graphics, CrafterMenu menu, Slot slot, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        Identifier texture = menu.isSlotDisabled(slot.index) ? CRAFTER_DISABLED_SLOT_TEXTURE : CRAFTER_SLOT_TEXTURE;
        blitRegion(
            graphics,
            texture,
            x - 1,
            y - 1,
            0,
            0,
            CRAFTER_SLOT_TEXTURE_SIZE,
            CRAFTER_SLOT_TEXTURE_SIZE,
            CRAFTER_SLOT_TEXTURE_SIZE,
            CRAFTER_SLOT_TEXTURE_SIZE,
            CRAFTER_SLOT_TEXTURE_SIZE,
            CRAFTER_SLOT_TEXTURE_SIZE
        );
        if (hovered) {
            renderSlotHighlightBack(graphics, x, y);
        }
        if (slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), x, y, slot.index);
        }
        if (hovered) {
            renderSlotHighlightFront(graphics, x, y);
        }
    }

    private void renderCrafterOutput(GuiGraphicsExtractor graphics, List<Slot> slots, int x, int y, int mouseX, int mouseY) {
        blitRegion(
            graphics,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE,
            x,
            y,
            0,
            0,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE,
            CRAFTER_OUTPUT_DISPLAY_TEXTURE_SIZE
        );
        Slot outputSlot = crafterOutputSlot(slots);
        if (outputSlot == null || !outputSlot.hasItem()) {
            return;
        }

        int itemX = x + CRAFTER_OUTPUT_ITEM_OFFSET;
        int itemY = y + CRAFTER_OUTPUT_ITEM_OFFSET;
        boolean hovered = contains(mouseX, mouseY, itemX - 1, itemY - 1, SLOT_SIZE, SLOT_SIZE);
        if (hovered) {
            renderSlotHighlightBack(graphics, itemX, itemY);
        }
        this.renderItemStack(graphics, outputSlot.getItem(), itemX, itemY, outputSlot.index);
        if (hovered) {
            renderSlotHighlightFront(graphics, itemX, itemY);
        }
    }

    private @Nullable SlotHit crafterSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = crafterContentX(window);
        int contentY = crafterContentY(window);
        for (int row = 0; row < CRAFTER_GRID_ROWS; row++) {
            for (int column = 0; column < CRAFTER_GRID_COLUMNS; column++) {
                int slotId = row * CRAFTER_GRID_COLUMNS + column;
                if (slotId >= slots.size()) {
                    continue;
                }

                int slotX = contentX + CRAFTER_GRID_X + column * SLOT_SIZE;
                int slotY = contentY + CRAFTER_GRID_Y + row * SLOT_SIZE;
                if (contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                    Slot slot = slots.get(slotId);
                    return new SlotHit(slot, menu.slots.indexOf(slot), slotX, slotY, menu, window.sessionId());
                }
            }
        }

        return null;
    }

    private boolean crafterSlotStateClicked(InventoryWindow window, MouseButtonEvent event) {
        if (!(window.containerMenu() instanceof CrafterMenu crafterMenu) || this.minecraft == null || this.minecraft.player == null || this.minecraft.player.isSpectator()) {
            return false;
        }

        SlotHit hit = this.crafterSlotAt(window, window.containerSlots(), crafterMenu, event.x(), event.y());
        if (hit == null || hit.slotId() < 0 || hit.slotId() >= CRAFTER_INPUT_SLOT_COUNT || hit.slot().hasItem()) {
            return false;
        }

        boolean disabled = crafterMenu.isSlotDisabled(hit.slotId());
        if (!disabled && !this.sharedCarried.isEmpty()) {
            return false;
        }

        boolean enabled = disabled;
        crafterMenu.setSlotState(hit.slotId(), enabled);
        DesktopDebug.trace("client crafter slot state desktop={} session={} slot={} enabled={}", this.desktopId, window.sessionId(), hit.slotId(), enabled);
        if (window.session != null) {
            if (!DesktopContainerClient.clickButton(window.session.sessionId(), crafterSlotStateButtonId(hit.slotId(), enabled))) {
                DesktopDebug.warn("client crafter slot state dropped session={} slot={} enabled={} reason=packet-send-failed", window.session.sessionId(), hit.slotId(), enabled);
            }
            return true;
        }

        if (this.minecraft.gameMode != null && window.legacyMenu == crafterMenu && this.minecraft.player.containerMenu == crafterMenu) {
            this.minecraft.gameMode.handleSlotStateChanged(hit.slotId(), crafterMenu.containerId, enabled);
        }
        return true;
    }

    private void renderBeaconWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        BeaconMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = beaconContentX(window);
        int contentY = beaconContentY(window);
        this.syncBeaconSelection(window, menu);
        blitRegion(
            graphics,
            BEACON_UI_TEXTURE,
            contentX,
            contentY,
            0,
            0,
            BEACON_UI_TEXTURE_WIDTH,
            BEACON_UI_TEXTURE_HEIGHT,
            BEACON_UI_TEXTURE_WIDTH,
            BEACON_UI_TEXTURE_HEIGHT,
            BEACON_UI_TEXTURE_WIDTH,
            BEACON_UI_TEXTURE_HEIGHT
        );

        this.renderCenteredBeaconLabel(graphics, Component.translatable("block.minecraft.beacon.primary"), contentX + BEACON_PRIMARY_LABEL_CENTER_X, contentY + BEACON_LABEL_Y);
        this.renderCenteredBeaconLabel(graphics, Component.translatable("block.minecraft.beacon.secondary"), contentX + BEACON_SECONDARY_LABEL_CENTER_X, contentY + BEACON_LABEL_Y);
        this.renderBeaconPowerButtons(graphics, window, menu, contentX, contentY, mouseX, mouseY);
        this.renderBeaconMaterials(graphics, contentX, contentY);
        this.renderContainerSlotIfPresent(graphics, slots, BEACON_PAYMENT_SLOT, contentX + BEACON_PAYMENT_SLOT_X, contentY + BEACON_PAYMENT_SLOT_Y, mouseX, mouseY);
        this.renderBeaconActionButton(
            graphics,
            BEACON_CONFIRM_SPRITE,
            contentX + BEACON_CONFIRM_X,
            contentY + BEACON_CONFIRM_Y,
            this.canConfirmBeacon(menu, window),
            contains(mouseX, mouseY, contentX + BEACON_CONFIRM_X, contentY + BEACON_CONFIRM_Y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)
        );
        this.renderBeaconActionButton(
            graphics,
            BEACON_CANCEL_SPRITE,
            contentX + BEACON_CANCEL_X,
            contentY + BEACON_CANCEL_Y,
            true,
            contains(mouseX, mouseY, contentX + BEACON_CANCEL_X, contentY + BEACON_CANCEL_Y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)
        );
    }

    private void renderCenteredBeaconLabel(GuiGraphicsExtractor graphics, Component label, int centerX, int y) {
        int x = centerX - this.font.width(label) / 2;
        graphics.text(this.font, label, x, y, this.uiColor(0xFFE8E8E8), false);
    }

    private void renderBeaconPowerButtons(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        BeaconMenu menu,
        int contentX,
        int contentY,
        int mouseX,
        int mouseY
    ) {
        int levels = menu.getLevels();
        for (int tier = 0; tier < Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.size()); tier++) {
            List<Holder<MobEffect>> effects = BeaconBlockEntity.BEACON_EFFECTS.get(tier);
            int rowWidth = effects.size() * BEACON_BUTTON_SIZE + Math.max(0, effects.size() - 1) * BEACON_BUTTON_GAP;
            int rowX = contentX + BEACON_PRIMARY_BUTTON_BASE_X + (2 * BEACON_BUTTON_SIZE + BEACON_BUTTON_GAP - rowWidth) / 2;
            int rowY = contentY + BEACON_PRIMARY_BUTTON_BASE_Y + tier * BEACON_BUTTON_STEP_Y;
            for (int i = 0; i < effects.size(); i++) {
                Holder<MobEffect> effect = effects.get(i);
                int x = rowX + i * BEACON_BUTTON_STEP_X;
                this.renderBeaconPowerButton(
                    graphics,
                    effect,
                    x,
                    rowY,
                    tier < levels,
                    sameBeaconEffect(effect, window.beaconPrimary),
                    contains(mouseX, mouseY, x, rowY, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)
                );
            }
        }

        if (BeaconBlockEntity.BEACON_EFFECTS.size() > 3) {
            for (Holder<MobEffect> effect : BeaconBlockEntity.BEACON_EFFECTS.get(3)) {
                int x = contentX + BEACON_SECONDARY_BUTTON_X;
                int y = contentY + BEACON_SECONDARY_BUTTON_Y;
                this.renderBeaconPowerButton(
                    graphics,
                    effect,
                    x,
                    y,
                    this.canSelectBeaconSecondary(menu, window.beaconPrimary, effect),
                    sameBeaconEffect(effect, window.beaconSecondary),
                    contains(mouseX, mouseY, x, y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)
                );
            }
        }

        if (window.beaconPrimary != null) {
            int x = contentX + BEACON_UPGRADE_BUTTON_X;
            int y = contentY + BEACON_SECONDARY_BUTTON_Y;
            this.renderBeaconPowerButton(
                graphics,
                window.beaconPrimary,
                x,
                y,
                this.canSelectBeaconSecondary(menu, window.beaconPrimary, window.beaconPrimary),
                sameBeaconEffect(window.beaconPrimary, window.beaconSecondary),
                contains(mouseX, mouseY, x, y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)
            );
        }
    }

    private void renderBeaconPowerButton(
        GuiGraphicsExtractor graphics,
        Holder<MobEffect> effect,
        int x,
        int y,
        boolean enabled,
        boolean selected,
        boolean hovered
    ) {
        Identifier buttonSprite = !enabled
            ? BEACON_BUTTON_DISABLED_SPRITE
            : selected ? BEACON_BUTTON_SELECTED_SPRITE : hovered ? BEACON_BUTTON_HIGHLIGHTED_SPRITE : BEACON_BUTTON_SPRITE;
        this.blitSprite(graphics, buttonSprite, x, y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE);
        if (this.minecraft != null) {
            Identifier effectSprite = Hud.getMobEffectSprite(effect);
            this.blitSprite(graphics,
                effectSprite,
                x + BEACON_BUTTON_ICON_OFFSET,
                y + BEACON_BUTTON_ICON_OFFSET,
                BEACON_BUTTON_ICON_SIZE,
                BEACON_BUTTON_ICON_SIZE
            );
        }
    }

    private void renderBeaconActionButton(GuiGraphicsExtractor graphics, Identifier icon, int x, int y, boolean enabled, boolean hovered) {
        Identifier buttonSprite = !enabled ? BEACON_BUTTON_DISABLED_SPRITE : hovered ? BEACON_BUTTON_HIGHLIGHTED_SPRITE : BEACON_BUTTON_SPRITE;
        this.blitSprite(graphics, buttonSprite, x, y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE);
        this.blitSprite(graphics,
            icon,
            x + BEACON_BUTTON_ICON_OFFSET,
            y + BEACON_BUTTON_ICON_OFFSET,
            BEACON_BUTTON_ICON_SIZE,
            BEACON_BUTTON_ICON_SIZE
        );
    }

    private void renderBeaconMaterials(GuiGraphicsExtractor graphics, int contentX, int contentY) {
        ItemStack[] materials = {
            Items.NETHERITE_INGOT.getDefaultInstance(),
            Items.EMERALD.getDefaultInstance(),
            Items.DIAMOND.getDefaultInstance(),
            Items.GOLD_INGOT.getDefaultInstance(),
            Items.IRON_INGOT.getDefaultInstance()
        };
        for (int i = 0; i < materials.length && i < BEACON_MATERIALS_X.length; i++) {
            this.renderItemStack(graphics, materials[i], contentX + BEACON_MATERIALS_X[i], contentY + BEACON_MATERIALS_Y, i);
        }
    }

    private @Nullable SlotHit beaconSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int x = beaconContentX(window) + BEACON_PAYMENT_SLOT_X;
        int y = beaconContentY(window) + BEACON_PAYMENT_SLOT_Y;
        if (!contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)) {
            return null;
        }

        Slot slot = containerSlot(slots, BEACON_PAYMENT_SLOT);
        return slot == null ? null : new SlotHit(slot, menu.slots.indexOf(slot), x, y, menu, window.sessionId());
    }

    private @Nullable BeaconButtonHit beaconButtonAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.minimized || !(window.containerMenu() instanceof BeaconMenu beaconMenu)) {
            return null;
        }

        int contentX = beaconContentX(window);
        int contentY = beaconContentY(window);
        this.syncBeaconSelection(window, beaconMenu);
        for (int tier = 0; tier < Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.size()); tier++) {
            List<Holder<MobEffect>> effects = BeaconBlockEntity.BEACON_EFFECTS.get(tier);
            int rowWidth = effects.size() * BEACON_BUTTON_SIZE + Math.max(0, effects.size() - 1) * BEACON_BUTTON_GAP;
            int rowX = contentX + BEACON_PRIMARY_BUTTON_BASE_X + (2 * BEACON_BUTTON_SIZE + BEACON_BUTTON_GAP - rowWidth) / 2;
            int rowY = contentY + BEACON_PRIMARY_BUTTON_BASE_Y + tier * BEACON_BUTTON_STEP_Y;
            for (int i = 0; i < effects.size(); i++) {
                int x = rowX + i * BEACON_BUTTON_STEP_X;
                if (contains(mouseX, mouseY, x, rowY, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)) {
                    return new BeaconButtonHit(BeaconButtonKind.PRIMARY, effects.get(i));
                }
            }
        }

        if (BeaconBlockEntity.BEACON_EFFECTS.size() > 3) {
            for (Holder<MobEffect> effect : BeaconBlockEntity.BEACON_EFFECTS.get(3)) {
                int x = contentX + BEACON_SECONDARY_BUTTON_X;
                int y = contentY + BEACON_SECONDARY_BUTTON_Y;
                if (contains(mouseX, mouseY, x, y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)) {
                    return new BeaconButtonHit(BeaconButtonKind.SECONDARY, effect);
                }
            }
        }

        if (window.beaconPrimary != null
            && contains(mouseX, mouseY, contentX + BEACON_UPGRADE_BUTTON_X, contentY + BEACON_SECONDARY_BUTTON_Y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)) {
            return new BeaconButtonHit(BeaconButtonKind.UPGRADE, window.beaconPrimary);
        }

        if (contains(mouseX, mouseY, contentX + BEACON_CONFIRM_X, contentY + BEACON_CONFIRM_Y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)) {
            return new BeaconButtonHit(BeaconButtonKind.CONFIRM, null);
        }
        if (contains(mouseX, mouseY, contentX + BEACON_CANCEL_X, contentY + BEACON_CANCEL_Y, BEACON_BUTTON_SIZE, BEACON_BUTTON_SIZE)) {
            return new BeaconButtonHit(BeaconButtonKind.CANCEL, null);
        }

        return null;
    }

    private void syncBeaconSelection(InventoryWindow window, BeaconMenu menu) {
        Holder<MobEffect> menuPrimary = menu.getPrimaryEffect();
        Holder<MobEffect> menuSecondary = menu.getSecondaryEffect();
        if (!window.beaconSelectionDirty || sameBeaconEffect(window.beaconPrimary, menuPrimary) && sameBeaconEffect(window.beaconSecondary, menuSecondary)) {
            window.beaconPrimary = menuPrimary;
            window.beaconSecondary = menuSecondary;
            window.beaconSelectionDirty = false;
        }
    }

    private boolean canSelectBeaconPrimary(BeaconMenu menu, @Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return false;
        }

        int unlockedTiers = Math.min(menu.getLevels(), Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.size()));
        for (int tier = 0; tier < unlockedTiers; tier++) {
            if (BeaconBlockEntity.BEACON_EFFECTS.get(tier).contains(effect)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSelectBeaconSecondary(BeaconMenu menu, @Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary) {
        if (secondary == null) {
            return true;
        }
        if (menu.getLevels() < 4 || primary == null) {
            return false;
        }
        if (sameBeaconEffect(primary, secondary)) {
            return true;
        }
        return BeaconBlockEntity.BEACON_EFFECTS.size() > 3 && BeaconBlockEntity.BEACON_EFFECTS.get(3).contains(secondary);
    }

    private boolean canConfirmBeacon(BeaconMenu menu, InventoryWindow window) {
        return menu.hasPayment()
            && this.canSelectBeaconPrimary(menu, window.beaconPrimary)
            && this.canSelectBeaconSecondary(menu, window.beaconPrimary, window.beaconSecondary);
    }

    private static boolean sameBeaconEffect(@Nullable Holder<MobEffect> first, @Nullable Holder<MobEffect> second) {
        return first == second || first != null && first.equals(second);
    }

    private static int beaconButtonId(@Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary) {
        int primaryId = BeaconMenu.encodeEffect(primary) & BEACON_EFFECT_ID_MASK;
        int secondaryId = BeaconMenu.encodeEffect(secondary) & BEACON_EFFECT_ID_MASK;
        return primaryId | (secondaryId << BEACON_SECONDARY_EFFECT_SHIFT);
    }

    private static int beaconEffectId(@Nullable Holder<MobEffect> effect) {
        return BeaconMenu.encodeEffect(effect);
    }

    private void renderBrewingStandWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        BrewingStandMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = brewingContentX(window);
        int contentY = brewingContentY(window);
        blitRegion(
            graphics,
            BREWING_UI_SLOTS_TEXTURE,
            contentX,
            contentY,
            0,
            0,
            BREWING_UI_SLOTS_TEXTURE_WIDTH,
            BREWING_UI_SLOTS_TEXTURE_HEIGHT,
            BREWING_UI_SLOTS_TEXTURE_WIDTH,
            BREWING_UI_SLOTS_TEXTURE_HEIGHT,
            BREWING_UI_SLOTS_TEXTURE_WIDTH,
            BREWING_UI_SLOTS_TEXTURE_HEIGHT
        );
        this.renderBrewingWidgets(graphics, menu, contentX, contentY);
        this.renderBrewingSlotItem(graphics, slots, BREWING_FUEL_SLOT, contentX + BREWING_FUEL_SLOT_X, contentY + BREWING_FUEL_SLOT_Y, mouseX, mouseY);
        this.renderBrewingSlotItem(graphics, slots, BREWING_INGREDIENT_SLOT, contentX + BREWING_INGREDIENT_SLOT_X, contentY + BREWING_INGREDIENT_SLOT_Y, mouseX, mouseY);
        this.renderBrewingSlotItem(graphics, slots, BREWING_BOTTLE_0_SLOT, contentX + BREWING_BOTTLE_0_SLOT_X, contentY + BREWING_BOTTLE_0_SLOT_Y, mouseX, mouseY);
        this.renderBrewingSlotItem(graphics, slots, BREWING_BOTTLE_1_SLOT, contentX + BREWING_BOTTLE_1_SLOT_X, contentY + BREWING_BOTTLE_1_SLOT_Y, mouseX, mouseY);
        this.renderBrewingSlotItem(graphics, slots, BREWING_BOTTLE_2_SLOT, contentX + BREWING_BOTTLE_2_SLOT_X, contentY + BREWING_BOTTLE_2_SLOT_Y, mouseX, mouseY);
    }

    private void renderBrewingWidgets(GuiGraphicsExtractor graphics, BrewingStandMenu menu, int contentX, int contentY) {
        int fuelWidth = Mth.clamp((BREWING_FUEL_LENGTH_WIDTH * menu.getFuel() + 20 - 1) / 20, 0, BREWING_FUEL_LENGTH_WIDTH);
        if (fuelWidth > 0) {
            this.blitSprite(graphics,
                BREWING_FUEL_LENGTH_SPRITE,
                BREWING_FUEL_LENGTH_WIDTH,
                BREWING_FUEL_LENGTH_HEIGHT,
                0,
                0,
                contentX + BREWING_FUEL_LENGTH_X,
                contentY + BREWING_FUEL_LENGTH_Y,
                fuelWidth,
                BREWING_FUEL_LENGTH_HEIGHT
            );
        }

        int brewingTicks = menu.getBrewingTicks();
        if (brewingTicks <= 0) {
            return;
        }

        int progressHeight = (int) (BREWING_PROGRESS_HEIGHT * (1.0F - brewingTicks / 400.0F));
        if (progressHeight > 0) {
            this.blitSprite(graphics,
                BREWING_PROGRESS_SPRITE,
                BREWING_PROGRESS_WIDTH,
                BREWING_PROGRESS_HEIGHT,
                0,
                0,
                contentX + BREWING_PROGRESS_X,
                contentY + BREWING_PROGRESS_Y,
                BREWING_PROGRESS_WIDTH,
                progressHeight
            );
        }

        int bubbleLength = BREWING_BUBBLE_LENGTHS[brewingTicks / 2 % BREWING_BUBBLE_LENGTHS.length];
        if (bubbleLength > 0) {
            this.blitSprite(graphics,
                BREWING_BUBBLES_SPRITE,
                BREWING_BUBBLES_WIDTH,
                BREWING_BUBBLES_HEIGHT,
                0,
                BREWING_BUBBLES_HEIGHT - bubbleLength,
                contentX + BREWING_BUBBLES_X,
                contentY + BREWING_BUBBLES_BASE_Y - bubbleLength,
                BREWING_BUBBLES_WIDTH,
                bubbleLength
            );
        }
    }

    private void renderBrewingSlotItem(GuiGraphicsExtractor graphics, List<Slot> slots, int containerSlot, int x, int y, int mouseX, int mouseY) {
        Slot slot = containerSlot(slots, containerSlot);
        if (slot == null) {
            return;
        }

        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightBack(graphics, x, y);
        }
        if (slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), x, y, slot.index);
        }
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightFront(graphics, x, y);
        }
    }

    private @Nullable SlotHit brewingSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = brewingContentX(window);
        int contentY = brewingContentY(window);
        int[] containerSlots = {BREWING_FUEL_SLOT, BREWING_INGREDIENT_SLOT, BREWING_BOTTLE_0_SLOT, BREWING_BOTTLE_1_SLOT, BREWING_BOTTLE_2_SLOT};
        int[] slotXs = {
            contentX + BREWING_FUEL_SLOT_X,
            contentX + BREWING_INGREDIENT_SLOT_X,
            contentX + BREWING_BOTTLE_0_SLOT_X,
            contentX + BREWING_BOTTLE_1_SLOT_X,
            contentX + BREWING_BOTTLE_2_SLOT_X
        };
        int[] slotYs = {
            contentY + BREWING_FUEL_SLOT_Y,
            contentY + BREWING_INGREDIENT_SLOT_Y,
            contentY + BREWING_BOTTLE_0_SLOT_Y,
            contentY + BREWING_BOTTLE_1_SLOT_Y,
            contentY + BREWING_BOTTLE_2_SLOT_Y
        };

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private void renderCartographyTableWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        CartographyTableMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = cartographyContentX(window);
        int contentY = cartographyContentY(window);
        this.renderContainerSlotIfPresent(graphics, slots, CARTOGRAPHY_MAP_SLOT, contentX + CARTOGRAPHY_MAP_SLOT_X, contentY + CARTOGRAPHY_MAP_SLOT_Y, mouseX, mouseY);
        blitRegion(
            graphics,
            CARTOGRAPHY_PLUS_TEXTURE,
            contentX + CARTOGRAPHY_PLUS_X,
            contentY + CARTOGRAPHY_PLUS_Y,
            0,
            0,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE,
            CARTOGRAPHY_PLUS_TEXTURE_SIZE
        );
        this.renderContainerSlotIfPresent(graphics, slots, CARTOGRAPHY_ADDITIONAL_SLOT, contentX + CARTOGRAPHY_ADDITIONAL_SLOT_X, contentY + CARTOGRAPHY_ADDITIONAL_SLOT_Y, mouseX, mouseY);
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            contentX + CARTOGRAPHY_ARROW_X,
            contentY + CARTOGRAPHY_ARROW_Y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );

        CartographyPreview preview = this.cartographyPreview(menu);
        if (preview.error()) {
            this.blitSprite(graphics,
                CARTOGRAPHY_ERROR_SPRITE,
                contentX + CARTOGRAPHY_ERROR_X,
                contentY + CARTOGRAPHY_ERROR_Y,
                CARTOGRAPHY_ERROR_WIDTH,
                CARTOGRAPHY_ERROR_HEIGHT
            );
        }
        this.renderCartographyPreview(graphics, preview, contentX + CARTOGRAPHY_PREVIEW_X, contentY + CARTOGRAPHY_PREVIEW_Y);
        this.renderContainerSlotIfPresent(graphics, slots, CARTOGRAPHY_RESULT_SLOT, contentX + CARTOGRAPHY_RESULT_SLOT_X, contentY + CARTOGRAPHY_RESULT_SLOT_Y, mouseX, mouseY);
    }

    private CartographyPreview cartographyPreview(CartographyTableMenu menu) {
        ItemStack additional = menu.getSlot(CARTOGRAPHY_ADDITIONAL_SLOT).getItem();
        boolean emptyMap = additional.is(Items.MAP);
        boolean paper = additional.is(Items.PAPER);
        boolean glassPane = additional.is(Items.GLASS_PANE);
        ItemStack mapStack = menu.getSlot(CARTOGRAPHY_MAP_SLOT).getItem();
        MapId mapId = mapStack.get(DataComponents.MAP_ID);
        MapItemSavedData mapData = null;
        boolean error = false;
        if (mapId != null && this.minecraft != null && this.minecraft.level != null) {
            mapData = MapItem.getSavedData(mapId, this.minecraft.level);
            if (mapData != null) {
                if (mapData.locked && (paper || glassPane)) {
                    error = true;
                }
                if (paper && mapData.scale >= 4) {
                    error = true;
                }
            }
        }

        return new CartographyPreview(mapId, mapData, emptyMap, paper, glassPane, error);
    }

    private void renderCartographyPreview(GuiGraphicsExtractor graphics, CartographyPreview preview, int x, int y) {
        if (preview.paper() && !preview.error()) {
            this.blitSprite(graphics, CARTOGRAPHY_SCALED_MAP_SPRITE, x, y, CARTOGRAPHY_PREVIEW_SIZE, CARTOGRAPHY_PREVIEW_SIZE);
            this.renderCartographyMap(graphics, preview.mapId(), preview.mapData(), x + 18, y + 18, 0.226F);
            return;
        }

        if (preview.emptyMap()) {
            this.blitSprite(graphics, CARTOGRAPHY_DUPLICATED_MAP_SPRITE, x + 16, y, CARTOGRAPHY_DUPLICATED_WIDTH, CARTOGRAPHY_PREVIEW_SIZE);
            this.renderCartographyMap(graphics, preview.mapId(), preview.mapData(), x + 19, y + 3, 0.34F);
            graphics.nextStratum();
            this.blitSprite(graphics, CARTOGRAPHY_DUPLICATED_MAP_SPRITE, x, y + 16, CARTOGRAPHY_DUPLICATED_WIDTH, CARTOGRAPHY_PREVIEW_SIZE);
            this.renderCartographyMap(graphics, preview.mapId(), preview.mapData(), x + 3, y + 19, 0.34F);
            return;
        }

        this.blitSprite(graphics, CARTOGRAPHY_MAP_SPRITE, x, y, CARTOGRAPHY_PREVIEW_SIZE, CARTOGRAPHY_PREVIEW_SIZE);
        this.renderCartographyMap(graphics, preview.mapId(), preview.mapData(), x + 4, y + 4, 0.45F);
        if (preview.glassPane()) {
            this.blitSprite(graphics,
                CARTOGRAPHY_LOCKED_SPRITE,
                x + 51,
                y + 47,
                CARTOGRAPHY_LOCKED_WIDTH,
                CARTOGRAPHY_LOCKED_HEIGHT
            );
        }
    }

    private void renderCartographyMap(GuiGraphicsExtractor graphics, @Nullable MapId mapId, @Nullable MapItemSavedData mapData, int x, int y, float scale) {
        if (mapId == null || mapData == null || this.minecraft == null) {
            return;
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        this.minecraft.getMapRenderer().extractRenderState(mapId, mapData, this.cartographyMapRenderState);
        graphics.map(this.cartographyMapRenderState);
        graphics.pose().popMatrix();
    }

    private @Nullable SlotHit cartographySlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = cartographyContentX(window);
        int contentY = cartographyContentY(window);
        int[] containerSlots = {CARTOGRAPHY_MAP_SLOT, CARTOGRAPHY_ADDITIONAL_SLOT, CARTOGRAPHY_RESULT_SLOT};
        int[] slotXs = {
            contentX + CARTOGRAPHY_MAP_SLOT_X,
            contentX + CARTOGRAPHY_ADDITIONAL_SLOT_X,
            contentX + CARTOGRAPHY_RESULT_SLOT_X
        };
        int[] slotYs = {
            contentY + CARTOGRAPHY_MAP_SLOT_Y,
            contentY + CARTOGRAPHY_ADDITIONAL_SLOT_Y,
            contentY + CARTOGRAPHY_RESULT_SLOT_Y
        };

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private void renderSmithingWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        SmithingMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = smithingContentX(window);
        int contentY = smithingContentY(window);
        SmithingWindowState state = window.smithingState();

        blitRegion(
            graphics,
            SMITHING_HAMMER_TEXTURE,
            contentX + SMITHING_HAMMER_X,
            contentY + SMITHING_HAMMER_Y,
            0,
            0,
            SMITHING_HAMMER_TEXTURE_WIDTH,
            SMITHING_HAMMER_TEXTURE_HEIGHT,
            SMITHING_HAMMER_TEXTURE_WIDTH,
            SMITHING_HAMMER_TEXTURE_HEIGHT,
            SMITHING_HAMMER_TEXTURE_WIDTH,
            SMITHING_HAMMER_TEXTURE_HEIGHT
        );
        graphics.text(this.font, Component.translatable("container.upgrade"), contentX + SMITHING_LABEL_X, contentY + SMITHING_LABEL_Y, this.uiColor(COLOR_WINDOW_TITLE), false);

        this.renderContainerSlotIfPresent(graphics, slots, SMITHING_TEMPLATE_SLOT, contentX + SMITHING_TEMPLATE_SLOT_X, contentY + SMITHING_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, SMITHING_BASE_SLOT, contentX + SMITHING_BASE_SLOT_X, contentY + SMITHING_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, SMITHING_ADDITION_SLOT, contentX + SMITHING_ADDITION_SLOT_X, contentY + SMITHING_SLOT_Y, mouseX, mouseY);
        state.renderSlotIcons(menu, graphics, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), contentX, contentY);
        this.renderSmithingArrow(graphics, contentX + SMITHING_ARROW_X, contentY + SMITHING_ARROW_Y);
        this.renderContainerSlotIfPresent(graphics, slots, SMITHING_RESULT_SLOT, contentX + SMITHING_RESULT_SLOT_X, contentY + SMITHING_SLOT_Y, mouseX, mouseY);
        this.renderSmithingPreview(graphics, state, menu, contentX, contentY);
    }

    private void renderSmithingArrow(GuiGraphicsExtractor graphics, int x, int y) {
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            x,
            y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );
    }

    private void renderSmithingPreview(GuiGraphicsExtractor graphics, SmithingWindowState state, SmithingMenu menu, int contentX, int contentY) {
        ItemStack result = menu.getSlot(SMITHING_RESULT_SLOT).getItem();
        state.updateArmorStandPreview(this.minecraft, result);
        graphics.entity(
            state.armorStandPreview(),
            25.0F,
            SMITHING_ARMOR_STAND_TRANSLATION,
            SMITHING_ARMOR_STAND_ANGLE,
            null,
            contentX + SMITHING_PREVIEW_LEFT,
            contentY + SMITHING_PREVIEW_TOP,
            contentX + SMITHING_PREVIEW_RIGHT,
            contentY + SMITHING_PREVIEW_BOTTOM
        );
    }

    private @Nullable SlotHit smithingSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = smithingContentX(window);
        int contentY = smithingContentY(window);
        int[] containerSlots = {SMITHING_TEMPLATE_SLOT, SMITHING_BASE_SLOT, SMITHING_ADDITION_SLOT, SMITHING_RESULT_SLOT};
        int[] slotXs = {
            contentX + SMITHING_TEMPLATE_SLOT_X,
            contentX + SMITHING_BASE_SLOT_X,
            contentX + SMITHING_ADDITION_SLOT_X,
            contentX + SMITHING_RESULT_SLOT_X
        };
        int[] slotYs = {
            contentY + SMITHING_SLOT_Y,
            contentY + SMITHING_SLOT_Y,
            contentY + SMITHING_SLOT_Y,
            contentY + SMITHING_SLOT_Y
        };

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private void renderGrindstoneWindow(GuiGraphicsExtractor graphics, InventoryWindow window, List<Slot> slots, int mouseX, int mouseY) {
        int contentX = grindstoneContentX(window);
        int contentY = grindstoneContentY(window);
        blitRegion(
            graphics,
            GRINDSTONE_TEXTURE,
            contentX + GRINDSTONE_SPRITE_X,
            contentY + GRINDSTONE_SPRITE_Y,
            0,
            0,
            GRINDSTONE_TEXTURE_WIDTH,
            GRINDSTONE_TEXTURE_HEIGHT,
            GRINDSTONE_TEXTURE_WIDTH,
            GRINDSTONE_TEXTURE_HEIGHT,
            GRINDSTONE_TEXTURE_WIDTH,
            GRINDSTONE_TEXTURE_HEIGHT
        );
        this.renderContainerSlotIfPresent(graphics, slots, GRINDSTONE_INPUT_SLOT, contentX + GRINDSTONE_INPUT_SLOT_X, contentY + GRINDSTONE_INPUT_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, GRINDSTONE_ADDITIONAL_SLOT, contentX + GRINDSTONE_ADDITIONAL_SLOT_X, contentY + GRINDSTONE_ADDITIONAL_SLOT_Y, mouseX, mouseY);
        this.renderSmithingArrow(graphics, contentX + GRINDSTONE_ARROW_X, contentY + GRINDSTONE_ARROW_Y);
        this.renderContainerSlotIfPresent(graphics, slots, GRINDSTONE_RESULT_SLOT, contentX + GRINDSTONE_RESULT_SLOT_X, contentY + GRINDSTONE_RESULT_SLOT_Y, mouseX, mouseY);
    }

    private @Nullable SlotHit grindstoneSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = grindstoneContentX(window);
        int contentY = grindstoneContentY(window);
        int[] containerSlots = {GRINDSTONE_INPUT_SLOT, GRINDSTONE_ADDITIONAL_SLOT, GRINDSTONE_RESULT_SLOT};
        int[] slotXs = {
            contentX + GRINDSTONE_INPUT_SLOT_X,
            contentX + GRINDSTONE_ADDITIONAL_SLOT_X,
            contentX + GRINDSTONE_RESULT_SLOT_X
        };
        int[] slotYs = {
            contentY + GRINDSTONE_INPUT_SLOT_Y,
            contentY + GRINDSTONE_ADDITIONAL_SLOT_Y,
            contentY + GRINDSTONE_RESULT_SLOT_Y
        };

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private void renderStonecutterWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        StonecutterMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = stonecutterContentX(window);
        int contentY = stonecutterContentY(window);
        this.clampStonecutterState(window, menu);

        this.renderContainerSlotIfPresent(graphics, slots, STONECUTTER_INPUT_SLOT, contentX + STONECUTTER_INPUT_SLOT_X, contentY + STONECUTTER_INPUT_SLOT_Y, mouseX, mouseY);
        blitRegion(
            graphics,
            STONECUTTER_CONTAINER_TEXTURE,
            contentX + STONECUTTER_PANEL_X,
            contentY + STONECUTTER_PANEL_Y,
            0,
            0,
            STONECUTTER_CONTAINER_TEXTURE_WIDTH,
            STONECUTTER_CONTAINER_TEXTURE_HEIGHT,
            STONECUTTER_CONTAINER_TEXTURE_WIDTH,
            STONECUTTER_CONTAINER_TEXTURE_HEIGHT,
            STONECUTTER_CONTAINER_TEXTURE_WIDTH,
            STONECUTTER_CONTAINER_TEXTURE_HEIGHT
        );
        this.renderStonecutterRecipes(graphics, window, menu, contentX, contentY, mouseX, mouseY);
        this.renderStonecutterScrollbar(graphics, window, menu, contentX, contentY);
        this.renderLargeContainerSlotIfPresent(graphics, slots, STONECUTTER_RESULT_SLOT, contentX + STONECUTTER_RESULT_SLOT_X, contentY + STONECUTTER_RESULT_SLOT_Y, mouseX, mouseY);
    }

    private void renderStonecutterRecipes(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        StonecutterMenu menu,
        int contentX,
        int contentY,
        int mouseX,
        int mouseY
    ) {
        int recipeCount = menu.getNumberOfVisibleRecipes();
        int selectedRecipe = menu.getSelectedRecipeIndex();
        int firstRecipe = window.stonecutterScroll * STONECUTTER_RECIPE_COLUMNS;
        int lastRecipe = Math.min(recipeCount, firstRecipe + STONECUTTER_VISIBLE_RECIPES);
        for (int recipeIndex = firstRecipe; recipeIndex < lastRecipe; recipeIndex++) {
            int visibleIndex = recipeIndex - firstRecipe;
            int x = contentX + STONECUTTER_RECIPE_GRID_X + visibleIndex % STONECUTTER_RECIPE_COLUMNS * STONECUTTER_RECIPE_BUTTON_WIDTH;
            int y = contentY + STONECUTTER_RECIPE_GRID_Y + visibleIndex / STONECUTTER_RECIPE_COLUMNS * STONECUTTER_RECIPE_BUTTON_HEIGHT;
            Identifier sprite = recipeIndex == selectedRecipe
                ? STONECUTTER_RECIPE_SELECTED_SPRITE
                : contains(mouseX, mouseY, x, y, STONECUTTER_RECIPE_BUTTON_WIDTH, STONECUTTER_RECIPE_BUTTON_HEIGHT)
                    ? STONECUTTER_RECIPE_HIGHLIGHTED_SPRITE
                    : STONECUTTER_RECIPE_SPRITE;
            this.blitSprite(graphics, sprite, x, y, STONECUTTER_RECIPE_BUTTON_WIDTH, STONECUTTER_RECIPE_BUTTON_HEIGHT);

            ItemStack result = this.stonecutterRecipeStack(menu, recipeIndex);
            if (!result.isEmpty()) {
                this.renderItemStack(graphics, result, x, y + 1);
            }
        }
    }

    private void renderStonecutterScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, StonecutterMenu menu, int contentX, int contentY) {
        int maxScroll = stonecutterMaxScroll(menu);
        Identifier sprite = maxScroll > 0 ? STONECUTTER_SCROLLER_SPRITE : STONECUTTER_SCROLLER_DISABLED_SPRITE;
        int thumbOffset = maxScroll <= 0 ? 0 : Math.round(STONECUTTER_SCROLLBAR_TRAVEL * (window.stonecutterScroll / (float) maxScroll));
        this.blitSprite(graphics,
            sprite,
            contentX + STONECUTTER_SCROLLBAR_X,
            contentY + STONECUTTER_SCROLLBAR_Y + thumbOffset,
            STONECUTTER_SCROLLBAR_WIDTH,
            STONECUTTER_SCROLLBAR_HEIGHT
        );
    }

    private @Nullable SlotHit stonecutterSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = stonecutterContentX(window);
        int contentY = stonecutterContentY(window);
        int inputX = contentX + STONECUTTER_INPUT_SLOT_X;
        int inputY = contentY + STONECUTTER_INPUT_SLOT_Y;
        if (contains(mouseX, mouseY, inputX - 1, inputY - 1, SLOT_SIZE, SLOT_SIZE)) {
            Slot slot = containerSlot(slots, STONECUTTER_INPUT_SLOT);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), inputX, inputY, menu, window.sessionId());
            }
        }

        int resultX = contentX + STONECUTTER_RESULT_SLOT_X + LARGE_SLOT_ITEM_OFFSET;
        int resultY = contentY + STONECUTTER_RESULT_SLOT_Y + LARGE_SLOT_ITEM_OFFSET;
        if (contains(mouseX, mouseY, resultX - 1, resultY - 1, SLOT_SIZE, SLOT_SIZE)) {
            Slot slot = containerSlot(slots, STONECUTTER_RESULT_SLOT);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), resultX, resultY, menu, window.sessionId());
            }
        }

        return null;
    }

    private int stonecutterRecipeAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.minimized || !(window.containerMenu() instanceof StonecutterMenu stonecutterMenu) || !this.stonecutterRecipeGridContains(window, mouseX, mouseY)) {
            return -1;
        }

        this.clampStonecutterState(window, stonecutterMenu);
        int contentX = stonecutterContentX(window);
        int contentY = stonecutterContentY(window);
        int relativeX = (int) mouseX - contentX - STONECUTTER_RECIPE_GRID_X;
        int relativeY = (int) mouseY - contentY - STONECUTTER_RECIPE_GRID_Y;
        int column = relativeX / STONECUTTER_RECIPE_BUTTON_WIDTH;
        int row = relativeY / STONECUTTER_RECIPE_BUTTON_HEIGHT;
        int recipeIndex = window.stonecutterScroll * STONECUTTER_RECIPE_COLUMNS + row * STONECUTTER_RECIPE_COLUMNS + column;
        return recipeIndex >= 0 && recipeIndex < stonecutterMenu.getNumberOfVisibleRecipes() ? recipeIndex : -1;
    }

    private boolean stonecutterRecipeGridContains(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            stonecutterContentX(window) + STONECUTTER_RECIPE_GRID_X,
            stonecutterContentY(window) + STONECUTTER_RECIPE_GRID_Y,
            STONECUTTER_RECIPE_COLUMNS * STONECUTTER_RECIPE_BUTTON_WIDTH,
            STONECUTTER_RECIPE_ROWS * STONECUTTER_RECIPE_BUTTON_HEIGHT
        );
    }

    private boolean stonecutterRecipePanelContains(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            stonecutterContentX(window) + STONECUTTER_PANEL_X,
            stonecutterContentY(window) + STONECUTTER_PANEL_Y,
            STONECUTTER_CONTAINER_TEXTURE_WIDTH,
            STONECUTTER_CONTAINER_TEXTURE_HEIGHT
        );
    }

    private void clampStonecutterState(InventoryWindow window, StonecutterMenu menu) {
        window.stonecutterScroll = clamp(window.stonecutterScroll, 0, stonecutterMaxScroll(menu));
    }

    private static int stonecutterMaxScroll(StonecutterMenu menu) {
        return Math.max(0, rowsForSlots(menu.getNumberOfVisibleRecipes(), STONECUTTER_RECIPE_COLUMNS) - STONECUTTER_RECIPE_ROWS);
    }

    private ItemStack stonecutterRecipeStack(StonecutterMenu menu, int recipeIndex) {
        if (this.minecraft == null || this.minecraft.level == null || recipeIndex < 0 || recipeIndex >= menu.getNumberOfVisibleRecipes()) {
            return ItemStack.EMPTY;
        }

        SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes = menu.getVisibleRecipes();
        if (recipeIndex >= recipes.entries().size()) {
            return ItemStack.EMPTY;
        }

        return recipes.entries()
            .get(recipeIndex)
            .recipe()
            .optionDisplay()
            .resolveForFirstStack(SlotDisplayContext.fromLevel(this.minecraft.level));
    }

    private void renderLoomWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        LoomMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = loomContentX(window);
        int contentY = loomContentY(window);
        this.clampLoomState(window, menu);

        blitRegion(
            graphics,
            LOOM_INPUTS_TEXTURE,
            contentX + LOOM_INPUTS_X,
            contentY + LOOM_INPUTS_Y,
            0,
            0,
            LOOM_INPUTS_TEXTURE_WIDTH,
            LOOM_INPUTS_TEXTURE_HEIGHT,
            LOOM_INPUTS_TEXTURE_WIDTH,
            LOOM_INPUTS_TEXTURE_HEIGHT,
            LOOM_INPUTS_TEXTURE_WIDTH,
            LOOM_INPUTS_TEXTURE_HEIGHT
        );
        this.renderLoomSlotIfPresent(graphics, slots, LOOM_BANNER_SLOT, contentX + LOOM_BANNER_SLOT_X, contentY + LOOM_BANNER_SLOT_Y, LOOM_BANNER_SLOT_SPRITE, mouseX, mouseY);
        this.renderLoomSlotIfPresent(graphics, slots, LOOM_DYE_SLOT, contentX + LOOM_DYE_SLOT_X, contentY + LOOM_DYE_SLOT_Y, LOOM_DYE_SLOT_SPRITE, mouseX, mouseY);
        this.renderLoomSlotIfPresent(graphics, slots, LOOM_PATTERN_SLOT, contentX + LOOM_PATTERN_SLOT_X, contentY + LOOM_PATTERN_SLOT_Y, LOOM_PATTERN_SLOT_SPRITE, mouseX, mouseY);

        blitRegion(
            graphics,
            LOOM_OPTIONS_TEXTURE,
            contentX + LOOM_OPTIONS_X,
            contentY + LOOM_OPTIONS_Y,
            0,
            0,
            LOOM_OPTIONS_TEXTURE_WIDTH,
            LOOM_OPTIONS_TEXTURE_HEIGHT,
            LOOM_OPTIONS_TEXTURE_WIDTH,
            LOOM_OPTIONS_TEXTURE_HEIGHT,
            LOOM_OPTIONS_TEXTURE_WIDTH,
            LOOM_OPTIONS_TEXTURE_HEIGHT
        );
        this.renderLoomPatterns(graphics, window, menu, contentX, contentY, mouseX, mouseY);
        this.renderLoomScrollbar(graphics, window, menu, contentX, contentY);

        this.renderLoomPreview(graphics, menu, contentX, contentY);
        this.renderLargeContainerSlotIfPresent(graphics, slots, LOOM_RESULT_SLOT, contentX + LOOM_RESULT_SLOT_X, contentY + LOOM_RESULT_SLOT_Y, mouseX, mouseY);
        if (this.loomHasMaxPatterns(menu)) {
            this.blitSprite(graphics,
                LOOM_ERROR_SPRITE,
                contentX + LOOM_RESULT_SLOT_X + LARGE_SLOT_ITEM_OFFSET - 5,
                contentY + LOOM_RESULT_SLOT_Y + LARGE_SLOT_ITEM_OFFSET - 5,
                26,
                26
            );
        }
    }

    private void renderLoomSlotIfPresent(
        GuiGraphicsExtractor graphics,
        List<Slot> slots,
        int containerSlot,
        int x,
        int y,
        Identifier emptySprite,
        int mouseX,
        int mouseY
    ) {
        Slot slot = containerSlot(slots, containerSlot);
        if (slot == null) {
            return;
        }

        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        renderSlotBackground(graphics, x, y);
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightBack(graphics, x, y);
        }
        if (slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), x, y, slot.index);
        } else {
            this.blitSprite(graphics, emptySprite, x, y, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE);
        }
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightFront(graphics, x, y);
        }
    }

    private void renderLoomPatterns(GuiGraphicsExtractor graphics, InventoryWindow window, LoomMenu menu, int contentX, int contentY, int mouseX, int mouseY) {
        if (!this.shouldDisplayLoomPatterns(menu)) {
            return;
        }

        List<Holder<BannerPattern>> patterns = menu.getSelectablePatterns();
        int selectedPattern = menu.getSelectedBannerPatternIndex();
        int firstPattern = window.loomScroll * LOOM_PATTERN_COLUMNS;
        int lastPattern = Math.min(patterns.size(), firstPattern + LOOM_VISIBLE_PATTERNS);
        for (int patternIndex = firstPattern; patternIndex < lastPattern; patternIndex++) {
            int visibleIndex = patternIndex - firstPattern;
            int x = contentX + LOOM_PATTERN_GRID_X + visibleIndex % LOOM_PATTERN_COLUMNS * LOOM_PATTERN_BUTTON_SIZE;
            int y = contentY + LOOM_PATTERN_GRID_Y + visibleIndex / LOOM_PATTERN_COLUMNS * LOOM_PATTERN_BUTTON_SIZE;
            boolean hovered = contains(mouseX, mouseY, x, y, LOOM_PATTERN_BUTTON_SIZE, LOOM_PATTERN_BUTTON_SIZE);
            Identifier sprite = patternIndex == selectedPattern
                ? LOOM_PATTERN_SELECTED_SPRITE
                : hovered ? LOOM_PATTERN_HIGHLIGHTED_SPRITE : LOOM_PATTERN_SPRITE;
            this.blitSprite(graphics, sprite, x, y, LOOM_PATTERN_BUTTON_SIZE, LOOM_PATTERN_BUTTON_SIZE);
            this.renderLoomPatternIcon(graphics, patterns.get(patternIndex), x, y);
        }
    }

    private void renderLoomPatternIcon(GuiGraphicsExtractor graphics, Holder<BannerPattern> pattern, int x, int y) {
        TextureAtlasSprite sprite = graphics.getSprite(Sheets.getBannerSprite(pattern));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 4.0F, y + 2.0F);
        float u0 = sprite.getU0();
        float u1 = u0 + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
        float vDelta = sprite.getV1() - sprite.getV0();
        float v0 = sprite.getV0() + vDelta / 64.0F;
        float v1 = v0 + vDelta * 40.0F / 64.0F;
        graphics.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
        graphics.blit(sprite.atlasLocation(), 0, 0, 5, 10, u0, u1, v0, v1);
        graphics.pose().popMatrix();
    }

    private void renderLoomScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, LoomMenu menu, int contentX, int contentY) {
        int maxScroll = loomMaxScroll(menu);
        Identifier sprite = maxScroll > 0 ? LOOM_SCROLLER_SPRITE : LOOM_SCROLLER_DISABLED_SPRITE;
        int thumbOffset = maxScroll <= 0 ? 0 : Math.round(LOOM_SCROLLBAR_TRAVEL * (window.loomScroll / (float) maxScroll));
        this.blitSprite(graphics,
            sprite,
            contentX + LOOM_SCROLLBAR_X,
            contentY + LOOM_SCROLLBAR_Y + thumbOffset,
            LOOM_SCROLLBAR_WIDTH,
            LOOM_SCROLLBAR_HEIGHT
        );
    }

    private void renderLoomPreview(GuiGraphicsExtractor graphics, LoomMenu menu, int contentX, int contentY) {
        int x = contentX + LOOM_PREVIEW_X;
        int y = contentY + LOOM_PREVIEW_Y;
        blitRegion(
            graphics,
            LOOM_PREVIEW_TEXTURE,
            x,
            y,
            0,
            0,
            LOOM_PREVIEW_TEXTURE_WIDTH,
            LOOM_PREVIEW_TEXTURE_HEIGHT,
            LOOM_PREVIEW_TEXTURE_WIDTH,
            LOOM_PREVIEW_TEXTURE_HEIGHT,
            LOOM_PREVIEW_TEXTURE_WIDTH,
            LOOM_PREVIEW_TEXTURE_HEIGHT
        );

        ItemStack result = menu.getResultSlot().getItem();
        if (!result.isEmpty() && !this.loomHasMaxPatterns(menu) && result.getItem() instanceof BannerItem bannerItem) {
            BannerPatternLayers patterns = result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            graphics.bannerPattern(this.loomBannerFlagModel(), bannerItem.getColor(), patterns, x, y, x + LOOM_PREVIEW_TEXTURE_WIDTH, y + LOOM_PREVIEW_TEXTURE_HEIGHT);
        }
    }

    private @Nullable SlotHit loomSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = loomContentX(window);
        int contentY = loomContentY(window);
        int[] containerSlots = {LOOM_BANNER_SLOT, LOOM_DYE_SLOT, LOOM_PATTERN_SLOT};
        int[] slotXs = {
            contentX + LOOM_BANNER_SLOT_X,
            contentX + LOOM_DYE_SLOT_X,
            contentX + LOOM_PATTERN_SLOT_X
        };
        int[] slotYs = {
            contentY + LOOM_BANNER_SLOT_Y,
            contentY + LOOM_DYE_SLOT_Y,
            contentY + LOOM_PATTERN_SLOT_Y
        };

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        int resultX = contentX + LOOM_RESULT_SLOT_X + LARGE_SLOT_ITEM_OFFSET;
        int resultY = contentY + LOOM_RESULT_SLOT_Y + LARGE_SLOT_ITEM_OFFSET;
        if (contains(mouseX, mouseY, resultX - 1, resultY - 1, SLOT_SIZE, SLOT_SIZE)) {
            Slot slot = containerSlot(slots, LOOM_RESULT_SLOT);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), resultX, resultY, menu, window.sessionId());
            }
        }

        return null;
    }

    private int loomPatternAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.minimized || !(window.containerMenu() instanceof LoomMenu loomMenu) || !this.shouldDisplayLoomPatterns(loomMenu) || !this.loomPatternGridContains(window, mouseX, mouseY)) {
            return -1;
        }

        this.clampLoomState(window, loomMenu);
        int contentX = loomContentX(window);
        int contentY = loomContentY(window);
        int relativeX = (int) mouseX - contentX - LOOM_PATTERN_GRID_X;
        int relativeY = (int) mouseY - contentY - LOOM_PATTERN_GRID_Y;
        int column = relativeX / LOOM_PATTERN_BUTTON_SIZE;
        int row = relativeY / LOOM_PATTERN_BUTTON_SIZE;
        int patternIndex = window.loomScroll * LOOM_PATTERN_COLUMNS + row * LOOM_PATTERN_COLUMNS + column;
        return patternIndex >= 0 && patternIndex < loomMenu.getSelectablePatterns().size() ? patternIndex : -1;
    }

    private boolean loomPatternGridContains(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            loomContentX(window) + LOOM_PATTERN_GRID_X,
            loomContentY(window) + LOOM_PATTERN_GRID_Y,
            LOOM_PATTERN_COLUMNS * LOOM_PATTERN_BUTTON_SIZE,
            LOOM_PATTERN_ROWS * LOOM_PATTERN_BUTTON_SIZE
        );
    }

    private boolean loomPatternPanelContains(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            loomContentX(window) + LOOM_OPTIONS_X,
            loomContentY(window) + LOOM_OPTIONS_Y,
            LOOM_OPTIONS_TEXTURE_WIDTH,
            LOOM_OPTIONS_TEXTURE_HEIGHT
        );
    }

    private void clampLoomState(InventoryWindow window, LoomMenu menu) {
        window.loomScroll = clamp(window.loomScroll, 0, loomMaxScroll(menu));
    }

    private boolean shouldDisplayLoomPatterns(LoomMenu menu) {
        return !menu.getBannerSlot().getItem().isEmpty()
            && !menu.getDyeSlot().getItem().isEmpty()
            && !this.loomHasMaxPatterns(menu)
            && !menu.getSelectablePatterns().isEmpty();
    }

    private boolean loomHasMaxPatterns(LoomMenu menu) {
        ItemStack banner = menu.getBannerSlot().getItem();
        if (banner.isEmpty()) {
            return false;
        }

        return banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size() >= 6;
    }

    private static int loomMaxScroll(LoomMenu menu) {
        return Math.max(0, rowsForSlots(menu.getSelectablePatterns().size(), LOOM_PATTERN_COLUMNS) - LOOM_PATTERN_ROWS);
    }

    private void renderEnchantmentWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        EnchantmentMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = enchantmentContentX(window);
        int contentY = enchantmentContentY(window);

        this.renderEnchantmentSlotIfPresent(graphics, slots, ENCHANTMENT_ITEM_SLOT, contentX + ENCHANTMENT_ITEM_SLOT_X, contentY + ENCHANTMENT_ITEM_SLOT_Y, mouseX, mouseY);
        this.renderEnchantmentSlotIfPresent(graphics, slots, ENCHANTMENT_LAPIS_SLOT, contentX + ENCHANTMENT_LAPIS_SLOT_X, contentY + ENCHANTMENT_LAPIS_SLOT_Y, mouseX, mouseY);
        this.renderEnchantmentBook(graphics, window, contentX, contentY);

        EnchantmentNames.getInstance().initSeed(menu.getEnchantmentSeed());
        int lapisCount = menu.getGoldCount();
        for (int option = 0; option < 3; option++) {
            this.renderEnchantmentOption(graphics, menu, option, contentX, contentY, lapisCount, mouseX, mouseY);
        }
    }

    private void renderEnchantmentBook(GuiGraphicsExtractor graphics, InventoryWindow window, int contentX, int contentY) {
        BookModel bookModel = this.enchantmentBookModel();
        EnchantmentBookState state = window.enchantmentBookState();
        float partialTick = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int x0 = contentX + ENCHANTMENT_BOOK_X;
        int y0 = contentY + ENCHANTMENT_BOOK_Y;
        graphics.book(
            bookModel,
            ENCHANTING_BOOK_TEXTURE,
            40.0F,
            state.open(partialTick),
            state.flip(partialTick),
            x0,
            y0,
            x0 + ENCHANTMENT_BOOK_WIDTH,
            y0 + ENCHANTMENT_BOOK_HEIGHT
        );
    }

    private void renderEnchantmentOption(
        GuiGraphicsExtractor graphics,
        EnchantmentMenu menu,
        int option,
        int contentX,
        int contentY,
        int lapisCount,
        int mouseX,
        int mouseY
    ) {
        int x = contentX + ENCHANTMENT_BUTTON_X;
        int y = contentY + ENCHANTMENT_BUTTON_Y + ENCHANTMENT_BUTTON_HEIGHT * option;
        int cost = menu.costs[option];
        if (cost <= 0) {
            this.blitSprite(graphics, ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT);
            return;
        }

        String costText = Integer.toString(cost);
        int textWidth = 86 - this.font.width(costText);
        FormattedText randomName = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
        boolean selectable = this.canSelectEnchantment(menu, option);
        boolean hovered = contains(mouseX, mouseY, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT);
        int clueColor = -9937334;
        int costColor;
        if (!selectable) {
            this.blitSprite(graphics, ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT);
            this.blitSprite(graphics, ENCHANTMENT_LEVEL_DISABLED_SPRITES[option], x + 1, y + 1, 16, 16);
            graphics.textWithWordWrap(this.font, randomName, x + 20, y + 2, textWidth, this.uiColor(0xFF6E6855), false);
            costColor = -12550384;
        } else {
            this.blitSprite(graphics,
                hovered ? ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE : ENCHANTMENT_SLOT_SPRITE,
                x,
                y,
                ENCHANTMENT_BUTTON_WIDTH,
                ENCHANTMENT_BUTTON_HEIGHT
            );
            this.blitSprite(graphics, ENCHANTMENT_LEVEL_SPRITES[option], x + 1, y + 1, 16, 16);
            graphics.textWithWordWrap(this.font, randomName, x + 20, y + 2, textWidth, this.uiColor(hovered ? -128 : clueColor), false);
            costColor = -8323296;
        }

        graphics.text(this.font, costText, x + 20 + 86 - this.font.width(costText), y + 9, this.uiColor(costColor));
        if (hovered) {
            this.renderEnchantmentTooltip(graphics, menu, option, lapisCount, cost, mouseX, mouseY);
        }
    }

    private void renderEnchantmentTooltip(GuiGraphicsExtractor graphics, EnchantmentMenu menu, int option, int lapisCount, int cost, int mouseX, int mouseY) {
        if (this.minecraft.level == null || option < 0 || option >= menu.enchantClue.length || option >= menu.levelClue.length) {
            return;
        }

        int enchantmentId = menu.enchantClue[option];
        int enchantmentLevel = menu.levelClue[option];
        if (cost <= 0 || enchantmentLevel < 0) {
            return;
        }

        var enchantment = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentId);
        if (enchantment.isEmpty()) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("container.enchant.clue", Enchantment.getFullname(enchantment.get(), enchantmentLevel)).withStyle(ChatFormatting.WHITE));
        LocalPlayer player = this.player();
        if (player != null && !player.hasInfiniteMaterials()) {
            int lapisCost = option + 1;
            tooltip.add(CommonComponents.EMPTY);
            if (player.experienceLevel < cost) {
                tooltip.add(Component.translatable("container.enchant.level.requirement", cost).withStyle(ChatFormatting.RED));
            } else {
                Component lapisText = lapisCost == 1
                    ? Component.translatable("container.enchant.lapis.one")
                    : Component.translatable("container.enchant.lapis.many", lapisCost);
                tooltip.add(lapisText.copy().withStyle(lapisCount >= lapisCost ? ChatFormatting.GRAY : ChatFormatting.RED));
                Component levelText = lapisCost == 1
                    ? Component.translatable("container.enchant.level.one")
                    : Component.translatable("container.enchant.level.many", lapisCost);
                tooltip.add(levelText.copy().withStyle(ChatFormatting.GRAY));
            }
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
    }

    private void renderEnchantmentSlotIfPresent(GuiGraphicsExtractor graphics, List<Slot> slots, int containerSlot, int x, int y, int mouseX, int mouseY) {
        Slot slot = containerSlot(slots, containerSlot);
        if (slot != null) {
            if (containerSlot == ENCHANTMENT_LAPIS_SLOT && !slot.hasItem()) {
                boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
                renderSlotBackground(graphics, x, y);
                if (hovered) {
                    renderSlotHighlightBack(graphics, x, y);
                }
                this.renderItemStack(graphics, Items.LAPIS_LAZULI.getDefaultInstance(), x, y, slot.index);
                graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, this.uiColor(0xAA8F8F8F));
                if (hovered) {
                    renderSlotHighlightFront(graphics, x, y);
                }
                return;
            }
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    private @Nullable SlotHit enchantmentSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = enchantmentContentX(window);
        int contentY = enchantmentContentY(window);
        int[] containerSlots = {ENCHANTMENT_ITEM_SLOT, ENCHANTMENT_LAPIS_SLOT};
        int[] slotXs = {contentX + ENCHANTMENT_ITEM_SLOT_X, contentX + ENCHANTMENT_LAPIS_SLOT_X};
        int[] slotYs = {contentY + ENCHANTMENT_ITEM_SLOT_Y, contentY + ENCHANTMENT_LAPIS_SLOT_Y};

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private int enchantmentButtonAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.minimized || !(window.containerMenu() instanceof EnchantmentMenu)) {
            return -1;
        }

        int contentX = enchantmentContentX(window);
        int contentY = enchantmentContentY(window);
        int x = contentX + ENCHANTMENT_BUTTON_X;
        for (int option = 0; option < 3; option++) {
            int y = contentY + ENCHANTMENT_BUTTON_Y + ENCHANTMENT_BUTTON_HEIGHT * option;
            if (contains(mouseX, mouseY, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT)) {
                return option;
            }
        }

        return -1;
    }

    private void renderMerchantWindow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        List<Slot> slots,
        MerchantMenu menu,
        int mouseX,
        int mouseY
    ) {
        int contentX = merchantContentX(window);
        int contentY = merchantContentY(window);
        MerchantOffers offers = menu.getOffers();
        this.clampMerchantState(window, offers);

        graphics.fill(
            contentX + MERCHANT_TRADE_LIST_X,
            contentY + MERCHANT_TRADE_LIST_Y,
            contentX + MERCHANT_TRADE_LIST_X + MERCHANT_TRADE_LIST_WIDTH,
            contentY + MERCHANT_TRADE_LIST_Y + MERCHANT_VISIBLE_TRADES * MERCHANT_TRADE_ROW_HEIGHT,
            this.uiColor(0x33111111)
        );

        for (int row = 0; row < MERCHANT_VISIBLE_TRADES; row++) {
            int tradeIndex = window.merchantScroll + row;
            int rowX = contentX + MERCHANT_TRADE_LIST_X;
            int rowY = contentY + MERCHANT_TRADE_LIST_Y + row * MERCHANT_TRADE_ROW_HEIGHT;
            if (tradeIndex < offers.size()) {
                this.renderMerchantTradeRow(graphics, window, offers.get(tradeIndex), tradeIndex, rowX, rowY, mouseX, mouseY);
            } else {
                graphics.outline(rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT - 1, this.uiColor(0x22000000));
            }
        }

        this.renderMerchantScrollbar(graphics, window, offers, contentX, contentY);
        this.renderMerchantDetailLabel(graphics, window, menu, contentX, contentY);
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_PAYMENT_1_SLOT, contentX + MERCHANT_PAYMENT_1_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_PAYMENT_2_SLOT, contentX + MERCHANT_PAYMENT_2_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);

        MerchantOffer selectedOffer = window.merchantSelectedTrade >= 0 && window.merchantSelectedTrade < offers.size() ? offers.get(window.merchantSelectedTrade) : null;
        this.renderMerchantSlotArrow(graphics, contentX + MERCHANT_TRADE_ARROW_X, contentY + MERCHANT_TRADE_ARROW_Y, selectedOffer != null && selectedOffer.isOutOfStock());
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_RESULT_SLOT, contentX + MERCHANT_RESULT_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);
        this.renderMerchantProgress(graphics, menu, selectedOffer, contentX, contentY);
    }

    private void renderMerchantSlotArrow(GuiGraphicsExtractor graphics, int x, int y, boolean outOfStock) {
        if (outOfStock) {
            this.blitSprite(graphics,
                MERCHANT_OUT_OF_STOCK_SPRITE,
                x - 3,
                y - 2,
                MERCHANT_OUT_OF_STOCK_WIDTH,
                MERCHANT_OUT_OF_STOCK_HEIGHT
            );
            return;
        }

        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            x,
            y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );
    }

    private void renderMerchantDetailLabel(GuiGraphicsExtractor graphics, InventoryWindow window, MerchantMenu menu, int contentX, int contentY) {
        int traderLevel = clamp(menu.getTraderLevel(), 1, VillagerData.MAX_VILLAGER_LEVEL);
        Component level = Component.translatable("merchant.level." + traderLevel);
        String label = Component.translatable("merchant.title", window.title, level).getString();
        int labelMaxWidth = MERCHANT_CONTENT_WIDTH - MERCHANT_PROGRESS_X;
        label = this.truncatedTitle(label, labelMaxWidth);
        int x = contentX + MERCHANT_PROGRESS_X + Math.max(0, (labelMaxWidth - this.font.width(label)) / 2) - 3;
        graphics.text(this.font, label, x, contentY + MERCHANT_DETAIL_LABEL_Y, this.uiColor(COLOR_WINDOW_TITLE), false);
    }

    private void renderMerchantTradeRow(
        GuiGraphicsExtractor graphics,
        InventoryWindow window,
        MerchantOffer offer,
        int tradeIndex,
        int rowX,
        int rowY,
        int mouseX,
        int mouseY
    ) {
        boolean hovered = contains(mouseX, mouseY, rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT - 1);
        boolean selected = tradeIndex == window.merchantSelectedTrade;
        int background = selected ? 0x665E8B57 : hovered ? 0x44FFFFFF : 0x22111111;
        if (offer.isOutOfStock()) {
            background = selected ? 0x665B4A4A : hovered ? 0x44A08080 : 0x22330000;
        }

        graphics.fill(rowX, rowY, rowX + MERCHANT_TRADE_LIST_WIDTH, rowY + MERCHANT_TRADE_ROW_HEIGHT - 1, this.uiColor(background));
        graphics.outline(rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT - 1, this.uiColor(selected ? 0xFF111111 : 0x55111111));

        ItemStack costA = offer.getCostA();
        ItemStack baseCostA = offer.getBaseCostA();
        this.renderMerchantOfferItem(graphics, costA, rowX + 4, rowY + 2);
        if (!ItemStack.matches(costA, baseCostA) || costA.getCount() != baseCostA.getCount()) {
            this.blitSprite(graphics, MERCHANT_DISCOUNT_STRIKETHROUGH_SPRITE, rowX + 11, rowY + 14, 9, 2);
        }

        ItemStack costB = offer.getCostB();
        if (!costB.isEmpty()) {
            this.renderMerchantOfferItem(graphics, costB, rowX + 25, rowY + 2);
        }

        this.blitSprite(graphics,
            offer.isOutOfStock() ? MERCHANT_TRADE_ARROW_OUT_OF_STOCK_SPRITE : MERCHANT_TRADE_ARROW_SPRITE,
            rowX + 50,
            rowY + 5,
            MERCHANT_TRADE_ARROW_WIDTH,
            MERCHANT_TRADE_ARROW_HEIGHT
        );
        this.renderMerchantOfferItem(graphics, offer.getResult(), rowX + 68, rowY + 2);

        if (offer.isOutOfStock()) {
            graphics.fill(rowX, rowY, rowX + MERCHANT_TRADE_LIST_WIDTH, rowY + MERCHANT_TRADE_ROW_HEIGHT - 1, this.uiColor(0x44000000));
        }
    }

    private void renderMerchantOfferItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        graphics.fakeItem(stack, x, y);
        graphics.itemDecorations(this.font, stack, x, y);
    }

    private void renderMerchantScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, MerchantOffers offers, int contentX, int contentY) {
        int x = contentX + MERCHANT_TRADE_SCROLLBAR_X;
        int y = contentY + MERCHANT_TRADE_LIST_Y;
        int height = MERCHANT_VISIBLE_TRADES * MERCHANT_TRADE_ROW_HEIGHT - 1;
        int maxScroll = merchantMaxScroll(offers);
        graphics.fill(x + 1, y, x + MERCHANT_TRADE_SCROLLBAR_WIDTH - 1, y + height, this.uiColor(0x33111111));
        Identifier sprite = maxScroll > 0 ? MERCHANT_SCROLLBAR_SPRITE : MERCHANT_SCROLLBAR_DISABLED_SPRITE;
        int thumbHeight = 27;
        int thumbY = y + (maxScroll <= 0 ? 0 : (height - thumbHeight) * window.merchantScroll / maxScroll);
        this.blitSprite(graphics, sprite, x, thumbY, MERCHANT_TRADE_SCROLLBAR_WIDTH, thumbHeight);
    }

    private void renderMerchantProgress(GuiGraphicsExtractor graphics, MerchantMenu menu, @Nullable MerchantOffer selectedOffer, int contentX, int contentY) {
        int traderLevel = menu.getTraderLevel();
        if (!menu.showProgressBar() || !VillagerData.canLevelUp(traderLevel)) {
            return;
        }

        int x = contentX + MERCHANT_PROGRESS_X;
        int y = contentY + MERCHANT_PROGRESS_Y;
        int minXp = VillagerData.getMinXpPerLevel(traderLevel);
        int maxXp = VillagerData.getMaxXpPerLevel(traderLevel);
        int xpRange = Math.max(1, maxXp - minXp);
        int currentXp = clamp(menu.getTraderXp() - minXp, 0, xpRange);
        int previewXp = selectedOffer == null || selectedOffer.isOutOfStock() ? 0 : selectedOffer.getXp();
        int futureXp = Math.max(currentXp, clamp(menu.getFutureTraderXp() - minXp, 0, xpRange));
        futureXp = Math.max(futureXp, clamp(menu.getTraderXp() + previewXp - minXp, 0, xpRange));
        int currentWidth = clamp(Math.round(MERCHANT_PROGRESS_WIDTH * (currentXp / (float) xpRange)), 0, MERCHANT_PROGRESS_WIDTH);
        int futureWidth = clamp(Math.round(MERCHANT_PROGRESS_WIDTH * (futureXp / (float) xpRange)), 0, MERCHANT_PROGRESS_WIDTH);
        graphics.fill(x, y, x + MERCHANT_PROGRESS_WIDTH, y + MERCHANT_PROGRESS_HEIGHT, this.uiColor(0xFF252A33));
        graphics.fill(x, y, x + futureWidth, y + MERCHANT_PROGRESS_HEIGHT, this.uiColor(0xFF6B6B37));
        graphics.fill(x, y, x + currentWidth, y + MERCHANT_PROGRESS_HEIGHT, this.uiColor(0xFF4A9F4A));
        graphics.outline(x, y, MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT, this.uiColor(0xFF111111));
    }

    private @Nullable SlotHit merchantSlotAt(InventoryWindow window, List<Slot> slots, AbstractContainerMenu menu, double mouseX, double mouseY) {
        int contentX = merchantContentX(window);
        int contentY = merchantContentY(window);
        int[] containerSlots = {MERCHANT_PAYMENT_1_SLOT, MERCHANT_PAYMENT_2_SLOT, MERCHANT_RESULT_SLOT};
        int[] slotXs = {contentX + MERCHANT_PAYMENT_1_X, contentX + MERCHANT_PAYMENT_2_X, contentX + MERCHANT_RESULT_X};
        int[] slotYs = {contentY + MERCHANT_SLOT_Y, contentY + MERCHANT_SLOT_Y, contentY + MERCHANT_SLOT_Y};

        for (int i = 0; i < containerSlots.length; i++) {
            if (!contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                continue;
            }

            Slot slot = containerSlot(slots, containerSlots[i]);
            if (slot != null) {
                return new SlotHit(slot, menu.slots.indexOf(slot), slotXs[i], slotYs[i], menu, window.sessionId());
            }
        }

        return null;
    }

    private int merchantTradeAt(InventoryWindow window, double mouseX, double mouseY) {
        if (window.minimized || !(window.containerMenu() instanceof MerchantMenu merchantMenu) || !this.merchantTradeRowsContain(window, mouseX, mouseY)) {
            return -1;
        }

        MerchantOffers offers = merchantMenu.getOffers();
        this.clampMerchantState(window, offers);
        int contentY = merchantContentY(window);
        int row = ((int) mouseY - contentY - MERCHANT_TRADE_LIST_Y) / MERCHANT_TRADE_ROW_HEIGHT;
        int tradeIndex = window.merchantScroll + row;
        return tradeIndex >= 0 && tradeIndex < offers.size() ? tradeIndex : -1;
    }

    private @Nullable MerchantOfferItemHit merchantOfferItemAt(double mouseX, double mouseY) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            InventoryWindow window = this.windows.get(i);
            if (window.minimized || !(window.containerMenu() instanceof MerchantMenu merchantMenu) || !this.merchantTradeRowsContain(window, mouseX, mouseY)) {
                continue;
            }

            MerchantOffers offers = merchantMenu.getOffers();
            this.clampMerchantState(window, offers);
            int contentX = merchantContentX(window);
            int contentY = merchantContentY(window);
            int row = ((int) mouseY - contentY - MERCHANT_TRADE_LIST_Y) / MERCHANT_TRADE_ROW_HEIGHT;
            int tradeIndex = window.merchantScroll + row;
            if (tradeIndex < 0 || tradeIndex >= offers.size()) {
                continue;
            }

            MerchantOffer offer = offers.get(tradeIndex);
            int rowX = contentX + MERCHANT_TRADE_LIST_X;
            int rowY = contentY + MERCHANT_TRADE_LIST_Y + row * MERCHANT_TRADE_ROW_HEIGHT;
            if (contains(mouseX, mouseY, rowX + 4, rowY + 2, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE)) {
                return new MerchantOfferItemHit(offer.getCostA());
            }
            if (!offer.getCostB().isEmpty() && contains(mouseX, mouseY, rowX + 25, rowY + 2, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE)) {
                return new MerchantOfferItemHit(offer.getCostB());
            }
            if (contains(mouseX, mouseY, rowX + 68, rowY + 2, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE)) {
                return new MerchantOfferItemHit(offer.getResult());
            }
        }

        return null;
    }

    private boolean merchantTradeRowsContain(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            merchantContentX(window) + MERCHANT_TRADE_LIST_X,
            merchantContentY(window) + MERCHANT_TRADE_LIST_Y,
            MERCHANT_TRADE_LIST_WIDTH,
            MERCHANT_VISIBLE_TRADES * MERCHANT_TRADE_ROW_HEIGHT
        );
    }

    private boolean merchantTradeListContains(InventoryWindow window, double mouseX, double mouseY) {
        return contains(
            mouseX,
            mouseY,
            merchantContentX(window) + MERCHANT_TRADE_LIST_X,
            merchantContentY(window) + MERCHANT_TRADE_LIST_Y,
            MERCHANT_TRADE_LIST_WIDTH + MERCHANT_TRADE_SCROLLBAR_WIDTH + 1,
            MERCHANT_VISIBLE_TRADES * MERCHANT_TRADE_ROW_HEIGHT
        );
    }

    private void clampMerchantState(InventoryWindow window, MerchantOffers offers) {
        int maxScroll = merchantMaxScroll(offers);
        window.merchantScroll = clamp(window.merchantScroll, 0, maxScroll);
        if (offers.isEmpty()) {
            window.merchantSelectedTrade = -1;
        } else if (window.merchantSelectedTrade < 0 || window.merchantSelectedTrade >= offers.size()) {
            window.merchantSelectedTrade = 0;
        }
    }

    private static int merchantMaxScroll(MerchantOffers offers) {
        return Math.max(0, offers.size() - MERCHANT_VISIBLE_TRADES);
    }

    private boolean canSelectEnchantment(EnchantmentMenu menu, int option) {
        if (option < 0 || option >= menu.costs.length || menu.costs[option] <= 0) {
            return false;
        }

        LocalPlayer player = this.player();
        if (player == null) {
            return false;
        }
        if (player.hasInfiniteMaterials()) {
            return true;
        }

        return menu.getGoldCount() >= option + 1 && player.experienceLevel >= menu.costs[option];
    }

    private BookModel enchantmentBookModel() {
        if (this.enchantmentBookModel == null) {
            this.enchantmentBookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
        }

        return this.enchantmentBookModel;
    }

    private BannerFlagModel loomBannerFlagModel() {
        if (this.loomBannerFlagModel == null) {
            this.loomBannerFlagModel = new BannerFlagModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        }

        return this.loomBannerFlagModel;
    }

    private static int enchantmentContentX(InventoryWindow window) {
        return window.x + ENCHANTMENT_CONTENT_MARGIN;
    }

    private static int enchantmentContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + ENCHANTMENT_CONTENT_MARGIN;
    }

    private static int merchantContentX(InventoryWindow window) {
        return window.x + MERCHANT_CONTENT_MARGIN;
    }

    private static int merchantContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + MERCHANT_CONTENT_MARGIN;
    }

    private static int smithingContentX(InventoryWindow window) {
        return window.x + SMITHING_CONTENT_MARGIN;
    }

    private static int smithingContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + SMITHING_CONTENT_MARGIN;
    }

    private static int grindstoneContentX(InventoryWindow window) {
        return window.x + GRINDSTONE_CONTENT_MARGIN;
    }

    private static int grindstoneContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + GRINDSTONE_CONTENT_MARGIN;
    }

    private static int stonecutterContentX(InventoryWindow window) {
        return window.x + STONECUTTER_CONTENT_MARGIN;
    }

    private static int stonecutterContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + STONECUTTER_CONTENT_MARGIN;
    }

    private static int loomContentX(InventoryWindow window) {
        return window.x + LOOM_CONTENT_MARGIN;
    }

    private static int loomContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + LOOM_CONTENT_MARGIN;
    }

    private static int furnaceContentX(InventoryWindow window) {
        return window.x + FURNACE_CONTENT_MARGIN;
    }

    private static int furnaceContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + FURNACE_CONTENT_MARGIN;
    }

    private static int craftingTableContentX(InventoryWindow window) {
        return window.x + CRAFTING_TABLE_CONTENT_MARGIN;
    }

    private static int craftingTableContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + CRAFTING_TABLE_CONTENT_MARGIN;
    }

    private static int anvilContentX(InventoryWindow window) {
        return window.x + ANVIL_CONTENT_MARGIN;
    }

    private static int anvilContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + ANVIL_CONTENT_MARGIN;
    }

    private static int crafterContentX(InventoryWindow window) {
        return window.x + CRAFTER_CONTENT_MARGIN;
    }

    private static int crafterContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + CRAFTER_CONTENT_MARGIN;
    }

    private static int beaconContentX(InventoryWindow window) {
        return window.x + BEACON_CONTENT_MARGIN;
    }

    private static int beaconContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + BEACON_CONTENT_MARGIN;
    }

    private static int brewingContentX(InventoryWindow window) {
        return window.x + BREWING_CONTENT_MARGIN;
    }

    private static int brewingContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + BREWING_CONTENT_MARGIN;
    }

    private static int cartographyContentX(InventoryWindow window) {
        return window.x + CARTOGRAPHY_CONTENT_MARGIN;
    }

    private static int cartographyContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + CARTOGRAPHY_CONTENT_MARGIN;
    }

    private static int characterContentX(InventoryWindow window) {
        return window.x + CHARACTER_CONTENT_MARGIN;
    }

    private static int characterContentY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + CHARACTER_CONTENT_MARGIN;
    }

    private void renderCompactSlots(GuiGraphicsExtractor graphics, InventoryWindow window, List<Slot> slots, SlotGridLayout layout, int mouseX, int mouseY) {
        int firstVisibleSlot = window.scrollRow * layout.columns();
        for (int row = 0; row < layout.visibleRows(); row++) {
            for (int column = 0; column < layout.columns(); column++) {
                int visibleIndex = firstVisibleSlot + row * layout.columns() + column;
                if (visibleIndex >= slots.size()) {
                    continue;
                }

                int x = window.contentX() + column * SLOT_SIZE;
                int y = window.contentY() + row * SLOT_SIZE;
                this.renderSlot(graphics, slots.get(visibleIndex), x, y, mouseX, mouseY);
            }
        }
    }

    private void renderIncreaseInventoryButton(GuiGraphicsExtractor graphics, InventoryWindow window, SlotGridLayout layout, int mouseX, int mouseY) {
        InventoryIncreaseButtonRect rect = this.increaseInventoryButtonRect(window, layout);
        if (rect == null) {
            return;
        }

        this.renderIncreaseInventoryButtonAt(graphics, rect, mouseX, mouseY);
    }

    private void renderIncreaseInventoryButtonAt(GuiGraphicsExtractor graphics, InventoryIncreaseButtonRect rect, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, rect.x() - 1, rect.y() - 1, SLOT_SIZE, SLOT_SIZE);
        int sourceX = hovered ? INCREASE_INVENTORY_BUTTON_FRAME_SIZE : 0;
        blitRegion(
            graphics,
            INCREASE_INVENTORY_BUTTON_TEXTURE,
            rect.x() - 1,
            rect.y() - 1,
            sourceX,
            0,
            INCREASE_INVENTORY_BUTTON_FRAME_SIZE,
            INCREASE_INVENTORY_BUTTON_FRAME_SIZE,
            INCREASE_INVENTORY_BUTTON_FRAME_SIZE,
            INCREASE_INVENTORY_BUTTON_FRAME_SIZE,
            INCREASE_INVENTORY_BUTTON_TEXTURE_WIDTH,
            INCREASE_INVENTORY_BUTTON_TEXTURE_HEIGHT
        );

        LocalPlayer player = this.player();
        if (player != null && player.experienceLevel < InventoryExpansion.costForNextSlot(player)) {
            graphics.fill(rect.x() - 1, rect.y() - 1, rect.x() - 1 + SLOT_SIZE, rect.y() - 1 + SLOT_SIZE, this.uiColor(0x66000000));
        }
    }

    private @Nullable InventoryIncreaseButtonRect increaseInventoryButtonRect(InventoryWindow window) {
        if (window.minimized || !SaltsInventoryConfig.get().expandableInventory) {
            return null;
        }

        if (window.kind == WindowKind.INVENTORY) {
            return this.increaseInventoryButtonRect(window, this.storageLayout(window, this.inventoryVirtualSlotCount()));
        }

        if (window.kind == WindowKind.CREATIVE) {
            return this.creativeIncreaseInventoryButtonRect(window);
        }

        return null;
    }

    private @Nullable InventoryIncreaseButtonRect increaseInventoryButtonRect(InventoryWindow window, SlotGridLayout layout) {
        if (window.kind != WindowKind.INVENTORY || window.minimized || !SaltsInventoryConfig.get().expandableInventory) {
            return null;
        }

        int buttonIndex = this.mainInventorySlots().size();
        window.scrollRow = clamp(window.scrollRow, 0, layout.maxScrollRow());
        int firstVisibleSlot = window.scrollRow * layout.columns();
        int visibleIndex = buttonIndex - firstVisibleSlot;
        if (visibleIndex < 0 || visibleIndex >= layout.visibleRows() * layout.columns()) {
            return null;
        }

        int column = visibleIndex % layout.columns();
        int row = visibleIndex / layout.columns();
        return new InventoryIncreaseButtonRect(window.contentX() + column * SLOT_SIZE, window.contentY() + row * SLOT_SIZE);
    }

    private @Nullable InventoryIncreaseButtonRect creativeIncreaseInventoryButtonRect(InventoryWindow window) {
        if (window.kind != WindowKind.CREATIVE || window.minimized || !SaltsInventoryConfig.get().expandableInventory) {
            return null;
        }

        CreativeModeTab selectedTab = this.selectedCreativeTab(window);
        if (selectedTab == null || !this.isCreativeInventoryTab(selectedTab)) {
            return null;
        }

        int buttonIndex = this.mainInventorySlots().size();
        int row = buttonIndex / CREATIVE_GRID_COLUMNS;
        if (row >= CREATIVE_GRID_ROWS) {
            return null;
        }

        int column = buttonIndex % CREATIVE_GRID_COLUMNS;
        return new InventoryIncreaseButtonRect(
            creativePanelX(window) + CREATIVE_GRID_X + column * SLOT_SIZE,
            creativePanelY(window) + CREATIVE_INVENTORY_GRID_Y + row * SLOT_SIZE
        );
    }

    private boolean increaseInventoryButtonContains(InventoryWindow window, double mouseX, double mouseY) {
        InventoryIncreaseButtonRect rect = this.increaseInventoryButtonRect(window);
        return rect != null && contains(mouseX, mouseY, rect.x() - 1, rect.y() - 1, SLOT_SIZE, SLOT_SIZE);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, SlotGridLayout layout) {
        if (!layout.scrollable()) {
            return;
        }

        int maxScroll = layout.maxScrollRow();
        int x = this.storageScrollbarX(window);
        int y = this.storageScrollbarY(window);
        int height = this.storageScrollbarHeight(layout);
        this.renderCreativeScrollbarBackground(graphics, x, y, height);
        Identifier sprite = maxScroll > 0 ? CREATIVE_SCROLLER_SPRITE : CREATIVE_SCROLLER_DISABLED_SPRITE;
        int offset = maxScroll == 0 ? 0 : Math.round((float) window.scrollRow / (float) maxScroll * this.storageScrollbarTrackHeight(layout));
        this.blitSprite(graphics, sprite, x + SCROLLBAR_INSET, y + SCROLLBAR_INSET + offset, SCROLLBAR_THUMB_WIDTH, SCROLLBAR_THUMB_HEIGHT);
    }

    private boolean storageScrollbarContains(InventoryWindow window, double mouseX, double mouseY) {
        if (!this.isResizableStorageWindow(window) || window.minimized) {
            return false;
        }

        SlotGridLayout layout = this.storageLayout(window, this.storageSlotCount(window));
        return layout.scrollable()
            && contains(mouseX, mouseY, this.storageScrollbarX(window), this.storageScrollbarY(window), SCROLLBAR_WIDTH, this.storageScrollbarHeight(layout));
    }

    private void updateStorageScrollFromMouse(InventoryWindow window, double mouseY) {
        if (!this.isResizableStorageWindow(window) || window.minimized) {
            return;
        }

        SlotGridLayout layout = this.storageLayout(window, this.storageSlotCount(window));
        if (!layout.scrollable()) {
            window.scrollRow = 0;
            return;
        }

        int trackTop = this.storageScrollbarY(window);
        double amount = (mouseY - trackTop - SCROLLBAR_INSET - SCROLLBAR_THUMB_HEIGHT / 2.0D) / Math.max(1.0D, this.storageScrollbarTrackHeight(layout));
        int oldScroll = window.scrollRow;
        window.scrollRow = clamp(Math.round((float) amount * layout.maxScrollRow()), 0, layout.maxScrollRow());
        if (oldScroll != window.scrollRow) {
            DesktopDebug.trace("client storage scrollbar drag desktop={} window={} old={} new={} max={}", this.desktopId, window.debugName(), oldScroll, window.scrollRow, layout.maxScrollRow());
        }
    }

    private int storageScrollbarX(InventoryWindow window) {
        return window.x + window.width - WINDOW_CONTENT_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_RIGHT_INSET - 1;
    }

    private int storageScrollbarY(InventoryWindow window) {
        return window.contentY() - 1;
    }

    private int storageScrollbarHeight(SlotGridLayout layout) {
        return layout.visibleRows() * SLOT_SIZE;
    }

    private int storageScrollbarTrackHeight(SlotGridLayout layout) {
        return Math.max(1, this.storageScrollbarHeight(layout) - SCROLLBAR_INSET * 2 - SCROLLBAR_THUMB_HEIGHT);
    }

    private void renderResizeGrip(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        if (!this.canResizeWindow(window)) {
            return;
        }

        int color = window.resizeGripAt(mouseX, mouseY) ? 0xFF111111 : 0x99111111;
        int right = window.x + window.width - 4;
        int bottom = window.y + window.height - 4;
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            graphics.fill(right - offset - 1, bottom - 1, right, bottom, color);
            graphics.fill(right - 1, bottom - offset - 1, right, bottom, color);
        }
    }

    private void renderCharacterWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        AbstractContainerMenu menu = this.playerMenu();
        int contentX = characterContentX(window);
        int contentY = characterContentY(window);
        int armorX = contentX + CHARACTER_ARMOR_X;
        int armorY = contentY + CHARACTER_ARMOR_Y;
        for (int i = 0; i < 4; i++) {
            int index = 5 + i;
            if (index < menu.slots.size()) {
                this.renderSlot(graphics, menu.slots.get(index), armorX, armorY + i * SLOT_SIZE, mouseX, mouseY);
            }
        }

        int modelX0 = contentX + CHARACTER_MODEL_X;
        int modelY0 = contentY + CHARACTER_MODEL_Y;
        int modelX1 = modelX0 + CHARACTER_MODEL_WIDTH;
        int modelY1 = modelY0 + CHARACTER_MODEL_HEIGHT;
        renderOnePixelNineSlice(graphics, MODEL_DISPLAY_TEXTURE, modelX0, modelY0, CHARACTER_MODEL_WIDTH, CHARACTER_MODEL_HEIGHT);
        if (this.minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, modelX0, modelY0, modelX1, modelY1, CHARACTER_MODEL_SCALE, 0.0625F, mouseX, mouseY, this.minecraft.player);
        }

        this.renderCharacterStats(graphics, contentX + CHARACTER_STATS_X, contentY + CHARACTER_STATS_Y);
        this.renderCharacterCraftingSlots(graphics, window, mouseX, mouseY);
        this.renderCharacterEffects(graphics, window);
    }

    private void renderCharacterStats(GuiGraphicsExtractor graphics, int x, int y) {
        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }

        FoodData food = player.getFoodData();
        this.renderCharacterStatLine(graphics, x, y, "Health", Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()));
        this.renderCharacterStatLine(graphics, x, y + CHARACTER_STATS_LINE_HEIGHT, "Hunger", food.getFoodLevel() + "/20");
        this.renderCharacterStatLine(graphics, x, y + CHARACTER_STATS_LINE_HEIGHT * 2, "XP", Integer.toString(player.experienceLevel));
    }

    private void renderCharacterStatLine(GuiGraphicsExtractor graphics, int x, int y, String label, String value) {
        graphics.text(this.font, label, x, y, this.uiColor(COLOR_WINDOW_TITLE), false);
        graphics.text(this.font, value, x + CHARACTER_STATS_VALUE_X, y, this.uiColor(COLOR_WINDOW_TITLE), false);
    }

    private void renderCharacterEffects(GuiGraphicsExtractor graphics, InventoryWindow window) {
        Player player = this.minecraft.player;
        if (player == null || player.getActiveEffects().isEmpty()) {
            return;
        }

        int x = Math.min(window.x + window.width + CHARACTER_EFFECT_GAP, Math.max(0, this.desktopWidth() - CHARACTER_EFFECT_WIDTH - 2));
        int y = Math.max(2, window.y + TOP_BAR_HEIGHT);
        int maxRows = Math.max(1, (this.desktopHeight() - y - 2) / CHARACTER_EFFECT_HEIGHT);
        Collection<MobEffectInstance> effectCollection = player.getActiveEffects();
        List<MobEffectInstance> effects = new ArrayList<>(effectCollection);
        effects.sort(null);
        int visibleEffectRows = effects.size() > maxRows ? Math.max(0, maxRows - 1) : maxRows;
        int rendered = 0;
        for (MobEffectInstance effect : effects) {
            if (rendered >= visibleEffectRows) {
                break;
            }
            int rowY = y + rendered * CHARACTER_EFFECT_HEIGHT;
            Identifier background = effect.getEffect().value().isBeneficial()
                ? INVENTORY_EFFECT_BACKGROUND_SPRITE
                : INVENTORY_EFFECT_BACKGROUND_AMBIENT_SPRITE;
            this.blitSprite(graphics, background, x, rowY, CHARACTER_EFFECT_WIDTH, CHARACTER_EFFECT_HEIGHT);
            Identifier effectSprite = Hud.getMobEffectSprite(effect.getEffect());
            this.blitSprite(graphics, effectSprite, x + CHARACTER_EFFECT_ICON_X, rowY + CHARACTER_EFFECT_ICON_Y, CHARACTER_EFFECT_ICON_SIZE, CHARACTER_EFFECT_ICON_SIZE);
            graphics.text(this.font, Component.translatable(effect.getDescriptionId()), x + CHARACTER_EFFECT_TEXT_X, rowY + CHARACTER_EFFECT_NAME_Y, this.uiColor(CHARACTER_EFFECT_NAME_COLOR), false);
            graphics.text(this.font, effectDurationText(effect), x + CHARACTER_EFFECT_TEXT_X, rowY + CHARACTER_EFFECT_DURATION_Y, this.uiColor(CHARACTER_EFFECT_DURATION_COLOR), false);
            rendered++;
        }

        if (rendered < effects.size()) {
            int rowY = y + rendered * CHARACTER_EFFECT_HEIGHT;
            this.blitSprite(graphics, INVENTORY_EFFECT_BACKGROUND_SPRITE, x, rowY, CHARACTER_EFFECT_WIDTH, CHARACTER_EFFECT_HEIGHT);
            graphics.text(this.font, "+" + (effects.size() - rendered) + " more", x + 8, rowY + 8, this.uiColor(CHARACTER_EFFECT_NAME_COLOR), false);
        }
    }

    private void renderBar(GuiGraphicsExtractor graphics, int x, int y, int width, float progress, int color) {
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        graphics.fill(x, y, x + width, y + 5, this.uiColor(0xFF252A33));
        graphics.fill(x, y, x + filled, y + 5, this.uiColor(color));
        graphics.outline(x, y, width, 5, this.uiColor(0xFF697386));
    }

    private void renderCharacterCraftingSlots(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        AbstractContainerMenu menu = this.playerMenu();
        int contentX = characterContentX(window);
        int contentY = characterContentY(window);
        int craftX = contentX + CHARACTER_CRAFT_X;
        int craftY = contentY + CHARACTER_CRAFT_Y;
        this.renderSlotIfPresent(graphics, menu, 1, craftX, craftY, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 2, craftX + SLOT_SIZE, craftY, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 3, craftX, craftY + SLOT_SIZE, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 4, craftX + SLOT_SIZE, craftY + SLOT_SIZE, mouseX, mouseY);
        blitRegion(
            graphics,
            CONTAINER_WIDGETS_TEXTURE,
            contentX + CHARACTER_CRAFT_ARROW_X,
            contentY + CHARACTER_CRAFT_ARROW_Y,
            WIDGET_ARROW_EMPTY_X,
            WIDGET_ARROW_EMPTY_Y,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            WIDGET_ARROW_WIDTH,
            WIDGET_ARROW_HEIGHT,
            CONTAINER_WIDGETS_TEXTURE_WIDTH,
            CONTAINER_WIDGETS_TEXTURE_HEIGHT
        );
        this.renderSlotIfPresent(graphics, menu, 0, contentX + CHARACTER_CRAFT_RESULT_X, contentY + CHARACTER_CRAFT_RESULT_Y, mouseX, mouseY);
        this.renderRecipeBookButton(graphics, window, contentX + CHARACTER_RECIPE_BUTTON_X, contentY + CHARACTER_RECIPE_BUTTON_Y, mouseX, mouseY);
    }

    private void renderRecipeBookButton(GuiGraphicsExtractor graphics, InventoryWindow window, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x, y, RECIPE_BOOK_BUTTON_WIDTH, RECIPE_BOOK_BUTTON_HEIGHT);
        RecipeBookComponent<?> recipeBook = window.recipeBook;
        boolean active = recipeBook != null && recipeBook.isVisible();
        Identifier sprite = RecipeBookComponent.RECIPE_BUTTON_SPRITES.get(true, hovered || active);
        this.blitSprite(graphics, sprite, x, y, RECIPE_BOOK_BUTTON_WIDTH, RECIPE_BOOK_BUTTON_HEIGHT);
    }

    private static String effectDurationText(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return "**:**";
        }

        int seconds = Math.max(0, effect.getDuration() / 20);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private void renderSlotIfPresent(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, int slotIndex, int x, int y, int mouseX, int mouseY) {
        if (slotIndex < menu.slots.size()) {
            this.renderSlot(graphics, menu.slots.get(slotIndex), x, y, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        DragSlotPreview dragPreview = this.dragSlotPreview(slot);
        if (hovered) {
            DesktopDebug.detail(
                "client render slot desktop={} slotIndex={} containerSlot={} x={} y={} active={} fake={} highlightable={} stack={} dragPreview={}",
                this.desktopId,
                slot.index,
                slot.getContainerSlot(),
                x,
                y,
                slot.isActive(),
                slot.isFake(),
                slot.isHighlightable(),
                slot.getItem(),
                dragPreview == null ? "none" : dragPreview.stack()
            );
        }
        renderSlotBackground(graphics, x, y);
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightBack(graphics, x, y);
        }
        if (dragPreview != null) {
            graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_DRAG_PREVIEW);
            this.renderItemStack(graphics, dragPreview.stack(), x, y, slot.index, dragPreview.countText());
        } else if (slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), x, y, slot.index);
        } else if (slot.getNoItemIcon() != null) {
            this.blitSprite(graphics, slot.getNoItemIcon(), x, y, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE);
        }
        if (hovered && slot.isHighlightable()) {
            renderSlotHighlightFront(graphics, x, y);
        }
    }

    private void renderItemStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        graphics.item(stack, x, y);
        graphics.itemDecorations(this.font, stack, x, y);
        this.renderGhostItemWash(graphics, x, y);
    }

    private void renderItemStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int seed) {
        this.renderItemStack(graphics, stack, x, y, seed, null);
    }

    private void renderItemStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int seed, @Nullable String countText) {
        if (stack.isEmpty()) {
            return;
        }

        graphics.item(stack, x, y, seed);
        if (countText == null) {
            graphics.itemDecorations(this.font, stack, x, y);
        } else {
            graphics.itemDecorations(this.font, stack, x, y, countText);
        }
        this.renderGhostItemWash(graphics, x, y);
    }

    private void renderGhostItemWash(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.renderingGhostWindow) {
            graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, this.uiColor(GHOST_ITEM_WASH));
        }
    }

    private void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, currentGuiAlpha());
    }

    private void blitSprite(
        GuiGraphicsExtractor graphics,
        Identifier sprite,
        int sourceWidth,
        int sourceHeight,
        int sourceX,
        int sourceY,
        int x,
        int y,
        int width,
        int height
    ) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, sourceWidth, sourceHeight, sourceX, sourceY, x, y, width, height);
        if (this.renderingGhostWindow) {
            graphics.fill(x, y, x + width, y + height, this.uiColor(GHOST_ITEM_WASH));
        }
    }

    private void extractHoveredTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        InventoryWindow hoveredWindow = this.windowAt(mouseX, mouseY);
        InventoryWindow recipeBookWindow = this.recipeBookWindowAt(mouseX, mouseY);
        if (recipeBookWindow != null && (hoveredWindow == null || hoveredWindow == recipeBookWindow) && this.sharedCarried.isEmpty()) {
            RecipeBookComponent<?> recipeBook = this.visibleRecipeBook(recipeBookWindow);
            if (recipeBook != null) {
                recipeBook.extractTooltip(graphics, mouseX, mouseY, null);
                return;
            }
        }

        if (hoveredWindow != null
            && this.sharedCarried.isEmpty()
            && (hoveredWindow.kind == WindowKind.INVENTORY || hoveredWindow.kind == WindowKind.CREATIVE)
            && this.increaseInventoryButtonContains(hoveredWindow, mouseX, mouseY)) {
            LocalPlayer player = this.player();
            if (player != null) {
                int cost = InventoryExpansion.costForNextSlot(player);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("tooltip.salts_inventory_update.inventory_slot.add"));
                tooltip.add(Component.translatable("tooltip.salts_inventory_update.inventory_slot.cost", cost)
                    .withStyle(player.experienceLevel >= cost ? ChatFormatting.GRAY : ChatFormatting.RED));
                tooltip.add(Component.translatable("tooltip.salts_inventory_update.inventory_slot.have", player.experienceLevel).withStyle(ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }

        if (this.sharedCarried.isEmpty() && hoveredWindow != null && this.apiAppendTooltip(graphics, hoveredWindow, mouseX, mouseY)) {
            return;
        }

        SlotHit hit = this.slotAt(mouseX, mouseY);
        if (hit != null && hit.slot().hasItem() && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hit.slot().getItem(), mouseX, mouseY);
            return;
        }

        MerchantOfferItemHit offerHit = this.merchantOfferItemAt(mouseX, mouseY);
        if (offerHit != null && !offerHit.stack().isEmpty() && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, offerHit.stack(), mouseX, mouseY);
            return;
        }

        CreativeItemHit creativeItemHit = this.creativeItemAt(mouseX, mouseY);
        if (creativeItemHit != null && !creativeItemHit.stack().isEmpty() && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, creativeItemHit.stack(), mouseX, mouseY);
            return;
        }

        CreativeTabHit creativeTabHit = this.creativeTabAt(mouseX, mouseY);
        if (creativeTabHit != null && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, creativeTabHit.tab().getDisplayName(), mouseX, mouseY);
            return;
        }

        InventoryWindow window = this.windowAt(mouseX, mouseY);
        if (window != null && window.kind == WindowKind.CREATIVE && this.creativeDeleteSlotContains(window, mouseX, mouseY) && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, CREATIVE_DELETE_TOOLTIP, mouseX, mouseY);
        }
    }

    private void renderDesktopHotbarAffordances(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot offhandSlot = this.offhandSlot();
        int offhandX = offhandSlotX();
        int hotbarY = hotbarY();
        this.blitSprite(graphics, HOTBAR_OFFHAND_LEFT_SPRITE, offhandX - 3, hotbarY - 4, 29, 24);
        boolean offhandHovered = offhandSlot != null && contains(mouseX, mouseY, offhandX - 1, hotbarY - 1, SLOT_SIZE, SLOT_SIZE);
        if (offhandSlot != null) {
            this.renderHotbarOverlaySlot(graphics, offhandSlot, offhandX, hotbarY, offhandHovered, true);
        }

        for (Slot slot : this.hotbarSlots()) {
            int x = hotbarSlotX(slot.getContainerSlot());
            boolean hovered = contains(mouseX, mouseY, x - 1, hotbarY - 1, SLOT_SIZE, SLOT_SIZE);
            this.renderHotbarOverlaySlot(graphics, slot, x, hotbarY, hovered, false);
        }

        // Offhand hover is handled above so the item always renders over the frame.
    }

    private void renderHotbarOverlaySlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, boolean hovered, boolean renderExistingWhenIdle) {
        DragSlotPreview dragPreview = this.dragSlotPreview(slot);
        if (hovered) {
            graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_HOTBAR_HOVER);
        }
        if (dragPreview != null) {
            graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_DRAG_PREVIEW);
            this.renderItemStack(graphics, dragPreview.stack(), x, y, slot.index, dragPreview.countText());
        } else if ((hovered || renderExistingWhenIdle) && slot.hasItem()) {
            this.renderItemStack(graphics, slot.getItem(), x, y, slot.index);
        }
    }

    private void renderDebugOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!DesktopDebug.enabled()) {
            return;
        }

        InventoryWindow focusedWindow = this.focusedWindow();
        SlotHit hoveredSlot = this.slotAt(mouseX, mouseY);
        InventoryWindow hoveredWindow = this.windowAt(mouseX, mouseY);
        int x = 6;
        int y = 6;
        graphics.fill(x - 3, y - 3, x + 230, y + 64, 0xAA080B10);
        graphics.text(this.font, "Desktop " + this.desktopId + " active=" + (this.minecraft != null && this.minecraft.gui.screen() == this), x, y, 0xFFFFD166, false);
        graphics.text(this.font, "windows=" + this.windows.size() + " sessions=" + this.sessions.size() + " hotbarOnly=" + this.hotbarOnly, x, y + 10, COLOR_TEXT, false);
        graphics.text(this.font, "focused=" + (focusedWindow == null ? "none" : focusedWindow.debugName()), x, y + 20, COLOR_TEXT, false);
        graphics.text(this.font, "hoverWindow=" + (hoveredWindow == null ? "none" : hoveredWindow.debugName()), x, y + 30, COLOR_TEXT, false);
        graphics.text(this.font, "hoverSlot=" + (hoveredSlot == null ? "none" : hoveredSlot.sessionId() + ":" + hoveredSlot.slotId()), x, y + 40, COLOR_TEXT, false);
        graphics.text(this.font, "carried=" + this.sharedCarried, x, y + 50, COLOR_TEXT, false);
    }

    private @Nullable InventoryWindow focusedWindow() {
        for (InventoryWindow window : this.windows) {
            if (window.focused) {
                return window;
            }
        }
        return null;
    }

    private @Nullable Slot hotbarSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.hotbarSlots()) {
            int x = hotbarSlotX(slot.getContainerSlot());
            int y = hotbarY();
            if (contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)) {
                return slot;
            }
        }

        return null;
    }

    private @Nullable Slot offhandSlotAt(double mouseX, double mouseY) {
        Slot slot = this.offhandSlot();
        if (slot == null) {
            return null;
        }

        return contains(mouseX, mouseY, offhandSlotX() - 1, hotbarY() - 1, SLOT_SIZE, SLOT_SIZE) ? slot : null;
    }

    private @Nullable Slot offhandSlot() {
        return offhandSlot(this.playerMenu());
    }

    private static void renderNineSlice(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        int edge = Math.min(WINDOW_EDGE_SIZE, Math.min(width, height) / 2);
        if (edge <= 0) {
            return;
        }

        int centerWidth = Math.max(0, width - edge * 2);
        int centerHeight = Math.max(0, height - edge * 2);
        blitRegion(graphics, texture, x, y, 0, 0, edge, edge, edge, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        blitRegion(graphics, texture, x + edge + centerWidth, y, 6, 0, edge, edge, edge, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        blitRegion(graphics, texture, x, y + edge + centerHeight, 0, 6, edge, edge, edge, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        blitRegion(graphics, texture, x + edge + centerWidth, y + edge + centerHeight, 6, 6, edge, edge, edge, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);

        if (centerWidth > 0) {
            blitRegion(graphics, texture, x + edge, y, 5, 0, centerWidth, edge, 1, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
            blitRegion(graphics, texture, x + edge, y + edge + centerHeight, 5, 6, centerWidth, edge, 1, edge, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        }
        if (centerHeight > 0) {
            blitRegion(graphics, texture, x, y + edge, 0, 5, edge, centerHeight, edge, 1, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
            blitRegion(graphics, texture, x + edge + centerWidth, y + edge, 6, 5, edge, centerHeight, edge, 1, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        }
        if (centerWidth > 0 && centerHeight > 0) {
            blitRegion(graphics, texture, x + edge, y + edge, 5, 5, centerWidth, centerHeight, 1, 1, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        }
    }

    private static void renderOnePixelNineSlice(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int edge = Math.min(1, Math.min(width, height) / 2);
        if (edge <= 0) {
            blitRegion(graphics, texture, x, y, 1, 1, width, height, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
            return;
        }

        int centerWidth = Math.max(0, width - 2);
        int centerHeight = Math.max(0, height - 2);
        blitRegion(graphics, texture, x, y, 0, 0, 1, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        blitRegion(graphics, texture, x + width - 1, y, 2, 0, 1, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        blitRegion(graphics, texture, x, y + height - 1, 0, 2, 1, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        blitRegion(graphics, texture, x + width - 1, y + height - 1, 2, 2, 1, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);

        if (centerWidth > 0) {
            blitRegion(graphics, texture, x + 1, y, 1, 0, centerWidth, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
            blitRegion(graphics, texture, x + 1, y + height - 1, 1, 2, centerWidth, 1, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        }
        if (centerHeight > 0) {
            blitRegion(graphics, texture, x, y + 1, 0, 1, 1, centerHeight, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
            blitRegion(graphics, texture, x + width - 1, y + 1, 2, 1, 1, centerHeight, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        }
        if (centerWidth > 0 && centerHeight > 0) {
            blitRegion(graphics, texture, x + 1, y + 1, 1, 1, centerWidth, centerHeight, 1, 1, MODEL_DISPLAY_TEXTURE_SIZE, MODEL_DISPLAY_TEXTURE_SIZE);
        }
    }

    private static void renderSlotBackground(GuiGraphicsExtractor graphics, int x, int y) {
        blitRegion(
            graphics,
            SLOT_TEXTURE,
            x - 1,
            y - 1,
            0,
            0,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_TEXTURE_WIDTH,
            SLOT_TEXTURE_HEIGHT
        );
    }

    private static void renderSlotHighlightBack(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SLOT_HIGHLIGHT_BACK_SPRITE,
            x - SLOT_HIGHLIGHT_OFFSET,
            y - SLOT_HIGHLIGHT_OFFSET,
            SLOT_HIGHLIGHT_SIZE,
            SLOT_HIGHLIGHT_SIZE,
            currentGuiAlpha()
        );
    }

    private static void renderSlotHighlightFront(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SLOT_HIGHLIGHT_FRONT_SPRITE,
            x - SLOT_HIGHLIGHT_OFFSET,
            y - SLOT_HIGHLIGHT_OFFSET,
            SLOT_HIGHLIGHT_SIZE,
            SLOT_HIGHLIGHT_SIZE,
            currentGuiAlpha()
        );
    }

    private static void blitRegion(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        int x,
        int y,
        int sourceX,
        int sourceY,
        int width,
        int height,
        int sourceWidth,
        int sourceHeight,
        int textureWidth,
        int textureHeight
    ) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            sourceX,
            sourceY,
            width,
            height,
            sourceWidth,
            sourceHeight,
            textureWidth,
            textureHeight,
            currentGuiTint
        );
    }

    private static void renderCarried(GuiGraphicsExtractor graphics, ItemStack carried, int mouseX, int mouseY, Minecraft minecraft, @Nullable String countText) {
        if (!carried.isEmpty()) {
            graphics.nextStratum();
            graphics.item(carried, mouseX - 8, mouseY - 8);
            if (countText == null) {
                graphics.itemDecorations(minecraft.font, carried, mouseX - 8, mouseY - 8);
            } else {
                graphics.itemDecorations(minecraft.font, carried, mouseX - 8, mouseY - 8, countText);
            }
        }
    }

    private static @Nullable Slot offhandSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.getContainerSlot() == OFFHAND_CONTAINER_SLOT) {
                return slot;
            }
        }

        if (OFFHAND_MENU_SLOT_FALLBACK < menu.slots.size()) {
            return menu.slots.get(OFFHAND_MENU_SLOT_FALLBACK);
        }

        return null;
    }

    private AbstractContainerMenu playerMenu() {
        LocalPlayer player = this.player();
        if (player == null) {
            throw new IllegalStateException("Inventory desktop has no active local player");
        }
        return player.inventoryMenu;
    }

    private @Nullable LocalPlayer player() {
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player;
        }
        return this.owner;
    }

    private void syncSharedCarriedToMenus() {
        LocalPlayer player = this.player();
        if (player != null) {
            player.inventoryMenu.setCarried(this.sharedCarried.copy());
        }

        for (DesktopContainerSession session : this.sessions) {
            session.setCarried(this.sharedCarried);
        }

        for (InventoryWindow window : this.windows) {
            if (window.legacyMenu != null) {
                window.legacyMenu.setCarried(this.sharedCarried.copy());
            }
        }
    }

    private static int hotbarSlotX(int hotbarIndex) {
        int screenCenter = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        return screenCenter - 90 + hotbarIndex * 20 + 2;
    }

    private static int offhandSlotX() {
        return hotbarSlotX(0) - SLOT_SIZE - OFFHAND_HOTBAR_GAP;
    }

    private static int hotbarY() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight() - 19;
    }

    private static int creativeContentX(InventoryWindow window) {
        return window.x + CREATIVE_CONTENT_MARGIN;
    }

    private static int creativeTopTabsY(InventoryWindow window) {
        return window.y - CREATIVE_TAB_HEIGHT + CREATIVE_TAB_FRAME_OVERLAP + CREATIVE_TOP_TAB_Y_OFFSET;
    }

    private static int creativePanelX(InventoryWindow window) {
        return creativeContentX(window);
    }

    private static int creativePanelY(InventoryWindow window) {
        return window.y + TOP_BAR_HEIGHT + CREATIVE_CONTENT_MARGIN;
    }

    private static int creativeBottomTabsY(InventoryWindow window) {
        return window.y + window.height - CREATIVE_TAB_FRAME_OVERLAP + CREATIVE_BOTTOM_TAB_Y_OFFSET;
    }

    private static Identifier[] creativeTabSprites(String baseName) {
        Identifier[] sprites = new Identifier[CREATIVE_TABS_PER_ROW];
        for (int i = 0; i < sprites.length; i++) {
            sprites[i] = Identifier.withDefaultNamespace("container/creative_inventory/" + baseName + "_" + (i + 1));
        }
        return sprites;
    }

    private static void slotClicked(AbstractContainerMenu menu, Slot slot, int button, ContainerInput input, Minecraft minecraft) {
        slotClicked(menu, menu.slots.indexOf(slot), button, input, minecraft);
    }

    private static void slotClicked(AbstractContainerMenu menu, int slotId, int button, ContainerInput input, Minecraft minecraft) {
        if (minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleContainerInput(menu.containerId, slotId, button, input, minecraft.player);
        }
    }

    private static List<Slot> findContainerSlots(AbstractContainerMenu menu, Inventory playerInventory) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container != playerInventory) {
                slots.add(slot);
            }
        }

        return List.copyOf(slots);
    }

    private static int minSlotX(List<Slot> slots) {
        int min = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            min = Math.min(min, slot.x);
        }

        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int minSlotY(List<Slot> slots) {
        int min = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            min = Math.min(min, slot.y);
        }

        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int containerContentWidth(List<Slot> slots, int minSlotX) {
        int max = 0;
        for (Slot slot : slots) {
            max = Math.max(max, slot.x - minSlotX + SLOT_SIZE);
        }

        return max;
    }

    private static int containerContentHeight(List<Slot> slots, int minSlotY) {
        int max = 0;
        for (Slot slot : slots) {
            max = Math.max(max, slot.y - minSlotY + SLOT_SIZE);
        }

        return max;
    }

    private static @Nullable Slot furnaceSlot(List<Slot> slots, int containerSlot) {
        return containerSlot(slots, containerSlot);
    }

    private static @Nullable Slot containerSlot(List<Slot> slots, int containerSlot) {
        for (Slot slot : slots) {
            if (slot.getContainerSlot() == containerSlot) {
                return slot;
            }
        }

        return containerSlot >= 0 && containerSlot < slots.size() ? slots.get(containerSlot) : null;
    }

    private static @Nullable Slot crafterOutputSlot(List<Slot> slots) {
        return slots.size() > CRAFTER_INPUT_SLOT_COUNT ? slots.get(CRAFTER_INPUT_SLOT_COUNT) : null;
    }

    private static int crafterSlotStateButtonId(int slotId, boolean enabled) {
        return slotId | (enabled ? CRAFTER_SLOT_STATE_ENABLED_FLAG : 0);
    }

    private static float clampProgress(float progress) {
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    private static String compactCount(long count) {
        if (count <= 1) {
            return "";
        }
        if (count < 1000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return count / 1000 + "k";
        }
        if (count < 1_000_000_000) {
            return count / 1_000_000 + "m";
        }
        return count / 1_000_000_000 + "b";
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum WindowKind {
        INVENTORY,
        CONTAINER,
        CHARACTER,
        CREATIVE
    }

    private enum WindowPlacement {
        CENTER,
        BOTTOM_LEFT,
        CONTAINER
    }

    private enum WindowControl {
        FOCUS("F"),
        PIN("P"),
        MINIMIZE("_"),
        CLOSE("x"),
        ELLIPSIS("..."),
        LOCK("L");

        private final String label;

        WindowControl(String label) {
            this.label = label;
        }
    }

    private enum BeaconButtonKind {
        PRIMARY,
        SECONDARY,
        UPGRADE,
        CONFIRM,
        CANCEL
    }

    private record SlotHit(Slot slot, int slotId, int x, int y, AbstractContainerMenu menu, int sessionId) {
    }

    private record RecipeBookPosition(int x, int y) {
    }

    private record RecipeBookButtonRect(int x, int y) {
    }

    private record PendingSlotClick(SlotHit hit, int button, int quickCraftType) {
    }

    private record DragSlotPreview(ItemStack stack, @Nullable String countText) {
    }

    private record DragCarriedPreview(ItemStack stack, @Nullable String countText) {
    }

    private record SlotKey(int sessionId, int slotId) {
        private static SlotKey of(SlotHit hit) {
            return new SlotKey(hit.sessionId(), hit.slotId());
        }
    }

    private static final class DragDistribution {
        private final int button;
        private final int quickCraftType;
        private final LinkedHashMap<SlotKey, SlotHit> slots = new LinkedHashMap<>();

        private DragDistribution(int button, int quickCraftType) {
            this.button = button;
            this.quickCraftType = quickCraftType;
        }

        private int button() {
            return this.button;
        }

        private int quickCraftType() {
            return this.quickCraftType;
        }

        private void add(SlotHit hit) {
            this.slots.putIfAbsent(SlotKey.of(hit), hit);
        }

        private boolean contains(SlotHit hit) {
            return this.slots.containsKey(SlotKey.of(hit));
        }

        private @Nullable SlotHit hitFor(Slot slot) {
            for (SlotHit hit : this.slots.values()) {
                if (hit.slot() == slot) {
                    return hit;
                }
            }

            return null;
        }

        private int size() {
            return this.slots.size();
        }

        private List<SlotHit> slots() {
            return List.copyOf(this.slots.values());
        }
    }

    private record BeaconButtonHit(BeaconButtonKind kind, @Nullable Holder<MobEffect> effect) {
    }

    private record MerchantOfferItemHit(ItemStack stack) {
    }

    private record CreativeItemHit(InventoryWindow window, ItemStack stack, int x, int y, int index) {
    }

    private record CreativeTabHit(InventoryWindow window, CreativeModeTab tab, CreativeTabRect rect) {
    }

    private record CreativeTabRect(int x, int y, int width, int height, int column, boolean topRow) {
    }

    private record CreativeTabPosition(int column, boolean topRow) {
    }

    private record CreativeTabPageButtonHit(InventoryWindow window, int direction) {
    }

    private record CreativeTabPageButtonRect(int x, int y, int width, int height, int direction) {
    }

    private record CreativeSearchRect(int x, int y, int width, int height) {
    }

    private record CreativeGridLayout(int totalRows, int maxScrollRow, boolean scrollable) {
    }

    private record InventoryIncreaseButtonRect(int x, int y) {
    }

    private record CartographyPreview(
        @Nullable MapId mapId,
        @Nullable MapItemSavedData mapData,
        boolean emptyMap,
        boolean paper,
        boolean glassPane,
        boolean error
    ) {
    }

    private record SlotGridLayout(int columns, int visibleRows, int totalRows, int maxScrollRow, boolean scrollable) {
    }

    private record StorageGridSize(int columns, int rows) {
    }

    private record WindowPosition(int x, int y) {
    }

    private record WindowBounds(int x, int y, int width, int height) {
        private static WindowBounds fromEdges(int x, int y, int right, int bottom) {
            return new WindowBounds(x, y, Math.max(0, right - x), Math.max(0, bottom - y));
        }

        private int right() {
            return this.x + this.width;
        }

        private int bottom() {
            return this.y + this.height;
        }

        private boolean intersectsWithGap(WindowBounds other, int gap) {
            return this.x < other.right() + gap
                && this.right() + gap > other.x
                && this.y < other.bottom() + gap
                && this.bottom() + gap > other.y;
        }
    }

    private record TitleBarLayout(String displayTitle, List<ControlRect> controls, boolean compact) {
        private @Nullable WindowControl controlAt(double mouseX, double mouseY) {
            ControlRect rect = this.controlRectAt(mouseX, mouseY);
            return rect == null ? null : rect.control();
        }

        private @Nullable ControlRect controlRect(WindowControl control) {
            for (ControlRect rect : this.controls) {
                if (rect.control() == control) {
                    return rect;
                }
            }

            return null;
        }

        private @Nullable ControlRect controlRectAt(double mouseX, double mouseY) {
            for (ControlRect rect : this.controls) {
                if (rect.contains(mouseX, mouseY)) {
                    return rect;
                }
            }

            return null;
        }
    }

    private record ControlRect(WindowControl control, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, CONTROL_SIZE, CONTROL_SIZE);
        }
    }

    private record PopupRect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }
    }

    private record ControlHit(InventoryWindow window, WindowControl control) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ApiContext apiContext(InventoryWindow window, @Nullable GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        return new ApiContext(window, graphics, mouseX, mouseY);
    }

    private final class ApiContext<T extends AbstractContainerMenu, S>
        implements DesktopRenderContext<T, S>, DesktopSlotContext<T, S>, DesktopInputContext<T, S> {
        private final InventoryWindow window;
        private final @Nullable GuiGraphicsExtractor graphics;
        private final int mouseX;
        private final int mouseY;

        private ApiContext(InventoryWindow window, @Nullable GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            this.window = window;
            this.graphics = graphics;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        private GuiGraphicsExtractor graphics() {
            if (this.graphics == null) {
                throw new IllegalStateException("This desktop API context is not in a render pass");
            }
            return this.graphics;
        }

        @Override
        public Minecraft minecraft() {
            return InventoryDesktopScreen.this.minecraft;
        }

        @Override
        public T menu() {
            return (T) this.window.containerMenu();
        }

        @Override
        public Component originalTitle() {
            return this.window.title;
        }

        @Override
        public int sessionId() {
            return this.window.sessionId();
        }

        @Override
        public String sourceKey() {
            return this.window.session == null ? "" : this.window.session.sourceKey();
        }

        @Override
        public S state() {
            return (S) this.window.apiState;
        }

        @Override
        public List<Slot> containerSlots() {
            return this.window.containerSlots();
        }

        @Override
        public int windowX() {
            return this.window.x;
        }

        @Override
        public int windowY() {
            return this.window.y;
        }

        @Override
        public int windowWidth() {
            return this.window.width;
        }

        @Override
        public int windowHeight() {
            return this.window.height;
        }

        @Override
        public int contentX() {
            return this.window.contentX();
        }

        @Override
        public int contentY() {
            return this.window.contentY();
        }

        @Override
        public int contentWidth() {
            return Math.max(0, this.window.width - WINDOW_CONTENT_PADDING * 2);
        }

        @Override
        public int contentHeight() {
            return Math.max(0, this.window.height - TOP_BAR_HEIGHT - WINDOW_CONTENT_PADDING * 2);
        }

        @Override
        public boolean focused() {
            return this.window.focused;
        }

        @Override
        public boolean minimized() {
            return this.window.minimized;
        }

        @Override
        public boolean ghosted() {
            return this.window.ghosted;
        }

        @Override
        public ItemStack carriedStack() {
            return InventoryDesktopScreen.this.sharedCarried.copy();
        }

        @Override
        public boolean recipeBookVisible() {
            return this.window.recipeBook != null && this.window.recipeBook.isVisible();
        }

        @Override
        public boolean refreshRecipeBook() {
            RecipeBookComponent<?> recipeBook = this.window.recipeBook;
            if (recipeBook == null) {
                return false;
            }

            recipeBook.recipesUpdated();
            return true;
        }

        @Override
        public @Nullable Slot menuSlot(int menuSlotId) {
            AbstractContainerMenu menu = this.window.containerMenu();
            return menu != null && menuSlotId >= 0 && menuSlotId < menu.slots.size() ? menu.slots.get(menuSlotId) : null;
        }

        @Override
        public @Nullable Slot containerSlot(int containerSlotIndex) {
            List<Slot> slots = this.window.containerSlots();
            return containerSlotIndex >= 0 && containerSlotIndex < slots.size() ? slots.get(containerSlotIndex) : null;
        }

        @Override
        public int menuSlotId(Slot slot) {
            AbstractContainerMenu menu = this.window.containerMenu();
            return menu == null ? -1 : menu.slots.indexOf(slot);
        }

        @Override
        public int fontWidth(String text) {
            return InventoryDesktopScreen.this.font.width(text);
        }

        @Override
        public int fontWidth(Component text) {
            return InventoryDesktopScreen.this.font.width(text);
        }

        @Override
        public String trimToWidth(String text, int maxWidth) {
            if (InventoryDesktopScreen.this.font.width(text) <= maxWidth) {
                return text;
            }
            return InventoryDesktopScreen.this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
        }

        @Override
        public int mouseX() {
            return this.mouseX;
        }

        @Override
        public int mouseY() {
            return this.mouseY;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            this.graphics().fill(x1, y1, x2, y2, InventoryDesktopScreen.this.uiColor(color));
        }

        @Override
        public void text(String text, int x, int y, int color, boolean shadow) {
            this.graphics().text(InventoryDesktopScreen.this.font, text, x, y, InventoryDesktopScreen.this.uiColor(color), shadow);
        }

        @Override
        public void scaledText(String text, int x, int y, int color, boolean shadow, float scale) {
            GuiGraphicsExtractor graphics = this.graphics();
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.text(InventoryDesktopScreen.this.font, text, x, y, InventoryDesktopScreen.this.uiColor(color), shadow);
            graphics.pose().popMatrix();
        }

        @Override
        public void text(Component text, int x, int y, int color, boolean shadow) {
            this.graphics().text(InventoryDesktopScreen.this.font, text, x, y, InventoryDesktopScreen.this.uiColor(color), shadow);
        }

        @Override
        public void sprite(Identifier sprite, int x, int y, int width, int height) {
            InventoryDesktopScreen.this.blitSprite(this.graphics(), sprite, x, y, width, height);
        }

        @Override
        public void sprite(Identifier sprite, int sourceWidth, int sourceHeight, int sourceX, int sourceY, int x, int y, int width, int height) {
            InventoryDesktopScreen.this.blitSprite(this.graphics(), sprite, sourceWidth, sourceHeight, sourceX, sourceY, x, y, width, height);
        }

        @Override
        public void texture(Identifier texture, int x, int y, int sourceX, int sourceY, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
            blitRegion(this.graphics(), texture, x, y, sourceX, sourceY, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight);
        }

        @Override
        public void windowNineSlice(Identifier texture, int x, int y, int width, int height) {
            renderNineSlice(this.graphics(), texture, x, y, width, height);
        }

        @Override
        public void onePixelNineSlice(Identifier texture, int x, int y, int width, int height) {
            renderOnePixelNineSlice(this.graphics(), texture, x, y, width, height);
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
            InventoryDesktopScreen.this.renderItemStack(this.graphics(), stack, x, y);
        }

        @Override
        public void item(ItemStack stack, int x, int y, int seed) {
            InventoryDesktopScreen.this.renderItemStack(this.graphics(), stack, x, y, seed);
        }

        @Override
        public void virtualItem(ItemStack stack, long count, int x, int y) {
            slotBackground(x, y);
            boolean hovered = contains(this.mouseX, this.mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
            if (hovered) {
                renderSlotHighlightBack(this.graphics(), x, y);
            }
            if (!stack.isEmpty()) {
                InventoryDesktopScreen.this.renderItemStack(this.graphics(), stack.copyWithCount(1), x, y);
                String countText = compactCount(count);
                if (!countText.isEmpty()) {
                    float scale = 0.5F;
                    float inverse = 1.0F / scale;
                    int textX = (int) (((float) x + 16.0F - InventoryDesktopScreen.this.font.width(countText) * scale) * inverse);
                    int textY = (int) (((float) y + 13.0F) * inverse);
                    scaledText(countText, textX, textY, count == 0 ? 0xFFFFFF00 : 0xFFFFFFFF, true, scale);
                }
            }
            if (hovered) {
                renderSlotHighlightFront(this.graphics(), x, y);
            }
        }

        @Override
        public void slot(int menuSlotId, int x, int y) {
            Slot slot = this.menuSlot(menuSlotId);
            if (slot != null) {
                InventoryDesktopScreen.this.renderSlot(this.graphics(), slot, x, y, this.mouseX, this.mouseY);
            }
        }

        @Override
        public void slot(Slot slot, int x, int y) {
            InventoryDesktopScreen.this.renderSlot(this.graphics(), slot, x, y, this.mouseX, this.mouseY);
        }

        @Override
        public void texturelessSlot(int menuSlotId, int x, int y) {
            Slot slot = this.menuSlot(menuSlotId);
            if (slot == null) {
                return;
            }

            boolean hovered = contains(this.mouseX, this.mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
            DragSlotPreview dragPreview = InventoryDesktopScreen.this.dragSlotPreview(slot);
            if (hovered) {
                DesktopDebug.detail(
                    "client api textureless slot render desktop={} window={} menu={} menuSlot={} containerSlot={} x={} y={} stack={} dragPreview={}",
                    InventoryDesktopScreen.this.desktopId,
                    this.window.debugName(),
                    safeMenuKey(this.window.containerMenu()),
                    menuSlotId,
                    slot.getContainerSlot(),
                    x,
                    y,
                    slot.getItem(),
                    dragPreview == null ? "none" : dragPreview.stack()
                );
            }
            if (hovered && slot.isHighlightable()) {
                renderSlotHighlightBack(this.graphics(), x, y);
            }
            if (dragPreview != null) {
                this.graphics().fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_DRAG_PREVIEW);
                InventoryDesktopScreen.this.renderItemStack(this.graphics(), dragPreview.stack(), x, y, slot.index, dragPreview.countText());
            } else if (slot.hasItem()) {
                InventoryDesktopScreen.this.renderItemStack(this.graphics(), slot.getItem(), x, y, slot.index);
            } else if (slot.getNoItemIcon() != null) {
                InventoryDesktopScreen.this.blitSprite(this.graphics(), slot.getNoItemIcon(), x, y, SLOT_ITEM_SIZE, SLOT_ITEM_SIZE);
            }
            if (hovered && slot.isHighlightable()) {
                renderSlotHighlightFront(this.graphics(), x, y);
            }
        }

        @Override
        public void slotBackground(int x, int y) {
            renderSlotBackground(this.graphics(), x, y);
        }

        @Override
        public void slotHighlight(int x, int y) {
            renderSlotHighlightBack(this.graphics(), x, y);
            renderSlotHighlightFront(this.graphics(), x, y);
        }

        @Override
        public void entityPreview(LivingEntity entity, int x0, int y0, int x1, int y1, int scale, float mouseScale) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(this.graphics(), x0, y0, x1, y1, scale, mouseScale, this.mouseX, this.mouseY, entity);
        }

        @Override
        public void tooltip(ItemStack stack, int mouseX, int mouseY) {
            this.graphics().setTooltipForNextFrame(InventoryDesktopScreen.this.font, stack, mouseX, mouseY);
        }

        @Override
        public void tooltip(Component text, int mouseX, int mouseY) {
            this.graphics().setTooltipForNextFrame(InventoryDesktopScreen.this.font, text, mouseX, mouseY);
        }

        @Override
        public void tooltip(List<Component> lines, int mouseX, int mouseY) {
            this.graphics().setComponentTooltipForNextFrame(InventoryDesktopScreen.this.font, lines, mouseX, mouseY);
        }

        @Override
        public boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
            return InventoryDesktopScreen.contains(mouseX, mouseY, x, y, width, height);
        }

        @Override
        public @Nullable DesktopSlotHit menuSlotHit(int menuSlotId, int x, int y, double mouseX, double mouseY) {
            return this.menuSlot(menuSlotId) != null && InventoryDesktopScreen.contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)
                ? DesktopSlotHit.of(menuSlotId, x, y)
                : null;
        }

        @Override
        public @Nullable DesktopSlotHit containerSlotHit(int containerSlotIndex, int x, int y, double mouseX, double mouseY) {
            Slot slot = this.containerSlot(containerSlotIndex);
            int slotId = slot == null ? -1 : this.menuSlotId(slot);
            return slotId >= 0 && InventoryDesktopScreen.contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)
                ? DesktopSlotHit.of(slotId, x, y)
                : null;
        }

        @Override
        public boolean shiftDown() {
            return InventoryDesktopScreen.this.isShiftHeld();
        }

        @Override
        public boolean ctrlDown() {
            return InventoryDesktopScreen.this.isControlHeld();
        }

        @Override
        public boolean altDown() {
            return InventoryDesktopScreen.this.minecraft != null && WindowedInventoryClient.isAltDown(InventoryDesktopScreen.this.minecraft);
        }

        @Override
        public boolean mouseButtonDown(int button) {
            return InventoryDesktopScreen.this.minecraft != null
                && GLFW.glfwGetMouseButton(InventoryDesktopScreen.this.minecraft.getWindow().handle(), button) == GLFW.GLFW_PRESS;
        }

        @Override
        public boolean sendMenuButton(int buttonId) {
            return DesktopContainerClient.clickButton(this.sessionId(), buttonId);
        }

        @Override
        public boolean sendRename(String name) {
            return DesktopContainerClient.renameAnvil(this.sessionId(), name);
        }

        @Override
        public boolean clickSlot(int menuSlotId, int button, ContainerInput input) {
            Slot slot = this.menuSlot(menuSlotId);
            AbstractContainerMenu menu = this.window.containerMenu();
            if (slot == null || menu == null) {
                return false;
            }

            InventoryDesktopScreen.this.slotClicked(new SlotHit(slot, menuSlotId, slot.x, slot.y, menu, this.sessionId()), button, input);
            return true;
        }

        @Override
        public boolean quickMoveSlot(int menuSlotId) {
            Slot slot = this.menuSlot(menuSlotId);
            AbstractContainerMenu menu = this.window.containerMenu();
            if (slot == null || menu == null) {
                return false;
            }

            InventoryDesktopScreen.this.quickMoveSlot(new SlotHit(slot, menuSlotId, slot.x, slot.y, menu, this.sessionId()));
            return true;
        }

        @Override
        public boolean toggleRecipeBook() {
            return InventoryDesktopScreen.this.toggleRecipeBook(this.window);
        }

        @Override
        public boolean setRecipeBookSearch(String search) {
            RecipeBookComponent<?> recipeBook = this.window.recipeBook;
            if (recipeBook == null) {
                return false;
            }

            EditBox searchBox = ((RecipeBookComponentAccessor) recipeBook).salts_inventory_update$getSearchBox();
            if (searchBox != null) {
                searchBox.setValue(search);
            }
            recipeBook.recipesUpdated();
            return true;
        }

        @Override
        public boolean sendPayload(Identifier channel, byte[] data) {
            return DesktopContainerClient.sendCustomPayload(this.sessionId(), channel, data);
        }

        @Override
        public <P> boolean sendPayload(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec) {
            LocalPlayer player = InventoryDesktopScreen.this.player();
            return player != null && this.sendPayload(channel, DesktopPayloadCodecs.encode(player.registryAccess(), codec, payload));
        }
    }

    private static final class FurnaceDesktopWindowDefinition implements DesktopWindowDefinition<AbstractFurnaceMenu, Void> {
        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractFurnaceMenu> context) {
            return DesktopWindowSize.of(context.defaultWindowWidth(), context.defaultWindowHeight());
        }

        @Override
        public void render(DesktopRenderContext<AbstractFurnaceMenu, Void> context) {
            AbstractFurnaceMenu menu = context.menu();
            int contentX = context.windowX() + FURNACE_CONTENT_MARGIN;
            int contentY = context.windowY() + TOP_BAR_HEIGHT + FURNACE_CONTENT_MARGIN;
            this.renderSlot(context, FURNACE_INPUT_SLOT, contentX + FURNACE_INPUT_X, contentY + FURNACE_INPUT_Y);
            this.renderSlot(context, FURNACE_FUEL_SLOT, contentX + FURNACE_FUEL_X, contentY + FURNACE_FUEL_Y);
            this.renderSlot(context, FURNACE_RESULT_SLOT, contentX + FURNACE_RESULT_X, contentY + FURNACE_RESULT_Y);

            int flameX = contentX + FURNACE_FLAME_X;
            int flameY = contentY + FURNACE_FLAME_Y;
            context.texture(
                CONTAINER_WIDGETS_TEXTURE,
                flameX,
                flameY,
                WIDGET_FLAME_EMPTY_X,
                WIDGET_FLAME_EMPTY_Y,
                WIDGET_FLAME_WIDTH,
                WIDGET_FLAME_HEIGHT,
                WIDGET_FLAME_WIDTH,
                WIDGET_FLAME_HEIGHT,
                CONTAINER_WIDGETS_TEXTURE_WIDTH,
                CONTAINER_WIDGETS_TEXTURE_HEIGHT
            );

            int flameFill = menu.isLit() ? Math.round(clampProgress(menu.getLitProgress()) * WIDGET_FLAME_HEIGHT) : 0;
            if (flameFill > 0) {
                int clippedY = WIDGET_FLAME_HEIGHT - flameFill;
                context.texture(
                    CONTAINER_WIDGETS_TEXTURE,
                    flameX,
                    flameY + clippedY,
                    WIDGET_FLAME_FULL_X,
                    WIDGET_FLAME_FULL_Y + clippedY,
                    WIDGET_FLAME_WIDTH,
                    flameFill,
                    WIDGET_FLAME_WIDTH,
                    flameFill,
                    CONTAINER_WIDGETS_TEXTURE_WIDTH,
                    CONTAINER_WIDGETS_TEXTURE_HEIGHT
                );
            }

            int arrowX = contentX + FURNACE_ARROW_X;
            int arrowY = contentY + FURNACE_ARROW_Y;
            context.texture(
                CONTAINER_WIDGETS_TEXTURE,
                arrowX,
                arrowY,
                WIDGET_ARROW_EMPTY_X,
                WIDGET_ARROW_EMPTY_Y,
                WIDGET_ARROW_WIDTH,
                WIDGET_ARROW_HEIGHT,
                WIDGET_ARROW_WIDTH,
                WIDGET_ARROW_HEIGHT,
                CONTAINER_WIDGETS_TEXTURE_WIDTH,
                CONTAINER_WIDGETS_TEXTURE_HEIGHT
            );

            int arrowFill = Math.round(clampProgress(menu.getBurnProgress()) * WIDGET_ARROW_WIDTH);
            if (arrowFill > 0) {
                context.texture(
                    CONTAINER_WIDGETS_TEXTURE,
                    arrowX,
                    arrowY,
                    WIDGET_ARROW_FULL_X,
                    WIDGET_ARROW_FULL_Y,
                    arrowFill,
                    WIDGET_ARROW_HEIGHT,
                    arrowFill,
                    WIDGET_ARROW_HEIGHT,
                    CONTAINER_WIDGETS_TEXTURE_WIDTH,
                    CONTAINER_WIDGETS_TEXTURE_HEIGHT
                );
            }

            int buttonX = contentX + FURNACE_RECIPE_BUTTON_X;
            int buttonY = contentY + FURNACE_RECIPE_BUTTON_Y;
            boolean hovered = contains(context.mouseX(), context.mouseY(), buttonX, buttonY, RECIPE_BOOK_BUTTON_WIDTH, RECIPE_BOOK_BUTTON_HEIGHT);
            Identifier sprite = RecipeBookComponent.RECIPE_BUTTON_SPRITES.get(true, hovered || context.recipeBookVisible());
            context.sprite(sprite, buttonX, buttonY, RECIPE_BOOK_BUTTON_WIDTH, RECIPE_BOOK_BUTTON_HEIGHT);
        }

        @Override
        public @Nullable RecipeBookComponent<?> createRecipeBook(DesktopWindowContext<AbstractFurnaceMenu, Void> context) {
            return createFurnaceRecipeBook(context.menu());
        }

        @Override
        public @Nullable DesktopSlotHit slotAt(DesktopSlotContext<AbstractFurnaceMenu, Void> context, double mouseX, double mouseY) {
            int contentX = context.windowX() + FURNACE_CONTENT_MARGIN;
            int contentY = context.windowY() + TOP_BAR_HEIGHT + FURNACE_CONTENT_MARGIN;
            DesktopSlotHit input = this.slotHit(context, FURNACE_INPUT_SLOT, contentX + FURNACE_INPUT_X, contentY + FURNACE_INPUT_Y, mouseX, mouseY);
            if (input != null) {
                return input;
            }
            DesktopSlotHit fuel = this.slotHit(context, FURNACE_FUEL_SLOT, contentX + FURNACE_FUEL_X, contentY + FURNACE_FUEL_Y, mouseX, mouseY);
            if (fuel != null) {
                return fuel;
            }
            return this.slotHit(context, FURNACE_RESULT_SLOT, contentX + FURNACE_RESULT_X, contentY + FURNACE_RESULT_Y, mouseX, mouseY);
        }

        private void renderSlot(DesktopRenderContext<AbstractFurnaceMenu, Void> context, int containerSlot, int x, int y) {
            Slot slot = containerSlot(context.containerSlots(), containerSlot);
            if (slot != null) {
                context.slot(slot, x, y);
            }
        }

        private @Nullable DesktopSlotHit slotHit(
            DesktopSlotContext<AbstractFurnaceMenu, Void> context,
            int containerSlot,
            int x,
            int y,
            double mouseX,
            double mouseY
        ) {
            Slot slot = containerSlot(context.containerSlots(), containerSlot);
            int slotId = slot == null ? -1 : context.menuSlotId(slot);
            return slotId >= 0 && context.contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)
                ? DesktopSlotHit.of(slotId, x, y)
                : null;
        }
    }

    private static final class EnchantmentBookState {
        private final RandomSource random = RandomSource.create();
        private ItemStack last = ItemStack.EMPTY;
        private float flip;
        private float oFlip;
        private float flipT;
        private float flipA;
        private float open;
        private float oOpen;

        private void tick(EnchantmentMenu menu) {
            ItemStack itemStack = menu.getSlot(0).getItem();
            if (!ItemStack.matches(itemStack, this.last)) {
                this.last = itemStack.copy();
                do {
                    this.flipT += this.random.nextInt(4) - this.random.nextInt(4);
                } while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
            }

            this.oFlip = this.flip;
            this.oOpen = this.open;
            boolean hasCosts = false;
            for (int cost : menu.costs) {
                if (cost != 0) {
                    hasCosts = true;
                    break;
                }
            }

            this.open += hasCosts ? 0.2F : -0.2F;
            this.open = Mth.clamp(this.open, 0.0F, 1.0F);
            float flipDelta = Mth.clamp((this.flipT - this.flip) * 0.4F, -0.2F, 0.2F);
            this.flipA += (flipDelta - this.flipA) * 0.9F;
            this.flip += this.flipA;
        }

        private float open(float partialTick) {
            return Mth.lerp(partialTick, this.oOpen, this.open);
        }

        private float flip(float partialTick) {
            return Mth.lerp(partialTick, this.oFlip, this.flip);
        }
    }

    private static final class SmithingWindowState {
        private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(SMITHING_TEMPLATE_SLOT);
        private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(SMITHING_BASE_SLOT);
        private final CyclingSlotBackground additionIcon = new CyclingSlotBackground(SMITHING_ADDITION_SLOT);
        private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();
        private ItemStack previewStack = ItemStack.EMPTY;

        private SmithingWindowState() {
            this.armorStandPreview.entityType = EntityTypes.ARMOR_STAND;
            this.armorStandPreview.showBasePlate = false;
            this.armorStandPreview.showArms = true;
            this.armorStandPreview.xRot = 25.0F;
            this.armorStandPreview.bodyRot = 210.0F;
        }

        private void tick(SmithingMenu menu) {
            this.templateIcon.tick(SMITHING_TEMPLATE_SPRITES);
            ItemStack templateStack = menu.getSlot(SMITHING_TEMPLATE_SLOT).getItem();
            if (templateStack.getItem() instanceof SmithingTemplateItem templateItem) {
                this.baseIcon.tick(templateItem.getBaseSlotEmptyIcons());
                this.additionIcon.tick(templateItem.getAdditionalSlotEmptyIcons());
            } else {
                this.baseIcon.tick(List.of());
                this.additionIcon.tick(List.of());
            }
        }

        private void renderSlotIcons(SmithingMenu menu, GuiGraphicsExtractor graphics, float partialTick, int leftPos, int topPos) {
            this.templateIcon.extractRenderState(menu, graphics, partialTick, leftPos, topPos);
            this.baseIcon.extractRenderState(menu, graphics, partialTick, leftPos, topPos);
            this.additionIcon.extractRenderState(menu, graphics, partialTick, leftPos, topPos);
        }

        private ArmorStandRenderState armorStandPreview() {
            return this.armorStandPreview;
        }

        private void updateArmorStandPreview(Minecraft minecraft, ItemStack stack) {
            if (ItemStack.matches(this.previewStack, stack) && this.previewStack.getCount() == stack.getCount()) {
                return;
            }

            this.previewStack = stack.copy();
            this.clearArmorStandPreview();
            if (stack.isEmpty()) {
                return;
            }

            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            EquipmentSlot slot = equippable == null ? null : equippable.slot();
            if (slot == EquipmentSlot.HEAD) {
                if (HumanoidArmorLayer.shouldRender(stack, EquipmentSlot.HEAD)) {
                    this.armorStandPreview.headEquipment = stack.copy();
                } else {
                    minecraft.getItemModelResolver().updateForTopItem(
                        this.armorStandPreview.headItem,
                        stack,
                        ItemDisplayContext.HEAD,
                        null,
                        null,
                        0
                    );
                }
            } else if (slot == EquipmentSlot.CHEST) {
                this.armorStandPreview.chestEquipment = stack.copy();
            } else if (slot == EquipmentSlot.LEGS) {
                this.armorStandPreview.legsEquipment = stack.copy();
            } else if (slot == EquipmentSlot.FEET) {
                this.armorStandPreview.feetEquipment = stack.copy();
            } else {
                this.armorStandPreview.leftHandItemStack = stack.copy();
                minecraft.getItemModelResolver().updateForTopItem(
                    this.armorStandPreview.leftHandItemState,
                    stack,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    null,
                    null,
                    0
                );
            }
        }

        private void clearArmorStandPreview() {
            this.armorStandPreview.leftHandItemStack = ItemStack.EMPTY;
            this.armorStandPreview.leftHandItemState.clear();
            this.armorStandPreview.rightHandItemStack = ItemStack.EMPTY;
            this.armorStandPreview.rightHandItemState.clear();
            this.armorStandPreview.headEquipment = ItemStack.EMPTY;
            this.armorStandPreview.headItem.clear();
            this.armorStandPreview.chestEquipment = ItemStack.EMPTY;
            this.armorStandPreview.legsEquipment = ItemStack.EMPTY;
            this.armorStandPreview.feetEquipment = ItemStack.EMPTY;
        }
    }

    private static final class InventoryWindow {
        private final WindowKind kind;
        private final Component title;
        private int x;
        private int y;
        private int width;
        private int height;
        private final @Nullable DesktopContainerSession session;
        private final @Nullable AbstractContainerMenu legacyMenu;
        private final List<Slot> legacyContainerSlots;
        private final int legacyMinSlotX;
        private final int legacyMinSlotY;
        private boolean minimized;
        private boolean focused;
        private boolean locked = !SaltsInventoryConfig.get().openUnlocked;
        private PinMode pinMode = PinMode.UNPINNED;
        private boolean ghosted;
        private int scrollRow;
        private int merchantScroll;
        private int merchantSelectedTrade;
        private int stonecutterScroll;
        private int loomScroll;
        private @Nullable CreativeModeTab creativeSelectedTab;
        private int creativeTabPage;
        private int creativeScrollRow;
        private String creativeSearch = "";
        private String anvilName = "";
        private String anvilLastInputName = "";
        private boolean anvilNameDirty;
        private @Nullable Holder<MobEffect> beaconPrimary;
        private @Nullable Holder<MobEffect> beaconSecondary;
        private boolean beaconSelectionDirty;
        private @Nullable EnchantmentBookState enchantmentBookState;
        private @Nullable SmithingWindowState smithingState;
        private @Nullable RecipeBookComponent<?> recipeBook;
        private int recipeBookX;
        private int recipeBookY;
        private int recipeBookSyntheticWidth = Integer.MIN_VALUE;
        private int recipeBookSyntheticHeight = Integer.MIN_VALUE;
        private @Nullable DesktopWindowDefinition<?, ?> apiDefinition;
        private @Nullable Object apiState;

        private InventoryWindow(WindowKind kind, Component title, int x, int y, int width, int height) {
            this(kind, title, x, y, width, height, null, null, List.of(), 0, 0);
        }

        private InventoryWindow(WindowKind kind, Component title, int x, int y, int width, int height, @Nullable DesktopContainerSession session) {
            this(kind, title, x, y, width, height, session, null, List.of(), 0, 0);
        }

        private InventoryWindow(
            WindowKind kind,
            Component title,
            int x,
            int y,
            int width,
            int height,
            @Nullable DesktopContainerSession session,
            @Nullable AbstractContainerMenu legacyMenu,
            List<Slot> legacyContainerSlots,
            int legacyMinSlotX,
            int legacyMinSlotY
        ) {
            this.kind = kind;
            this.title = title;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.session = session;
            this.legacyMenu = legacyMenu;
            this.legacyContainerSlots = List.copyOf(legacyContainerSlots);
            this.legacyMinSlotX = legacyMinSlotX;
            this.legacyMinSlotY = legacyMinSlotY;
        }

        private String debugName() {
            String backing = this.session == null ? this.legacyMenu == null ? "local" : "legacy:" + this.legacyMenu.containerId : "session:" + this.session.sessionId();
            return this.kind + "/" + backing + "/" + this.title.getString();
        }

        private @Nullable String stateKey() {
            if (this.session != null && !this.session.sourceKey().isEmpty()) {
                return "source:" + this.session.sourceKey();
            }
            if (this.session == null && this.legacyMenu == null) {
                return switch (this.kind) {
                    case INVENTORY -> "local:inventory";
                    case CREATIVE -> "local:creative";
                    case CHARACTER -> "local:character";
                    case CONTAINER -> null;
                };
            }
            if (this.legacyMenu != null) {
                return "legacy:" + this.legacyMenu.getType() + ":" + this.title.getString();
            }
            return null;
        }

        private int contentX() {
            return this.x + WINDOW_CONTENT_PADDING;
        }

        private int contentY() {
            return this.y + TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING;
        }

        private boolean contains(double mouseX, double mouseY) {
            int visibleHeight = this.minimized ? TOP_BAR_HEIGHT : this.height;
            if (InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, visibleHeight)) {
                return true;
            }

            if (this.kind != WindowKind.CREATIVE || this.minimized) {
                return false;
            }

            int tabsX = InventoryDesktopScreen.creativeContentX(this) + CREATIVE_TAB_X_OFFSET;
            int tabsWidth = CREATIVE_TAB_WIDTH * CREATIVE_TABS_PER_ROW;
            return InventoryDesktopScreen.contains(mouseX, mouseY, tabsX, InventoryDesktopScreen.creativeTopTabsY(this), tabsWidth, CREATIVE_TAB_HEIGHT)
                || InventoryDesktopScreen.contains(mouseX, mouseY, tabsX, InventoryDesktopScreen.creativeBottomTabsY(this), tabsWidth, CREATIVE_TAB_HEIGHT);
        }

        private boolean isTopBar(double mouseX, double mouseY) {
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, TOP_BAR_HEIGHT);
        }

        private boolean resizeGripAt(double mouseX, double mouseY) {
            return !this.minimized
                && InventoryDesktopScreen.contains(
                    mouseX,
                    mouseY,
                    this.x + this.width - RESIZE_GRIP_SIZE - 1,
                    this.y + this.height - RESIZE_GRIP_SIZE - 1,
                    RESIZE_GRIP_SIZE,
                    RESIZE_GRIP_SIZE
                );
        }

        private List<Slot> containerSlots() {
            if (this.session != null) {
                return this.session.containerSlots();
            }

            return this.legacyContainerSlots;
        }

        private int containerMinSlotX() {
            if (this.session != null) {
                return this.session.minSlotX();
            }

            return this.legacyMinSlotX;
        }

        private int containerMinSlotY() {
            if (this.session != null) {
                return this.session.minSlotY();
            }

            return this.legacyMinSlotY;
        }

        private @Nullable AbstractContainerMenu containerMenu() {
            if (this.session != null) {
                return this.session.menu();
            }

            return this.legacyMenu;
        }

        private int sessionId() {
            return this.session == null ? LEGACY_MENU_SESSION : this.session.sessionId();
        }

        private EnchantmentBookState enchantmentBookState() {
            if (this.enchantmentBookState == null) {
                this.enchantmentBookState = new EnchantmentBookState();
            }

            return this.enchantmentBookState;
        }

        private SmithingWindowState smithingState() {
            if (this.smithingState == null) {
                this.smithingState = new SmithingWindowState();
            }

            return this.smithingState;
        }

        private @Nullable SlotHit slotAt(InventoryDesktopScreen screen, double mouseX, double mouseY) {
            if (this.kind == WindowKind.INVENTORY) {
                List<Slot> inventorySlots = screen.mainInventorySlots();
                AbstractContainerMenu playerMenu = screen.playerMenu();
                SlotGridLayout layout = screen.storageLayout(this, screen.inventoryVirtualSlotCount());
                this.scrollRow = clamp(this.scrollRow, 0, layout.maxScrollRow());
                for (int row = 0; row < layout.visibleRows(); row++) {
                    for (int column = 0; column < layout.columns(); column++) {
                        int visibleIndex = this.scrollRow * layout.columns() + row * layout.columns() + column;
                        if (visibleIndex >= inventorySlots.size()) {
                            continue;
                        }

                        int slotX = this.contentX() + column * SLOT_SIZE;
                        int slotY = this.contentY() + row * SLOT_SIZE;
                        if (InventoryDesktopScreen.contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                            Slot slot = inventorySlots.get(visibleIndex);
                            return new SlotHit(slot, playerMenu.slots.indexOf(slot), slotX, slotY, playerMenu, DesktopPackets.PLAYER_MENU_SESSION);
                        }
                    }
                }
            } else if (this.kind == WindowKind.CONTAINER) {
                AbstractContainerMenu menu = this.containerMenu();
                if (menu == null) {
                    return null;
                }

                List<Slot> slots = this.containerSlots();
                if (this.session != null && this.session.isMountSession()) {
                    return screen.mountSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (this.apiDefinition != null) {
                    return screen.apiSlotAt(this, mouseX, mouseY);
                }
                if (menu instanceof AbstractFurnaceMenu) {
                    return screen.furnaceSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof CraftingMenu craftingMenu) {
                    return screen.craftingTableSlotAt(this, craftingMenu, mouseX, mouseY);
                }
                if (menu instanceof AnvilMenu) {
                    return screen.anvilSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof CrafterMenu) {
                    return screen.crafterSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof BeaconMenu) {
                    return screen.beaconSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof BrewingStandMenu) {
                    return screen.brewingSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof CartographyTableMenu) {
                    return screen.cartographySlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof SmithingMenu) {
                    return screen.smithingSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof GrindstoneMenu) {
                    return screen.grindstoneSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof StonecutterMenu) {
                    return screen.stonecutterSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof LoomMenu) {
                    return screen.loomSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof EnchantmentMenu) {
                    return screen.enchantmentSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof MerchantMenu) {
                    return screen.merchantSlotAt(this, slots, menu, mouseX, mouseY);
                }

                if (screen.isResizableStorageWindow(this)) {
                    SlotGridLayout layout = screen.storageLayout(this, slots.size());
                    this.scrollRow = clamp(this.scrollRow, 0, layout.maxScrollRow());
                    for (int row = 0; row < layout.visibleRows(); row++) {
                        for (int column = 0; column < layout.columns(); column++) {
                            int visibleIndex = this.scrollRow * layout.columns() + row * layout.columns() + column;
                            if (visibleIndex >= slots.size()) {
                                continue;
                            }

                            int slotX = this.contentX() + column * SLOT_SIZE;
                            int slotY = this.contentY() + row * SLOT_SIZE;
                            if (InventoryDesktopScreen.contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                                Slot slot = slots.get(visibleIndex);
                                return new SlotHit(slot, menu.slots.indexOf(slot), slotX, slotY, menu, this.sessionId());
                            }
                        }
                    }

                    return null;
                }

                int minX = this.containerMinSlotX();
                int minY = this.containerMinSlotY();
                for (Slot slot : slots) {
                    int slotX = this.contentX() + slot.x - minX;
                    int slotY = this.contentY() + slot.y - minY;
                    if (InventoryDesktopScreen.contains(mouseX, mouseY, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                        return new SlotHit(slot, menu.slots.indexOf(slot), slotX, slotY, menu, this.sessionId());
                    }
                }
            } else if (this.kind == WindowKind.CREATIVE) {
                return screen.creativeInventorySlotAt(this, mouseX, mouseY);
            } else {
                AbstractContainerMenu playerMenu = screen.playerMenu();
                int contentX = characterContentX(this);
                int contentY = characterContentY(this);
                int armorX = contentX + CHARACTER_ARMOR_X;
                int armorY = contentY + CHARACTER_ARMOR_Y;
                for (int i = 0; i < 4; i++) {
                    int slotIndex = 5 + i;
                    int slotY = armorY + i * SLOT_SIZE;
                    if (slotIndex < playerMenu.slots.size()
                        && InventoryDesktopScreen.contains(mouseX, mouseY, armorX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                        return new SlotHit(playerMenu.slots.get(slotIndex), slotIndex, armorX, slotY, playerMenu, DesktopPackets.PLAYER_MENU_SESSION);
                    }
                }

                int craftX = contentX + CHARACTER_CRAFT_X;
                int craftY = contentY + CHARACTER_CRAFT_Y;
                int[] slotIndexes = {1, 2, 3, 4, 0};
                int[] slotXs = {craftX, craftX + SLOT_SIZE, craftX, craftX + SLOT_SIZE, contentX + CHARACTER_CRAFT_RESULT_X};
                int[] slotYs = {craftY, craftY, craftY + SLOT_SIZE, craftY + SLOT_SIZE, contentY + CHARACTER_CRAFT_RESULT_Y};
                for (int i = 0; i < slotIndexes.length; i++) {
                    if (slotIndexes[i] < playerMenu.slots.size()
                        && InventoryDesktopScreen.contains(mouseX, mouseY, slotXs[i] - 1, slotYs[i] - 1, SLOT_SIZE, SLOT_SIZE)) {
                        return new SlotHit(playerMenu.slots.get(slotIndexes[i]), slotIndexes[i], slotXs[i], slotYs[i], playerMenu, DesktopPackets.PLAYER_MENU_SESSION);
                    }
                }
            }

            return null;
        }
    }
}
