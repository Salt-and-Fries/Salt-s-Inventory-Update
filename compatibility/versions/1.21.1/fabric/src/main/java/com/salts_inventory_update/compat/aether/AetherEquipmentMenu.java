package com.salts_inventory_update.compat.aether;

import java.util.*;
import com.mojang.datafixers.util.Pair;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Only native Aether equipment; no duplicate crafting, armor or backpack storage. */
public final class AetherEquipmentMenu extends AbstractContainerMenu {
    final Player player;
    public final UUID ownerWindow;
    final List<Slot> nativeSlots;
    final List<Cell> cells;
    private final List<Object> topology;
    private boolean obsolete;
    public record Cell(String identifier,int index,int x,int y,ResourceLocation icon,boolean toggle) { }
    public AetherEquipmentMenu(int id,Inventory inv,UUID owner){this(id,inv,owner,null);}
    public AetherEquipmentMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,data.readUUID(),read(data));}
    private AetherEquipmentMenu(int id,Inventory inv,UUID owner,List<Cell> described){
        super(AetherEquipment.MENU,id);player=inv.player;ownerWindow=owner;nativeSlots=AetherAccess.equipment(inv);
        topology=player.level().isClientSide?List.of():AetherAccess.topology(nativeSlots);
        if(described==null){
            var list=new ArrayList<Cell>();for(var s:nativeSlots){
                String name=AetherAccess.identifier(s),icon=name.replace("aether_","");if(icon.equals("accessory"))icon="misc";
                list.add(new Cell(name,s.getContainerSlot(),s.x-77,s.y-8,s.getNoItemIcon().getSecond(),AetherAccess.toggleable(s)));
            }cells=List.copyOf(list);
        }else cells=List.copyOf(described);
        if(cells.size()>DesktopMenuBindings.maxMenuSlots())throw new IllegalArgumentException("Too many Aether slots");
        var shadow=new SimpleContainer(cells.size());
        for(int i=0;i<cells.size();i++){
            var cell=cells.get(i);Slot slot;
            if(player.level().isClientSide){
                int index=i;var nativeSlot=nativeSlots.stream().filter(s->AetherAccess.identifier(s).equals(cell.identifier)&&s.getContainerSlot()==cell.index).findFirst().orElse(null);
                slot=new Slot(shadow,index,0,0){
                    @Override public boolean mayPlace(ItemStack s){return nativeSlot!=null&&nativeSlot.mayPlace(s);}
                    @Override public boolean mayPickup(Player p){return nativeSlot!=null&&nativeSlot.mayPickup(p);}
                    @Override public int getMaxStackSize(ItemStack s){return nativeSlot==null?1:nativeSlot.getMaxStackSize(s);}
                    @Override public Pair<ResourceLocation,ResourceLocation> getNoItemIcon(){return Pair.of(InventoryMenu.BLOCK_ATLAS,cell.icon);}
                };
            }else slot=nativeSlots.get(i);
            addSlot(slot);
        }
    }
    void writeOpening(FriendlyByteBuf data){data.writeUUID(ownerWindow);data.writeVarInt(cells.size());for(var c:cells){data.writeUtf(c.identifier,256);data.writeVarInt(c.index);data.writeVarInt(c.x);data.writeVarInt(c.y);data.writeResourceLocation(c.icon);data.writeBoolean(c.toggle);}}
    private static List<Cell> read(FriendlyByteBuf data){
        int count=data.readVarInt();if(count<0||count>DesktopMenuBindings.maxMenuSlots())throw new IllegalArgumentException("Aether slot count");var out=new ArrayList<Cell>();var seen=new HashSet<String>();
        for(int i=0;i<count;i++){String name=data.readUtf(256);int index=data.readVarInt(),x=data.readVarInt(),y=data.readVarInt();var icon=data.readResourceLocation();boolean toggle=data.readBoolean();
            if(index<0||x<0||x>2048||y<0||y>2048||!seen.add(name+":"+index))throw new IllegalArgumentException("Aether slot metadata");out.add(new Cell(name,index,x,y,icon,toggle));}
        return out;
    }
    boolean topologyChanged(){return obsolete||!topology.equals(AetherAccess.topology(AetherAccess.equipment(player.getInventory())));}
    @Override public boolean stillValid(Player p){return p==player&&p.isAlive()&&!p.isSpectator();}
    @Override public void clicked(int id,int button,ClickType action,Player p){if(!player.level().isClientSide&&topologyChanged()){obsolete=true;return;}super.clicked(id,button,action,p);}
    @Override public void broadcastChanges(){if(!player.level().isClientSide&&topologyChanged())return;super.broadcastChanges();}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
