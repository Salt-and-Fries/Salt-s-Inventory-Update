package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedScreenAccess;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTabControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Label;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

@Mixin(value = StorageScreenBase.class, remap = false)
public abstract class StorageScreenBaseMixin implements SophisticatedScreenAccess {
    @Shadow private UpgradeSettingsTabControl settingsTabControl;
    @Shadow private Button sortButton;
    @Shadow private ToggleButton<?> sortByButton;
    @Shadow private InventoryScrollPanel inventoryScrollPanel;
    @Shadow @Final private Set<ToggleButton<Boolean>> upgradeSwitches;
    @Shadow @Final private Map<Integer, UpgradeInventoryControlBase> inventoryControls;
    @Shadow @Final private Map<?, UpgradeInventoryControlBase> storageInventoryControls;
    @Shadow private Button transferToStorageButton;
    @Shadow private Button transferToInventoryButton;
    @Shadow private Label noResultsLabel;
    @Shadow private WidgetBase modalOverlay;

    @Invoker("renderSlot")
    protected abstract void salts_inventory_update$renderSlot(
        GuiGraphics graphics,
        Slot slot,
        int mouseX,
        int mouseY
    );

    @Invoker("drawSlotOverlays")
    protected abstract void salts_inventory_update$drawSlotOverlays(GuiGraphics graphics);

    @Invoker("renderErrorOverlay")
    protected abstract void salts_inventory_update$renderErrorOverlay(GuiGraphics graphics);

    @Invoker("renderTooltip")
    protected abstract void salts_inventory_update$renderTooltip(
        GuiGraphics graphics,
        int mouseX,
        int mouseY
    );

    /**
     * Sophisticated normally suppresses its quick-craft rendering while exactly one slot is selected.
     * Salt's desktop gesture is already live at that point, so bypass only that first guard while
     * leaving the later real set size intact for the native placement-count calculation.
     */
    @Redirect(
        method = "renderSlot",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I", ordinal = 0),
        require = 1,
        remap = false
    )
    private int salts_inventory_update$renderSingletonHostedQuickCraft(Set<?> slots) {
        int size = slots.size();
        return size == 1 && SophisticatedHostedScreenBridge.isHostedQuickCraftPreview() ? 2 : size;
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void salts_inventory_update$routeDetachedClick(int slotId, int button, ClickType input, CallbackInfo ci) {
        if (SophisticatedHostedScreenBridge.clickSlot(slotId, button, input)) {
            ci.cancel();
        }
    }

    @Override
    public Components components() {
        Label activeNoResults = noResultsLabel;
        if (activeNoResults != null
            && !((ScreenWidgetListsAccessor) (Object) this).salts_inventory_update$renderables().contains(activeNoResults)) {
            activeNoResults = null;
        }
        return new Components(
            settingsTabControl,
            salts_inventory_update$findSearchBox(),
            sortByButton,
            sortButton,
            transferToStorageButton,
            transferToInventoryButton,
            null,
            activeNoResults,
            modalOverlay,
            List.copyOf(upgradeSwitches)
        );
    }

    @Override
    public Collection<UpgradeInventoryControlBase> upgradeInventoryControls() {
        return List.copyOf(inventoryControls.values());
    }

    @Override
    public Collection<UpgradeInventoryControlBase> storageInventoryControls() {
        return List.copyOf(storageInventoryControls.values());
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
        salts_inventory_update$renderSlot(graphics.unwrap(), slot, mouseX, mouseY);
    }

    @Override
    public void extractSlotColorOverlays(GuiGraphicsExtractor graphics) {
        salts_inventory_update$drawSlotOverlays(graphics.unwrap());
    }

    @Override
    public void extractUpgradeErrors(GuiGraphicsExtractor graphics) {
        salts_inventory_update$renderErrorOverlay(graphics.unwrap());
    }

    @Override
    public void extractNativeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        StorageScreenBase<?> screen = (StorageScreenBase<?>) (Object) this;
        ((AbstractContainerScreenAccessor) (Object) this).salts_inventory_update$setHoveredSlot(
            screen.getHoveredSlot(mouseX, mouseY)
        );
        salts_inventory_update$renderTooltip(graphics.unwrap(), mouseX, mouseY);
    }

    private WidgetBase salts_inventory_update$findSearchBox() {
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof WidgetBase widget
                && child.getClass().getName().equals("net.p3pp3rf1y.sophisticatedcore.client.gui.SearchBox")) {
                return widget;
            }
        }
        return null;
    }
}
