package com.salts_inventory_update.controlify.input;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.navigation.ScreenDirection;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;

/** Supplies Salt's real hovered slot and click routing to Controlify's native container processor. */
public interface WorkspaceContainerScreen {
    /** May return null when the cursor is over ordinary UI or empty space. */
    Slot controllerHoveredSlot();

    /** Slot may be null for Controlify's standard outside-container drop action. */
    void controllerClickSlot(Slot slot, int slotId, int button, ClickType type);

    /** Logical D-pad target in GUI coordinates; null leaves ordinary native navigation available. */
    default SnapPoint controllerNavigate(ScreenDirection direction, double x, double y) { return null; }

    /** One list row, in GUI coordinates; direction is -1 up or +1 down. */
    default boolean controllerScroll(double x, double y, int direction, boolean edgeOnly) { return false; }

    /** One/full creative catalog stack into the hotbar, using the current controller cursor. */
    default boolean controllerCreativeHotbar(boolean stack) { return false; }

    /** Owns Quick Move before native cursor-drop handling for eligible creative hotbar slots. */
    default boolean controllerCreativeDeleteHotbar() { return false; }

    /** Only the hovered creative window with its Search tab selected exposes this action. */
    default boolean controllerCreativeSearchAvailable() { return false; }

    /** Field-only requests implement Select/A without changing ordinary catalog clicks. */
    default boolean controllerCreativeSearch(boolean fieldOnly) { return false; }

    default boolean contextualRadialActive() { return false; }
    default boolean controllerCanOpenContextual() { return false; }

    /** Live, read-only facts for the native Controlify action guide. */
    com.salts_inventory_update.controlify.WorkspaceActionGuide.State controllerGuideState();

    default boolean controllerContextualInput(boolean held, float rightX, float rightY, float leftX, float leftY,
            boolean categoryLeft, boolean categoryRight, boolean toggleCraftable, boolean confirm,
            boolean rotateLeftHeld, boolean rotateRightHeld, boolean craftStack) { return false; }
}
