"""Preserve the installed Fabric serialization context for detached native menus.

No new shipping API or required loader dependency: older Fabric and other
loaders execute the same writer directly when the optional context API is absent.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE='''package com.salts_inventory_update.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Carries the real connection context into native codecs serialized off the wire. */
final class NativePacketContext {
    private static final Method SUPPLY=findSupply();
    private NativePacketContext() { }

    private static Method findSupply() {
        try {
            Class<?> context=Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContext");
            Class<?> provider=Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider");
            return context.getMethod("supplyWithContext",provider,Supplier.class);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Unsupported native packet context API",incompatible);
        }
    }

    static void run(ServerPlayer player,Runnable action) {
        supply(player,()->{action.run();return null;});
    }

    @SuppressWarnings("unchecked")
    static <T> T supply(ServerPlayer player,Supplier<T> action) {
        if(SUPPLY==null)return action.get();
        if(!SUPPLY.getParameterTypes()[0].isInstance(player.connection))
            throw new IllegalStateException("Native packet context is missing from the player connection");
        try {
            return (T)SUPPLY.invoke(null,player.connection,action);
        } catch (InvocationTargetException failure) {
            Throwable cause=failure.getCause();
            if(cause instanceof RuntimeException runtime)throw runtime;
            if(cause instanceof Error error)throw error;
            throw new IllegalStateException("Native packet serialization failed",cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native packet context invocation failed",failure);
        }
    }
}
'''

for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    # The helper only names a vanilla player type, so its source works across
    # mappings and remains optional on Forge/NeoForge and older Fabric APIs.
    folder=ROOT/'versions'/version/'fabric/src/main/java/com/salts_inventory_update/server'
    (folder/'NativePacketContext.java').write_text(SOURCE)
    path=folder/'DesktopContainerSessions.java';source=path.read_text()
    old='openingDataWriter.accept(buffer);'
    new='NativePacketContext.run(player,()->openingDataWriter.accept(buffer));'
    if version=='1.20.1':
        corrected='Consumer<FriendlyByteBuf> nativeOpeningWriter=openingDataWriter;\n                    NativePacketContext.run(player,()->nativeOpeningWriter.accept(buffer));'
        source=source.replace(new,corrected)
        new=corrected
    if old in source:source=source.replace(old,new)
    elif new not in source:raise ValueError(f'Missing opening writer in {version}')
    receiver='handler' if version=='1.20.1' else 'this.serverHandler'
    old=receiver+'.tick(new ServerSessionContext(player, sessions, this));'
    new='NativePacketContext.run(player,()->'+receiver+'.tick(new ServerSessionContext(player, sessions, this)));'
    if old in source:source=source.replace(old,new)
    elif new not in source:raise ValueError(f'Missing native tick in {version}')
    if version!='1.20.1':
        old='MerchantOffers.STREAM_CODEC.encode(buffer, offers);'
        new='NativePacketContext.run(player,()->'+old[:-1]+');'
        if old in source and new not in source:source=source.replace(old,new)
        elif new not in source:raise ValueError(f'Missing merchant encoder in {version}')
    path.write_text(source)
    print(version,'native opening writer and tick context')
