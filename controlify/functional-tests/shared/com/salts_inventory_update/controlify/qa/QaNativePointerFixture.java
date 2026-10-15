package com.salts_inventory_update.controlify.qa;

import dev.isxander.controlify.controller.ControllerEntity;

import java.util.Objects;
import java.util.function.Consumer;

/** Test-only cursor preparation at the genuine native container binding-consumption edge. */
public final class QaNativePointerFixture {
    private static Consumer<ControllerEntity> prepare;

    private QaNativePointerFixture() { }

    public static void register(Consumer<ControllerEntity> callback) {
        prepare = Objects.requireNonNull(callback, "callback");
    }

    public static void beforeNativeBindings(ControllerEntity controller) {
        if (prepare != null) prepare.accept(controller);
    }
}
