package com.salts_inventory_update.controlify.input;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** Supplies Salt's real hovered slot and click routing to Controlify's native container processor. */
public interface WorkspaceContainerScreen {
    /** May return null when the cursor is over ordinary UI or empty space. */
    Slot controllerHoveredSlot();

    /** Slot may be null for Controlify's standard outside-container drop action. */
    void controllerClickSlot(Slot slot, int slotId, int button, ContainerInput type);
}
