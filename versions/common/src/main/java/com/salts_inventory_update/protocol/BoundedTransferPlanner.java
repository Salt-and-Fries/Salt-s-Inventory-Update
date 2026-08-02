package com.salts_inventory_update.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, work-bounded transfer planner. Each target is assigned one
 * variant because a Minecraft slot cannot contain mixed item variants.
 */
public final class BoundedTransferPlanner<K> {
    public record Requirement<K>(
        int targetId,
        int unitsPerCraft,
        int initialUnits,
        List<K> alternatives,
        Map<K, Integer> maximumUnitsByAlternative
    ) {
        public Requirement(int targetId, int unitsPerCraft, List<K> alternatives) {
            this(targetId, unitsPerCraft, 0, alternatives, Map.of());
        }

        public Requirement(int targetId, int unitsPerCraft, int initialUnits, List<K> alternatives) {
            this(targetId, unitsPerCraft, initialUnits, alternatives, Map.of());
        }

        public Requirement {
            if (targetId < 0) {
                throw new IllegalArgumentException("targetId must be nonnegative");
            }
            if (unitsPerCraft <= 0) {
                throw new IllegalArgumentException("unitsPerCraft must be positive");
            }
            if (initialUnits < 0) {
                throw new IllegalArgumentException("initialUnits must be nonnegative");
            }
            List<K> suppliedAlternatives = List.copyOf(Objects.requireNonNull(alternatives, "alternatives"));
            if (suppliedAlternatives.isEmpty()) {
                throw new IllegalArgumentException("a requirement needs at least one alternative");
            }
            DesktopProtocol.requireCount(
                "alternatives",
                suppliedAlternatives.size(),
                DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT
            );
            ArrayList<K> distinctAlternatives = new ArrayList<>(suppliedAlternatives.size());
            HashSet<K> seenAlternatives = new HashSet<>();
            for (K alternative : suppliedAlternatives) {
                K checkedAlternative = Objects.requireNonNull(alternative, "alternative");
                if (seenAlternatives.add(checkedAlternative)) {
                    distinctAlternatives.add(checkedAlternative);
                }
            }
            alternatives = List.copyOf(distinctAlternatives);
            if (initialUnits > 0 && alternatives.size() != 1) {
                throw new IllegalArgumentException(
                    "initialUnits requires exactly one distinct alternative"
                );
            }
            LinkedHashMap<K, Integer> maximumUnits = new LinkedHashMap<>();
            for (Map.Entry<K, Integer> entry : Objects.requireNonNull(
                maximumUnitsByAlternative,
                "maximumUnitsByAlternative"
            ).entrySet()) {
                K key = Objects.requireNonNull(entry.getKey(), "capacity alternative");
                Integer value = Objects.requireNonNull(entry.getValue(), "maximum units");
                if (!alternatives.contains(key)) {
                    throw new IllegalArgumentException("capacity specified for an unknown alternative");
                }
                if (value < 0) {
                    throw new IllegalArgumentException("maximum units must be nonnegative");
                }
                maximumUnits.put(key, value);
            }
            maximumUnitsByAlternative = Collections.unmodifiableMap(maximumUnits);
        }
    }

    public record Allocation<K>(int targetId, Map<K, Integer> units) {
        public Allocation {
            units = Collections.unmodifiableMap(new LinkedHashMap<>(units));
        }
    }

    public record Plan<K>(
        int crafts,
        List<Allocation<K>> allocations,
        Map<K, Integer> remainingSupply,
        int attempts
    ) {
        public Plan {
            allocations = List.copyOf(allocations);
            remainingSupply = Collections.unmodifiableMap(new LinkedHashMap<>(remainingSupply));
        }
    }

    private final Comparator<? super K> keyComparator;

    public BoundedTransferPlanner(Comparator<? super K> keyComparator) {
        this.keyComparator = Objects.requireNonNull(keyComparator, "keyComparator");
    }

