package com.salts_inventory_update.controlify.input;

import dev.isxander.controlify.controller.ControllerEntity;
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
        if (!ControlifyInputBridge.update(controller) && Minecraft.getInstance().screen == screen) {
            super.onControllerUpdate(controller);
        }
    }

    @Override
    protected void handleTabNavigation(ControllerEntity controller) {
        // Physical LB/RB are reserved for cycling workspace windows.
    }

    @Override
    protected void handleScreenVMouse(ControllerEntity controller, VirtualMouseHandler vmouse) {
        // A rebound close button owns its press/release even if it also has an item action binding.
        // Cursor movement and scrolling have already run in Controlify's normal handler.
        if (!ControlifyInputBridge.closeOwnsButtons(controller)) {
            super.handleScreenVMouse(controller, vmouse);
        }
    }

    @Override
    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.CURSOR_ONLY;
    }
}
