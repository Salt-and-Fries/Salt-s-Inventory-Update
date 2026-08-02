package com.salts_inventory_update.api.server.desktop;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.salts_inventory_update.protocol.DesktopProtocol;

/** Immutable result of server-side custom-menu recipe validation. */
public final class DesktopTransferDecision {
    public enum Status {
        UNSUPPORTED,
        DENIED,
        ALLOWED
    }

    private static final DesktopTransferDecision UNSUPPORTED = new DesktopTransferDecision(
        Status.UNSUPPORTED,
        List.of(),
        List.of(),
        0,
        ""
    );

    private final Status status;
    private final List<DesktopTransferRequirement> requirements;
    private final List<Integer> destinationSlots;
    private final int maximumCrafts;
    private final String reason;

    private DesktopTransferDecision(
        Status status,
        List<DesktopTransferRequirement> requirements,
        List<Integer> destinationSlots,
        int maximumCrafts,
        String reason
    ) {
        this.status = Objects.requireNonNull(status, "status");
        this.requirements = List.copyOf(requirements);
        this.destinationSlots = List.copyOf(destinationSlots);
        this.maximumCrafts = maximumCrafts;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public static DesktopTransferDecision unsupported() {
        return UNSUPPORTED;
    }

    public static DesktopTransferDecision denied(String reason) {
        String safeReason = Objects.requireNonNull(reason, "reason");
        if (safeReason.length() > 256) {
            throw new IllegalArgumentException("denial reason is too long");
        }
        return new DesktopTransferDecision(Status.DENIED, List.of(), List.of(), 0, safeReason);
    }

    public static DesktopTransferDecision allowed(
        List<DesktopTransferRequirement> requirements,
        List<Integer> destinationSlots,
        int maximumCrafts
    ) {
        List<DesktopTransferRequirement> safeRequirements = List.copyOf(requirements);
        List<Integer> safeDestinations = List.copyOf(destinationSlots);
        DesktopProtocol.requireCount(
            "transfer requirements",
            safeRequirements.size(),
            DesktopProtocol.MAX_TRANSFER_REQUIREMENTS
        );
        DesktopProtocol.requireCount(
            "transfer destinations",
            safeDestinations.size(),
            DesktopProtocol.MAX_RECIPE_SLOTS
        );
        if (safeRequirements.isEmpty() || maximumCrafts < 1 || maximumCrafts > DesktopProtocol.MAX_TRANSFER_CRAFTS) {
            throw new IllegalArgumentException("invalid allowed transfer decision");
        }

        Set<Integer> targets = new HashSet<>();
        int totalAlternatives = 0;
        for (DesktopTransferRequirement requirement : safeRequirements) {
            Objects.requireNonNull(requirement, "requirement");
            if (!targets.add(requirement.targetSlotId())) {
                throw new IllegalArgumentException("duplicate transfer target slot");
            }
            totalAlternatives = Math.addExact(totalAlternatives, requirement.alternatives().size());
        }
        if (totalAlternatives > DesktopProtocol.MAX_TRANSFER_ALTERNATIVES) {
            throw new IllegalArgumentException("too many transfer alternatives");
        }

        Set<Integer> uniqueDestinations = new HashSet<>();
        for (Integer destination : safeDestinations) {
            if (destination == null || destination < 0 || !uniqueDestinations.add(destination)) {
                throw new IllegalArgumentException("invalid or duplicate destination slot");
            }
        }
        if (!uniqueDestinations.containsAll(targets)) {
            throw new IllegalArgumentException("every requirement target must be an approved destination");
        }

        return new DesktopTransferDecision(
            Status.ALLOWED,
            safeRequirements,
            safeDestinations,
            maximumCrafts,
            ""
        );
    }

    public Status status() {
        return status;
    }

    public List<DesktopTransferRequirement> requirements() {
        return requirements;
    }

    public List<Integer> destinationSlots() {
        return destinationSlots;
    }

    public int maximumCrafts() {
        return maximumCrafts;
    }

    public String reason() {
        return reason;
    }

    public boolean supported() {
        return status != Status.UNSUPPORTED;
    }

    public boolean allowed() {
        return status == Status.ALLOWED;
    }
}

