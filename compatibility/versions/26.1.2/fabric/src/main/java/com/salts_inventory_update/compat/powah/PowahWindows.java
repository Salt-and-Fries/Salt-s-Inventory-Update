package com.salts_inventory_update.compat.powah;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/** Powah 5.x retains its real menus, item rules, live tiles and native actions. */
public final class PowahWindows {
    public enum Kind {
        REACTOR("reactor", "ReactorMenu", "reactor", 190, 75),
        CABLE("cable", "CableMenu", "cable", 174, 29),
        ENDER_CELL("ender_cell", "EnderCellMenu", "ender_cell", 190, 73),
        ENERGY_CELL("energy_cell", "EnergyCellMenu", "energy_cell", 190, 50),
        DISCHARGER("discharger", "DischargerMenu", "discharger", 204, 75),
        ENERGY_HOPPER("energy_hopper", "EnergyHopperMenu", "wide_energy", 190, 50),
        FURNATOR("furnator", "FurnatorMenu", "furnator", 190, 75),
        MAGMATOR("magmator", "MagmatorMenu", "magmator", 190, 75),
        PLAYER_TRANSMITTER("player_transmitter", "PlayerTransmitterMenu", "player_transmitter", 203, 50),
        SOLAR("solar", "SolarMenu", "wide_energy", 190, 50),
        THERMO("thermo", "ThermoMenu", "thermo", 200, 75);

        public final String path, menu, art;
        public final int width, bodyHeight;
        Kind(String path, String menu, String art, int width, int bodyHeight) {
            this.path=path; this.menu="owmii.powah.inventory."+menu; this.art=art;
            this.width=width; this.bodyHeight=bodyHeight;
        }
        public int bodyWidth() { return this==CABLE?153:176; }
    }
    private static final Set<Kind> BOUND=EnumSet.noneOf(Kind.class);
    static final Identifier ACTION=Identifier.fromNamespaceAndPath("salts_inventory_compat", "powah/native_action");
    private PowahWindows() { }

    public static void initializeCommon() { bind(); DesktopEnvironment.onServerStarting(server->bind()); }

    static synchronized void bind() {
        for(var kind:Kind.values()) {
            var type=NativeMenuSupport.type("powah:"+kind.path);
            if(type==null||!BOUND.add(kind))continue;
            NativeMenuSupport.support(type,kind.menu,true);
            // These are machine inputs/outputs and card slots, not general storage.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            DesktopServerApi.registerOpeningData(type,(player,menu,data)->{
                data.writeBlockPos(tile(menu).getBlockPos());
                if(kind==Kind.CABLE)data.writeInt(((Direction)invoke(menu,"getSide")).get3DDataValue());
            });
            DesktopServerApi.registerNativePayload(type,ACTION,c->PowahPackets.receive(kind,c));
        }
    }

    static AbstractContainerMenu create(Kind kind,int id,Inventory inventory,byte[] bytes) {
        if(bytes.length!=(kind==Kind.CABLE?12:8))throw new IllegalArgumentException("Invalid Powah opening data");
        var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            var menu=(AbstractContainerMenu)Class.forName(kind.menu)
                .getConstructor(int.class,Inventory.class,FriendlyByteBuf.class).newInstance(id,inventory,data);
            if(data.isReadable())throw new IllegalArgumentException("Trailing Powah opening data");
            return menu;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah menu construction",failure);}
        finally {data.release();}
    }

    /** The publisher only writes this face to its vanilla opening buffer.
     * Retain it on the server menu so SIU can reconstruct the same clicked face. */
    public static void rememberCableFace(Object menu,BlockHitResult hit) {
        if(!(menu instanceof AbstractContainerMenu m)||!m.getClass().getName().equals(Kind.CABLE.menu))return;
        try {
            @SuppressWarnings("unchecked") var side=(Optional<Direction>)Class.forName("owmii.powah.block.cable.CableBlock")
                .getMethod("getHitSide",net.minecraft.world.phys.Vec3.class,BlockPos.class).invoke(null,hit.getLocation(),hit.getBlockPos());
            if(side.isPresent()) {var field=m.getClass().getDeclaredField("side");field.setAccessible(true);field.set(m,side.get());}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah cable opening face",failure);}
    }

    public static BlockEntity tile(AbstractContainerMenu menu) {
        try {return (BlockEntity)menu.getClass().getField("blockEntity").get(menu);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah tile owner",failure);}
    }
    static Object invoke(Object target,String method) {
        try {return target.getClass().getMethod(method).invoke(target);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah native state: "+method,failure);}
    }
}
