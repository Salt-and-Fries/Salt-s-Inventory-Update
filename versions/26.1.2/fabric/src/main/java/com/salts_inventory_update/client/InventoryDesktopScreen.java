package com.salts_inventory_update.client;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.mixin.client.MenuScreensAccessor;
import com.salts_inventory_update.network.DesktopPackets;
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
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int OFFHAND_CONTAINER_SLOT = 40;
    private static final int OFFHAND_MENU_SLOT_FALLBACK = 45;
    private static final int OFFHAND_HOTBAR_GAP = 11;
    private static final int WINDOW_CONTENT_PADDING = 8;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int SCROLLBAR_TRACK_WIDTH = 4;
    private static final int RESIZE_GRIP_SIZE = 9;
    private static final int MIN_CONTAINER_WIDTH = 128;
    private static final int MIN_CONTAINER_HEIGHT = 64;
    private static final int LEGACY_MENU_SESSION = -1;
    private static final Identifier WINDOW_TEXTURE = WindowedInventoryClient.id("textures/gui/window.png");
    private static final Identifier WINDOW_CONTROLS_TEXTURE = WindowedInventoryClient.id("textures/gui/window_controls.png");
    private static final Identifier SLOT_TEXTURE = WindowedInventoryClient.id("textures/gui/slots.png");
    private static final Identifier CONTAINER_WIDGETS_TEXTURE = WindowedInventoryClient.id("textures/gui/container_widgets.png");
    private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final int WINDOW_TEXTURE_SIZE = 11;
    private static final int WINDOW_EDGE_SIZE = 5;
    private static final int CONTROL_TEXTURE_WIDTH = CONTROL_SIZE * 4;
    private static final int CONTROL_TEXTURE_HEIGHT = CONTROL_SIZE * 3;
    private static final int SLOT_TEXTURE_WIDTH = SLOT_SIZE * 2;
    private static final int SLOT_TEXTURE_HEIGHT = SLOT_SIZE;
    private static final int CONTAINER_WIDGETS_TEXTURE_WIDTH = 48;
    private static final int CONTAINER_WIDGETS_TEXTURE_HEIGHT = 30;
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
    private static final int CRAFTING_TABLE_CONTENT_MARGIN = 6;
    private static final int CRAFTING_TABLE_CONTENT_WIDTH = 122;
    private static final int CRAFTING_TABLE_CONTENT_HEIGHT = 54;
    private static final int CRAFTING_TABLE_GRID_COLUMNS = 3;
    private static final int CRAFTING_TABLE_GRID_ROWS = 3;
    private static final int CRAFTING_TABLE_GRID_X = 0;
    private static final int CRAFTING_TABLE_GRID_Y = 0;
    private static final int CRAFTING_TABLE_ARROW_X = 66;
    private static final int CRAFTING_TABLE_ARROW_Y = 19;
    private static final int CRAFTING_TABLE_RESULT_X = 104;
    private static final int CRAFTING_TABLE_RESULT_Y = 18;
    private static final int ENCHANTMENT_CONTENT_MARGIN = 6;
    private static final int ENCHANTMENT_CONTENT_WIDTH = 168;
    private static final int ENCHANTMENT_CONTENT_HEIGHT = 58;
    private static final int ENCHANTMENT_ITEM_SLOT = 0;
    private static final int ENCHANTMENT_LAPIS_SLOT = 1;
    private static final int ENCHANTMENT_ITEM_SLOT_X = 15;
    private static final int ENCHANTMENT_ITEM_SLOT_Y = 33;
    private static final int ENCHANTMENT_LAPIS_SLOT_X = 35;
    private static final int ENCHANTMENT_LAPIS_SLOT_Y = 33;
    private static final int ENCHANTMENT_BOOK_X = 14;
    private static final int ENCHANTMENT_BOOK_Y = 0;
    private static final int ENCHANTMENT_BOOK_WIDTH = 38;
    private static final int ENCHANTMENT_BOOK_HEIGHT = 31;
    private static final int ENCHANTMENT_BUTTON_X = 60;
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
    private static final int MERCHANT_TRADE_ARROW_X = 187;
    private static final int MERCHANT_TRADE_ARROW_Y = 49;
    private static final int MERCHANT_TRADE_ARROW_WIDTH = 10;
    private static final int MERCHANT_TRADE_ARROW_HEIGHT = 9;
    private static final int MERCHANT_DETAIL_LABEL_Y = 5;
    private static final int MERCHANT_PROGRESS_X = 134;
    private static final int MERCHANT_PROGRESS_Y = 22;
    private static final int MERCHANT_PROGRESS_WIDTH = 96;
    private static final int MERCHANT_PROGRESS_HEIGHT = 5;
    private static final Identifier MERCHANT_TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
    private static final Identifier MERCHANT_TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
    private static final Identifier MERCHANT_SCROLLBAR_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier MERCHANT_SCROLLBAR_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
    private static final Identifier MERCHANT_DISCOUNT_STRIKETHROUGH_SPRITE = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");
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
    private static final int COLOR_SLOT_HOVER = 0xFF465265;
    private static final int COLOR_SLOT_BORDER = 0xFF9AA3B2;
    private static final int COLOR_WINDOW_TITLE = 0xFF111111;
    private static final int COLOR_TEXT = 0xFFE8EDF5;
    private static final int COLOR_MUTED_TEXT = 0xFFB3BDCC;
    private static final int COLOR_HOTBAR_HOVER = 0x44000000;
    private static final Component TITLE = Component.literal("Salt's Inventory Desktop");
    private static final List<WindowControl> FULL_TITLE_CONTROLS = List.of(WindowControl.FOCUS, WindowControl.MINIMIZE, WindowControl.CLOSE);
    private static final List<WindowControl> COMPACT_TITLE_CONTROLS = List.of(WindowControl.ELLIPSIS, WindowControl.CLOSE);
    private static final List<WindowControl> POPUP_CONTROLS = List.of(WindowControl.FOCUS, WindowControl.MINIMIZE);

    private static @Nullable InventoryDesktopScreen singleton;
    private static final Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> VANILLA_SCREEN_CONSTRUCTORS = new LinkedHashMap<>();
    private static int nextDesktopId = 1;

    private final int desktopId;
    private final List<DesktopContainerSession> sessions = new ArrayList<>();
    private final List<InventoryWindow> windows = new ArrayList<>();
    private @Nullable LocalPlayer owner;
    private boolean hotbarOnly;
    private boolean cameraControl;
    private @Nullable InventoryWindow movingWindow;
    private @Nullable InventoryWindow pressedControlWindow;
    private @Nullable WindowControl pressedControl;
    private boolean pressedControlInPopup;
    private @Nullable InventoryWindow popupWindow;
    private int moveOffsetX;
    private int moveOffsetY;
    private @Nullable InventoryWindow resizingWindow;
    private int resizeStartMouseX;
    private int resizeStartMouseY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private @Nullable Slot dragStartSlot;
    private ItemStack sharedCarried = ItemStack.EMPTY;
    private @Nullable BookModel enchantmentBookModel;
    private boolean attackingWorld;
    private boolean usingWorld;

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
        registerContainerScreen(MenuType.LECTERN, constructor);
        registerContainerScreen(MenuType.LOOM, constructor);
        registerContainerScreen(MenuType.MERCHANT, constructor);
        registerContainerScreen(MenuType.SHULKER_BOX, constructor);
        registerContainerScreen(MenuType.SMITHING, constructor);
        registerContainerScreen(MenuType.SMOKER, constructor);
        registerContainerScreen(MenuType.CARTOGRAPHY_TABLE, constructor);
        registerContainerScreen(MenuType.STONECUTTER, constructor);
    }

    private static void registerContainerScreen(MenuType<?> menuType, MenuScreens.ScreenConstructor constructor) {
        Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> screens = MenuScreensAccessor.salts_inventory_update$getScreens();
        VANILLA_SCREEN_CONSTRUCTORS.putIfAbsent(menuType, screens.get(menuType));
        screens.put(menuType, constructor);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Screen createContainerFallbackScreen(
        Minecraft minecraft,
        AbstractContainerMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        MenuScreens.ScreenConstructor vanillaConstructor = VANILLA_SCREEN_CONSTRUCTORS.get(menu.getType());
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

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        DesktopDebug.log("client request E inventory desktop={} active={}", screen.desktopId, minecraft.screen == screen);
        screen.toggleWindow(WindowKind.INVENTORY);
        screen.showIfNeeded(minecraft);
    }

    public static void openOrToggleCharacter(Minecraft minecraft) {
        if (!canUseDesktopInput(minecraft)) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        DesktopDebug.log("client request C character desktop={} active={}", screen.desktopId, minecraft.screen == screen);
        screen.toggleWindow(WindowKind.CHARACTER);
        screen.showIfNeeded(minecraft);
    }

    public static void openHotbarOnly(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        screen.hotbarOnly = true;
        DesktopDebug.trace("client hotbar-only show desktop={}", screen.desktopId);
        screen.showIfNeeded(minecraft);
    }

    public static void openOrAddSession(Minecraft minecraft, DesktopContainerSession session) {
        if (minecraft.player == null) {
            return;
        }

        InventoryDesktopScreen screen = getOrCreate(minecraft);
        screen.addOrReplaceSession(session);
        screen.showIfNeeded(minecraft);
    }

    public static void updateDesktopCursorTarget(Minecraft minecraft) {
        if (minecraft.screen instanceof InventoryDesktopScreen screen && screen.shouldUseCursorWorldTarget()) {
            screen.updateCursorWorldTarget();
        }
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
        return minecraft.player != null
            && (minecraft.screen == null || minecraft.screen instanceof InventoryDesktopScreen);
    }

    public void updateSessionSlot(int sessionId, int slotIndex, int stateId, ItemStack stack) {
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
        }

        this.sharedCarried = copy;
        this.syncSharedCarriedToMenus();
    }

    public void removeSession(int sessionId) {
        boolean removedSession = this.sessions.removeIf(session -> session.sessionId() == sessionId);
        boolean removedWindow = false;
        for (InventoryWindow window : List.copyOf(this.windows)) {
            if (window.session != null && window.session.sessionId() == sessionId) {
                this.clearPopupStateFor(window);
                this.windows.remove(window);
                removedWindow = true;
            }
        }
        DesktopDebug.log("client session closed desktop={} session={} removedSession={} removedWindow={}", this.desktopId, sessionId, removedSession, removedWindow);
        this.closeIfEmpty();
    }

    public void applyMerchantOffers(DesktopMerchantOffersPayload payload) {
        DesktopContainerSession session = this.session(payload.sessionId());
        if (session != null) {
            session.applyMerchantOffers(payload);
            DesktopDebug.trace("client merchant offers desktop={} session={}", this.desktopId, payload.sessionId());
        }
    }

    @Override
    protected void init() {
        DesktopDebug.trace("client init desktop={} width={} height={} windows={}", this.desktopId, this.width, this.height, this.windows.size());
    }

    public boolean hasWindows() {
        return !this.windows.isEmpty();
    }

    public boolean isHotbarOnly() {
        return this.hotbarOnly && this.windows.isEmpty();
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
        for (InventoryWindow window : this.windows) {
            if (!window.minimized && window.containerMenu() instanceof EnchantmentMenu enchantmentMenu) {
                window.enchantmentBookState().tick(enchantmentMenu);
            }
        }

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
        this.minecraft.gui.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        int uiMouseX = this.cameraControl ? Integer.MIN_VALUE : mouseX;
        int uiMouseY = this.cameraControl ? Integer.MIN_VALUE : mouseY;
        for (InventoryWindow window : this.windows) {
            this.renderWindow(graphics, window, uiMouseX, uiMouseY);
        }

        this.renderControlPopup(graphics, uiMouseX, uiMouseY);
        this.renderDesktopHotbarAffordances(graphics, uiMouseX, uiMouseY);
        if (!this.cameraControl) {
            renderCarried(graphics, this.sharedCarried, mouseX, mouseY, this.minecraft);
            this.extractHoveredTooltip(graphics, mouseX, mouseY);
        }
        this.renderDebugOverlay(graphics, uiMouseX, uiMouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.cameraControl) {
            this.handleGameplayClick(event);
            return true;
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
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

        InventoryWindow window = this.windowAt(event.x(), event.y());
        WindowControl titleControl = window == null ? null : this.titleBarControlAt(window, event.x(), event.y());
        if (this.popupWindow != null
            && !this.popupContains(event.x(), event.y())
            && !(window == this.popupWindow && titleControl == WindowControl.ELLIPSIS)) {
            this.popupWindow = null;
        }

        if (window != null) {
            this.bringToFront(window);
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
                this.popupWindow = null;
                this.resizeStartMouseX = (int) event.x();
                this.resizeStartMouseY = (int) event.y();
                this.resizeStartWidth = window.width;
                this.resizeStartHeight = window.height;
                DesktopDebug.trace("client resize start desktop={} window={} width={} height={}", this.desktopId, window.debugName(), window.width, window.height);
                return true;
            }

            if (window.isTopBar(event.x(), event.y())) {
                this.movingWindow = window;
                this.resizingWindow = null;
                this.popupWindow = null;
                this.moveOffsetX = (int) event.x() - window.x;
                this.moveOffsetY = (int) event.y() - window.y;
                DesktopDebug.trace("client move start desktop={} window={}", this.desktopId, window.debugName());
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

            SlotHit slotHit = this.slotAt(event.x(), event.y());
            if (slotHit != null) {
                if (this.shouldQuickMove()) {
                    this.quickMoveSlot(slotHit);
                    return true;
                }
                this.dragStartSlot = slotHit.slot();
                this.slotClicked(slotHit, event.button(), ContainerInput.PICKUP);
            }
            return true;
        }

        SlotHit slotHit = this.slotAt(event.x(), event.y());
        if (slotHit != null) {
            if (this.shouldQuickMove()) {
                this.quickMoveSlot(slotHit);
                return true;
            }
            this.dragStartSlot = slotHit.slot();
            this.slotClicked(slotHit, event.button(), ContainerInput.PICKUP);
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

        if (this.resizingWindow != null) {
            InventoryWindow window = this.resizingWindow;
            int minWidth = this.minResizableWidth(window);
            int minHeight = this.minResizableHeight();
            int maxWidth = Math.max(minWidth, this.desktopWidth() - window.x);
            int maxHeight = Math.max(minHeight, this.desktopHeight() - window.y);
            window.width = clamp(this.resizeStartWidth + (int) event.x() - this.resizeStartMouseX, minWidth, maxWidth);
            window.height = clamp(this.resizeStartHeight + (int) event.y() - this.resizeStartMouseY, minHeight, maxHeight);
            this.clampStorageScroll(window);
            return true;
        }

        if (this.movingWindow != null) {
            this.movingWindow.x = clamp((int) event.x() - this.moveOffsetX, 0, Math.max(0, this.desktopWidth() - this.movingWindow.width));
            this.movingWindow.y = clamp((int) event.y() - this.moveOffsetY, 0, Math.max(0, this.desktopHeight() - TOP_BAR_HEIGHT));
            return true;
        }

        return this.sharedCarried.isEmpty() ? this.hotbarOnly : true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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

        this.movingWindow = null;
        InventoryWindow resizedWindow = this.resizingWindow;
        this.resizingWindow = null;
        if (resizedWindow != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.snapResizableWindow(resizedWindow);
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

    private void stopWorldAttack() {
        if (this.attackingWorld && this.minecraft != null) {
            CursorWorldInteraction.stopAttack(this.minecraft);
        }
        this.attackingWorld = false;
    }

    private void stopWorldUse() {
        this.usingWorld = false;
    }

    private void slotClicked(SlotHit hit, int button, ContainerInput input) {
        DesktopDebug.trace(
            "client slot click desktop={} session={} slot={} button={} input={} menu={}",
            this.desktopId,
            hit.sessionId(),
            hit.slotId(),
            button,
            input,
            hit.menu().containerId
        );

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
            return;
        }

        if (!DesktopContainerClient.clickSlot(hit.sessionId(), hit.slotId(), button, input)) {
            if (hit.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
                slotClicked(hit.menu(), hit.slotId(), button, input, this.minecraft);
                this.setSharedCarried(hit.menu().getCarried());
            } else {
                DesktopDebug.warn("client session click dropped session={} menu={} slot={} reason=packet-send-failed", hit.sessionId(), hit.menu().containerId, hit.slotId());
            }
        }
    }

    private boolean shouldQuickMove() {
        return this.sharedCarried.isEmpty() && this.minecraft != null
            && (InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
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

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.cameraControl) {
            return this.scrollHotbar(scrollY);
        }

        InventoryWindow window = this.windowAt(x, y);
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
        if (this.minecraft.options.keyInventory.matches(event)) {
            this.toggleWindow(WindowKind.INVENTORY);
            this.showIfNeeded(this.minecraft);
            return true;
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

        for (int i = 0; i < this.minecraft.options.keyHotbarSlots.length && i < HOTBAR_SLOT_COUNT; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                this.selectHotbarSlot(i);
                return true;
            }
        }

        this.syncMovementKey(event, true);
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
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
            this.minecraft.options.keyShift.setDown(down);
        } else if (this.minecraft.options.keySprint.matches(event)) {
            this.minecraft.options.keySprint.setDown(down);
        }
    }

    private void toggleWindow(WindowKind kind) {
        for (int i = 0; i < this.windows.size(); i++) {
            InventoryWindow window = this.windows.get(i);
            if (window.kind == kind && window.session == null && window.legacyMenu == null) {
                this.clearPopupStateFor(window);
                this.windows.remove(i);
                DesktopDebug.log("client window remove desktop={} kind={} reason=toggle", this.desktopId, kind);
                this.closeIfEmpty();
                return;
            }
        }

        this.addWindow(kind);
        this.hotbarOnly = false;
    }

    private void addWindow(WindowKind kind) {
        InventoryWindow window;
        if (kind == WindowKind.INVENTORY) {
            int windowWidth = storageWindowWidth(INVENTORY_DEFAULT_COLUMNS, false);
            int windowHeight = storageWindowHeight(INVENTORY_DEFAULT_VISIBLE_ROWS);
            window = new InventoryWindow(
                kind,
                Component.literal("Inventory"),
                this.clampedWindowX(this.desktopWidth() / 2 - 160, windowWidth),
                this.clampedWindowY(this.desktopHeight() / 2 - 72, windowHeight),
                windowWidth,
                windowHeight
            );
        } else if (kind == WindowKind.CHARACTER) {
            int windowWidth = 224;
            int windowHeight = 158;
            window = new InventoryWindow(
                kind,
                Component.literal("Character"),
                this.clampedWindowX(this.desktopWidth() / 2 + 20, windowWidth),
                this.clampedWindowY(this.desktopHeight() / 2 - 84, windowHeight),
                windowWidth,
                windowHeight
            );
        } else {
            return;
        }

        this.windows.add(window);
        this.setFocusedWindow(window);
        DesktopDebug.log("client window add desktop={} kind={} title={} windows={}", this.desktopId, kind, window.title.getString(), this.windows.size());
    }

    private void addLegacyContainerWindow(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        List<Slot> slots = findContainerSlots(menu, playerInventory);
        int minX = minSlotX(slots);
        int minY = minSlotY(slots);
        int contentWidth = containerContentWidth(slots, minX);
        int contentHeight = containerContentHeight(slots, minY);
        int windowWidth = this.containerWindowWidth(menu, title, slots.size(), contentWidth);
        int windowHeight = containerWindowHeight(menu, slots.size(), contentHeight);
        int offset = Math.min(64, this.windows.size() * 14);
        InventoryWindow window = new InventoryWindow(
            WindowKind.CONTAINER,
            title,
            this.clampedWindowX(this.desktopWidth() / 2 - windowWidth / 2 + offset, windowWidth),
            this.clampedWindowY(this.desktopHeight() / 2 - windowHeight / 2 - 32 + offset, windowHeight),
            windowWidth,
            windowHeight,
            null,
            menu,
            slots,
            minX,
            minY
        );
        this.windows.add(window);
        this.setFocusedWindow(window);
        this.hotbarOnly = false;
        this.setSharedCarried(menu.getCarried());
    }

    private void addOrReplaceSession(DesktopContainerSession session) {
        boolean replacedSource = false;
        if (!session.sourceKey().isEmpty()) {
            for (DesktopContainerSession existing : List.copyOf(this.sessions)) {
                if (!session.sourceKey().equals(existing.sourceKey())) {
                    continue;
                }

                DesktopDebug.warn(
                    "client duplicate source session desktop={} source={} oldSession={} incomingSession={} action=toggle-close",
                    this.desktopId,
                    session.sourceKey(),
                    existing.sessionId(),
                    session.sessionId()
                );
                DesktopContainerClient.closeSession(existing.sessionId());
                this.sessions.remove(existing);
                for (InventoryWindow window : List.copyOf(this.windows)) {
                    if (window.session == existing) {
                        this.clearPopupStateFor(window);
                        this.windows.remove(window);
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
                this.clearPopupStateFor(window);
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
        session.setCarried(this.sharedCarried);

        int windowWidth = this.containerWindowWidth(session.menu(), session.title(), session.containerSlots().size(), session.contentWidth());
        int windowHeight = containerWindowHeight(session.menu(), session.containerSlots().size(), session.contentHeight());
        int offset = Math.min(64, this.sessions.size() * 14);
        InventoryWindow window = new InventoryWindow(
            WindowKind.CONTAINER,
            session.title(),
            this.clampedWindowX(this.desktopWidth() / 2 - windowWidth / 2 + offset, windowWidth),
            this.clampedWindowY(this.desktopHeight() / 2 - windowHeight / 2 - 32 + offset, windowHeight),
            windowWidth,
            windowHeight,
            session
        );
        this.windows.add(window);
        this.setFocusedWindow(window);
        this.hotbarOnly = false;
        DesktopDebug.log(
            "client session window add desktop={} session={} title={} replacedSession={} replacedWindow={} replacedSource={} windows={} sessions={}",
            this.desktopId,
            session.sessionId(),
            session.title().getString(),
            replacedSession,
            replacedWindow,
            replacedSource,
            this.windows.size(),
            this.sessions.size()
        );
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
            if (this.isPlayerInventorySlot(slot)
                && slot.index >= 9
                && slot.getContainerSlot() >= HOTBAR_SLOT_COUNT
                && slot.getContainerSlot() < 36) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.comparingInt(Slot::getContainerSlot));
        return slots;
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
            case INVENTORY -> this.mainInventorySlots().size();
            case CONTAINER -> window.containerSlots().size();
            case CHARACTER -> 0;
        };
    }

    private SlotGridLayout storageLayout(InventoryWindow window, int slotCount) {
        int visibleRows = Math.max(1, (window.height - TOP_BAR_HEIGHT - WINDOW_CONTENT_PADDING * 2) / SLOT_SIZE);
        int availableWidth = Math.max(SLOT_SIZE, window.width - WINDOW_CONTENT_PADDING * 2);
        int columns = Math.max(1, availableWidth / SLOT_SIZE);
        int totalRows = rowsForSlots(slotCount, columns);
        boolean scrollable = totalRows > visibleRows;
        if (scrollable) {
            int availableWithScrollbar = Math.max(SLOT_SIZE, availableWidth - SCROLLBAR_WIDTH);
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
        return !this.cameraControl && !window.minimized && this.isResizableStorageWindow(window);
    }

    private boolean isResizableStorageWindow(InventoryWindow window) {
        if (window.kind == WindowKind.INVENTORY) {
            return true;
        }

        if (window.kind != WindowKind.CONTAINER) {
            return false;
        }

        AbstractContainerMenu menu = window.containerMenu();
        if (menu == null) {
            return false;
        }

        MenuType<?> type = menu.getType();
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
            || type == MenuType.LECTERN
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
        return Math.max(WINDOW_CONTENT_PADDING * 2 + SLOT_SIZE + SCROLLBAR_WIDTH, this.minimumTitleBarWidth(window.title));
    }

    private int minResizableHeight() {
        return TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + SLOT_SIZE;
    }

    private static int storageWindowWidth(int columns, boolean scrollbar) {
        return WINDOW_CONTENT_PADDING * 2 + columns * SLOT_SIZE + (scrollbar ? SCROLLBAR_WIDTH : 0);
    }

    private static int storageWindowHeight(int rows) {
        return TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING * 2 + rows * SLOT_SIZE;
    }

    private int containerWindowWidth(AbstractContainerMenu menu, Component title, int slotCount, int contentWidth) {
        int titleWidth = this.fullTitleBarWidth(title);
        if (menu instanceof AbstractFurnaceMenu) {
            return Math.max(titleWidth, FURNACE_CONTENT_MARGIN * 2 + FURNACE_CONTENT_WIDTH);
        }
        if (menu instanceof CraftingMenu) {
            return Math.max(titleWidth, CRAFTING_TABLE_CONTENT_MARGIN * 2 + CRAFTING_TABLE_CONTENT_WIDTH);
        }
        if (menu instanceof EnchantmentMenu) {
            return Math.max(titleWidth, ENCHANTMENT_CONTENT_MARGIN * 2 + ENCHANTMENT_CONTENT_WIDTH);
        }
        if (menu instanceof MerchantMenu) {
            return Math.max(titleWidth, MERCHANT_CONTENT_MARGIN * 2 + MERCHANT_CONTENT_WIDTH);
        }

        StorageGridSize defaultGrid = defaultStorageGridSize(menu.getType(), slotCount);
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
        if (menu instanceof EnchantmentMenu) {
            return TOP_BAR_HEIGHT + ENCHANTMENT_CONTENT_MARGIN * 2 + ENCHANTMENT_CONTENT_HEIGHT;
        }
        if (menu instanceof MerchantMenu) {
            return TOP_BAR_HEIGHT + MERCHANT_CONTENT_MARGIN * 2 + MERCHANT_CONTENT_HEIGHT;
        }

        StorageGridSize defaultGrid = defaultStorageGridSize(menu.getType(), slotCount);
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

    private void closeIfEmpty() {
        if (!this.windows.isEmpty()) {
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
        if (this.pressedControlWindow == window) {
            this.pressedControlWindow = null;
            this.pressedControl = null;
            this.pressedControlInPopup = false;
        }
    }

    private void showIfNeeded(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        if (!this.hasDesktopSurface()) {
            this.hideFromScreen();
            return;
        }

        if (minecraft.screen != this) {
            DesktopDebug.log("client screen show desktop={} windows={} sessions={} hotbarOnly={}", this.desktopId, this.windows.size(), this.sessions.size(), this.hotbarOnly);
            minecraft.setScreen(this);
        }
    }

    private void hideFromScreen() {
        Minecraft minecraft = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        if (minecraft.screen == this) {
            DesktopDebug.log("client screen hide desktop={} windows={} sessions={}", this.desktopId, this.windows.size(), this.sessions.size());
            minecraft.setScreen(null);
        }
    }

    private boolean hasDesktopSurface() {
        return this.hotbarOnly || !this.windows.isEmpty() || !this.sharedCarried.isEmpty();
    }

    private void closeAllWindowsAndHide() {
        DesktopDebug.log("client close all desktop={} windows={} sessions={}", this.desktopId, this.windows.size(), this.sessions.size());
        for (InventoryWindow window : List.copyOf(this.windows)) {
            if (window.session != null) {
                DesktopContainerClient.closeSession(window.session.sessionId());
            } else if (window.legacyMenu != null) {
                this.closeLegacyContainer(window);
            }
        }
        this.sessions.clear();
        this.windows.clear();
        this.hotbarOnly = false;
        this.movingWindow = null;
        this.resizingWindow = null;
        this.popupWindow = null;
        this.pressedControlWindow = null;
        this.pressedControl = null;
        this.pressedControlInPopup = false;
        this.dragStartSlot = null;
        this.usingWorld = false;
        this.hideFromScreen();
    }

    private void clearForOwnerChange(String reason) {
        DesktopDebug.log("client clear desktop={} reason={} windows={} sessions={}", this.desktopId, reason, this.windows.size(), this.sessions.size());
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
        this.dragStartSlot = null;
        this.usingWorld = false;
        this.sharedCarried = ItemStack.EMPTY;
        this.stopWorldAttack();
    }

    private void activateControl(InventoryWindow window, WindowControl control) {
        DesktopDebug.log("client control desktop={} window={} control={}", this.desktopId, window.debugName(), control);
        switch (control) {
            case CLOSE -> {
                this.clearPopupStateFor(window);
                if (window.session != null) {
                    DesktopContainerClient.closeSession(window.session.sessionId());
                    this.sessions.remove(window.session);
                    this.windows.remove(window);
                    this.closeIfEmpty();
                    return;
                }

                if (window.legacyMenu != null) {
                    this.closeLegacyContainer(window);
                    this.windows.remove(window);
                    this.closeIfEmpty();
                    return;
                }

                this.windows.remove(window);
                this.closeIfEmpty();
            }
            case MINIMIZE -> {
                window.minimized = !window.minimized;
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
            }
            case FOCUS -> {
                this.setFocusedWindow(window.focused ? null : window);
                if (this.popupWindow == window) {
                    this.popupWindow = null;
                }
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
        return this.windowAt(mouseX, mouseY) != null || this.hotbarSlotAt(mouseX, mouseY) != null || this.offhandSlotAt(mouseX, mouseY) != null;
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

    private void setFocusedWindow(@Nullable InventoryWindow focusedWindow) {
        for (InventoryWindow window : this.windows) {
            window.focused = window == focusedWindow;
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
        renderNineSlice(graphics, WINDOW_TEXTURE, window.x, window.y, window.width, visibleHeight);
        graphics.text(this.font, titleLayout.displayTitle(), window.x + TITLE_LEFT_PADDING, window.y + 5, COLOR_WINDOW_TITLE, false);
        this.renderControls(graphics, window, titleLayout, mouseX, mouseY);

        if (window.minimized) {
            return;
        }

        switch (window.kind) {
            case INVENTORY -> this.renderInventoryWindow(graphics, window, mouseX, mouseY);
            case CONTAINER -> this.renderContainerWindow(graphics, window, mouseX, mouseY);
            case CHARACTER -> this.renderCharacterWindow(graphics, window, mouseX, mouseY);
        }

        this.renderResizeGrip(graphics, window, mouseX, mouseY);
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
        int textureRow = pressed || toggled ? 2 : hovered ? 1 : 0;
        blitRegion(
            graphics,
            WINDOW_CONTROLS_TEXTURE,
            x,
            y,
            control.ordinal() * CONTROL_SIZE,
            textureRow * CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_SIZE,
            CONTROL_TEXTURE_WIDTH,
            CONTROL_TEXTURE_HEIGHT
        );
    }

    private TitleBarLayout titleBarLayout(InventoryWindow window) {
        Component titleBarTitle = this.titleBarTitle(window);
        List<WindowControl> controls = this.fullTitleBarWidth(titleBarTitle) <= window.width ? FULL_TITLE_CONTROLS : COMPACT_TITLE_CONTROLS;
        int availableTitleWidth = Math.max(0, window.width - TITLE_LEFT_PADDING - TITLE_TO_CONTROLS_GAP - controlsWidth(controls));
        String title = this.truncatedTitle(titleBarTitle.getString(), availableTitleWidth);
        return new TitleBarLayout(title, this.controlRects(window, controls), controls == COMPACT_TITLE_CONTROLS);
    }

    private Component titleBarTitle(InventoryWindow window) {
        return window.containerMenu() instanceof MerchantMenu
            ? Component.translatable("merchant.trades")
            : window.title;
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
        SlotGridLayout layout = this.storageLayout(window, inventorySlots.size());
        this.clampStorageScroll(window);
        this.renderCompactSlots(graphics, window, inventorySlots, layout, mouseX, mouseY);
        this.renderScrollbar(graphics, window, layout);
    }

    private void renderContainerWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<Slot> slots = window.containerSlots();
        AbstractContainerMenu menu = window.containerMenu();
        int minX = window.containerMinSlotX();
        int minY = window.containerMinSlotY();
        if (slots.isEmpty()) {
            graphics.text(this.font, "No item slots", window.contentX(), window.contentY(), COLOR_MUTED_TEXT, false);
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT);
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, ENCHANTMENT_BUTTON_WIDTH, ENCHANTMENT_BUTTON_HEIGHT);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_LEVEL_DISABLED_SPRITES[option], x + 1, y + 1, 16, 16);
            graphics.textWithWordWrap(this.font, randomName, x + 20, y + 2, textWidth, 0xFF6E6855, false);
            costColor = -12550384;
        } else {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                hovered ? ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE : ENCHANTMENT_SLOT_SPRITE,
                x,
                y,
                ENCHANTMENT_BUTTON_WIDTH,
                ENCHANTMENT_BUTTON_HEIGHT
            );
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_LEVEL_SPRITES[option], x + 1, y + 1, 16, 16);
            graphics.textWithWordWrap(this.font, randomName, x + 20, y + 2, textWidth, hovered ? -128 : clueColor, false);
            costColor = -8323296;
        }

        graphics.text(this.font, costText, x + 20 + 86 - this.font.width(costText), y + 9, costColor);
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
                renderSlotBackground(graphics, x, y, hovered);
                graphics.item(Items.LAPIS_LAZULI.getDefaultInstance(), x, y, slot.index);
                graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, 0xAA8F8F8F);
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
            0x33111111
        );

        for (int row = 0; row < MERCHANT_VISIBLE_TRADES; row++) {
            int tradeIndex = window.merchantScroll + row;
            int rowX = contentX + MERCHANT_TRADE_LIST_X;
            int rowY = contentY + MERCHANT_TRADE_LIST_Y + row * MERCHANT_TRADE_ROW_HEIGHT;
            if (tradeIndex < offers.size()) {
                this.renderMerchantTradeRow(graphics, window, offers.get(tradeIndex), tradeIndex, rowX, rowY, mouseX, mouseY);
            } else {
                graphics.outline(rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT - 1, 0x22000000);
            }
        }

        this.renderMerchantScrollbar(graphics, window, offers, contentX, contentY);
        this.renderMerchantDetailLabel(graphics, window, menu, contentX, contentY);
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_PAYMENT_1_SLOT, contentX + MERCHANT_PAYMENT_1_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_PAYMENT_2_SLOT, contentX + MERCHANT_PAYMENT_2_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);

        MerchantOffer selectedOffer = window.merchantSelectedTrade >= 0 && window.merchantSelectedTrade < offers.size() ? offers.get(window.merchantSelectedTrade) : null;
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            selectedOffer != null && selectedOffer.isOutOfStock() ? MERCHANT_TRADE_ARROW_OUT_OF_STOCK_SPRITE : MERCHANT_TRADE_ARROW_SPRITE,
            contentX + MERCHANT_TRADE_ARROW_X,
            contentY + MERCHANT_TRADE_ARROW_Y,
            MERCHANT_TRADE_ARROW_WIDTH,
            MERCHANT_TRADE_ARROW_HEIGHT
        );
        this.renderContainerSlotIfPresent(graphics, slots, MERCHANT_RESULT_SLOT, contentX + MERCHANT_RESULT_X, contentY + MERCHANT_SLOT_Y, mouseX, mouseY);
        this.renderMerchantProgress(graphics, menu, selectedOffer, contentX, contentY);
    }

    private void renderMerchantDetailLabel(GuiGraphicsExtractor graphics, InventoryWindow window, MerchantMenu menu, int contentX, int contentY) {
        int traderLevel = clamp(menu.getTraderLevel(), 1, VillagerData.MAX_VILLAGER_LEVEL);
        Component level = Component.translatable("merchant.level." + traderLevel);
        String label = Component.translatable("merchant.title", window.title, level).getString();
        int labelMaxWidth = MERCHANT_CONTENT_WIDTH - MERCHANT_PROGRESS_X;
        label = this.truncatedTitle(label, labelMaxWidth);
        int x = contentX + MERCHANT_PROGRESS_X + Math.max(0, (labelMaxWidth - this.font.width(label)) / 2);
        graphics.text(this.font, label, x, contentY + MERCHANT_DETAIL_LABEL_Y, COLOR_WINDOW_TITLE, false);
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

        graphics.fill(rowX, rowY, rowX + MERCHANT_TRADE_LIST_WIDTH, rowY + MERCHANT_TRADE_ROW_HEIGHT - 1, background);
        graphics.outline(rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT - 1, selected ? 0xFF111111 : 0x55111111);

        ItemStack costA = offer.getCostA();
        ItemStack baseCostA = offer.getBaseCostA();
        this.renderMerchantOfferItem(graphics, costA, rowX + 4, rowY + 2);
        if (!ItemStack.matches(costA, baseCostA) || costA.getCount() != baseCostA.getCount()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MERCHANT_DISCOUNT_STRIKETHROUGH_SPRITE, rowX + 11, rowY + 14, 9, 2);
        }

        ItemStack costB = offer.getCostB();
        if (!costB.isEmpty()) {
            this.renderMerchantOfferItem(graphics, costB, rowX + 25, rowY + 2);
        }

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            offer.isOutOfStock() ? MERCHANT_TRADE_ARROW_OUT_OF_STOCK_SPRITE : MERCHANT_TRADE_ARROW_SPRITE,
            rowX + 50,
            rowY + 5,
            MERCHANT_TRADE_ARROW_WIDTH,
            MERCHANT_TRADE_ARROW_HEIGHT
        );
        this.renderMerchantOfferItem(graphics, offer.getResult(), rowX + 68, rowY + 2);

        if (offer.isOutOfStock()) {
            graphics.fill(rowX, rowY, rowX + MERCHANT_TRADE_LIST_WIDTH, rowY + MERCHANT_TRADE_ROW_HEIGHT - 1, 0x44000000);
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
        graphics.fill(x + 1, y, x + MERCHANT_TRADE_SCROLLBAR_WIDTH - 1, y + height, 0x33111111);
        Identifier sprite = maxScroll > 0 ? MERCHANT_SCROLLBAR_SPRITE : MERCHANT_SCROLLBAR_DISABLED_SPRITE;
        int thumbHeight = 27;
        int thumbY = y + (maxScroll <= 0 ? 0 : (height - thumbHeight) * window.merchantScroll / maxScroll);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, thumbY, MERCHANT_TRADE_SCROLLBAR_WIDTH, thumbHeight);
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
        graphics.fill(x, y, x + MERCHANT_PROGRESS_WIDTH, y + MERCHANT_PROGRESS_HEIGHT, 0xFF252A33);
        graphics.fill(x, y, x + futureWidth, y + MERCHANT_PROGRESS_HEIGHT, 0xFF6B6B37);
        graphics.fill(x, y, x + currentWidth, y + MERCHANT_PROGRESS_HEIGHT, 0xFF4A9F4A);
        graphics.outline(x, y, MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT, 0xFF111111);
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

    private void renderScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window, SlotGridLayout layout) {
        if (!layout.scrollable()) {
            return;
        }

        int maxScroll = layout.maxScrollRow();
        int x = window.x + window.width - WINDOW_CONTENT_PADDING - SCROLLBAR_WIDTH + (SCROLLBAR_WIDTH - SCROLLBAR_TRACK_WIDTH) / 2;
        int y = window.contentY();
        int height = layout.visibleRows() * SLOT_SIZE;
        graphics.fill(x, y, x + SCROLLBAR_TRACK_WIDTH, y + height, 0xFF20242C);
        int thumbHeight = maxScroll == 0 ? height : Math.max(12, height / (maxScroll + 1));
        int thumbY = maxScroll == 0 ? y : y + (height - thumbHeight) * window.scrollRow / maxScroll;
        graphics.fill(x, thumbY, x + SCROLLBAR_TRACK_WIDTH, thumbY + thumbHeight, 0xFF96A0AF);
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
        int x = window.contentX();
        int y = window.contentY();
        graphics.text(this.font, "Armor", x, y, COLOR_MUTED_TEXT, false);
        for (int i = 0; i < 4; i++) {
            int index = 5 + i;
            if (index < menu.slots.size()) {
                this.renderSlot(graphics, menu.slots.get(index), x, y + 12 + i * SLOT_SIZE, mouseX, mouseY);
            }
        }

        int modelX0 = x + 36;
        int modelY0 = y + 12;
        int modelX1 = x + 86;
        int modelY1 = y + 96;
        graphics.outline(modelX0, modelY0, modelX1 - modelX0, modelY1 - modelY0, 0xFF596273);
        if (this.minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, modelX0, modelY0, modelX1, modelY1, 30, 0.0625F, mouseX, mouseY, this.minecraft.player);
        }

        this.renderStats(graphics, x + 96, y + 12);
        this.renderCraftingSlots(graphics, window, mouseX, mouseY);
    }

    private void renderStats(GuiGraphicsExtractor graphics, int x, int y) {
        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }

        FoodData food = player.getFoodData();
        graphics.text(this.font, "HP " + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()), x, y, COLOR_TEXT, false);
        this.renderBar(graphics, x, y + 11, 86, player.getHealth() / player.getMaxHealth(), 0xFF7E2E3A);
        graphics.text(this.font, "Hunger " + food.getFoodLevel() + "/20", x, y + 25, COLOR_TEXT, false);
        this.renderBar(graphics, x, y + 36, 86, food.getFoodLevel() / 20.0F, 0xFF8A6730);
        graphics.text(this.font, "XP Lv " + player.experienceLevel, x, y + 50, COLOR_TEXT, false);
        this.renderBar(graphics, x, y + 61, 86, player.experienceProgress, 0xFF3C7F4C);

        int effectY = y + 76;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            graphics.text(this.font, "No effects", x, effectY, COLOR_MUTED_TEXT, false);
            return;
        }

        int rendered = 0;
        for (MobEffectInstance effect : effects) {
            if (rendered >= 3) {
                graphics.text(this.font, "+" + (effects.size() - rendered) + " more", x, effectY + rendered * 10, COLOR_MUTED_TEXT, false);
                break;
            }
            graphics.text(this.font, Component.translatable(effect.getDescriptionId()), x, effectY + rendered * 10, COLOR_MUTED_TEXT, false);
            rendered++;
        }
    }

    private void renderBar(GuiGraphicsExtractor graphics, int x, int y, int width, float progress, int color) {
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        graphics.fill(x, y, x + width, y + 5, 0xFF252A33);
        graphics.fill(x, y, x + filled, y + 5, color);
        graphics.outline(x, y, width, 5, 0xFF697386);
    }

    private void renderCraftingSlots(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        AbstractContainerMenu menu = this.playerMenu();
        int craftX = window.x + 148;
        int craftY = window.y + 82;
        graphics.text(this.font, "Craft", craftX, craftY - 12, COLOR_MUTED_TEXT, false);
        this.renderSlotIfPresent(graphics, menu, 1, craftX, craftY, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 2, craftX + SLOT_SIZE, craftY, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 3, craftX, craftY + SLOT_SIZE, mouseX, mouseY);
        this.renderSlotIfPresent(graphics, menu, 4, craftX + SLOT_SIZE, craftY + SLOT_SIZE, mouseX, mouseY);
        graphics.text(this.font, ">", craftX + 42, craftY + 10, COLOR_MUTED_TEXT, false);
        this.renderSlotIfPresent(graphics, menu, 0, craftX + 56, craftY + 9, mouseX, mouseY);
    }

    private void renderSlotIfPresent(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, int slotIndex, int x, int y, int mouseX, int mouseY) {
        if (slotIndex < menu.slots.size()) {
            this.renderSlot(graphics, menu.slots.get(slotIndex), x, y, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
        renderSlotBackground(graphics, x, y, hovered);
        if (slot.hasItem()) {
            graphics.item(slot.getItem(), x, y, slot.index);
            graphics.itemDecorations(this.font, slot.getItem(), x, y);
        } else if (slot.getNoItemIcon() != null) {
            graphics.centeredText(this.font, ".", x + 8, y + 4, COLOR_MUTED_TEXT);
        }
    }

    private void extractHoveredTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SlotHit hit = this.slotAt(mouseX, mouseY);
        if (hit != null && hit.slot().hasItem() && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hit.slot().getItem(), mouseX, mouseY);
            return;
        }

        MerchantOfferItemHit offerHit = this.merchantOfferItemAt(mouseX, mouseY);
        if (offerHit != null && !offerHit.stack().isEmpty() && this.sharedCarried.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, offerHit.stack(), mouseX, mouseY);
        }
    }

    private void renderDesktopHotbarAffordances(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot offhandSlot = this.offhandSlot();
        int offhandX = offhandSlotX();
        int hotbarY = hotbarY();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, offhandX - 3, hotbarY - 4, 29, 24);

        for (Slot slot : this.hotbarSlots()) {
            int x = hotbarSlotX(slot.getContainerSlot());
            if (contains(mouseX, mouseY, x - 1, hotbarY - 1, SLOT_SIZE, SLOT_SIZE)) {
                this.renderHotbarHover(graphics, slot, x, hotbarY);
                return;
            }
        }

        if (offhandSlot != null && contains(mouseX, mouseY, offhandX - 1, hotbarY - 1, SLOT_SIZE, SLOT_SIZE)) {
            this.renderHotbarHover(graphics, offhandSlot, offhandX, hotbarY);
        }
    }

    private void renderHotbarHover(GuiGraphicsExtractor graphics, Slot slot, int x, int y) {
        graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_HOTBAR_HOVER);
        if (slot.hasItem()) {
            graphics.item(slot.getItem(), x, y, slot.index);
            graphics.itemDecorations(this.font, slot.getItem(), x, y);
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
        graphics.text(this.font, "Desktop " + this.desktopId + " active=" + (this.minecraft != null && this.minecraft.screen == this), x, y, 0xFFFFD166, false);
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

    private static void renderSlotBackground(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
        blitRegion(
            graphics,
            SLOT_TEXTURE,
            x - 1,
            y - 1,
            hovered ? SLOT_SIZE : 0,
            0,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_TEXTURE_WIDTH,
            SLOT_TEXTURE_HEIGHT
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
            textureHeight
        );
    }

    private static void renderCarried(GuiGraphicsExtractor graphics, ItemStack carried, int mouseX, int mouseY, Minecraft minecraft) {
        if (!carried.isEmpty()) {
            graphics.nextStratum();
            graphics.item(carried, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(minecraft.font, carried, mouseX - 8, mouseY - 8);
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

    private static float clampProgress(float progress) {
        return Math.max(0.0F, Math.min(1.0F, progress));
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
        CHARACTER
    }

    private enum WindowControl {
        FOCUS("F"),
        MINIMIZE("_"),
        CLOSE("x"),
        ELLIPSIS("...");

        private final String label;

        WindowControl(String label) {
            this.label = label;
        }
    }

    private record SlotHit(Slot slot, int slotId, int x, int y, AbstractContainerMenu menu, int sessionId) {
    }

    private record MerchantOfferItemHit(ItemStack stack) {
    }

    private record SlotGridLayout(int columns, int visibleRows, int totalRows, int maxScrollRow, boolean scrollable) {
    }

    private record StorageGridSize(int columns, int rows) {
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
        private int scrollRow;
        private int merchantScroll;
        private int merchantSelectedTrade;
        private @Nullable EnchantmentBookState enchantmentBookState;

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

        private int contentX() {
            return this.x + WINDOW_CONTENT_PADDING;
        }

        private int contentY() {
            return this.y + TOP_BAR_HEIGHT + WINDOW_CONTENT_PADDING;
        }

        private boolean contains(double mouseX, double mouseY) {
            int visibleHeight = this.minimized ? TOP_BAR_HEIGHT : this.height;
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, visibleHeight);
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

        private @Nullable SlotHit slotAt(InventoryDesktopScreen screen, double mouseX, double mouseY) {
            if (this.kind == WindowKind.INVENTORY) {
                List<Slot> inventorySlots = screen.mainInventorySlots();
                AbstractContainerMenu playerMenu = screen.playerMenu();
                SlotGridLayout layout = screen.storageLayout(this, inventorySlots.size());
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
                if (menu instanceof AbstractFurnaceMenu) {
                    return screen.furnaceSlotAt(this, slots, menu, mouseX, mouseY);
                }
                if (menu instanceof CraftingMenu craftingMenu) {
                    return screen.craftingTableSlotAt(this, craftingMenu, mouseX, mouseY);
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
            } else {
                AbstractContainerMenu playerMenu = screen.playerMenu();
                int armorX = this.contentX();
                int armorY = this.contentY() + 12;
                for (int i = 0; i < 4; i++) {
                    int slotIndex = 5 + i;
                    int slotY = armorY + i * SLOT_SIZE;
                    if (slotIndex < playerMenu.slots.size()
                        && InventoryDesktopScreen.contains(mouseX, mouseY, armorX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE)) {
                        return new SlotHit(playerMenu.slots.get(slotIndex), slotIndex, armorX, slotY, playerMenu, DesktopPackets.PLAYER_MENU_SESSION);
                    }
                }

                int craftX = this.x + 148;
                int craftY = this.y + 82;
                int[] slotIndexes = {1, 2, 3, 4, 0};
                int[] slotXs = {craftX, craftX + SLOT_SIZE, craftX, craftX + SLOT_SIZE, craftX + 56};
                int[] slotYs = {craftY, craftY, craftY + SLOT_SIZE, craftY + SLOT_SIZE, craftY + 9};
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
