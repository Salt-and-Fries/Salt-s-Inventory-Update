package com.salts_inventory_update.client;

import java.io.IOException;
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
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.storage.LevelResource;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.persistence.AtomicUtf8File;
import com.salts_inventory_update.persistence.StableStateIdentity;
import com.salts_inventory_update.protocol.BoundedLinkGraph;
import com.salts_inventory_update.protocol.DesktopProtocol;

final class DesktopWindowStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(SaltsInventoryUpdate.MOD_ID)
        .resolve("desktop_window_state.json");
    private static final int MAX_STATE_FILE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_WORLDS = 64;
    private static final int MAX_WINDOWS_PER_WORLD = 256;
    private static final int MAX_LOCAL_STATE_LENGTH = 32 * 1024;
    private static final int MAX_LOCAL_STATE_DEPTH = 64;
    private static final int MAX_LOCAL_STATE_NODES = 4_096;
    private static final int MAX_LOCAL_STATE_DELIMITERS = MAX_LOCAL_STATE_NODES * 4;
    private static StateFile stateFile;

    private DesktopWindowStateStore() {
    }

    static Optional<WindowState> load(Minecraft minecraft, String windowKey) {
        StateFile file = stateFile();
        boolean globalStateEnabled = usesGlobalState(windowKey);
        if (globalStateEnabled) {
            WindowState globalState = file.globalWindows.get(windowKey);
            if (globalState != null) {
                return Optional.of(globalState);
            }
        }
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return Optional.empty();
        }
        Map<String, WindowState> world = file.worlds.get(worldKey);
        if (world == null) {
            return Optional.empty();
        }

        WindowState state = world.get(windowKey);
        if (globalStateEnabled && state != null) {
            file.globalWindows.put(windowKey, state);
            write(file);
        }
        return Optional.ofNullable(state);
    }

    static void save(Minecraft minecraft, String windowKey, WindowState state) {
        if (windowKey == null || windowKey.isBlank() || windowKey.length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH) {
            return;
        }
        StateFile file = stateFile();
        if (usesGlobalState(windowKey)) {
            file.globalWindows.put(windowKey, state);
            write(file);
            return;
        }
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return;
        }
        file.worlds.computeIfAbsent(worldKey, ignored -> new LinkedHashMap<>()).put(windowKey, state);
        write(file);
    }

    private static boolean usesGlobalState(String windowKey) {
        return SaltsInventoryConfig.get().globalPins
            && windowKey != null
            && !windowKey.startsWith("source:block:")
            && !windowKey.startsWith("source:chest:");
    }

    static Set<String> linkedWindowKeys(Minecraft minecraft, String windowKey) {
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return Set.of();
        }
        Map<String, List<String>> worldLinks = stateFile().links.get(worldKey);
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
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.computeIfAbsent(worldKey, ignored -> new LinkedHashMap<>());
        addLink(worldLinks, firstKey, secondKey);
        addLink(worldLinks, secondKey, firstKey);
        write(file);
    }

    static void unlinkWindowKey(Minecraft minecraft, String windowKey) {
        if (windowKey == null || windowKey.isBlank()) {
            return;
        }
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.get(worldKey);
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
        String worldKey = worldKey(minecraft);
        if (worldKey == null) {
            return;
        }

        Set<String> originGroup = linkedWindowKeys(minecraft, originKey);
        if (originGroup.isEmpty() || !originGroup.contains(targetKey)) {
            return;
        }

        StateFile file = stateFile();
        Map<String, List<String>> worldLinks = file.links.get(worldKey);
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

        try {
            Optional<String> contents = AtomicUtf8File.read(CONFIG_PATH, MAX_STATE_FILE_BYTES, DesktopWindowStateStore::validStateJson);
            StateFile loaded = contents.isPresent() ? GSON.fromJson(contents.get(), StateFile.class) : null;
            stateFile = loaded == null ? new StateFile() : loaded.normalized();
        } catch (IOException | RuntimeException | StackOverflowError exception) {
            DesktopDebug.warn("client window state load failed path={} reason={}", CONFIG_PATH, exception.toString());
            stateFile = new StateFile();
        }
        return stateFile;
    }

    private static void write(StateFile file) {
        try {
            String contents = GSON.toJson(file.normalized());
            AtomicUtf8File.write(CONFIG_PATH, contents, MAX_STATE_FILE_BYTES, DesktopWindowStateStore::validStateJson);
        } catch (IOException | RuntimeException | StackOverflowError exception) {
            DesktopDebug.warn("client window state save failed path={} reason={}", CONFIG_PATH, exception.toString());
        }
    }

    private static boolean validStateJson(String contents) {
        try {
            return GSON.fromJson(contents, StateFile.class) != null;
        } catch (RuntimeException | StackOverflowError exception) {
            return false;
        }
    }

    private static String worldKey(Minecraft minecraft) {
        ServerData server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            try {
                ServerAddress address = ServerAddress.parseString(server.ip);
                String stableKey = StableStateIdentity.server(address.getHost(), address.getPort()).orElse(null);
                return migrateLegacyWorldKey(stableKey, "server:" + server.ip);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        if (minecraft.getSingleplayerServer() != null) {
            String stableKey = StableStateIdentity.singleplayer(
                minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
            ).orElse(null);
            String levelName = minecraft.getSingleplayerServer().getWorldData().getLevelName();
            String legacyKey = levelName == null || levelName.isBlank() ? null : "singleplayer:" + levelName;
            return migrateLegacyWorldKey(stableKey, legacyKey);
        }
        return null;
    }

    private static String migrateLegacyWorldKey(String stableKey, String legacyKey) {
        if (stableKey == null || legacyKey == null || stableKey.equals(legacyKey)) {
            return stableKey;
        }
        StateFile file = stateFile();
        Map<String, WindowState> legacyWorld = file.worlds.remove(legacyKey);
        Map<String, List<String>> legacyLinks = file.links.remove(legacyKey);
        if (legacyWorld == null && legacyLinks == null) {
            return stableKey;
        }

        if (legacyWorld != null) {
            Map<String, WindowState> stableWorld = file.worlds.computeIfAbsent(stableKey, ignored -> new LinkedHashMap<>());
            legacyWorld.forEach(stableWorld::putIfAbsent);
        }
        if (legacyLinks != null) {
            Map<String, List<String>> stableLinks = file.links.computeIfAbsent(stableKey, ignored -> new LinkedHashMap<>());
            legacyLinks.forEach((source, targets) -> {
                if (source == null || targets == null) {
                    return;
                }
                for (String target : targets) {
                    if (target != null) {
                        addLink(stableLinks, source, target);
                    }
                }
            });
        }
        file.normalized();
        write(file);
        DesktopDebug.log("client window state migrated legacy world identity");
        return stableKey;
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
            if (this.localState == null
                || this.localState.isBlank()
                || this.localState.length() > MAX_LOCAL_STATE_LENGTH
                || !hasSafeSnbtStructure(this.localState)) {
                this.localState = "{}";
                return new CompoundTag();
            }

            try {
                return TagParser.parseTag(this.localState);
            } catch (Exception | StackOverflowError exception) {
                DesktopDebug.warn("client window local state parse failed reason={}", exception.toString());
                this.localState = "{}";
                return new CompoundTag();
            }
        }

        void localState(CompoundTag tag) {
            try {
                String encoded = tag == null || tag.isEmpty() ? "{}" : tag.toString();
                this.localState = encoded.length() <= MAX_LOCAL_STATE_LENGTH && hasSafeSnbtStructure(encoded)
                    ? encoded
                    : "{}";
            } catch (RuntimeException | StackOverflowError exception) {
                DesktopDebug.warn("client window local state encode failed reason={}", exception.toString());
                this.localState = "{}";
            }
        }
    }

    private static boolean hasSafeSnbtStructure(String value) {
        char[] containers = new char[MAX_LOCAL_STATE_DEPTH];
        int depth = 0;
        int nodes = 1;
        int delimiters = 0;
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
                continue;
            }
            if (current == '{' || current == '[') {
                if (depth >= MAX_LOCAL_STATE_DEPTH || ++nodes > MAX_LOCAL_STATE_NODES) {
                    return false;
                }
                containers[depth++] = current;
                delimiters++;
            } else if (current == '}' || current == ']') {
                char expected = current == '}' ? '{' : '[';
                if (depth == 0 || containers[--depth] != expected) {
                    return false;
                }
                delimiters++;
            } else if (current == ',') {
                if (++nodes > MAX_LOCAL_STATE_NODES) {
                    return false;
                }
                delimiters++;
            } else if (current == ':') {
                delimiters++;
            }
            if (delimiters > MAX_LOCAL_STATE_DELIMITERS) {
                return false;
            }
        }
        return quote == 0 && !escaped && depth == 0;
    }

    private static final class StateFile {
        int schemaVersion = 3;
        Map<String, WindowState> globalWindows = new LinkedHashMap<>();
        Map<String, Map<String, WindowState>> worlds = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> links = new LinkedHashMap<>();

        private StateFile normalized() {
            this.schemaVersion = 3;
            if (this.globalWindows == null) {
                this.globalWindows = new LinkedHashMap<>();
            }
            if (this.worlds == null) {
                this.worlds = new LinkedHashMap<>();
            }
            if (this.links == null) {
                this.links = new LinkedHashMap<>();
            }

            trimOldest(this.worlds, MAX_WORLDS);
            trimOldest(this.links, MAX_WORLDS);
            this.globalWindows.entrySet().removeIf(window -> window.getKey() == null
                || window.getKey().isBlank()
                || window.getKey().length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH
                || window.getValue() == null);
            trimOldest(this.globalWindows, MAX_WINDOWS_PER_WORLD);

            for (Map.Entry<String, Map<String, WindowState>> entry : List.copyOf(this.worlds.entrySet())) {
                if (entry.getValue() == null) {
                    this.worlds.put(entry.getKey(), new LinkedHashMap<>());
                } else {
                    entry.getValue().entrySet().removeIf(window -> window.getKey() == null
                        || window.getKey().isBlank()
                        || window.getKey().length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH
                        || window.getValue() == null);
                    trimOldest(entry.getValue(), MAX_WINDOWS_PER_WORLD);
                }
            }
            for (Map.Entry<String, Map<String, List<String>>> worldEntry : List.copyOf(this.links.entrySet())) {
                Map<String, List<String>> worldLinks = worldEntry.getValue();
                if (worldLinks == null) {
                    this.links.put(worldEntry.getKey(), new LinkedHashMap<>());
                    continue;
                }
                for (Map.Entry<String, List<String>> linkEntry : List.copyOf(worldLinks.entrySet())) {
                    if (linkEntry.getKey() == null
                        || linkEntry.getKey().isBlank()
                        || linkEntry.getKey().length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH) {
                        worldLinks.remove(linkEntry.getKey());
                        continue;
                    }
                    List<String> values = linkEntry.getValue() == null
                        ? List.of()
                        : linkEntry.getValue().stream()
                            .filter(value -> value != null
                                && !value.isBlank()
                                && value.length() <= DesktopProtocol.MAX_SOURCE_TEXT_LENGTH
                                && !value.equals(linkEntry.getKey()))
                            .distinct()
                            .toList();
                    if (values.isEmpty()) {
                        worldLinks.remove(linkEntry.getKey());
                    } else {
                        worldLinks.put(linkEntry.getKey(), values);
                    }
                }
                BoundedLinkGraph<String> graph = new BoundedLinkGraph<>();
                worldLinks.forEach((source, targets) -> targets.forEach(target -> graph.link(source, target)));
                worldLinks.clear();
                graph.snapshot().forEach((source, targets) -> worldLinks.put(source, List.copyOf(targets)));
            }
            return this;
        }
    }

    private static <K, V> void trimOldest(Map<K, V> values, int maximumSize) {
        while (values.size() > maximumSize) {
            var iterator = values.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }
}
