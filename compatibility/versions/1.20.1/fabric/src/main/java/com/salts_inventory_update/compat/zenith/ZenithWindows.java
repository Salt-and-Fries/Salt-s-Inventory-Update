package com.salts_inventory_update.compat.zenith;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Original Zenith menus, ownership and actions; only their presentation changes. */
public final class ZenithWindows {
    static final String BASE="dev.shadowsoffire.apotheosis.";
    public enum Kind {
        ENCHANTING("enchanting_table","ench.table.ApothEnchantmentMenu","ench.table.ApothEnchScreen",false,2,0,178,126),
        LIBRARY("library","ench.library.EnchLibraryContainer","ench.library.EnchLibraryScreen",true,3,143,190,176),
        FLETCHING("fletching","village.fletching.FletchingContainer","village.fletching.FletchingScreen",false,4,0,176,93),
        SALVAGING("salvage","adventure.affix.salvaging.SalvagingMenu","adventure.affix.salvaging.SalvagingScreen",true,18,87,190,120),
        GEM_CUTTING("gem_cutting","adventure.socket.gem.cutting.GemCuttingMenu","adventure.socket.gem.cutting.GemCuttingScreen",false,4,93,190,126),
        REFORGING("reforging","adventure.affix.reforging.ReforgingMenu","adventure.affix.reforging.ReforgingScreen",true,6,179,190,212),
        AUGMENTING("augmenting","adventure.affix.augmenting.AugmentingMenu","adventure.affix.augmenting.AugmentingScreen",true,2,135,190,168);
        public final String path,menu,screen;
        public final boolean position;
        public final int slots,bodyHeight,width,height;
        Kind(String path,String menu,String screen,boolean position,int slots,int bodyHeight,int width,int height){
            this.path=path;this.menu=BASE+menu;this.screen=BASE+screen;this.position=position;
            this.slots=slots;this.bodyHeight=bodyHeight;this.width=width;this.height=height;
        }
    }
    public static final ResourceLocation STATS=id("enchanting/stats"),CLUES=id("enchanting/clues"),SYNC=id("enchanting/state");
    private static final class Owner {
        final ServerPlayer player;final int session;
        byte[] stats;final Map<Integer,byte[]> clues=new TreeMap<>();boolean dirty;
        ItemStack input=ItemStack.EMPTY;
        Owner(ServerPlayer player,int session){this.player=player;this.session=session;}
    }
    private static final Map<AbstractContainerMenu,Owner> ENCHANTING=new IdentityHashMap<>();
    private static final ThreadLocal<AbstractContainerMenu> GENERATING=new ThreadLocal<>();
    private static final Set<Kind> BOUND=EnumSet.noneOf(Kind.class);
    private ZenithWindows() { }
    static ResourceLocation id(String path){return new ResourceLocation("salts_inventory_compat","zenith/"+path);}
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}

    private static synchronized void bind(){
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("zenith:"+kind.path);
            if(type==null||!BOUND.add(kind))continue;
            NativeMenuSupport.bindNative(type,kind.position);
            // Distinct processing/filter/candidate roles must never be sorted as storage.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){return menu.getClass().getName().equals(kind.menu);}
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
                @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> c){
                    if(kind==Kind.ENCHANTING){
                        ENCHANTING.put(c.menu(),new Owner(c.player(),c.sessionId()));
                        // Opening may have produced clues before the detached session existed.
                        // Recompute through Zenith itself, using its normal seed and costs.
                        c.menu().slotsChanged(c.menu().getSlot(0).container);
                    }
                }
                @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){ENCHANTING.remove(c.menu());}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Void> c){
                    var owner=ENCHANTING.get(c.menu());if(owner==null||!owner.dirty||owner.stats==null)return;
                    // Stats and all offer clues are one committed update, after the
                    // menu's slot/cost snapshot. Never leave partial offer state.
                    var data=new FriendlyByteBuf(Unpooled.buffer());
                    try{
                        data.writeByteArray(owner.stats);data.writeByte(owner.clues.size());
                        for(var clue:owner.clues.values())data.writeByteArray(clue);
                        byte[] bytes=new byte[data.readableBytes()];data.getBytes(data.readerIndex(),bytes);
                        c.sendToClient(SYNC,bytes);owner.dirty=false;
                    }finally{data.release();}
                }
            });
            if(kind.position)DesktopServerApi.registerOpeningData(type,(player,menu,data)->data.writeBlockPos(position(menu)));
            if(kind==Kind.LIBRARY)DesktopServerApi.registerNativePayload(type,new ResourceLocation("fakerlib","button_click"),c->{
                if(c.data().length!=4){c.reject("Invalid Zenith library action");return;}
                var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{
                    int button=data.readInt(),enchantment=button&0x7FFFFFFF;
                    if(BuiltInRegistries.ENCHANTMENT.byId(enchantment)==null){c.reject("Unknown library enchantment");return;}
                    // The owning menu enforces point costs, levels and extraction limits.
                    c.menu().getClass().getMethod("onButtonClick",int.class).invoke(c.menu(),button);
                    c.broadcastChanges();
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith library action",failure);}
                finally{data.release();}
            });
        }
    }

    public static BlockPos position(AbstractContainerMenu menu){return (BlockPos)field(menu,"pos");}
    /** Only optional Zenith/FakerLib fields; no reflection into SIU or Minecraft internals. */
    public static Object field(Object owner,String name){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{
            var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
        }catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing Zenith field: "+name);
    }
    static void set(Object owner,String name,Object value){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{
            var f=type.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);return;
        }catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing Zenith field: "+name);
    }
    static AbstractContainerMenu create(Kind kind,int id,Inventory inventory,byte[] opening){
        try{
            var type=Class.forName(kind.menu);
            if(!kind.position)return (AbstractContainerMenu)type.getConstructor(int.class,Inventory.class).newInstance(id,inventory);
            if(opening.length!=8)throw new IllegalArgumentException("Invalid Zenith opening position");
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(opening));
            try{return (AbstractContainerMenu)type.getConstructor(int.class,Inventory.class,FriendlyByteBuf.class).newInstance(id,inventory,data);}
            finally{data.release();}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith menu reconstruction: "+kind.path,failure);}
    }
    /** Called by the original senders on the server thread, including native input callbacks. */
    public static boolean sendEnchanting(Player player,ResourceLocation channel,byte[] data){
        if(!(player instanceof ServerPlayer p))return false;
        // Cross-window transfers may have Inventory as their network owner.
        // The original Zenith menu generating these messages is authoritative.
        var menu=GENERATING.get();
        if(menu==null)menu=DesktopSessions.activeNetworkMenu(p).orElse(p.containerMenu);
        var owner=ENCHANTING.get(menu);
        if(owner==null||owner.player!=p)return false;
        if(channel.equals(STATS)){
            owner.stats=data.clone();var input=menu.getSlot(0).getItem();
            if(!ItemStack.isSameItemSameTags(owner.input,input)){owner.clues.clear();owner.input=input.copy();}
        }
        else if(channel.equals(CLUES)&&data.length>=2)owner.clues.put(Byte.toUnsignedInt(data[data.length-2]),data.clone());
        else return false;
        owner.dirty=true;return true;
    }
    public static void generating(AbstractContainerMenu menu,Runnable action){
        var previous=GENERATING.get();GENERATING.set(menu);
        try{action.run();}finally{if(previous==null)GENERATING.remove();else GENERATING.set(previous);}
    }
    public static boolean sendStats(Object stats,Player player){
        var data=new FriendlyByteBuf(Unpooled.buffer());
        try{
            stats.getClass().getMethod("write",FriendlyByteBuf.class).invoke(stats,data);
            byte[] bytes=new byte[data.readableBytes()];data.getBytes(data.readerIndex(),bytes);
            return sendEnchanting(player,STATS,bytes);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith enchanting stats",failure);}
        finally{data.release();}
    }
}
