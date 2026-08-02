package com.salts_inventory_update.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BoundedTransferPlannerTest {
    private final BoundedTransferPlanner<String> planner = new BoundedTransferPlanner<>(Comparator.naturalOrder());

    @Test
    void keepsOneVariantPerTargetAcrossMaximumCrafts() {
        BoundedTransferPlanner.Plan<String> plan = planner.planMaximum(
            Map.of("oak", 2, "birch", 2),
            List.of(new BoundedTransferPlanner.Requirement<>(4, 1, List.of("oak", "birch")))
        ).orElseThrow();

        assertEquals(2, plan.crafts());
        assertEquals(Map.of("birch", 2), plan.allocations().get(0).units());
        assertEquals(Map.of("oak", 2), plan.remainingSupply());
        assertTrue(plan.attempts() <= DesktopProtocol.MAX_TRANSFER_ATTEMPTS);
    }

    @Test
    void maximumSearchSelectsAVariantThatFitsTheTargetCapacity() {
        BoundedTransferPlanner.Plan<String> plan = planner.planMaximum(
            Map.of("large-supply-small-stack", 100, "smaller-supply-full-stack", 60),
            List.of(new BoundedTransferPlanner.Requirement<>(
                4,
                1,
                0,
                List.of("large-supply-small-stack", "smaller-supply-full-stack"),
                Map.of("large-supply-small-stack", 1, "smaller-supply-full-stack", 64)
            )),
            64
        ).orElseThrow();

        assertEquals(60, plan.crafts());
        assertEquals(Map.of("smaller-supply-full-stack", 60), plan.allocations().get(0).units());
        assertEquals(Map.of("large-supply-small-stack", 100), plan.remainingSupply());
        assertTrue(plan.attempts() <= DesktopProtocol.MAX_TRANSFER_ATTEMPTS);
    }

    @Test
    void maximumSearchDoesNotLockInAScarceFirstCraftVariant() {
        BoundedTransferPlanner.Plan<String> plan = planner.planMaximum(
            Map.of("a", 100, "b", 1),
            List.of(
                new BoundedTransferPlanner.Requirement<>(20, 1, List.of("a", "b")),
                new BoundedTransferPlanner.Requirement<>(10, 1, List.of("a"))
            )
        ).orElseThrow();

        assertEquals(50, plan.crafts());
        assertEquals(Map.of("a", 50), plan.allocations().get(0).units());
        assertEquals(Map.of("a", 50), plan.allocations().get(1).units());
        assertEquals(Map.of("b", 1), plan.remainingSupply());
    }

    @Test
    void multiUnitRequirementNeverMixesVariantsInOneSlot() {
        BoundedTransferPlanner.Plan<String> plan = planner.planExact(
            Map.of("a", 1, "b", 2),
            List.of(new BoundedTransferPlanner.Requirement<>(7, 2, List.of("a", "b"))),
            1
        ).orElseThrow();

        assertEquals(Map.of("b", 2), plan.allocations().get(0).units());
    }

    @Test
    void creditsExistingTargetUnitsAndKeepsTheirVariantLocked() {
        BoundedTransferPlanner.Plan<String> plan = planner.planExact(
            Map.of("existing", 3, "other", 20),
            List.of(new BoundedTransferPlanner.Requirement<>(7, 2, 3, List.of("existing"))),
            3
        ).orElseThrow();

        assertEquals(Map.of("existing", 3), plan.allocations().get(0).units());
        assertTrue(plan.remainingSupply().isEmpty());
    }

    @Test
    void rejectsCrossVariantCreditButDeduplicatesTheLockedVariant() {
        assertThrows(IllegalArgumentException.class, () ->
            new BoundedTransferPlanner.Requirement<>(7, 2, 1, List.of("existing", "other"))
        );

        BoundedTransferPlanner.Plan<String> plan = planner.planExact(
            Map.of("existing", 1),
            List.of(new BoundedTransferPlanner.Requirement<>(
                7,
                1,
                1,
                List.of("existing", "existing")
            )),
            2
        ).orElseThrow();

        assertEquals(Map.of("existing", 1), plan.allocations().get(0).units());
        assertTrue(plan.remainingSupply().isEmpty());
    }

    @Test
    void existingUnitsCanSatisfyARequirementWithoutSourceItems() {
        BoundedTransferPlanner.Plan<String> plan = planner.planMaximum(
            Map.of(),
            List.of(new BoundedTransferPlanner.Requirement<>(7, 2, 6, List.of("existing")))
        ).orElseThrow();

        assertEquals(3, plan.crafts());
        assertEquals(Map.of("existing", 0), plan.allocations().get(0).units());
        assertTrue(plan.remainingSupply().isEmpty());
    }

    @Test
    void constrainedRequirementWinsSharedSupply() {
        BoundedTransferPlanner.Plan<String> plan = planner.planExact(
            Map.of("a", 1, "b", 1),
            List.of(
                new BoundedTransferPlanner.Requirement<>(20, 1, List.of("a", "b")),
                new BoundedTransferPlanner.Requirement<>(10, 1, List.of("a"))
            ),
            1
        ).orElseThrow();

        assertEquals(Map.of("b", 1), plan.allocations().get(0).units());
        assertEquals(Map.of("a", 1), plan.allocations().get(1).units());
    }

    @Test
    void rejectsDuplicateTargetsAndOversizedAlternatives() {
        assertThrows(IllegalArgumentException.class, () ->
            new BoundedTransferPlanner.Requirement<>(1, 1, -1, List.of("a"))
        );
        assertThrows(IllegalArgumentException.class, () -> planner.planExact(
            Map.of("a", 2),
            List.of(
                new BoundedTransferPlanner.Requirement<>(1, 1, List.of("a")),
                new BoundedTransferPlanner.Requirement<>(1, 1, List.of("a"))
            ),
            1
        ));

        List<String> alternatives = java.util.stream.IntStream.range(0, 33).mapToObj(Integer::toString).toList();
        assertThrows(IllegalArgumentException.class, () -> planner.planExact(
            Map.of("0", 1),
            List.of(new BoundedTransferPlanner.Requirement<>(1, 1, alternatives)),
            1
        ));
    }

    @Test
    void ignoresSupplyVariantsThatTheRecipeCannotUse() {
        BoundedTransferPlanner.Plan<String> plan = planner.planExact(
            Map.of("ingredient", 1, "unrelated-a", Integer.MAX_VALUE, "unrelated-b", Integer.MAX_VALUE),
            List.of(new BoundedTransferPlanner.Requirement<>(1, 1, List.of("ingredient"))),
            1
        ).orElseThrow();

        assertTrue(plan.remainingSupply().isEmpty());
        assertEquals(Map.of("ingredient", 1), plan.allocations().get(0).units());
    }

    @Test
    void impossibleDemandOverflowFailsClosed() {
        assertTrue(planner.planExact(
            Map.of("ingredient", Integer.MAX_VALUE),
            List.of(new BoundedTransferPlanner.Requirement<>(1, Integer.MAX_VALUE, List.of("ingredient"))),
            2
        ).isEmpty());
        assertTrue(planner.planMaximum(
            Map.of("ingredient", Integer.MAX_VALUE),
            List.of(
                new BoundedTransferPlanner.Requirement<>(1, Integer.MAX_VALUE, List.of("ingredient")),
                new BoundedTransferPlanner.Requirement<>(2, Integer.MAX_VALUE, List.of("ingredient"))
            )
        ).isEmpty());
    }

    @Test
    void acceptsStructurallyMaximalStraightforwardInputWithinSearchBudget() {
        int alternativesPerRequirement = DesktopProtocol.MAX_TRANSFER_ALTERNATIVES
            / DesktopProtocol.MAX_TRANSFER_REQUIREMENTS;
        List<String> alternatives = java.util.stream.IntStream
            .range(0, alternativesPerRequirement)
            .mapToObj(index -> "variant-" + index)
            .toList();
        Map<String, Integer> supply = alternatives.stream().collect(java.util.stream.Collectors.toMap(
            key -> key,
            ignored -> DesktopProtocol.MAX_TRANSFER_REQUIREMENTS
        ));
        List<BoundedTransferPlanner.Requirement<String>> requirements = java.util.stream.IntStream
            .range(0, DesktopProtocol.MAX_TRANSFER_REQUIREMENTS)
            .mapToObj(index -> new BoundedTransferPlanner.Requirement<>(index, 1, alternatives))
            .toList();

        BoundedTransferPlanner.Plan<String> plan = planner.planExact(supply, requirements, 1).orElseThrow();
        assertEquals(DesktopProtocol.MAX_TRANSFER_REQUIREMENTS, plan.allocations().size());
        assertTrue(plan.attempts() <= DesktopProtocol.MAX_TRANSFER_ATTEMPTS);
    }

    @Test
    void maximalUnsatisfiableBacktrackingIsStoppedByTheSharedBudget() {
        List<String> sharedAlternatives = List.of("a", "b", "c", "d");
        LinkedHashMap<String, Integer> supply = new LinkedHashMap<>();
        supply.put("a", 32);
        supply.put("b", 32);
        supply.put("c", 31);
        supply.put("d", 31);
        supply.put("isolated", 2);

        List<BoundedTransferPlanner.Requirement<String>> requirements = new java.util.ArrayList<>();
        requirements.add(new BoundedTransferPlanner.Requirement<>(0, 1, List.of("isolated")));
        java.util.stream.IntStream.range(1, DesktopProtocol.MAX_TRANSFER_REQUIREMENTS)
            .mapToObj(index -> new BoundedTransferPlanner.Requirement<>(index, 1, sharedAlternatives))
            .forEach(requirements::add);

        assertEquals(
            DesktopProtocol.MAX_TRANSFER_ALTERNATIVES - 3,
            requirements.stream().mapToInt(value -> value.alternatives().size()).sum()
        );
        assertTimeout(Duration.ofSeconds(2), () ->
            assertTrue(planner.planExact(supply, requirements, 1).isEmpty())
        );
    }
}
