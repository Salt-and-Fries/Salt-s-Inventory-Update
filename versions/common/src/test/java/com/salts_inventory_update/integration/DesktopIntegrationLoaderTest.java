package com.salts_inventory_update.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext.Phase;
import com.salts_inventory_update.integration.DesktopIntegrationLoader.Source;

class DesktopIntegrationLoaderTest {
    @TempDir Path directory;
    private Path classes;
    private Path descriptor;

    @BeforeEach
    void compileExternalFixture() throws Exception {
        Path repository = Path.of(System.getProperty("user.dir"));
        while (!Files.isDirectory(repository.resolve("api-examples/optional-mod"))) {
            repository = repository.getParent();
            assertNotNull(repository, "Run tests from inside the repository");
        }
        Path fixture = repository.resolve("api-examples/optional-mod/src/main");
        classes = Files.createDirectory(directory.resolve("classes"));
        descriptor = fixture.resolve("resources").resolve(DesktopIntegrationContext.DESCRIPTOR_PATH);
        String apiPath = Path.of(DesktopIntegration.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        java.util.ArrayList<String> arguments = new java.util.ArrayList<>(List.of("-classpath", apiPath, "-d", classes.toString()));
        try (Stream<Path> paths = Files.walk(fixture.resolve("java"))) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> arguments.add(path.toString()));
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)),
            "External fixture must compile against the public integration API");
        System.clearProperty("salts.example.normal");
        System.clearProperty("salts.example.common");
        System.clearProperty("salts.example.client");
    }

    @Test
    void sameExternalModStartsWithoutSaltOnItsRuntimeClasspath() throws Exception {
        try (URLClassLoader absent = new URLClassLoader(new URL[]{classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            assertThrows(ClassNotFoundException.class, () -> absent.loadClass(DesktopIntegration.class.getName()));
            absent.loadClass("example.optional.ExampleMod").getMethod("initialize").invoke(null);
            assertEquals("ready", System.getProperty("salts.example.normal"));
            assertNull(System.getProperty("salts.example.common"));
            assertNull(System.getProperty("salts.example.client"));
        }
    }

    @Test
    void presentSaltRegistersProvidersOnceInTheirSeparatePhases() throws Exception {
        try (URLClassLoader present = new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            DesktopIntegrationLoader loader = new DesktopIntegrationLoader();
            loader.load(List.of(new Source("example_optional", descriptor)), Phase.COMMON, present, id -> true);
            assertEquals("example_optional:COMMON", System.getProperty("salts.example.common"));
            assertNull(System.getProperty("salts.example.client"));
            loader.load(List.of(new Source("example_optional", descriptor)), Phase.CLIENT, present, id -> true);
            assertEquals("example_optional:CLIENT", System.getProperty("salts.example.client"));
            System.clearProperty("salts.example.common");
            loader.load(List.of(new Source("example_optional", descriptor)), Phase.COMMON, present, id -> true);
            assertNull(System.getProperty("salts.example.common"), "World restarts must not duplicate registration");
        }
    }

    @Test
    void dedicatedServerNeverResolvesTheClientProvider() throws Exception {
        AtomicInteger forbiddenLoads = new AtomicInteger();
        try (URLClassLoader server = new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("example.optional.ExampleClientIntegration") || name.startsWith("net.minecraft.client.")
                    || name.startsWith("com.salts_inventory_update.api.client.")) {
                    forbiddenLoads.incrementAndGet();
                    throw new ClassNotFoundException("Client classes are absent on a dedicated server");
                }
                return super.loadClass(name, resolve);
            }
        }) {
            new DesktopIntegrationLoader().load(List.of(new Source("example_optional", descriptor)), Phase.COMMON, server, id -> true);
            assertEquals("example_optional:COMMON", System.getProperty("salts.example.common"));
            assertEquals(0, forbiddenLoads.get());
        }
    }

    @Test
    void missingPrerequisiteSkipsProvidersBeforeResolvingTheirClasses() throws Exception {
        Path gated = directory.resolve("gated.properties");
        Files.writeString(gated, Files.readString(descriptor) + "\nrequires=absent_dependency\n");
        ClassLoader trap = new ClassLoader() {
            @Override public Class<?> loadClass(String name) { fail("Skipped integration resolved " + name); return null; }
        };
        new DesktopIntegrationLoader().load(List.of(new Source("example_optional", gated)), Phase.COMMON, trap,
            id -> !id.equals("absent_dependency"));
        assertTrue(DesktopDiagnostics.snapshot().stream().anyMatch(event -> event.message().contains("absent_dependency")));
    }

    @Test
    void descriptorOwnerRunsOnceWhenJarContainsSeveralModRecords() throws Exception {
        try (URLClassLoader present = new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            new DesktopIntegrationLoader().load(List.of(new Source("another_mod", descriptor), new Source("example_optional", descriptor)),
                Phase.COMMON, present, id -> true);
            assertEquals("example_optional:COMMON", System.getProperty("salts.example.common"));
        }
    }

    @Test
    void incompatibleApiFailsWithoutResolvingProvider() throws Exception {
        Path future = directory.resolve("future.properties");
        Files.writeString(future, Files.readString(descriptor).replace("apiVersion=6", "apiVersion=999"));
        new DesktopIntegrationLoader().load(List.of(new Source("example_optional", future)), Phase.COMMON,
            ClassLoader.getPlatformClassLoader(), id -> true);
        assertNull(System.getProperty("salts.example.common"));
        assertTrue(DesktopDiagnostics.snapshot().stream().anyMatch(event -> event.message().contains("API 999")));
    }

    @Test
    void failedRegistrationRollsBackInReverseOrder() {
        java.util.ArrayList<String> state = new java.util.ArrayList<>();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> DesktopRegistrationScope.run("broken", () -> {
            state.add("first");
            DesktopRegistrationScope.onRollback(() -> state.remove(state.size() - 1));
            state.add("second");
            DesktopRegistrationScope.onRollback(() -> { assertEquals("second", state.get(1)); state.remove(1); });
            throw new RuntimeException("failed after partial registration");
        }));
        assertEquals("failed after partial registration", failure.getMessage());
        assertTrue(state.isEmpty());
    }
}
