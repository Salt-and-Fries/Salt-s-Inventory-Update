# Release notes draft

**Not approved for publication.** Candidate versions are Core 0.1.6,
Compatibility 0.1.3 and Controlify 0.1.1. The
[release manifest](../../output/release-readiness/release-manifest-final.json)
records exact accepted hashes and independent review status. See
[final verification](FINAL_VERIFICATION.md) for scoped evidence and limitations.

## Changes prepared for release

- Protects Gallery's complete held painting stack while its selector is open.
  Native search and selection use the owning window session, and closing keeps
  the exact native variant and item data. Applies to 1.20.1 Forge and 1.21.1
  NeoForge; the Core public API and ordinary native screen remain unchanged.
- Fixes Bountiful Analyzer refresh after decree insertion or removal on
  1.21.1 Fabric and NeoForge, retaining native mode and resolution controls.
- Project Everything adds dedicated inventory/container windows using the
  installed mods' native artwork, controls and slot rules. Version-specific AE2
  artwork follows each reviewed upstream release. See the
  [reviewed compatibility pins](compatibility-pins.md) for target scope.
- Conventional containers without a dedicated adapter retain their original
  upper interface between SIU's title/control bar and blank lower bar. Books,
  selectors and other deliberately native screens retain their own input.
- Equipment integrations use distinct launcher locations and exclusive editing
  surfaces. Aether reformats Character; Curios and Accessories use their joined
  equipment panels. Linked backpacks and aircraft panels keep their parent
  ownership and approved controls.
- Core and Compatibility communicate through supported public contracts.
  Existing exported API signatures remain binary compatible; new built-in
  subview and control-policy contracts have permissive defaults.
- Portable native hosts keep their real power drain and source protection.
  The click fixes retain strict cursor, editable-slot, topology and session
  validation while correcting publication ordering and modern revision handling.
- Tom's Storage terminal actions preserve Fabric's optional connection context
  when serializing item data. This fixes withdrawals with Polymer installed
  without changing the public API or packet format.
- Traveler's Backpack settings transitions preserve the canonical cursor and
  avoid committing through a retired menu context. Compatibility's Mixin
  descriptors declare their required minimum versions on every target.

## Additional fixes found during qualification

- Extended AE's optional client packet helpers remain isolated from dedicated
  server loading on the affected modern target.
- Native film renaming retains complete item data and the original publisher's
  confirmation and cancellation behavior.
- Complete modern text edits are coalesced, and inventory publication follows
  the last acknowledged player revision. Native field edits and inventory
  clicks therefore use consistent state while stale edits remain rejected.
- Create package addresses and stock windows retain native updates and closing.
- Mine Cells on 1.21.1 retains the native recipe identifier through recipe
  selection and crafting.
- Modern Industrialization excludes the foreign FTB sidebar button from its
  content measurements. The real native machine trays retain their own bounds.

The [repair-chain record](../../output/release-readiness/repair-prefix-equivalence-build14-loader-repairs-1466-01.json)
identifies the exact source/archive transitions and their focused native
checks. These are specific fixes, not another full-suite execution. Native
fixture initialization, observer corrections and corrected expected native
menu classes are test setup changes, not additional SIU product fixes.

## Installation and upgrading

Choose matching Minecraft/loader files and install publisher dependencies
separately. Core works independently; Compatibility and the Controlify companion
are optional. Follow the [installation guide](INSTALLATION.md), including the
client/server requirements, configuration location and world-backup procedure.
Replace previous module jars as a matching set; do not mix preview artifact
bytes merely because their filenames have the same version number.

## Qualification and limitations

[Final qualification](FINAL_VERIFICATION.md) binds the accepted candidate hashes
to actual new runs or independently verified retention of unaffected original
results. Earlier executions keep their original subjects and scopes. Focused
checks remain identified as focused checks; they are not repeated full suites.

The audited Charm Forked/Aether combination on 1.21.1 Fabric and
Bumblezone/Polymer combination on 26.1.2 Fabric reproduce native failures
without SIU. Compatible dependency-complete partitions have separate evidence;
the original combinations are not certified. See
[setup recovery](../../output/blocked-setup-recovery/summary.json) for exact
versions, causes and preserved controls.

