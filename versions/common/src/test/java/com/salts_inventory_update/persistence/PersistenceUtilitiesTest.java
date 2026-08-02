package com.salts_inventory_update.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenceUtilitiesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void stateKeysNormalizeHostButPreservePortAndSaveRoot() throws Exception {
        assertEquals(
            StableStateIdentity.server("Example.COM.", 25565),
            StableStateIdentity.server("example.com", 25565)
        );
        assertNotEquals(
            StableStateIdentity.server("example.com", 25565),
            StableStateIdentity.server("example.com", 25566)
        );
        assertEquals(
            StableStateIdentity.server("[2001:0db8:0:0:0:0:0:1]", 25565),
            StableStateIdentity.server("2001:db8::1", 25565)
        );
        assertEquals(
            StableStateIdentity.server("192.0.2.128", 25565),
            StableStateIdentity.server("[::ffff:192.0.2.128]", 25565)
        );
        assertEquals(
            StableStateIdentity.server("192.0.2.128", 25565),
            StableStateIdentity.server("::ffff:c000:0280", 25565)
        );

        Path first = Files.createDirectories(temporaryDirectory.resolve("one/world"));
        Path second = Files.createDirectories(temporaryDirectory.resolve("two/world"));
        assertNotEquals(StableStateIdentity.singleplayer(first), StableStateIdentity.singleplayer(second));
        assertFalse(StableStateIdentity.server(" ", 25565).isPresent());
        assertFalse(StableStateIdentity.server("not:ipv6", 25565).isPresent());
    }

    @Test
    void atomicWriteRecoversLastGoodBackupAndPreservesCorruption() throws Exception {
        Path target = temporaryDirectory.resolve("state.json");
        AtomicUtf8File.write(target, "valid-one", 128, text -> text.startsWith("valid"));
        AtomicUtf8File.write(target, "valid-two", 128, text -> text.startsWith("valid"));

        Files.writeString(target, "corrupt", StandardCharsets.UTF_8);
        assertEquals("valid-one", AtomicUtf8File.read(target, 128, text -> text.startsWith("valid")).orElseThrow());
        assertEquals("valid-one", Files.readString(target));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test
    void oversizedPrimaryStillFallsBackWithoutParsing() throws Exception {
        Path target = temporaryDirectory.resolve("config.json");
        AtomicUtf8File.write(target, "valid-backup", 128, text -> text.startsWith("valid"));
        AtomicUtf8File.write(target, "valid-current", 128, text -> text.startsWith("valid"));
        Files.write(target, new byte[256]);

        assertEquals("valid-backup", AtomicUtf8File.read(target, 128, text -> text.startsWith("valid")).orElseThrow());
    }

    @Test
    void malformedUtf8IsNeverPassedToTheValidator() throws Exception {
        Path target = temporaryDirectory.resolve("malformed.json");
        Files.write(target, new byte[] {(byte) 0xC3, (byte) 0x28});

        assertTrue(AtomicUtf8File.read(target, 128, ignored -> {
            throw new AssertionError("validator must not receive malformed UTF-8");
        }).isEmpty());
    }

    @Test
    void directoryTargetsAreRejectedWithoutMovingOrReplacingThem() throws Exception {
        Path target = Files.createDirectory(temporaryDirectory.resolve("state.json"));
        Path marker = Files.writeString(target.resolve("keep.txt"), "untouched", StandardCharsets.UTF_8);

        assertThrows(
            java.io.IOException.class,
            () -> AtomicUtf8File.read(target, 128, text -> true)
        );
        assertThrows(
            java.io.IOException.class,
            () -> AtomicUtf8File.write(target, "valid", 128, text -> true)
        );

        assertTrue(Files.isDirectory(target));
        assertEquals("untouched", Files.readString(marker, StandardCharsets.UTF_8));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test
    void validBackupIsReturnedWhenBestEffortRestorationFails() throws Exception {
        Path target = temporaryDirectory.resolve("restore.json");
        Files.writeString(
            target.resolveSibling(target.getFileName() + ".bak"),
            "valid-backup",
            StandardCharsets.UTF_8
        );

        assertEquals(
            "valid-backup",
            AtomicUtf8File.read(target, 128, text -> {
                assertTrue(target.toFile().mkdir(), "test must obstruct primary restoration");
                return text.startsWith("valid");
            }).orElseThrow()
        );
        assertTrue(Files.isDirectory(target));
    }
}