    public Optional<Plan<K>> planExact(Map<K, Integer> supply, List<Requirement<K>> requirements, int crafts) {
        if (crafts <= 0 || crafts > DesktopProtocol.MAX_TRANSFER_CRAFTS) {
            throw new IllegalArgumentException("crafts must be between 1 and " + DesktopProtocol.MAX_TRANSFER_CRAFTS);
        }
        NormalizedInput<K> input = normalize(supply, requirements);
        WorkBudget budget = new WorkBudget(DesktopProtocol.MAX_TRANSFER_ATTEMPTS);
        return Optional.ofNullable(run(input, crafts, budget));
    }

    public Optional<Plan<K>> planMaximum(Map<K, Integer> supply, List<Requirement<K>> requirements) {
        return planMaximum(supply, requirements, DesktopProtocol.MAX_TRANSFER_CRAFTS);
    }

    public Optional<Plan<K>> planMaximum(
        Map<K, Integer> supply,
        List<Requirement<K>> requirements,
        int maximumCrafts
    ) {
        if (maximumCrafts <= 0 || maximumCrafts > DesktopProtocol.MAX_TRANSFER_CRAFTS) {
            throw new IllegalArgumentException(
                "maximumCrafts must be between 1 and " + DesktopProtocol.MAX_TRANSFER_CRAFTS
            );
        }
        NormalizedInput<K> input = normalize(supply, requirements);
        long unitsPerCraft = input.requirements().stream()
            .map(IndexedRequirement::requirement)
            .mapToLong(Requirement::unitsPerCraft)
            .sum();
        long totalSupply = input.supply().values().stream().mapToLong(Integer::longValue).sum();
        long totalInitialUnits = input.requirements().stream()
            .map(IndexedRequirement::requirement)
            .mapToLong(Requirement::initialUnits)
            .sum();
        int low = 1;
        int high = (int) Math.min(
            maximumCrafts,
            (totalSupply + totalInitialUnits) / Math.max(1L, unitsPerCraft)
        );
        Plan<K> best = null;
        WorkBudget budget = new WorkBudget(DesktopProtocol.MAX_TRANSFER_ATTEMPTS);
        try {
            while (low <= high) {
                int middle = low + (high - low) / 2;
                Plan<K> candidate = run(input, middle, budget);
                if (budget.exhausted()) {
                    return Optional.empty();
                }
                if (candidate == null) {
                    high = middle - 1;
                } else {
                    best = candidate;
                    low = middle + 1;
                }
            }
        } catch (BudgetExceeded ignored) {
            return Optional.empty();
        }
        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(new Plan<>(
            best.crafts(),
            best.allocations(),
            best.remainingSupply(),
            budget.used()
        ));
    }

