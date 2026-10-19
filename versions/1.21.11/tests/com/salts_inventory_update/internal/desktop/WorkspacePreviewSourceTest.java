package com.salts_inventory_update.internal.desktop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspacePreviewSourceTest {
    @Test void resolvesOnlyExactCurrentDimensionAndFirstDoubleChestBlock() {
        assertEquals(new WorkspacePreviewSource.Position(-12, 64, 300),
                WorkspacePreviewSource.blockPosition("block:mod:dimension:-12,64,300", "mod:dimension"));
        assertEquals(new WorkspacePreviewSource.Position(1, 2, 3),
                WorkspacePreviewSource.blockPosition("chest:minecraft:overworld:1,2,3|1,2,4", "minecraft:overworld"));
        assertNull(WorkspacePreviewSource.blockPosition("block:mod:dimension:1,2,3", "minecraft:overworld"));
    }

    @Test void rejectsMalformedAndNonBlockSourcesWithoutThrowing() {
        for (String address : new String[]{"entity:minecraft:overworld:1,2,3", "block:minecraft:overworld:1,2",
                "block:minecraft:overworld:1,2,3,4", "block:minecraft:overworld:no,2,3",
                "block:minecraft:overworld:2147483648,2,3", "chest:minecraft:overworld:|1,2,3"})
            assertNull(WorkspacePreviewSource.blockPosition(address, "minecraft:overworld"));
        assertNull(WorkspacePreviewSource.blockPosition(null, "minecraft:overworld"));
    }
}
