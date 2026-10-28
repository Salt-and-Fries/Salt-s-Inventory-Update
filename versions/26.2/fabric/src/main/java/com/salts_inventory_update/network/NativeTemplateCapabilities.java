package com.salts_inventory_update.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Optional, separately negotiated native-host feature; ordinary API payloads are unchanged. */
public record NativeTemplateCapabilities(long nonce, java.util.List<String> menuIds) implements CustomPacketPayload {
    public static final Type<NativeTemplateCapabilities> TYPE=new Type<>(DesktopPackets.id("native_template_capabilities"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NativeTemplateCapabilities> CODEC=StreamCodec.of(
        (buffer,payload)->{buffer.writeLong(payload.nonce());buffer.writeVarInt(payload.menuIds().size());for(var id:payload.menuIds())buffer.writeUtf(id,com.salts_inventory_update.protocol.DesktopProtocol.MAX_IDENTIFIER_LENGTH);},buffer->new NativeTemplateCapabilities(buffer.readLong(),readIds(buffer)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

    public NativeTemplateCapabilities { menuIds=java.util.List.copyOf(menuIds);com.salts_inventory_update.protocol.DesktopProtocol.requireCount("native template menus",menuIds.size(),com.salts_inventory_update.protocol.DesktopProtocol.MAX_SUPPORTED_MENU_IDS); }
    private static java.util.List<String> readIds(RegistryFriendlyByteBuf buffer) {
        int n=buffer.readVarInt();com.salts_inventory_update.protocol.DesktopProtocol.requireCount("native template menus",n,com.salts_inventory_update.protocol.DesktopProtocol.MAX_SUPPORTED_MENU_IDS);
        var ids=new java.util.ArrayList<String>();for(int i=0;i<n;i++)ids.add(buffer.readUtf(com.salts_inventory_update.protocol.DesktopProtocol.MAX_IDENTIFIER_LENGTH));return ids;
    }
}
