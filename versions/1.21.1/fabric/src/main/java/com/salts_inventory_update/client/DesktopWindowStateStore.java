package com.salts_inventory_update.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salts_inventory_update.platform.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.debug.DesktopDebug;

final class DesktopWindowStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(SaltsInventoryUpdate.MOD_ID)
        .resolve("desktop_window_state.json");
    private static StateFile stateFile;

    private DesktopWindowStateStore() {
    }

    static Optional<WindowState> load(Minecraft minecraft, String windowKey) {
        StateFile file = stateFile();
        Map<String, WindowState> world = file.worlds.get(worldKey(minecraft));
        if (world == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(world.get(windowKey));
    }

    static void save(Minecraft minecraft, String windowKey, WindowState state) {
        StateFile file = stateFile();
        file.worlds.computeIfAbsent(worldKey(minecraft), ignored -> new LinkedHashMap<>()).put(windowKey, state);
        write(file);
    }

    static Set<String> linkedWindowKeys(Minecraft minecraft, String windowKey) {
        Map<String, List<String>> worldLinks = stateFile().links.get(worldKey(minecraft));
        if (worldLinks == null || windowKey == null || windowKey.isBlank()) {
            return Set.of();
        }

        Set<String> group = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(windowKey);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!group.add(current)) {
                continue;
            }
            for (String linked : worldLinks.getOrDefault(current, List.of())) {
                if (linked != null && !linked.isBlank() && !group.contains(linked)) {
                    pending.addLast(linked);
                }
            }
        }
        return group.size() <= 1 ? Set.of() : Set.copyOf(group);
    }

    static void linkWindowKeys(Minecraft minecraft, String firstKey, String secondKey) {
        if (firstKey == null || secondKey == null || firstKey.isBlank() || secondKey.isBlank() || firstKey.equals(secondKey)) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.computeIfAbsent(worldKey(minecraft), ignored -> new LinkedHashMap<>());
        addLink(worldLinks, firstKey, secondKey);
        addLink(worldLinks, secondKey, firstKey);
        write(file);
    }

    static void unlinkWindowKey(Minecraft minecraft, String windowKey) {
        if (windowKey == null || windowKey.isBlank()) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.get(worldKey(minecraft));
        if (worldLinks == null) {
            return;
        }

        worldLinks.remove(windowKey);
        for (Map.Entry<String, List<String>> entry : List.copyOf(worldLinks.entrySet())) {
            List<String> remaining = entry.getValue() == null
                ? List.of()
                : entry.getValue().stream().filter(key -> !windowKey.equals(key)).distinct().toList();
            if (remaining.isEmpty()) {
                worldLinks.remove(entry.getKey());
            } else {
                worldLinks.put(entry.getKey(), remaining);
            }
        }
        write(file);
    }

    static void unlinkWindowKeyFromGroup(Minecraft minecraft, String originKey, String targetKey) {
        if (originKey == null || targetKey == null || originKey.isBlank() || targetKey.isBlank() || originKey.equals(targetKey)) {
            return;
        }

        Set<String> originGroup = linkedWindowKeys(minecraft, originKey);
        if (originGroup.isEmpty() || !originGroup.contains(targetKey)) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.get(worldKey(minecraft));
        if (worldLinks == null) {
            return;
        }

        for (String groupKey : originGroup) {
            if (groupKey.equals(targetKey)) {
                continue;
            }
            removeLink(worldLinks, groupKey, targetKey);
            removeLink(worldLinks, targetKey, groupKey);
        }
        write(file);
    }

    private static void addLink(Map<String, List<String>> worldLinks, String sourceKey, String targetKey) {
        Set<String> linked = new LinkedHashSet<>(worldLinks.getOrDefault(sourceKey, List.of()));
        linked.add(targetKey);
        worldLinks.put(sourceKey, List.copyOf(linked));
    }

    private static void removeLink(Map<String, List<String>> worldLinks, String sourceKey, String targetKey) {
        List<String> existing = worldLinks.get(sourceKey);
        if (existing == null || existing.isEmpty()) {
            return;
        }

        List<String> remaining = existing.stream().filter(key -> !targetKey.equals(key)).distinct().toList();
        if (remaining.isEmpty()) {
            worldLinks.remove(sourceKey);
        } else {
            worldLinks.put(sourceKey, remaining);
        }
    }

    private static StateFile stateFile() {
        if (stateFile != null) {
            return stateFile;
        }

        if (!Files.isRegularFile(CONFIG_PATH)) {
            stateFile = new StateFile();
            return stateFile;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            StateFile loaded = GSON.fromJson(reader, StateFile.class);
            stateFile = loaded == null ? new StateFile() : loaded.normalized();
        } catch (IOException | RuntimeException exception) {
            DesktopDebug.warn("client window state load failed path={} reason={}", CONFIG_PATH, exception.toString());
            stateFile = new StateFile();
        }
        return stateFile;
    }

    private static void write(StateFile file) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(file.normalized(), writer);
            }
        } catch (IOException | RuntimeException exception) {
            DesktopDebug.warn("client window state save failed path={} reason={}", CONFIG_PATH, exception.toString());
        }
    }

    private static String worldKey(Minecraft minecraft) {
        Object server = minecraft.getCurrentServer();
        if (server != null) {
            String address = reflectedString(server, "ip");
            if (!address.isEmpty()) {
                return "server:" + address;
            }
            return "server:" + server;
        }

        Object singleplayer = minecraft.getSingleplayerServer();
        if (singleplayer != null) {
            String levelName = reflectedMethodString(reflectedMethod(singleplayer, "getWorldData"), "getLevelName");
            if (!levelName.isEmpty()) {
                return "singleplayer:" + levelName;
            }
        }

        if (minecraft.level != null) {
            return "level:" + minecraft.level.dimension().location();
        }
        return "unknown";
    }

    private static String reflectedString(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static Object reflectedMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String reflectedMethodString(Object target, String methodName) {
        if (target == null) {
            return "";
        }
        Object value = reflectedMethod(target, methodName);
        return value == null ? "" : value.toString();
    }

    static final class WindowState {
        int x;
        int y;
        int width;
        int height;
        boolean locked = true;
        String pinMode = PinMode.UNPINNED.name();
        String localState = "{}";

        WindowState() {
        }

        WindowState(int x, int y, int width, int height, boolean locked, PinMode pinMode) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.locked = locked;
            this.pinMode = pinMode.name();
        }

        PinMode pinMode() {
            try {
                return PinMode.valueOf(this.pinMode);
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return PinMode.UNPINNED;
            }
        }

        CompoundTag localState() {
            if (this.localState == null || this.localState.isBlank()) {
                return new CompoundTag();
            }

            try {
                return TagParser.parseTag(this.localState);
            } catch (Exception exception) {
                DesktopDebug.warn("client window local state parse failed reason={}", exception.toString());
                return new CompoundTag();
            }
        }

        void localState(CompoundTag tag) {
            this.localState = tag == null || tag.isEmpty() ? "{}" : tag.toString();
        }
    }

    private static final class StateFile {
        Map<String, Map<String, WindowState>> worlds = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> links = new LinkedHashMap<>();

        private StateFile normalized() {
            if (this.worlds == null) {
                this.worlds = new LinkedHashMap<>();
            }
            if (this.links == null) {
                this.links = new LinkedHashMap<>();
            }

            for (Map.Entry<String, Map<String, WindowState>> entry : List.copyOf(this.worlds.entrySet())) {
                if (entry.getValue() == null) {
                    this.worlds.put(entry.getKey(), new LinkedHashMap<>());
                }
            }
            for (Map.Entry<String, Map<String, List<String>>> worldEntry : List.copyOf(this.links.entrySet())) {
                Map<String, List<String>> worldLinks = worldEntry.getValue();
                if (worldLinks == null) {
                    this.links.put(worldEntry.getKey(), new LinkedHashMap<>());
                    continue;
                }
                for (Map.Entry<String, List<String>> linkEntry : List.copyOf(worldLinks.entrySet())) {
                    if (linkEntry.getKey() == null || linkEntry.getKey().isBlank()) {
                        worldLinks.remove(linkEntry.getKey());
                        continue;
                    }
                    List<String> values = linkEntry.getValue() == null
                        ? List.of()
                        : linkEntry.getValue().stream()
                            .filter(value -> value != null && !value.isBlank() && !value.equals(linkEntry.getKey()))
                            .distinct()
                            .toList();
                    if (values.isEmpty()) {
                        worldLinks.remove(linkEntry.getKey());
                    } else {
                        worldLinks.put(linkEntry.getKey(), values);
                    }
                }
            }
            return this;
        }
    }
}
