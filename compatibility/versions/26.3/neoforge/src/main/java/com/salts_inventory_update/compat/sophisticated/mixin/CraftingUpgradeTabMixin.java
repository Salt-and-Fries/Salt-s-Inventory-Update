package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.datafixers.util.Pair;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedPositionAccess;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeTab;

/** Keeps Crafting's optional result-choice popup attached when its hosted tab moves. */
@Mixin(value = CraftingUpgradeTab.class, remap = false)
public abstract class CraftingUpgradeTabMixin implements SophisticatedHostedPositionAccess {
    @Shadow private @Nullable Pair<Position, Dimension> resultListPositionDimensions;
    @Shadow @Final private List<Position> resultChoicePositions;

    @Override
    public void salts_inventory_update$translateHostedPosition(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        Pair<Position, Dimension> bounds = resultListPositionDimensions;
        if (bounds != null) {
            Position topLeft = bounds.getFirst();
            resultListPositionDimensions = Pair.of(
                new Position(topLeft.x() + dx, topLeft.y() + dy),
                bounds.getSecond()
            );
        }
        for (int index = 0; index < resultChoicePositions.size(); index++) {
            Position choice = resultChoicePositions.get(index);
            resultChoicePositions.set(index, new Position(choice.x() + dx, choice.y() + dy));
        }
    }
}
