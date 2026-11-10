package com.salts_inventory_update.compat.powah;

import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Publisher codecs and handlers, scoped to the authenticated owning window. */
final class PowahPackets {
    static final String PREFIX="owmii.powah.network.packet.";
    static final List<String> NAMES=List.of("NextEnergyConfigPacket","NextRedstoneModePacket","SetChannelPacket","SwitchGenModePacket","InteractWithTankPacket");
    private PowahPackets() { }
    static int index(Object message) {
        int i=NAMES.indexOf(message.getClass().getSimpleName());
        return i>=0&&message.getClass().getName().equals(PREFIX+NAMES.get(i))?i:-1;
    }
    @SuppressWarnings({"rawtypes","unchecked"})static byte[] encode(Object message,Player player) {
        int index=index(message);if(index<0)throw new IllegalArgumentException("Unknown Powah action");
        var data=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try {
            data.writeVarInt(index);((StreamCodec)message.getClass().getField("STREAM_CODEC").get(null)).encode(data,message);
            byte[] bytes=new byte[data.readableBytes()];data.readBytes(bytes);return bytes;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah action encoder",failure);}
        finally {data.release();}
    }
    @SuppressWarnings("rawtypes")static void receive(PowahWindows.Kind kind,DesktopServerPayloadContext<AbstractContainerMenu> c) {
        if(c.data().length<6||c.data().length>13){c.reject("Invalid Powah action size");return;}
        var data=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(c.data()),c.player().registryAccess());
        try {
            int id=data.readVarInt();
            if(id<0||id>=NAMES.size()){c.reject("Unknown Powah action");return;}
            int expected=id==0||id==2?12:id==4?5:8;
            if(data.readableBytes()!=expected){c.reject("Invalid Powah payload length");return;}
            Object message=((StreamCodec)Class.forName(PREFIX+NAMES.get(id)).getField("STREAM_CODEC").get(null)).decode(data);
            var tile=PowahWindows.tile(c.menu());
            if(data.isReadable()||tile.isRemoved()||tile.getLevel()!=c.player().level()||c.player().level().getBlockEntity(tile.getBlockPos())!=tile) {
                c.reject("Powah owner is no longer available");return;
            }
            if(id==4) {
                if(kind!=PowahWindows.Kind.REACTOR&&kind!=PowahWindows.Kind.MAGMATOR&&kind!=PowahWindows.Kind.THERMO
                    ||((Number)PowahWindows.invoke(message,"containerId")).intValue()!=c.menu().containerId) {
                    c.reject("Invalid Powah tank owner");return;
                }
                // The original method uses this menu's cursor. Bridge it through
                // the public shared-cursor API, preserving native item rules.
                c.menu().setCarried(c.carriedStack());
                c.menu().getClass().getMethod("interactWithTank",boolean.class).invoke(c.menu(),(Boolean)PowahWindows.invoke(message,"drain"));
                c.setCarriedStack(c.menu().getCarried());
            }else {
                if(!tile.getBlockPos().equals((BlockPos)PowahWindows.invoke(message,"pos"))){c.reject("Powah position does not match this window");return;}
                if(id==0) {
                    int face=((Number)PowahWindows.invoke(message,"mode")).intValue();
                    if(face<0||face>6||kind==PowahWindows.Kind.CABLE&&face!=((Direction)PowahWindows.invoke(c.menu(),"getSide")).get3DDataValue()) {
                        c.reject("Invalid Powah face");return;
                    }
                }
                if(id==2) {
                    int channel=((Number)PowahWindows.invoke(message,"channel")).intValue();
                    if(kind!=PowahWindows.Kind.ENDER_CELL||channel<0||channel>=((Number)PowahWindows.invoke(tile,"getMaxChannels")).intValue()) {
                        c.reject("Invalid Powah channel");return;
                    }
                }
                if(id==3&&kind!=PowahWindows.Kind.REACTOR){c.reject("Generation mode requires a reactor");return;}
                message.getClass().getMethod("handleOnServer",ServerPlayer.class).invoke(message,c.player());
            }
            c.broadcastChanges();
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah native action",failure);}
        finally {data.release();}
    }
}
