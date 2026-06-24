package net.fabricmc.fabric.api.client.command.v2;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@FunctionalInterface
public interface ClientCommandRegistrationCallback {
    Event EVENT = new Event();

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess);

    final class Event {
        private final List<ClientCommandRegistrationCallback> callbacks = new ArrayList<>();

        public void register(ClientCommandRegistrationCallback callback) {
            callbacks.add(callback);
        }

        public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            for (ClientCommandRegistrationCallback callback : callbacks) {
                callback.register(event.getDispatcher(), event.getBuildContext());
            }
        }
    }
}
