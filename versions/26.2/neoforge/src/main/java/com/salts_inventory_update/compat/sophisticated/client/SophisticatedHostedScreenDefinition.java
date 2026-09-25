package com.salts_inventory_update.compat.sophisticated.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopResizePolicy;
import com.salts_inventory_update.api.client.desktop.DesktopSlotContext;
import com.salts_inventory_update.api.client.desktop.DesktopSlotHit;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgets;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.InternalDesktopRenderContext;
import com.salts_inventory_update.client.InternalDesktopWindowDefinition;
import com.salts_inventory_update.client.InternalDesktopWindowRegion;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTabControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

/**
 * Composes the native Sophisticated controls into one Salt window. The official screen remains the behavior
 * owner, but its background, title, player inventory, and complete render pass are never hosted.
 */
public final class SophisticatedHostedScreenDefinition<T extends AbstractContainerMenu>
    implements DesktopWindowDefinition<T, SophisticatedHostedScreenDefinition.State>, InternalDesktopWindowDefinition {
    private static final int SALT_HORIZONTAL_CHROME = 16;
    private static final int SALT_VERTICAL_CHROME = 32;
    private static final int SLOT = DesktopWidgets.SLOT_SIZE;
    private static final int TOOLBAR_HEIGHT = 14;
    private static final int TOOLBAR_GAP = 3;
    private static final int TOOLBAR_CONTROL_GAP = 2;
    private static final int STANDARD_HEADER_RAISE = 8;
    private static final int SCROLLBAR_GAP = 2;
    private static final int SCROLLBAR_RESERVE = DesktopWidgets.SCROLLBAR_BACKGROUND_WIDTH + SCROLLBAR_GAP;
    private static final int MIN_CONTENT_WIDTH = 176;
    private static final int STANDARD_MIN_CONTENT_WIDTH = 9 * SLOT;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int DEFAULT_DESKTOP_MARGIN = 8;
    private static final int NATIVE_STORAGE_X = 7;
    private static final int NATIVE_STORAGE_Y = 17;
    private static final int INVENTORY_CONTROL_COLUMN_WIDTH = 36;
    private static final int INVENTORY_CONTROL_RENDER_X = 9;
    private static final int INVENTORY_CONTROL_RENDER_WIDTH = 18;
    private static final int LIMITED_BODY_TOP_CROP = 17;
    private static final int UPGRADE_RAIL_SLOT_GAP = 2;
    private static final int UPGRADE_RAIL_FRAME_PADDING = 5;
    private static final int UPGRADE_RAIL_FRAME_OVERLAP = 5;
    private static final int RIGHT_TAB_STRIDE = Tab.DEFAULT_HEIGHT + 1;
    private static final int HIDDEN_SLOT = -10_000;
    private static final Identifier SALT_WINDOW_TEXTURE = Identifier.fromNamespaceAndPath(
        "salts_inventory_update",
        "textures/gui/window.png"
    );

    private final LayoutKind kind;
    private final ScreenFactory<T> factory;

    public SophisticatedHostedScreenDefinition(LayoutKind kind, ScreenFactory<T> factory) {
        this.kind = kind;
        this.factory = factory;
    }

    @Override
    public State createState(DesktopWindowSetupContext<T> context) {
        AbstractContainerScreen<?> screen = this.factory.create(
            context.menu(),
            context.minecraft().player.getInventory(),
            context.originalTitle()
        );
        int width = context.minecraft().getWindow().getGuiScaledWidth();
        int height = context.minecraft().getWindow().getGuiScaledHeight();
        screen.init(width, height);
        if (!(screen instanceof SophisticatedScreenAccess access)) {
            throw new IllegalStateException("Sophisticated screen mixin was not applied to " + screen.getClass().getName());
        }
        State state = new State(this.kind, screen, access, width, height);
        state.captureNativeGeometry(context.menu());
        state.access.disableNativeInventoryScrollPanel();
        return state;
    }

    @Override
    public DesktopWindowSize defaultSize(DesktopWindowSetupContext<T> context) {
        if (this.kind == LayoutKind.STANDARD && context.menu() instanceof StorageContainerMenuBase<?> storage) {
            int rows = Math.max(1, storage.getNumberOfRows());
            int columns = Math.max(1, (storage.getNumberOfStorageInventorySlots() + rows - 1) / rows);
            int visibleRows = defaultVisibleRows(context, rows);
            int inventoryControlWidth = Math.max(0, storage.getColumnsTaken()) * SLOT;
            int contentWidth = standardContentWidth(columns, rows > visibleRows, inventoryControlWidth);
            int contentHeight = standardContentHeight(visibleRows);
            return saltSize(contentWidth, contentHeight);
        }
        if (context.menu() instanceof SettingsContainerMenu<?> settings) {
            if (this.kind == LayoutKind.LIMITED_BARREL_SETTINGS) {
                return saltSize(MIN_CONTENT_WIDTH, 92);
            }
            int rows = Math.max(1, settings.getNumberOfRows());
            int columns = Math.max(1, (settings.getNumberOfStorageInventorySlots() + rows - 1) / rows);
            return saltSize(columns * SLOT, rows * SLOT);
        }
        return saltSize(MIN_CONTENT_WIDTH, 92);
    }

    @Override
    public DesktopWindowSize minSize(DesktopWindowContext<T, State> context) {
        DesktopWindowSize base = this.kind == LayoutKind.STANDARD
            ? saltSize(STANDARD_MIN_CONTENT_WIDTH, standardContentHeight(MIN_VISIBLE_ROWS))
            : context.state().fixedSize;
        return sizeWithClosedTabs(context, context.state(), base);
    }

    @Override
    public DesktopResizePolicy resizePolicy(DesktopWindowContext<T, State> context) {
        return this.kind == LayoutKind.STANDARD ? DesktopResizePolicy.STORAGE_GRID : DesktopResizePolicy.FIXED;
    }

    @Override
    public @Nullable DesktopWindowSize snapSize(DesktopWindowContext<T, State> context) {
        if (this.kind != LayoutKind.STANDARD) {
            return null;
        }
        int inventoryControlWidth = inventoryControlReserve(context.state());
        int requestedRows = Math.max(
            MIN_VISIBLE_ROWS,
            Math.round((context.contentHeight() + STANDARD_HEADER_RAISE - TOOLBAR_HEIGHT - TOOLBAR_GAP) / (float) SLOT)
        );
        int storageSlotCount = context.menu() instanceof StorageContainerMenuBase<?> storage
            ? allStorageSlots(context.state(), storage).size()
            : context.containerSlots().size();
        int availableWidth = Math.max(SLOT, context.contentWidth() - inventoryControlWidth);
        // First test the closest no-scroll grid. A previously reserved 16-pixel gutter can then
        // become a real column when that is the nearest useful snap, instead of remaining blank.
        int columns = Math.max(1, Math.round(availableWidth / (float) SLOT));
        int totalRows = DesktopWidgets.rowsForCount(storageSlotCount, columns);
        boolean scrolling = totalRows > requestedRows;
        if (scrolling) {
            columns = Math.max(1, Math.round((availableWidth - SCROLLBAR_RESERVE) / (float) SLOT));
            totalRows = DesktopWidgets.rowsForCount(storageSlotCount, columns);
        }
        int rows = Math.min(requestedRows, Math.max(MIN_VISIBLE_ROWS, totalRows));
        scrolling = totalRows > rows;
        int contentWidth = standardContentWidth(columns, scrolling, inventoryControlWidth);
        int contentHeight = standardContentHeight(rows);
        return saltSize(contentWidth, contentHeight);
    }

    @Override
    public @Nullable DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> context) {
        if (this.kind == LayoutKind.STANDARD) {
            if (!(context.state() instanceof State state)) {
                return null;
            }
            DesktopWindowSize desired = null;
            if (!state.legacyScrollbarGutterMigrationPending) {
                return desiredSizeWithClosedTabs(context, state, null);
            }
            Layout layout = state.layout;
            if (layout == null) {
                return desiredSizeWithClosedTabs(context, state, null);
            }
            state.legacyScrollbarGutterMigrationPending = false;
            if (layout.scrollbar != null) {
                return desiredSizeWithClosedTabs(context, state, null);
            } else {
                // Remove either known obsolete compact remainder: the old 16-pixel scrollbar gutter or
                // Sophisticated's doubled 7-pixel native side margins. Preserve every other saved size.
                int occupiedContentWidth = layout.grid.getWidth() + inventoryControlReserve(state);
                int unusedWidth = context.contentWidth() - occupiedContentWidth;
                boolean obsoleteScrollbarGutter = unusedWidth == SCROLLBAR_RESERVE;
                boolean duplicatedNativeMargins = occupiedContentWidth == STANDARD_MIN_CONTENT_WIDTH
                    && context.contentWidth() == MIN_CONTENT_WIDTH;
                if ((obsoleteScrollbarGutter || duplicatedNativeMargins)
                    && context.contentWidth() - unusedWidth >= STANDARD_MIN_CONTENT_WIDTH) {
                    desired = DesktopWindowSize.of(
                    context.windowWidth() - unusedWidth,
                    context.windowHeight()
                    );
                }
            }
            return desiredSizeWithClosedTabs(context, state, desired);
        }
        return context.state() instanceof State state
            ? sizeWithClosedTabs(context, state, state.fixedSize)
            : null;
    }

    @Override
    public List<InternalDesktopWindowRegion> externalRegions(DesktopWindowContext<?, ?> context) {
        if (!(context.state() instanceof State state)) {
            return List.of();
        }
        updateLayout(context);
        return state.externalRegions;
    }

    @Override
    public void opened(DesktopWindowContext<T, State> context) {
        updateLayout(context);
    }

    @Override
    public void moved(DesktopWindowContext<T, State> context) {
        updateLayout(context);
    }

    @Override
    public void resized(DesktopWindowContext<T, State> context) {
        updateLayout(context);
    }

    @Override
    public void closed(DesktopWindowContext<T, State> context) {
        context.state().screen.removed();
    }

    @Override
    public void tick(DesktopWindowContext<T, State> context) {
        State state = context.state();
        Minecraft minecraft = context.minecraft();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (state.screenWidth != width || state.screenHeight != height) {
            state.screen.resize(width, height);
            state.screenWidth = width;
            state.screenHeight = height;
            state.captureNativeGeometry(context.menu());
            state.access.disableNativeInventoryScrollPanel();
        }
        state.screen.tick();
        updateLayout(context);
    }

    @Override
    public void renderBehindFrame(DesktopRenderContext<?, ?> context) {
        if (!(context instanceof InternalDesktopRenderContext internal)) {
            return;
        }
        updateLayout(context);
        if (!(context.state() instanceof State state)) {
            return;
        }
        Rect2i leftRailFrame = upgradeRailFrame(
            context,
            state.layout == null ? List.of() : state.layout.slots,
            state.access.components().upgradeSwitches()
        );
        if (leftRailFrame == null) {
            leftRailFrame = settingsTemplateRailFrame(
                context,
                state,
                state.access.components().templatePersistence()
            );
        }
        if (leftRailFrame != null) {
            context.windowNineSlice(
                SALT_WINDOW_TEXTURE,
                leftRailFrame.getX(),
                leftRailFrame.getY(),
                leftRailFrame.getWidth(),
                leftRailFrame.getHeight()
            );
        }
        WidgetBase sideTabs = state.access.components().sideTabs();
        if (sideTabs != null) {
            for (Rect2i rightTabFrame : rightTabFrames(context, sideTabs)) {
                context.windowNineSlice(
                    SALT_WINDOW_TEXTURE,
                    rightTabFrame.getX(),
                    rightTabFrame.getY(),
                    rightTabFrame.getWidth(),
                    rightTabFrame.getHeight()
                );
            }
            SophisticatedHostedScreenBridge.withSaltTabBackgrounds(() ->
                sideTabs.extractRenderState(internal.rawGraphics(), context.mouseX(), context.mouseY(), 0.0F)
            );
        }
    }

    @Override
    public void render(DesktopRenderContext<T, State> context) {
        if (!(context instanceof InternalDesktopRenderContext internal)) {
            return;
        }
        updateLayout(context);
        State state = context.state();
        Layout layout = state.layout;
        if (layout == null) {
            return;
        }
        GuiGraphicsExtractor graphics = internal.rawGraphics();

        renderSlotBackgrounds(context, state, layout, graphics);
        if (layout.scrollbar != null) {
            DesktopWidgets.renderScrollbar(
                context,
                layout.scrollbar.getX(),
                layout.scrollbar.getY(),
                layout.scrollbar.getHeight(),
                state.scrollRow,
                layout.maxScroll
            );
        }

        for (PlacedSlot placed : layout.slots) {
            if (contains(placed.frame, context.mouseX(), context.mouseY()) && placed.slot.isHighlightable()) {
                context.slotHighlight(placed.itemX, placed.itemY);
            }
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(state.screen.getLeftPos(), state.screen.getTopPos());
        try {
            SophisticatedHostedScreenBridge.withQuickCraftPreview(
                state.screen,
                internal.quickCraftPreview(),
                () -> {
                    for (PlacedSlot placed : layout.slots) {
                        if (!replacesSlot(state.access, placed.slot)) {
                            state.access.extractSlotContents(graphics, placed.slot, context.mouseX(), context.mouseY());
                        }
                    }
                }
            );
            for (UpgradeInventoryControlBase control : state.access.storageInventoryControls()) {
                control.extract(graphics, context.mouseX(), context.mouseY());
            }
            for (UpgradeInventoryControlBase control : state.access.upgradeInventoryControls()) {
                control.extract(graphics, context.mouseX(), context.mouseY());
            }
        } finally {
            graphics.pose().popMatrix();
        }

        // StorageScreenBase's color-overlay pass applies guiLeft/guiTop itself and the error pass is
        // normally extracted after the native contents pose has been popped. Keeping either call in
        // the translated slot-content pose would offset it a second time inside a hosted window.
        state.access.extractSlotColorOverlays(graphics);
        state.access.extractUpgradeErrors(graphics);

        SophisticatedScreenAccess.Components components = state.access.components();
        Set<WidgetBase> rendered = Collections.newSetFromMap(new IdentityHashMap<>());
        for (WidgetBase widget : components.layoutWidgets()) {
            if (widget == components.sideTabs() || widget == components.modal() || !rendered.add(widget)) {
                continue;
            }
            widget.extractRenderState(graphics, context.mouseX(), context.mouseY(), 0.0F);
        }
        if (components.sideTabs() instanceof UpgradeSettingsTabControl tabs) {
            tabs.extractForeground(graphics, context.mouseX(), context.mouseY(), 0.0F);
        }
        if (components.modal() != null) {
            context.fill(
                context.windowX(),
                context.windowY(),
                context.windowX() + context.windowWidth(),
                context.windowY() + context.windowHeight(),
                0x99000000
            );
            components.modal().extractRenderStateInLatePass(graphics, context.mouseX(), context.mouseY(), 0.0F);
        }
    }

    @Override
    public void updateTooltipState(
        DesktopWindowContext<?, ?> context,
        int mouseX,
        int mouseY,
        boolean active
    ) {
        if (!(context.state() instanceof State state)) {
            return;
        }
        updateLayout(context);
        Layout layout = state.layout;
        PlacedSlot hovered = active && layout != null && layout.containsVisual(mouseX, mouseY)
            ? interactiveSlotAt(state, layout, mouseX, mouseY)
            : null;
        SophisticatedHostedScreenBridge.setHoveredSlot(
            state.screen,
            hovered == null ? null : hovered.slot
        );
        if (!active || layout == null || !layout.containsVisual(mouseX, mouseY)) {
            dismissTemplateInputs(state);
        }
    }

    @Override
    public boolean appendTooltip(DesktopRenderContext<T, State> context, int mouseX, int mouseY) {
        if (!(context instanceof InternalDesktopRenderContext internal)) {
            return false;
        }
        updateLayout(context);
        Layout layout = context.state().layout;
        if (layout == null || !layout.containsVisual(mouseX, mouseY)) {
            return false;
        }
        SophisticatedHostedScreenBridge.withRendering(0, 0, () ->
            context.state().access.extractNativeTooltip(internal.rawGraphics(), mouseX, mouseY)
        );
        return true;
    }

    @Override
    public @Nullable DesktopSlotHit slotAt(DesktopSlotContext<T, State> context, double mouseX, double mouseY) {
        updateLayout(context);
        Layout layout = context.state().layout;
        if (layout == null) {
            return null;
        }
        PlacedSlot placed = interactiveSlotAt(context.state(), layout, mouseX, mouseY);
        if (placed == null || replacesSlot(context.state().access, placed.slot)) {
            return null;
        }
        int slotId = context.menuSlotId(placed.slot);
        return slotId < 0 ? null : DesktopSlotHit.of(slotId, placed.itemX, placed.itemY);
    }

    @Override
    public boolean mouseClicked(DesktopInputContext<T, State> context, MouseButtonEvent event, boolean doubleClick) {
        updateLayout(context);
        State state = context.state();
        Layout layout = state.layout;
        if (layout == null) {
            return false;
        }
        if (layout.scrollbar != null && contains(layout.scrollbar, event.x(), event.y())) {
            if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return true;
            }
            state.nativePointerOwner = null;
            state.nativeInventoryControlOwner = null;
            state.scrollbarDragging = true;
            updateScrollFromPointer(state, layout, event.y());
            return true;
        }

        WidgetBase templatePersistence = state.access.components().templatePersistence();
        WidgetBase templateTarget = deepestWidgetAt(templatePersistence, event.x(), event.y());
        if (templateTarget != null) {
            if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
                return true;
            }
            boolean consumed = SophisticatedHostedScreenBridge.withInput(
                context,
                () -> templateTarget.mouseClicked(event, doubleClick)
            );
            state.nativePointerActive = consumed;
            state.nativePointerOwner = consumed ? templateTarget : null;
            state.nativeInventoryControlOwner = null;
            if (consumed && templateTarget instanceof TextBox) {
                state.screen.setFocused(templateTarget);
            }
            return consumed;
        }

        PlacedSlot placed = interactiveSlotAt(state, layout, event.x(), event.y());
        if (placed != null
            && !this.kind.settings()
            && !replacesSlot(state.access, placed.slot)) {
            unfocusSearch(state);
            return false;
        }
        if (placed == null && contains(layout.grid, event.x(), event.y())) {
            // Empty cells in a resized or filtered final row belong to Salt's grid, not the
            // centered native screen. Forwarding them can make the native coordinates look like
            // an outside click and drop the carried stack from a moved window.
            unfocusSearch(state);
            state.nativePointerActive = false;
            state.nativePointerOwner = null;
            state.nativeInventoryControlOwner = null;
            return true;
        }
        if (!layout.containsInteractive(event.x(), event.y())) {
            unfocusSearch(state);
            return false;
        }
        UpgradeInventoryControlBase inventoryControl = inventoryControlAt(state, layout, event.x(), event.y());
        if (inventoryControl != null) {
            // Tank/battery columns perform their action from StorageScreenBase.mouseReleased(). Their
            // press is deliberately not sent through AbstractContainerScreen: a detached column can
            // be outside the native screen rectangle, where the vanilla press path means "drop the
            // carried stack outside" before the column gets its release callback.
            unfocusSearch(state);
            if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
                return true;
            }
            state.nativePointerOwner = null;
            state.nativeInventoryControlOwner = inventoryControl;
            state.nativePointerActive = true;
            return true;
        }
        WidgetBase searchBox = state.access.components().search();
        if (deepestWidgetAt(searchBox, event.x(), event.y()) == null
            && !DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            return true;
        }
        boolean consumed = SophisticatedHostedScreenBridge.withInput(
            context,
            () -> state.screen.mouseClicked(event, doubleClick)
        );
        state.nativePointerActive = consumed;
        state.nativePointerOwner = null;
        state.nativeInventoryControlOwner = null;
        return consumed;
    }

    @Override
    public boolean mouseReleased(DesktopInputContext<T, State> context, MouseButtonEvent event) {
        State state = context.state();
        boolean scrollbarWasDragging = state.scrollbarDragging;
        state.scrollbarDragging = false;
        if (scrollbarWasDragging) {
            return true;
        }
        if (!state.nativePointerActive) {
            return false;
        }
        GuiEventListener pointerOwner = state.nativePointerOwner;
        UpgradeInventoryControlBase inventoryControlOwner = state.nativeInventoryControlOwner;
        state.nativePointerActive = false;
        state.nativePointerOwner = null;
        state.nativeInventoryControlOwner = null;
        if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            SophisticatedHostedScreenBridge.cancelQuickCraft(state.screen);
            return true;
        }
        if (inventoryControlOwner != null) {
            // A detached tank/battery control owns this release. Calling the complete screen here
            // can fall through to vanilla's outside-click handling and drop the carried stack.
            SophisticatedHostedScreenBridge.withInput(context, () -> {
                inventoryControlOwner.handleMouseReleased(event);
                return true;
            });
            return true;
        }
        return SophisticatedHostedScreenBridge.withInput(context, () ->
            pointerOwner == null ? state.screen.mouseReleased(event) : pointerOwner.mouseReleased(event)
        );
    }

    @Override
    public void cancelPointer(DesktopWindowContext<?, ?> context) {
        if (!(context.state() instanceof State state)) {
            return;
        }
        state.scrollbarDragging = false;
        state.nativePointerActive = false;
        state.nativePointerOwner = null;
        state.nativeInventoryControlOwner = null;
        SophisticatedHostedScreenBridge.cancelQuickCraft(state.screen);
    }

    @Override
    public boolean mouseDragged(DesktopInputContext<T, State> context, MouseButtonEvent event, double dx, double dy) {
        State state = context.state();
        Layout layout = state.layout;
        if (state.scrollbarDragging && layout != null) {
            updateScrollFromPointer(state, layout, event.y());
            return true;
        }
        if (!state.nativePointerActive) {
            return false;
        }
        if (state.nativeInventoryControlOwner != null) {
            // These controls commit on release and have no drag gesture. Never let the complete
            // native screen reinterpret an in-between drag as a container/outside interaction.
            return true;
        }
        if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            state.nativePointerActive = false;
            state.nativePointerOwner = null;
            state.nativeInventoryControlOwner = null;
            SophisticatedHostedScreenBridge.cancelQuickCraft(state.screen);
            return true;
        }
        GuiEventListener pointerOwner = state.nativePointerOwner;
        return SophisticatedHostedScreenBridge.withInput(
            context,
            () -> pointerOwner == null
                ? state.screen.mouseDragged(event, dx, dy)
                : pointerOwner.mouseDragged(event, dx, dy)
        );
    }

    @Override
    public boolean mouseScrolled(
        DesktopInputContext<T, State> context,
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY
    ) {
        updateLayout(context);
        State state = context.state();
        Layout layout = state.layout;
        if (layout == null || !layout.containsInteractive(mouseX, mouseY)) {
            return false;
        }
        if (this.kind == LayoutKind.STANDARD
            && (contains(layout.grid, mouseX, mouseY)
                || layout.scrollbar != null && contains(layout.scrollbar, mouseX, mouseY))) {
            int next = DesktopWidgets.scrollByWheel(state.scrollRow, layout.maxScroll, scrollY);
            if (next != state.scrollRow) {
                state.scrollRow = next;
                updateLayout(context);
            }
            return true;
        }
        WidgetBase templateTarget = deepestWidgetAt(state.access.components().templatePersistence(), mouseX, mouseY);
        if (templateTarget != null) {
            if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
                return true;
            }
            return SophisticatedHostedScreenBridge.withInput(
                context,
                () -> templateTarget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
            );
        }
        if (!DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            return true;
        }
        return SophisticatedHostedScreenBridge.withInput(
            context,
            () -> state.screen.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        );
    }

    @Override
    public boolean keyPressed(DesktopInputContext<T, State> context, KeyEvent event) {
        if (!context.focused()) {
            return false;
        }
        AbstractContainerScreen<?> screen = context.state().screen;
        if (screen.getFocused() == null && context.minecraft().options.keyInventory.matches(event)) {
            return false;
        }
        if (event.isEscape() && !(screen instanceof SettingsScreen) && screen.getFocused() == null) {
            if (!(context.menu() instanceof StorageContainerMenuBase<?> storage) || storage.isFirstLevelStorage()) {
                return false;
            }
        }
        if (!widgetTreeContains(context.state().access.components().search(), screen.getFocused())
            && !DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            return true;
        }
        return SophisticatedHostedScreenBridge.withInput(context, () -> screen.keyPressed(event));
    }

    @Override
    public boolean charTyped(DesktopInputContext<T, State> context, CharacterEvent event) {
        if (!context.focused()) {
            return false;
        }
        if (!widgetTreeContains(context.state().access.components().search(), context.state().screen.getFocused())
            && !DesktopContainerClient.canDispatchOptimisticMutationNow()) {
            return true;
        }
        return SophisticatedHostedScreenBridge.withInput(context, () -> context.state().screen.charTyped(event));
    }

    @Override
    public boolean wantsTextInput(DesktopWindowContext<T, State> context) {
        if (!context.focused()) {
            return false;
        }
        GuiEventListener focused = context.state().screen.getFocused();
        Set<GuiEventListener> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (focused instanceof ContainerEventHandler container && visited.add(focused)) {
            GuiEventListener child = container.getFocused();
            if (child == null) {
                break;
            }
            focused = child;
        }
        return focused instanceof EditBox || focused instanceof TextBox;
    }

    @Override
    public void customPayload(DesktopWindowContext<T, State> context, Identifier channel, byte[] data) {
        SophisticatedHostedScreenPayloads.apply(context.minecraft(), context.menu(), channel, data);
    }

    private void updateLayout(DesktopWindowContext<?, ?> context) {
        if (!(context.state() instanceof State state)) {
            return;
        }
        state.access.disableNativeInventoryScrollPanel();
        state.refreshNativeGeometry((AbstractContainerMenu) context.menu());
        hidePlayerSlots(state);
        Layout layout = switch (this.kind) {
            case STANDARD -> standardLayout(context, state);
            case LIMITED_BARREL -> fixedStorageLayout(context, state, true);
            case SETTINGS -> settingsLayout(context, state, false);
            case LIMITED_BARREL_SETTINGS -> settingsLayout(context, state, true);
        };
        state.layout = layout;
        state.externalRegions = layout.externalRegions;
    }

    private static Layout standardLayout(DesktopWindowContext<?, ?> context, State state) {
        if (!(context.menu() instanceof StorageContainerMenuBase<?> menu)
            || !(state.screen instanceof StorageScreenBase<?> storageScreen)) {
            return Layout.empty(context);
        }
        List<Slot> storageSlots = filteredStorageSlots(state, menu, storageScreen.getStackFilter());
        hideSlots(state, storageSlots);
        for (Slot slot : allStorageSlots(state, menu)) {
            hideSlot(state, slot);
        }

        int gridX = context.contentX();
        int gridY = standardToolbarY(context) + TOOLBAR_HEIGHT + TOOLBAR_GAP;
        int availableHeight = Math.max(
            SLOT,
            context.contentHeight() + STANDARD_HEADER_RAISE - TOOLBAR_HEIGHT - TOOLBAR_GAP
        );
        int visibleRows = Math.max(1, availableHeight / SLOT);
        int inventoryControlWidth = inventoryControlReserve(state);
        int availableWidth = Math.max(SLOT, context.contentWidth() - inventoryControlWidth);
        int columns = Math.max(1, availableWidth / SLOT);
        int totalRows = DesktopWidgets.rowsForCount(storageSlots.size(), columns);
        boolean scrolling = totalRows > visibleRows;
        if (scrolling) {
            columns = Math.max(1, (availableWidth - SCROLLBAR_RESERVE) / SLOT);
            totalRows = DesktopWidgets.rowsForCount(storageSlots.size(), columns);
        }
        int maxScroll = Math.max(0, totalRows - visibleRows);
        state.scrollRow = DesktopWidgets.clamp(state.scrollRow, 0, maxScroll);
        int first = state.scrollRow * columns;
        int visibleCount = Math.min(storageSlots.size() - Math.min(first, storageSlots.size()), columns * visibleRows);
        storageScreen.setVisibleSlotsCount(Math.max(0, visibleCount));

        List<PlacedSlot> placed = new ArrayList<>();
        for (int visual = 0; visual < visibleCount; visual++) {
            Slot slot = storageSlots.get(first + visual);
            int frameX = gridX + visual % columns * SLOT;
            int frameY = gridY + visual / columns * SLOT;
            placeSlot(state, placed, slot, frameX, frameY, SlotGroup.MAIN, false);
        }

        // The native constants identify the 18-by-18 background origin; its item origin is +1.
        int nativeGridX = state.screen.getLeftPos() + NATIVE_STORAGE_X + 1;
        int nativeGridY = state.screen.getTopPos() + NATIVE_STORAGE_Y + 1;
        int baseDx = gridX - nativeGridX;
        int baseDy = gridY - nativeGridY;
        RailTranslation rail = placeUpgradeRail(context, state, menu, placed);
        WidgetPlacement widgets = positionWidgets(context, state, placed, gridX, gridY, columns, visibleRows, rail);
        Set<Slot> panelSlots = placeUpgradePanelSlots(
            state,
            menu,
            placed,
            widgets.sideMoveDx,
            widgets.sideMoveDy
        );
        placeExtraSlots(state, menu, placed, panelSlots, baseDx, baseDy);

        Rect2i grid = new Rect2i(gridX, gridY, columns * SLOT, visibleRows * SLOT);
        Rect2i scrollbar = scrolling
            ? new Rect2i(gridX + columns * SLOT + SCROLLBAR_GAP, gridY, DesktopWidgets.SCROLLBAR_BACKGROUND_WIDTH, visibleRows * SLOT)
            : null;
        int inventoryControlX = gridX + columns * SLOT + (scrolling ? SCROLLBAR_RESERVE : 1);
        List<Rect2i> inventoryControls = positionInventoryControls(state, inventoryControlX, gridY + 1);
        return finishLayout(context, state, grid, scrollbar, maxScroll, placed, widgets, inventoryControls);
    }

    private static Layout fixedStorageLayout(DesktopWindowContext<?, ?> context, State state, boolean limited) {
        if (!(context.menu() instanceof StorageContainerMenuBase<?> menu)) {
            return Layout.empty(context);
        }
        List<PlacedSlot> placed = new ArrayList<>();
        int bodyX = context.contentX();
        int bodyY = limited ? context.contentY() - LIMITED_BODY_TOP_CROP : context.contentY();
        int baseDx = bodyX - state.screen.getLeftPos();
        int baseDy = bodyY - state.screen.getTopPos();
        for (Slot slot : allStorageSlots(state, menu)) {
            SlotPosition original = state.originalSlots.get(slot);
            if (original == null) {
                continue;
            }
            placeSlot(
                state,
                placed,
                slot,
                original.itemX + baseDx,
                original.itemY + baseDy,
                SlotGroup.MAIN,
                limited
            );
        }
        RailTranslation rail = placeUpgradeRail(context, state, menu, placed);
        WidgetPlacement widgets = positionWidgets(context, state, placed, bodyX, context.contentY(), 1, 1, rail);
        Set<Slot> panelSlots = placeUpgradePanelSlots(
            state,
            menu,
            placed,
            widgets.sideMoveDx,
            widgets.sideMoveDy
        );
        placeExtraSlots(state, menu, placed, panelSlots, baseDx, baseDy);
        Rect2i grid = boundsOf(placed, SlotGroup.MAIN, new Rect2i(context.contentX(), context.contentY(), 1, 1));
        List<Rect2i> inventoryControls = positionInventoryControls(
            state,
            grid.getX() + grid.getWidth() + 1,
            grid.getY() + 1
        );
        return finishLayout(context, state, grid, null, 0, placed, widgets, inventoryControls);
    }

    private static Layout settingsLayout(DesktopWindowContext<?, ?> context, State state, boolean limited) {
        if (!(context.menu() instanceof SettingsContainerMenu<?> menu) || !(state.screen instanceof SettingsScreen screen)) {
            return Layout.empty(context);
        }
        for (Slot slot : menu.getStorageInventorySlots()) {
            hideSlot(state, slot);
        }
        List<PlacedSlot> placed = new ArrayList<>();
        int columns = Math.max(1, screen.getSlotsOnLine());
        int gridX = context.contentX();
        int gridY = context.contentY();
        int index = 0;
        for (Slot slot : menu.getStorageInventorySlots()) {
            int frameX;
            int frameY;
            if (limited) {
                SlotPosition original = state.originalSlots.get(slot);
                if (original == null) {
                    continue;
                }
                int dx = gridX - state.screen.getLeftPos();
                int dy = gridY - LIMITED_BODY_TOP_CROP - state.screen.getTopPos();
                frameX = original.itemX + dx;
                frameY = original.itemY + dy;
            } else {
                frameX = gridX + index % columns * SLOT;
                frameY = gridY + index / columns * SLOT;
            }
            placeSlot(state, placed, slot, frameX, frameY, SlotGroup.MAIN, limited);
            index++;
        }
        int rows = Math.max(1, DesktopWidgets.rowsForCount(menu.getStorageInventorySlots().size(), columns));
        WidgetPlacement widgets = positionWidgets(
            context,
            state,
            placed,
            gridX,
            gridY,
            columns,
            rows,
            RailTranslation.NONE
        );
        Rect2i grid = boundsOf(placed, SlotGroup.MAIN, new Rect2i(gridX, gridY, columns * SLOT, rows * SLOT));
        return finishLayout(context, state, grid, null, 0, placed, widgets, List.of());
    }

    private static WidgetPlacement positionWidgets(
        DesktopWindowContext<?, ?> context,
        State state,
        List<PlacedSlot> placed,
        int gridX,
        int gridY,
        int columns,
        int rows,
        RailTranslation rail
    ) {
        SophisticatedScreenAccess.Components components = state.access.components();
        int toolbarY = state.kind == LayoutKind.STANDARD ? standardToolbarY(context) : context.contentY();
        if (state.kind == LayoutKind.STANDARD) {
            positionStandardToolbar(context, state, components, toolbarY);
        } else {
            positionRightAlignedToolbar(context, state, components, toolbarY);
        }
        if (state.kind != LayoutKind.STANDARD && components.search() != null) {
            setWidgetPosition(
                state,
                components.search(),
                context.contentX(),
                toolbarY + Math.max(0, (TOOLBAR_HEIGHT - components.search().getHeight()) / 2)
            );
        }
        if (components.noResults() != null) {
            setWidgetPosition(
                state,
                components.noResults(),
                gridX + Math.max(0, (columns * SLOT - components.noResults().getWidth()) / 2),
                gridY + Math.max(0, (rows * SLOT - components.noResults().getHeight()) / 2)
            );
        }
        for (WidgetBase upgradeSwitch : components.upgradeSwitches()) {
            SlotPosition original = originalWidget(state, upgradeSwitch);
            setWidgetPosition(state, upgradeSwitch, original.itemX + rail.dx, rail.translateY(original.itemY));
        }

        int sideMoveDx = 0;
        int sideMoveDy = 0;
        if (components.sideTabs() != null) {
            int sideX = context.windowX() + context.windowWidth();
            int sideY = toolbarY;
            sideMoveDx = sideX - components.sideTabs().getX();
            sideMoveDy = sideY - components.sideTabs().getY();
            setWidgetPosition(state, components.sideTabs(), sideX, sideY);
        }
        if (components.templatePersistence() != null) {
            WidgetBase template = components.templatePersistence();
            if (state.kind.settings()) {
                setWidgetPosition(
                    state,
                    template,
                    context.windowX() - template.getWidth(),
                    context.contentY()
                );
            } else {
                int templateY = Math.min(
                    context.contentY() + context.contentHeight() - template.getHeight(),
                    gridY + rows * SLOT + 4
                );
                setWidgetPosition(state, template, context.contentX(), templateY);
            }
        }
        if (components.modal() != null) {
            setWidgetPosition(
                state,
                components.modal(),
                context.windowX() + (context.windowWidth() - components.modal().getWidth()) / 2,
                context.windowY() + (context.windowHeight() - components.modal().getHeight()) / 2
            );
        }
        return new WidgetPlacement(components, sideMoveDx, sideMoveDy);
    }

    private static void positionStandardToolbar(
        DesktopWindowContext<?, ?> context,
        State state,
        SophisticatedScreenAccess.Components components,
        int toolbarY
    ) {
        WidgetBase search = components.search();
        int nextX = context.contentX();
        boolean searchExpanded = false;
        if (search != null) {
            if (search instanceof SophisticatedSearchBoxAccess access) {
                access.salts_inventory_update$setHostedBounds(context.contentX(), context.contentWidth());
                searchExpanded = access.salts_inventory_update$isExpandedOrFocused();
            } else {
                searchExpanded = search.isFocused();
            }
            setWidgetPosition(
                state,
                search,
                context.contentX(),
                toolbarY + Math.max(0, (TOOLBAR_HEIGHT - search.getHeight()) / 2)
            );
            nextX += search.getWidth() + TOOLBAR_CONTROL_GAP;
        }

        WidgetBase[] controls = {
            components.sortMode(),
            components.sort(),
            components.transferToStorage(),
            components.transferToInventory()
        };
        for (WidgetBase widget : controls) {
            if (widget == null) {
                continue;
            }
            widget.setVisible(!searchExpanded);
            if (searchExpanded) {
                setWidgetPosition(state, widget, HIDDEN_SLOT, HIDDEN_SLOT);
                continue;
            }
            setWidgetPosition(
                state,
                widget,
                nextX,
                toolbarY + Math.max(0, (TOOLBAR_HEIGHT - widget.getHeight()) / 2)
            );
            nextX += widget.getWidth() + TOOLBAR_CONTROL_GAP;
        }
    }

    private static void positionRightAlignedToolbar(
        DesktopWindowContext<?, ?> context,
        State state,
        SophisticatedScreenAccess.Components components,
        int toolbarY
    ) {
        int right = context.contentX() + context.contentWidth();
        WidgetBase[] controls = {
            components.transferToInventory(),
            components.transferToStorage(),
            components.sort(),
            components.sortMode()
        };
        for (WidgetBase widget : controls) {
            if (widget == null) {
                continue;
            }
            widget.setVisible(true);
            right -= widget.getWidth();
            setWidgetPosition(
                state,
                widget,
                right,
                toolbarY + Math.max(0, (TOOLBAR_HEIGHT - widget.getHeight()) / 2)
            );
            right -= TOOLBAR_CONTROL_GAP;
        }
    }

    private static RailTranslation placeUpgradeRail(
        DesktopWindowContext<?, ?> context,
        State state,
        StorageContainerMenuBase<?> menu,
        List<PlacedSlot> placed
    ) {
        int railSlotCount = Math.min(menu.getNumberOfUpgradeSlots(), menu.upgradeSlots.size());
        if (railSlotCount <= 0) {
            return RailTranslation.NONE;
        }
        List<Slot> railSlots = menu.upgradeSlots.subList(0, railSlotCount);
        Rect2i originalBounds = originalBounds(state, railSlots);
        int dx = context.windowX() - originalBounds.getX() - originalBounds.getWidth();
        int railY = state.kind == LayoutKind.STANDARD ? standardToolbarY(context) : context.contentY();
        int dy = railY - originalBounds.getY();
        List<RailRowTranslation> rows = new ArrayList<>(railSlotCount);
        for (int index = 0; index < railSlots.size(); index++) {
            Slot slot = railSlots.get(index);
            SlotPosition original = state.originalSlots.get(slot);
            if (original != null) {
                int itemY = railY + 1 + index * (SLOT + UPGRADE_RAIL_SLOT_GAP);
                rows.add(new RailRowTranslation(original.itemY, itemY - original.itemY));
                placeSlot(
                    state,
                    placed,
                    slot,
                    original.itemX + dx,
                    itemY,
                    SlotGroup.UPGRADE,
                    false
                );
            }
        }
        return new RailTranslation(dx, dy, List.copyOf(rows));
    }

    private static Set<Slot> placeUpgradePanelSlots(
        State state,
        StorageContainerMenuBase<?> menu,
        List<PlacedSlot> placed,
        int sideMoveDx,
        int sideMoveDy
    ) {
        Set<Slot> panelSlots = Collections.newSetFromMap(new IdentityHashMap<>());
        UpgradeContainerBase<?, ?> openContainer = menu.getOpenContainer().orElse(null);
        Set<UpgradeContainerBase<?, ?>> containers = Collections.newSetFromMap(new IdentityHashMap<>());
        containers.addAll(menu.getUpgradeContainers().values());
        for (UpgradeContainerBase<?, ?> container : containers) {
            boolean open = container == openContainer;
            for (Slot slot : container.getSlots()) {
                if (!panelSlots.add(slot)) {
                    continue;
                }
                // UpgradeSettingsTab owns these coordinates. Closed tabs park them at -2000;
                // moving the tab itself does not move them, so apply only this layout pass's
                // actual tab movement to the native open-tab position.
                if (!open || slot.x <= -1000 || slot.y <= -1000) {
                    hideSlot(state, slot);
                    continue;
                }
                placeSlot(
                    state,
                    placed,
                    slot,
                    state.screen.getLeftPos() + slot.x + sideMoveDx,
                    state.screen.getTopPos() + slot.y + sideMoveDy,
                    SlotGroup.PANEL,
                    true
                );
            }
        }
        return panelSlots;
    }

    private static void placeExtraSlots(
        State state,
        StorageContainerMenuBase<?> menu,
        List<PlacedSlot> placed,
        Set<Slot> panelSlots,
        int fallbackDx,
        int fallbackDy
    ) {
        Set<Slot> existing = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PlacedSlot slot : placed) {
            existing.add(slot.slot);
        }
        for (Slot slot : menu.getExtraSlots()) {
            if (panelSlots.contains(slot) || !existing.add(slot)) {
                continue;
            }
            SlotPosition original = state.originalSlots.get(slot);
            if (original == null) {
                continue;
            }
            placeSlot(
                state,
                placed,
                slot,
                original.itemX + fallbackDx,
                original.itemY + fallbackDy,
                SlotGroup.EXTRA,
                false
            );
        }
    }

    private static Layout finishLayout(
        DesktopWindowContext<?, ?> context,
        State state,
        Rect2i grid,
        @Nullable Rect2i scrollbar,
        int maxScroll,
        List<PlacedSlot> placed,
        WidgetPlacement widgetPlacement,
        List<Rect2i> inventoryControls
    ) {
        List<Rect2i> interactive = new ArrayList<>();
        List<Rect2i> visual = new ArrayList<>();
        interactive.add(grid);
        visual.add(grid);
        if (scrollbar != null) {
            interactive.add(scrollbar);
            visual.add(scrollbar);
        }
        for (PlacedSlot slot : placed) {
            interactive.add(slot.frame);
            visual.add(slot.frame);
        }
        interactive.addAll(inventoryControls);
        visual.addAll(inventoryControls);
        for (GuiEventListener listener : widgetPlacement.components.interactiveWidgets()) {
            if (listener instanceof WidgetBase widget) {
                Rect2i rect = widgetRect(widget);
                interactive.add(rect);
                visual.add(rect);
            }
        }
        WidgetBase templatePersistence = widgetPlacement.components.templatePersistence();
        if (templatePersistence != null) {
            for (Rect2i rect : visibleWidgetTreeRects(templatePersistence)) {
                interactive.add(rect);
                visual.add(rect);
            }
        }
        if (widgetPlacement.components.noResults() != null) {
            visual.add(widgetRect(widgetPlacement.components.noResults()));
        }
        Rect2i leftRailFrame = upgradeRailFrame(context, placed, widgetPlacement.components.upgradeSwitches());
        if (leftRailFrame == null) {
            leftRailFrame = settingsTemplateRailFrame(
                context,
                state,
                widgetPlacement.components.templatePersistence()
            );
        }
        if (leftRailFrame != null) {
            visual.add(leftRailFrame);
        }
        visual.addAll(rightTabFrames(context, widgetPlacement.components.sideTabs()));

        List<InternalDesktopWindowRegion> external = new ArrayList<>();
        Set<RectKey> seen = new LinkedHashSet<>();
        for (Rect2i rect : unionExternalRects(
            context,
            state,
            placed,
            widgetPlacement.components,
            inventoryControls
        )) {
            if (insideBase(context, rect)) {
                continue;
            }
            RectKey key = new RectKey(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            if (seen.add(key)) {
                external.add(InternalDesktopWindowRegion.visualAndInteractive(
                    rect.getX() - context.windowX(),
                    rect.getY() - context.windowY(),
                    rect.getWidth(),
                    rect.getHeight()
                ));
            }
        }
        if (templatePersistence != null) {
            for (Rect2i rect : visibleWidgetTreeRects(templatePersistence)) {
                if (insideBase(context, rect)) {
                    continue;
                }
                RectKey key = new RectKey(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                if (seen.add(key)) {
                    external.add(InternalDesktopWindowRegion.transientVisualAndInteractive(
                        rect.getX() - context.windowX(),
                        rect.getY() - context.windowY(),
                        rect.getWidth(),
                        rect.getHeight()
                    ));
                }
            }
        }
        visual.addAll(interactive);
        return new Layout(
            grid,
            scrollbar,
            maxScroll,
            List.copyOf(placed),
            List.copyOf(inventoryControls),
            List.copyOf(interactive),
            List.copyOf(visual),
            List.copyOf(external)
        );
    }

    private static List<Rect2i> unionExternalRects(
        DesktopWindowContext<?, ?> context,
        State state,
        List<PlacedSlot> placed,
        SophisticatedScreenAccess.Components components,
        List<Rect2i> inventoryControls
    ) {
        List<Rect2i> result = new ArrayList<>();
        Rect2i leftRail = upgradeRailFrame(context, placed, components.upgradeSwitches());
        if (leftRail == null) {
            leftRail = settingsTemplateRailFrame(context, state, components.templatePersistence());
        }
        if (leftRail != null) {
            result.add(leftRail);
        }
        result.addAll(rightTabFrames(context, components.sideTabs()));
        Rect2i panelSlots = boundsOf(placed, SlotGroup.PANEL, null);
        if (panelSlots != null) {
            result.add(panelSlots);
        }
        Rect2i extraSlots = boundsOf(placed, SlotGroup.EXTRA, null);
        if (extraSlots != null) {
            result.add(extraSlots);
        }
        if (components.sideTabs() != null) {
            result.add(widgetRect(components.sideTabs()));
        }
        if (components.sideTabs() instanceof UpgradeSettingsTabControl tabs) {
            result.addAll(tabs.getTabRectangles());
        }
        if (state.screen instanceof SettingsScreen settings) {
            result.addAll(settings.getExtendedControlsRectangles());
        }
        for (WidgetBase widget : components.upgradeSwitches()) {
            result.add(widgetRect(widget));
        }
        if (components.modal() != null && !insideBase(context, widgetRect(components.modal()))) {
            result.add(widgetRect(components.modal()));
        }
        result.addAll(inventoryControls);
        return result;
    }

    /**
     * Frames the upgrade column as a small Salt window drawn below the main window. Its right edge
     * intentionally reaches into the base frame so that the subsequent base-window render hides
     * that edge and the rail reads as one attached tab rather than a second floating window.
     */
    private static @Nullable Rect2i upgradeRailFrame(
        DesktopWindowContext<?, ?> context,
        List<PlacedSlot> placed,
        List<WidgetBase> upgradeSwitches
    ) {
        Rect2i slots = boundsOf(placed, SlotGroup.UPGRADE, null);
        if (slots == null) {
            return null;
        }
        int minX = slots.getX();
        int minY = slots.getY();
        int maxY = slots.getY() + slots.getHeight();
        for (WidgetBase upgradeSwitch : upgradeSwitches) {
            Rect2i widget = widgetRect(upgradeSwitch);
            if (widget.getX() <= HIDDEN_SLOT || widget.getY() <= HIDDEN_SLOT) {
                continue;
            }
            minX = Math.min(minX, widget.getX());
            minY = Math.min(minY, widget.getY());
            maxY = Math.max(maxY, widget.getY() + widget.getHeight());
        }
        int frameX = minX - UPGRADE_RAIL_FRAME_PADDING;
        int frameY = minY - UPGRADE_RAIL_FRAME_PADDING;
        int frameRight = context.windowX() + UPGRADE_RAIL_FRAME_OVERLAP;
        int frameBottom = maxY + UPGRADE_RAIL_FRAME_PADDING;
        return new Rect2i(
            frameX,
            frameY,
            Math.max(1, frameRight - frameX),
            Math.max(1, frameBottom - frameY)
        );
    }

    /** Frames the settings save/load/export control in the same tucked-under shell as upgrade slots. */
    private static @Nullable Rect2i settingsTemplateRailFrame(
        DesktopWindowContext<?, ?> context,
        State state,
        @Nullable WidgetBase templatePersistence
    ) {
        if (!state.kind.settings() || templatePersistence == null) {
            return null;
        }
        Rect2i widget = widgetRect(templatePersistence);
        int frameX = widget.getX() - UPGRADE_RAIL_FRAME_PADDING;
        int frameY = widget.getY() - UPGRADE_RAIL_FRAME_PADDING;
        int frameRight = context.windowX() + UPGRADE_RAIL_FRAME_OVERLAP;
        int frameBottom = widget.getY() + widget.getHeight() + UPGRADE_RAIL_FRAME_PADDING;
        return new Rect2i(
            frameX,
            frameY,
            Math.max(1, frameRight - frameX),
            Math.max(1, frameBottom - frameY)
        );
    }

    /**
     * Replaces each visible native tab plate with its own Salt window. Open panels are painted last so
     * their Salt body covers native tabs hidden by Sophisticated's normal overlap rules. Every frame
     * starts inside the base window, letting the subsequently painted main frame hide its left edge.
     */
    private static List<Rect2i> rightTabFrames(
        DesktopWindowContext<?, ?> context,
        @Nullable WidgetBase sideTabs
    ) {
        if (sideTabs == null) {
            return List.of();
        }
        if (!(sideTabs instanceof SettingsTabControl<?, ?> tabs)) {
            Rect2i fallback = rightTabFrame(context, widgetRect(sideTabs));
            return fallback == null ? List.of() : List.of(fallback);
        }

        SettingsTabBase<?> openTab = tabs.getOpenTab().orElse(null);
        List<Rect2i> frames = new ArrayList<>();
        for (GuiEventListener listener : tabs.children()) {
            if (!(listener instanceof Tab tab) || tab == openTab || !isVisibleBesideOpenTab(tab, openTab)) {
                continue;
            }
            tab.getTabRectangle().map(rect -> rightTabFrame(context, rect)).ifPresent(frames::add);
        }
        if (openTab != null) {
            openTab.getTabRectangle().map(rect -> rightTabFrame(context, rect)).ifPresent(frames::add);
        }
        return List.copyOf(frames);
    }

    private static boolean isVisibleBesideOpenTab(Tab tab, @Nullable SettingsTabBase<?> openTab) {
        return openTab == null
            || openTab.getBottomY() < tab.getTopY()
            || openTab.getTopY() > tab.getTopY();
    }

    private static @Nullable Rect2i rightTabFrame(DesktopWindowContext<?, ?> context, Rect2i tab) {
        if (tab.getWidth() <= 0 || tab.getHeight() <= 0
            || tab.getX() <= HIDDEN_SLOT || tab.getY() <= HIDDEN_SLOT) {
            return null;
        }
        int frameX = context.windowX() + context.windowWidth() - UPGRADE_RAIL_FRAME_OVERLAP;
        int frameY = tab.getY();
        int frameRight = tab.getX() + tab.getWidth();
        int frameBottom = tab.getY() + tab.getHeight();
        return new Rect2i(
            frameX,
            frameY,
            Math.max(1, frameRight - frameX),
            Math.max(1, frameBottom - frameY)
        );
    }

    private static void renderSlotBackgrounds(
        DesktopRenderContext<?, ?> context,
        State state,
        Layout layout,
        GuiGraphicsExtractor graphics
    ) {
        if (state.kind.limited()) {
            List<PlacedSlot> main = layout.slots.stream().filter(slot -> slot.group == SlotGroup.MAIN).toList();
            if (!main.isEmpty()) {
                int bodyX = context.contentX();
                int bodyY = context.contentY() - LIMITED_BODY_TOP_CROP;
                SophisticatedLimitedBarrelRendering.drawSlotBackground(
                    state.screen,
                    graphics,
                    bodyX,
                    bodyY,
                    main.size()
                );
                SophisticatedLimitedBarrelRendering.renderCapacityBars(state.screen, context.menu(), graphics, bodyX, bodyY);
            }
        }
        for (PlacedSlot placed : layout.slots) {
            if (!placed.specializedBackground) {
                context.slotBackground(placed.itemX, placed.itemY);
            }
        }
    }

    private static List<Slot> filteredStorageSlots(
        State state,
        StorageContainerMenuBase<?> menu,
        Predicate<net.minecraft.world.item.ItemStack> filter
    ) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : allStorageSlots(state, menu)) {
            if (filter.test(slot.getItem())) {
                result.add(slot);
            }
        }
        return result;
    }

    private static List<Slot> allStorageSlots(State state, StorageContainerMenuBase<?> menu) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : state.originalSlots.keySet()) {
            if (menu.isStorageInventorySlot(slot)) {
                result.add(slot);
            }
        }
        result.sort((left, right) -> Integer.compare(state.slotOrder.getOrDefault(left, 0), state.slotOrder.getOrDefault(right, 0)));
        return result;
    }

    private static void placeSlot(
        State state,
        List<PlacedSlot> placed,
        Slot slot,
        int itemX,
        int itemY,
        SlotGroup group,
        boolean specializedBackground
    ) {
        // Vanilla/Sophisticated slot coordinates and Salt's slot render API both identify the
        // 16-by-16 item origin. The surrounding 18-by-18 frame begins one pixel above and left.
        ((SophisticatedSlotPositionAccess) slot).salts_inventory_update$setPosition(
            itemX - state.screen.getLeftPos(),
            itemY - state.screen.getTopPos()
        );
        state.hostedSlotPositions.put(
            slot,
            new SlotPosition(itemX - state.screen.getLeftPos(), itemY - state.screen.getTopPos())
        );
        placed.add(new PlacedSlot(
            slot,
            new Rect2i(itemX - 1, itemY - 1, SLOT, SLOT),
            itemX,
            itemY,
            group,
            specializedBackground
        ));
    }

    private static boolean replacesSlot(SophisticatedScreenAccess access, Slot slot) {
        for (UpgradeInventoryControlBase control : access.storageInventoryControls()) {
            if (control.replacesSlotRender(slot.index)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps Salt's validated slot path from stealing input through a native Sophisticated overlay.
     * Crafting's result picker is owned by the open tab and selectively covers its recipe slots,
     * while modal overlays cover every hosted slot until the modal is dismissed.
     */
    private static @Nullable PlacedSlot interactiveSlotAt(State state, Layout layout, double mouseX, double mouseY) {
        PlacedSlot placed = layout.slotAt(mouseX, mouseY);
        if (placed == null) {
            return null;
        }
        SophisticatedScreenAccess.Components components = state.access.components();
        if (components.modal() != null) {
            return null;
        }
        if (components.sideTabs() instanceof UpgradeSettingsTabControl tabs
            && !tabs.slotIsNotCoveredAt(placed.slot, mouseX, mouseY)) {
            return null;
        }
        return placed;
    }

    private static int inventoryControlReserve(State state) {
        int positionedControls = 0;
        for (UpgradeInventoryControlBase control : state.access.upgradeInventoryControls()) {
            if (control instanceof SophisticatedInventoryControlPositionAccess) {
                positionedControls++;
            }
        }
        return positionedControls * INVENTORY_CONTROL_COLUMN_WIDTH;
    }

    private static List<Rect2i> positionInventoryControls(State state, int anchorX, int anchorY) {
        List<PositionedInventoryControl> controls = new ArrayList<>();
        Set<UpgradeInventoryControlBase> active = Collections.newSetFromMap(new IdentityHashMap<>());
        for (UpgradeInventoryControlBase control : state.access.upgradeInventoryControls()) {
            if (!(control instanceof SophisticatedInventoryControlPositionAccess positionAccess)) {
                continue;
            }
            active.add(control);
            InventoryControlPosition original = state.originalInventoryControls.computeIfAbsent(
                control,
                ignored -> {
                    Position position = positionAccess.salts_inventory_update$getPosition();
                    return new InventoryControlPosition(
                        position.x(),
                        position.y(),
                        Math.max(1, positionAccess.salts_inventory_update$getHeight())
                    );
                }
            );
            controls.add(new PositionedInventoryControl(control, positionAccess, original));
        }
        state.originalInventoryControls.keySet().retainAll(active);
        state.hostedInventoryControls.keySet().retainAll(active);
        if (controls.isEmpty()) {
            state.hostedInventoryControls.clear();
            return List.of();
        }

        controls.sort((left, right) -> {
            int x = Integer.compare(left.original.x, right.original.x);
            return x != 0 ? x : Integer.compare(left.original.y, right.original.y);
        });
        int minX = controls.stream().mapToInt(control -> control.original.x).min().orElse(0);
        int minY = controls.stream().mapToInt(control -> control.original.y).min().orElse(0);
        int dx = anchorX - state.screen.getLeftPos() - minX;
        int dy = anchorY - state.screen.getTopPos() - minY;
        List<Rect2i> rectangles = new ArrayList<>(controls.size());
        for (PositionedInventoryControl control : controls) {
            int relativeX = control.original.x + dx;
            int relativeY = control.original.y + dy;
            control.access.salts_inventory_update$setPosition(new Position(relativeX, relativeY));
            Rect2i rectangle = new Rect2i(
                state.screen.getLeftPos() + relativeX + INVENTORY_CONTROL_RENDER_X,
                state.screen.getTopPos() + relativeY,
                INVENTORY_CONTROL_RENDER_WIDTH,
                control.original.height
            );
            rectangles.add(rectangle);
            state.hostedInventoryControls.put(control.control, rectangle);
        }
        return List.copyOf(rectangles);
    }

    private static @Nullable UpgradeInventoryControlBase inventoryControlAt(
        State state,
        Layout layout,
        double mouseX,
        double mouseY
    ) {
        if (!layout.containsInventoryControl(mouseX, mouseY)) {
            return null;
        }
        for (Map.Entry<UpgradeInventoryControlBase, Rect2i> entry : state.hostedInventoryControls.entrySet()) {
            if (contains(entry.getValue(), mouseX, mouseY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void hidePlayerSlots(State state) {
        for (Slot slot : state.originalSlots.keySet()) {
            if (slot.container instanceof Inventory) {
                hideSlot(state, slot);
            }
        }
    }

    private static void hideSlots(State state, List<Slot> slots) {
        for (Slot slot : slots) {
            hideSlot(state, slot);
        }
    }

    private static void hideSlot(State state, Slot slot) {
        ((SophisticatedSlotPositionAccess) slot).salts_inventory_update$setPosition(HIDDEN_SLOT, HIDDEN_SLOT);
        state.hostedSlotPositions.put(slot, new SlotPosition(HIDDEN_SLOT, HIDDEN_SLOT));
    }

    private static void unfocusSearch(State state) {
        WidgetBase search = state.access.components().search();
        if (search != null && state.screen.getFocused() == search) {
            search.setFocused(false);
            state.screen.setFocused(null);
        }
    }

    private static void updateScrollFromPointer(State state, Layout layout, double mouseY) {
        if (layout.scrollbar == null || layout.maxScroll <= 0) {
            state.scrollRow = 0;
            return;
        }
        int track = Math.max(1, layout.scrollbar.getHeight() - 2 - DesktopWidgets.SCROLLBAR_THUMB_HEIGHT);
        double offset = mouseY - layout.scrollbar.getY() - 1 - DesktopWidgets.SCROLLBAR_THUMB_HEIGHT / 2.0D;
        state.scrollRow = DesktopWidgets.clamp((int) Math.round(offset / track * layout.maxScroll), 0, layout.maxScroll);
    }

    private static void setWidgetPosition(State state, WidgetBase widget, int x, int y) {
        originalWidget(state, widget);
        int dx = x - widget.getX();
        int dy = y - widget.getY();
        widget.setPosition(new Position(x, y));
        if ((dx != 0 || dy != 0) && widget instanceof CompositeWidgetBase<?> composite) {
            translateDetachedTabChildren(composite, dx, dy);
        }
    }

    /**
     * Closed Sophisticated tabs keep their panel controls outside the composite child tree. Moving the
     * controller therefore moves the tab icons but not those detached controls. Apply the same delta to
     * detached controls now so a later tab open cannot reattach them at the native full-screen position.
     */
    private static void translateDetachedTabChildren(CompositeWidgetBase<?> composite, int dx, int dy) {
        for (GuiEventListener listener : composite.children()) {
            if (!(listener instanceof SettingsTabBase<?> tab)
                || !(listener instanceof SophisticatedSettingsTabChildrenAccess access)) {
                continue;
            }
            if (tab instanceof SophisticatedHostedPositionAccess positionAccess) {
                positionAccess.salts_inventory_update$translateHostedPosition(dx, dy);
            }
            List<? extends GuiEventListener> attachedChildren = tab.children();
            for (WidgetBase child : access.salts_inventory_update$hideableChildren()) {
                if (!attachedChildren.contains(child)) {
                    child.setPosition(new Position(child.getX() + dx, child.getY() + dy));
                }
                if (child instanceof SophisticatedHostedPositionAccess positionAccess) {
                    positionAccess.salts_inventory_update$translateHostedPosition(dx, dy);
                }
            }
        }
    }

    private static SlotPosition originalWidget(State state, WidgetBase widget) {
        return state.originalWidgets.computeIfAbsent(widget, ignored -> new SlotPosition(widget.getX(), widget.getY()));
    }

    private static Rect2i originalBounds(State state, List<Slot> slots) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Slot slot : slots) {
            SlotPosition position = state.originalSlots.get(slot);
            if (position == null) {
                continue;
            }
            minX = Math.min(minX, position.itemX - 1);
            minY = Math.min(minY, position.itemY - 1);
            maxX = Math.max(maxX, position.itemX - 1 + SLOT);
            maxY = Math.max(maxY, position.itemY - 1 + SLOT);
        }
        if (minX == Integer.MAX_VALUE) {
            return new Rect2i(state.screen.getLeftPos(), state.screen.getTopPos(), SLOT, SLOT);
        }
        return new Rect2i(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    private static @Nullable Rect2i boundsOf(List<PlacedSlot> slots, SlotGroup group, @Nullable Rect2i fallback) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (PlacedSlot slot : slots) {
            if (slot.group != group) {
                continue;
            }
            minX = Math.min(minX, slot.frame.getX());
            minY = Math.min(minY, slot.frame.getY());
            maxX = Math.max(maxX, slot.frame.getX() + slot.frame.getWidth());
            maxY = Math.max(maxY, slot.frame.getY() + slot.frame.getHeight());
        }
        return minX == Integer.MAX_VALUE ? fallback : new Rect2i(minX, minY, maxX - minX, maxY - minY);
    }

    private static Rect2i widgetRect(WidgetBase widget) {
        return new Rect2i(widget.getX(), widget.getY(), Math.max(1, widget.getWidth()), Math.max(1, widget.getHeight()));
    }

    private static List<Rect2i> visibleWidgetTreeRects(WidgetBase root) {
        List<Rect2i> result = new ArrayList<>();
        collectVisibleWidgetTreeRects(root, result);
        return List.copyOf(result);
    }

    private static void collectVisibleWidgetTreeRects(WidgetBase widget, List<Rect2i> result) {
        Rect2i rect = widgetRect(widget);
        double probeX = rect.getX() + rect.getWidth() / 2.0D;
        double probeY = rect.getY() + rect.getHeight() / 2.0D;
        if (!widget.isMouseOver(probeX, probeY)) {
            return;
        }
        result.add(rect);
        if (widget instanceof CompositeWidgetBase<?> composite) {
            for (GuiEventListener child : composite.children()) {
                if (child instanceof WidgetBase childWidget) {
                    collectVisibleWidgetTreeRects(childWidget, result);
                }
            }
        }
    }

    private static @Nullable WidgetBase deepestWidgetAt(
        @Nullable WidgetBase root,
        double mouseX,
        double mouseY
    ) {
        if (root == null) {
            return null;
        }
        if (root instanceof CompositeWidgetBase<?> composite) {
            for (GuiEventListener child : composite.children()) {
                if (child instanceof WidgetBase childWidget) {
                    WidgetBase target = deepestWidgetAt(childWidget, mouseX, mouseY);
                    if (target != null) {
                        return target;
                    }
                }
            }
        }
        return root.isMouseOver(mouseX, mouseY) ? root : null;
    }

    private static boolean widgetTreeContains(@Nullable WidgetBase root, @Nullable GuiEventListener target) {
        if (root == null || target == null) {
            return false;
        }
        if (root == target) {
            return true;
        }
        if (root instanceof CompositeWidgetBase<?> composite) {
            for (GuiEventListener child : composite.children()) {
                if (child == target || child instanceof WidgetBase childWidget && widgetTreeContains(childWidget, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void dismissTemplateInputs(State state) {
        WidgetBase templatePersistence = state.access.components().templatePersistence();
        if (templatePersistence == null) {
            return;
        }
        Set<GuiEventListener> inputs = Collections.newSetFromMap(new IdentityHashMap<>());
        collectTemplateInputs(templatePersistence, inputs);
        if (inputs.isEmpty()) {
            return;
        }
        for (GuiEventListener input : inputs) {
            if (input instanceof TextBox textBox) {
                textBox.setVisible(false);
                textBox.setFocused(false);
            }
        }
        GuiEventListener focused = state.screen.getFocused();
        Set<GuiEventListener> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (focused instanceof ContainerEventHandler container && visited.add(focused)) {
            GuiEventListener child = container.getFocused();
            if (child == null) {
                break;
            }
            focused = child;
        }
        if (inputs.contains(focused)) {
            state.screen.setFocused(null);
        }
        if (inputs.contains(state.nativePointerOwner)) {
            state.nativePointerOwner = null;
            state.nativePointerActive = false;
        }
    }

    private static void collectTemplateInputs(WidgetBase widget, Set<GuiEventListener> inputs) {
        if (widget instanceof TextBox) {
            inputs.add(widget);
        }
        if (widget instanceof CompositeWidgetBase<?> composite) {
            for (GuiEventListener child : composite.children()) {
                if (child instanceof WidgetBase childWidget) {
                    collectTemplateInputs(childWidget, inputs);
                }
            }
        }
    }

    private static boolean insideBase(DesktopWindowContext<?, ?> context, Rect2i rect) {
        return rect.getX() >= context.windowX()
            && rect.getY() >= context.windowY()
            && rect.getX() + rect.getWidth() <= context.windowX() + context.windowWidth()
            && rect.getY() + rect.getHeight() <= context.windowY() + context.windowHeight();
    }

    private static boolean contains(Rect2i rect, double x, double y) {
        return x >= rect.getX() && y >= rect.getY()
            && x < rect.getX() + rect.getWidth() && y < rect.getY() + rect.getHeight();
    }

    private static DesktopWindowSize saltSize(int contentWidth, int contentHeight) {
        return DesktopWindowSize.of(contentWidth + SALT_HORIZONTAL_CHROME, contentHeight + SALT_VERTICAL_CHROME);
    }

    /** Uses closed tab dimensions so opening a wide or tall panel never grows the base window. */
    private static DesktopWindowSize sizeWithClosedTabs(
        DesktopWindowContext<?, ?> context,
        State state,
        DesktopWindowSize base
    ) {
        WidgetBase sideTabs = state.access.components().sideTabs();
        if (!(sideTabs instanceof SettingsTabControl<?, ?> tabs)) {
            return base;
        }
        int tabCount = 0;
        for (GuiEventListener listener : tabs.children()) {
            if (listener instanceof Tab) {
                tabCount++;
            }
        }
        if (tabCount == 0) {
            return base;
        }
        int tabTop = (state.kind == LayoutKind.STANDARD ? standardToolbarY(context) : context.contentY())
            - context.windowY();
        int minimumHeight = tabTop + (tabCount - 1) * RIGHT_TAB_STRIDE + Tab.DEFAULT_HEIGHT;
        return DesktopWindowSize.of(base.width(), Math.max(base.height(), minimumHeight));
    }

    private static @Nullable DesktopWindowSize desiredSizeWithClosedTabs(
        DesktopWindowContext<?, ?> context,
        State state,
        @Nullable DesktopWindowSize desired
    ) {
        DesktopWindowSize candidate = desired == null
            ? DesktopWindowSize.of(context.windowWidth(), context.windowHeight())
            : desired;
        DesktopWindowSize adjusted = sizeWithClosedTabs(context, state, candidate);
        if (desired != null || adjusted.height() > context.windowHeight()) {
            return adjusted;
        }
        return null;
    }

    private static int defaultVisibleRows(DesktopWindowSetupContext<?> context, int totalRows) {
        // Match Salt's eight-pixel placement margin on both desktop edges. Request every native
        // row when possible and let oversized inventories start at the largest complete-row view.
        int desktopHeight = context.minecraft().getWindow().getGuiScaledHeight();
        int maximumWindowHeight = Math.max(1, desktopHeight - DEFAULT_DESKTOP_MARGIN * 2);
        int maximumContentHeight = Math.max(1, maximumWindowHeight - SALT_VERTICAL_CHROME);
        int rowsThatFit = Math.max(
            MIN_VISIBLE_ROWS,
            (maximumContentHeight + STANDARD_HEADER_RAISE - TOOLBAR_HEIGHT - TOOLBAR_GAP) / SLOT
        );
        return Math.max(MIN_VISIBLE_ROWS, Math.min(Math.max(1, totalRows), rowsThatFit));
    }

    private static int standardContentWidth(int columns, boolean scrolling, int inventoryControlWidth) {
        return Math.max(
            STANDARD_MIN_CONTENT_WIDTH,
            Math.max(1, columns) * SLOT + (scrolling ? SCROLLBAR_RESERVE : 0) + Math.max(0, inventoryControlWidth)
        );
    }

    private static int standardToolbarY(DesktopWindowContext<?, ?> context) {
        return context.contentY() - STANDARD_HEADER_RAISE;
    }

    private static int standardContentHeight(int rows) {
        return TOOLBAR_HEIGHT + TOOLBAR_GAP + rows * SLOT - STANDARD_HEADER_RAISE;
    }

    public enum LayoutKind {
        STANDARD,
        LIMITED_BARREL,
        SETTINGS,
        LIMITED_BARREL_SETTINGS;

        private boolean settings() {
            return this == SETTINGS || this == LIMITED_BARREL_SETTINGS;
        }

        private boolean limited() {
            return this == LIMITED_BARREL || this == LIMITED_BARREL_SETTINGS;
        }
    }

    public static final class State {
        private final LayoutKind kind;
        private final AbstractContainerScreen<?> screen;
        private final SophisticatedScreenAccess access;
        private final Map<Slot, SlotPosition> originalSlots = new IdentityHashMap<>();
        private final Map<Slot, Integer> slotOrder = new IdentityHashMap<>();
        private final Map<Slot, SlotPosition> hostedSlotPositions = new IdentityHashMap<>();
        private final Map<WidgetBase, SlotPosition> originalWidgets = new IdentityHashMap<>();
        private final Map<UpgradeInventoryControlBase, InventoryControlPosition> originalInventoryControls =
            new IdentityHashMap<>();
        private final Map<UpgradeInventoryControlBase, Rect2i> hostedInventoryControls = new IdentityHashMap<>();
        private final DesktopWindowSize fixedSize;
        private int screenWidth;
        private int screenHeight;
        private int scrollRow;
        private boolean scrollbarDragging;
        private boolean nativePointerActive;
        private @Nullable GuiEventListener nativePointerOwner;
        private @Nullable UpgradeInventoryControlBase nativeInventoryControlOwner;
        private boolean legacyScrollbarGutterMigrationPending;
        private @Nullable Layout layout;
        private List<InternalDesktopWindowRegion> externalRegions = List.of();

        private State(
            LayoutKind kind,
            AbstractContainerScreen<?> screen,
            SophisticatedScreenAccess access,
            int screenWidth,
            int screenHeight
        ) {
            this.kind = kind;
            this.screen = screen;
            this.access = access;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.legacyScrollbarGutterMigrationPending = kind == LayoutKind.STANDARD;
            int contentWidth = Math.max(MIN_CONTENT_WIDTH, screen.getImageWidth());
            int contentHeight;
            if (kind == LayoutKind.LIMITED_BARREL_SETTINGS) {
                contentHeight = 92;
            } else if (kind == LayoutKind.SETTINGS && screen instanceof SettingsScreen settings) {
                int rows = Math.max(1, DesktopWidgets.rowsForCount(
                    ((SettingsContainerMenu<?>) settings.getMenu()).getStorageInventorySlots().size(),
                    Math.max(1, settings.getSlotsOnLine())
                ));
                contentWidth = Math.max(1, settings.getSlotsOnLine()) * SLOT;
                contentHeight = rows * SLOT;
            } else if (kind == LayoutKind.LIMITED_BARREL) {
                contentHeight = 92;
            } else {
                contentHeight = Math.max(72, screen.getImageHeight() - 97);
            }
            this.fixedSize = saltSize(contentWidth, contentHeight);
        }

        private void captureNativeGeometry(AbstractContainerMenu menu) {
            this.originalSlots.clear();
            this.slotOrder.clear();
            this.hostedSlotPositions.clear();
            this.originalWidgets.clear();
            this.originalInventoryControls.clear();
            this.hostedInventoryControls.clear();
            int size = DesktopMenuSlots.size(menu);
            for (int slotId = 0; slotId < size; slotId++) {
                Slot slot;
                try {
                    slot = screenSlot(this.screen, slotId);
                } catch (IndexOutOfBoundsException exception) {
                    break;
                }
                this.originalSlots.put(slot, new SlotPosition(
                    this.screen.getLeftPos() + slot.x,
                    this.screen.getTopPos() + slot.y
                ));
                this.slotOrder.put(slot, slotId);
            }
            for (WidgetBase widget : this.access.components().layoutWidgets()) {
                this.originalWidgets.put(widget, new SlotPosition(widget.getX(), widget.getY()));
            }
        }

        /**
         * Incorporates dynamic slot-list rebuilds without overwriting the native baseline with Salt's
         * own relocated coordinates. A position different from the last hosted position means the
         * native screen has deliberately laid that slot out again (for example after a search or
         * upgrade-column change), so it becomes the new baseline.
         */
        private void refreshNativeGeometry(AbstractContainerMenu menu) {
            Set<Slot> active = Collections.newSetFromMap(new IdentityHashMap<>());
            int size = DesktopMenuSlots.size(menu);
            for (int slotId = 0; slotId < size; slotId++) {
                Slot slot;
                try {
                    slot = screenSlot(this.screen, slotId);
                } catch (IndexOutOfBoundsException exception) {
                    break;
                }
                active.add(slot);
                SlotPosition hosted = this.hostedSlotPositions.get(slot);
                if (!this.originalSlots.containsKey(slot)
                    || hosted != null && (slot.x != hosted.itemX || slot.y != hosted.itemY)) {
                    this.originalSlots.put(slot, new SlotPosition(
                        this.screen.getLeftPos() + slot.x,
                        this.screen.getTopPos() + slot.y
                    ));
                }
                this.slotOrder.put(slot, slotId);
            }
            this.originalSlots.keySet().retainAll(active);
            this.slotOrder.keySet().retainAll(active);
            this.hostedSlotPositions.keySet().retainAll(active);
        }

        private static Slot screenSlot(AbstractContainerScreen<?> screen, int slotId) {
            if (screen instanceof StorageScreenBase<?> storage) {
                return storage.getSlot(slotId);
            }
            if (screen instanceof SettingsScreen settings) {
                return settings.getSlot(slotId);
            }
            throw new IndexOutOfBoundsException(slotId);
        }
    }

    private record SlotPosition(int itemX, int itemY) {
    }

    private record InventoryControlPosition(int x, int y, int height) {
    }

    private record PositionedInventoryControl(
        UpgradeInventoryControlBase control,
        SophisticatedInventoryControlPositionAccess access,
        InventoryControlPosition original
    ) {
    }

    private enum SlotGroup {
        MAIN,
        UPGRADE,
        PANEL,
        EXTRA
    }

    private record PlacedSlot(
        Slot slot,
        Rect2i frame,
        int itemX,
        int itemY,
        SlotGroup group,
        boolean specializedBackground
    ) {
    }

    private record RailRowTranslation(int originalItemY, int dy) {
    }

    private record RailTranslation(int dx, int dy, List<RailRowTranslation> rows) {
        private static final RailTranslation NONE = new RailTranslation(0, 0, List.of());

        private int translateY(int originalY) {
            RailRowTranslation nearest = null;
            int nearestDistance = Integer.MAX_VALUE;
            for (RailRowTranslation row : this.rows) {
                int distance = Math.abs(originalY - row.originalItemY);
                if (distance < nearestDistance) {
                    nearest = row;
                    nearestDistance = distance;
                }
            }
            return originalY + (nearest == null ? this.dy : nearest.dy);
        }
    }

    private record WidgetPlacement(
        SophisticatedScreenAccess.Components components,
        int sideMoveDx,
        int sideMoveDy
    ) {
    }

    private record RectKey(int x, int y, int width, int height) {
    }

    private record Layout(
        Rect2i grid,
        @Nullable Rect2i scrollbar,
        int maxScroll,
        List<PlacedSlot> slots,
        List<Rect2i> inventoryControls,
        List<Rect2i> interactive,
        List<Rect2i> visual,
        List<InternalDesktopWindowRegion> externalRegions
    ) {
        private static Layout empty(DesktopWindowContext<?, ?> context) {
            Rect2i body = new Rect2i(
                context.contentX(),
                context.contentY(),
                Math.max(1, context.contentWidth()),
                Math.max(1, context.contentHeight())
            );
            return new Layout(body, null, 0, List.of(), List.of(), List.of(body), List.of(body), List.of());
        }

        private @Nullable PlacedSlot slotAt(double x, double y) {
            for (PlacedSlot slot : this.slots) {
                if (contains(slot.frame, x, y)) {
                    return slot;
                }
            }
            return null;
        }

        private boolean containsInteractive(double x, double y) {
            for (Rect2i rect : this.interactive) {
                if (contains(rect, x, y)) {
                    return true;
                }
            }
            return false;
        }

        private boolean containsVisual(double x, double y) {
            for (Rect2i rect : this.visual) {
                if (contains(rect, x, y)) {
                    return true;
                }
            }
            return false;
        }

        private boolean containsInventoryControl(double x, double y) {
            for (Rect2i rect : this.inventoryControls) {
                if (contains(rect, x, y)) {
                    return true;
                }
            }
            return false;
        }
    }

    @FunctionalInterface
    public interface ScreenFactory<T extends AbstractContainerMenu> {
        AbstractContainerScreen<?> create(T menu, Inventory inventory, Component title);
    }
}
