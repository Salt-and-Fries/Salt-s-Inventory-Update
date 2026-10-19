package com.salts_inventory_update.controlify.input;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.InputMode;
import com.salts_inventory_update.controlify.WorkspaceActionGuide;
import dev.isxander.controlify.screenop.compat.vanilla.AbstractContainerScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps Controlify's normal cursor and native UI handling outside radial selection. */
final class WorkspaceScreenProcessor<M extends AbstractContainerMenu, T extends AbstractContainerScreen<M> & WorkspaceContainerScreen>
        extends AbstractContainerScreenProcessor<T> {
    WorkspaceScreenProcessor(T screen) {
        super(screen, screen::controllerHoveredSlot, screen::controllerClickSlot, controller -> false);
    }

    @Override
    public void onControllerUpdate(ControllerEntity controller) {
        if (!ControlifyInputBridge.update(controller) && Minecraft.getInstance().gui.screen() == screen) {
            super.onControllerUpdate(controller);
        }
        WorkspaceActionGuide.update(screen, controller);
    }

    // The workspace explicitly renders its own native guide instance in every mode. These
    // overrides prevent the base container instance from adding a second, stale item guide.
    @Override public void onWidgetRebuild() { }
    @Override public void onInputModeChanged(InputMode mode) { }

    @Override
    public boolean tryOpenKeyboard(ControllerEntity controller,
            net.minecraft.client.gui.components.events.GuiEventListener component) {
        // Let the hosted widget acquire native focus first. The workspace then opens its
        // keyboard through the API owner, preserving modal input and the correct text target.
        return false;
    }

    @Override
    protected void handleTabNavigation(ControllerEntity controller) {
        // Physical LB/RB are reserved for cycling workspace windows.
    }

    @Override
    protected void handleScreenVMouse(ControllerEntity controller, VirtualMouseHandler vmouse) {
        // A rebound close button owns its press/release even if it also has an item action binding.
        // Native movement/scrolling already ran before this hook. Catalog clicks belong to
        // the configured select/inventory actions, not native virtual-mouse click aliases.
        var guideState = screen.controllerGuideState();
        if (!ControlifyInputBridge.closeOwnsButtons(controller)
                && (guideState == null || !guideState.creativeCatalogItem())) {
            super.handleScreenVMouse(controller, vmouse);
        }
    }

    @Override
    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.CURSOR_SCROLL;
    }
}
