package com.salts_inventory_update.compat.recipebrowser;

import java.util.List;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public interface RecipeBrowserView {
    Component title();

    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickProgress);

    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);

    boolean mouseReleased(MouseButtonEvent event);

    boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY);

    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);

    default @Nullable RecipeBrowserEntry entryAt(double mouseX, double mouseY) {
        return null;
    }

    default List<Component> tooltipAt(double mouseX, double mouseY) {
        return List.of();
    }

    default void close() {
    }
}
