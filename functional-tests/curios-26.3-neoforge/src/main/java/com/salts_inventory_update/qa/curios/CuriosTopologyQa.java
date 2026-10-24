package com.salts_inventory_update.qa.curios;

import java.nio.file.Files;
import java.nio.file.Path;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import com.salts_inventory_update.compat.curios.CuriosDesktopMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Opt-in server-side capacity fixture. It never enters either production JAR. */
@Mod("salts_curios_qa")
public final class CuriosTopologyQa {
    private static final Identifier QA = Identifier.fromNamespaceAndPath("salts_curios_qa", "capacity");
    public CuriosTopologyQa() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            String configured = System.getProperty("salts.curios.qa.directory");
            if (configured == null) return;
            Path directory = Path.of(configured), command = directory.resolve("capacity-command.txt");
            if (!Files.isRegularFile(command)) return;
            try {
                String action = Files.readString(command).trim();
                for (var player : event.getServer().getPlayerList().getPlayers()) {
                    if (!DesktopSessions.hasOpenSessionMatching(player, menu -> menu instanceof CuriosDesktopMenu)) continue;
                    var handler = CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler("ring")).orElseThrow();
                    Files.delete(command);
                    if (action.equals("grow") || action.equals("overflow")) {
                        handler.removeModifier(QA);
                        handler.addPermanentModifier(new AttributeModifier(QA, action.equals("overflow") ? 92 : 5, AttributeModifier.Operation.ADD_VALUE));
                    }
                    else if (action.equals("shrink")) {
                        handler.removeModifier(QA);
                        handler.removeModifier(Identifier.fromNamespaceAndPath("curios", "command"));
                    } else throw new IllegalArgumentException("Unknown QA capacity action: " + action);
                    Files.writeString(directory.resolve("capacity-result.txt"), action + ": native ring slots=" + handler.getStacks().getSlots());
                    break;
                }
            } catch (Exception failure) { throw new IllegalStateException("Curios capacity fixture failed", failure); }
        });
    }
}
