package example.desktop;

import java.util.Objects;
import net.minecraft.world.inventory.MenuType;

/** The ordinary mod assigns its registered menu types during its normal loader registration. */
public final class ExampleMenus {
    private static MenuType<MachineMenu> machine;
    private static MenuType<TerminalMenu> terminal;
    public static void bind(MenuType<MachineMenu> machineType, MenuType<TerminalMenu> terminalType) {
        machine = Objects.requireNonNull(machineType);
        terminal = Objects.requireNonNull(terminalType);
    }
    public static MenuType<MachineMenu> machine() { return Objects.requireNonNull(machine, "Register and bind the machine menu first"); }
    public static MenuType<TerminalMenu> terminal() { return Objects.requireNonNull(terminal, "Register and bind the terminal menu first"); }
}
