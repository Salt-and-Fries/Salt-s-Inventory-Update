package com.salts_inventory_update.compat.toms_storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

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
    private static final int MAX_DECOMPRESSED_NBT_BYTES = DesktopProtocol.MAX_TOMS_DECOMPRESSED_BYTES;
    private static final int MAX_TERMINAL_ENTRIES = MAX_PACKET_BYTES / 2;
    private static final int MAX_LINK_CHANNELS = MAX_PACKET_BYTES / 19;
    private static final int MAX_TAGS = MAX_PACKET_BYTES;
    private static final int MAX_UTF = DesktopProtocol.MAX_TOMS_STRING_BYTES;

    private TomsStoragePayloads() {
    }

    public static byte[] writeNbt(CompoundTag tag) {
        validateNbt(tag);
        try {
            try (DataOutputStream output = new DataOutputStream(new BoundedOutputStream(MAX_DECOMPRESSED_NBT_BYTES))) {
                NbtIo.write(tag, output);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            return requireEncodedSize(output.toByteArray(), "NBT");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Tom's Storage NBT payload", exception);
        }
    }

    public static CompoundTag readNbt(byte[] data) {
        requireInputSize(data, "NBT");
        try {
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(MAX_DECOMPRESSED_NBT_BYTES));
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
        buf.writeUtf(action, MAX_UTF);
        buf.writeBoolean(modifier);
        buf.writeBoolean(!stack.isEmpty());
        if (!stack.isEmpty()) {
            ItemStack one = stack.copyWithCount(1);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, one);
            buf.writeVarLong(quantity);
        }
        return requireEncodedSize(toByteArray(buf), "terminal action");
    }

    public static TerminalAction readTerminalAction(RegistryAccess registryAccess, byte[] data) {
        requireInputSize(data, "terminal action");
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
            }
            requireFullyRead(buf, "terminal action");
            return new TerminalAction(action, modifier, stack, quantity);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries) {
        List<byte[]> encodedEntries = new ArrayList<>();
        int encodedBytes = 0;
        int limit = Math.min(entries.size(), MAX_TERMINAL_ENTRIES);
        for (int i = 0; i < limit; i++) {
            TerminalEntry entry = entries.get(i);
            byte[] encoded = tryEncode(registryAccess, buf -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack().copyWithCount(1));
                buf.writeVarLong(entry.quantity());
            });
            int nextCount = encodedEntries.size() + 1;
            if (encoded == null || 1 + varIntSize(nextCount) + encodedBytes + encoded.length > MAX_PACKET_BYTES) {
                break;
            }
            encodedEntries.add(encoded);
            encodedBytes += encoded.length;
        }
        RegistryFriendlyByteBuf buf = boundedBuffer(registryAccess);
        try {
            buf.writeBoolean(encodedEntries.size() < entries.size());
            buf.writeVarInt(encodedEntries.size());
            encodedEntries.forEach(buf::writeBytes);
            return copyByteArray(buf);
        } finally {
            buf.release();
        }
    }

    public static TerminalSnapshot readTerminalSnapshot(RegistryAccess registryAccess, byte[] data) {
        requireInputSize(data, "terminal snapshot");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        try {
            boolean truncated = buf.readBoolean();
            int count = readBoundedCount(buf, MAX_TERMINAL_ENTRIES, 2, "terminal snapshot");
            List<TerminalEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                long quantity = buf.readVarLong();
                entries.add(new TerminalEntry(stack, quantity));
            }
            requireFullyRead(buf, "terminal snapshot");
            return new TerminalSnapshot(entries, truncated);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected) {
        List<byte[]> encodedChannels = new ArrayList<>();
        int encodedBytes = 0;
        int headerBytes = 1 + (selected == null ? 0 : 16);
        int limit = Math.min(channels.size(), MAX_LINK_CHANNELS);
        for (int i = 0; i < limit; i++) {
            LinkChannel channel = channels.get(i);
            byte[] encoded = tryEncode(registryAccess, buf -> {
                buf.writeUUID(channel.id());
                buf.writeUtf(channel.name(), MAX_UTF);
                buf.writeBoolean(channel.publicChannel());
                buf.writeBoolean(channel.ownerName() != null);
                if (channel.ownerName() != null) {
                    buf.writeUtf(channel.ownerName(), MAX_UTF);
                }
            });
            int nextCount = encodedChannels.size() + 1;
            if (encoded == null || headerBytes + varIntSize(nextCount) + encodedBytes + encoded.length > MAX_PACKET_BYTES) {
                break;
            }
            encodedChannels.add(encoded);
            encodedBytes += encoded.length;
        }
        RegistryFriendlyByteBuf buf = boundedBuffer(registryAccess);
        try {
            buf.writeBoolean(selected != null);
            if (selected != null) {
                buf.writeUUID(selected);
            }
            buf.writeVarInt(encodedChannels.size());
            encodedChannels.forEach(buf::writeBytes);
            return copyByteArray(buf);
        } finally {
            buf.release();
        }
    }

    public static LinkSnapshot readLinkSnapshot(RegistryAccess registryAccess, byte[] data) {
        requireInputSize(data, "link snapshot");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        try {
            UUID selected = buf.readBoolean() ? buf.readUUID() : null;
            int count = readBoundedCount(buf, MAX_LINK_CHANNELS, 19, "link snapshot");
            List<LinkChannel> channels = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID id = buf.readUUID();
                String name = buf.readUtf(MAX_UTF);
                boolean publicChannel = buf.readBoolean();
                String ownerName = buf.readBoolean() ? buf.readUtf(MAX_UTF) : null;
                channels.add(new LinkChannel(id, name, publicChannel, ownerName));
            }
            requireFullyRead(buf, "link snapshot");
            return new LinkSnapshot(channels, selected);
        } finally {
            buf.release();
        }
    }

    public static byte[] writeTagSnapshot(List<String> tags) {
        List<byte[]> encodedTags = new ArrayList<>();
        int encodedBytes = 0;
        int limit = Math.min(tags.size(), MAX_TAGS);
        for (int i = 0; i < limit; i++) {
            String tag = tags.get(i);
            byte[] encoded = tryEncode(RegistryAccess.EMPTY, buf -> buf.writeUtf(tag, MAX_UTF));
            int nextCount = encodedTags.size() + 1;
            if (encoded == null || varIntSize(nextCount) + encodedBytes + encoded.length > MAX_PACKET_BYTES) {
                break;
            }
            encodedTags.add(encoded);
            encodedBytes += encoded.length;
        }
        RegistryFriendlyByteBuf buf = boundedBuffer(RegistryAccess.EMPTY);
        try {
            buf.writeVarInt(encodedTags.size());
            encodedTags.forEach(buf::writeBytes);
            return copyByteArray(buf);
        } finally {
            buf.release();
        }
    }

    public static List<String> readTagSnapshot(byte[] data) {
        requireInputSize(data, "tag snapshot");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), RegistryAccess.EMPTY);
        try {
            int count = readBoundedCount(buf, MAX_TAGS, 1, "tag snapshot");
            List<String> tags = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                tags.add(buf.readUtf(MAX_UTF));
            }
            requireFullyRead(buf, "tag snapshot");
            return tags;
        } finally {
            buf.release();
        }
    }

    private static int readBoundedCount(RegistryFriendlyByteBuf buf, int maxCount, int minimumBytesPerEntry, String label) {
        int count = buf.readVarInt();
        if (count < 0 || count > maxCount || count > buf.readableBytes() / minimumBytesPerEntry) {
            throw new DecoderException("Tom's Storage " + label + " has an invalid count: " + count);
        }
        return count;
    }

    private static void requireInputSize(byte[] data, String label) {
        if (data == null || data.length > MAX_PACKET_BYTES) {
            throw new DecoderException("Tom's Storage " + label + " is too large: " + (data == null ? -1 : data.length));
        }
    }

    private static void validateNbt(CompoundTag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tom's Storage NBT compound is required");
        }
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

    private static byte[] requireEncodedSize(byte[] data, String label) {
        if (data.length > MAX_PACKET_BYTES) {
            throw new EncoderException("Tom's Storage " + label + " is too large: " + data.length);
        }
        return data;
    }

    private static void requireFullyRead(RegistryFriendlyByteBuf buf, String label) {
        if (buf.isReadable()) {
            throw new DecoderException("Tom's Storage " + label + " has " + buf.readableBytes() + " trailing bytes");
        }
    }

    private static RegistryFriendlyByteBuf boundedBuffer(RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(256, MAX_PACKET_BYTES), registryAccess);
    }

    private static byte[] tryEncode(RegistryAccess registryAccess, BufferWriter writer) {
        RegistryFriendlyByteBuf buf = boundedBuffer(registryAccess);
        try {
            writer.write(buf);
            return copyByteArray(buf);
        } catch (RuntimeException exception) {
            return null;
        } finally {
            buf.release();
        }
    }

    private static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            size++;
        }
        return size;
    }

    private static byte[] copyByteArray(RegistryFriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        return data;
    }

    private static byte[] toByteArray(RegistryFriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        return data;
    }

    @FunctionalInterface
    private interface BufferWriter {
        void write(RegistryFriendlyByteBuf buf);
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final int limit;
        private int written;

        private BoundedOutputStream(int limit) {
            this.limit = limit;
        }

        @Override
        public void write(int value) throws IOException {
            requireCapacity(1);
            this.written++;
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            if (offset < 0 || length < 0 || offset > data.length - length) {
                throw new IndexOutOfBoundsException();
            }
            requireCapacity(length);
            this.written += length;
        }

        private void requireCapacity(int length) throws IOException {
            if (length > this.limit - this.written) {
                throw new IOException("Tom's Storage NBT exceeds the decompressed size limit of " + this.limit + " bytes");
            }
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
