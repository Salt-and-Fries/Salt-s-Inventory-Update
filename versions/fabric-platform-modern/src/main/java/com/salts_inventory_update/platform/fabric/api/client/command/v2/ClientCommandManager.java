package com.salts_inventory_update.platform.fabric.api.client.command.v2;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class ClientCommandManager {
    private ClientCommandManager() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String literal) {
        return Commands.literal(literal);
    }
}
