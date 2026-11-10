package com.salts_inventory_update.compat.accessories;

import java.util.*;
import com.mojang.datafixers.util.Pair;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import static com.salts_inventory_update.compat.accessories.AccessoriesAccess.*;

/** Native server equipment slots; client snapshots never write into live wearable inventories. */
public final class AccessoriesDesktopMenu extends AbstractContainerMenu {
    static final int MAX=DesktopMenuBindings.maxMenuSlots();
    public final UUID ownerWindow;
    final Player player;
    final List<Group> groups;
    final List<Cell> cells=new ArrayList<>();
    private final List<Object> topology;
    private boolean obsolete;

    public AccessoriesDesktopMenu(int id,Inventory inventory,UUID owner){this(id,inventory,owner,describe(inventory.player));}
    public AccessoriesDesktopMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,data.readUUID(),readGroups(data));}
    private AccessoriesDesktopMenu(int id,Inventory inventory,UUID owner,List<Group> groups){
        super(AccessoriesIntegration.MENU,id);this.ownerWindow=owner;this.player=inventory.player;this.groups=List.copyOf(groups);
        this.topology=player.level().isClientSide?List.of():topology(player);
        int pairs=groups.stream().mapToInt(Group::amount).sum();if(pairs>MAX/2)throw new IllegalArgumentException("Too many accessory cells");
        SimpleContainer shadow=new SimpleContainer(pairs*2);
        for(Group group:groups)for(int i=0;i<group.amount;i++){
            Object ownerContainer=player.level().isClientSide?null:containers(player).get(group.name);
            for(boolean cosmetic:new boolean[]{false,true}){
                Cell cell=new Cell(group,i,cosmetic,ownerContainer);int menuIndex=cells.size();Slot slot;
                if(player.level().isClientSide){
                    slot=new Slot(shadow,menuIndex,0,0){
                        @Override public boolean mayPlace(ItemStack s){return insert(player,group.name,cell.index,s);}
                        @Override public boolean mayPickup(Player p){return cosmetic||AccessoriesAccess.remove(player,group.name,cell.index,getItem());}
                        @Override public int getMaxStackSize(ItemStack s){return maximum(s);}
                        @Override public Pair<ResourceLocation,ResourceLocation> getNoItemIcon(){return Pair.of(InventoryMenu.BLOCK_ATLAS,cosmetic?ResourceLocation.fromNamespaceAndPath("accessories","gui/slot/cosmetic"):group.icon);}
                    };
                }else{
                    if(ownerContainer==null)throw new IllegalStateException("Missing accessory container "+group.name);
                    slot=nativeSlot(ownerContainer,i,cosmetic,ignored->!obsolete&&containers(player).get(group.name)==ownerContainer&&((Number)call(ownerContainer,"getSize")).intValue()>cell.index);
                }
                cell.slot=slot;addSlot(slot);cells.add(cell);
            }
        }
    }
    void writeOpening(FriendlyByteBuf b){
        b.writeUUID(ownerWindow);b.writeVarInt(groups.size());for(Group g:groups){b.writeUtf(g.name,256);b.writeUtf(g.translation,512);b.writeResourceLocation(g.icon);b.writeVarInt(g.amount);}
    }
    private static List<Group> readGroups(FriendlyByteBuf b){
        int n=b.readVarInt(),total=0;if(n<0||n>MAX/2)throw new IllegalArgumentException("Accessory type count");var result=new ArrayList<Group>();var names=new HashSet<String>();
        for(int i=0;i<n;i++){
            String name=b.readUtf(256),translation=b.readUtf(512);ResourceLocation icon=b.readResourceLocation();int amount=b.readVarInt();
            if(amount<0||amount>MAX/2||(total+=amount)>MAX/2||!names.add(name))throw new IllegalArgumentException("Accessory slot count");
            result.add(new Group(name,translation,icon,amount));
        }return result;
    }
    static List<Group> describe(Player p){
        var available=containers(p);var result=new ArrayList<Group>();var seen=new HashSet<String>();
        var groups=new ArrayList<>((Collection<?>)call(type("data.SlotGroupLoader"),"getGroups",p.level(),false));
        groups.sort(Comparator.comparingInt(g->-((Number)call(g,"order")).intValue()));
        for(Object group:groups){
            var members=new ArrayList<Object>();for(Object name:(Collection<?>)call(group,"slots")){Object c=available.get(name);if(c!=null&&call(c,"slotType")!=null)members.add(c);}
            members.sort(Comparator.<Object>comparingInt(c->-((Number)call(call(c,"slotType"),"order")).intValue()).thenComparing(c->(String)call(c,"getSlotName")));
            for(Object container:members)addDescription(result,seen,container);
        }
        available.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->addDescription(result,seen,e.getValue()));return List.copyOf(result);
    }
    private static void addDescription(List<Group> out,Set<String> seen,Object container){
        String name=(String)call(container,"getSlotName");Object slotType=call(container,"slotType");int amount=((Number)call(container,"getSize")).intValue();
        if(amount<=0||slotType==null||!seen.add(name))return;
        out.add(new Group(name,(String)call(slotType,"translation"),(ResourceLocation)call(slotType,"icon"),amount));
    }
    private static List<Object> topology(Player p){var values=new ArrayList<Object>();containers(p).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{values.add(e.getKey());values.add(e.getValue());values.add(call(e.getValue(),"getAccessories"));values.add(call(e.getValue(),"getCosmeticAccessories"));values.add(call(e.getValue(),"getSize"));});return values;}
    boolean topologyChanged(){return obsolete||!groups.equals(describe(player))||!topology.equals(topology(player));}
    @Override public boolean stillValid(Player p){return p==player&&p.isAlive()&&!p.isSpectator();}
    @Override public void clicked(int id,int button,ClickType action,Player p){if(!player.level().isClientSide&&topologyChanged()){obsolete=true;return;}super.clicked(id,button,action,p);}
    @Override public void broadcastChanges(){if(!player.level().isClientSide&&topologyChanged())return;super.broadcastChanges();}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    record Group(String name,String translation,ResourceLocation icon,int amount) { }
    static final class Cell {
        final Group group;final int index;final boolean cosmetic;final Object backing;Slot slot;boolean render=true;
        Cell(Group group,int index,boolean cosmetic,Object backing){this.group=group;this.index=index;this.cosmetic=cosmetic;this.backing=backing;}
    }
}
