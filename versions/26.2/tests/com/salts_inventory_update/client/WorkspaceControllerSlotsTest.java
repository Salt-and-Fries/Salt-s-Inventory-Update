package com.salts_inventory_update.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WorkspaceControllerSlotsTest {
    @Test void brewingBottleCellsAreConsecutiveAndIngredientIsOverTheMiddle() {
        for (int i = 0; i < 3; i++) assertEquals(new WorkspaceControllerSlots.Cell(1, i), WorkspaceControllerSlots.authored("BrewingStandMenu", i));
        assertEquals(new WorkspaceControllerSlots.Cell(0, 1), WorkspaceControllerSlots.authored("BrewingStandMenu", 3));
        assertEquals(new WorkspaceControllerSlots.Cell(0, 0), WorkspaceControllerSlots.authored("BrewingStandMenu", 4));
    }

    @Test void craftingAndCrafterGridIndicesProduceCompleteLogicalRows() {
        assertEquals(new WorkspaceControllerSlots.Cell(1, 3), WorkspaceControllerSlots.authored("CraftingMenu", 0));
        for (int i = 0; i < 9; i++) {
            assertEquals(new WorkspaceControllerSlots.Cell(i / 3, i % 3), WorkspaceControllerSlots.authored("CraftingMenu", i + 1));
            assertEquals(new WorkspaceControllerSlots.Cell(i / 3, i % 3), WorkspaceControllerSlots.authored("CrafterMenu", i));
        }
        assertNull(WorkspaceControllerSlots.authored("CraftingMenu", 10));
    }

    @Test void verticalInputsAndOffsetOutputsAreAuthoredAcrossTheRemainingStations() {
        for (String menu : new String[] {"FurnaceMenu", "BlastFurnaceMenu", "SmokerMenu", "GrindstoneMenu", "CartographyTableMenu"}) {
            assertEquals(new WorkspaceControllerSlots.Cell(0, 0), WorkspaceControllerSlots.authored(menu, 0));
            assertEquals(new WorkspaceControllerSlots.Cell(1, 0), WorkspaceControllerSlots.authored(menu, 1));
            assertEquals(new WorkspaceControllerSlots.Cell(0, 1), WorkspaceControllerSlots.authored(menu, 2));
        }
        for (String menu : new String[] {"AnvilMenu", "SmithingMenu", "MerchantMenu", "EnchantmentMenu"}) {
            assertEquals(new WorkspaceControllerSlots.Cell(0, 1), WorkspaceControllerSlots.authored(menu, 1));
        }
        assertNull(WorkspaceControllerSlots.authored("ModdedContainerMenu", 0));
    }
}
