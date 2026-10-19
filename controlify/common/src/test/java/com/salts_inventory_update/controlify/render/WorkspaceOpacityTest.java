package com.salts_inventory_update.controlify.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceOpacityTest {
    @Test void nestedScopeMultipliesAndRestoresAfterException() {
        assertEquals(1, WorkspaceOpacity.current());
        try (var outer = WorkspaceOpacity.enter(0.5f)) {
            assertThrows(IllegalStateException.class, () -> {
                try (var inner = WorkspaceOpacity.enter(0.2f)) {
                    assertEquals(0.1f, WorkspaceOpacity.current(), 0.00001f);
                    throw new IllegalStateException();
                }
            });
            assertEquals(0.5f, WorkspaceOpacity.current());
        }
        assertEquals(1, WorkspaceOpacity.current());
    }

    @Test void capturesIdentityAndPropagatesTextItemAndPipConversionWithoutDoubleFade() {
        var capture = new WorkspaceOpacity.Capture();
        Object text = new String("same"), item = new String("same"), glyph = new Object();
        try (var scope = WorkspaceOpacity.enter(0.25f)) { capture.record(text); }
        try (var scope = WorkspaceOpacity.enter(0.75f)) { capture.record(item); }
        assertEquals(0.25f, capture.opacity(text));
        assertEquals(0.75f, capture.opacity(item));
        assertEquals(1, capture.opacity(new String("same")));
        try (var unrelated = WorkspaceOpacity.enter(0.4f)) {
            capture.wrap(state -> {
                assertEquals(0.25f, WorkspaceOpacity.current());
                capture.record(glyph);
            }).accept(text);
            assertEquals(0.4f, WorkspaceOpacity.current());
        }
        assertEquals(0.25f, capture.opacity(glyph));
        capture.wrap(state -> assertEquals(0.75f, WorkspaceOpacity.current())).accept(item);
        assertEquals(1, WorkspaceOpacity.current());
    }

    @Test void conversionExceptionAndFrameResetDoNotLeakOpacity() {
        var capture = new WorkspaceOpacity.Capture();
        Object picture = new Object();
        try (var scope = WorkspaceOpacity.enter(0.3f)) { capture.record(picture); }
        assertThrows(IllegalStateException.class, () -> capture.wrap(state -> {
            assertEquals(0.3f, WorkspaceOpacity.current()); throw new IllegalStateException();
        }).accept(picture));
        assertEquals(1, WorkspaceOpacity.current());
        capture.clear();
        assertEquals(1, capture.opacity(picture));
        try (var scope = WorkspaceOpacity.enter(0.3f)) { capture.record(picture); }
        capture.record(picture);
        assertEquals(1, capture.opacity(picture));
    }

    @Test void premultipliedTextureScalesRgbAndAlphaWhileOrdinaryColorPreservesRgb() {
        assertEquals(0x40201008, WorkspaceOpacity.color(0x80402010, 0.5f, true));
        assertEquals(0x40402010, WorkspaceOpacity.color(0x80402010, 0.5f, false));
        assertEquals(0, WorkspaceOpacity.color(0xffabcdef, 0, true));
        assertEquals(0x00abcdef, WorkspaceOpacity.color(0xffabcdef, 0, false));
        assertEquals(0xffabcdef, WorkspaceOpacity.color(0xffabcdef, 1, true));
    }

    @Test void opacityIsBoundedAndDoesNotCrossThreads() throws Exception {
        try (var scope = WorkspaceOpacity.enter(0.2f)) {
            float[] observed = { -1 };
            var thread = new Thread(() -> observed[0] = WorkspaceOpacity.current());
            thread.start(); thread.join();
            assertEquals(1, observed[0]);
        }
        try (var scope = WorkspaceOpacity.enter(Float.NaN)) { assertEquals(1, WorkspaceOpacity.current()); }
        try (var scope = WorkspaceOpacity.enter(-1)) { assertEquals(0, WorkspaceOpacity.current()); }
        try (var scope = WorkspaceOpacity.enter(2)) { assertEquals(1, WorkspaceOpacity.current()); }
        var scope = WorkspaceOpacity.enter(0.5f);
        scope.close(); scope.close();
        assertEquals(1, WorkspaceOpacity.current());
    }
}
