package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

/** Immutable computed gesture state; NONE means this window is not participating. */
public record DesktopGestureSnapshot(long id, DesktopGesturePhase phase, int button, int quickCraftType,
                                     @Nullable UUID ownerInstanceId, List<DesktopSlotPreview> slots,
                                     ItemStack carriedPreview, @Nullable String carriedCountText,
                                     boolean pointerCaptured) {
    public DesktopGestureSnapshot { Objects.requireNonNull(phase); slots = List.copyOf(slots); carriedPreview = carriedPreview.copy(); }
    @Override public ItemStack carriedPreview() { return carriedPreview.copy(); }
    public @Nullable DesktopSlotPreview slot(int sessionId, int menuSlotId) {
        return slots.stream().filter(slot -> slot.sessionId() == sessionId && slot.menuSlotId() == menuSlotId).findFirst().orElse(null);
    }
}
