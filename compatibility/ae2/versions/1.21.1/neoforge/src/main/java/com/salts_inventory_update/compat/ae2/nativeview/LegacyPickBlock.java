package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import net.minecraft.world.inventory.Slot;

/** Adapts keyboard cloning to AE2 19's existing native mouse-only pick handlers. */
public final class LegacyPickBlock {
    private LegacyPickBlock() { }

    public static boolean storage(MEStorageScreen<?> screen, Slot slot) {
        return slot instanceof RepoSlot repo && repo.isCraftable()
                && LegacyPickScope.invoke(screen, slot, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }

    public static boolean pattern(PatternEncodingTermScreen<?> screen, Slot slot) {
        if (slot != null && screen.getMenu().canModifyAmountForSlot(slot)
                && GenericStack.fromItemStack(slot.getItem()) != null)
            return LegacyPickScope.invoke(screen, slot, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
        return storage(screen, slot);
    }
}
