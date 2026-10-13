package com.salts_inventory_update.compat.ae2;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DesktopResourceFidelityTest {
    private static byte[] resource(String path) throws Exception {
        try (var stream = DesktopResourceFidelityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing desktop resource: " + path);
            return stream.readAllBytes();
        }
    }

    @Test void everyDesktopProfileMatchesTheForkExactly() throws Exception {
        var manifest = JsonParser.parseString(new String(resource("META-INF/ae2-desktop-reference.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        var styles = manifest.getAsJsonObject("styles");
        assertEquals(42, styles.size());
        for (var entry : styles.entrySet()) {
            byte[] data = resource(entry.getKey());
            assertEquals(entry.getValue().getAsString(),
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)), entry.getKey());
            var style = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(style.has("desktop"), entry.getKey());
        }
    }

    @Test void translationsContainOnlyTheForksNewKeys() throws Exception {
        var manifest = JsonParser.parseString(new String(resource("META-INF/ae2-desktop-reference.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        byte[] data = resource("assets/ae2/lang/en_us.json");
        assertEquals(manifest.get("translationSha256").getAsString(),
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
        var language = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(59, language.size());
        for (var key : manifest.getAsJsonArray("translationKeys")) {
            assertTrue(language.has(key.getAsString()));
        }
        assertFalse(language.has("item.ae2.certus_quartz_crystal"));
    }
}
