package example.desktop;

import com.salts_inventory_update.api.server.desktop.*;
import java.util.List;

/** Opts the example machine into server-resolved smelting recipe transfers. */
public final class MachineTransferHandler implements DesktopServerWindowHandler<MachineMenu, Void> {
    @Override public DesktopTransferDecision validateTransfer(DesktopTransferRequest<MachineMenu, Void> request) {
        var session = request.session();
        if (!session.menu().stillValid(session.player())) return DesktopTransferDecision.denied("Machine access denied");
        var plan = session.menu().normalizeRecipe(request.recipe());
        if (plan == null) return DesktopTransferDecision.unsupported();
        if (plan.alternatives().size() > 32) return DesktopTransferDecision.denied("Recipe exceeds this adapter's alternative limit");
        if (!session.menu().slots.get(plan.inputSlotId()).isActive()) return DesktopTransferDecision.denied("Input is disabled");
        return DesktopTransferDecision.allowed(
            List.of(new DesktopTransferRequirement(plan.inputSlotId(), 1, plan.alternatives())),
            List.of(plan.inputSlotId()), request.maxTransfer() ? plan.maximumCrafts() : 1);
    }
}
