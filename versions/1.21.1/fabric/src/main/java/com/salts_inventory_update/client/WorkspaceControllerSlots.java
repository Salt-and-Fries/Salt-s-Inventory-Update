package com.salts_inventory_update.client;

/** Authored logical cells for native workstation slots, independent of decorative offsets. */
final class WorkspaceControllerSlots {
    record Cell(int row, int column) { }
    private WorkspaceControllerSlots() { }

    static Cell authored(String menu, int slot) {
        return switch (menu) {
            case "BrewingStandMenu" -> switch (slot) {
                case 0, 1, 2 -> new Cell(1, slot);
                case 3 -> new Cell(0, 1);
                case 4 -> new Cell(0, 0);
                default -> null;
            };
            case "FurnaceMenu", "BlastFurnaceMenu", "SmokerMenu" -> switch (slot) {
                case 0 -> new Cell(0, 0);
                case 1 -> new Cell(1, 0);
                case 2 -> new Cell(0, 1);
                default -> null;
            };
            case "GrindstoneMenu", "CartographyTableMenu" -> switch (slot) {
                case 0, 1 -> new Cell(slot, 0);
                case 2 -> new Cell(0, 1);
                default -> null;
            };
            case "CraftingMenu" -> slot == 0 ? new Cell(1, 3)
                : slot <= 9 && slot > 0 ? new Cell((slot - 1) / 3, (slot - 1) % 3) : null;
            case "CrafterMenu" -> slot >= 0 && slot < 9 ? new Cell(slot / 3, slot % 3) : null;
            case "InventoryMenu" -> slot == 0 ? new Cell(1, 2)
                : slot > 0 && slot <= 4 ? new Cell((slot - 1) / 2, (slot - 1) % 2) : null;
            case "AnvilMenu", "SmithingMenu", "MerchantMenu", "EnchantmentMenu" -> new Cell(0, slot);
            case "LoomMenu" -> slot < 3 ? new Cell(0, slot) : slot == 3 ? new Cell(1, 3) : null;
            default -> null;
        };
    }
}
