package com.salts_inventory_update.compat.curios;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.DynamicStackHandler;
import top.theillusivec4.curios.common.inventory.CosmeticCurioSlot;
import top.theillusivec4.curios.api.SlotContext;

/** Real native CurioSlots over Curios' inventory, with stable equipment/cosmetic menu indices. */
public final class CuriosDesktopMenu extends AbstractContainerMenu implements ICuriosMenu {
    private static final int MAX_SLOTS = com.salts_inventory_update.api.desktop.DesktopMenuBindings.maxMenuSlots();
    public final Player player;
    public final UUID ownerWindow;
    public final List<Group> groups;
    public boolean cosmetics;
    private boolean dirty;
    private final List<AccessorySlot> accessories = new ArrayList<>();
    private final List<Object> nativeTopology;

    public CuriosDesktopMenu(int id, Inventory inventory, UUID owner, boolean cosmetics) {
        this(id, inventory, owner, cosmetics, describe(inventory.player));
    }
    public CuriosDesktopMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readUUID(), data.readBoolean(), readGroups(data));
    }
    private CuriosDesktopMenu(int id, Inventory inventory, UUID owner, boolean cosmetics, List<Group> groups) {
        super(CuriosIntegration.MENU.get(), id);
        this.player = inventory.player;
        this.ownerWindow = owner;
        this.cosmetics = cosmetics;
        this.groups = List.copyOf(groups);
        this.nativeTopology = topology(this.player);
        long total = groups.stream().mapToLong(g -> g.size() * (g.cosmetic() ? 2L : 1L)).sum();
        if (total > MAX_SLOTS) throw new IllegalArgumentException("Too many Curios menu slots: " + total);
        for (Group group : groups) {
            var nativeGroup = group(this.player, group.id());
            for (boolean cosmetic : group.cosmetic() ? new boolean[]{false, true} : new boolean[]{false}) {
                IDynamicStackHandler handler;
                if (this.player.level().isClientSide) {
                    // Core snapshots own detached menu contents on the client. Do not overwrite the
                    // live wearable inventory when prediction or an acknowledgement updates a slot.
                    handler = new DynamicStackHandler(group.size(), index ->
                        new SlotContext(group.id(), this.player, index, cosmetic, true));
                } else {
                    if (nativeGroup == null) throw new IllegalStateException("Missing Curios inventory: " + group.id());
                    handler = cosmetic ? nativeGroup.getCosmeticStacks() : nativeGroup.getStacks();
                }
                for (int i = 0; i < group.size(); i++) {
                    var accessory = new AccessorySlot(this, group, handler, i, cosmetic);
                    this.addSlot(accessory.nativeSlot);
                    this.accessories.add(accessory);
                }
            }
        }
    }
    public void writeOpening(FriendlyByteBuf data) {
        data.writeUUID(this.ownerWindow); data.writeBoolean(this.cosmetics);
        data.writeVarInt(this.groups.size());
        for (Group group : this.groups) {
            data.writeUtf(group.id(), 256); data.writeVarInt(group.size());
            data.writeBoolean(group.cosmetic()); data.writeBoolean(group.toggle());
        }
    }
    private static List<Group> readGroups(FriendlyByteBuf data) {
        int count = data.readVarInt();
        if (count < 0 || count > MAX_SLOTS) throw new IllegalArgumentException("Invalid Curios group count");
        List<Group> result = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < count; i++) {
            Group group = new Group(data.readUtf(256), data.readVarInt(), data.readBoolean(), data.readBoolean());
            if (group.size() < 0 || group.size() > MAX_SLOTS || (total += group.size() * (group.cosmetic() ? 2 : 1)) > MAX_SLOTS)
                throw new IllegalArgumentException("Invalid Curios slot count");
            result.add(group);
        }
        return result;
    }
    static List<Group> describe(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inv -> inv.getCurios().values().stream()
            .filter(ICurioStacksHandler::isVisible)
            .map(g -> new Group(g.getIdentifier(), g.getStacks().getSlots(), g.hasCosmetic(), g.canToggleRendering()))
            .toList()).orElse(List.of());
    }
    static ICurioStacksHandler group(Player player, String id) {
        return CuriosApi.getCuriosInventory(player).resolve().flatMap(inv -> inv.getStacksHandler(id)).orElse(null);
    }
    private static List<Object> topology(Player player) {
        List<Object> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.getCurios().values().forEach(g -> {
            result.add(g.getIdentifier()); result.add(g.isVisible()); result.add(g.hasCosmetic());
            result.add(g.getStacks()); result.add(g.getCosmeticStacks()); result.add(g.getStacks().getSlots());
        }));
        return result;
    }
    public boolean topologyChanged() { return this.dirty || !this.groups.equals(describe(this.player)) || !this.nativeTopology.equals(topology(this.player)); }
    public List<AccessorySlot> displayedSlots() {
        return this.accessories.stream().filter(AccessorySlot::displayed).toList();
    }
    public boolean hasCosmetics() { return this.groups.stream().anyMatch(Group::cosmetic); }
    @Override public void resetSlots() { this.dirty = true; }
    @Override public boolean stillValid(Player player) { return player == this.player && player.isAlive(); }
    @Override public void clicked(int slotIndex, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        // A queued action may arrive before the public lifecycle hook replaces this layout.
        if (!this.player.level().isClientSide && this.topologyChanged()) return;
        super.clicked(slotIndex, button, clickType, player);
    }
    @Override public void broadcastChanges() {
        // An obsolete native layout must be replaced before publishing another snapshot.
        if (!this.player.level().isClientSide && this.topologyChanged()) return;
        super.broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    public record Group(String id, int size, boolean cosmetic, boolean toggle) { }

    public AccessorySlot accessory(Slot slot) { return this.accessories.get(slot.index); }

    /** Metadata around native equipment/cosmetic subclasses; no native slot behavior is copied. */
    public static final class AccessorySlot {
        public final CuriosDesktopMenu menu;
        public final Group group;
        public final IDynamicStackHandler backing;
        public final int accessoryIndex;
        private final boolean cosmetic;
        public final CurioSlot nativeSlot;
        AccessorySlot(CuriosDesktopMenu menu, Group group, IDynamicStackHandler handler, int index, boolean cosmetic) {
            this.menu = menu; this.group = group; this.backing = handler;
            this.accessoryIndex = index; this.cosmetic = cosmetic;
            if (cosmetic) {
                this.nativeSlot = new CosmeticCurioSlot(menu.player, handler, index, group.id(), 0, 0) {
                    @Override public ItemStack getItem() { return AccessorySlot.this.accessoryIndex < AccessorySlot.this.backing.getSlots() ? super.getItem() : ItemStack.EMPTY; }
                    @Override public boolean isActive() { return AccessorySlot.this.active(); }
                    @Override public boolean getRenderStatus() { return AccessorySlot.this.getRenderStatus(); }

                };
            } else {
                this.nativeSlot = new CurioSlot(menu.player, handler, index, group.id(), 0, 0, NonNullList.withSize(group.size(), true), group.toggle()) {
                    @Override public ItemStack getItem() { return AccessorySlot.this.accessoryIndex < AccessorySlot.this.backing.getSlots() ? super.getItem() : ItemStack.EMPTY; }
                    @Override public boolean isActive() { return AccessorySlot.this.active(); }
                    @Override public boolean getRenderStatus() { return AccessorySlot.this.getRenderStatus(); }

                };
            }
        }
        public boolean displayed() { return this.cosmetic == (this.menu.cosmetics && this.group.cosmetic()); }
        private boolean active() {
            return this.displayed() && this.accessoryIndex < this.backing.getSlots()
                && (this.menu.player.level().isClientSide || !this.menu.topologyChanged());
        }
        public boolean isCosmetic() { return this.cosmetic; }
        public boolean canToggleRender() { return this.group.toggle(); }
        public ItemStack getItem() { return this.nativeSlot.getItem(); }
        public boolean isActiveState() {
            var nativeGroup = group(this.menu.player, this.group.id());
            return nativeGroup != null;
        }
        public boolean getRenderStatus() {
            var nativeGroup = group(this.menu.player, this.group.id());
            return !this.group.toggle() || nativeGroup != null && this.accessoryIndex < nativeGroup.getRenders().size()
                && nativeGroup.getRenders().get(this.accessoryIndex);
        }
        public ItemStack displayStack() { return getItem(); }
        public java.util.List<net.minecraft.network.chat.Component> getSlotTooltip() {
            return java.util.List.of(net.minecraft.network.chat.Component.literal(this.nativeSlot.getSlotName()));
        }
    }
}
