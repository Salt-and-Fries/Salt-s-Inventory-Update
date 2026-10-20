package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.UUID;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Client-thread access to existing windows, without transferring their model/session ownership.
 * External presentations transform rendering and pointer coordinates themselves.
 */
public interface DesktopWorkspace {
    List<DesktopWorkspaceWindow> windows();
    DesktopWindowHandle ensureWindow(DesktopWorkspaceWindow.Kind kind);
    /** Restores an existing window's full surface without changing its lifetime or requesting screen focus. */
    void restoreWindow(UUID id);
    void initialize(int width, int height);
    void tick();
    void renderWindow(UUID id, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
    /** Draws Salt's ordinary resizable window frame and title without creating a window or session. */
    void renderWindowFrame(GuiGraphicsExtractor graphics, DesktopWindowBounds bounds, Component title);
    /** Read-only item representation of a window's source for inactive external previews. */
    default ItemStack previewIcon(UUID id) { return ItemStack.EMPTY; }
    void renderCarried(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
    /** Tests a displayed window's native interactive extent, including external regions and popups.
     * Coordinates are before presentation transforms. A modal owner also receives outside presses
     * for native popup dismissal and blocks other displayed windows.
     */
    boolean containsPointer(UUID id, double x, double y);
    /** Dispatches native popup outside-press dismissal for this displayed owner only.
     * Coordinates are before presentation transforms; no body click or pointer capture is started.
     */
    boolean dismissOutsidePointer(UUID id, double x, double y);
    boolean pointer(UUID id, PointerAction action, double x, double y, int button, double deltaX, double deltaY);
    boolean keyPressed(int key, int scanCode, int modifiers);
    boolean keyReleased(int key, int scanCode, int modifiers);
    boolean charTyped(char character, int modifiers);
    /** Preserves the native Unicode code point on modern text-input events. */
    boolean charTyped(int codepoint, int modifiers);
    /** True only while this window owns an editable text field, rather than a modal dialog or button. */
    default boolean textInputActive(UUID id) { return false; }
    /** Current editable field rectangles, before presentation transforms; empty when an owner supplies focus only. */
    default List<DesktopWindowBounds> textInputBounds(UUID id) { return List.of(); }
    net.minecraft.world.inventory.Slot hoveredSlot(UUID id, double x, double y);
    /** Visible native slot geometry for cursor snapping; coordinates are before presentation transforms. */
    List<DesktopSlotHit> visibleSlots(UUID id);
    /** Enabled native controls, including an attached recipe guide and its expanded choices.
     * Coordinates are before presentation transforms; slots remain available through visibleSlots.
     */
    List<DesktopWindowBounds> visibleControls(UUID id);
    /** Logical native navigation cells, including catalogs, tabs, slots and menu choices. */
    List<DesktopControllerTarget> controllerTargets(UUID id);
    /** Scrolls one row in the hovered list. Direction is -1 for up and +1 for down.
     * Edge-only requests scroll only at its first/last visible row and only if another row exists.
     */
    boolean scrollController(UUID id, double x, double y, int direction, boolean edgeOnly);
    /** Read-only test for a hovered native list with more rows in at least one direction.
     * Coordinates are before presentation transforms; custom handlers are not invoked as probes.
     */
    default boolean controllerScrollAvailable(UUID id, double x, double y) { return false; }
    /** Clears an occupied creative cursor, otherwise adds a catalog item to a compatible or empty hotbar slot.
     * Returns true when the pointer belongs to the catalog, including a full-hotbar rejection.
     */
    boolean creativeHotbarAt(UUID id, double x, double y, boolean stack);
    /** Read-only creative catalog hit test, without adding an item or changing the selected tab. */
    default boolean creativeCatalogAt(UUID id, double x, double y) { return false; }
    /** Read-only test for quick-move deleting an unlocked hotbar stack in a regular creative tab.
     * Other windows and the creative Inventory tab retain normal quick-move behavior.
     */
    default boolean creativeHotbarDeleteAt(UUID id, double x, double y) { return false; }
    /** Native visual extent of the window and attached panels, before presentation transforms. */
    DesktopWindowBounds presentationBounds(UUID id);
    void clickAt(UUID id, double x, double y, int button, net.minecraft.world.inventory.ClickType type);
    void dropCarried(int button);
    /** A defensive copy of the shared cursor stack, or empty when no item is carried. */
    default ItemStack carriedStack() { return ItemStack.EMPTY; }
    void cancelInput();
    /** Route quick-move to the other member of this pair; an inventory-less destination rejects it. */
    void transferPair(UUID first, UUID second);
    /** Uses the normal sort-window action against all eligible open storage windows.
     * Force has the same meaning as holding Shift while pressing that window's sort button.
     */
    boolean sortWindow(UUID source, boolean force);
    /** Read-only eligibility for the normal sort action, including its empty-cursor requirement. */
    default boolean canSortWindow(UUID source) { return false; }
    /** Native unlocked crafting recipes or available creative tabs for this displayed window. */
    DesktopContextualMenu contextualMenu(UUID id);
    /** Crafts one native recipe operation and routes its complete result to the opposite window. */
    boolean craftToWindow(UUID source, UUID destination, net.minecraft.resources.ResourceLocation recipe);
    /** Crafts complete native batches up to one output stack, stopping at supply/destination limits. */
    default boolean craftStackToWindow(UUID source, UUID destination, net.minecraft.resources.ResourceLocation recipe) { return false; }
    /** Places one cooking input and fuel when needed; native furnace timing and output remain untouched. */
    boolean prepareCookingRecipe(UUID source, net.minecraft.resources.ResourceLocation recipe);
    /** Starts a server-owned reversible recipe preview, safely clearing native inputs/output. */
    boolean beginRecipeHover(UUID source);
    /** Stages available ingredients in the native grid while retaining their original source. */
    boolean updateRecipeHover(UUID source, net.minecraft.resources.ResourceLocation recipe);
    /** Restores unconsumed ingredients; native furnace fuel is never a preview ingredient. */
    void endRecipeHover(UUID source);
    /** Defensive copies of missing ingredient previews in native menu slot coordinates. */
    List<DesktopRecipeGhost> recipeHoverGhosts(UUID source);
    /** True when a server rejected or safely interrupted this active wheel's preview. */
    default boolean recipeHoverRejected(UUID source) { return false; }
    /** Replaces the normal guide button/panel only while an external host opts in. */
    void setContextualRecipePresentation(boolean enabled);
    /** Lets an external presentation draw Character effects separately; native desktop rendering stays unchanged. */
    default void setCharacterEffectsPresentation(boolean enabled) {}
    /** Null for unsupported windows. Bounds are native coordinates, before host transforms. */
    DesktopContextualBookHint contextualBookHint(UUID id);
    boolean selectContextualTab(UUID id, net.minecraft.resources.ResourceLocation tab);
    void protectWindow(UUID id);
    void forceInventoryHud(boolean enabled);
    long closeAllHoldMillis();
    void closeClosableWindows();
    boolean isValid();
    boolean isNativeScreenActive();
    enum PointerAction { PRESS, RELEASE, MOVE, DRAG, SCROLL }
}
