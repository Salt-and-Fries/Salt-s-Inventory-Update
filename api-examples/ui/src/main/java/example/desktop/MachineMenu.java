package example.desktop;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.Holder;
import java.util.List;

/** Example mod-owned backend: no Salt classes, even when Salt is absent. */
public class MachineMenu extends AbstractContainerMenu {
    public record TransferPlan(int inputSlotId, List<ItemStack> alternatives, int maximumCrafts) { }
    /** Ordinary backend recipe interpretation, usable with or without Salt installed. */
    public TransferPlan normalizeRecipe(Recipe<?> recipe) {
        if (recipe.getType() != RecipeType.SMELTING) return null;
        var ingredients = recipe.placementInfo().ingredients();
        if (ingredients.size() != 1) return null;
        var alternatives = ingredients.get(0).items().map(Holder::value).map(Item::getDefaultInstance).toList();
        if (alternatives.isEmpty()) return null;
        return new TransferPlan(0, alternatives, 64);
    }
    public int progress;
    public MachineMenu(int id, Inventory inventory) { this(ExampleMenus.machine(), id, inventory); }
    protected MachineMenu(MenuType<?> type, int id, Inventory inventory) {
        super(type, id);
        var machine = new SimpleContainer(3);
        addSlot(new Slot(machine, 0, 12, 12));
        addSlot(new Slot(machine, 1, 12, 40));
        addSlot(new Slot(machine, 2, 100, 24) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        // Deliberately nonstandard coordinates: Salt identifies ownership, not a 4x9 rectangle.
        for (int i = 0; i < 36; i++) addSlot(new Slot(inventory, i, 170 + i % 4 * 18, 12 + i / 4 * 18));
        addDataSlot(new DataSlot() {
            @Override public int get() { return progress; }
            @Override public void set(int value) { progress = value; }
        });
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 1 || !stillValid(player)) return false;
        progress = (progress + 10) % 110;
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) { return player.isAlive() && !player.isSpectator(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack before = source.getItem().copy();
        if (!moveItemStackTo(source.getItem(), index < 3 ? 3 : 0, index < 3 ? slots.size() : 2, false)) return ItemStack.EMPTY;
        if (source.getItem().isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
        ItemStack taken = before.copy();
        taken.setCount(before.getCount() - source.getItem().getCount());
        source.onTake(player, taken);
        return before;
    }
}
