package com.salts_inventory_update.compat.sophisticated.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

/**
 * Internal view of the native Sophisticated screen pieces used by Salt's component host.
 *
 * <p>The interface deliberately exposes role-addressable widgets instead of the native
 * screen's complete renderable list. The latter also contains the Sophisticated inventory
 * scroll panel and other full-screen implementation details that must not be rendered by a
 * detached Salt window.</p>
 */
public interface SophisticatedScreenAccess {
    Components components();

    default Collection<UpgradeInventoryControlBase> upgradeInventoryControls() {
        return List.of();
    }

    default Collection<UpgradeInventoryControlBase> storageInventoryControls() {
        return List.of();
    }

    /** Removes Sophisticated's own storage scroll panel; Salt owns responsive scrolling. */
    void disableNativeInventoryScrollPanel();

    /** Extracts the native stack, count, memory/filter icon, and per-slot decoration. */
    void extractSlotContents(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY);

    /** Extracts synchronized color overlays after the visible slots have been positioned. */
    default void extractSlotColorOverlays(GuiGraphicsExtractor graphics) {
    }

    /** Extracts upgrade error highlights/messages after slots and upgrade controls. */
    default void extractUpgradeErrors(GuiGraphicsExtractor graphics) {
    }

    void extractNativeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    /** Native controls grouped by the role the Salt layout needs to assign. */
    record Components(
        @Nullable WidgetBase sideTabs,
        @Nullable WidgetBase search,
        @Nullable WidgetBase sortMode,
        @Nullable WidgetBase sort,
        @Nullable WidgetBase transferToStorage,
        @Nullable WidgetBase transferToInventory,
        @Nullable WidgetBase templatePersistence,
        @Nullable WidgetBase noResults,
        @Nullable WidgetBase modal,
        List<WidgetBase> upgradeSwitches
    ) {
        public Components {
            upgradeSwitches = List.copyOf(upgradeSwitches);
        }

        /** All role widgets in stable paint order, excluding null/unsupported roles. */
        public List<WidgetBase> layoutWidgets() {
            List<WidgetBase> widgets = new ArrayList<>();
            addIfPresent(widgets, search);
            addIfPresent(widgets, sortMode);
            addIfPresent(widgets, sort);
            addIfPresent(widgets, transferToStorage);
            addIfPresent(widgets, transferToInventory);
            widgets.addAll(upgradeSwitches);
            addIfPresent(widgets, sideTabs);
            addIfPresent(widgets, templatePersistence);
            addIfPresent(widgets, noResults);
            addIfPresent(widgets, modal);
            return List.copyOf(widgets);
        }

        /** Interactive widgets in input-priority order; modal controls are considered first. */
        public List<GuiEventListener> interactiveWidgets() {
            List<GuiEventListener> widgets = new ArrayList<>();
            addIfPresent(widgets, modal);
            addIfPresent(widgets, sideTabs);
            addIfPresent(widgets, templatePersistence);
            addIfPresent(widgets, search);
            addIfPresent(widgets, sortMode);
            addIfPresent(widgets, sort);
            addIfPresent(widgets, transferToStorage);
            addIfPresent(widgets, transferToInventory);
            widgets.addAll(upgradeSwitches);
            return List.copyOf(widgets);
        }

        private static <T> void addIfPresent(List<T> values, @Nullable T value) {
            if (value != null) {
                values.add(value);
            }
        }
    }
}
