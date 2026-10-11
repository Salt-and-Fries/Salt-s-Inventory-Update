package example.companion;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Own real server menu over the exact source inventory; closing any parent does not invalidate it. */
public final class ToolInventoryMenu extends AbstractContainerMenu {
    public final ToolInventorySource tool;
    public ToolInventoryMenu(int id, Inventory playerInventory, ToolInventorySource tool) {
        super(CompanionMenus.tool(), id); this.tool = tool;
        for (int i = 0; i < 9; i++) {
            final int index = i;
            addSlot(new Slot(tool.inventory, i, 8 + i * 18, 27) {
                @Override public boolean mayPlace(ItemStack stack) { return index != 8 && (stack.is(Items.REDSTONE) || stack.is(Items.PAPER)); }
                @Override public boolean mayPickup(Player player) { return index != 8 && tool.authorized(player); }
                @Override public int getMaxStackSize() { return index == 7 ? 1 : 64; }
            });
        }
        for (int i = 0; i < 36; i++) addSlot(new Slot(playerInventory, i, 8 + i % 9 * 18, 70 + i / 9 * 18));
    }
    @Override public boolean stillValid(Player player) { return tool.authorized(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack before = slot.getItem().copy();
        if (!moveItemStackTo(slot.getItem(), index < 9 ? 9 : 0, index < 9 ? slots.size() : 8, false)) return ItemStack.EMPTY;
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - slot.getItem().getCount()));
        return before;
    }
}
