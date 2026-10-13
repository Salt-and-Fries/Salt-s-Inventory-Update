package com.salts_inventory_update.compat.ae2.menu;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The fork's desktop-only AEBaseMenu additions, owned by the extension.
 * Native packets keep their native codecs and handlers. This class only queues
 * them until the public Salt input context can send an authenticated action.
 */
public final class DesktopMenuState<P> {
    private UUID identity;
    private boolean toolboxCompanion;
    private final ArrayDeque<P> actions = new ArrayDeque<>();

    public UUID identity() {
        return identity;
    }

    public void identity(UUID identity) {
        this.identity = identity;
    }

    public boolean toolboxCompanion() {
        return toolboxCompanion;
    }

    public void toolboxCompanion(boolean companion) {
        toolboxCompanion = companion;
    }

    public void send(P packet, Consumer<P> nativeTransport) {
        Objects.requireNonNull(packet, "packet");
        if (identity == null) {
            nativeTransport.accept(packet);
        } else {
            if (actions.size() >= 256) {
                throw new IllegalStateException("Too many queued desktop actions");
            }
            actions.addLast(packet);
        }
    }

    public <C> void flush(Function<P, C> channel, Function<P, byte[]> encode, BiConsumer<C, byte[]> send) {
        while (!actions.isEmpty()) {
            var packet = actions.removeFirst();
            send.accept(channel.apply(packet), encode.apply(packet));
        }
    }

    public void discardActions() {
        actions.clear();
    }
}
