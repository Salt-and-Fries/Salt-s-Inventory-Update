package com.salts_inventory_update.internal.desktop;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Reserves one ingredient before assigning fuel, so one physical item cannot satisfy both roles. */
public final class ContextualCookingAvailability {
    private ContextualCookingAvailability() { }
    public record Allocation(boolean ready, int inputIndex, int fuelIndex, int fuelAvailable) { }
    public static Allocation allocate(List<ItemStack> sources, Predicate<ItemStack> input, Predicate<ItemStack> fuel, boolean needsFuel) {
        long fuelTotal = 0;
        for (ItemStack stack : sources) if (!stack.isEmpty() && fuel.test(stack)) fuelTotal += stack.getCount();
        Allocation unavailable = new Allocation(false, -1, -1, clamp(fuelTotal));
        for (int i = 0; i < sources.size(); i++) {
            ItemStack candidate = sources.get(i);
            if (candidate.isEmpty() || !input.test(candidate)) continue;
            int remainingFuel = clamp(fuelTotal - (fuel.test(candidate) ? 1 : 0));
            if (unavailable.inputIndex() < 0) unavailable = new Allocation(false, i, -1, remainingFuel);
            if (!needsFuel) return new Allocation(true, i, -1, remainingFuel);
            for (int j = 0; j < sources.size(); j++) {
                ItemStack fuelCandidate = sources.get(j);
                if (!fuelCandidate.isEmpty() && fuel.test(fuelCandidate) && fuelCandidate.getCount() - (i == j ? 1 : 0) > 0)
                    return new Allocation(true, i, j, remainingFuel);
            }
        }
        return unavailable;
    }
    private static int clamp(long value) { return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value)); }
}
