package com.salts_inventory_update.compat.chipped;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Source metadata captured at Chipped's own opener, never from Salt internals. */
public final class ChippedSources {
    private record Opening(ServerPlayer player,BlockPos pos) { }
    private static final ThreadLocal<Opening> OPENING=new ThreadLocal<>();
    private static final Map<AbstractContainerMenu,Source> SOURCES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object,Source> PROVIDERS=Collections.synchronizedMap(new WeakHashMap<>());
    public record Source(ServerPlayer player,net.minecraft.world.level.Level world,BlockPos pos,BlockState main,BlockPos side,BlockState sideState) {
        public boolean valid(ServerPlayer user) {
            return player==user&&user.level()==world
                &&user.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64
                &&user.level().getBlockState(pos).equals(main)&&user.level().getBlockState(side).equals(sideState);
        }
    }
    private ChippedSources() { }
    public static void begin(ServerPlayer player,BlockPos pos) {
        OPENING.remove();
        // Recording native metadata does not enable capture or change the native menu.
        // Readiness can temporarily be false during a desktop's native opening probe.
        OPENING.set(new Opening(player,pos.immutable()));
    }
    public static void end() { OPENING.remove(); }
    private static Source capture() {
        var opening=OPENING.get();if(opening==null)return null;
        var main=opening.player.level().getBlockState(opening.pos);
        if(!main.hasProperty(HorizontalDirectionalBlock.FACING))return null;
        var side=opening.pos.relative(main.getValue(HorizontalDirectionalBlock.FACING).getClockWise());
        // Chipped places SIDE clockwise from the MAIN block's facing.
        var other=opening.player.level().getBlockState(side);
        if(other.getBlock()==main.getBlock()&&sideModel(other)
            &&other.getValue(HorizontalDirectionalBlock.FACING)==main.getValue(HorizontalDirectionalBlock.FACING))
            return new Source(opening.player,opening.player.level(),opening.pos,main,side,other);
        return null;
    }
    public static void attach(AbstractContainerMenu menu) {
        if(!menu.getClass().getName().equals(ChippedNative.MENU))return;
        var source=capture();if(source!=null)SOURCES.put(menu,source);
    }
    /** The original provider may construct its menu after the interaction has returned. */
    public static void provider(Object provider) { var source=capture();if(source!=null)PROVIDERS.put(provider,source); }
    public static void attachProvider(Object provider,AbstractContainerMenu menu) {
        var source=PROVIDERS.get(provider);
        if(source!=null&&menu!=null&&menu.getClass().getName().equals(ChippedNative.MENU))SOURCES.put(menu,source);
    }
    private static boolean sideModel(BlockState state) {
        return state.getProperties().stream().filter(p->p.getName().equals("model"))
            .anyMatch(p->value(state,p).equals("side"));
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static String value(BlockState state,net.minecraft.world.level.block.state.properties.Property property){return state.getValue(property).toString();}
    public static Source get(AbstractContainerMenu menu) {
        var source=SOURCES.get(menu);
        if(source==null){attach(menu);source=SOURCES.get(menu);}
        return source;
    }
}
