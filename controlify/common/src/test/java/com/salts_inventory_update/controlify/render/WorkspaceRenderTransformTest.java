package com.salts_inventory_update.controlify.render;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WorkspaceRenderTransformTest {
    @Test void scopeEndsBeforeUnrelatedGuiRendering() {
        assertFalse(WorkspaceRenderTransform.active());
        try (var ignored = WorkspaceRenderTransform.enter()) {
            assertTrue(WorkspaceRenderTransform.active());
        }
        assertFalse(WorkspaceRenderTransform.active());
    }

    @Test void nestedNativeRenderScopesDoNotEndTheOuterWindowScope() {
        try (var outer = WorkspaceRenderTransform.enter()) {
            try (var inner = WorkspaceRenderTransform.enter()) {
                assertTrue(WorkspaceRenderTransform.active());
            }
            assertTrue(WorkspaceRenderTransform.active());
        }
        assertFalse(WorkspaceRenderTransform.active());
    }

    @Test void failedNativeRenderingStillRestoresOrdinaryClipping() {
        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = WorkspaceRenderTransform.enter()) {
                throw new IllegalStateException("render failed");
            }
        });
        assertFalse(WorkspaceRenderTransform.active());
    }

    @Test void closeIsIdempotentAndOtherThreadsRemainOutsideTheScope() throws InterruptedException {
        var scope = WorkspaceRenderTransform.enter();
        AtomicBoolean otherThreadActive = new AtomicBoolean(true);
        Thread thread = new Thread(() -> otherThreadActive.set(WorkspaceRenderTransform.active()));
        thread.start();
        thread.join();
        assertFalse(otherThreadActive.get());
        assertTrue(WorkspaceRenderTransform.active());
        scope.close();
        scope.close();
        assertFalse(WorkspaceRenderTransform.active());
    }
}
