package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedScreenAccess;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.TemplatePersistanceControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.settings.StorageSettingsTabControlBase;

@Mixin(value = SettingsScreen.class, remap = false)
public abstract class SettingsScreenMixin implements SophisticatedScreenAccess {
    @Shadow private StorageSettingsTabControlBase settingsTabControl;
    @Shadow private InventoryScrollPanel inventoryScrollPanel;
    @Shadow private TemplatePersistanceControl templatePersistanceControl;

    @Invoker("extractSlot")
    protected abstract void salts_inventory_update$extractSlot(
        GuiGraphicsExtractor graphics,
        Slot slot,
        int mouseX,
        int mouseY
    );

    @Invoker("extractSlotOverlay")
    protected abstract void salts_inventory_update$extractSlotOverlay(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        int width,
        int color
    );

    @Invoker("isTemplateLoadHovered")
    protected abstract boolean salts_inventory_update$isTemplateLoadHovered();

    @Invoker("extractTooltip")
    protected abstract void salts_inventory_update$extractTooltip(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY
    );

    @Invoker("getHoveredSlot")
    protected abstract Slot salts_inventory_update$getHoveredSlot(double mouseX, double mouseY);

    @Override
    public Components components() {
        return new Components(
            settingsTabControl,
            null,
            null,
            null,
            null,
            null,
            templatePersistanceControl,
            null,
            null,
            java.util.List.of()
        );
    }

    @Override
    public void disableNativeInventoryScrollPanel() {
        InventoryScrollPanel panel = inventoryScrollPanel;
        if (panel == null) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        ScreenWidgetListsAccessor widgetLists = (ScreenWidgetListsAccessor) (Object) this;
        widgetLists.salts_inventory_update$children().remove(panel);
        widgetLists.salts_inventory_update$renderables().remove(panel);
        widgetLists.salts_inventory_update$narratables().remove(panel);
        if (screen.getFocused() == panel) {
            screen.setFocused(null);
        }
        inventoryScrollPanel = null;
    }

    @Override
    public void extractSlotContents(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        salts_inventory_update$extractSlot(graphics, slot, mouseX, mouseY);
        boolean templateHovered = salts_inventory_update$isTemplateLoadHovered();
        settingsTabControl.extractSlotOverlays(
            graphics,
            slot,
            this::salts_inventory_update$extractSlotOverlay,
            templateHovered
        );
        settingsTabControl.extractSlotExtra(graphics, slot);
    }

    @Override
    public void extractNativeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ((AbstractContainerScreenAccessor) (Object) this).salts_inventory_update$setHoveredSlot(
            salts_inventory_update$getHoveredSlot(mouseX, mouseY)
        );
        salts_inventory_update$extractTooltip(graphics, mouseX, mouseY);
    }
}
