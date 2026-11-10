"""Preserve optional Fabric codec context for Tom's Storage client actions."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = '''package com.salts_inventory_update.compat.toms_storage.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Supplies the real connection to optional native item-codec hooks off the wire. */
final class TerminalPacketContext {
    private static final Method SUPPLY = findSupply();
    private TerminalPacketContext() { }

    private static Method findSupply() {
        try {
            Class<?> context = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContext");
            Class<?> provider = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider");
            return context.getMethod("supplyWithContext", provider, Supplier.class);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Unsupported native packet context API", incompatible);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T supply(Object connection, Supplier<T> encoder) {
        if (SUPPLY == null) return encoder.get();
        if (!SUPPLY.getParameterTypes()[0].isInstance(connection))
            throw new IllegalStateException("Native packet context is missing from the client connection");
        try {
            return (T) SUPPLY.invoke(null, connection, encoder);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Native item serialization failed", cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native packet context invocation failed", failure);
        }
    }
}
'''

def main():
    for version in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
        folder = ROOT / 'compatibility/versions' / version / 'fabric/src/main/java/com/salts_inventory_update/compat/toms_storage/client'
        (folder / 'TerminalPacketContext.java').write_text(SOURCE)
        path = folder / 'TomsStorageDesktopWindows.java'
        text = path.read_text()
        registry = 'RegistryAccess.EMPTY' if version == '1.20.1' else 'context.minecraft().player.registryAccess()'
        old = f'TomsStoragePayloads.writeTerminalAction({registry}, action, modifier, stack, quantity)'
        new = f'TerminalPacketContext.supply(context.minecraft().getConnection(), () -> {old})'
        if new not in text:
            assert text.count(old) == 1, version
            path.write_text(text.replace(old, new))
        print(version)

if __name__ == '__main__': main()
