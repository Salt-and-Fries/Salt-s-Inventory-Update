package com.salts_inventory_update.persistence;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.IDN;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/** Stable, non-identifying keys for client window-state partitions. */
public final class StableStateIdentity {
    private StableStateIdentity() {
    }

    public static Optional<String> singleplayer(Path saveRoot) {
        if (saveRoot == null) {
            return Optional.empty();
        }
        Path normalized;
        try {
            normalized = saveRoot.toRealPath();
        } catch (IOException ignored) {
            normalized = saveRoot.toAbsolutePath().normalize();
        }
        String value = normalized.toString();
        if (isWindows()) {
            value = value.toLowerCase(Locale.ROOT);
        }
        return Optional.of("singleplayer:v2:" + sha256(value));
    }

    public static Optional<String> server(String host, int effectivePort) {
        if (host == null || effectivePort < 1 || effectivePort > 65_535) {
            return Optional.empty();
        }
        String normalized = host.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.contains(":")) {
            try {
                InetAddress address = InetAddress.getByName(normalized);
                // The JDK intentionally returns Inet4Address for IPv4-mapped
                // IPv6 literals. Accept that representation so mapped and
                // direct forms partition state under the same stable key.
                if (!(address instanceof Inet6Address) && address.getAddress().length != 4) {
                    return Optional.empty();
                }
                normalized = address.getHostAddress();
            } catch (UnknownHostException ignored) {
                return Optional.empty();
            }
        } else {
            try {
                normalized = IDN.toASCII(normalized, IDN.USE_STD3_ASCII_RULES);
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return Optional.of("server:v2:" + sha256(normalized + ":" + effectivePort));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }
}
