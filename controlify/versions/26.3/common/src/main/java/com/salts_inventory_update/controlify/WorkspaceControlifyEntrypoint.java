package com.salts_inventory_update.controlify;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;

/** Supported common SPI hook: platform ready, before either loader loads client resources. */
public final class WorkspaceControlifyEntrypoint implements ControlifyEntrypoint {
    @Override public void onControlifyPreInit(PreInitContext context) {
        WorkspaceActionGuide.initialize(context.contextualDomains());
    }

    @Override public void onControlifyInit(InitContext context) { }
    @Override public void onControllersDiscovered(ControlifyApi controlify) { }
}
