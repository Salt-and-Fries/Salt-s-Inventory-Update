package com.salts_inventory_update.compat.aircraft;

import java.util.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import static com.salts_inventory_update.compat.aircraft.AircraftAccess.*;

/** Disjoint views of the original vehicle slots, never independent copies of its storage. */
public final class AircraftDesktopMenu extends AbstractContainerMenu {
    enum Zone { MAIN,PORT,STARBOARD }
    final Entity vehicle;
    final Player player;
    final Zone zone;
    final AbstractContainerMenu original;
    final List<Cell> cells=new ArrayList<>();
    final int bodyWidth,bodyHeight,width,height;
    boolean followingParent;
    public boolean cargoWing(){return zone!=Zone.MAIN;}
    public boolean followingParent(){return followingParent;}
    static final class Cell {
        final Object description;final String kind;final int nativeIndex,nativeX,nativeY;
        int x,y;Slot slot;
        Cell(Object d,int i){description=d;nativeIndex=i;kind=(String)call(d,"type");nativeX=number(d,"x");nativeY=number(d,"y");}
    }
    static Zone zone(Object d){int x=number(d,"x");return call(d,"type").equals("inventory")?(x<0?Zone.PORT:x>=176?Zone.STARBOARD:Zone.MAIN):Zone.MAIN;}
    AircraftDesktopMenu(int id,Inventory inventory,Entity vehicle,Zone zone){
        super(AircraftWindows.MENU,id);this.vehicle=vehicle;this.player=inventory.player;this.zone=zone;
        if(!(vehicle instanceof MenuProvider provider))throw new IllegalArgumentException("Aircraft has no native inventory");
        original=Objects.requireNonNull(provider.createMenu(id,inventory,player));var descriptions=descriptions(vehicle);
        if(descriptions.size()+36>DesktopMenuBindings.maxMenuSlots()||original.slots.size()!=descriptions.size()+36)throw new IllegalArgumentException("Aircraft inventory shape changed");
        for(int i=0;i<descriptions.size();i++)if(zone(descriptions.get(i))==zone)cells.add(new Cell(descriptions.get(i),i));
        SimpleContainer shadow=new SimpleContainer(cells.size());
        for(int i=0;i<cells.size();i++){
            Cell cell=cells.get(i);Slot nativeSlot=original.getSlot(cell.nativeIndex);
            Slot slot=inventory.player.level().isClientSide()?new Slot(shadow,i,0,0){
                @Override public boolean mayPlace(ItemStack stack){return nativeSlot.mayPlace(stack);}
                @Override public boolean mayPickup(Player p){return nativeSlot.mayPickup(p);}
                @Override public int getMaxStackSize(){return nativeSlot.getMaxStackSize();}
                @Override public int getMaxStackSize(ItemStack stack){return nativeSlot.getMaxStackSize(stack);}
                @Override public boolean isActive(){return nativeSlot.isActive();}
            }:nativeSlot;
            cell.slot=addSlot(slot);
        }
        // Only the central menu advertises the usual player-inventory companion.
        if(zone==Zone.MAIN)for(int i=descriptions.size();i<original.slots.size();i++)addSlot(original.getSlot(i));
        if(zone==Zone.MAIN){
            // Retain the approved Cargo Airship padding, but do not give short
            // aircraft an empty Cargo-sized lower half. Taller sprites expand it.
            int left=6,top=16,right=171,bottom=Math.min(98,cells.stream().mapToInt(c->c.nativeY).max().orElse(16)+26);
            for(Cell c:cells){boolean fuel=c.kind.equals("boiler"),cargo=c.kind.equals("inventory");left=Math.min(left,c.nativeX-(fuel?4:cargo?1:3));top=Math.min(top,c.nativeY-(fuel?18:cargo?1:3));right=Math.max(right,c.nativeX+(fuel?20:cargo?17:19));bottom=Math.max(bottom,c.nativeY+(fuel?21:cargo?17:19));}
            bodyWidth=right-left;bodyHeight=bottom-top;for(Cell c:cells){c.x=c.nativeX-left;c.y=c.nativeY-top;}
        }else{
            int left=cells.stream().mapToInt(c->c.nativeX).min().orElse(0),right=cells.stream().mapToInt(c->c.nativeX).max().orElse(0);
            var rows=cells.stream().map(c->c.nativeY).distinct().sorted().toList();var mapped=new HashMap<Integer,Integer>();int y=0;
            for(int i=0;i<rows.size();i++){if(i>0)y+=rows.get(i)-rows.get(i-1)==18?18:20;mapped.put(rows.get(i),y);}
            bodyWidth=right-left+18;bodyHeight=rows.isEmpty()?18:y+18;
            for(Cell c:cells){c.x=c.nativeX-left+1;c.y=mapped.get(c.nativeY)+1;}
        }
        // Both wings reserve the longer Starboard title, keeping mirrored cargo margins.
        width=Math.max(zone==Zone.MAIN?180:100,bodyWidth+14);height=bodyHeight+29;
    }
    void opening(FriendlyByteBuf b){b.writeInt(vehicle.getId());b.writeByte(zone.ordinal());}
    @Override public boolean stillValid(Player p){return p==player&&vehicle.isAlive()&&vehicle.level()==p.level()&&original.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public void removed(Player p){super.removed(p);original.removed(p);}
}
