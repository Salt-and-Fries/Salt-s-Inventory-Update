package example.companion;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A main machine session. It contains no tool slots or proxies of the companion inventory. */
public final class CompanionMainMenu extends AbstractContainerMenu {
    public final ToolInventorySource tool;
    public final int machineNumber;
    private boolean valid = true;
    public CompanionMainMenu(int id, Inventory playerInventory, ToolInventorySource tool, int machineNumber) {
        super(CompanionMenus.main(), id);
        this.tool = tool; this.machineNumber = machineNumber;
        addSlot(new Slot(new SimpleContainer(1), 0, 8, 32));
        for (int i = 0; i < 36; i++) addSlot(new Slot(playerInventory, i, 8 + i % 9 * 18, 80 + i / 9 * 18));
    }
    public void invalidate() { valid = false; }
    @Override public boolean stillValid(Player player) { return valid && player.isAlive() && !player.isSpectator(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack before = slot.getItem().copy();
        if (!moveItemStackTo(slot.getItem(), index == 0 ? 1 : 0, index == 0 ? slots.size() : 1, false)) return ItemStack.EMPTY;
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - slot.getItem().getCount()));
        return before;
    }
}
