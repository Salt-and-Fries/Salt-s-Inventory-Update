package com.salts_inventory_update.compat.toms_storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.protocol.DesktopProtocol;

public final class TomsStoragePayloads {
    private static final int MAX_PACKET_BYTES = DesktopProtocol.MAX_TOMS_COMPRESSED_BYTES;
    private static final int MAX_UTF = DesktopProtocol.MAX_TOMS_STRING_BYTES / 3;
    private static final int MAX_ENTRIES = DesktopProtocol.MAX_TOMS_NODES;

    private TomsStoragePayloads() {
    }

    public static byte[] writeNbt(CompoundTag tag) {
        validateNbt(tag);
        try (DataOutputStream measured = new DataOutputStream(new LimitedOutputStream(DesktopProtocol.MAX_TOMS_DECOMPRESSED_BYTES))) {
            NbtIo.write(tag, measured);
        } catch (PayloadTooLargeIOException exception) {
            throw new IllegalArgumentException("Tom's Storage NBT payload is too large before compression", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to measure Tom's Storage NBT payload", exception);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            byte[] data = output.toByteArray();
            requirePacketSize(data);
            return data;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Tom's Storage NBT payload", exception);
        }
    }

    public static CompoundTag readNbt(byte[] data) {
        requirePacketSize(data);
        try {
            CompoundTag tag = NbtIo.readCompressed(
                new ByteArrayInputStream(data),
                NbtAccounter.create(DesktopProtocol.MAX_TOMS_DECOMPRESSED_BYTES)
            );
            validateNbt(tag);
            return tag;
        } catch (IOException | RuntimeException | StackOverflowError exception) {
            throw new IllegalStateException("Failed to decode Tom's Storage NBT payload", exception);
        }
    }

    public static byte[] writeTerminalAction(
        RegistryAccess registryAccess,
        String action,
        boolean modifier,
        ItemStack stack,
        long quantity
    ) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            buf.writeUtf(action, MAX_UTF);
            buf.writeBoolean(modifier);
            buf.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                ItemStack one = stack.copyWithCount(1);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, one);
                buf.writeVarLong(quantity);
            }
            return toByteArray(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static TerminalAction readTerminalAction(RegistryAccess registryAccess, byte[] data) {
        requirePacketSize(data);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        try {
            String action = buf.readUtf(MAX_UTF);
            boolean modifier = buf.readBoolean();
            boolean hasStack = buf.readBoolean();
            ItemStack stack = ItemStack.EMPTY;
            long quantity = 0L;
            if (hasStack) {
                stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                quantity = buf.readVarLong();
                if (stack.isEmpty() || quantity <= 0L) {
                    throw new IllegalArgumentException("Invalid Tom's Storage terminal action stack");
                }
            }
            requireFullyRead(buf);
            return new TerminalAction(action, modifier, stack, quantity);
        } finally {
            releaseIfOwned(buf);
        }
    }

    public static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries) {
        int limit = Math.min(entries.size(), MAX_ENTRIES);
        byte[] encoded = writeTerminalSnapshot(registryAccess, entries, limit, entries.size() > limit);
        while (encoded == null && limit > 0) {
            limit = Math.max(0, limit / 2);
            encoded = writeTerminalSnapshot(registryAccess, entries, limit, true);
        }
        if (encoded == null) {
            throw new IllegalArgumentException("Tom's Storage terminal snapshot cannot fit the packet limit");
        }
        return encoded;
    }

    private static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries, int limit, boolean truncated) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            int count = Math.min(entries.size(), limit);
            buf.writeBoolean(truncated || count < entries.size());
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                TerminalEntry entry = entries.get(i);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack().copyWithCount(1));
                buf.writeVarLong(entry.quantity());
            }
            return toByteArrayOrNull(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static TerminalSnapshot readTerminalSnapshot(RegistryAccess registryAccess, byte[] data) {
        requirePacketSize(data);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        try {
            boolean truncated = buf.readBoolean();
            int count = DesktopProtocol.requireCount("Tom's terminal entries", buf.readVarInt(), MAX_ENTRIES);
            List<TerminalEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                long quantity = buf.readVarLong();
                if (stack.isEmpty() || quantity <= 0L) {
                    throw new IllegalArgumentException("Invalid Tom's Storage terminal snapshot entry");
                }
                entries.add(new TerminalEntry(stack, quantity));
            }
            requireFullyRead(buf);
            return new TerminalSnapshot(entries, truncated);
        } finally {
            releaseIfOwned(buf);
        }
    }

    public static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected) {
        int limit = Math.min(channels.size(), MAX_ENTRIES);
        byte[] encoded = writeLinkSnapshot(registryAccess, channels, selected, limit);
        while (encoded == null && limit > 0) {
            limit /= 2;
            encoded = writeLinkSnapshot(registryAccess, channels, selected, limit);
        }
        if (encoded == null) {
            throw new IllegalArgumentException("Tom's Storage link snapshot cannot fit the packet limit");
        }
        return encoded;
    }

    private static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected, int limit) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            buf.writeBoolean(selected != null);
            if (selected != null) {
                buf.writeUUID(selected);
            }
            int count = Math.min(channels.size(), limit);
            buf.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                LinkChannel channel = channels.get(index);
                buf.writeUUID(channel.id());
                buf.writeUtf(channel.name(), MAX_UTF);
                buf.writeBoolean(channel.publicChannel());
                buf.writeBoolean(channel.ownerName() != null);
                if (channel.ownerName() != null) {
                    buf.writeUtf(channel.ownerName(), MAX_UTF);
                }
            }
            return toByteArrayOrNull(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static LinkSnapshot readLinkSnapshot(RegistryAccess registryAccess, byte[] data) {
        requirePacketSize(data);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        try {
            UUID selected = buf.readBoolean() ? buf.readUUID() : null;
            int count = DesktopProtocol.requireCount("Tom's link channels", buf.readVarInt(), MAX_ENTRIES);
            List<LinkChannel> channels = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID id = buf.readUUID();
                String name = buf.readUtf(MAX_UTF);
                boolean publicChannel = buf.readBoolean();
                String ownerName = buf.readBoolean() ? buf.readUtf(MAX_UTF) : null;
                channels.add(new LinkChannel(id, name, publicChannel, ownerName));
            }
            requireFullyRead(buf);
            return new LinkSnapshot(channels, selected);
        } finally {
            releaseIfOwned(buf);
        }
    }

    public static byte[] writeTagSnapshot(List<String> tags) {
        int limit = Math.min(tags.size(), MAX_ENTRIES);
        byte[] encoded = writeTagSnapshot(tags, limit);
        while (encoded == null && limit > 0) {
            limit /= 2;
            encoded = writeTagSnapshot(tags, limit);
        }
        if (encoded == null) {
            throw new IllegalArgumentException("Tom's Storage tag snapshot cannot fit the packet limit");
        }
        return encoded;
    }

    private static byte[] writeTagSnapshot(List<String> tags, int limit) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            int count = Math.min(tags.size(), limit);
            buf.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                buf.writeUtf(tags.get(index), MAX_UTF);
            }
            return toByteArrayOrNull(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static List<String> readTagSnapshot(byte[] data) {
        requirePacketSize(data);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), RegistryAccess.EMPTY);
        try {
            int count = DesktopProtocol.requireCount("Tom's tags", buf.readVarInt(), MAX_ENTRIES);
            List<String> tags = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                tags.add(buf.readUtf(MAX_UTF));
            }
            requireFullyRead(buf);
            return List.copyOf(tags);
        } finally {
            releaseIfOwned(buf);
        }
    }

    private static byte[] toByteArray(RegistryFriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > MAX_PACKET_BYTES) {
                throw new IllegalArgumentException("Tom's Storage payload is too large: " + buf.readableBytes());
            }
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static byte[] toByteArrayOrNull(RegistryFriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > MAX_PACKET_BYTES) {
                return null;
            }
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void requirePacketSize(byte[] data) {
        if (data == null || data.length > MAX_PACKET_BYTES) {
            throw new IllegalArgumentException("Tom's Storage payload is too large: " + (data == null ? -1 : data.length));
        }
    }

    private static void requireFullyRead(RegistryFriendlyByteBuf buf) {
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Tom's Storage payload has trailing data");
        }
    }

    private static void validateNbt(CompoundTag tag) {
        int[] remainingNodes = {DesktopProtocol.MAX_TOMS_NODES};
        validateNbt(tag, 0, remainingNodes);
    }

    private static void validateNbt(Tag tag, int depth, int[] remainingNodes) {
        if (depth > DesktopProtocol.MAX_TOMS_DEPTH || --remainingNodes[0] < 0) {
            throw new IllegalArgumentException("Tom's Storage NBT exceeds structural limits");
        }
        if (tag instanceof CompoundTag compound) {
            if (compound.size() > DesktopProtocol.MAX_TOMS_NODES) {
                throw new IllegalArgumentException("Tom's Storage NBT compound is too large: " + compound.size());
            }
            for (String key : compound.keySet()) {
                if (key.getBytes(StandardCharsets.UTF_8).length > DesktopProtocol.MAX_TOMS_STRING_BYTES) {
                    throw new IllegalArgumentException("Tom's Storage NBT key is too long");
                }
                Tag child = compound.get(key);
                if (child != null) {
                    validateNbt(child, depth + 1, remainingNodes);
                }
            }
        } else if (tag instanceof ListTag list) {
            if (list.size() > DesktopProtocol.MAX_TOMS_NODES) {
                throw new IllegalArgumentException("Tom's Storage NBT list is too large: " + list.size());
            }
            for (Tag child : list) {
                validateNbt(child, depth + 1, remainingNodes);
            }
        } else if (tag instanceof StringTag string
            && string.value().getBytes(StandardCharsets.UTF_8).length > DesktopProtocol.MAX_TOMS_STRING_BYTES) {
            throw new IllegalArgumentException("Tom's Storage NBT string is too long");
        } else if (tag instanceof ByteArrayTag bytes
            && bytes.getAsByteArray().length > DesktopProtocol.MAX_TOMS_PRIMITIVE_ARRAY_BYTES
            || tag instanceof IntArrayTag ints
            && ints.getAsIntArray().length > DesktopProtocol.MAX_TOMS_PRIMITIVE_ARRAY_BYTES / Integer.BYTES
            || tag instanceof LongArrayTag longs
            && longs.getAsLongArray().length > DesktopProtocol.MAX_TOMS_PRIMITIVE_ARRAY_BYTES / Long.BYTES) {
            throw new IllegalArgumentException("Tom's Storage NBT array is too large");
        }
    }

    private static void releaseIfOwned(RegistryFriendlyByteBuf buf) {
        if (buf.refCnt() > 0) {
            buf.release();
        }
    }

    private static final class PayloadTooLargeIOException extends IOException {
        private PayloadTooLargeIOException() {
            super("payload limit exceeded");
        }
    }

    private static final class LimitedOutputStream extends OutputStream {
        private final int maximum;
        private int written;

        private LimitedOutputStream(int maximum) {
            this.maximum = maximum;
        }

        @Override
        public void write(int value) throws IOException {
            this.reserve(1);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            if (data == null) {
                throw new NullPointerException("data");
            }
            if (offset < 0 || length < 0 || offset > data.length - length) {
                throw new IndexOutOfBoundsException();
            }
            this.reserve(length);
        }

        private void reserve(int length) throws PayloadTooLargeIOException {
            if (length > this.maximum - this.written) {
                throw new PayloadTooLargeIOException();
            }
            this.written += length;
        }
    }

    public record TerminalEntry(ItemStack stack, long quantity) {
        public TerminalEntry {
            if (stack == null || stack.isEmpty() || quantity <= 0L) {
                throw new IllegalArgumentException("Invalid Tom's Storage terminal entry");
            }
            stack = stack.copyWithCount(1);
        }
    }

    public record TerminalSnapshot(List<TerminalEntry> entries, boolean truncated) {
        public TerminalSnapshot {
            entries = List.copyOf(entries);
        }
    }

    public record TerminalAction(String action, boolean modifier, ItemStack stack, long quantity) {
        public TerminalAction {
            if (action == null || action.length() > MAX_UTF || quantity < 0L || !stack.isEmpty() && quantity == 0L) {
                throw new IllegalArgumentException("Invalid Tom's Storage terminal action");
            }
            stack = stack.copy();
        }
    }

    public record LinkChannel(UUID id, String name, boolean publicChannel, String ownerName) {
        public LinkChannel {
            if (id == null || name == null || name.length() > MAX_UTF || ownerName != null && ownerName.length() > MAX_UTF) {
                throw new IllegalArgumentException("Invalid Tom's Storage link channel");
            }
        }
    }

    public record LinkSnapshot(List<LinkChannel> channels, UUID selected) {
        public LinkSnapshot {
            channels = List.copyOf(channels);
        }
    }
}
