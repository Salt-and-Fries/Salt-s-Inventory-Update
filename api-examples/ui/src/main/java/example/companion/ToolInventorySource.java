package example.companion;

import java.util.UUID;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Ordinary backend. A mod such as AE supplies its actual tool inventory here, with no Salt dependency. */
public final class ToolInventorySource {
    public final UUID id;
    public final Container inventory;
    private final UUID owner;
    private final ItemStack sourceStack;
    private final int sourceSlot;
    private boolean valid = true;

    public ToolInventorySource(UUID id, Player owner, int sourceSlot, Container actualInventory) {
        this.id = java.util.Objects.requireNonNull(id);
        this.owner = owner.getUUID();
        this.sourceSlot = sourceSlot;
        this.sourceStack = owner.getInventory().getItem(sourceSlot);
        this.inventory = java.util.Objects.requireNonNull(actualInventory);
        if (sourceStack.isEmpty() || inventory.getContainerSize() != 9) throw new IllegalArgumentException("A real nine-slot tool is required");
    }

    private ToolInventorySource(UUID id) {
        this.id = id; this.owner = null; this.sourceStack = null; this.sourceSlot = -1;
        // This is only the ordinary client menu's synchronized replica, never the authoritative server inventory.
        this.inventory = new SimpleContainer(9);
    }

    public static ToolInventorySource clientReplica(UUID id) { return new ToolInventorySource(id); }
    public boolean authorized(Player player) {
        return valid && player.isAlive() && !player.isSpectator()
            && (owner == null ? !(player instanceof net.minecraft.server.level.ServerPlayer) : owner.equals(player.getUUID())
                && player.getInventory().getItem(sourceSlot) == sourceStack);
    }
    public void invalidate() { valid = false; }
}
