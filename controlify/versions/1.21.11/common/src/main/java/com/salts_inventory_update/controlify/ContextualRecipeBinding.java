package com.salts_inventory_update.controlify;

import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.BindContext;
import dev.isxander.controlify.bindings.input.AxisInput;
import dev.isxander.controlify.controller.input.GamepadInputs;
import net.minecraft.network.chat.Component;

/** A real configurable action shared by the wheel input and its displayed controller glyph. */
public final class ContextualRecipeBinding {
    public static final InputBindingSupplier OPEN_WHEEL = ControlifyBindApi.get().registerBinding(builder -> builder
            .id("salts_inventory_controlify", "contextual_recipe_wheel")
            .category(Component.translatable("controlify.binding_category.salts_inventory_controlify"))
            .defaultInput(new AxisInput(GamepadInputs.RIGHT_TRIGGER_AXIS))
            .allowedContexts(BindContext.CONTAINER));

    private ContextualRecipeBinding() { }
    public static void initialize() { /* Force registration before any controller builds its bindings. */ }
}
