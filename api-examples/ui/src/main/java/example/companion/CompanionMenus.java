package example.companion;

import java.util.Objects;
import net.minecraft.world.inventory.MenuType;

/** Bind these registered menu types from the ordinary mod's loader lifecycle, before Salt discovery. */
public final class CompanionMenus {
    private static MenuType<CompanionMainMenu> main;
    private static MenuType<ToolInventoryMenu> tool;
    public static void bind(MenuType<CompanionMainMenu> mainType, MenuType<ToolInventoryMenu> toolType) {
        main = Objects.requireNonNull(mainType); tool = Objects.requireNonNull(toolType);
    }
    public static MenuType<CompanionMainMenu> main() { return Objects.requireNonNull(main, "Bind main menu first"); }
    public static MenuType<ToolInventoryMenu> tool() { return Objects.requireNonNull(tool, "Bind tool menu first"); }
    private CompanionMenus() { }
}
