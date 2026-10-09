package com.salts_inventory_update.compat.toms_storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;

import com.salts_inventory_update.debug.DesktopDebug;

public final class TomsStorageReflect {
    private static @Nullable Method numberFormatMethod;
    private static boolean numberFormatLookupDone;

    private TomsStorageReflect() {
    }

    public static @Nullable Object field(Object target, String name) {
        if (target == null) {
            return null;
        }

        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed field target={} field={} reason={}", target.getClass().getName(), name, exception.toString());
            return null;
        }
    }

    public static boolean setField(Object target, String name, Object value) {
        if (target == null) {
            return false;
        }

        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            field.set(target, value);
            return true;
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed set field target={} field={} reason={}", target.getClass().getName(), name, exception.toString());
            return false;
        }
    }

    public static boolean setIntField(Object target, String name, int value) {
        return setField(target, name, value);
    }

    public static String formatNumber(long number) {
        Method method = numberFormatMethod();
        if (method != null) {
            try {
                Object value = method.invoke(null, number);
                if (value instanceof String string) {
                    return string;
                }
            } catch (ReflectiveOperationException exception) {
                DesktopDebug.warn("Tom's Storage compat failed number format reason={}", exception.toString());
            }
        }
        return Long.toString(number);
    }

    public static int intField(Object target, String name, int fallback) {
        Object value = field(target, name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public static int enumOrdinalField(Object target, String name, int fallback) {
        Object value = field(target, name);
        if (value instanceof Enum<?> enumValue) {
            return enumValue.ordinal();
        }
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public static String enumNameField(Object target, String name, String fallback) {
        Object value = field(target, name);
        return value instanceof Enum<?> enumValue ? enumValue.name() : fallback;
    }

    public static boolean booleanField(Object target, String name, boolean fallback) {
        Object value = field(target, name);
        return value instanceof Boolean bool ? bool.booleanValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listField(Object target, String name) {
        Object value = field(target, name);
        return value instanceof List<?> list ? (List<T>) list : List.of();
    }

    public static @Nullable ItemStack storedStack(Object storedStack) {
        Object value = invoke(storedStack, "getStack");
        return value instanceof ItemStack stack ? stack.copy() : null;
    }

    public static long storedQuantity(Object storedStack) {
        Object value = invoke(storedStack, "getQuantity");
        return value instanceof Number number ? number.longValue() : 0L;
    }

    public static @Nullable Object newStoredStack(ItemStack stack, long quantity) {
        try {
            Class<?> clazz = Class.forName("com.tom.storagemod.inventory.StoredItemStack");
            Constructor<?> constructor = clazz.getConstructor(ItemStack.class, long.class);
            return constructor.newInstance(stack.copyWithCount(1), quantity);
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed stored stack create reason={}", exception.toString());
            return null;
        }
    }

    public static void syncTerminalClientItems(AbstractContainerMenu menu, List<TomsStoragePayloads.TerminalEntry> entries) {
        List<Object> storedStacks = new ArrayList<>(entries.size());
        for (TomsStoragePayloads.TerminalEntry entry : entries) {
            Object storedStack = newStoredStack(entry.stack(), entry.quantity());
            if (storedStack != null) {
                storedStacks.add(storedStack);
            }
        }

        setField(menu, "itemList", storedStacks);
        setField(menu, "itemListClient", new ArrayList<>(storedStacks));
        setField(menu, "itemListClientSorted", new ArrayList<>(storedStacks));
        setField(menu, "itemsLoaded", true);
        DesktopDebug.trace(
            "Tom's Storage compat synced client terminal items menu={} entries={} storedStacks={}",
            menu.getClass().getName(),
            entries.size(),
            storedStacks.size()
        );
    }

    public static boolean invokeTerminalAction(AbstractContainerMenu menu, String action, boolean modifier, ItemStack stack, long quantity) {
        Object storedStack = stack.isEmpty() ? null : newStoredStack(stack, quantity);
        try {
            Class<?> storedClass = Class.forName("com.tom.storagemod.inventory.StoredItemStack");
            Class<?> actionClass = Class.forName("com.tom.storagemod.util.TerminalSyncManager$SlotAction");
            Object actionValue = Enum.valueOf((Class) actionClass.asSubclass(Enum.class), action);
            Method method = menu.getClass().getMethod("onInteract", storedClass, actionClass, boolean.class);
            method.invoke(menu, storedStack, actionValue, modifier);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            DesktopDebug.warn("Tom's Storage compat failed terminal action menu={} action={} reason={}", menu.getClass().getName(), action, exception.toString());
            return false;
        }
    }

    public static boolean receiveNbt(ServerPlayer player, AbstractContainerMenu menu, CompoundTag tag) {
        try {
            Class<?> receiver = Class.forName("com.tom.storagemod.util.IDataReceiver");
            if (!receiver.isInstance(menu)) {
                return false;
            }

            Method receive = receiver.getMethod("receive", net.minecraft.world.level.storage.ValueInput.class);
            receive.invoke(menu, TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag));
            return true;
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed receive menu={} reason={}", menu.getClass().getName(), exception.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<Object> terminalStoredStacks(AbstractContainerMenu menu) {
        Object sync = field(menu, "sync");
        Object items = field(sync, "items");
        if (items instanceof Map<?, ?> map) {
            return (Collection<Object>) map.values();
        }
        return List.of();
    }

    public static @Nullable UUID inventoryLinkSelected(AbstractContainerMenu menu) {
        Object te = field(menu, "te");
        Object selected = invoke(te, "getChannel");
        return selected instanceof UUID uuid ? uuid : null;
    }

    @SuppressWarnings("unchecked")
    public static List<TomsStoragePayloads.LinkChannel> inventoryLinkChannels(ServerPlayer player) {
        try {
            Class<?> remoteConnectionsClass = Class.forName("com.tom.storagemod.inventory.RemoteConnections");
            Method get = remoteConnectionsClass.getMethod("get", net.minecraft.world.level.Level.class);
            Object remoteConnections = get.invoke(null, player.level());
            Method streamChannels = remoteConnectionsClass.getMethod("streamChannels", net.minecraft.world.entity.player.Player.class);
            Object streamObject = streamChannels.invoke(remoteConnections, player);
            if (!(streamObject instanceof Stream<?> stream)) {
                return List.of();
            }

            return stream
                .map(entry -> {
                    if (!(entry instanceof Map.Entry<?, ?> mapEntry) || !(mapEntry.getKey() instanceof UUID uuid)) {
                        return null;
                    }
                    Object channel = mapEntry.getValue();
                    String name = stringField(channel, "displayName", "Channel");
                    String ownerName = stringField(channel, "ownerName", null);
                    boolean publicChannel = booleanField(channel, "publicChannel", false);
                    return new TomsStoragePayloads.LinkChannel(uuid, name, publicChannel, ownerName);
                })
                .filter(TomsStoragePayloads.LinkChannel.class::isInstance)
                .map(TomsStoragePayloads.LinkChannel.class::cast)
                .toList();
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed inventory link channel snapshot reason={}", exception.toString());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> tagFilterTags(AbstractContainerMenu menu) {
        Object filter = field(menu, "filter");
        Object tags = invoke(filter, "getTags");
        if (tags instanceof Collection<?> collection) {
            return collection.stream().map(TomsStorageReflect::tagLocationString).toList();
        }
        return List.of();
    }

    private static String tagLocationString(Object tag) {
        Object location = invoke(tag, "location");
        return location != null ? location.toString() : tag.toString();
    }

    private static @Nullable Object invoke(@Nullable Object target, String name) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static @Nullable String stringField(Object target, String name, @Nullable String fallback) {
        Object value = field(target, name);
        return value instanceof String string ? string : fallback;
    }

    private static @Nullable Method numberFormatMethod() {
        if (numberFormatLookupDone) {
            return numberFormatMethod;
        }

        numberFormatLookupDone = true;
        try {
            Class<?> clazz = Class.forName("com.tom.storagemod.util.NumberFormatUtil");
            numberFormatMethod = clazz.getMethod("formatNumber", long.class);
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed number formatter lookup reason={}", exception.toString());
            numberFormatMethod = null;
        }
        return numberFormatMethod;
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