    private NormalizedInput<K> normalize(Map<K, Integer> rawSupply, List<Requirement<K>> rawRequirements) {
        Objects.requireNonNull(rawSupply, "supply");
        Objects.requireNonNull(rawRequirements, "requirements");
        DesktopProtocol.requireCount("requirements", rawRequirements.size(), DesktopProtocol.MAX_TRANSFER_REQUIREMENTS);
        if (rawRequirements.isEmpty()) {
            throw new IllegalArgumentException("requirements must not be empty");
        }

        List<IndexedRequirement<K>> indexed = new ArrayList<>(rawRequirements.size());
        Set<Integer> targetIds = new HashSet<>();
        Set<K> referencedVariants = new HashSet<>();
        int totalAlternatives = 0;
        for (int index = 0; index < rawRequirements.size(); index++) {
            Requirement<K> requirement = Objects.requireNonNull(rawRequirements.get(index), "requirement");
            if (!targetIds.add(requirement.targetId())) {
                throw new IllegalArgumentException("duplicate targetId " + requirement.targetId());
            }
            List<K> alternatives = requirement.alternatives().stream()
                .map(key -> Objects.requireNonNull(key, "alternative"))
                .distinct()
                .sorted(keyComparator)
                .toList();
            DesktopProtocol.requireCount(
                "alternatives",
                alternatives.size(),
                DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT
            );
            totalAlternatives = Math.addExact(totalAlternatives, alternatives.size());
            if (totalAlternatives > DesktopProtocol.MAX_TRANSFER_ALTERNATIVES) {
                throw new IllegalArgumentException("too many transfer alternatives");
            }
            referencedVariants.addAll(alternatives);
            LinkedHashMap<K, Integer> maximumUnits = new LinkedHashMap<>();
            for (K alternative : alternatives) {
                Integer maximum = requirement.maximumUnitsByAlternative().get(alternative);
                if (maximum != null) {
                    maximumUnits.put(alternative, maximum);
                }
            }
            indexed.add(new IndexedRequirement<>(
                index,
                new Requirement<>(
                    requirement.targetId(),
                    requirement.unitsPerCraft(),
                    requirement.initialUnits(),
                    alternatives,
                    maximumUnits
                )
            ));
        }

        LinkedHashMap<K, Integer> supply = new LinkedHashMap<>();
        rawSupply.entrySet().stream()
            .filter(entry -> entry.getKey() != null && referencedVariants.contains(entry.getKey()))
            .sorted(Map.Entry.comparingByKey(keyComparator))
            .forEach(entry -> {
                K key = entry.getKey();
                int count = Objects.requireNonNull(entry.getValue(), "supply count");
                if (count < 0) {
                    throw new IllegalArgumentException("supply counts must be nonnegative");
                }
                if (count > 0) {
                    supply.put(key, count);
                }
            });

        indexed.sort(Comparator
            .comparingInt((IndexedRequirement<K> value) -> value.requirement().alternatives().size())
            .thenComparingInt(value -> value.requirement().targetId()));
        return new NormalizedInput<>(supply, indexed, rawRequirements.size());
    }

    private Plan<K> run(NormalizedInput<K> input, int crafts, WorkBudget budget) {
        try {
            int requirementCount = input.requirements().size();
            int[] demands = new int[requirementCount];
            long[] requiredTotals = new long[requirementCount];
            long totalDemand = 0L;
            for (int index = 0; index < requirementCount; index++) {
                budget.step();
                Requirement<K> requirement = input.requirements().get(index).requirement();
                requiredTotals[index] = (long) requirement.unitsPerCraft() * crafts;
                long demand = Math.max(
                    0L,
                    requiredTotals[index] - requirement.initialUnits()
                );
                if (demand > Integer.MAX_VALUE) {
                    return null;
                }
                demands[index] = (int) demand;
                totalDemand = Math.addExact(totalDemand, demands[index]);
            }
            long totalSupply = 0L;
            for (Integer available : input.supply().values()) {
                budget.step();
                totalSupply = Math.addExact(totalSupply, available.longValue());
            }
            if (totalDemand > totalSupply) {
                return null;
            }

            LinkedHashMap<K, Integer> remaining = new LinkedHashMap<>(input.supply());
            boolean[] assigned = new boolean[requirementCount];
            Object[] selected = new Object[requirementCount];
            if (!assignUniformVariants(
                input.requirements(),
                demands,
                requiredTotals,
                remaining,
                assigned,
                selected,
                0,
                budget
            )) {
                return null;
            }

            List<Allocation<K>> allocations = new ArrayList<>(Collections.nCopies(input.originalRequirementCount(), null));
            for (int requirementIndex = 0; requirementIndex < requirementCount; requirementIndex++) {
                IndexedRequirement<K> indexed = input.requirements().get(requirementIndex);
                @SuppressWarnings("unchecked")
                K selectedKey = (K) selected[requirementIndex];
                LinkedHashMap<K, Integer> units = new LinkedHashMap<>();
                units.put(selectedKey, demands[requirementIndex]);
                allocations.set(indexed.originalIndex(), new Allocation<>(indexed.requirement().targetId(), units));
            }
            remaining.entrySet().removeIf(entry -> entry.getValue() == 0);
            return new Plan<>(crafts, allocations, remaining, budget.used());
        } catch (BudgetExceeded ignored) {
            return null;
        }
    }

