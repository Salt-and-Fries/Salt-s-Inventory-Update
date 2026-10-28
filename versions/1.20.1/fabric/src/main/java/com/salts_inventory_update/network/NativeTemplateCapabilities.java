package com.salts_inventory_update.network;

import java.util.List;
import java.util.ArrayList;
import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import com.salts_inventory_update.protocol.DesktopProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Separate optional channel: older automatic-host peers keep their original wire encoding. */
public record NativeTemplateCapabilities(long nonce,List<String> menuIds) implements DesktopPacket {
    public static final ResourceLocation TYPE=DesktopPackets.id("native_template_capabilities");
    public NativeTemplateCapabilities {menuIds=List.copyOf(menuIds);DesktopProtocol.requireCount("native template menus",menuIds.size(),DesktopProtocol.MAX_SUPPORTED_MENU_IDS);}
    public NativeTemplateCapabilities(FriendlyByteBuf buffer){this(buffer.readLong(),read(buffer));}
    private static List<String> read(FriendlyByteBuf buffer){int n=buffer.readVarInt();DesktopProtocol.requireCount("native template menus",n,DesktopProtocol.MAX_SUPPORTED_MENU_IDS);var ids=new ArrayList<String>();for(int i=0;i<n;i++)ids.add(buffer.readUtf(DesktopProtocol.MAX_IDENTIFIER_LENGTH));return ids;}
    @Override public ResourceLocation id(){return TYPE;}
    @Override public void write(FriendlyByteBuf buffer){buffer.writeLong(nonce);buffer.writeVarInt(menuIds.size());for(var id:menuIds)buffer.writeUtf(id,DesktopProtocol.MAX_IDENTIFIER_LENGTH);}
}
