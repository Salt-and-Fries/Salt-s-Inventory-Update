package com.salts_inventory_update.api.server.desktop;

import java.util.List;

import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.protocol.DesktopProtocol;

/** One normalized, server-approved ingredient requirement. */
public record DesktopTransferRequirement(int targetSlotId, int count, List<ItemStack> alternatives) {
    public DesktopTransferRequirement {
        if (targetSlotId < 0 || count <= 0) {
            throw new IllegalArgumentException("target slot and count must be valid");
        }
        alternatives = alternatives == null
            ? List.of()
            : alternatives.stream().map(ItemStack::copy).toList();
        if (alternatives.isEmpty() || alternatives.size() > DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT) {
            throw new IllegalArgumentException("invalid transfer alternatives");
        }
    }

    @Override
    public List<ItemStack> alternatives() {
        return alternatives.stream().map(ItemStack::copy).toList();
    }
}

