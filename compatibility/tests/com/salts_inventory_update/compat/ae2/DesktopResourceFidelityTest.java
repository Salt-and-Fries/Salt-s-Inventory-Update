package com.salts_inventory_update.compat.ae2;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DesktopResourceFidelityTest {
    private static byte[] resource(String path) throws Exception {
        try (var stream = DesktopResourceFidelityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing desktop resource: " + path);
            return stream.readAllBytes();
        }
    }

    @Test void everyApprovedDesktopProfileKeepsItsNativeParentAndLayoutContract() throws Exception {
        var manifest = JsonParser.parseString(new String(resource("META-INF/ae2-desktop-reference.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        var styles = manifest.getAsJsonObject("styles");
        assertEquals(42, styles.size());
        for (var entry : styles.entrySet()) {
            byte[] data = resource(entry.getKey());
            var style = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            // The fork hashes are historical provenance, not a snapshot of the
            // later approved native-layout overhaul. Test the maintained contract.
            assertEquals(64, entry.getValue().getAsString().length());
            var desktop = style.getAsJsonObject("desktop");
            assertNotNull(desktop, entry.getKey());
            assertTrue(desktop.get("preserveAeLayout").getAsBoolean(), entry.getKey());
            assertTrue(desktop.get("showTitle").getAsBoolean(), entry.getKey());
            assertTrue(desktop.get("width").getAsInt() >= 60 && desktop.get("width").getAsInt() <= 4096, entry.getKey());
            assertTrue(desktop.get("height").getAsInt() >= 40 && desktop.get("height").getAsInt() <= 4096, entry.getKey());
            assertTrue(Set.of("fixed", "list", "grid").contains(desktop.get("resize").getAsString()), entry.getKey());
            assertTrue(desktop.get("inventoryCut").getAsInt() >= 0, entry.getKey());
            String parent = entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1);
            parent = parent.equals("toolbox_companion.json") ? "../common/palette.json"
                : (entry.getKey().contains("/terminals/") ? "../../terminals/" : "../") + parent;
            assertEquals(1, style.getAsJsonArray("includes").size(), entry.getKey());
            assertEquals(parent, style.getAsJsonArray("includes").get(0).getAsString(), entry.getKey());
        }
    }

    @Test void translationsContainTheForkAndApprovedAddonKeysOnly() throws Exception {
        var manifest = JsonParser.parseString(new String(resource("META-INF/ae2-desktop-reference.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        byte[] data = resource("assets/ae2/lang/en_us.json");
        var language = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        var expected = new HashSet<String>();
        for (var key : manifest.getAsJsonArray("translationKeys")) expected.add(key.getAsString());
        expected.add("gui.ae2.DesktopWindow.wireless_requester_terminal");
        expected.add("gui.ae2.DesktopWindow.portable_mana_cell");
        assertEquals(expected, language.keySet(), "Keep additions scoped to SIU's addon window titles");
        for (var key : manifest.getAsJsonArray("translationKeys")) {
            assertTrue(language.has(key.getAsString()));
        }
        assertFalse(language.has("item.ae2.certus_quartz_crystal"));
    }
}
