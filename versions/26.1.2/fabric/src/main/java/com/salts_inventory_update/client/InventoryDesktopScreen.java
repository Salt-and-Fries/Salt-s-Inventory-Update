package com.salts_inventory_update.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
    private static final int INVENTORY_COLUMNS = 6;
    private static final int INVENTORY_VISIBLE_ROWS = 3;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int MIN_CONTAINER_WIDTH = 128;
    private static final int MIN_CONTAINER_HEIGHT = 64;
    private static final int LEGACY_MENU_SESSION = -1;
    private static final int COLOR_WINDOW = 0xEE16191F;
    private static final int COLOR_WINDOW_BORDER = 0xFF7C8799;
    private static final int COLOR_TOP_BAR = 0xFF263143;
    private static final int COLOR_TOP_BAR_FOCUSED = 0xFF345372;
    private static final int COLOR_SLOT = 0xFF252A33;
    private static final int COLOR_SLOT_HOVER = 0xFF465265;
    private static final int COLOR_SLOT_BORDER = 0xFF9AA3B2;
    private static final int COLOR_TEXT = 0xFFE8EDF5;
    private static final int COLOR_MUTED_TEXT = 0xFFB3BDCC;
    private static final Component TITLE = Component.literal("Salt's Inventory Desktop");

    private static @Nullable InventoryDesktopScreen singleton;
    private static @Nullable Slot externalDragStartSlot;
    private static int nextDesktopId = 1;

    private final int desktopId;
    private final List<DesktopContainerSession> sessions = new ArrayList<>();
    private final List<InventoryWindow> windows = new ArrayList<>();
    private @Nullable LocalPlayer owner;
    private boolean hotbarOnly;
    private boolean cameraControl;
    private @Nullable InventoryWindow movingWindow;
    private int moveOffsetX;
    private int moveOffsetY;
    private @Nullable Slot dragStartSlot;
    private ItemStack sharedCarried = ItemStack.EMPTY;
    private boolean attackingWorld;

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
            InventoryDesktopScreen.addLegacyContainerWindow(Minecraft.getInstance(), (AbstractContainerMenu) menu, inventory, title);
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
        MenuScreensAccessor.salts_inventory_update$getScreens().put(menuType, constructor);
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
        boolean removedWindow = this.windows.removeIf(window -> window.session != null && window.session.sessionId() == sessionId);
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

    public static void extractExternalHotbarOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canUseExternalHotbar(minecraft)) {
            return;
        }

        renderHotbarOverlay(graphics, mouseX, mouseY, minecraft.player.inventoryMenu, true);
        renderCarried(graphics, minecraft.player.inventoryMenu.getCarried(), mouseX, mouseY, minecraft);
    }

    public static boolean handleExternalHotbarClick(MouseButtonEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canUseExternalHotbar(minecraft) || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        Slot slot = hotbarSlotAt(minecraft.player.inventoryMenu, event.x(), event.y());
        if (slot == null) {
            return false;
        }

        externalDragStartSlot = slot;
        DesktopDebug.trace("client external hotbar click slot={} button={}", slot.getContainerSlot(), event.button());
        slotClicked(minecraft.player.inventoryMenu, slot, event.button(), ContainerInput.PICKUP, minecraft);
        return true;
    }

    public static boolean handleExternalHotbarRelease(MouseButtonEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canUseExternalHotbar(minecraft) || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        Slot slot = hotbarSlotAt(minecraft.player.inventoryMenu, event.x(), event.y());
        if (slot != null && slot != externalDragStartSlot && !minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            DesktopDebug.trace("client external hotbar release slot={} button={}", slot.getContainerSlot(), event.button());
            slotClicked(minecraft.player.inventoryMenu, slot, event.button(), ContainerInput.PICKUP, minecraft);
            externalDragStartSlot = null;
            return true;
        }

        externalDragStartSlot = null;
        return false;
    }

    private static boolean canUseExternalHotbar(Minecraft minecraft) {
        return minecraft.player != null
            && minecraft.gameMode != null
            && minecraft.player.containerMenu == minecraft.player.inventoryMenu
            && !(minecraft.screen instanceof InventoryDesktopScreen);
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
        double mouseX = 0.0D;
        double mouseY = 0.0D;
        if (this.minecraft != null) {
            mouseX = this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
            mouseY = this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
            if (this.shouldUpdateCursorWorldTarget(mouseX, mouseY)) {
                CursorWorldInteraction.updateHitResultAtCursor(this.minecraft, mouseX, mouseY);
            }
        }

        if (this.attackingWorld && this.minecraft != null) {
            CursorWorldInteraction.continueAttackAtCursor(this.minecraft, mouseX, mouseY);
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
        for (InventoryWindow window : this.windows) {
            this.renderWindow(graphics, window, mouseX, mouseY);
        }

        this.renderActiveHotbarOverlay(graphics, mouseX, mouseY);
        renderCarried(graphics, this.sharedCarried, mouseX, mouseY, this.minecraft);
        this.extractHoveredTooltip(graphics, mouseX, mouseY);
        this.renderDebugOverlay(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.cameraControl) {
            this.handleWorldClick(event);
            return true;
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        InventoryWindow window = this.windowAt(event.x(), event.y());
        if (window != null) {
            this.bringToFront(window);
            WindowControl control = window.controlAt(event.x(), event.y());
            if (control != null) {
                this.activateControl(window, control);
                return true;
            }

            if (window.isTopBar(event.x(), event.y())) {
                this.movingWindow = window;
                this.moveOffsetX = (int) event.x() - window.x;
                this.moveOffsetY = (int) event.y() - window.y;
                DesktopDebug.trace("client move start desktop={} window={}", this.desktopId, window.debugName());
                return true;
            }

            SlotHit slotHit = this.slotAt(event.x(), event.y());
            if (slotHit != null) {
                this.dragStartSlot = slotHit.slot();
                this.slotClicked(slotHit, event.button(), ContainerInput.PICKUP);
            }
            return true;
        }

        SlotHit slotHit = this.slotAt(event.x(), event.y());
        if (slotHit != null) {
            this.dragStartSlot = slotHit.slot();
            this.slotClicked(slotHit, event.button(), ContainerInput.PICKUP);
            return true;
        }

        if (this.hasWindows() && this.sharedCarried.isEmpty()) {
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
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.stopWorldAttack();
            }
            return true;
        }

        this.movingWindow = null;
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }

        SlotHit slotHit = this.slotAt(event.x(), event.y());
        if (slotHit != null && slotHit.slot() != this.dragStartSlot && !this.sharedCarried.isEmpty()) {
            this.slotClicked(slotHit, event.button(), ContainerInput.PICKUP);
            this.dragStartSlot = null;
            return true;
        }

        this.dragStartSlot = null;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.stopWorldAttack();
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
            CursorWorldInteraction.useAtCursor(this.minecraft, event.x(), event.y());
        }
    }

    private void stopWorldAttack() {
        if (this.attackingWorld && this.minecraft != null) {
            CursorWorldInteraction.stopAttack(this.minecraft);
        }
        this.attackingWorld = false;
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

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        InventoryWindow window = this.windowAt(x, y);
        if (window != null && window.kind == WindowKind.INVENTORY && !window.minimized) {
            int maxScroll = this.maxInventoryScroll();
            if (maxScroll > 0) {
                int oldScroll = window.scrollRow;
                window.scrollRow = clamp(window.scrollRow + (scrollY < 0.0 ? 1 : -1), 0, maxScroll);
                DesktopDebug.trace("client inventory scroll desktop={} old={} new={}", this.desktopId, oldScroll, window.scrollRow);
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
            int windowWidth = 138;
            int windowHeight = 88;
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
        int windowWidth = Math.max(MIN_CONTAINER_WIDTH, 16 + contentWidth);
        int windowHeight = TOP_BAR_HEIGHT + 16 + Math.max(MIN_CONTAINER_HEIGHT - TOP_BAR_HEIGHT - 16, contentHeight);
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
        boolean replacedSession = this.sessions.removeIf(existing -> existing.sessionId() == session.sessionId());
        boolean replacedWindow = this.windows.removeIf(window -> window.session != null && window.session.sessionId() == session.sessionId());
        this.sessions.add(session);
        session.setCarried(this.sharedCarried);

        int windowWidth = Math.max(MIN_CONTAINER_WIDTH, 16 + session.contentWidth());
        int windowHeight = TOP_BAR_HEIGHT + 16 + Math.max(MIN_CONTAINER_HEIGHT - TOP_BAR_HEIGHT - 16, session.contentHeight());
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
            "client session window add desktop={} session={} title={} replacedSession={} replacedWindow={} windows={} sessions={}",
            this.desktopId,
            session.sessionId(),
            session.title().getString(),
            replacedSession,
            replacedWindow,
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
            if (this.isPlayerInventorySlot(slot) && slot.index >= 9 && slot.getContainerSlot() >= HOTBAR_SLOT_COUNT) {
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

    private int maxInventoryScroll() {
        int totalSlots = this.mainInventorySlots().size();
        int totalRows = (totalSlots + INVENTORY_COLUMNS - 1) / INVENTORY_COLUMNS;
        return Math.max(0, totalRows - INVENTORY_VISIBLE_ROWS);
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
        this.dragStartSlot = null;
        this.sharedCarried = ItemStack.EMPTY;
        this.stopWorldAttack();
    }

    private void activateControl(InventoryWindow window, WindowControl control) {
        DesktopDebug.log("client control desktop={} window={} control={}", this.desktopId, window.debugName(), control);
        switch (control) {
            case CLOSE -> {
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
            case MINIMIZE -> window.minimized = !window.minimized;
            case FOCUS -> this.setFocusedWindow(window.focused ? null : window);
        }
    }

    private boolean shouldUpdateCursorWorldTarget(double mouseX, double mouseY) {
        if (!this.hasWindows()) {
            return false;
        }

        if (this.cameraControl) {
            return true;
        }

        return this.sharedCarried.isEmpty() && !this.isPointOverInteractiveUi(mouseX, mouseY);
    }

    private boolean isPointOverInteractiveUi(double mouseX, double mouseY) {
        return this.windowAt(mouseX, mouseY) != null || this.hotbarSlotAt(mouseX, mouseY) != null;
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
        return hotbarSlot == null
            ? null
            : new SlotHit(hotbarSlot, this.playerMenu().slots.indexOf(hotbarSlot), hotbarSlotX(hotbarSlot.getContainerSlot()), hotbarY(), this.playerMenu(), DesktopPackets.PLAYER_MENU_SESSION);
    }

    private void renderWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        int visibleHeight = window.minimized ? TOP_BAR_HEIGHT : window.height;
        graphics.fill(window.x, window.y, window.x + window.width, window.y + visibleHeight, COLOR_WINDOW);
        graphics.outline(window.x, window.y, window.width, visibleHeight, COLOR_WINDOW_BORDER);
        graphics.fill(window.x + 1, window.y + 1, window.x + window.width - 1, window.y + TOP_BAR_HEIGHT, window.focused ? COLOR_TOP_BAR_FOCUSED : COLOR_TOP_BAR);
        graphics.text(this.font, window.title, window.x + 6, window.y + 5, COLOR_TEXT, false);
        this.renderControls(graphics, window);

        if (window.minimized) {
            return;
        }

        switch (window.kind) {
            case INVENTORY -> this.renderInventoryWindow(graphics, window, mouseX, mouseY);
            case CONTAINER -> this.renderContainerWindow(graphics, window, mouseX, mouseY);
            case CHARACTER -> this.renderCharacterWindow(graphics, window, mouseX, mouseY);
        }
    }

    private void renderControls(GuiGraphicsExtractor graphics, InventoryWindow window) {
        for (WindowControl control : WindowControl.values()) {
            int x = window.controlX(control);
            int y = window.y + 3;
            int color = switch (control) {
                case CLOSE -> 0xFF6A2E35;
                case MINIMIZE -> 0xFF5E6130;
                case FOCUS -> window.focused ? 0xFF2E6752 : 0xFF374151;
            };
            graphics.fill(x, y, x + CONTROL_SIZE, y + CONTROL_SIZE, color);
            graphics.outline(x, y, CONTROL_SIZE, CONTROL_SIZE, 0xFFBAC2CF);
            graphics.centeredText(this.font, control.label, x + CONTROL_SIZE / 2, y + 2, COLOR_TEXT);
        }
    }

    private void renderInventoryWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<Slot> inventorySlots = this.mainInventorySlots();
        for (int row = 0; row < INVENTORY_VISIBLE_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                int visibleIndex = (window.scrollRow + row) * INVENTORY_COLUMNS + column;
                if (visibleIndex >= inventorySlots.size()) {
                    continue;
                }

                int x = window.contentX() + column * SLOT_SIZE;
                int y = window.contentY() + row * SLOT_SIZE;
                this.renderSlot(graphics, inventorySlots.get(visibleIndex), x, y, mouseX, mouseY);
            }
        }

        this.renderScrollbar(graphics, window);
    }

    private void renderContainerWindow(GuiGraphicsExtractor graphics, InventoryWindow window, int mouseX, int mouseY) {
        List<Slot> slots = window.containerSlots();
        int minX = window.containerMinSlotX();
        int minY = window.containerMinSlotY();
        if (slots.isEmpty()) {
            graphics.text(this.font, "No item slots", window.contentX(), window.contentY(), COLOR_MUTED_TEXT, false);
            return;
        }

        for (Slot slot : slots) {
            int x = window.contentX() + slot.x - minX;
            int y = window.contentY() + slot.y - minY;
            this.renderSlot(graphics, slot, x, y, mouseX, mouseY);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, InventoryWindow window) {
        int maxScroll = this.maxInventoryScroll();
        int x = window.x + window.width - 12;
        int y = window.contentY();
        int height = INVENTORY_VISIBLE_ROWS * SLOT_SIZE;
        graphics.fill(x, y, x + 4, y + height, 0xFF20242C);
        int thumbHeight = maxScroll == 0 ? height : Math.max(12, height / (maxScroll + 1));
        int thumbY = maxScroll == 0 ? y : y + (height - thumbHeight) * window.scrollRow / maxScroll;
        graphics.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xFF96A0AF);
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
        graphics.fill(x - 1, y - 1, x + SLOT_ITEM_SIZE + 1, y + SLOT_ITEM_SIZE + 1, hovered ? COLOR_SLOT_HOVER : COLOR_SLOT_BORDER);
        graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, COLOR_SLOT);
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
        }
    }

    private void renderActiveHotbarOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Slot slot : this.hotbarSlots()) {
            int x = hotbarSlotX(slot.getContainerSlot());
            int y = hotbarY();
            boolean hovered = contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
            graphics.outline(x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, hovered ? 0xFFFFD166 : 0xFF94A3B8);
            graphics.outline(x - 2, y - 2, SLOT_SIZE + 2, SLOT_SIZE + 2, 0x88232A35);
            if (slot.hasItem() && hovered) {
                graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, 0x33FFFFFF);
            }
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

    private static void renderHotbarOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, AbstractContainerMenu menu, boolean highlight) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            int slotIndex = 36 + i;
            if (slotIndex >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.slots.get(slotIndex);
            int x = hotbarSlotX(i);
            int y = hotbarY();
            boolean hovered = highlight && contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE);
            graphics.outline(x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, hovered ? 0xFFFFD166 : 0xFF94A3B8);
            graphics.outline(x - 2, y - 2, SLOT_SIZE + 2, SLOT_SIZE + 2, 0x88232A35);
            if (slot.hasItem() && hovered) {
                graphics.fill(x, y, x + SLOT_ITEM_SIZE, y + SLOT_ITEM_SIZE, 0x33FFFFFF);
            }
        }
    }

    private static void renderCarried(GuiGraphicsExtractor graphics, ItemStack carried, int mouseX, int mouseY, Minecraft minecraft) {
        if (!carried.isEmpty()) {
            graphics.nextStratum();
            graphics.item(carried, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(minecraft.font, carried, mouseX - 8, mouseY - 8);
        }
    }

    private static @Nullable Slot hotbarSlotAt(AbstractContainerMenu menu, double mouseX, double mouseY) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            int slotIndex = 36 + i;
            if (slotIndex >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.slots.get(slotIndex);
            int x = hotbarSlotX(i);
            int y = hotbarY();
            if (contains(mouseX, mouseY, x - 1, y - 1, SLOT_SIZE, SLOT_SIZE)) {
                return slot;
            }
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
        CLOSE("x");

        private final String label;

        WindowControl(String label) {
            this.label = label;
        }
    }

    private record SlotHit(Slot slot, int slotId, int x, int y, AbstractContainerMenu menu, int sessionId) {
    }

    private static final class InventoryWindow {
        private final WindowKind kind;
        private final Component title;
        private int x;
        private int y;
        private final int width;
        private final int height;
        private final @Nullable DesktopContainerSession session;
        private final @Nullable AbstractContainerMenu legacyMenu;
        private final List<Slot> legacyContainerSlots;
        private final int legacyMinSlotX;
        private final int legacyMinSlotY;
        private boolean minimized;
        private boolean focused;
        private int scrollRow;

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
            return this.x + 8;
        }

        private int contentY() {
            return this.y + TOP_BAR_HEIGHT + 8;
        }

        private boolean contains(double mouseX, double mouseY) {
            int visibleHeight = this.minimized ? TOP_BAR_HEIGHT : this.height;
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, visibleHeight);
        }

        private boolean isTopBar(double mouseX, double mouseY) {
            return InventoryDesktopScreen.contains(mouseX, mouseY, this.x, this.y, this.width, TOP_BAR_HEIGHT);
        }

        private int controlX(WindowControl control) {
            int fromRight = switch (control) {
                case CLOSE -> 1;
                case MINIMIZE -> 2;
                case FOCUS -> 3;
            };
            return this.x + this.width - fromRight * CONTROL_SIZE - fromRight * CONTROL_GAP;
        }

        private @Nullable WindowControl controlAt(double mouseX, double mouseY) {
            for (WindowControl control : WindowControl.values()) {
                if (InventoryDesktopScreen.contains(mouseX, mouseY, this.controlX(control), this.y + 3, CONTROL_SIZE, CONTROL_SIZE)) {
                    return control;
                }
            }

            return null;
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

        private @Nullable SlotHit slotAt(InventoryDesktopScreen screen, double mouseX, double mouseY) {
            if (this.kind == WindowKind.INVENTORY) {
                List<Slot> inventorySlots = screen.mainInventorySlots();
                AbstractContainerMenu playerMenu = screen.playerMenu();
                for (int row = 0; row < INVENTORY_VISIBLE_ROWS; row++) {
                    for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                        int visibleIndex = (this.scrollRow + row) * INVENTORY_COLUMNS + column;
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
