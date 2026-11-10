package com.salts_inventory_update.compat.quark;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Only the equipped bag's 27 slots; the player inventory remains SIU's shared inventory. */
public final class QuarkBackpackMenu extends AbstractContainerMenu {
    public final UUID ownerWindow;
    final Player owner;
    final ItemStack source;
    final Object storage;
    public boolean internalClosing;
    QuarkBackpackMenu(int id,Inventory inventory,UUID window) {
        super(QuarkWindows.BACKPACK,id);owner=inventory.player;ownerWindow=window;
        source=owner.getItemBySlot(EquipmentSlot.CHEST);
        storage=owner.level().isClientSide?null:QuarkAccess.backpackInventory(source);
        var shadow=new SimpleContainer(27);
        for(int i=0;i<27;i++) {
            int index=i;
            addSlot(storage==null?new Slot(shadow,i,0,0) {
                @Override public boolean mayPlace(ItemStack stack) {
                    var tag=(net.minecraft.tags.TagKey<net.minecraft.world.item.Item>)QuarkAccess.field(QuarkAccess.type("addons.oddities.module.BackpackModule"),"backpackBlockedTag");
                    return !stack.is(tag);
                }
            }:QuarkAccess.backpackSlot(storage,index));
        }
    }
    public static QuarkBackpackMenu read(int id,Inventory inventory,FriendlyByteBuf b) { return new QuarkBackpackMenu(id,inventory,b.readUUID()); }
    @Override public boolean stillValid(Player p) {
        return p==owner&&p.isAlive()&&!p.isSpectator()&&QuarkAccess.wearing(p)
            &&(p.level().isClientSide||p.getItemBySlot(EquipmentSlot.CHEST)==source);
    }
    @Override public ItemStack quickMoveStack(Player p,int index) { return ItemStack.EMPTY; }
}
