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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/** Powah 5.x retains its real menus, item rules, live tiles and native actions. */
public final class PowahWindows {
    public enum Kind {
        REACTOR("reactor", "ReactorContainer", "reactor", 190, 75),
        CABLE("cable", "CableContainer", "cable", 174, 29),
        ENDER_CELL("ender_cell", "EnderCellContainer", "ender_cell", 190, 73),
        ENERGY_CELL("energy_cell", "EnergyCellContainer", "energy_cell", 190, 50),
        DISCHARGER("discharger", "DischargerContainer", "discharger", 204, 75),
        ENERGY_HOPPER("energy_hopper", "EnergyHopperContainer", "wide_energy", 190, 50),
        FURNATOR("furnator", "FurnatorContainer", "furnator", 190, 75),
        MAGMATOR("magmator", "MagmatorContainer", "magmator", 190, 75),
        PLAYER_TRANSMITTER("player_transmitter", "PlayerTransmitterContainer", "player_transmitter", 203, 50),
        SOLAR("solar", "SolarContainer", "wide_energy", 190, 50),
        THERMO("thermo", "ThermoContainer", "thermo", 200, 75);

        public final String path, menu, art;
        public final int width, bodyHeight;
        Kind(String path, String menu, String art, int width, int bodyHeight) {
            this.path=path; this.menu="owmii.powah.inventory."+menu; this.art=art;
            this.width=width; this.bodyHeight=bodyHeight;
        }
        public int bodyWidth() { return this==CABLE?153:176; }
    }
    private static final Set<Kind> BOUND=EnumSet.noneOf(Kind.class);
    private static final String[] PACKETS={"NextEnergyConfigPacket", "NextRedstoneModePacket", "SetChannelPacket", "SwitchGenModePacket"};
    static final ResourceLocation ACTION=new ResourceLocation("powah", "packet");
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
            DesktopServerApi.registerNativePayload(type,ACTION,c->{
                byte[] bytes=c.data();
                if(bytes.length<9||bytes.length>13){c.reject("Invalid Powah action length");return;}
                var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
                try {
                    int packet=data.readVarInt(),value=-1;
                    if(packet<0||packet>=PACKETS.length) {c.reject("Unknown Powah action");return;}
                    int payloadLength=(packet==0||packet==2)?12:8;
                    if(data.readableBytes()!=payloadLength) {c.reject("Invalid Powah action payload");return;}
                    BlockPos pos;
                    if(packet==0){value=data.readInt();pos=data.readBlockPos();}
                    else if(packet==1||packet==3)pos=data.readBlockPos();
                    else if(packet==2){pos=data.readBlockPos();value=data.readInt();}
                    else {c.reject("Unknown Powah action");return;}
                    var owner=tile(c.menu());
                    if(data.isReadable()||owner.isRemoved()||owner.getLevel()!=c.player().level()
                        ||c.player().level().getBlockEntity(pos)!=owner||!owner.getBlockPos().equals(pos)) {
                        c.reject("Powah action does not belong to this window");return;
                    }
                    if(packet==0&&(value<0||value>6||kind==Kind.CABLE&&value!=((Direction)invoke(c.menu(),"getSide")).get3DDataValue())) {
                        c.reject("Invalid Powah face");return;
                    }
                    if(packet==2&&(kind!=Kind.ENDER_CELL||value<0||value>=((Number)invoke(owner,"getMaxChannels")).intValue())) {
                        c.reject("Invalid Powah channel");return;
                    }
                    if(packet==3&&kind!=Kind.REACTOR){c.reject("Generation mode requires a reactor");return;}
                    data.readerIndex(0);data.readVarInt();
                    Object message=Class.forName("owmii.powah.network.packet."+PACKETS[packet])
                        .getConstructor(FriendlyByteBuf.class).newInstance(data);
                    message.getClass().getMethod("handle",Player.class).invoke(message,c.player());
                    c.broadcastChanges();
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah native action",failure);}
                finally {data.release();}
            });
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
        try {return (BlockEntity)menu.getClass().getField("te").get(menu);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah tile owner",failure);}
    }
    static Object invoke(Object target,String method) {
        try {return target.getClass().getMethod(method).invoke(target);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah native state: "+method,failure);}
    }
}
