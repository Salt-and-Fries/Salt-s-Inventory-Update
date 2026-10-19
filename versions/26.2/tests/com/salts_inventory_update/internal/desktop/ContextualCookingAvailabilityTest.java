package com.salts_inventory_update.internal.desktop;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ContextualCookingAvailabilityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        for (var item : List.of(Items.OAK_LOG, Items.STRIPPED_OAK_LOG, Items.COAL)) item.builtInRegistryHolder().bindComponents(
            net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64).build());
    }
    @Test void oneLogCannotSupplyBothCharcoalInputAndFuel() {
        ItemStack log = new ItemStack(Items.OAK_LOG);
        var result = ContextualCookingAvailability.allocate(List.of(log), stack -> stack.is(Items.OAK_LOG), stack -> stack.is(Items.OAK_LOG), true);
        assertFalse(result.ready());
        assertEquals(0, result.fuelAvailable(), "Fuel Have must exclude the one reserved ingredient");
        assertEquals(1, log.getCount(), "Availability checks cannot consume the input");
    }
    @Test void twoLogsSupplyDistinctInputAndFuelUnitsWithoutChangingTheSource() {
        ItemStack logs = new ItemStack(Items.OAK_LOG, 2);
        var result = ContextualCookingAvailability.allocate(List.of(logs), stack -> stack.is(Items.OAK_LOG), stack -> stack.is(Items.OAK_LOG), true);
        assertTrue(result.ready());
        assertEquals(0, result.inputIndex()); assertEquals(0, result.fuelIndex());
        assertEquals(1, result.fuelAvailable());
        assertEquals(2, logs.getCount());
    }
    @Test void separateFuelIsAvailableAfterReservingOneLog() {
        ItemStack log = new ItemStack(Items.OAK_LOG), coal = new ItemStack(Items.COAL);
        var result = ContextualCookingAvailability.allocate(List.of(log, coal), stack -> stack.is(Items.OAK_LOG),
            stack -> stack.is(Items.OAK_LOG) || stack.is(Items.COAL), true);
        assertTrue(result.ready());
        assertEquals(0, result.inputIndex()); assertEquals(1, result.fuelIndex());
        assertEquals(1, result.fuelAvailable());
        assertEquals(1, log.getCount()); assertEquals(1, coal.getCount());
    }
    @Test void allocationCanChooseAnotherInputVariantToPreserveTheOnlyFuel() {
        ItemStack fuelInput = new ItemStack(Items.OAK_LOG), otherInput = new ItemStack(Items.STRIPPED_OAK_LOG);
        var result = ContextualCookingAvailability.allocate(List.of(fuelInput, otherInput),
            stack -> stack.is(Items.OAK_LOG) || stack.is(Items.STRIPPED_OAK_LOG), stack -> stack.is(Items.OAK_LOG), true);
        assertTrue(result.ready());
        assertEquals(1, result.inputIndex()); assertEquals(0, result.fuelIndex());
        assertEquals(1, result.fuelAvailable());
    }
    @Test void AlreadyBurningFurnaceNeedsOnlyOneAvailableIngredient() {
        var result = ContextualCookingAvailability.allocate(List.of(new ItemStack(Items.OAK_LOG)),
            stack -> stack.is(Items.OAK_LOG), stack -> stack.is(Items.OAK_LOG), false);
        assertTrue(result.ready());
        assertEquals(-1, result.fuelIndex());
        assertEquals(0, result.fuelAvailable());
    }
}