    private boolean assignUniformVariants(
        List<IndexedRequirement<K>> requirements,
        int[] demands,
        long[] requiredTotals,
        LinkedHashMap<K, Integer> remaining,
        boolean[] assigned,
        Object[] selected,
        int assignedCount,
        WorkBudget budget
    ) {
        if (assignedCount == requirements.size()) {
            return true;
        }

        int next = -1;
        int nextFeasibleCount = Integer.MAX_VALUE;
        int nextDemand = -1;
        for (int index = 0; index < requirements.size(); index++) {
            if (assigned[index]) {
                continue;
            }
            int feasibleCount = 0;
            for (K alternative : requirements.get(index).requirement().alternatives()) {
                if (isFeasibleAlternative(
                    requirements.get(index).requirement(),
                    alternative,
                    demands[index],
                    requiredTotals[index],
                    remaining,
                    budget
                )) {
                    feasibleCount++;
                }
            }
            if (feasibleCount == 0) {
                return false;
            }
            if (feasibleCount < nextFeasibleCount
                || feasibleCount == nextFeasibleCount && demands[index] > nextDemand
                || feasibleCount == nextFeasibleCount && demands[index] == nextDemand
                    && requirements.get(index).requirement().targetId()
                        < requirements.get(next).requirement().targetId()) {
                next = index;
                nextFeasibleCount = feasibleCount;
                nextDemand = demands[index];
            }
        }

        int requirementIndex = next;
        int demand = demands[requirementIndex];
        List<K> candidates = new ArrayList<>();
        for (K alternative : requirements.get(requirementIndex).requirement().alternatives()) {
            if (isFeasibleAlternative(
                requirements.get(requirementIndex).requirement(),
                alternative,
                demand,
                requiredTotals[requirementIndex],
                remaining,
                budget
            )) {
                candidates.add(alternative);
            }
        }
        candidates.sort((left, right) -> {
            budget.step();
            int availableComparison = Integer.compare(
                remaining.getOrDefault(right, 0),
                remaining.getOrDefault(left, 0)
            );
            return availableComparison != 0
                ? availableComparison
                : keyComparator.compare(left, right);
        });
        for (K candidate : candidates) {
            budget.step();
            int available = remaining.getOrDefault(candidate, 0);
            remaining.put(candidate, available - demand);
            assigned[requirementIndex] = true;
            selected[requirementIndex] = candidate;
            if (assignUniformVariants(
                requirements,
                demands,
                requiredTotals,
                remaining,
                assigned,
                selected,
                assignedCount + 1,
                budget
            )) {
                return true;
            }
            selected[requirementIndex] = null;
            assigned[requirementIndex] = false;
            remaining.put(candidate, available);
        }
        return false;
    }

    private boolean isFeasibleAlternative(
        Requirement<K> requirement,
        K alternative,
        int demand,
        long requiredTotal,
        Map<K, Integer> remaining,
        WorkBudget budget
    ) {
        budget.step();
        if (remaining.getOrDefault(alternative, 0) < demand) {
            return false;
        }
        Integer maximumUnits = requirement.maximumUnitsByAlternative().get(alternative);
        return maximumUnits == null || requiredTotal <= maximumUnits;
    }

    private record IndexedRequirement<K>(int originalIndex, Requirement<K> requirement) {
    }

    private record NormalizedInput<K>(
        LinkedHashMap<K, Integer> supply,
        List<IndexedRequirement<K>> requirements,
        int originalRequirementCount
    ) {
    }

    private static final class WorkBudget {
        private final int maximum;
        private int used;
        private boolean exhausted;

        private WorkBudget(int maximum) {
            this.maximum = maximum;
        }

        private void step() {
            if (used >= maximum) {
                exhausted = true;
                throw new BudgetExceeded();
            }
            used++;
        }

        private int used() {
            return used;
        }

        private boolean exhausted() {
            return exhausted;
        }
    }

    private static final class BudgetExceeded extends RuntimeException {
        private BudgetExceeded() {
            super(null, null, false, false);
        }
    }

}
