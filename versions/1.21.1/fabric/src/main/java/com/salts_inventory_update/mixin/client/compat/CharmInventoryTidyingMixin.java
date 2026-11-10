package com.salts_inventory_update.mixin.client.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SIU supplies sorting for both player and container inventories.
 * Suppress only Charm's tidying feature, for every native screen rather than
 * a per-menu whitelist. Covers original full-screen menus, extracted native
 * windows and rebuilt/resized screens, without changing Charm configuration. */
@Pseudo
@Mixin(targets={
    "svenhjol.charm.feature.inventory_tidying.InventoryTidyingClient",
    "svenhjol.charm.feature.item_tidying.client.Handlers"
},remap=false)
public abstract class CharmInventoryTidyingMixin {
    // Legacy Charm 6 and modern Charm 7 name the same feature entry point
    // differently. Cancel setup before it can scan slots or attach widgets;
    // retain the factory guard for other callers of addSortingButton.
    @Inject(method={"handleScreenSetup","screenSetup","addSortingButton"},
        at=@At("HEAD"),cancellable=true,require=0)
    private void salts_inventory_update$hideRedundantTidyButton(CallbackInfo ci) {
        ci.cancel();
    }
}
