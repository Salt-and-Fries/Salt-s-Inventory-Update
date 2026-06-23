package com.salts_inventory_update.compat.toms_storage.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.compat.toms_storage.TomsStoragePayloads;
import com.salts_inventory_update.compat.toms_storage.TomsStoragePayloads.LinkChannel;
import com.salts_inventory_update.compat.toms_storage.TomsStoragePayloads.TerminalEntry;
import com.salts_inventory_update.compat.toms_storage.TomsStorageReflect;
import com.salts_inventory_update.debug.DesktopDebug;

public final class TomsStorageDesktopWindows {
    private static final int SLOT = 18;
    private static final int SLOT_ITEM = 16;
    private static final int PAD = 8;
    private static final int BUTTON = 16;
    private static final int SMALL_BUTTON = 14;
    private static final int DESKTOP_TOP_BAR_HEIGHT = 16;
    private static final int DESKTOP_WINDOW_PADDING = 8;
    private static final int STORAGE_TERMINAL_ROWS = 7;
    private static final int STORAGE_TERMINAL_COLUMNS = 9;
    private static final int STORAGE_TERMINAL_MIN_COLUMNS = 3;
    private static final int STORAGE_TERMINAL_MIN_ROWS = 2;
    private static final int STORAGE_TERMINAL_SEARCH_HEIGHT = 12;
    private static final int STORAGE_TERMINAL_SEARCH_GRID_GAP = 7;
    private static final int STORAGE_TERMINAL_SCROLLBAR_GAP = 4;
    private static final int STORAGE_TERMINAL_CONTROLS_GAP = 8;
    private static final int STORAGE_TERMINAL_CONTROL_COUNT = 5;
    private static final int STORAGE_TERMINAL_CONTROLS_HEIGHT = (STORAGE_TERMINAL_CONTROL_COUNT - 1) * 18 + BUTTON;
    private static final int STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH = 14;
    private static final int STORAGE_TERMINAL_SCROLLBAR_TEXTURE_WIDTH = 14;
    private static final int STORAGE_TERMINAL_SCROLLBAR_TEXTURE_HEIGHT = 3;
    private static final int STORAGE_TERMINAL_SCROLLBAR_INSET = 1;
    private static final int SEARCH_POPUP_ROW_HEIGHT = 18;
    private static final int SEARCH_POPUP_ROWS = 4;
    private static final int SEARCH_POPUP_PADDING = 6;
    private static final int SEARCH_POPUP_LABEL_BUTTON_GAP = 10;
    private static final int SEARCH_POPUP_STATE_BUTTON_WIDTH = 42;
    private static final int SEARCH_POPUP_STATE_BUTTON_HEIGHT = 14;
    private static final int STORAGE_TERMINAL_GRID_WIDTH = STORAGE_TERMINAL_COLUMNS * SLOT;
    private static final int STORAGE_TERMINAL_GRID_HEIGHT = STORAGE_TERMINAL_ROWS * SLOT;
    private static final int STORAGE_TERMINAL_SEARCH_WIDTH = STORAGE_TERMINAL_GRID_WIDTH + STORAGE_TERMINAL_SCROLLBAR_GAP + STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH;
    private static final int STORAGE_TERMINAL_CONTENT_WIDTH = STORAGE_TERMINAL_SEARCH_WIDTH + STORAGE_TERMINAL_CONTROLS_GAP + BUTTON;
    private static final int STORAGE_TERMINAL_CONTENT_HEIGHT = STORAGE_TERMINAL_SEARCH_HEIGHT + STORAGE_TERMINAL_SEARCH_GRID_GAP + STORAGE_TERMINAL_GRID_HEIGHT;
    private static final int STORAGE_TERMINAL_WINDOW_WIDTH = DESKTOP_WINDOW_PADDING * 2 + STORAGE_TERMINAL_CONTENT_WIDTH;
    private static final int STORAGE_TERMINAL_WINDOW_HEIGHT = DESKTOP_TOP_BAR_HEIGHT + DESKTOP_WINDOW_PADDING * 2 + STORAGE_TERMINAL_CONTENT_HEIGHT;
    private static final int CRAFTING_TERMINAL_CRAFT_GAP = 8;
    private static final int CRAFTING_TERMINAL_CRAFT_INPUT_X = 36;
    private static final int CRAFTING_TERMINAL_CRAFT_INPUT_Y = 0;
    private static final int CRAFTING_TERMINAL_CRAFT_ARROW_X = 90;
    private static final int CRAFTING_TERMINAL_CRAFT_ARROW_Y = 18;
    private static final int CRAFTING_TERMINAL_CRAFT_OUTPUT_X = 126;
    private static final int CRAFTING_TERMINAL_CRAFT_OUTPUT_Y = 18;
    private static final int CRAFTING_TERMINAL_CLEAR_X = 92;
    private static final int CRAFTING_TERMINAL_CLEAR_Y = 44;
    private static final int CRAFTING_TERMINAL_RECIPE_BUTTON_WIDTH = 20;
    private static final int CRAFTING_TERMINAL_RECIPE_BUTTON_HEIGHT = 18;
    private static final int CRAFTING_TERMINAL_RECIPE_BUTTON_LEFT_GUTTER_X = 16;
    private static final int CRAFTING_TERMINAL_RECIPE_BUTTON_Y = 30;
    private static final int CRAFTING_TERMINAL_CRAFT_WIDTH = CRAFTING_TERMINAL_CRAFT_OUTPUT_X + SLOT;
    private static final int CRAFTING_TERMINAL_CRAFT_HEIGHT = CRAFTING_TERMINAL_CLEAR_Y + BUTTON;
    private static final int CRAFTING_TERMINAL_CONTENT_HEIGHT = STORAGE_TERMINAL_CONTENT_HEIGHT + CRAFTING_TERMINAL_CRAFT_GAP + CRAFTING_TERMINAL_CRAFT_HEIGHT;
    private static final int CRAFTING_TERMINAL_WINDOW_HEIGHT = DESKTOP_TOP_BAR_HEIGHT + DESKTOP_WINDOW_PADDING * 2 + CRAFTING_TERMINAL_CONTENT_HEIGHT;
    private static final int STORAGE_TERMINAL_MIN_LEFT_WIDTH = STORAGE_TERMINAL_MIN_COLUMNS * SLOT + STORAGE_TERMINAL_SCROLLBAR_GAP + STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH;
    private static final int STORAGE_TERMINAL_MIN_CONTENT_WIDTH = STORAGE_TERMINAL_MIN_LEFT_WIDTH + STORAGE_TERMINAL_CONTROLS_GAP + BUTTON;
    private static final int STORAGE_TERMINAL_MIN_CONTENT_HEIGHT = Math.max(
        STORAGE_TERMINAL_CONTROLS_HEIGHT,
        STORAGE_TERMINAL_SEARCH_HEIGHT + STORAGE_TERMINAL_SEARCH_GRID_GAP + STORAGE_TERMINAL_MIN_ROWS * SLOT
    );
    private static final int STORAGE_TERMINAL_MIN_WINDOW_WIDTH = DESKTOP_WINDOW_PADDING * 2 + STORAGE_TERMINAL_MIN_CONTENT_WIDTH;
    private static final int STORAGE_TERMINAL_MIN_WINDOW_HEIGHT = DESKTOP_TOP_BAR_HEIGHT + DESKTOP_WINDOW_PADDING * 2 + STORAGE_TERMINAL_MIN_CONTENT_HEIGHT;
    private static final int CRAFTING_TERMINAL_MIN_LEFT_WIDTH = Math.max(STORAGE_TERMINAL_MIN_LEFT_WIDTH, CRAFTING_TERMINAL_CRAFT_WIDTH);
    private static final int CRAFTING_TERMINAL_MIN_CONTENT_WIDTH = CRAFTING_TERMINAL_MIN_LEFT_WIDTH + STORAGE_TERMINAL_CONTROLS_GAP + BUTTON;
    private static final int CRAFTING_TERMINAL_MIN_CONTENT_HEIGHT = Math.max(
        STORAGE_TERMINAL_CONTROLS_HEIGHT,
        STORAGE_TERMINAL_SEARCH_HEIGHT + STORAGE_TERMINAL_SEARCH_GRID_GAP + STORAGE_TERMINAL_MIN_ROWS * SLOT + CRAFTING_TERMINAL_CRAFT_GAP + CRAFTING_TERMINAL_CRAFT_HEIGHT
    );
    private static final int CRAFTING_TERMINAL_MIN_WINDOW_WIDTH = DESKTOP_WINDOW_PADDING * 2 + CRAFTING_TERMINAL_MIN_CONTENT_WIDTH;
    private static final int CRAFTING_TERMINAL_MIN_WINDOW_HEIGHT = DESKTOP_TOP_BAR_HEIGHT + DESKTOP_WINDOW_PADDING * 2 + CRAFTING_TERMINAL_MIN_CONTENT_HEIGHT;

