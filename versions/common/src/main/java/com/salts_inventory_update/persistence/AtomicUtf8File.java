package com.salts_inventory_update.persistence;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/** Same-directory, forced, backup-preserving UTF-8 file persistence. */
public final class AtomicUtf8File {
    private static final ConcurrentHashMap<Path, LockEntry> LOCKS = new ConcurrentHashMap<>();

    private AtomicUtf8File() {
    }

    public static Optional<String> read(Path target, int maximumBytes, Predicate<String> validator) throws IOException {
        Path normalized = normalize(target);
        LockEntry lock = acquire(normalized);
        try {
            requireAbsentOrRegularFile(normalized);
            Optional<String> primary = readCandidate(normalized, maximumBytes, validator);
            if (primary.isPresent()) {
                return primary;
            }
            if (regularFileExists(normalized)) {
                preserveCorrupt(normalized);
            }
            Optional<String> backup = readCandidate(backupPath(normalized), maximumBytes, validator);
            if (backup.isPresent()) {
                try {
                    restorePrimary(normalized, backup.get());
                } catch (IOException ignored) {
                    // The validated backup is still useful even when best-effort self-healing fails.
                }
            }
            return backup;
        } finally {
            release(normalized, lock);
        }
    }

    public static void write(
        Path target,
        String contents,
        int maximumBytes,
        Predicate<String> validator
    ) throws IOException {
        Path normalized = normalize(target);
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        requireLength(bytes.length, maximumBytes);
        if (!validator.test(contents)) {
            throw new IllegalArgumentException("refusing to write invalid data");
        }

        LockEntry lock = acquire(normalized);
        Path temporary = temporarySibling(normalized, ".tmp-");
        try {
            Path parent = normalized.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            requireAbsentOrRegularFile(normalized);
            writeForced(temporary, bytes);

            Optional<String> existing = readCandidate(normalized, maximumBytes, validator);
            if (existing.isPresent()) {
                Path backupTemporary = temporarySibling(normalized, ".bak-tmp-");
                try {
                    writeForced(backupTemporary, existing.get().getBytes(StandardCharsets.UTF_8));
                    moveReplacing(backupTemporary, backupPath(normalized));
                } finally {
                    Files.deleteIfExists(backupTemporary);
                }
            } else if (regularFileExists(normalized)) {
                preserveCorrupt(normalized);
            }

            requireAbsentOrRegularFile(normalized);
            moveReplacing(temporary, normalized);
            forceDirectory(normalized.getParent());
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } finally {
                release(normalized, lock);
            }
        }
    }

    private static Optional<String> readCandidate(
        Path path,
        int maximumBytes,
        Predicate<String> validator
    ) throws IOException {
        if (maximumBytes <= 0) {
            throw new IllegalArgumentException("maximumBytes must be positive");
        }
        Optional<BasicFileAttributes> attributes = attributesIfPresent(path);
        if (attributes.isEmpty() || !attributes.get().isRegularFile()) {
            return Optional.empty();
        }
        long length = attributes.get().size();
        if (length < 0L || length > maximumBytes) {
            return Optional.empty();
        }

        byte[] buffer = new byte[Math.min(8 * 1024, maximumBytes)];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) Math.min(length, 8 * 1024L));
        try (InputStream input = Files.newInputStream(path)) {
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                total = Math.addExact(total, read);
                if (total > maximumBytes) {
                    return Optional.empty();
                }
                bytes.write(buffer, 0, read);
            }
        }

        String contents;
        try {
            contents = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes.toByteArray()))
                .toString();
        } catch (CharacterCodingException ignored) {
            return Optional.empty();
        }
        return validator.test(contents) ? Optional.of(contents) : Optional.empty();
    }

    private static void writeForced(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static void restorePrimary(Path target, String contents) throws IOException {
        requireAbsentOrRegularFile(target);
        Path temporary = temporarySibling(target, ".restore-");
        try {
            writeForced(temporary, contents.getBytes(StandardCharsets.UTF_8));
            requireAbsentOrRegularFile(target);
            moveReplacing(temporary, target);
            forceDirectory(target.getParent());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void preserveCorrupt(Path target) throws IOException {
        requireAbsentOrRegularFile(target);
        String suffix = ".corrupt-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
        moveReplacing(target, target.resolveSibling(target.getFileName() + suffix));
    }

    private static void requireAbsentOrRegularFile(Path target) throws IOException {
        Optional<BasicFileAttributes> attributes = attributesIfPresent(target);
        if (attributes.isPresent() && !attributes.get().isRegularFile()) {
            throw new IOException("target exists but is not a regular file: " + target);
        }
    }

    private static boolean regularFileExists(Path target) throws IOException {
        return attributesIfPresent(target).map(BasicFileAttributes::isRegularFile).orElse(false);
    }

    private static Optional<BasicFileAttributes> attributesIfPresent(Path target) throws IOException {
        try {
            return Optional.of(Files.readAttributes(
                target,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS
            ));
        } catch (NoSuchFileException ignored) {
            return Optional.empty();
        }
    }

    private static LockEntry acquire(Path target) {
        LockEntry entry = LOCKS.compute(target, (ignored, existing) -> {
            LockEntry selected = existing == null ? new LockEntry() : existing;
            selected.references++;
            return selected;
        });
        entry.lock.lock();
        return entry;
    }

    private static void release(Path target, LockEntry entry) {
        entry.lock.unlock();
        LOCKS.computeIfPresent(target, (ignored, current) -> {
            if (current != entry) {
                return current;
            }
            current.references--;
            return current.references == 0 ? null : current;
        });
    }

    private static Path backupPath(Path target) {
        return target.resolveSibling(target.getFileName() + ".bak");
    }

    private static Path temporarySibling(Path target, String marker) {
        return target.resolveSibling(target.getFileName() + marker + UUID.randomUUID());
    }

    private static Path normalize(Path target) {
        if (target == null || target.getFileName() == null) {
            throw new IllegalArgumentException("target must name a file");
        }
        return target.toAbsolutePath().normalize();
    }

    private static void requireLength(long length, int maximumBytes) throws IOException {
        if (maximumBytes <= 0 || length < 0L || length > maximumBytes) {
            throw new IOException("file length " + length + " exceeds limit " + maximumBytes);
        }
    }

    private static void forceDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // The file itself is already forced; some platforms cannot force directories.
        }
    }

    private static final class LockEntry {
        private final ReentrantLock lock = new ReentrantLock();
        private int references;
    }
}
