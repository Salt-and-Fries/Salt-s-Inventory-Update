# Quark windows across supported targets

Quark's official releases intersect two SIU targets. The same reviewed window
design is implemented in both Compatibility JARs; Quark remains an optional
external dependency. Core and the public API are unchanged.

| Minecraft | Loader | Reviewed Quark release | Windows |
| --- | --- | --- | --- |
| 1.20.1 | Forge | 4.0-462, Zeta 1.0-31 | Crate, Trough, Crafter, held Shulker, Backpack, Matrix Enchanting |
| 1.21.1 | NeoForge | 4.1-488, Zeta 1.1-40 | Crate, Trough, held Shulker, Backpack, Matrix Enchanting; Minecraft's Crafter uses SIU's existing vanilla window |

Publisher availability was checked on November 1, 2026 against
[Quark releases](https://modrinth.com/mod/quark/versions) and
[Quark Oddities releases](https://modrinth.com/mod/quark-oddities/versions).
There are no official Quark releases for SIU's Fabric targets, or for its
1.21.11, 26.1.2, 26.2 and 26.3 targets. Older Minecraft releases outside SIU's
supported target set are not added by this compatibility port. Unofficial forks
are not claimed as tested.

Crate, Matrix Enchanting and Backpack follow their Quark module/Oddities
availability. The 1.21.1 test profile uses NeoForge 21.1.252, which is required
by Quark 4.1-488. An older Quark installation may require a different NeoForge
patch; SIU's minimum Core loader requirement is not raised by this adapter.

## Preserved behavior

- The equipped Backpack opens above Inventory, without its own Close/X action.
  Both windows share lifetime, movement, pin and lock state. Native leather
  artwork and all 27 storage slots remain intact.
- Crate scrolling, search, capacity and original item rules remain active.
- Held Shulker contents retain native source-item protection and persistence.
- Matrix Enchanting uses its native puzzle, charge, piece docket, output and
  eligibility rules. Its native packets are addressed to the authenticated SIU
  menu session, with the original operations executed on the server.
- The modern port uses Minecraft item components for Backpack and held Shulker
  persistence. The 1.20.1 adapter retains its original NBT implementation.

The 1.20.1 implementation and its focused verification are documented in
[quark-1.20.1.md](quark-1.20.1.md). Modern focused observations, exact publisher
artifact hashes and launcher lineage are retained under
`build/quark-all-targets/` and
`build/remaining-ui-audit/gameplay/1.21.1-neoforge-integrated-quark-*`.

## Focused verification on 1.21.1

The final production NeoForge JAR was tested with the unmodified publisher
Quark, Zeta and Oddities JARs on NeoForge 21.1.252. The delivered JAR's SHA-256
matches the test profile's recorded input. The shared Fabric source set also
builds successfully, without claiming Quark availability on Fabric.

- All five Quark layouts and the existing vanilla Crafter window opened.
- Crate wheel scrolling, native search/clear and Trough transfers passed.
- Held Shulker transfers, source protection and contents after reopening passed.
- Backpack contents survived reopening; both headers moved the pair; pin/lock
  synchronized; parent close/reopen and GUI scaling passed. Native filled-bag
  binding and empty-bag unequip/re-equip behavior also passed.
- Matrix generation consumed XP/lapis. Piece selection, rotation, placement,
  removal, enchanted output and the visible native docket passed.
- A second profile without Oddities opened Trough and ordinary Inventory without
  adding a Backpack child.

The twelve existing Core JAR hashes were checked before and after staging and
were unchanged. Production archive checks cover metadata, configured mixin
classes, ZIP integrity and exclusion of test observer classes.

The complete target table, screenshots and verification summary are saved in
`output/quark-all-targets/`. The earlier 1.20.1 Forge JAR and its evidence remain
unchanged.

The separate optional Aether/Quark backpack integration is outside this
Quark-alone adapter. Focused window checks do not constitute a complete gameplay
suite or a test of every configured blacklist and optional cross-mod integration.
