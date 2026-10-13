package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;

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
    void renderWindow(UUID id, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    void renderCarried(GuiGraphics graphics, int mouseX, int mouseY);
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
    net.minecraft.world.inventory.Slot hoveredSlot(UUID id, double x, double y);
    /** Visible native slot geometry for cursor snapping; coordinates are before presentation transforms. */
    List<DesktopSlotHit> visibleSlots(UUID id);
    void clickAt(UUID id, double x, double y, int button, net.minecraft.world.inventory.ClickType type);
    void dropCarried(int button);
    void cancelInput();
    /** Route quick-move to the other member of this pair; an inventory-less destination rejects it. */
    void transferPair(UUID first, UUID second);
    void protectWindow(UUID id);
    void forceInventoryHud(boolean enabled);
    long closeAllHoldMillis();
    void closeClosableWindows();
    boolean isValid();
    boolean isNativeScreenActive();
    enum PointerAction { PRESS, RELEASE, MOVE, DRAG, SCROLL }
}
