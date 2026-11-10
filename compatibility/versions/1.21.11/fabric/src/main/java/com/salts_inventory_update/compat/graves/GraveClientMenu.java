package com.salts_inventory_update.compat.graves;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The server retains the original SGUI handler. This is its client slot model. */
public final class GraveClientMenu extends ChestMenu {
    long real,take;
    GraveClientMenu(int id,Inventory inventory,long real,long take){
        super(UniversalGravesWindows.STORAGE,id,inventory,new SimpleContainer(45),5);
        this.real=real;this.take=take;
        for(int i=0;i<45;i++){
            final int index=i;var old=slots.get(i);
            var cell=new Slot(getContainer(),i,old.x,old.y){
                @Override public boolean mayPlace(ItemStack stack){return false;}
                @Override public boolean mayPickup(Player player){return index<36&&(GraveClientMenu.this.take&(1L<<index))!=0;}
            };cell.index=i;slots.set(i,cell);
        }
    }
    boolean real(int index){return index<36&&(real&(1L<<index))!=0;}
}
