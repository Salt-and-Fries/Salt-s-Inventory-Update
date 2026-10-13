package com.salts_inventory_update.compat.ae2.nativeview;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyDesktopStyleTest {
    @Test void horizontalTabHitboxesMatchTheNativeArtworkAndFinalFooter() {
        var document = JsonParser.parseString("{\"desktop\":{\"inventoryCut\":89},\"widgets\":{}}").getAsJsonObject();
        var widgets = document.getAsJsonObject("widgets");
        for (int index = 0; index < 4; index++) {
            widgets.add("modeTabButton" + index, JsonParser.parseString("{\"left\":173,\"bottom\":"
                    + (170 - index * 20) + ",\"width\":20,\"height\":" + (index == 3 ? 22 : 20) + "}"));
        }
        LegacyDesktopStyle.prepare(document);
        var prepared = document.deepCopy();
        LegacyDesktopStyle.prepare(document);
        assertEquals(prepared, document);
        for (int index = 0; index < 4; index++) {
            var tab = widgets.getAsJsonObject("modeTabButton" + index);
            assertEquals(22, tab.get("width").getAsInt());
            assertEquals(22, tab.get("height").getAsInt());
            assertEquals(111 + (3 - index) * 21, tab.get("bottom").getAsInt());
        }
    }

    private static com.google.gson.JsonObject style(String title, String widget) {
        return JsonParser.parseString("{\"desktop\":{},\"text\":{\"dialog_title\":{\"text\":{\"translate\":\""
                + title + "\"}}},\"widgets\":{" + widget + "}}").getAsJsonObject();
    }

    @Test void preservesTheVersionSpecificKnifeAndCondenserDefaults() {
        var knife = style("gui.ae2.QuartzCuttingKnife", "\"name\":{\"left\":21,\"top\":30,\"width\":90,\"height\":12}");
        var condenser = style("gui.ae2.Condenser", "\"mode\":{\"left\":128,\"top\":52}");
        for (int repeat = 0; repeat < 2; repeat++) {
            LegacyDesktopStyle.prepare(knife);
            LegacyDesktopStyle.prepare(condenser);
        }
        assertEquals(30, knife.getAsJsonObject("widgets").getAsJsonObject("name").get("top").getAsInt());
        assertEquals(JsonParser.parseString("{\"left\":128,\"top\":52}"), condenser.getAsJsonObject("widgets").get("mode"));
    }

    @Test void preservesCustomFieldBoundsAndUnrelatedNativeWidgets() {
        var changedWidth = style("gui.ae2.QuartzCuttingKnife", "\"name\":{\"left\":21,\"top\":30,\"width\":100,\"height\":12}");
        var unrelatedTitle = style("gui.ae2.Other", "\"name\":{\"left\":21,\"top\":30,\"width\":90,\"height\":12},\"mode\":{\"left\":128,\"top\":52}");
        var sizedMode = style("gui.ae2.Condenser", "\"mode\":{\"left\":128,\"top\":52,\"width\":24}");
        var ordinary = style("gui.ae2.QuartzCuttingKnife", "\"name\":{\"left\":21,\"top\":30,\"width\":90,\"height\":12}");
        ordinary.remove("desktop");
        for (var document : java.util.List.of(changedWidth, unrelatedTitle, sizedMode, ordinary)) {
            var before = document.deepCopy();
            LegacyDesktopStyle.prepare(document);
            assertEquals(before, document);
        }
    }

    @Test void preservesLiteralAndCustomTitleComponentsWithoutRejectingTheirJson() {
        for (String component : java.util.List.of("\"My knife\"", "[\"My knife\"]", "{\"text\":\"My knife\"}", "{\"translate\":null}")) {
            var document = style("gui.ae2.QuartzCuttingKnife", "\"name\":{\"left\":21,\"top\":30,\"width\":90,\"height\":12}");
            document.getAsJsonObject("text").getAsJsonObject("dialog_title").add("text", JsonParser.parseString(component));
            var before = document.deepCopy();
            LegacyDesktopStyle.prepare(document);
            assertEquals(before, document);
        }
    }

    @Test void alignsOldPriorityDefaultsOnlyForDesktopDocuments() {
        var document = JsonParser.parseString("{\"desktop\":{},\"widgets\":{\"openPriority\":{\"left\":154,\"top\":0}}}").getAsJsonObject();
        LegacyDesktopStyle.prepare(document);
        LegacyDesktopStyle.prepare(document);
        assertEquals(JsonParser.parseString("{\"left\":152,\"top\":-5,\"width\":20,\"height\":20}"),
                document.getAsJsonObject("widgets").get("openPriority"));
    }

    @Test void alignsExplicitNativePrioritySizeUsedByInterfaces() {
        var document = JsonParser.parseString("{\"desktop\":{},\"widgets\":{\"openPriority\":{\"left\":154,\"top\":0,\"width\":16,\"height\":16}}}").getAsJsonObject();
        LegacyDesktopStyle.prepare(document);
        LegacyDesktopStyle.prepare(document);
        assertEquals(JsonParser.parseString("{\"left\":152,\"top\":-5,\"width\":20,\"height\":20}"),
                document.getAsJsonObject("widgets").get("openPriority"));
    }

    @Test void preservesNativeDocumentsAndResourcePackOverrides() {
        for (String source : new String[]{
                "{\"widgets\":{\"openPriority\":{\"left\":154,\"top\":0}}}",
                "{\"desktop\":{},\"widgets\":{\"openPriority\":{\"left\":145,\"top\":2}}}",
                "{\"desktop\":{},\"widgets\":{\"openPriority\":{\"left\":154,\"top\":0,\"width\":24,\"height\":16}}}"}) {
            var document = JsonParser.parseString(source).getAsJsonObject();
            var before = document.deepCopy();
            LegacyDesktopStyle.prepare(document);
            assertEquals(before, document);
        }
    }
}