    private static final Identifier SLOT_BUTTON = TomsStorageCompat.id("widget/slot_button");
    private static final Identifier SLOT_BUTTON_HOVERED = TomsStorageCompat.id("widget/slot_button_hovered");
    private static final Identifier SMALL_BUTTON_SPRITE = TomsStorageCompat.id("widget/small_button");
    private static final Identifier SMALL_BUTTON_HOVERED = TomsStorageCompat.id("widget/small_button_hovered");
    private static final Identifier SMALL_BUTTON_SELECTED = TomsStorageCompat.id("widget/small_button_selected");
    private static final Identifier SCROLLER = Identifier.parse("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.parse("container/creative_inventory/scroller_disabled");
    private static final Identifier CONTAINER_WIDGETS_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/container_widgets.png");
    private static final Identifier SCROLLBAR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/scroll_bar_behind.png");
    private static final Identifier SEARCH_BAR_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/search_bar.png");
    private static final Identifier SALT_WINDOW_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/window.png");
    private static final int SEARCH_BAR_TEXTURE_WIDTH = 3;
    private static final int SEARCH_BAR_TEXTURE_HEIGHT = 12;
    private static final Map<String, String> KEPT_TERMINAL_SEARCHES = new HashMap<>();

    private TomsStorageDesktopWindows() {
    }

    public interface Definition<T extends AbstractContainerMenu, S> extends DesktopWindowDefinition<T, S> {
    }

    public static final class Terminal implements Definition<AbstractContainerMenu, TerminalState> {
        private final boolean crafting;

        public Terminal(boolean crafting) {
            this.crafting = crafting;
        }

        @Override
        public TerminalState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            TerminalState state = new TerminalState();
            state.searchMemoryKey = terminalSearchMemoryKey(context);
            state.pendingKeptSearch = KEPT_TERMINAL_SEARCHES.get(state.searchMemoryKey);
            return state;
        }

        @Override
        public Component title(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return Component.literal(this.crafting ? "Crafting Terminal" : "Storage Terminal");
        }

        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return this.crafting
                ? DesktopWindowSize.of(STORAGE_TERMINAL_WINDOW_WIDTH, CRAFTING_TERMINAL_WINDOW_HEIGHT)
                : DesktopWindowSize.of(STORAGE_TERMINAL_WINDOW_WIDTH, STORAGE_TERMINAL_WINDOW_HEIGHT);
        }

