package example.desktop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Virtual rows represent a mod-owned catalog, not fabricated inventory slots. */
public final class TerminalMenu extends MachineMenu {
    public static final int ROWS = 10000;
    public final int catalogSeed;
    public int selectedRow = -1;
    public TerminalMenu(int id, Inventory inventory) { this(id, inventory, 7); }
    public TerminalMenu(int id, Inventory inventory, int catalogSeed) {
        super(ExampleMenus.terminal(), id, inventory);
        this.catalogSeed = catalogSeed;
    }
    public ItemStack icon(int row) { return new ItemStack(row % 2 == 0 ? Items.DIAMOND : Items.IRON_INGOT); }
    public long count(int row) { return 1000000L + (long) row * (1 + Math.floorMod(catalogSeed, 100)); }
    public String label(int row) { return "Catalog entry " + row; }
}
