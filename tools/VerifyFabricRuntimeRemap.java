import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.RuntimeModRemapper;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.VersionOverrides;

/** Reproduce the installed Fabric Loader's transformation, without launching Minecraft. */
public final class VerifyFabricRuntimeRemap {
    private record Pair(Path original, Path actual, ModCandidateImpl candidate) {}

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static Path file(String value) throws Exception {
        Path path = Path.of(value).toRealPath();
        require(Files.isRegularFile(path), "Not a regular file: " + path);
        return path;
    }

    private static String digest(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static LoaderModMetadata metadata(Path jar, VersionOverrides versions,
                                               DependencyOverrides dependencies) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            require(entry != null && !entry.isDirectory(), "Missing fabric.mod.json: " + jar);
            try (InputStream input = zip.getInputStream(entry)) {
                return ModMetadataParser.parseMetadata(input, jar.toString(), List.of(), versions,
                    dependencies, true);
            }
        }
    }

    private static Map<String, byte[]> members(Path path) throws Exception {
        Map<String, byte[]> output = new TreeMap<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                require(!output.containsKey(entry.getName()), "Duplicate archive member: " + path + "!/" + entry.getName());
                try (InputStream input = zip.getInputStream(entry)) {
                    output.put(entry.getName(), input.readAllBytes());
                }
            }
        }
        return output;
    }

    public static void main(String[] args) throws Exception {
        require(args.length >= 5 && (args.length - 3) % 2 == 0,
            "Usage: VerifyFabricRuntimeRemap mappings.jar remapClasspath.txt emptyScratch original.jar actual.jar [...]");
        Path mappings = file(args[0]);
        Path classpath = file(args[1]);
        Path scratch = Path.of(args[2]).toAbsolutePath().normalize();
        Files.createDirectories(scratch);
        try (var contents = Files.list(scratch)) {
            require(contents.findAny().isEmpty(), "Scratch directory must be empty: " + scratch);
        }
        scratch = scratch.toRealPath();
        Path config = Files.createDirectory(scratch.resolve("config"));
        Path temporary = Files.createDirectory(scratch.resolve("temporary"));
        Path output = Files.createDirectory(scratch.resolve("output"));

        // Explicit namespaces avoid consulting a game provider. MappingConfiguration itself
        // still reads the actual native mapping JAR and applies Loader's mapping filters.
        System.setProperty("fabric.development", "true");
        System.setProperty("fabric.mappingPath", mappings.toString());
        System.setProperty("fabric.runtimeMappingNamespace", "named");
        System.setProperty("fabric.defaultModDistributionNamespace", "intermediary");
        System.setProperty("fabric.defaultMixinRemapType", "mixin");
        System.setProperty("fabric.remapClasspathFile", classpath.toString());
        MappingConfiguration mapping = new MappingConfiguration();
        FabricLauncher launcher = (FabricLauncher) Proxy.newProxyInstance(
            FabricLauncher.class.getClassLoader(), new Class<?>[]{FabricLauncher.class},
            (proxy, method, values) -> {
                return switch (method.getName()) {
                    case "getMappingConfiguration" -> mapping;
                    case "isDevelopment" -> true;
                    case "toString" -> "Fabric runtime lineage verification launcher";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == values[0];
                    default -> throw new UnsupportedOperationException("Unexpected native launcher call: " + method);
                };
            });
        FabricLauncherBase.setLauncher(launcher);
        require(mapping.hasAnyMappings() && "intermediary".equals(mapping.getDefaultModDistributionNamespace())
            && "named".equals(mapping.getRuntimeNamespace()), "Unexpected native runtime mappings");
        VersionOverrides versions = new VersionOverrides();
        DependencyOverrides dependencies = new DependencyOverrides(config);
        require(versions.getAffectedModIds().isEmpty() && dependencies.getAffectedModIds().isEmpty(),
            "Verification cannot apply metadata overrides");
        Method create = ModCandidateImpl.class.getDeclaredMethod("createPlain", List.class,
            LoaderModMetadata.class, boolean.class, Collection.class);
        create.setAccessible(true);
        List<Pair> pairs = new ArrayList<>();
        List<ModCandidateImpl> candidates = new ArrayList<>();
        Map<Path, String> unchanged = new LinkedHashMap<>();
        unchanged.put(mappings, digest(mappings));
        unchanged.put(classpath, digest(classpath));
        Set<String> selectedIds = new HashSet<>();
        for (int index = 3; index < args.length; index += 2) {
            Path original = file(args[index]);
            Path actual = file(args[index + 1]);
            require(!original.startsWith(scratch) && !actual.startsWith(scratch) && !original.equals(actual),
                "Verification inputs must be separate from scratch outputs");
            LoaderModMetadata metadata = metadata(original, versions, dependencies);
            LoaderModMetadata actualMetadata = metadata(actual, versions, dependencies);
            require(selectedIds.add(metadata.getId()), "Duplicate selected mod ID: " + metadata.getId());
            require(metadata.getId().equals(actualMetadata.getId())
                && metadata.getVersion().equals(actualMetadata.getVersion()),
                "Original/processed metadata identity mismatch: " + original + " / " + actual);
            ModCandidateImpl candidate = (ModCandidateImpl) create.invoke(null, List.of(original),
                metadata, true, List.of());
            candidates.add(candidate);
            pairs.add(new Pair(original, actual, candidate));
            unchanged.put(original, digest(original));
            unchanged.put(actual, digest(actual));
        }

        // Invoke the installed native implementation as one batch. This includes its exact
        // TinyRemapper, selected-input hierarchy, mixin policy and merged class tweakers.
        RuntimeModRemapper.remap(candidates, temporary, output);
        long count = 0;
        for (Pair pair : pairs) {
            List<Path> generated = pair.candidate().getPaths();
            require(generated.size() == 1, "Unexpected native remap output count: " + pair.original());
            Path expected = generated.get(0).toRealPath();
            require(expected.getParent().equals(output), "Native output escaped scratch: " + expected);
            Map<String, byte[]> reproduced = members(expected);
            Map<String, byte[]> actual = members(pair.actual());
            require(reproduced.keySet().equals(actual.keySet()),
                "Native remap member inventory differs: " + pair.actual());
            for (String name : reproduced.keySet()) {
                require(Arrays.equals(reproduced.get(name), actual.get(name)),
                    "Native remap member bytes differ: " + pair.actual() + "!/" + name);
                count++;
            }
        }
        for (var input : unchanged.entrySet()) {
            require(input.getValue().equals(digest(input.getKey())), "Verification input changed: " + input.getKey());
        }
        System.out.println("FABRIC_LINEAGE_VERIFIED count=" + pairs.size() + " members=" + count);
    }
}
