package com.salts_inventory_update.internal.desktop;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared eligibility for contextual counts and authoritative ingredient extraction. */
public final class ContextualRecipeSources {
    private ContextualRecipeSources() { }
    public record OwnedSlot(AbstractContainerMenu menu, Slot slot) { }
    public record PhysicalKey(Object owner,int index) {
        @Override public boolean equals(Object other){return other instanceof PhysicalKey key&&owner==key.owner&&index==key.index;}
        @Override public int hashCode(){return 31*System.identityHashCode(owner)+index;}
    }
    public static PhysicalKey key(AbstractContainerMenu menu,Slot slot){
        return new PhysicalKey(DesktopMenuBindings.physicalOwner(menu,slot),DesktopMenuBindings.physicalIndex(menu,slot));
    }

    /** Native transfer slots can share a dummy Slot.container; integrations own physical identity. */
    public static List<Slot> uniquePhysical(List<OwnedSlot> candidates,List<OwnedSlot> excluded) {
        return List.copyOf(physicalPool(candidates,excluded).values());
    }
    public static Map<PhysicalKey,Slot> physicalPool(List<OwnedSlot> candidates,List<OwnedSlot> excluded) {
        Set<PhysicalKey> blocked=new HashSet<>();
        for(OwnedSlot entry:excluded)blocked.add(key(entry.menu(),entry.slot()));
        Map<PhysicalKey,Slot> result=new LinkedHashMap<>();
        for(OwnedSlot entry:candidates){
            if(DesktopMenuBindings.isVirtualSlot(entry.menu(),entry.slot()))continue;
            PhysicalKey key=key(entry.menu(),entry.slot());
            if(key.owner()==null||key.index()<0||blocked.contains(key))continue;
            result.putIfAbsent(key,entry.slot());
        }
        return result;
    }
    /** Capacity after a recipe consumes ingredients, using native integration identity. */
    public static int outputCapacity(AbstractContainerMenu menu,Slot slot,ItemStack output,Map<PhysicalKey,ItemStack> plannedSources){
        if(!slot.isActive()||DesktopMenuBindings.isVirtualSlot(menu,slot)||!slot.mayPlace(output))return 0;
        ItemStack present=plannedSources.getOrDefault(key(menu,slot),slot.getItem());
        if(!present.isEmpty()&&!ItemStack.isSameItemSameTags(present,output))return 0;
        return Math.max(0,Math.min(output.getMaxStackSize(),slot.getMaxStackSize(output))-present.getCount());
    }
    public static boolean eligible(Player player, Slot slot, boolean allowEmpty) {
        if (slot == null || !slot.isActive() || !slot.mayPickup(player) || DesktopItemSourceLocks.isSlotLocked(player, slot)) return false;
        // Armor/offhand use the equipment container, so the inventory-index check below alone
        // cannot distinguish them from an ordinary open storage container or a menu alias.
        if (player.inventoryMenu != null) for (int index : new int[]{0, 5, 6, 7, 8, 45}) {
            if (index >= player.inventoryMenu.slots.size()) continue;
            Slot protectedSlot = player.inventoryMenu.slots.get(index);
            if (slot.container == protectedSlot.container && slot.getContainerSlot() == protectedSlot.getContainerSlot()) return false;
        }
        if (slot.getItem().isEmpty() && !allowEmpty) return false;
        if (slot.container == player.getInventory()) return slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36;
        if (InventoryExpansion.isExtraSlot(slot)) return true;
        return slot.mayPlace(slot.getItem());
    }
}
