package com.salts_inventory_update.compat.toms_storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class TomsStoragePayloads {
    private static final int MAX_PACKET_BYTES = 30 * 1024;
    private static final int MAX_UTF = 32767;

    private TomsStoragePayloads() {
    }

    public static byte[] writeNbt(CompoundTag tag) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Tom's Storage NBT payload", exception);
        }
    }

    public static CompoundTag readNbt(byte[] data) {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (IOException exception) {
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
        return toByteArray(buf);
    }

    public static TerminalAction readTerminalAction(RegistryAccess registryAccess, byte[] data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        String action = buf.readUtf(MAX_UTF);
        boolean modifier = buf.readBoolean();
        boolean hasStack = buf.readBoolean();
        ItemStack stack = ItemStack.EMPTY;
        long quantity = 0L;
        if (hasStack) {
            stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            quantity = buf.readVarLong();
        }
        return new TerminalAction(action, modifier, stack, quantity);
    }

    public static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries) {
        int limit = entries.size();
        byte[] encoded = writeTerminalSnapshot(registryAccess, entries, limit, false);
        while (encoded.length > MAX_PACKET_BYTES && limit > 0) {
            limit = Math.max(0, limit / 2);
            encoded = writeTerminalSnapshot(registryAccess, entries, limit, true);
        }
        return encoded;
    }

    private static byte[] writeTerminalSnapshot(RegistryAccess registryAccess, List<TerminalEntry> entries, int limit, boolean truncated) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        int count = Math.min(entries.size(), limit);
        buf.writeBoolean(truncated || count < entries.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            TerminalEntry entry = entries.get(i);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack().copyWithCount(1));
            buf.writeVarLong(entry.quantity());
        }
        return toByteArray(buf);
    }

    public static TerminalSnapshot readTerminalSnapshot(RegistryAccess registryAccess, byte[] data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        boolean truncated = buf.readBoolean();
        int count = buf.readVarInt();
        List<TerminalEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long quantity = buf.readVarLong();
            entries.add(new TerminalEntry(stack, quantity));
        }
        return new TerminalSnapshot(entries, truncated);
    }

    public static byte[] writeLinkSnapshot(RegistryAccess registryAccess, List<LinkChannel> channels, UUID selected) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeBoolean(selected != null);
        if (selected != null) {
            buf.writeUUID(selected);
        }
        buf.writeVarInt(channels.size());
        for (LinkChannel channel : channels) {
            buf.writeUUID(channel.id());
            buf.writeUtf(channel.name(), MAX_UTF);
            buf.writeBoolean(channel.publicChannel());
            buf.writeBoolean(channel.ownerName() != null);
            if (channel.ownerName() != null) {
                buf.writeUtf(channel.ownerName(), MAX_UTF);
            }
        }
        return toByteArray(buf);
    }

    public static LinkSnapshot readLinkSnapshot(RegistryAccess registryAccess, byte[] data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        UUID selected = buf.readBoolean() ? buf.readUUID() : null;
        int count = buf.readVarInt();
        List<LinkChannel> channels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(MAX_UTF);
            boolean publicChannel = buf.readBoolean();
            String ownerName = buf.readBoolean() ? buf.readUtf(MAX_UTF) : null;
            channels.add(new LinkChannel(id, name, publicChannel, ownerName));
        }
        return new LinkSnapshot(channels, selected);
    }

    public static byte[] writeTagSnapshot(List<String> tags) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeVarInt(tags.size());
        for (String tag : tags) {
            buf.writeUtf(tag, MAX_UTF);
        }
        return toByteArray(buf);
    }

    public static List<String> readTagSnapshot(byte[] data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), RegistryAccess.EMPTY);
        int count = buf.readVarInt();
        List<String> tags = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tags.add(buf.readUtf(MAX_UTF));
        }
        return tags;
    }

    private static byte[] toByteArray(RegistryFriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        return data;
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
