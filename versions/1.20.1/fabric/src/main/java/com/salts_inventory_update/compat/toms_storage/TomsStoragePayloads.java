package com.salts_inventory_update.compat.toms_storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class TomsStoragePayloads {
    private static final int MAX_PACKET_BYTES = DesktopProtocol.MAX_TOMS_COMPRESSED_BYTES;
    private static final int MAX_COMPRESSED_NBT_BYTES = DesktopProtocol.MAX_TOMS_COMPRESSED_BYTES;
    private static final int MAX_DECOMPRESSED_NBT_BYTES = DesktopProtocol.MAX_TOMS_DECOMPRESSED_BYTES;
    private static final int MAX_NBT_ACCOUNTED_BYTES = DesktopProtocol.MAX_TOMS_DECOMPRESSED_BYTES;
    private static final int MAX_NBT_DEPTH = DesktopProtocol.MAX_TOMS_DEPTH;
    private static final int MAX_NBT_NODES = DesktopProtocol.MAX_TOMS_NODES;
    private static final int MAX_NBT_COLLECTION_SIZE = 8192;
    private static final int MAX_NBT_STRING_BYTES = DesktopProtocol.MAX_TOMS_STRING_BYTES;
    private static final int MAX_NBT_ARRAY_BYTES = DesktopProtocol.MAX_TOMS_PRIMITIVE_ARRAY_BYTES;
    private static final int MAX_ACTION_LENGTH = 64;
    private static final int MAX_NAME_LENGTH = 256;
    private static final int MAX_TAG_LENGTH = 256;
    private static final int MAX_TERMINAL_ENTRIES = 2048;
    private static final int MAX_LINK_CHANNELS = 256;
    private static final int MAX_TAGS = 256;

    private TomsStoragePayloads() {
    }

    public static byte[] writeNbt(CompoundTag tag) {
        validateNbt(tag);
        try (DataOutputStream measured = new DataOutputStream(new LimitedOutputStream(MAX_DECOMPRESSED_NBT_BYTES))) {
            NbtIo.write(tag, measured);
        } catch (PayloadTooLargeIOException exception) {
            throw new IllegalArgumentException("Tom's Storage uncompressed NBT payload is too large", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to measure Tom's Storage NBT payload", exception);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            byte[] encoded = output.toByteArray();
            if (encoded.length > MAX_COMPRESSED_NBT_BYTES) {
                throw new IllegalArgumentException("Tom's Storage NBT payload is too large: " + encoded.length);
            }
            return encoded;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Tom's Storage NBT payload", exception);
        }
    }

    public static CompoundTag readNbt(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Tom's Storage compressed NBT payload is required");
        }
        if (data.length > MAX_COMPRESSED_NBT_BYTES) {
            throw new IllegalArgumentException("Tom's Storage compressed NBT payload is too large: " + data.length);
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(data.length * 4, MAX_DECOMPRESSED_NBT_BYTES))) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_DECOMPRESSED_NBT_BYTES) {
                    throw new IllegalArgumentException("Tom's Storage decompressed NBT payload is too large: " + total);
                }
                output.write(buffer, 0, read);
            }
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()))) {
                CompoundTag tag = NbtIo.read(input, new NbtAccounter(MAX_NBT_ACCOUNTED_BYTES));
                if (tag == null) {
                    throw new IllegalArgumentException("Tom's Storage NBT payload has no compound root");
                }
                if (input.read() != -1) {
                    throw new IllegalArgumentException("Tom's Storage NBT payload has trailing bytes");
                }
                validateNbt(tag);
                return tag;
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException | RuntimeException | StackOverflowError exception) {
            throw new IllegalArgumentException("Failed to decode Tom's Storage NBT payload", exception);
        }
    }

    public static byte[] writeTerminalAction(
        RegistryAccess registryAccess,
        String action,
        boolean modifier,
        ItemStack stack,
        long quantity
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeUtf(action, MAX_ACTION_LENGTH);
            buf.writeBoolean(modifier);
            buf.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                ItemStack one = stack.copyWithCount(1);
                buf.writeItem(one);
                buf.writeVarLong(quantity);
            }
            return toByteArray(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static TerminalAction readTerminalAction(RegistryAccess registryAccess, byte[] data) {
        FriendlyByteBuf buf = readBuffer(data);
        try {
            String action = buf.readUtf(MAX_ACTION_LENGTH);
            boolean modifier = buf.readBoolean();
            boolean hasStack = buf.readBoolean();
            ItemStack stack = ItemStack.EMPTY;
            long quantity = 0L;
            if (hasStack) {
                stack = buf.readItem();
                quantity = buf.readVarLong();
                if (stack.isEmpty() || quantity <= 0L) {
                    throw new IllegalArgumentException("Tom's Storage terminal action has an invalid stack quantity");
                }
            }
            requireFullyRead(buf, "terminal action");
            return new TerminalAction(action, modifier, stack, quantity);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries) {
        int limit = Math.min(entries.size(), MAX_TERMINAL_ENTRIES);
        while (true) {
            try {
                return writeTerminalSnapshot(registryAccess, entries, limit, limit < entries.size());
            } catch (OversizedSnapshotException exception) {
                if (limit == 0) {
                    throw new IllegalArgumentException("Tom's Storage terminal snapshot header exceeds the packet limit", exception);
                }
                limit /= 2;
            }
        }
    }

    private static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries, int limit, boolean truncated) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            int count = Math.min(entries.size(), limit);
            buf.writeBoolean(truncated || count < entries.size());
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                TerminalEntry entry = entries.get(i);
                buf.writeItem(entry.stack().copyWithCount(1));
                buf.writeVarLong(entry.quantity());
            }
            return toSnapshotByteArray(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static TerminalSnapshot readTerminalSnapshot(RegistryAccess registryAccess, byte[] data) {
        FriendlyByteBuf buf = readBuffer(data);
        try {
            boolean truncated = buf.readBoolean();
            int count = readBoundedCount(buf, MAX_TERMINAL_ENTRIES, "terminal entries");
            List<TerminalEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack stack = buf.readItem();
                long quantity = buf.readVarLong();
                if (stack.isEmpty() || quantity <= 0L) {
                    throw new IllegalArgumentException("Tom's Storage terminal snapshot contains an invalid entry");
                }
                entries.add(new TerminalEntry(stack, quantity));
            }
            requireFullyRead(buf, "terminal snapshot");
            return new TerminalSnapshot(entries, truncated);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected) {
        int limit = Math.min(channels.size(), MAX_LINK_CHANNELS);
        while (true) {
            try {
                return writeLinkSnapshot(registryAccess, channels, selected, limit);
            } catch (OversizedSnapshotException exception) {
                if (limit == 0) {
                    throw new IllegalArgumentException("Tom's Storage link snapshot header exceeds the packet limit", exception);
                }
                limit /= 2;
            }
        }
    }

    private static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected, int limit) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeBoolean(selected != null);
            if (selected != null) {
                buf.writeUUID(selected);
            }
            int count = Math.min(Math.min(channels.size(), MAX_LINK_CHANNELS), limit);
            buf.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                LinkChannel channel = channels.get(index);
                buf.writeUUID(channel.id());
                buf.writeUtf(channel.name(), MAX_NAME_LENGTH);
                buf.writeBoolean(channel.publicChannel());
                buf.writeBoolean(channel.ownerName() != null);
                if (channel.ownerName() != null) {
                    buf.writeUtf(channel.ownerName(), MAX_NAME_LENGTH);
                }
            }
            return toSnapshotByteArray(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static LinkSnapshot readLinkSnapshot(RegistryAccess registryAccess, byte[] data) {
        FriendlyByteBuf buf = readBuffer(data);
        try {
            UUID selected = buf.readBoolean() ? buf.readUUID() : null;
            int count = readBoundedCount(buf, MAX_LINK_CHANNELS, "link channels");
            List<LinkChannel> channels = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID id = buf.readUUID();
                String name = buf.readUtf(MAX_NAME_LENGTH);
                boolean publicChannel = buf.readBoolean();
                String ownerName = buf.readBoolean() ? buf.readUtf(MAX_NAME_LENGTH) : null;
                channels.add(new LinkChannel(id, name, publicChannel, ownerName));
            }
            requireFullyRead(buf, "link snapshot");
            return new LinkSnapshot(channels, selected);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeTagSnapshot(List<String> tags) {
        int limit = Math.min(tags.size(), MAX_TAGS);
        while (true) {
            try {
                return writeTagSnapshot(tags, limit);
            } catch (OversizedSnapshotException exception) {
                if (limit == 0) {
                    throw new IllegalArgumentException("Tom's Storage tag snapshot header exceeds the packet limit", exception);
                }
                limit /= 2;
            }
        }
    }

    private static byte[] writeTagSnapshot(List<String> tags, int limit) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            int count = Math.min(Math.min(tags.size(), MAX_TAGS), limit);
            buf.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                buf.writeUtf(tags.get(index), MAX_TAG_LENGTH);
            }
            return toSnapshotByteArray(buf);
        } catch (RuntimeException | Error exception) {
            releaseIfOwned(buf);
            throw exception;
        }
    }

    public static List<String> readTagSnapshot(byte[] data) {
        FriendlyByteBuf buf = readBuffer(data);
        try {
            int count = readBoundedCount(buf, MAX_TAGS, "tags");
            List<String> tags = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                tags.add(buf.readUtf(MAX_TAG_LENGTH));
            }
            requireFullyRead(buf, "tag snapshot");
            return tags;
        } finally {
            buf.release();
        }
    }

    private static FriendlyByteBuf readBuffer(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Tom's Storage payload is required");
        }
        if (data.length > MAX_PACKET_BYTES) {
            throw new IllegalArgumentException("Tom's Storage payload is too large: " + data.length);
        }
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }

    private static int readBoundedCount(FriendlyByteBuf buf, int maximum, String label) {
        int count = buf.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Tom's Storage " + label + " count is invalid: " + count);
        }
        return count;
    }

    private static void requireFullyRead(FriendlyByteBuf buf, String label) {
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Tom's Storage " + label + " has trailing bytes: " + buf.readableBytes());
        }
    }

    private static void validateNbt(CompoundTag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tom's Storage NBT compound is required");
        }
        int[] remainingNodes = {MAX_NBT_NODES};
        validateNbt(tag, 0, remainingNodes);
    }

    private static void validateNbt(Tag tag, int depth, int[] remainingNodes) {
        if (depth > MAX_NBT_DEPTH || --remainingNodes[0] < 0) {
            throw new IllegalArgumentException("Tom's Storage NBT exceeds structural limits");
        }
        if (tag instanceof CompoundTag compound) {
            if (compound.size() > MAX_NBT_COLLECTION_SIZE) {
                throw new IllegalArgumentException("Tom's Storage NBT compound is too large: " + compound.size());
            }
            for (String key : compound.getAllKeys()) {
                int keyBytes = key.getBytes(StandardCharsets.UTF_8).length;
                if (keyBytes > MAX_NBT_STRING_BYTES) {
                    throw new IllegalArgumentException("Tom's Storage NBT key is too long: " + keyBytes);
                }
                Tag child = compound.get(key);
                if (child != null) {
                    validateNbt(child, depth + 1, remainingNodes);
                }
            }
        } else if (tag instanceof ListTag list) {
            if (list.size() > MAX_NBT_COLLECTION_SIZE) {
                throw new IllegalArgumentException("Tom's Storage NBT list is too large: " + list.size());
            }
            for (Tag child : list) {
                validateNbt(child, depth + 1, remainingNodes);
            }
        } else if (tag instanceof StringTag string
            && string.getAsString().getBytes(StandardCharsets.UTF_8).length > MAX_NBT_STRING_BYTES) {
            throw new IllegalArgumentException("Tom's Storage NBT string is too long");
        } else if (tag instanceof ByteArrayTag bytes && bytes.getAsByteArray().length > MAX_NBT_ARRAY_BYTES
            || tag instanceof IntArrayTag ints && ints.getAsIntArray().length > MAX_NBT_ARRAY_BYTES / Integer.BYTES
            || tag instanceof LongArrayTag longs && longs.getAsLongArray().length > MAX_NBT_ARRAY_BYTES / Long.BYTES) {
            throw new IllegalArgumentException("Tom's Storage NBT array is too large");
        }
    }

    private static byte[] toByteArray(FriendlyByteBuf buf) {
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

    private static byte[] toSnapshotByteArray(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > MAX_PACKET_BYTES) {
                throw new OversizedSnapshotException(buf.readableBytes());
            }
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void releaseIfOwned(FriendlyByteBuf buf) {
        if (buf.refCnt() > 0) {
            buf.release();
        }
    }

    private static final class OversizedSnapshotException extends RuntimeException {
        private OversizedSnapshotException(int size) {
            super("Tom's Storage snapshot is too large: " + size, null, false, false);
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
    }

    public record TerminalSnapshot(List<TerminalEntry> entries, boolean truncated) {
    }

    public record TerminalAction(String action, boolean modifier, ItemStack stack, long quantity) {
    }

    public record LinkChannel(UUID id, String name, boolean publicChannel, String ownerName) {
    }

    public record LinkSnapshot(List<LinkChannel> channels, UUID selected) {
    }
}
