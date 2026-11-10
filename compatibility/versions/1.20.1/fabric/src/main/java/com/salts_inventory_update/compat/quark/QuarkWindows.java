package com.salts_inventory_update.compat.quark;

import java.nio.ByteBuffer;
import java.util.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;

/** Quark menus retain their original server inventory, recipes and puzzle operations. */
public final class QuarkWindows {
    public static MenuType<QuarkBackpackMenu> BACKPACK;
    public static final ResourceLocation ACTION=id("quark/action");
    private static final Set<String> BOUND=new HashSet<>();
    private QuarkWindows() { }
    public static ResourceLocation id(String path) { return new ResourceLocation("salts_inventory_compat",path); }
    public static void initializeCommon() {
        if(!DesktopEnvironment.isModLoaded("forge"))return;
        NativeMenuRegistration.register("quark_backpack",()->new MenuType<>((i,inv)->new QuarkBackpackMenu(i,inv,new UUID(0,0)),FeatureFlags.DEFAULT_FLAGS),type->{
            BACKPACK=type;
            DesktopMenuBindings.register(type,m->m.slots.size(),true,(p,m,data)->"quark-backpack:"+p.getUUID());
            DesktopMenuBindings.registerStorage(type,(m,s)->true);
            DesktopServerApi.registerOpeningData(type,(p,m,b)->b.writeUUID(m.ownerWindow));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<QuarkBackpackMenu,Void>() { });
        });
        try { Class.forName("com.salts_inventory_update.compat.quark.forge.QuarkForgeTransport").getMethod("initialize").invoke(null); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        bind();DesktopEnvironment.onServerStarting(server->bind());
    }
    private static synchronized void bind() {
        for(String path:List.of("crate","crafter","feeding_trough","held_shulker_box","matrix_enchanting")) {
            var type=NativeMenuSupport.type("quark:"+path);if(type==null||!BOUND.add(path))continue;
            NativeMenuSupport.bindNative(type,Set.of("crate","held_shulker_box","matrix_enchanting","feeding_trough").contains(path));
            if(path.equals("feeding_trough"))DesktopServerApi.registerOpeningData(type,(p,m,b)->b.writeByte(1));
            boolean storage=Set.of("crate","feeding_trough","held_shulker_box").contains(path);
            DesktopMenuBindings.registerStorage(type,(m,s)->storage&&!(s.container instanceof net.minecraft.world.entity.player.Inventory));
            if(path.equals("held_shulker_box"))com.salts_inventory_update.api.desktop.DesktopItemSources.register(type,(p,m)->{
                int slot=(Integer)QuarkAccess.field(m,"blockedSlot");if(slot<0||slot>=p.getInventory().getContainerSize())return null;
                var source=p.getInventory().getItem(slot);var identity=source.copy();identity.removeTagKey("BlockEntityTag");
                return new com.salts_inventory_update.api.desktop.DesktopItemSources.Source(slot,"quark-held-shulker:"+slot,source,s->{
                    var candidate=s.copy();candidate.removeTagKey("BlockEntityTag");return net.minecraft.world.item.ItemStack.isSameItemSameTags(identity,candidate);
                });
            });
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu m) { return true; }
            });
            if(path.equals("crate")||path.equals("matrix_enchanting"))DesktopServerApi.registerNativePayload(type,ACTION,c->{
                if(!c.menu().stillValid(c.player()))return;
                var b=ByteBuffer.wrap(c.data());
                if(path.equals("crate")) {
                    if(b.remaining()!=1||c.data()[0]<0||c.data()[0]>1)return;
                    QuarkAccess.call(c.menu(),"scroll",c.data()[0]==1,false);
                } else {
                    if(b.remaining()!=16)return;int op=b.getInt(),a=b.getInt(),x=b.getInt(),y=b.getInt();
                    if(op<0||op>4)return;
                    Object entity=QuarkAccess.field(c.menu(),"enchanter"),matrix=QuarkAccess.field(entity,"matrix");
                    if(matrix==null)return;
                    var pieces=(Map<?,?>)QuarkAccess.field(matrix,"pieces");
                    if(op!=0&&!pieces.containsKey(a))return;
                    if(op==1&&(x<0||x>=5||y<0||y>=5)||op==4&&!pieces.containsKey(x))return;
                    QuarkAccess.call(entity,"onOperation",c.player(),op,a,x,y);
                }
                c.broadcastChanges();
            });
        }
    }
    public static void openBackpack(ServerPlayer p,UUID owner) {
        if(BACKPACK==null||!DesktopSessions.shouldCapture(p)||!p.isAlive()||p.isSpectator()||!QuarkAccess.wearing(p)
            ||DesktopSessions.hasOpenSessionMatching(p,m->m instanceof QuarkBackpackMenu))return;
        DesktopSessions.openMenuSession(p,new SimpleMenuProvider((id,inv,player)->new QuarkBackpackMenu(id,inv,owner),Component.literal("Backpack")),null);
    }
    public static void initializeClient() { if(BACKPACK!=null)QuarkClient.initialize(); }
}
