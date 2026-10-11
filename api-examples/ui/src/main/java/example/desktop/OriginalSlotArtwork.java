package example.desktop;

import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgets;

/** Mod-owned artwork reads computed previews without changing a Slot or carried stack. */
public final class OriginalSlotArtwork {
    public static void draw(DesktopRenderContext<?, ?> context, int id, int x, int y) {
        var slot = context.menuSlot(id);
        if (slot == null) return;
        var preview = context.gesture().slot(context.sessionId(), id);
        var stack = preview == null ? slot.getItem().copy() : preview.displayStack();
        String count = preview == null ? (stack.getCount() > 1 ? Integer.toString(stack.getCount()) : null) : preview.countText();
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF648091);
        context.fill(x, y, x + 16, y + 16, 0xFF18252B);
        context.item(stack, x, y);
        if (count != null) context.text(count, x + 17 - context.minecraft().font.width(count), y + 9, 0xFFFFFFFF, true);
        if (preview != null && preview.selected() || DesktopWidgets.contains(context.mouseX(), context.mouseY(), x, y, 16, 16)) {
            context.fill(x, y, x + 16, y + 16, 0x55FFFFFF);
        }
    }
    private OriginalSlotArtwork() { }
}