Small Ships 2.0.2 on 1.21.1 NeoForge has a config-unload exception also recorded
without SIU using the identical mod jar. Its original menu assertions remain
separate from that upstream shutdown limitation. The
[diagnostic review](INSTALLATION_ACCEPTANCE.md#native-publisher-diagnostics)
retains the exact current log, publisher hash and earlier native-only control.

The Rocket Sensor keeps its original source-lifetime behavior. With Giselle
Addon 6.19 on 1.20.1 Fabric, 8.2 on 1.21.1 Fabric, and 8.1 on 1.21.1 NeoForge,
removing the source block leaves the native sensor menu open; ordinary close
still works. Matching SIU-absent controls confirm this behavior. The tested
1.20.1 Forge 6.20 menu closes automatically after source removal. The focused
settings and close checks passed on all four pins; automatic removal closure
is not claimed for the three persistent native menus. See the
[target-specific sensor evidence](../../output/release-readiness/sensor-all-native-witnesses-598-01/manifest.json)
for the exact publishers, native controls and original SIU workflows. This is
an upstream behavior limitation, not an SIU fix or a claim about other machines.

SmitherZ 1.0.4 on 1.20.1 Fabric has an upstream gem-recovery defect: the
native grinder previews an extracted gem but loses it when the result is taken.
A matching [SIU-absent control](../../output/release-readiness/smither-native-grinding-663-01/native-control.json)
confirms the defect with the exact publisher jar. SIU preserves that native
behavior; guaranteed gem recovery is not claimed for this pin. Cancellation,
material costs and the remaining item data have separate focused checks. Newer
publisher releases require their own extraction-return checks.

Simple Storage Network 1.13.2 retains two original upstream filter
limitations on the tested 1.20.1 Forge, 1.21.1 NeoForge and 26.1.2 NeoForge
pins. Both filter menus remain open after their source block is removed, then
close normally. The Collector keeps its visible ghost filter blank after
copying and reopening, while the server retains the setting; right-clicking
with the same carried item re-copies one ghost rather than incrementing it.

Independent SIU-absent controls reproduce source lifetime on
[1.20.1 Forge](../../output/release-readiness/ssn-native-filter-lifetime-672-01/native-control.json),
[1.21.1 NeoForge](../../output/release-readiness/ssn-native-lifetime-685-1211-01/native-control.json)
and [26.1.2 NeoForge](../../output/release-readiness/ssn-native-lifetime-689-2612-01/native-control.json),
and Collector synchronization on
[1.20.1 Forge](../../output/release-readiness/ssn-native-collector-ghost-678-01/native-control.json),
[1.21.1 NeoForge](../../output/release-readiness/ssn-native-collector-695-1211-01/native-control.json)
and [26.1.2 NeoForge](../../output/release-readiness/ssn-native-collector-695-2612-01/native-control.json).
SIU retains the native callbacks and item ownership; automatic source-removal
closure and Collector display/count synchronization are not claimed on these
exact pins. Other releases require their own focused checks.

On the same SSN 1.13.2 / 1.21.1 and 26.1.2 NeoForge pins, the original
process-cable redstone control changes its server setting while its cached
client capability and icon remain unchanged, including after ordinary reopen.
The original process menu also stays open after source removal. Independent
SIU-absent checks reproduce this on
[1.21.1 NeoForge](../../output/release-readiness/ssn-native-process-redstone-705-1211-01/native-control.json)
and [26.1.2 NeoForge](../../output/release-readiness/ssn-native-process-redstone-705-2612-01/native-control.json).
SIU preserves the original callbacks; immediate redstone icon synchronization
and automatic source-removal closure are not claimed on these pins. Ghost
recipes, server settings, item ownership and ordinary close retain separate
focused checks. Processing automation and adjacent-machine import are outside
that configuration-only evidence.

Additional Create addons deferred by the user remain outside this release's
agreed scope. Arbitrary upstream patches/modpacks, hardware controller models
and WAN conditions are not exhaustively certified by the local matrix.

Raised 6.0.2 on 1.21.11 Fabric and NeoForge has an upstream resource-pack
metadata error in the tested jars. Its build14 HUD interaction checks pass,
while the original metadata errors remain documented in the
[input diagnostic review](INSTALLATION_ACCEPTANCE.md#native-publisher-diagnostics).
No publisher patch or new SIU-absent control is claimed for this correlation.

Publishing, tagging, pushing and release-page changes require separate explicit
instructions and are not performed by this qualification task.

The pinned Accessories beta.48/beta.49 releases on 1.20.1 Fabric were qualified
with Sodium 0.5.11 and Indium 1.0.34. The initial fixture without Sodium failed
inside Accessories' native equipment rendering because its mixin referenced
an absent Sodium interface. Adding Sodium alone stopped at a No Indium warning;
the complete renderer setup passed the original equipment checks. These are
recorded setup limits, rather than a claimed SIU rendering repair or a new
SIU-absent control. The same Accessories releases without those renderer
prerequisites are not certified by these results.

The older beta.48 test partition additionally excludes Create Fabric 6.0.8.1
and its required Steam 'n' Rails 1.7.3 dependent. That original combination
failed before equipment testing when Create's world renderer called a
SpriteUtil class absent from Sodium 0.5.11. The supported test partition keeps
the exact Accessories release and every other original fixture artifact.
It qualifies Accessories equipment only; it does not certify the excluded
Create/Sodium combination. See the [preserved native crash and partition](../../output/release-readiness/accessories-older-renderer-partition-795-01/manifest.json)
and [six exact-release equipment results](../../output/release-readiness/accessories-six-release-native-qualified-804-01/manifest.json).
