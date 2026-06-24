package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class ClientCommands {
    private ClientCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String literal) {
        return Commands.literal(literal);
    }
}
