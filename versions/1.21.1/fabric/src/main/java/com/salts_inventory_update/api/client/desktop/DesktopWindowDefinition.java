package com.salts_inventory_update.api.client.desktop;

import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.client.input.CharacterEvent;
import com.salts_inventory_update.client.input.KeyEvent;
import com.salts_inventory_update.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopWindowDefinition<T, S> {
    /** Immutable surface-relative rectangles. Each control kind may occur once; rectangles must not overlap. */
    default java.util.List<DesktopSurfaceControl> managementControls(DesktopWindowContext<T, S> context) { return java.util.List.of(); }

    /** Paint inside the original artwork using the same committed geometry Salt routes. No host frame is added. */
    default void renderManagementControls(DesktopRenderContext<T, S> context, java.util.List<DesktopControlState> controls) { }

    default java.util.List<DesktopPopup> popups(DesktopWindowContext<T, S> context) { return java.util.List.of(); }

    /** Return true after removing the popup from the model and its next layout/paint. */
    default boolean dismissPopup(DesktopWindowContext<T, S> context, String id, DesktopPopup.Dismissal reason) { return false; }

    default void gestureChanged(DesktopWindowContext<T, S> context, DesktopGestureEvent event) { }
    /** Called for an unconsumed Tab/Shift-Tab within this keyboard owner. */
    default boolean focusNext(DesktopWindowContext<T, S> context, boolean forward) { return false; }

    /** Metadata narration remains available when there is no visible host title. */
    default void narrate(DesktopWindowContext<T, S> context, net.minecraft.client.gui.narration.NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, title(context));
    }

    /** Null uses Salt's artwork. A custom renderer owns all native chrome painting, not its routing. */
    default @Nullable DesktopWindowDecoration<T, S> decoration(DesktopWindowContext<T, S> context) { return null; }

    /** Pure layout query. The manager commits the result for rendering and input. */
    default DesktopWindowSurface surface(DesktopWindowContext<T, S> context) {
        return DesktopWindowSurface.SALT;
    }

    /** Prepare native widget geometry using this candidate surface before final hit regions are sampled. */
    default void prepareLayout(DesktopWindowContext<T, S> context) { }

    default void windowStateChanged(DesktopWindowContext<T, S> context, DesktopWindowState state) { }

    /** Preference schema/layout revision. A mismatch starts with fresh local state. */
    default String layoutRevision(DesktopWindowContext<T, S> context) { return ""; }

    default void environmentChanged(DesktopWindowContext<T, S> context, DesktopWindowEnvironment environment) { }

    /** Called once per unavailable size/viewport for ADAPT. Select a compact layout or another policy; dimensions remain unscaled until then. */
    default void surfaceUnavailable(DesktopWindowContext<T, S> context) { }

    default @Nullable S createState(DesktopWindowSetupContext<T> context) {
        return null;
    }

    default void loadLocalState(DesktopWindowContext<T, S> context, CompoundTag tag) {
    }

    default void saveLocalState(DesktopWindowContext<T, S> context, CompoundTag tag) {
    }

    default Component title(DesktopWindowContext<T, S> context) {
        return context.originalTitle();
    }

    default DesktopWindowSize defaultSize(DesktopWindowSetupContext<T> context) {
        return DesktopWindowSize.of(context.defaultWindowWidth(), context.defaultWindowHeight());
    }

    default DesktopWindowSize minSize(DesktopWindowContext<T, S> context) {
        return DesktopWindowSize.of(64, 48);
    }

    /** Null imposes no author maximum. Dimensions describe the complete surface, including declared insets. */
    default @Nullable DesktopWindowSize maxSize(DesktopWindowContext<T, S> context) { return null; }

    default DesktopResizePolicy resizePolicy(DesktopWindowContext<T, S> context) {
        return DesktopResizePolicy.FIXED;
    }

    default @Nullable DesktopWindowSize snapSize(DesktopWindowContext<T, S> context) {
        return null;
    }

    default void opened(DesktopWindowContext<T, S> context) {
    }

    default void closed(DesktopWindowContext<T, S> context) {
    }

    default void moved(DesktopWindowContext<T, S> context) {
    }

    default void resized(DesktopWindowContext<T, S> context) {
    }

    /** Keyboard focus is independent of the selected shift-transfer inventory target. */
    default void keyboardFocusChanged(DesktopWindowContext<T, S> context, boolean focused) { }

    default DesktopWindowOptions options(DesktopWindowContext<T, S> context) {
        return DesktopWindowOptions.DEFAULT;
    }

    /** A desktop-modal window blocks pointer, keyboard and inventory interaction behind it. */
    default boolean modal(DesktopWindowContext<T, S> context) { return false; }

    /** Render above this window's content/frame and below later windows. */
    default void renderOverlay(DesktopRenderContext<T, S> context) { }

    default void focusChanged(DesktopWindowContext<T, S> context, boolean focused) {
    }

    default void ghosted(DesktopWindowContext<T, S> context) {
    }

    default void unghosted(DesktopWindowContext<T, S> context) {
    }

    default void tick(DesktopWindowContext<T, S> context) {
    }

    default void render(DesktopRenderContext<T, S> context) {
    }

    default @Nullable RecipeBookComponent createRecipeBook(DesktopWindowContext<T, S> context) {
        return null;
    }

    default @Nullable DesktopSlotHit slotAt(DesktopSlotContext<T, S> context, double mouseX, double mouseY) {
        return null;
    }

    /** Hover/motion is routed to the topmost window or the current pointer capture owner. */
    default void mouseMoved(DesktopInputContext<T, S> context, double mouseX, double mouseY) { }

    default boolean mouseClicked(DesktopInputContext<T, S> context, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    default boolean mouseReleased(DesktopInputContext<T, S> context, MouseButtonEvent event) {
        return false;
    }

    default boolean mouseDragged(DesktopInputContext<T, S> context, MouseButtonEvent event, double dx, double dy) {
        return false;
    }

    default boolean mouseScrolled(DesktopInputContext<T, S> context, double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    default boolean keyPressed(DesktopInputContext<T, S> context, KeyEvent event) {
        return false;
    }

    default boolean keyReleased(DesktopInputContext<T, S> context, KeyEvent event) { return false; }

    default boolean charTyped(DesktopInputContext<T, S> context, CharacterEvent event) {
        return false;
    }

    default boolean wantsTextInput(DesktopWindowContext<T, S> context) {
        return false;
    }

    default boolean appendTooltip(DesktopRenderContext<T, S> context, int mouseX, int mouseY) {
        return false;
    }

    default void customPayload(DesktopWindowContext<T, S> context, ResourceLocation channel, byte[] data) {
    }
}
