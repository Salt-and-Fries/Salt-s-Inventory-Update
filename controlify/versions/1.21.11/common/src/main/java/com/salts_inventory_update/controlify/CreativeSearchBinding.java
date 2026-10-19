package com.salts_inventory_update.controlify;

import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.BindContext;
import dev.isxander.controlify.bindings.input.ButtonInput;
import dev.isxander.controlify.controller.input.GamepadInputs;
import net.minecraft.network.chat.Component;

/** Rebindable shortcut, active only over the creative inventory's selected Search tab. */
public final class CreativeSearchBinding {
    public static final InputBindingSupplier SEARCH = ControlifyBindApi.get().registerBinding(builder -> builder
            .id("salts_inventory_controlify", "creative_search")
            .category(Component.translatable("controlify.binding_category.salts_inventory_controlify"))
            .defaultInput(new ButtonInput(GamepadInputs.WEST_BUTTON))
            .allowedContexts(BindContext.CONTAINER));

    private CreativeSearchBinding() { }
    public static void initialize() { }
}
