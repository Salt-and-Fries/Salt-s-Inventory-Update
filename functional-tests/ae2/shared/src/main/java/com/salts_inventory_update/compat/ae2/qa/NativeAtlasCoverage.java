package com.salts_inventory_update.compat.ae2.qa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.TreeSet;

/** Reviewed native presentation coverage, independent of cross-version artwork. QA only. */
public final class NativeAtlasCoverage {
    private NativeAtlasCoverage() { }
    private static final String PRESENTATIONS = """
            cell-workbench
            cell-workbench-toolbox-pickup
            cell-workbench-toolbox-return
            craft-amount
            craft-confirm
            craft-confirm-error-cpu_busy
            craft-confirm-error-cpu_busy-return
            craft-confirm-error-cpu_offline
            craft-confirm-error-cpu_offline-return
            craft-confirm-error-cpu_too_small
            craft-confirm-error-cpu_too_small-return
            craft-confirm-error-incomplete_plan
            craft-confirm-error-incomplete_plan-return
            craft-confirm-error-missing_ingredient
            craft-confirm-error-missing_ingredient-return
            craft-confirm-error-no_cpu_found
            craft-confirm-error-no_cpu_found-return
            craft-confirm-error-no_suitable_cpu_found
            craft-confirm-error-no_suitable_cpu_found-return
            crafting-cpu
            crafting-status
            crafting-terminal
            crafting-terminal-key-types
            crafting-terminal-key-types-return
            crafting-terminal-settings
            crafting-terminal-settings-return
            energy-level-emitter
            export-bus
            formation-plane
            import-bus
            import-bus-key-types
            import-bus-key-types-return
            inscriber
            matter-condenser
            me-chest
            me-chest-storage
            me-chest-storage-settings
            me-chest-storage-settings-return
            me-drive
            me-interface
            me-io-port
            me-terminal
            me-terminal-key-types
            me-terminal-key-types-return
            me-terminal-settings
            me-terminal-settings-return
            molecular-assembler
            network-status-controller
            network-status-tool
            network-tool
            pattern-access-terminal
            pattern-encoding-terminal
            pattern-encoding-terminal-key-types
            pattern-encoding-terminal-key-types-return
            pattern-encoding-terminal-mode-crafting-patterns
            pattern-encoding-terminal-mode-processing-patterns
            pattern-encoding-terminal-mode-smithing-table-patterns
            pattern-encoding-terminal-mode-stonecutting-patterns
            pattern-encoding-terminal-processing-amount
            pattern-encoding-terminal-processing-amount-return
            pattern-encoding-terminal-settings
            pattern-encoding-terminal-settings-return
            pattern-provider
            portable-fluid-cell
            portable-fluid-cell-settings
            portable-fluid-cell-settings-return
            portable-item-cell
            portable-item-cell-settings
            portable-item-cell-settings-return
            priority
            quantum-network-bridge
            quartz-cutting-knife
            sky-stone-chest
            spatial-anchor
            spatial-io-port
            stock-amount
            storage-bus
            storage-level-emitter
            vibration-chamber
            wireless-access-point
            wireless-crafting-terminal
            wireless-crafting-terminal-key-types
            wireless-crafting-terminal-key-types-return
            wireless-crafting-terminal-settings
            wireless-crafting-terminal-settings-return
            wireless-terminal
            wireless-terminal-key-types
            wireless-terminal-key-types-return
            wireless-terminal-settings
            wireless-terminal-settings-return
            """;

    public static void verify(Path atlas, String minecraft) throws IOException {
        boolean legacy = minecraft.equals("1.20.1");
        boolean keyTypes = available("appeng.client.gui.implementations.KeyTypeSelectionScreen");
        boolean separateChest = available("appeng.menu.me.items.BasicCellChestMenu");
        if (keyTypes == legacy || separateChest == legacy)
            throw new IllegalStateException("Upstream AE2 features differ from this reviewed coverage profile");
        var expected = new TreeSet<String>(PRESENTATIONS.lines().map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (legacy) expected.removeIf(label -> label.endsWith("-key-types") || label.endsWith("-key-types-return")
                || label.equals("me-chest-storage-settings") || label.equals("me-chest-storage-settings-return"));
        var rows = Files.readAllLines(atlas.resolve("manifest.csv"));
        var actual = new TreeSet<String>();
        var menus = new HashSet<String>();
        var images = new HashSet<String>();
        for (String row : rows.subList(1, rows.size())) {
            String[] columns = row.split(",");
            if (columns.length != 18 || !actual.add(columns[1]) || !images.add(columns[9]))
                throw new IllegalStateException("Invalid or duplicate atlas presentation: " + row);
            menus.add(columns[2]);
            if (!Files.isRegularFile(atlas.resolve(columns[9]))
                    || !Files.isRegularFile(atlas.resolve(columns[9].replace(".png", ".geometry.json"))))
                throw new IllegalStateException("Missing screenshot or geometry: " + columns[1]);
        }
        var missing = new TreeSet<String>(expected); missing.removeAll(actual);
        var extra = new TreeSet<String>(actual); extra.removeAll(expected);
        long count;
        try (var files = Files.list(atlas)) { count = files.filter(p -> p.toString().endsWith(".png")).count(); }
        if (!missing.isEmpty() || !extra.isEmpty() || count != expected.size() || menus.size() != (legacy ? 39 : 40))
            throw new IllegalStateException("Native coverage mismatch; missing=" + missing + ", extra=" + extra
                    + ", images=" + count + ", native menus=" + menus.size());
        Files.writeString(atlas.resolve("native-coverage.txt"), "Minecraft: " + minecraft + "\nNative menus: "
                + menus.size() + "\nPresentations: " + expected.size() + "\nVersion-specific native coverage: PASS\n");
    }

    private static boolean available(String name) {
        return NativeAtlasCoverage.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }
}