        @Override
        public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.crafting
                ? DesktopWindowSize.of(CRAFTING_TERMINAL_MIN_WINDOW_WIDTH, CRAFTING_TERMINAL_MIN_WINDOW_HEIGHT)
                : DesktopWindowSize.of(STORAGE_TERMINAL_MIN_WINDOW_WIDTH, STORAGE_TERMINAL_MIN_WINDOW_HEIGHT);
        }

        @Override
        public DesktopResizePolicy resizePolicy(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return DesktopResizePolicy.STORAGE_GRID;
        }

        @Override
        public @Nullable DesktopWindowSize snapSize(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int columns = this.terminalColumns(context);
            int rows = this.visibleRows(context);
            int leftPaneWidth = Math.max(
                columns * SLOT + STORAGE_TERMINAL_SCROLLBAR_GAP + STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
                this.crafting ? CRAFTING_TERMINAL_CRAFT_WIDTH : 0
            );
            int contentWidth = leftPaneWidth + STORAGE_TERMINAL_CONTROLS_GAP + BUTTON;
            int gridContentHeight = STORAGE_TERMINAL_SEARCH_HEIGHT + STORAGE_TERMINAL_SEARCH_GRID_GAP + rows * SLOT
                + (this.crafting ? CRAFTING_TERMINAL_CRAFT_GAP + CRAFTING_TERMINAL_CRAFT_HEIGHT : 0);
            int contentHeight = Math.max(STORAGE_TERMINAL_CONTROLS_HEIGHT, gridContentHeight);
            return DesktopWindowSize.of(
                DESKTOP_WINDOW_PADDING * 2 + contentWidth,
                DESKTOP_TOP_BAR_HEIGHT + DESKTOP_WINDOW_PADDING * 2 + contentHeight
            );
        }

        @Override
        public void tick(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            TerminalState state = context.state();
            state.sorting = TomsStorageReflect.intField(context.menu(), "sorting", state.sorting);
            state.modes = TomsStorageReflect.intField(context.menu(), "modes", state.modes);
            state.searchType = TomsStorageReflect.intField(context.menu(), "searchType", state.searchType);
            if (!state.keepSearchRestored) {
                state.keepSearchRestored = true;
                if ((normalizedSearchType(state.searchType) & 2) != 0 && state.pendingKeptSearch != null) {
                    state.search = state.pendingKeptSearch;
                    state.scrollRow = 0;
                    TomsStorageReflect.setField(context.menu(), "search", state.search);
                    DesktopDebug.trace(
                        "Tom's Storage restored kept search session={} key={} search='{}'",
                        context.sessionId(),
                        state.searchMemoryKey,
                        state.search
                    );
                }
                state.pendingKeptSearch = null;
            }
            if (state.searchType != -1 && (state.searchType & 1) != 0) {
                state.searchFocused = true;
            }
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, TerminalState> context) {
            this.renderStorageTerminal(context);
        }

        @Override
        public @Nullable RecipeBookComponent<?> createRecipeBook(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.crafting ? TomsStorageClientReflect.createCraftingTerminalRecipeBook(context.menu()) : null;
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY) {
            if (!this.crafting) {
                return null;
            }

            int x = this.craftingAreaX(context);
            int y = this.craftingAreaY(context);
            DesktopSlotHit output = context.menuSlotHit(0, x + CRAFTING_TERMINAL_CRAFT_OUTPUT_X, y + CRAFTING_TERMINAL_CRAFT_OUTPUT_Y, mouseX, mouseY);
            if (output != null) {
                return output;
            }
            for (int i = 0; i < 9; i++) {
                int slotX = x + CRAFTING_TERMINAL_CRAFT_INPUT_X + i % 3 * SLOT;
                int slotY = y + CRAFTING_TERMINAL_CRAFT_INPUT_Y + i / 3 * SLOT;
                DesktopSlotHit hit = context.menuSlotHit(i + 1, slotX, slotY, mouseX, mouseY);
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, TerminalState> context, MouseButtonEvent event, boolean doubleClick) {
            TerminalState state = context.state();
            int x = this.terminalOriginX(context);
            int y = this.terminalOriginY(context);
            int searchX = x;
            int searchWidth = this.terminalSearchWidth(context);
            if (hitSearch(context, state, event.x(), event.y(), searchX, y, searchWidth)) {
                state.searchFocused = true;
                state.searchPopupOpen = false;
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    state.search = "";
                    sendTerminalSettings(context, state);
                }
                return true;
            }
            state.searchFocused = false;

            if (handleTerminalControlClick(context, state, event, this.terminalControlsX(context), y)) {
                return true;
            }
            int craftingX = this.craftingAreaX(context);
            int craftingY = this.craftingAreaY(context);
            if (this.crafting && this.craftingRecipeBookButtonContains(context, event.x(), event.y())) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (context.toggleRecipeBook() && recipeBookSearchSyncEnabled(state)) {
                        context.setRecipeBookSearch(state.search);
                    }
                }
                return true;
            }
            if (this.crafting && this.craftingOutputContains(event.x(), event.y(), craftingX, craftingY)) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (event.hasShiftDown() || context.shiftDown()) {
                        context.clickSlot(0, 0, ContainerInput.QUICK_MOVE);
                    } else {
                        context.clickSlot(0, event.button(), ContainerInput.PICKUP);
                    }
                }
                return true;
            }
            if (this.crafting && buttonHit(event, craftingX + CRAFTING_TERMINAL_CLEAR_X, craftingY + CRAFTING_TERMINAL_CLEAR_Y, BUTTON, BUTTON)) {
                context.sendMenuButton(0);
                return true;
            }
            if (this.crafting && this.craftingSlotContains(event.x(), event.y(), craftingX, craftingY)) {
                DesktopDebug.trace(
                    "Tom's Storage terminal click passed through session={} mode={} button={} shift={} ctrl={} carried={} reason=normal-crafting-slot",
                    context.sessionId(),
                    controlModeName(state),
                    event.button(),
                    event.hasShiftDown(),
                    event.hasControlDown(),
                    context.carriedStack()
                );
                return false;
            }

            int gridY = this.terminalGridY(context);
            int gridX = this.terminalGridX(context);
            int visibleRows = this.visibleRows(context);
            TerminalEntry entry = terminalEntryAt(context, event.x(), event.y(), gridX, gridY, visibleRows);
            boolean overTerminalGrid = this.terminalGridContains(context, event.x(), event.y(), gridX, gridY, visibleRows);
            TerminalClickAction action = resolveTerminalClick(
                state,
                event,
                context.carriedStack().isEmpty(),
                entry,
                overTerminalGrid,
                keyDown(context, GLFW.GLFW_KEY_SPACE)
            );
            if (action != null) {
                DesktopDebug.trace(
                    "Tom's Storage terminal click consumed session={} mode={} button={} shift={} ctrl={} carried={} grid={} entry={} action={} modifier={} stack={} quantity={}",
                    context.sessionId(),
                    controlModeName(state),
                    event.button(),
                    event.hasShiftDown(),
                    event.hasControlDown(),
                    context.carriedStack(),
                    overTerminalGrid,
                    entry != null,
                    action.action(),
                    action.modifier(),
                    action.stack(),
                    action.quantity()
                );
                sendTerminalAction(context, action.action(), action.modifier(), action.stack(), action.quantity());
                return true;
            }

            if (overTerminalGrid) {
                DesktopDebug.trace(
                    "Tom's Storage terminal grid click consumed without action session={} mode={} button={} shift={} ctrl={} carried={} entry=false",
                    context.sessionId(),
                    controlModeName(state),
                    event.button(),
                    event.hasShiftDown(),
                    event.hasControlDown(),
                    context.carriedStack()
                );
                return true;
            }

            if (entry == null) {
                DesktopDebug.trace(
                    "Tom's Storage terminal click passed through session={} mode={} button={} shift={} ctrl={} carried={} reason=outside-virtual-grid",
                    context.sessionId(),
                    controlModeName(state),
                    event.button(),
                    event.hasShiftDown(),
                    event.hasControlDown(),
                    context.carriedStack()
                );
                return false;
            }

            DesktopDebug.trace(
                "Tom's Storage terminal entry click ignored session={} mode={} button={} shift={} ctrl={} carried={} reason=no-terminal-action",
                context.sessionId(),
                controlModeName(state),
                event.button(),
                event.hasShiftDown(),
                event.hasControlDown(),
                context.carriedStack()
            );
            return false;
        }

        @Override
        public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY, double scrollX, double scrollY) {
            int gridX = this.terminalGridX(context);
            int gridY = this.terminalGridY(context);
            int visibleRows = this.visibleRows(context);
            int columns = this.terminalColumns(context);
            int scrollAreaWidth = this.terminalSearchWidth(context);
            if (!contains(mouseX, mouseY, gridX, gridY, scrollAreaWidth, visibleRows * SLOT)) {
                return false;
            }

            int rows = Math.max(1, rowsForEntries(filteredEntries(context).size(), columns));
            int maxScroll = Math.max(0, rows - visibleRows);
            if (maxScroll <= 0) {
                return false;
            }
            context.state().scrollRow = clamp(context.state().scrollRow + (scrollY < 0 ? 1 : -1), 0, maxScroll);
            return true;
        }

        @Override
        public boolean keyPressed(DesktopInputContext<AbstractContainerMenu, TerminalState> context, KeyEvent event) {
            TerminalState state = context.state();
            if (state.searchPopupOpen) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    state.searchPopupOpen = false;
                    return true;
                }
                return true;
            }
            if (!state.searchFocused) {
                return false;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                state.searchFocused = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !state.search.isEmpty()) {
                state.search = state.search.substring(0, state.search.length() - 1);
                state.scrollRow = 0;
                sendTerminalSettings(context, state);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_DELETE) {
                state.search = "";
                state.scrollRow = 0;
                sendTerminalSettings(context, state);
                return true;
            }
            return true;
        }

        @Override
        public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return context.state().searchFocused;
        }

        @Override
        public boolean charTyped(DesktopInputContext<AbstractContainerMenu, TerminalState> context, CharacterEvent event) {
            TerminalState state = context.state();
            if (!state.searchFocused || !event.isAllowedChatCharacter()) {
                return false;
            }
            state.search += event.codepointAsString();
            state.scrollRow = 0;
            sendTerminalSettings(context, state);
            return true;
        }

        @Override
        public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, int mouseX, int mouseY) {
            if (this.crafting && this.craftingRecipeBookButtonContains(context, mouseX, mouseY)) {
                context.tooltip(Component.translatable("tooltip.toms_storage.recipe_book"), mouseX, mouseY);
                return true;
            }

            List<Component> controlTooltip = terminalControlTooltip(context, context.state(), mouseX, mouseY, this.terminalControlsX(context), this.terminalOriginY(context));
            if (controlTooltip != null) {
                context.tooltip(controlTooltip, mouseX, mouseY);
                return true;
            }

            TerminalEntry entry = terminalEntryAt(context, mouseX, mouseY, this.terminalGridX(context), this.terminalGridY(context), this.visibleRows(context));
            if (entry != null) {
                context.tooltip(entry.stack().copyWithCount((int) Math.min(entry.quantity(), entry.stack().getMaxStackSize())), mouseX, mouseY);
                return true;
            }
            return false;
        }

        @Override
        public void customPayload(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, Identifier channel, byte[] data) {
            if (!TomsStorageCompat.TERMINAL_SNAPSHOT_CHANNEL.equals(channel)) {
                return;
            }

            TomsStoragePayloads.TerminalSnapshot snapshot = TomsStoragePayloads.readTerminalSnapshot(
                context.minecraft().player.registryAccess(),
                data
            );
            context.state().entries.clear();
            context.state().entries.addAll(snapshot.entries());
            context.state().truncated = snapshot.truncated();
            TomsStorageReflect.syncTerminalClientItems(context.menu(), snapshot.entries());
            if (context.recipeBookVisible()) {
                context.refreshRecipeBook();
            }
            DesktopDebug.log(
                "Tom's Storage terminal snapshot received client session={} title='{}' entries={} truncated={} bytes={}",
                context.sessionId(),
                context.originalTitle().getString(),
                snapshot.entries().size(),
                snapshot.truncated(),
                data.length
            );
            TomsStorageCompat.info(
                "client terminal snapshot received session={} title='{}' entries={} truncated={} bytes={}",
                context.sessionId(),
                context.originalTitle().getString(),
                snapshot.entries().size(),
                snapshot.truncated(),
                data.length
            );
        }

        private void renderCraftingArea(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, int x, int y) {
            context.text(Component.literal("Craft"), x, y - 10, 0x404040, false);
            renderRecipeBookButton(context, this.craftingRecipeBookButtonX(context), this.craftingRecipeBookButtonY(context));
            for (int i = 0; i < 9; i++) {
                context.slot(i + 1, x + CRAFTING_TERMINAL_CRAFT_INPUT_X + i % 3 * SLOT, y + CRAFTING_TERMINAL_CRAFT_INPUT_Y + i / 3 * SLOT);
            }
            context.texture(
                CONTAINER_WIDGETS_TEXTURE,
                x + CRAFTING_TERMINAL_CRAFT_ARROW_X,
                y + CRAFTING_TERMINAL_CRAFT_ARROW_Y,
                0,
                14,
                24,
                16,
                24,
                16,
                48,
                30
            );
            context.slot(0, x + CRAFTING_TERMINAL_CRAFT_OUTPUT_X, y + CRAFTING_TERMINAL_CRAFT_OUTPUT_Y);
            renderIconButton(
                context,
                x + CRAFTING_TERMINAL_CLEAR_X,
                y + CRAFTING_TERMINAL_CLEAR_Y,
                TomsStorageCompat.id("widget/clear_button"),
                TomsStorageCompat.id("widget/clear_button_hovered"),
                false
            );
        }

        private void renderStorageTerminal(DesktopRenderContext<AbstractContainerMenu, TerminalState> context) {
            TerminalState state = context.state();
            int x = this.terminalOriginX(context);
            int y = this.terminalOriginY(context);
            int gridY = this.terminalGridY(context);
            int columns = this.terminalColumns(context);
            int rows = this.visibleRows(context);
            int searchWidth = this.terminalSearchWidth(context);
            int gridHeight = rows * SLOT;
            renderTerminalControls(context, state, this.terminalControlsX(context), y);
            renderSearch(context, state, x, y, searchWidth);

            List<TerminalEntry> visibleEntries = filteredEntries(context);
            int totalRows = Math.max(1, rowsForEntries(visibleEntries.size(), columns));
            state.scrollRow = clamp(state.scrollRow, 0, Math.max(0, totalRows - rows));
            if (!state.storageRenderLogged || state.lastRenderedEntryCount != state.entries.size() || state.lastRenderedVisibleCount != visibleEntries.size()) {
                state.storageRenderLogged = true;
                state.lastRenderedEntryCount = state.entries.size();
                state.lastRenderedVisibleCount = visibleEntries.size();
                DesktopDebug.log(
                    "Tom's Storage terminal render client session={} title='{}' window={}x{} content=({}, {}) search='{}' entries={} visible={} columns={} rows={} scrollRow={} controlsX={} grid=({}, {}) searchWidth={}",
                    context.sessionId(),
                    context.originalTitle().getString(),
                    context.windowWidth(),
                    context.windowHeight(),
                    context.contentX(),
                    context.contentY(),
                    state.search,
                    state.entries.size(),
                    visibleEntries.size(),
                    columns,
                    rows,
                    state.scrollRow,
                    this.terminalControlsX(context),
                    x,
                    gridY,
                    searchWidth
                );
                TomsStorageCompat.info(
                    "client terminal render session={} title='{}' window={}x{} content=({}, {}) search='{}' entries={} visible={} columns={} rows={} scrollRow={} controlsX={} grid=({}, {}) searchWidth={}",
                    context.sessionId(),
                    context.originalTitle().getString(),
                    context.windowWidth(),
                    context.windowHeight(),
                    context.contentX(),
                    context.contentY(),
                    state.search,
                    state.entries.size(),
                    visibleEntries.size(),
                    columns,
                    rows,
                    state.scrollRow,
                    this.terminalControlsX(context),
                    x,
                    gridY,
                    searchWidth
                );
            }
            if (!state.search.isBlank() && !state.entries.isEmpty() && visibleEntries.isEmpty()) {
                DesktopDebug.log(
                    "Tom's Storage terminal search filtered all entries client session={} search='{}' totalEntries={}",
                    context.sessionId(),
                    state.search,
                    state.entries.size()
                );
                TomsStorageCompat.warn(
                    "client terminal search filtered all entries session={} search='{}' totalEntries={}",
                    context.sessionId(),
                    state.search,
                    state.entries.size()
                );
            }
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    int index = (state.scrollRow + row) * columns + col;
                    int slotX = x + col * SLOT;
                    int slotY = gridY + row * SLOT;
                    context.slotBackground(slotX, slotY);
                    if (index < visibleEntries.size()) {
                        TerminalEntry entry = visibleEntries.get(index);
                        context.item(entry.stack().copyWithCount(1), slotX, slotY);
                        drawCount(context, entry.quantity(), slotX, slotY);
                    }
                }
            }
            int scrollbarX = x + searchWidth - STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH;
            renderScrollbar(context, scrollbarX, gridY, gridHeight, state.scrollRow, Math.max(0, totalRows - rows));

            if (this.crafting) {
                renderCraftingArea(context, this.craftingAreaX(context), this.craftingAreaY(context));
            }

            if (state.truncated) {
                int warningY = this.crafting
                    ? this.craftingAreaY(context) + CRAFTING_TERMINAL_CRAFT_HEIGHT + 4
                    : gridY + gridHeight + 4;
                context.text(Component.literal("Large network truncated"), x, warningY, 0xFFAA00, false);
            }
            renderTerminalSearchPopup(context, state, this.terminalControlsX(context), y);
        }

        private List<TerminalEntry> filteredEntries(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            TerminalState state = context.state();
            String search = state.search.toLowerCase(Locale.ROOT).trim();
            List<TerminalEntry> entries = new ArrayList<>();
            for (TerminalEntry entry : state.entries) {
                if (search.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)) {
                    entries.add(entry);
                }
            }

            int sortType = state.sorting & 0xFF;
            boolean reversed = (state.sorting & 0x100) != 0;
            Comparator<TerminalEntry> comparator = switch (sortType) {
                case 1 -> Comparator.comparing(entry -> entry.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
                case 2 -> Comparator.comparing(entry -> entry.stack().getItem().toString());
                default -> Comparator.<TerminalEntry>comparingLong(TerminalEntry::quantity).reversed();
            };
            if (reversed) {
                comparator = comparator.reversed();
            }
            entries.sort(comparator);
            return entries;
        }

        private TerminalEntry terminalEntryAt(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY, int gridX, int gridY, int visibleRows) {
            if (!this.terminalGridContains(context, mouseX, mouseY, gridX, gridY, visibleRows)) {
                return null;
            }
            int col = (int) ((mouseX - gridX) / SLOT);
            int row = (int) ((mouseY - gridY) / SLOT);
            int index = (context.state().scrollRow + row) * this.terminalColumns(context) + col;
            List<TerminalEntry> entries = filteredEntries(context);
            return index >= 0 && index < entries.size() ? entries.get(index) : null;
        }

        private boolean terminalGridContains(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY, int gridX, int gridY, int visibleRows) {
            return contains(mouseX, mouseY, gridX, gridY, this.terminalColumns(context) * SLOT, visibleRows * SLOT);
        }

        private int terminalOriginX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return context.contentX();
        }

        private int terminalOriginY(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return context.contentY();
        }

        private int terminalGridX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.terminalOriginX(context);
        }

        private int terminalGridY(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int y = this.terminalOriginY(context);
            return y + STORAGE_TERMINAL_SEARCH_HEIGHT + STORAGE_TERMINAL_SEARCH_GRID_GAP;
        }

        private int terminalControlsX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.terminalOriginX(context) + this.terminalSearchWidth(context) + STORAGE_TERMINAL_CONTROLS_GAP;
        }

        private int terminalSearchWidth(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.storageTerminalLeftPaneWidth(context);
        }

        private int terminalColumns(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int available = this.storageTerminalLeftPaneWidth(context) - STORAGE_TERMINAL_SCROLLBAR_GAP - STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH;
            return Math.max(STORAGE_TERMINAL_MIN_COLUMNS, available / SLOT);
        }

        private int storageTerminalLeftPaneWidth(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int contentWidth = context.windowWidth() - DESKTOP_WINDOW_PADDING * 2;
            int available = contentWidth - STORAGE_TERMINAL_CONTROLS_GAP - BUTTON;
            int minLeftWidth = this.crafting ? CRAFTING_TERMINAL_MIN_LEFT_WIDTH : STORAGE_TERMINAL_MIN_LEFT_WIDTH;
            return Math.max(minLeftWidth, available);
        }

        private int visibleRows(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int contentHeight = context.windowHeight() - DESKTOP_TOP_BAR_HEIGHT - DESKTOP_WINDOW_PADDING * 2;
            int available = contentHeight
                - STORAGE_TERMINAL_SEARCH_HEIGHT
                - STORAGE_TERMINAL_SEARCH_GRID_GAP
                - (this.crafting ? CRAFTING_TERMINAL_CRAFT_GAP + CRAFTING_TERMINAL_CRAFT_HEIGHT : 0);
            return Math.max(STORAGE_TERMINAL_MIN_ROWS, available / SLOT);
        }

        private int craftingAreaX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int gridWidth = this.terminalColumns(context) * SLOT;
            return this.terminalGridX(context) + Math.max(0, (gridWidth - CRAFTING_TERMINAL_CRAFT_WIDTH) / 2);
        }

        private int craftingAreaY(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.terminalGridY(context) + this.visibleRows(context) * SLOT + CRAFTING_TERMINAL_CRAFT_GAP;
        }

        private boolean craftingSlotContains(double mouseX, double mouseY, int x, int y) {
            if (this.craftingOutputContains(mouseX, mouseY, x, y)) {
                return true;
            }
            for (int i = 0; i < 9; i++) {
                int slotX = x + CRAFTING_TERMINAL_CRAFT_INPUT_X + i % 3 * SLOT;
                int slotY = y + CRAFTING_TERMINAL_CRAFT_INPUT_Y + i / 3 * SLOT;
                if (contains(mouseX, mouseY, slotX, slotY, SLOT, SLOT)) {
                    return true;
                }
            }
            return false;
        }

        private boolean craftingOutputContains(double mouseX, double mouseY, int x, int y) {
            return contains(mouseX, mouseY, x + CRAFTING_TERMINAL_CRAFT_OUTPUT_X, y + CRAFTING_TERMINAL_CRAFT_OUTPUT_Y, SLOT, SLOT);
        }

        private int craftingRecipeBookButtonX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            int left = this.terminalGridX(context);
            int craftInputX = this.craftingAreaX(context) + CRAFTING_TERMINAL_CRAFT_INPUT_X;
            int preferred = left + CRAFTING_TERMINAL_RECIPE_BUTTON_LEFT_GUTTER_X;
            int maxBeforeGrid = craftInputX - CRAFTING_TERMINAL_RECIPE_BUTTON_WIDTH - 6;
            return clamp(Math.min(preferred, maxBeforeGrid), left, Math.max(left, maxBeforeGrid));
        }

        private int craftingRecipeBookButtonY(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
            return this.craftingAreaY(context) + CRAFTING_TERMINAL_RECIPE_BUTTON_Y;
        }

        private boolean craftingRecipeBookButtonContains(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY) {
            return contains(
                mouseX,
                mouseY,
                this.craftingRecipeBookButtonX(context),
                this.craftingRecipeBookButtonY(context),
                CRAFTING_TERMINAL_RECIPE_BUTTON_WIDTH,
                CRAFTING_TERMINAL_RECIPE_BUTTON_HEIGHT
            );
        }

        private static void renderRecipeBookButton(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, int x, int y) {
            boolean hovered = contains(context.mouseX(), context.mouseY(), x, y, CRAFTING_TERMINAL_RECIPE_BUTTON_WIDTH, CRAFTING_TERMINAL_RECIPE_BUTTON_HEIGHT);
            Identifier sprite = RecipeBookComponent.RECIPE_BUTTON_SPRITES.get(true, hovered || context.recipeBookVisible());
            context.sprite(sprite, x, y, CRAFTING_TERMINAL_RECIPE_BUTTON_WIDTH, CRAFTING_TERMINAL_RECIPE_BUTTON_HEIGHT);
        }
    }

    public static final class InventoryConfigurator implements Definition<AbstractContainerMenu, Void> {
        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(154, 82);
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, Void> context) {
            int x = context.contentX() + 34;
            int y = context.contentY() + 24;
            context.text(Component.literal("Connector Filter"), context.contentX() + PAD, context.contentY() + PAD, 0x404040, false);
            context.slotBackground(x, y);
            context.slot(0, x, y);
            int bx = context.contentX() + PAD;
            int by = y;
            renderCycleButton(context, bx, by, priorityIcon(context.menu()), false);
            renderCycleButton(context, bx + 18, by, TomsStorageCompat.id("icons/add"), false);
            renderCycleButton(context, bx + 36, by, TomsStorageCompat.id("icons/deny"), false);
            renderCycleButton(context, bx + 54, by, sideIcon(context.menu()), false);
            renderCycleButton(context, bx + 72, by, TomsStorageCompat.id(skip(context.menu()) ? "icons/skip_inv" : "icons/include_inv"), skip(context.menu()));
            renderCycleButton(context, bx + 90, by, TomsStorageCompat.id(keepLast(context.menu()) ? "icons/keep_last_1" : "icons/keep_last_off"), keepLast(context.menu()));
            renderCycleButton(context, bx + 108, by, TomsStorageCompat.id("icons/deny"), false);
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> context, double mouseX, double mouseY) {
            return context.containerSlotHit(0, context.contentX() + 34, context.contentY() + 24, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, Void> context, MouseButtonEvent event, boolean doubleClick) {
            int bx = context.contentX() + PAD;
            int y = context.contentY() + 24;
            int index = buttonIndex(event, bx, y, 7);
            if (index < 0) {
                return false;
            }
            switch (index) {
                case 0 -> context.sendMenuButton((((TomsStorageReflect.enumOrdinalField(context.menu(), "priority", 2) + 1) % 5) << 3));
                case 1 -> context.sendMenuButton(1);
                case 2 -> context.sendMenuButton(2);
                case 3 -> context.sendMenuButton((((TomsStorageReflect.enumOrdinalField(context.menu(), "side", 0) + 1) % 6) << 3) | 3);
                case 4 -> context.sendMenuButton((skip(context.menu()) ? 0 : 1) << 3 | 4);
                case 5 -> context.sendMenuButton((keepLast(context.menu()) ? 0 : 1) << 3 | 5);
                case 6 -> context.sendMenuButton(7);
                default -> {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class LevelEmitter implements Definition<AbstractContainerMenu, LevelEmitterState> {
        @Override
        public LevelEmitterState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return new LevelEmitterState();
        }

        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(150, 86);
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, LevelEmitterState> context) {
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            int count = TomsStorageReflect.intField(context.menu(), "count", 0);
            boolean lessThan = TomsStorageReflect.booleanField(context.menu(), "lessThan", false);
            context.slot(0, x, y + 24);
            context.text(Component.literal(lessThan ? "Emit below" : "Emit above"), x + 28, y + 10, 0x404040, false);
            context.fill(x + 28, y + 25, x + 112, y + 38, 0xFF8F8F8F);
            context.text(Integer.toString(count), x + 32, y + 28, 0xFFFFFF, false);
            renderCycleButton(context, x + 116, y + 24, TomsStorageCompat.id(lessThan ? "icons/less_than" : "icons/greater_than"), lessThan);
            for (int i = 0; i < 4; i++) {
                int value = switch (i) {
                    case 0 -> -10;
                    case 1 -> -1;
                    case 2 -> 1;
                    default -> 10;
                };
                int bx = x + 28 + i * 24;
                context.fill(bx, y + 46, bx + 22, y + 60, 0xFF777777);
                context.text((value > 0 ? "+" : "") + value, bx + 3, y + 49, 0xFFFFFF, false);
            }
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, LevelEmitterState> context, double mouseX, double mouseY) {
            return context.containerSlotHit(0, context.contentX() + PAD, context.contentY() + PAD + 24, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, LevelEmitterState> context, MouseButtonEvent event, boolean doubleClick) {
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            if (buttonHit(event, x + 116, y + 24, BUTTON, BUTTON)) {
                sendLevelEmitter(context, TomsStorageReflect.intField(context.menu(), "count", 0), !TomsStorageReflect.booleanField(context.menu(), "lessThan", false));
                return true;
            }
            for (int i = 0; i < 4; i++) {
                int value = switch (i) {
                    case 0 -> -10;
                    case 1 -> -1;
                    case 2 -> 1;
                    default -> 10;
                };
                int bx = x + 28 + i * 24;
                if (buttonHit(event, bx, y + 46, 22, 14)) {
                    int count = Math.max(0, TomsStorageReflect.intField(context.menu(), "count", 0) + value);
                    sendLevelEmitter(context, count, TomsStorageReflect.booleanField(context.menu(), "lessThan", false));
                    return true;
                }
            }
            return false;
        }
    }

    public static final class InventoryLink implements Definition<AbstractContainerMenu, InventoryLinkState> {
        @Override
        public InventoryLinkState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return new InventoryLinkState();
        }

        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(192, 146);
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, InventoryLinkState> context) {
            InventoryLinkState state = context.state();
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            context.text(Component.literal("Beacon " + TomsStorageReflect.intField(context.menu(), "beaconLvl", 0)), x, y, 0x404040, false);
            renderTextBox(context, state.name, x, y + 14, 106, state.nameFocused);
            renderCycleButton(context, x + 110, y + 13, TomsStorageCompat.id("icons/add"), false);
            renderCycleButton(context, x + 128, y + 13, TomsStorageCompat.id("icons/deny"), false);
            renderCycleButton(context, x + 146, y + 13, TomsStorageCompat.id(state.publicChannel ? "icons/allow" : "icons/deny"), state.publicChannel);
            int listY = y + 34;
            for (int i = 0; i < 6; i++) {
                int index = state.scroll + i;
                int rowY = listY + i * 16;
                boolean selected = index < state.channels.size() && state.channels.get(index).id().equals(state.selected);
                context.fill(x, rowY, x + 164, rowY + 15, selected ? 0xFF7A7A7A : 0xFF9A9A9A);
                if (index < state.channels.size()) {
                    LinkChannel channel = state.channels.get(index);
                    context.text(channel.name(), x + 4, rowY + 3, 0xFFFFFF, false);
                    context.sprite(TomsStorageCompat.id(channel.publicChannel() ? "icons/allow" : "icons/deny"), x + 146, rowY, 14, 14);
                }
            }
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, InventoryLinkState> context, MouseButtonEvent event, boolean doubleClick) {
            InventoryLinkState state = context.state();
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            if (contains(event.x(), event.y(), x, y + 14, 106, 13)) {
                state.nameFocused = true;
                return true;
            }
            state.nameFocused = false;
            if (buttonHit(event, x + 110, y + 13, BUTTON, BUTTON)) {
                CompoundTag tag = new CompoundTag();
                tag.putString("d", state.name.isBlank() ? "Channel" : state.name);
                tag.putBoolean("p", state.publicChannel);
                context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
                return true;
            }
            if (buttonHit(event, x + 128, y + 13, BUTTON, BUTTON) && state.selected != null) {
                CompoundTag tag = new CompoundTag();
                tag.store("id", UUIDUtil.CODEC, state.selected);
                context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
                return true;
            }
            if (buttonHit(event, x + 146, y + 13, BUTTON, BUTTON)) {
                state.publicChannel = !state.publicChannel;
                if (state.selected != null) {
                    CompoundTag tag = new CompoundTag();
                    tag.store("id", UUIDUtil.CODEC, state.selected);
                    tag.putBoolean("p", state.publicChannel);
                    context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
                }
                return true;
            }
            int listY = y + 34;
            if (contains(event.x(), event.y(), x, listY, 164, 96)) {
                int index = state.scroll + (int) ((event.y() - listY) / 16);
                if (index >= 0 && index < state.channels.size()) {
                    LinkChannel channel = state.channels.get(index);
                    state.selected = channel.id();
                    state.publicChannel = channel.publicChannel();
                    state.name = channel.name();
                    CompoundTag tag = new CompoundTag();
                    tag.store("id", UUIDUtil.CODEC, state.selected);
                    tag.putBoolean("select", true);
                    context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu, InventoryLinkState> context, double mouseX, double mouseY, double scrollX, double scrollY) {
            InventoryLinkState state = context.state();
            int max = Math.max(0, state.channels.size() - 6);
            if (max <= 0) {
                return false;
            }
            state.scroll = clamp(state.scroll + (scrollY < 0 ? 1 : -1), 0, max);
            return true;
        }

        @Override
        public boolean keyPressed(DesktopInputContext<AbstractContainerMenu, InventoryLinkState> context, KeyEvent event) {
            InventoryLinkState state = context.state();
            if (!state.nameFocused) {
                return false;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                state.nameFocused = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !state.name.isEmpty()) {
                state.name = state.name.substring(0, state.name.length() - 1);
                return true;
            }
            return true;
        }

        @Override
        public boolean charTyped(DesktopInputContext<AbstractContainerMenu, InventoryLinkState> context, CharacterEvent event) {
            if (!context.state().nameFocused || !event.isAllowedChatCharacter()) {
                return false;
            }
            if (context.state().name.length() < 50) {
                context.state().name += event.codepointAsString();
            }
            return true;
        }

        @Override
        public void customPayload(DesktopWindowContext<AbstractContainerMenu, InventoryLinkState> context, Identifier channel, byte[] data) {
            if (!TomsStorageCompat.LINK_SNAPSHOT_CHANNEL.equals(channel)) {
                return;
            }
            TomsStoragePayloads.LinkSnapshot snapshot = TomsStoragePayloads.readLinkSnapshot(context.minecraft().player.registryAccess(), data);
            context.state().channels.clear();
            context.state().channels.addAll(snapshot.channels());
            context.state().selected = snapshot.selected();
            context.state().scroll = clamp(context.state().scroll, 0, Math.max(0, context.state().channels.size() - 6));
        }
    }

    public static final class ItemFilter implements Definition<AbstractContainerMenu, Void> {
        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(108, 98);
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, Void> context) {
            int x = context.contentX() + 28;
            int y = context.contentY() + PAD;
            renderToggleColumn(context, context.contentX() + PAD, y, new Identifier[] {
                TomsStorageCompat.id(TomsStorageReflect.booleanField(context.menu(), "allowList", false) ? "icons/allow" : "icons/deny"),
                TomsStorageCompat.id(TomsStorageReflect.booleanField(context.menu(), "matchNBT", false) ? "icons/match_tag_on" : "icons/match_tag_off")
            });
            for (int i = 0; i < 9; i++) {
                context.slot(i, x + i % 3 * SLOT, y + i / 3 * SLOT);
            }
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> context, double mouseX, double mouseY) {
            int x = context.contentX() + 28;
            int y = context.contentY() + PAD;
            for (int i = 0; i < 9; i++) {
                DesktopSlotHit hit = context.menuSlotHit(i, x + i % 3 * SLOT, y + i / 3 * SLOT, mouseX, mouseY);
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, Void> context, MouseButtonEvent event, boolean doubleClick) {
            int index = buttonIndex(event, context.contentX() + PAD, context.contentY() + PAD, 2);
            if (index == 0) {
                context.sendMenuButton((1 << 1) | (TomsStorageReflect.booleanField(context.menu(), "allowList", false) ? 0 : 1));
                return true;
            }
            if (index == 1) {
                context.sendMenuButton(TomsStorageReflect.booleanField(context.menu(), "matchNBT", false) ? 0 : 1);
                return true;
            }
            return false;
        }
    }

    public static final class TagItemFilter implements Definition<AbstractContainerMenu, TagFilterState> {
        @Override
        public TagFilterState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return new TagFilterState();
        }

        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(198, 104);
        }

        @Override
        public void tick(DesktopWindowContext<AbstractContainerMenu, TagFilterState> context) {
            Slot slot = context.menuSlot(0);
            List<String> itemTags = slot == null || slot.getItem().isEmpty()
                ? List.of()
                : slot.getItem().tags().map(tag -> tag.location().toString()).toList();
            if (!context.state().availableTags.equals(itemTags)) {
                context.state().availableTags.clear();
                context.state().availableTags.addAll(itemTags);
            }
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, TagFilterState> context) {
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            renderCycleButton(context, x, y, TomsStorageCompat.id(TomsStorageReflect.booleanField(context.menu(), "allowList", false) ? "icons/allow" : "icons/deny"), false);
            context.slot(0, x, y + 22);
            renderTagList(context, context.state().availableTags, context.state().selectedAvailable, x + 28, y, 66, 64);
            renderCycleButton(context, x + 98, y + 12, TomsStorageCompat.id("icons/add"), false);
            renderCycleButton(context, x + 98, y + 32, TomsStorageCompat.id("icons/deny"), false);
            renderTagList(context, context.state().filterTags, context.state().selectedFilter, x + 118, y, 66, 64);
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, TagFilterState> context, double mouseX, double mouseY) {
            return context.menuSlotHit(0, context.contentX() + PAD, context.contentY() + PAD + 22, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, TagFilterState> context, MouseButtonEvent event, boolean doubleClick) {
            TagFilterState state = context.state();
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            if (buttonHit(event, x, y, BUTTON, BUTTON)) {
                context.sendMenuButton(TomsStorageReflect.booleanField(context.menu(), "allowList", false) ? 0 : 1);
                return true;
            }
            if (buttonHit(event, x + 98, y + 12, BUTTON, BUTTON) && state.selectedAvailable != null) {
                if (!state.filterTags.contains(state.selectedAvailable)) {
                    state.filterTags.add(state.selectedAvailable);
                }
                sendTagFilter(context, state.filterTags);
                return true;
            }
            if (buttonHit(event, x + 98, y + 32, BUTTON, BUTTON) && state.selectedFilter != null) {
                state.filterTags.remove(state.selectedFilter);
                state.selectedFilter = null;
                sendTagFilter(context, state.filterTags);
                return true;
            }
            String available = pickTag(state.availableTags, event, x + 28, y, 66, 64);
            if (available != null) {
                state.selectedAvailable = available;
                return true;
            }
            String filter = pickTag(state.filterTags, event, x + 118, y, 66, 64);
            if (filter != null) {
                state.selectedFilter = filter;
                return true;
            }
            return false;
        }

        @Override
        public void customPayload(DesktopWindowContext<AbstractContainerMenu, TagFilterState> context, Identifier channel, byte[] data) {
            if (!TomsStorageCompat.TAG_SNAPSHOT_CHANNEL.equals(channel)) {
                return;
            }
            context.state().filterTags.clear();
            context.state().filterTags.addAll(TomsStoragePayloads.readTagSnapshot(data));
        }
    }

    public static final class FilingCabinet implements Definition<AbstractContainerMenu, FilingCabinetState> {
        @Override
        public FilingCabinetState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return new FilingCabinetState();
        }

        @Override
        public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(196, 126);
        }

        @Override
        public void render(DesktopRenderContext<AbstractContainerMenu, FilingCabinetState> context) {
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    context.slot(row * 9 + col, x + col * SLOT, y + row * SLOT);
                }
            }
            int max = Math.max(0, Math.max(0, context.menu().slots.size() - 36) / 9 - 5);
            renderScrollbar(context, x + 9 * SLOT + 6, y, 5 * SLOT, context.state().scrollRow, max);
        }

        @Override
        public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, FilingCabinetState> context, double mouseX, double mouseY) {
            int x = context.contentX() + PAD;
            int y = context.contentY() + PAD;
            for (int i = 0; i < 45; i++) {
                DesktopSlotHit hit = context.menuSlotHit(i, x + i % 9 * SLOT, y + i / 9 * SLOT, mouseX, mouseY);
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }

        @Override
        public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu, FilingCabinetState> context, double mouseX, double mouseY, double scrollX, double scrollY) {
            int max = Math.max(0, Math.max(0, context.menu().slots.size() - 36) / 9 - 5);
            if (max <= 0) {
                return false;
            }
            context.state().scrollRow = clamp(context.state().scrollRow + (scrollY < 0 ? 1 : -1), 0, max);
            context.sendMenuButton(context.state().scrollRow);
            return true;
        }
    }

    public static final class TerminalState {
        private final List<TerminalEntry> entries = new ArrayList<>();
        private String search = "";
        private int scrollRow;
        private int sorting;
        private int modes;
        private int searchType;
        private boolean searchFocused;
        private boolean searchPopupOpen;
        private boolean truncated;
        private boolean storageRenderLogged;
        private int lastRenderedEntryCount = -1;
        private int lastRenderedVisibleCount = -1;
        private @Nullable String searchMemoryKey;
        private @Nullable String pendingKeptSearch;
        private boolean keepSearchRestored;
    }

    public static final class LevelEmitterState {
    }

    public static final class InventoryLinkState {
        private final List<LinkChannel> channels = new ArrayList<>();
        private UUID selected;
        private String name = "";
        private boolean publicChannel;
        private boolean nameFocused;
        private int scroll;
    }

    public static final class TagFilterState {
        private final List<String> availableTags = new ArrayList<>();
        private final List<String> filterTags = new ArrayList<>();
        private String selectedAvailable;
        private String selectedFilter;
    }

    public static final class FilingCabinetState {
        private int scrollRow;
    }

    private static void renderTerminalControls(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, TerminalState state, int x, int y) {
        renderCycleButton(context, x, y, TomsStorageCompat.id(sortIcon(state.sorting & 0xFF)), false);
        renderCycleButton(context, x, y + 18, TomsStorageCompat.id((state.sorting & 0x100) != 0 ? "icons/sort_asc" : "icons/sort_desc"), false);
        renderSearchModeButton(context, x, y + 36, state.searchType);
        renderCycleButton(context, x, y + 54, TomsStorageCompat.id(controlIcon(state.modes & 0xF)), false);
        renderCycleButton(context, x, y + 72, TomsStorageCompat.id((state.sorting & 0x200) == 0 ? "icons/keep_last_0" : "icons/keep_last_off"), false);
    }

    private static boolean handleTerminalControlClick(DesktopInputContext<AbstractContainerMenu, TerminalState> context, TerminalState state, MouseButtonEvent event, int x, int y) {
        if (state.searchPopupOpen) {
            int popupIndex = searchPopupIndex(context, event.x(), event.y(), x, y);
            if (popupIndex >= 0) {
                state.searchType = flipSearchBit(normalizedSearchType(state.searchType), switch (popupIndex) {
                    case 0 -> 1;
                    case 1 -> 2;
                    case 2 -> 4;
                    case 3 -> 8;
                    default -> 0;
                });
                sendTerminalSettings(context, state);
                return true;
            }
            if (buttonIndex(event, x, y, STORAGE_TERMINAL_CONTROL_COUNT) < 0) {
                state.searchPopupOpen = false;
                return true;
            }
        }

        int index = buttonIndex(event, x, y, STORAGE_TERMINAL_CONTROL_COUNT);
        if (index < 0) {
            return false;
        }
        switch (index) {
            case 0 -> {
                state.searchPopupOpen = false;
                int sort = state.sorting & 0xFF;
                state.sorting = (state.sorting & ~0xFF) | cycle(sort, 3, event.hasShiftDown() ? -1 : 1);
            }
            case 1 -> state.sorting ^= 0x100;
            case 2 -> {
                state.searchPopupOpen = !state.searchPopupOpen;
                return true;
            }
            case 3 -> {
                state.searchPopupOpen = false;
                int mode = state.modes & 0xF;
                state.modes = (state.modes & ~0xF) | cycle(mode, 3, event.hasShiftDown() ? -1 : 1);
            }
            case 4 -> {
                state.searchPopupOpen = false;
                state.sorting ^= 0x200;
            }
            default -> {
                return false;
            }
        }
        sendTerminalSettings(context, state);
        return true;
    }

    private static int cycle(int value, int size, int step) {
        return Math.floorMod(value + step, size);
    }

    private static int flipSearchBit(int searchType, int bit) {
        return (searchType & bit) != 0 ? searchType & ~bit : searchType | bit;
    }

    private static void sendTerminalSettings(DesktopInputContext<AbstractContainerMenu, TerminalState> context, TerminalState state) {
        TomsStorageReflect.setIntField(context.menu(), "sorting", state.sorting);
        TomsStorageReflect.setIntField(context.menu(), "searchType", state.searchType);
        TomsStorageReflect.setIntField(context.menu(), "modes", state.modes);
        TomsStorageReflect.setField(context.menu(), "search", state.search);
        syncKeptSearchMemory(context, state);
        CompoundTag tag = new CompoundTag();
        CompoundTag child = new CompoundTag();
        child.putInt("s", state.sorting);
        child.putInt("st", state.searchType);
        child.putInt("m", state.modes);
        tag.put("c", child);
        tag.putString("s", state.search);
        if (recipeBookSearchSyncEnabled(state)) {
            context.setRecipeBookSearch(state.search);
        }
        context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
    }

    private static boolean recipeBookSearchSyncEnabled(TerminalState state) {
        return (normalizedSearchType(state.searchType) & 4) != 0;
    }

    private static void syncKeptSearchMemory(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, TerminalState state) {
        String key = state.searchMemoryKey;
        if (key == null || key.isBlank()) {
            key = terminalSearchMemoryKey(context);
            state.searchMemoryKey = key;
        }

        if ((normalizedSearchType(state.searchType) & 2) != 0) {
            KEPT_TERMINAL_SEARCHES.put(key, state.search);
            DesktopDebug.trace("Tom's Storage kept search save session={} key={} search='{}'", context.sessionId(), key, state.search);
        } else {
            KEPT_TERMINAL_SEARCHES.remove(key);
        }
    }

    private static String terminalSearchMemoryKey(DesktopWindowSetupContext<AbstractContainerMenu> context) {
        String sourceKey = context.sourceKey();
        if (sourceKey != null && !sourceKey.isBlank()) {
            return "source:" + sourceKey;
        }
        return "menu:" + context.menu().getType() + ":" + context.originalTitle().getString();
    }

    private static String terminalSearchMemoryKey(DesktopWindowContext<AbstractContainerMenu, TerminalState> context) {
        String sourceKey = context.sourceKey();
        if (sourceKey != null && !sourceKey.isBlank()) {
            return "source:" + sourceKey;
        }
        return "menu:" + context.menu().getType() + ":" + context.originalTitle().getString();
    }

    private static @Nullable List<Component> terminalControlTooltip(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, TerminalState state, int mouseX, int mouseY, int x, int y) {
        if (state.searchPopupOpen) {
            int popupIndex = searchPopupIndex(context, mouseX, mouseY, x, y);
            if (popupIndex >= 0 || searchPopupContains(context, mouseX, mouseY, x, y)) {
                return List.of();
            }
        }

        int index = buttonIndex(mouseX, mouseY, x, y, STORAGE_TERMINAL_CONTROL_COUNT);
        return switch (index) {
            case 0 -> List.of(Component.translatable("tooltip.toms_storage.sorting_" + sortingTooltipSuffix(state.sorting & 0xFF)));
            case 1 -> List.of(Component.translatable("narrator.toms_storage.terminal_sort_rev"));
            case 2 -> List.of(Component.translatable(searchTooltipKey(normalizedSearchType(state.searchType)), Component.translatable("tooltip.toms_storage.recipe_book")));
            case 3 -> controlModeTooltip(state.modes & 0xF);
            case 4 -> List.of(Component.translatable((state.sorting & 0x200) == 0 ? "tooltip.toms_storage.ghostMode_on" : "tooltip.toms_storage.ghostMode_off"));
            default -> null;
        };
    }

    private static String sortingTooltipSuffix(int sortType) {
        return switch (sortType) {
            case 1 -> "name";
            case 2 -> "by_mod";
            default -> "amount";
        };
    }

    private static String controlTooltipSuffix(int controlMode) {
        return switch (controlMode) {
            case 1 -> "ae";
            case 2 -> "rs";
            default -> "def";
        };
    }

    private static List<Component> controlModeTooltip(int controlMode) {
        return java.util.Arrays.stream(I18n.get("tooltip.toms_storage.ctrlMode_" + controlTooltipSuffix(controlMode)).split("\\\\"))
            .map(Component::literal)
            .map(Component.class::cast)
            .toList();
    }

    private static String searchTooltipKey(int searchType) {
        StringBuilder key = new StringBuilder("tooltip.toms_storage.search");
        if ((searchType & 1) != 0) {
            key.append("_auto");
        }
        if ((searchType & 2) != 0) {
            key.append("_keep");
        }
        if ((searchType & 4) != 0) {
            key.append("_sync");
        }
        return key.toString();
    }

    private static int normalizedSearchType(int searchType) {
        return Math.max(0, searchType);
    }

    private static boolean isPullOne(TerminalState state, MouseButtonEvent event, boolean hasCarriedStack) {
        return switch (state.modes & 0xF) {
            case 1 -> event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.hasShiftDown();
            case 2 -> event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default -> event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        };
    }

    private static boolean isTransferOne(TerminalState state, MouseButtonEvent event) {
        return switch (state.modes & 0xF) {
            case 1 -> event.hasShiftDown() && event.hasControlDown();
            case 2 -> event.hasShiftDown() && event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default -> event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.hasShiftDown();
        };
    }

    private static boolean pullHalf(TerminalState state, MouseButtonEvent event, boolean carriedEmpty) {
        return switch (state.modes & 0xF) {
            case 1, 2 -> event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            default -> event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && carriedEmpty;
        };
    }

    private static boolean pullNormal(TerminalState state, MouseButtonEvent event) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    private static @Nullable TerminalClickAction resolveTerminalClick(
        TerminalState state,
        MouseButtonEvent event,
        boolean carriedEmpty,
        @Nullable TerminalEntry entry,
        boolean overTerminalGrid,
        boolean spaceDown
    ) {
        if (entry == null) {
            if (overTerminalGrid) {
                if (pullNormal(state, event) && !carriedEmpty) {
                    return new TerminalClickAction("PULL_OR_PUSH_STACK", false, ItemStack.EMPTY, 0L);
                }
                return null;
            }
            return spaceDown ? new TerminalClickAction("SPACE_CLICK", false, ItemStack.EMPTY, 0L) : null;
        }

        if (isPullOne(state, event, !carriedEmpty)) {
            return new TerminalClickAction("PULL_ONE", isTransferOne(state, event), entry.stack(), entry.quantity());
        }
        if (pullHalf(state, event, carriedEmpty)) {
            String action = event.hasControlDown() ? "GET_QUARTER" : "GET_HALF";
            return carriedEmpty
                ? new TerminalClickAction(action, false, entry.stack(), entry.quantity())
                : new TerminalClickAction(action, false, ItemStack.EMPTY, 0L);
        }
        if (pullNormal(state, event)) {
            if (carriedEmpty) {
                return new TerminalClickAction(event.hasShiftDown() ? "SHIFT_PULL" : "PULL_OR_PUSH_STACK", false, entry.stack(), entry.quantity());
            }
            return new TerminalClickAction("PULL_OR_PUSH_STACK", false, ItemStack.EMPTY, 0L);
        }
        return null;
    }

    private static String controlModeName(TerminalState state) {
        return switch (state.modes & 0xF) {
            case 1 -> "AE";
            case 2 -> "RS";
            default -> "DEF";
        };
    }

    private record TerminalClickAction(String action, boolean modifier, ItemStack stack, long quantity) {
    }

    private static void sendTerminalAction(
        DesktopInputContext<AbstractContainerMenu, TerminalState> context,
        String action,
        boolean modifier,
        ItemStack stack,
        long quantity
    ) {
        context.sendPayload(
            TomsStorageCompat.TERMINAL_ACTION_CHANNEL,
            TomsStoragePayloads.writeTerminalAction(context.minecraft().player.registryAccess(), action, modifier, stack, quantity)
        );
    }

    private static void sendLevelEmitter(DesktopInputContext<AbstractContainerMenu, LevelEmitterState> context, int count, boolean lessThan) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("count", count);
        tag.store("lessThan", Codec.BOOL, lessThan);
        context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
    }

    private static void sendTagFilter(DesktopInputContext<AbstractContainerMenu, TagFilterState> context, List<String> tags) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String value : tags) {
            list.add(StringTag.valueOf(value));
        }
        tag.put("l", list);
        context.sendPayload(TomsStorageCompat.NBT_CHANNEL, TomsStoragePayloads.writeNbt(tag));
    }

    private static void renderSearch(DesktopRenderContext<?, ?> context, TerminalState state, int x, int y, int width) {
        renderTextBox(context, state.search, x, y, width, state.searchFocused);
    }

    private static boolean hitSearch(DesktopInputContext<?, ?> context, TerminalState state, double mouseX, double mouseY, int x, int y, int width) {
        return contains(mouseX, mouseY, x, y, width, SEARCH_BAR_TEXTURE_HEIGHT);
    }

    private static void renderTextBox(DesktopRenderContext<?, ?> context, String text, int x, int y, int width, boolean focused) {
        DesktopWidgets.renderTextBox(context, text, focused, x, y, width);
    }

    private static void renderTerminalSearchPopup(DesktopRenderContext<AbstractContainerMenu, TerminalState> context, TerminalState state, int controlsX, int controlsY) {
        if (!state.searchPopupOpen) {
            return;
        }

        int x = searchPopupX(context, controlsX);
        int y = searchPopupY(controlsY);
        int width = searchPopupWidth(context);
        int height = searchPopupHeight();
        context.windowNineSlice(SALT_WINDOW_TEXTURE, x, y, width, height);
        for (int i = 0; i < SEARCH_POPUP_ROWS; i++) {
            int rowY = y + SEARCH_POPUP_PADDING + i * SEARCH_POPUP_ROW_HEIGHT;
            int rowX = x + SEARCH_POPUP_PADDING;
            int rowWidth = width - SEARCH_POPUP_PADDING * 2;
            int buttonX = x + width - SEARCH_POPUP_PADDING - SEARCH_POPUP_STATE_BUTTON_WIDTH;
            int buttonY = rowY + (SEARCH_POPUP_ROW_HEIGHT - SEARCH_POPUP_STATE_BUTTON_HEIGHT) / 2;
            boolean hovered = contains(context.mouseX(), context.mouseY(), rowX, rowY, rowWidth, SEARCH_POPUP_ROW_HEIGHT);
            boolean buttonHovered = contains(context.mouseX(), context.mouseY(), buttonX, buttonY, SEARCH_POPUP_STATE_BUTTON_WIDTH, SEARCH_POPUP_STATE_BUTTON_HEIGHT);
            if (hovered) {
                context.fill(rowX, rowY, rowX + rowWidth, rowY + SEARCH_POPUP_ROW_HEIGHT, 0x22999999);
            }
            int labelWidth = buttonX - rowX - 6;
            context.text(shortenToWidth(context, searchPopupLabel(i), labelWidth), rowX + 2, rowY + 5, hovered ? 0xFF111111 : 0xFF303030, false);
            renderSearchPopupStateButton(context, state, i, buttonX, buttonY, buttonHovered);
        }
    }

    private static int searchPopupIndex(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY, int controlsX, int controlsY) {
        return searchPopupIndex(mouseX, mouseY, searchPopupX(context, controlsX), searchPopupY(controlsY), searchPopupWidth(context));
    }

    private static int searchPopupIndex(double mouseX, double mouseY, int x, int y, int width) {
        if (!contains(mouseX, mouseY, x, y, width, searchPopupHeight())) {
            return -1;
        }
        int row = (int) ((mouseY - y - SEARCH_POPUP_PADDING) / SEARCH_POPUP_ROW_HEIGHT);
        return row >= 0 && row < SEARCH_POPUP_ROWS ? row : -1;
    }

    private static boolean searchPopupContains(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, double mouseX, double mouseY, int controlsX, int controlsY) {
        return contains(mouseX, mouseY, searchPopupX(context, controlsX), searchPopupY(controlsY), searchPopupWidth(context), searchPopupHeight());
    }

    private static int searchPopupX(DesktopWindowContext<AbstractContainerMenu, TerminalState> context, int controlsX) {
        int width = searchPopupWidth(context);
        return controlsX < context.windowX() + 24 ? controlsX + BUTTON + 2 : controlsX - width - 2;
    }

    private static int searchPopupY(int controlsY) {
        return controlsY + 36 + BUTTON + 1;
    }

    private static Component searchPopupLine(TerminalState state, int index) {
        int searchType = normalizedSearchType(state.searchType);
        Component recipeBook = Component.translatable("tooltip.toms_storage.recipe_book");
        return switch (index) {
            case 0 -> Component.translatable("tooltip.toms_storage.opt.search_auto", (searchType & 1) != 0 ? CommonComponents.GUI_YES : CommonComponents.GUI_NO);
            case 1 -> Component.translatable("tooltip.toms_storage.opt.search_keep", (searchType & 2) != 0 ? CommonComponents.GUI_YES : CommonComponents.GUI_NO);
            case 2 -> Component.translatable("tooltip.toms_storage.opt.search_sync", recipeBook, (searchType & 4) != 0 ? CommonComponents.GUI_YES : CommonComponents.GUI_NO);
            case 3 -> Component.translatable("tooltip.toms_storage.opt.search_smart", recipeBook, (searchType & 8) == 0 ? CommonComponents.GUI_YES : CommonComponents.GUI_NO);
            default -> Component.empty();
        };
    }

    private static int searchPopupHeight() {
        return SEARCH_POPUP_PADDING * 2 + SEARCH_POPUP_ROWS * SEARCH_POPUP_ROW_HEIGHT;
    }

    private static int searchPopupWidth(DesktopWindowContext<?, ?> context) {
        int labelWidth = 0;
        for (int i = 0; i < SEARCH_POPUP_ROWS; i++) {
            labelWidth = Math.max(labelWidth, context.minecraft().font.width(searchPopupLabel(i)));
        }
        return SEARCH_POPUP_PADDING * 2 + labelWidth + SEARCH_POPUP_LABEL_BUTTON_GAP + SEARCH_POPUP_STATE_BUTTON_WIDTH;
    }

    private static String searchPopupLabel(int index) {
        return switch (index) {
            case 0 -> "Auto Select Search";
            case 1 -> "Keep Search";
            case 2 -> "Sync Recipe Book";
            case 3 -> "Smart Recipe Book";
            default -> "";
        };
    }

    private static void renderSearchPopupStateButton(DesktopRenderContext<?, ?> context, TerminalState state, int index, int x, int y, boolean hovered) {
        boolean enabled = searchPopupStateEnabled(state, index);
        int border = hovered ? 0xFF202020 : 0xFF4A4A4A;
        int fill = enabled ? hovered ? 0xFF78C66E : 0xFF63B75B : hovered ? 0xFFB6B6B6 : 0xFF9D9D9D;
        int textColor = enabled ? 0xFF103D12 : 0xFF303030;
        String text = enabled ? "On" : "Off";
        context.fill(x, y, x + SEARCH_POPUP_STATE_BUTTON_WIDTH, y + SEARCH_POPUP_STATE_BUTTON_HEIGHT, border);
        context.fill(x + 1, y + 1, x + SEARCH_POPUP_STATE_BUTTON_WIDTH - 1, y + SEARCH_POPUP_STATE_BUTTON_HEIGHT - 1, fill);
        int textX = x + Math.max(2, (SEARCH_POPUP_STATE_BUTTON_WIDTH - context.minecraft().font.width(text)) / 2);
        context.text(text, textX, y + 3, textColor, false);
    }

    private static boolean searchPopupStateEnabled(TerminalState state, int index) {
        int searchType = normalizedSearchType(state.searchType);
        return switch (index) {
            case 0 -> (searchType & 1) != 0;
            case 1 -> (searchType & 2) != 0;
            case 2 -> (searchType & 4) != 0;
            case 3 -> (searchType & 8) == 0;
            default -> false;
        };
    }

    private static void renderToggleColumn(DesktopRenderContext<?, ?> context, int x, int y, Identifier[] icons) {
        for (int i = 0; i < icons.length; i++) {
            renderCycleButton(context, x, y + i * 18, icons[i], false);
        }
    }

    private static void renderCycleButton(DesktopRenderContext<?, ?> context, int x, int y, Identifier icon, boolean selected) {
        boolean hovered = contains(context.mouseX(), context.mouseY(), x, y, BUTTON, BUTTON);
        context.sprite(selected ? SMALL_BUTTON_SELECTED : hovered ? SMALL_BUTTON_HOVERED : SMALL_BUTTON_SPRITE, x, y, BUTTON, BUTTON);
        context.sprite(icon, x + 1, y + 1, 14, 14);
    }

    private static void renderSearchModeButton(DesktopRenderContext<?, ?> context, int x, int y, int searchType) {
        searchType = normalizedSearchType(searchType);
        renderCycleButton(context, x, y, TomsStorageCompat.id("icons/search_mode"), false);
        if ((searchType & 1) != 0) {
            context.sprite(TomsStorageCompat.id("icons/search_mode_auto"), x + 1, y + 1, 14, 14);
        }
        if ((searchType & 2) != 0) {
            context.sprite(TomsStorageCompat.id("icons/search_mode_keep"), x + 1, y + 1, 14, 14);
        }
        if ((searchType & 4) != 0) {
            context.sprite(TomsStorageCompat.id("icons/search_mode_sync"), x + 1, y + 1, 14, 14);
        }
    }

    private static void renderIconButton(DesktopRenderContext<?, ?> context, int x, int y, Identifier normal, Identifier hoveredSprite, boolean selected) {
        boolean hovered = contains(context.mouseX(), context.mouseY(), x, y, BUTTON, BUTTON);
        context.sprite(hovered ? hoveredSprite : normal, x, y, BUTTON, BUTTON);
    }

    private static void renderTagList(DesktopRenderContext<?, ?> context, List<String> tags, String selected, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFF5F5544);
        int rows = height / 12;
        for (int i = 0; i < rows && i < tags.size(); i++) {
            String tag = tags.get(i);
            int rowY = y + i * 12;
            if (tag.equals(selected)) {
                context.fill(x, rowY, x + width, rowY + 12, 0xFF8F8265);
            }
            context.text(shorten(tag, 12), x + 2, rowY + 2, 0xFFFFFF, false);
        }
    }

    private static String pickTag(List<String> tags, MouseButtonEvent event, int x, int y, int width, int height) {
        if (!contains(event.x(), event.y(), x, y, width, height)) {
            return null;
        }
        int row = (int) ((event.y() - y) / 12);
        return row >= 0 && row < tags.size() ? tags.get(row) : null;
    }

    private static void renderScrollbar(DesktopRenderContext<?, ?> context, int x, int y, int height, int scroll, int maxScroll) {
        DesktopWidgets.renderScrollbar(context, x, y, height, scroll, maxScroll);
    }

    private static void renderScrollbarBackground(DesktopRenderContext<?, ?> context, int x, int y, int height) {
        context.texture(
            SCROLLBAR_BACKGROUND_TEXTURE,
            x,
            y,
            0,
            0,
            STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            STORAGE_TERMINAL_SCROLLBAR_TEXTURE_WIDTH,
            STORAGE_TERMINAL_SCROLLBAR_TEXTURE_HEIGHT
        );
        if (height > 2) {
            context.texture(
                SCROLLBAR_BACKGROUND_TEXTURE,
                x,
                y + 1,
                0,
                1,
                STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
                height - 2,
                STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
                1,
                STORAGE_TERMINAL_SCROLLBAR_TEXTURE_WIDTH,
                STORAGE_TERMINAL_SCROLLBAR_TEXTURE_HEIGHT
            );
        }
        context.texture(
            SCROLLBAR_BACKGROUND_TEXTURE,
            x,
            y + height - 1,
            0,
            2,
            STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            STORAGE_TERMINAL_SCROLLBAR_BACKGROUND_WIDTH,
            1,
            STORAGE_TERMINAL_SCROLLBAR_TEXTURE_WIDTH,
            STORAGE_TERMINAL_SCROLLBAR_TEXTURE_HEIGHT
        );
    }

    private static void drawCount(DesktopRenderContext<?, ?> context, long count, int x, int y) {
        if (count < 0) {
            return;
        }
        float scaleFactor = 0.6F;
        String text = TomsStorageReflect.formatNumber(count);
        float inverseScaleFactor = 1.0F / scaleFactor;
        int textX = (int) (((float) x + 16.0F - context.minecraft().font.width(text) * scaleFactor) * inverseScaleFactor);
        int textY = (int) (((float) y + 16.0F - 7.0F * scaleFactor) * inverseScaleFactor);
        context.scaledText(text, textX, textY, count == 0 ? 0xFFFFFF00 : 0xFFFFFFFF, true, scaleFactor);
    }

    private static int buttonIndex(MouseButtonEvent event, int x, int y, int count) {
        for (int i = 0; i < count; i++) {
            if (buttonHit(event, x + i * 18, y, BUTTON, BUTTON) || buttonHit(event, x, y + i * 18, BUTTON, BUTTON)) {
                return i;
            }
        }
        return -1;
    }

    private static int buttonIndex(double mouseX, double mouseY, int x, int y, int count) {
        for (int i = 0; i < count; i++) {
            if (contains(mouseX, mouseY, x + i * 18, y, BUTTON, BUTTON) || contains(mouseX, mouseY, x, y + i * 18, BUTTON, BUTTON)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean buttonHit(MouseButtonEvent event, int x, int y, int width, int height) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && contains(event.x(), event.y(), x, y, width, height);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static boolean keyDown(DesktopWindowContext<?, ?> context, int key) {
        return org.lwjgl.glfw.GLFW.glfwGetKey(context.minecraft().getWindow().handle(), key) != GLFW.GLFW_RELEASE;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int rowsForEntries(int entryCount, int columns) {
        return Math.max(1, (entryCount + Math.max(1, columns) - 1) / Math.max(1, columns));
    }

    private static String shorten(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(1, max - 3)) + "...";
    }

    private static String shortenToWidth(DesktopWindowContext<?, ?> context, String text, int maxWidth) {
        if (context.minecraft().font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = context.minecraft().font.width(suffix);
        String result = text;
        while (!result.isEmpty() && context.minecraft().font.width(result) + suffixWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? suffix : result + suffix;
    }

    private static String sortIcon(int sortType) {
        return switch (sortType) {
            case 1 -> "icons/sort_name";
            case 2 -> "icons/sort_by_mod";
            default -> "icons/sort_amount";
        };
    }

    private static String controlIcon(int controlMode) {
        return switch (controlMode) {
            case 1 -> "icons/control_ae";
            case 2 -> "icons/control_rs";
            default -> "icons/control_def";
        };
    }

    private static Identifier sideIcon(AbstractContainerMenu menu) {
        return TomsStorageCompat.id("icons/side_" + TomsStorageReflect.enumNameField(menu, "side", "DOWN").toLowerCase(Locale.ROOT));
    }

    private static Identifier priorityIcon(AbstractContainerMenu menu) {
        return TomsStorageCompat.id("icons/priority_" + TomsStorageReflect.enumNameField(menu, "priority", "NORMAL").toLowerCase(Locale.ROOT));
    }

    private static boolean skip(AbstractContainerMenu menu) {
        return TomsStorageReflect.booleanField(menu, "skip", false);
    }

    private static boolean keepLast(AbstractContainerMenu menu) {
        return TomsStorageReflect.booleanField(menu, "keepLast", false);
    }
}
