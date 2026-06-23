package com.salts_inventory_update.api.client.desktop;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopWindowDefinition<T extends AbstractContainerMenu, S> {
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
        return DesktopWindowSize.of(context.windowWidth(), context.windowHeight());
    }

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
