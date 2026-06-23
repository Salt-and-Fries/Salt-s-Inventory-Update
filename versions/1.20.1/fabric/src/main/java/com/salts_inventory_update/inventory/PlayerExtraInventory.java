package com.salts_inventory_update.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerExtraInventory implements Container {
    private final Player owner;
    private NonNullList<ItemStack> items = NonNullList.create();
    private int timesChanged;

    public PlayerExtraInventory(Player owner) {
        this.owner = owner;
    }

    public void resize(int size) {
        int clampedSize = Math.max(0, size);
        if (this.items.size() == clampedSize) {
            return;
        }

        NonNullList<ItemStack> resized = NonNullList.withSize(clampedSize, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(this.items.size(), resized.size()); i++) {
            resized.set(i, this.items.get(i));
        }
        this.items = resized;
        this.setChanged();
    }

    public void loadSnapshot(int slotCount, List<ItemStack> stacks) {
        this.resize(slotCount);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
        }
        this.setChanged();
    }

    public void copyFrom(PlayerExtraInventory other, boolean copyContents) {
        this.resize(other.getContainerSize());
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, copyContents ? other.getItem(i).copy() : ItemStack.EMPTY);
        }
        this.setChanged();
    }

    public List<ItemStack> snapshot() {
        List<ItemStack> snapshot = new ArrayList<>(this.items.size());
        for (ItemStack item : this.items) {
            snapshot.add(item.copy());
        }
        return snapshot;
    }

    public boolean insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        int originalCount = stack.getCount();
        if (stack.isStackable()) {
            for (ItemStack item : this.items) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!item.isEmpty() && ItemStack.isSameItemSameTags(item, stack)) {
                    moveIntoExistingStack(stack, item, this.maxStackSize(item));
                }
            }
        }

        for (int i = 0; i < this.items.size() && !stack.isEmpty(); i++) {
            if (this.items.get(i).isEmpty()) {
                int moved = Math.min(stack.getCount(), this.maxStackSize(stack));
                this.items.set(i, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }

        boolean changed = stack.getCount() != originalCount;
        if (changed) {
            this.setChanged();
        }
        return changed;
    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> predicate, int maxCount, boolean simulate) {
        int count = 0;
        for (ItemStack stack : this.items) {
            int remaining = maxCount == 0 ? 0 : maxCount - count;
            count += ContainerHelper.clearOrCountMatchingItems(stack, predicate, remaining, simulate);
            if (maxCount != 0 && count >= maxCount) {
                break;
            }
        }
        if (!simulate && count > 0) {
            this.setChanged();
        }
        return count;
    }

    public void dropAll() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                this.owner.drop(stack, true);
                this.items.set(i, ItemStack.EMPTY);
            }
        }
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }

        this.items.set(slot, stack);
        if (!stack.isEmpty()) {
            stack.setCount(Math.min(stack.getCount(), this.maxStackSize(stack)));
        }
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.timesChanged++;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.owner;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
    }

    private static void moveIntoExistingStack(ItemStack moving, ItemStack target, int maxStackSize) {
        int available = Math.min(maxStackSize, target.getMaxStackSize()) - target.getCount();
        if (available <= 0) {
            return;
        }

        int moved = Math.min(available, moving.getCount());
        target.grow(moved);
        moving.shrink(moved);
    }

    private int maxStackSize(ItemStack stack) {
        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
    }
}
