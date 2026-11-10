package com.salts_inventory_update.compat.rei;

import me.shedaniel.rei.api.client.registry.screen.OverlayDecider;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionResult;

/** SIU supplies its own recipe browser inside the controller workspace. */
final class WorkspaceReiOverlayPolicy implements OverlayDecider {

    @Override
    public <R extends Screen> boolean isHandingScreen(Class<R> screenClass) {
        return com.salts_inventory_update.api.client.recipebrowser.DesktopRecipeBrowserScreen.class.isAssignableFrom(screenClass);
    }

    @Override
    public InteractionResult shouldScreenBeOverlaid(Class<?> screenClass) {
        if (com.salts_inventory_update.api.client.recipebrowser.DesktopRecipeBrowserScreen.class.isAssignableFrom(screenClass)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public double getPriority() {
        return 1000.0;
    }
}
