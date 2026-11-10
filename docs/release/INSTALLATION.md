# Candidate installation

Use the exact candidate identified by the [release manifest](../../output/release-readiness/release-manifest-final.json).
Its scoped acceptance evidence and review status are described in [FINAL_VERIFICATION.md](FINAL_VERIFICATION.md).
The modules are Core **0.1.6**, Compatibility **0.1.3** and Controlify **0.1.1**.
No release is published by this qualification task.

## Choose the matching files

| Minecraft | Loaders | Minimum Java |
| --- | --- | ---: |
| 1.20.1 | Fabric, Forge | 17 |
| 1.21.1 | Fabric, NeoForge | 21 |
| 1.21.11 | Fabric, NeoForge | 21 |
| 26.1.2 | Fabric, NeoForge | 25 |
| 26.2 | Fabric, NeoForge | 25 |
| 26.3 | Fabric, NeoForge | 25 |

Use one Minecraft/loader folder from the combined archive. Put its Core jar in
`mods`; add the matching Compatibility jar for the reviewed integrations, and
the matching Controlify companion for controller support. Remove superseded
copies of these modules before starting. Install the loader/API dependencies
declared by those jars. Upstream mods and their dependencies are installed
separately; they are not bundled publisher implementations.

For full desktop inventory functionality, use matching Core builds on client
and server. Use matching Compatibility builds on both sides for integrations
that register menus, native packet bridges or server actions. The controller
companion and native Controlify/YACL dependencies belong on the client; follow
the [controller installation matrix](../../controlify/README.md). Detailed
one-sided and mismatch checks are recorded in the
[installation acceptance record](INSTALLATION_ACCEPTANCE.md). A successful
server boot alone does not establish multiplayer functionality.

The [reviewed support table](compatibility-pins.md) records exact upstream
releases and loaders. Registered inventory windows, automatic fallback and
intentionally native screens have different scopes. An available Core jar
does not establish that every optional mod is supported on that target.

The [pinned dependency guide](UPSTREAM_DEPENDENCIES.md) lists base mods and libraries declared by those exact publisher jars, including version and side constraints. Follow the publisher or mod manager when installing them; some libraries are already bundled inside their owning mod.

## First run and configuration

Use Minecraft's Inventory binding to open the workspace. In a world, run
`/saltsinventory config` to open SIU's settings, or `/saltsinventory help` to
reopen the guide. The mod-list Config button is another route when available;
Fabric's mod-list route uses Mod Menu. Configure bindings under **Controls >
Key Binds > Salt's Inventory Update**. Window Movement Control defaults to
Left Alt; the Hold/Toggle preference uses the same binding.

Client preferences are stored in the instance's
`config/salts_inventory_update/config.json`. Change settings in game and close
the game before editing files. Native machine, trading and equipment rules
continue to come from their owning mods.

Before upgrading an existing world, stop the server/game and copy the whole
world plus the instance's configuration. Keep the previous jars and their
checksums with that backup. Test the upgrade on a copy, including expanded
inventory contents and modded equipment. Restore the backup together with its
matching mods if rolling back; avoid mixing an older player-data snapshot
with newer world storage.

## Known scope limits

Charm Forked plus Aether on the audited 1.21.1 Fabric combination, and
Bumblezone plus Polymer on the audited 26.1.2 Fabric combination, have native
failures that reproduce without SIU. Their compatible partition fixtures
passed separate tests. Exact versions and evidence are retained in
[setup recovery](../../output/blocked-setup-recovery/summary.json).
The pinned Tech Reborn 6.0.5/RebornCore 6.0.5 resources also report missing
`reborncore:padded` recipe serializers during world load. Window/configuration
and formed-heat evidence does not certify those affected native recipes;
see the [exact Tech Reborn scope](../../compatibility/tech-reborn-all-targets.md).
Small Ships 2.0.2 on 1.21.1 NeoForge also has a config-unload exception recorded
with and without SIU using the identical publisher jar. Its menu results do
not certify that upstream shutdown behavior; see the
[diagnostic evidence](INSTALLATION_ACCEPTANCE.md#native-publisher-diagnostics).
Additional Create addons deferred by the user are outside the agreed release
scope. Unknown upstream patches, arbitrary modpacks, hardware controller
models and WAN conditions require their own qualification.

## Reporting a problem

Use the repository's issue tracker and include Minecraft, loader, Java, all
three installed SIU module versions, relevant upstream versions, `latest.log`
and any crash report. Give the entry path, expected behavior and exact steps;
include GUI scale, resource packs and whether the problem occurs with SIU
removed. For inventory problems, describe the items, counts and components/NBT
involved and provide a reproducible test-world copy when practical.
