package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;

/** Negotiated automatic-window envelope. Ordinary/API packet bytes stay unchanged. */
public final class AutomaticCountCodec {
    private static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();
    private static final class State {
        final List<Integer> written = new ArrayList<>();
        final int[] restoring;
        int cursor;
        State(int[] restoring) { this.restoring = restoring; }
    }
    public record Encoded(byte[] body, int[] counts, boolean wide) { }
    private AutomaticCountCodec() { }
    public static void write(FriendlyByteBuf buffer, ItemStack stack) {
        State state = ACTIVE.get();
        if (state != null && state.restoring == null) state.written.add(stack.getCount());
        buffer.writeItem(state != null && state.restoring == null && stack.getCount() > 127 ? stack.copyWithCount(1) : stack);
    }
    public static ItemStack read(FriendlyByteBuf buffer) {
        ItemStack stack = buffer.readItem(); State state = ACTIVE.get();
        if (state != null && state.restoring != null) {
            if (state.cursor >= state.restoring.length) throw new IllegalArgumentException("Missing native stack count");
            int count = state.restoring[state.cursor++];
            if (count < 0 || stack.isEmpty() && count != 0 || !stack.isEmpty() && count == 0)
                throw new IllegalArgumentException("Invalid native stack count");
            if (!stack.isEmpty()) stack.setCount(count);
        }
        return stack;
    }
    public static Encoded encode(DesktopPacket packet) {
        State previous = ACTIVE.get(), state = new State(null); ACTIVE.set(state);
        try {
            FriendlyByteBuf buffer = DesktopPackets.toBuffer(packet);
            try {
                byte[] data = new byte[buffer.readableBytes()]; buffer.readBytes(data);
                int[] counts = state.written.stream().mapToInt(Integer::intValue).toArray();
                return new Encoded(data, counts, state.written.stream().anyMatch(c -> c > 127));
            } finally { buffer.release(); }
        } finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static <T> T restore(int[] counts, Supplier<T> decoderAndDispatch) {
        State previous = ACTIVE.get(), state = new State(counts); ACTIVE.set(state);
        try {
            T result = decoderAndDispatch.get();
            if (state.cursor != counts.length) throw new IllegalArgumentException("Trailing native stack counts");
            return result;
        } finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static void requireComplete() {
        State state = ACTIVE.get();
        if (state != null && state.restoring != null && state.cursor != state.restoring.length)
            throw new IllegalArgumentException("Trailing native stack counts");
    }
}
