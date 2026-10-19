package com.salts_inventory_update.internal.desktop;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PartialRecipeAllocationTest {
    @Test void reassignsFlexibleIngredientForSpecificIngredient() {
        assertArrayEquals(new int[]{1, 0}, PartialRecipeAllocation.allocate(new boolean[][]{{true, true}, {true, false}}, new int[]{1, 1}));
    }
    @Test void missingIngredientsDoNotPreventAvailableIngredients() {
        assertArrayEquals(new int[]{0, -1, 1}, PartialRecipeAllocation.allocate(new boolean[][]{{true, false}, {false, false}, {false, true}}, new int[]{1, 2}));
    }
    @Test void oneStackCanFillSeveralSlotsWithoutOverAllocation() {
        int[] assigned = PartialRecipeAllocation.allocate(new boolean[][]{{true}, {true}, {true}}, new int[]{2});
        assertEquals(2, java.util.Arrays.stream(assigned).filter(index -> index == 0).count());
        assertEquals(1, java.util.Arrays.stream(assigned).filter(index -> index == -1).count());
    }
    @Test void emptySupplyAndEmptyRecipeAreSupported() {
        assertArrayEquals(new int[]{-1, -1}, PartialRecipeAllocation.allocate(new boolean[][]{{}, {}}, new int[]{}));
        assertArrayEquals(new int[]{}, PartialRecipeAllocation.allocate(new boolean[][]{}, new int[]{64}));
    }
    @Test void limitsAndInvalidCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PartialRecipeAllocation.allocate(new boolean[10][0], new int[0]));
        assertThrows(IllegalArgumentException.class, () -> PartialRecipeAllocation.allocate(new boolean[][]{{true}}, new int[]{-1}));
    }
}
