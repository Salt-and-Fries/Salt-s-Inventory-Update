package com.salts_inventory_update.compat.adastra;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Original menu factories, permissions and packets; only presentation changes. */
public final class AdAstraWindows {
    private static final Set<String> BOUND=new HashSet<>();
    private AdAstraWindows() { }
    /** Native block-addressed packets may act only on that player's live menu. */
    public static Optional<BlockEntity> machine(BlockPos pos,Player player,Level level){
        if(!(player instanceof ServerPlayer)||player.level()!=level||player.distanceToSqr(pos.getCenter())>64)return Optional.empty();
        var result=new ArrayList<BlockEntity>();
        DesktopSessions.hasOpenSessionMatching(player,menu->{
            String name=menu.getClass().getName();
            if(!name.startsWith("earth.terrarium.adastra.")&&!name.startsWith("ad_astra_giselle_addon."))return false;
            String id=BuiltInRegistries.MENU.getKey(menu.getType()).toString();
            if(AdAstraSpec.all().stream().noneMatch(s->s.menu.equals(id)&&!s.vehicle))return false;
            try{
                Object owner=menu.getClass().getMethod("getEntity").invoke(menu);
                if(owner instanceof BlockEntity be&&!be.isRemoved()&&be.getLevel()==level&&be.getBlockPos().equals(pos)
                    &&level.getBlockEntity(pos)==be&&menu.stillValid(player))result.add(be);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Ad Astra menu owner",error);}
            return false;
        });
        return result.stream().findFirst();
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    static synchronized void bind(){
        for(var spec:AdAstraSpec.all()){
            var type=NativeMenuSupport.type(spec.menu);
            if(type==null||!BOUND.add(spec.menu))continue;
            NativeMenuSupport.bindNative(type,true);
            // Battery, recipe and output cells never opt into generic sorting.
            // Vehicle refueling/return ports retain their native restrictions.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->{
                int index=menu.slots.indexOf(slot),cargo=spec.menu.endsWith("rover_menu")?16:8;
                return spec.vehicle&&!spec.menu.endsWith("lander_menu")&&!(slot.container instanceof Inventory)
                    &&index>=0&&index<cargo;
            });
        }
        var planets=NativeMenuSupport.type("ad_astra:planets_menu");
        if(planets!=null&&BOUND.add("native-planets"))DesktopServerApi.registerWindow(planets,
            new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){return false;}
            });
    }
}
